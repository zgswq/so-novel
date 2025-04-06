package com.pcdd.sonovel.web.service.impl;

import com.pcdd.sonovel.core.Crawler;
import com.pcdd.sonovel.core.Source;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Book;
import com.pcdd.sonovel.model.Chapter;
import com.pcdd.sonovel.model.SearchResult;
import com.pcdd.sonovel.parse.BookParser;
import com.pcdd.sonovel.parse.TocParser;
import com.pcdd.sonovel.util.ConfigUtils;
import com.pcdd.sonovel.web.config.AppProperties;
import com.pcdd.sonovel.web.dto.DownloadTaskDTO;
import com.pcdd.sonovel.web.service.DownloadService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 下载服务实现类
 * 
 * @author pcdd
 */
@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    private final Map<String, DownloadTaskDTO> taskMap = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> futureMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AppProperties appProperties;
    
    @Autowired
    public DownloadServiceImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.executorService = Executors.newFixedThreadPool(appProperties.getMaxConcurrentDownloads());
    }
    
    @Override
    public String createDownloadTask(String bookId, String format) {
        // 创建下载任务
        String taskId = UUID.randomUUID().toString();
        DownloadTaskDTO task = new DownloadTaskDTO();
        task.setTaskId(taskId);
        task.setFormat(format);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        
        // 保存任务
        taskMap.put(taskId, task);
        
        // 异步执行下载，并保存Future对象用于取消任务
        Future<?> future = executorService.submit(() -> startDownload(taskId, bookId, format));
        futureMap.put(taskId, future);
        
        return taskId;
    }
    
    @Async
    public void startDownload(String taskId, String bookId, String format) {
        DownloadTaskDTO task = taskMap.get(taskId);
        if (task == null) {
            return;
        }
        
        try {
            task.setStatus("RUNNING");
            log.info("开始下载任务: {}, bookId: {}, format: {}", taskId, bookId, format);
            
            // 定期检查任务是否被取消
            if (isCanceled(task)) {
                log.info("任务已取消: {}", taskId);
                return;
            }
            
            // 获取配置并设置下载格式
            AppConfig config = ConfigUtils.config();
            config.setExtName(format);
            
            // 先解析书籍信息
            log.info("解析书籍信息: {}", bookId);
            Book book = new BookParser(config).parse(bookId);
            
            if (book == null) {
                throw new RuntimeException("无法获取书籍信息");
            }
            
            // 定期检查任务是否被取消
            if (isCanceled(task)) {
                log.info("任务已取消: {}", taskId);
                return;
            }
            
            // 更新任务信息
            task.setTitle(book.getBookName());
            task.setAuthor(book.getAuthor());
            
            // 然后解析章节列表
            log.info("解析章节列表: {}", bookId);
            TocParser tocParser = new TocParser(config);
            List<Chapter> chapters = tocParser.parse(bookId, 1, Integer.MAX_VALUE);
            
            // 定期检查任务是否被取消
            if (isCanceled(task)) {
                log.info("任务已取消: {}", taskId);
                return;
            }
            
            log.info("获取到章节数量: {}", chapters.size());
            
            // 创建SearchResult对象，并填充完整信息
            Source source = new Source(config);
            SearchResult searchResult = SearchResult.builder()
                    .url(bookId)
                    .sourceId(source.rule.getId())
                    .bookName(book.getBookName())
                    .author(book.getAuthor())
                    .intro(book.getIntro())
                    .category(book.getCategory())
                    .build();
            
            // 更新任务信息
            task.setTotalChapters(chapters.size());
            
            // 创建一个Crawler，并设置章节下载回调
            Crawler crawler = new Crawler(config);
            crawler.setOnChapterDownloaded((current, total) -> {
                // 检查任务是否被取消
                if (isCanceled(task)) {
                    Thread.currentThread().interrupt(); // 尝试中断当前线程
                    return;
                }
                
                task.setDownloadedChapters(current);
                int progress = (int) (current * 100.0 / total);
                task.setProgress(progress);
                log.info("下载进度更新 - 任务: {}, 进度: {}%, 已下载: {}/{}", taskId, progress, current, total);
            });
            
            // 执行下载
            double timeSpent = crawler.crawl(searchResult, chapters);
            
            // 最后一次检查任务是否被取消
            if (isCanceled(task)) {
                log.info("任务已取消: {}", taskId);
                return;
            }
            
            log.info("下载完成 - 任务: {}, 耗时: {} 秒", taskId, timeSpent);
            
            // 获取下载的文件路径
            String downloadDir = config.getDownloadPath();
            String bookDir = String.format("%s (%s) %s", searchResult.getBookName(), searchResult.getAuthor(), format.toUpperCase());
            String downloadPath = Paths.get(downloadDir, bookDir).toString();
            
            // 尝试找到生成的文件（EPUB、TXT等）
            File downloadFolder = new File(downloadPath);
            File[] files = downloadFolder.listFiles((dir, name) -> name.toLowerCase().endsWith("." + format.toLowerCase()));
            
            // 更新任务信息
            if (files != null && files.length > 0) {
                // 使用相对路径和API端点
                String fileName = bookDir + "/" + files[0].getName();
                task.setDownloadUrl("/api/file/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
            } else {
                // 如果找不到具体文件，不再使用目录而是设置错误信息
                log.error("找不到生成的{}文件", format);
                task.setStatus("FAILED");
                task.setErrorMessage("找不到生成的" + format + "文件");
                return;
            }
            
            task.setFinishTime(LocalDateTime.now());
            task.setStatus("COMPLETED");
            task.setProgress(100);
            
        } catch (Exception e) {
            // 检查是否是由于取消导致的异常
            if (task.getStatus().equals("CANCELED")) {
                log.info("任务已被取消: {}", taskId);
            } else {
                log.error("下载任务执行失败: " + taskId, e);
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
            }
        } finally {
            // 清理Future
            futureMap.remove(taskId);
        }
    }
    
    /**
     * 检查任务是否已被取消
     */
    private boolean isCanceled(DownloadTaskDTO task) {
        return "CANCELED".equals(task.getStatus());
    }
    
    @Override
    public DownloadTaskDTO getTaskInfo(String taskId) {
        return taskMap.get(taskId);
    }
    
    @Override
    public List<DownloadTaskDTO> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        DownloadTaskDTO task = taskMap.get(taskId);
        if (task == null) {
            return false;
        }
        
        if (!"COMPLETED".equals(task.getStatus()) && !"FAILED".equals(task.getStatus())) {
            // 设置任务状态为已取消
            task.setStatus("CANCELED");
            log.info("标记任务为已取消: {}", taskId);
            
            // 尝试取消正在运行的Future
            Future<?> future = futureMap.get(taskId);
            if (future != null && !future.isDone() && !future.isCancelled()) {
                log.info("尝试取消下载任务: {}", taskId);
                future.cancel(true); // 尝试中断正在执行的任务
            }
            
            return true;
        }
        return false;
    }
} 
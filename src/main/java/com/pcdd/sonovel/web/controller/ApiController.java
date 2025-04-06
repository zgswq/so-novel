package com.pcdd.sonovel.web.controller;

import com.pcdd.sonovel.web.config.AppProperties;
import com.pcdd.sonovel.web.dto.DownloadTaskDTO;
import com.pcdd.sonovel.web.dto.ResponseResult;
import com.pcdd.sonovel.web.dto.SearchResultDTO;
import com.pcdd.sonovel.web.dto.SourceInfoDTO;
import com.pcdd.sonovel.web.service.DownloadService;
import com.pcdd.sonovel.web.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * API控制器
 * 
 * @author pcdd
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {
    
    private final SearchService searchService;
    private final DownloadService downloadService;
    private final AppProperties appProperties;
    
    @Autowired
    public ApiController(SearchService searchService, DownloadService downloadService, AppProperties appProperties) {
        this.searchService = searchService;
        this.downloadService = downloadService;
        this.appProperties = appProperties;
    }
    
    /**
     * 聚合搜索
     * 
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseResult<List<SearchResultDTO>> search(@RequestParam String keyword) {
        log.info("搜索关键词: {}", keyword);
        return ResponseResult.success(searchService.searchNovel(keyword));
    }
    
    /**
     * 指定书源搜索
     * 
     * @param keyword 关键词
     * @param sourceId 书源ID
     * @return 搜索结果
     */
    @GetMapping("/search/{sourceId}")
    public ResponseResult<List<SearchResultDTO>> searchFromSource(
            @RequestParam String keyword,
            @PathVariable Integer sourceId) {
        log.info("从书源 {} 搜索关键词: {}", sourceId, keyword);
        return ResponseResult.success(searchService.searchNovelFromSource(keyword, sourceId));
    }
    
    /**
     * 获取所有书源
     * 
     * @return 书源列表
     */
    @GetMapping("/sources")
    public ResponseResult<List<SourceInfoDTO>> getAllSources() {
        return ResponseResult.success(searchService.getAllSources());
    }
    
    /**
     * 创建下载任务
     * 
     * @param bookId 书籍ID
     * @param format 下载格式
     * @return 任务ID
     */
    @PostMapping("/download")
    public ResponseResult<String> createDownloadTask(
            @RequestParam String bookId,
            @RequestParam(defaultValue = "epub") String format) {
        log.info("创建下载任务: bookId={}, format={}", bookId, format);
        String taskId = downloadService.createDownloadTask(bookId, format);
        return ResponseResult.success(taskId);
    }
    
    /**
     * 获取下载任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/download/{taskId}")
    public ResponseResult<DownloadTaskDTO> getTaskStatus(@PathVariable String taskId) {
        DownloadTaskDTO task = downloadService.getTaskInfo(taskId);
        if (task == null) {
            return ResponseResult.error("任务不存在");
        }
        return ResponseResult.success(task);
    }
    
    /**
     * 获取所有下载任务
     * 
     * @return 任务列表
     */
    @GetMapping("/downloads")
    public ResponseResult<List<DownloadTaskDTO>> getAllTasks() {
        return ResponseResult.success(downloadService.getAllTasks());
    }
    
    /**
     * 取消下载任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    @DeleteMapping("/download/{taskId}")
    public ResponseResult<Boolean> cancelTask(@PathVariable String taskId) {
        boolean success = downloadService.cancelTask(taskId);
        if (success) {
            return ResponseResult.success(true);
        } else {
            return ResponseResult.error("取消任务失败");
        }
    }
    
    /**
     * 下载文件端点
     * 
     * @param fileName 文件名
     * @return 文件资源
     */
    @GetMapping("/file/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        // 构建文件路径
        Path filePath = Paths.get(appProperties.getDownloadPath(), fileName);
        File file = filePath.toFile();
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        // 检查是否是目录，如果是目录则返回错误
        if (file.isDirectory()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        FileSystemResource resource = new FileSystemResource(file);
        
        try {
            // 设置Content-Disposition头，使浏览器下载文件
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 
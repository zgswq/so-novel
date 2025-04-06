package com.pcdd.sonovel.web.controller;

import com.pcdd.sonovel.web.config.AppProperties;
import com.pcdd.sonovel.web.dto.DownloadTaskDTO;
import com.pcdd.sonovel.web.dto.SourceInfoDTO;
import com.pcdd.sonovel.web.service.DownloadService;
import com.pcdd.sonovel.web.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Web页面控制器
 * 
 * @author zgswq
 */
@Slf4j
@Controller
public class WebController {
    
    private final SearchService searchService;
    private final DownloadService downloadService;
    private final AppProperties appProperties;
    
    @Autowired
    public WebController(SearchService searchService, DownloadService downloadService, AppProperties appProperties) {
        this.searchService = searchService;
        this.downloadService = downloadService;
        this.appProperties = appProperties;
    }
    
    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("version", appProperties.getVersion());
        
        // 获取书源列表
        List<SourceInfoDTO> sources = searchService.getAllSources();
        model.addAttribute("sources", sources);
        
        return "index";
    }
    
    /**
     * 搜索结果页面
     */
    @GetMapping("/search")
    public String search(@RequestParam String keyword, 
                         @RequestParam(required = false) Integer sourceId,
                         Model model) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("sourceId", sourceId);
        
        if (sourceId != null) {
            model.addAttribute("results", searchService.searchNovelFromSource(keyword, sourceId));
        } else {
            model.addAttribute("results", searchService.searchNovel(keyword));
        }
        
        return "search";
    }
    
    /**
     * 下载任务列表页面
     */
    @GetMapping("/downloads")
    public String downloads(Model model) {
        List<DownloadTaskDTO> tasks = downloadService.getAllTasks();
        model.addAttribute("tasks", tasks);
        
        return "downloads";
    }
    
    /**
     * 下载任务详情页面
     */
    @GetMapping("/download/{taskId}")
    public String downloadDetail(@PathVariable String taskId, Model model) {
        DownloadTaskDTO task = downloadService.getTaskInfo(taskId);
        model.addAttribute("task", task);
        
        return "download-detail";
    }
    
    /**
     * 关于页面
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("version", appProperties.getVersion());
        return "about";
    }
} 
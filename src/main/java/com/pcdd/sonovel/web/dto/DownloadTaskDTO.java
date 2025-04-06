package com.pcdd.sonovel.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载任务DTO
 * 
 * @author zgswq
 */
@Data
public class DownloadTaskDTO {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 书名
     */
    private String title;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 下载进度，0-100
     */
    private int progress;
    
    /**
     * 总章节数
     */
    private int totalChapters;
    
    /**
     * 已下载章节数
     */
    private int downloadedChapters;
    
    /**
     * 下载文件格式
     */
    private String format;
    
    /**
     * 任务状态：PENDING, RUNNING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 下载文件地址
     */
    private String downloadUrl;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
} 
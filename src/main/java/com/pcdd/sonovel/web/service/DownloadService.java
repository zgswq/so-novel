package com.pcdd.sonovel.web.service;

import com.pcdd.sonovel.web.dto.DownloadTaskDTO;

import java.util.List;

/**
 * 下载服务接口
 * 
 * @author zgswq
 */
public interface DownloadService {
    
    /**
     * 创建下载任务
     * 
     * @param bookId 书籍ID
     * @param format 下载格式：epub, txt, html
     * @return 下载任务ID
     */
    String createDownloadTask(String bookId, String format);
    
    /**
     * 获取下载任务信息
     * 
     * @param taskId 任务ID
     * @return 下载任务信息
     */
    DownloadTaskDTO getTaskInfo(String taskId);
    
    /**
     * 获取所有下载任务
     * 
     * @return 下载任务列表
     */
    List<DownloadTaskDTO> getAllTasks();
    
    /**
     * 取消下载任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(String taskId);
} 
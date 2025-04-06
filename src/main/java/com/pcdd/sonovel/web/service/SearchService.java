package com.pcdd.sonovel.web.service;

import com.pcdd.sonovel.web.dto.SearchResultDTO;
import com.pcdd.sonovel.web.dto.SourceInfoDTO;

import java.util.List;

/**
 * 搜索服务接口
 * 
 * @author zgswq
 */
public interface SearchService {
    
    /**
     * 聚合搜索小说
     * 
     * @param keyword 关键词
     * @return 搜索结果列表
     */
    List<SearchResultDTO> searchNovel(String keyword);
    
    /**
     * 从特定书源搜索小说
     * 
     * @param keyword 关键词
     * @param sourceId 书源ID
     * @return 搜索结果列表
     */
    List<SearchResultDTO> searchNovelFromSource(String keyword, Integer sourceId);
    
    /**
     * 获取所有可用书源
     * 
     * @return 书源信息列表
     */
    List<SourceInfoDTO> getAllSources();
} 
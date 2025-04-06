package com.pcdd.sonovel.web.dto;

import lombok.Data;

/**
 * 搜索结果DTO
 * 
 * @author zgswq
 */
@Data
public class SearchResultDTO {
    /**
     * 书籍ID
     */
    private String bookId;
    
    /**
     * 书名
     */
    private String title;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 最新章节
     */
    private String latestChapter;
    
    /**
     * 书籍来源
     */
    private String source;
    
    /**
     * 封面图片URL
     */
    private String coverUrl;
    
    /**
     * 简介
     */
    private String description;
    
    /**
     * 书籍详情页URL
     */
    private String detailUrl;
} 
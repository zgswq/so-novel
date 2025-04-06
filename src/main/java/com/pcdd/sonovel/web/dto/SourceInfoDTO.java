package com.pcdd.sonovel.web.dto;

import lombok.Data;

/**
 * 书源信息DTO
 * 
 * @author zgswq
 */
@Data
public class SourceInfoDTO {
    /**
     * 书源ID
     */
    private Integer id;
    
    /**
     * 书源名称
     */
    private String name;
    
    /**
     * 书源网址
     */
    private String url;
    
    /**
     * 书源状态：是否可用
     */
    private boolean available;
    
    /**
     * 书源描述
     */
    private String description;
} 
package com.pcdd.sonovel.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 应用配置属性
 * 
 * @author zgswq
 */
@Data
@Component
@ConfigurationProperties(prefix = "so-novel")
public class AppProperties {
    private String version;
    private String downloadPath;
    private String extname;
    private Integer maxConcurrentDownloads;
    private String bookDirFormat;
} 
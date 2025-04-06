package com.pcdd.sonovel.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置
 * 
 * @author pcdd
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;
    
    @Autowired
    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射下载目录为静态资源
        String downloadPath = appProperties.getDownloadPath();
        registry.addResourceHandler("/downloads/**")
                .addResourceLocations("file:" + Paths.get(downloadPath).toAbsolutePath().toString() + "/");
    }
} 
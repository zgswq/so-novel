package com.pcdd.sonovel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot应用入口类
 * 
 * @author zgswq
 */
@SpringBootApplication
@EnableAsync // 启用异步支持，用于下载任务
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
} 
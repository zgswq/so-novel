package com.pcdd.sonovel.web.service.impl;

import cn.hutool.core.lang.Console;
import com.pcdd.sonovel.core.Source;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Rule;
import com.pcdd.sonovel.model.SearchResult;
import com.pcdd.sonovel.parse.SearchParser;
import com.pcdd.sonovel.parse.SearchParser6;
import com.pcdd.sonovel.util.ConfigUtils;
import com.pcdd.sonovel.util.SourceUtils;
import com.pcdd.sonovel.web.dto.SearchResultDTO;
import com.pcdd.sonovel.web.dto.SourceInfoDTO;
import com.pcdd.sonovel.web.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * 
 * @author zgswq
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final AppConfig config;
    
    public SearchServiceImpl() {
        this.config = ConfigUtils.config();
    }
    
    @Override
    public List<SearchResultDTO> searchNovel(String keyword) {
        try {
            List<Source> searchableSources = SourceUtils.getSearchableSources();
            ExecutorService threadPool = Executors.newFixedThreadPool(searchableSources.size());
            List<List<SearchResult>> results = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(searchableSources.size());
            
            for (Source source : searchableSources) {
                threadPool.execute(() -> {
                    try {
                        // 根据书源ID决定使用哪个解析器
                        List<SearchResult> res;
                        if (source.rule.getId() == 6) {
                            res = new SearchParser6(source.config).parse(keyword);
                        } else {
                            res = new SearchParser(source.config).parse(keyword);
                        }
                        
                        Rule rule = source.rule;
                        log.info("书源 {} ({}) 搜索到 {} 条记录", rule.getId(), rule.getName(), res.size());
                        results.add(res);
                    } catch (Exception e) {
                        log.error("书源 {} 搜索异常", source.rule.getId(), e);
                        results.add(Collections.emptyList());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            threadPool.shutdown();
            
            List<SearchResult> flatList = new ArrayList<>();
            results.forEach(flatList::addAll);
            
            return convertToDTO(flatList);
        } catch (Exception e) {
            log.error("聚合搜索小说失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SearchResultDTO> searchNovelFromSource(String keyword, Integer sourceId) {
        try {
            // 创建配置副本
            AppConfig tmpConfig = new AppConfig();
            tmpConfig.setSourceId(sourceId);
            
            // 根据书源ID决定使用哪个解析器
            List<SearchResult> results;
            if (sourceId == 6) {
                results = new SearchParser6(tmpConfig).parse(keyword);
            } else {
                results = new SearchParser(tmpConfig).parse(keyword);
            }
            
            log.info("从书源 {} 搜索关键词: {}, 结果数: {}", sourceId, keyword, results.size());
            
            return convertToDTO(results);
        } catch (Exception e) {
            log.error("从指定书源搜索小说失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SourceInfoDTO> getAllSources() {
        try {
            List<SourceInfoDTO> sourceDTOs = new ArrayList<>();
            
            // 获取所有书源
            for (Integer sourceId : SourceUtils.ALL_IDS) {
                try {
                    Source source = new Source(sourceId);
                    Rule rule = source.rule;
                    
                    SourceInfoDTO dto = new SourceInfoDTO();
                    dto.setId(rule.getId());
                    dto.setName(rule.getName());
                    dto.setUrl(rule.getUrl());
                    dto.setAvailable(true); // 实际项目中可能需要检测可用性
                    dto.setDescription(rule.getComment());
                    
                    sourceDTOs.add(dto);
                } catch (Exception e) {
                    log.error("获取书源 {} 信息失败", sourceId, e);
                }
            }
            
            return sourceDTOs;
        } catch (Exception e) {
            log.error("获取所有书源失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 将搜索结果转换为DTO
     */
    private List<SearchResultDTO> convertToDTO(List<SearchResult> results) {
        return results.stream().map(r -> {
            SearchResultDTO dto = new SearchResultDTO();
            dto.setBookId(r.getUrl());
            dto.setTitle(r.getBookName());
            dto.setAuthor(r.getAuthor());
            dto.setLatestChapter(r.getLatestChapter());
            dto.setSource(r.getSourceId() != null ? r.getSourceId().toString() : "");
            dto.setCoverUrl("");
            dto.setDescription(r.getIntro());
            dto.setDetailUrl(r.getUrl());
            return dto;
        }).collect(Collectors.toList());
    }
} 
package com.sky.filter.service;

import com.sky.filter.factory.BloomFilterFactory;
import com.sky.filter.bloom.BloomFilter;
import com.sky.filter.hash.SimpleHashStrategy;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BloomFilterService {

    private BloomFilter bloomFilter;

    @PostConstruct
    public void init() {
        log.info("初始化布隆过滤器服务");
        bloomFilter = BloomFilterFactory.createBloomFilter(10000, new SimpleHashStrategy());
    }

    public void addKey(String key) {
        bloomFilter.add(key);
    }

    public boolean containsKey(String key) {
        return bloomFilter.contains(key);
    }
}
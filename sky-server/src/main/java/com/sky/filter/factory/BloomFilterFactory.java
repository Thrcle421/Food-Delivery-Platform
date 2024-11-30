package com.sky.filter.factory;

import com.sky.filter.bloom.BloomFilter;
import com.sky.filter.bloom.SimpleBloomFilter;
import com.sky.filter.hash.HashStrategy;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BloomFilterFactory {
    public static BloomFilter createBloomFilter(int size, HashStrategy hashStrategy) {
        log.info("创建布隆过滤器, size: {}", size);
        return new SimpleBloomFilter(size, hashStrategy);
    }
}
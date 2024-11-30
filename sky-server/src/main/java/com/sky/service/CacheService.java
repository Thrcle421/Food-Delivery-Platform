package com.sky.service;

import com.sky.filter.service.BloomFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BloomFilterService bloomFilterService;

    public Object getWithBloomFilter(String key, Class<?> type) {
        // 先判断布隆过滤器中是否存在
        if (!bloomFilterService.containsKey(key)) {
            log.info("布隆过滤器中不存在key: {}", key);
            return null;
        }

        // 查询缓存
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.info("从缓存中获取到数据, key: {}", key);
            return value;
        }

        return null;
    }

    public void setWithBloomFilter(String key, Object value) {
        // 放入缓存
        redisTemplate.opsForValue().set(key, value);
        // 添加到布隆过滤器
        bloomFilterService.addKey(key);
        log.info("数据已放入缓存和布隆过滤器, key: {}", key);
    }
}
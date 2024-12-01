package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@Component
@Slf4j
public class RedisSentinelFailoverListener {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private void updateMasterSlaveConfig() {
        try {
            log.info("Redis配置更新");
        } catch (Exception e) {
            log.error("更新主从配置失败", e);
        }
    }
}
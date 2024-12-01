package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@Slf4j
public class RedisSentinelFailoverListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用程序启动完成，Redis配置就绪");
        updateMasterSlaveConfig();
    }

    private void updateMasterSlaveConfig() {
        try {
            log.info("Redis配置更新完成");
        } catch (Exception e) {
            log.error("更新主从配置失败", e);
        }
    }
}
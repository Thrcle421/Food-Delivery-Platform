package com.sky;

import com.sky.service.RedisSentinelService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import java.util.List;

@SpringBootApplication
@EnableTransactionManagement // 开启注解方式的事务管理
@Slf4j
public class SkyApplication {

    @Autowired
    private RedisSentinelService redisSentinelService;

    @PostConstruct
    public void init() {
        log.info("========== Redis 连接状态检查 ==========");
        try {
            boolean connected = redisSentinelService.checkConnection();
            log.info("连接状态: {}", connected ? "正常" : "异常");

            if (connected) {
                String master = redisSentinelService.getCurrentMaster();
                List<String> slaves = redisSentinelService.getSlaveNodes();

                log.info("主节点: {}", master);
                if (slaves.isEmpty()) {
                    log.info("从节点: 无");
                } else {
                    log.info("从节点列表:");
                    for (String slave : slaves) {
                        log.info("  - {}", slave);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Redis检查失败: {}", e.getMessage());
        }
        log.info("======================================");
    }

    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}

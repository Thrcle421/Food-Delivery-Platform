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
@EnableTransactionManagement // Enable annotation-based transaction management
@Slf4j
public class SkyApplication {

    @Autowired
    private RedisSentinelService redisSentinelService;

    @PostConstruct
    public void init() {
        log.info("========== Checking Redis Connection Status ==========");
        try {
            boolean connected = redisSentinelService.checkConnection();
            log.info("Connection Status: {}", connected ? "Normal" : "Abnormal");

            if (connected) {
                boolean isSentinel = redisSentinelService.isSentinelMode();
                log.info("Redis Running Mode: {}", isSentinel ? "Sentinel Mode" : "Standalone/Cluster Mode");

                // Get master and slave node information
                String master = redisSentinelService.getCurrentMaster();
                List<String> slaves = redisSentinelService.getSlaveNodes();

                log.info("Master Node: {}", master);
                if (slaves.isEmpty()) {
                    log.info("Slave Nodes: None");
                } else {
                    log.info("Slave Node List:");
                    for (String slave : slaves) {
                        log.info("  - {}", slave);
                    }
                }

                // If in sentinel mode, additionally output sentinel information
                if (isSentinel) {
                    List<String> sentinels = redisSentinelService.getSentinelNodes();
                    log.info("Sentinel Node List:");
                    for (String sentinel : sentinels) {
                        log.info("  - {}", sentinel);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Redis Check Failed: {}", e.getMessage());
        }
        log.info("======================================");
    }

    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}

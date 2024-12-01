package com.sky.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RedisSentinelService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    /**
     * 检查Redis连接状态
     */
    public boolean checkConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前主节点
     */
    public String getCurrentMaster() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info("replication");
            String role = info.getProperty("role");

            if ("master".equals(role)) {
                Properties serverInfo = connection.serverCommands().info("server");
                return serverInfo.getProperty("tcp_port", "6379");
            } else {
                return info.getProperty("master_host") + ":" + info.getProperty("master_port");
            }
        } catch (Exception e) {
            log.error("获取主节点信息失败", e);
            return null;
        }
    }

    /**
     * 获取从节点列表
     */
    public List<String> getSlaveNodes() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info("replication");
            List<String> slaves = new ArrayList<>();

            // 获取连接的从节点数量
            String connectedSlaves = info.getProperty("connected_slaves");
            int slaveCount = Integer.parseInt(connectedSlaves);

            // 解析每个从节点的信息
            for (int i = 0; i < slaveCount; i++) {
                String slaveInfo = info.getProperty("slave" + i);
                if (slaveInfo != null) {
                    String[] parts = slaveInfo.split(",");
                    if (parts.length >= 3) {
                        String ip = parts[0].split("=")[1];
                        String port = parts[1].split("=")[1];
                        slaves.add(ip + ":" + port);
                    }
                }
            }
            return slaves;
        } catch (Exception e) {
            log.error("获取从节点信息失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 检查Redis是否运行在哨兵模式下
     */
    public boolean isSentinelMode() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

            // 1. 先检查复制信息
            Properties replInfo = connection.serverCommands().info("replication");
            log.debug("复制信息: {}", replInfo);

            // 2. 检查哨兵信息
            Properties sentinelInfo = connection.serverCommands().info("sentinel");
            log.debug("哨兵信息: {}", sentinelInfo);

            // 3. 检查是否有哨兵配置
            if (sentinelNodes != null && !sentinelNodes.isEmpty()) {
                log.info("检测到哨兵配置: {}", sentinelNodes);
                return true;
            }

            // 4. 检查是否是哨兵进程
            String role = replInfo.getProperty("role");
            if ("sentinel".equalsIgnoreCase(role)) {
                log.info("当前节点是哨兵节点");
                return true;
            }

            log.info("未检测到哨兵模式配置");
            return false;
        } catch (Exception e) {
            log.error("检查Redis哨兵模式失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取哨兵节点列表
     */
    public List<String> getSentinelNodes() {
        if (!isSentinelMode()) {
            log.warn("当前Redis未运行在哨兵模式下");
            return new ArrayList<>();
        }

        try {
            // 首先从配置文件获取配置的哨兵节点
            List<String> configuredNodes = new ArrayList<>();
            for (String node : sentinelNodes.split(",")) {
                configuredNodes.add("已配置节点: " + node.trim());
            }

            // 然后获取实际运行的哨兵信息
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info("sentinel");

            for (String key : info.stringPropertyNames()) {
                if (key.startsWith("sentinel_")) {
                    String value = info.getProperty(key);
                    if (value != null && !value.isEmpty()) {
                        configuredNodes.add(key + ": " + value);
                    }
                }
            }

            return configuredNodes;
        } catch (Exception e) {
            log.error("获取哨兵节点信息失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
package com.sky.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 获取当前主节点信息
     */
    public String getCurrentMaster() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info("server");
            String host = info.getProperty("tcp_port", "6379");
            String result = "localhost:" + host;
            log.info("获取到Redis主节点信息: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取主节点信息失败: {}", e.getMessage());
            return "localhost:6379";
        }
    }

    /**
     * 获取从节点信息
     */
    public List<String> getSlaveNodes() {
        List<String> slaves = new ArrayList<>();
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info("replication");

            // 获取从节点数量
            String connectedSlaves = info.getProperty("connected_slaves", "0");
            int slaveCount = Integer.parseInt(connectedSlaves);

            // 获取每个从节点的信息
            for (int i = 0; i < slaveCount; i++) {
                String slaveInfo = info.getProperty("slave" + i);
                if (slaveInfo != null) {
                    slaves.add("Slave " + i + ": " + slaveInfo);
                }
            }

            log.info("获取到Redis从节点信息: {}", slaves);
            return slaves;
        } catch (Exception e) {
            log.error("获取从节点信息失败: {}", e.getMessage());
            return slaves;
        }
    }

    /**
     * 检查连接状态
     */
    public boolean checkConnection() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            return connection.ping() != null;
        } catch (Exception e) {
            log.error("Redis连接检查失败", e);
            return false;
        }
    }
}
package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOSSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AliOSSUtils getAliOSSUtils(AliOssProperties aliOssProperties) {
        log.info("创建了阿里云上传工具:{}", aliOssProperties);
        return new AliOSSUtils(aliOssProperties.getEndpoint(),aliOssProperties.getBucketName());
    }
}

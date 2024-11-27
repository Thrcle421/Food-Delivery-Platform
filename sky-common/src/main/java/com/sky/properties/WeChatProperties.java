package com.sky.properties;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "sky.wechat")
public class WeChatProperties {

    private String appid;
    private String secret;
    private String mchid;
    private String mchSerialNo;
    private String privateKeyFilePath;
    private String apiV3Key;
    private String weChatPayCertFilePath;
    private String notifyUrl;
    private String refundNotifyUrl;

}
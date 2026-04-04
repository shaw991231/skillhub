package com.iflytek.skillhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Feishu Open Platform integration.
 */
@Component
@ConfigurationProperties(prefix = "skillhub.feishu")
public class FeishuProperties {

    private String appId = "placeholder";
    private String appSecret = "placeholder";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}

package com.iflytek.skillhub.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.IOException;
import java.util.List;

/**
 * SPA 路由配置 - 将前端路由请求转发到 index.html，由前端路由器处理。
 * 注意：此配置仅处理静态资源，认证由 Spring Security 负责。
 */
@Configuration
public class SpaRoutingConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * 自定义 ResourceResolver 处理 SPA 路由回退
     */
    private static class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requestedResource = location.createRelative(resourcePath);

            // 如果资源存在且可读，直接返回（包括 index.html 本身）
            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }

            // 资源不存在时，检查是否需要返回 index.html（SPA 路由回退）
            // 条件：
            // 1. 不是 API 请求（/api/, /actuator/, /v3/api-docs/）
            // 2. 不是静态资源文件（没有扩展名或扩展名是 .html）
            // 3. 不是敏感路径（/.well-known/, /login/oauth2/ 等）
            if (shouldFallbackToIndex(resourcePath)) {
                Resource indexResource = location.createRelative("index.html");
                if (indexResource.exists() && indexResource.isReadable()) {
                    return indexResource;
                }
            }

            return null;
        }

        private boolean shouldFallbackToIndex(String resourcePath) {
            // 排除 API 路径
            if (resourcePath.startsWith("api/") ||
                resourcePath.startsWith("actuator/") ||
                resourcePath.startsWith("v3/api-docs/") ||
                resourcePath.startsWith("swagger-ui/")) {
                return false;
            }

            // 排除 OAuth2 回调路径
            if (resourcePath.startsWith("login/oauth2/")) {
                return false;
            }

            // 排除 .well-known 路径
            if (resourcePath.startsWith(".well-known/")) {
                return false;
            }

            // 排除静态资源文件（有扩展名且不是 HTML）
            int lastDotIndex = resourcePath.lastIndexOf('.');
            if (lastDotIndex > 0) {
                // 有扩展名，只有 HTML 文件才回退
                return resourcePath.endsWith(".html");
            }

            // 没有扩展名，视为前端路由，回退到 index.html
            return true;
        }
    }
}

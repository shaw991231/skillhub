package com.iflytek.skillhub.auth.oauth;

import java.net.URI;
import java.util.Set;

/**
 * Utility methods and constants for safely handling post-login redirect targets in OAuth flows.
 */
public final class OAuthLoginRedirectSupport {

    public static final String SESSION_RETURN_TO_ATTRIBUTE = "skillhub.oauth.returnTo";
    public static final String DEFAULT_TARGET_URL = "/dashboard";
    private static final Set<String> DEV_FRONTEND_HOSTS = Set.of("localhost", "127.0.0.1");
    private static final Set<String> DEV_FRONTEND_SCHEMES = Set.of("http", "https");
    private static final int DEV_FRONTEND_PORT = 3000;

    private OAuthLoginRedirectSupport() {
    }

    public static String sanitizeReturnTo(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String trimmed = candidate.trim();

        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return null;
        }

        // 允许开发环境下明确校验过的完整前端 URL。
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return sanitizeDevFrontendUrl(trimmed);
        }

        // 允许相对路径（以 / 开头但不以 // 开头）
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return null;
        }
        return trimmed;
    }

    private static String sanitizeDevFrontendUrl(String candidate) {
        try {
            URI uri = URI.create(candidate);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            String rawPath = uri.getRawPath();

            if (host == null || scheme == null) {
                return null;
            }
            if (!DEV_FRONTEND_HOSTS.contains(host) || !DEV_FRONTEND_SCHEMES.contains(scheme)) {
                return null;
            }
            if (uri.getPort() != DEV_FRONTEND_PORT || uri.getRawUserInfo() != null) {
                return null;
            }
            if (rawPath != null && !rawPath.startsWith("/")) {
                return null;
            }
            return candidate;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

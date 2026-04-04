package com.iflytek.skillhub.auth.feishu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Encapsulates calls to the Feishu Open Platform APIs for user identity resolution.
 */
@Service
public class FeishuUserService {

    private final RestClient restClient;

    public FeishuUserService() {
        this.restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Obtains an app_access_token using the internal application credential flow.
     */
    public String getAppAccessToken(String appId, String appSecret) {
        AppTokenResponse resp = restClient.post()
            .uri("https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal/")
            .body(Map.of("app_id", appId, "app_secret", appSecret))
            .retrieve()
            .body(AppTokenResponse.class);

        if (resp == null || resp.code != 0) {
            throw new IllegalStateException(
                "Feishu app_access_token request failed: code=%d msg=%s"
                    .formatted(resp != null ? resp.code : -1, resp != null ? resp.msg : "null"));
        }
        return resp.app_access_token;
    }

    /**
     * Retrieves basic user information from the {@code /authen/v1/user_info} endpoint.
     */
    public FeishuUserInfo getUserInfo(String userAccessToken) {
        FeishuApiResponse<FeishuUserInfo> resp = restClient.get()
            .uri("https://open.feishu.cn/open-apis/authen/v1/user_info")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<>() {});

        throwOnNonZero(resp);
        return resp.data();
    }

    /**
     * Retrieves detailed user information (including email) from the {@code /contact/v3/users/me} endpoint.
     */
    public FeishuUserDetail getUserDetail(String userAccessToken) {
        FeishuApiResponse<FeishuUserDetail> resp = restClient.get()
            .uri("https://open.feishu.cn/open-apis/contact/v3/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
            .retrieve()
            .body(new org.springframework.core.ParameterizedTypeReference<>() {});

        throwOnNonZero(resp);
        return resp.data();
    }

    private <T> void throwOnNonZero(FeishuApiResponse<T> resp) {
        if (resp == null || resp.code() != 0) {
            throw new IllegalStateException(
                "Feishu API error: code=%d msg=%s"
                    .formatted(resp != null ? resp.code() : -1, resp != null ? resp.msg() : "null"));
        }
    }

    /**
     * Normalized Feishu user identity returned by {@link #getUserInfo} and enriched by {@link #getUserDetail}.
     */
    public record FeishuUserInfo(
        String open_id,
        String name,
        String avatar_url,
        String email
    ) {}

    /**
     * Detailed user info returned by {@code /contact/v3/users/me}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeishuUserDetail(
        String name,
        String open_id,
        Avatar avatar,
        String enterprise_email,
        String email
    ) {
        /**
         * Resolves the best available avatar URL (prefers the origin size).
         */
        public String avatarUrl() {
            return avatar != null
                ? (avatar.avatar_origin != null ? avatar.avatar_origin : avatar.avatar_72)
                : null;
        }

        /**
         * Resolves the best available email (enterprise email first).
         */
        public String bestEmail() {
            return enterprise_email != null ? enterprise_email : email;
        }

        public record Avatar(
            String avatar_72,
            String avatar_240,
            String avatar_640,
            String avatar_origin
        ) {}
    }

    /** Generic Feishu API envelope where {@code code == 0} means success. */
    record FeishuApiResponse<T>(int code, String msg, T data) {}

    /** Response envelope for the app_access_token endpoint. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AppTokenResponse(int code, String msg, String app_access_token, int expire) {}
}

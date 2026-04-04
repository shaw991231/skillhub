package com.iflytek.skillhub.auth.oauth;

import com.iflytek.skillhub.auth.feishu.FeishuUserService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Handles Feishu's non-standard OAuth2 token exchange.
 *
 * <p>Feishu requires an {@code app_access_token} (obtained separately via
 * {@code /auth/v3/app_access_token/internal/}) as a Bearer header when calling
 * the OIDC token endpoint, instead of the standard {@code client_secret_basic}
 * or {@code client_secret_post} authentication.
 *
 * <p>For non-Feishu registrations, this class delegates to Spring Security's
 * default token exchange implementation.
 */
@Component
public class FeishuOAuth2AccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private static final String FEISHU_REGISTRATION = "feishu";

    private final DefaultAuthorizationCodeTokenResponseClient defaultClient =
            new DefaultAuthorizationCodeTokenResponseClient();

    private final FeishuUserService feishuUserService;
    private final RestClient restClient;

    public FeishuOAuth2AccessTokenResponseClient(FeishuUserService feishuUserService) {
        this.feishuUserService = feishuUserService;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration registration = request.getClientRegistration();
        if (FEISHU_REGISTRATION.equals(registration.getRegistrationId())) {
            return exchangeFeishuToken(request);
        }
        return defaultClient.getTokenResponse(request);
    }

    @SuppressWarnings("unchecked")
    private OAuth2AccessTokenResponse exchangeFeishuToken(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration registration = request.getClientRegistration();

        // Step 1: Get app_access_token using credentials from ClientRegistration
        // (same source as spring.security.oauth2.client.registration.feishu.client-id/secret)
        String appAccessToken = feishuUserService.getAppAccessToken(
                registration.getClientId(), registration.getClientSecret());

        // Step 2: Exchange authorization code for user_access_token
        // Use the resolved redirect URI from the original authorization request,
        // not registration.getRedirectUri() which still contains the unresolved
        // {baseUrl} template and would cause Feishu error 20029 (invalid redirect uri).
        String resolvedRedirectUri = request.getAuthorizationExchange()
                .getAuthorizationRequest().getRedirectUri();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", request.getAuthorizationExchange().getAuthorizationResponse().getCode());
        body.add("redirect_uri", resolvedRedirectUri);

        Map<String, Object> response = restClient.post()
                .uri(registration.getProviderDetails().getTokenUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appAccessToken)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Feishu token exchange returned null response");
        }

        Number code = (Number) response.get("code");
        if (code == null || code.intValue() != 0) {
            throw new IllegalStateException(
                    "Feishu token exchange failed: code=%s msg=%s"
                            .formatted(code, response.get("msg")));
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");

        String accessToken = (String) data.get("access_token");
        int expiresIn = data.get("expires_in") != null ? ((Number) data.get("expires_in")).intValue() : 7200;
        String refreshToken = (String) data.get("refresh_token");

        Set<String> scopes = data.get("scope") != null
                ? new HashSet<>(List.of(((String) data.get("scope")).split(" ")))
                : Set.of();

        return OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(expiresIn)
                .scopes(scopes)
                .refreshToken(refreshToken)
                .build();
    }
}

package com.iflytek.skillhub.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 authorization request resolver that preserves a sanitized post-login redirect target in
 * the HTTP session and customizes the authorization request for Feishu's non-standard OAuth2 flow.
 */
@Component
public class SkillHubOAuth2AuthorizationRequestResolver
        implements org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver {

    private static final String FEISHU_AUTH_HOST = "open.feishu.cn";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final OAuthLoginFlowService oauthLoginFlowService;

    public SkillHubOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                      OAuthLoginFlowService oauthLoginFlowService) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
        this.oauthLoginFlowService = oauthLoginFlowService;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        oauthLoginFlowService.rememberReturnTo(request);
        return customizeForFeishu(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
        oauthLoginFlowService.rememberReturnTo(request);
        return customizeForFeishu(authorizationRequest);
    }

    /**
     * Feishu's authorization endpoint expects {@code app_id} instead of the standard {@code client_id}.
     */
    private OAuth2AuthorizationRequest customizeForFeishu(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getAuthorizationRequestUri();
        if (uri.contains(FEISHU_AUTH_HOST)) {
            String modifiedUri = uri.replace("client_id=", "app_id=");
            return OAuth2AuthorizationRequest.from(request)
                    .authorizationRequestUri(modifiedUri)
                    .build();
        }
        return request;
    }
}

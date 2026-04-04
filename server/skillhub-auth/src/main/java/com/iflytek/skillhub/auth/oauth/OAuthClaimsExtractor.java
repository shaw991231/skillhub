package com.iflytek.skillhub.auth.oauth;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Strategy interface for converting provider-specific OAuth user payloads into normalized claims.
 */
public interface OAuthClaimsExtractor {
    String getProvider();
    OAuthClaims extract(OAuth2UserRequest request, OAuth2User oAuth2User);

    /**
     * Return {@code true} if this extractor relies on the {@code DefaultOAuth2UserService}
     * to fetch user info from the provider's user-info endpoint. Extractors that call
     * provider APIs directly (e.g. Feishu) should return {@code false}.
     */
    default boolean requiresDefaultUserInfo() { return true; }
}

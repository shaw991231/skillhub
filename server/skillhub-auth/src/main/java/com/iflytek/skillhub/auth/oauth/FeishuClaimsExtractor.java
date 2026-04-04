package com.iflytek.skillhub.auth.oauth;

import com.iflytek.skillhub.auth.feishu.FeishuUserService;
import com.iflytek.skillhub.auth.feishu.FeishuUserService.FeishuUserDetail;
import com.iflytek.skillhub.auth.feishu.FeishuUserService.FeishuUserInfo;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider-specific claims extractor that resolves Feishu OAuth users into
 * normalized {@link OAuthClaims}.
 */
@Component
public class FeishuClaimsExtractor implements OAuthClaimsExtractor {

    private final FeishuUserService feishuUserService;

    public FeishuClaimsExtractor(FeishuUserService feishuUserService) {
        this.feishuUserService = feishuUserService;
    }

    @Override
    public String getProvider() { return "feishu"; }

    @Override
    public boolean requiresDefaultUserInfo() { return false; }

    @Override
    public OAuthClaims extract(OAuth2UserRequest request, OAuth2User oAuth2User) {
        String userAccessToken = request.getAccessToken().getTokenValue();

        FeishuUserInfo userInfo = feishuUserService.getUserInfo(userAccessToken);

        // Attempt to enrich with detailed contact info (email, higher-res avatar).
        String email = userInfo.email();
        String avatarUrl = userInfo.avatar_url();
        try {
            FeishuUserDetail detail = feishuUserService.getUserDetail(userAccessToken);
            email = detail.bestEmail() != null ? detail.bestEmail() : email;
            avatarUrl = detail.avatarUrl() != null ? detail.avatarUrl() : avatarUrl;
        } catch (Exception ignored) {
            // user_info alone is sufficient for basic login; detail is best-effort.
        }

        Map<String, Object> extra = new HashMap<>(oAuth2User.getAttributes());
        extra.put("avatar_url", avatarUrl);
        extra.put("login", userInfo.name());

        return new OAuthClaims(
            "feishu",
            userInfo.open_id(),
            email,
            email != null,
            userInfo.name(),
            extra
        );
    }
}

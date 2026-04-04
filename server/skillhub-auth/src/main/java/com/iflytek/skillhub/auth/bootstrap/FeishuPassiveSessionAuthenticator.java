package com.iflytek.skillhub.auth.bootstrap;

import com.iflytek.skillhub.auth.feishu.FeishuUserService;
import com.iflytek.skillhub.auth.feishu.FeishuUserService.FeishuUserInfo;
import com.iflytek.skillhub.auth.identity.IdentityBindingService;
import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.domain.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

/**
 * Passive session authenticator that establishes a SkillHub session from a Feishu
 * SSO context. Expects a valid Feishu {@code user_access_token} in the
 * {@code X-Feishu-Token} request header.
 */
@Component
public class FeishuPassiveSessionAuthenticator implements PassiveSessionAuthenticator {

    static final String TOKEN_HEADER = "X-Feishu-Token";

    private final FeishuUserService feishuUserService;
    private final IdentityBindingService identityBindingService;

    public FeishuPassiveSessionAuthenticator(FeishuUserService feishuUserService,
                                             IdentityBindingService identityBindingService) {
        this.feishuUserService = feishuUserService;
        this.identityBindingService = identityBindingService;
    }

    @Override
    public String providerCode() { return "feishu"; }

    @Override
    public String displayName() { return "飞书"; }

    @Override
    public Optional<PlatformPrincipal> authenticate(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        FeishuUserInfo userInfo = feishuUserService.getUserInfo(token);

        HashMap<String, Object> extra = new HashMap<>();
        extra.put("avatar_url", userInfo.avatar_url());

        OAuthClaims claims = new OAuthClaims(
            "feishu",
            userInfo.open_id(),
            userInfo.email(),
            userInfo.email() != null,
            userInfo.name(),
            extra
        );

        PlatformPrincipal principal = identityBindingService.bindOrCreate(claims, UserStatus.ACTIVE);
        return Optional.of(principal);
    }
}

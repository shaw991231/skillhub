package com.iflytek.skillhub.auth.oauth;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.session.PlatformSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Login success handler that copies the resolved platform principal into the
 * HTTP session and then redirects to the stored return target.
 */
@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final PlatformSessionService platformSessionService;
    private final OAuthLoginFlowService oauthLoginFlowService;

    public OAuth2LoginSuccessHandler(PlatformSessionService platformSessionService,
                                     OAuthLoginFlowService oauthLoginFlowService) {
        this.platformSessionService = platformSessionService;
        this.oauthLoginFlowService = oauthLoginFlowService;
        setDefaultTargetUrl(OAuthLoginRedirectSupport.DEFAULT_TARGET_URL);
    }

    /**
     * Checks if the redirect URL is allowed in development mode.
     * Allows redirecting to localhost:3000 (frontend) from localhost:8000 (backend) in development.
     */
    private boolean isAllowedDevRedirect(String url) {
        if (url == null) {
            return false;
        }
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            int port = parsedUrl.getPort();

            // Allow localhost redirects to common frontend ports in development
            if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
                return port == 3000 || port == 5173 || port == 8080;
            }
            return false;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            PlatformPrincipal principal = (PlatformPrincipal) oAuth2User.getAttributes().get("platformPrincipal");
            if (principal != null) {
                // Always normalize the authenticated session to PlatformPrincipal before any redirect
                // logic runs. The saved returnTo marker can be missing after OAuth round-trips, but
                // downstream controllers still expect a platform session.
                platformSessionService.establishSession(principal, request, true);
            }
        }

        // 检查是否有保存的 returnTo 参数
        String returnTo = oauthLoginFlowService.consumeReturnTo(request.getSession(false));

        // 如果有 returnTo，先处理 Session 和 Principal，然后手动重定向
        if (returnTo != null) {
            // 在开发环境下，允许重定向到前端 localhost:3000
            // Spring Security 的 DefaultRedirectStrategy 默认不允许跨域重定向
            // 但在开发环境（前后端分离）需要此功能
            if (isAllowedDevRedirect(returnTo)) {
                response.sendRedirect(returnTo);
            } else {
                getRedirectStrategy().sendRedirect(request, response, returnTo);
            }
            clearAuthenticationAttributes(request);
            return;
        }

        // 没有 returnTo 时，使用父类的默认行为
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

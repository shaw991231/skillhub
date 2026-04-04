# Feishu OAuth2 Integration Design

**Date:** 2026-04-04
**Status:** Approved

## Goal

Integrate Feishu (Lark) as an OAuth2 login provider for SkillHub, covering two scenarios:
1. Web OAuth2 login (browser redirect flow)
2. Enterprise SSO Bootstrap (PassiveSessionAuthenticator)

GitHub OAuth login will be hidden via configuration (code preserved, runtime toggle).

## Architecture

### Backend Changes

#### New Files

| File | Responsibility |
|------|---------------|
| `skillhub-auth/.../oauth/FeishuClaimsExtractor.java` | Extract user claims from Feishu OAuth2 flow |
| `skillhub-auth/.../feishu/FeishuUserService.java` | Call Feishu Open Platform APIs (user info, email) |
| `skillhub-auth/.../bootstrap/FeishuPassiveSessionAuthenticator.java` | Enterprise SSO via Feishu user_access_token |
| `skillhub-app/.../config/FeishuProperties.java` | Feishu configuration properties |

#### Modified Files

| File | Change |
|------|--------|
| `application.yml` | Add feishu OAuth2 client registration + provider config |
| `AuthMethodCatalog.java` | Support hidden flag per OAuth provider (GITHUB_AUTH_VISIBLE) |
| `AuthMethodResponse.java` | Add `hidden` field |

### Frontend Changes

| File | Change |
|------|--------|
| `login-button.tsx` | Feishu icon + provider-specific icons, respect hidden flag |
| `runtime-config.js.template` | Add `authProviders.github.visible` / `authProviders.feishu.visible` |
| `use-auth-methods.ts` | Filter hidden methods |

### OAuth2 Flow (Web Login)

```
Browser -> /oauth2/authorization/feishu?returnTo=xxx
       -> Feishu auth page (open.feishu.cn)
       -> Callback /login/oauth2/code/feishu
       -> FeishuClaimsExtractor extracts user info
       -> IdentityBindingService.bindOrCreate() creates/binds account
       -> OAuth2LoginSuccessHandler establishes Session
       -> Redirect to returnTo
```

### Enterprise SSO Bootstrap Flow

```
Upstream -> POST /api/v1/auth/session/bootstrap {provider: "feishu", token: "u-xxx"}
         -> FeishuPassiveSessionAuthenticator.validate()
           -> Verify user_access_token via Feishu API
           -> Fetch user info via /open-apis/contact/v3/users/me
           -> IdentityBindingService.bindOrCreate()
         -> Establish Session
```

### New Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FEISHU_APP_ID` | `placeholder` | Feishu App ID |
| `FEISHU_APP_SECRET` | `placeholder` | Feishu App Secret |
| `GITHUB_AUTH_VISIBLE` | `false` | Show GitHub login button |

### Unchanged

- Session mechanism, RBAC, API Token, Device Flow
- Access Policy system (Feishu auto-reuses)
- Account Merge functionality
- `identity_binding` table structure (provider_code = `feishu`)

## Implementation Plan

1. Backend Feishu service layer (FeishuUserService + FeishuClaimsExtractor + FeishuPassiveSessionAuthenticator + FeishuProperties + application.yml)
2. Backend AuthMethodCatalog hidden provider support
3. Frontend Feishu login button + GitHub visibility toggle + runtime config

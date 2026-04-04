# Feishu OAuth Review Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Review the current Feishu OAuth/login changes, fix the confirmed redirect regression(s), and verify the affected login flows before committing.

**Architecture:** The review focuses on the active auth/login diff in `web/` and `server/skillhub-auth/`. Fixes must be driven by regression tests first, with the smallest code change that restores safe redirect behavior across development and normal app routing.

**Tech Stack:** React 19, Vite, Vitest, Spring Boot 3.2, Spring Security OAuth2, JUnit 5

### Task 1: Review the changed login and OAuth redirect paths

**Files:**
- Modify: `web/src/pages/login.tsx`
- Modify: `web/src/shared/lib/auth-route.ts`
- Modify: `server/skillhub-auth/src/main/java/com/iflytek/skillhub/auth/oauth/OAuthLoginRedirectSupport.java`
- Modify: `server/skillhub-auth/src/main/java/com/iflytek/skillhub/auth/oauth/OAuth2LoginSuccessHandler.java`
- Test: `web/src/pages/login.test.tsx`
- Test: `server/skillhub-auth/src/test/java/com/iflytek/skillhub/auth/oauth/OAuth2LoginHandlersTest.java`

**Step 1: Write the failing regression test**

Add or extend targeted tests that prove the broken redirect behavior with the current code.

**Step 2: Run the failing tests**

Run: `cd web && pnpm test -- run src/pages/login.test.tsx`
Expected: FAIL if the frontend redirect normalization is incorrect

Run: `cd server && ./mvnw -pl skillhub-auth -Dtest=OAuth2LoginHandlersTest test`
Expected: FAIL if the backend redirect handling accepts an unsafe target or fails to preserve the valid one

**Step 3: Write the minimal implementation**

Adjust the redirect sanitization / normalization logic only where the regression is proven.

**Step 4: Re-run the targeted tests**

Run: `cd web && pnpm test -- run src/pages/login.test.tsx`
Expected: PASS

Run: `cd server && ./mvnw -pl skillhub-auth -Dtest=OAuth2LoginHandlersTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add docs/plans/2026-04-04-feishu-oauth-review-fixes.md web/src/pages/login.tsx web/src/shared/lib/auth-route.ts web/src/pages/login.test.tsx server/skillhub-auth/src/main/java/com/iflytek/skillhub/auth/oauth/OAuthLoginRedirectSupport.java server/skillhub-auth/src/main/java/com/iflytek/skillhub/auth/oauth/OAuth2LoginSuccessHandler.java server/skillhub-auth/src/test/java/com/iflytek/skillhub/auth/oauth/OAuth2LoginHandlersTest.java
git commit -m "fix: harden oauth redirect handling"
```

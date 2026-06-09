# Android Auth Integration Guide

> Target: Android frontend implementation for ClueRoom JWT/OAuth auth.
> Backend source of truth: PR #53 auth foundation.
> API base URL: `https://api.clueroom.xyz`

---

## 1. Current Backend Contract

The backend exposes ClueRoom JWT auth through these endpoints:

| Method | Path | Auth header | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/oauth` | No | Exchange Google/Kakao provider token for ClueRoom tokens |
| `POST` | `/api/auth/refresh` | No | Rotate refresh token and issue a new access token |
| `POST` | `/api/auth/logout` | No | Revoke the submitted refresh token |
| `GET` | `/api/auth/me` | Yes | Read current authenticated user |
| `POST` | `/api/auth/dev` | No | Local/staging only. Disabled in prod by default |

Backend transition flags:

```text
AUTH_REQUIRE_AUTHENTICATION=false  # current compatibility mode until Android is ready
AUTH_MOCK_FALLBACK_ENABLED=true    # legacy fallback only while auth is not required
```

Android should still implement token storage and attach Bearer tokens now. When backend later switches `AUTH_REQUIRE_AUTHENTICATION=true`, protected APIs will require the token without another app change.

---

## 2. Response Envelope

All responses use the common wrapper.

Success:

```json
{
  "success": true,
  "data": {}
}
```

Empty success:

```json
{
  "success": true
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "timestamp": "2026-06-09T00:00:00",
    "status": 401,
    "error": "UNAUTHORIZED",
    "code": "AUTH_003",
    "message": "Invalid token.",
    "path": "/api/auth/me"
  }
}
```

Frontend branching should use `error.code`, not `message`.

---

## 3. Token Response DTO

`POST /api/auth/oauth`, `POST /api/auth/dev`, and `POST /api/auth/refresh` return:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "nickname": "User",
      "profileImageUrl": "https://example.com/profile.png",
      "role": "USER"
    }
  }
}
```

Nullable fields:

```text
user.email can be null.
user.profileImageUrl can be null.
```

`role` values currently used by Android:

```text
USER
ADMIN
```

Do not implement rate-limit bypass on the client. The backend will enforce AI rate limits by server-side role.

---

## 4. OAuth Login

### 4.1 Google

Android obtains a Google ID token from Google Sign-In / Credential Manager, then sends it to the backend.

Request:

```http
POST /api/auth/oauth
Content-Type: application/json
```

```json
{
  "provider": "GOOGLE",
  "idToken": "google-id-token-from-android-sdk",
  "deviceId": "android-device-id-or-installation-id"
}
```

Rules:

```text
provider must be GOOGLE.
idToken is required for GOOGLE.
accessToken should be omitted/null for GOOGLE.
deviceId is optional but recommended, max 100 chars.
```

Backend verification:

```text
Google tokeninfo aud must match server GOOGLE_CLIENT_ID / GOOGLE_CLIENT_IDS.
Email linking only happens when Google email_verified=true.
If email is unavailable or unverified, backend can create a user with email=null.
```

### 4.2 Kakao

Android obtains a Kakao access token from Kakao SDK, then sends it to the backend.

Request:

```http
POST /api/auth/oauth
Content-Type: application/json
```

```json
{
  "provider": "KAKAO",
  "accessToken": "kakao-access-token-from-android-sdk",
  "deviceId": "android-device-id-or-installation-id"
}
```

Rules:

```text
provider must be KAKAO.
accessToken is required for KAKAO.
idToken should be omitted/null for KAKAO.
deviceId is optional but recommended, max 100 chars.
```

Backend verification:

```text
Kakao access_token_info app_id must match server KAKAO_APP_ID.
Email linking only happens when Kakao email is valid and verified.
If email is unavailable or unverified, backend can create a user with email=null.
```

---

## 5. Local/Staging Dev Login

`POST /api/auth/dev` exists only for local/staging token-flow testing.

Request:

```json
{
  "email": "dev@example.com",
  "nickname": "Dev User",
  "deviceId": "android-emulator"
}
```

Prod expectation:

```text
AUTH_DEV_LOGIN_ENABLED=false
/api/auth/dev returns AUTH_001 when disabled.
```

Android production code must not depend on dev login.

---

## 6. Token Storage

Android should store:

```text
accessToken
refreshToken
expiresIn or accessTokenExpiresAt
user summary
```

Recommended storage:

```text
EncryptedSharedPreferences or encrypted DataStore for tokens.
Normal DataStore is acceptable only for non-sensitive user display cache.
```

Do not log:

```text
accessToken
refreshToken
provider idToken/accessToken
Authorization header
```

Refresh token is an opaque random string. Do not parse it as JWT.

---

## 7. Authorization Header

For protected API calls, attach:

```http
Authorization: Bearer {accessToken}
```

Attach Bearer token to gameplay/write/user-specific APIs when an access token exists, even before protected mode is enabled.

Do not attach Bearer token to these auth endpoints if the HTTP client can exclude them:

```text
POST /api/auth/oauth
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/dev
```

Backend currently skips JWT filtering for those public auth endpoints, so an expired Authorization header should not block refresh. Still, excluding the header is simpler and avoids client-side ambiguity.

---

## 8. Refresh Flow

Trigger refresh when:

```text
access token is expired locally, or
protected API returns 401 with AUTH_003 / C003, and refreshToken exists.
```

Request:

```http
POST /api/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "stored-refresh-token",
  "deviceId": "android-device-id-or-installation-id"
}
```

Success behavior:

```text
Replace both accessToken and refreshToken.
Retry the original request once.
```

Failure behavior:

```text
Clear local tokens.
Move user to login screen.
Do not retry refresh in an infinite loop.
```

Recommended interceptor policy:

```text
Use a single-flight refresh lock.
If several requests fail with 401 at once, only one refresh request should run.
Other requests should wait for the refresh result and then retry once.
```

Refresh rotation note:

```text
Each refresh success revokes the old refresh token.
The app must persist the new refreshToken immediately.
```

---

## 9. Logout Flow

Request:

```http
POST /api/auth/logout
Content-Type: application/json
```

```json
{
  "refreshToken": "stored-refresh-token"
}
```

Client behavior:

```text
Call logout if refreshToken exists.
Clear local tokens and user cache regardless of network result.
Move user to logged-out UI state.
```

Logout only revokes the submitted refresh token. It does not revoke every device.

---

## 10. Me Endpoint

Request:

```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

Response data:

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "User",
  "profileImageUrl": null,
  "role": "USER"
}
```

Use cases:

```text
Validate stored access token on app start.
Refresh user profile cache.
Confirm login state after OAuth login or refresh.
```

If `/api/auth/me` returns 401:

```text
try refresh if refreshToken exists;
otherwise clear tokens and show logged-out state.
```

---

## 11. Public vs Protected APIs

Public without Bearer:

```text
GET /api/scenarios
GET /api/scenarios/{scenarioId}
POST /api/auth/oauth
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/dev
```

Bearer recommended even when endpoint is public:

```text
GET /api/scenarios/{scenarioId}
```

Reason: authenticated creator can see own DRAFT/PRIVATE scenarios. Anonymous users only see public/playable data.

Protected when backend enables `AUTH_REQUIRE_AUTHENTICATION=true`:

```text
/api/play-sessions/**
/api/device-tokens/**
/api/notifications/**
POST /api/scenarios/**
PATCH /api/scenarios/**
GET /api/scenarios/{scenarioId}/validation-result
POST /api/ai/scenarios/{scenarioId}/validate
/api/** new endpoints by default
```

Gameplay endpoints to prepare with Bearer now:

```text
POST /api/play-sessions
POST /api/play-sessions/{sessionId}/interrogations
POST /api/play-sessions/{sessionId}/final-deduction
GET /api/play-sessions/{sessionId}/result
```

---

## 12. Error Codes to Handle

| Code | HTTP | Meaning | Android action |
|---|---:|---|---|
| `AUTH_001` | 403 | Dev login disabled | Do not use dev login in prod |
| `AUTH_002` | 500 | Server JWT secret missing | Show generic server error; report to backend |
| `AUTH_003` | 401 | Invalid/expired access token | Try refresh once |
| `AUTH_004` | 401 | Refresh token not valid | Clear tokens; login required |
| `AUTH_005` | 401 | Refresh token expired | Clear tokens; login required |
| `AUTH_006` | 401 | User not found | Clear tokens; login required |
| `AUTH_007` | 403 | User blocked/withdrawn | Clear tokens; show access restricted |
| `AUTH_008` | 400 | Unsupported OAuth provider | Client bug or unsupported login type |
| `AUTH_009` | 400 | Required provider token missing | Client request bug |
| `AUTH_010` | 500 | Provider config missing | Show generic server error; report to backend |
| `AUTH_011` | 401 | Provider token verification failed | Ask user to retry provider login |
| `AUTH_012` | 409 | OAuth account conflict | Show account linking conflict message |
| `C003` | 401 | Generic unauthorized | Try refresh once if token exists |
| `C004` | 403 | Generic forbidden | Do not refresh-loop; show forbidden state |

Validation errors can return another common validation code with `details`. Display field-level messages when present.

---

## 13. Recommended Android State Machine

```text
LoggedOut
  -> OAuth provider success
  -> POST /api/auth/oauth success
  -> Authenticated

Authenticated
  -> request with Bearer
  -> 401 AUTH_003/C003
  -> Refreshing

Refreshing
  -> POST /api/auth/refresh success
  -> replace tokens
  -> retry original request once
  -> Authenticated

Refreshing
  -> refresh failure
  -> clear tokens
  -> LoggedOut

Authenticated
  -> logout click
  -> POST /api/auth/logout best effort
  -> clear tokens
  -> LoggedOut
```

---

## 14. QA Checklist

Before backend turns on protected mode:

- [ ] Google login calls `/api/auth/oauth` with `provider=GOOGLE` and `idToken`.
- [ ] Kakao login calls `/api/auth/oauth` with `provider=KAKAO` and `accessToken`.
- [ ] App stores access/refresh tokens securely.
- [ ] Protected/gameplay requests attach `Authorization: Bearer` when token exists.
- [ ] Auth endpoints are excluded from automatic Bearer header if practical.
- [ ] `/api/auth/me` succeeds after login.
- [ ] Forced expired/invalid access token triggers one refresh and retries original request once.
- [ ] Refresh success replaces both tokens.
- [ ] Refresh failure clears tokens and moves to logged-out UI.
- [ ] Logout clears local tokens even if network fails.
- [ ] UI tolerates `user.email == null`.
- [ ] UI does not expose admin/rate-limit bypass controls based only on client role.

Protected mode smoke after backend sets `AUTH_REQUIRE_AUTHENTICATION=true`:

```text
Tokenless protected gameplay API -> 401
OAuth login -> token issued
Same gameplay API with Bearer -> succeeds or returns normal domain error
Expired access token + valid refresh token -> refresh -> retry succeeds
Invalid refresh token -> logged-out state
```

---

## 15. Implementation Notes

Keep API base URL configuration separate from path strings:

```text
baseUrl = https://api.clueroom.xyz
path = /api/auth/oauth
```

Do not build URLs like:

```text
https://api.clueroom.xyz/api + /api/auth/oauth
```

Use server response `expiresIn` to compute a local expiry time. Refresh slightly before expiry if the app is active.

Suggested local expiry calculation:

```text
accessTokenExpiresAt = now + expiresIn - 30 seconds
```

If the app wakes from background and the token is already expired, refresh before sending protected requests.

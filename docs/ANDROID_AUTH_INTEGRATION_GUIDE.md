# Android 인증 연동 가이드

> 대상: ClueRoom Android의 JWT/OAuth 인증 연동.
> 백엔드 SoT: 현재 auth foundation 코드.
> API base URL: `https://api.clueroom.xyz`

---

## 1. 현재 백엔드 계약

백엔드는 ClueRoom JWT 인증을 아래 엔드포인트로 제공한다.

| Method | Path | Auth header | 목적 |
|---|---|---|---|
| `POST` | `/api/auth/oauth` | 없음 | Google/Kakao provider token을 ClueRoom token으로 교환 |
| `POST` | `/api/auth/oauth/kakao/code` | 없음 | Web Kakao authorizationCode를 ClueRoom token으로 교환. Android 호출 대상 아님 |
| `POST` | `/api/auth/refresh` | 없음 | refresh token rotation 후 새 access token 발급 |
| `POST` | `/api/auth/logout` | 없음 | 제출한 refresh token revoke |
| `GET` | `/api/auth/me` | 필요 | 현재 인증 사용자 조회 |
| `POST` | `/api/auth/dev` | 없음 | local/staging 전용. 운영 기본 disabled |

로컬 호환 모드 플래그:

```text
AUTH_REQUIRE_AUTHENTICATION=false  # local/test 호환 모드에서만 사용
AUTH_MOCK_FALLBACK_ENABLED=true    # local/test legacy fallback
```

운영 보호 모드는 `AUTH_REQUIRE_AUTHENTICATION=true`를 사용한다.
이 상태에서는 `AUTH_MOCK_FALLBACK_ENABLED=true`가 남아 있어도 token 없는 보호 API 요청에 `MOCK_USER_ID`를 부여하지 않는다.
Android는 token 저장과 Bearer header 첨부를 기본 전제로 구현한다.

---

## 2. 공통 응답 래퍼

모든 응답은 공통 wrapper를 사용한다.

성공:

```json
{
  "success": true,
  "data": {}
}
```

빈 성공:

```json
{
  "success": true
}
```

실패:

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

프론트 분기는 `message`가 아니라 `error.code` 기준으로 한다.

---

## 3. Token Response DTO

`POST /api/auth/oauth`, `POST /api/auth/oauth/kakao/code`, `POST /api/auth/dev`, `POST /api/auth/refresh`는 같은 token response shape를 반환한다.

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

nullable field:

```text
user.email은 null일 수 있다.
user.profileImageUrl은 null일 수 있다.
```

현재 Android가 고려할 role:

```text
USER
ADMIN
```

클라이언트에서 rate-limit bypass를 구현하지 않는다.
AI rate limit 우회 여부는 서버가 role/정책으로 판단한다.

---

## 4. OAuth 로그인

### 4.1 Google

Android는 Google Sign-In / Credential Manager로 Google ID token을 받은 뒤 백엔드에 전달한다.

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

규칙:

```text
provider는 GOOGLE이어야 한다.
idToken은 GOOGLE에서 필수다.
accessToken은 GOOGLE 요청에서 생략하거나 null로 둔다.
deviceId는 선택값이지만 권장한다. 최대 100자다.
```

백엔드 검증:

```text
Google tokeninfo aud가 서버 GOOGLE_CLIENT_ID / GOOGLE_CLIENT_IDS와 일치해야 한다.
email linking은 Google email_verified=true일 때만 수행한다.
email이 없거나 검증되지 않았으면 email=null 사용자도 생성될 수 있다.
```

QA 전용 계정은 서버 secret env의 `AUTH_QA_SEED_*`로 일반 USER row를 먼저 만들 수 있다. Google이 같은 verified email을 반환하면 해당 OAuth provider 계정은 기존 QA user에 연결된다.

### 4.2 Kakao

Android는 Kakao SDK로 Kakao access token을 받은 뒤 백엔드에 전달한다.

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

규칙:

```text
provider는 KAKAO여야 한다.
accessToken은 KAKAO에서 필수다.
idToken은 KAKAO 요청에서 생략하거나 null로 둔다.
deviceId는 선택값이지만 권장한다. 최대 100자다.
```

백엔드 검증:

```text
Kakao access_token_info app_id가 서버 KAKAO_APP_ID와 일치해야 한다.
email linking은 Kakao email이 유효하고 검증된 경우에만 수행한다.
email이 없거나 검증되지 않았으면 email=null 사용자도 생성될 수 있다.
```

QA 전용 계정은 서버 secret env의 `AUTH_QA_SEED_*`로 일반 USER row를 먼저 만들 수 있다. Kakao가 같은 verified email을 반환하면 해당 OAuth provider 계정은 기존 QA user에 연결된다.

### 4.3 Web Kakao code-flow

Android native 앱은 4.2의 `/api/auth/oauth` access token 방식을 유지한다.
웹 프론트는 Kakao JS SDK v2에서 access token을 직접 받지 않고, redirect로 받은 authorization code를 아래 endpoint에 전달한다.

```http
POST /api/auth/oauth/kakao/code
Content-Type: application/json
```

```json
{
  "authorizationCode": "kakao-authorization-code-from-web-redirect",
  "redirectUri": "https://www.clueroom.xyz",
  "deviceId": "browser-installation-id"
}
```

규칙:

```text
authorizationCode는 Kakao.Auth.authorize() redirect query의 code 값이다.
redirectUri는 Kakao console에 등록된 redirect URI이자 authorize 호출 시 사용한 값과 정확히 같아야 한다.
deviceId는 웹 프론트의 per-install 또는 browser-scoped 식별자다. 최대 100자다.
웹 프론트는 Kakao access/refresh token을 저장하지 않는다.
응답 shape는 /api/auth/oauth와 동일하다.
```

---

## 5. Local/Staging Dev Login

`POST /api/auth/dev`는 local/staging token flow 테스트 전용이다.

Request:

```json
{
  "email": "dev@example.com",
  "nickname": "Dev User",
  "deviceId": "android-emulator"
}
```

운영 기대값:

```text
AUTH_DEV_LOGIN_ENABLED=false
/api/auth/dev는 disabled 상태에서 AUTH_001을 반환한다.
```

Android 운영 코드는 dev login에 의존하면 안 된다.

---

## 6. Token 저장

Android는 아래 값을 저장한다.

```text
accessToken
refreshToken
expiresIn 또는 accessTokenExpiresAt
user summary
```

권장 저장소:

```text
token은 EncryptedSharedPreferences 또는 encrypted DataStore.
일반 DataStore는 민감하지 않은 사용자 표시 cache에만 사용한다.
```

로그에 남기면 안 되는 값:

```text
accessToken
refreshToken
provider idToken/accessToken
Authorization header
```

refresh token은 opaque random string이다. JWT처럼 파싱하지 않는다.

---

## 7. Authorization Header

보호 API 호출에는 아래 header를 붙인다.

```http
Authorization: Bearer {accessToken}
```

access token이 있으면 protected/gameplay/write/user-specific API에는 지금부터 Bearer token을 붙인다.
백엔드 protected mode가 아직 꺼져 있어도 같은 방식으로 구현한다.

실제 access token이 없으면 `Authorization` header를 아예 보내지 않는다.
`Authorization: Bearer mock_jwt_token_here` 같은 dummy 값은 보내지 않는다.
백엔드 `JWT_SECRET`이 설정된 뒤에는 일반 API의 malformed/expired Bearer token이 legacy mock fallback 대신 401로 거부될 수 있다.

가능하면 아래 auth endpoint에는 Bearer header를 붙이지 않는다.

```text
POST /api/auth/oauth
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/dev
```

백엔드는 현재 public auth endpoint에서 JWT filter를 skip하므로 만료된 Authorization header가 refresh를 막지는 않는다.
그래도 client 쪽에서 auth endpoint header를 제외하는 편이 단순하고 혼동이 적다.

---

## 8. Refresh Flow

refresh trigger:

```text
access token이 로컬 기준 만료됨
또는 protected API가 AUTH_003 / C003 401을 반환하고 refreshToken이 있음
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

성공 처리:

```text
accessToken과 refreshToken을 모두 교체한다.
원래 요청은 한 번만 재시도한다.
```

실패 처리:

```text
로컬 token을 삭제한다.
로그인 화면으로 이동한다.
refresh를 무한 반복하지 않는다.
```

권장 interceptor 정책:

```text
single-flight refresh lock을 사용한다.
여러 요청이 동시에 401을 받으면 refresh 요청은 하나만 실행한다.
나머지 요청은 refresh 결과를 기다린 뒤 한 번만 재시도한다.
```

refresh rotation 주의:

```text
refresh 성공 때마다 기존 refresh token은 revoke된다.
앱은 새 refreshToken을 즉시 저장해야 한다.
refresh rotation은 기존 refresh token의 deviceId를 유지한다.
refresh 중 deviceId를 바꾸지 않는다. 앱 설치/기기 identity가 바뀌면 OAuth login을 다시 수행한다.
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

Client 처리:

```text
refreshToken이 있으면 logout을 호출한다.
네트워크 결과와 무관하게 로컬 token과 user cache를 삭제한다.
logged-out UI 상태로 이동한다.
```

logout은 제출한 refresh token만 revoke한다. 모든 기기 session을 revoke하지는 않는다.

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

사용처:

```text
앱 시작 시 저장된 access token 검증
user profile cache 갱신
OAuth login 또는 refresh 후 로그인 상태 확인
```

`/api/auth/me`가 401을 반환하면:

```text
refreshToken이 있으면 refresh를 시도한다.
없거나 refresh 실패면 token을 삭제하고 logged-out 상태를 표시한다.
```

---

## 11. Public / Protected API 구분

Bearer 없이 호출 가능한 public endpoint:

```text
GET /api/scenarios
GET /api/scenarios/{scenarioId}
POST /api/auth/oauth
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/dev
```

public이지만 Bearer 권장:

```text
GET /api/scenarios/{scenarioId}
```

이유: 인증된 작성자는 자기 DRAFT/PRIVATE 시나리오를 볼 수 있다. 익명 사용자는 public/playable data만 본다.

백엔드가 `AUTH_REQUIRE_AUTHENTICATION=true`로 전환하면 보호되는 endpoint:

```text
/api/play-sessions/**
/api/device-tokens/**
/api/notifications/**
POST /api/scenarios/**
PATCH /api/scenarios/**
GET /api/scenarios/{scenarioId}/validation-result
POST /api/ai/scenarios/{scenarioId}/validate
/api/** 신규 endpoint 기본 보호
```

지금부터 Bearer 준비가 필요한 gameplay endpoint:

```text
POST /api/play-sessions
POST /api/play-sessions/{sessionId}/interrogations
POST /api/play-sessions/{sessionId}/final-deduction
GET /api/play-sessions/{sessionId}/result
```

---

## 12. 처리해야 할 Error Code

| Code | HTTP | 의미 | Android 처리 |
|---|---:|---|---|
| `AUTH_001` | 403 | dev login disabled | 운영에서 dev login 사용 금지 |
| `AUTH_002` | 500 | 서버 JWT secret 미설정 | 일반 서버 오류 표시, 백엔드 보고 |
| `AUTH_003` | 401 | invalid/expired access token | refresh 1회 시도 |
| `AUTH_004` | 401 | refresh token invalid | token 삭제, 로그인 필요 |
| `AUTH_005` | 401 | refresh token expired | token 삭제, 로그인 필요 |
| `AUTH_006` | 401 | user not found | token 삭제, 로그인 필요 |
| `AUTH_007` | 403 | user blocked/withdrawn | token 삭제, 접근 제한 표시 |
| `AUTH_008` | 400 | 지원하지 않는 OAuth provider | client bug 또는 미지원 로그인 |
| `AUTH_009` | 400 | provider token 누락 | client request bug |
| `AUTH_010` | 500 | provider config 누락 | 일반 서버 오류 표시, 백엔드 보고 |
| `AUTH_011` | 401 | provider token 검증 실패 | provider login 재시도 안내 |
| `AUTH_012` | 409 | OAuth account conflict | 계정 연결 충돌 메시지 표시 |
| `C003` | 401 | generic unauthorized | token이 있으면 refresh 1회 시도 |
| `C004` | 403 | generic forbidden | refresh loop 금지, forbidden 상태 표시 |

validation error는 다른 common validation code와 `details`를 반환할 수 있다.
field-level message가 있으면 표시한다.

---

## 13. 권장 Android 상태 머신

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

## 14. QA 체크리스트

백엔드 protected mode 전환 전 확인:

- [ ] Google login이 `/api/auth/oauth`에 `provider=GOOGLE`, `idToken`을 보낸다.
- [ ] Kakao login이 `/api/auth/oauth`에 `provider=KAKAO`, `accessToken`을 보낸다.
- [ ] Web Kakao login은 `/api/auth/oauth/kakao/code`에 `authorizationCode`, `redirectUri`, `deviceId`를 보낸다.
- [ ] 앱이 access/refresh token을 안전하게 저장한다.
- [ ] protected/gameplay 요청에 token이 있으면 `Authorization: Bearer`를 붙인다.
- [ ] 가능하면 auth endpoint는 자동 Bearer header 첨부에서 제외한다.
- [ ] 로그인 후 `/api/auth/me`가 성공한다.
- [ ] 강제로 만료/invalid access token을 넣었을 때 refresh 1회 후 원래 요청을 한 번만 재시도한다.
- [ ] refresh 성공 시 두 token을 모두 교체한다.
- [ ] refresh 실패 시 token을 삭제하고 logged-out UI로 이동한다.
- [ ] logout은 network 실패 여부와 무관하게 로컬 token을 삭제한다.
- [ ] UI가 `user.email == null`을 허용한다.
- [ ] client role만 보고 admin/rate-limit bypass UI를 노출하지 않는다.

백엔드가 `AUTH_REQUIRE_AUTHENTICATION=true`로 전환한 뒤 smoke:

```text
token 없는 protected gameplay API -> 401
OAuth login -> token 발급
같은 gameplay API + Bearer -> 성공 또는 정상 domain error
expired access token + valid refresh token -> refresh -> retry 성공
invalid refresh token -> logged-out 상태
```

---

## 15. 구현 메모

API base URL과 path string은 분리한다.

```text
baseUrl = https://api.clueroom.xyz
path = /api/auth/oauth
```

아래처럼 만들지 않는다.

```text
https://api.clueroom.xyz/api + /api/auth/oauth
```

서버 응답의 `expiresIn`으로 local expiry time을 계산한다.
앱이 active 상태라면 만료 직전에 refresh한다.

권장 local expiry 계산:

```text
accessTokenExpiresAt = now + expiresIn - 30 seconds
```

앱이 background에서 돌아왔을 때 token이 이미 만료됐다면 protected request를 보내기 전에 refresh한다.

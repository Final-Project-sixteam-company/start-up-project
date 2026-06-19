# CaseLab AI API 명세서 & API 테이블

> 버전: MVP v0.1  
> 기준 플랫폼: Android App + Spring Boot Backend  
> 프로젝트 정체성: AI 용의자 심문형 추리게임 + 커스텀 시나리오 공유 플랫폼  
> 작성 목적: 팀 백엔드/프론트/Android 개발자가 공통으로 참고할 API 설계 초안

---

## 0. 프로젝트 요약

**CaseLab AI**는 사용자가 탐정이 되어 사건을 조사하고, AI 용의자를 심문하며, 증거와 알리바이를 조합해 범인·동기·범행 방법을 추리하는 Android 추리게임 앱이다.

핵심 구조는 다음과 같다.

```text
시나리오 선택
→ 사건 브리핑
→ 현장 정보 확인
→ 증거 카드 확인
→ 용의자 심문
→ 시간/조건별 증거 해금
→ 힌트 확인
→ 최종 추리 제출
→ 결과 해설
```

또한 제작진이 만든 공식 시나리오뿐 아니라, 유저가 직접 만든 **커스텀 시나리오**를 등록·공유·평가할 수 있는 구조를 가진다.

---

## 1. API 설계 원칙

### 1.1 REST 기본 규칙

```text
GET     조회
POST    생성 / 실행
PATCH   부분 수정
DELETE  삭제
```

### 1.2 Base URL

개발 환경 예시:

```text
http://localhost:8080
```

배포 환경 예시:

```text
https://api.clueroom.xyz
```

실제 컨트롤러 경로는 `/api/...` prefix를 포함한다.

### 1.3 공통 Header

```http
Content-Type: application/json
Authorization: Bearer {accessToken}
```

인증이 필요 없는 API는 `Authorization` 생략 가능.

### 1.4 ID 필드 네이밍 규칙

- JPA Entity의 PK 필드명은 `id`를 사용한다.
- API Request/Response DTO에서는 Android 화면 매핑과 혼동을 줄이기 위해 `scenarioId`, `evidenceId`, `suspectId`처럼 자원명이 포함된 필드명을 우선 사용한다.
- 중첩 객체도 다른 엔티티를 참조하면 `creatorId`, `locationId`, `reviewId`처럼 명시한다.

---

## 2. 공통 응답 형식

### 2.1 성공 응답

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### 2.2 실패 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "SCENARIO_NOT_FOUND",
    "message": "시나리오를 찾을 수 없습니다."
  }
}
```

### 2.3 페이지 응답

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 125,
    "totalPages": 7,
    "hasNext": true
  },
  "error": null
}
```

---

## 3. 주요 Enum

### 3.1 ScenarioType

```text
OFFICIAL
CUSTOM
```

### 3.2 ScenarioVisibility

```text
PRIVATE
UNLISTED
PUBLIC
OFFICIAL
```

### 3.3 ScenarioStatus

```text
DRAFT
VALIDATING
PUBLISHED
HIDDEN
DELETED
```

### 3.4 Difficulty

```text
EASY
NORMAL
HARD
```

### 3.5 PlaySessionStatus

```text
PLAYING
COMPLETED
ABANDONED
```

### 3.6 EvidenceImportance

```text
LOW
NORMAL
HIGH
CORE
FAKE
```

### 3.7 EvidenceUnlockType

```text
NONE
TIME
INTERROGATION
EVIDENCE_PRESENTED
MANUAL
```

### 3.8 QuestionType

```text
FREE
RECOMMENDED
EVIDENCE_PRESENTED
```

### 3.9 AIRequestType

```text
SCENARIO_DRAFT
SCENARIO_VALIDATE
NPC_INTERROGATION
FINAL_DEDUCTION_SCORE
CASE_EXPLANATION
```

### 3.10 ScenarioValidationStatus

```text
PENDING
PASSED
PASSED_WITH_WARNINGS
NEEDS_FIX
FAILED
```

---

# 4. API 전체 테이블

## 4.1 인증 / 유저 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/auth/oauth` | Google/Kakao provider token으로 ClueRoom token 발급 | X | O |
| 2 | POST | `/api/auth/oauth/kakao/code` | Web Kakao authorizationCode로 ClueRoom token 발급 | X | O |
| 3 | POST | `/api/auth/dev` | local/staging 개발용 로그인. 운영 기본 disabled | X | O |
| 4 | POST | `/api/auth/refresh` | Refresh token rotation + 새 access token 발급 | X/Refresh | O |
| 5 | POST | `/api/auth/logout` | 제출한 refresh token revoke | X/Refresh | O |
| 6 | GET | `/api/auth/me` | 현재 인증 사용자 조회 | O | O |

---

## 4.2 시나리오 라이브러리 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | GET | `/api/scenarios` | 시나리오 목록 조회 | 선택 | O |
| 2 | GET | `/api/scenarios/{scenarioId}` | 시나리오 상세 조회 | 선택 | O |
| 3 | POST | `/api/scenarios` | 커스텀 시나리오 생성 | O | O |
| 4 | PATCH | `/api/scenarios/{scenarioId}` | 시나리오 기본 정보 수정 | O | O |
| 5 | DELETE | `/api/scenarios/{scenarioId}` | 시나리오 삭제 | O | O |
| 6 | POST | `/api/scenarios/{scenarioId}/publish` | 시나리오 공개 등록 | O | O |
| 7 | POST | `/api/scenarios/{scenarioId}/hide` | 시나리오 비공개/숨김 | O | O |
| 8 | GET | `/api/scenarios/me` | 내가 만든 시나리오 조회 | O | O |
| 9 | GET | `/api/scenarios/bookmarked` | 북마크한 시나리오 조회 | O | O |

---

## 4.3 시나리오 제작 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/scenarios/{scenarioId}/locations` | 장소 등록 | O | O |
| 2 | GET | `/api/scenarios/{scenarioId}/locations` | 장소 목록 조회 | O | O |
| 3 | POST | `/api/scenarios/{scenarioId}/victim` | 피해자 정보 등록/수정 | O | O |
| 4 | GET | `/api/scenarios/{scenarioId}/victim` | 피해자 정보 조회 | O | O |
| 5 | POST | `/api/scenarios/{scenarioId}/suspects` | 용의자 등록 | O | O |
| 6 | GET | `/api/scenarios/{scenarioId}/suspects` | 용의자 목록 조회 | O | O |
| 7 | PATCH | `/api/suspects/{suspectId}` | 용의자 수정 | O | O |
| 8 | DELETE | `/api/suspects/{suspectId}` | 용의자 삭제 | O | O |
| 9 | POST | `/api/scenarios/{scenarioId}/evidences` | 증거 등록 | O | O |
| 10 | GET | `/api/scenarios/{scenarioId}/evidences` | 증거 목록 조회 | O | O |
| 11 | PATCH | `/api/evidences/{evidenceId}` | 증거 수정 | O | O |
| 12 | DELETE | `/api/evidences/{evidenceId}` | 증거 삭제 | O | O |
| 13 | POST | `/api/scenarios/{scenarioId}/hints` | 힌트 등록 | O | O |
| 14 | GET | `/api/scenarios/{scenarioId}/hints` | 힌트 목록 조회 | O | O |
| 15 | POST | `/api/scenarios/{scenarioId}/solution` | 정답 등록/수정 | O/작성자·관리자 | O |
| 16 | GET | `/api/scenarios/{scenarioId}/solution` | 정답 조회 | O/작성자·관리자 전용, Android 플레이 화면 호출 금지 | O |

현재 `develop` 기준 커스텀 시나리오 컨트롤러는 create/upsert, 조회, 수정, 삭제, 정답 관리의 기본 API를 제공한다.
Android/웹 플레이 화면은 제작자 전용 solution API를 호출하지 않는다.

---

## 4.4 AI 시나리오 생성 / 검증 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/ai/scenarios/draft` | AI 시나리오 초안 생성 | O | △ 미구현 |
| 2 | POST | `/api/ai/scenarios/{scenarioId}/validate` | 시나리오 논리 검증 | O | O |
| 3 | GET | `/api/scenarios/{scenarioId}/validation-result` | 검증 결과 조회 | O | O |
| 4 | GET | `/api/ai/logs` | 내 AI 요청 로그 조회 | O | △ 미구현 |

현재 공개 AI 컨트롤러는 시나리오 검증 요청과 검증 결과 조회만 제공한다.
AI draft 생성과 AI log 조회 REST API는 아직 없다.

---

## 4.5 게임 플레이 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/play-sessions` | 게임 세션 시작 | O | O |
| 2 | GET | `/api/play-sessions/active?scenarioId={scenarioId}` | 진행 중 세션 조회 | O | O |
| 3 | GET | `/api/play-sessions/records` | 내 플레이 기록 조회 | O | O |
| 4 | GET | `/api/play-sessions/{sessionId}` | 게임 세션 기본 정보 조회 | O | O |
| 5 | GET | `/api/play-sessions/{sessionId}/dashboard` | 탐정 대시보드 조회 | O | O |
| 6 | GET | `/api/play-sessions/{sessionId}/locations` | 현장 정보 조회 | O | O |
| 7 | GET | `/api/play-sessions/{sessionId}/evidences` | 현재 해금된 증거 조회 | O | O |
| 8 | GET | `/api/play-sessions/{sessionId}/evidences/{evidenceId}` | 증거 상세 조회 | O | O |
| 9 | POST | `/api/play-sessions/{sessionId}/evidences/{evidenceId}/unlock` | 증거 수동/조건 해금 | O | O |
| 10 | GET | `/api/play-sessions/{sessionId}/suspects` | 용의자 목록 조회 | O | O |
| 11 | GET | `/api/play-sessions/{sessionId}/suspects/{suspectId}` | 용의자 상세 조회 | O | O |
| 12 | GET | `/api/play-sessions/{sessionId}/timeline` | 타임라인 조회 | O | O |
| 13 | GET | `/api/play-sessions/{sessionId}/hints` | 사용 가능 힌트 조회 | O | O |
| 14 | POST | `/api/play-sessions/{sessionId}/hints/{hintId}/use` | 힌트 사용 | O | O |
| 15 | POST | `/api/play-sessions/{sessionId}/abandon` | 게임 포기/중단 | O | O |

---

## 4.6 심문 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/play-sessions/{sessionId}/interrogations` | AI 용의자 심문 | O | O |
| 2 | GET | `/api/play-sessions/{sessionId}/interrogations` | 심문 로그 조회 | O | O |
| 3 | GET | `/api/play-sessions/{sessionId}/interrogations?suspectId={suspectId}` | 특정 용의자 심문 로그 조회 | O | O |
| 4 | GET | `/api/play-sessions/{sessionId}/recommended-questions` | 별도 추천 질문 API | O | X 미구현. 증거 기반 질문은 증거 상세 `guidance.suggestedQuestions` 사용 |

---

## 4.7 최종 추리 / 결과 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/play-sessions/{sessionId}/final-deduction` | 최종 추리 제출 | O | O |
| 2 | GET | `/api/play-sessions/{sessionId}/result` | 결과/해설 조회 | O | O |

---

## 4.8 커뮤니티 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/scenarios/{scenarioId}/bookmarks` | 시나리오 북마크 | O | O |
| 2 | DELETE | `/api/scenarios/{scenarioId}/bookmarks` | 북마크 취소 | O | O |
| 3 | POST | `/api/scenarios/{scenarioId}/reviews` | 리뷰 작성 | O | O |
| 4 | GET | `/api/scenarios/{scenarioId}/reviews` | 리뷰 목록 조회 | 선택 | O |
| 5 | PATCH | `/api/reviews/{reviewId}` | 리뷰 수정 | O | O |
| 6 | DELETE | `/api/reviews/{reviewId}` | 리뷰 삭제 | O | O |
| 7 | POST | `/api/scenarios/{scenarioId}/reports` | 시나리오 신고 | O | O |

---

## 4.9 푸시 알림 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/api/device-tokens` | Android FCM registration token 등록/upsert | O | O |
| 2 | POST | `/api/notifications/test` | 현재 사용자 active token 대상 테스트 푸시 발송 | O | local/test only |

---

# 5. 상세 API 명세

---

> 현재 인증 foundation은 구현되어 있다.
> 로컬/전환 테스트에서는 `AUTH_REQUIRE_AUTHENTICATION=false`, `AUTH_MOCK_FALLBACK_ENABLED=true`로 token 없는 기존 gameplay API를 `MOCK_USER_ID`에 fallback할 수 있다.
> 운영 보호 모드에서는 `AUTH_REQUIRE_AUTHENTICATION=true`를 사용하며, 명시 public endpoint 외 `/api/**`는 인증 대상이다.
> Android 상세 연동 기준은 [ANDROID_AUTH_INTEGRATION_GUIDE.md](ANDROID_AUTH_INTEGRATION_GUIDE.md)를 따른다.

## 5.1 OAuth 로그인

```http
POST /api/auth/oauth
```

### Request

Google:

```json
{
  "provider": "GOOGLE",
  "idToken": "google-id-token-from-android",
  "deviceId": "android-installation-id"
}
```

Kakao:

```json
{
  "provider": "KAKAO",
  "accessToken": "kakao-access-token-from-android",
  "deviceId": "android-installation-id"
}
```

Web Kakao JS SDK v2 authorization-code flow는 access token을 프론트에서 받지 않고 별도 endpoint를 사용한다.
프론트는 `Kakao.Auth.authorize()`가 redirect로 반환한 code만 백엔드에 전달하고, 백엔드는 서버 간 token exchange 후 기존 ClueRoom token response만 반환한다.

```http
POST /api/auth/oauth/kakao/code
```

```json
{
  "authorizationCode": "kakao-authorization-code-from-web-redirect",
  "redirectUri": "https://www.clueroom.xyz",
  "deviceId": "browser-installation-id"
}
```

`redirectUri`는 Kakao console에 등록된 redirect URI와 프론트가 `Kakao.Auth.authorize()`에 넘긴 값과 정확히 같아야 한다.
백엔드는 `KAKAO_REST_API_KEY`로 Kakao token endpoint를 호출한 뒤 access token info/user info를 검증한다.
Kakao access/refresh token 원문은 프론트 응답과 로그에 남기지 않는다.

### Response

`/api/auth/oauth`, `/api/auth/oauth/kakao/code`, `/api/auth/dev`, `/api/auth/refresh`는 같은 token response shape를 반환한다.

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
      "nickname": "탐정순구",
      "profileImageUrl": null,
      "role": "USER"
    }
  },
  "error": null
}
```

## 5.2 개발용 로그인

```http
POST /api/auth/dev
```

운영 기본값은 disabled다.
`AUTH_DEV_LOGIN_ENABLED=false`이면 `AUTH_001`로 실패한다.

```json
{
  "email": "dev@example.com",
  "nickname": "Dev User",
  "deviceId": "android-emulator"
}
```

## 5.4 Token refresh

```http
POST /api/auth/refresh
```

Refresh token은 JWT가 아닌 opaque random token이다.
refresh 성공 시 기존 refresh token은 revoke되고 새 access/refresh token pair가 발급된다.

```json
{
  "refreshToken": "stored-refresh-token",
  "deviceId": "android-installation-id"
}
```

## 5.5 Logout

```http
POST /api/auth/logout
```

제출한 refresh token만 revoke한다.
모든 기기 세션을 한 번에 revoke하는 API는 아직 없다.

```json
{
  "refreshToken": "stored-refresh-token"
}
```

## 5.6 내 정보 조회

```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

### Response

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "탐정순구",
    "profileImageUrl": null,
    "role": "USER"
  },
  "error": null
}
```

## 5.6 FCM 디바이스 토큰 등록

Android 앱이 Firebase Cloud Messaging에서 발급받은 registration token을 백엔드 사용자와 연결한다.

```http
POST /api/device-tokens
Authorization: Bearer {accessToken}
```

운영 인증 강제 전환 전에는 `MockUserProvider` 호환 경로로 현재 사용자를 결정할 수 있다.

### Request

```json
{
  "token": "fcm_registration_token",
  "deviceType": "ANDROID"
}
```

규칙:

```text
token: 필수, 512자 이하
deviceType: 선택, 30자 이하, 생략 시 ANDROID 기본값
```

현재 구현은 token unique 기준으로 신규 등록과 재등록을 같은 경로에서 처리한다.
같은 token이 다시 들어오면 userId, deviceType, active, lastUsedAt을 최신값으로 갱신한다.

### Response

```json
{
  "success": true,
  "data": {
    "deviceTokenId": 1,
    "active": true
  },
  "error": null
}
```

응답에는 FCM token 원문을 반환하지 않는다.

## 5.7 테스트 푸시 발송

개발/검증용 API다.
`NotificationTestController`는 `local`, `test` profile에서만 route를 등록한다.

```http
POST /api/notifications/test
Authorization: Bearer {accessToken}
```

### Request

```json
{
  "title": "ClueRoom",
  "body": "테스트 푸시 알림입니다."
}
```

규칙:

```text
title: 필수, 100자 이하
body: 필수, 500자 이하
```

### Response

```json
{
  "success": true
}
```

주의:

```text
운영 profile에서는 /api/notifications/test route가 없어야 한다.
FCM_ENABLED=false 또는 FirebaseApp 미초기화 상태에서는 N001 FCM_DISABLED로 실패한다.
```

---

# 6. 시나리오 라이브러리 API

## 6.1 시나리오 목록 조회

```http
GET /api/scenarios
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| keyword | String | N | 제목/설명 검색어 |
| type | String | N | OFFICIAL, CUSTOM |
| difficulty | String | N | EASY, NORMAL, HARD |
| visibility | String | N | PUBLIC, OFFICIAL |
| minPlayers | Integer | N | 최소 플레이 인원 |
| maxPlayers | Integer | N | 최대 플레이 인원 |
| maxPlayTime | Integer | N | 최대 예상 플레이 시간 |
| sort | String | N | popular, latest, rating |
| page | Integer | N | 페이지 번호 |
| size | Integer | N | 페이지 크기 |

### Example

```http
GET /api/scenarios?type=CUSTOM&difficulty=NORMAL&sort=popular&page=0&size=20
```

### Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "scenarioId": 10,
        "title": "데모데이 전야 살인사건",
        "description": "AI 스타트업 대표가 데모데이 전날 사망한 사건",
        "thumbnailUrl": "https://example.com/thumb.png",
        "scenarioType": "OFFICIAL",
        "status": "PUBLISHED",
        "difficulty": "NORMAL",
        "estimatedPlayTimeMinutes": 30,
        "playerCountMin": 1,
        "playerCountMax": 1,
        "suspectCount": 5,
        "evidenceCount": 15,
        "playCount": 1234,
        "averageRating": 4.7,
        "isBookmarked": false,
        "canPlay": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "error": null
}
```

---

## 6.1.1 내가 만든 시나리오 조회

```http
GET /api/scenarios/me
Authorization: Bearer {accessToken}
```

내가 작성한 시나리오 목록을 조회한다. 일반 목록과 달리 `DRAFT`, `VALIDATING`, `HIDDEN`, `PUBLISHED` 상태의 시나리오가 모두 반환된다. (`DELETED` 제외)

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| page | Integer | N | 페이지 번호 (기본값: 0) |
| size | Integer | N | 페이지 크기 (기본값: 20) |

### Response

`6.1 시나리오 목록 조회`의 응답 규격과 동일하며, 내 시나리오의 상태 구분을 위해 `status` 필드를 참조한다.

---

## 6.1.2 내가 북마크한 시나리오 조회

```http
GET /api/scenarios/bookmarked
Authorization: Bearer {accessToken}
```

내가 북마크한 시나리오 목록을 조회한다. 
단, 원작자가 삭제(`DELETED`)하거나 비공개(`HIDDEN`) 처리한 시나리오는 북마크 목록에서도 제외되어 보이지 않는다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| page | Integer | N | 페이지 번호 (기본값: 0) |
| size | Integer | N | 페이지 크기 (기본값: 20) |

### Response

`6.1 시나리오 목록 조회`의 응답 규격과 동일하다. 북마크한 항목이므로 응답 내 `isBookmarked`는 항상 `true`로 고정된다.

---

## 6.2 시나리오 상세 조회

```http
GET /api/scenarios/{scenarioId}
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 10,
    "title": "데모데이 전야 살인사건",
    "description": "AI 스타트업 모노로그랩스의 대표 강도현이 사망한 사건",
    "synopsis": "처음에는 알레르기 쇼크로 보였지만 현장에는 수상한 단서가 남아 있었다.",
    "scenarioType": "OFFICIAL",
    "visibility": "OFFICIAL",
    "difficulty": "NORMAL",
    "estimatedPlayTimeMinutes": 30,
    "playerCountMin": 1,
    "playerCountMax": 1,
    "playCount": 1234,
    "averageRating": 4.7,
    "ratingCount": 312,
    "suspectCount": 5,
    "evidenceCount": 15,
    "hintCount": 3,
    "tags": ["스타트업", "살인", "디지털증거", "보통"],
    "creator": {
      "creatorId": 1,
      "nickname": "운영자"
    },
    "isBookmarked": false,
    "canPlay": true
  },
  "error": null
}
```

---

## 6.3 커스텀 시나리오 생성

```http
POST /api/scenarios
```

### Request

```json
{
  "title": "동아리 회비 실종 사건",
  "description": "MT 회비 5만 원이 사라진 사건",
  "synopsis": "모두가 돈을 냈다고 주장하지만 정산표에는 빈칸이 있다.",
  "scenarioType": "CUSTOM",
  "visibility": "PRIVATE",
  "difficulty": "EASY",
  "playerCountMin": 1,
  "playerCountMax": 3,
  "estimatedPlayTimeMinutes": 20
}
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 25,
    "status": "DRAFT"
  },
  "error": null
}
```

---

## 6.4 시나리오 기본 정보 수정

```http
PATCH /api/scenarios/{scenarioId}
```

### Request

```json
{
  "title": "동아리 회비 실종 사건",
  "description": "MT 회비가 사라진 사건",
  "difficulty": "NORMAL",
  "estimatedPlayTimeMinutes": 25
}
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 25,
    "updated": true
  },
  "error": null
}
```

---

## 6.5 시나리오 공개 등록

```http
POST /api/scenarios/{scenarioId}/publish
```

### Request

```json
{
  "visibility": "PUBLIC"
}
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 25,
    "visibility": "PUBLIC",
    "status": "PUBLISHED",
    "publishedAt": "2026-05-15T19:30:00"
  },
  "error": null
}
```

---

## 6.6 시나리오 숨김 (비공개 전환)

```http
POST /api/scenarios/{scenarioId}/hide
Authorization: Bearer {accessToken}
```

공개(`PUBLISHED`) 상태의 시나리오를 숨김(`HIDDEN`) 상태로 변경한다. (본인만 가능)

### Request

(Empty Body)

### Response

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 6.7 시나리오 삭제 (Soft Delete)

```http
DELETE /api/scenarios/{scenarioId}
Authorization: Bearer {accessToken}
```

작성자가 본인의 시나리오를 삭제한다. (DB에서 Hard Delete되지 않으며, 상태가 `DELETED`로 변경됨)
삭제 시 목록 조회, 상세 조회 및 커뮤니티(리뷰, 북마크) 도메인에서의 신규 조작이 원천 차단된다. (진행 중이던 플레이 세션은 끝까지 플레이 가능)

### Request

(Empty Body)

### Response

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

# 7. 시나리오 제작 API

## 7.1 장소 등록

```http
POST /api/scenarios/{scenarioId}/locations
```

### Request

```json
{
  "name": "데모룸",
  "description": "피해자가 발견된 장소",
  "floor": "1F",
  "mapX": 120,
  "mapY": 80,
  "imageAssetKey": "bg_demoroom_01",
  "sortOrder": 1
}
```

### Response

```json
{
  "success": true,
  "data": {
    "locationId": 1
  },
  "error": null
}
```

---

## 7.2 장소 목록 조회

```http
GET /api/scenarios/{scenarioId}/locations
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "locationId": 1,
      "name": "데모룸",
      "description": "피해자가 발견된 장소",
      "mapX": 120,
      "mapY": 80,
      "floor": "1F",
      "imageAssetKey": "bg_demoroom_01",
      "sortOrder": 1,
      "evidenceCount": 3
    }
  ],
  "error": null
}
```

---

## 7.3 피해자 정보 등록/수정

```http
POST /api/scenarios/{scenarioId}/victim
```

### Request

```json
{
  "name": "강도현",
  "age": 34,
  "role": "모노로그랩스 대표",
  "description": "독단적이고 성과 중심적인 대표",
  "causeOfDeath": "알레르기 쇼크",
  "foundLocationId": 1,
  "foundCondition": "데모룸 바닥에 쓰러진 채 발견됨"
}
```

### Response

```json
{
  "success": true,
  "data": {
    "victimId": 1
  },
  "error": null
}
```

---

## 7.4 용의자 등록

```http
POST /api/scenarios/{scenarioId}/suspects
```

### Request

```json
{
  "name": "박재민",
  "role": "CFO",
  "relationToVictim": "공동창업자",
  "publicProfile": "회사의 재무를 담당하는 인물",
  "publicStatement": "사건 당시 재무팀 자리에서 투자 자료를 정리하고 있었다.",
  "alibi": "22시 이후 데모룸 근처에 가지 않았다고 주장한다.",
  "personalityPrompt": "차분하지만 방어적이고, 불리한 질문에는 짧게 회피한다.",
  "responsePolicyJson": {
    "maxSentences": 2,
    "allowExternalFacts": false,
    "defaultStance": "defensive"
  },
  "suspicionLevel": 60
}
```

### Response

```json
{
  "success": true,
  "data": {
    "suspectId": 1
  },
  "error": null
}
```

---

## 7.5 용의자 목록 조회

```http
GET /api/scenarios/{scenarioId}/suspects
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "suspectId": 1,
      "name": "박재민",
      "role": "CFO",
      "relationToVictim": "공동창업자",
      "publicStatement": "재무팀 자리에서 투자 자료를 정리하고 있었다.",
      "alibi": "22시 이후 데모룸 근처에 가지 않았다고 주장한다.",
      "suspicionLevel": 60
    }
  ],
  "error": null
}
```

---

## 7.6 증거 등록

```http
POST /api/scenarios/{scenarioId}/evidences
```

### Request

```json
{
  "locationId": 1,
  "title": "찢긴 컵 라벨",
  "description": "쓰레기통에서 발견된 컵 라벨 조각에는 '...MOND LAT...'라는 글자가 남아 있다.",
  "evidenceType": "PHYSICAL",
  "importance": "CORE",
  "imageUrl": null,
  "isInitialPublic": true,
  "unlockType": "NONE",
  "unlockAfterMinutes": null,
  "relatedSuspectIds": [1]
}
```

### Response

```json
{
  "success": true,
  "data": {
    "evidenceId": 1
  },
  "error": null
}
```

---

## 7.7 증거 목록 조회

```http
GET /api/scenarios/{scenarioId}/evidences
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "evidenceId": 1,
      "title": "찢긴 컵 라벨",
      "description": "쓰레기통에서 발견된 컵 라벨 조각",
      "locationName": "데모룸",
      "importance": "CORE",
      "isInitialPublic": true,
      "unlockType": "NONE",
      "relatedSuspects": [
        {
          "suspectId": 1,
          "name": "박재민"
        }
      ]
    }
  ],
  "error": null
}
```

---

## 7.8 힌트 등록

```http
POST /api/scenarios/{scenarioId}/hints
```

### Request

```json
{
  "hintLevel": 1,
  "content": "피해자가 마신 음료와 알레르기 정보를 함께 보세요.",
  "unlockAfterMinutes": 10,
  "penaltyScore": 5
}
```

### Response

```json
{
  "success": true,
  "data": {
    "hintId": 1
  },
  "error": null
}
```

---

## 7.9 정답 등록/수정

```http
POST /api/scenarios/{scenarioId}/solution
```

### Request

```json
{
  "culpritSuspectId": 1,
  "motive": "회사 자금 유용 사실이 데모데이에서 공개될 것을 막기 위해",
  "method": "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 했다.",
  "coverUp": "피해자의 휴대폰으로 사망 이후 메시지를 보내 생존한 것처럼 위장했다.",
  "keyEvidenceIds": [1, 2, 3],
  "fullExplanation": "범인은 CFO 박재민이다. 그는 회계 비리 발각을 막기 위해..."
}
```

### Response

```json
{
  "success": true,
  "data": {
    "solutionId": 1
  },
  "error": null
}
```

---

# 8. AI 시나리오 생성 / 검증 API

## 8.1 AI 시나리오 초안 생성 (2차)

현재 `develop` 기준 컨트롤러가 없는 후속 API다.
아래 request/response는 2차 MVP 후보 계약으로만 본다.

```http
POST /api/ai/scenarios/draft
```

### Request

```json
{
  "background": "스타트업",
  "caseType": "살인",
  "difficulty": "NORMAL",
  "suspectCount": 5,
  "estimatedPlayTimeMinutes": 30,
  "style": "크라임씬 느낌의 현대 미스터리",
  "additionalRequest": "디지털 증거와 알리바이 조작을 포함해줘."
}
```

### Response

```json
{
  "success": true,
  "data": {
    "draftTitle": "데모데이 전야 살인사건",
    "summary": "AI 스타트업 대표가 데모데이 전날 사망한 사건",
    "caseGraph": {
      "victim": {
        "name": "강도현",
        "role": "대표"
      },
      "suspects": [],
      "evidences": [],
      "hints": [],
      "solution": {}
    }
  },
  "error": null
}
```

`caseGraph.solution`은 커스텀 시나리오 제작자 편집 화면 전용 초안이다. 플레이 API, 심문 프롬프트, 일반 시나리오 상세 응답에 그대로 노출하면 안 된다.

---

## 8.2 AI 시나리오 검증

```http
POST /api/ai/scenarios/{scenarioId}/validate
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 25,
    "validationStatus": "PASSED_WITH_WARNINGS",
    "validationScore": 78,
    "problemSummary": "범인을 가리키는 핵심 증거는 존재하지만, 일부 페이크 단서가 약합니다.",
    "suggestion": "무고한 용의자에게도 숨기는 비밀을 추가하면 난이도가 더 좋아집니다.",
    "checkItems": [
      {
        "name": "범인 설정 여부",
        "passed": true
      },
      {
        "name": "결정적 증거 존재 여부",
        "passed": true
      },
      {
        "name": "힌트 존재 여부",
        "passed": true
      },
      {
        "name": "페이크 단서 품질",
        "passed": false
      }
    ]
  },
  "error": null
}
```

---

# 9. 게임 플레이 API

## 9.1 게임 세션 시작

```http
POST /api/play-sessions
```

### Request

```json
{
  "scenarioId": 10
}
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 100,
    "scenarioId": 10,
    "status": "PLAYING",
    "startedAt": "2026-05-15T20:00:00"
  },
  "error": null
}
```

### Error Response - P002

이미 같은 사용자/시나리오에 `PLAYING` 세션이 있으면 409를 반환한다. `error.details`에는 `activeSessionId`만 포함할 수 있다. 일반 중복 생성과 unique race 경로 모두 별도 read-only 재조회로 `activeSessionId` 포함을 우선한다. 단, 장애성 조회 실패나 미발견 시에는 `details`가 없을 수 있으므로 클라이언트는 `GET /api/play-sessions/active?scenarioId=`로 fallback한다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "timestamp": "2026-06-04T15:30:00",
    "status": 409,
    "error": "CONFLICT",
    "code": "P002",
    "message": "이미 진행 중인 플레이 세션이 존재합니다.",
    "path": "/api/play-sessions",
    "details": {
      "activeSessionId": 100
    }
  }
}
```

## 9.1.1 진행 중인 플레이 세션 조회

```http
GET /api/play-sessions/active?scenarioId=10
```

### Response - active exists

```json
{
  "success": true,
  "data": {
    "hasActiveSession": true,
    "activeSessionId": 100,
    "scenarioId": 10,
    "status": "PLAYING",
    "startedAt": "2026-05-15T20:00:00"
  },
  "error": null
}
```

### Response - no active session

```json
{
  "success": true,
  "data": {
    "hasActiveSession": false,
    "activeSessionId": null,
    "scenarioId": 10,
    "status": null,
    "startedAt": null
  },
  "error": null
}
```

---

## 9.2 탐정 대시보드 조회

```http
GET /api/play-sessions/{sessionId}/dashboard
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 100,
    "scenarioId": 10,
    "scenarioTitle": "데모데이 전야 살인사건",
    "status": "PLAYING",
    "elapsedSeconds": 320,
    "unlockedEvidenceCount": 5,
    "totalEvidenceCount": 15,
    "hintUsedCount": 0,
    "interrogationCount": 3,
    "briefing": {
      "victimName": "강도현",
      "foundLocation": "데모룸",
      "summary": "대표 강도현이 데모데이 전날 밤 데모룸에서 사망했다."
    }
  },
  "error": null
}
```

---

## 9.3 현장 정보 조회

```http
GET /api/play-sessions/{sessionId}/locations
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "scenarioId": 1,
    "scenarioTitle": "서월채",
    "mapImageUrl": "https://assets.example.com/official/seowolchae/v1/scenario/map.png",
    "locations": [
      {
        "locationId": 1,
        "locationCode": "LOC_DINING_ROOM",
        "name": "다이닝룸",
        "floor": "1F",
        "description": "만찬이 진행된 장소",
        "imageUrl": "https://assets.example.com/official/seowolchae/v1/locations/location-001.png",
        "mapX": 120,
        "mapY": 80,
        "totalEvidenceCount": 3,
        "unlockedEvidenceCount": 1
      }
    ]
  },
  "error": null
}
```

---

## 9.4 현재 해금된 증거 목록 조회

```http
GET /api/play-sessions/{sessionId}/evidences
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| includeLocked | Boolean | N | 잠긴 증거 포함 여부 |
| status | String | N | unlocked 등 해금 상태 필터. `includeLocked`와 함께 사용하지 않음 |

### Response

플레이어용 증거 목록 응답은 정답성/중요도 추론 metadata를 포함하지 않는다.
`importance`, `imageAssetKey` 같은 내부 seed/admin 필드는 public play 응답에 노출하지 않는다.

```json
{
  "success": true,
  "data": [
    {
      "evidenceId": 1,
      "title": "찢긴 컵 라벨",
      "oneLine": "컵 라벨 일부가 찢긴 채 발견됨",
      "description": "라벨 조각에는 '...MOND LAT...'라는 글자가 남아 있다.",
      "locationName": "데모룸",
      "isUnlocked": true,
      "imageUrl": "https://assets.example.com/official/demo/evidence/evidence-001.png",
      "relatedSuspects": [
        {
          "suspectId": 1,
          "name": "박재민"
        }
      ]
    },
    {
      "evidenceId": 2,
      "title": "휴대폰 위치 기록",
      "oneLine": null,
      "description": null,
      "locationName": null,
      "isUnlocked": false,
      "unlockHint": "15분 후 공개",
      "imageUrl": null
    }
  ],
  "error": null
}
```

---

## 9.5 증거 상세 조회

```http
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

### Response

플레이어용 증거 상세 응답도 `importance`, `imageAssetKey`를 포함하지 않는다.

```json
{
  "success": true,
  "data": {
    "evidenceId": 1,
    "title": "찢긴 컵 라벨",
    "description": "쓰레기통에서 발견된 컵 라벨 조각에는 '...MOND LAT...'라는 글자가 남아 있다.",
    "location": {
      "locationId": 1,
      "name": "데모룸"
    },
    "relatedSuspects": [
      {
        "suspectId": 1,
        "name": "박재민"
      }
    ],
    "relatedTimelineEvents": [
      {
        "time": "22:16",
        "title": "음료 컵이 데모룸 앞에 놓임"
      }
    ],
    "guidance": {
      "readingPoints": [
        "이 증거의 시간대가 다른 기록과 일치하는지 확인한다."
      ],
      "compareEvidences": [
        {
          "evidenceId": 2,
          "evidenceCode": "EVIDENCE_UNLOCKED_SAMPLE",
          "title": "해금된 비교 증거",
          "isUnlocked": true
        },
        {
          "evidenceId": 3,
          "title": "잠긴 비교 증거",
          "isUnlocked": false,
          "unlockHint": "조사 단계 진행 시 공개"
        }
      ],
      "suggestedQuestions": [
        {
          "targetSuspectId": 1,
          "targetName": "박재민",
          "question": "이 증거와 다른 기록의 차이를 설명할 수 있습니까?",
          "presentedEvidenceId": 1,
          "questionType": "EVIDENCE_PRESENTED"
        }
      ]
    }
  },
  "error": null
}
```

`guidance`는 optional이다. 해당 증거에 guidance seed가 없으면 `null` 또는 생략될 수 있다.

마스킹 규칙:

```text
현재 증거가 잠겨 있으면 기존 정책대로 상세 조회 자체가 차단된다.
compareEvidences의 해금 증거는 evidenceCode를 포함할 수 있다.
compareEvidences의 잠긴 증거는 evidenceCode를 포함하지 않는다.
잠긴 비교 증거는 `evidenceId`가 내려오더라도 `isUnlocked=false`이면 상세 이동에 사용하지 않는다.
프론트 표시에는 title / isUnlocked / unlockHint 수준만 사용한다.
suggestedQuestions는 심문 입력 prefill 용도이며 자동 전송하면 안 된다.
```

---

## 9.6 증거 해금

```http
POST /api/play-sessions/{sessionId}/evidences/{evidenceId}/unlock
```

### Request

```json
{
  "reason": "TIME_UNLOCK"
}
```

### Response

플레이어용 용의자 목록 응답은 후보 가능 여부나 의심도 점수 같은 정답성 metadata를 포함하지 않는다.
`culpritEligible`, `suspicionLevel`, `portraitAssetKey`는 public play 응답에 노출하지 않는다.

```json
{
  "success": true,
  "data": {
    "evidenceId": 2,
    "isUnlocked": true,
    "unlockedAt": "2026-05-15T20:15:00"
  },
  "error": null
}
```

---

## 9.7 용의자 목록 조회

```http
GET /api/play-sessions/{sessionId}/suspects
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "suspectId": 1,
      "name": "박재민",
      "role": "CFO",
      "relationToVictim": "공동창업자",
      "publicStatement": "재무팀 자리에서 투자 자료를 정리하고 있었다.",
      "alibi": "22시 이후 데모룸 근처에 가지 않았다고 주장한다.",
      "portraitImageUrl": "https://assets.example.com/official/demo/characters/character-001.png",
      "interrogationCount": 2
    }
  ],
  "error": null
}
```

---

## 9.8 용의자 상세 조회

```http
GET /api/play-sessions/{sessionId}/suspects/{suspectId}
```

### Response

플레이어용 용의자 상세 응답도 `suspicionLevel`, `culpritEligible`, `portraitAssetKey`를 포함하지 않는다.

```json
{
  "success": true,
  "data": {
    "suspectId": 1,
    "name": "박재민",
    "role": "CFO",
    "relationToVictim": "공동창업자",
    "publicProfile": "회사 재무를 담당하는 인물",
    "publicStatement": "사건 당시 재무팀 자리에서 투자자료를 정리하고 있었다.",
    "alibi": "데모룸 근처에는 가지 않았다고 주장한다.",
    "portraitImageUrl": "https://assets.example.com/official/demo/characters/character-001.png",
    "relatedEvidences": [
      {
        "evidenceId": 1,
        "title": "찢긴 컵 라벨",
        "isUnlocked": true
      }
    ],
    "interrogationLogs": []
  },
  "error": null
}
```

---

## 9.9 타임라인 조회

```http
GET /api/play-sessions/{sessionId}/timeline
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "time": "22:05",
      "title": "카페 결제",
      "description": "오트라떼와 아몬드라떼가 결제됨",
      "eventType": "FACT",
      "relatedEvidenceId": 3
    },
    {
      "time": "22:36",
      "title": "피해자 계정 메시지 전송",
      "description": "피해자 계정으로 단톡방 메시지가 전송됨",
      "eventType": "FACT",
      "relatedEvidenceId": 4
    }
  ],
  "error": null
}
```

---

## 9.10 힌트 목록 조회

```http
GET /api/play-sessions/{sessionId}/hints
```

### Response

```json
{
  "success": true,
  "data": [
    {
      "hintId": 1,
      "hintLevel": 1,
      "content": null,
      "isAvailable": true,
      "isUsed": false,
      "penaltyScore": 5
    },
    {
      "hintId": 2,
      "hintLevel": 2,
      "content": null,
      "isAvailable": false,
      "unlockAfterMinutes": 20,
      "isUsed": false,
      "penaltyScore": 10
    }
  ],
  "error": null
}
```

---

## 9.11 힌트 사용

```http
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
```

### Response

```json
{
  "success": true,
  "data": {
    "hintId": 1,
    "content": "피해자가 마신 음료와 알레르기 정보를 함께 보세요.",
    "penaltyScore": 5,
    "usedAt": "2026-05-15T20:10:00"
  },
  "error": null
}
```

---

# 10. 심문 API

## 10.1 AI 용의자 심문

```http
POST /api/play-sessions/{sessionId}/interrogations
```

### Request

```json
{
  "suspectId": 1,
  "questionType": "FREE",
  "question": "사건 당시 어디에 있었습니까?",
  "presentedEvidenceId": null
}
```

### Response

```json
{
  "success": true,
  "data": {
    "interrogationId": 500,
    "suspectId": 1,
    "suspectName": "박재민",
    "question": "사건 당시 어디에 있었습니까?",
    "answer": "저는 그 시간에 재무팀 자리에서 투자 자료를 정리하고 있었습니다. 데모룸 근처에는 가지 않았습니다.",
    "unlockedEvidences": [],
    "createdAt": "2026-05-15T20:12:00"
  },
  "error": null
}
```

---

## 10.2 증거 제시 심문

```http
POST /api/play-sessions/{sessionId}/interrogations
```

### Request

```json
{
  "suspectId": 1,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "이 카페 결제 내역에 대해 설명해주시겠습니까?",
  "presentedEvidenceId": 3
}
```

### Response

```json
{
  "success": true,
  "data": {
    "interrogationId": 501,
    "suspectId": 1,
    "suspectName": "박재민",
    "question": "이 카페 결제 내역에 대해 설명해주시겠습니까?",
    "answer": "커피를 산 건 맞습니다. 하지만 그건 제가 마시려고 산 것이지 대표님께 드린 건 아닙니다.",
    "unlockedEvidences": [
      {
        "evidenceId": 6,
        "title": "박재민의 법인카드 결제 내역"
      }
    ],
    "createdAt": "2026-05-15T20:14:00"
  },
  "error": null
}
```

---

## 10.3 심문 로그 조회

```http
GET /api/play-sessions/{sessionId}/interrogations
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| suspectId | Long | N | 특정 용의자 기준 로그 필터 |

### Response

```json
{
  "success": true,
  "data": [
    {
      "interrogationId": 500,
      "suspectId": 1,
      "suspectName": "박재민",
      "questionType": "FREE",
      "question": "사건 당시 어디에 있었습니까?",
      "answer": "저는 그 시간에 재무팀 자리에서 투자 자료를 정리하고 있었습니다.",
      "presentedEvidence": null,
      "createdAt": "2026-05-15T20:12:00"
    }
  ],
  "error": null
}
```

---

## 10.4 추천 질문 조회

현재 백엔드는 별도 `recommended-questions` 컨트롤러를 제공하지 않는다.
증거 기반 추천 질문은 증거 상세 API의 `guidance.suggestedQuestions`를 사용한다.

```http
GET /api/play-sessions/{sessionId}/recommended-questions
```

```text
상태:
미구현 / 호출 금지

현재 대체 계약:
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
→ guidance.suggestedQuestions[]
```

`guidance.suggestedQuestions`는 질문 chip prefill 용도다.
자동 제출하지 않으며, 사용자가 전송 버튼을 눌렀을 때만 심문 API를 호출한다.

증거 기반 추천 질문을 전송할 때는 아래 값을 사용한다.

```json
{
  "suspectId": 3,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "이 증거에 대해 설명해주시겠습니까?",
  "presentedEvidenceId": 10
}
```

`QuestionType.RECOMMENDED` enum은 남아 있지만, 현재 evidence guidance chip 경로에서는 사용하지 않는다.

---

# 11. 최종 추리 / 결과 API

## 11.1 최종 추리 제출

```http
POST /api/play-sessions/{sessionId}/final-deduction
```

### Request

`selectedCulpritId`, `motiveText`, `methodText`, `coverUpText`, `selectedEvidenceIds`는 모두 필수다.
텍스트 필드는 공백만 보낼 수 없고, `motiveText`/`methodText`/`coverUpText`는 각각 1000자 이하로 제한한다.
`selectedEvidenceIds`는 1~15개이며, 모두 현재 세션에서 해금된 증거여야 한다.

```json
{
  "selectedCulpritId": 1,
  "motiveText": "회사 자금 유용 사실이 데모데이에서 공개될 것을 막기 위해서입니다.",
  "methodText": "피해자의 알레르기를 이용해 아몬드라떼를 마시게 했고, 에피펜을 숨겨 응급처치를 막았습니다.",
  "coverUpText": "피해자의 휴대폰으로 메시지를 보내 사망 시간을 조작했습니다.",
  "selectedEvidenceIds": [1, 3, 6]
}
```

### Response

```json
{
  "success": true,
  "data": {
    "finalDeductionId": 900,
    "score": 87,
    "grade": "A",
    "feedbackSummary": "범인과 범행 방법을 정확히 파악했습니다.",
    "resultAvailable": true,
    "submittedAt": "2026-05-15T20:40:00"
  },
  "error": null
}
```

---

## 11.2 결과 / 해설 조회

```http
GET /api/play-sessions/{sessionId}/result
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 100,
    "score": 87,
    "grade": "A",
    "correctCulprit": {
      "suspectId": 1,
      "name": "박재민",
      "role": "CFO"
    },
    "matched": {
      "culprit": true,
      "motive": true,
      "method": true,
      "coverUp": false,
      "keyEvidences": 2
    },
    "matchedParts": [
      "범인을 정확히 지목했습니다.",
      "알레르기 음료를 이용한 범행 방법을 맞혔습니다."
    ],
    "missedParts": [
      "사망 이후 메시지를 통한 은폐 방법 설명이 조금 부족했습니다."
    ],
    "feedback": "범인과 범행 방법, 동기를 정확히 파악했습니다. 다만 사망 이후 메시지를 통한 은폐 방법 설명이 조금 부족했습니다.",
    "fullExplanation": "범인은 CFO 박재민입니다. 그는 회사 자금 유용 사실이 공개될 위기에 놓이자...",
    "keyEvidences": [
      {
        "evidenceId": 1,
        "title": "찢긴 컵 라벨"
      },
      {
        "evidenceId": 3,
        "title": "카페 결제 내역"
      }
    ],
    "nextRecommendedScenarios": [
      {
        "scenarioId": 12,
        "title": "단톡방 캡처 유출 사건"
      }
    ]
  },
  "error": null
}
```

---

## 11.3 내 플레이 기록 조회

```http
GET /api/play-sessions/records
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| page | Integer | N | 페이지 |
| size | Integer | N | 크기 |

`PLAYING` 세션은 `IN_PROGRESS`, `COMPLETED` 세션은 `COMPLETED`로 내려간다.
`ABANDONED` 세션은 현재 기록 목록에서 제외한다.
완료 결과의 `score`/`grade`만 포함하고, 범인명, 정오 여부, matched/missed breakdown, AI feedback 원문은 이 목록 API에 포함하지 않는다.

### Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "recordId": "session-100",
        "sessionId": 100,
        "scenarioId": 10,
        "scenarioTitle": "데모데이 전야 살인사건",
        "status": "COMPLETED",
        "score": 87,
        "grade": "A",
        "updatedAt": "2026-05-15T20:40:00",
        "completedAt": "2026-05-15T20:40:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "error": null
}
```

---

# 12. 커뮤니티 API

현재 `develop` 기준 북마크/리뷰 컨트롤러는 구현되어 있다.
웹/앱은 북마크와 리뷰를 서버 계정 기준으로 저장하고, 리뷰 별점은 정수 `1`~`5`만 전송한다.

## 12.1 시나리오 북마크

```http
POST /api/scenarios/{scenarioId}/bookmarks
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 10,
    "bookmarked": true
  },
  "error": null
}
```

---

## 12.2 북마크 취소

```http
DELETE /api/scenarios/{scenarioId}/bookmarks
```

### Response

```json
{
  "success": true,
  "data": {
    "scenarioId": 10,
    "bookmarked": false
  },
  "error": null
}
```

---

## 12.3 리뷰 작성

```http
POST /api/scenarios/{scenarioId}/reviews
```

### Request

```json
{
  "rating": 5,
  "content": "증거 조합이 재밌었고 마지막 반전이 납득됐습니다.",
  "isSpoiler": false
}
```

### Response

```json
{
  "success": true,
  "data": {
    "reviewId": 1
  },
  "error": null
}
```

---

## 12.4 리뷰 목록 조회

```http
GET /api/scenarios/{scenarioId}/reviews
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| includeSpoiler | Boolean | N | 스포일러 리뷰 포함 여부 |
| page | Integer | N | 페이지 |
| size | Integer | N | 크기 |

### Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reviewId": 1,
        "user": {
          "userId": 2,
          "nickname": "추리러버",
          "profileImageUrl": "https://cdn.clueroom.xyz/profiles/2.jpg"
        },
        "rating": 5,
        "content": "증거 조합이 재밌었습니다.",
        "isSpoiler": false,
        "createdAt": "2026-05-15T21:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "error": null
}
```

---

## 12.5 시나리오 신고

```http
POST /api/scenarios/{scenarioId}/reports
```

### Request

```json
{
  "reason": "스포일러가 제목에 포함되어 있습니다.",
  "detail": "시나리오 설명에 범인 이름이 노출되어 있습니다."
}
```

### Response

```json
{
  "success": true,
  "data": {
    "reportId": 1,
    "status": "PENDING"
  },
  "error": null
}
```

---

# 13. Android / Frontend 화면별 API 매핑

화면별 호출 순서와 Android/Frontend 상태 처리는 아래 정본 문서로 분리한다.
이 API Spec은 request/response 계약과 에러 코드만 관리한다.

```text
docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md
```

화면 흐름 표를 이 문서에 다시 복제하지 않는다.
같은 API가 계약 문서와 화면 문서에 서로 다른 상태로 남는 것을 막기 위해서다.

---

# 14. MVP 우선 구현 API

## 14.1 1차 MVP 필수

```text
POST /api/auth/oauth
POST /api/auth/oauth/kakao/code
POST /api/auth/dev
POST /api/auth/refresh
POST /api/auth/logout
GET /api/auth/me
POST /api/device-tokens
GET /api/scenarios
GET /api/scenarios/{scenarioId}
POST /api/play-sessions
GET /api/play-sessions/active
GET /api/play-sessions/{sessionId}/dashboard
GET /api/play-sessions/{sessionId}/locations
GET /api/play-sessions/{sessionId}/evidences
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
GET /api/play-sessions/{sessionId}/suspects
GET /api/play-sessions/{sessionId}/suspects/{suspectId}
GET /api/play-sessions/{sessionId}/timeline
POST /api/play-sessions/{sessionId}/interrogations
GET /api/play-sessions/{sessionId}/interrogations
GET /api/play-sessions/{sessionId}/hints
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
POST /api/play-sessions/{sessionId}/final-deduction
GET /api/play-sessions/{sessionId}/result
POST /api/scenarios
PATCH /api/scenarios/{scenarioId}
POST /api/scenarios/{scenarioId}/locations
POST /api/scenarios/{scenarioId}/victim
POST /api/scenarios/{scenarioId}/suspects
POST /api/scenarios/{scenarioId}/evidences
POST /api/scenarios/{scenarioId}/hints
POST /api/scenarios/{scenarioId}/solution
POST /api/ai/scenarios/{scenarioId}/validate
POST /api/scenarios/{scenarioId}/publish
```

커뮤니티 API는 현재 컨트롤러가 없으므로 1차 MVP 필수 목록에서 제외한다.
`POST /api/notifications/test`는 local/test profile 전용 검증 API라 운영 1차 필수 목록에는 넣지 않는다.
시나리오 상세의 `isBookmarked`, `rating`, `ratingCount` 같은 표시 필드는 응답에 있을 수 있지만, 쓰기 API가 있다는 뜻은 아니다.

---

## 14.2 2차 MVP

```text
POST /api/ai/scenarios/draft
GET /api/scenarios/me
GET /api/scenarios/bookmarked
```

---

# 15. 백엔드 서비스 구조 제안

```text
AuthController
UserController

ScenarioController
ScenarioEditorController
ScenarioCommunityController

PlaySessionController
EvidenceController
SuspectController
InterrogationController
HintController
FinalDeductionController

AiScenarioController
AiValidationController
```

---

## 15.1 Service Layer

```text
AuthService
UserService

ScenarioService
CustomScenarioService
ScenarioAccessService
ScenarioVariantService
ScenarioRankingService

PlaySessionService
InterrogationEvidenceUnlockService
TimeEvidenceUnlockSyncer
AiInterrogationService
ResponsePolicyResolver
HintService
AiDeductionScorer
RuleBasedScorer

AiScenarioValidationService
AiCallRecorder / AiCallLogWriter
```

---

# 16. 핵심 로직 설명

## 16.1 심문 처리 흐름

```text
사용자 질문 입력
  ↓
play_session 조회
  ↓
suspect 조회
  ↓
현재 해금된 evidence 조회
  ↓
presentedEvidenceId 확인
  ↓
ResponsePolicyResolver가 답변 정책 결정
  ↓
AI에게 현재 허용된 정보만 전달
  ↓
AI가 1~2줄 답변 생성
  ↓
interrogation_logs 저장
  ↓
조건 충족 시 추가 evidence 해금
  ↓
응답 반환
```

---

## 16.2 최종 추리 채점 흐름

```text
사용자 최종 추리 제출
  ↓
세션 소유자 확인
  ↓
중복 제출 확인 + final deduction lock 획득
  ↓
세션 시나리오/active variant 확인
  ↓
시간 기반 증거 해금 동기화
  ↓
선택한 용의자가 해당 시나리오 소속인지 확인
  ↓
선택한 증거가 모두 해금됐는지 확인
  ↓
active variant의 VariantSolution 조회
  ↓
variant 정답이 없으면 legacy Solution 조회 경로로 fallback
  ↓
RuleBasedScorer가 범인/동기/방법/은폐/증거 점수 계산
  ↓
힌트 사용 penalty 반영
  ↓
AI feedback 생성, 실패 시 fallback feedback 사용
  ↓
final_deductions와 final_deduction_evidences 저장
  ↓
저장 성공 시 play_session COMPLETED 처리
  ↓
결과 해설 반환
```

---

## 16.3 커스텀 시나리오 검증 흐름

```text
시나리오 작성 완료
  ↓
DRAFT 상태 확인
  ↓
시나리오 검증 lock 획득
  ↓
ScenarioDataReader가 검증용 데이터 로드
  ↓
RuleBasedScenarioValidator가 필수 항목 / 범인 / 증거 / 힌트 / 정책 구조 검사
  ↓
hard blocker가 있으면 AI 호출 없이 rule-only 결과 저장
  ↓
hard blocker가 없으면 AI 검증 호출
  ↓
ScenarioValidationResult 저장
  ↓
validationStatus / validationScore / checkItems 반환
```

---

# 17. 에러 코드 초안

| 코드 | 설명 |
|---|---|
| `USER_NOT_FOUND` | 사용자를 찾을 수 없음 |
| `INVALID_PASSWORD` | 비밀번호 불일치 |
| `SCENARIO_NOT_FOUND` | 시나리오를 찾을 수 없음 |
| `SCENARIO_NOT_PUBLIC` | 공개되지 않은 시나리오 |
| `NO_PERMISSION` | 권한 없음 |
| `PLAY_SESSION_NOT_FOUND` | 플레이 세션 없음 |
| `PLAY_SESSION_ALREADY_COMPLETED` | 이미 완료된 게임 |
| `SUSPECT_NOT_FOUND` | 용의자를 찾을 수 없음 |
| `EVIDENCE_NOT_FOUND` | 증거를 찾을 수 없음 |
| `EVIDENCE_LOCKED` | 아직 해금되지 않은 증거 |
| `EVIDENCE_NOT_UNLOCKABLE` / `P010` | 아직 해금 조건을 충족하지 않은 증거 |
| `SUSPECT_NOT_FOUND` / `P011` | 플레이 세션 시나리오에 속하지 않는 용의자 |
| `INVALID_FILTER_STATUS` / `P012` | 잘못된 상태 필터 값 |
| `HINT_NOT_AVAILABLE` | 아직 사용 불가능한 힌트 |
| `EVIDENCE_USED_IN_POLICY` | 답변 정책에서 참조 중인 증거 |
| `FINAL_DEDUCTION_ALREADY_SUBMITTED` | 이미 최종 추리를 제출함 |
| `AI_REQUEST_FAILED` | AI 요청 실패 |
| `SCENARIO_VALIDATION_FAILED` | 시나리오 검증 실패 |
| `N001` / `FCM_DISABLED` | FCM 비활성 또는 FirebaseApp 미초기화 |
| `N002` / `FCM_SEND_FAILED` | FCM push 발송 실패 |
| `N003` / `DEVICE_TOKEN_SAVE_FAILED` | 디바이스 토큰 저장 실패 |

---

# 18. 보안 / 정책 메모

## 18.1 AI NPC 정답 누설 방지

AI에게 전달하지 말아야 할 정보:

```text
진범 ID
전체 정답
비공개 핵심 증거
아직 해금되지 않은 비밀
최종 해설 전문
```

AI에게 전달할 수 있는 정보:

```text
현재 용의자 공개 프로필
현재 공개된 증거
현재 제시된 증거
현재 답변 정책
질문 내용
답변 길이 제한
```

---

## 18.2 스포일러 리뷰 관리

```text
리뷰 작성 시 스포일러 여부 체크
스포일러 리뷰는 기본 접힘 처리
플레이 완료자만 리뷰 작성 가능
신고 기능 제공
```

---

## 18.3 커스텀 시나리오 공개 조건

```text
범인 존재
용의자 2명 이상
증거 3개 이상
힌트 1개 이상
정답 등록
AI 검증 완료
금칙 소재 없음
```

---

# 19. 향후 확장 API 후보

```text
POST /api/scenarios/{scenarioId}/fork
시나리오 변주 생성

POST /api/scenarios/{scenarioId}/variants
범인 변경 버전 생성

GET /api/rankings/scenarios
시나리오 랭킹 조회

POST /api/play-sessions/{sessionId}/invite
친구 초대 협력 모드

GET /api/play-sessions/{sessionId}/shared
협력 세션 공유 상태 조회

GET /api/wallet
내 크레딧 지갑 조회

POST /api/wallet/charge-mock
Mock 크레딧 충전

POST /api/scenarios/{scenarioId}/purchase
시나리오 구매/언락

GET /api/users/me/purchases
내 구매 시나리오 조회

GET /api/scenarios/{scenarioId}/access
시나리오 접근 권한 확인

POST /api/scenarios/{scenarioId}/refund
시나리오 구매 환불
```

---

## 19.1 거래/크레딧 API 계획

거래/크레딧 API는 1차 MVP 범위가 아니다.
인증/인가 도입 후 Mock 크레딧 기반으로 추가한다.

### 지갑 조회

```http
GET /api/wallet
```

Response:

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "balance": 1200
  },
  "error": null
}
```

### Mock 크레딧 충전

```http
POST /api/wallet/charge-mock
```

Request:

```json
{
  "amount": 1000
}
```

Response:

```json
{
  "success": true,
  "data": {
    "transactionId": 10,
    "balance": 2000
  },
  "error": null
}
```

### 시나리오 구매

```http
POST /api/scenarios/{scenarioId}/purchase
```

정책:

```text
이미 구매한 경우 실패 또는 기존 Access 반환
잔액 부족 시 실패
무료 시나리오는 구매 없이 접근 가능
작성자는 자신의 시나리오를 구매하지 않음
구매 성공 시 ScenarioPurchase, CreditTransaction, ScenarioAccess를 함께 기록
```

Response:

```json
{
  "success": true,
  "data": {
    "purchaseId": 5,
    "scenarioId": 12,
    "priceCredit": 300,
    "balanceAfter": 700,
    "accessGranted": true
  },
  "error": null
}
```

### 구매 내역 조회

```http
GET /api/users/me/purchases
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "purchaseId": 5,
      "scenarioId": 12,
      "scenarioTitle": "데모데이 전야 살인사건",
      "priceCredit": 300,
      "purchaseStatus": "COMPLETED",
      "purchasedAt": "2026-05-18T12:00:00"
    }
  ],
  "error": null
}
```

### 접근 권한 확인

```http
GET /api/scenarios/{scenarioId}/access
```

Response:

```json
{
  "success": true,
  "data": {
    "scenarioId": 12,
    "canPlay": true,
    "accessType": "PURCHASED"
  },
  "error": null
}
```

### 환불

```http
POST /api/scenarios/{scenarioId}/refund
```

초기 정책:

```text
아직 플레이를 시작하지 않은 구매 건만 환불 가능
플레이 세션이 생성되었으면 환불 불가
환불 시 ScenarioAccess 비활성화
CreditTransaction REFUND 생성
ScenarioPurchase 상태 REFUNDED 변경
```

Response:

```json
{
  "success": true,
  "data": {
    "scenarioId": 12,
    "purchaseId": 5,
    "refundTransactionId": 11,
    "refundCredit": 300,
    "purchaseStatus": "REFUNDED",
    "accessRevoked": true
  },
  "error": null
}
```

거래 동시성 방어:

```text
scenario_purchases에 user_id + scenario_id Unique 제약
wallet 차감은 하나의 Transaction 안에서 처리
잔액 검증 후 차감
중복 요청 방지를 위해 idempotency key 검토
필요 시 wallet:user:{userId}, purchase:scenario:{scenarioId}:user:{userId} Redis Lock 적용
```

---

# 20. 최종 정리

이 API 설계의 핵심은 다음이다.

```text
1. Android 앱은 탐정 플레이 경험에 집중한다.
2. 백엔드는 시나리오, 증거, 용의자, 플레이 상태를 관리한다.
3. AI는 사건 생성, 시나리오 검증, 용의자 심문, 최종 채점에 사용한다.
4. AI에게 전체 정답을 넘기지 않고, 현재 허용된 정보만 전달한다.
5. 공식 시나리오와 커스텀 시나리오를 모두 지원한다.
6. 커스텀 시나리오 공유 플랫폼 구조를 가진다.
7. 후속 단계에서 Mock 크레딧 기반 구매/언락 거래 흐름을 붙인다.
```

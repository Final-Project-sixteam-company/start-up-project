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
http://localhost:8080/api
```

배포 환경 예시:

```text
https://api.caselab.ai/api
```

### 1.3 공통 Header

```http
Content-Type: application/json
Authorization: Bearer {accessToken}
```

인증이 필요 없는 API는 `Authorization` 생략 가능.

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
EXPERT
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

---

# 4. API 전체 테이블

## 4.1 인증 / 유저 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/auth/signup` | 회원가입 | X | O |
| 2 | POST | `/auth/login` | 로그인 | X | O |
| 3 | POST | `/auth/logout` | 로그아웃 | O | △ |
| 4 | POST | `/auth/refresh` | Access Token 재발급 | X/Refresh | △ |
| 5 | GET | `/users/me` | 내 정보 조회 | O | O |
| 6 | PATCH | `/users/me` | 내 정보 수정 | O | △ |

---

## 4.2 시나리오 라이브러리 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | GET | `/scenarios` | 시나리오 목록 조회 | 선택 | O |
| 2 | GET | `/scenarios/{scenarioId}` | 시나리오 상세 조회 | 선택 | O |
| 3 | POST | `/scenarios` | 커스텀 시나리오 생성 | O | O |
| 4 | PATCH | `/scenarios/{scenarioId}` | 시나리오 기본 정보 수정 | O | O |
| 5 | DELETE | `/scenarios/{scenarioId}` | 시나리오 삭제 | O | △ |
| 6 | POST | `/scenarios/{scenarioId}/publish` | 시나리오 공개 등록 | O | O |
| 7 | POST | `/scenarios/{scenarioId}/hide` | 시나리오 비공개/숨김 | O | △ |
| 8 | GET | `/scenarios/me` | 내가 만든 시나리오 조회 | O | O |
| 9 | GET | `/scenarios/bookmarked` | 북마크한 시나리오 조회 | O | △ |

---

## 4.3 시나리오 제작 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/scenarios/{scenarioId}/locations` | 장소 등록 | O | O |
| 2 | GET | `/scenarios/{scenarioId}/locations` | 장소 목록 조회 | O | O |
| 3 | POST | `/scenarios/{scenarioId}/victim` | 피해자 정보 등록/수정 | O | O |
| 4 | GET | `/scenarios/{scenarioId}/victim` | 피해자 정보 조회 | O | O |
| 5 | POST | `/scenarios/{scenarioId}/suspects` | 용의자 등록 | O | O |
| 6 | GET | `/scenarios/{scenarioId}/suspects` | 용의자 목록 조회 | O | O |
| 7 | PATCH | `/suspects/{suspectId}` | 용의자 수정 | O | O |
| 8 | DELETE | `/suspects/{suspectId}` | 용의자 삭제 | O | △ |
| 9 | POST | `/scenarios/{scenarioId}/evidences` | 증거 등록 | O | O |
| 10 | GET | `/scenarios/{scenarioId}/evidences` | 증거 목록 조회 | O | O |
| 11 | PATCH | `/evidences/{evidenceId}` | 증거 수정 | O | O |
| 12 | DELETE | `/evidences/{evidenceId}` | 증거 삭제 | O | △ |
| 13 | POST | `/scenarios/{scenarioId}/hints` | 힌트 등록 | O | O |
| 14 | GET | `/scenarios/{scenarioId}/hints` | 힌트 목록 조회 | O | O |
| 15 | POST | `/scenarios/{scenarioId}/solution` | 정답 등록/수정 | O | O |
| 16 | GET | `/scenarios/{scenarioId}/solution` | 정답 조회 | O/작성자 | O |

---

## 4.4 AI 시나리오 생성 / 검증 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/ai/scenarios/draft` | AI 시나리오 초안 생성 | O | O |
| 2 | POST | `/ai/scenarios/{scenarioId}/validate` | 시나리오 논리 검증 | O | O |
| 3 | GET | `/scenarios/{scenarioId}/validation-result` | 검증 결과 조회 | O | O |
| 4 | GET | `/ai/logs` | 내 AI 요청 로그 조회 | O | △ |

---

## 4.5 게임 플레이 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/play-sessions` | 게임 세션 시작 | O | O |
| 2 | GET | `/play-sessions/{sessionId}` | 게임 세션 상세 조회 | O | O |
| 3 | GET | `/play-sessions/{sessionId}/dashboard` | 탐정 대시보드 조회 | O | O |
| 4 | GET | `/play-sessions/{sessionId}/locations` | 현장 정보 조회 | O | O |
| 5 | GET | `/play-sessions/{sessionId}/evidences` | 현재 해금된 증거 조회 | O | O |
| 6 | GET | `/play-sessions/{sessionId}/evidences/{evidenceId}` | 증거 상세 조회 | O | O |
| 7 | POST | `/play-sessions/{sessionId}/evidences/{evidenceId}/unlock` | 증거 수동/조건 해금 | O | O |
| 8 | GET | `/play-sessions/{sessionId}/suspects` | 용의자 목록 조회 | O | O |
| 9 | GET | `/play-sessions/{sessionId}/suspects/{suspectId}` | 용의자 상세 조회 | O | O |
| 10 | GET | `/play-sessions/{sessionId}/timeline` | 타임라인 조회 | O | O |
| 11 | GET | `/play-sessions/{sessionId}/hints` | 사용 가능 힌트 조회 | O | O |
| 12 | POST | `/play-sessions/{sessionId}/hints/{hintId}/use` | 힌트 사용 | O | O |
| 13 | POST | `/play-sessions/{sessionId}/abandon` | 게임 포기/중단 | O | △ |

---

## 4.6 심문 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/play-sessions/{sessionId}/interrogations` | AI 용의자 심문 | O | O |
| 2 | GET | `/play-sessions/{sessionId}/interrogations` | 심문 로그 조회 | O | O |
| 3 | GET | `/play-sessions/{sessionId}/suspects/{suspectId}/interrogations` | 특정 용의자 심문 로그 조회 | O | △ |
| 4 | GET | `/play-sessions/{sessionId}/recommended-questions` | 추천 질문 조회 | O | △ |

---

## 4.7 최종 추리 / 결과 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/play-sessions/{sessionId}/final-deduction` | 최종 추리 제출 | O | O |
| 2 | GET | `/play-sessions/{sessionId}/result` | 결과/해설 조회 | O | O |
| 3 | GET | `/play-sessions/me` | 내 플레이 기록 조회 | O | O |

---

## 4.8 커뮤니티 API

| No | Method | Endpoint | 설명 | 인증 | MVP |
|---:|---|---|---|---|---|
| 1 | POST | `/scenarios/{scenarioId}/bookmarks` | 시나리오 북마크 | O | O |
| 2 | DELETE | `/scenarios/{scenarioId}/bookmarks` | 북마크 취소 | O | O |
| 3 | POST | `/scenarios/{scenarioId}/reviews` | 리뷰 작성 | O | O |
| 4 | GET | `/scenarios/{scenarioId}/reviews` | 리뷰 목록 조회 | 선택 | O |
| 5 | PATCH | `/reviews/{reviewId}` | 리뷰 수정 | O | △ |
| 6 | DELETE | `/reviews/{reviewId}` | 리뷰 삭제 | O | △ |
| 7 | POST | `/scenarios/{scenarioId}/reports` | 시나리오 신고 | O | △ |

---

# 5. 상세 API 명세

---

## 5.1 회원가입

```http
POST /api/auth/signup
```

### Request

```json
{
  "email": "user@example.com",
  "password": "password1234",
  "nickname": "탐정순구"
}
```

### Response

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "탐정순구"
  },
  "error": null
}
```

---

## 5.2 로그인

```http
POST /api/auth/login
```

### Request

```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

### Response

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "탐정순구"
    }
  },
  "error": null
}
```

---

## 5.3 내 정보 조회

```http
GET /api/users/me
```

### Response

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "탐정순구",
    "profileImageUrl": null,
    "role": "USER"
  },
  "error": null
}
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
| difficulty | String | N | EASY, NORMAL, HARD, EXPERT |
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
        "id": 10,
        "title": "데모데이 전야 살인사건",
        "description": "AI 스타트업 대표가 데모데이 전날 사망한 사건",
        "thumbnailUrl": "https://example.com/thumb.png",
        "scenarioType": "OFFICIAL",
        "difficulty": "NORMAL",
        "estimatedPlayTimeMinutes": 30,
        "playerCountMin": 1,
        "playerCountMax": 1,
        "suspectCount": 5,
        "evidenceCount": 15,
        "playCount": 1234,
        "averageRating": 4.7,
        "isBookmarked": false
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

## 6.2 시나리오 상세 조회

```http
GET /api/scenarios/{scenarioId}
```

### Response

```json
{
  "success": true,
  "data": {
    "id": 10,
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
    "tags": ["스타트업", "살인", "디지털증거", "보통"],
    "creator": {
      "id": 1,
      "nickname": "운영자"
    },
    "isBookmarked": false
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
  "mapX": 120,
  "mapY": 80,
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
      "id": 1,
      "name": "데모룸",
      "description": "피해자가 발견된 장소",
      "mapX": 120,
      "mapY": 80,
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
      "id": 1,
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
      "id": 1,
      "title": "찢긴 컵 라벨",
      "description": "쓰레기통에서 발견된 컵 라벨 조각",
      "locationName": "데모룸",
      "importance": "CORE",
      "isInitialPublic": true,
      "unlockType": "NONE",
      "relatedSuspects": [
        {
          "id": 1,
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

## 8.1 AI 시나리오 초안 생성

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
    "scenario": {
      "id": 10,
      "title": "데모데이 전야 살인사건",
      "difficulty": "NORMAL",
      "estimatedPlayTimeMinutes": 30
    },
    "elapsedSeconds": 320,
    "unlockedEvidenceCount": 5,
    "totalEvidenceCount": 15,
    "hintCount": 0,
    "interrogationCount": 3,
    "status": "PLAYING"
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
  "data": [
    {
      "id": 1,
      "name": "데모룸",
      "description": "피해자가 발견된 장소",
      "mapX": 120,
      "mapY": 80,
      "evidences": [
        {
          "id": 1,
          "title": "찢긴 컵 라벨",
          "isUnlocked": true
        }
      ]
    }
  ],
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

### Response

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "찢긴 컵 라벨",
      "description": "라벨 조각에는 '...MOND LAT...'라는 글자가 남아 있다.",
      "locationName": "데모룸",
      "importance": "CORE",
      "isUnlocked": true,
      "relatedSuspects": [
        {
          "id": 1,
          "name": "박재민"
        }
      ]
    },
    {
      "id": 2,
      "title": "휴대폰 위치 기록",
      "description": null,
      "locationName": null,
      "importance": "HIGH",
      "isUnlocked": false,
      "unlockHint": "15분 후 공개"
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

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "찢긴 컵 라벨",
    "description": "쓰레기통에서 발견된 컵 라벨 조각에는 '...MOND LAT...'라는 글자가 남아 있다.",
    "location": {
      "id": 1,
      "name": "데모룸"
    },
    "importance": "CORE",
    "relatedSuspects": [
      {
        "id": 1,
        "name": "박재민"
      }
    ],
    "relatedTimelineEvents": [
      {
        "time": "22:16",
        "title": "음료 컵이 데모룸 앞에 놓임"
      }
    ]
  },
  "error": null
}
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
      "id": 1,
      "name": "박재민",
      "role": "CFO",
      "relationToVictim": "공동창업자",
      "publicStatement": "재무팀 자리에서 투자 자료를 정리하고 있었다.",
      "alibi": "22시 이후 데모룸 근처에 가지 않았다고 주장한다.",
      "suspicionLevel": 60,
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

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "박재민",
    "role": "CFO",
    "relationToVictim": "공동창업자",
    "publicProfile": "회사 재무를 담당하는 인물",
    "publicStatement": "사건 당시 재무팀 자리에서 투자자료를 정리하고 있었다.",
    "alibi": "데모룸 근처에는 가지 않았다고 주장한다.",
    "relatedEvidences": [
      {
        "id": 1,
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
      "isTrueEvent": true,
      "relatedEvidenceId": 3
    },
    {
      "time": "22:36",
      "title": "피해자 계정 메시지 전송",
      "description": "피해자 계정으로 단톡방 메시지가 전송됨",
      "eventType": "FACT",
      "isTrueEvent": true,
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
      "id": 1,
      "hintLevel": 1,
      "content": null,
      "isAvailable": true,
      "isUsed": false,
      "penaltyScore": 5
    },
    {
      "id": 2,
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
        "id": 6,
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

### Response

```json
{
  "success": true,
  "data": [
    {
      "id": 500,
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

```http
GET /api/play-sessions/{sessionId}/recommended-questions
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| suspectId | Long | N | 특정 용의자 기준 추천 질문 |
| evidenceId | Long | N | 특정 증거 기준 추천 질문 |

### Response

```json
{
  "success": true,
  "data": [
    {
      "question": "사건 당시 어디에 있었습니까?",
      "questionType": "RECOMMENDED"
    },
    {
      "question": "피해자와 마지막으로 대화한 것은 언제입니까?",
      "questionType": "RECOMMENDED"
    },
    {
      "question": "이 증거에 대해 설명해주시겠습니까?",
      "questionType": "EVIDENCE_PRESENTED"
    }
  ],
  "error": null
}
```

---

# 11. 최종 추리 / 결과 API

## 11.1 최종 추리 제출

```http
POST /api/play-sessions/{sessionId}/final-deduction
```

### Request

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
    "score": 87,
    "grade": "A",
    "correctCulprit": {
      "id": 1,
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
    "feedback": "범인과 범행 방법, 동기를 정확히 파악했습니다. 다만 사망 이후 메시지를 통한 은폐 방법 설명이 조금 부족했습니다.",
    "fullExplanation": "범인은 CFO 박재민입니다. 그는 회사 자금 유용 사실이 공개될 위기에 놓이자...",
    "nextRecommendations": [
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
GET /api/play-sessions/me
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | String | N | PLAYING, COMPLETED, ABANDONED |
| page | Integer | N | 페이지 |
| size | Integer | N | 크기 |

### Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "sessionId": 100,
        "scenarioId": 10,
        "scenarioTitle": "데모데이 전야 살인사건",
        "status": "COMPLETED",
        "score": 87,
        "grade": "A",
        "startedAt": "2026-05-15T20:00:00",
        "endedAt": "2026-05-15T20:40:00"
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
        "id": 1,
        "user": {
          "id": 2,
          "nickname": "추리러버"
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

# 13. Android 화면별 API 매핑

| Android 화면 | 주요 API |
|---|---|
| 스플래시 / 온보딩 | 없음 |
| 로그인 | `POST /auth/login` |
| 회원가입 | `POST /auth/signup` |
| 홈 | `GET /scenarios?sort=popular`, `GET /play-sessions/me` |
| 시나리오 라이브러리 | `GET /scenarios` |
| 시나리오 상세 | `GET /scenarios/{scenarioId}`, `GET /scenarios/{scenarioId}/reviews` |
| 사건 시작 | `POST /play-sessions` |
| 사건 브리핑 | `GET /play-sessions/{sessionId}` |
| 탐정 대시보드 | `GET /play-sessions/{sessionId}/dashboard` |
| 현장 정보 | `GET /play-sessions/{sessionId}/locations` |
| 증거 보드 | `GET /play-sessions/{sessionId}/evidences` |
| 증거 상세 | `GET /play-sessions/{sessionId}/evidences/{evidenceId}` |
| 용의자 목록 | `GET /play-sessions/{sessionId}/suspects` |
| 용의자 상세 | `GET /play-sessions/{sessionId}/suspects/{suspectId}` |
| 심문 채팅 | `POST /play-sessions/{sessionId}/interrogations` |
| 심문 로그 | `GET /play-sessions/{sessionId}/interrogations` |
| 타임라인 | `GET /play-sessions/{sessionId}/timeline` |
| 힌트 | `GET /play-sessions/{sessionId}/hints`, `POST /play-sessions/{sessionId}/hints/{hintId}/use` |
| 최종 추리 제출 | `POST /play-sessions/{sessionId}/final-deduction` |
| 결과 / 해설 | `GET /play-sessions/{sessionId}/result` |
| 커스텀 제작 | `POST /scenarios`, `POST /scenarios/{id}/suspects`, `POST /scenarios/{id}/evidences` |
| AI 초안 생성 | `POST /ai/scenarios/draft` |
| AI 검증 | `POST /ai/scenarios/{scenarioId}/validate` |
| 내 기록 | `GET /play-sessions/me`, `GET /scenarios/me`, `GET /scenarios/bookmarked` |
| 리뷰 작성 | `POST /scenarios/{scenarioId}/reviews` |
| 북마크 | `POST /scenarios/{scenarioId}/bookmarks` |

---

# 14. MVP 우선 구현 API

## 14.1 1차 MVP 필수

```text
POST /auth/signup
POST /auth/login
GET /users/me

GET /scenarios
GET /scenarios/{scenarioId}
POST /play-sessions
GET /play-sessions/{sessionId}/dashboard
GET /play-sessions/{sessionId}/locations
GET /play-sessions/{sessionId}/evidences
GET /play-sessions/{sessionId}/suspects
POST /play-sessions/{sessionId}/interrogations
GET /play-sessions/{sessionId}/hints
POST /play-sessions/{sessionId}/hints/{hintId}/use
POST /play-sessions/{sessionId}/final-deduction
GET /play-sessions/{sessionId}/result
```

---

## 14.2 2차 MVP

```text
POST /scenarios
PATCH /scenarios/{scenarioId}
POST /scenarios/{scenarioId}/locations
POST /scenarios/{scenarioId}/victim
POST /scenarios/{scenarioId}/suspects
POST /scenarios/{scenarioId}/evidences
POST /scenarios/{scenarioId}/hints
POST /scenarios/{scenarioId}/solution
POST /ai/scenarios/draft
POST /ai/scenarios/{scenarioId}/validate
POST /scenarios/{scenarioId}/publish
POST /scenarios/{scenarioId}/reviews
POST /scenarios/{scenarioId}/bookmarks
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
ScenarioEditorService
ScenarioValidationService
ScenarioRankingService

PlaySessionService
EvidenceUnlockService
InterrogationService
NpcResponsePolicyService
HintService
FinalDeductionService
ScoringService

AiGenerationService
AiValidationService
AiInterrogationService
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
NpcResponsePolicyService가 답변 정책 결정
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
solution 조회
  ↓
범인 일치 여부 확인
  ↓
동기 / 방법 / 은폐 텍스트 AI 또는 규칙 기반 평가
  ↓
결정적 증거 선택 여부 확인
  ↓
점수 계산
  ↓
final_deductions 저장
  ↓
play_session COMPLETED 처리
  ↓
결과 해설 반환
```

---

## 16.3 커스텀 시나리오 검증 흐름

```text
시나리오 작성 완료
  ↓
기본 필수 항목 검사
  ↓
범인 설정 여부 검사
  ↓
증거 / 힌트 / 용의자 존재 여부 검사
  ↓
AI 검증 요청
  ↓
논리적 문제와 보완 제안 저장
  ↓
검증 통과 시 공개 가능
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
| `HINT_NOT_AVAILABLE` | 아직 사용 불가능한 힌트 |
| `FINAL_DEDUCTION_ALREADY_SUBMITTED` | 이미 최종 추리를 제출함 |
| `AI_REQUEST_FAILED` | AI 요청 실패 |
| `SCENARIO_VALIDATION_FAILED` | 시나리오 검증 실패 |

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
POST /scenarios/{scenarioId}/fork
시나리오 변주 생성

POST /scenarios/{scenarioId}/variants
범인 변경 버전 생성

GET /rankings/scenarios
시나리오 랭킹 조회

POST /play-sessions/{sessionId}/invite
친구 초대 협력 모드

GET /play-sessions/{sessionId}/shared
협력 세션 공유 상태 조회

POST /payments/credits
AI 생성 크레딧 충전

GET /users/me/credits
내 크레딧 조회
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
```

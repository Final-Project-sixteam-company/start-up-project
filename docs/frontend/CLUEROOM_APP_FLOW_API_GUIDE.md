# ClueRoom App Flow API Guide

> 작성 범위: 현재 백엔드 구현 기준 API 표준화, Android/Frontend 화면 구조, 게임 시작 전 사용자 흐름, 게임 내부 탭 흐름, 심문/증거 제시 흐름, 최종 추리/결과 흐름, 로딩/오류/미구현 기능 처리
>
> 이 문서는 Android/Frontend가 "어떤 화면에서 어떤 API를 호출해야 하는지"를 실제 백엔드 구현 기준으로 정리한다.
> `docs/ANDROID_SCREEN_API_MAPPING.md`의 고유 내용은 이 문서로 흡수됐고, 원문은 Git history에서 확인한다.

---

## 1. 문서 기준

### 1.1 목적

프론트 구현 중 다음 혼선을 줄이는 것이 목적이다.

```text
기획 문서에는 있지만 아직 백엔드에 없는 API를 호출하는 문제
현재 구현 API를 모르고 더미 데이터로 화면을 유지하는 문제
화면 상세/상태 갱신을 어떤 API 기준으로 해야 하는지 불명확한 문제
```

### 1.2 참조 기준

현재 API 기준은 컨트롤러 구현을 우선한다.

```text
src/main/java/com/startup/domain/scenario/controller/ScenarioController.java
src/main/java/com/startup/domain/play/controller/PlaySessionController.java
src/main/java/com/startup/domain/ai/controller/AiInterrogationController.java
src/main/java/com/startup/domain/ai/controller/AiDeductionController.java
```

기존 문서는 아래처럼 사용한다.

| 문서 | 사용 방식 |
|---|---|
| `docs/ANDROID_SCREEN_API_MAPPING.md` | 흡수 완료 후 제거. 원문 확인은 Git history 사용 |
| `docs/CaseLab_AI_API_Spec.md` | 계획 API와 DTO 설계 참고 |
| `docs/CaseLab_AI_PRD.md` | 사용자 진행 흐름과 MVP 기능 참고 |
| `docs/scenarios/SCENARIO_YAML_SCHEMA.md` | 공식 시나리오 표시 필드 기준 |

### 1.3 공개 문서 원칙

이 문서는 public repo에 둘 수 있는 프론트 연동 문서다.

따라서 아래 정보는 넣지 않는다.

```text
범인
정답 Variant
정답 동기/수법/은폐 전문
시나리오 내부 비밀
AI NPC에게 전달하면 안 되는 백엔드 secret
```

---

## 2. 공통 API 규칙

### 2.1 Base URL

| 환경 | Base URL |
|---|---|
| 로컬 Android Emulator | `http://10.0.2.2:8080` |
| 운영 | `https://api.clueroom.xyz` |

Base URL에는 `/api`를 붙이지 않는다.
각 API path가 `/api/...`를 포함한다.

### 2.2 공통 응답 래퍼

성공 응답은 공통적으로 아래 구조를 사용한다.

```json
{
  "success": true,
  "data": {}
}
```

프론트는 실제 화면 데이터를 `data` 아래에서 읽는다.
`error`는 실패 응답에서만 내려오며, 성공 응답에서는 생략될 수 있다.

페이지 응답은 `data.content`에 목록이 들어간다.

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalPages": 1,
    "totalElements": 0,
    "hasNext": false
  }
}
```

### 2.3 인증 상태

현재 MVP는 로그인 없이 Mock user 기준으로 호출 가능하다.

```text
Authorization Header 없이 호출 가능
백엔드는 MockUserProvider.currentUserId() 기준으로 사용자 ID를 결정
```

JWT 인증이 붙으면 `Authorization: Bearer {accessToken}`을 추가한다.

### 2.4 ID 필드명 규칙

API DTO의 ID 필드명은 `CaseLab_AI_API_Spec.md`를 따른다.
엔티티 내부 PK가 `id`여도 Android 응답 DTO에서는 아래 이름을 우선 사용한다.

| 대상 | API DTO 필드명 |
|---|---|
| User | `userId` |
| Scenario | `scenarioId` |
| PlaySession | `sessionId` |
| Suspect | `suspectId` |
| Evidence | `evidenceId` |
| Hint | `hintId` |
| Review | `reviewId` |

프론트 모델에서 `id` 별칭을 추가할 수는 있지만, API boundary와 navigation argument에서는 위 필드명을 유지한다.

### 2.5 Android 화면 구조

Bottom Navigation 기본 구조:

```text
홈
시나리오
제작
내 기록
마이페이지
```

`내 기록`, `마이페이지`는 인증 도입 후 완성한다.
MVP에서는 mock 또는 비활성 상태로 둘 수 있다.

플레이 세션이 시작된 뒤 게임 내부 탭은 아래 구조를 기준으로 한다.

```text
현장
증거
용의자
타임라인
추리 제출
```

탭별 API 기준은 7절을 따른다.

---

## 3. 현재 구현된 플레이 플로우 API

### 3.1 시나리오 목록

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/scenarios` |
| 사용 화면 | 홈, 사건 라이브러리 |
| 호출 시점 | 앱 진입 후 사건 목록 표시, 검색/필터 변경 |
| Query | `keyword`, `type`, `difficulty`, `visibility`, `minPlayers`, `maxPlayers`, `maxPlayTime`, `sort`, `page`, `size` |
| 응답 핵심 | `data.content[].scenarioId`, `title`, `description`, `thumbnailUrl`, `scenarioType`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `playCount`, `averageRating`, `isBookmarked` |

`thumbnailUrl`은 시나리오 `coverAssetKey`와 서버의 `AWS_S3_PUBLIC_BASE_URL` 설정으로 생성된다.
서버에 이미지 base URL이 설정되지 않았거나 asset key가 없으면 `null`일 수 있으므로, 프론트는 placeholder를 준비한다.

### 3.2 시나리오 상세

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/scenarios/{scenarioId}` |
| 사용 화면 | 사건 상세, 사건 브리핑 |
| 호출 시점 | 라이브러리 카드 선택 후 상세 진입 |
| 응답 핵심 | `scenarioId`, `title`, `description`, `synopsis`, `coverImageUrl`, `mapImageUrl`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `hintCount`, `canPlay` |

표시 기준은 아래처럼 나눈다.

| 필드 | 화면 사용 |
|---|---|
| `title` | 카드, 상세, 브리핑 제목 |
| `description` | 카드/상세의 짧은 사건 설명 |
| `synopsis` | 사건 상세 또는 브리핑의 오프닝 본문 |
| `coverImageUrl` | 사건 상세/브리핑 대표 이미지 |
| `mapImageUrl` | 게임 진입 전후 전체 지도/평면도 이미지 |

공식 시나리오의 오프닝은 `synopsis`를 우선 사용한다.

### 3.3 플레이 세션 시작

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/play-sessions` |
| 사용 화면 | 사건 상세 또는 사건 브리핑의 `수사 시작` 버튼 |
| 호출 시점 | 사용자가 실제 게임 시작을 확정할 때 |
| Request | `{ "scenarioId": 1 }` |
| 응답 핵심 | `sessionId`, `scenarioId`, `status`, `startedAt` |

프론트는 응답의 `sessionId`를 게임 진행 동안 보관해야 한다.
이후 게임 내부 API는 모두 이 `sessionId`를 path parameter로 사용한다.

### 3.4 탐정 대시보드

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/dashboard` |
| 사용 화면 | 게임 메인 허브, 상단 상태 요약 |
| 호출 시점 | 세션 시작 직후, 증거 해금/심문/힌트 사용 후 상태 갱신 |
| 응답 핵심 | `sessionId`, `scenarioId`, `scenarioTitle`, `status`, `elapsedSeconds`, `unlockedEvidenceCount`, `totalEvidenceCount`, `hintUsedCount`, `interrogationCount`, `briefing` |

`briefing`에는 피해자/발견 장소/요약 표시용 데이터가 들어간다.
긴 오프닝 문장은 시나리오 상세의 `synopsis`를 사용한다.

### 3.5 증거 목록

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/evidences` |
| 사용 화면 | 증거 보드, 증거 상세, 심문 중 증거 제시 모달 |
| 호출 시점 | 게임 탭 진입, 심문 후 새 증거 해금 시, 필터 변경 |
| Query | `includeLocked`, `status` |
| 응답 핵심 | `evidenceId`, `title`, `oneLine`, `description`, `imageAssetKey`, `imageUrl`, `locationName`, `importance`, `isUnlocked`, `unlockHint`, `relatedSuspects` |

증거 단건 상세는 아래 API를 사용한다.

```text
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

증거 상세 응답에는 목록보다 자세한 연관 정보와 optional `guidance`가 포함될 수 있다.

```text
guidance.readingPoints
guidance.compareEvidences
guidance.suggestedQuestions
```

`guidance`는 현재 해금되어 상세 조회 가능한 증거에서만 사용한다.
잠긴 비교 증거는 `title`, `isUnlocked`, `unlockHint` 수준으로만 표시하고,
`evidenceCode`는 기대하지 않는다.
현재 응답에서 `evidenceId`가 함께 내려올 수 있지만, `isUnlocked=false`이면 상세 이동에 사용하지 않는다.

이미지 관련 필드는 아래처럼 처리한다.

| 필드 | 현재 처리 |
|---|---|
| `imageAssetKey` | YAML/S3 기준 asset key. 현재 응답 가능 |
| `imageUrl` | 해금 증거의 표시 이미지 URL. `AWS_S3_PUBLIC_BASE_URL` 미설정 또는 asset key 없음이면 `null` |

### 3.6 용의자 목록

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/suspects` |
| 사용 화면 | 용의자 목록, 용의자 상세, 심문 진입 |
| 호출 시점 | 용의자 탭 진입, 심문 후 interrogation count 갱신 |
| 응답 핵심 | `suspectId`, `name`, `role`, `relationToVictim`, `publicStatement`, `alibi`, `portraitImageUrl`, `suspicionLevel`, `interrogationCount` |

용의자 단건 상세는 아래 API를 사용한다.

```text
GET /api/play-sessions/{sessionId}/suspects/{suspectId}
```

### 3.7 심문 로그 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/interrogations` |
| 사용 화면 | 심문 채팅 화면 |
| 호출 시점 | 심문 화면 진입, 심문 제출 후 로그 재조회 |
| Query | `suspectId` optional |
| 응답 핵심 | `interrogationId`, `suspectId`, `suspectName`, `questionType`, `question`, `answer`, `presentedEvidence`, `createdAt` |

특정 용의자 채팅방처럼 보여줄 때는 `suspectId` query를 붙인다.

### 3.8 AI 용의자 심문

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/play-sessions/{sessionId}/interrogations` |
| 사용 화면 | 심문 채팅 화면 |
| 호출 시점 | 사용자가 질문 전송 버튼을 누를 때 |
| Request | `suspectId`, `questionType`, `question`, `presentedEvidenceId` |
| 응답 핵심 | `interrogationId`, `suspectId`, `suspectName`, `question`, `answer`, `unlockedEvidences`, `createdAt` |

`questionType`은 현재 아래 값을 사용한다.

```text
FREE
RECOMMENDED
EVIDENCE_PRESENTED
```

`RECOMMENDED` enum은 호환성상 남아 있지만, 증거 상세 `guidance.suggestedQuestions` chip은 `EVIDENCE_PRESENTED`로 prefill한다.
증거 제시 질문일 때만 `presentedEvidenceId`를 넣는다.
현재 구현에서는 `unlockedEvidences`가 비어 있을 수 있으므로, 심문 성공 후에는 증거 목록과 대시보드를 다시 조회한다.

### 3.9 힌트 목록

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/hints` |
| 사용 화면 | 힌트 화면 또는 게임 내부 도움말 |
| 호출 시점 | 힌트 화면 진입, 시간 경과 후 갱신 |
| 응답 핵심 | `hintId`, `hintLevel`, `content`, `isAvailable`, `isUsed`, `remainingMinutes`, `penaltyScore` |

힌트 내용 `content`는 사용 전에는 `null`일 수 있다.
사용자는 `isAvailable=true`인 힌트만 열람 요청할 수 있다.

### 3.10 힌트 사용

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/play-sessions/{sessionId}/hints/{hintId}/use` |
| 사용 화면 | 힌트 화면 |
| 호출 시점 | 사용자가 힌트 열람을 확정할 때 |
| 응답 핵심 | `hintId`, `content`, `penaltyScore`, `usedAt` |

힌트 사용 후 대시보드를 재조회해 `hintUsedCount`를 갱신한다.

### 3.11 최종 추리 제출

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/play-sessions/{sessionId}/final-deduction` |
| 사용 화면 | 최종 추리 제출 화면 |
| 호출 시점 | 사용자가 최종 제출 버튼을 누를 때 |
| Request | `selectedCulpritId`, `motiveText`, `methodText`, `coverUpText`, `selectedEvidenceIds` |
| 응답 핵심 | `finalDeductionId`, `score`, `grade`, `feedbackSummary`, `resultAvailable`, `submittedAt` |

프론트 검증 기준은 아래와 같다.

```text
selectedCulpritId 필수
motiveText 필수
methodText 필수
selectedEvidenceIds 1개 이상 15개 이하
coverUpText는 API상 optional이지만 공식 시나리오 채점 품질을 위해 입력 UI를 유지
```

현재 API에는 별도의 `reasoningText` 또는 `comprehensiveReasoning` 필드가 없다.
최종 추리 화면에 종합 추론 입력칸이 있다면, 현재는 프론트 표시용으로 유지하거나 내용을 `motiveText`, `methodText`, `coverUpText` 입력 구조에 맞게 나누어 전송한다.

### 3.12 결과/해설 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| Path | `/api/play-sessions/{sessionId}/result` |
| 사용 화면 | 결과/해설 화면 |
| 호출 시점 | 최종 추리 제출 후 결과 화면 진입 |
| 응답 핵심 | `score`, `grade`, `correctCulprit`, `matched`, `matchedParts`, `missedParts`, `feedback`, `fullExplanation`, `keyEvidences`, `nextRecommendedScenarios` |

`correctCulprit`, `fullExplanation`은 결과 화면에서만 노출한다.
심문 화면이나 진행 중 화면에는 정답 데이터를 보여주면 안 된다.

### 3.13 게임 포기

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/play-sessions/{sessionId}/abandon` |
| 사용 화면 | 게임 종료/나가기 확인 모달 |
| 호출 시점 | 사용자가 진행 중 사건 포기를 확정할 때 |
| 응답 | `success=true`, `data` 없음 |

### 3.14 FCM 디바이스 토큰 등록

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/device-tokens` |
| 사용 화면 | 앱 시작 / 로그인 후 / FCM token refresh 시점 |
| 호출 시점 | Android FCM registration token을 확보했을 때 |
| Request | `token`, `deviceType` |
| 응답 핵심 | `deviceTokenId`, `active` |

`token` 원문은 응답에 돌아오지 않는다.
같은 token을 다시 보내도 백엔드는 token unique 기준으로 upsert한다.
`/api/device-tokens`는 보호 API이므로 Android는 Auth token provider 연결 이후 best-effort로 호출한다.
`POST /api/notifications/test`는 local/test profile 전용 검증 API이므로 운영 앱에서 호출하지 않는다.

---

## 4. 현재 구현되지 않은 문서상 예정 API

아래 API들은 기존 문서에 있거나 화면상 필요해 보이지만, 현재 백엔드 컨트롤러 기준으로는 구현되어 있지 않다.
프론트는 이 API를 바로 호출하지 않는다.

| 예정 API | 현재 대체 방식 |
|---|---|
| `GET /api/play-sessions/{sessionId}/recommended-questions` | 별도 추천 질문 API는 호출하지 않음. 증거 기반 질문은 evidence detail `guidance.suggestedQuestions` 사용 |
| `GET /api/play-sessions/me` | 내 기록 화면은 인증/기록 API 전까지 더미 또는 empty state |
| `POST/DELETE /api/scenarios/{scenarioId}/bookmarks` | 북마크 UI는 비활성 또는 optimistic action 금지 |
| `GET/POST /api/scenarios/{scenarioId}/reviews` | 리뷰 UI는 더미 또는 숨김 |

---

## 5. 1단계 결정사항

프론트 MVP는 우선 아래 API만 실제 연동 대상으로 본다.

```text
GET  /api/scenarios
GET  /api/scenarios/{scenarioId}
POST /api/play-sessions
GET  /api/play-sessions/active?scenarioId={scenarioId}
GET  /api/play-sessions/{sessionId}/dashboard
GET  /api/play-sessions/{sessionId}/evidences
GET  /api/play-sessions/{sessionId}/evidences/{evidenceId}
GET  /api/play-sessions/{sessionId}/suspects
GET  /api/play-sessions/{sessionId}/suspects/{suspectId}
GET  /api/play-sessions/{sessionId}/timeline
GET  /api/play-sessions/{sessionId}/interrogations
POST /api/play-sessions/{sessionId}/interrogations
GET  /api/play-sessions/{sessionId}/hints
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
POST /api/play-sessions/{sessionId}/final-deduction
GET  /api/play-sessions/{sessionId}/result
POST /api/play-sessions/{sessionId}/abandon
POST /api/device-tokens
```

게임 시작 이후의 상세 화면 흐름은 6~9절을 따른다.

---

## 6. 게임 시작 전 사용자 흐름

이 절은 앱 진입부터 플레이 세션 생성 직후까지의 흐름을 정의한다.
프론트는 이 구간에서 아직 범인/정답/Variant 관련 데이터를 다루지 않는다.

### 6.1 전체 흐름

```text
홈
→ 사건 라이브러리
→ 사건 상세
→ 사건 브리핑
→ 플레이 세션 생성
→ 탐정 대시보드 진입
```

중요한 기준은 아래와 같다.

```text
사건 상세의 "조사 시작" 버튼은 브리핑 화면으로 이동한다.
브리핑 화면의 "수사 시작하기" 버튼만 POST /api/play-sessions를 호출한다.
```

브리핑을 읽기 전에 세션을 만들면 사용자가 사건을 실제로 시작하지 않았는데도 진행 중 세션이 생긴다.
따라서 세션 생성은 브리핑 확인 후로 미룬다.

### 6.2 홈 화면

| 항목 | 내용 |
|---|---|
| 화면 목적 | 서비스 첫 진입, 사건 라이브러리로 유도 |
| 최초 호출 API | `GET /api/scenarios?page=0&size=5` |
| 주요 표시 | 앱명, 대표 CTA, 추천/공식 사건 카드, 하단 네비게이션 |
| 사용 필드 | `scenarioId`, `title`, `description`, `thumbnailUrl`, `difficulty`, `estimatedPlayTimeMinutes` |

현재 홈 화면의 주요 액션은 아래처럼 처리한다.

| 사용자 액션 | 처리 |
|---|---|
| `사건 보러 가기` 클릭 | 사건 라이브러리 화면으로 이동 |
| 사건 카드 클릭 | 해당 `scenarioId`로 사건 상세 화면 이동 |
| 하단 `홈` 클릭 | 홈 유지 또는 홈으로 복귀 |
| 하단 `라이브러리` 클릭 | 사건 라이브러리 화면 이동 |
| 하단 `기록` 클릭 | 현재는 내 기록 API가 없으므로 empty/mock 화면 |
| 하단 `만들기` 클릭 | 커스텀 제작 API 완성 전까지 placeholder |
| 하단 `내 정보` 클릭 | 인증 API 완성 전까지 placeholder |

홈에서 시나리오 목록 API가 실패하면 카드 영역만 empty/error 상태로 표시하고, 하단 네비게이션은 유지한다.

### 6.3 사건 라이브러리 화면

| 항목 | 내용 |
|---|---|
| 화면 목적 | 공식/커스텀 사건 목록 탐색, 검색/필터 |
| 호출 API | `GET /api/scenarios` |
| 호출 시점 | 화면 진입, 검색어 변경, 필터 변경, 페이지 추가 로드 |
| 사용 필드 | `scenarioId`, `title`, `description`, `scenarioType`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `averageRating`, `playCount`, `isBookmarked`, `canPlay` |

추천 query 예시는 아래와 같다.

```text
GET /api/scenarios?page=0&size=20
GET /api/scenarios?type=OFFICIAL&page=0&size=20
GET /api/scenarios?keyword=서월채&page=0&size=20
GET /api/scenarios?difficulty=NORMAL&page=0&size=20
```

카드 클릭 시에는 목록 item의 `scenarioId`를 다음 화면으로 넘긴다.
화면에 보이는 `CL-004`, `CL-005` 같은 표시용 번호가 있더라도 API 호출에는 반드시 `scenarioId`를 사용한다.
목록 item의 `canPlay=false`는 사건 시작 불가 상태로 표시하고, 프론트 하드코딩 allowlist로 대체하지 않는다.

| 사용자 액션 | 처리 |
|---|---|
| 검색어 입력 | debounce 후 `GET /api/scenarios?keyword=...` 재호출 |
| 필터 변경 | 선택된 query로 `GET /api/scenarios` 재호출 |
| 사건 카드 클릭 | `GET /api/scenarios/{scenarioId}` 호출 후 상세 화면 표시 |
| 뒤로가기 | 홈 또는 이전 탭으로 복귀 |

### 6.4 사건 상세 화면

| 항목 | 내용 |
|---|---|
| 화면 목적 | 사건을 시작하기 전 공개 정보 확인 |
| 호출 API | `GET /api/scenarios/{scenarioId}` |
| 호출 시점 | 사건 카드 클릭 후 상세 진입 |
| 사용 필드 | `title`, `description`, `synopsis`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `hintCount`, `averageRating`, `ratingCount`, `canPlay` |

표시 우선순위는 아래처럼 잡는다.

| 영역 | 표시 필드 |
|---|---|
| 제목 | `title` |
| 한 줄/짧은 설명 | `description` |
| 긴 사건 소개 | `synopsis` |
| 메타 정보 | `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `hintCount` |

`canPlay=false`이면 `조사 시작` 버튼을 비활성화한다.
`canPlay=true`이면 버튼을 활성화한다.

| 사용자 액션 | 처리 |
|---|---|
| `조사 시작` 클릭 | 세션 생성 없이 사건 브리핑 화면으로 이동 |
| 뒤로가기 | 사건 라이브러리로 복귀 |
| 리뷰/북마크 클릭 | 현재 리뷰/북마크 API 미구현. UI는 숨기거나 비활성 |

### 6.5 사건 브리핑 화면

| 항목 | 내용 |
|---|---|
| 화면 목적 | 실제 게임 시작 전 오프닝과 탐정 목표 확인 |
| 호출 API | 원칙적으로 추가 호출 없음. 상세 화면의 `ScenarioDetailResponse` 재사용 |
| 보조 호출 | 상세 데이터가 없으면 `GET /api/scenarios/{scenarioId}` 재호출 |
| 사용 필드 | `title`, `description`, `synopsis`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount` |

브리핑 화면은 한 줄짜리 설명만 보여주면 안 된다.
공식 시나리오는 `synopsis`에 유저가 읽어야 할 오프닝 본문이 들어간다.

권장 구성은 아래와 같다.

```text
사건 제목
짧은 사건 요약(description)
오프닝 본문(synopsis)
수사 목표
메타 정보: 난이도, 예상 시간, 용의자 수, 증거 수
수사 시작하기 버튼
```

현재 API에는 브리핑 전용 피해자 상세 필드가 없다.
피해자 카드가 화면에 필요하면 후속 API 전까지는 아래 중 하나로 처리한다.

| 방식 | 설명 |
|---|---|
| 권장 | 피해자 카드 영역을 접거나 `synopsis` 중심 브리핑으로 구성 |
| 임시 | 공식 시나리오에 한해 프론트 표시용 더미를 사용 |
| 후속 | `ScenarioDetailResponse` 또는 별도 briefing API에 victim 정보를 추가 |

### 6.6 수사 시작 버튼

브리핑 화면의 `수사 시작하기` 버튼을 누르면 이때 플레이 세션을 생성한다.

```http
POST /api/play-sessions
Content-Type: application/json
```

```json
{
  "scenarioId": 1
}
```

성공하면 응답의 `sessionId`를 저장한다.

```json
{
  "success": true,
  "data": {
    "sessionId": 10,
    "scenarioId": 1,
    "status": "PLAYING",
    "startedAt": "2026-06-02T16:00:00"
  }
}
```

409 `P002`가 내려오면 진행 중 세션 복구 흐름으로 처리한다.

```text
1. error.details.activeSessionId가 있으면 그 값을 sessionId로 저장한다.
2. details가 없으면 GET /api/play-sessions/active?scenarioId={scenarioId}를 호출한다.
3. hasActiveSession=true이면 activeSessionId를 sessionId로 저장한다.
4. hasActiveSession=false이면 버튼 잠금을 풀고 재시도 가능 상태로 되돌린다.
```

세션 생성 성공 후 즉시 대시보드를 조회한다.

```http
GET /api/play-sessions/{sessionId}/dashboard
```

그 다음 게임 화면으로 이동한다.
현재 프론트 흐름에서는 첫 탭을 `현장` 또는 `대시보드/현장`으로 둔다.
게임 내부 탭별 데이터 호출은 7절을 따른다.

### 6.7 게임 시작 전 상태 저장

프론트는 최소한 아래 상태를 관리한다.

| 상태 | 설명 |
|---|---|
| `selectedScenarioId` | 라이브러리/상세에서 선택한 시나리오 ID |
| `scenarioDetail` | 상세/브리핑에 사용하는 `ScenarioDetailResponse` |
| `sessionId` | `POST /api/play-sessions` 성공 후 저장 |
| `sessionStatus` | `PLAYING`, `COMPLETED`, `ABANDONED` |

`sessionId`가 없는 상태에서는 증거/용의자/심문/최종 추리 API를 호출하지 않는다.

### 6.8 2단계 결정사항

게임 시작 전 플로우는 아래 순서로 고정한다.

```text
1. 홈/라이브러리에서 GET /api/scenarios 호출
2. 사건 카드 클릭 시 GET /api/scenarios/{scenarioId} 호출
3. 사건 상세에서 "조사 시작" 클릭 시 브리핑 화면 이동
4. 브리핑은 ScenarioDetailResponse의 synopsis를 중심으로 표시
5. 브리핑의 "수사 시작하기" 클릭 시 POST /api/play-sessions 호출
6. sessionId 저장
7. GET /api/play-sessions/{sessionId}/dashboard 호출
8. 게임 화면 첫 탭으로 이동
```

---

## 7. 게임 내부 탭 흐름

이 절은 `sessionId`가 생성된 뒤 게임 화면 안에서 사용하는 탭 구조와 API 호출 기준을 정의한다.

현재 구현 기준으로 게임 내부의 실제 연동 탭은 아래와 같다.

| 탭 | 현재 연동 상태 | 핵심 API |
|---|---|---|
| 현장 | 실제 연동 | `GET /api/play-sessions/{sessionId}/locations` |
| 증거 | 실제 연동 | `GET /api/play-sessions/{sessionId}/evidences` |
| 용의자 | 실제 연동 | `GET /api/play-sessions/{sessionId}/suspects` |
| 타임라인 | 실제 연동 | `GET /api/play-sessions/{sessionId}/timeline` |
| 힌트 | 실제 연동 | `GET /api/play-sessions/{sessionId}/hints`, `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` |
| 추리 제출 | 실제 연동 | `POST /api/play-sessions/{sessionId}/final-deduction` |

### 7.1 게임 화면 최초 진입

브리핑의 `수사 시작하기`에서 세션 생성이 성공하면 즉시 대시보드를 조회한다.

```text
POST /api/play-sessions
→ sessionId 저장
→ GET /api/play-sessions/{sessionId}/dashboard
→ 게임 화면 진입
```

게임 화면 상단 공통 영역은 `dashboard` 응답을 사용한다.

| UI 영역 | 사용 필드 |
|---|---|
| 사건 제목 | `scenarioTitle` |
| 진행 상태 | `status` |
| 경과 시간 | `elapsedSeconds` |
| 증거 진행률 | `unlockedEvidenceCount`, `totalEvidenceCount` |
| 힌트 사용 수 | `hintUsedCount` |
| 심문 횟수 | `interrogationCount` |
| 짧은 브리핑 | `briefing.victimName`, `briefing.foundLocation`, `briefing.summary` |

`dashboard`와 `evidences` 조회는 서버에서 자동 증거 해금을 동기화한다.
따라서 탭 진입/복귀 시에는 화면 상태를 오래 캐시하지 말고 필요한 API를 다시 조회한다.

### 7.2 현장 탭

| 항목 | 내용 |
|---|---|
| 화면 목적 | 사건 공간, 발견 장소, 조사 분위기 제공 |
| 호출 API | `GET /api/play-sessions/{sessionId}/locations` |
| 보조 API | `GET /api/play-sessions/{sessionId}/dashboard` |
| 사용 필드 | `scenarioTitle`, `mapImageUrl`, `locations[].name`, `locations[].description`, `locations[].imageUrl`, `locations[].mapX`, `locations[].mapY`, `locations[].totalEvidenceCount`, `locations[].unlockedEvidenceCount` |

현장 탭은 locations API를 우선 사용한다.

```text
사건 제목
전체 지도/평면도 mapImageUrl
장소 카드 locations[]
방 사진 locations[].imageUrl
장소별 증거 수 totalEvidenceCount / unlockedEvidenceCount
증거 탭으로 이동하는 CTA
용의자 탭으로 이동하는 CTA
```

`mapImageUrl`과 `locations[].imageUrl`은 서버가 S3 asset key를 변환해서 내려준다.
프론트는 S3 object key를 직접 조합하지 않고, URL이 `null`이면 placeholder를 사용한다.
피해자/발견 장소/요약 briefing은 dashboard 응답을 보조로 사용한다.

### 7.3 증거 탭

| 항목 | 내용 |
|---|---|
| 화면 목적 | 해금/잠금 증거 목록 확인 |
| 호출 API | `GET /api/play-sessions/{sessionId}/evidences?includeLocked=true` |
| 보조 필터 | `status=unlocked`, `status=locked` |
| 사용 필드 | `evidenceId`, `title`, `oneLine`, `description`, `imageAssetKey`, `imageUrl`, `locationName`, `importance`, `isUnlocked`, `unlockHint`, `relatedSuspects` |

증거 보드의 기본 호출은 `includeLocked=true`를 권장한다.
그래야 잠긴 증거 슬롯과 해금 힌트를 함께 보여줄 수 있다.

```text
GET /api/play-sessions/{sessionId}/evidences?includeLocked=true
```

필터 버튼은 아래처럼 연결한다.

| 필터 | API |
|---|---|
| 전체 | `GET /api/play-sessions/{sessionId}/evidences?includeLocked=true` |
| 해금됨 | `GET /api/play-sessions/{sessionId}/evidences?status=unlocked` |
| 잠김 | `GET /api/play-sessions/{sessionId}/evidences?status=locked` |

잠긴 증거는 서버가 상세 정보를 마스킹한다.

| 잠긴 증거 필드 | 응답 |
|---|---|
| `title` | 표시 가능 |
| `isUnlocked` | `false` |
| `unlockHint` | 표시 가능 |
| `description` | `null` |
| `oneLine` | `null` |
| `imageAssetKey` | `null` |
| `imageUrl` | `null` |
| `locationName` | `null` |
| `relatedSuspects` | `null` |

### 7.4 증거 상세

증거 상세 화면은 단건 상세 API를 사용한다.

```text
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

| 사용자 액션 | 처리 |
|---|---|
| 해금 증거 클릭 | 단건 상세 API 조회 후 상세 화면 표시 |
| 잠긴 증거 클릭 | 상세 진입 차단, `unlockHint` 중심의 잠금 상태 표시 |
| 이미지 클릭 | `imageUrl`이 있으면 이미지 확대, 없으면 placeholder |
| 관련 용의자 클릭 | 해당 `suspectId`로 용의자 상세 화면 이동 |
| guidance 추천 질문 클릭 | 심문 화면으로 이동하고 suspect/question/presentedEvidence를 prefill |

해금 증거는 서버가 `imageAssetKey`를 `imageUrl`로 변환해서 내려준다.
그래도 `AWS_S3_PUBLIC_BASE_URL` 미설정, asset 누락, 잠긴 증거 마스킹 때문에 `imageUrl`이 `null`일 수 있다.
이 경우 프론트는 S3 URL을 임의 조립하지 않고 placeholder를 표시한다.

`guidance.compareEvidences`의 잠긴 증거는 `evidenceCode`가 내려오지 않을 수 있다.
프론트는 잠긴 비교 증거를 code 기반으로 라우팅하지 말고 `isUnlocked=false`와 `unlockHint` 중심으로 표시한다.
`evidenceId`가 있더라도 `isUnlocked=false`이면 상세 이동 버튼을 만들지 않는다.

### 7.5 용의자 탭

| 항목 | 내용 |
|---|---|
| 화면 목적 | 심문 가능한 인물 목록 확인 |
| 호출 API | `GET /api/play-sessions/{sessionId}/suspects` |
| 사용 필드 | `suspectId`, `name`, `role`, `relationToVictim`, `publicStatement`, `alibi`, `portraitImageUrl`, `suspicionLevel`, `interrogationCount` |

용의자 목록은 공개 정보만 보여준다.
범인 여부나 정답 정보는 절대 표시하지 않는다.

| 사용자 액션 | 처리 |
|---|---|
| 용의자 카드 클릭 | 단건 상세 API 호출 후 용의자 상세 화면 표시 |
| 검색어 입력 | 현재는 프론트 로컬 필터 가능 |
| 정렬/필터 | 현재는 프론트 로컬 처리 가능 |

심문 후에는 `interrogationCount`가 바뀔 수 있으므로 용의자 탭 복귀 시 목록을 다시 조회한다.

### 7.6 용의자 상세

용의자 상세 화면은 단건 상세 API를 호출한다.

```text
GET /api/play-sessions/{sessionId}/suspects/{suspectId}
```

| 영역 | 사용 필드 |
|---|---|
| 이름 | `name` |
| 역할 | `role` |
| 피해자와의 관계 | `relationToVictim` |
| 공개 진술 | `publicStatement` |
| 공개 알리바이 | `alibi` |
| 초상 이미지 | `portraitImageUrl` |
| 의심도 UI | `suspicionLevel` |
| 심문 횟수 | `interrogationCount` |

| 사용자 액션 | 처리 |
|---|---|
| `심문하기` 클릭 | `sessionId`, `suspectId`를 들고 심문 채팅 화면 이동 |
| `범인 지목` 클릭 | 최종 추리 탭으로 이동하고 `selectedCulpritId`를 해당 suspect로 사전 선택 |
| 뒤로가기 | 용의자 목록으로 복귀 |

`범인 지목` 버튼은 정답 확인 버튼이 아니다.
누르면 바로 결과를 보여주지 않고 최종 추리 작성 화면으로 이동해야 한다.

### 7.7 타임라인 탭

타임라인 탭은 서버 timeline API를 호출한다.

```text
GET /api/play-sessions/{sessionId}/timeline
```

응답이 비어 있으면 empty state를 보여준다.

| 방식 | 설명 |
|---|---|
| 정상 | 서버 timeline event 목록 표시 |
| empty | `타임라인은 수사 기록이 쌓이면 제공됩니다.` |
| 금지 | 정답/범인/숨겨진 실제 사건 순서를 프론트 더미로 노출 |

타임라인 더미를 쓰더라도 정답/범인/숨겨진 실제 사건 순서를 노출하면 안 된다.
유저가 이미 확인한 공개 사건 흐름만 표시한다.

### 7.8 힌트 탭

| 항목 | 내용 |
|---|---|
| 화면 목적 | 필요 시 패널티를 감수하고 힌트 확인 |
| 목록 API | `GET /api/play-sessions/{sessionId}/hints` |
| 사용 API | `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` |
| 사용 필드 | `hintId`, `hintLevel`, `content`, `isAvailable`, `isUsed`, `remainingMinutes`, `penaltyScore` |

힌트 목록 표시 기준은 아래와 같다.

| 상태 | 표시 |
|---|---|
| `isAvailable=false` | 잠금 상태, `remainingMinutes` 표시 |
| `isAvailable=true`, `isUsed=false` | 사용 가능, 내용은 숨김 |
| `isUsed=true` | 이미 사용한 힌트, `content` 표시 |

힌트 사용 버튼을 누르면 사용 API를 호출한다.

```http
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
```

성공 후에는 아래 데이터를 갱신한다.

```text
GET /api/play-sessions/{sessionId}/hints
GET /api/play-sessions/{sessionId}/dashboard
```

힌트는 점수 패널티가 있으므로 사용 전 확인 모달을 둔다.

### 7.9 탭 전환 시 갱신 규칙

탭 전환마다 모든 API를 무조건 호출하면 불필요한 요청이 늘어난다.
다만 현재 증거 자동 해금은 `dashboard` 또는 `evidences` 조회 시점에 동기화되므로 아래 기준을 권장한다.

| 상황 | 권장 갱신 |
|---|---|
| 게임 화면 최초 진입 | `dashboard` |
| 현장 탭 진입 | `locations`, 필요 시 `dashboard` |
| 증거 탭 진입 | `evidences?includeLocked=true` |
| 용의자 탭 진입 | `suspects` |
| 힌트 탭 진입 | `hints` |
| 심문 완료 후 | `dashboard`, `evidences?includeLocked=true`, `suspects`, `interrogations?suspectId=...` |
| 힌트 사용 후 | `dashboard`, `hints` |
| 최종 추리 제출 후 | `result` |

시간 기반/phase 기반 증거 해금이 있으므로, 사용자가 오래 머무는 화면에서는 `dashboard` 또는 `evidences`를 주기적으로 갱신할 수 있다.
MVP에서는 30~60초 간격 또는 탭 재진입 시 갱신으로 충분하다.

### 7.10 3단계 결정사항

게임 내부 탭은 아래 기준으로 구현한다.

```text
1. 게임 화면 공통 상태는 dashboard를 기준으로 한다.
2. 현장 탭은 locations API를 기본 호출로 사용하고, 피해자/발견 장소 briefing은 dashboard를 보조로 사용한다.
3. 증거 탭은 evidences?includeLocked=true를 기본 호출로 사용한다.
4. 증거 상세는 `GET /api/play-sessions/{sessionId}/evidences/{evidenceId}`를 사용한다.
5. 용의자 상세는 `GET /api/play-sessions/{sessionId}/suspects/{suspectId}`를 사용한다.
6. 타임라인은 `GET /api/play-sessions/{sessionId}/timeline`을 사용하고, 빈 응답만 empty state로 처리한다.
7. 힌트는 목록 조회와 사용 API를 실제 연동한다.
8. dashboard/evidences 조회가 자동 증거 해금 동기화 지점이라는 점을 고려해 탭 복귀 시 갱신한다.
```

---

## 8. 심문 채팅과 증거 제시 흐름

이 절은 용의자 상세에서 심문 채팅으로 들어간 뒤 질문을 보내고, 필요하면 증거를 제시하는 흐름을 정의한다.

심문에서 가장 중요한 원칙은 아래와 같다.

```text
프론트는 범인/정답/Variant를 알 필요가 없다.
프론트는 용의자 공개 정보, 해금 증거, 사용자 질문만 다룬다.
AI 답변 정책과 정답 누설 방지는 백엔드가 처리한다.
```

### 8.1 심문 진입

심문 화면은 용의자 상세의 `심문하기` 버튼에서 진입한다.

진입 시 필요한 상태는 아래와 같다.

| 상태 | 설명 |
|---|---|
| `sessionId` | 현재 플레이 세션 ID |
| `suspectId` | 심문 대상 용의자 ID |
| `suspectName` | 상단 제목/말풍선 표시용 |
| `suspectRole` | 보조 프로필 표시용 |

심문 화면 진입 직후 기존 로그를 조회한다.

```http
GET /api/play-sessions/{sessionId}/interrogations?suspectId={suspectId}
```

응답은 시간순으로 내려오며, 채팅 UI는 `question`과 `answer`를 한 쌍으로 표시한다.

| 응답 필드 | 화면 사용 |
|---|---|
| `interrogationId` | 메시지 key |
| `suspectId` | 현재 채팅 대상 검증 |
| `suspectName` | AI 말풍선 이름 |
| `questionType` | 일반 질문/증거 제시 라벨 |
| `question` | 사용자 말풍선 |
| `answer` | 용의자 답변 말풍선 |
| `presentedEvidence` | 증거 제시 배지 |
| `createdAt` | 메시지 시간 |

### 8.2 자유 질문 전송

사용자가 입력창에 질문을 쓰고 전송하면 아래 API를 호출한다.

```http
POST /api/play-sessions/{sessionId}/interrogations
Content-Type: application/json
```

```json
{
  "suspectId": 1,
  "questionType": "FREE",
  "question": "사건 당시 어디에 있었습니까?",
  "presentedEvidenceId": null
}
```

프론트 입력 검증은 아래 기준을 사용한다.

| 항목 | 기준 |
|---|---|
| `suspectId` | 필수 |
| `questionType` | 필수 |
| `question` | 필수, 500자 이하 |
| `presentedEvidenceId` | 자유 질문에서는 `null` |

전송 버튼을 누른 뒤에는 중복 제출을 막기 위해 버튼을 loading/disabled 상태로 둔다.

### 8.3 추천 질문

증거 상세의 `guidance.suggestedQuestions`는 증거 기반 추천 질문 chip으로 사용한다.
별도 `GET /api/play-sessions/{sessionId}/recommended-questions` API는 아직 호출하지 않는다.

노출 조건:

```text
현재 증거가 해금되어 있고 상세 조회 가능한 경우에만 표시한다.
targetSuspectId가 없거나 현재 플레이에서 유효하지 않으면 chip을 숨기거나 disabled 처리한다.
잠긴 증거 상세에서는 추천 질문을 표시하지 않는다.
```

추천 질문 chip을 누르면 심문 화면으로 이동하되 자동 전송하지 않는다.
입력창에는 질문을 prefill하고 커서는 맨 뒤에 둔다.
기존 draft가 있으면 추천 질문으로 override하는 것을 기본 정책으로 한다.

```json
{
  "suspectId": 1,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "이 증거와 다른 기록의 차이를 설명할 수 있습니까?",
  "presentedEvidenceId": 10
}
```

작성 기준:

```text
guidance 추천 질문은 실제 심문 smoke에서 응답 품질을 1회 이상 확인한 문구만 seed에 넣는다.
질문은 정답 유도 문구가 아니라 증거 비교/확인 질문이어야 한다.
```

### 8.4 증거 제시 모달

심문 화면의 `증거 제시` 버튼을 누르면 해금된 증거 목록을 조회한다.

```http
GET /api/play-sessions/{sessionId}/evidences?status=unlocked
```

증거 제시 모달에는 해금된 증거만 보여준다.
잠긴 증거는 제시할 수 없다.

| 모달 영역 | 사용 필드 |
|---|---|
| 증거 제목 | `title` |
| 짧은 설명 | `oneLine` |
| 위치 | `locationName` |
| 중요도 | `importance` |
| 이미지 | `imageUrl` 또는 placeholder |
| 관련 용의자 | `relatedSuspects` |

사용자가 증거를 선택하면 `selectedEvidenceId`를 심문 화면 상태에 저장한다.
입력창 근처에는 선택된 증거 배지를 표시한다.

| 사용자 액션 | 처리 |
|---|---|
| 증거 선택 | `presentedEvidenceId`로 저장 |
| 선택 해제 | `presentedEvidenceId=null` |
| 잠긴 증거 선택 | 불가. 모달에 표시하지 않음 |
| 증거 상세 보기 | 목록 item 기반으로 간단 상세 표시 |

### 8.5 증거 제시 질문 전송

증거를 선택한 상태에서 질문을 보내면 `questionType`은 `EVIDENCE_PRESENTED`를 사용한다.

```http
POST /api/play-sessions/{sessionId}/interrogations
Content-Type: application/json
```

```json
{
  "suspectId": 1,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "이 증거에 나온 내용과 당신의 진술이 다른데 설명해 주세요.",
  "presentedEvidenceId": 10
}
```

백엔드는 `presentedEvidenceId`가 현재 세션에서 해금된 증거인지 검증한다.
잠긴 증거를 보내면 실패한다.

| 실패 상황 | 처리 |
|---|---|
| 잠긴 증거 제시 | `AI009`, 사용자에게 "아직 사용할 수 없는 증거입니다." 표시 |
| 세션 종료 상태 | `AI007`, 게임 상태 재조회 |
| 존재하지 않는 용의자 | `AI008`, 용의자 목록 재조회 |
| 질문 500자 초과 | 입력 단계에서 차단 |

### 8.6 심문 응답 처리

심문 성공 응답은 아래 필드를 사용한다.

| 응답 필드 | 화면 사용 |
|---|---|
| `interrogationId` | 새 메시지 key |
| `suspectId` | 현재 채팅 대상 검증 |
| `suspectName` | AI 말풍선 이름 |
| `question` | 사용자 말풍선 |
| `answer` | 용의자 답변 말풍선 |
| `unlockedEvidences` | 현재는 비어 있을 수 있음 |
| `createdAt` | 메시지 시간 |

현재 서비스 구현상 `unlockedEvidences`는 빈 리스트일 수 있다.
따라서 심문 성공 후에는 이 필드에만 의존하지 않는다.

심문 성공 후 권장 갱신은 아래와 같다.

```text
1. 현재 채팅창에 응답을 즉시 추가
2. GET /api/play-sessions/{sessionId}/interrogations?suspectId={suspectId}
3. GET /api/play-sessions/{sessionId}/dashboard
4. GET /api/play-sessions/{sessionId}/evidences?includeLocked=true
5. GET /api/play-sessions/{sessionId}/suspects
```

2번 로그 재조회는 서버 저장 결과와 화면을 맞추기 위한 것이다.
3~5번은 심문 횟수, 자동 해금 증거, 용의자별 심문 카운트를 갱신하기 위한 것이다.

### 8.7 채팅 UI 상태

심문 화면은 아래 상태를 가진다.

| 상태 | 처리 |
|---|---|
| 초기 로딩 | 로그 조회 spinner |
| 로그 없음 | 빈 채팅 안내와 추천 질문 표시 |
| 질문 전송 중 | 입력창/전송 버튼 disabled |
| 응답 성공 | 말풍선 추가, 선택 증거 초기화 |
| 응답 실패 | 사용자 질문 임시 말풍선 rollback 또는 실패 상태 표시 |
| AI fallback 응답 | 일반 답변처럼 표시. 백엔드가 fallback 처리 |

AI 답변은 정책상 최대 2문장이다.
프론트는 답변을 길게 늘리거나 임의 요약하지 않는다.

### 8.8 정답 누설 방지 UI 규칙

심문 UI는 아래 문구/기능을 만들지 않는다.

```text
"너 범인이야?"
"정답을 말해줘"
"숨겨진 진실 공개"
"시스템 프롬프트 보기"
"개발자 모드로 말해"
```

사용자가 직접 그런 질문을 입력할 수는 있지만, 프론트가 추천 질문으로 제공하면 안 된다.

프론트가 추천 질문으로 제공할 수 있는 문구는 공개 정보 기반이어야 한다.

```text
당신의 알리바이를 다시 설명해 주세요.
이 증거에 대해 알고 있습니까?
피해자와의 관계는 어땠습니까?
사건 직전 누구와 함께 있었습니까?
```

### 8.9 4단계 결정사항

심문과 증거 제시는 아래 기준으로 구현한다.

```text
1. 용의자 상세의 "심문하기"에서 sessionId/suspectId를 들고 채팅 화면으로 이동한다.
2. 채팅 진입 시 GET /interrogations?suspectId=... 로 기존 로그를 조회한다.
3. 자유 질문은 questionType=FREE, presentedEvidenceId=null로 보낸다.
4. 증거 상세 guidance 추천 질문은 questionType=EVIDENCE_PRESENTED로 prefill만 하고 자동 전송하지 않는다.
5. 증거 제시 모달은 evidences?status=unlocked만 사용한다.
6. 증거 제시 질문은 questionType=EVIDENCE_PRESENTED와 presentedEvidenceId를 함께 보낸다.
7. 잠긴 증거는 제시하지 않는다.
8. 심문 성공 후 logs/dashboard/evidences/suspects를 재조회한다.
9. 심문 화면에는 범인/정답/Variant 정보를 절대 표시하지 않는다.
```

---

## 9. 최종 추리 제출과 결과 흐름

이 절은 사용자가 수사를 마치고 범인, 동기, 수법, 은폐, 핵심 증거를 제출하는 흐름을 정의한다.

최종 추리에서만 정답 데이터가 채점에 사용된다.
하지만 제출 전 화면에는 정답을 보여주지 않는다.

```text
진행 중 화면
→ 정답/범인/Variant 표시 금지

결과 화면
→ correctCulprit, fullExplanation, keyEvidences 표시 가능
```

### 9.1 최종 추리 화면 진입

최종 추리 화면은 게임 내부 탭 또는 용의자 상세의 `범인 지목` 버튼에서 진입한다.

진입 시 필요한 데이터는 아래 API로 준비한다.

```text
GET /api/play-sessions/{sessionId}/dashboard
GET /api/play-sessions/{sessionId}/suspects
GET /api/play-sessions/{sessionId}/evidences?status=unlocked
```

| API | 사용 목적 |
|---|---|
| `dashboard` | 세션 상태, 사건 제목, 진행 정보 확인 |
| `suspects` | 범인 선택 목록 |
| `evidences?status=unlocked` | 최종 추리에 사용할 증거 선택 목록 |

`sessionStatus`가 `COMPLETED`이면 새 제출 화면을 보여주지 않고 결과 화면으로 이동한다.

### 9.2 입력 필드

최종 추리 제출 API의 request는 아래 구조다.

```json
{
  "selectedCulpritId": 1,
  "motiveText": "범행 동기를 사용자가 서술합니다.",
  "methodText": "범행 방법을 사용자가 서술합니다.",
  "coverUpText": "은폐 방법을 사용자가 서술합니다.",
  "selectedEvidenceIds": [10, 12, 15]
}
```

화면 입력 영역은 아래처럼 매핑한다.

| 화면 영역 | API 필드 | 필수 여부 |
|---|---|---|
| 범인 선택 | `selectedCulpritId` | 필수 |
| 동기 입력 | `motiveText` | 필수 |
| 수법 입력 | `methodText` | 필수 |
| 은폐 입력 | `coverUpText` | API상 선택, UI상 입력 권장 |
| 증거 선택 | `selectedEvidenceIds` | 필수, 1~15개 |

현재 API에는 별도의 종합 추론 필드가 없다.
화면에 `종합 추론` 입력칸이 있다면 프론트 표시용으로만 두거나, 사용자가 입력한 내용을 동기/수법/은폐 입력칸으로 나누어 전송한다.

### 9.3 범인 선택

범인 선택 목록은 `GET /api/play-sessions/{sessionId}/suspects` 응답을 사용한다.

| 표시 | 필드 |
|---|---|
| 이름 | `name` |
| 역할 | `role` |
| 관계 | `relationToVictim` |
| 공개 진술 요약 | `publicStatement` |
| 의심도 UI | `suspicionLevel` |

용의자 상세의 `범인 지목` 버튼에서 들어온 경우, 해당 `suspectId`를 `selectedCulpritId`로 사전 선택한다.
사전 선택은 사용자가 수정할 수 있어야 한다.

### 9.4 증거 선택

최종 추리에 사용할 증거는 해금된 증거만 선택할 수 있다.

```http
GET /api/play-sessions/{sessionId}/evidences?status=unlocked
```

프론트는 아래 기준으로 선택을 제한한다.

| 조건 | 처리 |
|---|---|
| 선택 증거 0개 | 제출 버튼 비활성 |
| 선택 증거 1~15개 | 제출 가능 |
| 선택 증거 16개 이상 | 추가 선택 차단 또는 경고 |
| 잠긴 증거 | 목록에 표시하지 않음 |

백엔드도 제출 시 `selectedEvidenceIds`가 모두 해금된 증거인지 검증한다.
해금되지 않은 증거가 포함되면 `AI021`이 발생한다.

### 9.5 제출 전 검증

최종 제출 버튼은 아래 조건을 모두 만족할 때만 활성화한다.

```text
selectedCulpritId가 있다.
motiveText가 비어 있지 않다.
methodText가 비어 있지 않다.
selectedEvidenceIds가 1개 이상 15개 이하이다.
채점 요청이 진행 중이 아니다.
```

`coverUpText`는 API상 optional이지만, 공식 시나리오의 채점 품질을 위해 화면에는 유지한다.
가능하면 프론트에서는 `coverUpText`도 입력을 유도한다.

제출 버튼을 누르면 확인 모달을 띄운다.

```text
최종 추리를 제출하면 이 세션은 완료 처리됩니다.
제출 후에는 심문/힌트/증거 탐색을 계속 진행할 수 없습니다.
```

### 9.6 최종 추리 제출

제출 API는 아래와 같다.

```http
POST /api/play-sessions/{sessionId}/final-deduction
Content-Type: application/json
```

```json
{
  "selectedCulpritId": 1,
  "motiveText": "피해자에게 오래된 원한과 직접적인 이해관계가 있었다.",
  "methodText": "피해자가 평소 사용하는 루틴을 이용해 치명적인 상황을 만들었다.",
  "coverUpText": "사고나 지병처럼 보이도록 현장 흔적과 로그를 흐렸다.",
  "selectedEvidenceIds": [10, 12, 15]
}
```

요청 중에는 제출 버튼을 disabled 상태로 둔다.
중복 클릭으로 같은 세션에 여러 번 제출하지 않게 막아야 한다.

성공 응답은 아래처럼 처리한다.

| 응답 필드 | 화면 사용 |
|---|---|
| `finalDeductionId` | 결과 식별자 |
| `score` | 즉시 표시 가능한 총점 |
| `grade` | 즉시 표시 가능한 등급 |
| `feedbackSummary` | 결과 요약 |
| `resultAvailable` | 결과 상세 조회 가능 여부 |
| `submittedAt` | 제출 시각 |

현재 구현에서는 저장 성공 시 세션이 `COMPLETED`로 전환된다.
성공 후에는 진행 중 게임 화면으로 돌아가지 않고 결과 화면으로 이동한다.

### 9.7 제출 오류 처리

최종 추리 제출에서 자주 만날 수 있는 오류는 아래와 같다.

| 코드 | 의미 | 프론트 처리 |
|---|---|---|
| `AI010` | 이미 최종 추리를 제출함 | 결과 화면으로 이동 시도 |
| `AI011` | 정답 데이터 없음 | 운영/시드 문제. 사용자에게 일시 오류 표시 |
| `AI014` | 채점 처리 실패 | 재시도 안내 |
| `AI015` | 채점 진행 중 | 버튼 비활성 유지, 잠시 후 재시도 |
| `AI021` | 사용할 수 없는 증거 포함 | 증거 목록 재조회 후 선택 초기화 |
| `ACCESS_DENIED` | 세션 소유자 불일치 | 세션 상태 초기화 또는 홈 이동 |

`AI010`이 발생했을 때는 아래 API로 결과가 있는지 확인한다.

```http
GET /api/play-sessions/{sessionId}/result
```

### 9.8 결과 화면 진입

제출 성공 후 `resultAvailable=true`이면 결과 상세를 조회한다.

```http
GET /api/play-sessions/{sessionId}/result
```

결과 응답은 게임이 끝난 뒤에만 표시한다.

| 응답 필드 | 화면 사용 |
|---|---|
| `sessionId` | 현재 결과 검증 |
| `score` | 총점 |
| `grade` | 등급 |
| `correctCulprit` | 정답 범인 카드 |
| `matched` | 항목별 정오 여부 |
| `matchedParts` | 맞힌 부분 목록 |
| `missedParts` | 놓친 부분 목록 |
| `feedback` | 피드백 요약 |
| `fullExplanation` | 전체 해설 |
| `keyEvidences` | 핵심 증거 목록 |
| `nextRecommendedScenarios` | 후속 추천 시나리오 |

결과 화면 구성은 아래 순서를 권장한다.

```text
점수와 등급
맞힌 부분 / 놓친 부분
정답 범인
핵심 증거
전체 해설
다음 사건 추천 또는 라이브러리 이동 CTA
```

### 9.9 결과 화면 정보 노출 경계

아래 데이터는 결과 화면에서만 표시한다.

```text
correctCulprit
fullExplanation
keyEvidences
matchedParts
missedParts
```

이 데이터는 심문 화면, 증거 화면, 용의자 화면으로 되돌아가서 표시하지 않는다.
사용자가 결과 화면에서 뒤로가기를 눌러도 완료된 세션의 탐색 화면으로 돌아가 정답을 섞어 보여주지 않는다.

권장 처리:

```text
결과 화면 뒤로가기
→ 사건 라이브러리 또는 홈

결과 화면의 "다시 보기"
→ 결과 화면 내부에서만 해설 재표시

동일 시나리오 재도전
→ 새 play session 생성
```

### 9.10 점수/등급 표시

현재 등급 기준은 아래와 같다.

| 점수 | 등급 |
|---|---|
| 90점 이상 | `S` |
| 80점 이상 | `A` |
| 70점 이상 | `B` |
| 60점 이상 | `C` |
| 60점 미만 | `D` |

힌트를 사용했다면 힌트 패널티가 최종 점수에서 차감된다.
프론트는 결과 화면에서 `score`와 `grade`를 서버 응답 그대로 표시한다.

### 9.11 5단계 결정사항

최종 추리와 결과는 아래 기준으로 구현한다.

```text
1. 최종 추리 화면 진입 시 dashboard/suspects/unlocked evidences를 조회한다.
2. 범인은 suspects 목록에서 선택한다.
3. 증거는 evidences?status=unlocked 목록에서만 선택한다.
4. selectedEvidenceIds는 1~15개로 제한한다.
5. selectedCulpritId, motiveText, methodText는 필수다.
6. coverUpText는 API상 optional이지만 UI에서는 입력을 유도한다.
7. 제출 중에는 버튼을 disabled 처리한다.
8. 제출 성공 시 세션은 COMPLETED가 되며 결과 화면으로 이동한다.
9. 결과 화면에서만 correctCulprit/fullExplanation/keyEvidences를 표시한다.
10. 결과 화면 뒤로가기는 홈/라이브러리로 보내고, 완료 세션의 수사 화면으로 정답을 섞어 돌려보내지 않는다.
```

---

## 10. 상태 처리와 기존 문서 연결

이 절은 프론트 구현 중 공통으로 적용할 로딩, empty, error, placeholder 처리 기준을 정의한다.

### 10.1 문서 우선순위

프론트 연동 기준은 아래 순서로 본다.

```text
1. 현재 백엔드 컨트롤러 구현
2. 이 문서: docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md
3. docs/CaseLab_AI_API_Spec.md
4. docs/CaseLab_AI_PRD.md
```

`CaseLab_AI_API_Spec.md`에는 계획 API가 포함되어 있다.
따라서 실제 구현 여부가 애매하면 이 문서의 "현재 구현되지 않은 문서상 예정 API" 표를 우선 확인한다.

### 10.2 공통 로딩 상태

각 화면은 API 호출 중 아래 상태를 가져야 한다.

| 화면/동작 | 로딩 처리 |
|---|---|
| 홈/라이브러리 목록 | 카드 skeleton 또는 목록 spinner |
| 사건 상세 | 본문 skeleton |
| 수사 시작 | `수사 시작하기` 버튼 disabled |
| 게임 대시보드 | 상단 상태 영역 skeleton |
| 증거/용의자/힌트 목록 | 리스트 skeleton 또는 spinner |
| 심문 질문 전송 | 입력창과 전송 버튼 disabled |
| 최종 추리 제출 | 제출 버튼 disabled, 중복 제출 차단 |
| 결과 조회 | 결과 카드 skeleton |

로딩 중에도 사용자가 뒤로가기를 누를 수는 있어야 한다.
단, 최종 추리 제출 요청 중에는 중복 제출을 막기 위해 제출 버튼을 반드시 잠근다.

### 10.3 공통 empty 상태

데이터가 없을 때는 화면을 깨뜨리지 않고 empty state를 보여준다.

| 화면 | empty 기준 | 표시 기준 |
|---|---|---|
| 시나리오 목록 | `data.content`가 비어 있음 | 검색 조건을 바꾸거나 나중에 다시 시도 안내 |
| 심문 로그 | 로그 배열이 비어 있음 | 첫 질문 유도, 추천 질문 표시 |
| 증거 목록 | 해금 증거가 없음 | 수사를 진행하면 증거가 열릴 수 있음을 안내 |
| 잠긴 증거 필터 | 잠긴 증거가 없음 | 모든 증거가 해금되었거나 조건 없음 표시 |
| 용의자 목록 | 배열이 비어 있음 | 시나리오 데이터 문제로 표시 |
| 힌트 목록 | 배열이 비어 있음 | 사용 가능한 힌트 없음 |
| 타임라인 | 응답 배열이 비어 있음 | 공개된 사건 흐름이 아직 없음을 안내 |
| 결과 조회 | 결과 없음 | 아직 최종 추리가 제출되지 않았음을 안내 |

empty state에서는 정답이나 숨겨진 진행 정보를 암시하지 않는다.

### 10.4 공통 오류 처리

공통 실패 응답은 아래 구조로 내려온다.

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지"
  }
}
```

`error.details`는 오류에 따라 없을 수 있다. 프론트는 `error.message`를 그대로 노출할 수 있지만, 운영/시드/서버 내부 문제는 사용자 친화 문구로 바꿔도 된다.

| 오류 성격 | 처리 |
|---|---|
| 네트워크 실패 | 재시도 버튼 표시 |
| 400 입력 오류 | 해당 입력 영역 강조 |
| 403 접근 거부 | 홈 또는 라이브러리로 이동 |
| 404 리소스 없음 | 목록 재조회 또는 이전 화면 이동 |
| 409 중복/진행 중 | `P002`이면 `details.activeSessionId` 또는 `GET /api/play-sessions/active?scenarioId=`로 이어가기 |
| 500 서버 오류 | 일시 오류 안내, 재시도 제공 |

### 10.5 주요 도메인 오류 코드

프론트가 MVP에서 직접 분기할 가능성이 높은 코드는 아래와 같다.

| 코드 | 상황 | 프론트 처리 |
|---|---|---|
| `AI007` | 플레이 중인 세션이 아님 | dashboard 재조회, 완료면 결과 화면 이동 |
| `AI008` | 용의자를 찾을 수 없음 | suspects 재조회 |
| `AI009` | 제시 증거가 미해금 | evidences 재조회, 증거 선택 초기화 |
| `AI010` | 최종 추리 이미 제출 | result 조회 |
| `AI011` | 정답 정보 없음 | 운영/시드 문제 안내 |
| `AI014` | 채점 실패 | 재시도 안내 |
| `AI015` | 채점 진행 중 | 제출 버튼 disabled, 잠시 후 재시도 |
| `AI021` | 최종 추리 증거 미해금 | evidences 재조회, 선택 초기화 |
| `P002` | 같은 시나리오의 진행 중 세션 존재 | `details.activeSessionId`가 있으면 이어가기, 없으면 `GET /api/play-sessions/active?scenarioId=` fallback |

서버 오류 코드를 세부적으로 알 수 없으면 기본 오류 모달을 사용한다.

### 10.6 미구현 기능 placeholder 기준

아래 기능은 현재 실제 연동 API가 없거나 후속 구현이 필요하다.

| 기능 | 현재 처리 |
|---|---|
| S3 assetKey 직접 변환 | 프론트에서 임의 조립하지 않음 |
| 증거 상세 API | `GET /api/play-sessions/{sessionId}/evidences/{evidenceId}` 사용 |
| 용의자 상세 API | `GET /api/play-sessions/{sessionId}/suspects/{suspectId}` 사용 |
| 타임라인 API | `GET /api/play-sessions/{sessionId}/timeline` 사용. 빈 응답만 empty state |
| 추천 질문 API | 별도 API는 호출하지 않음. 증거 기반 질문은 detail `guidance.suggestedQuestions` 사용 |
| 내 기록 API | empty/mock |
| 마이페이지 API | empty/mock |
| 북마크/리뷰 API | 숨김 또는 disabled |
| 커스텀 제작 | 별도 제작 플로우 확정 전 placeholder |

placeholder는 "아직 구현 전"이라는 내부 표현보다 유저 관점의 자연스러운 문구를 사용한다.

```text
아직 표시할 기록이 없습니다.
수사를 진행하면 새로운 단서가 이곳에 정리됩니다.
현재 사용할 수 있는 정보가 없습니다.
```

### 10.7 이미지 처리 기준

현재 백엔드는 공식 S3 asset key를 응답용 이미지 URL로 변환한다.
프론트는 응답에 내려온 URL만 사용하고, S3 object key를 직접 조합하지 않는다.

현재 URL 응답이 붙은 범위는 아래와 같다.

| API | 이미지 필드 |
|---|---|
| `GET /api/scenarios` | `thumbnailUrl` |
| `GET /api/scenarios/{scenarioId}` | `coverImageUrl`, `mapImageUrl` |
| `GET /api/play-sessions/{sessionId}/locations` | `mapImageUrl`, `locations[].imageUrl` |
| `GET /api/play-sessions/{sessionId}/evidences` | `imageUrl` |
| `GET /api/play-sessions/{sessionId}/suspects` | `portraitImageUrl` |

이미지 업로드 API, signed URL API는 아직 후속 범위다.

프론트 기준은 아래와 같다.

| 응답 상태 | 처리 |
|---|---|
| 이미지 URL 필드 존재 | 해당 URL 표시 |
| 이미지 URL 필드 `null`, asset key 존재 | placeholder 표시. 프론트에서 S3 URL 임의 조립 금지 |
| 둘 다 null | placeholder 표시 |
| 이미지 로드 실패 | placeholder와 재시도 또는 기본 이미지 |

이미지를 클릭해 확대하는 UX는 해당 화면의 이미지 URL 필드가 있을 때만 활성화한다.

### 10.8 화면별 최초 연동 체크리스트

프론트가 더미에서 API로 전환할 때 아래 순서로 확인한다.

```text
1. GET /api/scenarios로 라이브러리 목록이 뜬다.
2. GET /api/scenarios/{scenarioId}로 title/description/synopsis가 뜬다.
3. 브리핑의 "수사 시작하기"로 POST /api/play-sessions가 성공한다.
4. sessionId로 dashboard가 조회된다.
5. evidences?includeLocked=true로 잠금/해금 증거가 구분된다.
6. suspects로 용의자 목록이 뜬다.
7. interrogations?suspectId=...로 채팅 로그가 조회된다.
8. POST interrogations로 답변이 온다.
9. evidences?status=unlocked로 증거 제시 모달이 열린다.
10. POST final-deduction으로 결과가 생성된다.
11. GET result로 점수/해설이 표시된다.
```

### 10.9 Android 구현 우선순위

1순위는 공식 시나리오 플레이 전체 흐름이다.

```text
시나리오 목록
시나리오 상세
게임 세션 시작
탐정 대시보드
현장 정보
증거 목록 / 상세
용의자 목록 / 상세
심문 채팅
힌트
최종 추리 제출
결과 / 해설
```

2순위는 커스텀 제작 기본형이다.

```text
시나리오 생성 / 수정
장소 / 피해자 / 용의자 / 증거 / 힌트 / 정답 등록
AI 검증
공개 등록
```

3순위 이후는 커뮤니티/계정/거래 기능이다.

```text
리뷰 조회 / 작성
북마크 추가 / 해제
내 기록
마이페이지
AI 시나리오 초안 생성
인증 / JWT
구매 / 크레딧
```

현재 컨트롤러가 없는 API는 UI를 숨기거나 disabled/mock 상태로 둔다.

### 10.10 프론트 구현 금지사항

아래 처리는 하지 않는다.

```text
시나리오 정답/Variant를 프론트 상수로 들고 있기
S3 object key를 조합해서 임의 URL 만들기
잠긴 증거의 description/image를 프론트 더미로 채우기
추천 질문에 정답 유도 문구 넣기
결과 화면 데이터를 진행 중 화면에 섞어서 보여주기
미구현 API를 실제 API처럼 호출하기
DTO ID 필드명을 navigation/API boundary에서 임의로 id로 축약하기
API 응답 JSON 예시를 프론트 문서에 새로 복제하기
```

Android 개발 AI는 다음 기준을 따른다.

```text
1. Request/Response JSON 예시는 CaseLab_AI_API_Spec.md를 기준으로 한다.
2. DTO 필드명은 CaseLab_AI_API_Spec.md를 기준으로 한다.
3. 화면 목적과 MVP 범위는 CaseLab_AI_PRD.md를 기준으로 한다.
4. AI 심문 화면은 AI_NPC_PROMPT_POLICY.md의 답변 제약을 따른다.
5. 공식 시나리오 데이터는 OFFICIAL_SCENARIO_DEMO_DAY.md를 기준으로 한다.
6. 1차 MVP에서는 인증/거래/크레딧을 필수 흐름으로 만들지 않는다.
7. sessionId, scenarioId, evidenceId, suspectId 필드명을 임의로 id로 축약하지 않는다.
```

### 10.11 최종 결정사항

최종 프론트 연동 기준은 아래로 고정한다.

```text
1. 현재 구현 API는 이 문서를 기준으로 연동한다.
2. 기존 Android/API spec 문서의 미구현 API는 바로 호출하지 않는다.
3. 모든 목록/상세/제출 화면에 loading, empty, error 상태를 둔다.
4. 이미지 URL이 없으면 placeholder를 사용하고 S3 URL을 임의 조립하지 않는다.
5. 최종 추리 제출 중에는 버튼을 잠그고 중복 제출을 막는다.
6. 결과 데이터는 결과 화면에서만 표시한다.
7. 추후 백엔드 API가 추가되면 이 문서를 먼저 갱신한 뒤 프론트 연동을 바꾼다.
```

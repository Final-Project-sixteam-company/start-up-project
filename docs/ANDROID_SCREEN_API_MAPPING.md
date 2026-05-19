# CaseLab AI - Android Screen API Mapping

> 문서 목적: Android 화면이 어떤 API를 호출하는지 정리한다.
> 이 문서는 화면-API 매핑만 관리하고, Request/Response JSON 예시는 관리하지 않는다.

---

## 0. 정본 원칙

### 0.1 이 문서가 관리하는 것

```text
화면 ID
화면 이름
사용자 액션
호출 API
화면에서 필요한 주요 필드
API Spec 참조 위치
MVP 여부
```

### 0.2 이 문서가 관리하지 않는 것

아래 내용은 `CaseLab_AI_API_Spec.md`가 정본이다.

```text
Request JSON
Response JSON
DTO 필드 타입
공통 ApiResponse 구조
에러 코드
Enum 상세
인증 Header 상세
```

Android 문서에 API 응답 JSON을 복사하지 않는다.
화면 구현에 필요한 필드명만 적고, DTO 상세는 API Spec을 따른다.

### 0.3 참조 문서

| 문서 | 용도 |
|---|---|
| `CaseLab_AI_PRD.md` | 화면 목적, MVP 범위, 기능 우선순위 |
| `CaseLab_AI_API_Spec.md` | API 경로, Request/Response, DTO 필드명 정본 |
| `AI_NPC_PROMPT_POLICY.md` | AI 심문/답변 정책 |
| `OFFICIAL_SCENARIO_DEMO_DAY.md` | 공식 시나리오 seed data |

---

## 1. Android 공통 규칙

### 1.1 API Base URL

| 환경 | Base URL |
|---|---|
| Android Emulator 로컬 개발 | `http://10.0.2.2:8080` |
| 배포 환경 | `https://api.caselab.ai` |

Base URL에는 `/api`를 넣지 않는다.
실제 API 경로는 `/api/...` prefix를 포함한다.

### 1.2 임시 사용자 처리

초기 MVP에서는 로그인 없이 진행한다.

```text
MOCK_USER_ID = 1
```

Android는 초반에 Authorization Header 없이 API를 호출할 수 있다.
백엔드는 `MockUserProvider.currentUserId()`를 사용한다.

JWT 인증 도입 후에는 Android가 다음 Header를 추가한다.

```text
Authorization: Bearer {accessToken}
```

### 1.3 ID 필드명 규칙

API DTO의 ID 필드명은 `CaseLab_AI_API_Spec.md`를 따른다.

| 대상 | API DTO 필드명 |
|---|---|
| User | `userId` |
| Scenario | `scenarioId` |
| PlaySession | `sessionId` |
| Suspect | `suspectId` |
| Evidence | `evidenceId` |
| Hint | `hintId` |
| Review | `reviewId` |

엔티티 내부 PK가 `id`여도 Android 응답 DTO에서는 위 이름을 우선 사용한다.

---

## 2. Android 화면 구조

### 2.1 Bottom Navigation

```text
홈
시나리오
제작
내 기록
마이페이지
```

`내 기록`, `마이페이지`는 인증 도입 후 완성한다. MVP에서는 Mock 또는 비활성 상태로 둘 수 있다.

### 2.2 게임 플레이 내부 탭

플레이 세션이 시작된 뒤 탐정 대시보드 내부에서 다음 탭을 사용한다.

```text
현장
증거
용의자
타임라인
추리 제출
```

---

## 3. 화면별 API 매핑 요약

| 화면 | 주요 목적 | 핵심 API | MVP |
|---|---|---|---|
| 스플래시 | 앱 진입 | 없음 | 선택 |
| 온보딩 | 서비스 설명 | 없음 | 선택 |
| 홈 | 추천/인기 사건 표시 | `GET /api/scenarios` | O |
| 시나리오 라이브러리 | 시나리오 검색/필터 | `GET /api/scenarios` | O |
| 시나리오 상세 | 사건 상세 확인 | `GET /api/scenarios/{scenarioId}` | O |
| 사건 시작 | 플레이 세션 생성 | `POST /api/play-sessions` | O |
| 사건 브리핑 | 게임 시작 전 브리핑 | `GET /api/scenarios/{scenarioId}` | O |
| 탐정 대시보드 | 플레이 상태 허브 | `GET /api/play-sessions/{sessionId}/dashboard` | O |
| 현장 정보 | 장소/단면도/증거 위치 | `GET /api/play-sessions/{sessionId}/locations` | O |
| 증거 보드 | 해금 증거 목록 | `GET /api/play-sessions/{sessionId}/evidences` | O |
| 증거 상세 | 증거 상세 확인 | `GET /api/play-sessions/{sessionId}/evidences/{evidenceId}` | O |
| 용의자 목록 | 용의자 목록 조회 | `GET /api/play-sessions/{sessionId}/suspects` | O |
| 용의자 상세 | 프로필/알리바이 확인 | `GET /api/play-sessions/{sessionId}/suspects/{suspectId}` | O |
| 심문 채팅 | AI 용의자 심문 | `POST /api/play-sessions/{sessionId}/interrogations` | O |
| 심문 로그 | 기존 대화 조회 | `GET /api/play-sessions/{sessionId}/interrogations` | O |
| 추천 질문 | 질문 버튼 표시 | `GET /api/play-sessions/{sessionId}/recommended-questions` | 선택 |
| 증거 제시 모달 | 심문 중 증거 선택 | `GET /api/play-sessions/{sessionId}/evidences` | O |
| 타임라인 | 사건 시간 흐름 | `GET /api/play-sessions/{sessionId}/timeline` | O |
| 힌트 | 힌트 조회/사용 | `GET /api/play-sessions/{sessionId}/hints`, `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` | O |
| 최종 추리 제출 | 정답 제출 | `POST /api/play-sessions/{sessionId}/final-deduction` | O |
| 결과/해설 | 채점 결과 확인 | `GET /api/play-sessions/{sessionId}/result` | O |
| 커스텀 제작 | 시나리오 제작 | `POST /api/scenarios`, 하위 리소스 API | O |
| AI 검증 | 시나리오 논리 검증 | `POST /api/ai/scenarios/{scenarioId}/validate` | O |
| 리뷰 | 리뷰 조회/작성 | `GET/POST /api/scenarios/{scenarioId}/reviews` | O |
| 북마크 | 시나리오 북마크 | `POST/DELETE /api/scenarios/{scenarioId}/bookmarks` | O |
| AI 초안 생성 | 시나리오 초안 생성 | `POST /api/ai/scenarios/draft` | 2차 |
| 내 기록 | 플레이/제작 기록 | `GET /api/play-sessions/me`, `GET /api/scenarios/me`, `GET /api/scenarios/bookmarked` | 2차 |
| 마이페이지 | 내 정보 | `GET /api/users/me` | 인증 후 |
| 구매/크레딧 | 거래 확장 | 거래/크레딧 API | 후순위 |

---

## 4. 앱 진입 화면

### 4.1 스플래시

| 항목 | 내용 |
|---|---|
| 목적 | 앱 로고와 분위기 표시 후 온보딩 또는 홈으로 이동 |
| 호출 API | 없음 |
| 상태 | MVP 생략 가능 |

### 4.2 온보딩

| 항목 | 내용 |
|---|---|
| 목적 | 사건 선택, 증거 분석, AI 심문, 최종 추리 흐름 안내 |
| 호출 API | 없음 |
| 상태 | MVP 생략 가능 |

---

## 5. 시나리오 탐색 화면

### 5.1 홈

| 항목 | 내용 |
|---|---|
| 목적 | 추천/인기/최신 시나리오 진입 |
| 호출 API | `GET /api/scenarios?sort=popular&page=0&size=5` |
| 선택 API | `GET /api/scenarios?sort=latest&page=0&size=5` |
| 후순위 API | `GET /api/play-sessions/me?status=PLAYING&page=0&size=3` |
| 필요한 필드 | `scenarioId`, `title`, `description`, `difficulty`, `estimatedPlayTimeMinutes`, `averageRating`, `playCount`, `thumbnailUrl`, `scenarioType` |
| API Spec | 6.1 시나리오 목록 조회, 11.3 내 플레이 기록 조회 |

최근 플레이 기록은 인증/내 기록 기능이 준비되기 전까지 Mock 데이터 또는 미노출로 처리한다.

### 5.2 시나리오 라이브러리

| 항목 | 내용 |
|---|---|
| 목적 | 공식/커스텀 시나리오 검색 및 필터 |
| 호출 API | `GET /api/scenarios` |
| 주요 Query | `type`, `difficulty`, `sort`, `keyword`, `page`, `size` |
| 필요한 필드 | `scenarioId`, `title`, `description`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `averageRating`, `scenarioType`, `isBookmarked` |
| API Spec | 6.1 시나리오 목록 조회 |

### 5.3 시나리오 상세

| 항목 | 내용 |
|---|---|
| 목적 | 사건 정보 확인 후 플레이 시작 또는 북마크/리뷰 확인 |
| 호출 API | `GET /api/scenarios/{scenarioId}` |
| 선택 API | `GET /api/scenarios/{scenarioId}/reviews` |
| 액션 API | `POST /api/play-sessions`, `POST /api/scenarios/{scenarioId}/bookmarks`, `DELETE /api/scenarios/{scenarioId}/bookmarks` |
| 필요한 필드 | `scenarioId`, `title`, `description`, `synopsis`, `difficulty`, `estimatedPlayTimeMinutes`, `suspectCount`, `evidenceCount`, `averageRating`, `ratingCount`, `isBookmarked`, `scenarioType`, `visibility`, `canPlay` |
| API Spec | 6.2 시나리오 상세 조회, 9.1 게임 세션 시작, 12.1~12.4 커뮤니티 API |

---

## 6. 게임 플레이 화면

### 6.1 사건 시작 / 사건 브리핑

| 항목 | 내용 |
|---|---|
| 목적 | 사용자가 선택한 시나리오로 플레이 세션 생성 |
| 호출 API | `POST /api/play-sessions` |
| 보조 API | `GET /api/scenarios/{scenarioId}` |
| 필요한 필드 | `sessionId`, `scenarioId`, `status`, `startedAt` |
| API Spec | 6.2 시나리오 상세 조회, 9.1 게임 세션 시작 |

세션 시작 후 Android는 `sessionId`를 화면 이동 인자로 보관한다.

### 6.2 탐정 대시보드

| 항목 | 내용 |
|---|---|
| 목적 | 플레이 상태와 주요 진입점을 표시 |
| 호출 API | `GET /api/play-sessions/{sessionId}/dashboard` |
| 필요한 필드 | `sessionId`, `scenarioId`, `scenarioTitle`, `status`, `elapsedSeconds`, `unlockedEvidenceCount`, `totalEvidenceCount`, `hintUsedCount`, `interrogationCount`, `briefing` |
| API Spec | 9.2 탐정 대시보드 조회 |

### 6.3 현장 정보

| 항목 | 내용 |
|---|---|
| 목적 | 장소 정보, 단면도, 증거 위치 표시 |
| 호출 API | `GET /api/play-sessions/{sessionId}/locations` |
| 필요한 필드 | `locationId`, `name`, `description`, `mapX`, `mapY`, `evidenceCount`, `evidences` |
| API Spec | 9.3 현장 정보 조회 |

### 6.4 증거 보드

| 항목 | 내용 |
|---|---|
| 목적 | 현재 해금된 증거와 잠긴 증거 상태 표시 |
| 호출 API | `GET /api/play-sessions/{sessionId}/evidences` |
| 주요 Query | `importance`, `unlockStatus` |
| 필요한 필드 | `evidenceId`, `title`, `description`, `locationName`, `importance`, `isUnlocked`, `unlockHint`, `relatedSuspects` |
| API Spec | 9.4 현재 해금된 증거 목록 조회 |

`evidenceId`가 증거 카드의 식별자다. Android 화면 모델에서 단순 `id`로 바꿔 저장하지 않는다.

### 6.5 증거 상세

| 항목 | 내용 |
|---|---|
| 목적 | 증거 설명, 발견 위치, 관련 용의자 확인 |
| 호출 API | `GET /api/play-sessions/{sessionId}/evidences/{evidenceId}` |
| 필요한 필드 | `evidenceId`, `title`, `description`, `location`, `importance`, `relatedSuspects`, `relatedTimelineEvents` |
| API Spec | 9.5 증거 상세 조회 |

### 6.6 용의자 목록

| 항목 | 내용 |
|---|---|
| 목적 | 심문 가능한 용의자 목록 표시 |
| 호출 API | `GET /api/play-sessions/{sessionId}/suspects` |
| 필요한 필드 | `suspectId`, `name`, `role`, `relationToVictim`, `publicStatement`, `alibi`, `suspicionLevel`, `interrogationCount` |
| API Spec | 9.7 용의자 목록 조회 |

화면 라벨은 "공개 알리바이"로 표시할 수 있지만 API 필드명은 `alibi`를 사용한다.

### 6.7 용의자 상세

| 항목 | 내용 |
|---|---|
| 목적 | 용의자 공개 프로필과 공개 알리바이 확인 |
| 호출 API | `GET /api/play-sessions/{sessionId}/suspects/{suspectId}` |
| 필요한 필드 | `suspectId`, `name`, `role`, `relationToVictim`, `publicProfile`, `publicStatement`, `alibi`, `relatedEvidences`, `interrogationLogs` |
| API Spec | 9.8 용의자 상세 조회 |

### 6.8 심문 채팅

| 항목 | 내용 |
|---|---|
| 목적 | AI 용의자에게 질문하고 답변 로그 표시 |
| 진입 API | `GET /api/play-sessions/{sessionId}/interrogations` |
| 선택 API | `GET /api/play-sessions/{sessionId}/recommended-questions` |
| 질문 API | `POST /api/play-sessions/{sessionId}/interrogations` |
| 필요한 필드 | `interrogationId`, `suspectId`, `suspectName`, `questionType`, `question`, `answer`, `presentedEvidence`, `unlockedEvidences`, `createdAt` |
| API Spec | 10.1 AI 용의자 심문, 10.2 증거 제시 심문, 10.3 심문 로그 조회, 10.4 추천 질문 조회 |

Android는 자유 질문과 증거 제시 질문을 같은 심문 API로 보낸다.
Request 상세는 API Spec을 따른다.

### 6.9 증거 제시 모달

| 항목 | 내용 |
|---|---|
| 목적 | 심문 중 제시할 증거 선택 |
| 호출 API | `GET /api/play-sessions/{sessionId}/evidences` |
| 필요한 필드 | `evidenceId`, `name`, `summary`, `importance`, `isUnlocked` |
| 반환값 | 선택한 `evidenceId` 목록 |
| API Spec | 9.4 현재 해금된 증거 목록 조회 |

잠긴 증거는 선택할 수 없게 처리한다.

### 6.10 타임라인

| 항목 | 내용 |
|---|---|
| 목적 | 사건 발생 전후 시간 흐름 확인 |
| 호출 API | `GET /api/play-sessions/{sessionId}/timeline` |
| 필요한 필드 | `time`, `title`, `description`, `eventType`, `isTrueEvent`, `relatedEvidenceId` |
| API Spec | 9.9 타임라인 조회 |

### 6.11 힌트

| 항목 | 내용 |
|---|---|
| 목적 | 사용 가능한 힌트 확인 및 사용 |
| 목록 API | `GET /api/play-sessions/{sessionId}/hints` |
| 사용 API | `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` |
| 필요한 필드 | `hintId`, `hintLevel`, `content`, `isAvailable`, `isUsed`, `unlockAfterMinutes`, `penaltyScore`, `usedAt` |
| API Spec | 9.10 힌트 목록 조회, 9.11 힌트 사용 |

힌트 사용 API는 `POST /api/play-sessions/{sessionId}/hints/{hintId}/use`로 통일한다.

### 6.12 최종 추리 제출

| 항목 | 내용 |
|---|---|
| 목적 | 범인, 동기, 방법, 은폐, 결정적 증거 제출 |
| 호출 API | `POST /api/play-sessions/{sessionId}/final-deduction` |
| 필요한 입력 | `selectedCulpritId`, `motiveText`, `methodText`, `coverUpText`, `selectedEvidenceIds` |
| 결과 필드 | `finalDeductionId`, `score`, `grade`, `feedbackSummary`, `resultAvailable`, `submittedAt` |
| API Spec | 11.1 최종 추리 제출 |

중복 제출은 서버 정책을 따른다. Android는 실패 응답을 공통 에러 UI로 처리한다.

### 6.13 결과 / 해설

| 항목 | 내용 |
|---|---|
| 목적 | 채점 결과, 정답 해설, 놓친 증거 표시 |
| 호출 API | `GET /api/play-sessions/{sessionId}/result` |
| 필요한 필드 | `sessionId`, `score`, `grade`, `correctCulprit`, `matched`, `matchedParts`, `missedParts`, `feedback`, `fullExplanation`, `keyEvidences`, `nextRecommendedScenarios` |
| API Spec | 11.2 결과 / 해설 조회 |

정답 상세는 결과/해설 화면에서만 노출한다.

---

## 7. 커스텀 시나리오 제작 화면

### 7.1 제작 Step

```text
1. 기본 정보 입력
2. 장소 등록
3. 피해자 등록
4. 용의자 등록
5. 증거 등록
6. 힌트 등록
7. 정답 등록
8. AI 검증
9. 공개 등록
```

### 7.2 Step별 API

| Step | 사용자 액션 | 호출 API | 필요한 주요 필드 | API Spec |
|---|---|---|---|---|
| 기본 정보 | 시나리오 생성 | `POST /api/scenarios` | `title`, `description`, `synopsis`, `scenarioType`, `visibility`, `difficulty`, `playerCountMin`, `playerCountMax`, `estimatedPlayTimeMinutes` | 6.3 |
| 기본 정보 | 시나리오 수정 | `PATCH /api/scenarios/{scenarioId}` | 수정된 기본 정보 | 6.4 |
| 장소 | 장소 등록 | `POST /api/scenarios/{scenarioId}/locations` | `name`, `description`, `mapX`, `mapY`, `sortOrder` | 7.1 |
| 피해자 | 피해자 등록 | `POST /api/scenarios/{scenarioId}/victim` | `name`, `age`, `role`, `description`, `causeOfDeath`, `foundLocationId`, `foundCondition` | 7.3 |
| 용의자 | 용의자 등록 | `POST /api/scenarios/{scenarioId}/suspects` | `name`, `role`, `relationToVictim`, `publicProfile`, `publicStatement`, `alibi`, `personalityPrompt`, `responsePolicyJson`, `suspicionLevel` | 7.4 |
| 증거 | 증거 등록 | `POST /api/scenarios/{scenarioId}/evidences` | `locationId`, `title`, `description`, `evidenceType`, `importance`, `imageUrl`, `isInitialPublic`, `unlockType`, `unlockAfterMinutes`, `relatedSuspectIds` | 7.6 |
| 힌트 | 힌트 등록 | `POST /api/scenarios/{scenarioId}/hints` | `hintLevel`, `content`, `unlockAfterMinutes`, `penaltyScore` | 7.8 |
| 정답 | 정답 등록 | `POST /api/scenarios/{scenarioId}/solution` | `culpritSuspectId`, `motive`, `method`, `coverUp`, `keyEvidenceIds`, `fullExplanation` | 7.9 |
| 검증 | AI 검증 | `POST /api/ai/scenarios/{scenarioId}/validate` | `scenarioId` | 8.2 |
| 검증 결과 | 결과 확인 | `GET /api/scenarios/{scenarioId}/validation-result` | `validationStatus`, `issues`, `suggestions` | 8.2 |
| 공개 | 공개 등록 | `POST /api/scenarios/{scenarioId}/publish` | `visibility` | 6.5 |

AI 검증은 1차 MVP에 포함한다.
AI 시나리오 초안 생성은 2차 기능이다.

---

## 8. 커뮤니티 화면

### 8.1 리뷰

| 항목 | 내용 |
|---|---|
| 리뷰 목록 | `GET /api/scenarios/{scenarioId}/reviews` |
| 리뷰 작성 | `POST /api/scenarios/{scenarioId}/reviews` |
| 필요한 필드 | `reviewId`, `user`, `rating`, `content`, `isSpoiler`, `createdAt` |
| API Spec | 12.3 리뷰 작성, 12.4 리뷰 목록 조회 |

스포일러 리뷰는 접어서 표시한다.

### 8.2 북마크

| 항목 | 내용 |
|---|---|
| 추가 | `POST /api/scenarios/{scenarioId}/bookmarks` |
| 해제 | `DELETE /api/scenarios/{scenarioId}/bookmarks` |
| 필요한 필드 | `scenarioId`, `isBookmarked` |
| API Spec | 12.1 시나리오 북마크, 12.2 북마크 취소 |

---

## 9. 후순위 화면

### 9.1 AI 시나리오 생성 화면

| 항목 | 내용 |
|---|---|
| 상태 | 2차 기능 |
| 호출 API | `POST /api/ai/scenarios/draft` |
| 필요한 필드 | `draftId`, `title`, `synopsis`, `suspects`, `evidences`, `warnings` |
| API Spec | 8.1 AI 시나리오 초안 생성 |

1차 MVP에서는 버튼을 숨기거나 비활성화한다.

### 9.2 내 기록

| 항목 | 내용 |
|---|---|
| 상태 | 2차 기능 |
| 플레이 기록 | `GET /api/play-sessions/me` |
| 내가 만든 시나리오 | `GET /api/scenarios/me` |
| 북마크 목록 | `GET /api/scenarios/bookmarked` |
| API Spec | 11.3 내 플레이 기록 조회, 4.2 시나리오 라이브러리 API |

### 9.3 마이페이지

| 항목 | 내용 |
|---|---|
| 상태 | 인증 도입 후 |
| 내 정보 | `GET /api/users/me` |
| 내 정보 수정 | `PATCH /api/users/me` |
| API Spec | 5.3 내 정보 조회 |

### 9.4 구매 / 크레딧

| 항목 | 내용 |
|---|---|
| 상태 | 후순위 |
| 기준 | 인증/인가와 거래/크레딧 설계 확정 후 추가 |
| 주의 | 1차 MVP 화면에서는 구매/결제처럼 보이는 CTA를 노출하지 않는다 |

---

## 10. 화면별 네비게이션 플로우

### 10.1 공식 시나리오 플레이

```text
홈 또는 시나리오 라이브러리
→ 시나리오 상세
→ 사건 시작
→ 사건 브리핑
→ 탐정 대시보드
→ 현장 / 증거 / 용의자 / 타임라인 탐색
→ 심문 채팅
→ 힌트 사용
→ 최종 추리 제출
→ 결과 / 해설
```

### 10.2 커스텀 시나리오 제작

```text
제작 탭
→ 기본 정보 입력
→ 장소 / 피해자 / 용의자 / 증거 / 힌트 / 정답 등록
→ AI 검증
→ 공개 등록
→ 시나리오 상세
```

### 10.3 이어하기

```text
홈 또는 내 기록
→ 진행 중 플레이 세션 선택
→ 탐정 대시보드
```

이어하기는 2차에서 완성한다. MVP에서는 최근 플레이 Mock 또는 미노출로 처리할 수 있다.

---

## 11. Android 구현 우선순위

### 11.1 1순위: 공식 시나리오 플레이

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

### 11.2 2순위: 커스텀 제작 기본형

```text
시나리오 생성 / 수정
장소 / 피해자 / 용의자 / 증거 / 힌트 / 정답 등록
AI 검증
공개 등록
```

### 11.3 3순위: 커뮤니티 기본형

```text
리뷰 조회 / 작성
북마크 추가 / 해제
```

### 11.4 4순위: 후순위 기능

```text
내 기록
마이페이지
AI 시나리오 초안 생성
인증 / JWT
구매 / 크레딧
```

---

## 12. Android 개발 AI 규칙

Android 개발 AI는 다음 규칙을 따른다.

```text
1. 이 문서의 API 응답 예시를 만들지 않는다.
2. DTO 필드명은 CaseLab_AI_API_Spec.md를 기준으로 한다.
3. 화면 목적과 MVP 범위는 CaseLab_AI_PRD.md를 기준으로 한다.
4. AI 심문 화면은 AI_NPC_PROMPT_POLICY.md의 답변 제약을 따른다.
5. 공식 시나리오 데이터는 OFFICIAL_SCENARIO_DEMO_DAY.md를 기준으로 한다.
6. 1차 MVP에서는 인증/거래/크레딧을 필수 흐름으로 만들지 않는다.
7. `sessionId`, `scenarioId`, `evidenceId`, `suspectId` 필드명을 임의로 `id`로 축약하지 않는다.
```

---

## 13. 체크리스트

- [ ] Android 화면에서 호출할 API가 이 문서에 매핑되어 있는가?
- [ ] Request/Response JSON은 `CaseLab_AI_API_Spec.md`에만 존재하는가?
- [ ] `ANDROID_SCREEN_API_MAPPING.md`에 전체 응답 JSON을 복사하지 않았는가?
- [ ] Base URL에 `/api`가 중복 포함되어 있지 않은가?
- [ ] 실제 API path에는 `/api/...` prefix가 포함되어 있는가?
- [ ] DTO ID 필드명이 API Spec과 일치하는가?
- [ ] AI 시나리오 초안 생성이 1차 MVP처럼 보이지 않는가?
- [ ] 인증/거래/크레딧 화면이 후순위로 표시되어 있는가?

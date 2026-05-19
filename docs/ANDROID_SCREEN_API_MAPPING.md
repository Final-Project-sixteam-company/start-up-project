# CaseLab AI - Android Screen API Mapping

> 문서 목적: Android 담당자와 Android Studio/IntelliJ에 연결된 AI가 **화면별로 어떤 API를 호출해야 하는지** 빠르게 이해하도록 정리한 문서  
> 기준 문서: `CaseLab_AI_PRD.md`, `CaseLab_AI_Project_Planning.md`, `CaseLab_AI_API_Spec.md`, `OFFICIAL_SCENARIO_DEMO_DAY.md`  
> 기준 플랫폼: Android App + Spring Boot Backend  
> 현재 개발 단계: 인증/거래는 점진 확장, 우선 핵심 게임 플레이 기능 구현

---

## 0. 핵심 원칙

### 0.1 현재 우선순위

현재 Android 앱은 아래 흐름이 먼저 동작해야 한다.

```text
시나리오 목록
→ 시나리오 상세
→ 게임 시작
→ 사건 브리핑 / 탐정 대시보드
→ 증거 조회
→ 용의자 조회
→ AI 심문
→ 힌트 확인
→ 최종 추리 제출
→ 결과 해설
```

인증/거래/유료 시나리오/정산은 이후 단계에서 확장한다.

---

### 0.2 임시 사용자 처리

초기 개발 단계에서는 로그인 없이 진행할 수 있다.

```text
MOCK_USER_ID = 1
```

Android는 초반에 Authorization Header 없이 API를 호출할 수 있다.  
백엔드는 `MockUserProvider.currentUserId()`를 사용한다.

추후 JWT 인증이 추가되면 Android는 다음 Header를 추가한다.

```http
Authorization: Bearer {accessToken}
```

---

### 0.3 API Base URL

로컬 개발:

```text
http://10.0.2.2:8080
```

Android Emulator에서 로컬 PC의 백엔드를 호출할 때는 `localhost`가 아니라 `10.0.2.2`를 사용한다.

배포 환경:

```text
https://api.caselab.ai
```

실제 API 호출 경로는 `/api/...` prefix를 포함한다.

---

### 0.4 공통 응답 형식

모든 API는 다음 형식을 기본으로 한다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 응답:

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

Android에서는 `success == false`이면 공통 에러 처리로 Toast, Snackbar, Dialog 중 하나를 띄운다.

이 문서의 화면별 `응답 데이터 예시`는 가독성을 위해 대부분 `ApiResponse.data` 내부 값만 표기한다. 실제 Retrofit DTO는 `success`, `data`, `error` 공통 래퍼를 먼저 받은 뒤 `data`를 화면 모델로 매핑한다.

---

## 1. Android 화면 구조

### 1.1 Bottom Navigation

Android 앱의 기본 하단 탭은 다음과 같다.

```text
홈
시나리오
제작
내 기록
마이페이지
```

### 1.2 게임 플레이 내부 탭

게임 세션이 시작된 뒤에는 탐정 대시보드 내부에서 다음 탭을 사용한다.

```text
현장
증거
용의자
타임라인
추리제출
```

---

## 2. 화면별 API 매핑 요약표

| 화면 | 주요 목적 | 핵심 API | MVP 여부 |
|---|---|---|---|
| 스플래시 | 앱 진입 | 없음 | 선택 |
| 온보딩 | 서비스 설명 | 없음 | 선택 |
| 홈 | 추천/인기 사건 표시 | `GET /api/scenarios` | MVP |
| 시나리오 라이브러리 | 시나리오 검색/필터 | `GET /api/scenarios` | MVP |
| 시나리오 상세 | 사건 상세 확인 | `GET /api/scenarios/{scenarioId}` | MVP |
| 사건 브리핑 | 게임 시작 전 브리핑 | `GET /api/scenarios/{scenarioId}`, `POST /api/play-sessions` | MVP |
| 탐정 대시보드 | 플레이 상태 허브 | `GET /api/play-sessions/{sessionId}/dashboard` | MVP |
| 현장 정보 | 장소/단면도/증거 위치 | `GET /api/play-sessions/{sessionId}/locations` | MVP |
| 증거 보드 | 증거 목록 조회 | `GET /api/play-sessions/{sessionId}/evidences` | MVP |
| 증거 상세 | 증거 상세 확인 | `GET /api/play-sessions/{sessionId}/evidences/{evidenceId}` | MVP |
| 용의자 목록 | 용의자 목록 조회 | `GET /api/play-sessions/{sessionId}/suspects` | MVP |
| 용의자 상세 | 용의자 상세/알리바이 | `GET /api/play-sessions/{sessionId}/suspects/{suspectId}` | MVP |
| 심문 채팅 | AI 용의자 심문 | `POST /api/play-sessions/{sessionId}/interrogations`, `GET /api/play-sessions/{sessionId}/interrogations` | MVP |
| 증거 제시 모달 | 심문 중 증거 선택 | `GET /api/play-sessions/{sessionId}/evidences` | MVP |
| 타임라인 | 사건 시간 흐름 | `GET /api/play-sessions/{sessionId}/timeline` | MVP |
| 힌트 | 힌트 조회/사용 | `GET /api/play-sessions/{sessionId}/hints`, `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` | MVP |
| 최종 추리 제출 | 정답 제출 | `POST /api/play-sessions/{sessionId}/final-deduction` | MVP |
| 결과/해설 | 채점 결과 확인 | `GET /api/play-sessions/{sessionId}/result` | MVP |
| 커스텀 제작 | 시나리오 제작 | `POST/PATCH /api/scenarios`, 하위 리소스 API | MVP |
| AI 초안 생성 | 시나리오 초안 생성 | `POST /api/ai/scenarios/draft` | 2차 |
| AI 검증 | 시나리오 검증 | `POST /api/ai/scenarios/{scenarioId}/validate` | MVP |
| 리뷰 | 리뷰 조회/작성 | `GET/POST /api/scenarios/{scenarioId}/reviews` | MVP |
| 북마크 | 시나리오 북마크 | `POST/DELETE /api/scenarios/{scenarioId}/bookmarks` | MVP |
| 마이페이지 | 내 정보 | `GET /api/users/me` | 인증 후 |
| 구매/크레딧 | 거래 확장 | `POST /api/scenarios/{scenarioId}/purchase`, `GET /api/wallet` | 후순위 |

---

## 3. 스플래시 화면

### 목적

앱 로고와 다크 미스터리 분위기를 보여주고 홈 또는 온보딩으로 이동한다.

### API

없음.

### 화면 동작

```text
앱 실행
→ 로고 표시
→ 최초 실행이면 온보딩
→ 아니면 홈
```

### Android 구현 메모

- 로컬 SharedPreferences 또는 DataStore에 온보딩 완료 여부 저장
- MVP에서는 생략 가능

---

## 4. 온보딩 화면

### 목적

CaseLab AI의 플레이 방식을 간단히 설명한다.

### API

없음.

### 화면 내용

```text
1. 사건을 선택하세요.
2. 증거를 분석하세요.
3. AI 용의자를 심문하세요.
4. 최종 추리를 제출하세요.
5. 직접 사건을 만들고 공유하세요.
```

### Android 구현 메모

- ViewPager 또는 Compose Pager 형태
- MVP에서는 홈 화면의 배너로 대체 가능

---

## 5. 홈 화면

### 목적

사용자에게 바로 플레이할 수 있는 시나리오와 최근 기록을 보여준다.

### 호출 API

#### 5.1 추천/인기 시나리오

```http
GET /api/scenarios?sort=popular&page=0&size=5
```

#### 5.2 최신 시나리오

```http
GET /api/scenarios?sort=latest&page=0&size=5
```

#### 5.3 최근 플레이 기록

```http
GET /api/play-sessions/me?status=PLAYING&page=0&size=3
```

인증 전 MVP에서는 이 API를 생략하거나 Mock 데이터로 처리할 수 있다.

### 화면 표시 데이터

```text
오늘의 추천 사건
인기 커스텀 시나리오
최근 플레이한 사건
공식 시나리오 바로가기
커스텀 제작 시작 버튼
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 추천 사건 카드 클릭 | 시나리오 상세 화면 |
| 시나리오 더보기 | 시나리오 라이브러리 화면 |
| 커스텀 제작 시작 | 커스텀 시나리오 제작 화면 |
| 최근 플레이 이어하기 | 해당 playSession의 탐정 대시보드 |

### 화면 상태

```text
Loading
Success
Empty
Error
```

---

## 6. 시나리오 라이브러리 화면

### 목적

공식/커스텀 시나리오를 검색하고 필터링한다.

### 호출 API

```http
GET /api/scenarios?type={type}&difficulty={difficulty}&sort={sort}&page={page}&size={size}
```

### Query Parameter

| 이름 | 예시 | 설명 |
|---|---|---|
| type | OFFICIAL, CUSTOM | 시나리오 유형 |
| difficulty | EASY, NORMAL, HARD | 난이도 |
| sort | popular, latest, rating | 정렬 기준 |
| page | 0 | 페이지 |
| size | 20 | 페이지 크기 |
| keyword | 데모데이 | 검색어 |
| minPlayers | 1 | 최소 인원 |
| maxPlayers | 3 | 최대 인원 |

### 화면 표시 데이터

```text
검색바
필터 칩
시나리오 카드 목록
무한 스크롤 또는 페이지네이션
```

### Scenario Card 필드

```json
{
  "scenarioId": 1,
  "title": "데모데이 전야 살인사건",
  "description": "데모데이 전날, 스타트업 대표가 사망했다.",
  "thumbnailUrl": "...",
  "scenarioType": "OFFICIAL",
  "difficulty": "NORMAL",
  "estimatedPlayTimeMinutes": 30,
  "suspectCount": 5,
  "evidenceCount": 15,
  "playCount": 120,
  "averageRating": 4.7,
  "isBookmarked": false
}
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 시나리오 카드 클릭 | 시나리오 상세 |
| 필터 변경 | `GET /api/scenarios` 재호출 |
| 검색어 입력 | debounce 후 API 재호출 |
| 북마크 클릭 | `POST/DELETE /api/scenarios/{scenarioId}/bookmarks` |

---

## 7. 시나리오 상세 화면

### 목적

시나리오를 플레이하기 전에 사건 정보와 난이도를 확인한다.

### 호출 API

```http
GET /api/scenarios/{scenarioId}
```

### 응답 데이터 예시

```json
{
  "scenarioId": 1,
  "title": "데모데이 전야 살인사건",
  "synopsis": "AI 스타트업 대표가 데모데이 전날 밤 사망했다.",
  "scenarioType": "OFFICIAL",
  "difficulty": "NORMAL",
  "estimatedPlayTimeMinutes": 30,
  "playerCountMin": 1,
  "playerCountMax": 1,
  "suspectCount": 5,
  "evidenceCount": 15,
  "hintCount": 3,
  "averageRating": 4.7,
  "playCount": 120,
  "tags": ["스타트업", "독살", "디지털증거"],
  "isBookmarked": false,
  "canPlay": true
}
```

### 주요 UI 액션

| 액션 | 호출 |
|---|---|
| 시작하기 | `POST /api/play-sessions` |
| 북마크 | `POST /api/scenarios/{scenarioId}/bookmarks` |
| 북마크 해제 | `DELETE /api/scenarios/{scenarioId}/bookmarks` |
| 리뷰 보기 | `GET /api/scenarios/{scenarioId}/reviews` |

### 시작하기 Request

```http
POST /api/play-sessions
```

```json
{
  "scenarioId": 1
}
```

### 시작하기 Response

```json
{
  "sessionId": 1001,
  "scenarioId": 1,
  "status": "PLAYING",
  "startedAt": "2026-05-18T20:00:00"
}
```

성공 후 사건 브리핑 또는 탐정 대시보드로 이동한다.

---

## 8. 사건 브리핑 화면

### 목적

게임 시작 직후 사건의 기본 정보를 전달한다.

### 호출 API

선택 1: 시나리오 상세 데이터를 재사용한다.

```http
GET /api/scenarios/{scenarioId}
```

선택 2: 게임 세션 대시보드에서 브리핑을 가져온다.

```http
GET /api/play-sessions/{sessionId}/dashboard
```

MVP에서는 `dashboard` 응답에 브리핑 정보를 포함하는 것을 추천한다.

### 화면 표시 데이터

```text
사건 제목
사건 개요
피해자 정보
발견 장소
초기 공개 증거
탐정 목표
조사 시작 버튼
```

### 주요 UI 액션

| 액션 | 이동 |
|---|---|
| 조사 시작 | 탐정 대시보드 |
| 초기 증거 클릭 | 증거 상세 |
| 용의자 보기 | 용의자 탭 |

---

## 9. 탐정 대시보드 화면

### 목적

게임 플레이 중 핵심 허브 화면이다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/dashboard
```

### 응답 데이터 예시

```json
{
  "sessionId": 1001,
  "scenarioId": 1,
  "scenarioTitle": "데모데이 전야 살인사건",
  "status": "PLAYING",
  "elapsedSeconds": 720,
  "unlockedEvidenceCount": 7,
  "totalEvidenceCount": 15,
  "hintUsedCount": 1,
  "interrogationCount": 5,
  "briefing": {
    "victimName": "강도현",
    "foundLocation": "데모룸",
    "summary": "대표 강도현이 데모데이 전날 밤 데모룸에서 사망했다."
  }
}
```

### 화면 구성

```text
상단:
- 사건 제목
- 진행 시간
- 해금 증거 수
- 힌트 버튼

하단 탭:
- 현장
- 증거
- 용의자
- 타임라인
- 추리제출
```

### 주요 UI 액션

| 액션 | 호출 |
|---|---|
| 화면 진입/복귀 | dashboard 재조회 |
| 힌트 버튼 | 힌트 화면 |
| 최종 추리 버튼 | 최종 추리 제출 화면 |

---

## 10. 현장 정보 화면

### 목적

맵 탐색 대신 사건 현장 단면도와 증거 발견 위치를 보여준다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/locations
```

### 응답 데이터 예시

```json
{
  "locations": [
    {
      "locationId": 1,
      "name": "데모룸",
      "description": "피해자가 발견된 장소",
      "mapX": 40,
      "mapY": 55,
      "evidenceCount": 3,
      "evidences": [
        {
          "evidenceId": 1,
          "title": "마시다 만 음료 컵",
          "isUnlocked": true
        }
      ]
    }
  ]
}
```

### 화면 표시 데이터

```text
간단한 방/건물 단면도
장소별 마커
피해자 발견 위치
장소 카드
장소별 증거 목록
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 장소 마커 클릭 | 장소 카드 상세 |
| 증거 클릭 | 증거 상세 |
| 잠긴 증거 클릭 | 잠김 안내 Dialog |

---

## 11. 증거 보드 화면

### 목적

현재 해금된 증거와 잠긴 증거를 카드로 보여준다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/evidences
```

### Query Parameter

| 이름 | 예시 | 설명 |
|---|---|---|
| status | unlocked, locked, all | 증거 해금 상태 |
| importance | CORE, HIGH, NORMAL | 중요도 |
| suspectId | 1 | 관련 용의자 필터 |

### 응답 데이터 예시

```json
{
  "evidences": [
    {
      "evidenceId": 1,
      "title": "찢긴 컵 라벨",
      "description": "쓰레기통에서 발견된 라벨 조각",
      "locationName": "데모룸",
      "importance": "CORE",
      "isUnlocked": true,
      "relatedSuspects": ["박재민"]
    },
    {
      "evidenceId": 8,
      "title": "피해자 휴대폰 위치 기록",
      "description": null,
      "locationName": null,
      "importance": "HIGH",
      "isUnlocked": false,
      "unlockHint": "게임 진행 후 공개"
    }
  ]
}
```

### 화면 표시 데이터

```text
공개 증거
잠긴 증거
새로 해금된 증거
핵심 증거
페이크 가능성 있는 증거
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 증거 카드 클릭 | 증거 상세 |
| 필터 변경 | evidences API 재호출 |
| 새로 해금된 증거 확인 | 카드 강조 제거 |

---

## 12. 증거 상세 화면

### 목적

증거의 자세한 내용과 연결된 용의자/타임라인을 확인한다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

### 응답 데이터 예시

```json
{
  "evidenceId": 1,
  "title": "찢긴 컵 라벨",
  "description": "라벨 조각에는 ...MOND LAT...라는 글자가 남아 있다.",
  "imageUrl": null,
  "location": {
    "locationId": 1,
    "name": "데모룸 쓰레기통"
  },
  "importance": "CORE",
  "relatedSuspects": [
    {
      "suspectId": 1,
      "name": "박재민"
    }
  ],
  "relatedTimelineEvents": [
    {
      "time": "22:16",
      "title": "라벨이 찢긴 컵이 데모룸 앞에 놓임"
    }
  ]
}
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 심문에 사용하기 | 증거 제시 모달 또는 용의자 선택 화면 |
| 관련 용의자 클릭 | 용의자 상세 |
| 관련 타임라인 클릭 | 타임라인 화면 |

---

## 13. 용의자 목록 화면

### 목적

사건의 모든 용의자를 확인한다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/suspects
```

### 응답 데이터 예시

```json
{
  "suspects": [
    {
      "suspectId": 1,
      "name": "박재민",
      "role": "CFO",
      "relationToVictim": "공동창업자",
      "publicStatement": "저는 재무팀 자리에서 투자자료를 정리하고 있었습니다.",
      "suspicionLevel": 70,
      "relatedEvidenceCount": 3
    }
  ]
}
```

### 화면 표시 데이터

```text
용의자 카드
이름
직책
피해자와의 관계
공개 진술
의심도
관련 증거 수
심문하기 버튼
```

### 주요 UI 액션

| 액션 | 이동 |
|---|---|
| 용의자 카드 클릭 | 용의자 상세 |
| 심문하기 | 심문 채팅 |

---

## 14. 용의자 상세 화면

### 목적

용의자의 공개 정보, 알리바이, 관련 증거, 심문 기록을 확인한다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/suspects/{suspectId}
```

### 응답 데이터 예시

```json
{
  "suspectId": 1,
  "name": "박재민",
  "role": "CFO",
  "relationToVictim": "공동창업자",
  "publicProfile": "회사의 투자금과 재무 관리를 담당한다.",
  "publicStatement": "사건 당시 재무팀 자리에서 투자자료를 정리하고 있었다.",
  "alibi": "22시 이후 데모룸 근처에 가지 않았다고 주장한다.",
  "suspicionLevel": 70,
  "relatedEvidences": [
    {
      "evidenceId": 4,
      "title": "카페 결제 내역"
    }
  ],
  "recentInterrogations": [
    {
      "question": "사건 당시 어디에 있었습니까?",
      "answer": "재무팀 자리에서 투자 자료를 정리하고 있었습니다."
    }
  ]
}
```

### 주요 UI 액션

| 액션 | 이동 |
|---|---|
| 심문 시작 | 심문 채팅 |
| 관련 증거 클릭 | 증거 상세 |
| 알리바이 비교 | 타임라인 화면 |

---

## 15. 심문 채팅 화면

### 목적

AI 용의자와 대화한다.

### 진입 시 호출 API

#### 15.1 기존 심문 로그 조회

```http
GET /api/play-sessions/{sessionId}/interrogations?suspectId={suspectId}
```

#### 15.2 추천 질문 조회

```http
GET /api/play-sessions/{sessionId}/recommended-questions?suspectId={suspectId}
```

### 질문 전송 API

```http
POST /api/play-sessions/{sessionId}/interrogations
```

### Request - 자유 질문

```json
{
  "suspectId": 1,
  "questionType": "FREE",
  "question": "사건 당시 어디에 있었습니까?",
  "presentedEvidenceId": null
}
```

### Request - 증거 제시 질문

```json
{
  "suspectId": 1,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "카페 결제 내역에 대해 설명해주시겠습니까?",
  "presentedEvidenceId": 4
}
```

### Response

```json
{
  "interrogationId": 55,
  "suspectId": 1,
  "suspectName": "박재민",
  "question": "카페 결제 내역에 대해 설명해주시겠습니까?",
  "answer": "커피를 산 건 맞습니다. 하지만 그건 제가 마시려고 산 것이지 대표님께 드린 건 아닙니다.",
  "unlockedEvidences": [
    {
      "evidenceId": 9,
      "title": "법인카드 결제자 정보"
    }
  ],
  "createdAt": "2026-05-18T21:10:00"
}
```

### Android 화면 구성

```text
상단:
- 용의자 이름
- 용의자 역할
- 증거 제시 버튼

본문:
- 채팅 로그
- 사용자 질문 bubble
- AI 답변 bubble

하단:
- 추천 질문 버튼 목록
- 질문 입력창
- 전송 버튼
```

### 주요 UI 액션

| 액션 | 호출 |
|---|---|
| 추천 질문 클릭 | 질문 입력창에 채우기 또는 즉시 전송 |
| 증거 제시 클릭 | 증거 제시 모달 |
| 전송 | interrogation API |
| 새 증거 해금됨 | 증거 해금 Toast/Dialog 표시 |

### 주의사항

AI 답변은 1~2줄이 원칙이다.  
Android는 긴 답변이 오더라도 bubble에서 접거나 요약 표시할 수 있다.

---

## 16. 증거 제시 모달

### 목적

심문 중 특정 증거를 선택해 용의자를 추궁한다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/evidences?status=unlocked
```

### 화면 구성

```text
증거 검색
증거 카드 목록
선택한 증거 미리보기
이 증거로 추궁하기 버튼
```

### 모달 결과

증거 선택 후 심문 화면으로 돌아가며 `presentedEvidenceId`를 포함한 질문을 전송한다.

```json
{
  "presentedEvidenceId": 4
}
```

---

## 17. 타임라인 화면

### 목적

사건의 시간 흐름, 확인된 사실, 용의자 주장을 비교한다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/timeline
```

### 응답 데이터 예시

```json
{
  "events": [
    {
      "time": "22:05",
      "title": "카페 결제",
      "description": "오트라떼 1잔, 아몬드라떼 1잔 결제",
      "eventType": "FACT",
      "isTrueEvent": true,
      "relatedSuspect": "박재민",
      "relatedEvidenceId": 4,
      "isContradiction": false
    },
    {
      "time": "22:36",
      "title": "피해자 계정 메시지 전송",
      "description": "단톡방에 먼저 들어간다는 메시지가 전송됨",
      "eventType": "DIGITAL_LOG",
      "isTrueEvent": true,
      "isContradiction": true
    }
  ]
}
```

### 화면 표시 데이터

```text
시간
사건 내용
확인된 사실 / 주장 / 디지털 로그 구분
관련 용의자
관련 증거
모순 가능성 표시
```

### 주요 UI 액션

| 액션 | 이동 |
|---|---|
| 관련 증거 클릭 | 증거 상세 |
| 관련 용의자 클릭 | 용의자 상세 |
| 모순 표시 클릭 | 관련 카드 안내 |

---

## 18. 힌트 화면

### 목적

사용자가 막혔을 때 단계별 힌트를 확인한다.

### 힌트 목록 조회

```http
GET /api/play-sessions/{sessionId}/hints
```

### 힌트 사용

```http
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
```

### 응답 데이터 예시

```json
{
  "hints": [
    {
      "hintId": 1,
      "hintLevel": 1,
      "content": "피해자가 마신 음료와 알레르기 정보를 함께 보세요.",
      "isUsed": true,
      "penaltyScore": 5
    },
    {
      "hintId": 2,
      "hintLevel": 2,
      "content": null,
      "isUsed": false,
      "penaltyScore": 10,
      "unlockAfterMinutes": 20
    }
  ]
}
```

### 주요 UI 액션

| 액션 | 호출 |
|---|---|
| 힌트 열기 | `POST /api/play-sessions/{sessionId}/hints/{hintId}/use` |
| 힌트 사용 확인 | 확인 Dialog |
| 사용된 힌트 보기 | content 표시 |

---

## 19. 최종 추리 제출 화면

### 목적

사용자가 범인, 동기, 방법, 증거를 제출한다.

### 화면 진입 시 필요한 API

```http
GET /api/play-sessions/{sessionId}/suspects
GET /api/play-sessions/{sessionId}/evidences?status=unlocked
```

### 제출 API

```http
POST /api/play-sessions/{sessionId}/final-deduction
```

### Request

```json
{
  "selectedCulpritId": 1,
  "motiveText": "회계 비리 발각을 막기 위해서입니다.",
  "methodText": "아몬드라떼를 마시게 해 알레르기 쇼크를 유발했습니다.",
  "coverUpText": "피해자의 휴대폰으로 메시지를 보내 생존한 것처럼 위장했습니다.",
  "selectedEvidenceIds": [4, 8, 12]
}
```

### Response

```json
{
  "finalDeductionId": 1,
  "score": 85,
  "grade": "A",
  "feedbackSummary": "범인과 범행 방법을 정확히 파악했습니다.",
  "resultAvailable": true
}
```

성공 후 결과/해설 화면으로 이동한다.

### 중복 제출 처리

이미 제출한 세션이면 실패 응답이 온다.

```json
{
  "success": false,
  "error": {
    "code": "FINAL_DEDUCTION_ALREADY_SUBMITTED",
    "message": "이미 최종 추리를 제출했습니다."
  }
}
```

Android는 이 경우 결과 화면으로 이동하도록 처리한다.

---

## 20. 결과 / 해설 화면

### 목적

사용자의 추리 결과와 정답 해설을 보여준다.

### 호출 API

```http
GET /api/play-sessions/{sessionId}/result
```

### 응답 데이터 예시

```json
{
  "sessionId": 1001,
  "score": 85,
  "grade": "A",
  "correctCulprit": {
    "suspectId": 1,
    "name": "박재민"
  },
  "matchedParts": [
    "범인을 정확히 지목했습니다.",
    "알레르기 음료를 이용한 범행 방법을 맞혔습니다."
  ],
  "missedParts": [
    "에피펜을 미리 숨긴 점이 계획범행의 핵심입니다."
  ],
  "fullExplanation": "범인은 박재민입니다. 그는 회계 비리 발각을 막기 위해...",
  "keyEvidences": [
    {
      "evidenceId": 4,
      "title": "카페 결제 내역"
    }
  ],
  "nextRecommendedScenarios": []
}
```

### 주요 UI 액션

| 액션 | 이동/호출 |
|---|---|
| 리뷰 남기기 | 리뷰 작성 화면 |
| 다음 사건 플레이 | 시나리오 상세 |
| 내 기록 보기 | 내 기록 화면 |
| 공유하기 | Android Share Intent |

---

## 21. 내 기록 화면

### 목적

사용자의 플레이 기록과 제작한 시나리오를 보여준다.

### 호출 API

```http
GET /api/play-sessions/me?page=0&size=20
```

추후 인증 적용 후 사용한다.  
초기 MVP에서는 Mock 데이터 또는 `MOCK_USER_ID=1` 기준으로 조회한다.

### 화면 표시 데이터

```text
최근 플레이한 사건
완료한 사건
중단한 사건
내 탐정 등급
내가 만든 시나리오
북마크한 시나리오
```

---

## 22. 커스텀 시나리오 제작 화면

### 목적

유저가 직접 추리 시나리오를 만든다.

### 제작 Step

```text
Step 1. 사건 기본 정보
Step 2. 피해자 / 사건 개요
Step 3. 용의자 등록
Step 4. 증거 등록
Step 5. 힌트 등록
Step 6. 정답 설정
Step 7. AI 검증
Step 8. 공개 설정
```

### 22.1 시나리오 생성

```http
POST /api/scenarios
```

```json
{
  "title": "동아리 회비 실종 사건",
  "description": "MT 회비 5만원이 사라졌다.",
  "difficulty": "NORMAL",
  "playerCountMin": 1,
  "playerCountMax": 3,
  "estimatedPlayTimeMinutes": 30,
  "visibility": "PRIVATE"
}
```

### 22.2 시나리오 수정

```http
PATCH /api/scenarios/{scenarioId}
```

### 22.3 장소 등록

```http
POST /api/scenarios/{scenarioId}/locations
```

### 22.4 피해자 등록

```http
POST /api/scenarios/{scenarioId}/victim
```

### 22.5 용의자 등록

```http
POST /api/scenarios/{scenarioId}/suspects
```

### 22.6 증거 등록

```http
POST /api/scenarios/{scenarioId}/evidences
```

### 22.7 힌트 등록

```http
POST /api/scenarios/{scenarioId}/hints
```

### 22.8 정답 등록

```http
POST /api/scenarios/{scenarioId}/solution
```

### 22.9 공개

```http
POST /api/scenarios/{scenarioId}/publish
```

### Android 구현 메모

- MVP에서는 모든 Step을 한 번에 완성하지 않아도 된다.
- 우선은 단순 폼과 저장 버튼 위주로 구현한다.
- 각 Step 저장 후 scenarioId를 유지한다.
- 작성 중인 시나리오는 `DRAFT` 상태다.

---

## 23. AI 시나리오 생성 화면 (2차)

### 목적

AI가 시나리오 초안을 만들어준다.

### 호출 API

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
  "tone": "현대 미스터리"
}
```

### Response

```json
{
  "draftId": "draft_001",
  "title": "데모데이 전야 살인사건",
  "summary": "스타트업 대표가 데모데이 전날 사망했다.",
  "suspects": [],
  "evidences": [],
  "hints": [],
  "solution": {}
}
```

### Android 구현 메모

- 초안 생성은 시간이 걸릴 수 있으므로 Loading 화면 필요
- 실패 시 “다시 생성하기” 버튼 제공
- 생성 결과는 사용자가 수정할 수 있어야 한다
- `solution`은 제작자 편집 화면 전용이다. 플레이 화면, 심문 화면, 일반 시나리오 상세 화면에 노출하지 않는다.

---

## 24. AI 검증 결과 화면

### 목적

커스텀 시나리오가 플레이 가능한지 AI가 점검한다.

### 호출 API

```http
POST /api/ai/scenarios/{scenarioId}/validate
```

### Response

```json
{
  "validationStatus": "PASSED_WITH_WARNINGS",
  "validationScore": 78,
  "problems": [
    "범인을 가리키는 결정적 증거가 부족합니다."
  ],
  "suggestions": [
    "출입 로그나 피해자와의 마지막 대화 기록을 추가하는 것이 좋습니다."
  ],
  "difficultyEvaluation": "NORMAL"
}
```

### 화면 표시 데이터

```text
검증 상태
검증 점수
논리적 문제
부족한 증거
보완 제안
검증 통과 버튼
다시 검증 버튼
```

---

## 25. 리뷰 화면

### 25.1 리뷰 목록 조회

```http
GET /api/scenarios/{scenarioId}/reviews?page=0&size=20
```

### 25.2 리뷰 작성

```http
POST /api/scenarios/{scenarioId}/reviews
```

```json
{
  "rating": 5,
  "content": "증거 연결이 재밌었습니다.",
  "isSpoiler": false
}
```

### Android 구현 메모

- 플레이 완료자만 리뷰 가능하도록 추후 인가 처리
- 스포일러 리뷰는 접어서 표시

---

## 26. 북마크

### 북마크 추가

```http
POST /api/scenarios/{scenarioId}/bookmarks
```

### 북마크 해제

```http
DELETE /api/scenarios/{scenarioId}/bookmarks
```

### Android 구현 메모

- 로그인 전 MVP에서는 Mock 처리 가능
- 낙관적 UI 업데이트 가능
- 실패 시 원복 처리

---

## 27. 화면별 네비게이션 플로우

### 27.1 공식 시나리오 플레이

```text
홈
→ 시나리오 라이브러리
→ 시나리오 상세
→ 사건 브리핑
→ 탐정 대시보드
→ 증거 / 용의자 / 타임라인 확인
→ 심문 채팅
→ 최종 추리 제출
→ 결과 해설
```

### 27.2 커스텀 시나리오 제작

```text
홈
→ 제작
→ 직접 작성
→ AI 초안 생성은 2차 기능으로 선택 제공
→ 사건 정보 입력
→ 용의자 등록
→ 증거 등록
→ 힌트 등록
→ 정답 설정
→ AI 검증
→ 공개 설정
→ 시나리오 상세
```

### 27.3 이어하기

```text
홈
→ 최근 플레이한 사건
→ 탐정 대시보드
```

---

## 28. Android 구현 우선순위

### 1순위

```text
시나리오 목록
시나리오 상세
게임 시작
탐정 대시보드
증거 보드
용의자 목록
심문 채팅
최종 추리 제출
결과 화면
```

### 2순위

```text
현장 정보
타임라인
힌트
증거 제시 모달
용의자 상세
증거 상세
커스텀 시나리오 제작
AI 검증
리뷰
북마크
```

### 3순위

```text
AI 초안 생성
내 기록
```

### 4순위

```text
로그인
마이페이지
거래/크레딧
시나리오 구매/언락
```

---

## 29. Android 개발 AI가 지켜야 할 규칙

Android Studio에 연결된 AI는 아래 규칙을 지켜야 한다.

```text
1. 이 문서의 화면/API 매핑을 우선한다.
2. 존재하지 않는 API를 임의로 만들지 않는다.
3. API 경로는 CaseLab_AI_API_Spec.md와 맞춘다.
4. 인증/거래 기능은 현재 MVP에서 우선 구현하지 않는다.
5. Mock 데이터로 먼저 화면을 구성할 수 있다.
6. API 연동 시 Loading / Success / Error / Empty 상태를 모두 고려한다.
7. 심문 채팅 화면은 메시지 UI를 단순하게 유지한다.
8. 증거/용의자/타임라인은 카드 기반 UI로 구현한다.
9. 화면이 많으므로 공통 컴포넌트를 적극적으로 분리한다.
10. 실제 AI API Key를 Android 코드에 넣지 않는다.
```

---

## 30. 백엔드 개발 AI가 지켜야 할 규칙

백엔드 AI는 Android 화면 요구를 기준으로 필요한 데이터를 제공해야 한다.

```text
1. 화면에서 필요한 필드는 DTO에 명확히 포함한다.
2. Entity를 API 응답으로 직접 반환하지 않는다.
3. 증거 해금 여부는 playSession 기준으로 계산한다.
4. 시나리오 원본 증거와 플레이 세션의 해금 상태를 분리한다.
5. AI 심문 API는 응답과 함께 새로 해금된 증거 목록을 반환할 수 있어야 한다.
6. 최종 추리 제출은 중복 제출을 막아야 한다.
7. 현재는 MockUserProvider를 사용하되 user_id 구조는 유지한다.
8. 추후 인증/거래 확장을 고려해 creator_id, visibility, price_credit 필드를 유지한다.
```

---

## 31. MVP 체크리스트

아래가 되면 Android MVP 시연이 가능하다.

```text
[ ] 시나리오 목록이 보인다.
[ ] 시나리오 상세로 이동할 수 있다.
[ ] 시작하기를 누르면 게임 세션이 생성된다.
[ ] 사건 브리핑이 보인다.
[ ] 증거 목록이 보인다.
[ ] 증거 상세가 보인다.
[ ] 용의자 목록이 보인다.
[ ] 용의자에게 질문할 수 있다.
[ ] AI 또는 Mock 답변이 표시된다.
[ ] 힌트를 볼 수 있다.
[ ] 최종 추리를 제출할 수 있다.
[ ] 결과 해설 화면이 보인다.
```

---

## 32. 1차 MVP 확장 체크리스트

```text
[ ] 커스텀 시나리오를 만들 수 있다.
[ ] AI가 시나리오를 검증한다.
[ ] 시나리오를 공개/비공개로 설정할 수 있다.
[ ] 시나리오에 리뷰를 남길 수 있다.
[ ] 시나리오를 북마크할 수 있다.
```

---

## 33. 후속 확장 체크리스트

```text
[ ] AI가 시나리오 초안을 생성한다.
[ ] 로그인 사용자를 구분한다.
[ ] 유료 시나리오 구매/언락 기능을 붙인다.
```

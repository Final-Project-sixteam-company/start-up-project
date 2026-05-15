# CaseLab AI 프로젝트 기획서 / 개발 핸드오버 문서

> 이 문서는 IntelliJ, Cursor, Claude Code, Copilot, JetBrains AI Assistant 등 개발용 AI에게 프로젝트 전체 맥락을 주입하기 위한 기준 문서다.  
> 코드 생성 전 이 문서를 먼저 읽고, 여기에 정의된 서비스 정체성, 도메인 구조, 게임 규칙, API 방향, 데이터 모델을 기준으로 구현한다.

---

## 0. 문서 목적

이 문서는 **CaseLab AI** 프로젝트의 개발 기준을 정의한다.

개발용 AI에게 이 문서를 먹일 때의 목표는 다음과 같다.

```text
1. 프로젝트가 어떤 서비스인지 이해시킨다.
2. 어떤 기능을 MVP로 구현할지 명확히 한다.
3. 백엔드 / Android / AI 기능의 책임을 구분한다.
4. 도메인 모델과 API 구조를 일관되게 유지한다.
5. AI 용의자 심문 로직에서 정답 누설을 방지한다.
6. 커스텀 시나리오 제작 / 공유 플랫폼 구조를 이해시킨다.
```

---

## 1. 프로젝트명

```text
CaseLab AI
```

---

## 2. 한 줄 정의

**CaseLab AI는 사용자가 탐정이 되어 AI 용의자를 심문하고, 증거와 알리바이를 조합해 사건의 범인·동기·범행 방법을 밝혀내는 Android 기반 AI 추리게임 플랫폼이다.**

---

## 3. 서비스 정체성

CaseLab AI는 단순한 AI 챗봇 게임이 아니다.

```text
AI 크라임씬 ❌
단순 챗봇 추리게임 ❌
역할극 마피아 게임 ❌
랜덤 온라인 매칭 추리게임 ❌

AI 용의자 심문형 추리게임 ⭕
증거 카드 기반 탐정 게임 ⭕
커스텀 시나리오 공유 플랫폼 ⭕
사이버 방탈출 / 스타 유즈맵식 추리 플랫폼 ⭕
```

핵심은 다음 두 가지다.

```text
1. 제작진이 만든 공식 시나리오를 플레이한다.
2. 유저가 직접 만든 커스텀 시나리오를 공유하고 플레이한다.
```

---

## 4. 핵심 컨셉

사용자는 **탐정**이다.  
AI는 **용의자 NPC**다.  
시스템은 **사건의 진실, 증거, 알리바이, 힌트, 정답을 관리하는 게임 엔진**이다.

플레이어는 맵을 직접 돌아다니지 않는다.  
대신 Android 앱에서 아래 요소를 조사한다.

```text
사건 브리핑
현장 정보
증거 카드
용의자 프로필
AI 심문
타임라인
힌트
최종 추리 제출
결과 해설
```

---

## 5. 문제 인식

기존 추리 콘텐츠의 문제는 다음과 같다.

```text
1. 사건 하나를 제작하는 비용이 크다.
2. 한 번 범인을 알면 재플레이 가치가 낮다.
3. 크라임씬처럼 실제 맵/세트장을 구현하려면 제작 난이도가 너무 높다.
4. 유저가 직접 역할극을 해야 하면 재미가 유저의 연기력에 의존한다.
5. 온라인 랜덤 매칭은 트롤, 잠수, 스포일러 문제가 생긴다.
6. AI가 즉흥적으로 사건 전체를 만들면 설정이 무너질 수 있다.
```

CaseLab AI의 해결 방향은 다음과 같다.

```text
1. 사용자는 탐정 역할만 한다.
2. AI가 용의자 NPC 역할을 한다.
3. 사건은 Case Graph 구조로 저장한다.
4. 맵 탐색 대신 증거 카드와 현장 정보로 조사감을 만든다.
5. 공식 시나리오와 유저 커스텀 시나리오를 모두 제공한다.
6. 커스텀 시나리오는 AI 검증을 거쳐 공개할 수 있다.
```

---

## 6. MVP 목표

### 6.1 1차 MVP 목표

1차 MVP는 Android 앱과 Spring Boot 백엔드 기준으로 다음 기능을 구현한다.

```text
1. 회원가입 / 로그인
2. 시나리오 라이브러리 조회
3. 공식 시나리오 1개 플레이
4. 사건 브리핑 확인
5. 현장 정보 확인
6. 증거 카드 조회
7. 시간별 증거 해금
8. 용의자 목록 / 상세 조회
9. AI 용의자 심문
10. 힌트 사용
11. 최종 추리 제출
12. 결과 / 해설 조회
13. 커스텀 시나리오 생성 기본형
14. AI 시나리오 검증 기본형
15. 커스텀 시나리오 공개 / 비공개
16. 시나리오 리뷰 / 북마크
```

### 6.2 1차 MVP에서 제외

```text
실시간 랜덤 매칭
대형 멀티플레이
3D 맵
음성 심문
실제 결제 / 시나리오 거래
복잡한 애니메이션
실제 사건 직접 재현
완전 자동 무검수 사건 공개
```

---

## 7. 핵심 게임 플로우

```text
사용자 로그인
  ↓
시나리오 선택
  ↓
사건 브리핑 확인
  ↓
플레이 세션 시작
  ↓
현장 정보 / 초기 증거 확인
  ↓
용의자 프로필 확인
  ↓
AI 용의자 심문
  ↓
증거 제시 심문
  ↓
시간 또는 조건에 따라 핵심 증거 해금
  ↓
힌트 사용
  ↓
최종 추리 제출
  ↓
정답 판정 / 탐정 등급 / 해설 확인
  ↓
리뷰 작성 또는 다른 사건 플레이
```

---

## 8. 주요 사용자

```text
추리게임을 좋아하는 사용자
크라임씬, 방탈출, 마피아 게임을 좋아하는 사용자
혼자 즐길 수 있는 추리 콘텐츠를 원하는 사용자
친구와 함께 사건을 풀고 싶은 사용자
AI 기반 스토리 게임에 관심 있는 사용자
직접 시나리오를 만들어보고 싶은 창작형 사용자
```

---

## 9. Android 앱 구조

### 9.1 Bottom Navigation

Android 앱은 기본적으로 하단 탭 구조를 사용한다.

```text
홈
시나리오
제작
내 기록
마이페이지
```

### 9.2 주요 화면 목록

```text
스플래시 화면
온보딩 화면
홈 화면
시나리오 라이브러리
시나리오 상세
사건 브리핑
탐정 대시보드
현장 정보
증거 보드
증거 상세
용의자 목록
용의자 상세
심문 채팅
증거 제시 모달
타임라인
힌트
최종 추리 제출
결과 / 해설
커스텀 시나리오 제작
AI 시나리오 생성
AI 검증 결과
내 기록
마이페이지
```

---

## 10. Android 화면별 핵심 설명

### 10.1 홈 화면

```text
오늘의 추천 사건
인기 커스텀 시나리오
최근 플레이한 사건
공식 시나리오 바로가기
커스텀 제작 시작 버튼
```

### 10.2 시나리오 라이브러리

```text
검색바
공식 / 커스텀 필터
인기 / 최신 필터
난이도 필터
플레이 시간 필터
인원수 필터
시나리오 카드 목록
```

시나리오 카드 정보:

```text
제목
설명
난이도
예상 플레이 시간
용의자 수
증거 수
평점
플레이 수
태그
```

### 10.3 사건 브리핑 화면

```text
사건 제목
사건 개요
피해자 정보
발견 장소
초기 공개 증거
탐정 목표
조사 시작 버튼
```

### 10.4 탐정 대시보드

플레이 중 핵심 화면이다.

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

### 10.5 현장 정보

```text
간단한 방 / 건물 단면도
장소별 증거 위치 마커
피해자 발견 위치
장소 카드
```

### 10.6 증거 보드

```text
초기 공개 증거
잠긴 증거
시간 해금 증거
새로 해금된 증거
핵심 증거
페이크 증거
```

### 10.7 심문 채팅

```text
용의자 이름
용의자 프로필 요약
채팅 로그
추천 질문 버튼
증거 제시 버튼
질문 입력창
```

AI 용의자 답변은 1~2줄로 제한한다.

### 10.8 최종 추리 제출

```text
범인 선택
범행 동기 입력
범행 방법 입력
은폐 방법 입력
결정적 증거 3개 선택
추리 설명 입력
최종 제출 버튼
```

---

## 11. 공식 데모 시나리오

### 11.1 사건명

```text
데모데이 전야 살인사건
```

### 11.2 사건 개요

```text
AI 스타트업 모노로그랩스의 대표 강도현이 데모데이 전날 밤 데모룸에서 사망했다.
처음에는 알레르기 쇼크로 보였지만, 현장에는 찢긴 컵 라벨, 사라진 에피펜, 사망 이후 전송된 메시지가 남아 있었다.
```

### 11.3 용의자

```text
박재민 - CFO
이준호 - CTO
서유라 - 마케팅 리드
김나은 - 인턴
오세훈 - 투자사 심사역
```

### 11.4 초기 공개 증거

```text
피해자 발견 현장
찢긴 컵 라벨
단톡방 메시지
피해자의 알레르기 정보
카페 영수증
```

### 11.5 시간 해금 증거

```text
카페 결제자 정보
출입 로그
휴대폰 위치 기록
회계 파일
에피펜 발견 위치
```

### 11.6 진실

```text
범인은 박재민.
그는 회사 자금 유용 사실이 데모데이에서 공개될 위기에 놓이자,
피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 하고 에피펜을 숨겼다.
이후 피해자의 휴대폰으로 메시지를 보내 사망 시간을 조작했다.
```

---

## 12. AI NPC 설계 원칙

### 12.1 가장 중요한 원칙

```text
AI 용의자에게 범인이 누구인지 직접 알려주지 않는다.
범인 정보는 백엔드 시크릿으로 관리한다.
AI에게는 현재 장면에서 말해도 되는 정보와 답변 정책만 전달한다.
```

### 12.2 AI 답변 기본 규칙

```text
답변은 1~2줄로 제한한다.
설정에 없는 사실은 만들지 않는다.
아직 공개되지 않은 비밀은 말하지 않는다.
증거가 없으면 부인하거나 회피한다.
증거가 제시되면 일부 인정하거나 말을 바꾼다.
결정적 증거가 나오기 전까지 자백하지 않는다.
범인 여부를 직접 말하지 않는다.
```

### 12.3 시민 용의자

무고한 용의자는 범행에 대해서는 거짓말하지 않는다.  
하지만 자기에게 불리한 개인 비밀은 숨기거나 회피할 수 있다.

```text
범행 자체는 하지 않았다.
자기 비밀은 숨길 수 있다.
증거가 나오면 일부 인정한다.
```

### 12.4 범인 용의자

범인 용의자는 거짓말할 수 있다.  
하지만 공개된 증거와 정면으로 충돌하는 어색한 거짓말은 하지 않는다.

```text
정해진 알리바이에 따라 답변한다.
증거가 없으면 부인한다.
증거가 제시되면 일부 인정한다.
결정적 증거가 나오기 전까지 범행을 인정하지 않는다.
```

---

## 13. AI 심문 처리 구조

나쁜 구조:

```text
AI에게 전체 사건 진실을 모두 전달
→ 말하지 말라고 지시
→ 사용자 질문에 답변
```

좋은 구조:

```text
사용자 질문
  ↓
질문 의도 분석
  ↓
현재 공개 증거 확인
  ↓
백엔드 Rule Engine이 답변 정책 결정
  ↓
AI Actor에게 현재 말해도 되는 정보만 전달
  ↓
AI Actor가 1~2줄 답변 생성
  ↓
심문 로그 저장
```

### 예시

현재 상태:

```text
카페 결제 내역 미공개
```

답변 정책:

```text
커피를 산 적 없다고 부인한다.
```

AI에게 전달되는 내용:

```text
인물: 박재민, CFO
성격: 차분하지만 방어적
현재 답변 정책: 커피 구매 사실을 부인한다.
사용자 질문: 사건 당일 커피를 산 적 있습니까?
```

AI 답변:

```text
아니요. 저는 그날 커피를 산 기억이 없습니다.
계속 재무팀 자리에서 자료를 정리하고 있었습니다.
```

---

## 14. 커스텀 시나리오 제작

### 14.1 제작 단계

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

### 14.2 AI 보조 기능

```text
사건 초안 생성
용의자 페르소나 추천
증거 아이디어 추천
힌트 생성
알리바이 모순 검사
난이도 평가
시나리오 보완 제안
```

### 14.3 공개 상태

```text
PRIVATE:
나만 플레이 가능

UNLISTED:
링크가 있는 사람만 플레이 가능

PUBLIC:
시나리오 라이브러리에 공개

OFFICIAL:
운영진 검수 완료 공식 시나리오
```

---

## 15. AI 시나리오 검증

커스텀 시나리오를 공개하기 전 검증한다.

검증 항목:

```text
범인이 설정되어 있는가?
결정적 증거가 존재하는가?
용의자 알리바이가 비어 있지 않은가?
힌트가 존재하는가?
증거와 정답이 연결되는가?
사건 해설이 논리적으로 가능한가?
시나리오가 플레이 가능한 최소 요건을 만족하는가?
```

검증 결과 예시:

```text
이 시나리오는 플레이 가능하지만,
범인을 가리키는 결정적 증거가 부족합니다.
출입 로그나 피해자와의 마지막 대화 기록을 추가하는 것이 좋습니다.
```

---

## 16. 백엔드 도메인 구조

권장 패키지 구조:

```text
com.caselab
 ├─ auth
 ├─ user
 ├─ scenario
 ├─ suspect
 ├─ evidence
 ├─ hint
 ├─ solution
 ├─ play
 ├─ interrogation
 ├─ deduction
 ├─ review
 ├─ bookmark
 ├─ report
 ├─ ai
 └─ common
```

---

## 17. 핵심 엔티티

### 17.1 User

사용자 계정.

```text
id
email
password
nickname
profileImageUrl
role
createdAt
updatedAt
```

### 17.2 Scenario

공식 / 커스텀 시나리오.

```text
id
creatorId
title
description
synopsis
thumbnailUrl
scenarioType
visibility
status
difficulty
playerCountMin
playerCountMax
estimatedPlayTimeMinutes
playCount
averageRating
ratingCount
createdAt
updatedAt
publishedAt
```

### 17.3 ScenarioLocation

사건 장소.

```text
id
scenarioId
name
description
mapX
mapY
sortOrder
```

### 17.4 Victim

피해자 정보.

```text
id
scenarioId
foundLocationId
name
age
role
description
causeOfDeath
foundCondition
```

### 17.5 Suspect

용의자.

```text
id
scenarioId
name
role
relationToVictim
publicProfile
publicStatement
alibi
personalityPrompt
responsePolicyJson
suspicionLevel
sortOrder
```

### 17.6 Evidence

증거 카드.

```text
id
scenarioId
locationId
title
description
evidenceType
importance
imageUrl
isInitialPublic
unlockType
unlockConditionJson
unlockAfterMinutes
sortOrder
```

### 17.7 Hint

힌트.

```text
id
scenarioId
hintLevel
content
unlockAfterMinutes
penaltyScore
```

### 17.8 Solution

정답.

```text
id
scenarioId
culpritSuspectId
motive
method
coverUp
fullExplanation
```

### 17.9 PlaySession

사용자 플레이 진행 상태.

```text
id
userId
scenarioId
status
startedAt
endedAt
currentElapsedSeconds
score
grade
hintCount
interrogationCount
```

### 17.10 InterrogationLog

심문 로그.

```text
id
playSessionId
suspectId
presentedEvidenceId
questionType
question
answer
aiModel
createdAt
```

### 17.11 FinalDeduction

최종 추리 제출.

```text
id
playSessionId
selectedCulpritId
motiveText
methodText
coverUpText
score
grade
feedback
submittedAt
```

---

## 18. API 설계 방향

API 명세서는 별도 문서 `CaseLab_AI_API_Spec.md`를 기준으로 한다.

주요 API 그룹은 다음과 같다.

```text
Auth API
User API
Scenario API
Custom Scenario API
AI Scenario Generation API
Game Session API
Evidence API
Suspect API
Interrogation API
Hint API
Final Deduction API
Review API
Bookmark API
Report API
```

---

## 19. 주요 API 예시

### 19.1 시나리오 목록 조회

```http
GET /api/scenarios
```

Query:

```text
type=OFFICIAL|CUSTOM
difficulty=EASY|NORMAL|HARD
keyword=
page=
size=
```

### 19.2 플레이 세션 시작

```http
POST /api/play-sessions
```

Request:

```json
{
  "scenarioId": 1
}
```

### 19.3 증거 목록 조회

```http
GET /api/play-sessions/{sessionId}/evidences
```

### 19.4 용의자 심문

```http
POST /api/play-sessions/{sessionId}/interrogations
```

Request:

```json
{
  "suspectId": 1,
  "questionType": "FREE",
  "question": "사건 당시 어디에 있었습니까?",
  "presentedEvidenceId": null
}
```

### 19.5 최종 추리 제출

```http
POST /api/play-sessions/{sessionId}/final-deduction
```

Request:

```json
{
  "selectedCulpritId": 1,
  "motiveText": "회계 비리 은폐",
  "methodText": "아몬드라떼를 이용한 알레르기 쇼크 유발",
  "coverUpText": "피해자 휴대폰으로 사망 이후 메시지를 보냄",
  "selectedEvidenceIds": [1, 2, 3]
}
```

---

## 20. AI 관련 서비스 구조

권장 서비스 구조:

```text
AiScenarioGenerator
AiScenarioValidator
AiInterrogationService
AiDeductionScorer
AiHintGenerator
AiPromptBuilder
AiUsageLogger
```

### 20.1 AiScenarioGenerator

역할:

```text
커스텀 시나리오 초안 생성
용의자 페르소나 생성
증거 아이디어 생성
힌트 초안 생성
```

### 20.2 AiScenarioValidator

역할:

```text
시나리오 논리 검증
정답 존재 여부 확인
증거와 정답 연결성 검토
보완 제안 생성
```

### 20.3 AiInterrogationService

역할:

```text
사용자 질문을 받고 AI 용의자 답변 생성
답변은 1~2줄로 제한
현재 공개 정보만 사용
```

### 20.4 AiDeductionScorer

역할:

```text
사용자의 최종 추리와 정답 데이터 비교
점수 계산
피드백 생성
탐정 등급 산출
```

---

## 21. AI 프롬프트 정책

### 21.1 공통 NPC 프롬프트 규칙

```text
너는 추리게임의 용의자 NPC다.
설정에 없는 사실을 만들지 마라.
답변은 최대 2문장으로 제한한다.
아직 공개되지 않은 비밀을 먼저 말하지 마라.
현재 답변 정책을 반드시 따른다.
사용자가 범인 여부를 직접 물어도 답하지 마라.
모르는 내용은 모른다고 말하라.
```

### 21.2 AI에게 전달하면 안 되는 정보

```text
전체 정답
진범 여부
아직 해금되지 않은 핵심 비밀
다른 용의자의 비밀
최종 해설 전체
```

### 21.3 AI에게 전달할 정보

```text
현재 용의자 공개 프로필
현재 용의자 공개 알리바이
현재 공개된 증거
사용자가 제시한 증거
현재 답변 정책
사용자 질문
```

---

## 22. 점수 계산 방향

최종 추리 점수는 100점 기준이다.

```text
범인 선택: 30점
범행 방법: 25점
동기: 20점
은폐 방법: 10점
결정적 증거 선택: 15점
```

힌트 사용 시 감점:

```text
힌트 1단계: -5점
힌트 2단계: -10점
힌트 3단계: -15점
```

탐정 등급:

```text
90점 이상: S
80점 이상: A
70점 이상: B
60점 이상: C
60점 미만: D
```

---

## 23. 시나리오 품질 관리

### 23.1 공개 전 최소 조건

커스텀 시나리오는 공개 전 아래 조건을 만족해야 한다.

```text
제목 존재
사건 개요 존재
용의자 2명 이상
증거 3개 이상
힌트 1개 이상
정답 설정 완료
범인을 가리키는 핵심 증거 1개 이상
AI 검증 통과
```

### 23.2 시나리오 랭킹 기준

```text
플레이 수
완료율
평점
북마크 수
신고 수
최근 인기도
```

---

## 24. 스포일러 관리

추리게임 플랫폼에서는 스포일러 관리가 중요하다.

필요 기능:

```text
플레이 완료자만 리뷰 작성 가능
리뷰 작성 시 스포일러 여부 체크
스포일러 리뷰는 접어서 표시
정답 직접 언급 리뷰 신고 가능
운영자 블라인드 처리 가능
```

---

## 25. 리스크와 대응

### 25.1 사건 품질 리스크

문제:

```text
사건이 재미없으면 서비스 가치가 없다.
```

대응:

```text
공식 시나리오는 사람이 검수한다.
커스텀 시나리오는 AI 검증을 거친다.
유저 평점과 완료율로 품질을 관리한다.
```

### 25.2 AI 정답 누설 리스크

문제:

```text
AI 용의자가 범인이나 비밀을 먼저 말할 수 있다.
```

대응:

```text
AI에게 전체 정답을 전달하지 않는다.
백엔드에서 secret을 관리한다.
현재 공개 정보만 AI에게 전달한다.
답변을 1~2줄로 제한한다.
```

### 25.3 1회성 콘텐츠 리스크

문제:

```text
추리 사건은 한 번 풀면 재플레이 가치가 낮다.
```

대응:

```text
공식 시나리오 라이브러리
커스텀 시나리오 공유
인기 시나리오 랭킹
시나리오 변주 기능
AI 시나리오 생성
```

### 25.4 실제 사건 모티브 윤리 리스크

문제:

```text
실제 사건을 그대로 재현하면 법적/윤리적 문제가 생길 수 있다.
```

대응:

```text
실제 사건의 구조만 참고한다.
인물, 장소, 관계, 세부 범행 방식은 충분히 각색한다.
최근 사건, 민감 사건, 특정 가능한 사건은 사용하지 않는다.
```

---

## 26. 기술 스택 제안

### Android

```text
Kotlin
Jetpack Compose
Retrofit
OkHttp
ViewModel
Coroutine
Navigation
DataStore
```

### Backend

```text
Java 17+
Spring Boot
Spring Security
JWT
Spring Data JPA
MySQL
Redis 선택
```

### AI

```text
Spring AI 또는 외부 LLM API
OpenAI / Claude / Gemini 중 택1 가능
AI 호출 로그 저장
프롬프트 템플릿 관리
```

### Infra

```text
AWS EC2 또는 Render/Railway
RDS 또는 MySQL 서버
S3 또는 이미지 저장소
```

---

## 27. 개발 우선순위

### 1단계: 기본 플레이

```text
회원가입 / 로그인
시나리오 목록
공식 시나리오 상세
플레이 세션 시작
증거 조회
용의자 조회
심문 Mock 응답
최종 추리 제출
결과 조회
```

### 2단계: AI 심문

```text
AI 프롬프트 연결
답변 정책 적용
심문 로그 저장
증거 제시 심문
```

### 3단계: 커스텀 시나리오

```text
시나리오 생성
용의자 등록
증거 등록
힌트 등록
정답 설정
공개 / 비공개
```

### 4단계: AI 검증 / 생성

```text
AI 시나리오 초안 생성
AI 시나리오 검증
보완 제안
```

### 5단계: 플랫폼화

```text
리뷰
평점
북마크
인기 시나리오
신고
스포일러 관리
```

---

## 28. 개발용 AI에게 주는 작업 원칙

개발용 AI는 다음 원칙을 지켜야 한다.

```text
1. CaseLab AI는 Android 기반 추리게임 플랫폼이다.
2. 사용자는 탐정이고, AI는 용의자 NPC다.
3. AI에게 전체 정답을 전달하지 않는다.
4. 심문 응답은 1~2줄로 제한한다.
5. 시나리오는 공식 / 커스텀으로 나뉜다.
6. 커스텀 시나리오는 유저가 만들고 공유할 수 있다.
7. 시나리오 공개 전 AI 검증이 가능해야 한다.
8. 플레이 세션은 사용자별 진행 상태를 저장해야 한다.
9. 증거 해금 상태는 play_session 단위로 관리한다.
10. 최종 추리 결과는 점수와 등급으로 계산한다.
```

---

## 29. 개발용 AI가 하면 안 되는 것

```text
정답을 AI NPC 프롬프트에 직접 넣지 말 것
AI 용의자가 장문으로 설명하게 만들지 말 것
설정에 없는 사실을 임의로 추가하지 말 것
시나리오와 플레이 세션을 혼동하지 말 것
모든 증거를 처음부터 공개하지 말 것
커스텀 시나리오 공개 시 검증 단계를 생략하지 말 것
리뷰에 스포일러 관리 없이 공개하지 말 것
```

---

## 30. 최종 요약

CaseLab AI는 다음 구조를 가진다.

```text
공식 시나리오
+ 커스텀 시나리오
+ 시나리오 공유 플랫폼
+ 증거 카드 기반 조사
+ AI 용의자 심문
+ 시간별 증거 해금
+ 최종 추리 제출
+ AI 판정 / 해설
```

핵심 차별점은 다음이다.

```text
AI가 사건을 즉흥적으로 아무렇게나 만드는 것이 아니라,
Case Graph와 답변 정책을 기반으로 일관성 있게 용의자 NPC 역할을 수행한다.

또한 유저가 직접 추리 시나리오를 만들고 공유할 수 있어,
스타 유즈맵이나 사이버 방탈출 플랫폼처럼 확장될 수 있다.
```

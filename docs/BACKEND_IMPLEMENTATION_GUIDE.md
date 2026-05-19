# CaseLab AI - Backend Implementation Guide

> 문서 목적: Spring Boot 백엔드 구현 규칙의 정본을 정의한다.  
> 이 문서는 백엔드 구현 규칙과 인증/거래 확장 계획의 핵심 내용을 통합한다.

---

## 0. 정본 범위

이 문서는 백엔드 구현자가 반드시 따라야 할 기준을 관리한다.

| 이 문서에서 관리 | 다른 정본 문서로 분리 |
|---|---|
| 패키지 구조, 계층 책임 | 제품 요구사항, MVP 범위 |
| 공통 응답, 예외 처리, 트랜잭션 규칙 | API Request/Response JSON |
| MockUserProvider, ScenarioAccessService | ERD, DDL, DB 제약 전문 |
| AI 호출 경계, Fallback, 로그 저장 | AI 프롬프트 템플릿 전문 |
| 인증/인가, 거래/크레딧 후순위 확장 계획 | 로컬 실행, Docker, 배포 |

세부 정본은 다음 문서를 따른다.

| 문서 | 용도 |
|---|---|
| `CaseLab_AI_PRD.md` | 제품 요구사항, MVP 범위, 구현 우선순위 |
| `CaseLab_AI_API_Spec.md` | API 경로, Request/Response, DTO 필드명 |
| `CaseLab_AI_ERD_Design.md` | 엔티티, 관계, Enum, 동시성 메모 |
| `CaseLab_AI_ERDCloud.sql` | ERDCloud/DB 설계용 SQL |
| `AI_NPC_PROMPT_POLICY.md` | AI NPC 프롬프트, 답변 정책, 정답 누설 방지 |

---

## 1. 현재 구현 전제

### 1.1 기술 기준

```text
Language: Java 21
Framework: Spring Boot 4
Persistence: Spring Data JPA
Database: MySQL
Cache/Lock 확장: Redis
Client: Android Kotlin
```

### 1.2 1차 MVP 우선순위

현재 백엔드는 아래 흐름을 먼저 완성한다.

```text
1. 공식 시나리오 1개 조회
2. 게임 세션 시작
3. 탐정 대시보드 조회
4. 증거 / 용의자 / 힌트 조회
5. AI 용의자 심문
6. 최종 추리 제출
7. 결과 해설 조회
8. 커스텀 시나리오 제작 기본형
9. AI 시나리오 검증 기본형
10. 리뷰 / 북마크 기본형
```

### 1.3 현재 보류 기능

아래 기능은 현재 MVP에서 완성하지 않는다.

```text
JWT 인증 완성
실제 결제
크레딧 구매
유료 시나리오 접근 제한
제작자 정산
랜덤 매칭 멀티플레이
AI 시나리오 초안 생성 고도화
```

단, 아래 확장 필드는 제거하지 않는다.

| 영역 | 유지할 필드 |
|---|---|
| User | `userId` |
| Scenario | `creatorId`, `visibility`, `scenarioType`, `priceCredit`, `status` |
| PlaySession | `userId`, `scenarioId`, `status` |
| Review/Bookmark | `userId`, `scenarioId` |

DB 컬럼명은 snake_case를 사용하고, Java/DTO 필드명은 camelCase를 사용한다.

---

## 2. 추천 패키지 구조

루트 패키지는 `com.startup`을 기준으로 한다.

```text
com.startup
 ├─ common
 │   ├─ config
 │   ├─ dto
 │   ├─ error
 │   ├─ auth
 │   └─ util
 │
 ├─ domain
 │   ├─ user
 │   ├─ scenario
 │   ├─ play
 │   ├─ ai
 │   ├─ review
 │   └─ bookmark
 │
 └─ infrastructure
     ├─ persistence
     └─ redis
```

각 도메인 패키지는 필요에 따라 아래 구조를 따른다.

```text
controller
service
repository
entity
dto
enums
error
support
```

MVP 초반에는 `domain.user`, `domain.scenario`, `domain.play`, `domain.ai` 중심으로 시작한다.  
리뷰/북마크는 1차 MVP 확장 시 분리하고, 거래/인증은 후속 단계에서 분리한다.

---

## 3. 계층별 책임

### 3.1 Controller

Controller는 HTTP 경계만 담당한다.

```text
Request DTO validation
현재 사용자 ID 조회
Service 호출
ApiResponse로 응답 반환
```

Controller에서 하면 안 되는 일:

```text
비즈니스 로직 작성
Entity 직접 조작
Repository 직접 호출
AI 프롬프트 생성
복잡한 권한 판단
트랜잭션 처리
```

### 3.2 Service

Service는 비즈니스 로직과 트랜잭션 경계를 담당한다.

```text
Entity 조회/생성/변경
Repository 호출
도메인 정책 검증
접근 권한 확인
다른 Service와 협력
Response DTO 생성
```

Controller가 Entity를 DTO로 직접 변환하지 않게 한다.

### 3.3 Repository

Repository는 저장소 접근만 담당한다.

```text
Entity 저장/조회/삭제
단순 JpaRepository 쿼리
QueryDSL 기반 복잡 조회
```

Repository에서 권한, AI 호출, 도메인 정책을 판단하지 않는다.

### 3.4 Entity

Entity는 DB 매핑과 도메인 상태 변경을 담당한다.

```text
DB 테이블 매핑
상태 변경 메서드
생성/수정 시 불변 조건 보호
```

Entity는 Request/Response DTO, Controller, Service, 외부 API에 의존하지 않는다.  
setter를 무분별하게 열지 말고 의미 있는 상태 변경 메서드를 둔다.

### 3.5 DTO

Request DTO와 Response DTO는 분리한다.

```text
ScenarioCreateRequest
ScenarioUpdateRequest
ScenarioSummaryResponse
ScenarioDetailResponse
EvidenceResponse
SuspectResponse
```

API DTO의 ID 필드명은 `CaseLab_AI_API_Spec.md`를 따른다.

| 대상 | DTO 필드명 |
|---|---|
| Scenario | `scenarioId` |
| PlaySession | `sessionId` |
| Suspect | `suspectId` |
| Evidence | `evidenceId` |
| Hint | `hintId` |
| Review | `reviewId` |

엔티티 내부 PK가 `id`여도 응답 DTO에는 도메인별 ID 이름을 사용한다.

---

## 4. 공통 응답과 예외 처리

### 4.1 ApiResponse

모든 API는 공통 응답 래퍼를 사용한다.

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

### 4.2 ErrorCode

공통 에러와 도메인 에러를 분리한다.

```text
CommonErrorCode
ScenarioErrorCode
PlaySessionErrorCode
EvidenceErrorCode
AiErrorCode
UserErrorCode
```

### 4.3 BusinessException

도메인 예외는 `BusinessException` 계열로 처리한다.

금지:

```java
throw new RuntimeException("시나리오가 없습니다.");
```

권장:

```java
throw new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
```

---

## 5. 트랜잭션 규칙

### 5.1 조회

조회 전용 Service 메서드는 `readOnly = true`를 사용한다.

```java
@Transactional(readOnly = true)
```

### 5.2 변경

생성, 수정, 삭제, 상태 변경은 일반 트랜잭션을 사용한다.

```java
@Transactional
```

### 5.3 외부 AI 호출

트랜잭션을 오래 잡은 채 외부 AI API를 호출하지 않는다.

권장 흐름:

```text
DB 조회
트랜잭션 종료
AI 호출
트랜잭션 시작
AI 결과 저장
트랜잭션 종료
```

상태 일관성이 필요한 경우 상태값을 명확히 둔다.

```text
PENDING
VALIDATING
AI_RUNNING
PASSED
PASSED_WITH_WARNINGS
NEEDS_FIX
FAILED
```

---

## 6. 사용자 식별과 접근 권한

### 6.1 MockUserProvider

초기 MVP에서는 인증 없이 MockUser를 사용한다.

```java
@Component
public class MockUserProvider {

    private final Long mockUserId;

    public MockUserProvider(@Value("${app.mock-user-id:1}") Long mockUserId) {
        this.mockUserId = mockUserId;
    }

    public Long currentUserId() {
        return mockUserId;
    }
}
```

코드에 `1L`을 직접 하드코딩하지 않는다.

금지:

```java
Long userId = 1L;
```

권장:

```java
Long userId = mockUserProvider.currentUserId();
```

### 6.2 ScenarioAccessService

시나리오 접근 권한 판단은 `ScenarioAccessService` 한 곳에 모은다.

MVP 초기 구현:

```java
@Service
public class ScenarioAccessService {

    public boolean canPlay(Long userId, Long scenarioId) {
        return true;
    }

    public boolean canEdit(Long userId, Long scenarioId) {
        return true;
    }

    public boolean canView(Long userId, Long scenarioId) {
        return true;
    }
}
```

나중에 반영할 조건:

```text
공식 시나리오인가?
공개 시나리오인가?
내가 만든 시나리오인가?
구매/언락한 시나리오인가?
신고/차단된 시나리오인가?
```

Controller에서 접근 권한을 직접 판단하지 않는다.

---

## 7. 도메인 구현 규칙

### 7.1 Scenario

핵심 책임:

```text
공식/커스텀 시나리오 관리
목록/상세 조회
커스텀 시나리오 생성/수정
공개/비공개 상태 관리
```

필수 확장 필드:

```text
creatorId
visibility
scenarioType
priceCredit
status
```

MVP에서는 `priceCredit = 0`으로 시작한다.
거래 기능이 붙기 전까지 `ScenarioAccessService.canPlay()`는 무료 접근으로 처리한다.

### 7.2 PlaySession

핵심 책임:

```text
사용자별 플레이 진행 상태 저장
게임 시작
대시보드 조회
증거 해금 상태 조회
최종 추리 제출 여부 관리
```

`PlaySession`에는 `userId`를 포함한다.  
시나리오 자체 상태와 플레이 세션 상태를 혼동하지 않는다.

권장 상태:

```text
PLAYING
SUBMITTED
COMPLETED
ABANDONED
```

최종 추리는 세션당 1회 제출을 기본 정책으로 한다.

### 7.3 Evidence

핵심 책임:

```text
증거 카드 관리
초기 공개 증거 구분
조건/시간 기반 해금
해금된 증거만 사용자에게 노출
```

AI 심문에는 현재 해금된 증거만 전달한다.  
아직 해금되지 않은 핵심 증거를 프롬프트에 넣지 않는다.

### 7.4 Suspect

핵심 책임:

```text
용의자 공개 프로필 관리
공개 진술/알리바이 관리
AI 답변에 필요한 공개 정보 제공
```

범인 여부, 핵심 비밀, 정답 조각은 AI 프롬프트로 직접 넘기지 않는다.

### 7.5 Hint

핵심 책임:

```text
힌트 목록 조회
힌트 사용 처리
점수 패널티 관리
```

힌트 사용 API는 아래 경로로 통일한다.

```text
POST /api/play-sessions/{sessionId}/hints/{hintId}/use
```

### 7.6 Review / Bookmark

리뷰:

```text
플레이 완료 후 작성 가능
평점과 내용 저장
스포일러 여부 관리
```

북마크:

```text
사용자-시나리오 단위로 중복 방지
목록 조회는 인증 도입 후 완성
```

### 7.7 파일 / 이미지 처리

이미지는 DB에 바이너리로 직접 저장하지 않는다.

금지:

```text
evidence.image_blob
suspect.profile_image_blob
scenario.thumbnail_blob
```

권장:

```text
imageUrl
imageKey
thumbnailUrl
profileImageUrl
```

초기 MVP에서는 Mock URL 또는 정적 샘플 URL을 사용할 수 있다.
운영에서는 Cloudflare R2 또는 AWS S3 같은 Object Storage에 파일을 저장하고, DB에는 URL/Key만 저장한다.

파일 업로드를 붙일 때는 아래 원칙을 따른다.

```text
Controller는 multipart 요청 검증만 담당한다.
파일 저장 책임은 별도 StorageService로 분리한다.
도메인 Entity에는 저장소 URL/Key만 반영한다.
Android에는 이미지 바이너리가 아니라 접근 가능한 이미지 URL을 내려준다.
실제 Storage provider는 infrastructure 계층에 둔다.
```

---

## 8. AI 도메인 구현 규칙

### 8.1 AI 코드는 domain.ai에 둔다

AI 호출 관련 코드는 `domain.ai`에 집중한다.

```text
AiClient
PromptTemplateService
NpcInterrogationService
ScenarioValidationAiService
FinalDeductionScoringAiService
AiGenerationLogService
ResponsePolicyResolver
```

ScenarioService나 PlaySessionService에서 프롬프트 문자열을 직접 조립하지 않는다.

### 8.2 AI에게 전달하지 않는 정보

NPC 심문 프롬프트에는 아래 정보를 직접 넣지 않는다.

```text
진범
범행 방법 전체
범행 동기 전체
은폐 방법 전체
아직 해금되지 않은 비밀
아직 공개되지 않은 핵심 증거
Solution 전체
```

AI에게 전달 가능한 정보:

```text
용의자 공개 프로필
공개 진술/알리바이
현재 해금된 증거
사용자 질문
사용자가 제시한 증거
ResponsePolicyResolver가 결정한 답변 정책
답변 길이 제한
```

### 8.3 AI 심문 흐름

```text
사용자 질문
→ PlaySession 조회
→ 현재 해금 증거 조회
→ 용의자 조회
→ ResponsePolicyResolver가 답변 정책 결정
→ PromptTemplateService가 프롬프트 생성
→ AiClient 호출
→ InterrogationLog 저장
→ 응답 반환
```

### 8.4 ResponsePolicyResolver

`ResponsePolicyResolver`는 AI가 어떤 태도로 답해야 하는지 결정한다.

입력:

```text
sessionId
suspectId
question
presentedEvidenceId
unlockedEvidenceIds
```

출력:

```text
responsePolicyText
allowedFacts
forbiddenFacts
tone
```

AI가 정책을 직접 판단하게 하지 않는다.  
백엔드가 정책을 결정하고, AI는 정책을 1~2줄 자연어 답변으로 변환한다.

### 8.5 AI 실패 Fallback

AI 호출 실패 시 게임 흐름이 멈추면 안 된다.

Fallback 후보:

```text
용의자 기본 답변 반환
재질문 안내 문구 반환
추천 질문 기반 정적 답변 반환
AI 실패 로그 저장
```

---

## 9. 커스텀 시나리오와 AI 검증

### 9.1 커스텀 시나리오 상태

커스텀 시나리오는 처음에는 `DRAFT`로 생성한다.

```text
DRAFT
VALIDATING
PUBLISHED
HIDDEN
DELETED
```

### 9.2 공개 전 최소 조건

공개 전에는 최소 조건을 확인한다.

```text
제목 존재
용의자 1명 이상
증거 1개 이상
힌트 1개 이상
정답 존재
AI 검증 결과가 통과 또는 경고 수준
```

### 9.3 ScenarioValidationStatus

AI 검증 결과 상태는 아래 값을 사용한다.

```text
PENDING
PASSED
PASSED_WITH_WARNINGS
NEEDS_FIX
FAILED
```

`PENDING`은 검증 생성 직후 기본 상태로 사용할 수 있다.

---

## 10. 인증/인가 확장 계획

### 10.1 Phase 1: MVP Mock 단계

```text
로그인 없음
MockUserProvider 사용
MOCK_USER_ID=1
모든 시나리오 접근 허용
모든 시나리오 무료 취급
```

### 10.2 Phase 2: JWT 인증 도입

목표:

```text
회원가입 / 로그인
JWT 기반 사용자 식별
내 시나리오 / 내 플레이 기록 / 내 북마크 구분
```

변경:

```text
MockUserProvider를 SecurityContext 기반 provider로 교체
creatorId 기반 수정 권한 적용
PRIVATE 시나리오는 작성자만 접근
UNLISTED 시나리오는 링크 기반 접근
PUBLIC / OFFICIAL 시나리오는 공개 접근
```

### 10.3 인증 도입 후 인가 정책

| 액션 | 정책 |
|---|---|
| 시나리오 수정 | `creatorId == currentUserId` |
| 시나리오 삭제 | 작성자 또는 관리자 |
| 비공개 시나리오 조회 | 작성자만 |
| 플레이 시작 | `ScenarioAccessService.canPlay()` |
| 정답 조회 | 작성자 또는 관리자 전용, Android 플레이 화면 호출 금지 |

---

## 11. 거래/크레딧 확장 계획

### 11.1 Phase 3: 크레딧/구매 도입

목표:

```text
무료/유료 시나리오 구분
Mock 크레딧 충전
크레딧 기반 시나리오 구매
구매한 시나리오 접근 권한 생성
구매 내역 조회
```

### 11.2 확장 도메인 후보

| 도메인 | 역할 |
|---|---|
| CreditWallet | 사용자 크레딧 잔액 관리 |
| CreditTransaction | 충전/사용/환불 거래 로그 |
| ScenarioPurchase | 시나리오 구매 기록 |
| ScenarioAccess | 구매/소유/관리자 권한 기록 |
| CreatorSettlementMock | 제작자 정산 Mock |

### 11.3 거래 도입 후 ScenarioAccessService

거래 도입 후 `canPlay()`는 아래 조건을 순서대로 확인한다.

```text
공식 시나리오인가?
무료 시나리오인가?
내가 만든 시나리오인가?
구매한 시나리오인가?
관리자 권한인가?
```

### 11.4 거래 동시성 고려

중복 구매와 잔액 차감은 동시성 제어가 필요하다.

권장 방어:

```text
wallet row에 비관락 또는 낙관락 적용
user_id + scenario_id Unique 제약으로 중복 구매 방지
결제/구매 요청 idempotency key 검토
Redis Lock은 다중 인스턴스에서만 후보로 검토
```

MVP에서는 거래 테이블과 API를 만들지 않는다.  
단, `priceCredit`, `creatorId`, `visibility`, `scenarioType` 필드는 유지한다.

---

## 12. 로깅 규칙

### 12.1 AI 호출 로그

AI 호출은 추적 가능해야 한다.

```text
요청 타입
scenarioId
sessionId
suspectId
사용 모델
성공/실패
실패 사유
응답 시간
토큰/비용 정보
```

프롬프트 전문 저장은 민감도와 비용을 고려해 별도 정책을 둔다.

### 12.2 게임 로그

플레이 흐름에서 최소한 아래 이벤트는 추적 가능해야 한다.

```text
게임 시작
증거 해금
힌트 사용
심문 요청
최종 추리 제출
결과 조회
```

---

## 13. 테스트 기준

우선 테스트 대상:

```text
ScenarioAccessService
MockUserProvider
시나리오 목록/상세 조회
게임 세션 시작
증거 해금/조회
AI 심문 Fallback
최종 추리 중복 제출 방지
커스텀 시나리오 공개 전 검증
리뷰/북마크 중복 방지
```

테스트 이름은 행위와 기대 결과가 드러나게 작성한다.

```text
startSession_withValidScenario_createsPlaySession
submitFinalDeduction_twice_throwsAlreadySubmitted
canPlay_withMockUser_returnsTrue
```

---

## 14. 개발 AI 금지 사항

개발 AI는 아래 작업을 하지 않는다.

```text
AI NPC 프롬프트에 범인 정보를 직접 넣기
Solution 전체를 AI 심문 프롬프트에 전달하기
해금되지 않은 증거를 사용자나 AI에게 공개하기
Controller에서 비즈니스 로직 처리하기
Repository를 Controller에서 직접 호출하기
Entity를 API 응답으로 직접 반환하기
userId = 1L 하드코딩하기
ScenarioAccessService를 우회해서 권한 판단하기
최종 추리 채점을 단순 문자열 비교로 끝내기
시나리오 상태와 플레이 세션 상태를 혼동하기
거래/인증을 1차 MVP 필수 흐름처럼 구현하기
```

---

## 15. PR 체크리스트

- [ ] Controller는 요청/응답 경계만 담당하는가?
- [ ] Service에 트랜잭션 경계가 있는가?
- [ ] API 응답은 `ApiResponse` 래퍼를 사용하는가?
- [ ] Entity를 직접 응답으로 반환하지 않는가?
- [ ] 예외는 `BusinessException`과 `ErrorCode`를 사용하는가?
- [ ] 현재 사용자 ID는 `MockUserProvider`를 통해 얻는가?
- [ ] 시나리오 접근 판단은 `ScenarioAccessService`를 통하는가?
- [ ] AI 프롬프트에 범인/정답/미해금 비밀이 들어가지 않는가?
- [ ] AI 호출 실패 시 Fallback이 있는가?
- [ ] 심문 로그가 저장되는가?
- [ ] DTO ID 필드명이 API Spec과 일치하는가?
- [ ] 인증/거래/크레딧 기능이 후순위로 분리되어 있는가?

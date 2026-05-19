# CaseLab AI - Backend Implementation Rules

> 이 문서는 CaseLab AI 백엔드를 구현할 때 팀원과 개발용 AI가 반드시 따라야 하는 구현 기준 문서입니다.  
> 목적은 **AI가 제각각 다른 구조의 코드를 만들지 않도록 방지**하고, Spring Boot 백엔드의 패키지 구조, 계층 책임, 공통 응답, 예외 처리, MockUser, AI 호출 경계를 통일하는 것입니다.

---

## 0. 문서 목적

CaseLab AI는 단순 CRUD 서비스가 아니라 다음 요소가 함께 있는 프로젝트입니다.

```text
AI 용의자 심문형 추리게임
+ 공식 시나리오
+ 커스텀 시나리오
+ 증거 카드
+ 증거 해금 상태
+ 플레이 세션
+ 최종 추리 제출
+ AI 시나리오 검증
+ 향후 인증/거래 확장
```

따라서 개발용 AI가 단편 기능만 보고 구현하면 아래 문제가 생길 수 있습니다.

```text
- Controller에 비즈니스 로직을 작성함
- Entity를 요청/응답 DTO로 직접 사용함
- 기존 ERD와 API 명세를 무시하고 새 테이블/필드를 마음대로 추가함
- AI NPC에게 범인 정보와 전체 정답을 직접 전달함
- 인증/거래를 지금 당장 과하게 구현함
- 반대로 userId, creatorId, visibility, priceCredit 같은 확장 필드를 없애버림
- 공통 응답 형식과 예외 처리 규칙을 사용하지 않음
```

이 문서는 위 문제를 막기 위한 **백엔드 구현 기준선**입니다.

---

## 1. 개발 AI가 먼저 알아야 할 전제

### 1.1 현재 MVP 우선순위

현재 우선순위는 다음입니다.

```text
1. 공식 시나리오 1개 조회
2. 게임 세션 시작
3. 증거 / 용의자 / 힌트 조회
4. AI 용의자 심문
5. 최종 추리 제출
6. 결과 해설 조회
7. 커스텀 시나리오 제작 기본형
8. AI 시나리오 검증 기본형
9. 리뷰 / 북마크 기본형
```

인증/인가와 거래 기능은 나중에 붙입니다.  
다만 나중에 붙일 수 있도록 확장 지점은 반드시 유지합니다.

---

### 1.2 현재 보류 기능

아래 기능은 지금 당장 구현하지 않습니다.

```text
JWT 인증 완성
실제 결제
크레딧 구매
유료 시나리오 접근 제한
제작자 정산
랜덤 매칭 멀티플레이
음성 심문
3D 맵
실제 사건 직접 재현
```

하지만 아래 필드는 유지합니다.

```text
user_id
creator_id
visibility
scenario_type
price_credit
status
```

---

### 1.3 현재 사용자 처리 방식

초기 MVP에서는 인증을 붙이지 않고 임시 사용자로 개발합니다.

```text
MockUserProvider.currentUserId() 사용
기본값은 `MOCK_USER_ID=1`
```

단, 코드 여기저기에 `1L`을 직접 박으면 안 됩니다.

좋은 방식:

```java
Long userId = mockUserProvider.currentUserId();
```

나쁜 방식:

```java
Long userId = 1L; // 여러 Service/Controller에 직접 하드코딩
```

나중에 JWT 인증을 붙이면 `MockUserProvider`를 SecurityContext 기반 provider로 교체합니다.

---

## 2. 추천 패키지 구조

현재 프로젝트 루트 패키지는 `com.startup`입니다.  
아래 구조를 기준으로 새 도메인 패키지를 추가합니다.

```text
com.startup
 ├─ common
 │   ├─ config
 │   ├─ dto
 │   ├─ error
 │   ├─ auth
 │   ├─ util
 │
 ├─ domain
 │   ├─ user
 │   │   ├─ controller
 │   │   ├─ service
 │   │   ├─ repository
 │   │   ├─ entity
 │   │   ├─ dto
 │   │   └─ error
 │   │
 │   ├─ scenario
 │   │   ├─ controller
 │   │   ├─ service
 │   │   ├─ repository
 │   │   ├─ entity
 │   │   ├─ dto
 │   │   ├─ error
 │   │   └─ support
 │   │
 │   ├─ play
 │   │   ├─ controller
 │   │   ├─ service
 │   │   ├─ repository
 │   │   ├─ entity
 │   │   ├─ dto
 │   │   ├─ error
 │   │   └─ support
 │   │
 │   ├─ ai
 │   │   ├─ controller
 │   │   ├─ service
 │   │   ├─ dto
 │   │   ├─ prompt
 │   │   ├─ client
 │   │   ├─ error
 │   │   └─ support
 │   │
 │   ├─ review
 │   └─ bookmark
 │
 └─ infrastructure
     ├─ persistence
     └─ redis
```

### 2.1 초반에는 너무 잘게 나누지 않는다

MVP 초반에는 아래 4개 도메인 중심으로 시작해도 됩니다.

```text
domain.user
domain.scenario
domain.play
domain.ai
```

리뷰/북마크는 1차 MVP 확장 구현 시 분리하고, 거래/인증은 후속 단계에서 분리합니다.

---

## 3. 계층별 책임

### 3.1 Controller

Controller의 역할은 다음으로 제한합니다.

```text
- HTTP 요청 받기
- Request DTO validation
- 현재 사용자 ID 조회
- Service 호출
- ApiResponse로 응답 반환
```

Controller에서 하면 안 되는 것:

```text
- 비즈니스 로직 작성
- Entity 직접 조작
- Repository 직접 호출
- AI 프롬프트 생성
- DB 트랜잭션 처리
- 복잡한 if/else 상태 판단
```

좋은 예시:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final MockUserProvider mockUserProvider;

    @GetMapping
    public ApiResponse<PageResponse<ScenarioSummaryResponse>> getScenarios(
            ScenarioSearchCondition condition,
            Pageable pageable
    ) {
        Long userId = mockUserProvider.currentUserId();
        PageResponse<ScenarioSummaryResponse> response =
                scenarioService.getScenarios(userId, condition, pageable);

        return ApiResponse.success(response);
    }
}
```

나쁜 예시:

```java
@GetMapping
public List<Scenario> getScenarios() {
    return scenarioRepository.findAll();
}
```

---

### 3.2 Service

Service의 역할은 다음입니다.

```text
- 핵심 비즈니스 로직 처리
- 트랜잭션 경계 설정
- Entity 조회/생성/변경
- Repository 호출
- 도메인 정책 검증
- 다른 Service와 협력
```

Service에서 반환하는 값은 가능하면 Response DTO입니다.  
Controller에서 Entity를 DTO로 직접 변환하지 않게 합니다.

---

### 3.3 Repository

Repository의 역할은 다음입니다.

```text
- Entity 저장/조회/삭제
- 단순 JpaRepository 쿼리
- QueryDSL 기반 복잡 조회
```

Repository에서 하면 안 되는 것:

```text
- 비즈니스 정책 판단
- 사용자 권한 검증
- AI 호출
- DTO 변환 로직 과도하게 작성
```

---

### 3.4 Entity

Entity의 역할은 다음입니다.

```text
- DB 테이블 매핑
- 도메인 상태 변경 메서드 보유
- 생성/수정 시 불변 조건 일부 보호
```

Entity에서 하면 안 되는 것:

```text
- Request DTO 의존
- Response DTO 의존
- AI 호출
- 외부 API 호출
- Controller/Service 의존
```

Entity는 `setter`를 무분별하게 열지 않습니다.  
상태 변경은 의미 있는 메서드로 표현합니다.

예시:

```java
public void publish() {
    if (!this.status.canPublish()) {
        throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH);
    }
    this.status = ScenarioStatus.PUBLISHED;
    this.publishedAt = LocalDateTime.now();
}
```

---

### 3.5 DTO

DTO는 요청/응답을 명확히 분리합니다.

```text
Request DTO
- 외부에서 들어오는 값
- Validation annotation 포함

Response DTO
- 외부로 나가는 값
- Entity를 직접 노출하지 않음
```

예시:

```text
ScenarioCreateRequest
ScenarioUpdateRequest
ScenarioSummaryResponse
ScenarioDetailResponse
EvidenceResponse
SuspectResponse
```

---

## 4. 공통 응답 규칙

모든 API는 공통 응답 형식을 사용합니다.

### 4.1 성공 응답

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### 4.2 실패 응답

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

### 4.3 규칙

```text
- Controller는 Entity를 직접 반환하지 않는다.
- Controller는 ApiResponse.success(data)를 사용한다.
- 실패 응답은 직접 만들지 말고 예외를 던진다.
- GlobalExceptionHandler가 실패 응답을 만든다.
```

나쁜 방식:

```java
return ResponseEntity.status(404).body("없음");
```

좋은 방식:

```java
throw new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
```

---

## 5. 예외 처리 규칙

### 5.1 ErrorCode 분리

공통 에러와 도메인 에러를 분리합니다.

```text
CommonErrorCode
ScenarioErrorCode
PlaySessionErrorCode
EvidenceErrorCode
AiErrorCode
UserErrorCode
```

### 5.2 BusinessException 사용

도메인 예외는 `BusinessException` 계열로 처리합니다.

예시:

```java
public class ScenarioException extends BusinessException {
    public ScenarioException(ScenarioErrorCode errorCode) {
        super(errorCode);
    }
}
```

### 5.3 예외 메시지 직접 문자열 남발 금지

나쁜 방식:

```java
throw new RuntimeException("시나리오가 없습니다.");
```

좋은 방식:

```java
throw new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
```

---

## 6. 트랜잭션 규칙

### 6.1 조회 메서드

조회 전용 Service 메서드는 다음을 사용합니다.

```java
@Transactional(readOnly = true)
```

### 6.2 변경 메서드

생성/수정/삭제/상태 변경은 다음을 사용합니다.

```java
@Transactional
```

### 6.3 트랜잭션 안에서 외부 AI 호출 주의

AI 호출은 시간이 오래 걸릴 수 있습니다.  
가능하면 DB 트랜잭션을 오래 잡은 채 AI를 호출하지 않습니다.

나쁜 흐름:

```text
트랜잭션 시작
DB 조회
AI 호출 10초 대기
DB 저장
트랜잭션 종료
```

좋은 흐름:

```text
DB 조회
트랜잭션 종료
AI 호출
트랜잭션 시작
AI 결과 저장
트랜잭션 종료
```

단, 상태 일관성이 필요한 경우에는 별도 상태값을 사용합니다.

```text
VALIDATING
AI_RUNNING
AI_FAILED
PUBLISHED
```

---

## 7. MockUser / 인증 확장 규칙

### 7.1 MockUserProvider 사용

초기에는 MockUser를 사용하지만, 코드는 나중에 JWT로 바꿀 수 있어야 합니다.

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

나중에 JWT 구현:

```java
@Component
public class SecurityUserProvider {

    public Long currentUserId() {
        // SecurityContext에서 userId 조회
    }
}
```

### 7.2 코드에 userId = 1L 직접 하드코딩 금지

금지:

```java
Long userId = 1L;
```

허용:

```java
Long userId = mockUserProvider.currentUserId();
```

---

## 8. ScenarioAccessService 확장 규칙

인증/거래를 나중에 붙이기 위해 접근 권한 검사를 담당하는 서비스를 둡니다.

```java
@Service
public class ScenarioAccessService {

    public boolean canPlay(Long userId, Long scenarioId) {
        return true; // MVP 초기에는 전부 허용
    }

    public boolean canEdit(Long userId, Long scenarioId) {
        return true; // MVP 초기에는 전부 허용
    }

    public boolean canView(Long userId, Long scenarioId) {
        return true; // MVP 초기에는 전부 허용
    }
}
```

나중에 확장:

```text
- 공식 시나리오인가?
- 공개 시나리오인가?
- 내가 만든 시나리오인가?
- 구매/언락한 시나리오인가?
- 비공개 시나리오인가?
```

이 검증이 모두 `ScenarioAccessService`로 들어갑니다.

Controller나 각 Service에 권한 검사를 흩뿌리지 않습니다.

---

## 9. 도메인 구현 우선순위

### 9.1 1차 구현

```text
Scenario
Suspect
Evidence
Hint
Solution
PlaySession
UnlockedEvidence
InterrogationLog
FinalDeduction
```

### 9.2 2차 구현

```text
Review
Bookmark
ScenarioValidationResult
AiGenerationLog
```

### 9.3 3차 구현

```text
User Authentication
CreditWallet
ScenarioPurchase
ScenarioAccess
CreatorSettlementMock
```

현재는 1차 구현에 집중합니다.

---

## 10. Scenario 도메인 구현 규칙

### 10.1 핵심 API

```text
GET /api/scenarios
GET /api/scenarios/{scenarioId}
POST /api/scenarios
PATCH /api/scenarios/{scenarioId}
```

### 10.2 목록 조회

시나리오 목록은 페이징을 기본으로 합니다.

정렬 후보:

```text
latest
popular
rating
playCount
```

필터 후보:

```text
scenarioType
difficulty
visibility
keyword
estimatedTime
```

### 10.3 상세 조회

상세 조회는 다음 정보를 포함합니다.

```text
시나리오 기본 정보
난이도
예상 플레이 시간
용의자 수
증거 수
평점
플레이 수
북마크 여부
시놉시스
```

단, 정답 정보는 포함하지 않습니다.

```text
culprit
motive
method
solution
```

위 정보는 플레이어에게 노출되면 안 됩니다.

---

## 11. PlaySession 도메인 구현 규칙

### 11.1 게임 시작

```text
POST /api/play-sessions
```

게임 시작 시 해야 할 일:

```text
1. 시나리오 접근 가능 여부 확인
2. PlaySession 생성
3. 초기 공개 증거 unlock 처리
4. 상태 PLAYING 설정
5. 시작 시간 기록
```

### 11.2 PlaySession 상태

```text
PLAYING
COMPLETED
ABANDONED
```

### 11.3 중복 최종 제출 방지

한 PlaySession은 최종 추리를 한 번만 제출할 수 있습니다.

```text
final_deductions.play_session_id UNIQUE
```

또는 Service에서 상태 검증합니다.

```java
if (playSession.isCompleted()) {
    throw new PlaySessionException(PlaySessionErrorCode.ALREADY_COMPLETED);
}
```

---

## 12. Evidence 도메인 구현 규칙

### 12.1 증거 공개 구분

증거는 다음 상태를 가집니다.

```text
초기 공개 증거
시간 해금 증거
심문 후 해금 증거
증거 제시 후 해금 증거
수동 해금 증거
```

초기 MVP에서는 다음만 구현해도 됩니다.

```text
초기 공개 증거
시간 해금 증거
```

### 12.2 해금된 증거만 사용자에게 보여준다

사용자가 `GET /api/play-sessions/{sessionId}/evidences`를 호출하면, 해당 PlaySession에서 해금된 증거만 반환합니다.

시나리오의 모든 증거를 그대로 반환하면 안 됩니다.

나쁜 방식:

```java
evidenceRepository.findAllByScenarioId(scenarioId);
```

좋은 방식:

```java
unlockedEvidenceRepository.findUnlockedEvidenceBySessionId(sessionId);
```

---

## 13. AI 도메인 구현 규칙

### 13.1 AI 호출은 domain.ai로 분리

AI 호출 관련 코드는 `domain.ai`에 둡니다.

```text
AiClient
PromptTemplateService
NpcInterrogationService
ScenarioValidationAiService
FinalDeductionScoringAiService
AiGenerationLogService
```

ScenarioService나 PlaySessionService에서 프롬프트 문자열을 직접 만들지 않습니다.

### 13.2 AI에게 전체 정답을 주지 않는다

특히 NPC 심문에서는 아래 정보를 AI에게 직접 주지 않습니다.

```text
진범
범행 방법 전체
범행 동기 전체
은폐 방법 전체
아직 해금되지 않은 비밀
아직 공개되지 않은 핵심 증거
```

AI에게 전달하는 정보:

```text
용의자 공개 프로필
현재 공개된 증거
현재 질문
제시한 증거
ResponsePolicyResolver가 결정한 답변 정책
답변 길이 제한
```

### 13.3 AI 심문 흐름

```text
사용자 질문
  ↓
PlaySession 조회
  ↓
현재 해금 증거 조회
  ↓
용의자 조회
  ↓
ResponsePolicyResolver가 답변 정책 결정
  ↓
PromptTemplateService가 프롬프트 생성
  ↓
AiClient 호출
  ↓
InterrogationLog 저장
  ↓
응답 반환
```

### 13.4 AI 실패 시 Fallback

AI 호출 실패 시 게임이 완전히 멈추면 안 됩니다.

Fallback 예시:

```text
1. 용의자 기본 답변 반환
2. "지금은 답변을 정리하지 못했습니다. 다른 질문을 시도해 주세요." 안내
3. 추천 질문 기반 정적 답변 반환
```

---

## 14. ResponsePolicy 구현 규칙

### 14.1 ResponsePolicyResolver

`ResponsePolicyResolver`는 AI가 어떤 태도로 답해야 하는지 결정합니다.

입력:

```text
playSessionId
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

예시:

```text
조건:
카페 결제 내역이 아직 공개되지 않음

정책:
커피 구매 사실을 부인한다.
```

```text
조건:
카페 결제 내역이 공개됨

정책:
커피 구매는 인정하되 피해자에게 준 것은 아니라고 주장한다.
```

### 14.2 AI가 정책을 결정하게 하지 않는다

나쁜 방식:

```text
AI야, 이 용의자가 어떻게 답해야 할지 알아서 판단해.
```

좋은 방식:

```text
백엔드가 답변 정책을 결정하고,
AI는 그 정책을 1~2줄 자연어로 변환한다.
```

---

## 15. 커스텀 시나리오 구현 규칙

### 15.1 커스텀 시나리오 생성

커스텀 시나리오는 처음에는 DRAFT 상태로 생성합니다.

```text
DRAFT
VALIDATING
PUBLISHED
HIDDEN
DELETED
```

### 15.2 공개 전 검증

커스텀 시나리오는 공개 전에 최소 조건을 만족해야 합니다.

```text
제목이 존재함
용의자가 1명 이상 존재함
증거가 1개 이상 존재함
힌트가 1개 이상 존재함
정답이 설정됨
범인을 가리키는 핵심 증거가 존재함
```

공개 전 AI 검증은 호출합니다. 단, 개발 중 임시 저장 단계에서는 생략할 수 있습니다.

### 15.3 검증 결과 저장

AI 검증 결과는 저장합니다.

```text
scenario_validation_results
- validation_status
- validation_score
- problem_summary
- suggestion
```

---

## 16. 리뷰 / 북마크 구현 규칙

### 16.1 리뷰

리뷰는 시나리오 플레이 완료 후 작성하는 것이 원칙입니다.  
초기 MVP에서는 이 제한을 생략할 수 있지만, 확장 시 적용합니다.

리뷰 필드:

```text
rating
content
is_spoiler
```

스포일러 리뷰는 UI에서 접어서 표시합니다.

### 16.2 북마크

같은 사용자가 같은 시나리오를 중복 북마크하지 않도록 합니다.

DB Unique Constraint 권장:

```text
user_id + scenario_id
```

---

## 17. 거래 / 인증 확장 대비 규칙

현재 거래/인증은 구현하지 않지만 아래 구조를 유지합니다.

### 17.1 Scenario 필드 유지

```text
creator_id
visibility
scenario_type
status
price_credit
```

`price_credit`이 없으면 나중에 유료 시나리오 구매 구조를 붙이기 어렵습니다.

### 17.2 ScenarioAccessService 유지

현재는 모두 허용하더라도 `ScenarioAccessService`를 거쳐야 합니다.

나쁜 방식:

```java
scenarioService.getScenarioDetail(scenarioId);
```

좋은 방식:

```java
scenarioAccessService.validateViewable(userId, scenarioId);
scenarioService.getScenarioDetail(scenarioId);
```

### 17.3 구매/언락은 나중에 추가

나중에 추가할 도메인:

```text
CreditWallet
CreditTransaction
ScenarioPurchase
ScenarioAccess
CreatorSettlementMock
```

---

## 18. 파일 / 이미지 처리 규칙

이미지는 DB에 바이너리로 저장하지 않습니다.

나쁜 방식:

```text
evidence.image_blob
```

좋은 방식:

```text
evidence.image_url
evidence.image_key
```

초기에는 Mock URL을 사용해도 됩니다.  
운영 시 Cloudflare R2 또는 S3 계열 Object Storage를 사용합니다.

---

## 19. 로깅 규칙

### 19.1 AI 호출 로그

AI 호출은 반드시 로그로 남깁니다.

```text
request_type
model_name
input_tokens
output_tokens
latency_ms
result_status
error_code
created_at
```

### 19.2 게임 로그

주요 게임 이벤트는 저장합니다.

```text
게임 시작
증거 해금
힌트 사용
심문 질문
최종 추리 제출
게임 완료
```

---

## 20. 테스트 작성 기준

초기에는 모든 테스트를 완벽히 만들 필요는 없지만, 핵심 로직은 테스트합니다.

### 20.1 우선 테스트 대상

```text
Scenario 목록/상세 조회
PlaySession 시작 시 초기 증거 해금
FinalDeduction 중복 제출 방지
ScenarioAccessService 기본 동작
ResponsePolicyResolver 정책 결정
```

### 20.2 테스트 이름

테스트 이름은 한글 또는 설명형으로 작성합니다.

예시:

```java
@Test
void 게임을_시작하면_초기_공개_증거가_해금된다() {
}
```

---

## 21. 개발 AI가 하면 안 되는 것

개발용 AI는 아래 작업을 임의로 하면 안 됩니다.

```text
1. 기존 ERD에 없는 테이블을 마음대로 추가
2. 기존 API Spec과 다른 endpoint 생성
3. Entity를 Controller에서 직접 반환
4. Controller에서 Repository 직접 호출
5. RuntimeException 문자열 예외 남발
6. AI NPC에게 전체 정답/범인 정보 전달
7. 인증/거래를 갑자기 완성 구현
8. userId = 1L을 코드 여러 곳에 직접 하드코딩
9. 시나리오 상세 API에 정답 정보 포함
10. 해금되지 않은 증거를 사용자에게 노출
11. 프롬프트 문자열을 여러 Service에 분산 작성
12. 이미지 파일을 DB 바이너리로 저장
```

---

## 22. 개발 AI가 반드시 지켜야 할 것

```text
1. 기존 문서와 API Spec을 우선한다.
2. 코드 작성 전 관련 도메인 문서를 확인한다.
3. Entity, DTO, Service, Controller 책임을 분리한다.
4. 공통 응답 ApiResponse를 사용한다.
5. 도메인 예외는 ErrorCode 기반으로 처리한다.
6. MockUserProvider를 사용한다.
7. ScenarioAccessService를 통해 접근 권한 확장 지점을 유지한다.
8. AI 호출은 domain.ai에 모은다.
9. AI NPC에게는 현재 공개된 정보와 답변 정책만 전달한다.
10. MVP 범위를 넘는 기능은 제안만 하고 임의 구현하지 않는다.
```

---

## 23. PR 체크리스트

백엔드 PR을 올리기 전 아래를 확인합니다.

```text
[ ] API Spec과 endpoint가 일치하는가?
[ ] 공통 응답 형식을 사용하는가?
[ ] Entity를 직접 반환하지 않는가?
[ ] Controller에 비즈니스 로직이 없는가?
[ ] Service에 트랜잭션이 적절히 적용되었는가?
[ ] 도메인 예외가 ErrorCode 기반인가?
[ ] userId를 직접 하드코딩하지 않았는가?
[ ] 해금되지 않은 증거를 노출하지 않는가?
[ ] AI에게 전체 정답을 전달하지 않는가?
[ ] 테스트 또는 최소한 수동 테스트 결과가 있는가?
```

---

## 24. 구현 순서 권장안

### 24.1 1차

```text
Scenario 목록 / 상세
공식 시나리오 Seed Data
Suspect 조회
Evidence 조회
Hint 조회
```

### 24.2 2차

```text
PlaySession 시작
초기 증거 해금
시간 기반 증거 해금
InterrogationLog 저장
```

### 24.3 3차

```text
AI 용의자 심문
ResponsePolicyResolver
PromptTemplateService
AI Fallback
```

### 24.4 4차

```text
FinalDeduction 제출
채점
결과 해설
```

### 24.5 5차

```text
커스텀 시나리오 제작
AI 시나리오 검증
시나리오 공개/비공개
리뷰
북마크
```

### 24.6 6차

```text
내 기록
인증/거래 확장
```

---

## 25. 최종 요약

CaseLab AI 백엔드 구현의 핵심은 다음입니다.

```text
1. 게임 데이터는 DB와 Case Graph가 관리한다.
2. AI는 자유 창작자가 아니라 현재 장면의 Actor다.
3. 범인/정답/비밀은 백엔드가 secret으로 관리한다.
4. AI에게는 현재 공개된 정보와 답변 정책만 전달한다.
5. 인증/거래는 나중에 붙이되 확장 지점은 유지한다.
6. Controller, Service, Repository, DTO, Entity 책임을 분리한다.
7. 공통 응답과 예외 처리 규칙을 통일한다.
```

한 문장으로 정리하면:

> **백엔드는 사건의 진실과 게임 상태를 통제하고, AI는 허용된 범위 안에서 용의자처럼 짧게 답변한다.**

이 원칙을 지키면 CaseLab AI는 단순 챗봇 앱이 아니라, 안정적인 AI 추리게임 플랫폼으로 구현될 수 있습니다.

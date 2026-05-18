# CaseLab AI

AI 용의자를 심문하고, 증거와 알리바이를 조합해 범인·동기·범행 방법을 추리하는 Android 기반 AI 추리게임 플랫폼.
Android 앱(Kotlin) + Spring Boot 백엔드(Java) + AI 심문 엔진(LLM API) 구조.

---

## 기술 스택

```text
Java 21, Spring Boot 4, Spring Security, JWT
Spring Data JPA, MySQL, Redis
Spring AI (OpenAI / Claude / Gemini 택1)
Gradle, Docker / Docker Compose
Swagger (springdoc-openapi)
Prometheus + Grafana (모니터링)
```

---

## 빌드 / 실행

```bash
bash scripts/compose-up.sh     # Docker 빌드 + 실행 (MySQL, Redis, App)
bash scripts/compose-down.sh   # 종료

./gradlew bootJar              # jar 생성
./gradlew composeUp            # Gradle task로 실행
./gradlew test                 # 테스트
```

실행 후 URL:
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Grafana: `http://localhost:3000`

Spring AI 기본값은 `SPRING_AI_MODEL_CHAT=none`이다. AI 기능을 붙일 때만 `.env`에서 `openai`로 전환한다.

---

## 패키지 구조

```text
com.startup
 ├─ common/                  공통 설정, DTO, 에러, 유틸
 │   ├─ config/              SwaggerConfig, WebMvcConfig
 │   ├─ dto/                 ApiResponse, PageResponse, ErrorResponse
 │   ├─ entity/              BaseEntity
 │   ├─ error/               BusinessException, ErrorCode, GlobalExceptionHandler
 │   └─ util/                QueryDSL support 등
 ├─ domain/                  도메인별 비즈니스 로직
 │   └─ example/             패키지 템플릿 (새 도메인은 이 구조를 따른다)
 │       ├─ controller/
 │       ├─ dto/
 │       ├─ entity/
 │       ├─ enums/
 │       ├─ error/
 │       ├─ repository/
 │       ├─ service/
 │       └─ support/
 └─ infrastructure/          외부 시스템 연동
     ├─ persistence/
     └─ redis/               config, lock
```

새 도메인 패키지를 만들 때는 `domain/example/`의 controller-dto-entity-enums-error-repository-service-support 구조를 따른다.

---

## 팀 역할

| 담당자 | 역할 | 핵심 범위 |
|--------|------|----------|
| 황도윤 | 리더 / 인프라 / 공식 시나리오 / PR 리뷰 | 공통 세팅, Seed Data, 문서 최신화 |
| 배강혁 | AI 엔진 / 프롬프트 / AI 백엔드 | interrogation, final-deduction, validate |
| 소수경 | 핵심 백엔드 CRUD / 게임 세션 / 증거 해금 | scenarios, game-sessions, evidences, hints |
| 정채림 | Android UI / 화면 흐름 / API 연동 / QA | Android 화면, Mock → API 전환 |

다른 담당자의 패키지를 수정해야 할 때는 사유를 밝히고 최소 범위로 한정한다.
상세 역할 배분: 팀 공유 Notion 또는 리더에게 확인.

---

## AI 엔진 핵심 규칙

### 가장 중요한 원칙

AI 용의자에게 "네가 범인이다"라는 정보를 직접 주지 않는다.
범인 정보와 핵심 비밀은 백엔드 secret으로 관리한다.
AI에게는 현재 장면에서 말해도 되는 정보와 답변 정책만 전달한다.

### AI에게 전달하는 정보

```text
현재 용의자 공개 프로필
현재 용의자 공개 알리바이
현재 공개된 증거
사용자가 제시한 증거
현재 답변 정책 (ResponsePolicy)
사용자 질문
```

### AI에게 전달하면 안 되는 정보

```text
전체 정답
진범 여부
아직 해금되지 않은 핵심 비밀
다른 용의자의 비밀
최종 해설 전체
```

### NPC 응답 제약

```text
답변은 1~2줄로 제한한다.
설정에 없는 사실을 만들지 않는다.
해금되지 않은 비밀을 말하지 않는다.
결정적 증거가 제시되기 전까지 자백하지 않는다.
질문에 답할 수 없으면 "기억나지 않는다", "잘 모르겠다"로 답한다.
```

### 심문 처리 흐름

```text
사용자 질문 → play_session 조회 → suspect 조회
→ 현재 해금 evidence 조회 → presentedEvidenceId 확인
→ NpcResponsePolicyService가 답변 정책 결정
→ AI에게 허용된 정보만 전달 → 1~2줄 답변 생성
→ interrogation_logs 저장 → 조건 충족 시 추가 evidence 해금
```

### AI 서비스 구조

```text
AiInterrogationService     심문 응답 생성
AiDeductionScorer          최종 추리 채점
AiScenarioValidator        커스텀 시나리오 논리 검증
AiPromptBuilder            프롬프트 조립
AiUsageLogger              AI 호출 로그
```

---

## 담당 API

### 배강혁 담당

```text
POST /api/play-sessions/{sessionId}/interrogations      AI 용의자 심문
POST /api/play-sessions/{sessionId}/final-deduction      최종 추리 제출/채점
POST /api/ai/scenarios/{scenarioId}/validate             시나리오 논리 검증
```

### 소수경 담당

```text
GET  /api/scenarios                                      시나리오 목록
GET  /api/scenarios/{scenarioId}                         시나리오 상세
POST /api/game-sessions                                  게임 세션 생성
GET  /api/game-sessions/{sessionId}/evidences            해금 증거 조회
GET  /api/game-sessions/{sessionId}/suspects             용의자 조회
GET  /api/game-sessions/{sessionId}/hints                힌트 조회
```

API 명세 전체: `docs/CaseLab_AI_API_Spec.md`

---

## 점수 계산

```text
범인 선택: 30점 / 범행 방법: 25점 / 범행 동기: 20점
은폐 방법: 10점 / 결정적 증거 선택: 15점

힌트 감점: 1단계 -3점, 2단계 -5점, 3단계 -10점

등급: S(90+) A(80+) B(70+) C(60+) D(60-)
```

---

## Mock / Fallback 정책

AI API가 실패해도 시연 플로우가 끊기지 않아야 한다.
- `SPRING_AI_MODEL_CHAT=none`일 때 Mock 응답을 반환하는 Fallback 구현 필수
- 심문 Mock 응답, 채점 Mock 결과, 검증 Mock 결과를 준비한다.

---

## 금지 사항

```text
AI NPC에게 "너는 범인이다"라는 정보를 프롬프트에 넣지 않는다.
AI가 설정에 없는 사실을 만들어내는 구조를 허용하지 않는다.
심문 로그를 저장하지 않는 구조를 만들지 않는다.
최종 추리 채점을 단순 텍스트 비교로 구현하지 않는다.
시나리오와 플레이 세션 상태를 혼동하지 않는다.
모든 증거를 처음부터 공개하지 않는다.
스포일러 리뷰 관리 없이 리뷰를 공개하지 않는다.
```

---

## 참조 문서

```text
docs/CaseLab_AI_PRD.md                 제품 요구사항 전체
docs/CaseLab_AI_API_Spec.md            API 명세 전체 (Request/Response 포함)
docs/CaseLab_AI_Project_Planning.md    프로젝트 기획 / 도메인 모델 / AI 프롬프트 정책
docs/docker-run.md                     Docker 실행 / 에러 해결
docs/android-client.md                 Android 앱 연동
```

권장 읽기 순서: PRD → Project Planning → API Spec

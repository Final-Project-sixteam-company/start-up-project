# CaseLab AI — Codex Review Guide

AI 용의자 심문형 추리게임 플랫폼. Android(Kotlin) + Spring Boot(Java 21, Spring Boot 4) 백엔드.
프로젝트 상세 컨텍스트는 `CLAUDE.md`를 함께 참조한다.
구현 규칙 상세는 `docs/BACKEND_IMPLEMENTATION_GUIDE.md`를 따른다.

---

## 이름 / 도메인 기준

```text
공개 앱 이름: ClueRoom
서비스 도메인: clueroom.xyz
운영 API 도메인: https://api.clueroom.xyz
```

`CaseLab AI`, `CaseLab_AI`, `start-up`, `startup`, `com.startup`은 내부 레거시 식별자로 유지한다.
리뷰나 수정 중 패키지명, 문서 파일명, Gradle 프로젝트명, Docker/DB 이름을 앱명 변경만을 이유로 바꾸지 않는다.

---

## 프로젝트 핵심 제약

```text
AI 용의자에게 범인 정보를 직접 전달하지 않는다.
범인·정답·핵심 비밀은 백엔드 secret으로 관리한다.
AI에게는 현재 공개 정보 + 답변 정책만 전달한다.
NPC 답변은 1~2줄(최대 2문장)로 제한한다.
설정에 없는 사실을 생성하지 않는다.
답변 정책은 백엔드(ResponsePolicyResolver)가 결정한다 — AI가 판단하지 않는다.
트랜잭션 안에서 AI API를 호출하지 않는다.
```

---

## 패키지 구조

```text
com.startup
 ├─ common/         공통 (config, dto, error, auth, entity, util)
 ├─ domain/         도메인 비즈니스 로직
 │   ├─ example/    패키지 템플릿 (controller/dto/entity/enums/error/repository/service/support)
 │   ├─ scenario/   시나리오 CRUD / 커스텀 시나리오
 │   ├─ play/       플레이 세션 / 증거 해금 / 힌트
 │   └─ ai/         AI 심문 / 채점 / 검증 / 프롬프트 (controller/service/dto/prompt/client/error/support)
 └─ infrastructure/ 외부 시스템 연동 (persistence, redis)
```

새 도메인 패키지는 `domain/example/` 구조를 따른다.
AI 호출 관련 코드는 `domain/ai`에 집중한다 — 다른 도메인 Service에서 프롬프트를 직접 만들지 않는다.
패키지/계층/예외/트랜잭션 상세 규칙은 `docs/BACKEND_IMPLEMENTATION_GUIDE.md`를 따른다.

---

## 빌드 / 테스트

```bash
bash scripts/compose-up.sh     # Docker 빌드 + 실행
./gradlew test                 # 테스트
./gradlew bootJar              # jar 생성
```

실행, Android 연결, Docker Compose, 배포 명령 요약은 `docs/RUN_AND_DEPLOY.md`를 따른다.
상세 운영 명령어, Blue-Green rollback, 백업/복구, 장애 대응은 `docs/infra/OPS_RUNBOOK.md`를 따른다.
인프라 선택 이유, 운영 구조, 확장 계획, PoC/ADR 후보는 `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md`를 따른다.

---

## 팀 역할

| 담당자 | 역할 | 핵심 범위 |
|--------|------|----------|
| 황도윤 | 리더 / 인프라 / 공식 시나리오 / PR 리뷰 | 공통 세팅, Seed Data, 문서 최신화 |
| 배강혁 | AI 엔진 / 프롬프트 / AI 백엔드 | interrogation, final-deduction, validate |
| 소수경 | 핵심 백엔드 CRUD / 게임 세션 / 증거 해금 | scenarios, play-sessions, evidences, hints |
| 정채림 | Android UI / 화면 흐름 / API 연동 / QA | Android 화면, Mock → API 전환 |

---

## 리뷰 시 중점 확인 사항

### 정답 누설 방지 (최우선)

코드 리뷰 시 아래 패턴이 보이면 반드시 지적한다:

```text
AI 프롬프트에 "너는 범인이다" 또는 이에 준하는 정보가 포함된 경우
Solution 엔티티의 데이터가 AI 프롬프트 빌드 과정에 유입되는 경우
해금되지 않은 SuspectSecret이 AI에게 전달되는 경우
전체 정답(culprit, motive, method, coverUp)이 한 곳에 조립되어 AI에게 넘어가는 경우
suspect_response_policies에서 conditionKey를 검증하지 않고 전체 policy를 AI에게 넘기는 경우
```

### 프롬프트 구조 검증

```text
AI에게 전달되는 정보가 다음으로 한정되는지 확인:
  - 용의자 공개 프로필, 공개 진술, 공개 알리바이
  - 현재 해금 증거, 사용자 제시 증거
  - ResponsePolicyResolver가 결정한 답변 정책 (policyText, allowedFacts, tone)
  - 사용자 질문

답변 정책이 ResponsePolicyResolver를 통해 결정되는지 확인 (AI가 정책을 스스로 판단하면 안 된다).
프롬프트에 "답변은 2문장 이내" 제약이 포함되는지 확인.
프롬프트 인젝션 방어 문구가 System Prompt에 포함되는지 확인.
프롬프트 문자열이 AiPromptBuilder에 집중되어 있는지 확인 (여러 Service에 분산되면 안 된다).
프롬프트 템플릿이 resources/prompts/ 파일 기반인지 확인 (하드코딩 금지).
상세 기준: `docs/AI_NPC_PROMPT_POLICY.md`
```

### 트랜잭션과 AI 호출 분리

```text
AI API 호출이 @Transactional 메서드 안에서 실행되지 않는지 확인.
올바른 흐름: DB 조회 → 트랜잭션 종료 → AI 호출 → 새 트랜잭션으로 결과 저장.
```

### MockUser / 접근 권한

```text
userId가 직접 하드코딩(1L)되지 않고 MockUserProvider를 통해 조회하는지 확인.
시나리오 접근 시 ScenarioAccessService를 경유하는지 확인.
```

### 일반 코드 품질

```text
domain/example/ 패키지 구조를 따르는지 확인.
공통 ApiResponse 래퍼를 사용하는지 확인.
BusinessException(ErrorCode) 체계를 통해 에러를 처리하는지 확인 (RuntimeException 문자열 금지).
Mock/Fallback 응답이 준비되어 있는지 확인 (AI API 실패 시 시연 보장).
다른 담당자 패키지를 불필요하게 수정하지 않았는지 확인.
Entity를 Controller에서 직접 반환하지 않는지 확인 (DTO 분리).
AI 호출 로그(model, tokens, latency, status)를 저장하는지 확인.
```

---

## 담당 API (배강혁)

```text
POST /api/play-sessions/{sessionId}/interrogations      AI 용의자 심문
POST /api/play-sessions/{sessionId}/final-deduction      최종 추리 제출/채점
POST /api/ai/scenarios/{scenarioId}/validate             시나리오 논리 검증
```

---

## 금지 사항

```text
AI NPC 프롬프트에 범인 정보를 넣지 않는다.
AI가 설정에 없는 사실을 만드는 구조를 허용하지 않는다.
심문 로그를 저장하지 않는 구조를 만들지 않는다.
최종 추리 채점을 단순 텍스트 비교로 구현하지 않는다.
시나리오와 플레이 세션 상태를 혼동하지 않는다.
트랜잭션 안에서 AI API를 호출하지 않는다.
프롬프트 문자열을 여러 Service에 분산 작성하지 않는다.
```

---

## 참조 문서

```text
CLAUDE.md                                   프로젝트 전체 컨텍스트
docs/CaseLab_AI_PRD.md                      제품 요구사항
docs/CaseLab_AI_API_Spec.md                 API 명세 (Request/Response 포함)
docs/ANDROID_SCREEN_API_MAPPING.md          Android 화면-API 매핑
docs/AI_NPC_PROMPT_POLICY.md                AI 심문 프롬프트 정책 정본
docs/OFFICIAL_SCENARIO_DEMO_DAY.md          공식 데모 시나리오 정본 (채점 기준 포함)
docs/BACKEND_IMPLEMENTATION_GUIDE.md        백엔드 구현 규칙 정본
docs/CaseLab_AI_ERD_Design.md               ERD 설계 / 비판적 리뷰
docs/RUN_AND_DEPLOY.md                      실행 / Android 연결 / Docker Compose / 배포 명령 요약
docs/infra/OPS_RUNBOOK.md                   운영 명령어 / Blue-Green rollback / 백업 / 장애 대응
docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md    인프라 선택 이유 / 운영 구조 / 확장 계획
docs/scenarios/README.md                    시나리오 문서 공개 범위 / 내부 스포일러 문서 관리 기준
```

리뷰 요청 시 **리뷰 지시서**가 함께 전달된다. 지시서에는 작업 목표, 변경 범위, 핵심 결정, 리뷰 초점, 참조 문서가 포함된다.
리뷰 우선순위: 지시서의 "리뷰 초점" → AGENTS.md의 체크포인트 → 일반 코드 품질 순서.
지시서에서 참조 문서가 지정되면 해당 문서의 해당 섹션을 기준으로 검증한다.
예: "AI_NPC_PROMPT_POLICY.md 섹션 8~9 기준으로 프롬프트 구조를 검증해줘"

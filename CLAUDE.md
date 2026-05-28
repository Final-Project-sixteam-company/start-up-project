# CaseLab AI

AI 용의자를 심문하고, 증거와 알리바이를 조합해 범인·동기·범행 방법을 추리하는 Android 기반 AI 추리게임 플랫폼.

```text
Android App(Kotlin)
Spring Boot Backend(Java 21, Spring Boot 4)
MySQL + Redis
Spring AI 기반 LLM 연동
```

---

## 0. 이름 / 도메인 기준

```text
공개 앱 이름: ClueRoom
서비스 도메인: clueroom.xyz
운영 API 도메인: https://api.clueroom.xyz
```

`CaseLab AI`, `CaseLab_AI`, `start-up`, `startup`, `com.startup`은 초기 설계/코드에서 사용한 내부 레거시 식별자다.
문서 파일명, Java package, Gradle rootProject, Docker container/volume, DB 이름은 별도 마이그레이션 작업 전까지 유지한다.

AI는 사용자가 "프로젝트명/앱명"을 물으면 **ClueRoom**으로 답하고, 코드 구조나 파일 경로를 다룰 때는 현재 저장소의 실제 식별자를 그대로 사용한다.

---

## 1. 가장 중요한 원칙

```text
AI 용의자에게 범인 정보를 직접 전달하지 않는다.
범인, 정답, 핵심 비밀은 백엔드 secret으로 관리한다.
AI에게는 현재 공개 정보와 답변 정책만 전달한다.
답변 정책은 ResponsePolicyResolver가 결정한다.
NPC 답변은 1~2줄, 최대 2문장으로 제한한다.
설정에 없는 사실을 생성하지 않는다.
트랜잭션 안에서 AI API를 호출하지 않는다.
```

AI 프롬프트 상세 기준은 `docs/AI_NPC_PROMPT_POLICY.md`를 따른다.

---

## 2. 현재 MVP 범위

1차 MVP는 아래 흐름을 먼저 완성한다.

```text
공식 시나리오 조회
게임 세션 시작
탐정 대시보드
현장 / 증거 / 용의자 / 힌트 조회
AI 용의자 심문
최종 추리 제출
결과 해설 조회
커스텀 시나리오 제작 기본형
AI 시나리오 검증
리뷰 / 북마크 기본형
```

아래 기능은 후순위다.

```text
JWT 인증 완성
거래 / 크레딧 / 구매 / 언락
제작자 정산
AI 시나리오 초안 생성 고도화
내 기록 / 마이페이지 완성
```

제품 범위와 우선순위 정본은 `docs/CaseLab_AI_PRD.md`를 따른다.

---

## 3. 패키지 구조 요약

```text
com.startup
 ├─ common/         공통 설정, DTO, 에러, 인증, 유틸
 ├─ domain/         도메인 비즈니스 로직
 │   ├─ example/    패키지 템플릿
 │   ├─ scenario/   시나리오 CRUD / 커스텀 시나리오
 │   ├─ play/       플레이 세션 / 증거 해금 / 힌트
 │   └─ ai/         AI 심문 / 채점 / 검증 / 프롬프트
 └─ infrastructure/ 외부 시스템 연동
```

새 도메인 패키지는 `domain/example/`의 구조를 따른다.
상세 구현 규칙은 `docs/BACKEND_IMPLEMENTATION_GUIDE.md`를 따른다.

---

## 4. 백엔드 구현 규칙 요약

```text
Controller는 HTTP 요청/응답 경계만 담당한다.
Service는 비즈니스 로직과 트랜잭션 경계를 담당한다.
Repository는 저장소 접근만 담당한다.
Entity를 API 응답으로 직접 반환하지 않는다.
모든 API는 ApiResponse 래퍼를 사용한다.
예외는 BusinessException + ErrorCode 체계를 사용한다.
userId를 1L로 하드코딩하지 않고 MockUserProvider를 사용한다.
시나리오 접근 판단은 ScenarioAccessService를 통한다.
DTO ID 필드명은 API Spec을 따른다.
```

정본:

```text
docs/BACKEND_IMPLEMENTATION_GUIDE.md
docs/CaseLab_AI_API_Spec.md
```

---

## 5. AI 구현 규칙 요약

AI에게 전달 가능한 정보:

```text
용의자 공개 프로필
공개 진술 / 알리바이
현재 해금된 증거
사용자 질문
사용자가 제시한 증거
ResponsePolicyResolver가 결정한 답변 정책
답변 길이 제한
```

AI에게 전달 금지:

```text
진범 여부
Solution 전체
범행 동기 / 방법 / 은폐 전체
아직 해금되지 않은 핵심 증거
아직 공개되지 않은 용의자 비밀
게임 종료 전 최종 해설
```

AI 호출 흐름:

```text
DB 조회
→ 트랜잭션 종료
→ ResponsePolicyResolver 정책 결정
→ PromptTemplateService 프롬프트 생성
→ AiClient 호출
→ 새 트랜잭션으로 InterrogationLog 저장
→ 응답 반환
```

프롬프트, Fallback, 모델 파라미터 상세는 `docs/AI_NPC_PROMPT_POLICY.md`를 따른다.

---

## 6. 담당 API

### AI 담당

```text
POST /api/play-sessions/{sessionId}/interrogations
POST /api/play-sessions/{sessionId}/final-deduction
POST /api/ai/scenarios/{scenarioId}/validate
```

### 핵심 플레이 담당

```text
GET  /api/scenarios
GET  /api/scenarios/{scenarioId}
POST /api/play-sessions
GET  /api/play-sessions/{sessionId}/dashboard
GET  /api/play-sessions/{sessionId}/evidences
GET  /api/play-sessions/{sessionId}/suspects
GET  /api/play-sessions/{sessionId}/hints
```

API 명세 전체는 `docs/CaseLab_AI_API_Spec.md`를 따른다.

---

## 7. 실행

```bash
bash scripts/compose-up.sh
bash scripts/compose-down.sh
./gradlew test
./gradlew bootJar
```

로컬 실행, Android 연결, Docker Compose, 배포 명령 요약은 `docs/RUN_AND_DEPLOY.md`를 따른다.
상세 운영 명령어, Blue-Green rollback, 백업/복구, 장애 대응은 `docs/infra/OPS_RUNBOOK.md`를 따른다.
인프라 선택 이유, 운영 구조, 확장 계획, PoC/ADR 후보는 `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md`를 따른다.

---

## 8. 팀 역할

| 담당자 | 역할 | 핵심 범위 |
|---|---|---|
| 황도윤 | 리더 / 인프라 / 공식 시나리오 / PR 리뷰 | 공통 세팅, Seed Data, 문서 최신화 |
| 배강혁 | AI 엔진 / 프롬프트 / AI 백엔드 | interrogation, final-deduction, validate |
| 소수경 | 핵심 백엔드 CRUD / 게임 세션 / 증거 해금 | scenarios, play-sessions, evidences, hints |
| 정채림 | Android UI / 화면 흐름 / API 연동 / QA | Android 화면, Mock -> API 전환 |

다른 담당자의 패키지를 수정해야 할 때는 사유를 밝히고 최소 범위로 한정한다.

---

## 9. 참조 문서

| 문서 | 역할 |
|---|---|
| `docs/CaseLab_AI_PRD.md` | 제품 요구사항, MVP 범위, 구현 우선순위 |
| `docs/CaseLab_AI_API_Spec.md` | API 경로, Request/Response, DTO 필드명 정본 |
| `docs/ANDROID_SCREEN_API_MAPPING.md` | Android 화면별 호출 API 매핑 |
| `docs/CaseLab_AI_ERD_Design.md` | ERD 설계, 엔티티, 관계, Enum |
| `docs/CaseLab_AI_ERDCloud.sql` | ERDCloud import SQL |
| `docs/AI_NPC_PROMPT_POLICY.md` | AI NPC 프롬프트 정책 정본 |
| `docs/BACKEND_IMPLEMENTATION_GUIDE.md` | 백엔드 구현 규칙 정본 |
| `docs/RUN_AND_DEPLOY.md` | 로컬 실행, Android 연결, Docker Compose, 배포 명령 요약 |
| `docs/infra/OPS_RUNBOOK.md` | 운영 명령어, Blue-Green rollback, 백업/복구, 장애 대응 |
| `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 인프라 선택 이유, 운영 구조, 확장 계획, PoC/ADR 후보 |
| `docs/scenarios/README.md` | 시나리오 문서 공개 범위, 내부 스포일러 문서 관리 기준 |
| `docs/OFFICIAL_SCENARIO_DEMO_DAY.md` | 공식 데모 시나리오 seed 정본 |

권장 읽기 순서:

```text
CLAUDE.md
→ CaseLab_AI_PRD.md
→ CaseLab_AI_API_Spec.md
→ ANDROID_SCREEN_API_MAPPING.md
→ CaseLab_AI_ERD_Design.md
→ AI_NPC_PROMPT_POLICY.md
→ BACKEND_IMPLEMENTATION_GUIDE.md
→ RUN_AND_DEPLOY.md
→ OPS_RUNBOOK.md
→ CLUEROOM_INFRASTRUCTURE_STRATEGY.md
→ docs/scenarios/README.md
```

# CaseLab AI — Codex Review Guide

AI 용의자 심문형 추리게임 플랫폼. Android(Kotlin) + Spring Boot(Java 21, Spring Boot 4) 백엔드.
프로젝트 상세 컨텍스트는 `CLAUDE.md`를 함께 참조한다.

---

## 프로젝트 핵심 제약

```text
AI 용의자에게 범인 정보를 직접 전달하지 않는다.
범인·정답·핵심 비밀은 백엔드 secret으로 관리한다.
AI에게는 현재 공개 정보 + 답변 정책만 전달한다.
NPC 답변은 1~2줄로 제한한다.
설정에 없는 사실을 생성하지 않는다.
```

---

## 패키지 구조

```text
com.startup
 ├─ common/         공통 (config, dto, error, entity, util)
 ├─ domain/         도메인 비즈니스 로직
 │   └─ example/    패키지 템플릿 (controller/dto/entity/enums/error/repository/service/support)
 └─ infrastructure/ 외부 시스템 연동 (persistence, redis)
```

새 도메인 패키지는 `domain/example/` 구조를 따른다.

---

## 빌드 / 테스트

```bash
bash scripts/compose-up.sh     # Docker 빌드 + 실행
./gradlew test                 # 테스트
./gradlew bootJar              # jar 생성
```

---

## 팀 역할

| 담당자 | 역할 | 핵심 범위 |
|--------|------|----------|
| 황도윤 | 리더 / 인프라 / 공식 시나리오 / PR 리뷰 | 공통 세팅, Seed Data, 문서 최신화 |
| 배강혁 | AI 엔진 / 프롬프트 / AI 백엔드 | interrogation, final-deduction, validate |
| 소수경 | 핵심 백엔드 CRUD / 게임 세션 / 증거 해금 | scenarios, game-sessions, evidences, hints |
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
```

### 프롬프트 구조 검증

```text
AI에게 전달되는 정보가 다음으로 한정되는지 확인:
  - 용의자 공개 프로필, 공개 알리바이
  - 현재 공개 증거, 사용자 제시 증거
  - 현재 답변 정책 (ResponsePolicy)
  - 사용자 질문

답변 정책이 NpcResponsePolicyService를 통해 결정되는지 확인.
프롬프트에 "답변은 2문장 이내" 제약이 포함되는지 확인.
```

### 일반 코드 품질

```text
domain/example/ 패키지 구조를 따르는지 확인.
공통 ApiResponse 래퍼를 사용하는지 확인.
BusinessException 체계를 통해 에러를 처리하는지 확인.
Mock/Fallback 응답이 준비되어 있는지 확인 (AI API 실패 시 시연 보장).
다른 담당자 패키지를 불필요하게 수정하지 않았는지 확인.
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
```

---

## 참조 문서

```text
CLAUDE.md                               프로젝트 전체 컨텍스트
docs/CaseLab_AI_PRD.md                  제품 요구사항
docs/CaseLab_AI_API_Spec.md             API 명세 (Request/Response 포함)
docs/CaseLab_AI_Project_Planning.md     기획 / 도메인 모델 / AI 프롬프트 정책
```

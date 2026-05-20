# CaseLab AI

AI 용의자를 심문하고, 증거와 알리바이를 조합해 범인·동기·범행 방법을 추리하는 Android 기반 AI 추리게임 플랫폼입니다.

```text
Client: Android Kotlin
Backend: Java 21, Spring Boot 4
Database: MySQL
Cache/Lock: Redis
AI: Spring AI 기반 LLM 연동
```

---

## 이름 / 도메인 기준

```text
공개 앱 이름: ClueRoom
서비스 도메인: clueroom.xyz
운영 API 도메인: https://api.clueroom.xyz
```

기존 문서명, 코드 패키지, Gradle 프로젝트명, Docker/DB 이름에 남아 있는 `CaseLab AI`, `CaseLab_AI`, `start-up`, `startup`, `com.startup`은 내부 레거시 식별자로 유지한다.
이름 변경 자체가 목적이 아닌 작업에서는 위 내부 식별자를 임의로 바꾸지 않는다.

---

## 문서

처음 보는 팀원이나 개발용 AI는 아래 순서로 읽으면 됩니다.

| 순서 | 문서 | 역할 |
|---:|---|---|
| 1 | [CLAUDE.md](CLAUDE.md) | 프로젝트 핵심 컨텍스트와 작업 규칙 요약 |
| 2 | [AGENTS.md](AGENTS.md) | Codex 리뷰 기준 |
| 3 | [docs/CaseLab_AI_PRD.md](docs/CaseLab_AI_PRD.md) | 제품 요구사항, MVP 범위, 구현 우선순위 |
| 4 | [docs/CaseLab_AI_API_Spec.md](docs/CaseLab_AI_API_Spec.md) | API 경로, Request/Response, DTO 필드명 정본 |
| 5 | [docs/ANDROID_SCREEN_API_MAPPING.md](docs/ANDROID_SCREEN_API_MAPPING.md) | Android 화면별 호출 API 매핑 |
| 6 | [docs/CaseLab_AI_ERD_Design.md](docs/CaseLab_AI_ERD_Design.md) | ERD 설계, 엔티티, 관계, Enum |
| 7 | [docs/CaseLab_AI_ERDCloud.sql](docs/CaseLab_AI_ERDCloud.sql) | ERDCloud import SQL |
| 8 | [docs/AI_NPC_PROMPT_POLICY.md](docs/AI_NPC_PROMPT_POLICY.md) | AI NPC 프롬프트 정책과 정답 누설 방지 |
| 9 | [docs/BACKEND_IMPLEMENTATION_GUIDE.md](docs/BACKEND_IMPLEMENTATION_GUIDE.md) | 백엔드 구현 규칙, MockUser, 접근 권한, 인증/거래 확장 |
| 10 | [docs/RUN_AND_DEPLOY.md](docs/RUN_AND_DEPLOY.md) | 로컬 실행, Android 연결, Docker, 배포 |
| 11 | [docs/OFFICIAL_SCENARIO_DEMO_DAY.md](docs/OFFICIAL_SCENARIO_DEMO_DAY.md) | 공식 시나리오 seed 기준 |

### 문서 수정 기준

같은 사실을 여러 문서에 복사하지 않습니다. 내용이 바뀌면 아래 정본 문서만 수정하고, 다른 문서에서는 링크로 참조합니다.

| 수정할 내용 | 정본 문서 |
|---|---|
| MVP 범위, Phase, 기능 우선순위 | [docs/CaseLab_AI_PRD.md](docs/CaseLab_AI_PRD.md) |
| API 경로, Request/Response, DTO 필드명 | [docs/CaseLab_AI_API_Spec.md](docs/CaseLab_AI_API_Spec.md) |
| Android 화면별 호출 API | [docs/ANDROID_SCREEN_API_MAPPING.md](docs/ANDROID_SCREEN_API_MAPPING.md) |
| 엔티티, 관계, Enum, DB 제약 | [docs/CaseLab_AI_ERD_Design.md](docs/CaseLab_AI_ERD_Design.md) |
| ERDCloud import SQL | [docs/CaseLab_AI_ERDCloud.sql](docs/CaseLab_AI_ERDCloud.sql) |
| AI 프롬프트, 답변 정책, 정답 누설 방지 | [docs/AI_NPC_PROMPT_POLICY.md](docs/AI_NPC_PROMPT_POLICY.md) |
| 백엔드 구현 규칙, MockUser, 접근 권한, 인증/거래 확장 | [docs/BACKEND_IMPLEMENTATION_GUIDE.md](docs/BACKEND_IMPLEMENTATION_GUIDE.md) |
| 로컬 실행, Android 연결, Docker, 배포 | [docs/RUN_AND_DEPLOY.md](docs/RUN_AND_DEPLOY.md) |
| 공식 시나리오 seed data | [docs/OFFICIAL_SCENARIO_DEMO_DAY.md](docs/OFFICIAL_SCENARIO_DEMO_DAY.md) |

현재 `docs` 기준 문서는 위 9개입니다.

## 빠른 실행

```bash
bash scripts/compose-up.sh
```

종료:

```bash
bash scripts/compose-down.sh
```

Gradle task:

```bash
./gradlew composeUp
./gradlew composeDown
./gradlew test
```

실행과 배포 상세는 [docs/RUN_AND_DEPLOY.md](docs/RUN_AND_DEPLOY.md)를 따릅니다.

---

## 주요 URL

| 대상 | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| Actuator Health | `http://localhost:8080/actuator/health` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

Android Emulator에서 로컬 백엔드를 호출할 때는 아래 주소를 사용합니다.

```text
http://10.0.2.2:8080
```

운영 API Base URL:

```text
https://api.clueroom.xyz
```

Base URL에는 `/api`를 넣지 않고, 실제 API path에만 `/api/...` prefix를 포함합니다.

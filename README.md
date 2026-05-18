# CaseLab AI

Spring Boot 기반 CaseLab AI 백엔드 프로젝트입니다.

CaseLab AI는 AI 용의자 심문형 추리게임에 커스텀 시나리오 마켓플레이스와 Mock 크레딧 기반 구매/언락 거래 흐름을 붙이는 Android 기반 서비스입니다.

## 문서

처음 보는 팀원이나 개발용 AI는 아래 순서로 읽으면 됩니다.

1. 프로젝트 전제와 문서 읽는 순서: [docs/AI_CONTEXT_GUIDE.md](docs/AI_CONTEXT_GUIDE.md)
2. 제품 요구사항: [docs/CaseLab_AI_PRD.md](docs/CaseLab_AI_PRD.md)
3. 개발 핸드오버: [docs/CaseLab_AI_Project_Planning.md](docs/CaseLab_AI_Project_Planning.md)
4. API 명세: [docs/CaseLab_AI_API_Spec.md](docs/CaseLab_AI_API_Spec.md)
5. ERD 설계/리뷰: [docs/CaseLab_AI_ERD_Design.md](docs/CaseLab_AI_ERD_Design.md)
6. ERDCloud import SQL: [docs/CaseLab_AI_ERDCloud.sql](docs/CaseLab_AI_ERDCloud.sql)
7. 백엔드 구현 규칙: [docs/BACKEND_IMPLEMENTATION_RULES.md](docs/BACKEND_IMPLEMENTATION_RULES.md)
8. 인증/거래 확장 계획: [docs/AUTH_TRANSACTION_EXPANSION_PLAN.md](docs/AUTH_TRANSACTION_EXPANSION_PLAN.md)
9. AI NPC 프롬프트 정책: [docs/AI_NPC_PROMPT_POLICY.md](docs/AI_NPC_PROMPT_POLICY.md)
10. Android 화면-API 매핑: [docs/ANDROID_SCREEN_API_MAPPING.md](docs/ANDROID_SCREEN_API_MAPPING.md)
11. 공식 시나리오 Seed 기준: [docs/OFFICIAL_SCENARIO_DEMO_DAY.md](docs/OFFICIAL_SCENARIO_DEMO_DAY.md)
12. 인프라/배포 계획: [docs/CaseLab_AI_Infrastructure_Deployment_Plan.md](docs/CaseLab_AI_Infrastructure_Deployment_Plan.md)

## 실행

빠른 실행:

```bash
bash scripts/compose-up.sh
```

종료:

```bash
bash scripts/compose-down.sh
```

## 실행 관련 문서

- Docker/MySQL/Redis 실행, Spring AI 환경변수, IntelliJ Run Configuration, 에러 해결: [docs/docker-run.md](docs/docker-run.md)
- Android 앱에서 백엔드 붙이는 방법: [docs/android-client.md](docs/android-client.md)

## 주요 URL

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Actuator Health: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Android Emulator에서 백엔드 API를 호출할 때는 `localhost` 대신 아래 주소를 사용합니다.

```text
http://10.0.2.2:8080
```


# start-up

<!-- 팀원이 처음 클론했을 때 실행 문서 위치와 주요 URL만 빠르게 찾도록 README는 짧게 유지한다. -->

Spring Boot 기본 세팅 프로젝트입니다.

## 실행 문서

- Docker/MySQL/Redis 실행, Spring AI 환경변수, IntelliJ Run Configuration, 에러 해결: [docs/docker-run.md](docs/docker-run.md)
- Android 앱에서 백엔드 붙이는 방법: [docs/android-client.md](docs/android-client.md)

빠른 실행:

```bash
bash scripts/compose-up.sh
```

실행 후 주요 URL:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

종료:

```bash
bash scripts/compose-down.sh
```



테스트

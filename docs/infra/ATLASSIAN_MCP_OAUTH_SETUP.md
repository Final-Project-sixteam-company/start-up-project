# Atlassian MCP OAuth Secret Setup

> 목적: Jira/Confluence MCP 연동에 필요한 Atlassian OAuth credential을 서버 secret으로 입력하는 절차다. 실제 값은 이 문서, Git, PR 본문, 채팅에 기록하지 않는다.

## 1. 입력 위치

로컬 개발:

```text
.env
```

운영 서버:

```text
/opt/clueroom/secrets/env.d/oauth.env
```

`scripts/deploy-bluegreen.sh`와 `/opt/clueroom/bg-compose`는 `oauth.env`가 존재하면 runtime env에 합친다.

주의: `.env` 또는 `oauth.env`에 값을 넣는 것만으로 Claude Desktop, Codex, `npx`로 실행한 MCP 프로세스에 자동 주입되지는 않는다. MCP gateway/client를 별도 프로세스로 실행한다면 해당 프로세스의 실행 설정에서 이 env 파일을 명시적으로 로드하거나 동일한 변수를 process env로 전달한다.

## 2. 필수 환경 변수

```env
ATLASSIAN_CLIENT_ID=...
ATLASSIAN_CLIENT_SECRET=...
ATLASSIAN_REDIRECT_URI=...
ATLASSIAN_MODULES=jira,confluence
```

입력 기준:

```text
ATLASSIAN_CLIENT_ID      Atlassian Developer Console Settings의 Client ID
ATLASSIAN_CLIENT_SECRET  Atlassian Developer Console Settings의 Secret
ATLASSIAN_REDIRECT_URI   Developer Console Authorization callback URL과 완전히 동일한 값
ATLASSIAN_MODULES        사용할 MCP 모듈 목록. 현재 목표는 jira,confluence
```

`ATLASSIAN_REDIRECT_URI`는 Spring 백엔드 API 주소가 아니라 OAuth 응답을 실제로 처리하는 MCP gateway/client의 callback 주소여야 한다. 현재 백엔드에는 `/callback` OAuth handler가 없으므로, 예를 들어 `http://localhost:8080/callback`을 쓰려면 그 포트에서 MCP gateway/client가 callback을 받고 있어야 한다. Spring API도 같은 `8080` 포트를 쓰는 경우 포트 충돌을 피한다.

## 3. Developer Console 권한 확인

Atlassian Developer Console에서 앱 생성과 권한 추가를 먼저 완료한다.

Jira API:

```text
read:jira-work
read:jira-user
write:jira-work
```

Confluence API:

```text
read:page:confluence
read:space:confluence
write:page:confluence
```

User Identity API:

```text
read:me
```

Refresh token:

```text
offline_access
```

Atlassian 문서는 가능한 경우 classic scope 사용을 권장한다. 위 Jira scope들은 classic scope이며, 위 Confluence page/space scope들은 granular scope다. MCP 서버 문서가 더 좁은 scope set을 요구하면 실제 사용 API 기준으로 Developer Console 권한과 인증 요청 scope를 함께 맞춘다.

공식 문서:

```text
https://developer.atlassian.com/cloud/jira/platform/oauth-2-3lo-apps/
https://developer.atlassian.com/cloud/jira/platform/scopes-for-oauth-2-3LO-and-forge-apps/
https://developer.atlassian.com/cloud/confluence/scopes-for-oauth-2-3LO-and-forge-apps/
https://developer.atlassian.com/cloud/oauth/getting-started/faq/
```

## 4. 서버 입력 절차

```bash
sudo nano /opt/clueroom/secrets/env.d/oauth.env
sudo chmod 600 /opt/clueroom/secrets/env.d/oauth.env
```

값 존재 여부만 확인한다.

```bash
awk -F= '/^ATLASSIAN_/ { if ($2 == "") print $1"=empty"; else print $1"=set" }' /opt/clueroom/secrets/env.d/oauth.env
```

금지:

```bash
cat /opt/clueroom/secrets/env.d/oauth.env
printenv ATLASSIAN_CLIENT_SECRET
```

## 5. 배포 전 확인

Compose 설정이 env 파일을 읽을 수 있는지만 검증한다. 출력에는 secret이 포함될 수 있으므로 화면에 출력하지 않는다.

```bash
cd /opt/clueroom/app
/opt/clueroom/bg-compose config > /dev/null
```

최초 인증은 MCP gateway/client를 재시작한 뒤 브라우저에서 Atlassian 동의 화면을 완료한다. refresh token이나 token cache가 생성되는 경우 해당 저장 위치도 서버 secret 영역으로 보고 파일 권한을 `600` 수준으로 제한한다.

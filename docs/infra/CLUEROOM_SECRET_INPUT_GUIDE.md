# ClueRoom Secret Input Guide

> 운영 secret 입력 담당자를 위한 가이드다. 이 문서는 입력 위치와 절차만 기록하며 실제 secret 값은 저장하지 않는다.

## 1. PEM 공유 금지

서버 접속용 PEM/private key는 팀 채팅, Git, Notion, 메일로 공유하지 않는다.
운영 서버 접근은 팀원별 SSH public key 등록 방식으로 관리한다.

## 2. ubuntu 계정 공유 금지

공용 `ubuntu` 계정 비밀번호나 private key를 돌려 쓰지 않는다.
서버에는 팀원별 public key를 등록하고, 필요 시 담당 범위별 제한 계정을 둔다.

## 3. 팀원별 SSH public key만 전달

팀원은 로컬에서 public key만 생성해 전달한다.

```bash
ssh-keygen -t ed25519 -C "name@clueroom"
cat ~/.ssh/id_ed25519.pub
```

전달 금지:

```text
~/.ssh/id_ed25519
*.pem
*.key
```

## 4. 제한 계정

secret 입력만 필요한 담당자는 범위별 제한 계정을 사용한다.

```text
ai-secret       -> /opt/clueroom/secrets/env.d/ai.env
portone-secret  -> /opt/clueroom/secrets/env.d/portone.env
oauth-secret    -> /opt/clueroom/secrets/env.d/oauth.env
```

## 5. ACL로 담당 env 파일만 수정 가능

예시:

```bash
sudo mkdir -p /opt/clueroom/secrets/env.d
sudo touch /opt/clueroom/secrets/env.d/ai.env
sudo touch /opt/clueroom/secrets/env.d/portone.env
sudo touch /opt/clueroom/secrets/env.d/oauth.env

sudo chown root:root /opt/clueroom/secrets/env.d/*.env
sudo chmod 600 /opt/clueroom/secrets/env.d/*.env

sudo setfacl -m u:ai-secret:rw /opt/clueroom/secrets/env.d/ai.env
sudo setfacl -m u:portone-secret:rw /opt/clueroom/secrets/env.d/portone.env
sudo setfacl -m u:oauth-secret:rw /opt/clueroom/secrets/env.d/oauth.env
```

ACL 확인:

```bash
getfacl /opt/clueroom/secrets/env.d/ai.env
```

## 6. env 파일 입력 예시

파일 위치:

```text
/opt/clueroom/secrets/env.d/ai.env
/opt/clueroom/secrets/env.d/portone.env
/opt/clueroom/secrets/env.d/oauth.env
```

편집:

```bash
nano /opt/clueroom/secrets/env.d/ai.env
```

저장 후 권한:

```bash
chmod 600 /opt/clueroom/secrets/env.d/*.env
```

## 7. DeepSeek 설정 예시

```env
SPRING_AI_MODEL_CHAT=openai
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_API_KEY=...
OPENAI_CHAT_MODEL=deepseek-v4-flash
OPENAI_CHAT_TEMPERATURE=0.4
```

`OPENAI_API_KEY`에는 실제 값을 서버에서만 입력한다.

## 8. PortOne 설정 예시

```env
PORTONE_API_SECRET=...
PORTONE_STORE_ID=...
PORTONE_CHANNEL_KEY=...
```

실제 키 이름은 백엔드 설정과 결제 연동 코드에서 사용하는 환경변수명과 맞춘다.

## 9. OAuth/JWT 설정 예시

```env
JWT_SECRET=...
KAKAO_CLIENT_ID=...
KAKAO_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

JWT secret은 충분히 긴 난수로 생성하고 레포에 기록하지 않는다.

## 10. Atlassian MCP OAuth 설정 예시

Jira/Confluence MCP 연동용 Atlassian OAuth credential은 `/opt/clueroom/secrets/env.d/oauth.env`에 입력한다.

```env
ATLASSIAN_CLIENT_ID=...
ATLASSIAN_CLIENT_SECRET=...
ATLASSIAN_REDIRECT_URI=...
ATLASSIAN_MODULES=jira,confluence
```

`ATLASSIAN_REDIRECT_URI`는 Atlassian Developer Console의 Authorization callback URL과 MCP gateway/client가 실제로 처리하는 callback URL이 완전히 같아야 한다. 현재 Spring 백엔드는 `/callback` OAuth handler를 제공하지 않으므로, `http://localhost:8080/callback` 같은 값을 사용할 때는 해당 포트의 MCP gateway/client가 callback을 받고 있는지 확인한다.

필수 scope와 최초 인증 절차는 `docs/infra/ATLASSIAN_MCP_OAUTH_SETUP.md`를 따른다.

## 11. 접속 실패 시 점검

```bash
whoami
id
pwd
ls -ld /opt/clueroom/secrets /opt/clueroom/secrets/env.d
ls -l /opt/clueroom/secrets/env.d
```

확인할 것:

```text
SSH public key가 서버 계정에 등록되었는지
접속 계정이 올바른지
해당 계정에 env 파일 ACL이 있는지
```

## 12. 저장 실패 시 ACL 점검

```bash
getfacl /opt/clueroom/secrets/env.d/ai.env
getfacl /opt/clueroom/secrets/env.d/portone.env
getfacl /opt/clueroom/secrets/env.d/oauth.env
```

필요 시 운영 담당자가 ACL을 다시 부여한다.

```bash
sudo setfacl -m u:ai-secret:rw /opt/clueroom/secrets/env.d/ai.env
```

## 13. secret 값 확인은 set/empty 방식만 사용

값 전체를 출력하지 않는다. 설정 여부만 확인한다.

```bash
awk -F= '{ if ($2 == "") print $1"=empty"; else print $1"=set" }' /opt/clueroom/secrets/env.d/ai.env
```

금지:

```bash
cat /opt/clueroom/secrets/env.d/ai.env
printenv OPENAI_API_KEY
```

## 14. Firebase service account

Firebase service account JSON은 아래 같은 서버 secret 경로에만 둔다.

```text
/opt/clueroom/secrets/firebase-service-account.json
```

Compose는 `/opt/clueroom/secrets`를 컨테이너에 read-only로 마운트한다. JSON 내용은 Git, 문서, 채팅에 붙여넣지 않는다.

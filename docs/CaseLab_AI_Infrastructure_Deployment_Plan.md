# CaseLab AI 인프라 운영 계획서

> 목적: CaseLab AI Android 앱의 백엔드/API 서버를 **저비용, 직접 운영 경험 중심**으로 배포하기 위한 인프라 계획서입니다.  
> 방향성은 “과제용 인증샷 인프라”가 아니라, 실제 초기 스타트업처럼 비용을 아끼면서 Linux, Nginx, Docker, DB, HTTPS, 백업을 직접 다뤄보는 운영 구조입니다.

---

## 0. 핵심 결론

CaseLab AI의 1차 운영 구조는 아래처럼 가져간다.

```text
Android App
  ↓ HTTPS
api.caselab.ai
  ↓
Cloudflare DNS
  ↓
AWS Lightsail Ubuntu Server
  ↓
Nginx Reverse Proxy
  ↓
Docker Compose
  ├─ Spring Boot App
  ├─ MySQL
  └─ Redis
  ↓
Object Storage
  └─ Cloudflare R2 또는 S3
```

초기에는 AWS ALB, RDS 같은 완전 관리형 리소스를 바로 붙이지 않는다.

```text
초기 목표:
- Lightsail 단일 서버
- Nginx 직접 설정
- Docker Compose 직접 운영
- MySQL/Redis 직접 운영
- HTTPS 직접 설정
- 백업 직접 설정
```

이후 트래픽이나 운영 필요성이 생기면 단계적으로 분리한다.

```text
1단계: 단일 Lightsail 서버 운영
2단계: Nginx upstream으로 app-blue/app-green 로드밸런싱
3단계: DB를 Managed DB 또는 RDS로 분리
4단계: 실제 Load Balancer / App Server 분리
```

---

## 1. Android 앱 배포와 백엔드 배포의 차이

Android 앱 배포와 백엔드 서버 배포는 다르다.

### Android 앱 자체 배포

Android 앱은 웹처럼 서버에 `index.html`을 올리는 방식이 아니다.

```text
개발 중:
- Android Studio에서 직접 실행

팀원/테스터 배포:
- APK 직접 공유
- Firebase App Distribution

실제 출시:
- Google Play Console
```

Firebase App Distribution은 Android/iOS 앱을 신뢰할 수 있는 테스터에게 빠르게 배포하고 피드백을 받을 수 있는 도구다. 팀 프로젝트 테스트 배포에는 Firebase App Distribution이 적합하다.

### 백엔드 서버 배포

Android 앱은 결국 백엔드 API를 호출한다.

```text
Android App
  ↓
https://api.caselab.ai/api/scenarios
```

이 백엔드 배포는 웹 백엔드 배포와 거의 같다.

```text
Spring Boot
Nginx
Docker
MySQL
Redis
HTTPS
도메인
로그
백업
```

정리하면:

```text
Android 앱 배포:
웹과 다름

Android 앱이 호출하는 백엔드 서버 배포:
웹 백엔드와 거의 같음
```

---

## 2. 인프라 선택 기준

이번 프로젝트의 인프라 목표는 다음과 같다.

```text
1. 저렴해야 한다.
2. 직접 운영 경험이 남아야 한다.
3. Linux 명령어에 익숙해져야 한다.
4. Nginx reverse proxy와 로드밸런싱을 직접 설정해봐야 한다.
5. Docker Compose 기반 배포를 경험해야 한다.
6. DB와 Redis를 직접 운영해봐야 한다.
7. 백업과 HTTPS까지 직접 구성해봐야 한다.
8. 확장 가능성은 남겨둔다.
```

그래서 초기에는 다음을 우선한다.

```text
AWS Lightsail
Nginx
Docker Compose
MySQL Docker
Redis Docker
Cloudflare R2
Cloudflare DNS
Let's Encrypt
Firebase App Distribution
```

---

## 3. 추천 인프라 구조

## 3.1 1차 MVP 운영 구조

```text
[Android App]
  - Firebase App Distribution으로 테스트 배포
- API Base URL: https://api.caselab.ai

[DNS]
  - Cloudflare DNS
  - api.caselab.ai → Lightsail Public IP

[Server]
  - AWS Lightsail Ubuntu
  - Nginx
  - Docker / Docker Compose
  - Spring Boot App Container
  - MySQL Container
  - Redis Container

[Storage]
  - Cloudflare R2
  - 시나리오 썸네일, 증거 이미지, 용의자 이미지, 현장 이미지 저장

[AI]
  - 초기 구현은 Spring AI OpenAI를 백엔드에서만 호출
  - Claude / Gemini는 Provider 추상화 후 확장
  - Android 앱에는 AI API Key를 절대 넣지 않음

[Monitoring]
  - 초기: docker logs, Nginx logs
  - 이후: Prometheus / Grafana 선택
```

---

## 3.2 권장 서버 스펙

초기에는 Lightsail 2GB 인스턴스를 추천한다.

```text
권장 시작:
- Lightsail Small 2GB
- 2 vCPU
- 2GB RAM
- 60GB SSD
- 월 약 $12

여유 운영:
- Lightsail Medium 4GB
- 2 vCPU
- 4GB RAM
- 80GB SSD
- 월 약 $24
```

2GB 인스턴스에서 시작하고, 아래 문제가 생기면 4GB로 올린다.

```text
Spring Boot + MySQL + Redis를 동시에 돌릴 때 메모리가 부족함
Blue/Green app-blue/app-green 2개를 같이 띄우고 싶음
Prometheus/Grafana까지 같이 돌리고 싶음
```

---

## 4. 비용 계획

> 가격은 클라우드 사업자 정책에 따라 바뀔 수 있으므로, 실제 결제 전 공식 가격 페이지를 다시 확인한다.

## 4.1 최소 운영 비용

```text
Lightsail 2GB:
- 약 $12 / month

Cloudflare R2:
- 소규모 이미지 저장은 무료 티어 또는 소액 가능

도메인:
- 별도 구매 필요

AI API:
- 사용량 기반 별도 비용

Firebase App Distribution:
- 테스트 배포 용도
```

## 4.2 여유 운영 비용

```text
Lightsail 4GB:
- 약 $24 / month

Lightsail Load Balancer:
- 약 $18 / month
- 단, 초반에는 직접 Nginx upstream으로 대체

Managed Database:
- 추후 필요 시 별도 비용
```

초기 스타트업식 저비용 운영 목표에서는 아래 방식이 적합하다.

```text
초기:
Lightsail 2GB + MySQL Docker + Redis Docker + Nginx

확장:
Lightsail 4GB + app-blue/app-green + Nginx upstream

운영 안정화:
Managed DB 또는 RDS 분리
```

---

## 5. 배포 단계 로드맵

## Phase 0. 로컬 개발

```text
Android Studio
Spring Boot Local
MySQL Docker
Redis Docker
```

목표:

```text
API 개발
Android 연동
AI 기능 테스트
DB 마이그레이션 테스트
```

---

## Phase 1. 초저비용 운영 MVP

```text
Lightsail 2GB Ubuntu
Nginx
Docker Compose
Spring Boot app 1개
MySQL Docker
Redis Docker
Cloudflare R2
Cloudflare DNS
Let's Encrypt HTTPS
```

목표:

```text
외부에서 Android 앱이 실제 서버 API를 호출
HTTPS 도메인 연결
DB/Redis 운영 경험
수동 배포 경험
백업 스크립트 경험
```

---

## Phase 2. Blue/Green + 직접 로드밸런싱

```text
Lightsail 4GB 권장
Nginx upstream
app-blue:8081
app-green:8082
MySQL Docker
Redis Docker
```

목표:

```text
Nginx 로드밸런싱 직접 설정
무중단 배포 구조 연습
한쪽 컨테이너 재시작 중에도 API 유지
```

주의:

```text
같은 서버 안의 app-blue/app-green은 진짜 고가용성은 아니다.
서버 자체가 죽으면 모두 죽는다.
하지만 로드밸런싱과 Blue/Green 배포 원리 학습에는 좋다.
```

---

## Phase 3. DB 분리

```text
Lightsail App Server
Lightsail Managed DB 또는 RDS
Redis 별도 또는 같은 서버 유지
Object Storage 유지
```

목표:

```text
DB 장애 리스크 감소
백업/복구 안정성 증가
운영 안정화
```

---

## Phase 4. 실제 확장 구조

```text
Load Balancer
App Server 2대 이상
Managed DB
Managed Redis
CDN
Monitoring
Alert
```

이 단계는 MVP 이후다.

---

## 6. 서버 초기 세팅

## 6.1 Lightsail 인스턴스 생성

추천 OS:

```text
Ubuntu 22.04 LTS
또는
Ubuntu 24.04 LTS
```

열어야 할 포트:

```text
22   SSH
80   HTTP
443  HTTPS
```

열면 안 되는 포트:

```text
3306 MySQL 외부 공개 금지
6379 Redis 외부 공개 금지
8080 Spring Boot 외부 직접 공개 금지
```

---

## 6.2 SSH 접속

```bash
ssh ubuntu@서버_PUBLIC_IP
```

처음 접속 후 기본 업데이트:

```bash
sudo apt update
sudo apt upgrade -y
```

---

## 6.3 필수 패키지 설치

```bash
sudo apt install -y curl wget git unzip htop vim nano
```

---

## 6.4 Docker 설치

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

현재 유저를 docker 그룹에 추가:

```bash
sudo usermod -aG docker $USER
```

로그아웃 후 다시 로그인한다.

```bash
exit
ssh ubuntu@서버_PUBLIC_IP
```

확인:

```bash
docker --version
docker compose version
```

---

## 6.5 Nginx 설치

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
sudo systemctl status nginx
```

---

## 7. 서버 디렉터리 구조

서버에는 아래 구조로 배치한다.

```text
/opt/caselab/
  ├─ docker-compose.yml
  ├─ .env
  ├─ nginx/
  │   └─ caselab-api.conf
  ├─ mysql/
  │   └─ data/
  ├─ redis/
  │   └─ data/
  ├─ logs/
  └─ scripts/
      ├─ deploy.sh
      ├─ backup-mysql.sh
      └─ rollback.sh
```

생성:

```bash
sudo mkdir -p /opt/caselab
sudo chown -R $USER:$USER /opt/caselab

mkdir -p /opt/caselab/nginx
mkdir -p /opt/caselab/mysql/data
mkdir -p /opt/caselab/redis/data
mkdir -p /opt/caselab/logs
mkdir -p /opt/caselab/scripts
```

---

## 8. 환경변수 관리

`.env` 예시:

```env
SPRING_PROFILES_ACTIVE=prod

MYSQL_DATABASE=caselab
MYSQL_USER=caselab_user
MYSQL_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-root-me

REDIS_HOST=redis
REDIS_PORT=6379

SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=change-me
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_CHAT_TEMPERATURE=0.4

R2_ACCESS_KEY=change-me
R2_SECRET_KEY=change-me
R2_BUCKET=caselab-images
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_PUBLIC_BASE_URL=https://cdn.caselab.ai
```

주의:

```text
.env 파일은 절대 Git에 올리지 않는다.
Android 앱에 AI API Key를 넣지 않는다.
AI API Key는 백엔드 서버 환경변수로만 관리한다.
```

---

## 9. Docker Compose 예시

1차 MVP용 단일 app 구성이다.

```yaml
services:
  app:
    image: caselab-api:latest
    container_name: caselab-app
    env_file:
      - .env
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    container_name: caselab-mysql
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - ./mysql/data:/var/lib/mysql
    restart: unless-stopped

  redis:
    image: redis:7
    container_name: caselab-redis
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - ./redis/data:/data
    restart: unless-stopped
```

중요:

```text
MySQL과 Redis는 127.0.0.1에만 바인딩한다.
외부에서 3306, 6379로 접근할 수 없게 한다.
```

실행:

```bash
cd /opt/caselab
docker compose up -d
docker compose ps
docker compose logs -f app
```

---

## 10. Nginx Reverse Proxy

## 10.1 단일 app reverse proxy

`/etc/nginx/sites-available/caselab-api`:

```nginx
server {
    listen 80;
    server_name api.caselab.ai;

    location / {
        proxy_pass http://127.0.0.1:8080;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

적용:

```bash
sudo ln -s /etc/nginx/sites-available/caselab-api /etc/nginx/sites-enabled/caselab-api
sudo nginx -t
sudo systemctl reload nginx
```

---

## 10.2 Blue/Green Nginx upstream 예시

Phase 2에서 사용한다.

```nginx
upstream caselab_backend {
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
}

server {
    listen 80;
    server_name api.caselab.ai;

    location / {
        proxy_pass http://caselab_backend;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

이 구조로 직접 로드밸런싱과 app-blue/app-green 배포를 연습할 수 있다.

---

## 11. HTTPS 설정

Let's Encrypt + Certbot 사용.

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.caselab.ai
```

확인:

```bash
sudo certbot certificates
sudo systemctl status nginx
```

자동 갱신 테스트:

```bash
sudo certbot renew --dry-run
```

---

## 12. Cloudflare DNS 설정

Cloudflare에서 DNS 레코드 설정:

```text
Type: A
Name: api
Value: Lightsail Public IP
Proxy: DNS only 또는 Proxied
```

초기에는 문제를 줄이기 위해 `DNS only`로 먼저 연결하고, 안정화 후 Cloudflare Proxy를 켜도 된다.

---

## 13. 이미지 저장 전략

이미지는 DB에 직접 저장하지 않는다.

```text
DB에 이미지 바이너리 저장 ❌
서버 로컬 디스크 저장 ❌
Object Storage 저장 ⭕
DB에는 URL/key만 저장 ⭕
```

CaseLab에서 저장할 이미지:

```text
시나리오 썸네일
증거 이미지
용의자 프로필 이미지
현장 단면도
커스텀 시나리오 이미지
```

추천 저장소:

```text
1순위: Cloudflare R2
2순위: AWS S3
3순위: Lightsail Object Storage
```

DB에는 아래 정보만 저장한다.

```text
image_url
image_key
bucket_name
content_type
file_size
```

예시:

```text
evidences.image_url = https://cdn.caselab.ai/evidences/123/cup-label.png
```

---

## 14. MySQL 백업 전략

초기에는 MySQL을 같은 서버의 Docker로 운영하므로 백업이 필수다.

## 14.1 수동 백업

```bash
docker exec caselab-mysql mysqldump \
  -u root \
  -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} > backup_$(date +%Y%m%d).sql
```

압축:

```bash
gzip backup_$(date +%Y%m%d).sql
```

---

## 14.2 백업 스크립트

`/opt/caselab/scripts/backup-mysql.sh`:

```bash
#!/bin/bash

set -e

BACKUP_DIR="/opt/caselab/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

source /opt/caselab/.env

docker exec caselab-mysql mysqldump \
  -u root \
  -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} > ${BACKUP_DIR}/caselab_${DATE}.sql

gzip ${BACKUP_DIR}/caselab_${DATE}.sql

find ${BACKUP_DIR} -name "*.gz" -mtime +7 -delete

echo "Backup completed: caselab_${DATE}.sql.gz"
```

권한 부여:

```bash
chmod +x /opt/caselab/scripts/backup-mysql.sh
```

---

## 14.3 Cron 등록

```bash
crontab -e
```

매일 새벽 3시 백업:

```cron
0 3 * * * /opt/caselab/scripts/backup-mysql.sh >> /opt/caselab/logs/backup.log 2>&1
```

추후 백업 파일을 Cloudflare R2 또는 S3에 업로드하도록 확장한다.

---

## 15. 배포 방식

## 15.1 1단계: 수동 배포

초기에는 직접 수동 배포를 경험한다.

```bash
./gradlew clean bootJar
docker build -t caselab-api:latest .
```

서버에 이미지 또는 소스를 전달한 뒤:

```bash
cd /opt/caselab
docker compose up -d
docker compose logs -f app
```

장점:

```text
Linux, Docker, 로그 확인, 문제 해결 경험이 쌓인다.
```

---

## 15.2 2단계: 배포 스크립트

`deploy.sh` 예시:

```bash
#!/bin/bash

set -e

cd /opt/caselab

echo "Pull latest code or image..."
# git pull origin main
# docker build -t caselab-api:latest .

echo "Restart app..."
docker compose up -d app

echo "Check containers..."
docker compose ps

echo "Tail logs..."
docker compose logs -f app
```

---

## 15.3 3단계: GitHub Actions 자동화

나중에 적용한다.

```text
push to main
  ↓
build
  ↓
test
  ↓
docker image build
  ↓
SSH to Lightsail
  ↓
docker compose restart
```

초반에는 자동화보다 수동 운영을 먼저 경험한다.

---

## 16. Android 앱 배포 전략

## 16.1 개발 중

```text
Android Studio에서 직접 실행
```

로컬 백엔드 호출:

```text
Android Emulator:
http://10.0.2.2:8080

실제 기기:
서버에 배포된 https://api.caselab.ai 사용
```

---

## 16.2 테스터 배포

Firebase App Distribution 사용.

```text
APK 또는 AAB 업로드
테스터 이메일 추가
테스터가 앱 설치
피드백 수집
```

팀 프로젝트에서는 Google Play 출시 전까지 Firebase App Distribution이 적합하다.

---

## 16.3 실제 출시

Google Play Console 사용.

```text
개발자 계정 생성
AAB 업로드
스토어 정보 입력
테스트 트랙 운영
프로덕션 출시
```

실제 출시에는 개발자 계정 비용과 심사 과정이 필요하다.

---

## 17. 보안 체크리스트

반드시 지킬 것:

```text
SSH 비밀번호 로그인 끄기
SSH 키 로그인만 사용
DB 포트 외부 공개 금지
Redis 포트 외부 공개 금지
Spring Boot 8080 외부 직접 공개 금지
Nginx 80/443만 외부 공개
AI API Key는 서버 환경변수로만 관리
Android 앱에 API Key 넣지 않기
.env 파일 Git에 올리지 않기
업로드 파일 확장자 제한
업로드 파일 크기 제한
CORS 허용 도메인 제한
```

절대 하면 안 되는 것:

```text
3306 MySQL을 0.0.0.0으로 공개 ❌
6379 Redis를 0.0.0.0으로 공개 ❌
AI Provider API Key를 Android에 포함 ❌
이미지 파일을 DB BLOB으로 직접 저장 ❌
root 계정으로 서비스 운영 ❌
```

---

## 18. 로그와 모니터링

## 18.1 초기 로그 확인

```bash
docker compose logs -f app
docker compose logs -f mysql
docker compose logs -f redis
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## 18.2 서버 상태 확인

```bash
df -h
free -m
top
htop
docker ps
docker stats
ss -tulnp
```

---

## 18.3 이후 모니터링 확장

```text
Prometheus
Grafana
Spring Boot Actuator
Nginx metrics
Docker metrics
```

MVP에서는 선택 사항이다.

---

## 19. Linux 명령어 연습 목록

이번 프로젝트에서 익숙해질 명령어:

```bash
ssh
pwd
ls -al
cd
mkdir
cp
mv
rm
cat
less
tail -f
nano
vim
chmod
chown
systemctl status nginx
systemctl restart nginx
docker ps
docker logs
docker compose up -d
docker compose down
docker compose logs -f
df -h
free -m
top
htop
curl
ss -tulnp
crontab -e
```

---

## 20. 장애 대응 기본 Runbook

## 20.1 API가 안 열릴 때

```bash
curl -I https://api.caselab.ai
sudo nginx -t
sudo systemctl status nginx
docker compose ps
docker compose logs -f app
```

---

## 20.2 Spring Boot 컨테이너가 죽었을 때

```bash
docker compose ps
docker compose logs app
docker compose restart app
```

---

## 20.3 DB 연결 실패

```bash
docker compose ps mysql
docker compose logs mysql
docker exec -it caselab-mysql mysql -u root -p
```

확인할 것:

```text
.env 비밀번호
DB 이름
Docker network
Spring datasource URL
```

---

## 20.4 Nginx 설정 오류

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo tail -f /var/log/nginx/error.log
```

---

## 20.5 디스크 부족

```bash
df -h
docker system df
docker system prune
```

주의:

```text
운영 중에는 docker volume 삭제 금지
MySQL data volume 삭제 금지
```

---

## 21. 발표할 때 설명 문장

인프라 방향은 이렇게 설명하면 좋다.

```text
이번 프로젝트에서는 단순히 EC2와 ALB를 연결하는 과제식 배포가 아니라,
초기 스타트업처럼 비용 효율적인 운영 구조를 목표로 했습니다.

AWS Lightsail Ubuntu 서버에 Nginx reverse proxy와 Docker Compose를 구성하고,
Spring Boot, MySQL, Redis를 직접 운영합니다.

초기에는 단일 서버로 비용을 최소화하고,
이후에는 Nginx upstream을 이용해 app-blue/app-green 구조로 직접 로드밸런싱과 무중단 배포를 실험할 계획입니다.

이미지 파일은 DB에 저장하지 않고 Cloudflare R2 같은 Object Storage에 저장하며,
DB에는 URL과 key만 저장합니다.

또한 MySQL 백업 스크립트와 HTTPS 인증서 갱신, 로그 확인, 서버 상태 점검까지 직접 구성해 실제 운영 경험을 쌓는 것을 목표로 합니다.
```

---

## 22. 최종 추천 구성

## 1차 운영 MVP

```text
Lightsail 2GB / Ubuntu
Docker Compose
Nginx
Spring Boot
MySQL Docker
Redis Docker
Cloudflare R2
Cloudflare DNS
Let's Encrypt
Firebase App Distribution
```

## 2차 운영 개선

```text
Lightsail 4GB 업그레이드
Nginx upstream으로 app-blue/app-green 구성
배포 스크립트 작성
MySQL 백업 자동화
로그/모니터링 추가
```

## 3차 확장

```text
DB를 Lightsail Managed DB 또는 RDS로 분리
실제 Load Balancer 도입
Redis 분리
CDN/이미지 도메인 연결
GitHub Actions 자동 배포
```

---

## 23. 참고 공식 문서

- AWS Lightsail Pricing: https://aws.amazon.com/lightsail/pricing/
- AWS Lightsail Bundles: https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html
- AWS Lightsail Load Balancing: https://aws.amazon.com/lightsail/features/load-balancing/
- Cloudflare R2 Pricing: https://developers.cloudflare.com/r2/pricing/
- Cloudflare R2 Product: https://www.cloudflare.com/products/r2/
- Firebase App Distribution: https://firebase.google.com/docs/app-distribution
- Firebase Android Distribution Guide: https://firebase.google.com/docs/app-distribution/android/distribute-console

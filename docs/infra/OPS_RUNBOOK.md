# ClueRoom 운영 Runbook

## 1. 현재 서비스 상태 확인

```bash
cd /opt/clueroom/app
docker compose ps
curl https://api.clueroom.xyz/actuator/health
```

## 2. Spring Boot App 로그 확인

단일 app 모드:

```bash
cd /opt/clueroom/app
docker compose logs -f app
```

최근 로그만 확인:

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 app
```

Blue-Green 모드:

```bash
/opt/clueroom/bg-compose logs -f app-blue
/opt/clueroom/bg-compose logs -f app-green
```

Blue-Green 최근 로그:

```bash
/opt/clueroom/bg-compose logs --tail=120 app-blue
/opt/clueroom/bg-compose logs --tail=120 app-green
```

## 3. MySQL 로그 확인

```bash
cd /opt/clueroom/app
docker compose logs -f mysql
```

## 4. Redis 로그 확인

```bash
cd /opt/clueroom/app
docker compose logs -f redis
```

## 5. Nginx 상태 확인

```bash
sudo systemctl status nginx
sudo nginx -t
```

## 6. Nginx 로그 확인

Access log:

```bash
sudo tail -f /var/log/nginx/access.log
```

Error log:

```bash
sudo tail -f /var/log/nginx/error.log
```

최근 에러 80줄:

```bash
sudo tail -n 80 /var/log/nginx/error.log
```

## 7. Certbot / HTTPS 확인

```bash
sudo certbot certificates
curl -I http://api.clueroom.xyz/actuator/health
```

정상이라면 HTTP 요청은 HTTPS로 리다이렉트된다.

## 8. 서버 리소스 확인

메모리:

```bash
free -m
```

디스크:

```bash
df -h
```

Docker 리소스:

```bash
docker stats
```

Docker 디스크 사용량:

```bash
docker system df
```

## 9. 배포

레포 원본 스크립트를 서버 실행 위치로 배치한다.

```bash
cd /opt/clueroom/app
git pull origin develop
sudo cp scripts/deploy-bluegreen.sh /opt/clueroom/deploy.sh
sudo cp scripts/bg-compose.sh /opt/clueroom/bg-compose
sudo cp scripts/backup-mysql.sh /opt/clueroom/backup-mysql.sh
sudo cp scripts/bg-status.sh /opt/clueroom/bg-status.sh
sudo cp scripts/stop-standby.sh /opt/clueroom/stop-standby.sh
sudo cp scripts/rollback-bluegreen.sh /opt/clueroom/rollback-bluegreen.sh
sudo chmod +x \
  /opt/clueroom/deploy.sh \
  /opt/clueroom/bg-compose \
  /opt/clueroom/backup-mysql.sh \
  /opt/clueroom/bg-status.sh \
  /opt/clueroom/stop-standby.sh \
  /opt/clueroom/rollback-bluegreen.sh
```

배포 실행:

```bash
/opt/clueroom/deploy.sh
```

Blue-Green 상태 확인:

```bash
/opt/clueroom/bg-status.sh
```

배포 성공 후 standby 정리:

```bash
/opt/clueroom/stop-standby.sh
```

새 배포에 문제가 있을 때 이전 slot으로 rollback:

```bash
/opt/clueroom/rollback-bluegreen.sh
```

주의사항:

- `stop-standby.sh`는 active slot을 자동 감지하고 active가 아닌 slot만 중지한다.
- `rollback-bluegreen.sh`는 반대편 slot container가 기존에 존재할 때만 rollback한다.
- rollback script는 target slot을 새로 build하지 않는다.
- blue/green을 사람이 직접 판단해 stop하지 말고 helper script를 사용한다.

## 10. MySQL 백업

수동 백업:

```bash
/opt/clueroom/backup-mysql.sh
```

백업 파일 확인:

```bash
ls -lh /opt/clueroom/backups/mysql
```

백업 로그 확인:

```bash
tail -f /opt/clueroom/logs/mysql-backup.log
```

## 11. 장애 확인 순서

1. API health check 확인
2. `docker compose ps` 확인
3. app 로그 확인
4. nginx 상태 확인
5. nginx error log 확인
6. mysql / redis 로그 확인
7. 서버 메모리와 디스크 확인

## 12. Prometheus / Grafana 확인

운영 서버에서는 외부에 직접 공개하지 않고 SSH 터널로 확인한다.

```bash
ssh -N -L 3000:localhost:3000 -L 9090:localhost:9090 clueroom
```

Blue-Green scrape target:

```text
app:8080
app-blue:8080
app-green:8080
```

standby app을 중지한 경우 `app-blue` 또는 `app-green` target이 `DOWN`으로 보일 수 있다. 단일 서버 Blue-Green PoC에서는 active app이 정상이고 외부 health check가 통과하면 정상 범위로 본다.

## 13. 주의사항

- `.env` 전체 내용을 출력하지 않는다.
- AWS Secret Key, FCM service account JSON, DB password를 캡처하지 않는다.
- `docker compose down -v`는 MySQL 볼륨을 삭제할 수 있으므로 사용하지 않는다.
- 백업 파일은 현재 같은 서버에 저장되므로 서버 자체 장애에는 취약하다.
- 추후 백업 파일을 S3로 업로드하는 방식으로 고도화한다.

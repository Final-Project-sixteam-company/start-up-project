# ClueRoom 운영 Runbook

## 1. 현재 서비스 상태 확인

```bash
cd /opt/clueroom/app
docker compose ps
curl https://api.clueroom.xyz/actuator/health
```

## 2. Spring Boot App 로그 확인

```bash
cd /opt/clueroom/app
docker compose logs -f app
```

최근 로그만 확인:

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 app
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

```bash
/opt/clueroom/deploy.sh
```

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

## 12. 주의사항

- `.env` 전체 내용을 출력하지 않는다.
- AWS Secret Key, FCM service account JSON, DB password를 캡처하지 않는다.
- `docker compose down -v`는 MySQL 볼륨을 삭제할 수 있으므로 사용하지 않는다.
- 백업 파일은 현재 같은 서버에 저장되므로 서버 자체 장애에는 취약하다.
- 추후 백업 파일을 S3로 업로드하는 방식으로 고도화한다.

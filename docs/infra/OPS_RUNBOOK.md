# ClueRoom OPS Runbook

> 운영 서버에서 장애 확인, 배포, 백업, Blue-Green 전환을 수행할 때 사용하는 절차다. 실제 secret 값은 이 문서에 기록하지 않는다.

## 1. 현재 서비스 상태 확인

```bash
curl https://api.clueroom.xyz/actuator/health
curl -I https://api.clueroom.xyz/actuator/health
docker ps
cd /opt/clueroom/app
docker compose ps
```

`curl -I` 응답의 `X-ClueRoom-Upstream` 헤더로 현재 Nginx가 바라보는 upstream을 확인한다.

## 2. Spring Boot App 로그 확인

단일 app 모드:

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 app
docker compose logs -f app
```

Blue-Green 모드:

```bash
/opt/clueroom/bg-compose logs --tail=120 app-blue
/opt/clueroom/bg-compose logs --tail=120 app-green
```

## 3. MySQL 로그 확인

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 mysql
docker compose ps mysql
docker compose exec -T mysql mysqladmin ping -uroot -p
```

운영 DB 비밀번호는 `.env`에만 두며, 문서나 채팅에 남기지 않는다.

## 4. Redis 로그 확인

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 redis
docker compose ps redis
docker compose exec -T redis redis-cli ping
```

Redis 비밀번호를 활성화한 환경에서는 `.env`의 `REDIS_PASSWORD` 기준으로 접속한다.

## 5. Nginx 상태/문법 검사

```bash
sudo nginx -t
sudo systemctl status nginx
sudo systemctl reload nginx
```

설정 파일 위치:

```text
/etc/nginx/sites-available/clueroom-api
/etc/nginx/conf.d/clueroom-upstream.conf
```

## 6. Nginx access/error log 확인

```bash
sudo tail -n 80 /var/log/nginx/access.log
sudo tail -n 80 /var/log/nginx/error.log
sudo journalctl -u nginx --no-pager -n 100
```

## 7. Certbot / HTTPS 확인

```bash
sudo certbot certificates
sudo certbot renew --dry-run
curl https://api.clueroom.xyz/actuator/health
curl -I http://api.clueroom.xyz/actuator/health
```

인증서 private key 내용은 서버 밖으로 복사하지 않는다.

## 8. 서버 리소스 확인

```bash
free -m
df -h
docker stats
docker system df
```

`docker image prune` 같은 정리 명령은 현재 사용 중인 이미지와 볼륨을 확인한 뒤 실행한다.

## 9. 배포

운영 배포는 서버의 Blue-Green 배포 스크립트로 수행한다.

```bash
/opt/clueroom/deploy.sh
```

레포 원본 스크립트:

```text
scripts/deploy-bluegreen.sh
```

CD workflow도 SSH로 서버에 접속해 같은 `/opt/clueroom/deploy.sh`를 실행한다.

## 10. Blue-Green 상태 확인

```bash
curl -I https://api.clueroom.xyz/actuator/health
cat /etc/nginx/conf.d/clueroom-upstream.conf
/opt/clueroom/bg-compose ps
```

포트 기준:

```text
app-blue  -> 127.0.0.1:8081
app-green -> 127.0.0.1:8082
```

## 11. Blue-Green rollback

8081로 되돌리는 예시:

```bash
sudo sed -i -E 's#127\.0\.0\.1:808[12]#127.0.0.1:8081#g' /etc/nginx/conf.d/clueroom-upstream.conf
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

8082로 되돌릴 때는 마지막 포트만 `8082`로 바꾼다.

## 12. MySQL 백업

```bash
/opt/clueroom/backup-mysql.sh
ls -lh /opt/clueroom/backups/mysql
```

레포 원본 스크립트:

```text
scripts/backup-mysql.sh
```

백업 파일은 `*.sql.gz`이며 Git에 커밋하지 않는다.

## 13. 장애 확인 순서

1. 외부 health check를 확인한다.
2. `curl -I`로 현재 upstream 헤더를 확인한다.
3. Nginx 문법과 상태를 확인한다.
4. active app 로그를 확인한다.
5. MySQL/Redis 컨테이너 상태를 확인한다.
6. 디스크/메모리 부족 여부를 확인한다.
7. 최근 배포가 원인이라면 upstream을 직전 포트로 rollback한다.

필수 명령어:

```bash
curl https://api.clueroom.xyz/actuator/health
curl -I https://api.clueroom.xyz/actuator/health
docker ps
docker compose ps
sudo nginx -t
sudo tail -n 80 /var/log/nginx/error.log
free -m
df -h
```

## 14. 주의사항

- `.env`, `/opt/clueroom/secrets`, Firebase service account JSON, PEM/private key, DB backup 파일은 레포에 넣지 않는다.
- 운영 secret은 GitHub Actions Secrets에 몰아넣지 않고 서버 `.env`와 `/opt/clueroom/secrets/env.d`에서 관리한다.
- `app-blue`와 `app-green`은 같은 MySQL/Redis를 공유하므로 파괴적 DB 스키마 변경은 Blue-Green과 함께 배포하지 않는다.
- old service 정리는 외부 health check와 사용자 동작 확인 후 수동으로 수행한다.
- 2GB Lightsail 서버에서는 Prometheus/Grafana 상시 운영 여부를 메모리 사용량 기준으로 판단한다.

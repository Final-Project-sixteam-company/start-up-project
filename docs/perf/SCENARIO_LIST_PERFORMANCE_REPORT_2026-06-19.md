# Scenario List Performance Report 2026-06-19

## Scope

이 문서는 PR #73 `Feat/performance optimization`에서 반영된 시나리오 목록 API 성능 개선 결과를 public-safe 형태로 요약한다.

대상 API:

```http
GET /api/scenarios?page=0&size=20&sort=createdAt,desc
```

이 API는 앱/웹의 홈 또는 시나리오 라이브러리 진입 시 반복 호출되는 핵심 read path다.

## Goal

| 항목 | 목표 |
|---|---:|
| 기준 사용자 가정 | DAU 5,000명 peak window |
| 방어 목표 | 30 RPS |
| latency 목표 | P95 200ms 이하 |
| error 목표 | error rate 1% 미만 |

## Test Setup

| 항목 | 값 |
|---|---|
| 도구 | k6 |
| 스크립트 | [scripts/k6/scenario-list-load-test.js](../../scripts/k6/scenario-list-load-test.js) |
| 테스트 유형 | load/stress 형태의 50 VU 유지 테스트 |
| VU ramp-up | 10초 동안 50 VU까지 증가 |
| steady window | 30초 동안 50 VU 유지 |
| ramp-down | 10초 동안 0 VU로 감소 |
| threshold | `http_req_duration p(95)<200`, `http_req_failed rate<0.01` |

실행 예:

```bash
k6 run scripts/k6/scenario-list-load-test.js
```

## Bottleneck

초기 병목은 시나리오 목록 조회에서 발생했다.

| 원인 | 영향 |
|---|---|
| `status`, `visibility` 조건 필터링 | 공개 시나리오 목록 조회에서 반복 적용 |
| `created_at DESC` 정렬 | 홈/최신순 목록에서 정렬 비용 발생 |
| 연관 count 조립 | suspect/evidence count 조립 비용 발생 |
| index 부재 | Full Table Scan 및 filesort 발생 |

## Changes

### 1. Query Path 정리

- QueryDSL 기반 동적 필터로 시나리오 목록 조건을 명확히 분리했다.
- keyword/type/difficulty/sort/page 조건을 목록 read path에 맞춰 정리했다.

### 2. Composite Index 추가

마이그레이션:

[docs/db/migrations/20260618_add_scenario_list_index.sql](../db/migrations/20260618_add_scenario_list_index.sql)

```sql
CREATE INDEX idx_scenarios_status_visibility_created
ON scenarios (status, visibility, created_at DESC);
```

기대 효과:

- `status`, `visibility` 조건 필터링 비용 감소
- `created_at DESC` 정렬의 filesort 제거
- 목록 read path의 DB scan 범위 축소

### 3. Redis Short TTL Cache

- 시나리오 목록 응답에 Redis 30초 캐시를 적용했다.
- 신규 등록, 북마크 변동 등 실시간성을 완전히 포기하지 않도록 긴 TTL 대신 짧은 TTL을 선택했다.
- `PageResponse` generic 직렬화 이슈를 피하기 위해 `StringRedisTemplate` 기반 수동 캐시 경로를 사용했다.

## Result

| 단계 | P95 latency | 판단 |
|---|---:|---|
| Baseline | 241ms | 목표 200ms 초과 |
| Index 적용 후 | 185ms | 목표 이하로 개선 |
| Redis cache 적용 후 | 19ms | hot read path 기준 목표 대비 여유 확보 |

최종 결과:

```text
P95 latency: 241ms -> 19ms
개선 폭: 약 12배
error rate: 0.00%
```

## Reliability Notes

- 리뷰 평점 갱신은 신뢰성이 중요하므로 DB 비관적 락으로 lost update를 방어한다.
- playCount 증가는 엔티티 조회 없이 query-level atomic update로 처리한다.
- 캐시는 목록 read path에 한정하고, 플레이 세션/정답/진행 상태 같은 stateful path에는 적용하지 않는다.

## Limits

- 이 문서는 PR #73에 기록된 public-safe 성능 요약을 기준으로 한다.
- raw k6 console output 전문은 저장소에 커밋하지 않았다.
- 운영 트래픽은 네트워크, 인스턴스 상태, DB cache 상태, 데이터량에 따라 달라질 수 있다.
- 성능 수치는 “해당 테스트 조건에서의 측정값”이며, 모든 운영 window에서 항상 동일하다는 의미는 아니다.

## Follow-up

| 항목 | 목적 |
|---|---|
| k6 결과 JSON/summary artifact 보존 | 다음 성능 PR에서 before/after 검증 근거 강화 |
| Grafana read latency panel 추가 | 운영 read path latency 추세 확인 |
| cache hit/miss metric 추가 | Redis TTL 정책 조정 근거 확보 |
| 목록 API 조건별 테스트 확대 | keyword/type/difficulty/rating sort별 병목 확인 |


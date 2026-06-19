# ClueRoom LLMOps Daily Report - 2026-06-19

> 상태: 2026-06-19 운영 LLMOps daily report다. 원문 prompt, 사용자 질문 전문, AI 답변 원문, token/session id, 정답/후보 상세는 포함하지 않는다.

## 0. Scope

이 문서는 2026-06-19 KST 오전에 공유된 `ClueRoom Infra Codex Handoff Report`와 `ClueRoom LLMOps Codex Handoff Report v3`를 기준으로, 최근 24시간 운영 AI 호출 상태와 인프라 상태를 public-safe 형식으로 정리한다.

```text
Report source:
  - ClueRoom Infra Codex Handoff Report
  - ClueRoom LLMOps Codex Handoff Report v3
Window: 2026-06-18T00:40:17.053Z ~ 2026-06-19T00:40:17.053Z
Captured infra snapshot: 2026-06-19T09:30:15+09:00
LLMOps status: WARNING
Infra handoff raw status: CRITICAL
Environment: 운영 Loki 기반 AI_CALL / AI_CALL_CONTEXT aggregate
Primary model: deepseek-v4-flash
```

주의:

```text
이 문서는 운영 LLMOps 집계 보고서다.
같은 날짜의 Web E2E QA 보고서는 운영 API를 사용한 QA activity이므로, 이번 24시간 AI_CALL 표본은 organic production traffic으로 보지 않는다.
비용은 실제 청구액이 아니라 token 기반 추정이다.
AI_CALL_CONTEXT의 block token 값은 exact tokenizer 결과가 아니라 estimate일 수 있다.
Infra handoff의 CRITICAL 표기는 운영 상태 원문으로 보존하되, 이 문서에서는 원인 신호를 분해해서 판단한다.
```

## 1. Source Documents Reviewed

| Source | 반영 내용 |
|---|---|
| `ClueRoom Infra Codex Handoff Report` | prod/data/ops health, Blue-Green 상태, memory/disk, App warning sample, AI_CALL sample mismatch |
| `ClueRoom LLMOps Codex Handoff Report v3` | 최근 24시간 AI_CALL, AI_CALL_CONTEXT, token, latency, failure/fallback 집계 |
| `docs/infra/agent/LLMOPS_DAILY_SUMMARY_2026-06-09_TO_2026-06-13.md` | 이전 LLMOps 추세와 prompt context dominant block 해석 |
| `docs/infra/agent/LLMOPS_COST_AND_RATE_LIMIT_PLAN_2026-06-17.md` | 비용/레이트리밋 기준, 30~50 목표와 80~100 현실 baseline 구분 |
| `docs/infra/agent/LLMOPS_DAILY_REPORT_2026-06-18.md` | 전일 daily report 구조, QA/local 비용 경계, coverage 해석 |
| `docs/infra/agent/LLMOPS_OPERATING_GUIDE.md` | raw-free telemetry, high-cardinality label 금지, smoke/runbook 기준 |
| `docs/qa/archive/QA_ANDROID_E2E_LOCAL_RETEST_REPORT_2026-06-18.md` | local none/mock Android QA는 provider 비용 표본에서 제외 |
| `docs/qa/archive/QA_WEB_E2E_REPORT_2026-06-19.md` | 2026-06-19 웹 QA가 운영 API write와 AI 호출을 발생시킨 맥락 |

## 2. Safety Declaration

```text
Raw prompt recorded in this report: no
Raw AI answer recorded in this report: no
Raw user question recorded in this report: no
Raw token/session id recorded in this report: no
High-cardinality metric labels recommended: no
Scenario/suspect/culprit identity exposed: no
Result score/grade/correctness/breakdown exposed: no
Provider cost treated as exact billing: no
AI_CALL_CONTEXT treated as exact tokenizer count: no
Production API writes acknowledged: yes, from web QA context
Production infra/config mutation performed by this report: no
```

이 보고서는 raw-free aggregate만 다룬다. 운영 디버깅이 필요하더라도 prompt/answer/question 원문 저장을 권장하지 않는다.

## 3. Final Judgment

```text
전체 판단: WARNING
Failure/fallback: PASS
Latency: WATCH
Cost/token: WATCH
Prompt context size: IMPROVED but not proven optimized
Data boundary: QA-driven production window, not organic traffic
Infra cross-check: WATCH, due to memory pressure and report mismatch
Rate limit change: NO CHANGE
Next action: report query alignment + provider-enabled QA cost/session measurement
```

핵심 해석:

```text
최근 24시간 운영 AI 호출은 95건 모두 성공했고 failure/fallback은 0건이다.
전일 10건 smoke 수준보다 표본은 늘었지만, 6/19 웹 E2E QA와 겹치므로 일반 사용자 traffic 기준으로 해석하면 안 된다.
총 토큰은 140,760으로 전일보다 증가했지만, 호출당 평균 total token은 1,482로 6/13/6/18 보고서보다 낮다.
이번 window의 AI_CALL_CONTEXT는 INTERROGATION 91건과 1:1로 맞아 보이며, FINAL_DEDUCTION 4건은 context breakdown 대상이 아닌 것으로 해석한다.
Infra handoff는 CRITICAL로 표시됐지만 data/server/ops health와 Nginx 5xx는 OK다. 주요 watch item은 낮은 available memory, refresh token warning noise, Infra handoff의 AI_CALL sample 0과 LLMOps handoff의 AI_CALL 95 불일치다.
```

## 4. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P1 | Data boundary | 이번 24시간 AI_CALL은 웹 QA activity가 섞인 운영 표본이다 | `QA_WEB_E2E_REPORT_2026-06-19.md`는 운영 API에서 login, interrogation, final deduction 경로를 수행했고 LLMOps window는 같은 기간 AI_CALL 95건을 기록 | 운영 비용 기준은 organic traffic, QA, smoke를 구분 | 현재 집계는 QA-driven production window로 보는 것이 안전 | rate limit이나 사용자당 비용 기준을 과도하게 조정할 수 있음 | 보고서에 QA-driven window임을 명시하고, organic traffic 또는 controlled provider smoke를 별도 표본으로 모은다 |
| P1 | Report consistency | Infra handoff는 최근 AI_CALL 샘플 0건, LLMOps handoff는 95건으로 서로 다르다 | Infra summary: AI_CALL sample 0. LLMOps summary: AI_CALL 95, AI_CALL_CONTEXT 91 | 같은 window/LogQL이면 큰 차이가 없어야 함 | workflow별 query/window/source가 다르게 동작한 것으로 보임 | 운영자가 AI traffic 없음으로 오판할 수 있음 | Infra/Ops Snapshot Agent와 LLMOps Handoff의 Loki query, job label, window, `AI_CALL ` vs `AI_CALL_CONTEXT ` filter를 대조 |
| P1 | Cost/token | `npc_interrogation_v1`이 여전히 token 총량을 지배한다 | INTERROGATION 91건, totalTokens 137,792, prompt ratio 97.4% | 심문 prompt가 관리 가능한 budget 안에 있음 | 실패는 없지만 비용 대부분이 심문 prompt에서 발생 | 사용자 수 증가 시 비용이 심문 횟수에 선형 증가 | evidence top-k, history compaction, calls-to-submit-ready 계측 계획 유지 |
| P1/P2 | Infra memory | prod 서버 available memory가 낮다 | `free -m`: available 362MB, Blue/Green app 둘 다 10시간 이상 running | 운영 안정 상태에서는 memory warning이 누적되지 않음 | active/standby app + monitoring stack으로 여유가 낮음 | deploy/restart/AI traffic spike 시 지연 또는 OOM 위험 증가 | 배포 안정 확인 후 standby slot stop 기준을 운영자가 판단하고, memory alert threshold를 runbook과 맞춘다 |
| P2 | Prompt context | 호출당 token은 이전 보고서보다 낮지만 workload composition 영향일 수 있다 | 6/13 avgTotal 3,031, 6/18 avgTotal 2,253, 6/19 avgTotal 1,482. 6/19 avgEvidence 803, evidenceCount 8 | token 감소가 prompt 개선 때문인지 workload 때문인지 구분 | 웹 QA에서 초기/중간 evidence 중심 호출이 많았을 가능성 | 개선 효과로 오판하면 v2_compact 우선순위가 밀릴 수 있음 | 동일 시나리오/동일 질문 set으로 v1/v2 또는 날짜별 비교를 수행 |
| P2 | FINAL_DEDUCTION latency | 최종 추리 지연은 계속 WATCH다 | FINAL_DEDUCTION 4건, avgLatency 2,955ms, maxLatency 3,931ms | 표본이 쌓이면 p95/p99로 관리 | 현재 표본은 4건이라 일반화 불가 | 결과 제출 UX가 느려질 수 있음 | 표본 30건 이상 확보 전까지 threshold 변경 금지, slow sample metadata만 추적 |
| P2 | Auth noise | App warning 3건은 refresh token invalid에 집중됐다 | 2026-06-19T00:03:10Z `BusinessException: Refresh token is not valid.` 3건 | OAuth/refresh cookie 전환 후 bootstrap noise가 과도하지 않음 | 3건 warning은 서비스 장애는 아니지만 auth transition과 연결 가능 | App ERROR alert가 실제 장애와 auth noise를 구분하지 못할 수 있음 | Web OAuth/HttpOnly cookie 배포 이후 refresh 401/invalid token baseline을 분리해서 alert 조정 |
| P2 | Telemetry coverage | AI_CALL_CONTEXT coverage는 전일보다 명확해졌다 | AI_CALL 95, INTERROGATION 91, FINAL_DEDUCTION 4, AI_CALL_CONTEXT 91 | prompt context breakdown은 INTERROGATION 기준으로 해석 | 전체 AI_CALL 대비 91/95로 보면 4건 누락처럼 보임 | coverage 오탐 가능 | daily report에 `INTERROGATION context coverage 91/91`와 `FINAL_DEDUCTION context N/A`를 분리 표기 |
| P3 | Report formatting | Slack handoff 일부에 literal `\n`이 계속 표시된다 | feature/prompt/sample line에 `\n2.` 형태가 남아 있음 | 사람이 읽는 report는 실제 줄바꿈 렌더링 | 복붙 보고서 가독성 저하 | 운영자가 수치 해석을 놓칠 수 있음 | n8n Slack formatter에서 escaped newline 처리 수정 |

## 5. Metric Summary

| Metric | Value |
|---|---:|
| AI_CALL | 95 |
| AI_CALL_CONTEXT | 91 |
| success / failure / fallback | 95 / 0 / 0 |
| failure rate | 0% |
| fallback rate | 0% |
| avg latency | 1,567ms |
| max latency | 3,931ms |
| prompt tokens | 136,314 |
| completion tokens | 4,446 |
| total tokens | 140,760 |
| avg prompt tokens / call | 1,435 |
| avg completion tokens / call | 47 |
| avg total tokens / call | 1,482 |
| prompt token ratio | 96.8% |

해석:

```text
실패와 fallback은 없다.
completion token은 작고, prompt token이 비용 대부분을 차지한다.
전일보다 call count와 total token은 늘었지만, 호출당 평균 token은 낮아졌다.
이 감소는 prompt 최적화가 아니라 QA 시나리오 진행 상태와 포함 evidence 수 차이일 수 있다.
```

## 6. Trend Against Previous Reports

| Report | AI_CALL | AI_CALL_CONTEXT | Avg total/call | Prompt ratio | Avg evidence tokens | Avg history tokens | Avg evidence count | 해석 |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| 2026-06-13 summary | 157 | 155 | 3,031 | 97.9% | 1,830 | 579 | 18 | extended/backend QA 중심, evidence/history block 큼 |
| 2026-06-18 daily | 10 | 9 | 2,253 | 97.2% | 1,778 | 37 | 17 | low-volume smoke, 표본 부족 |
| 2026-06-19 daily | 95 | 91 | 1,482 | 96.8% | 803 | 76 | 8 | web QA window, evidence count 낮아 평균 token 감소 |

판단:

```text
2026-06-19는 token/call이 개선된 것처럼 보이지만, evidenceCount가 평균 8개로 낮아져 생긴 workload effect일 가능성이 크다.
따라서 `npc_interrogation_v2_compact` 필요성은 유지한다.
다만 6/19 수치는 "evidence block을 줄이면 비용이 실제로 줄어든다"는 방향성 근거로 사용할 수 있다.
```

## 7. Feature / PromptVersion Summary

| Feature | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `INTERROGATION` | 91 | 91/0/0 | 1,506ms / 3,042ms | 1,475 / 1,514 | 97.4% | 137,792 |
| `FINAL_DEDUCTION` | 4 | 4/0/0 | 2,955ms / 3,931ms | 516 / 742 | 69.5% | 2,968 |

| PromptVersion | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `npc_interrogation_v1` | 91 | 91/0/0 | 1,506ms / 3,042ms | 1,475 / 1,514 | 97.4% | 137,792 |
| `final_deduction_scoring_v1` | 4 | 4/0/0 | 2,955ms / 3,931ms | 516 / 742 | 69.5% | 2,968 |

`FINAL_DEDUCTION`은 4건뿐이라 지연/비용 특성을 일반화하지 않는다. 비용 최적화 판단은 계속 `npc_interrogation_v1` 중심으로 유지한다.

## 8. Prompt Context Breakdown

`npc_interrogation_v1` 기준:

| Block | Avg tokens |
|---|---:|
| system | 271 |
| policy | 54 |
| NPC | 103 |
| evidence | 803 |
| history | 76 |
| question | 16 |
| prompt chars | 2,188 |
| history turns | 1 |
| evidence count | 8 |
| template hash count | 2 |

판단:

```text
dominant block은 여전히 evidenceContextTokens다.
다만 이번 window의 average evidence token은 803으로, 6/13의 1,830과 6/18의 1,778보다 낮다.
history token은 76으로 낮은 편이며, long-running extended session 표본이 적었을 가능성이 있다.
templateHashCount=2는 이전 보고서와 동일하게 관찰된다. 배포 차이, prompt template 분기, hash 계산 기준 차이일 수 있으므로 drift로 단정하지 않는다.
```

## 9. Infra Cross-Check

| Area | Signal | Judgment |
|---|---|---|
| Data health | MySQL OK, Redis OK, disk OK, backup OK, backup_age_minutes 380 | PASS |
| Server health | prod heartbeat OK, disk 26% | PASS |
| Ops health | n8n OK, Loki OK, Loki ready OK, disk 13%, memory OK by ops report | PASS |
| Nginx 5xx | recent sample count 0 | PASS |
| App warning | refresh token invalid 3건 | WATCH |
| AI_CALL in infra handoff | sample count 0 | MISMATCH with LLMOps handoff |
| Blue-Green | active app-green, standby app-blue, both up 10h | PASS/WATCH |
| Prod memory | available 362MB, swap used 628MB | WATCH |
| External data | DB/Redis target 172.26.1.185, TCP OK | PASS |
| LLMOps DB logging | `AI_LLMOPS_DB_LOGGING_ENABLED=false` | EXPECTED |

Infra handoff raw status는 CRITICAL이지만, 이 문서 기준으로는 즉시 서비스 장애 증거보다 watch 항목이 더 선명하다.

```text
CRITICAL로 볼 수 있는 직접 증거:
  - 없음. health, data, Nginx 5xx는 정상.

WARNING/WATCH로 볼 증거:
  - available memory 362MB
  - auth refresh warning 3건
  - Infra handoff AI_CALL 0 vs LLMOps AI_CALL 95 query mismatch
  - Blue/Green app 두 slot이 장시간 동시에 떠 있어 memory를 소비
```

## 10. QA Cross-Check

6/19 웹 QA 보고서는 이번 LLMOps window의 중요한 해석 기준이다.

| Source | Environment | Observation | LLMOps cost usage |
|---|---|---|---|
| `QA_ANDROID_E2E_LOCAL_RETEST_REPORT_2026-06-18.md` | local backend, Android emulator, local none/mock AI | Android reachability와 local QA 확인 | 운영 provider 비용 표본에서 제외 |
| `QA_WEB_E2E_REPORT_2026-06-19.md` | web frontend, production API, QA account scoped writes | Studio9/Seowolchae web flow, interrogation, final-deduction/result path 확인 | 이번 운영 AI_CALL window에 포함된 QA activity로 해석 |
| `LLMOps Codex Handoff Report v3` | 운영 Loki AI_CALL / AI_CALL_CONTEXT | AI_CALL 95건, 실패 0건, prompt ratio 96.8% | 운영 provider token/cost aggregate |

QA 관점에서 중요한 점:

```text
6/19 LLMOps 수치는 웹 E2E 과정에서 발생한 실제 provider 호출을 포함한다.
따라서 "웹 배포 시 실제 비용 smoke"로는 의미가 있지만, 일반 사용자 평균 비용으로 보기에는 표본이 편향되어 있다.
웹 QA 보고서의 interrogation turns 48건보다 LLMOps INTERROGATION 91건이 많은 것은 retest/addendum, repeated QA, OAuth/auth smoke 또는 별도 운영 호출이 섞였을 가능성이 있다.
raw question/answer 없이도 QA-driven traffic이라는 경계는 보고서에 남겨야 한다.
```

## 11. Cost Interpretation

2026-06-17 비용 계획 문서의 단가 가정을 그대로 적용하면, 이번 운영 24시간 140,760 total tokens는 약 `$0.0203`, 약 31원 수준으로 추정된다.

계산 기준:

```text
input/prompt tokens: 136,314
output/completion tokens: 4,446
input price: $0.14 / 1M tokens
output price: $0.28 / 1M tokens
exchange rate assumption: 1 USD = 1,513 KRW
rough cost: 136,314 / 1,000,000 * 0.14 + 4,446 / 1,000,000 * 0.28 = $0.0203
```

이 값은 rough estimate다.

```text
포함:
  - 운영 AI_CALL 95건
  - 운영 AI_CALL_CONTEXT 91건 기반 해석
  - 6/19 web QA activity로 보이는 provider 호출

미포함:
  - 2026-06-18 local Android E2E QA의 none/mock 심문
  - provider invoice 조정
  - cache hit 실제 적용 여부
  - retry, timeout, 장애성 중복 호출
  - organic user와 QA user 분리 비용
```

비용/레이트리밋 기준은 유지한다.

| 기준 | 현재 판단 |
|---|---|
| 30~50회 | 달성된 평균이 아니라 제품 목표 |
| 80~100회 | 이전 extended QA 기반 현실적 비용 대표값 |
| 120회 | session hard cap 후보 |
| 6/19 AI_CALL 95건 | QA-driven smoke로 유효하지만 cap 변경 근거는 아님 |

## 12. Verification

```text
LLMOps:
  - AI_CALL count: 95
  - AI_CALL_CONTEXT count: 91
  - INTERROGATION count: 91
  - FINAL_DEDUCTION count: 4
  - success/failure/fallback: 95/0/0
  - failure/fallback sample: none
  - prompt token ratio: 96.8%
  - INTERROGATION context coverage: 91/91
  - FINAL_DEDUCTION context breakdown: not expected in current report

Infra:
  - data health: OK
  - server health: OK
  - ops health: OK
  - Nginx 5xx sample: 0
  - app warning sample: refresh token invalid x3
  - external DB/Redis connectivity: OK
  - active upstream: 127.0.0.1:8082
  - available memory: 362MB, WATCH

Data boundary:
  - 2026-06-18 Android QA is local none/mock mode and excluded from provider cost
  - 2026-06-19 Web QA used production API and likely contributes to this provider cost window
  - organic production traffic cannot be separated from QA traffic in this aggregate alone

Privacy:
  - raw prompt not included
  - raw AI answer not included
  - raw user question not included
  - token/session id not included
  - high-cardinality metric label recommendation not added
```

## 13. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Ops / n8n | P1 | Infra Handoff와 LLMOps Handoff의 AI_CALL LogQL/window/job label 차이를 확인하고, `AI_CALL `과 `AI_CALL_CONTEXT `가 섞이지 않게 query를 고정 |
| LLMOps | P1 | daily report에 `AI_CALL_CONTEXT coverage by feature`를 추가. 전체 91/95가 아니라 `INTERROGATION 91/91`, `FINAL_DEDUCTION N/A`로 표시 |
| LLMOps / QA | P1 | web QA, Android local QA, organic traffic을 구분할 수 있도록 report에 source category 또는 QA window marker를 남김 |
| Backend / AI | P1 | `npc_interrogation_v2_compact`, evidence top-k, history compaction 실험 유지. 6/19 low token/call은 workload effect로 보고 검증 필요 |
| Ops | P1/P2 | Blue-Green 배포 안정 확인 후 standby app stop 기준을 runbook대로 판단해 memory pressure 완화 |
| Backend / Auth | P2 | refresh token invalid warning baseline을 웹 OAuth/HttpOnly cookie 전환 이후 따로 관측. spike 시 auth bootstrap/retry flow 확인 |
| Ops / n8n | P2 | Slack report의 literal `\n` 렌더링을 수정 |
| LLMOps | P2 | provider invoice와 내부 AI_CALL token aggregate 대조 절차를 다음 report에 추가 |
| QA | P2 | production web QA를 수행할 때 QA account scoped write 승인 여부와 대략적인 AI_CALL count를 같이 기록 |

## 14. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw prompt
raw AI answer
raw user question
raw token
raw session id
QA account email
scenario/suspect/culprit identity
정답 수법/동기/은폐
result score/grade/correctness/breakdown
provider invoice 원문
private seed 원문
```

필요한 원천 로그 확인은 운영자가 redacted snippet으로 수행하고, public 문서에는 aggregate만 남긴다.

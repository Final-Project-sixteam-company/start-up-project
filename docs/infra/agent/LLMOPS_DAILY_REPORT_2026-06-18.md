# ClueRoom LLMOps Daily Report - 2026-06-18

> 상태: 2026-06-18 운영 LLMOps daily report다. 원문 prompt, 사용자 질문 전문, AI 답변 원문, token/session id, 정답/후보 상세는 포함하지 않는다.

## 0. Scope

이 문서는 2026-06-18 KST에 공유된 `ClueRoom LLMOps Codex Handoff Report v3`를 기준으로, 최근 24시간 운영 AI 호출 상태를 public-safe 형식으로 정리한다.

```text
Report source: ClueRoom LLMOps Codex Handoff Report v3
Window: 2026-06-17T00:40:17.039Z ~ 2026-06-18T00:40:17.039Z
Status: INFO
Environment: 운영 Loki 기반 AI_CALL / AI_CALL_CONTEXT aggregate
Primary model: deepseek-v4-flash
```

주의:

```text
이 문서는 운영 LLMOps 집계 보고서다.
같은 날짜의 Android E2E QA 보고서는 local backend + local none/mock AI mode였으므로, 실제 provider 비용/토큰 표본으로 합산하지 않는다.
비용은 실제 청구액이 아니라 token 기반 추정이다.
AI_CALL_CONTEXT의 block token 값은 exact tokenizer 결과가 아니라 estimate일 수 있다.
```

## 1. Source Documents Reviewed

| Source | 반영 내용 |
|---|---|
| `ClueRoom LLMOps Codex Handoff Report v3` | 최근 24시간 AI_CALL, AI_CALL_CONTEXT, token, latency, failure/fallback 집계 |
| `docs/infra/agent/LLMOPS_DAILY_SUMMARY_2026-06-09_TO_2026-06-13.md` | 이전 LLMOps daily report 구조와 token risk 해석 |
| `docs/infra/agent/LLMOPS_COST_AND_RATE_LIMIT_PLAN_2026-06-17.md` | 비용/레이트리밋 기준, 30~50회 목표 재해석, hard cap 후보 |
| `docs/qa/archive/QA_ANDROID_E2E_LOCAL_RETEST_REPORT_2026-06-18.md` | 같은 날짜 QA 결과와 데이터 경계 대조. 비용 표본으로는 사용하지 않음 |

## 2. Safety Declaration

```text
Raw prompt recorded in this report: no
Raw AI answer recorded in this report: no
Raw user question recorded in this report: no
Raw token/session id recorded in this report: no
High-cardinality metric labels recommended: no
Scenario/suspect/culprit identity exposed: no
Provider cost treated as exact billing: no
AI_CALL_CONTEXT treated as exact tokenizer count: no
```

이 보고서는 raw-free aggregate만 다룬다. 운영 디버깅이 필요하더라도 prompt/answer/question 원문 저장을 권장하지 않는다.

## 3. Final Judgment

```text
전체 판단: INFO
Failure/fallback: PASS
Latency: PASS for current low-volume window
Cost/token: WATCH
Data completeness: PARTIAL, because total AI_CALL count is low and 2026-06-18 Android QA was local none/mock mode
Rate limit change: NO CHANGE
Next action: production/staging AI provider enabled smoke 필요
```

핵심 해석:

```text
최근 24시간 운영 AI 호출은 실패 없이 정상 처리됐다.
다만 AI_CALL 10건은 운영 비용 기준을 조정하기에 너무 적은 표본이다.
prompt token 비율은 97.2%로 여전히 높고, 비용 구조는 INTERROGATION / npc_interrogation_v1이 지배한다.
AI_CALL_CONTEXT 기준으로 evidenceContextTokens가 가장 큰 block이다.
18일 Android E2E QA는 local none/mock mode였으므로, 그 QA의 30~50/51~87턴 흐름을 provider 비용으로 계산하면 안 된다.
```

## 4. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P1 | Data boundary | 2026-06-18 QA 보고서와 운영 LLMOps 집계를 합산하면 비용 판단이 왜곡됨 | QA report는 local profile + local none/mock AI mode, LLMOps report는 운영 Loki AI_CALL 10건 | local QA reachability와 운영 provider token/cost를 분리 | 둘 다 같은 날짜라 혼동 가능 | 플레이당 비용, rate limit, QA 품질 판단이 섞일 수 있음 | LLMOps 문서에 데이터 경계를 명시하고, provider-enabled staging/production QA smoke를 별도로 수행 |
| P1 | Cost/token | `npc_interrogation_v1`이 token 총량을 계속 지배함 | INTERROGATION 9건, totalTokens 21,802, prompt ratio 98.3% | 심문 prompt block이 관리 가능한 budget 안에 있음 | 호출당 total 2,422, top sample은 4,448까지 상승 | 사용자 수 증가 시 비용이 심문 횟수에 선형 증가 | evidence top-k, evidence token budget, history summary 실험 유지 |
| P1 | Prompt context | evidence context가 가장 큰 비용 block임 | AI_CALL_CONTEXT 평균 evidence 1,778 tokens, 평균 evidenceCount 17, top sample evidenceCount 35 | 질문/상태와 관련 높은 증거 중심으로 prompt 구성 | 해금 증거가 많아질수록 evidence block이 커짐 | 후반부/extended play에서 prompt spike 가능 | `evidenceContextTokens >= 2500`, `includedEvidenceCount >= 20` 경보 후보 유지 |
| P2 | Sample size | 최근 24시간 운영 표본이 작아 기준 변경 근거로 부족함 | AI_CALL 10건, AI_CALL_CONTEXT 9건 | 운영 판단은 충분한 표본으로 수행 | smoke 수준의 정상 확인만 가능 | p95/p99, 비용 평균, rate limit 재조정이 불안정 | AI_CALL >= 100, INTERROGATION >= 80 조건에서 재평가 |
| P2 | Telemetry coverage | AI_CALL 10건 대비 AI_CALL_CONTEXT 9건으로 1건 차이가 있음 | FINAL_DEDUCTION 1건은 context breakdown 대상이 아닐 수 있음 | 누락인지 설계인지 명확히 구분 | 현재 보고서만으로는 단정 불가 | coverage 경보 오탐 가능 | FINAL_DEDUCTION context logging 제외가 의도인지 코드/문서 확인 |
| P3 | Report formatting | Slack handoff 일부에 literal `\n`이 그대로 표시됨 | feature/prompt breakdown 문장에 `\n2.` 형태가 포함됨 | 사람이 읽는 Slack report는 실제 줄바꿈 렌더링 | 복붙 보고서 가독성 저하 | 운영자가 수치 해석을 놓칠 수 있음 | n8n Slack formatter에서 escaped newline 처리 수정 |

## 5. Metric Summary

| Metric | Value |
|---|---:|
| AI_CALL | 10 |
| AI_CALL_CONTEXT | 9 |
| success / failure / fallback | 10 / 0 / 0 |
| failure rate | 0% |
| fallback rate | 0% |
| avg latency | 2012ms |
| max latency | 3011ms |
| prompt tokens | 21,906 |
| completion tokens | 624 |
| total tokens | 22,530 |
| avg prompt tokens / call | 2,191 |
| avg completion tokens / call | 62 |
| avg total tokens / call | 2,253 |
| prompt token ratio | 97.2% |

해석:

```text
실패와 fallback은 없다.
completion token은 작고, prompt token이 비용 대부분을 차지한다.
호출량이 낮아 오늘 비용이 낮게 보이지만, 비용 위험이 사라진 것은 아니다.
```

## 6. Feature / PromptVersion Summary

| Feature | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `INTERROGATION` | 9 | 9/0/0 | 1919ms / 3011ms | 2382 / 2422 | 98.3% | 21,802 |
| `FINAL_DEDUCTION` | 1 | 1/0/0 | 2850ms / 2850ms | 467 / 728 | 64.1% | 728 |

| PromptVersion | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `npc_interrogation_v1` | 9 | 9/0/0 | 1919ms / 3011ms | 2382 / 2422 | 98.3% | 21,802 |
| `final_deduction_scoring_v1` | 1 | 1/0/0 | 2850ms / 2850ms | 467 / 728 | 64.1% | 728 |

`FINAL_DEDUCTION`은 1건뿐이라 지연/비용 특성을 일반화하지 않는다. 비용 최적화 판단은 `npc_interrogation_v1` 중심으로 유지한다.

## 7. Prompt Context Breakdown

`npc_interrogation_v1` 기준:

| Block | Avg tokens |
|---|---:|
| system | 271 |
| policy | 49 |
| NPC | 75 |
| evidence | 1,778 |
| history | 37 |
| question | 14 |
| prompt chars | 3,507 |
| history turns | 1 |
| evidence count | 17 |
| template hash count | 2 |

판단:

```text
dominant block은 evidenceContextTokens다.
history token은 낮지만 이번 표본이 초기/저회차 중심이라 낮게 나온 것으로 본다.
evidenceCount 24~35개 샘플에서 promptTokens 4,000대가 발생했다.
후반 플레이 또는 extended route에서는 evidence/history block이 다시 커질 수 있다.
```

## 8. QA Cross-Check

같은 날짜의 QA 보고서는 운영 provider 비용 표본이 아니라 로컬 앱 연결성 검증이다.

| Source | Environment | Observation | LLMOps cost usage |
|---|---|---|---|
| `QA_ANDROID_E2E_LOCAL_RETEST_REPORT_2026-06-18.md` | local backend, Android emulator, local none/mock AI | 서월채 31~43 구간, 스튜디오9 51~87 구간까지 reachability 확인 | 비용 계산에 사용하지 않음 |
| `LLMOps Codex Handoff Report v3` | 운영 Loki AI_CALL / AI_CALL_CONTEXT | AI_CALL 10건, 실패 0건, prompt ratio 97.2% | 운영 provider token/cost smoke로 사용 |

QA 관점에서 중요한 점:

```text
서월채는 30~50턴 범위에 가까워졌지만 strict blind validity는 제한된다.
스튜디오9는 여전히 30~50턴 목표를 넘는 extended route가 필요하다.
local none/mock 답변 품질은 후보 축소에 충분히 기여하지 못했다.
따라서 플레이당 실제 비용은 provider-enabled staging/production QA smoke로 다시 재야 한다.
```

## 9. Cost Interpretation

2026-06-17 비용 계획 문서의 단가 가정을 그대로 적용하면, 이번 운영 24시간 22,530 total tokens는 약 `$0.0032`, 약 5원 수준으로 추정된다.

이 값은 오늘 운영 window의 실제 provider 호출에 대한 rough estimate일 뿐이다.

```text
포함:
- 운영 AI_CALL 10건
- 운영 AI_CALL_CONTEXT 9건 기반 해석

미포함:
- 2026-06-18 local Android E2E QA의 none/mock 심문
- provider invoice 조정
- cache hit 실제 적용 여부
- retry, timeout, 장애성 중복 호출
```

비용/레이트리밋 기준은 유지한다.

| 기준 | 현재 판단 |
|---|---|
| 30~50회 | 달성된 평균이 아니라 제품 목표 |
| 80~100회 | 이전 extended QA 기반 현실적 비용 대표값 |
| 120회 | session hard cap 후보 |
| 오늘 AI_CALL 10건 | 기준 변경 근거 아님 |

## 10. Verification

```text
LLMOps:
  - AI_CALL count: 10
  - AI_CALL_CONTEXT count: 9
  - success/failure/fallback: 10/0/0
  - failure/fallback sample: none
  - prompt token ratio: 97.2%

Data boundary:
  - 2026-06-18 Android QA is local none/mock mode
  - Android QA turns are not counted as provider cost sample
  - production/staging provider-enabled smoke still required

Privacy:
  - raw prompt not included
  - raw AI answer not included
  - raw user question not included
  - token/session id not included
  - high-cardinality metric label recommendation not added
```

## 11. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| LLMOps | P1 | provider-enabled staging/production QA smoke를 시나리오별 fresh session 1회씩 수행해 실제 플레이당 token/cost 표본 확보 |
| AI / Backend | P1 | `npc_interrogation_v1` evidence context top-k, token budget, history summary 실험 유지 |
| Ops / n8n | P2 | Slack handoff report의 escaped newline 렌더링 수정 |
| Backend | P2 | FINAL_DEDUCTION이 AI_CALL_CONTEXT 대상에서 제외되는 것이 의도인지 확인하고 문서화 |
| QA | P2 | local none/mock QA 보고서와 provider-enabled QA 보고서를 분리해서 기록 |
| LLMOps | P2 | `AI_CALL >= 100`, `INTERROGATION >= 80`, `AI_CALL_CONTEXT coverage >= 90%` 조건에서 다음 비용 판단 수행 |

## 12. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw prompt
raw AI answer
raw user question
raw token
raw session id
scenario/suspect/culprit identity
정답 수법/동기/은폐
provider invoice 원문
private QA account identity
```

필요한 원천 로그 확인은 운영자가 redacted snippet으로 수행하고, public 문서에는 aggregate만 남긴다.

# ClueRoom LLMOps 일자별 통합 보고서

작성일: 2026-06-13

이 문서는 2026-06-09부터 2026-06-13까지 Slack/작업 로그로 공유된 LLMOps 보고서와 Codex 분석 결과를 중복 제거해 합친 일자별 정리본이다. 운영 로그 원문, 프롬프트 원문, 사용자 질문/AI 답변 원문은 포함하지 않고, 집계 지표와 조치 판단만 남긴다.

## 0. 통합 결론

| 구분 | 판단 |
|---|---|
| 안정성 | 관측된 LLM 호출은 전 기간 failure/fallback 0건이다. 장애성 LLM 실패는 확인되지 않았다. |
| 비용/토큰 | 2026-06-10 이후 호출량과 prompt token이 빠르게 증가했다. 최신 v3 기준 prompt token 비중이 97.9%라 비용 최적화가 필요하다. |
| 주요 원인 | `npc_interrogation_v1`의 증거 컨텍스트와 대화 이력이 prompt 대부분을 차지한다. |
| 지연시간 | 평균 지연은 운영 장애 수준은 아니지만, `FINAL_DEDUCTION`은 표본이 적어 p95/p99 판단을 미룬다. |
| 인프라 연계 | 2026-06-13 기준 LLMOps는 WARNING이다. 실패/fallback은 없지만 `npc_interrogation_v1` prompt token 비중이 높고, `AI_CALL_CONTEXT`는 estimate 기반이므로 추세 지표로 사용한다. |

최우선 개선 방향은 `npc_interrogation_v2_compact` 성격의 압축 프롬프트 경로를 만들고, 증거 top-k/토큰 예산/대화 이력 요약을 적용하는 것이다.

## 1. 일자별 요약

| 일자 | 기준 보고서 | 상태 | AI_CALL | 성공/실패/fallback | 평균/최대 지연 | 총 토큰 | 핵심 판단 |
|---|---|---:|---:|---:|---:|---:|---|
| 2026-06-09 | LLMOps v2 | INFO | 4 | 4/0/0 | 2950ms / 3923ms | 5,221 | 저용량 기준선. 실패는 없고 `INTERROGATION` 3건, `FINAL_DEDUCTION` 1건만 관측됨. |
| 2026-06-10 | Infra + LLMOps v2 | WARNING | 48 | 48/0/0 | 1917ms / 2772ms | 136,194 | LLM 실패는 없지만 호출량과 토큰 사용량이 전일 대비 크게 증가. Infra는 App ERROR 샘플 때문에 CRITICAL로 표시됐으나 Nginx 5xx와 health 실패는 없음. |
| 2026-06-11 | LLMOps v2 | WARNING | 144 | 144/0/0 | 1934ms / 4749ms | 400,097 | `INTERROGATION` 141건이 대부분을 차지. `FINAL_DEDUCTION` 3건은 평균 지연이 더 높아 추적 필요. |
| 2026-06-12 | Infra + LLMOps v3 | WARNING | 152 | 152/0/0 | 1789ms / 3656ms | 482,041 | v3에서 `AI_CALL_CONTEXT` 150건 확인. prompt token 비중 약 98%, 증거/이력 컨텍스트가 비용의 주 원인. |
| 2026-06-13 | LLMOps Codex Handoff v3 | WARNING | 157 | 157/0/0 | 1632ms / 3494ms | 475,900 | `AI_CALL_CONTEXT` 155건. 실패/fallback은 없지만 prompt token 비중 97.9%, `INTERROGATION` totalTokens 474,197로 비용 대부분을 차지. |

2026-06-11 v2와 2026-06-12 v3는 일부 기간이 겹친다. 단순 호출량 추세는 둘 다 참고하고, 컨텍스트 원인 분석은 `AI_CALL_CONTEXT`가 포함된 2026-06-12 v3를 최신 기준으로 본다.
2026-06-13 v3는 2026-06-11T17:38:47Z부터 2026-06-12T17:38:47Z까지의 최근 24시간 window이며, KST 기준 2026-06-13 새벽에 공유된 handoff다.

## 2. 일자별 상세

### 2026-06-09

LLMOps 상태는 INFO다.

- `AI_CALL`: 4건
- 성공/실패/fallback: 4/0/0
- 평균/최대 지연: 2950ms / 3923ms
- 토큰: prompt 4,866, completion 355, total 5,221
- 기능별 호출: `INTERROGATION` 3건, `FINAL_DEDUCTION` 1건
- promptVersion: `npc_interrogation_v1` 3건, `final_deduction_scoring_v1` 1건

저용량 기준선으로 볼 수 있다. 호출 수가 적어 성능 판단에는 제한이 있지만, 실패와 fallback이 없다는 점은 정상이다.

### 2026-06-10

LLMOps 상태는 WARNING이다.

- `AI_CALL`: 48건
- 성공/실패/fallback: 48/0/0
- 평균/최대 지연: 1917ms / 2772ms
- 토큰: prompt 134,124, completion 2,070, total 136,194
- 기능별 호출: `INTERROGATION` 48건
- promptVersion: `npc_interrogation_v1` 48건

LLM 자체 실패는 없다. 다만 전일 대비 호출량과 총 토큰이 급증했으므로, 이 시점부터 비용 관점의 WARNING으로 보는 것이 맞다.

같은 날 Infra 보고서는 App ERROR/Exception 샘플 7건 때문에 CRITICAL로 표시됐다. Nginx 5xx는 0건이고 data/server/ops health는 OK였으므로, LLM 장애와 직접 연결된 신호로 보지는 않는다. 해당 App ERROR는 별도 애플리케이션 로그 원인 분석 대상이다.

### 2026-06-11

LLMOps 상태는 WARNING이다.

- `AI_CALL`: 144건
- 성공/실패/fallback: 144/0/0
- 평균/최대 지연: 1934ms / 4749ms
- 토큰: prompt 390,938, completion 9,159, total 400,097
- 기능별 호출:
  - `INTERROGATION`: 141건, totalTokens 396,959
  - `FINAL_DEDUCTION`: 3건, totalTokens 3,138
- promptVersion:
  - `npc_interrogation_v1`: 141건
  - `final_deduction_scoring_v1`: 3건

호출 성공률은 좋지만, 비용은 `INTERROGATION` 쪽에 집중된다. `FINAL_DEDUCTION`은 표본이 3건뿐이라 결론을 내리기는 이르지만 평균/최대 지연이 상대적으로 높아 이후 표본 30건 이상에서 p95/p99를 다시 봐야 한다.

### 2026-06-12

Infra 상태는 INFO다.

- data health: MySQL, Redis, disk, backup OK
- prod Blue-Green: active upstream `127.0.0.1:8081`, active `app-blue`, standby `app-green`
- external data target: MySQL/Redis TCP OK
- Nginx 5xx 샘플: 0건
- App ERROR/Exception 샘플: 0건
- ops health: n8n, Loki, disk, memory OK
- `AI_LLMOPS_DB_LOGGING_ENABLED=false`

Infra 보고서의 `AI_CALL samples=0`은 "LLM 호출이 0건"이라고 단정하면 안 된다. DB logging 비활성화와 트래픽 부재를 구분할 별도 metric/log 경로가 필요하다.

LLMOps v3 상태는 WARNING이다.

- `AI_CALL`: 152건
- `AI_CALL_CONTEXT`: 150건
- 성공/실패/fallback: 152/0/0
- 평균/최대 지연: 1789ms / 3656ms
- 토큰: prompt 472,499, completion 9,542, total 482,041
- 호출당 평균 토큰: prompt 3,109, completion 63, total 3,171
- prompt token 비중: 약 98%

`npc_interrogation_v1` 컨텍스트 집계:

| 항목 | 평균 |
|---|---:|
| system token | 271 |
| policy token | 58 |
| NPC token | 51 |
| evidence token | 1,924 |
| history token | 598 |
| question token | 46 |
| prompt chars | 4,660 |
| history turns | 6 |
| evidence count | 19 |

증거 컨텍스트와 이력이 prompt의 대부분을 차지한다. 특히 일부 샘플은 evidence token이 3,700대, evidence count가 35개까지 올라가므로, 모든 해금 증거를 넓게 싣는 방식은 장기적으로 비용과 지연을 키운다.

### 2026-06-13

LLMOps Codex Handoff v3 상태는 WARNING이다.

- window: 2026-06-11T17:38:47.230Z ~ 2026-06-12T17:38:47.230Z
- `AI_CALL`: 157건
- `AI_CALL_CONTEXT`: 155건
- 성공/실패/fallback: 157/0/0
- 실패율/fallback율: 0% / 0%
- 평균/최대 지연: 1632ms / 3494ms
- 토큰: prompt 465,821, completion 10,079, total 475,900
- 호출당 평균 토큰: prompt 2,967, completion 64, total 3,031
- prompt token 비중: 97.9%

Feature별 요약:

| Feature | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `INTERROGATION` | 155 | 155/0/0 | 1608ms / 3207ms | 2998 / 3059 | 98% | 474,197 |
| `FINAL_DEDUCTION` | 2 | 2/0/0 | 3487ms / 3494ms | 596 / 852 | 69.9% | 1,703 |

PromptVersion별 요약:

| PromptVersion | Count | Success/Failure/Fallback | Avg/Max Latency | Prompt/Total Per Call | Prompt Ratio | Total Tokens |
|---|---:|---:|---:|---:|---:|---:|
| `npc_interrogation_v1` | 155 | 155/0/0 | 1608ms / 3207ms | 2998 / 3059 | 98% | 474,197 |
| `final_deduction_scoring_v1` | 2 | 2/0/0 | 3487ms / 3494ms | 596 / 852 | 69.9% | 1,703 |

`npc_interrogation_v1` 컨텍스트 집계:

| 항목 | 평균 |
|---|---:|
| system token | 271 |
| policy token | 57 |
| NPC token | 49 |
| evidence token | 1,830 |
| history token | 579 |
| question token | 33 |
| prompt chars | 4,459 |
| history turns | 6 |
| evidence count | 18 |
| template hash count | 2 |

해석:

- `INTERROGATION`이 전체 token의 거의 전부를 차지한다.
- 비용 원인은 completion이 아니라 prompt다. 특히 `evidenceContextTokens`가 dominant block이다.
- top token 샘플들은 `npc_interrogation_v1`에서 promptTokens 5,000대까지 올라갔다.
- recent context sample에서는 evidence token 3,672, evidence count 35, history turns 8~10인 구간이 반복됐다.
- `FINAL_DEDUCTION`은 평균 지연이 높지만 표본이 2건뿐이라 p95/p99 판단에는 부족하다.
- 실패/fallback이 없으므로 provider 안정성보다 context size 관리가 우선이다.
- `AI_CALL_CONTEXT` block token은 exact tokenizer 결과가 아니라 estimate일 수 있으므로 절대값보다 추세와 block 비중 중심으로 본다.

## 3. 통합 LLMOps 분석

### 정상으로 볼 수 있는 부분

- 관측 구간 전체에서 LLM failure/fallback은 0건이다.
- `INTERROGATION` 대량 호출에서도 평균 지연은 2초 안팎으로 유지됐다.
- raw prompt/answer/user question을 보고서에 싣지 않는 방향은 적절하다.

### 위험으로 볼 부분

- 비용 원인은 completion이 아니라 prompt다. 최신 v3 기준 prompt token 비중이 97.9%다.
- `npc_interrogation_v1`이 전체 토큰 대부분을 사용한다.
- evidence context가 최신 v3 기준 평균 1,830 tokens로 가장 크고, history context가 평균 579 tokens로 뒤를 잇는다.
- evidence + history가 prompt 대부분을 차지하므로, 단순 모델 교체보다 컨텍스트 구성 개선의 효과가 더 클 가능성이 높다.
- `AI_LLMOPS_DB_LOGGING_ENABLED=false` 상태에서 `AI_CALL samples=0`만 보면 실제 호출 부재와 로깅 경로 부재를 구분하기 어렵다.
- top token 구간에서 evidence count가 35개까지 올라가므로, 해금 증거 전체를 싣는 방식은 장기적으로 비용이 선형 또는 그 이상으로 늘 수 있다.

### 아직 결론을 내리면 안 되는 부분

- `FINAL_DEDUCTION` 지연시간은 표본이 2~3건 수준이라 p95/p99 판단이 불가능하다.
- 최대 지연 샘플이 항상 최대 토큰 샘플과 일치하지 않으므로, provider/network/cold path 영향도 같이 봐야 한다.
- 2026-06-10 App ERROR 샘플은 LLM 호출 실패와 직접 연결된 증거가 아니므로 별도 애플리케이션 로그 분석이 필요하다.
- `templateHashCount=2`는 배포 차이, prompt template 분기, 또는 hash 계산 기준 차이일 수 있으므로 현재 수치만으로 drift라고 단정하지 않는다.

## 4. 비용 절감 개선 의견

현재 비용 절감의 핵심은 모델 교체가 아니라 `npc_interrogation_v1`의 prompt context를 줄이는 것이다. 2026-06-13 v3 기준 completion은 호출당 평균 64 tokens에 불과하고, prompt가 호출당 평균 2,967 tokens다. 따라서 completion 길이를 줄이는 것보다 evidence/history context를 줄이는 작업이 우선순위가 높다.

### 4.1 우선순위 판단

| Priority | 개선 후보 | 근거 | 기대 효과 | 주의점 |
|---|---|---|---|---|
| P0 | Evidence relevance top-k | 최신 v3 평균 evidence token 1,830, top 구간 evidence count 35 | prompt/call의 가장 큰 block을 직접 축소 | 정답 단서 누락 위험. 사용자가 제시한 증거와 현재 suspect 관련 증거는 무조건 포함 |
| P0 | Evidence token budget | top token 샘플에서 promptTokens 5,000대 반복 | 극단값을 제한해 비용/지연 spike 완화 | 단순 자르기는 위험. evidence별 요약/우선순위가 필요 |
| P1 | History compaction | 최신 v3 평균 history token 579, top 구간 historyTurns 8~10 | 장기 심문에서 비용 증가를 완화 | 최근 발화와 이미 확인된 사실 요약을 분리해야 함 |
| P1 | Static context 축약 | system/policy/NPC 합계는 evidence보다 작지만 매 호출 반복 | 작은 폭이지만 모든 호출에 누적 절감 | 안전 정책 문구를 과도하게 줄이면 누설 방어가 약해질 수 있음 |
| P2 | Final deduction latency 추적 | `FINAL_DEDUCTION` 평균 3487ms지만 표본 2건 | 지연 원인 후보 확인 | 호출량/토큰 비중이 낮아 비용 절감 1순위는 아님 |

### 4.2 권장 설계

`npc_interrogation_v2_compact`는 아래 원칙으로 설계한다.

```text
1. 항상 포함:
   - NPC public profile / alibi / public statement
   - 사용자가 이번 질문에 직접 제시한 증거
   - 현재 심문 대상 suspect와 직접 연결된 해금 증거
   - ResponsePolicyResolver가 요구하는 allowed facts

2. 제한 포함:
   - 최근 해금 증거 top N
   - 질문 키워드와 matching되는 증거
   - 같은 장소/시간대/관련 인물로 연결되는 증거

3. 제외 또는 요약:
   - 질문/대상 suspect와 직접 관련이 약한 오래된 해금 증거
   - 이미 여러 번 prompt에 들어간 장문 증거 설명
   - UI 표시용 상세 설명 중 AI 답변에 필요 없는 문장
```

증거 top-k는 spoiler metadata를 쓰면 안 된다. `importance=CORE`, `culpritEligible` 같은 truth-adjacent field를 relevance 계산에 쓰면 비용은 줄어도 blind gameplay 안전성을 해친다. 대신 public-safe signal을 사용한다.

```text
허용 후보:
- user-presented evidence id
- relatedSuspects
- location/time overlap
- recent unlock
- question keyword match
- previous conversation references

피해야 할 후보:
- culpritEligible
- solution/variant truth
- importance=CORE/FAKE
- private seed role
```

### 4.3 예상 절감 효과

실제 단가가 없으므로 token 기반 상대 비교로만 본다.

| 실험 | 목표 | 상대 절감 추정 |
|---|---|---:|
| evidence count cap 35 -> 12 이하 | top token 샘플의 evidence block 축소 | top 구간 prompt 30~45% 감소 가능 |
| average evidence tokens 1,830 -> 900~1,200 | 평균 호출 비용 축소 | 전체 `INTERROGATION` prompt 20~35% 감소 가능 |
| history turns 8~10 -> 최근 4~6 + summary | 장기 심문 비용 증가 완화 | history block 30~50% 감소 가능 |
| static context 중복 문구 축약 | 모든 호출에 누적되는 고정 비용 축소 | 전체 prompt 3~8% 감소 가능 |

위 수치는 `AI_CALL_CONTEXT` estimate 기반 추정이다. 실제 절감률은 동일 시나리오/동일 질문 세트로 v1과 v2를 비교해야 확정할 수 있다.

### 4.4 품질 리스크와 방어선

비용 절감이 추리 품질을 깨면 안 된다. 특히 현재 QA에서 30~50턴 내 후보 축소가 약하다는 문제가 있었기 때문에, context를 줄이되 “다음 비교 방향”을 잃지 않아야 한다.

필수 방어선:

```text
- 사용자가 명시적으로 제시한 증거는 항상 포함
- NPC가 알 수 없는 private solution/culprit fact는 계속 제외
- compact prompt에서도 answer shape는 인정 사실 / 모르는 범위 / 다음 비교 대상 유지
- evidence top-k에서 제외된 증거 때문에 답변이 "단정 불가"만 반복되는지 QA로 확인
- promptVersion별로 token뿐 아니라 usefulness/safety를 같이 기록
```

### 4.5 다음 실험 계획

| Step | 실험 | 성공 기준 |
|---|---|---|
| 1 | `npc_interrogation_v2_compact` shadow build | raw prompt 저장 없이 `AI_CALL_CONTEXT` block tokens만 비교 가능 |
| 2 | 동일 시나리오/유사 질문 30~50개로 v1/v2 비교 | avgTotalTokensPerCall 30% 이상 감소, failure/fallback 0 유지 |
| 3 | top token 구간 재현 | promptTokens 5,000대 샘플이 3,500 이하로 내려가는지 확인 |
| 4 | blind QA 재검 | 답변이 더 짧아져도 후보 축소에 필요한 비교 방향이 유지되는지 확인 |
| 5 | rollout | promptVersion별 dashboard에서 token/latency/safety를 같이 추적 |

## 5. 조치 계획

### 바로 할 일

1. `npc_interrogation_v2_compact` 프롬프트 버전을 만든다.
2. 증거 컨텍스트에 relevance top-k와 최대 evidence count/token budget을 적용한다.
3. 대화 이력은 최근 N턴 + 요약 방식으로 바꾼다.
4. `AI_CALL_CONTEXT`는 raw prompt 없이 block token, evidenceCount, historyTurns, promptVersion, templateHash만 남긴다.
5. DB logging이 꺼진 상태에서도 호출량/실패율/지연시간/토큰 집계를 볼 수 있는 metric 또는 로그 경로를 만든다.
6. `AI_CALL`과 `AI_CALL_CONTEXT` LogQL은 substring 충돌이 없도록 `AI_CALL `, `AI_CALL_CONTEXT `처럼 공백 포함 prefix로 분리한다.
7. 비용 절감 실험은 token 절감률만 보지 말고 blind QA의 후보 축소 성공률과 함께 본다.

### 다음 단계

1. `FINAL_DEDUCTION` 표본을 30건 이상 확보한 뒤 p95/p99 지연시간을 다시 산출한다.
2. `templateHashCount=2`가 정상 배포 차이인지, 프롬프트 템플릿 분기인지 확인한다.
3. evidence top-k 기준을 시나리오별 핵심 증거/최근 해금/사용자 질문 키워드 중심으로 설계한다.
4. suspect static context와 session dynamic context를 분리해 매 호출마다 반복되는 고정 텍스트를 줄인다.
5. LLMOps 대시보드에 호출량, 실패율, fallback율, 평균/p95 지연, 총 토큰, prompt ratio, promptVersion별 비용을 올린다.
6. compact prompt 실험은 같은 시나리오/유사 질문 묶음에서 token, latency, 정답 누설 안전성, 답변 유용성을 함께 비교한다.

### 하지 말아야 할 일

- raw prompt, AI 답변 원문, 사용자 질문 원문을 운영 집계 로그에 저장하지 않는다.
- Prometheus label에 sessionId, scenarioId, suspectId, npcCode 같은 고카디널리티 값을 넣지 않는다.
- failure가 0건이라는 이유로 token warning을 무시하지 않는다.
- 비용 문제를 모델 교체로만 해결하려 하지 않는다. 현재 병목은 컨텍스트 크기다.
- `importance=CORE`, `culpritEligible`, solution truth 같은 spoiler metadata를 relevance top-k에 사용하지 않는다.
- 단순 evidence count cap만 걸고 품질 QA 없이 배포하지 않는다.

## 6. 코드/운영 확인 지점

| 영역 | 확인할 것 |
|---|---|
| Prompt Builder | `npc_interrogation_v1`에서 증거/이력 컨텍스트가 어디서 조립되는지 확인 |
| Evidence Query | 해금 증거 전체를 싣는지, 관련도 정렬/필터가 있는지 확인 |
| History Context | 최근 대화 전체를 싣는지, 요약/턴 제한이 있는지 확인 |
| Prompt Versioning | `npc_interrogation_v2_compact` 추가와 templateHash 기록 확인 |
| Telemetry | `AI_CALL_CONTEXT`가 raw-free 집계 필드만 저장하는지 확인 |
| Metrics | DB logging off 상태에서도 AI_CALL 지표가 남는지 확인 |
| Alerts | high prompt ratio, high evidenceCount, high historyTurns, AI_CALL zero-rate, Loki/Alloy ingest freshness 알림 검토 |
| LogQL | `AI_CALL `과 `AI_CALL_CONTEXT ` 쿼리가 서로 섞이지 않는지 확인 |
| QA | compact prompt가 30~50턴 후보 축소와 정답 누설 방지에 미치는 영향 확인 |

## 7. 중복 제거 기준

- 같은 기간의 v2/v3 LLMOps 보고서는 모두 보관 가치가 있지만, 원인 분석은 context breakdown이 있는 v3를 우선한다.
- Infra health 반복 내용은 2026-06-13 최신 LLMOps v3 상태만 통합 결론에 반영했다.
- 긴 샘플 로그, stack trace, 프롬프트 원문, 사용자 질문/AI 답변 원문은 이 문서에서 제외했다.
- scale-out PoC와 직접 관련된 서버 증설/원복 절차는 별도 PoC 문서에 두고, 이 문서에는 LLMOps 판단에 필요한 운영 상태만 남겼다.

# ClueRoom LLMOps 비용·레이트리밋 계획

작성일: 2026-06-17

이 문서는 `LLMOPS_DAILY_SUMMARY_2026-06-09_TO_2026-06-13.md`의 운영 관측치를 기준으로, 현재 구조 그대로 배포했을 때 플레이 1회당 AI 호출/토큰/비용을 추정하고 계정별 레이트리밋 기준을 제안한다.

원문 프롬프트, 사용자 질문, AI 답변, 세션/용의자/정답 식별자는 포함하지 않는다.

## 0. 결론

| 항목 | 권장 기준 |
|---|---|
| 제품 목표 기준 | 30~50회 심문 안에 submit-ready 도달 후 최종 추리 1회 |
| 현재 QA 관측 기준 | 30~50회는 안정적 범인 특정 미달, submit-ready까지 서월채 약 80회, 스튜디오9 약 100회 extended interrogation 필요 |
| QA overrun / abuse 기준 | 120회 이상 심문은 비용·UX 남용 신호로 보고 강하게 최종추리로 유도한다 |
| 계정+시나리오 hard cap | 실제 AI provider 호출 150회 / day |
| 계정당 기본 daily cap | 계정+시나리오별 실제 AI provider 호출 150회 / day, 계정 전체 350회 / day |
| 계정당 기본 monthly cap | `INTERROGATION` 1,500회 / month, `FINAL_DEDUCTION` 30회 / month |
| QA 전용 계정 | 별도 role 또는 allowlist로 일반 cap보다 높게 부여하고 public report에 raw 결과를 남기지 않음 |
| 우선 최적화 대상 | `npc_interrogation_v1`의 evidence/history prompt context |
| 비용 위험 판단 | provider 실패보다 prompt token 증가와 free-form 반복 심문이 비용 위험의 핵심 |

현재 DeepSeek V4 Flash 단가 기준으로는 **목표형 30~50회 플레이는 약 20~33원**, 현재 QA에서 submit-ready까지 필요했던 **extended 80~100회 플레이는 약 53~66원** 수준으로 추정된다. 다만 이 값은 토큰 관측치와 provider 단가가 유지된다는 전제이며, 실제 청구액은 provider cache hit, 환율, 모델 변경, 재시도, 장애성 중복 호출, QA/운영 호출을 포함해 달라질 수 있다.

## 1. 기준 데이터

출처: `docs/infra/agent/LLMOPS_DAILY_SUMMARY_2026-06-09_TO_2026-06-13.md`

2026-06-13 LLMOps 기준:

| Feature | 호출 수 | 평균 input/prompt token | 평균 output token | 평균 total token | 평균 latency |
|---|---:|---:|---:|---:|---:|
| `INTERROGATION` / `npc_interrogation_v1` | 155 | 2,998 | 61 | 3,059 | 1,608ms |
| `FINAL_DEDUCTION` / `final_deduction_scoring_v1` | 2 | 596 | 256 | 852 | 3,487ms |

해석:

- 전체 비용은 `INTERROGATION`이 지배한다.
- `INTERROGATION`은 output보다 input/prompt가 압도적으로 크다.
- 2026-06-13 기준 prompt 비중은 약 97.9%다.
- `FINAL_DEDUCTION`은 호출 수가 적어 p95/p99 판단에는 부족하지만, 비용 총량 관점에서는 비중이 작다.

## 2. 단가 가정

2026-06-17 기준 계산 가정:

| 항목 | 값 |
|---|---:|
| 모델 | `deepseek-v4-flash` |
| input token 단가 | $0.14 / 1M tokens |
| output token 단가 | $0.28 / 1M tokens |
| cache hit input 단가 | $0.0028 / 1M tokens |
| 계산 방식 | cache hit을 0%로 본 보수적 cache-miss 기준 |
| 환율 | 1 USD = 1,513 KRW |

참고:

- DeepSeek 공식 문서는 V4 Flash의 cache-miss input $0.14/1M, output $0.28/1M, cache-hit input $0.0028/1M 기준을 제시한다: https://api-docs.deepseek.com/quick_start/pricing
- 같은 문서는 가격이 변동될 수 있으므로 실제 운영 예산은 최신 단가를 다시 확인하라고 안내한다.
- 환율은 2026-06-17 전후 USD/KRW 약 1,513원을 운영 예산 계산용으로 사용한다. 실제 회계/결제에는 결제 시점 카드사/은행 환율을 따른다.
- 이 장의 `cache hit`은 DeepSeek provider의 prompt cache 단가를 뜻한다. 8장의 추천 질문/답변 캐시는 ClueRoom 애플리케이션 레벨 캐시이므로 별도로 판단한다.

## 3. 플레이 1회당 AI 호출·토큰·비용

30~50회 심문은 **제품 목표**이지 현재 QA에서 안정적으로 검증된 submit-ready 기준이 아니다. 2026-06-12~15 QA에서는 30~50회 안에 submit-ready 판단이 반복 미달이었고, extended interrogation 기준으로 서월채는 약 80회, 스튜디오9는 약 100회 전후가 필요했다. 따라서 비용 계획은 목표형 30/50회와 현재 관측형 80/100회, 안전 hard cap 후보 120회를 함께 본다.

### 3.1 30~50회 기준의 출처와 재해석

`30~50회`는 LLMOps 관측값이나 실제 submit-ready 도달 평균이 아니다. 출처는 `docs/QA_OPERATING_GUIDE.md`의 fresh-session/blind QA 측정 목표와 2026-06-11~15 QA 보고서의 후보 축소 평가 기준이다.

```text
QA 목표:
  신규 유저가 과도한 반복 심문 없이 30~50턴 안에 후보를 1~2명으로 좁힐 수 있는가?

QA 결과:
  현재 시나리오/UX에서는 안정적으로 충족하지 못함.
  30~50회는 "달성된 기준"이 아니라 "미달로 확인된 제품 목표"다.
```

따라서 운영 비용 계획에서는 `30~50회`를 hard limit이나 submit-ready 보장 기준으로 사용하면 안 된다. 현재 배포 기준으로는 `80~100회`를 현실적인 플레이 비용 대표값으로 보고, 개선 후 목표는 `60~80회`, 장기 stretch goal은 `30~50회`로 재설정하는 편이 안전하다.

공식:

```text
input_tokens =
  interrogation_count * 2,998
  + final_deduction_count * 596

output_tokens =
  interrogation_count * 61
  + final_deduction_count * 256

cost_usd =
  input_tokens / 1,000,000 * 0.14
  + output_tokens / 1,000,000 * 0.28
```

| 심문 횟수 | 의미 | 총 AI_CALL | input tokens | output tokens | total tokens | 예상 비용 USD | 예상 비용 KRW |
|---:|---|---:|---:|---:|---:|---:|---:|
| 30 | 목표 하한 | 31 | 90,536 | 2,086 | 92,622 | $0.0133 | 약 20원 |
| 50 | 목표 상한, 현재 QA에서는 미달 | 51 | 150,496 | 3,306 | 153,802 | $0.0220 | 약 33원 |
| 70 | 강한 UX warning | 71 | 210,456 | 4,526 | 214,982 | $0.0307 | 약 47원 |
| 80 | 서월채 extended 관측 근사 | 81 | 240,436 | 5,136 | 245,572 | $0.0351 | 약 53원 |
| 100 | 스튜디오9 extended 관측 근사 | 101 | 300,396 | 6,356 | 306,752 | $0.0438 | 약 66원 |
| 120 | session hard cap 후보 | 121 | 360,356 | 7,576 | 367,932 | $0.0526 | 약 80원 |

판단:

- 현재 단가 기준으로 extended 플레이 1회도 절대 비용은 낮다.
- 그러나 비용은 사용자 수보다 **심문 횟수에 거의 선형으로 증가**한다.
- 따라서 “가입자 수 제한”보다 “AI 호출 수 제한”이 더 직접적인 비용 방어다.
- 50회 이하에서 안정적으로 submit-ready에 도달하지 못하는 상태에서 50회 hard cap을 걸면 UX를 망친다. 50회는 차단이 아니라 guidance/힌트 유도 기준으로만 쓴다.

## 4. 사용자 수별 비용 추정

아래 표는 사용자 1명이 공식 시나리오 1개를 **100회 심문 + 최종 추리 1회까지 진행**한다고 가정한 값이다. 이는 현재 QA에서 더 보수적인 스튜디오9 extended 관측에 맞춘 대표값이다.

| 플레이 사용자 수 | AI_CALL 수 | 총 토큰 | 예상 비용 USD | 예상 비용 KRW |
|---:|---:|---:|---:|---:|
| 100 | 10,100 | 30,675,200 | $4.38 | 약 6,632원 |
| 1,000 | 101,000 | 306,752,000 | $43.84 | 약 66,323원 |
| 10,000 | 1,010,000 | 3,067,520,000 | $438.35 | 약 663,225원 |
| 100,000 | 10,100,000 | 30,675,200,000 | $4,383.51 | 약 6,632,254원 |

목표형 30~50회, 현재 관측형 80~100회, hard cap 후보 120회를 비교하면 아래와 같다.

| 플레이 사용자 수 | 30회 목표형 | 50회 목표형 | 80회 현재 관측형 | 100회 현재 관측형 | 120회 hard cap |
|---:|---:|---:|---:|---:|---:|
| 100 | 약 2,006원 | 약 3,328원 | 약 5,311원 | 약 6,632원 | 약 7,954원 |
| 1,000 | 약 20,061원 | 약 33,279원 | 약 53,115원 | 약 66,323원 | 약 79,530원 |
| 10,000 | 약 200,611원 | 약 332,786원 | 약 531,150원 | 약 663,225원 | 약 795,299원 |
| 100,000 | 약 2,006,105원 | 약 3,327,862원 | 약 5,311,009원 | 약 6,632,254원 | 약 7,952,986원 |

주의:

- 두 공식 시나리오를 모두 최종 추리까지 진행하면 위 비용에 `x2`를 곱한다.
- QA, 운영 smoke, 재시도, provider timeout retry, 악성 반복 호출은 별도 가산해야 한다.
- cache hit이 잘 잡히면 비용은 더 내려갈 수 있지만, 예산 계획은 cache miss 기준으로 잡는다.
- `deepseek-v4-pro`로 모델을 바꾸면 같은 토큰량 기준 비용은 약 3.1배 증가한다.

## 5. 계정당 레이트리밋 제안

### 5.1 세션 단위 제한

| 제한 | 값 | 이유 | 사용자 처리 |
|---|---:|---|---|
| guidance nudge | 35 `INTERROGATION` / session | 목표형 30~50턴 구간 진입 | guidance, 힌트, 증거 비교 UI 노출을 강화 |
| soft cap | 50 `INTERROGATION` / session | 제품 목표 상한이지만 현재 QA 미달 | 차단하지 않고 “막힌 상태”로 보고 힌트/정리 화면 유도 |
| guidance nudge | 70 `INTERROGATION` / session | extended play 진입 신호 | 힌트/함께 볼 증거/추천 질문 활용 유도 |
| final deduction checklist | 100 `INTERROGATION` / session | 현재 QA submit-ready baseline 도달 구간 | 범인·동기·수단·은폐 정황 정리 유도 |
| strong warning | 120 `INTERROGATION` / session | 비용·UX overrun 신호 | 추가 심문보다 최종 추리 CTA를 강하게 노출 |
| hard cap | 150 actual AI calls / account+scenario / day | 하루에 공식 시나리오 하나를 충분히 플레이하되 반복 호출 abuse 방어 | 추가 AI 호출 차단, 증거/힌트/최종추리 화면으로 유도 |
| final submit cap | 3 `FINAL_DEDUCTION` / session | 찍기 제출/반복 채점 방어 | 제출 전 근거 completeness check 유도 |

150회 hard cap 전까지는 조용히 허용하지 않고 35/50/70/100/120 threshold에서 정리, 힌트, 추천 질문, 최종 추리로 유도한다. 현재 QA 기준으로 50회 hard stop은 금지한다.

### 5.2 계정 단위 제한

MVP 무료 계정 기본값:

| 제한 | 값 |
|---|---:|
| `INTERROGATION` burst | 8회 / minute |
| `INTERROGATION` hourly | 60회 / hour |
| AI provider call daily per scenario | 150회 / day |
| AI provider call daily total | 350회 / day |
| `INTERROGATION` monthly | 1,500회 / month |
| `FINAL_DEDUCTION` daily | 별도 분리 전까지 account daily cap에 합산 |
| `FINAL_DEDUCTION` monthly | 30회 / month |
| concurrent AI call | 1~2회 / account |

현재 구현 baseline은 feature별 분리 전 단계이므로 `INTERROGATION`, `FINAL_DEDUCTION`, `SCENARIO_VALIDATION`의 실제 provider 호출을 계정+시나리오별 150회/day로 합산한다. 계정 전체 daily cap은 350회/day로 두어 한 계정이 하루에 서월채와 스튜디오9를 모두 길게 플레이할 수 있게 한다.

1,500회/month + final deduction 30회는 보수적으로 약 4.6M tokens, 약 $0.66, 약 1,000원 수준이다. 현재 QA 기준 100회 심문형 플레이로 약 15회까지 허용하는 값이라, per-scenario `daily 150회`와 충돌하지 않으면서도 월 단위 남용을 막는 후보로 볼 수 있다.

QA/운영 계정:

| 계정 유형 | 제안 |
|---|---|
| QA 전용 계정 | `INTERROGATION` 500~2,000회/day, public report redaction 필수 |
| 관리자 계정 | 일반 cap 우회 가능하되 모든 AI_CALL에 owner/purpose tag 기록 |
| 공유 mock 계정 | 운영 public API에서는 사용 지양. 사용 시 시간대 조율과 세션 abandon 주의 |

### 5.3 IP / 디바이스 보조 제한

인증 계정 기준이 1차 방어다. IP 기반 제한은 NAT/학교/회사망에서 정상 사용자를 같이 막을 수 있으므로 보조 신호로 둔다.

| 제한 | 값 |
|---|---:|
| same IP AI calls | 300회 / 10분 warning |
| same IP hard cap | 1,000회 / 1시간 |
| same deviceId AI calls | 150회 / day |
| anonymous/public path | AI 호출 없음. 시나리오 목록/상세만 허용 |

## 6. 구현 방식

레이트리밋은 provider 호출 직전에 걸어야 한다. 호출 후에 비용을 계산하는 방식은 예산 방어가 아니다.

권장 Redis key:

```text
ai:limit:user:{userId}:minute:{yyyyMMddHHmm}
ai:limit:user:{userId}:hour:{yyyyMMddHH}
ai:rate:daily:{yyyyMMdd}:user:{userId}:scenario:{scenarioId}
ai:rate:daily:{yyyyMMdd}:user:{userId}:total
ai:limit:user:{userId}:month:{yyyyMM}
ai:limit:session:{sessionId}:interrogation
ai:limit:session:{sessionId}:final-deduction
ai:limit:ip:{ipHash}:hour:{yyyyMMddHH}
ai:limit:device:{deviceHash}:day:{yyyyMMdd}
```

로그/메트릭에 raw email, raw IP, raw deviceId는 남기지 않는다. 필요하면 HMAC hash 또는 내부 numeric id만 쓴다.

응답 정책:

| 상황 | HTTP | 에러 코드 후보 | 메시지 방향 |
|---|---:|---|---|
| session hard cap | 429 | `AI_RATE_001` | 심문량이 많아 품질/비용 보호를 위해 제한. 힌트/증거 정리/최종 추리 유도 |
| account daily cap | 429 | `AI_RATE_002` | 오늘 사용량 초과. 다음 날 또는 QA 계정 사용 안내 |
| rate-limit state unavailable | 503 | `AI_RATE_003` | Redis 장애 등으로 quota 상태를 확인할 수 없어 비용 방어를 위해 일시 제한 |

## 7. 글로벌 예산 경보

계정 제한과 별개로 전체 운영 예산 경보가 필요하다.

초기 후보:

| Alert | Window | Warning | Critical |
|---|---:|---:|---:|
| AI_CALL count spike | 60m | baseline x3 | baseline x5 |
| token total spike | 60m | 300k tokens | 1M tokens |
| projected daily cost | 60m extrapolated | $20/day | $50/day |
| daily actual cost | 24h | $50/day | $100/day |
| per-account AI_CALL spike | 10m | 30 calls | 60 calls |
| prompt ratio high | 24h | >98% 유지 | >99% 또는 top-k regression |
| provider failure | 5m | failure >= 3 | failure >= 10 |
| fallback spike | 5m | fallback >= 3 | fallback >= 10 |

100,000명이 한 달에 1개 시나리오를 100회 심문으로 최종 추리까지 진행하면 월 약 $4,384, 일평균 약 $146다. 따라서 MVP 기간에는 `$20/day`가 warning, `$50/day`가 critical에 가까울 수 있지만, 실제 사용자 규모가 커지면 threshold를 사용자 수와 매출 기준으로 다시 잡아야 한다.

## 8. 비용·UX 개선 로드맵

비용을 줄이는 가장 좋은 방법은 사용자를 막는 것이 아니라 `calls-to-submit-ready`를 줄이는 것이다. 현재 문제는 단순히 AI 호출이 비싼 것이 아니라, 플레이어가 어떤 증거를 읽고 누구에게 무엇을 물어야 할지 몰라 반복 질문을 하면서 비용과 UX가 같이 나빠지는 점이다.

### 8.0 개선 목표 재정의

| 단계 | 목표 calls-to-submit-ready | 해석 |
|---|---:|---|
| 현재 관측 | 80~100회 | extended interrogation 후에야 submit-ready |
| 1차 개선 목표 | 60~80회 | guidance/추천 질문/증거 비교 UX로 반복 질문 감소 |
| 2차 개선 목표 | 50~60회 | 핵심 증거 비교가 앱 안에서 자연스럽게 이어짐 |
| 장기 stretch goal | 30~50회 | 시나리오 난이도·힌트 UX까지 재설계해야 가능 |

`30~50회`를 당장 hard target으로 두면 설계가 왜곡된다. 현재는 먼저 80~100회를 60~80회로 낮추는 것이 현실적이다.

### 8.1 먼저 계측해야 할 지표

아래 지표 없이 바로 캐시나 레이트리밋부터 걸면, 비용은 줄어도 게임이 더 딱딱해질 수 있다.

| 지표 | 의미 |
|---|---|
| `calls_to_first_guidance_click` | 사용자가 추천 질문/함께 볼 증거를 언제부터 쓰는지 |
| `guided_question_ratio` | 전체 심문 중 추천 질문 기반 비율 |
| `calls_to_submit_ready` | 최종 제출 근거가 갖춰졌다고 판단한 시점까지 AI_CALL 수 |
| `dead_end_turns` | 같은 후보/같은 증거 축에서 반복 질문한 횟수 |
| `repeat_question_rate` | 동일/유사 질문 반복률 |
| `hint_after_ai_count` | AI 심문 후 힌트를 찾은 횟수 |
| `abandon_after_ai_count` | AI 반복 심문 후 이탈한 세션 수 |
| `guided_question_success_rate` | 추천 질문 클릭 후 후보 축소/증거 해금/다음 행동으로 이어진 비율 |

이 지표는 raw question/answer를 저장하지 않고도 집계할 수 있어야 한다. 질문 원문 대신 `questionSource=GUIDANCE|FREE|STATIC_CHIP`, `suggestedQuestionId`, `evidenceId`, `suspectId`, `sessionTurnIndex`, `unlockedEvidenceCount` 같은 구조화 필드만 남긴다.

### 8.2 Guidance로 AI 호출을 줄이는 방향

대부분의 유저가 추천 질문을 누를 가능성이 높다면, 비용 절감의 1차 지점은 "추천 질문 자체"가 아니라 **추천 질문 전후의 비-AI 안내**다.

| 개선 | AI 호출 여부 | 기대 효과 | 주의점 |
|---|---|---|---|
| 증거 상세 readingPoints 강화 | 없음 | "무엇을 봐야 하는지"를 AI 없이 안내 | 정답 경로처럼 보이면 blind성 훼손 |
| 함께 볼 증거 compareEvidences 강화 | 없음 | 사용자가 비교할 증거를 찾는 반복 질문 감소 | locked evidence code/name 노출 금지 |
| 추천 질문 prefill-only | 없음 | 질문 작성 부담 감소, 자동 전송 방지 | draft overwrite confirm 필요 |
| 추천 질문 클릭 후 "왜 이 질문을 추천하는지" 한 줄 표시 | 없음 | 맥락 이해 향상 | 정답성 힌트가 되면 안 됨 |
| 50/70턴 도달 시 증거 정리 화면 유도 | 없음 | 막힌 상태에서 free-form 반복 질문 감소 | 강제 이동은 피하고 nudge로 처리 |

즉, AI 답변을 캐시하기 전에 **AI를 부르지 않아도 되는 guidance 표면**을 먼저 늘려야 한다.

### 8.3 추천 질문 답변 캐싱 검토

추천 질문은 많은 유저가 누를 가능성이 높아 캐싱 후보처럼 보인다. 하지만 "추천 질문이면 정해진 문구를 그대로 반환"은 품질 리스크가 크다.

문제:

```text
같은 추천 질문이어도 답변은 아래 상태에 따라 달라질 수 있다.

- 현재 해금된 증거
- 사용자가 이번 질문에 제시한 증거
- 해당 용의자에게 이전에 어떤 모순을 지적했는지
- ResponsePolicyResolver가 선택한 policyKey
- 심문 대상 용의자의 공개 알리바이/진술
- scenario content hash / seed version
- 직전 대화에서 이미 인정·부인한 사실
```

따라서 단순 key는 금지한다.

```text
금지:
cacheKey = scenarioId + suspectId + suggestedQuestionId
cacheKey = questionText
```

이렇게 하면 의심도/모순 진행 상태가 반영되지 않아, 플레이가 "정해진 대사 클릭"처럼 느껴지고 NPC 반응 품질이 떨어진다.

허용 가능한 캐싱 수준:

| 방식 | 추천도 | 설명 |
|---|---|---|
| 최종 AI 답변을 questionText만으로 캐싱 | 금지 | 상태 차이를 무시해 품질과 추리 흐름을 망침 |
| 최종 AI 답변을 상태 해시까지 포함해 캐싱 | 제한적 허용 | 완전히 같은 상태에서 같은 질문이면 가능하지만 hit rate가 낮을 수 있음 |
| prompt context 조립 결과 캐싱 | 권장 | evidence/history block 계산 비용과 지연을 줄이고 답변은 AI가 생성 |
| ResponsePolicyResolver 결과 캐싱 | 권장 | 같은 unlock/presented evidence 상태에서 policy 계산 반복 감소 |
| 반복 질문 dedupe | 권장 | 같은 상태에서 같은 질문을 다시 물으면 "이미 확인한 답변"을 재사용 또는 요약 |
| guidance/read-only 설명 캐싱 | 강력 권장 | 증거 판독 포인트/추천 질문 이유는 deterministic content로 제공 |

최종 AI 답변 캐시를 쓰려면 최소 아래 key를 포함해야 한다.

```text
scenarioId
scenarioContentHash
sessionId 또는 sessionStateHash
suspectId
questionType
suggestedQuestionId 또는 normalizedQuestionHash
presentedEvidenceId
unlockedEvidenceSetHash
recentConversationSummaryHash
responsePolicyKey
policyVersion
contradictionStage 또는 contradictionStateHash
promptTemplateHash
model
```

`suspicionLevel` 같은 플레이어-facing 숫자는 캐시 key로 쓰지 않는다. 이 값은 과거 스포일러성/고정값 문제를 만든 축이고, public DTO에서 제거된 방향과도 충돌한다. 대신 백엔드 내부의 `responsePolicyKey`, `contradictionStateHash`, `unlockedEvidenceSetHash`처럼 답변 가능 범위를 실제로 결정하는 상태를 key로 사용한다.

### 8.4 추천 질문 캐시 적용 단계

| 단계 | 적용 | 목적 | 위험 |
|---|---|---|---|
| 1 | 추천 질문 자체는 계속 prefill-only | 자동 AI 호출 방지 | 없음 |
| 2 | guidance/read-only 설명 deterministic 제공 | AI 없이 다음 행동 안내 | 과도한 answer-leading 주의 |
| 3 | 같은 상태의 완전 동일 질문은 "이전에 확인한 내용" 재사용 | 반복 질문 비용 감소 | stale state 방지 필요 |
| 4 | prompt context / policy result 캐싱 | AI 품질 유지하면서 지연·CPU 비용 감소 | token 비용 자체는 크게 줄지 않을 수 있음 |
| 5 | 상태 해시 기반 최종 답변 캐시 A/B 테스트 | 추천 질문 대량 사용 시 provider 비용 절감 | 품질 저하, 딱딱한 대사화 위험 |

5단계는 바로 운영 기본값으로 켜지 않는다. QA 전용 계정 또는 shadow mode에서 `cacheHit`, `answerUsefulness`, `unsupportedFact`, `calls-to-submit-ready`, `user repeat rate`를 비교한 뒤 결정한다.

### 8.5 캐시 품질 가드레일

최종 답변 캐시를 실험할 때는 아래 조건을 지킨다.

```text
1. scenarioContentHash가 바뀌면 캐시 무효화
2. unlockedEvidenceSetHash가 바뀌면 캐시 무효화
3. responsePolicyKey가 바뀌면 캐시 무효화
4. contradictionStateHash가 바뀌면 캐시 무효화
5. final deduction/scoring은 캐시하지 않음
6. AI 답변 원문을 장기 저장하지 않고 TTL을 짧게 둠
7. cache hit 응답도 AI_CALL 또는 AI_CACHE_HIT telemetry로 집계
8. cache hit이 늘어도 calls-to-submit-ready가 악화되면 rollback
```

캐시를 "답변 품질 개선"으로 보면 안 된다. 캐시는 비용과 지연을 줄이는 장치이고, 품질은 guidance/seed/prompt policy 개선으로 해결해야 한다.

### P0. evidence top-k / token budget

현재 평균 evidence context는 `INTERROGATION` prompt의 가장 큰 블록이다. 모든 해금 증거를 넓게 넣는 방식은 플레이 후반에 비용을 키운다.

금지:

```text
- importance=CORE/FAKE
- culpritEligible
- solution/variant truth
- private seed role
```

허용 public-safe signal:

```text
- user-presented evidence
- current suspect related evidence
- location/time overlap
- recent unlock
- question keyword match
- previous conversation reference
```

### P1. history compaction

최근 N턴 + 요약 구조로 바꾼다. 질문/답변 원문을 장기 저장하거나 LLMOps report에 싣지 않는다.

요약은 아래처럼 역할을 나눈다.

```text
최근 4~6턴:
  실제 대화 흐름 유지용

세션 요약:
  이미 확인한 공개 사실, 인정/부인 범위, 다음 비교 대상

금지:
  범인/solution/private seed 기반 요약
```

### P1. repeated question dedupe

같은 세션에서 같은 용의자에게 동일 또는 거의 동일한 질문을 반복하면 provider 호출 전 캐시/요약 응답을 검토한다. 다만 캐시 응답이 사건 상태 변경 이후 stale해지지 않도록 unlock state hash를 key에 포함한다.

### P2. model/prompt A/B

`npc_interrogation_v2_compact`를 만들고 아래 지표로 v1과 비교한다.

| 지표 | 목표 |
|---|---|
| avg prompt tokens | v1 대비 25~40% 감소 |
| answer usefulness | blind QA에서 후보 축소 성능 유지 또는 개선 |
| unsupported fact | 증가 없음 |
| spoiler leakage | 0건 유지 |
| latency p95 | 감소 또는 유지 |

## 9. 단계별 대응 계획

### Phase 0. 문서 기준 정정과 계측 준비

목표:

```text
- 30~50회를 "관측 submit-ready 기준"이 아니라 "미달로 확인된 제품 목표"로 재정의
- 현재 baseline은 80~100회로 기록
- 50회 hard stop 금지
```

작업:

```text
- QA 문서와 LLMOps 문서에서 30~50 표현을 목표/관측으로 분리
- AI_CALL에 questionSource, suggestedQuestionId, presentedEvidenceId, turnIndex 추가 검토
- calls-to-submit-ready, calls-to-result-path, repeat_question_rate 대시보드 후보 정의
```

### Phase 1. AI 호출 없이 줄일 수 있는 반복부터 제거

목표:

```text
- 사용자가 무엇을 읽고 비교해야 할지 몰라 묻는 질문을 줄임
- AI 호출 수를 줄이되 답변 품질은 건드리지 않음
```

작업:

```text
- 핵심 증거 guidance 보강
- suggested question reason / compare evidence 설명 추가
- 35/50/70/100/120턴 nudge 문구 설계
- 최종 추리 준비도 checklist 추가
```

성공 기준:

```text
- guided_question_ratio 증가
- repeat_question_rate 감소
- calls-to-submit-ready 80~100 -> 60~80 근접
```

### Phase 2. 프롬프트 비용 절감

목표:

```text
- AI 호출 수를 강제로 줄이지 않고, 호출당 token을 줄임
```

작업:

```text
- evidence top-k
- evidence token budget
- history compaction
- policy/static context 축약
```

성공 기준:

```text
- avg prompt tokens v1 대비 25~40% 감소
- unsupported fact 증가 없음
- spoiler leakage 0건 유지
- calls-to-submit-ready 악화 없음
```

### Phase 3. 안전한 캐시부터 적용

목표:

```text
- 추천 질문/반복 질문 사용량이 많을 때 비용을 줄이되, NPC 반응 품질을 보존
```

작업:

```text
- guidance/read-only content 캐시
- ResponsePolicyResolver 결과 캐시
- prompt context block 캐시
- exact repeated question dedupe
```

성공 기준:

```text
- cache hit이 늘어도 user retry/repeat이 증가하지 않음
- cache hit 응답 후 abandon 증가 없음
- 같은 질문 반복 비용 감소
```

### Phase 4. 최종 답변 캐시 A/B 테스트

목표:

```text
- 추천 질문이 대량으로 눌리는 구간에서 provider 비용을 추가 절감할 수 있는지 검증
```

조건:

```text
- 상태 해시 기반 key 사용
- TTL 짧게 운영
- QA/staging 또는 shadow mode 우선
- cache hit/AI fresh answer를 blind QA로 비교
```

중단 기준:

```text
- 답변이 딱딱한 고정 대사처럼 느껴짐
- contradiction 흐름을 반영하지 못함
- calls-to-submit-ready가 악화됨
- unsupported fact 또는 spoiler risk가 증가함
```

## 10. LLMOps가 추가로 봐야 할 것

비용 표만으로는 부족하다. LLMOps는 아래를 계속 봐야 한다.

| 범주 | 지표 |
|---|---|
| 비용 | cost/session, cost/submit-ready, cost/result-path, cost/account/day, token/call, token/block |
| 게임성 | calls-to-submit-ready, calls-to-result-path, abandon-after-ai, dead-end turns, hint-after-ai, final submit readiness |
| 안전 | prompt forbidden fact scan, answer unsupported fact sample review, spoiler metadata regression |
| 품질 | answer length, refusal loop rate, next-action usefulness, scenario timeline consistency |
| 운영 | provider failure/fallback, retry count, timeout, p95/p99 latency |
| 드리프트 | promptVersion, templateHash, model, response policy version |
| 프라이버시 | raw prompt/question/answer 미저장, user/session id redaction, report public/private boundary |
| 청구 대조 | internal AI_CALL tokens vs provider invoice 차이 |

특히 `calls-to-submit-ready`는 QA와 비용을 동시에 설명하는 핵심 지표다. guidance 개선 후 `calls-to-submit-ready`가 내려가면 UX 개선과 비용 절감이 같이 일어난다. `calls-to-result-path`는 최종 제출 API와 결과 화면 진입까지의 비용을 보는 별도 지표로 둔다.

## 11. 다음 작업

| 우선순위 | 작업 |
|---|---|
| P0 | Redis 기반 계정+시나리오별 AI provider call rate limit 구현 |
| P0 | 35/50/70/100/120 threshold를 심문 응답 `aiQuota`로 노출 |
| P0 | AI_CALL에 `estimatedCostUsd`, `inputTokens`, `outputTokens`, `promptVersion`, `model`을 안정적으로 남김 |
| P0 | 30~50 목표 미달을 기준으로 calls-to-submit-ready baseline을 80~100회로 기록 |
| P1 | n8n LLMOps daily report에 cost/session, cost/account percentile 추가 |
| P1 | `npc_interrogation_v2_compact` 설계와 blind QA 비교 |
| P1 | guidance/read-only content와 ResponsePolicyResolver 결과 캐시부터 검토 |
| P1 | provider invoice와 내부 token 집계 대조 절차 추가 |
| P2 | scenario별 calls-to-submit-ready / calls-to-result-path / token-to-result-path 대시보드 추가 |
| P2 | abuse user/device/IP hash top-N report 추가 |
| P2 | 상태 해시 기반 최종 답변 캐시는 QA/staging A/B 이후에만 운영 검토 |

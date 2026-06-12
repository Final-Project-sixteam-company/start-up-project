# ClueRoom Blind Retest Report - 2026-06-11

## 0. Scope

- Tester: Codex API-only retest agent
- Gameplay knowledge state before final submission: no private seed/solution/culprit/variant truth/prior QA opened
- API: `https://api.clueroom.xyz`
- Device: not available in this terminal
- App build: not verified
- Backend commit/version: not verified
- Constraint: initial gameplay was API-only; later frontend E2E follow-up confirmed Android chip auto-send behavior
- API-only spoiler metadata masking audit: not recorded; exposed metadata was observed during spoiler-leak verification
- Candidate narrowing blind validity: blind invalid for API-only 30~50 turn measurement; use as diagnostic only until Android or masked-runner retest

## 1. Spoiler Safety Declaration

아래 자료를 보지 않았다.

- private seed
- solution/culprit/variant truth
- prior QA result
- DB direct query
- result API before final submission

초기 플레이와 최종 제출 전까지는 위 자료를 보지 않았다. 사용자 요청 이후에는 2026-06-10 QA 문서를 사후 비교 목적으로만 참고했고, result API도 최종 제출 이후 사용자 확인 요청으로만 호출했다.

## 2. Final Judgment

```text
전체 판단: PARTIAL for flow/guidance, but API-only candidate narrowing is blind invalid
가장 큰 blocker: 10일 P0 API 스포일러 메타데이터가 지속되고, guidance chip 자동 전송과 guidance/prompt 약점 때문에 30~50회 내 최종 후보 확정이 어렵다.
최종 제출 여부: 50턴 기준 두 시나리오 모두 보류. user 요청 후 extended retest에서 두 시나리오 모두 제출 완료.
30~50회 심문 내 후보 축소 가능성: API-only diagnostic으로는 어려움이 보였지만, 마스킹 감사 기록이 없어 blind-valid 근거로 쓰지 않는다.
후보 축소 blind validity: INVALID. Android 또는 spoiler metadata 마스킹 runner로 재측정 필요.
guidance가 추리 보조인지 정답 경로 고정인지: 정답 경로 고정은 아니지만, 많은 증거에서 guidance가 없어 보조력이 약함.
```

## 3. Findings First

| Priority | Scenario | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P0 | Cross-scenario | 2026-06-10 P0 스포일러성 API 메타데이터가 2026-06-11에도 지속됨 | 현재 운영 응답에서 evidence `importance=CORE/FAKE`, suspect `culpritEligible`, 역할성 asset path가 확인됨 | 플레이어용 API에는 정답성/레드헤링/비후보 판정 메타데이터가 없어야 함 | API-only 사용자나 네트워크 로그 사용자는 핵심/가짜 증거와 배제 후보를 추정 가능 | 추리게임 핵심 경험과 blind QA 신뢰성을 직접 훼손함 | public DTO에서 `importance`, `culpritEligible`, 역할성 asset path 제거. admin/debug DTO와 분리 |
| P1 | Cross-scenario | API-only diagnostic 기준 50턴 안에 최종 후보를 안전하게 확정하지 못함 | fresh/API-only 기준 두 시나리오 모두 50턴 진행 후 최종 제출 보류. 단, runner-side spoiler metadata masking 기록이 없어 blind validity는 invalid | guidance와 심문만으로 1~2명까지 후보 축소 | 일부 축소는 됐지만, 수단/기회/은폐를 한 명에게 묶는 근거가 부족 | 신규 유저가 찍기 제출을 하거나 중도 이탈할 가능성. 다만 이 행은 blind-valid 판정이 아니라 diagnostic UX signal임 | 핵심 증거별 reading/compare/question 확장 후 Android 또는 masked API runner로 blind 재측정 |
| P1 | Cross-scenario | 10일에 지적된 AI 회피 답변 문제가 11일에도 재현됨 | `단정할 수 없다`, `추가 증거 필요`, `기록을 함께 봐야 한다` 반복 | 증거 제시 시 인정 가능한 사실과 다음 비교 대상을 제공 | 답변이 안전하지만 후보 귀속을 충분히 돕지 못함 | 사용자가 잘못된 후보로 확신할 수 있음 | prompt responseShape와 policy allowedFacts에 `인정 사실/부인 범위/다음 비교 대상` 강제 |
| P1 | Cross-scenario | Android suggested-question chip이 prefill-only가 아니라 즉시 AI 호출을 수행함 | Frontend E2E follow-up에서 guidance chip tap 시 자동 심문 호출 확인 | chip tap은 심문 화면 이동과 입력창 prefill까지만 수행 | 사용자가 전송 전 질문 수정/취소할 기회 없이 AI 호출됨 | 원치 않는 심문 로그가 생성되고 QA prompt의 P1 기준을 위반함 | Android chip handler를 navigate/prefill 전용으로 바꾸고 send는 전송 버튼 클릭에만 연결 |
| P2 | Cross-scenario | 증거 해금 이유 UX는 API-only retest에서 미확인 | 서월채 7->13->20->25, 스튜디오9 8->14->29->35로 count 급증만 확인. Android의 신규 해금 표시/이유 UX는 미검증 | 시간/질문/증거제시 중 무엇으로 열렸는지 앱에서 확인 가능 | API-only 기준 count 변화만 관찰했고 Android reason/recent-unlock feedback은 확인하지 못함 | 실제 앱에도 이유 표시가 없으면 “내가 잘해서 열린 건지 기다려서 열린 건지” 진행감이 약할 수 있음 | Android E2E에서 newly unlocked evidence feedback을 먼저 확인. 미노출이 재현되면 unlock reason 이벤트, 최근 해금 내역, toast/snackbar 계약 추가 |
| P1 | Cross-scenario | 핵심 이미지/기록형 증거의 판독값 텍스트화가 여전히 부족함 | 여러 증거가 “함께 봐야 한다”만 말하고 작은 이미지/기록 판독을 별도 구조로 제공하지 않음 | 관찰 정보/판독 결과/비교 대상/물어볼 대상 분리 | description에 섞여 있거나 일부 증거에는 없음 | 모바일에서 핵심 단서 판독 실패 가능성 지속 | evidence detail에 `readingPoints`를 전 증거로 확대하고 판독값을 텍스트 필드로 분리 |
| P2 | Cross-scenario | API-only 후보 축소 평가의 blind 유효성 기록이 누락됨 | 리포트는 API-only retest와 spoiler metadata 노출을 모두 기록하지만, 후보 축소 전 runner-side masking 또는 수동 무시 절차를 기록하지 않음 | API-only 후보 축소 전 `importance`, `culpritEligible`, suspicion/candidate metadata, 역할성 asset path를 숨긴 방식이 남아야 함 | 감사 가능한 masking record가 없음 | 후보 축소 판정을 blind QA 정본으로 사용할 수 없음 | 이 리포트의 API-only candidate narrowing은 blind invalid로 표기하고, Android 또는 masked runner로 재검 |
| P2 | 서월채 | guidance coverage가 초기와 중후반 증거에서 부족함 | 초기 7개 중 2개만 guidance 존재, 5분/10분 해금 증거 대부분 `guidance: null` | 최소 3개 이상 해금 증거에서 읽을 점, 비교 대상, 질문 방향 제공 | 일부 핵심 증거와 조건 해금 증거에만 guidance 존재 | 질문 설계가 증거 설명문과 QA tester 추론에 의존함 | 시간 해금 증거에도 guidance를 균등 적용 |
| P2 | 스튜디오9 | 초기/5분 구간 guidance가 전무하고 10분 후에도 낮음 | 초기 8개 0개, 5분 14개 0개, 10분 29개 중 2개만 guidance 존재 | 초반부터 읽을 점과 비교 방향을 제공 | 10분 전까지 추천 질문 chip을 확인할 수 없음 | 초반 플레이어가 무엇을 물어야 할지 알기 어렵다 | 초기 CORE 증거와 5분 해금 증거에 guidance 우선 추가 |
| P2 | 서월채 | 일부 AI 답변이 공개 타임라인과 충돌하는 시간 표현을 생성함 | 한 보안 계층 답변에서 공개 시간대와 다른 “밤 10시부터 11시 사이” 표현 후 재질문에서 다른 시간대로 변경 | 설정에 없는 시간/장소를 만들지 않음 | 정확한 기준 시각이 흔들림 | 플레이어가 잘못된 시간축으로 추리할 수 있음 | ResponsePolicy 또는 prompt context에 공개 타임라인 기준 준수 문구 강화 |
| P2 | Cross-scenario | active session 복구 UX는 10일 이슈가 해결됐다고 볼 수 없음 | 11일 API-only에서도 스튜디오9는 기존 active session 때문에 신규 생성 409 발생, abandon 후 fresh 생성 | 사용자는 이어하기/포기 후 새 시작을 명확히 선택 | API는 동작하지만 Android UX는 미검증 | 실제 앱에서 10일과 같은 진입 차단이 재발할 수 있음 | Android E2E로 P002 modal/이어하기/포기 후 새 시작 재검증 |
| P3 | Cross-scenario | suggestedQuestions payload에 내부 캐릭터 코드가 포함됨 | guidance가 있는 증거의 suggestedQuestions에서 내부 character code 필드 확인 | public response에는 UI에 필요한 식별자만 노출 | target suspect id/name 외 내부 코드도 내려옴 | UI가 실수로 표시하면 내부 식별자 노출 가능 | API에서 제거하거나 Android에서 절대 렌더링하지 않도록 contract 명시 |

## 4. Summary

| Scenario | Basic Flow | Guidance UX | Interrogation Quality | Candidate Narrowing | Final Submit | Overall |
|---|---|---|---|---|---|---|
| 서월채 | PASS | PARTIAL | PARTIAL/PASS | diagnostic PARTIAL / blind invalid | 50턴 미완료 / extended 완료 | PARTIAL |
| 스튜디오9 | PASS | FAIL/PARTIAL | PARTIAL/PASS | diagnostic PARTIAL / blind invalid | 50턴 미완료 / extended 완료 | PARTIAL |

## 5. Scenario A: 서월채

### 5.1 Flow Result

```text
sessionId: <redacted>
basic flow: scenarios/detail/create/dashboard/locations/evidences/suspects/timeline/interrogations/unlock PASS
final submit: 50턴 기준 not submitted / extended retest에서 submitted
result screen: initial retest not called / post-submit user check called result API
```

### 5.2 Guidance UX

```text
readingPoints: 일부 초기 증거와 조건 해금 증거에서 확인
compareEvidences: guidance가 있는 증거에서는 비교 방향 제공
suggestedQuestions: guidance가 있는 증거에서는 EVIDENCE_PRESENTED + presentedEvidenceId 형태 확인
locked compare masking: locked compare evidenceCode 노출 없음
chip prefill-only: 초기 API-only 단계에서는 미확인, Frontend E2E follow-up에서 자동 전송 재현(P1)
draft override policy: chip 자동 전송 때문에 전송 전 수정/override 통제 검증 불가
```

### 5.3 Interrogation / Deduction Path

```text
turns used: 50
candidate narrowing: API-only diagnostic 기준 넓음 -> 여러 증거 축 비교 -> 2~3명 수준
blind validity: INVALID. runner-side spoiler metadata masking 또는 수동 무시 절차 기록 없음
blocked moments: 15분 비교 증거 전에는 수단/기회/은폐를 한 명에게 묶기 어려움
red herring handling: 일부 동기 증거는 직접 방법 증거가 아님을 분리 가능
rejected-candidate rationale detail: public report 생략, private note 분리
remaining doubt: public-safe 범주 요약만 기록. 상세 근거는 private note 분리
10-turn summaries: not captured in public 10-turn format; this is a report-format gap and the aggregate narrowing above is diagnostic only
```

### 5.4 Scenario Notes

```text
What worked: AI는 대체로 1~2문장, 정답 직접 누설 없음, 증거를 제시하면 비교할 기록을 안내함.
What did not work: 많은 시간 해금 증거에서 guidance가 null이라 질문 설계가 어렵다.
AI answer quality: 전반적으로 도움됨. 일부 시간대 표현은 재질문 필요.
UX friction: API-only diagnostic 기준 50턴 내 최종 제출 기준을 충족하지 못함.
```

## 6. Scenario B: 스튜디오9

### 6.1 Flow Result

```text
sessionId: <redacted>
basic flow: active recovery/create/dashboard/locations/evidences/suspects/timeline/interrogations/unlock PASS
active session: 기존 active session이 있어 abandon 후 fresh session 생성
final submit: 50턴 기준 not submitted / extended retest에서 submitted
result screen: initial retest not called / post-submit user check called result API
```

### 6.2 Guidance UX

```text
readingPoints: 10분 해금 후 일부 증거에서만 확인
compareEvidences: guidance가 있는 증거에서는 제공
suggestedQuestions: guidance가 있는 증거에서는 EVIDENCE_PRESENTED + presentedEvidenceId 형태 확인
locked compare masking: locked compare evidenceCode 노출 없음
chip prefill-only: 초기 API-only 단계에서는 미확인, Frontend E2E follow-up에서 자동 전송 재현(P1)
draft override policy: chip 자동 전송 때문에 전송 전 수정/override 통제 검증 불가
```

### 6.3 Interrogation / Deduction Path

```text
turns used: 50
candidate narrowing: API-only diagnostic 기준 넓음 -> 여러 공개 단서 축 비교 -> 3명 이상 상위 후보
blind validity: INVALID. runner-side spoiler metadata masking 또는 수동 무시 절차 기록 없음
blocked moments: 10분 전까지 guidance가 없어 초반 질문 방향이 약함
red herring handling: 일부 비최종 후보군 반박은 private note 분리
rejected-candidate rationale detail: public report 생략, private note 분리
remaining doubt: public-safe 범주 요약만 기록. 상세 근거는 private note 분리
10-turn summaries: not captured in public 10-turn format; this is a report-format gap and the aggregate narrowing above is diagnostic only
```

### 6.4 Scenario Notes

```text
What worked: AI가 정답을 직접 말하지 않고, 증거 단독 확정을 피하며 비교 방향을 제시함.
What did not work: 초기/5분 증거 guidance가 전혀 없어 신규 유저가 질문을 만들기 어렵다.
AI answer quality: 대체로 1~2문장. 회피가 있지만 비교 증거 요청 형태라 큰 누설은 없음.
UX friction: fresh session은 active session abandon 후에야 생성 가능했다.
```

## 7. Cross-Scenario Findings

```text
What improved: locked evidence detail masking은 대체로 안전함. AI 답변은 직접 정답 누설 없이 짧게 유지됨.
What still blocks users: 10일 P0 API 스포일러 메타데이터 지속, guidance coverage 부족, Android chip 자동 전송, API-only diagnostic 기준 50턴 내 최종 확정 어려움.
Whether guidance feels like a clue-reading aid or answer railroading: railroading은 아님. 오히려 부족한 쪽.
Whether 30~50 interrogation target is realistic: API-only diagnostic 기준으로는 최종 제출 기준 충족이 어려웠다. blind-valid 평가는 Android 또는 masked runner 재검이 필요하다.
```

## 8. Good Points

```text
Backend: health, scenario list/detail, fresh session, active recovery, evidence unlock, interrogation API 정상.
Android: Frontend E2E follow-up에서 guidance chip 자동 전송 등 핵심 blocker를 확인했다.
Scenario seed: 증거 설명문 자체는 레드헤링과 비교 방향을 일부 제공함.
AI behavior: 정답/범인 직접 누설 없음, 대부분 1~2문장 유지, 증거 단독 확정을 피함.
```

## 9. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Backend | P0 | public play API에서 `importance`, `culpritEligible`, 역할성 asset path 노출 제거 |
| Scenario seed | P1 | 5분/10분/조건 해금 증거에 guidance coverage 확장 |
| Android / Backend | P2 | Android E2E로 newly unlocked evidence feedback 확인. 미노출 재현 시 unlock reason/recent unlocks 응답 계약 추가 |
| Backend | P2 | guidance null coverage를 smoke metric으로 추가 |
| QA | P2 | Android 또는 spoiler metadata masked API runner로 30~50턴 후보 축소를 blind-valid하게 재측정 |
| Android | P1 | guidance suggested question chip 자동 전송 제거, prefill-only 동작으로 수정 |
| Android | P2 | active session P002 복구 UX와 timeline/image 판독 UX를 10일 체크리스트 기준으로 재검증 |
| Android | P2 | chip auto-send 수정 후 draft override policy를 실제 기기에서 재검증 |
| AI policy | P2 | 공개 타임라인 밖 시간 생성 방지 문구 강화 |
| Backend/Android | P3 | suggestedQuestions 내부 character code 필드 미노출 또는 UI 미렌더링 contract 명시 |

## 9.1 Privacy Spot Check

```text
status: 미확인
reason: 운영자 redacted log snippet 또는 Loki/Grafana 접근 권한 없음
AI_CALL exists: 미확인
AI_CALL_CONTEXT exists: 미확인
raw prompt/answer/user question absence: 미확인
sessionId/scenarioId/suspectId/npcCode absence in AI_CALL_CONTEXT: 미확인
```

## 10. Private Artifact Notice

정답 상세, raw result, raw session id, 제출 후보 상세, 스포일러성 deduction note는 public report에 포함하지 않았다.

초기 50턴 retest 시점에는 최종 제출을 하지 않았고, 이후 extended retest에서 제출을 진행했다. 사용자 확인 요청 전까지 result API는 호출하지 않았다.

## 11. Extended Retest Addendum

User request에 따라 50턴 제한 이후에도 확정 기준을 채울 때까지 추가 심문을 진행했다.

```text
서월채:
  추가 진행: 전체 증거 25/25 해금 후 68턴까지 심문
  최종 제출: 완료
  result API: 사용자 확인 요청 후 제출 이후에만 호출
  public-safe 판단: 제출 기준 충족. 상세 근거와 정오 결과는 private artifact로 분리

스튜디오9:
  추가 진행: 전체 증거 35/35 해금 후 80턴까지 심문
  최종 제출: 완료
  result API: 사용자 확인 요청 후 제출 이후에만 호출
  public-safe 판단: 제출 기준 충족. 상세 근거와 정오 결과는 private artifact로 분리
```

추가 심문 후에도 guidance coverage 이슈는 유지된다. 확정 제출은 가능했지만, 두 시나리오 모두 50턴 이후 전체 증거가 열린 뒤에야 제출 기준을 채울 수 있었다.

## 12. Post-Submit Result Check

사용자 요청으로 최종 제출 이후에만 result를 확인했다.

```text
서월채:
  result check: post-submit checked
  public-safe detail: private artifact로 분리

스튜디오9:
  result check: post-submit checked
  public-safe detail: private artifact로 분리
```

정오 여부, 점수/등급, 정답 인물명, 정답 해설 원문, raw result 전문은 public report에 포함하지 않는다.

## 13. June 10 QA Cross-Reference

2026-06-10 QA 문서 두 건을 사후 참고해 11일 retest 결과와 대조했다.

참고 문서:

```text
docs/MVP_PLAY_FLOW_QA_2026-06-10.md
docs/MVP_PLAY_FLOW_QA_SEOWOLCHAE_2026-06-10.md
docs/QA_HANDOFF.md
```

### 13.1 Still Open / Stronger Than June 10

| June 10 Issue | June 11 Status | Strengthened Judgment |
|---|---|---|
| API 스포일러 메타데이터 | 여전히 재현. `importance=CORE/FAKE`, `culpritEligible`, 역할성 asset path 확인 | P0 유지. 앱에서 숨겨도 네트워크/API 사용자에게 정답성 메타가 보이므로 데모 전 차단 필요 |
| locked evidence masking | 11일 fresh session 초반에도 locked title/unlockHint가 반환됨 | P0 회수. 현재 API/FE contract상 title/unlockHint는 Evidence 탭 정상 표시이며, 검증 초점은 description/oneLine/imageAssetKey/imageUrl/locationName/relatedSuspects/code 같은 민감 필드 masking 유지 |
| AI 답변 회피 반복 | 11일에도 50턴 내 확정 불가. post-submit result는 private artifact로 분리 | P1 유지. 단순 UX 불편이 아니라 후보 귀속 근거 부족으로 이어짐 |
| 후보 축소 blind validity | API-only run에서 spoiler metadata masking audit가 남지 않음 | API-only 후보 축소 결론은 blind invalid. Android 또는 masked runner 재검 필요 |
| 시간 답변 guard 부족 | 10일의 22시대 hallucination만큼 심하지는 않지만 11일에도 공개 시간축과 맞지 않는 표현 발생 | P2 유지. prompt/context hard guard가 아직 충분하지 않음 |
| 증거 해금 이유 불명확 | 11일에도 증거 수가 단계적으로 급증하지만 이유 설명은 API-only 기준 확인 불가 | P2 follow-up. Android에서 newly unlocked evidence reason/recent-unlock feedback 부재가 재현되기 전까지 blocker로 단정하지 않음 |
| 이미지/기록 판독 어려움 | 11일 API-only라 모바일 판독은 미확인. 다만 description/guidance coverage가 낮아 텍스트 판독값 부족은 계속 보임 | P1 유지. `readingPoints`를 모든 핵심 이미지/기록 증거로 확대 필요 |
| active session 복구 UX | 11일 API-only에서도 active session 충돌 후 abandon이 필요했음. Android UX는 미확인 | P2/P1 경계. 10일 Android 실패가 해결됐다고 볼 근거 없음 |

### 13.2 Improved Since June 10

```text
서월채:
  10일에는 25턴/13개 증거 기준으로 제출했으나 공개 보고서에는 정오를 남기지 않는다.
  11일에는 50턴 후 보류, 전체 증거 해금 후 extended 제출을 진행했다.
  개선은 guidance 때문이라기보다 전체 증거 해금과 추가 심문 덕분으로 보는 것이 타당하다.

스튜디오9:
  10일과 11일 모두 post-submit 결과 상세는 private artifact로 분리한다.
  공개 보고서 기준으로는 50턴 내 확정 보류와 후보 귀속 근거 부족을 diagnostic UX signal로만 남긴다.
  따라서 스튜디오9는 "플레이 가능"과 별개로, guidance/prompt가 후보 귀속 혼란을 충분히 줄이지 못한다.

공통:
  답변 길이와 직접 정답 누설 방지는 계속 양호하다.
  locked evidence title/unlockHint 표시는 현재 contract상 정상이다.
  잠긴 증거의 description/image/relatedSuspects/code 등 민감 필드 masking은 계속 spot check한다.
```

### 13.3 Newly Added In June 11

```text
1. guidance coverage metric:
   - 서월채 초기 7개 중 2개
   - 스튜디오9 초기/5분 구간 0개, 10분 29개 중 2개

2. suggestedQuestions payload internal code:
   - targetCharacterCode가 내려오므로 UI 미렌더링 contract가 필요하다.

3. post-submit outcome handling:
   - 두 시나리오 모두 제출 이후 result 확인은 완료했다.
   - scenario별 정오, 점수, 부분정답 breakdown은 private artifact로 분리한다.
   - 공개 보고서에는 guidance/prompt가 "방법 계층"과 "후보 귀속"을 분리해 안내하는 힘이 약했다는 UX 판단만 남긴다.

4. prompt-level root cause:
   - 정답 누설 방지는 강하지만, 증거 제시 답변의 response shape가 없어 다음 추리 단계 안내가 약하다.

5. frontend E2E blocker:
   - suggested question chip 자동 전송은 June 11 Frontend E2E follow-up에서 새로 확인된 P1이다.
```

### 13.4 Stronger Wording For Carry-Over Issues

```text
API metadata:
  "수정 필요"가 아니라 "데모/공개 플레이 전 차단해야 하는 P0"로 본다.

AI 회피 답변:
  "개선 필요"가 아니라 "잘못된 후보 귀속을 유발할 수 있는 P1"로 본다.

Guidance:
  "있으면 좋은 UX"가 아니라 "30~50턴 목표를 만족하기 위한 필수 layer"로 본다.

Image 판독:
  "모바일 편의성"만이 아니라 "핵심 증거를 읽을 수 있는 접근성/공정성 문제"로 본다.

Unlock reason:
  "toast 있으면 좋음"이 아니라 "플레이어가 자기 행동과 진행을 연결하는 핵심 피드백"으로 본다.
```

## 14. Prompt / AI Behavior Analysis

### 14.1 Current Prompt Shape

현재 심문 프롬프트는 아래 구조다.

```text
system:
  - NPC 역할
  - 최대 2문장
  - 설정에 없는 사실 생성 금지
  - 미공개 정보/비밀/정답 누설 금지
  - 범인 여부 직접 발화 금지
  - 응답 정책 우선
  - 모르면 모른다고 답변

free user prompt:
  - 용의자 공개 정보
  - 현재 공개된 모든 증거 요약
  - 사용자가 제시한 증거
  - 이전 대화
  - ResponsePolicyResolver 결과
  - tone
  - allowedFacts
  - 사용자 질문

evidence user prompt:
  - 용의자 이름/역할/알리바이
  - 제시된 증거 제목/설명
  - 현재 공개 증거 전체
  - 이전 대화
  - ResponsePolicyResolver 결과
  - allowedFacts
  - 사용자 추궁
```

근거 파일:

```text
src/main/resources/prompts/interrogation_system_prompt.txt
src/main/resources/prompts/interrogation_user_prompt.txt
src/main/resources/prompts/evidence_interrogation_user_prompt.txt
src/main/java/com/startup/domain/ai/prompt/AiPromptBuilder.java
src/main/java/com/startup/domain/ai/support/ResponsePolicyResolver.java
src/main/java/com/startup/domain/ai/support/InterrogationContextLoader.java
```

### 14.2 What Worked

```text
정답 누설 방지:
  system prompt가 범인 여부, 미공개 정보, 비밀, 정답 발화를 강하게 막고 있다.

답변 길이:
  대부분 1~2문장으로 유지됐다.

증거 제시 반응:
  evidence prompt에 "제시된 증거를 무시하지 마라"가 있어 증거 자체를 완전히 무시하는 답변은 적었다.

정책 라우팅:
  ResponsePolicyResolver가 presentedEvidenceId와 unlockedEvidenceIds 기준으로 단일 우선 정책을 선택한다.
```

### 14.3 Prompt-Level Problems

| Priority | Problem | Why It Happened | Runtime Symptom | Recommended Fix |
|---|---|---|---|---|
| P1 | 답변이 후보 축소에 필요한 “인정/부인/비교 대상” 구조를 안정적으로 주지 않음 | system/user prompt가 안전 규칙 중심이고 출력 계약이 없음 | “추가 증거를 봐야 한다”, “단정할 수 없다”가 반복됨 | evidence prompt에 `인정 가능한 사실 1개 + 부인/회피 범위 1개 + 비교할 다음 기록 1개` 구조를 강제 |
| P1 | 50턴 안에 확정이 어려움 | guidance coverage 부족과 함께 prompt가 “진행감”을 생성하지 않음 | 후보는 줄지만 마지막 attribution이 흐림 | 정책별 allowedFacts에 “이 증거로 좁혀지는 방향”을 public-safe하게 넣고, prompt가 그 방향을 1문장으로 말하게 함 |
| P2 | 시간축 충돌/흔들림이 생김 | prompt에 공개 타임라인 기준으로 답변 시간을 고정하라는 규칙이 약함 | 일부 답변에서 공개 타임라인과 다른 시간대 표현 발생 | system prompt에 “제공된 타임라인 밖 새 시각 생성 금지” 추가 |
| P2 | evidence prompt에서 용의자 맥락이 부족함 | evidence prompt는 publicProfile/publicStatement/relationToVictim을 빼고 alibi만 넣음 | 동기/관계 압박 질문에서 답변이 일반적 방어로 흐름 | evidence prompt에도 공개 프로필, 공개 진술, 피해자와의 관계 포함 |
| P2 | 공개 증거 전체가 매번 평면적으로 들어감 | `formatEvidences`가 모든 unlocked evidence를 `title: description`으로 나열함 | 핵심 비교 대상과 현재 질문 초점이 흐려짐 | 현재 제시 증거, guidance compare target, 최근 해금 증거, 나머지 공개 증거를 분리 |
| P2 | 이전 대화가 “요약”이 아니라 원문 10턴 | InterrogationContextLoader가 suspect별 최근 10턴을 그대로 포함 | 회피/부인 표현이 반복 패턴으로 강화될 수 있음 | 최근 3턴 원문 + suspect ledger summary로 변경 |
| P2 | ResponsePolicy가 단일 최고 우선순위만 선택됨 | resolver가 matching policy 중 priority max 1개만 사용 | 여러 증거가 동시에 열렸을 때 복합 추론이 약함 | selected policy + global scenario-safe comparison hints를 병합 |
| P3 | maxTokens 150이 구조화 답변에 빠듯함 | 2문장 제약은 맞지만, 인정/비교/다음 단서를 모두 담기 어려움 | 답변이 “단정 불가” 한 문장으로 끝남 | 출력 구조를 짧게 고정하거나 maxTokens를 180~220으로 조정 |

### 14.4 Suggested Prompt Changes

System prompt에 아래 규칙을 추가하는 것이 좋다.

```text
추가 규칙:
1. 제공된 타임라인, 공개 증거, allowedFacts에 없는 시간/장소/행동을 새로 만들지 않는다.
2. 증거를 제시받으면 가능한 경우 아래 3가지를 2문장 안에 포함한다.
   - 이 증거에서 인정할 수 있는 사실
   - 본인이 인정하지 않거나 모른다고 해야 하는 범위
   - 다음에 비교해야 할 공개 기록 또는 증거 유형
3. 무조건 "모른다"로 끝내지 말고, allowedFacts 안에서 플레이어가 다음에 확인할 방향을 하나만 말한다.
4. 이전 답변과 충돌하는 질문을 받으면 새 사실을 만들지 말고, 직접 본 사실과 추정/기억을 분리한다.
```

Evidence prompt는 아래처럼 바꾸는 것이 좋다.

```text
[용의자 공개 정보]
이름: {suspectName}
직책/역할: {suspectRole}
피해자와의 관계: {relationToVictim}
공개 프로필: {publicProfile}
공개 진술: {publicStatement}
공개 알리바이: {publicAlibi}

[현재 제시된 증거 - 최우선]
증거명: {evidenceTitle}
증거 내용: {evidenceDescription}

[이 증거와 비교할 공개 단서]
{focusedCompareEvidenceSummary}

[그 외 현재 공개된 증거]
{revealedEvidenceSummary}

[현재 답변 정책]
{responsePolicy}

[말해도 되는 사실]
{allowedFacts}

[사용자 추궁]
{question}

답변 형식:
- 1문장: 제시된 증거에서 인정 가능한 사실을 말한다.
- 1문장: 단정할 수 없는 범위 또는 다음 비교 대상을 말한다.
- 범인 여부, 정답, 미해금 단서는 말하지 않는다.
```

Free prompt는 아래 보강이 필요하다.

```text
추가 섹션:
[공개 타임라인 기준]
{timelineSummary}

[현재 질문의 초점]
{questionFocus}

답변 규칙:
- 시간 질문이면 공개 타임라인 안에서만 답한다.
- 직접 본 사실, 나중에 들은 사실, 추정한 사실을 구분한다.
- 모르는 경우에도 "어떤 기록을 보면 구분되는지"를 allowedFacts 범위에서 하나 말한다.
```

### 14.5 Policy Data Improvements

프롬프트만 바꿔도 일부 개선되지만, 핵심은 response policy seed도 같이 손봐야 한다.

```text
현재 문제:
  allowedFacts가 없거나 약한 policy에서는 모델이 안전하게 "모른다/추가 확인 필요"로 빠진다.

권장 policy shape:
  policyText:
    이 증거는 본인의 공개 진술 중 어느 부분을 압박하는지 명시

  allowedFacts:
    - 이 증거에서 인정 가능한 최소 사실
    - 본인이 부인해야 하는 범위
    - 다음에 비교할 공개 증거 또는 기록 유형
    - 시간축상 앞/뒤로 연결되는 공개 사실

  tone:
    defensive / cautious / shaken 같은 감정만 주고, 추론 판단은 backend policy가 제공
```

예시 방향:

```text
나쁜 allowedFacts:
  - 이 증거만으로는 단정할 수 없다.

좋은 allowedFacts:
  - 이 증거는 특정 시각 이후의 기록 정리 가능성을 압박한다.
  - 본인은 실제 행위 시점과 작성 시점이 다를 수 있다고 주장할 수 있다.
  - 다음 비교 대상은 관련 시간 기록과 호출 로그다.
```

### 14.6 Expected Impact

```text
기대 개선:
  - AI가 정답을 직접 말하지 않으면서도 매 답변마다 "다음 비교 방향"을 제공한다.
  - 회피 답변 반복이 줄어든다.
  - 시간축 hallucination 가능성이 줄어든다.
  - 30~50턴 안에 후보 축소가 더 현실적이 된다.

남는 한계:
  - guidance coverage가 부족하면 prompt만으로는 충분하지 않다.
  - public API의 CORE/FAKE importance 노출은 별도 제거가 필요하다.
```

## 15. Frontend E2E Follow-up

프론트 프로젝트 `C:\java\assignment\spring\start-up-fe`에서 Android emulator 기준 E2E smoke를 별도 수행했다.
프론트 repo의 현재 PR은 이미 merged 상태라 이 backend PR에는 아래 public-safe 요약만 포함한다.

핵심 결론:

```text
Basic app smoke:
  Home -> Library -> Scenario Detail -> Briefing -> Case Screen -> Evidence -> Suspect -> Interrogation -> Timeline -> Submit screen 진입은 확인됨.

Frontend blockers:
  - evidence guidance가 모델/UI에 구현되어 있지 않다.
  - suggested question chip이 prefill-only가 아니라 즉시 AI 호출을 수행한다.
  - suspect candidate metadata와 suspicion score가 UI에서 후보 축소 신호로 노출된다.
  - timeline API가 앱에서 사용되지 않고 placeholder가 표시된다.
```

June 10/11 QA에서 반복된 문제 중 `guidance`, `chip 자동 전송`, `candidate metadata 노출`은 API-only 문제가 아니라 실제 프론트 플레이 화면에서도 재현되므로 우선순위를 유지하거나 강화해야 한다.
`locked evidence title/unlockHint` 표시는 현재 contract상 정상 흐름이므로 blocker에서 제외하고, locked row의 민감 필드 masking만 별도 spot check로 유지한다.

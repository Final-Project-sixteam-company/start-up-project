# ClueRoom Blind Retest Report - 2026-06-12

> 상태: 역사 QA 보고서다. 현재 QA 실행 지시, 보고서 템플릿, 오픈 이슈 board는 [QA_OPERATING_GUIDE.md](../../QA_OPERATING_GUIDE.md)를 따른다.

## 0. Scope

- Tester: fresh Codex QA agent
- Knowledge state: blind, no private seed/solution/prior QA access
- API: https://api.clueroom.xyz
- Device: API-only fallback; Android UI not available in this session
- App build: not checked
- Backend branch/commit: infra/scaleout-manual-lb-poc / 089d41c
- Test time: 2026-06-12 20:00 KST
- API-only spoiler metadata masking audit: gameplay responses were masked/ignored for candidate narrowing, but raw evidence responses expose spoiler-like metadata field names
- Candidate narrowing blind validity: conditionally valid only with masking; Android UI retest required for chip/prefill behavior
- QA account/session isolation: shared public API context; 서월채 fresh session required one active-session abandon after 409

## 1. Spoiler Safety Declaration

아래 자료를 보지 않았다.

- private seed
- solution/culprit/variant truth
- prior QA result
- DB direct query
- result API before final submission
- docs/OFFICIAL_SCENARIO_DEMO_DAY.md

## 2. Final Judgment

```text
전체 판단: PARTIAL/FAIL
가장 큰 blocker: evidence guidance coverage가 너무 낮아 30~50턴 내 1~2명 후보 축소가 안정적으로 되지 않음
최종 제출 여부: 두 시나리오 모두 미완료; 확정 기준 미충족으로 추측 제출하지 않음
30~50회 심문 내 후보 축소 가능성: 서월채 PARTIAL, 스튜디오9 FAIL에 가까운 PARTIAL
후보 축소 blind validity: API-only masking 적용 시 조건부 유효; raw API에는 metadata leak risk 있음
guidance가 추리 보조인지 정답 경로 고정인지: 있는 guidance는 보조 성격이나, 적용 증거가 너무 적음
```

## 3. Findings First

| Priority | Scenario | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P1 | 공통 | API evidence response에 spoiler-like metadata 필드가 노출됨 | locked/unlocked evidence raw field names에 `importance`, `imageAssetKey`, `imageUrl` 존재 | public play API는 후보성/중요도/asset 기반 역할 추론 필드를 gameplay 응답에서 제거하거나 서버에서 redaction | API-only runner가 별도 마스킹하지 않으면 blind validity가 깨질 수 있음 | QA/외부 클라이언트가 CORE/FAKE 또는 역할성 asset 추론을 볼 위험 | public DTO에서 제거하거나 앱 전용 안전 필드만 반환 |
| P1 | 스튜디오9 | 50턴 내 후보를 1~2명으로 좁히기 어려움 | 50턴 후에도 여러 현장/역할/운영 조건 축이 병렬로 남음 | guidance와 AI 답변이 비교 순서를 제시해 후보가 단계적으로 축소됨 | 물리 단서는 늘어나지만 사용자가 직접 비교 그래프를 구성해야 함 | 신규 유저가 찍기 없이 최종 제출하기 어려움 | 핵심 증거군별 reading/compare/question guidance를 앞당겨 추가 |
| P1 | 서월채 | 50턴 내 최종 후보 확정 기준 미충족 | 접근 가능성/행동 흐름 신호는 생기지만 동기/수단/기회/은폐가 한 후보로 수렴하지 않음 | 30~50턴 안에 최종 후보 1~2명 수준까지 축소 | 중반 이후에도 여러 축이 남아 최종 제출 보류 | 최종 추리 제출 흐름까지 자연스럽게 도달하지 못함 | 5분 해금 증거에도 guidance를 확장하고 비교 순서를 명확히 제공 |
| P2 | 공통 | guidance coverage가 낮음 | 서월채: 5분 후 13개 해금 중 2개 guidance, 스튜디오9: 10분 후 29개 해금 중 2개 guidance | 주요 해금 증거에는 readingPoints/compareEvidences/suggestedQuestions 제공 | 일부 증거에만 제공되어 사용자가 다음 질문을 직접 설계해야 함 | 진행감은 있으나 UX 안내가 약함 | 핵심/분기 증거마다 최소 readingPoints와 next compare target 제공 |
| P2 | 공통 | Android chip prefill-only 동작 미확인 | 현재 세션에서 Android UI 조작 불가 | chip tap, draft override, 자동 전송 여부 검증 | API-only로는 UI 동작을 검증할 수 없음 | 프론트 계약 핵심 항목이 미검증 상태 | Android retest에서 별도 확인 필요 |

## 4. Summary

| Scenario | Basic Flow | Guidance UX | Interrogation Quality | Candidate Narrowing | Final Submit | Overall |
|---|---|---|---|---|---|---|
| 서월채 | PARTIAL | PARTIAL | PASS | PARTIAL/FAIL | 미완료 | PARTIAL |
| 스튜디오9 | PARTIAL | FAIL/PARTIAL | PASS | FAIL/PARTIAL | 미완료 | PARTIAL/FAIL |

## 5. Scenario A: 서월채

### 5.1 Flow Result

```text
sessionId: <redacted>
basic flow: scenario list/detail, active restore, fresh create, dashboard, locations, evidences, suspects, timeline, interrogation OK
final submit: 미완료; 확정 기준 미충족
result screen: 미확인; final-deduction 전 result API 호출하지 않음
```

### 5.2 Guidance UX

```text
readingPoints: 일부 초기 증거에서만 확인
compareEvidences: 일부 증거에서 locked compare title/hint는 보이고 내부 code는 확인되지 않음
suggestedQuestions: 일부 증거에서 target/question/questionType 제공
locked compare masking: 내부 code 노출은 확인되지 않음
chip prefill-only: Android UI 미확인
draft override policy: Android UI 미확인
```

### 5.3 Interrogation / Deduction Path

```text
turns used: 50
candidate narrowing: broad -> medium, but final 1~2명 확정 실패
blind validity: API metadata masking 적용
blocked moments: guidance 없는 해금 증거가 많아 다음 비교 순서를 사용자가 직접 구성해야 함
public narrowing summary: 여러 접근/행동/상태/기록 계열 축은 생겼지만 한 후보로 수렴하지 않음
rejected-candidate rationale detail: omitted from public report; private note only
remaining doubt: public-safe broad summary only
```

### 5.4 10-Turn Summaries

```text
Turn 1~10:
  main targets: all suspects
  evidence used: initial motive/meeting evidence
  public narrowing delta: broader
  AI answer quality: mostly concise; some evasive
  next interrogation plan: initial timeline and movement comparison

Turn 11~20:
  main targets: all suspects
  evidence used: initial public evidence set
  public narrowing delta: narrower
  AI answer quality: helpful enough, no direct spoiler
  next interrogation plan: newly unlocked clue comparison

Turn 21~30:
  main targets: all suspects
  evidence used: newly unlocked location/context evidence
  public narrowing delta: narrower but not decisive
  AI answer quality: concise; several "cannot confirm" answers
  next interrogation plan: wait for timed unlock and compare logs

Turn 31~40:
  main targets: care/medical/security/admin roles
  evidence used: 5-minute unlock log evidence
  public narrowing delta: narrower
  AI answer quality: useful comparison hints, still non-decisive
  next interrogation plan: top-candidate motive/method/opportunity/cover-up separation

Turn 41~50:
  main targets: top candidate group plus remaining high-risk roles
  evidence used: strongest available public evidence
  public narrowing delta: stalled before final certainty
  AI answer quality: no direct answer leak; mostly 1~2 sentences
  next interrogation plan: Android/UI retest or additional guided evidence needed
```

### 5.5 Scenario Notes

```text
What worked: basic API flow, time-based evidence unlock, AI spoiler avoidance
What did not work: guidance coverage too narrow after timed unlock
AI answer quality: generally acceptable; not enough to replace missing UX guidance
UX friction: user must know which logs to compare without enough on-screen guidance
```

## 6. Scenario B: 스튜디오9

### 6.1 Flow Result

```text
sessionId: <redacted>
basic flow: scenario list/detail, fresh create, dashboard, locations, evidences, suspects, timeline, interrogation OK
final submit: 미완료; 확정 기준 미충족
result screen: 미확인; final-deduction 전 result API 호출하지 않음
```

### 6.2 Guidance UX

```text
readingPoints: 10분 해금 후 일부 증거에서만 확인
compareEvidences: guidance 있는 일부 증거에서 정상 제공
suggestedQuestions: guidance 있는 일부 증거에서 target/question/questionType 제공
locked compare masking: 내부 code 노출은 확인되지 않음
chip prefill-only: Android UI 미확인
draft override policy: Android UI 미확인
```

### 6.3 Interrogation / Deduction Path

```text
turns used: 50
candidate narrowing: broad -> multiple axes, but final 1~2명 확정 실패
blind validity: API metadata masking 적용
blocked moments: 초기 30턴 동안 guidance가 전혀 없어 질문 설계 부담이 큼
public narrowing summary: 여러 역할, 현장 조건, 운영 단서 축이 병렬로 남음
rejected-candidate rationale detail: omitted from public report; private note only
remaining doubt: public-safe broad summary only
```

### 6.4 10-Turn Summaries

```text
Turn 1~10:
  main targets: all suspects
  evidence used: initial scene/role evidence
  public narrowing delta: broader
  AI answer quality: concise, safe
  next interrogation plan: location / safety / visibility comparison

Turn 11~20:
  main targets: all suspects
  evidence used: initial physical and audio evidence
  public narrowing delta: narrower only by broad axes
  AI answer quality: good at saying what needs comparison
  next interrogation plan: repeat comparison without guidance

Turn 21~30:
  main targets: several production/context roles
  evidence used: initial evidence only
  public narrowing delta: stalled
  AI answer quality: useful but non-decisive
  next interrogation plan: wait for timed unlock

Turn 31~40:
  main targets: several role/context groups
  evidence used: 5-minute unlock physical evidence
  public narrowing delta: narrower, still multiple axes
  AI answer quality: one late evidence signal became stronger, still no final certainty
  next interrogation plan: 10-minute unlock and guidance check

Turn 41~50:
  main targets: remaining role/context groups
  evidence used: 10-minute unlock guidance and related evidence
  public narrowing delta: narrower but not enough for final submit
  AI answer quality: safe and mostly helpful
  next interrogation plan: add guidance for branch resolution
```

### 6.5 Scenario Notes

```text
What worked: timed unlock produced meaningful new evidence; AI did not reveal answer
What did not work: guidance appeared too late and on too few evidence details
AI answer quality: mostly PASS; repeated deflection is acceptable but leaves UX burden
UX friction: new user must independently build a complex comparison graph
```

## 7. Cross-Scenario Findings

```text
What improved: AI answer style is generally short, non-spoiler, and acknowledges evidence limits.
What still blocks users: guidance coverage and candidate narrowing.
Whether guidance feels like clue-reading aid or answer railroading: where present, it feels like an aid rather than railroading.
Whether 30~50 interrogation target is realistic: not reliably realistic with current guidance coverage.
```

## 8. Good Points

```text
Backend: scenario list/detail/session/dashboard/evidence/suspect/timeline/interrogation flows worked.
Android: not checked in this session.
Scenario seed: public evidence descriptions are readable and comparison-oriented.
AI behavior: no direct culprit/solution leak observed; answers stayed mostly within 1~2 sentences.
```

## 9. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Backend | P1 | Remove/redact `importance` and asset-style inference fields from public gameplay evidence responses. |
| Scenario seed | P1 | Add guidance to major timed unlock evidence, especially branch-resolving evidence. |
| Android | P2 | Verify chip tap is prefill-only, draft override does not auto-send, and locked compare evidence does not reveal internal codes. |
| AI policy | P2 | Keep current non-spoiler stance, but tune evasive replies to provide one concrete next comparison when possible. |

## 10. Privacy Spot Check

```text
status: 미확인
AI_CALL exists: 미확인
AI_CALL_CONTEXT exists: 미확인
raw prompt 없음: 미확인
raw answer 없음: 미확인
raw user question 없음: 미확인
sessionId/scenarioId/suspectId/npcCode가 AI_CALL_CONTEXT에 없음: 미확인
note: 운영 로그 접근 권한/redacted snippet이 없어 확인하지 못함.
```

## 11. Private Artifact Notice

정답 상세, raw result, raw session id, 제출 후보 상세, rejected-candidate rationale, 스포일러성 deduction note는 public report에 포함하지 않았다.
필요하면 private artifact로 별도 전달한다.

## 12. Extended Interrogation Addendum

사용자 요청에 따라 최초 50턴 기준 판정 이후 추가 심문을 진행했다.
이 구간은 원래 QA 목표였던 "30~50회 심문 안 후보 축소" 판정과 분리한다.

```text
추가 심문 범위:
서월채: Turn 51~80
스튜디오9: Turn 51~75

final submit:
서월채: 완료
스튜디오9: 완료

result screen:
서월채: reachable
스튜디오9: reachable

public report policy:
제출 후보명, 정오답, 점수, 해설, raw result는 기록하지 않음
```

### 12.1 서월채 Extended Result

```text
candidate narrowing: additional evidence unlocked and narrowed candidate reasoning substantially
key public-safe shift: late evidence made one hypothesis more supportable while earlier broad alternatives became less persuasive
confidence after extra turns: submit-ready
original 30~50 target impact: still PARTIAL because submit-ready narrowing required turn 62+ unlock/interrogation
```

추가 심문 중 남은 증거가 해금되면서 후보별 설명 가능성을 더 안전하게 비교할 수 있었다.
이후 같은 증거를 여러 용의자에게 제시해 반응 차이를 확인했고, 최종 제출 가능한 수준까지 후보 논리가 수렴했다.

### 12.2 스튜디오9 Extended Result

```text
candidate narrowing: all evidence unlocked; primary hypothesis separated from secondary contextual conditions
key public-safe shift: late comparison evidence made one hypothesis stronger while several contextual signals became secondary
confidence after extra turns: submit-ready
original 30~50 target impact: still PARTIAL/FAIL because decisive separation required all-evidence interrogation after turn 50
```

추가 심문에서는 여러 현장 조건과 행동 단서를 분리해서 물었다.
그 결과 여러 가능성 중 핵심 설명 축이 더 선명해졌고, 최종 제출 가능한 수준까지 후보 논리가 수렴했다.

### 12.3 Updated Public-Safe Follow-up

```text
30~50턴 목표는 여전히 안정적으로 충족되지 않음.
추가 심문을 허용하면 두 시나리오 모두 제출까지 도달 가능.
따라서 개선 초점은 정답 자체가 아니라, 결정적 비교 증거와 질문 방향을 30~50턴 안에 더 빨리 노출/안내하는 것.
```

# ClueRoom Android E2E Local Retest Report - 2026-06-18

> 상태: 역사 QA 보고서다. 현재 QA 실행 지시, 보고서 템플릿, 오픈 이슈 board는 [QA_OPERATING_GUIDE.md](../../QA_OPERATING_GUIDE.md)를 따른다.

## 0. Scope

이 문서는 2026-06-18 KST에 수행한 로컬 백엔드 + Android 앱 연결 E2E 재검증 결과를 public-safe로 정리한다.

```text
Backend repo: C:\java\assignment\spring\start-up
Backend branch/commit: codex/llmops-cost-rate-limit-plan / 2192e2c
Frontend repo: C:\java\assignment\spring\start-up-fe
Frontend branch/commit: codex/qa-p0-p1-spoiler-safe / 22dc636
API: local backend http://localhost:18080, Android app base URL http://10.0.2.2:18080
Backend mode: local profile, dev login enabled, official scenario import enabled, AI provider set to local none/mock mode
App build: release APK with API_BASE_URL=http://10.0.2.2:18080 and ENABLE_DEV_LOGIN=true
Device: ClueRoom_QA_API35 Android emulator, Android 15 API 35, emulator-5554
Flutter/Dart: Flutter 3.44.0, Dart 3.12.0
Test date: 2026-06-18 KST
```

주의:

```text
정답 범인명, 선택 후보명, 정답 수법/동기/은폐 원문, private seed, raw session id, token 값은 이 문서에 기록하지 않는다.
결과 점수/등급/부분 정오답 breakdown, solution fullExplanation, raw result는 private artifact로 분리한다.
GET /api/play-sessions/{sessionId}/result 는 final-deduction 제출 후에만 호출했다.
이번 문서는 기존 QA 보고서를 참고한 operator retest이며, strict blind retest로 보지 않는다.
```

## 1. Source Documents Reviewed

| Source | 반영 내용 |
|---|---|
| `docs/QA_OPERATING_GUIDE.md` | current QA board, public-safe report rule, final result privacy rule |
| `docs/QA_BLIND_RETEST_PROMPT_2026-06-11.md` | blind QA prompt가 현재 guide로 흡수됐음을 확인 |
| `docs/QA_HANDOFF.md` | 현재 정본이 `QA_OPERATING_GUIDE.md`임을 확인 |
| `docs/qa/archive/QA_BLIND_RETEST_REPORT_2026-06-12.md` | report shape, 30~50턴 후보 축소 기준 |
| `docs/qa/archive/QA_E2E_FOLLOWUP_REPORT_2026-06-12.md` | Android/API 연결 follow-up report 구성 |
| `docs/qa/archive/QA_E2E_CULPRIT_CONFIRMATION_REPORT_2026-06-15.md` | final submit reachability와 private artifact 분리 방식 |

## 2. Safety Declaration

```text
Private seed/solution opened: no
DB direct query: no
Server file grep for solution/culprit: no
Result API before final deduction: no
Raw token/session id recorded in report: no
Raw user question/AI answer transcript recorded in report: no
Result score/grade/correctness/breakdown kept private: yes
Rejected-candidate rationale kept private: yes
Privacy spot check: partial/inconclusive for local temp logs
```

이번 실행은 기존 QA 보고서를 참고한 뒤 진행했으므로 blind validity는 제한된다. 판단 목적은 "정답을 모르는 신규 유저성"이 아니라, 로컬 백엔드와 Android 앱을 실제 연결했을 때 공식 시나리오가 세션 시작, 증거 해금, 심문, 최종 제출, 결과 조회까지 도달 가능한지 확인하는 것이다.

## 3. Final Judgment

```text
전체 판단: PARTIAL/PASS
Android E2E: PASS for login, library, scenario detail, session start, briefing, investigation tabs, evidence/suspect/timeline/submit screen, and manual interrogation smoke
Final submit/result path: PASS for both official scenarios, with final result checked only after final-deduction
Candidate narrowing:
  - 서월채: API-assisted extended route에서 submit-ready 도달. 30~50턴 범위 안에 들어왔지만 strict blind validity는 제한됨
  - 스튜디오9: submit-ready 도달. 그러나 30~50턴 목표는 여전히 미달에 가깝고 extended route가 필요함
Merge/release blocker:
  - Android runtime itself is viable with the new API35 emulator profile
  - AI interrogation quality remains a gameplay blocker in local none/mock mode
```

핵심 해석:

```text
이전 Pixel_10/API37 emulator crash 때문에 막혔던 Android E2E는, API35 emulator + host GPU + headless 실행으로 재개 가능했다.
두 공식 시나리오 모두 앱에서 시작해 같은 세션으로 최종 제출/result path까지 도달했다.
다만 최종 추리 본문 입력과 다중 증거 선택은 ADB 좌표 자동화 안정성 때문에 API로 제출했고, 앱에서는 제출 화면 진입과 25/25, 35/35 상태 표시까지 확인했다.
따라서 "연결 E2E reachability"는 통과, "순수 앱 UI만으로 모든 제출 조작"은 별도 재검증이 필요하다.
```

## 4. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P1 | Android QA infra | 기존 Pixel_10/API37 emulator는 QA 표준 장치로 부적합 | `avdmanager`에서 Pixel_10 device profile invalid, Android 37 preview/16KB image에서 repeated emulator crash. API35 `ClueRoom_QA_API35` + `-gpu host -no-window`는 login 이후 장시간 안정 동작 | QA runner가 재현 가능한 Android emulator profile을 사용 | 기존 AVD는 login/input 전후로 crash, 새 AVD는 두 시나리오 E2E에 사용 가능 | QA가 앱 문제가 아닌 emulator 문제로 중단될 수 있음 | API35 Google APIs x86_64 기반 QA AVD와 launch command를 runbook/docs에 고정 |
| P1 | AI interrogation | 증거 제시 심문 답변이 너무 회피적이거나 사건 맥락과 맞지 않는 경우가 반복됨 | 스튜디오9 다수 심문에서 generic deflection이 반복됨. 서월채 일부 답변은 현재 사건과 어긋나는 용어가 섞임. raw 답변 원문은 public report에서 생략 | 증거 제시 답변은 public-safe 범위에서 인정 사실, 모르는 범위, 다음 비교 대상을 짧게 제공 | 정답 누설은 없지만 후보 축소에 필요한 차이를 거의 만들지 못하는 답변이 많음 | 심문이 플레이어의 추리 도구로 충분히 작동하지 않음 | AI response shape smoke를 추가하고, local none/mock mode 답변도 시나리오별 public facts에 맞게 조정 |
| P1 | Candidate narrowing | 스튜디오9는 여전히 30~50턴 안에 submit-ready로 자연 수렴하기 어렵다 | 35/35 해금과 extended interrogation 이후 제출 가능. 50턴 전에는 MARK 9, 프롬프터/인이어, 조명, 안전장비, 사후 편집 축이 병렬로 남음 | 30~50턴 안에 핵심 실행 축과 보조 조건이 분리됨 | 최종 제출 가능성은 확인했으나 extended route가 필요 | 신규 유저는 찍기 제출 또는 과도한 질문 반복으로 흐를 수 있음 | branch-resolving guidance를 더 이른 시점에 노출하고, 증거별 next compare target을 강화 |
| P1/P2 | Frontend submit E2E | 앱 제출 화면은 도달했지만 최종 제출 조작은 API로 수행 | Android app에서 submit screen, suspect dropdown area, evidence selector, 25/25 및 35/35 상태 확인. 긴 추리 본문/다중 증거 선택/final submit은 같은 session에 API로 수행 | Android 앱 UI만으로 final deduction submit까지 안정 자동화 | reachability는 확인됐으나 submit form full UI automation은 미완료 | release candidate 검증 시 UI submit regression을 놓칠 수 있음 | Playwright/ADB보다 안정적인 Flutter integration test 또는 accessibility key 기반 submit E2E 추가 |
| P2 | Guidance / chip | 추천 질문은 자동 호출 없이 입력 전 상태를 확인했으나 모든 chip route는 검증되지 않음 | 심문 화면 진입 시 자동 AI call은 관찰되지 않았고, 수동 입력/전송은 성공. evidence guidance suggestedQuestions route 전체는 좌표 자동화로 재현하지 않음 | 모든 추천 질문 chip이 prefill-only이고 사용자가 전송 전 수정 가능 | 최소 smoke는 PASS, 전체 chip matrix는 pending | 자동 AI 호출 회귀를 완전히 배제할 수 없음 | hardcoded/default chip과 evidence guidance chip 전체를 Android E2E로 분리 검증 |
| P2 | Ops/privacy | local temp log privacy spot check가 완전하지 않음 | selected temp logs에서 token-like strings는 관찰되지 않았지만, old local debug logs에 prompt/answer key 문자열이 많아 raw content absence를 확정하지 못함 | raw token/session/question/answer/prompt가 운영 로그에 없어야 함 | 로컬 로그 기준 partial only. 운영 Loki/Grafana redacted snippet 미확인 | privacy compliance 판단을 완료할 수 없음 | 운영/스테이징 redacted log snippet으로 AI_CALL/AI_CALL_CONTEXT privacy checklist 재확인 |

## 5. Flow Summary

| Scenario | Surface | Session | Guidance | Chip | AI quality | Candidate narrowing | Final submit |
|---|---|---|---|---|---|---|---|
| 서월채 | Android + same-session API assist | fresh local dev-login session | evidence detail guidance and unlock flow usable | no auto-call observed on manual interrogation smoke; full chip matrix pending | PARTIAL, some off-context local/mock answers | extended route PASS; strict blind validity limited | completed via API after Android submit screen reached; result checked after submit |
| 스튜디오9 | Android + same-session API assist | fresh local dev-login session | rich guidance after timed/evidence unlock; decisive guidance appears late | full chip matrix pending | FAIL/PARTIAL, repeated generic deflection in local/mock mode | 30~50 target PARTIAL/FAIL, extended route PASS | completed via API after Android submit screen reached; result checked after submit |

## 6. Android E2E Surface Checked

```text
Common:
  - release APK installed on Android 15 emulator
  - notification permission dialog handled
  - onboarding skip
  - Dev Login visible and successful
  - library loaded from local backend
  - official scenario cards visible
  - scenario detail and public synopsis visible
  - play session create/resume flow visible
  - briefing screen visible
  - investigation screen visible
  - scene/evidence/suspect/timeline/submit bottom tabs visible
  - evidence count updated after unlock
  - submit screen visible after evidence unlock

서월채:
  - scenario detail -> briefing -> investigation PASS
  - suspect detail PASS
  - interrogation screen PASS
  - one manual question send/answer smoke PASS
  - evidence count reached 25/25
  - submit screen showed completed investigation state

스튜디오9:
  - scenario detail -> briefing -> investigation PASS
  - scene board and timed unlock count update PASS
  - evidence count reached 35/35
  - submit screen reachable
```

## 7. Scenario A - 서월채

### 7.1 Flow Result

```text
sessionId: <redacted>
Android app flow: PASS through login, library, detail, briefing, investigation, suspect detail, interrogation, evidence board, timeline, submit screen
evidence unlock: 25/25
interrogation count: public-safe approximate count in the 30~50 range
final submit: completed through same-session API
result screen/API: result API checked only after final-deduction
correctness detail: private artifact
```

### 7.2 Public-Safe Deduction Path

```text
초반:
  회의, 관계, 기본 동선, 물품, 복약/음료 계열 가능성이 넓게 열림.

중반:
  시간 해금과 증거 제시 해금으로 로그/체크리스트/처방/바이탈 계열 비교가 가능해짐.

후반:
  섭취 경로, 시간대 변화, 서비스 기록, 수정 흔적을 비교하면서 하나의 설명 축이 더 강해짐.
  최종 제출 가능한 수준까지 동기/방법/은폐 서술이 수렴함.
```

### 7.3 Public-Safe Finding

```text
서월채는 이번 로컬 연결 E2E에서 최종 제출까지 도달 가능했다.
다만 답변 품질은 안정적이지 않았고, 일부 답변이 사건 맥락과 어긋나는 표현을 포함했다.
guidance와 evidence unlock 자체는 제출 가능 상태까지 이어졌으므로, 다음 개선 초점은 답변 shape와 앱 submit UI full automation이다.
```

## 8. Scenario B - 스튜디오9

### 8.1 Flow Result

```text
sessionId: <redacted>
Android app flow: PASS through login, library, detail, briefing, investigation, evidence count updates, submit screen
evidence unlock: 35/35
interrogation count: extended route, above the 30~50 target
final submit: completed through same-session API
result screen/API: result API checked only after final-deduction
correctness detail: private artifact
```

### 8.2 Public-Safe Deduction Path

```text
초반:
  MARK 위치, Actor POV, 프롬프터/인이어, 과노출, 안전장비, 제작비/장비 교체 축이 동시에 열림.

중반:
  5분 해금 후 MARK 위치 조작, 겔/테이프, 저가형 안전 클립, 비승인 장비, 하네스 결착 축이 병렬로 남음.

후반:
  10분 해금과 증거 제시 해금 이후 프롬프터 반사, 대본 불일치, 현장 오디오, 원본/편집 자료, 조명 필터 증거를 비교할 수 있게 됨.
  핵심 실행 축과 보조 혼선 조건이 분리되면서 submit-ready 상태에 도달함.
```

### 8.3 Public-Safe Finding

```text
스튜디오9는 evidence/guidance가 풍부하지만, 단서 축이 많아 30~50턴 안에 자연스럽게 닫히지 않는다.
특히 local none/mock interrogation 답변이 대부분 후보 축소에 기여하지 못해, 플레이어가 증거 그래프를 직접 관리해야 한다.
최종 제출 가능성은 확인했지만, 신규 유저 UX 목표 기준으로는 guidance 우선순위와 답변 shape 개선이 필요하다.
```

## 9. 10-Turn Broad Summary

| Scenario | Turns | Public coverage | Evidence categories | Candidate breadth | Narrowing signal | Private detail |
|---|---|---|---|---|---|---|
| 서월채 | 1-10 | most public suspects | initial scene, meeting, dining/bedside items | wide | several motive/opportunity axes opened | private artifact |
| 서월채 | 11-20 | most public suspects | timed logs, care/medicine/security records | medium | key time/log comparisons became available | private artifact |
| 서월채 | 21-30 | focused role groups | checklist, package/memo, route/time evidence | medium/narrow | one hypothesis became more supportable | private artifact |
| 서월채 | 31-43 | focused role groups | all public evidence including late comparison data | narrow | submit-ready public-safe story formed | private artifact |
| 스튜디오9 | 1-10 | all/most public suspects | scene, MARK, Actor POV, audio/light/safety basics | wide | broad technical and role axes opened | private artifact |
| 스튜디오9 | 11-30 | all/most public suspects | initial evidence plus early evidence-presented unlock | wide/medium | one or two clue families strengthened but no final convergence | private artifact |
| 스튜디오9 | 31-50 | most role groups | MARK/gel/tape, safety, budget, light categories | unsettled | multiple plausible branches remained | private artifact |
| 스튜디오9 | 51-87 | focused role groups | all evidence including prompt/reflection/original-vs-edited materials | narrow | primary execution axis separated from secondary conditions | private artifact |

## 10. Verification

```text
Backend:
  - local backend /api/scenarios returned official scenarios
  - local dev login succeeded without printing token
  - play session create/active/dashboard/locations/evidences/suspects/timeline/interrogation/final-deduction/result paths exercised
  - result API called only after final-deduction

Frontend:
  - flutter test: PASS, 32 tests
  - release APK build: PASS
  - Android app E2E smoke: PASS on ClueRoom_QA_API35 with host GPU headless
  - Chrome/web smoke: blocked by Google Sign-In web client configuration; not used for final QA

Device:
  - Pixel_10/API37 AVD: unstable, not recommended
  - ClueRoom_QA_API35/API35 AVD: stable for this run with host GPU headless launch

Privacy:
  - token strings not observed in selected local temp log spot check
  - prompt/answer raw absence not proven from local debug logs; operations redacted log check still required
```

## 11. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Docs/QA | P1 | Standardize Android QA emulator profile: API35 Google APIs x86_64, `ClueRoom_QA_API35`, host GPU headless command |
| AI policy | P1 | Local none/mock and production AI responses should use the same public-safe shape: 인정 사실, 모르는 범위, 다음 비교 대상 |
| Scenario/AI | P1 | 스튜디오9 branch-resolving guidance를 30~50턴 안에 더 빨리 노출하고, 보조 조건과 핵심 실행 조건을 구분하는 안내 강화 |
| Frontend | P1/P2 | Final submit form full Android UI automation 추가. 긴 추리 본문, suspect dropdown, evidence multiselect, result navigation 포함 |
| Frontend | P1 | 모든 suggested question route가 prefill-only인지 Android E2E로 검증 |
| Ops | P1 | AI_CALL/AI_CALL_CONTEXT redacted log snippet으로 raw prompt/answer/user question/session/token 부재 확인 |
| Backend/QA | P2 | Local AI provider none/mock mode가 QA candidate narrowing에 미치는 영향을 문서화하고, production/staging AI smoke와 구분 |

## 12. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw session id
raw access token
선택 후보명
정답 범인명
정답 수법/동기/은폐 원문
solution fullExplanation
점수/등급/부분 정오답 breakdown
제출 후보별 상세 반증 매트릭스
raw user question and raw AI answer transcript
DB raw result dump
```

Private detail은 local QA run note에만 남기고 repo 문서에는 커밋하지 않는다.

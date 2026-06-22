# ClueRoom QA E2E Follow-up Report - 2026-06-12

> 상태: 역사 QA 보고서다. 현재 QA 실행 지시, 보고서 템플릿, 오픈 이슈 board는 [QA_OPERATING_GUIDE.md](../../QA_OPERATING_GUIDE.md)를 따른다.

## 0. Scope

이 문서는 2026-06-10 Android E2E QA, 2026-06-11 blind retest, 2026-06-12 API-only extended retest, 2026-06-12 Flutter emulator E2E follow-up을 public-safe로 연결한 추가 QA 보고서다.

```text
Backend repo: C:\java\assignment\spring\start-up
Backend branch/commit: infra/scaleout-manual-lb-poc / c0ee3b4
Frontend repo: C:\java\assignment\spring\start-up-fe
Frontend branch/commit: develop / ca84f39
API: https://api.clueroom.xyz
Frontend E2E device: Android emulator emulator-5554, Android 17 API 37
Flutter/Dart: Flutter 3.44.0, Dart 3.12.0
Test date: 2026-06-12 KST
```

주의:

```text
정답 범인명, 정답 수법/은폐 원문, private seed, raw session id, raw result, token 값은 이 문서에 기록하지 않는다.
점수/등급/세부 채점 breakdown은 private artifact로 분리한다.
운영 API를 직접 변경하는 프론트 E2E는 수행하지 않았다.
```

## 1. Source Documents Reviewed

아래 QA 관련 문서를 읽고 후속 판단에 반영했다.

| Source | 반영 내용 |
|---|---|
| `docs/QA_HANDOFF.md` | 당시 기준 handoff. 현재 정본은 `docs/QA_OPERATING_GUIDE.md` |
| `docs/QA_BLIND_RETEST_PROMPT_2026-06-11.md` | blind QA 안전 기준, report format, 30~50턴 기준 |
| `docs/qa/archive/QA_BLIND_RETEST_REPORT_2026-06-11_CODEX.md` | 6/11 API-only retest와 frontend follow-up 결과 |
| `docs/qa/archive/QA_BLIND_RETEST_REPORT_2026-06-12.md` | 6/12 blind/extended retest 결과 |
| `docs/qa/archive/MVP_PLAY_FLOW_QA_2026-06-10.md` | 스튜디오9 Android E2E 이슈와 개선안 |
| `docs/qa/archive/MVP_PLAY_FLOW_QA_SEOWOLCHAE_2026-06-10.md` | 서월채 Android/API E2E 이슈와 active session blocker |
| `start-up-fe/docs/FRONTEND_E2E_QA_2026-06-11_CODEX.md` | 6/11 프론트 E2E 기준과 당시 drift |

## 2. Final Judgment

```text
전체 판단: backend gameplay는 extended retest 기준 제출까지 도달 가능. 그러나 30~50턴 목표는 아직 안정적으로 충족되지 않는다.
최종 제출 검증: 두 공식 시나리오 모두 post-submit result/DB 확인 경로까지 도달했다. 정오, 점수/등급, breakdown은 private artifact로 분리한다.
가장 큰 UX blocker: guidance coverage 부족과 AI 답변의 다음 비교 방향 부족.
가장 큰 API safety blocker: public gameplay 응답의 spoiler-like metadata exposure.
추가 확인: spoiler-like metadata는 DTO/API spec/test에도 남아 있어 단순 UI 표시 문제가 아니라 public contract 문제로 봐야 한다.
프론트 active-session blocker: 현재 Flutter 코드 기준으로는 재현되지 않음. 로컬 저장소가 비어도 server active session으로 복구되는 경로를 emulator E2E와 단위 테스트로 확인했다.
운영 로그/telemetry blocker: final deduction DB 행의 interrogation_count가 0으로 기록되어, 실제 심문 수와 집계/저장 연결이 어긋날 가능성이 있다.
```

## 3. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P0/P1 | API spoiler safety | public play API에 answer-adjacent metadata가 남아 있음 | 6/10, 6/11, 6/12 QA에서 `importance`, 후보 가능성, 역할성 asset path 문제가 반복 관찰됨. 추가 코드 검토에서 public DTO/spec/test에도 남아 있음 | 플레이어용 API는 정답성/레드헤링/비후보 추론 필드를 제거하거나 안전한 public DTO로 변환 | API-only 또는 디버그 클라이언트에서 추리 shortcut이 가능 | blind QA와 실제 추리 경험 훼손 | public DTO와 admin/debug DTO 분리. 앱에 필요한 표시 상태만 안전하게 제공 |
| P0/P1 | Frontend/API contract | 프론트가 spoiler-like metadata를 실제 UX 제어에 소비함 | `culpritEligible`로 지목 가능 후보를 필터링하고 `importance`로 핵심 증거 상태를 계산 | 앱 UX는 정답성/후보성 필드 없이 public-safe 상태만 사용 | 서버 truth-adjacent field가 UI affordance로 이어짐 | API 노출을 제거해도 프론트 계약을 같이 바꾸지 않으면 회귀 가능 | FE/BE 동시 변경. public candidate/status 필드를 새로 정의하고 `culpritEligible`/`importance` 소비 제거 |
| P1 | Guidance UX | 30~50턴 내 후보 축소를 안정적으로 돕기에는 guidance coverage가 부족함 | 서월채/스튜디오9 모두 50턴 기준 확정 제출 보류, extended turns 후 제출 가능 | 주요 해금 증거마다 readingPoints/compareEvidences/suggestedQuestions 제공 | 일부 증거에만 guidance가 있고 시간 해금 후에도 질문 방향을 사용자가 직접 구성해야 함 | 신규 유저가 찍기 제출하거나 이탈할 가능성 | 시간 해금/분기 증거까지 guidance 확장. 증거별 next compare target 제공 |
| P1 | AI interrogation | 안전하지만 회피적인 답변이 반복됨 | “단정 불가”, “다른 증거와 함께 봐야 함” 유형 반복 | 증거 제시 답변은 인정 사실, 부인 범위, 다음 비교 대상을 짧게 제공 | 정답 누설은 막지만 다음 행동 정보가 약함 | 심문이 후보 축소 도구로 충분히 작동하지 않음 | responseShape를 정책화하고 allowedFacts에 public-safe comparison hint 추가 |
| P1 | Frontend active session | 6/10에 앱 시작 전 active session 충돌 blocker가 있었으나 현재 Flutter 코드에서는 복구됨 | 2026-06-12 emulator fake API E2E: `GET /active` 1회, `POST /play-sessions` 0회, case screen 진입 | local storage가 비어도 server active session으로 이어가기 | 현재 코드 기준 PASS | APK 재설치/다른 기기에서 “세션 이미 있음”으로 막히던 위험 감소 | 현재 구현 유지. prod QA 계정으로 운영 API spot check는 별도 승인 후 수행 |
| P1 | Telemetry/data integrity | final deduction DB 조회에서 `interrogation_count=0`으로 표시됨 | post-submit DB/result path 확인 중 interrogation_count가 0으로 관찰됨. 정오 판단은 private artifact | 많은 심문 후 제출된 세션은 심문 수/집계가 보존되어야 함 | 제출 행 또는 join 대상의 count가 실제 QA 행동과 맞지 않음 | LLMOps/QA metric, 점수 분석, 사용자 기록 신뢰도 저하 | `play_sessions.interrogation_count`, final deduction 저장 시점, result query join을 재검증 |
| P2 | Frontend QA coverage | active recovery는 확인했지만 guidance/chip/timeline/final submit UI는 이번 6/12 E2E 범위 밖 | 6/12 E2E는 active-session bug 집중. prod write 방지를 위해 fake API 사용 | QA prompt A~F 전체를 앱에서 반복 측정 | active recovery만 current-pass, 나머지는 6/11 문서 기준 open/pending | 프론트 전체 PASS로 오해할 위험 | 별도 프론트 full E2E에서 guidance/chip/timeline/final submit 재검 |
| P2 | Ops/privacy | AI_CALL/AI_CALL_CONTEXT privacy spot check 미확인 | redacted Loki/Grafana snippet 미제공 | raw prompt/answer/user question/session id가 로그에 없어야 함 | 이번 세션에서는 확인 불가 | 운영 로그 privacy 상태 미확정 | 운영자가 redacted snippet 제공 후 QA_HANDOFF checklist로 재검 |

## 4. Backend Retest Result

### 4.1 API Gameplay

```text
서월채:
  basic flow: PASS
  30~50턴 후보 축소: PARTIAL/FAIL
  extended interrogation: submit-ready까지 도달
  final submit: 완료
  post-submit result/db check: result path 확인, 세부 정오는 private artifact

스튜디오9:
  basic flow: PASS
  30~50턴 후보 축소: PARTIAL/FAIL
  extended interrogation: submit-ready까지 도달
  final submit: 완료
  post-submit result/db check: result path 확인, 세부 정오는 private artifact
```

해석:

```text
시나리오와 AI가 "끝까지 풀 수 없는" 상태는 아니다.
하지만 QA prompt가 목표로 잡은 30~50회 심문 안에 자연스럽게 1~2명 후보로 좁히는 흐름은 아직 안정적이지 않다.
추가 심문과 전체 증거 해금 이후에는 제출 가능한 논리까지 도달했다.
```

### 4.2 Post-submit DB Verification

운영 DB에서 최종 제출 이후 public-safe 범위로만 확인했다.

```text
확인한 것:
  - 두 공식 시나리오 latest final deduction row 조회 가능
  - post-submit result/DB 확인 경로 도달
  - raw session id, 정답 인물명, 정답 해설 원문은 기록하지 않음

추가 관찰:
  - 두 latest row 모두 interrogation_count가 0으로 조회됨
  - 실제 QA에서는 많은 심문을 수행했으므로 count 저장/조회 경로 재검 필요
```

## 5. Frontend E2E Follow-up Result

프론트 repo에서 2026-06-12에 Android emulator E2E를 추가 수행했다.
운영 서버 쓰기 방지를 위해 active-session 재현은 local fake API로 수행했다.

```text
Fake API target: http://10.0.2.2:18081
APK build: flutter build apk --debug --dart-define=API_BASE_URL=http://10.0.2.2:18081
Device: emulator-5554
Flow: Home -> Library -> Scenario Detail -> Briefing -> Case Screen
```

Fake API request summary:

```text
GET /api/scenarios
GET /api/scenarios/1
GET /api/play-sessions/active?scenarioId=1
GET /api/play-sessions/<active-session>/dashboard
GET /api/play-sessions/<active-session>/evidences?includeLocked=true
GET /api/play-sessions/<active-session>/suspects
GET /api/play-sessions/<active-session>/locations

active_get_count = 1
create_post_count = 0
```

판정:

```text
local SharedPreferences가 비어 있는 fresh install/reinstall 유사 상태에서도,
server active session이 있으면 새 세션 생성 POST를 치지 않고 active session으로 복구한다.
따라서 6/10의 “세션 이미 있음 때문에 시작 전 차단” 버그는 현재 Flutter 코드 기준으로는 재현되지 않았다.
```

관련 frontend 검증:

```text
flutter test: PASS, 11 tests
flutter analyze: PASS, No issues
debug APK build with prod API: PASS
active-session regression tests: 추가됨
```

주의:

```text
prod API 대상 APK는 다시 빌드만 했고 실행하지 않았다.
앱 시작 시 FCM device-token 등록 API가 호출될 수 있으므로, 운영 API E2E는 QA 계정/승인 후 수행해야 한다.
```

## 6. Updated Status Against Previous QA

### 6.1 Resolved / Verified

| Item | Status | Evidence |
|---|---|---|
| Frontend server active session recovery | VERIFIED in current Flutter code | fake API emulator E2E, unit regression tests |
| Frontend static analysis noise | VERIFIED for current worktree | `flutter analyze` PASS |
| Frontend model/controller tests | VERIFIED | `flutter test` PASS, 11 tests |
| Backend final submission reachability after extended interrogation | VERIFIED | 두 시나리오 final submit + post-submit result/DB 확인 경로 도달. 정오/성공 여부는 private artifact |

### 6.2 Still Open

| Item | Priority | Note |
|---|---|---|
| API spoiler-like metadata exposure | P0/P1 | 6/10~6/12 반복 carry-over |
| Public DTO/spec/test에 남은 spoiler contract | P0/P1 | `PlayEvidenceResponse`, `PlaySuspectResponse`, API spec, `PlaySuspectCulpritEligibleTest`에서 확인 |
| Frontend metadata consumption | P0/P1 | `culpritEligible`/`importance`가 UI 후보 필터와 핵심 증거 계산에 사용됨 |
| 30~50턴 내 candidate narrowing | P1 | extended turns 후 가능하나 목표 턴 내 안정성 부족 |
| Guidance coverage | P1/P2 | 특히 시간 해금/분기 증거에서 부족 |
| AI response shape | P1/P2 | “인정/부인/다음 비교” 구조화 필요 |
| Frontend guidance/chip/timeline full E2E | P2 | 6/12는 active session 집중이라 full retest 아님 |
| DB/recorded interrogation count | P1 | final deduction latest rows에서 0 조회 |
| Ops privacy spot check | P1 | redacted log snippet 필요 |

## 7. Additional Whole-Project Review - 2026-06-12

문서 작성 후 backend/frontend 전체에서 QA 관련 키워드를 다시 훑었다. 아래 항목은 이전 QA에서 반복됐고, 이번 코드 검토로 확정도가 더 높아졌다.

| Priority | Issue | Additional Evidence | Why It Matters |
|---|---|---|---|
| P0/P1 | public gameplay spoiler metadata는 아직 계약화되어 있음 | `src/main/java/com/startup/domain/play/dto/PlayEvidenceResponse.java`의 `importance`, `PlayEvidenceDetailResponse.java`의 `importance`, `PlaySuspectResponse.java`의 `culpritEligible`, `docs/CaseLab_AI_API_Spec.md`의 gameplay response 예시, `src/test/java/com/startup/domain/play/service/PlaySuspectCulpritEligibleTest.java` | 6/10~6/12 관찰 이슈가 우발적 로그 노출이 아니라 DTO/API/test에 고정된 상태다. 수정 시 spec/test까지 같이 바꿔야 한다. |
| P0/P1 | frontend도 해당 metadata에 의존함 | `start-up-fe/lib/controllers/game_session_controller.dart`의 `accusableSuspects`, `importance == EvidenceImportance.core`, `start-up-fe/lib/screens/submit_screen.dart`, `start-up-fe/lib/screens/suspect_detail_bottom_bar.dart` | BE에서 필드를 제거하면 FE가 깨질 수 있고, FE를 그대로 두면 후보/핵심 증거 UX가 정답성 metadata에 계속 의존한다. |
| P1 | guidance 개선은 backend seed만으로 끝나지 않음 | frontend `play_evidence_models.dart`에는 guidance model이 없고, 6/11/6/12 frontend QA 모두 guidance rendering 미검증/미구현으로 기록 | 30~50턴 내 후보 축소 실패가 backend 데이터 coverage와 frontend 표시 미구현으로 중복 발생한다. |
| P1 | suggested question chip 자동 전송은 current code에도 남아 있음 | `start-up-fe/lib/screens/interrogation_chat_screen.dart`의 hardcoded `_suggestedQuestions`와 `QuestionType.recommended` 즉시 전송 | 사용자가 질문을 검토/수정하기 전에 AI 호출이 발생해 QA prompt의 prefill-only 기준을 계속 위반한다. |
| P1 | interrogation count 집계가 두 경로로 갈라질 수 있음 | `PlaySession.interrogationCount++`와 suspect별 log count 집계가 공존하고, post-submit DB 확인에서는 latest final deduction의 `interrogation_count=0` | 실제 QA 심문량과 결과/통계가 불일치하면 점수 분석, 운영 모니터링, 회귀 판단이 흔들린다. 저장 시점과 조회 join을 따로 검증해야 한다. |
| P2 | frontend timeline은 아직 sample/fallback 기반 | `start-up-fe/lib/screens/timeline_screen.dart`가 `sampleCase.timeline`을 사용하고 서버 timeline API 연동 TODO를 남김 | timeline 기반 추리 보조가 앱에서 작동하지 않아 공식 시나리오의 시간순 단서 파악이 약해진다. |
| P2 | prod APK 실행 자체가 운영 write가 될 수 있음 | fake API E2E 로그에서 앱 시작 시 `POST /api/device-tokens`가 호출됨. frontend `main.dart`, `login_screen.dart`에서도 확인 | 운영 QA는 “실행만 했다”가 read-only가 아니다. QA 계정/승인/로컬 fake API 정책을 명확히 해야 한다. |

강조 판단:

```text
active session recovery는 current Flutter code 기준으로 해결 확인.
하지만 전체 QA PASS는 아니다.
반복 핵심 이슈는 spoiler metadata contract, guidance 미구현/부족, chip 자동 전송, timeline 미연동, interrogation_count 불일치다.
특히 spoiler metadata는 backend와 frontend가 함께 바뀌어야 하므로 단일 레포 수정으로는 종료 처리하면 안 된다.
```

## 8. Recommended Follow-up

| Owner | Priority | Action |
|---|---|---|
| Backend | P0/P1 | public gameplay DTO에서 `importance`, 후보 가능성, 역할성 asset path 제거 또는 안전 변환. API spec/test에서 해당 public 노출 기대값도 함께 제거 |
| Frontend | P0/P1 | `culpritEligible`/`importance` 기반 후보 필터와 핵심 증거 계산 제거. 서버가 제공하는 public-safe UI state로 대체 |
| Backend | P1 | final deduction/result 조회의 interrogation count 집계 경로 재검증 |
| Backend/AI | P1 | evidence-presented 답변 responseShape 적용: 인정 사실, 모르는 범위, 다음 비교 대상 |
| Scenario seed | P1 | 시간 해금/분기 증거 guidance coverage 확대 |
| Frontend | P1 | current active recovery 구현 유지 및 회귀 테스트 CI 포함 |
| Frontend | P1 | hardcoded suggested question chip 즉시 전송을 prefill-only로 변경 |
| Frontend | P1 | evidence guidance model/rendering/navigation 구현 후 full E2E 재수행 |
| Frontend | P2 | timeline/final-submit/result full E2E 별도 수행 |
| Ops | P1 | AI_CALL/AI_CALL_CONTEXT redacted log snippet으로 privacy spot check 수행 |

## 9. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw session id
raw access token
정답 범인명
정답 수법/은폐/동기 원문
solution fullExplanation
점수/등급/부분정답 breakdown
제출 후보별 상세 반증 매트릭스
DB raw result dump
```

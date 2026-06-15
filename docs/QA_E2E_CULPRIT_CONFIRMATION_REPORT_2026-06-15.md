# ClueRoom E2E Culprit Confirmation QA Report - 2026-06-15

> 상태: 역사 QA 보고서다. 현재 QA 실행 지시, 보고서 템플릿, 오픈 이슈 board는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)를 따른다.

## 0. Scope

이 문서는 2026-06-15 Android emulator E2E 시도와 public API fallback extended QA 결과를 public-safe로 정리한 보고서다.

```text
Backend repo: C:\java\assignment\spring\start-up
Backend branch/commit: codex/backend-qa-reports-20260612 / 2ca9686
Frontend repo: C:\java\assignment\spring\start-up-fe
Frontend branch/commit: develop / 51a1ac8
API: https://api.clueroom.xyz
Device: Pixel_10 Android emulator, Android 17 API 37
Flutter/Dart: Flutter 3.44.0, Dart 3.12.0
Test date: 2026-06-15 KST
```

주의:

```text
정답 범인명, 정답 수법/동기/은폐 원문, private seed, raw session id, token 값은 이 문서에 기록하지 않는다.
결과 점수/등급/부분 정오답 breakdown, solution fullExplanation은 private artifact로 분리한다.
GET /api/play-sessions/{sessionId}/result 는 final-deduction 제출 후에만 호출했다.
```

## 1. Knowledge / Safety Declaration

아래 자료는 열람하지 않았다.

```text
private seed
solution/culprit/variant truth 문서
docs/OFFICIAL_SCENARIO_DEMO_DAY.md
기존 정답 해설
DB direct query
final-deduction 제출 전 result API
Swagger/API 문서의 정답 예시 섹션
```

허용한 범위:

```text
Android 앱 UI
운영 public API
앱 사용자 관점 public play API
코드상 UI 동작 확인
```

## 2. Final Judgment

```text
전체 판단: PARTIAL
Android UI E2E: FAIL - 운영 API 연결 앱에서 로그인 통과 불가
API-only gameplay: PASS for culprit confirmation, PARTIAL for new-user UX target
최종 제출: 서월채/스튜디오9 모두 완료
결과 화면/API: 제출 후 진입 확인
범인 확정: 두 시나리오 모두 결과 기준 범인 지목 성공 확인, 세부 정답성은 private note
30~50회 심문 내 후보 축소: 여전히 안정적으로 충족하지 못함
```

핵심 해석:

```text
두 공식 시나리오는 끝까지 풀 수 있다.
하지만 신규 유저가 앱과 AI 심문만으로 30~50회 안에 막히지 않고 범인을 확정하는 흐름은 아직 약하다.
범인 확정까지는 서월채 약 80턴, 스튜디오9 약 100턴의 extended interrogation이 필요했다.
```

## 3. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P0 | Android/Auth | 운영 API 연결 앱에서 로그인 통과 불가 | Android fresh install 후 Dev Login은 `AUTH_001 Dev login is disabled`, Google/Apple 로그인은 준비 중 | QA/신규 유저가 앱에서 시나리오 선택과 플레이 시작 가능 | 앱 UI E2E가 로그인 화면에서 차단됨 | 실제 앱 기준 E2E와 신규 유저 검증 불가 | 운영 QA용 인증 경로, staging/dev login, 또는 실제 OAuth wiring 제공 |
| P0/P1 | Auth/API contract | 앱은 로그인을 요구하지만 public play API는 인증 없이 세션 생성/진행 가능 | 앱 UI는 로그인 gate에서 차단, API fallback으로 scenario list/session/interrogation/final-deduction 가능 | 앱 UX와 API 인증 정책이 같은 사용자 모델을 공유 | UI와 API의 접근 정책이 다름 | QA 재현성과 운영 보안/계정 격리가 흐려짐 | 운영 API 인증 정책과 앱 gate 정책을 일치시키고 QA 계정 정책 문서화 |
| P0/P1 | Spoiler safety | public gameplay metadata 노출 문제가 반복됨 | locked evidence 응답에도 `importance`가 존재했고, FE가 `importance == CORE`를 핵심/분석 상태에 사용 | 플레이어용 응답은 정답성/중요도/비후보 추론 필드를 제거 또는 안전 변환 | API-only 또는 디버그 클라이언트에서 shortcut 가능 | blind QA validity 훼손, UI가 정답성 metadata에 의존 | public DTO/admin DTO 분리. locked evidence는 importance/proofDimensions/asset key 마스킹 |
| P1 | Frontend UX | hardcoded suggested question chip 자동 전송 문제가 반복됨 | 현재 FE 코드에서 기본 심문 chip tap이 `_sendMessage(...RECOMMENDED)`로 직접 연결 | 모든 추천 질문 chip은 prefill-only | guidance 경로는 prefill 구현이 있으나 기본 chip은 자동 전송 유지 | 사용자가 AI 호출 전 질문을 통제하지 못함 | hardcoded chip 제거 또는 입력창 prefill-only로 변경 |
| P1 | Candidate narrowing | 30~50턴 내 범인 확정 목표가 반복 미달 | 서월채는 50턴에서 확정 불가, 스튜디오9도 50턴에서 여러 축이 남음 | 30~50회 안에 후보를 1~2명으로 축소 | extended turns와 late evidence 후에야 submit-ready | 신규 유저가 찍기 제출 또는 이탈할 가능성 | 결정적 비교 증거 guidance를 더 빠르게 노출하고 next compare를 명시 |
| P1 | Scenario/Guidance | late evidence가 있어야 추리 축이 닫힘 | 서월채는 마지막 증거 해금 후 물병/바이탈 축이 정리됨. 스튜디오9는 전체 증거 해금 후에도 추가 교차 심문 필요 | 핵심 비교가 30~50턴 안에 앱에서 자연스럽게 제시 | 사용자가 직접 긴 비교 그래프를 구성해야 함 | 플레이 감각이 "기다리고 많이 묻기"에 가까움 | phase/unlock 설계와 guidance priority 재조정 |
| P2 | Scoring alignment | 범인 지목 성공과 세부 서술 채점 사이 간극 발생 | 두 시나리오 모두 범인 지목은 성공했으나 일부 세부 추론 채점은 private note 기준 불일치 | guidance/AI 답변이 최종 제출 서술 기준까지 이어짐 | 범인은 맞히지만 세부 정답 문장과 맞추기 어려움 | 결과 UX에서 사용자가 납득하기 어려울 수 있음 | final-deduction rubric과 in-game guidance 용어를 맞추고 public-safe feedback 개선 |
| P2 | Docs drift | 프론트 문서 일부가 현재 코드 상태와 다름 | 기존 FE 문서에는 guidance/timeline 미구현으로 남아 있으나 현재 코드는 모델/렌더링/timeline API가 일부 구현됨 | 문서가 코드 SoT를 반영 | QA 기준 문서와 실제 코드가 어긋남 | 리뷰/QA 우선순위 혼선 | FE 구현 상태 문서와 drift 문서 갱신 |

## 4. Repeated Issues 강조

아래 항목은 2026-06-10~2026-06-12 QA에서 이미 지적됐고, 2026-06-15에도 다시 확인됐다.

| Repeated Issue | 이번 재확인 | 현재 판단 |
|---|---|---|
| public gameplay spoiler-like metadata | locked evidence에도 `importance` field 존재. FE는 importance를 핵심/분석 UI에 사용 | 단순 표시 문제가 아니라 BE/FE contract 문제 |
| 30~50회 안 후보 축소 실패 | extended interrogation 전에는 두 시나리오 모두 확정 어려움 | QA 목표 미달. "풀 수 있음"과 "적정 턴 내 풀림"은 분리해야 함 |
| suggested question prefill-only 위반 | guidance route는 개선됐으나 기본 심문 chip은 자동 전송 유지 | 부분 해결 상태. default chip 제거/수정 필요 |
| guidance가 결정적 비교를 늦게 제공 | late evidence 이후에야 후보 축이 닫힘 | seed/guidance 우선순위 재조정 필요 |
| 운영/QA 계정 격리 부족 | 운영 API public fallback으로 세션 생성/진행 가능 | 인증 정책과 QA 계정 운영 기준 필요 |

## 5. 신규 발견 / 이번에 더 선명해진 문제

| Priority | New / Sharpened Finding | Detail | Recommended Action |
|---|---|---|---|
| P0 | 운영 앱 로그인 blocker | Dev Login disabled + OAuth disabled라 실제 앱 신규 유저 흐름이 시작 전 차단됨 | QA/staging auth route 제공 전까지 Android E2E PASS 불가로 간주 |
| P0/P1 | 앱 로그인 gate와 public play API 인증 정책 불일치 | 앱은 로그인을 요구하지만 API-only로 세션 생성, 심문, 최종 제출까지 가능 | backend auth enforcement, mock user policy, FE login gate 정책을 한 문서로 정리 |
| P2 | 범인 성공과 세부 추론 채점 간극 | 범인 지목은 성공했지만 일부 세부 서술은 채점 기준과 어긋남 | result feedback과 in-game guidance가 같은 rubric 언어를 쓰도록 조정 |
| P2 | FE 문서가 current code를 따라가지 못함 | guidance/timeline이 일부 구현됐는데 기존 drift/status 문서에는 미구현으로 남아 있음 | FE docs를 코드 기준으로 갱신 |

## 6. Scenario Result Summary

| Scenario | Android UI | API Basic Flow | Evidence Unlock | Candidate Narrowing | Final Submit | Result | Overall |
|---|---|---|---|---|---|---|---|
| 서월채 | FAIL at login | PASS | 25/25 reached | 30~50 FAIL, extended PASS | 완료 | 제출 후 확인 | PARTIAL |
| 스튜디오9 | FAIL at login | PASS | 35/35 reached | 30~50 PARTIAL/FAIL, extended PASS | 완료 | 제출 후 확인 | PARTIAL |

## 7. Scenario A - 서월채

### 7.1 Flow Result

```text
sessionId: <redacted>
Android app flow: login blocker로 시나리오 목록 진입 불가
API fallback basic flow: PASS
evidence unlock: 25/25
turns used before final submit: 약 80
final submit: 완료
result screen/API: final-deduction 제출 후 확인
correctness detail: private note
```

### 7.2 Deduction Path

```text
초반:
  회의/동기/기본 동선은 넓게 열림.

중반:
  물병, 약통, 와인, 보안, 케어 기록 축이 병렬로 남음.
  50턴 시점에는 최종 후보 확정 기준을 충족하지 못함.

후반:
  late evidence가 위험 징후 시간대와 특정 섭취 경로를 더 잘 설명하면서 후보 축이 좁아짐.
  같은 증거를 여러 용의자에게 제시해 역할별 반응 차이를 비교한 뒤 submit-ready.
```

### 7.3 Public-Safe Finding

```text
서월채는 guidance/AI 답변만으로 "어느 경로가 시간상 더 자연스러운지"를 50턴 안에 닫기 어렵다.
마지막 증거와 후속 질문 이후에는 논리가 수렴하므로 시나리오 자체가 불가능한 것은 아니다.
문제는 결정적 비교가 늦게 열리고, 앱이 그 비교 순서를 충분히 압축해 주지 못하는 점이다.
```

## 8. Scenario B - 스튜디오9

### 8.1 Flow Result

```text
sessionId: <redacted>
Android app flow: login blocker로 시나리오 목록 진입 불가
API fallback basic flow: PASS
evidence unlock: 35/35
turns used before final submit: 약 100
final submit: 완료
result screen/API: final-deduction 제출 후 확인
correctness detail: private note
```

### 8.2 Deduction Path

```text
초반:
  위치 표시, 프롬프터, 조명, 안전장치, 제작비, 사후 자료 정리 축이 동시에 열림.

중반:
  전체 증거가 열려도 여러 조건이 병렬로 남아 50턴 안에는 핵심 실행 축과 보조 조건을 구분하기 어려움.

후반:
  추락을 실제로 가능하게 만든 필수 실패 조건과 주변 유도/은폐 조건을 분리하면서 후보가 수렴함.
  public-safe 관점에서는 "위험 조건"과 "최종 실패 조건"을 분리하는 guidance가 더 필요하다.
```

### 8.3 Public-Safe Finding

```text
스튜디오9는 단서가 풍부하지만, 단서 축이 많아 사용자가 직접 비교 그래프를 관리해야 한다.
AI는 정답을 직접 말하지 않고 안전하게 답하지만, "이 증거는 보조 조건이고 저 증거는 필수 조건" 같은 구조화 안내가 부족하다.
결과적으로 범인 확정은 가능하나 목표 턴 수를 크게 넘긴다.
```

## 9. Backend / API Notes

```text
PASS:
- scenario list/detail
- active session lookup
- play session create
- dashboard/locations/evidences/suspects/timeline
- evidence detail guidance
- interrogation
- final-deduction
- post-submit result

OPEN:
- public DTO spoiler-like metadata
- auth policy mismatch between app and API
- 30~50턴 내 추리 수렴 부족
- final result 세부 서술 채점과 in-game guidance 간극
```

## 10. Frontend Notes

```text
PASS/PARTIAL:
- guidance model/rendering code는 현재 일부 구현되어 있음
- evidence-detail guidance question path는 prefill-only 형태가 코드상 존재
- timeline API 연동도 현재 코드에 존재

FAIL/OPEN:
- 운영 앱 로그인 blocker
- default interrogation suggested question chip 자동 전송
- locked evidence importance 기반 핵심/분석 UI 추론 가능성
- current docs가 구현 상태를 일부 오래된 기준으로 설명
```

## 11. Recommended Follow-up

| Owner | Priority | Action |
|---|---|---|
| Backend | P0/P1 | 운영 auth 정책 정리. public play API를 인증 필요로 할지, mock/public gameplay를 유지할지 결정 |
| Backend | P0/P1 | public gameplay DTO에서 locked/public metadata redaction 적용 |
| Frontend | P0 | 운영 QA/신규 유저가 통과 가능한 로그인 경로 제공 |
| Frontend | P1 | hardcoded suggested question chip 자동 전송 제거 |
| Frontend | P1 | locked evidence에서는 importance 기반 핵심/분석 상태와 필터 매핑 금지 |
| Scenario seed | P1 | 30~50턴 안에 결정적 비교가 열리도록 late evidence guidance와 unlock priority 조정 |
| AI policy | P1 | 답변에 "인정 사실 / 모르는 범위 / 다음 비교 대상" 구조를 더 일관되게 적용 |
| Product/Backend | P2 | final-deduction 세부 rubric과 in-game guidance 문구 정렬 |
| Docs | P2 | backend/frontend QA 보고서와 FE implementation/drift 문서 갱신 |

## 12. Verification

```text
flutter build apk --debug --dart-define=API_BASE_URL=https://api.clueroom.xyz: PASS
flutter test: PASS
flutter analyze: FAIL, 기존 lint/info 4건
backend/frontend git status before docs: backend clean, frontend had pre-existing pubspec changes
destructive command: 실행하지 않음
result API: final-deduction 제출 후에만 호출
```

## 13. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw session id
raw access token
정답 범인명
정답 수법/동기/은폐 원문
solution fullExplanation
점수/등급/부분 정오답 breakdown
제출 후보별 상세 반증 매트릭스
AI 답변 전체 원문
```

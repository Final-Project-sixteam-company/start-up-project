# ClueRoom Web E2E QA Report - 2026-06-19

> 상태: 역사 QA 보고서다. 현재 QA 실행 지시, 보고서 템플릿, 오픈 이슈 board는 [QA_OPERATING_GUIDE.md](../../QA_OPERATING_GUIDE.md)를 따른다.

## 0. Scope

이 문서는 2026-06-19 KST에 수행한 ClueRoom React/Vite 웹 프론트 기준 E2E QA 결과를 public-safe로 정리한다. Android 앱 배포 대신 웹 배포로 전환하는 현재 상황에서, 실제 웹 사용자가 공식 시나리오를 시작하고 심문, 증거 확인, 최종 제출/result 경로까지 도달할 수 있는지 확인하는 데 초점을 둔다.

```text
Backend repo: C:\java\assignment\spring\start-up
Backend branch/commit: codex/llmops-cost-rate-limit-plan / c86fe98
Web frontend repo: local React/Vite web checkout
Web frontend branch/commit: main / 33618a3
API: https://api.clueroom.xyz
Web surface: Vite production build served by local preview http://127.0.0.1:4173
Browser: Playwright Chromium, desktop and mobile viewport
Desktop viewport: 1365x900
Mobile viewport: 390x844
Test date: 2026-06-19 KST
QA login route: web Login screen QA button
```

주의:

```text
정답 범인명, 선택 후보명, 정답 수법/동기/은폐 원문, private seed, raw session id, token 값은 이 문서에 기록하지 않는다.
결과 점수/등급/부분 정오답 breakdown, feedback/detail explanation, raw result는 private artifact로 분리한다.
GET /api/play-sessions/{sessionId}/result 는 final-deduction 제출 후에만 호출했다.
이번 문서는 기존 QA guide와 과거 보고서를 참고한 operator web QA이며, strict blind retest로 보지 않는다.
```

## 1. Source Documents Reviewed

| Source | 반영 내용 |
|---|---|
| `docs/QA_OPERATING_GUIDE.md` | current QA board, public-safe report rule, final result privacy rule |
| `docs/QA_BLIND_RETEST_PROMPT_2026-06-11.md` | blind QA prompt가 현재 guide로 흡수됐음을 확인 |
| `docs/qa/archive/QA_E2E_CULPRIT_CONFIRMATION_REPORT_2026-06-15.md` | final submit/result reachability와 private artifact 분리 방식 |
| `docs/qa/archive/QA_ANDROID_E2E_LOCAL_RETEST_REPORT_2026-06-18.md` | 최근 Android E2E 보고서 형식과 public-safe 문장 구조 |

## 2. Safety Declaration

```text
Private seed/solution opened: no
DB direct query: no
Server file grep for solution/culprit: no
Result API before final deduction: no
Raw token/session id recorded in report: no
QA account email recorded in report: no
Raw user question/AI answer transcript recorded in report: no
Result score/grade/correctness/breakdown kept private: yes
Rejected-candidate rationale kept private: yes
Production infra/config modification: no
Production API writes: yes, QA account scoped
Production API write approval: yes, operator-approved web E2E QA
Privacy spot check: API/browser output only, no ops log check
```

Blind validity는 제한된다. 이번 실행은 "정답을 전혀 모르는 신규 유저성"보다 "웹 배포 표면이 실제로 작동하는지"를 확인하는 QA다. 따라서 후보 축소 판단보다 웹 기능 완성도, 모바일 안정성, 최종 제출/result reachability, public-safe 응답 형태를 우선으로 봤다.

## 3. Final Judgment

```text
전체 판단: PARTIAL/PASS
Web E2E reachability: PASS for login, library, scenario detail, session start, investigation tabs, evidence/suspect/timeline, interrogation, and Studio9 final-submit/result path
Desktop official scenarios: PASS for broad exploration on Studio9 and Seowolchae
Mobile web: PARTIAL, Seowolchae evidence list/bookmark/image issues observed
Final submit/result:
  - Studio9: result path reached after final-deduction; detailed judgement kept private
  - Seowolchae: desktop broad QA completed, but final "until correct" loop was not performed
Web deployment readiness: PARTIAL, P1 web UX issues should be fixed before calling it release-ready
```

핵심 해석:

```text
웹은 백엔드 API와 연결되어 공식 시나리오 진입, 심문, 타임라인, 최종 제출/result 경로까지 동작한다.
Android 앱 대비 웹은 배포 가능한 표면에 가까워졌지만, 모바일 웹 안정성, 증거 상세 guidance 표시, 이미지 렌더링, 북마크 상태 동기화가 아직 부족하다.
특히 부트캠프 제출물이 웹 기준이라면 "작동한다"보다 "모바일에서 막히지 않고 완성돼 보인다"가 더 중요하다.
```

## 4. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P1 | Web evidence UX | 증거 상세에서 guidance/compare/suggested question 블록이 보이지 않는다 | desktop Studio9/Seowolchae inspected evidence detail에서 `guidanceEvidenceCount=0`, `suggestedQuestionCount=0` 관찰. mobile Studio9에서도 동일 계열 신호 | 증거 상세는 판독 포인트, 같이 볼 증거, 다음 질문을 제공해야 함 | 증거명/기본 정보는 보이나 다음 조사 방향이 부족함 | 신규 유저가 증거를 보고도 어떤 용의자에게 무엇을 물어야 할지 막힘 | backend detail DTO의 guidance 필드, web `GuidanceBlock` 렌더링, seed data import 상태를 함께 확인 |
| P1 | Mobile web evidence list | Seowolchae 모바일 증거 카드 클릭이 불안정하다 | 390x844 viewport에서 evidence card click 중 `Element is not attached to the DOM` 발생 | 모바일에서 증거 목록 스크롤과 카드 클릭이 안정적으로 유지 | 클릭 대상 DOM이 재렌더링되며 테스트가 중단됨 | 실제 모바일 유저가 증거 상세 진입 중 흐름을 잃을 수 있음 | evidence list item 높이/키 안정화, 이미지 lazy-load와 re-render 영향 점검, mobile Playwright regression 추가 |
| P1 | Bookmark contract/state | 모바일 Seowolchae 북마크 토글에서 409가 발생했다 | `POST /api/scenarios/{id}/bookmarks -> 409` 관찰 | 상세 진입 시 서버 bookmark 상태가 UI에 반영되고 중복 저장은 사용자 오류로 보이지 않아야 함 | UI는 저장 가능 상태처럼 보였으나 서버는 conflict 응답 | 저장 상태 신뢰도가 떨어지고, QA/사용자에게 실패처럼 보임 | detail load 후 bookmark 상태 재동기화, idempotent 409 처리, stale button disable 또는 optimistic rollback |
| P2 | Scenario assets | Seowolchae hero/map 이미지가 웹에서 실패했다 | desktop Seowolchae hero/map image fail, mobile Seowolchae map image fail 관찰 | 공식 시나리오의 대표 이미지와 지도 이미지는 데스크톱/모바일 모두 표시 | 일부 이미지가 깨지거나 로딩 실패 | 웹 완성도와 현장 조사 몰입감이 낮아짐 | S3/public URL, CORS/cache, `thumbnailUrl`/`coverImageUrl`/`mapImageUrl` 매핑과 fallback UI 확인 |
| P2 | Final deduction UI | Studio9 final form의 evidence chip 선택은 추가 수동 검증이 필요하다 | 자동화 pointer click에서 selected count가 기대대로 갱신되지 않아 DOM click로 제출 경로를 확인함 | 증거 chip 클릭이 명확히 선택 상태를 토글하고 제출 가능 상태를 만든다 | 제출/result reachability는 확인했지만 pointer hit target 회귀 가능성이 남음 | 최종 제출 직전 사용자가 증거 선택에서 막힐 수 있음 | hit target, overlay, disabled state, selected count 갱신을 수동/자동 regression으로 재검증 |

## 5. Flow Summary

| Scenario | Surface | Session | Guidance | Chip | AI quality | Candidate narrowing | Final submit |
|---|---|---|---|---|---|---|---|
| Studio9 | Web desktop | fresh QA login session | evidence detail guidance not visible in inspected surfaces | manual question send route usable; full chip matrix pending | PASS for shape checks | diagnostic only, strict blind limited | result path reached after final-deduction; detailed judgement private |
| Seowolchae | Web desktop | fresh QA login session | evidence detail guidance not visible in inspected surfaces | manual question send route usable; full chip matrix pending | PASS for shape checks | diagnostic only, strict blind limited | not repeated until correct in this run |
| Studio9 | Web mobile | fresh QA login session | guidance block not confirmed | basic suspect/interrogation smoke PASS | PASS on sampled turns | not assessed | not assessed |
| Seowolchae | Web mobile | fresh QA login session | blocked by evidence list instability | not fully assessed | not fully assessed | not assessed | not assessed |

## 6. Web Surface Checked

```text
Common:
  - QA login button visible and successful on production preview
  - home/profile/bookmarks/records basic navigation checked
  - library search/filter checked
  - scenario card/detail checked
  - scenario start/resume checked
  - investigation screen checked
  - evidence/suspect/timeline/final tabs checked
  - suspect detail and interrogation screen checked
  - manual question send and answer receive checked
  - final submit/result path checked for Studio9

Build:
  - npm run lint: PASS
  - npm run build: PASS
  - Vite production preview boot: PASS
  - temporary QA specs and local preview process removed after run
```

## 7. Scenario A - Studio9

### 7.1 Flow Result

```text
sessionId: <redacted>
Web desktop flow: PASS through login, library, detail, investigation tabs, evidence, suspect, timeline, interrogation, final submit, result
evidence list count: 35
observed unlocked evidence count at inspected point: 8
suspect count: 7
timeline count: 11
interrogated suspects: 7/7
interrogation turns: 28
API result: no 4xx observed in desktop deep run
final submit: completed
result screen/API: reached after final-deduction
correctness detail: private artifact
```

### 7.2 Public-Safe Finding

```text
Studio9는 웹 desktop 기준으로 공식 시나리오 탐색과 최종 제출/result 경로가 이어진다.
심문 답변은 public-safe 형태 검사에서 과도한 장문이나 기술 metadata 노출 없이 동작했다.
다만 증거 상세 guidance가 보이지 않아 단서가 풍부한 이 시나리오에서 플레이어가 다음 비교 대상을 직접 추적해야 한다.
```

## 8. Scenario B - Seowolchae

### 8.1 Flow Result

```text
sessionId: <redacted>
Web desktop flow: PASS through login, library, detail, investigation tabs, evidence, suspect, timeline, interrogation
evidence list count: 25
observed unlocked evidence count at inspected point: 7
suspect count: 5
timeline count: 11
interrogated suspects: 5/5
interrogation turns: 20
API result: no 4xx observed in desktop deep run
final submit: not repeated until correct in this run
correctness detail: not applicable for this report
```

### 8.2 Mobile Finding

```text
Seowolchae는 desktop에서는 broad QA가 통과했지만, mobile web에서 이미지와 증거 목록 안정성 문제가 더 선명했다.
390x844 viewport에서 map image fail, evidence card DOM detach, bookmark POST 409가 관찰됐다.
웹 배포가 실제 모바일 브라우저 유입을 전제로 한다면, Seowolchae mobile flow는 release-ready로 보기 어렵다.
```

## 9. AI / Interrogation Quality

```text
Desktop deep QA total:
  - official scenarios: 2
  - total suspects interrogated: 12
  - total interrogation turns: 48
  - Studio9: 7 suspects, 28 turns
  - Seowolchae: 5 suspects, 20 turns

Observed:
  - no 4xx API in desktop deep run
  - no two-sentence policy violation observed in sampled AI answers
  - no technical metadata leak observed by regex spot check
  - no raw solution/culprit/private seed was used for gameplay decisions
```

이번 검사는 답변 shape와 웹 통합 안정성 중심이다. 답변이 실제로 후보 축소에 충분히 기여하는지는 strict blind retest로 다시 봐야 한다.

## 10. Web Deployment Gaps

앱 기준 완성도와 비교했을 때, 웹 배포 전환에서 특히 부족한 부분은 아래다.

| Priority | Gap | 왜 중요한가 | Recommended Action |
|---|---|---|---|
| P1 | 모바일 증거 목록 안정성 | 웹 배포의 실제 사용자 표면은 모바일 브라우저일 가능성이 높음 | Seowolchae mobile evidence list를 우선 고정하고 regression test 추가 |
| P1 | 증거 상세 guidance 표시 | 추리 게임의 핵심은 "다음에 무엇을 비교할지"를 플레이어가 이해하는 것 | guidance DTO와 web 렌더링 연결 확인 |
| P1 | 북마크 상태 동기화 | 저장/해제 같은 기본 UX가 409로 보이면 서비스 완성도가 낮아 보임 | detail load state, optimistic update, conflict 처리 정리 |
| P2 | 시나리오 이미지 렌더링 | 첫인상과 현장 조사 맥락에 직접 영향 | 이미지 URL, fallback, CORS/cache 점검 |
| P2 | 최종 제출 form 신뢰도 | 최종 추리 제출은 핵심 conversion 지점 | evidence chip hit target과 selected count 갱신을 수동/자동 모두 재검증 |
| P2 | 실제 OAuth web login 미검증 | QA button은 제출용 검증에는 충분하지만 실제 사용자 인증과 다름 | 운영 웹 OAuth route를 별도 smoke로 분리 |
| P2 | 시각 회귀 기준 부족 | 반응형 UI의 깨짐은 API 테스트로 잡히지 않음 | desktop/mobile screenshot baseline과 주요 화면 visual checklist 추가 |

## 11. Verification

```text
Frontend checks:
  - npm run lint: PASS
  - npm run build: PASS
  - production preview: PASS
  - frontend git status after cleanup: clean

Web E2E checks:
  - QA login: PASS
  - library/search/filter: PASS
  - scenario detail/start/resume: PASS
  - desktop evidence/suspect/timeline: PASS/PARTIAL due to missing guidance display
  - desktop interrogation: PASS for API and answer shape
  - Studio9 final-deduction API: 200 after web submit route
  - Studio9 result API/screen: reached after final-deduction
  - Seowolchae mobile evidence: FAIL/PARTIAL due to DOM detach
  - Seowolchae mobile bookmark: FAIL/PARTIAL due to 409
  - Seowolchae image render: FAIL/PARTIAL for map/hero image
```

## 12. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Frontend | P1 | Evidence detail guidance, compare evidence, suggested question rendering을 web에서 확인하고 누락 원인 수정 |
| Frontend | P1 | Seowolchae mobile evidence card click 안정화. list key, layout shift, lazy image, route transition 점검 |
| Frontend/Backend | P1 | Bookmark state contract 정리. 409를 사용자 실패로 보이지 않게 처리하고 server state를 detail 진입 시 반영 |
| Frontend/Assets | P2 | Seowolchae hero/map image URL과 fallback 렌더링 점검 |
| Frontend/QA | P2 | Final deduction form의 suspect 선택, evidence multi-select, selected count, submit button enablement regression 추가 |
| Frontend/QA | P2 | Web Playwright regression을 login -> library -> detail -> evidence -> suspect -> interrogation -> final submit -> redacted result 경로로 고정 |
| Product/QA | P2 | 웹 배포 기준 QA checklist를 Android 앱 checklist와 분리. 모바일 브라우저, 이미지, OAuth, 북마크, final form을 release gate로 추가 |

## 13. Retest Addendum - 2026-06-19 Web Fix Verification

사용자가 웹 프론트 수정 후 재검을 요청해, 같은 날짜에 production build + local preview + Playwright Chrome으로 이전 지적 항목과 추가 수정 항목을 재확인했다.

```text
Frontend checks:
  - npm run lint: PASS
  - npm run build: PASS
  - Vite production preview: PASS
  - frontend git status after retest: clean
```

| Area | Result | Evidence | Note |
|---|---|---|---|
| Evidence guidance fallback | PASS | 증거 상세에서 `이 증거에서 볼 점`, `심문에 활용할 질문` 표시 확인 | seed/API guidance가 없어도 fallback 조사 가이드가 표시됨 |
| Guidance question behavior | PASS | 질문 chip 클릭 후 textarea prefill, interrogation POST delta 0 | 자동 AI 호출 없이 prefill-only 동작 |
| Bookmark 409 handling | PASS | injected `POST /bookmarks -> 409` 후 UI `저장됨`, error text 없음 | 이미 저장됨을 성공 처리 |
| Mobile evidence card click | PASS | 390x844 viewport에서 Seowolchae evidence card 일반 클릭 후 evidence detail 진입 | 이전 DOM detach 재현 안 됨 |
| Final deduction validation/chip | PASS | 4자 입력은 submit disabled, 5자 입력은 enabled, evidence chip `aria-pressed=true`, `제출 증거 1/15` 표시 | hit target과 5자 validation 동작 |
| Image fallback | PASS | scenario hero와 scene map 이미지 실패 주입 시 `CL-*`, `MAP` fallback 표시 | broken image 대신 fallback 표시 |
| Timeline filters | PASS | 전체/모순/진술 filter `aria-pressed=true` 전환 확인 | 필터 전환 중 UI/API 오류 없음 |
| Review POST/rating | PASS | review POST 발생, payload rating integer 확인 | 실제 review 생성은 route mock으로 차단 |
| Result fallback | PASS | injected final-deduction 409 후 result GET mock으로 `수사 결과` 화면 진입 | 실제 정답/점수값은 사용하지 않음 |
| Closed session guard | PASS | dashboard status completed 주입 시 `이미 종결된 사건입니다`, `결과 보기` 표시 | 제출 완료 세션 조작 guard 동작 |
| My scenario placeholder | PASS | `내 시나리오` filter에서 제작 시나리오 없음 placeholder 표시 | 웹 미지원 상태 설명 표시 |
| Library load more | N/A | 현재 운영 목록에서 `더보기` 버튼 미노출 | next page가 노출되는 데이터셋에서 별도 확인 필요 |
| HttpOnly refresh cookie | PASS | 운영 OAuth 로그인 후 `clueroom_refresh_token` cookie 저장, HttpOnly/Secure 적용, localStorage refresh token 부재 확인 | cookie 값, access token 원문은 문서에 기록하지 않음 |

추가로 발견된 이슈:

| Priority | Area | Finding | Evidence | Impact | Recommended Action |
|---|---|---|---|---|---|
| P1 | Mobile scenario detail | 모바일 390x844에서 Seowolchae 상세의 `수사 시작` 일반 클릭이 review empty 영역에 의해 intercept됨 | Playwright normal click timeout. 강제 클릭으로 우회하면 briefing/case 진입 가능 | 모바일 사용자가 시나리오 상세에서 시작 버튼을 눌러도 반응하지 않을 수 있음 | scenario detail 하단 CTA와 review empty 영역의 stacking/spacing/pointer-events를 점검하고 mobile click regression 추가 |

Unexpected API note:

```text
Fresh browser bootstrap 중 /api/auth/refresh 401 1건이 관찰됐다.
저장된 refresh cookie/token이 없는 첫 진입에서 발생한 auth bootstrap 실패이며, 이후 QA login과 gameplay 흐름은 통과했다.
```

## 14. Private Artifact Notice

아래 항목은 이 public report에 포함하지 않았다.

```text
raw session id
raw access token
QA account email
선택 후보명
정답 범인명
정답 수법/동기/은폐 원문
final deduction raw payload
result score
result grade
result correctness/breakdown
result feedback/detail explanation
raw AI answer transcript
```

Public report에는 result screen/API 도달 여부와 웹 기능 결함만 남긴다. 최종 정오답 세부 판단은 운영 가이드 기준에 따라 비공개로 유지한다.

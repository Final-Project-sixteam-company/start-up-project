# ClueRoom Web Production PR #7 Smoke QA Report - 2026-06-20

> 상태: `clueroom-web-fe` PR #7 merge 후 최신 운영 배포본 smoke QA 결과다. public-safe 규칙에 따라 raw token, raw session id, QA 계정 이메일, 정답/범인/점수 breakdown은 기록하지 않는다.

## 0. Scope

```text
Web: https://www.clueroom.xyz
API: https://api.clueroom.xyz
Build basis: latest production deploy after PR #7
Browser: Chromium 1228 via Playwright
Viewport: desktop 1365x900, mobile 390x844
Execution time: 2026-06-20 13:05-13:35 KST
Login method: explicit QA login URL, QA account redacted
Production API writes: yes
Final deduction submitted: no
```

Production API writes were performed under the operator-provided QA account policy:

```text
Auth/dev QA login, bookmark add/remove, review POST attempt, play-session start/recover, and two interrogation POST calls.
The review POST was rejected by server policy because the QA account has no completed case.
No final-deduction POST was sent.
```

## 1. Final Judgment

```text
Overall: PARTIAL
PR #7 target fixes: PASS for public QA-login hiding, bookmark server persistence, saved-cases list reflection, core play smoke
Review regression: PARTIAL because review success path requires completed play history and final submit was explicitly forbidden
Auth/session: PASS for reload persistence and no refresh token in localStorage; access-expiry refresh retry remains unconfirmed
```

## 2. Smoke Summary

| Area | Result | Evidence |
|---|---|---|
| Public QA login exposure | PASS | Normal `/` login screen showed Google/Kakao only. No QA test login button. |
| Google/Kakao buttons | PASS | Both visible on public login screen. |
| Toss/App-in-Toss exposure | PASS | No related text/button/path visible. |
| QA login | PASS | Explicit QA login URL worked. |
| Reload session persistence | PASS | After refresh, logged-in home/library state remained. |
| Refresh token localStorage | PASS | `refreshToken` and `clueroom.refreshToken` absent. |
| Access-expiry refresh retry | PARTIAL | Not waited through expiry; only storage/cookie-adjacent behavior observed. |
| Library list/search/sort/filter | PASS | 2 scenarios loaded; search/filter/sort did not break layout. |
| Bookmark add/list/detail/remove | PASS | Save reflected in saved-cases list, detail stayed saved, remove changed list to 0. |
| Review rating integer | PASS | Rating input `min=1`, `max=5`, `step=1`. |
| Review submit/visibility | PARTIAL | POST attempted, but server returned completion-required policy. No final submit performed. |
| Detail -> briefing -> case | PASS | Scenario detail, briefing, and case screen entered. |
| Mobile start CTA | PASS | 390x844 detail CTA visible and clickable, briefing entered. |
| Case tabs | PASS | Scene/evidence/suspects/timeline/submit tabs entered. |
| Timeline overlap | PASS | Observed desktop timeline showed no time/title overlap. |
| Suspect detail | PASS | Suspect detail entered from suspect list. |
| Interrogation screen | PASS | Chat screen entered from suspect detail. |
| Recommended chips | PASS | Chip filled input only; interrogation POST count delta 0. |
| Direct question | PASS | One direct question returned an AI response. |
| Evidence-presented question | PASS | Evidence picker opened and evidence-presented question returned a response. |
| AI quota UX | PASS/PARTIAL | Responses contained `aiQuota.stage=NONE`, remaining and nextThreshold fields; no banner expected. Non-NONE missing-message fallback unconfirmed. |
| Final deduction validation | PASS | Initial/short input disabled; after culprit, 5+ char fields, and 1 evidence, submit button enabled. Actual submit not clicked. |
| Layout/text overlap | PASS | No `undefined/null/NaN`; mobile/detail/list/CTA/review areas did not show obvious overlap in observed states. |

## 3. Findings

| Priority | Area | Finding | Repro Steps | Actual | Expected | Screenshot |
|---|---|---|---|---|---|---|
| P3 | Review QA coverage | Review success-path persistence could not be verified with the provided QA account because the account has 0 completed cases and final submit was prohibited. | Login with QA account -> scenario detail -> review write -> rating 5 -> body input -> submit | Server rejected with completion-required policy; review list persistence could not be checked. | Use a QA account/session with a completed case, or approve a dedicated test final submit in a private-safe session. | no |

No P1/P2 regression was found in this PR #7 smoke.

## 4. Flow Notes

### Public Login

```text
PASS:
  - normal public login screen no longer exposes QA test login
  - Google and Kakao buttons are visible
  - miniapp-specific login copy is not visible

NOTE:
  - explicit QA login URL still exposes QA form, as documented for controlled QA access
```

### Bookmark

```text
PASS:
  - scenario saved from detail
  - saved-cases screen immediately showed 1 saved scenario
  - saved scenario detail showed saved state as "저장 해제" with pressed state
  - unsave from detail returned saved-cases list to 0
```

### Review

```text
PASS:
  - review entry opens
  - rating control is integer 1-5
  - submit button enables after body input

PARTIAL:
  - server policy blocked actual review creation because this QA account has no completed case
```

### Play

```text
PASS:
  - desktop and mobile detail CTA
  - briefing and active session recovery/start
  - all case tabs
  - suspect detail
  - interrogation direct question and evidence-presented question
  - final deduction validation only

Not performed:
  - final deduction submit
  - result correctness/score/grade check
```

## 5. Private Artifact Notice

This report intentionally omits:

```text
QA account email
raw access token
raw refresh cookie
raw session id
selected culprit treated as a submitted answer
final deduction payload
result score/grade/correctness
solution/full explanation
AI raw transcript beyond behavior-level summary
```

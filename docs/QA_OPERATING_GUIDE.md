# ClueRoom QA Operating Guide

> 상태: QA 실행 지시, public-safe 보고서 형식, 현재 QA 이슈 추적의 정본 문서다.
> `QA_HANDOFF.md`, `QA_BLIND_RETEST_PROMPT_2026-06-11.md`, 2026-06-10~2026-06-15 QA 보고서의 반복되는 실행 규칙과 후속 액션을 이 문서에 흡수했다.

## 0. 목적

이 문서는 새 QA를 시작할 때 보는 단일 진입점이다.

```text
1. 어떤 환경에서 QA를 돌릴지 결정한다.
2. blind 조건과 public/private 기록 경계를 지킨다.
3. 결과 보고서는 같은 형식으로 작성한다.
4. 날짜별 보고서의 반복 이슈는 이 문서의 Current QA Board로 흡수한다.
```

날짜별 QA 보고서는 역사 기록이다. 현재 실행 기준이나 오픈 이슈 판단은 이 문서를 우선한다.

## 1. 흡수한 문서

| 문서 | 현재 역할 |
|---|---|
| `QA_HANDOFF.md` | 이 문서로 흡수. 기존 경로 호환용 안내 문서로 유지 |
| `QA_BLIND_RETEST_PROMPT_2026-06-11.md` | 이 문서의 "복붙용 QA 프롬프트"로 흡수. 기존 경로 호환용 안내 문서로 유지 |
| `MVP_PLAY_FLOW_QA_2026-06-10.md` | 2026-06-10 스튜디오9 역사 보고서 |
| `MVP_PLAY_FLOW_QA_SEOWOLCHAE_2026-06-10.md` | 2026-06-10 서월채 역사 보고서 |
| `QA_BLIND_RETEST_REPORT_2026-06-11_CODEX.md` | 2026-06-11 API-only blind retest 역사 보고서 |
| `QA_BLIND_RETEST_REPORT_2026-06-12.md` | 2026-06-12 backend/API retest 역사 보고서 |
| `QA_E2E_FOLLOWUP_REPORT_2026-06-12.md` | 2026-06-12 Android/API follow-up 역사 보고서 |
| `QA_E2E_CULPRIT_CONFIRMATION_REPORT_2026-06-15.md` | 2026-06-15 Android login blocker/API fallback 역사 보고서 |

## 2. QA 종류

| 종류 | 목적 | 우선 환경 | 결과 사용 |
|---|---|---|---|
| Android E2E | 실제 신규 유저 앱 경험 검증 | Android 실기기/에뮬레이터 + 운영 API 또는 staging API | 제품/프론트 우선순위 판단 |
| Blind Retest | 정답을 모르는 상태에서 30~50턴 내 후보 축소 가능성 검증 | Android 우선, 불가 시 masked API runner | 시나리오/AI/UX 품질 판단 |
| API Diagnostic | 앱 없이 API 계약, seed, AI 답변, privacy를 진단 | 운영 public API 또는 승인된 staging API | 원인 분석 신호. blind 판정과 분리 |
| Regression Smoke | 수정된 P0/P1 항목만 빠르게 재확인 | 수정 영역별 최소 환경 | PR/배포 전 확인 |

## 3. 절대 규칙

### 3.1 Public 문서에 남기지 않는 것

```text
- 범인명, 정답 수법, 동기, 은폐, full explanation
- rejected candidate matrix와 제거 후보별 상세 근거
- 최종 제출 점수, 등급, matched/missed breakdown
- raw sessionId, token, 실제 사용자 질문/AI 답변 원문 로그
- private seed 원문, private scenario brief, solution 예시
- 운영 secret, key, Firebase JSON, DB password, API key
```

점수/등급은 "public-safe"라고 판단하지 않는다. 결과 화면에 진입했더라도 점수/등급/정오/해설 상세는 private artifact에만 남긴다.

### 3.2 Blind QA 중 열람 금지 자료

Blind QA tester는 아래 자료를 열람하지 않는다.

```text
- `.private/` 하위 파일
- private seed YAML
- solution, culprit, variant truth, answer key가 들어간 파일
- docs/OFFICIAL_SCENARIO_DEMO_DAY.md
- 기존 QA 결과 문서와 이전 채팅 로그
- 운영 DB 직접 조회
- 서버 파일 grep으로 정답 찾기
- GET /api/play-sessions/{sessionId}/result 최종 제출 전 호출
- Swagger/API 문서의 solution 등록/조회 예시 또는 final-result correctness/fullExplanation 예시
```

QA runner가 위 자료 중 하나라도 열람했다면 해당 run은 blind run이 아니다.
결과는 diagnostic으로만 기록하고, blind candidate narrowing 판단에는 사용하지 않는다.

### 3.3 Public 문서에 남겨도 되는 것

```text
- 앱/흐름 차단 여부
- endpoint 계약과 HTTP status
- guidance/chip/timeline/result UX의 동작 여부
- 후보 축소가 가능/부분 가능/불가능했는지의 broad summary
- owner, priority, recommended action
- redacted log/privacy spot check 결과
```

10턴 단위 요약은 public-safe broad summary만 남긴다. "어떤 후보가 왜 탈락했는지"는 private artifact에 둔다.

## 4. Blind Validity

Blind QA는 정답을 모르는 신규 유저의 상태를 보존해야 한다.

### Android 우선

Android E2E가 가능하면 앱 화면을 기준으로 판단한다. Android가 실제 사용자 surface이므로 API-only보다 우선한다.

### API-only 허용 조건

앱 접근이 막히거나 화면 상태를 확인하기 어렵다면 public API로 보조 확인할 수 있다.
단, 후보 축소 평가에 API 응답을 사용할 때는 deduction 시작 전에 아래 필드를 runner/tooling에서 숨기거나 무시한 절차를 기록해야 한다.

```text
- evidence importance
- suspect culpritEligible
- suspicion/candidate metadata
- 역할성 asset path 또는 정답성/레드헤링 추정 필드
- solution/final-result 예시와 correctness/fullExplanation 예시
```

마스킹 기록이 없으면 API-only 후보 축소 결론은 `blind invalid`로 표기한다.
이 경우 "진단 신호"로만 사용하고, Android 또는 masked API runner로 다시 측정한다.

### API 문서 열람 제한

Swagger/API 문서는 endpoint shape, request/response 필드명, status code 확인에만 사용한다.
solution registration 예시, final-result correctness/fullExplanation 예시, 채점 breakdown 예시는 열람하지 않는다.

## 5. Fresh Session 원칙

30~50턴 후보 축소 측정에는 fresh session만 사용한다.

```http
GET  /api/scenarios
GET  /api/scenarios/{scenarioId}
GET  /api/play-sessions/active?scenarioId={scenarioId}
POST /api/play-sessions
POST /api/play-sessions/{sessionId}/abandon
```

`POST /api/play-sessions`가 `409 P002`로 막히면 active session 내용을 보지 말고 abandon 후 create를 1회 재시도한다.

운영 public API에서 abandon을 호출할 때는 QA 전용 계정 또는 팀 합의된 시간대를 사용한다. mock/shared 계정이면 다른 사람의 진행 중 세션을 종료할 수 있다.

## 6. QA 실행 순서

1. 환경 확인
   - 앱 빌드/commit, API URL, 계정 종류, device/emulator 상태를 기록한다.
   - 운영 API write가 발생하면 QA 계정/승인 여부를 기록한다.

2. 신규 세션 시작
   - active session 여부를 확인한다.
   - fresh session이 아니면 후보 축소 측정에는 사용하지 않는다.

3. 초기 관찰
   - scenario list/detail, intro, suspects, evidence board, locked evidence masking을 확인한다.
   - locked evidence는 title/unlockHint 노출 자체만으로 blocker로 보지 않는다. description, code, image, secret-like metadata가 노출되는지 확인한다.

4. Guidance UX
   - evidence detail에서 읽을 점, 함께 볼 증거, 추천 질문이 보이는지 확인한다.
   - guidance가 없는 증거는 "없는 상태"와 "불필요한 상태"를 구분한다.

5. Suggested Question Chip
   - chip tap은 심문 화면 이동과 입력창 prefill까지만 수행해야 한다.
   - tap 즉시 AI 호출이 발생하면 P1이다.
   - 기존 draft가 있으면 override 전 확인/보존 정책을 확인한다.

6. Interrogation / Deduction
   - AI 답변이 설정에 없는 시간/장소/사실을 만들지 않는지 본다.
   - 증거 제시 시 "인정 사실 / 모르는 범위 / 다음 비교 대상"을 제공하는지 본다.
   - 30~50턴 안에 동기/수단/기회/은폐를 한 후보에게 연결할 수 있는지 broad summary로 기록한다.

7. Final Deduction
   - 충분한 근거가 없으면 제출하지 않고 "후보 미확정"으로 기록한다.
   - 제출했다면 결과 화면 진입 여부만 public에 남긴다. 점수/등급/정오/해설 상세는 private에 둔다.

8. Privacy Spot Check
   - AI_CALL, AI_CALL_CONTEXT, app log, access log에서 raw prompt, answer, user question, token, secret, raw session id가 없는지 redacted snippet으로 확인한다.
   - 확인하지 못했으면 `미확인`으로 명시한다.

## 7. 복붙용 QA 프롬프트

아래 블록을 새 채팅방의 QA 에이전트에게 전달한다.

````md
# 역할

너는 ClueRoom 신규 사용자 관점의 blind QA tester다.
정답, private seed, 이전 QA 상세를 모르는 상태로 Android 앱을 우선 사용한다.
Android가 불가능하면 public API를 보조 surface로 사용하되, spoiler metadata masking 없이 후보 축소를 측정하지 않는다.

# 금지

- private seed, solution, 정답 문서, 이전 QA의 후보 제거 상세를 보지 않는다.
- `.private/`, docs/OFFICIAL_SCENARIO_DEMO_DAY.md, 기존 QA 결과 문서, 이전 채팅 로그를 보지 않는다.
- DB 직접 조회, 서버 파일 grep으로 정답 찾기, final result 선조회는 하지 않는다.
- API 문서의 solution/final-result 예시와 correctness/fullExplanation 예시를 보지 않는다.
- public 보고서에 범인명, 정답 수법, 점수/등급, rejected-candidate rationale, raw sessionId/token을 쓰지 않는다.
- 최종 후보가 충분히 확정되지 않았으면 추측 제출하지 않는다.

# 목표

1. 신규 세션으로 두 공식 시나리오의 기본 플레이 흐름을 확인한다.
2. evidence guidance, 함께 볼 증거, 추천 질문 UX가 실제 추리에 도움이 되는지 확인한다.
3. suggested-question chip이 prefill-only인지 확인한다.
4. AI 답변이 설정에 없는 사실을 만들지 않고, 증거 제시 시 다음 비교 방향을 주는지 확인한다.
5. 30~50턴 안에 후보 축소가 가능한지 판단한다.
6. 운영/privacy spot check 결과를 기록한다.

# fresh session

active session이 있으면 내용을 보지 않는다.
QA 전용 계정 또는 합의된 시간대에서만 abandon 후 create를 1회 재시도한다.

# API-only blind validity

API-only로 진행할 때는 deduction 전 아래 필드를 숨긴 방식 또는 수동 무시 절차를 기록한다.

- importance
- culpritEligible
- suspicion/candidate metadata
- 역할성 asset path
- solution/result correctness 예시

기록이 없으면 candidate narrowing 결론은 blind invalid로 표기한다.

# 보고

Findings First를 먼저 쓴다.
각 finding은 Priority, Area, Evidence, Expected, Actual, Impact, Recommended Action을 포함한다.
10-turn summary는 broad/public-safe로만 쓴다.
제거 후보별 상세 근거와 최종 결과 점수/등급/해설은 private artifact notice로 분리한다.
````

## 8. 보고서 템플릿

````md
# ClueRoom QA Report - YYYY-MM-DD

## 0. Scope

- Tester:
- Environment:
- API:
- App build:
- Backend commit/version:
- Account policy:
- Blind validity:
- Private artifact:

## 1. Safety Declaration

- Private seed/solution opened: yes/no
- API-only masking recorded: yes/no/not applicable
- Result score/grade kept private: yes/no/not submitted
- Rejected-candidate rationale kept private: yes/no
- Privacy spot check: pass/fail/not checked

## 2. Final Judgment

- Overall:
- Android E2E:
- API diagnostic:
- Candidate narrowing:
- Merge/release blocker:

## 3. Findings First

| Priority | Area | Finding | Evidence | Expected | Actual | Impact | Recommended Action |
|---|---|---|---|---|---|---|---|
| P0/P1/P2/P3 |  |  |  |  |  |  |  |

## 4. Flow Summary

| Scenario | Surface | Session | Guidance | Chip | AI quality | Candidate narrowing | Final submit |
|---|---|---|---|---|---|---|---|
|  | Android/API | fresh/active/invalid |  |  |  |  |  |

## 5. 10-Turn Broad Summary

| Scenario | Turns | Main targets | Evidence used | More plausible | Less plausible |
|---|---|---|---|---|---|
|  | 1-10 | broad only | public-safe categories 또는 redacted labels only | broad only | private artifact에 상세 기록 |

## 6. Public-Safe Follow-up

| Owner | Priority | Action |
|---|---|---|
| Backend/AI/Frontend/Scenario/Ops/Docs |  |  |

## 7. Private Artifact Notice

- Contains:
- Stored at:
- Not included in public report:
````

## 9. Priority 기준

| Priority | 기준 |
|---|---|
| P0 | 신규 유저가 앱을 시작할 수 없거나, 정답/스포일러성 metadata가 public gameplay surface에 노출되거나, 운영 보안/개인정보 위험이 즉시 발생 |
| P1 | 핵심 플레이 흐름, blind validity, 후보 축소, AI 답변 품질, 자동 AI 호출처럼 게임 경험을 크게 훼손 |
| P2 | UX 설명 부족, 문서 drift, 특정 환경 미확인, 개선하지 않으면 QA/운영 혼선 발생 |
| P3 | 표현 정리, 낮은 위험의 문서/관측성 개선 |

## 10. Current QA Board

현재 상태는 날짜별 보고서의 반복 이슈를 public-safe로 합친 것이다. 수정이 끝난 항목은 이 표에서 `재검 필요` 또는 `완료`로 바꾼다.

| Owner | Priority | Status | Source / Last verified | Issue | Next verification |
|---|---|---|---|---|---|
| Backend | P0/P1 | Open | 2026-06-10~2026-06-15 reports | public gameplay DTO/API에서 정답성/핵심성/비후보 추론 metadata가 플레이어 surface에 노출되는 문제 | public DTO/admin DTO 분리 후 API-only spoiler scan |
| Frontend | P0/P1 | Open | 2026-06-12 follow-up, 2026-06-15 report | FE가 정답성 metadata에 후보/핵심 증거 UI를 의존할 수 있음 | metadata 제거 후 앱 후보/증거 UI regression |
| Frontend | P0 | Changed, needs retest | 2026-06-15 report, OAuth 연결 후 미재검 | 운영 앱 로그인 경로가 막혔던 이슈. OAuth 연결 후 Android E2E 재검 필요 | fresh install에서 OAuth login, token refresh, `/api/auth/me`, scenario 진입 |
| Backend/Product | P0/P1 | Open | 2026-06-15 report | 앱 login gate와 public play API 인증 정책 불일치 | `AUTH_REQUIRE_AUTHENTICATION` 운영 정책, QA 계정 정책, mock/public gameplay 정책 확정 |
| Frontend | P1 | Open | 2026-06-11~2026-06-15 reports | 기본 suggested-question chip 자동 전송 가능성 | 모든 chip route가 prefill-only인지 Android E2E |
| Scenario seed | P1 | Changed, needs blind retest | 2026-06 guidance seed 반영 후 미재검 | 30~50턴 안에 후보를 안정적으로 좁히기 어려웠고, guidance coverage가 부족했음 | Android 또는 masked API blind retest |
| AI policy | P1 | Open | 2026-06-10~2026-06-15 reports | 증거 제시 답변이 회피형으로 끝나고 다음 비교 대상을 충분히 주지 못함 | response shape smoke: 인정 사실/모르는 범위/다음 비교 대상 |
| Backend | P1 | Open | 2026-06-12 report | final deduction/result의 interrogation count 집계 불일치 가능성 | 제출 전후 DB/log count, result response count 대조 |
| Frontend | P1/P2 | Open | 2026-06-12~2026-06-15 reports | guidance rendering, timeline, final submit/result full E2E coverage 부족 | Android full E2E |
| Product/Backend | P2 | Open | 2026-06-15 report | final-deduction 세부 rubric과 in-game guidance 용어가 어긋날 수 있음 | result feedback public-safe review |
| Ops | P1 | Open | 반복 QA reports | AI_CALL/AI_CALL_CONTEXT privacy spot check를 보고서마다 확인해야 함 | redacted Loki/Grafana snippet으로 raw prompt/answer/user question 부재 확인 |
| Docs | P2 | Open | 2026-06-15 report | QA/FE 문서가 현재 구현 상태와 어긋나는 drift | 코드 SoT 기준 docs update |

## 11. Regression Checklist Appendix

이 섹션은 이전 `QA_HANDOFF.md`의 re-smoke checklist를 흡수한 것이다.
새 보고서에는 필요한 항목만 참조하고, 긴 실행 로그를 반복해서 붙이지 않는다.

### Backend / AI

```text
INTERROGATION 기반 해금:
- 특정 용의자 심문 후 INTERROGATION 조건 증거가 해금되는지 확인
- InterrogationResponse.unlockedEvidences 포함 여부 확인
- GET /evidences와 dashboard count 반영 확인

EVIDENCE_PRESENTED 기반 해금:
- 제시 증거 조건으로 새 증거가 해금되는지 확인
- 같은 증거 반복 제시가 idempotent인지 확인
- locked evidence 제시 시 400 유지
- questionType / presentedEvidenceId 조합 validation 유지

final-deduction 입력/상태:
- coverUpText 필수/선택 정책과 API spec 일치
- final-deduction in-flight 중 abandon 처리 정책 확인
- abandon 후 final-deduction/result 메시지 정합성 확인
- result 조회의 interrogation count와 실제 로그 count 대조

validation API:
- hints=0 rule 정책 확인
- timelineEvents 포함 여부 확인
- active variants 전체 검증 여부 확인
- AI 호출 완료/실패 로그가 raw prompt 없이 남는지 확인
```

### Android / Frontend

```text
Scenario playability:
- 공식 시나리오 상세 진입
- 시작 버튼이 준비 중 gate로 막히지 않는지 확인
- 브리핑/현장/증거/용의자/타임라인 진입
- thumbnailUrl, coverImageUrl, mapImageUrl 표시

Auth / active recovery:
- fresh install에서 OAuth login 성공
- access token 저장, refresh, /api/auth/me 확인
- active PLAYING 세션이 있을 때 P002 details.activeSessionId 또는 GET /active fallback으로 이어가기

Guidance / chip:
- evidence detail guidance 표시
- suggested question chip prefill-only
- 기존 draft override/보존 정책이 UI에서 명확한지 확인
- hardcoded 기본 chip도 자동 전송하지 않는지 확인

Timeline / result:
- 서버 timeline API 응답을 앱에서 렌더링
- final submit, loading, timeout/network fallback, result 화면 복구
- score/grade/detail breakdown은 public QA 문서에 기록하지 않음
```

### Ops / Privacy

```text
운영 로그 privacy:
- 사용자 질문 원문 미노출
- 최종 추리 원문 미노출
- Hibernate bind TRACE 비활성
- AI debug는 redacted preview / model / latency / status / token 중심

AI_CALL / AI_CALL_CONTEXT:
- raw prompt 없음
- raw answer 없음
- user question 전문 없음
- raw sessionId/token 없음
- prompt block estimate, templateHash, latency, token count 같은 metadata만 기록
```

## 12. 역사 보고서 사용법

날짜별 QA 보고서는 원문 재현과 당시 판단을 보존하기 위한 자료다.

```text
- 새 QA 실행 지시는 이 문서를 사용한다.
- 새 오픈 이슈 판단은 Current QA Board에 흡수한다.
- 날짜별 보고서에서 반복되는 findings table을 새 문서에 그대로 복사하지 않는다.
- 보고서의 private-sensitive 추론은 public 문서로 끌어오지 않는다.
```

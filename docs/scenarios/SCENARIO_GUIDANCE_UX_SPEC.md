# Scenario Guidance UX Spec 한글 정본

> 상태: accepted design / backend contract implemented / surface status는 아래 matrix 기준
>
> 마지막 업데이트: 2026-06-10
>
> 범위: player-facing evidence guidance, Android evidence UX, backend/API support, YAML authoring rules

> 최신 상태 기준: 이 문서의 본문에는 2026-06-10 설계 당시의 gap 표현이 남아 있다.
> 현재 정본 판단은 아래 status matrix, 백엔드 README, Android/Web README, QA 보고서를 함께 본다.

## 0. Current Status Matrix

| Layer | Current status | Source of truth |
|---|---|---|
| Backend guidance contract | Done | `ScenarioYaml` guidance field, `evidences.guidance_json`, validator, `PlayEvidenceDetailResponse.guidance`, locked compare evidence masking |
| API contract | Done | `CaseLab_AI_API_Spec.md`, `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` |
| Android display | Implemented for current README surface; full chip-route matrix remains QA follow-up | `project-fe` README, 2026-06-18 Android local retest |
| Web display | Uses the same backend gameplay/auth contract; guidance-specific full matrix is not the primary PR #7 smoke gate | `clueroom-web-fe` README/PORTING_STATUS |
| Official seed guidance | Exists for selected high-impact evidence; full coverage remains iterative UX work | QA reports and scenario YAML/import docs |
| Product goal | Reduce repeated free-form interrogation by guiding reading points, compare evidence, and prefill-only questions | LLMOps cost/rate-limit plan, QA operating guide |

해석:

- `30~50회`는 현재 달성된 평균이 아니라 장기 제품 목표다.
- 현재 QA 기준에서는 70~100회 플레이를 현실 baseline으로 보고, guidance는 이를 60~80회, 이후 30~50회로 낮추기 위한 UX/비용 개선 축이다.
- 본문에 남아 있는 "Android/Frontend pending" 표현은 초기 설계 시점의 gap을 설명하는 문맥으로 읽고, 최신 release 상태는 위 matrix와 각 README를 우선한다.

## 1. 목적

이 문서는 ClueRoom이 정답을 노출하지 않으면서, 플레이어를 증거에서 다음 조사 행동으로 안내하는 방식을 정의한다.

현재 scenario 구조는 사건 정답, variant, evidence role, NPC policy를 담을 수 있다.
하지만 product layer에서 부족한 것은 player-facing guidance다.

```text
Player opens evidence
-> understands what to read
-> knows which evidence to compare
-> knows which suspect to question
-> continues investigation without direct answer leakage
```

이 문서는 public-safe다.
UX, schema shape, API contract, implementation task만 설명한다.
공식 scenario answer, culprit name, private seed text, answer-bearing reasoning을 이 문서에 붙여 넣지 않는다.

## 2. 현재 문제

QA feedback에서 반복적으로 나온 문제:

```text
Evidence exists.
NPC policies exist.
The correct answer structure exists.

But players often do not know the next action.
```

흔한 실패 패턴:

```text
1. Player가 evidence card를 보지만 핵심 관찰점을 놓친다.
2. 어떤 다른 evidence와 비교해야 하는지 모른다.
3. 다음에 어떤 suspect에게 질문해야 하는지 모른다.
4. Player가 넓은 질문을 하면 AI answer가 지나치게 회피적일 수 있다.
5. App progression이 free-form AI interrogation에 너무 의존한다.
```

해결책은 story나 evidence를 더 많이 추가하는 것이 아니다.
기존 evidence 주변에 얇은 guidance layer를 추가하는 것이다.

## 3. Product Decision 제품 결정

첫 UX 개선은 evidence-detail guidance로 한다.

```text
Evidence detail becomes the investigation hub for the next action.
```

중요 evidence는 세 가지 guidance를 노출할 수 있다.

```text
readingPoints
이 evidence에서 player가 무엇을 봐야 하는지.

compareWithEvidenceCodes
이 evidence와 함께 비교해야 할 다른 evidence.

suggestedQuestions
어떤 suspect에게 어떤 질문으로 시작하면 좋은지.
```

이것은 full hint system보다 의도적으로 좁은 기능이다.
사건을 풀어주는 것이 아니라, 비교와 심문 방향으로 player를 밀어주는 역할이다.

## 4. 목표

```text
1. Mid-game investigation의 dead end를 줄인다.
2. Evidence card가 어떻게 사용되어야 하는지 설명하게 한다.
3. Evidence를 실행 가능한 interrogation prompt로 연결한다.
4. Answer-bearing truth를 player-facing guidance 밖에 둔다.
5. AI NPC가 모든 progression guidance를 책임지는 구조를 피한다.
6. V1은 작은 backend/Android 변경으로 구현 가능하게 유지한다.
```

## 5. Non-Goals 제외 범위

별도 PR로 명시적으로 계획하지 않는 한, 첫 구현에는 아래를 포함하지 않는다.

```text
1. 공식 scenario story structure를 다시 쓰지 않는다.
2. 새로운 culprit/variant logic을 추가하지 않는다.
3. solution, scoring, variant truth를 player에게 노출하지 않는다.
4. V1에서 AI prompt construction을 변경하지 않는다.
5. V1에서 ACTIVE_INVESTIGATION을 새 unlock enum으로 추가하지 않는다.
6. Play 중 red-herring refutation을 direct answer guidance로 보여주지 않는다.
7. 사용자 확인 없이 recommended question을 auto-submit하지 않는다.
```

## 6. UX 원칙

### 6.1 Nudge, Do Not Solve 방향만 주고 풀어주지 않기

Guidance가 답해야 하는 질문:

```text
What should I inspect or ask next?
```

Guidance가 답하면 안 되는 질문:

```text
Who is the culprit?
What is the exact final method?
Which variant is active?
What is the full solution chain?
```

### 6.2 Evidence First, AI Second 증거 먼저, AI는 그다음

App은 player가 완벽한 AI question을 추측하는 것에 의존하면 안 된다.

Evidence detail screen이 interrogation 전에 player를 준비시켜야 한다.

```text
Observe evidence
-> compare related evidence
-> choose a suggested question
-> ask NPC with evidence attached
```

### 6.3 Locked Content Must Stay Masked 잠긴 내용은 계속 마스킹

Guidance는 locked evidence 방향을 가리킬 수 있지만, locked evidence detail을 드러내면 안 된다.

비교 대상 evidence가 locked 상태라면 UI는 안전한 placeholder를 보여준다.

```text
Locked related evidence
Unlock more evidence to compare this clue.
```

기존 evidence list/detail API와 같은 masking policy를 사용한다.

### 6.4 Public Text And Secret Truth Stay Separate 공개 문구와 비밀 정답 분리

Guidance는 player-facing text다.
반드시 public-safe content로 작성해야 한다.

Guidance에서 금지:

```text
culprit identity
active variant answer
solution explanation
private seed notes
hidden NPC truth not yet revealed
full answer-bearing timeline
```

Guidance에서 허용:

```text
observable marks
player에게 이미 보이는 time range
evidence comparison direction
clarification을 요청하는 question prompt
safe suspicion framing
```

## 7. Target UX 목표 UX

### 7.1 Evidence Detail Layout 증거 상세 레이아웃

권장 순서:

```text
Evidence title
Image / thumbnail
One-line summary
Detailed description

Section: 판독 포인트
Section: 함께 볼 증거
Section: 추천 질문
Section: 관련 용의자
```

새 guidance section 3개는 optional이다.
Data가 없으면 빈 상태를 보여주지 말고 section을 숨긴다.

### 7.2 판독 포인트

목적:

```text
Player가 이 evidence에서 무엇을 봐야 하는지 알려준다.
```

표시 방식:

```text
짧은 bullet list, 보통 2~4개.
```

좋은 예:

```text
- 기록된 행동 시각과 관찰된 결과 사이의 간격이 핵심이다.
- 물체 위치는 이전 checklist와 비교해야 한다.
- 보이는 흔적은 두 표면을 가로지르기 때문에 중요하다.
```

나쁜 예:

```text
- 이것은 범인이 실제 방법을 썼다는 증거다.
- 이 증거는 모든 red herring을 배제한다.
- 이것은 active variant의 결정적 증거다.
```

### 7.3 함께 볼 증거

목적:

```text
Player가 evidence를 단독으로 읽는 데서 멈추지 않고 evidence comparison으로 이동하게 한다.
```

표시 방식:

```text
Related evidence card 또는 chip.
```

동작:

```text
Unlocked evidence
-> tap opens evidence detail.

Locked evidence
-> tap shows safe locked message or unlock hint.
```

이 section을 통해 locked evidence detail을 드러내지 않는다.

### 7.4 추천 질문

목적:

```text
Player가 생산적인 interrogation을 시작하도록 돕는다.
```

표시 방식:

```text
Target suspect label이 붙은 question chip.
```

동작:

```text
Tap question chip
-> navigate to interrogation screen
-> preselect target suspect
-> prefill question text
-> attach current evidence as presentedEvidenceId
-> user reviews and sends manually
```

권장 request mapping:

```json
{
  "suspectId": 10,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "What should be clarified about this evidence?",
  "presentedEvidenceId": 100
}
```

App은 auto-submit하면 안 된다.
Player가 최종 통제권을 가져야 한다.

## 8. YAML Proposal YAML 제안

`evidences[]`에 추가할 vNext 제안이다.
Backend importer가 업데이트되기 전까지는 현재 구현 schema의 일부가 아니다.

권장 shape:

```yaml
evidences:
  - code: EVIDENCE_SAMPLE
    title: "Sample evidence"
    oneLine: "Short public summary"
    baseDetail: "Player-facing description"

    guidance:
      readingPoints:
        - "Notice the visible mismatch between the two recorded states."
        - "Check whether the timestamp is an action time or a confirmation time."
      compareWithEvidenceCodes:
        - EVIDENCE_RELATED_A
        - EVIDENCE_RELATED_B
      suggestedQuestions:
        - targetCharacterCode: SUSPECT_SAMPLE
          question: "Can you explain why this evidence differs from the earlier record?"
        - targetCharacterCode: WITNESS_SAMPLE
          question: "Who could verify this detail before the incident?"
```

### 8.1 Field Rules 필드 규칙

| Field | Type | Required | Rule 규칙 |
|---|---:|---:|---|
| `guidance` | object | no | guidance가 필요 없으면 생략한다. |
| `readingPoints` | list<string> | no | 0~5개. Player-facing only. |
| `compareWithEvidenceCodes` | list<string> | no | `evidences[].code`에 존재해야 한다. |
| `suggestedQuestions` | list<object> | no | 0~5개. Auto-submit 금지. |
| `targetCharacterCode` | string | question에는 yes | `characters[].code`에 존재해야 한다. |
| `question` | string | question에는 yes | Answer-neutral이고 evidence-specific이어야 한다. |

권장 작성 제한:

```text
readingPoints item: 한글 120자 이하
suggested question: 한글 140자 이하
compare evidence count: 대부분의 evidence에서 1~4개
suggested question count: 대부분의 evidence에서 1~3개
```

### 8.2 Validation Rules 검증 규칙

Importer/validator는 아래를 reject해야 한다.

```text
1. compareWithEvidenceCodes가 unknown evidence code를 참조한다.
2. suggestedQuestions.targetCharacterCode가 unknown character code를 참조한다.
3. 하나의 guidance block 안에 duplicate compare evidence code가 있다.
4. Evidence가 자기 자신과 compare된다.
5. Blank reading point 또는 blank question이 있다.
6. Guidance text가 blocked private marker 또는 known secret label을 포함한다.
```

아래는 reject가 아니라 warning을 권장한다.

```text
1. Important evidence에 guidance가 없다.
2. Guidance block에 reading point가 5개를 초과한다.
3. Suggested question이 mobile chip에 비해 너무 길다.
4. Compare target이 late-phase locked evidence다.
```

## 9. Backend Design 백엔드 설계

### 9.1 현재 구현 상태

현재 backend는 guidance V1 계약을 구현했다.

구현된 범위:

```text
ScenarioYaml.EvidenceYaml guidance field
evidences.guidance_json storage
ScenarioYamlValidator guidance reference/private marker validation
PlayEvidenceDetailResponse guidance response
locked compare evidence evidenceCode masking
malformed guidance_json fail-soft parsing
```

초기 설계 시점의 남은 gap은 Android/Frontend 표시와 official seed guidance 작성이었다.
현재는 backend contract와 API 응답은 구현 완료로 보고, Android/Web 표시와 official seed coverage는 각 README/QA 보고서의 최신 상태를 함께 확인한다.
YAML data만 추가하면 API 응답에는 반영되지만, 사용자에게 보이는 품질은 surface별 UI 구현과 scenario별 guidance coverage에 좌우된다.

### 9.2 Persistence Recommendation 저장 방식 권장안

MVP에서는 evidence에 JSON column 하나를 두는 것을 우선한다.

```text
evidences.guidance_json TEXT or JSON nullable
```

이유:

```text
1. Guidance는 transactional gameplay state가 아니라 authored content다.
2. QA 이후 shape가 바뀔 수 있다.
3. V1에서는 개별 guidance field로 query할 필요가 없다.
4. UX가 검증되기 전에 table 3개를 새로 만들지 않아도 된다.
```

Guidance가 searchable해지거나 admin UI에서 편집되거나 analytics-heavy해지면 normalized table을 나중에 검토한다.

### 9.3 YAML Import Changes YAML import 변경

Record 추가:

```java
public record EvidenceGuidanceYaml(
        List<String> readingPoints,
        List<String> compareWithEvidenceCodes,
        List<SuggestedQuestionYaml> suggestedQuestions
) {}

public record SuggestedQuestionYaml(
        String targetCharacterCode,
        String question
) {}
```

확장:

```java
public record EvidenceYaml(
        ...
        EvidenceGuidanceYaml guidance
) {}
```

Importer 책임:

```text
1. Evidence code reference를 validate한다.
2. Character code reference를 validate한다.
3. Guidance JSON을 evidence row에 저장한다.
4. V1에서는 guidance를 AI prompt structure로 import하지 않는다.
5. Guidance response를 만들 때 variants, solution, scoring을 읽지 않는다.
```

### 9.4 API Response Changes API 응답 변경

먼저 evidence detail response에만 guidance를 추가한다.

```text
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

V1에서는 evidence list에 full guidance를 넣지 않는다.
List는 가볍게 유지한다.

제안 response addition:

```json
{
  "evidenceId": 100,
  "title": "Sample evidence",
  "isUnlocked": true,
  "guidance": {
    "readingPoints": [
      "Notice the visible mismatch between the two recorded states."
    ],
    "compareEvidences": [
      {
        "evidenceId": 101,
        "evidenceCode": "EVIDENCE_RELATED_A",
        "title": "Related evidence",
        "isUnlocked": true,
        "unlockHint": null
      },
      {
        "evidenceId": 102,
        "title": "Locked evidence",
        "isUnlocked": false,
        "unlockHint": "Continue investigation to unlock this evidence."
      }
    ],
    "suggestedQuestions": [
      {
        "targetSuspectId": 10,
        "targetName": "Suspect",
        "question": "Can you explain why this evidence differs from the earlier record?",
        "presentedEvidenceId": 100,
        "questionType": "EVIDENCE_PRESENTED"
      }
    ]
  }
}
```

Masking rule 마스킹 규칙:

```text
Current evidence가 locked이면 guidance는 null 또는 omitted이어야 한다.
Compared evidence가 locked이면 `evidenceCode`를 반환하지 않는다.
현재 구현은 locked compare evidence에도 `evidenceId`를 반환할 수 있지만, Android는 `isUnlocked=false`이면 상세 이동에 사용하지 않는다.
```

### 9.5 DTO Recommendation DTO 권장안

추천 DTO 이름:

```text
EvidenceGuidanceResponse
ComparableEvidenceResponse
SuggestedEvidenceQuestionResponse
```

추천 Java shape:

```java
public record EvidenceGuidanceResponse(
        List<String> readingPoints,
        List<ComparableEvidenceResponse> compareEvidences,
        List<SuggestedEvidenceQuestionResponse> suggestedQuestions
) {}

public record ComparableEvidenceResponse(
        Long evidenceId,
        String evidenceCode,
        String title,
        Boolean isUnlocked,
        String unlockHint
) {}

public record SuggestedEvidenceQuestionResponse(
        Long targetSuspectId,
        String targetName,
        String question,
        Long presentedEvidenceId,
        String questionType
) {}
```

### 9.6 Backend Acceptance Criteria 백엔드 완료 기준

Backend implementation 완료 기준:

```text
1. Scenario YAML이 evidences[].guidance를 포함할 수 있다.
2. Import가 invalid evidence/character reference를 reject한다.
3. Evidence detail API가 unlocked evidence에 guidance를 반환한다.
4. Evidence detail API가 locked evidence에 guidance를 반환하지 않는다.
5. Compared locked evidence가 기존 masking policy를 따른다.
6. Suggested question에 target suspect와 current presentedEvidenceId가 포함된다.
7. Valid guidance, invalid reference, locked masking, empty guidance test가 있다.
```

## 10. Android Design Android 설계

### 10.1 Evidence Detail UI 증거 상세 UI

현재 evidence description 아래에 section을 추가한다.

```text
판독 포인트
함께 볼 증거
추천 질문
```

Section behavior 섹션 동작:

```text
No data
-> hide section.

Loading
-> use existing evidence detail loading state.

API error
-> core evidence fields가 load되었다면 guidance 없이 evidence detail을 보여준다.
```

### 10.2 판독 포인트 UI

권장 layout:

```text
작은 제목이 있는 section
2~4개 bullet row
읽기 쉬운 line height
빽빽한 paragraph block 피하기
```

이 section은 hint answer가 아니라 detective's observation note처럼 느껴져야 한다.

### 10.3 함께 볼 증거 UI

권장 layout:

```text
Horizontal chip 또는 compact card
Title
Lock state
Optional unlock hint
```

Tap behavior 탭 동작:

```text
Unlocked item
-> 해당 evidence detail 열기

Locked item
-> locked message와 optional unlock hint 표시
```

### 10.4 추천 질문 UI

권장 layout:

```text
Question chip
Target suspect label
Optional "ask" CTA
```

Tap behavior 탭 동작:

```text
1. Suspect interrogation screen으로 이동한다.
2. Target suspect를 preselect한다.
3. Question을 prefill한다.
4. Current evidence를 presentedEvidenceId로 attach한다.
5. User가 직접 send를 누르게 한다.
```

Auto-send 금지 이유:

```text
1. Player가 통제권을 가져야 한다.
2. Auto-send는 interrogation attempt를 낭비할 수 있다.
3. Player가 question을 수정하고 싶을 수 있다.
4. 우발적인 AI call을 피한다.
```

### 10.5 Android Acceptance Criteria Android 완료 기준

Android implementation 완료 기준:

```text
1. Evidence detail이 reading point를 표시한다.
2. Evidence detail이 comparable evidence를 표시한다.
3. Unlocked comparable evidence를 tap하면 해당 detail로 이동한다.
4. Locked comparable evidence를 tap해도 hidden detail을 노출하지 않는다.
5. Suggested question tap이 suspect를 preselect하고 question을 prefill한다.
6. Suggested question tap이 current evidence를 presentedEvidenceId로 attach한다.
7. User가 명시적으로 question을 submit해야 한다.
8. Empty guidance가 blank UI section을 만들지 않는다.
```

## 11. AI Scope AI 범위

### 11.1 V1

V1에서는 AI prompt를 변경하지 않는다.

이유:

```text
1. Evidence detail과 question prefill만으로 UX benefit을 얻을 수 있다.
2. Prompt 변경은 answer leakage risk를 높인다.
3. 기존 interrogation endpoint가 이미 presentedEvidenceId를 지원한다.
4. 작은 PR이 review와 QA에 유리하다.
```

### 11.2 V2 Candidates V2 후보

V1 release 후 QA에서 유용성이 확인되면 아래를 검토한다.

```text
responseShape
mustAcknowledge, mustDeny, mustPointTo, mustNotSay를 가진 structured NPC reaction policy.

timelineGuard
Invented timeline claim을 방지하는 scenario-level safe time rule.

recommendedInvestigationFlow
Hint 또는 onboarding에서 사용할 optional non-spoiler sequence.
```

`ResponsePolicyResolver`와 prompt safety rule이 업데이트되기 전까지는 이를 prompt에 직접 넣지 않는다.

## 12. Unlock Scope 해금 범위

V1에서는 `ACTIVE_INVESTIGATION`을 추가하지 않는다.

기존 mechanism을 먼저 사용한다.

```text
EVIDENCE_PRESENTED
INTERROGATION
PHASE
```

현재 code는 이미 condition JSON으로 `EVIDENCE_PRESENTED` style progression을 지원한다.
새 enum은 기존 type과 product semantics가 다를 때만 이후 PR에서 추가한다.

## 13. Authoring Guide 작성 가이드

### 13.1 좋은 Guidance

좋은 guidance:

```text
1. 관찰 가능한 detail을 가리킨다.
2. Evidence 간 비교를 유도한다.
3. 정답을 단정하지 않고 질문을 제안한다.
4. Public-safe wording을 사용한다.
5. Player가 아직 사건을 풀지 못했어도 작동한다.
```

예시:

```yaml
guidance:
  readingPoints:
    - "The time shown here should be compared with the later confirmation record."
    - "The object position is different from the earlier checklist."
  compareWithEvidenceCodes:
    - EVIDENCE_SAMPLE_CHECKLIST
    - EVIDENCE_SAMPLE_TIME_LOG
  suggestedQuestions:
    - targetCharacterCode: WITNESS_SAMPLE
      question: "Can you confirm whether this record means the action happened or only that an alert was sent?"
```

### 13.2 나쁜 Guidance

나쁜 guidance:

```text
1. Culprit를 지목한다.
2. True method를 확정한다.
3. Player가 증명하기 전에 red herring이 false라고 말한다.
4. Active variant truth를 언급한다.
5. Private implementation note를 사용한다.
```

나쁜 예시:

```yaml
guidance:
  readingPoints:
    - "This is the decisive proof that the real culprit used the final method."
```

## 14. Privacy And Spoiler Checklist 공개/스포일러 점검표

Guidance content를 commit하기 전에 확인한다.

```text
[ ] Culprit name 없음
[ ] Active variant code 없음
[ ] Private YAML의 solution object 복사 없음
[ ] Final explanation paragraph 없음
[ ] 아직 player-facing이 아닌 hidden NPC truth 없음
[ ] Private seed label 또는 reviewer note 없음
[ ] Secret, token, key, server private path, user log 없음
[ ] Raw user prompt/answer logs 없음
```

## 15. Team Work Split 팀 작업 분담

권장 ownership:

| Area 영역 | Primary owner 담당 | Work 작업 |
|---|---|---|
| Product/story guidance rules | Scenario lead | Public-safe guidance 작성과 leakage risk review |
| Backend import/API | Backend play/scenario owner | YAML record, validation, persistence, evidence detail response 작업 |
| Android UX | Android owner | Evidence detail section, compare navigation, question prefill 작업 |
| AI policy v2 | AI backend owner | V1 guidance 유용성 검증 후 responseShape/timelineGuard |
| QA | QA owner | 30~50회 interrogation playthrough로 dead end 감소 확인 |

Concrete first-pass split 1차 작업 분리:

```text
Backend:
- guidance schema support 추가
- guidance_json 저장
- evidence detail API에서 guidance 반환
- validation/test 추가

Android:
- guidance section render
- compare evidence navigation 구현
- recommended question prefill 구현

Scenario author:
- 먼저 high-impact evidence 5~10개에 guidance 추가
- answer-bearing wording 피하기
- QA로 pattern 확인 후 확대
```

## 16. Rollout Plan 적용 계획

### Phase 1: Contract And UI 계약과 UI

```text
1. Backend가 guidance schema, import, validation, API response를 추가한다.
2. Android가 evidence detail UI section을 추가한다.
3. QA에는 sample 또는 제한된 private scenario guidance를 사용한다.
```

### Phase 2: Scenario Data 시나리오 데이터

```text
1. Early/mid-game evidence에 guidance를 먼저 추가한다.
2. Decisive evidence에는 조심스러운 non-answer wording으로만 guidance를 추가한다.
3. QA playthrough를 실행한다.
4. Reading point 길이와 question count를 조정한다.
```

### Phase 3: AI And Unlock Enhancements AI와 해금 고도화

```text
1. NPC answer quality를 위해 responseShape를 검토한다.
2. Time consistency를 위해 timelineGuard를 검토한다.
3. Evidence guidance가 안정된 뒤에만 추가 unlock rule을 검토한다.
```

## 17. QA Scenarios QA 시나리오

최소 QA case:

```text
1. Player가 early evidence를 열고 reading point를 본다.
2. Player가 related unlocked evidence를 tap하고 정상적으로 돌아온다.
3. Player가 related locked evidence를 tap해도 hidden detail을 보지 못한다.
4. Player가 suggested question을 tap해 interrogation screen으로 이동한다.
5. Interrogation screen에 올바른 suspect, question, presented evidence가 들어 있다.
6. Player가 send 전에 question을 수정한다.
7. Guidance가 없는 evidence도 정상 render된다.
8. Deduction result 전에는 final answer가 노출되지 않는다.
```

UX success criteria 성공 기준:

```text
1. Player가 대부분의 key evidence에서 최소 하나의 next action을 식별할 수 있다.
2. Player가 모든 interrogation question을 처음부터 발명하지 않아도 된다.
3. Player가 "what should I do next?" 같은 broad prompt 없이 진행할 수 있다.
4. Guidance 때문에 app이 사건을 대신 풀어준 느낌이 들지 않는다.
```

## 18. Open Decisions 미결정 사항

구현 전에 결정할 사항:

```text
1. Guidance를 evidences.guidance_json에 저장할 것인가, normalized table로 저장할 것인가?
   Recommendation: MVP에서는 guidance_json.

2. Evidence list에 hasGuidance boolean을 넣을 것인가?
   Recommendation: optional. V1에서는 detail-only guidance로 충분하다.

3. Locked compared evidence가 title을 보여줘도 되는가?
   Recommendation: 현재 locked evidence masking policy를 따른다.

4. Suggested question은 EVIDENCE_PRESENTED인가 RECOMMENDED인가?
   Recommendation: evidence detail에서 current evidence를 attach해 시작할 때는 EVIDENCE_PRESENTED.

5. Guidance를 AI prompt에 포함할 것인가?
   Recommendation: V1에서는 아니다.
```

## 19. Definition Of Done 완료 기준

Scenario guidance UX 완료 기준:

```text
1. Team이 guidance YAML shape에 합의한다.
2. Backend가 guidance를 import하고 validate한다.
3. Evidence detail API가 lock-safe guidance를 반환한다.
4. Android가 모든 guidance section을 render한다.
5. Suggested question chip이 auto-submit 없이 interrogation을 preload한다.
6. 최소 하나의 official scenario가 key early/mid-game evidence에 guidance를 가진다.
7. QA에서 direct answer leakage 없이 player가 next action을 찾을 수 있음을 확인한다.
```

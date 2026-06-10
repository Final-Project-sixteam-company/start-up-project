# Scenario Guidance UX Spec

> Status: proposal
>
> Last updated: 2026-06-10
>
> Scope: player-facing evidence guidance, Android evidence UX, backend/API support, YAML authoring rules

## 1. Purpose

This document defines how ClueRoom should guide players from evidence to the
next investigation action without revealing the answer.

The current scenario structure can hold the mystery answer, variants, evidence
roles, and NPC policies. The missing product layer is player-facing guidance:

```text
Player opens evidence
-> understands what to read
-> knows which evidence to compare
-> knows which suspect to question
-> continues investigation without direct answer leakage
```

This document is public-safe. It describes UX, schema shape, API contract, and
implementation tasks only. Do not paste official scenario answers, culprit
names, private seed text, or answer-bearing reasoning into this file.

## 2. Current Problem

QA feedback points to a recurring issue:

```text
Evidence exists.
NPC policies exist.
The correct answer structure exists.

But players often do not know the next action.
```

Common failure modes:

```text
1. The player sees an evidence card but misses the key observation.
2. The player does not know which other evidence should be compared.
3. The player does not know which suspect should be questioned next.
4. AI answers can become too evasive when the player asks a broad question.
5. The app relies too much on free-form AI interrogation for progression.
```

Adding more story or more evidence is not the main fix. The fix is to add a
thin guidance layer around existing evidence.

## 3. Product Decision

The first UX improvement should be evidence-detail guidance.

```text
Evidence detail becomes the investigation hub for the next action.
```

Each important evidence can expose three kinds of guidance:

```text
readingPoints
What the player should notice in this evidence.

compareWithEvidenceCodes
Which other evidence should be compared with this evidence.

suggestedQuestions
Which suspect to ask and what question to start with.
```

This is intentionally narrower than a full hint system. It should nudge the
player toward comparison and interrogation, not solve the case.

## 4. Goals

```text
1. Reduce dead ends during mid-game investigation.
2. Make evidence cards explain how they should be used.
3. Turn evidence into actionable interrogation prompts.
4. Keep answer-bearing truth out of player-facing guidance.
5. Avoid making the AI NPC responsible for all progression guidance.
6. Keep v1 implementable with small backend and Android changes.
```

## 5. Non-Goals

The first implementation must not include these unless explicitly planned in a
separate PR:

```text
1. Do not rewrite official scenario story structure.
2. Do not add new culprit/variant logic.
3. Do not expose solution, scoring, or variant truth to the player.
4. Do not change AI prompt construction in v1.
5. Do not add ACTIVE_INVESTIGATION as a new unlock enum in v1.
6. Do not show red-herring refutation as direct answer guidance during play.
7. Do not auto-submit recommended questions without user confirmation.
```

## 6. UX Principles

### 6.1 Nudge, Do Not Solve

Guidance should answer this:

```text
What should I inspect or ask next?
```

Guidance must not answer this:

```text
Who is the culprit?
What is the exact final method?
Which variant is active?
What is the full solution chain?
```

### 6.2 Evidence First, AI Second

The app should not rely on the player guessing the perfect AI question.

The evidence detail screen should prepare the player before interrogation:

```text
Observe evidence
-> compare related evidence
-> choose a suggested question
-> ask NPC with evidence attached
```

### 6.3 Locked Content Must Stay Masked

Guidance can point toward locked evidence, but it must not reveal locked
evidence details.

If a compared evidence is locked, the UI should show a safe placeholder:

```text
Locked related evidence
Unlock more evidence to compare this clue.
```

Use the same masking policy as the existing evidence list/detail API.

### 6.4 Public Text And Secret Truth Stay Separate

Guidance is player-facing text. It must be authored as public-safe content.

Forbidden in guidance:

```text
culprit identity
active variant answer
solution explanation
private seed notes
hidden NPC truth not yet revealed
full answer-bearing timeline
```

Allowed in guidance:

```text
observable marks
time ranges already visible to the player
evidence comparison direction
question prompts that ask for clarification
safe suspicion framing
```

## 7. Target UX

### 7.1 Evidence Detail Layout

Recommended order:

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

The three new guidance sections are optional. If a section has no data, hide
the section rather than showing an empty state.

### 7.2 판독 포인트

Purpose:

```text
Tell the player what to notice in this evidence.
```

Display:

```text
Short bullet list, usually 2-4 items.
```

Good examples:

```text
- The key time is the gap between the recorded action and the observed result.
- The object position should be compared with the earlier checklist.
- The visible mark is important because it crosses two surfaces.
```

Bad examples:

```text
- This proves the culprit used the real method.
- This evidence excludes all red herrings.
- This is the decisive proof for the active variant.
```

### 7.3 함께 볼 증거

Purpose:

```text
Move the player from isolated evidence reading to evidence comparison.
```

Display:

```text
Cards or chips for related evidence.
```

Behavior:

```text
Unlocked evidence
-> tap opens evidence detail.

Locked evidence
-> tap shows safe locked message or unlock hint.
```

Do not reveal locked evidence detail through this section.

### 7.4 추천 질문

Purpose:

```text
Help the player start a productive interrogation.
```

Display:

```text
Question chip with target suspect label.
```

Behavior:

```text
Tap question chip
-> navigate to interrogation screen
-> preselect target suspect
-> prefill question text
-> attach current evidence as presentedEvidenceId
-> user reviews and sends manually
```

Recommended request mapping:

```json
{
  "suspectId": 10,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "What should be clarified about this evidence?",
  "presentedEvidenceId": 100
}
```

The app must not auto-submit. The player should remain in control.

## 8. YAML Proposal

This is a proposed vNext addition to `evidences[]`. It is not part of the
current implemented schema until the backend importer is updated.

Recommended shape:

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

### 8.1 Field Rules

| Field | Type | Required | Rule |
|---|---:|---:|---|
| `guidance` | object | no | Omit when no guidance is needed. |
| `readingPoints` | list<string> | no | 0-5 items. Player-facing only. |
| `compareWithEvidenceCodes` | list<string> | no | Codes must exist in `evidences[].code`. |
| `suggestedQuestions` | list<object> | no | 0-5 items. No auto-submit. |
| `targetCharacterCode` | string | yes for question | Must exist in `characters[].code`. |
| `question` | string | yes for question | Should be answer-neutral and evidence-specific. |

Recommended authoring limits:

```text
readingPoints item: 120 Korean chars or less
suggested question: 140 Korean chars or less
compare evidence count: 1-4 for most evidence
suggested question count: 1-3 for most evidence
```

### 8.2 Validation Rules

The importer/validator should reject:

```text
1. compareWithEvidenceCodes referencing unknown evidence code.
2. suggestedQuestions.targetCharacterCode referencing unknown character code.
3. Duplicate compare evidence codes in one guidance block.
4. Evidence comparing with itself.
5. Blank reading point or blank question.
6. Guidance text containing blocked private markers or known secret labels.
```

Validation should warn, not necessarily reject, when:

```text
1. An important evidence has no guidance.
2. A guidance block has more than 5 reading points.
3. A suggested question is too long for a mobile chip.
4. A compare target is a late-phase locked evidence.
```

## 9. Backend Design

### 9.1 Current Implementation Gap

Current backend does not persist or serve guidance data.

Known gaps:

```text
ScenarioYaml.EvidenceYaml has no guidance field.
Evidence entity has no guidance storage.
PlayEvidenceDetailResponse has no guidance response.
ScenarioYamlImportService does not validate guidance references.
Android cannot display guidance because API does not return it.
```

Therefore, adding YAML data alone will not change the user experience.

### 9.2 Persistence Recommendation

For MVP, prefer one JSON column on evidence:

```text
evidences.guidance_json TEXT or JSON nullable
```

Reason:

```text
1. Guidance is authored content, not transactional gameplay state.
2. The shape may evolve after QA.
3. Querying by individual guidance fields is not required in v1.
4. It avoids three new tables before the UX proves itself.
```

Normalized tables can be considered later if guidance becomes searchable,
editable in an admin UI, or analytics-heavy.

### 9.3 YAML Import Changes

Add records:

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

Extend:

```java
public record EvidenceYaml(
        ...
        EvidenceGuidanceYaml guidance
) {}
```

Importer responsibilities:

```text
1. Validate evidence code references.
2. Validate character code references.
3. Store guidance JSON with the evidence row.
4. Do not import guidance into AI prompt structures in v1.
5. Do not read variants, solution, or scoring while building guidance response.
```

### 9.4 API Response Changes

Add guidance only to evidence detail response first:

```text
GET /api/play-sessions/{sessionId}/evidences/{evidenceId}
```

Do not add full guidance to the evidence list in v1. The list should stay
lightweight.

Proposed response addition:

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
        "evidenceCode": "EVIDENCE_RELATED_B",
        "title": "Locked evidence",
        "isUnlocked": false,
        "unlockHint": "Continue investigation to unlock this evidence."
      }
    ],
    "suggestedQuestions": [
      {
        "targetCharacterCode": "SUSPECT_SAMPLE",
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

Masking rule:

```text
If the current evidence is locked, guidance must be null or omitted.
If a compared evidence is locked, return only fields allowed by the current
locked-evidence masking policy.
```

### 9.5 DTO Recommendation

Suggested DTO names:

```text
EvidenceGuidanceResponse
ComparableEvidenceResponse
SuggestedEvidenceQuestionResponse
```

Suggested Java shape:

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
        String targetCharacterCode,
        Long targetSuspectId,
        String targetName,
        String question,
        Long presentedEvidenceId,
        String questionType
) {}
```

### 9.6 Backend Acceptance Criteria

Backend implementation is complete when:

```text
1. Scenario YAML can include evidences[].guidance.
2. Import rejects invalid evidence/character references.
3. Evidence detail API returns guidance for unlocked evidence.
4. Evidence detail API does not return guidance for locked evidence.
5. Compared locked evidence follows existing masking policy.
6. Suggested questions include target suspect and current presentedEvidenceId.
7. Tests cover valid guidance, invalid references, locked masking, and empty guidance.
```

## 10. Android Design

### 10.1 Evidence Detail UI

Add sections under the current evidence description:

```text
판독 포인트
함께 볼 증거
추천 질문
```

Section behavior:

```text
No data
-> hide section.

Loading
-> use existing evidence detail loading state.

API error
-> show evidence detail without guidance if core evidence fields loaded.
```

### 10.2 판독 포인트 UI

Recommended layout:

```text
Small titled section
2-4 bullet rows
Use readable line height
Avoid dense paragraph blocks
```

This section should feel like a detective's observation note, not a hint answer.

### 10.3 함께 볼 증거 UI

Recommended layout:

```text
Horizontal chips or compact cards
Title
Lock state
Optional unlock hint
```

Tap behavior:

```text
Unlocked item
-> open evidence detail for that evidence.

Locked item
-> show locked message and optional unlock hint.
```

### 10.4 추천 질문 UI

Recommended layout:

```text
Question chip
Target suspect label
Optional "ask" CTA
```

Tap behavior:

```text
1. Navigate to suspect interrogation screen.
2. Preselect target suspect.
3. Prefill question.
4. Attach current evidence as presentedEvidenceId.
5. Let user press send manually.
```

Do not auto-send because:

```text
1. The player should stay in control.
2. Auto-send can waste interrogation attempts.
3. The player may want to edit the question.
4. It avoids accidental AI calls.
```

### 10.5 Android Acceptance Criteria

Android implementation is complete when:

```text
1. Evidence detail shows reading points when present.
2. Evidence detail shows comparable evidence when present.
3. Tapping unlocked comparable evidence opens its detail.
4. Tapping locked comparable evidence does not reveal hidden detail.
5. Suggested question tap preselects suspect and prefills question.
6. Suggested question tap attaches current evidence as presentedEvidenceId.
7. User must explicitly submit the question.
8. Empty guidance does not create blank UI sections.
```

## 11. AI Scope

### 11.1 V1

No AI prompt change in v1.

Reason:

```text
1. UX benefit can be achieved through evidence detail and question prefill.
2. Prompt changes increase answer leakage risk.
3. Existing interrogation endpoint already supports presentedEvidenceId.
4. Smaller PRs are easier to review and QA.
```

### 11.2 V2 Candidates

After v1 ships and QA confirms usefulness, consider:

```text
responseShape
Structured NPC reaction policy with mustAcknowledge, mustDeny, mustPointTo,
and mustNotSay.

timelineGuard
Scenario-level safe time rules that prevent invented timeline claims.

recommendedInvestigationFlow
Optional non-spoiler sequence used by hints or onboarding.
```

Do not feed these directly into prompts until ResponsePolicyResolver and prompt
safety rules are updated.

## 12. Unlock Scope

Do not add `ACTIVE_INVESTIGATION` in v1.

Use existing mechanisms first:

```text
EVIDENCE_PRESENTED
INTERROGATION
PHASE
```

Current code already supports `EVIDENCE_PRESENTED`-style progression with
condition JSON. A later PR can add a new enum only if product semantics differ
from the existing types.

## 13. Authoring Guide

### 13.1 Good Guidance

Good guidance:

```text
1. Points to observable details.
2. Encourages comparison between evidence.
3. Suggests a question without asserting the answer.
4. Uses public-safe wording.
5. Works even if the player has not solved the case.
```

Example:

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

### 13.2 Bad Guidance

Bad guidance:

```text
1. Names the culprit.
2. Confirms the true method.
3. Says a red herring is false before the player can prove it.
4. Refers to active variant truth.
5. Uses private implementation notes.
```

Bad example:

```yaml
guidance:
  readingPoints:
    - "This is the decisive proof that the real culprit used the final method."
```

## 14. Privacy And Spoiler Checklist

Before committing guidance content, check:

```text
[ ] No culprit name.
[ ] No active variant code.
[ ] No solution object copied from private YAML.
[ ] No final explanation paragraph.
[ ] No hidden NPC truth unless already player-facing.
[ ] No private seed labels or reviewer notes.
[ ] No secret, token, key, server private path, or user log.
[ ] No raw user prompt/answer logs.
```

## 15. Team Work Split

Recommended ownership:

| Area | Primary owner | Work |
|---|---|---|
| Product/story guidance rules | Scenario lead | Author public-safe guidance and review leakage risk. |
| Backend import/API | Backend play/scenario owner | YAML record, validation, persistence, evidence detail response. |
| Android UX | Android owner | Evidence detail sections, compare navigation, question prefill. |
| AI policy v2 | AI backend owner | responseShape/timelineGuard after v1 guidance proves useful. |
| QA | QA owner | Play through 30-50 interrogations and verify reduced dead ends. |

Concrete first-pass split:

```text
Backend:
- Add guidance schema support.
- Persist guidance_json.
- Return guidance from evidence detail API.
- Add validation/tests.

Android:
- Render guidance sections.
- Implement compare evidence navigation.
- Implement recommended question prefill.

Scenario author:
- Add guidance to 5-10 high-impact evidence items first.
- Avoid answer-bearing wording.
- Expand after QA confirms the pattern.
```

## 16. Rollout Plan

### Phase 1: Contract And UI

```text
1. Backend adds guidance schema, import, validation, API response.
2. Android adds evidence detail UI sections.
3. Use sample or limited private scenario guidance for QA.
```

### Phase 2: Scenario Data

```text
1. Add guidance to early and mid-game evidence first.
2. Add guidance to decisive evidence only with careful non-answer wording.
3. Run QA playthrough.
4. Adjust reading point length and question count.
```

### Phase 3: AI And Unlock Enhancements

```text
1. Consider responseShape for NPC answer quality.
2. Consider timelineGuard for time consistency.
3. Consider extra unlock rules only after evidence guidance is stable.
```

## 17. QA Scenarios

Minimum QA cases:

```text
1. Player opens early evidence and sees reading points.
2. Player taps related unlocked evidence and returns successfully.
3. Player taps related locked evidence and sees no hidden detail.
4. Player taps suggested question and reaches interrogation screen.
5. Interrogation screen has correct suspect, question, and presented evidence.
6. Player edits question before sending.
7. Evidence with no guidance still renders normally.
8. Final answer is not exposed before deduction result.
```

UX success criteria:

```text
1. Player can identify at least one next action from most key evidence.
2. Player does not need to invent all interrogation questions from scratch.
3. Player can progress without broad "what should I do next?" prompts.
4. Guidance does not make the case feel solved by the app.
```

## 18. Open Decisions

These should be decided before implementation:

```text
1. Store guidance as evidences.guidance_json or normalized tables?
   Recommendation: guidance_json for MVP.

2. Should evidence list include a hasGuidance boolean?
   Recommendation: optional. Detail-only guidance is enough for v1.

3. Should locked compared evidence reveal title?
   Recommendation: follow current locked evidence masking policy.

4. Should suggested question use EVIDENCE_PRESENTED or RECOMMENDED?
   Recommendation: EVIDENCE_PRESENTED when launched from evidence detail with
   current evidence attached.

5. Should guidance be included in AI prompt?
   Recommendation: no for v1.
```

## 19. Definition Of Done

The scenario guidance UX is done when:

```text
1. Team agrees on the guidance YAML shape.
2. Backend imports and validates guidance.
3. Evidence detail API returns lock-safe guidance.
4. Android renders all guidance sections.
5. Suggested question chip preloads interrogation without auto-submit.
6. At least one official scenario has guidance for key early/mid-game evidence.
7. QA confirms the player can find next actions without direct answer leakage.
```

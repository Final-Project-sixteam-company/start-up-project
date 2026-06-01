# Scenario YAML Schema v1

This document defines the spoiler-free YAML shape for official ClueRoom
scenario imports.

The schema is public-safe because it describes fields and validation rules
only. Actual answer-bearing YAML files must stay outside the public
repository.

## Privacy Boundary

Public repository may contain:

- YAML schema and importer design
- DTO / loader / validator / importer code
- spoiler-free sample YAML
- public prompt-boundary documentation

Public repository must not contain:

- actual official scenario YAML with answers
- `culpritCode` values for official scenarios
- variant solution text
- proof-dimension answer mappings
- full truth timelines
- NPC hidden truth
- complete evidence-role matrix by variant

Official scenario YAML with answers is private content and should be loaded
from a private local/server path such as:

```text
CLUEROOM_SCENARIO_IMPORT_PATH=/opt/clueroom/private-scenarios
```

## Root Shape

```yaml
metadata: {}
scenario: {}
victim: {}
locations: []
characters: []
evidences: []
evidenceVariantStates: []
variants: []
unlockRules: []
npcPolicies: []
scoring: {}
assets: []
```

Required root sections for a published official scenario:

| Section | Required | Public-safe shape | Contains spoiler data in real YAML |
|---|---:|---:|---:|
| `metadata` | yes | yes | no |
| `scenario` | yes | yes | no |
| `victim` | yes | yes | usually no |
| `locations` | yes | yes | no |
| `characters` | yes | yes | usually no |
| `evidences` | yes | yes | some detail can be spoiler-like |
| `evidenceVariantStates` | variant scenarios only | shape only | yes |
| `variants` | variant scenarios only | shape only | yes |
| `unlockRules` | yes | mostly | can reveal progression |
| `npcPolicies` | yes | shape only | yes |
| `scoring` | yes | shape only | yes |
| `assets` | yes | yes | no |

## Design Rules

- YAML is source content. Runtime APIs must read DB data after import.
- Every cross-reference uses canonical codes, not display names.
- Local Windows paths must never be stored in YAML.
- Images are referenced by `assetKey` or `s3ObjectKey`.
- Public/player-facing text and backend-only truth must be separate fields.
- Same `scenario.code + scenario.version` with a different content hash should fail import.
- Prompt builders must not read `variants`, `solution`, `scoring`, or full variant evidence state directly.

## Metadata

```yaml
metadata:
  schemaVersion: 1
  contentStatus: DRAFT
  locale: ko-KR
  sourceCanonRef: SAMPLE_IMPLEMENTATION_CANON_v1.md
  canonicalCodeRef: CANONICAL_CODES.md
  assetKeyPattern: official/sample/v1/{category}/{canonicalCode}.png
```

| Field | Required | Notes |
|---|---:|---|
| `schemaVersion` | yes | Must be `1` for this document. |
| `contentStatus` | yes | `DRAFT` or `PUBLISHED`. |
| `locale` | yes | Example: `ko-KR`. |
| `sourceCanonRef` | yes | Human reference only. |
| `canonicalCodeRef` | yes | Human reference for frozen codes. |
| `assetKeyPattern` | yes | Documents intended asset key convention. |

`DRAFT` may allow partial content. `PUBLISHED` must pass all validator rules.

## Scenario

```yaml
scenario:
  code: SCENARIO_SAMPLE
  version: "1.0.0"
  title: "Sample Scenario"
  description: "Short public description"
  synopsis: "Public setup text"
  genre: "closed-room mystery"
  difficulty: NORMAL
  estimatedPlayTimeMinutes: 30
  scenarioType: OFFICIAL
  visibility: PUBLIC
  status: PUBLISHED
  culpritMode: RANDOM_REQUIRED
  deductionMode: VARIANT_PROOF
  mapMode: REFERENCE_ONLY
  evidenceMode: PHASE_BASED_EVIDENCE_TAB
```

`scenario.code` and `scenario.version` are the stable import identity.
`contentHash` should be calculated by the importer, not authored by hand.

## Victim

```yaml
victim:
  code: VICTIM_SAMPLE
  name: "Victim Name"
  roleLabel: "Victim role"
  publicProfile: "Player-facing profile"
  deathLocationCode: LOC_PRIMARY_SCENE
  discoveredAtText: "Player-facing discovery time"
  publicCauseOfDeathText: "Player-facing cause text"
```

`deathLocationCode` must reference `locations[].code`.

## Locations

```yaml
locations:
  - code: LOC_PRIMARY_SCENE
    name: "Primary Scene"
    floor: "2F"
    description: "Player-facing location description"
    imageAssetKey: official/sample/v1/maps/LOC_PRIMARY_SCENE.png
    sortOrder: 10
```

Locations are used for evidence source, filtering, and future map hotspot
support. MVP does not require coordinates.

## Characters

```yaml
characters:
  - code: SUSPECT_SAMPLE
    name: "Character Name"
    roleLabel: "Role"
    characterType: CULPRIT_ELIGIBLE
    culpritEligible: true
    publicProfile: "Player-facing profile"
    publicAlibi: "Player-facing alibi claim"
    personalityTone: "calm, defensive"
    portraitAssetKey: official/sample/v1/characters/SUSPECT_SAMPLE.png
    sortOrder: 10
```

Recommended `characterType` values:

- `CULPRIT_ELIGIBLE`
- `PERMANENT_RED_HERRING`
- `NEUTRAL_WITNESS`

If `culpritEligible` is `false`, validators must reject any variant that uses
that character as culprit.

## Evidences

```yaml
evidences:
  - code: EVIDENCE_SAMPLE
    title: "Evidence Title"
    category: PHYSICAL
    unlockPhase: PHASE_1_BASIC
    locationCode: LOC_PRIMARY_SCENE
    baseRole: COMMON
    oneLine: "Short card text"
    baseDetail: "Detail text shown when the player opens this evidence."
    imageAssetKey: official/sample/v1/evidence/EVIDENCE_SAMPLE.png
    thumbnailAssetKey: official/sample/v1/evidence/EVIDENCE_SAMPLE.thumb.png
    relatedCharacterCodes:
      - SUSPECT_SAMPLE
    tags:
      - timeline
    sortOrder: 10
```

`baseDetail` is the default text shown with image evidence in the UI. If a
specific variant needs different wording, use `evidenceVariantStates`.

## Evidence Variant States

```yaml
evidenceVariantStates:
  - variantCode: VARIANT_SAMPLE
    evidenceCode: EVIDENCE_SAMPLE
    role: METHOD_KEY
    detailOverride: null
    detailAppend: "Variant-specific extra detail"
    contradictionKeys:
      - CONTRADICTION_SAMPLE
    proofDimensions:
      - METHOD
```

This section is private for real official scenarios. It tells the backend how
the same evidence functions under each active variant.

API response logic should combine:

```text
evidence.baseDetail
+ activeVariant matching detailOverride/detailAppend
```

The frontend should receive only the final player-safe evidence text for the
current session, not the full matrix.

## Variants

```yaml
variants:
  - code: VARIANT_SAMPLE
    enabled: true
    weight: 1
    culpritCode: SUSPECT_SAMPLE
    solution:
      motiveSummary: "Backend-only motive summary"
      methodSummary: "Backend-only method summary"
      coverUpSummary: "Backend-only cover-up summary"
      solutionText: "Backend-only final explanation"
      timeline:
        - order: 10
          text: "Backend-only timeline item"
      proofDimensions:
        CULPRIT:
          primary:
            - EVIDENCE_SAMPLE
          support:
            - EVIDENCE_SUPPORT
```

This section must never be exposed to AI NPC prompts or public APIs.
The importer derives the culprit display name and role from `characters[]`
using `culpritCode`; official YAML should not duplicate them as required
variant fields.

For `PUBLISHED` official YAML, every `enabled: true` variant must provide a
non-empty `solution` with `motiveSummary`, `methodSummary`, `coverUpSummary`,
`solutionText`, and `proofDimensions`. `solution.methodSummary` is the primary
source for the persisted `VariantSolution.method`; legacy top-level method
layer fields are optional fallback only.

## Unlock Rules

```yaml
unlockRules:
  - evidenceCode: EVIDENCE_SAMPLE
    unlockType: PHASE
    condition:
      requiredPhase: PHASE_1_BASIC
      requiredEvidenceCodes: []
      requiredCharacterCode: null
      requiredInterrogationTopic: null
      hintFallbackAllowed: true
    sortOrder: 10
```

Unlock rules decide when evidence appears or becomes available. MVP can use
phase-based unlocks first and add active-investigation conditions later.

## NPC Policies

```yaml
npcPolicies:
  - characterCode: SUSPECT_SAMPLE
    publicAlibi: "Public alibi claim"
    promptSafeKnowledge:
      selfRole: "What this NPC can say about their role"
      directKnowledge:
        - "Facts this NPC directly knows and may discuss when allowed"
      inferredKnowledge:
        - "Limited inferences this NPC may state"
      forbiddenKnowledge:
        - "Facts this NPC must not reveal"
    stagePolicies:
      - stage: DEFAULT
        policyText: "Answer briefly and stay within known facts."
        allowedFacts:
          - "Public alibi"
        forbiddenFacts:
          - "Backend-only truth"
        tone: "guarded"
    evidenceReactionPolicies:
      - evidenceCode: EVIDENCE_SAMPLE
        policyText: "Reaction when this evidence is presented"
        allowedFacts:
          - "Specific allowed reaction fact"
        forbiddenFacts:
          - "Specific forbidden truth"
        tone: "uneasy"
```

The importer may store NPC policy data, but the AI prompt builder must receive
only the policy selected by `ResponsePolicyResolver` for the current question,
stage, character, and presented evidence.

Prompt builders must not receive:

- `activeVariant`
- `culpritCode`
- `solutionText`
- full truth timeline
- full evidence-role matrix
- scoring answer keys

## Scoring

```yaml
scoring:
  totalScore: 100
  passScore: 70
  deductionFields:
    - field: culprit
      proofDimension: CULPRIT
      maxScore: 30
    - field: method
      proofDimension: METHOD
      maxScore: 30
  consistencyScore:
    maxScore: 10
```

Actual scoring answer keys live under each variant's solution. Public docs can
describe the scoring shape, but official mappings are private.

## Assets

```yaml
assets:
  - assetKey: official/sample/v1/evidence/EVIDENCE_SAMPLE.png
    type: EVIDENCE_IMAGE
    targetKind: EVIDENCE
    targetCode: EVIDENCE_SAMPLE
    s3ObjectKey: official/sample/v1/evidence/EVIDENCE_SAMPLE.png
    contentType: image/png
    altText: "Evidence image alt text"
```

The YAML should reference stable asset keys. A separate private asset map can
map local source filenames to final S3 keys.

## Validator Rules

For `PUBLISHED` content, the validator must enforce:

- root sections required by this document are present
- `scenario.code` and `scenario.version` are present
- all canonical `code` values are unique within their section
- `victim.deathLocationCode` exists in `locations`
- `evidences[].locationCode` exists in `locations`
- `evidences[].relatedCharacterCodes` exist in `characters`
- `variants[].culpritCode` exists in `characters`
- `variants[].culpritCode` has `culpritEligible=true`
- `PUBLISHED` content has at least one `enabled: true` variant
- enabled variants include required solution fields:
  `motiveSummary`, `methodSummary`, `coverUpSummary`, `solutionText`, `proofDimensions`
- `evidenceVariantStates[].variantCode` exists in `variants`
- `evidenceVariantStates[].evidenceCode` exists in `evidences`
- `unlockRules[].evidenceCode` exists in `evidences`
- `unlockRules[].condition.requiredEvidenceCodes` exist in `evidences`
- `npcPolicies[].characterCode` exists in `characters`
- `npcPolicies[].evidenceReactionPolicies[].evidenceCode` exists in `evidences`
- `assets[].type`, `assets[].targetKind`, and `assets[].targetCode` are present
- `assets[].targetKind + targetCode` references an existing scenario object
- `assetKey` and `s3ObjectKey` do not contain local filesystem paths
- same `scenario.code + scenario.version` with different content hash fails import

## Import Order

Recommended persistence order:

```text
1. Scenario
2. Victim
3. Locations
4. Characters
5. Evidences
6. EvidenceSuspects
7. Variants
8. VariantSolutions
9. EvidenceVariantStates
10. UnlockRules
11. NpcPolicies
12. Scoring
13. Assets
```

## Runtime Contract

At play-session start:

```text
scenario selected
  -> active variant selected by backend
  -> activeVariant stored on PlaySession
```

During evidence lookup:

```text
base evidence
  -> activeVariant evidence state
  -> player-safe evidence response with image URL and detail text
```

During interrogation:

```text
user question + character + presented evidence
  -> ResponsePolicyResolver
  -> prompt-safe policy only
  -> AiPromptBuilder
```

During final deduction:

```text
submitted answer
  -> activeVariant solution
  -> proof-dimension scoring
  -> result response
```

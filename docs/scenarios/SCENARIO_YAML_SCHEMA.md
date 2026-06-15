# Scenario YAML Schema v1 한글 정본

이 문서는 공식 ClueRoom 시나리오 import에 사용하는 spoiler-free YAML 구조를 정의한다.

이 스키마는 field와 validation rule만 설명하므로 public-safe하다.
정답을 포함한 실제 공식 YAML 파일은 public repository 밖에 있어야 한다.

## Privacy Boundary 공개 범위 경계

Public repository에 포함해도 되는 것:

- YAML schema와 importer design
- DTO / loader / validator / importer code 코드
- spoiler-free sample YAML 예시
- public prompt-boundary documentation 문서
- 정답을 포함하지 않는 content pipeline note

Public repository에 포함하면 안 되는 것:

- 정답이 포함된 실제 공식 scenario YAML
- 공식 scenario의 `culpritCode` 값
- variant solution text 정답 문구
- proof-dimension answer mapping 정답 매핑
- full truth timeline 전체 진실 타임라인
- NPC hidden truth 숨겨진 진실
- variant별 complete evidence-role matrix

팀 내부 전용 scenario material:

```text
- working brief history
- implementation canon with culprit / variant truth
- variant solution
- proof dimension answer mapping
- NPC hidden knowledge and forbidden truth
- final explanation with answer-bearing reasoning
```

정답이 포함된 공식 scenario YAML은 private content다.
아래와 같은 private local/server path에서 load해야 한다.

```text
CLUEROOM_SCENARIO_IMPORT_PATHS=/opt/clueroom/private-scenarios
```

`application.yml`은 복수형 `CLUEROOM_SCENARIO_IMPORT_PATHS`만 바인딩한다.
단수형 `CLUEROOM_SCENARIO_IMPORT_PATH`는 현재 코드에서 읽지 않는다.

## Root Shape 루트 구조

```yaml
metadata: {}
scenario: {}
victim: {}
locations: []
characters: []
evidences: []
timelineEvents: []
evidenceVariantStates: []
variants: []
hints: []
unlockRules: []
npcPolicies: []
scoring: {}
assets: []
```

Published official scenario에서 필요한 root section:

| Section | Required | Public-safe shape | 실제 YAML에서 spoiler data 포함 가능성 |
|---|---:|---:|---:|
| `metadata` | yes | yes | no |
| `scenario` | yes | yes | no |
| `victim` | yes | yes | 보통 no |
| `locations` | yes | yes | no |
| `characters` | yes | yes | 보통 no |
| `evidences` | yes | yes | 일부 detail은 spoiler-like일 수 있음 |
| `timelineEvents` | no | yes | public event로 제한하면 no |
| `evidenceVariantStates` | variant scenario에서만 | shape only | yes |
| `variants` | variant scenario에서만 | shape only | yes |
| `hints` | yes | yes | hint content는 player-facing으로만 작성 |
| `unlockRules` | yes | mostly | progression을 드러낼 수 있음 |
| `npcPolicies` | yes | shape only | yes |
| `scoring` | yes | shape only | yes |
| `assets` | yes | yes | no |

## Design Rules 설계 규칙

- YAML은 source content다. Runtime API는 import 이후 DB data를 읽어야 한다.
- 모든 cross-reference는 display name이 아니라 canonical code를 사용한다.
- Local Windows path는 YAML에 저장하면 안 된다.
- Image는 `assetKey` 또는 `s3ObjectKey`로 참조한다.
- Public URL로 노출될 수 있는 asset key/file name에는 `CULPRIT`, `FAKE`, `RED_HERRING`, `CORE`, solution role 같은 정답성 marker를 넣지 않는다.
- Public/player-facing text와 backend-only truth는 별도 field로 분리한다.
- 같은 `scenario.code + scenario.version`인데 content hash가 다르면 import가 실패해야 한다.
- Prompt builder는 `variants`, `solution`, `scoring`, full variant evidence state를 직접 읽으면 안 된다.

## Metadata 메타데이터

```yaml
metadata:
  schemaVersion: 1
  contentStatus: DRAFT
  locale: ko-KR
  sourceCanonRef: SAMPLE_IMPLEMENTATION_CANON_v1.md
  canonicalCodeRef: CANONICAL_CODES.md
  assetKeyPattern: official/sample/v1/{category}/{canonicalCode}.png
```

| Field | Required | Notes 설명 |
|---|---:|---|
| `schemaVersion` | yes | 이 문서에서는 `1`이어야 한다. |
| `contentStatus` | yes | `DRAFT` 또는 `PUBLISHED`. |
| `locale` | yes | 예: `ko-KR`. |
| `sourceCanonRef` | yes | 사람이 참고하는 reference. |
| `canonicalCodeRef` | yes | frozen code를 확인하는 human reference. |
| `assetKeyPattern` | yes | 의도한 asset key convention을 문서화한다. |

`DRAFT`는 부분 content를 허용할 수 있다. `PUBLISHED`는 모든 validator rule을 통과해야 한다.

## Scenario 시나리오

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

`scenario.code`와 `scenario.version`은 stable import identity다.
`contentHash`는 작성자가 직접 쓰지 않고 importer가 계산해야 한다.

### Existing Official Seed Hotfix 기존 공식 seed hotfix

공식 시나리오가 이미 운영 DB에 import된 뒤 `evidences[].guidance` 같은 보조 UX 데이터만 보강하는 경우에는 일반 신규 버전 import와 hotfix 절차를 구분한다.

Hotfix 기준:

- 기존 운영 row와 같은 `scenario.code + scenario.version`을 유지한다.
- `guidance_json`처럼 대상이 명확한 DB column만 별도 SQL로 patch한다.
- 서버 private YAML의 SHA-256과 DB `scenarios.content_hash`를 같은 값으로 맞춘다.
- 배포 후 importer 로그가 `SKIPPED <scenario>@<version>`인지 확인한다.

주의:

- `scenario.version`만 올리고 기존 `assetKey`를 재사용한 full import를 시도하지 않는다.
- 현재 운영 schema에서는 `scenario_assets.asset_key`가 unique이므로, 같은 asset key를 쓰는 새 scenario version import가 실패할 수 있다.
- 이 절차는 private seed 운영 절차이며 실제 YAML/SQL 내용은 public repository에 커밋하지 않는다.

명령형 절차는 [Run and Deploy Guide의 공식 시나리오 guidance seed hotfix](../RUN_AND_DEPLOY.md#101-공식-시나리오-guidance-seed-hotfix)를 따른다.

## Victim 피해자

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

`deathLocationCode`는 `locations[].code`를 참조해야 한다.

## Locations 장소

```yaml
locations:
  - code: LOC_PRIMARY_SCENE
    name: "Primary Scene"
    floor: "2F"
    description: "Player-facing location description"
    imageAssetKey: official/sample/v1/maps/LOC_PRIMARY_SCENE.png
    mapX: 120
    mapY: 80
    sortOrder: 10
```

Location은 evidence source, filtering, scene map hotspot에 사용된다.
`PUBLISHED` official scenario에서는 frontend가 scenario map에서 selectable location point를 그릴 수 있도록 `mapX`와 `mapY`가 필요하다.

## Characters 인물

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

권장 `characterType` 값:

- `CULPRIT_ELIGIBLE`
- `PERMANENT_RED_HERRING`
- `NEUTRAL_WITNESS`

`culpritEligible`이 `false`이면 validator는 해당 character를 culprit로 사용하는 variant를 거부해야 한다.

## Evidences 증거

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
    guidance:
      readingPoints:
        - "What the player should notice in this evidence."
      compareWithEvidenceCodes:
        - EVIDENCE_RELATED_SAMPLE
      suggestedQuestions:
        - targetCharacterCode: SUSPECT_SAMPLE
          question: "What should be clarified about this evidence?"
```

`baseDetail`은 UI에서 image evidence를 열었을 때 기본으로 보여주는 text다.
특정 variant에서 다른 wording이 필요하면 `evidenceVariantStates`를 사용한다.

## Evidence Guidance 증거 안내 메타데이터

`guidance`는 증거 상세 API용 player-facing UX metadata다.
import 시 `evidences.guidance_json`에 저장되고, 현재 증거가 해금되어 상세 조회 가능한 경우에만 응답에 포함된다.

Guidance 규칙:

- `readingPoints`는 플레이어가 이미 해금된 증거에서 읽어야 할 짧은 관찰점이다.
- `compareWithEvidenceCodes`는 기존 `evidences[].code`만 참조한다.
- `compareWithEvidenceCodes`는 현재 증거 자신을 참조하지 않는다.
- `suggestedQuestions[].targetCharacterCode`는 기존 `characters[].code`만 참조한다.
- `suggestedQuestions[].question`은 player-facing 문장이고 정답을 단정하지 않는다.
- guidance에는 culprit identity, active variant truth, solution text, private seed note, 정답성 추론을 넣지 않는다.
- guidance text에 `culprit`, `culpritCode`, `variant`, `activeVariant`, `solution`, `solutionText`, `proofDimensions`, `정답`, `범인` 같은 private/solution marker가 들어가면 validation에서 거부한다.

Guidance와 hint의 역할은 분리한다.

```text
guidance: 이미 해금된 증거를 어떻게 읽을지 안내한다.
hint: 플레이어가 막혔을 때 다음 조사 방향을 안내한다.
```

허용 문장 예:

```text
이 시간이 행동 시점인지 확인 질문을 해보세요.
이 증거의 위치가 이전 기록과 같은지 비교해 보세요.
이 증거만으로 단정하지 말고 관련 증거와 함께 보세요.
```

금지 문장 예:

```text
이 인물의 주장은 거짓입니다.
이 증거가 정답 경로입니다.
범인은 이 증거와 직접 연결됩니다.
현재 Variant의 진실을 확인하세요.
```

Runtime 노출 정책:

- 현재 증거가 잠겨 있으면 기존 증거 상세 정책과 동일하게 상세 응답을 주지 않는다.
- 비교 대상 증거가 잠겨 있으면 `title`, lock state, `unlockHint` 같은 lock-safe field만 내려주고 `evidenceCode`는 노출하지 않는다.
- 추천 질문은 UI prefill data다. Android는 자동 전송하지 않고 사용자가 확인 후 직접 제출하게 해야 한다.

## Timeline Events 타임라인 이벤트

```yaml
timelineEvents:
  - code: TIMELINE_SAMPLE_2100_DISCOVERY
    eventOrder: 10
    eventTime: "21:00"
    title: "Public Timeline Title"
    description: "Player-facing event description"
    eventType: FACT
    visibility: PUBLIC
    isTrueEvent: true
    locationCode: LOC_PRIMARY_SCENE
    relatedEvidenceCode: EVIDENCE_SAMPLE
    relatedCharacterCode: SUSPECT_SAMPLE
```

`timelineEvents`는 `GET /api/play-sessions/{sessionId}/timeline`용 `timeline_events`로 import된다.

Official YAML에는 player-safe public timeline item만 넣어야 한다.
현재 `visibility`는 `PUBLIC`만 허용한다. `PULBIC` 같은 typo는 모든 player에게 event를 조용히 숨기지 말고 import validation에서 실패해야 한다.
Variant truth timeline, culprit-only action, hidden cover-up step, final solution timeline은 backend-only variant/solution data 또는 private design document에 남겨야 한다.
`relatedEvidenceCode`가 있으면 runtime은 해당 evidence가 unlock될 때까지 event를 숨길 수 있다.

## Evidence Variant States 증거 Variant 상태

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

실제 official scenario에서 이 section은 private이다.
같은 evidence가 각 active variant에서 어떤 역할을 하는지 backend에 알려준다.

API response logic은 아래를 결합해야 한다.

```text
evidence.baseDetail
+ activeVariant matching detailOverride/detailAppend
```

Frontend는 full matrix가 아니라 current session에 대한 최종 player-safe evidence text만 받아야 한다.

## Variants 변형 정답

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

이 section은 AI NPC prompt 또는 public API에 절대 노출하면 안 된다.
Importer는 `culpritCode`를 사용해 `characters[]`에서 culprit display name과 role을 가져온다.
Official YAML은 이 값을 required variant field로 중복 저장하지 않아야 한다.

`PUBLISHED` official YAML에서 `enabled: true`인 모든 variant는 비어 있지 않은 `solution`을 제공해야 한다.
필수 field는 `motiveSummary`, `methodSummary`, `coverUpSummary`, `solutionText`, `proofDimensions`다.
`solution.methodSummary`는 persisted `VariantSolution.method`의 primary source다.
Legacy top-level method layer field는 optional fallback일 뿐이다.

## Hints 힌트

```yaml
hints:
  - hintLevel: 1
    content: "First non-spoiler investigation direction."
    unlockAfterMinutes: 0
    penaltyScore: 5
```

`hints`는 `hints` table로 import되고 `GET /api/play-sessions/{sessionId}/hints`에서 목록 메타데이터로 사용된다.
힌트 본문 `content`는 사용자가 `/hints/{hintId}/use`를 호출한 뒤에만 노출된다.

Guidance와 hint의 역할은 분리한다.

```text
guidance: 이미 해금된 증거를 어떻게 읽을지 안내한다.
hint: 플레이어가 막혔을 때 다음 조사 방향을 안내한다.
```

힌트 문장은 player-facing이어야 하며, private seed note, solution text, variant truth, 정답 후보 단정 문장을 넣지 않는다.
`hintLevel`은 1부터 시작하고 중복되지 않아야 한다.
`unlockAfterMinutes`와 `penaltyScore`는 0 이상이어야 한다.

## Unlock Rules 해금 규칙

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

Unlock rule은 evidence가 언제 나타나거나 사용 가능해지는지 결정한다.
MVP는 phase-based unlock을 먼저 사용하고, 나중에 active-investigation condition을 추가할 수 있다.

## NPC Policies NPC 정책

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

Importer는 NPC policy data를 저장할 수 있다.
하지만 AI prompt builder는 현재 suspect, unlocked evidence IDs, presented evidence ID에 대해 `ResponsePolicyResolver`가 선택한 policy만 받아야 한다.

Prompt builder가 받으면 안 되는 것:

- `activeVariant`
- `culpritCode`
- `solutionText`
- full truth timeline 전체 진실 타임라인
- full evidence-role matrix
- scoring answer keys

## Scoring 채점

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
      maxScore: 25
    - field: motive
      proofDimension: MOTIVE
      maxScore: 20
    - field: coverUp
      proofDimension: COVER_UP
      maxScore: 10
  evidenceScore:
    maxScore: 15
```

실제 scoring answer key는 각 variant의 solution 아래에 있다.
Public docs는 scoring shape를 설명할 수 있지만 official mapping은 private이다.
현재 code의 기본 score split은 culprit 30, method 25, motive 20, coverUp 10, evidence 15다.

MVP importer bridge:

```text
Importer는 variants[].solution.proofDimensions를 보존한다.
현재 final deduction scoring path와 호환되도록 solution evidence code를 VariantSolution.keyEvidenceIds로도 변환한다.
Proof-dimension-specific scoring은 이후 proofDimensionJson을 authoritative scoring source로 삼도록 이동해야 한다.
```

## Assets 에셋

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

YAML은 stable asset key를 참조해야 한다.
별도의 private asset map으로 local source filename을 final S3 key에 매핑할 수 있다.
S3 object key는 public image URL에 그대로 보일 수 있으므로 player-facing 중립 이름을 사용한다.
예: `characters/character-001.png`, `evidence/evidence-014.png`.
금지 예: `characters/CULPRIT_*.png`, `evidence/FAKE_*.png`, `evidence/CORE_*.png`.

## Validator Rules 검증 규칙

`PUBLISHED` content에 대해 validator는 아래를 강제해야 한다.

- 이 문서에서 required로 지정한 root section이 존재한다.
- `scenario.code`와 `scenario.version`이 존재한다.
- 모든 canonical `code` 값이 해당 section 안에서 unique하다.
- `victim.deathLocationCode`가 `locations`에 존재한다.
- `evidences[].locationCode`가 `locations`에 존재한다.
- `evidences[].relatedCharacterCodes`가 `characters`에 존재한다.
- `evidences[].guidance.compareWithEvidenceCodes`가 `evidences`에 존재한다.
- `evidences[].guidance.compareWithEvidenceCodes`가 현재 증거 자신을 참조하지 않는다.
- `evidences[].guidance.suggestedQuestions[].targetCharacterCode`가 `characters`에 존재한다.
- `evidences[].guidance.suggestedQuestions[].question`이 비어 있지 않다.
- `evidences[].guidance` text에 blocked private/solution marker가 들어가지 않는다.
- `timelineEvents[].eventOrder` 값이 unique하다.
- `timelineEvents[].locationCode`가 `locations`에 존재한다.
- `timelineEvents[].relatedEvidenceCode`가 `evidences`에 존재한다.
- `timelineEvents[].relatedCharacterCode`가 `characters`에 존재한다.
- `variants[].culpritCode`가 `characters`에 존재한다.
- `variants[].culpritCode`의 `culpritEligible=true`다.
- `PUBLISHED` content는 최소 하나의 `enabled: true` variant를 가진다.
- enabled variant는 필수 solution field를 가진다: `motiveSummary`, `methodSummary`, `coverUpSummary`, `solutionText`, `proofDimensions`.
- `evidenceVariantStates[].variantCode`가 `variants`에 존재한다.
- `evidenceVariantStates[].evidenceCode`가 `evidences`에 존재한다.
- `unlockRules[].evidenceCode`가 `evidences`에 존재한다.
- `unlockRules[].condition.requiredEvidenceCodes`가 `evidences`에 존재한다.
- `npcPolicies[].characterCode`가 `characters`에 존재한다.
- `npcPolicies[].evidenceReactionPolicies[].evidenceCode`가 `evidences`에 존재한다.
- `assets[].type`, `assets[].targetKind`, `assets[].targetCode`가 존재한다.
- `assets[].targetKind + targetCode`가 실제 scenario object를 참조한다.
- `assetKey`와 `s3ObjectKey`에 local filesystem path가 들어가지 않는다.
- 같은 `scenario.code + scenario.version`인데 content hash가 다르면 import가 실패한다.

## Import Order Import 순서

권장 persistence 순서:

```text
1. Scenario
2. Locations
3. Victim
4. Characters
5. Evidences
6. EvidenceSuspects
7. TimelineEvents
8. Variants
9. VariantSolutions
10. EvidenceVariantStates
11. UnlockRules
12. NpcPolicies
13. Scoring
14. Assets
```

## Runtime Contract 런타임 계약

Play-session 시작 시:

```text
scenario selected
  -> active variant selected by backend
  -> activeVariant stored on PlaySession
```

Evidence lookup 중:

```text
base evidence
  -> activeVariant evidence state
  -> player-safe evidence response with image URL and detail text
```

Interrogation 중:

```text
suspect + unlocked evidence IDs + presented evidence ID
  -> ResponsePolicyResolver
  -> prompt-safe policy only
  -> AiPromptBuilder
```

Final deduction 중:

```text
submitted answer
  -> activeVariant solution
  -> proof-dimension scoring
  -> result response
```

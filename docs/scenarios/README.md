# Scenario Documentation Policy

이 디렉터리는 public repository에 공유해도 되는 spoiler-free 시나리오 구현 메모만 보관한다.

범인 Variant, Solution, Proof Dimension 정답표, NPC hidden truth, 최종 해설을 포함한 내부 시나리오 문서는 커밋하지 않는다.

아래 문서는 private 팀 공유 위치에서만 관리한다.

- Working brief history
- Implementation canon with culprit / variant truth
- Variant solution
- Proof Dimension answer mapping
- NPC hidden knowledge and forbidden truth

public repository에 둘 수 있는 문서는 아래 범위로 제한한다.

- YAML importer design
- Spoiler-free schema rules
- AI NPC prompt boundary policy
- Backend implementation guidance
- Non-answer content pipeline notes

## Public Scenario Docs

- `SCENARIO_YAML_SCHEMA.md`: 공식 시나리오 YAML의 spoiler-free 구조, 검증 규칙, 런타임 연결 계약.

## MVP Scoring Note

현재 importer는 `variants[].solution.proofDimensions`를 보존하면서도, 기존 최종 추리 채점 코드와 연결하기 위해 solution 내부 evidence code들을 `VariantSolution.keyEvidenceIds`로 변환하는 legacy bridge를 함께 사용한다.

Proof Dimension별 세부 배점/채점은 후속 작업에서 `proofDimensionJson` 기준으로 분리한다.

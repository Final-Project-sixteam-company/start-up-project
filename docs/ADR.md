# Architecture Decision Records

> CaseLab AI 프로젝트의 핵심 설계 결정을 기록한다.
> 형식: 결정 / 대안 / 근거 / Trade-off

---

## ADR-1. 트랜잭션 밖 AI 호출 (TX1/TX2 분리)

- **결정:** DB 조회(TX1) -> AI 호출(트랜잭션 밖) -> 결과 저장(TX2)
- **대안:** 하나의 트랜잭션에서 AI 호출 포함
- **근거:** AI 응답 지연 동안 DB 커넥션과 lock을 점유하면 다른 요청이 지연되고 데드락 위험이 증가한다.
- **Trade-off:** AI 성공 후 TX2 실패 시 결과 유실 가능성이 있다. 에러 분기 분리와 재시도 경로로 보완한다.

---

## ADR-2. ResponsePolicyResolver - AI에게 정책 판단을 맡기지 않는다

- **결정:** 백엔드가 증거 해금 상태 기반으로 답변 정책을 결정하고, AI는 정책을 자연어로 변환한다.
- **대안:** AI에게 전체 컨텍스트를 주고 스스로 판단하게 한다.
- **근거:** AI 판단에 맡기면 프롬프트 인젝션이나 환각으로 정답이 누설될 수 있다.
- **Trade-off:** 정책 테이블 관리 부담이 생긴다.

---

## ADR-3. Mock*Reader 패턴 - 도메인 간 의존성 분리

- **결정:** AI 도메인이 scenario/play Entity를 직접 참조하지 않고 Reader 인터페이스를 통해 데이터를 조회한다.
- **대안:** AI 서비스에서 ScenarioRepository, PlaySessionRepository를 직접 주입한다.
- **근거:** 담당자 간 병렬 개발이 가능하고 AI 도메인 단독 테스트가 쉬워진다.
- **Trade-off:** Mock 데이터와 실 데이터가 불일치할 수 있다. 인터페이스 반환 계약과 계약 테스트로 보완한다.

---

## ADR-4. 세션 완료 시점 = TX2 저장 성공 시 + in-flight lock

- **결정:** FinalDeduction 저장과 PlaySession COMPLETED 전환을 같은 TX2에서 수행하고, AI 호출 전 in-flight lock으로 동시 요청을 차단한다.
- **대안:** AI 호출 전 SCORING 상태로 전환한다.
- **근거:** 결과 없이 COMPLETED가 되면 결과 조회가 깨지고, 동시 요청 시 AI 비용이 이중 발생한다.
- **Trade-off:** lock 실패 시 해제 경로가 필요하다. releaseFinalDeductionLock()으로 보완한다.

---

## ADR-5. 정답 누설 방지 - 구조적 분리 + 회귀 테스트

- **결정:** 심문 프롬프트 빌더는 SolutionInfo를 입력으로 받지 않는다. AiPromptBuilderTest로 SolutionInfo 필드가 심문 프롬프트에 포함되지 않음을 검증한다.
- **대안:** 코드 리뷰로만 확인한다.
- **근거:** 새 PR에서 정답 누설이 회귀하는 것을 자동 감지해야 한다.
- **Trade-off:** 테스트 유지 비용이 생긴다.

---

## ADR-6. 시나리오 검증 결과는 이력 저장, in-flight만 lock

- **결정:** ScenarioValidationResult는 scenario_id unique 없이 이력으로 저장한다. 동일 scenarioId의 동시 검증만 Redis lock으로 차단한다.
- **대안:** scenario_id unique로 최신 결과를 덮어쓴다.
- **근거:** 검증 결과 변화 추적이 가능하고, AI 비용 중복은 in-flight lock으로 충분히 막을 수 있다.
- **Trade-off:** 최신 결과 조회 시 정렬 기준이 필요하다. checkedAt DESC, id DESC로 조회한다.

---

## ADR-7. AI provider 호출 quota는 계정+시나리오 150회/day, 계정 전체 350회/day로 둔다

- **결정:** 실제 AI provider 호출 전에 Redis daily counter를 차감하고, 계정+시나리오별 150회/day와 계정 전체 350회/day를 적용한다. 초과 시 provider를 호출하지 않고 `429 / AI_RATE_002`로 차단한다.
- **대안:** 50회 hard stop, 세션 단위 hard cap, feature별 quota, 전역 비용 cap만 적용.
- **근거:** QA 문서 기준 목표는 30~50턴 내 후보 축소지만, 2026-06-18 Android E2E QA에서는 공식 시나리오가 30~50턴 안에 안정적으로 submit-ready에 도달하지 못했고 extended route가 필요했다. LLMOps 비용 계획도 현재 baseline을 80~100회 플레이 + buffer로 보고 50회 hard stop을 금지한다. 따라서 150회는 하루에 공식 시나리오 하나를 충분히 플레이할 수 있는 상한이며, 350회 total은 같은 계정이 서월채와 스튜디오9를 모두 길게 플레이할 여지를 둔다.
- **Trade-off:** 150회까지 무제한으로 방치하면 UX가 반복 심문으로 흐를 수 있다. 그래서 35/50/70/100/120회 threshold에서 정리, 힌트, 추천 질문, 최종 추리 유도를 프론트가 표시할 수 있도록 quota metadata를 응답에 포함한다.
- **관측:** quota hit는 provider failure가 아니므로 `AI_QUOTA_BLOCK`과 별도 metric으로 기록한다. Redis quota 상태를 확인할 수 없으면 비용 방어를 위해 `503 / AI_RATE_003`으로 실패시킨다.
- **후속 후보:** 세션 단위 hard cap, feature별 quota, 월간 quota, 관리자 override audit은 실제 운영 데이터가 쌓인 뒤 분리한다.

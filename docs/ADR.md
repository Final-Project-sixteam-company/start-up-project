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

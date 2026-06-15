# ClueRoom QA Handoff

## 0. 문서 목적

이 문서는 MVP QA의 현재 상태를 보는 정본 handoff다.
날짜별 QA 원문 문서를 계속 늘리는 대신, 현재 미해결 이슈와 해결된 이슈, 재검증 항목을 이 문서에 흡수한다.

```text
원칙:
1. 긴 실행 로그 원문은 반복해서 붙이지 않는다.
2. 현재 의사결정에 필요한 결론, 영향, 재검증 기준만 남긴다.
3. 정답/variant/private seed 상세는 public 문서에 추가하지 않는다.
4. 해결된 이슈는 Open 목록에 남기지 않고 Resolved로 이동한다.
5. 원문이 필요한 경우 source 문서 링크를 남긴다.
```

## 1. Source

| Source | 상태 |
|---|---|
| `MVP_QA_ISSUE_HANDOFF_2026-06-04.md` | 흡수 완료. 최종 링크 정리 후 제거했다. 원문은 Git history에서 확인한다. |
| `MVP_PLAY_FLOW_QA_2026-06-04.md` | scrub 완료. 정답/variant/증거 ID/운영 marker 상세가 섞여 있어 원문은 커밋하지 않고, public-safe 결론만 이 문서에 흡수한다. |
| PR #55의 2026-06-10 QA 문서 | 흡수 완료. 공개 문서에는 정답/세션 식별자/오답 후보 역할군/solution 원문을 남기지 않는다. |

흡수 완료 범위:

| Source section | 흡수 위치 |
|---|---|
| 백엔드 / AI 담당 P0/P1/P2 | `Open Issues > Backend / AI`, `Resolved / Verified`, `Re-smoke Checklist` |
| 인프라 / 운영 담당 | `Open Issues > Infra / Ops / Privacy`, `Ops / Privacy` 재검증 항목 |
| Android / Frontend 담당 | `Open Issues > Android / Frontend`, `Android / Frontend` 재검증 항목 |
| Android Implementation Notes / Fix DoD | `Re-smoke Checklist > Android / Frontend` |
| 2026-06-05 Evidence Presented Smoke | `Resolved / Verified` |
| 2026-06-05 Demo Variant Final Deduction Smoke | `Resolved / Verified` |
| 2026-06-05 Codex Local Re-check | `Resolved / Verified`, 남은 이슈는 `Open Issues` |
| 2026-06-10 Scenario Play QA | `2026-06-10 Scenario Play QA Absorption`, 남은 이슈는 `Open Issues`와 `Re-smoke Checklist` |

## 2. 현재 결론

```text
운영 API 기준 MVP 기본 플레이 흐름은 동작한다.
다만 능동 증거 제시 기반 해금, validation/hint 정책, 일부 final-deduction 상태 전이, Android E2E UX, 운영 로그 privacy는 계속 재검증이 필요하다.
```

기본 동작 확인 범위:

```text
시나리오 목록/상세
이미지 URL
세션 생성/active 복구
장소/타임라인/증거 조회
증거 잠금 마스킹
시간 기반 PHASE 해금
심문 기본 응답/로그 저장
최종 추리 제출/결과 조회
동시 final-deduction lock
```

## 2.1 2026-06-10 Scenario Play QA Absorption

Android E2E와 운영 API 기준으로 공식 시나리오 2종을 신규 유저 관점에서 플레이한 결과를 흡수한다.

공개 문서 원칙:

```text
정답 범인명, 정답 수법 원문, private seed, solution 원문, raw session id는 기록하지 않는다.
제출 후보 라벨, 정오 breakdown, 특정 오답 후보 역할군도 공개 문서에 남기지 않는다.
상세 raw result와 정답 검증 산출물은 private handoff 또는 로컬 QA 산출물에서만 확인한다.
```

핵심 결론:

```text
기본 API와 AI 응답은 동작한다.
다만 현재 플레이 감각은 "심문으로 후보를 좁혀가는 게임"보다 "시간이 지나 핵심 증거가 열릴 때까지 기다리는 게임"에 가깝다.
AI 답변은 자백 방지는 잘 지키지만, 핵심 사실 인정과 다음 비교 대상 안내가 약하다.
30~50회 심문 안에 막히지 않고 최종 후보를 좁히려면 결정론적 guidance layer가 필요하다.
```

관찰:

```text
- 이미지/기록형 증거는 모바일에서 판독값을 놓치기 쉽다.
- 일부 시나리오는 후반 핵심 증거가 열리기 전 체감 난이도가 높다.
- 증거를 보더라도 무엇과 비교하고 누구에게 물어봐야 하는지 안내가 부족하다.
- 추천 질문/힌트가 없으면 회피형 AI 답변을 반복해서 캐묻게 된다.
- 시간 PHASE 해금만으로는 "기다리면 열린다"는 진행감이 생긴다.
```

후속 방향:

```text
Backend:
- evidence detail `guidance` 계약으로 readingPoints / compareEvidences / suggestedQuestions를 제공한다.
- 별도 recommended-questions API보다 증거 상세 guidance를 우선한다.

Android:
- 해금된 현재 증거 상세에서 guidance를 표시한다.
- suggestedQuestions는 심문 화면 prefill까지만 사용하고 자동 전송하지 않는다.
- target suspect가 현재 플레이에서 유효하지 않으면 chip을 숨기거나 disabled 처리한다.

Scenario seed:
- 공식 seed에 증거별 판독 포인트, 비교 증거, 추천 질문을 추가한다.
- 문장은 "보는 방법 안내" 수준으로 제한하고 정답/범인/variant truth를 암시하지 않는다.

AI policy v2:
- timeline hard guard, responseShape, red herring refutation은 v1 QA 검증 후 별도 설계로 진행한다.
```

재검증 목표:

```text
- 신규 플레이에서 30~50회 심문 안에 최종 후보를 1~2명으로 좁힐 수 있는가
- 앱이 대신 풀어준 느낌 없이 "무엇을 비교할지"만 안내하는가
- 증거 제시 답변이 다음 비교 대상 1개 이상을 제공하는가
- 플레이어용 API에 정답성 메타데이터가 남아 있지 않은가
- 최종 추리 결과가 timeout/network 후에도 result 화면에서 복구되는가
```

## 3. Open Issues

### Backend / AI

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P1 | 능동 증거 제시 기반 해금 E2E | 현재 런타임에서 지원되는 능동 해금 경로는 `EVIDENCE_PRESENTED`다. 일반 `INTERROGATION` unlock type은 schema/enum에는 있으나 심문 write-path에서 처리하지 않는다. | official 1.1.2 seed import 후 특정 증거 제시로 `InterrogationResponse.unlockedEvidences`와 evidence board count가 증가하는지 재검증. |
| P1 | official scenario validation `NEEDS_FIX` | scenario validation이 hints=0 때문에 `NEEDS_FIX`로 확인됨. YAML `hints` import 지원과 private 1.1.2 hint seed 준비는 완료. | 운영 seed import 후 `GET /hints`, `/use`, validation status 재검증. |
| P1 | PHASE 해금이 시간 경과만으로 과도하게 열림 | Studio9 QA에서 15분 후 전체 증거 공개 흐름 확인. private 1.1.2 seed에서 후반 증거 일부를 `EVIDENCE_PRESENTED`로 전환해 시간 자동 공개량을 줄였다. | official 1.1.2 import 후 0/5/10/15분 count와 능동 제시 해금 count를 재측정. |
| P1 | AI policy 반응 약함 | FREE와 EVIDENCE_PRESENTED 답변 차이가 약하고, topic/userIntent/stagePolicies 활용이 제한적이다. | questionType + presentedEvidenceId 기반 정책 강화 후 답변 차이 smoke. |
| P2 | final-deduction 입력 품질 제한 | 긴 텍스트 제한, selectedEvidenceIds 중복 처리 등 UX/계약 정리가 필요하다. | API Spec과 validation 정책 확정. |
| P2 | suspect detail 데이터 품질 | `relationToVictim=null`, publicProfile/publicStatement 중복 기록. | seed/import 또는 response mapping 정리. |

### Android / Frontend

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P0/P1 | Studio9 앱 진입 차단 | 백엔드/운영 seed는 플레이 가능 상태지만 Android 상세에서 준비 중 gate로 차단된다. | 데모 대상이면 앱 whitelist에 포함하거나 백엔드 `canPlay`를 신뢰하도록 전환. |
| P1 | P002 active session 복구 계약 연동 | 앱 로컬 sessionId가 없으면 `details.activeSessionId`와 `/active` fallback을 사용하지 못한다. | `error.details.activeSessionId` 우선, 없으면 `GET /active` fallback 적용. |
| P1 | Timeline API 화면 연동 | 서버 timeline API는 존재하지만 앱은 placeholder/준비 중 화면을 표시한다. | timeline 탭에서 서버 응답 표시, `isTrueEvent` 미기대 확인. |
| P1 | 시나리오 목록/상세 이미지 사용 | API/S3 이미지는 정상이나 Android 목록/상세는 placeholder를 표시한다. | `thumbnailUrl`, `coverImageUrl` 우선 사용. map/place image는 발견성 UX 개선. |
| P1 | 라이브러리 검색/필터 반영 | 검색/필터 UI가 있어도 결과가 바뀌지 않아 고장처럼 보일 수 있다. | 백엔드 필터 구현 또는 MVP에서 미지원 필터 숨김. |
| P1 | 브리핑/결과 문구와 실제 시나리오 정보 불일치 | 피해자 정보, 결정적 증거 개수, 내부 variant 용어 노출 등 하드코딩/내부 용어 문제가 확인됐다. | 서버 scenario/채점 계약 기반 copy로 정리하고 내부 용어 제거. |
| P1 | 현장 지도/장소 이미지 발견성 | map/place image는 렌더링되지만 선택 결과와 이미지 위치가 약해 사용자가 놓칠 수 있다. | 장소 선택 피드백, 이미지 CTA, 지도 영역 UX 개선. |
| P2 | Hint 빈 상태 UX | hints=0일 때 명확한 빈 상태/닫기 동선 없이 힌트 안내만 보인다. | "현재 사용할 수 있는 힌트가 없습니다" 류의 empty state 적용. |

### Infra / Ops / Privacy

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P0 | 운영 로그에 사용자 입력 원문 노출 방지 | 2026-06-04 운영 QA에서 Hibernate bind parameter TRACE로 사용자 질문/최종 추리 입력 원문 노출이 확인됐다. 이후 설정 변경 여부 재확인이 필요하다. | prod `HIBERNATE_SQL_PARAM_LOG` off/warn, `org.hibernate.orm.jdbc.bind` TRACE 비활성, marker 재검증. |
| P1 | AI/LLMOps 로그 privacy | AI 고도화용 로그는 필요하지만 prompt/answer/user question 원문 저장은 금지해야 한다. | `AI_CALL`은 원문 미포함을 확인한다. `AI_CALL_CONTEXT`는 prompt block estimate/templateHash만 남기고 prompt/answer/user question 원문과 sessionId/scenarioId/suspectId/npcCode를 남기지 않는지 확인한다. |

## 4. Resolved / Verified

| 날짜 | 항목 | 결과 |
|---|---|---|
| 2026-06-05 | request body parse/type/enum 오류 | 400으로 내려오는 것으로 local re-check 완료. |
| 2026-06-05 | `EVIDENCE_PRESENTED + presentedEvidenceId=null` | 400으로 차단 확인. |
| 2026-06-05 | `FREE + presentedEvidenceId` | 400으로 차단 확인. |
| 2026-06-05 | locked evidence를 `presentedEvidenceId`로 전송 | 400 `AI009` 확인. |
| 2026-06-05 | `EVIDENCE_PRESENTED` 기반 unlock | `unlockedEvidences` 1건 반환과 target evidence detail 200 확인. |
| 2026-06-05 | 동일 해금 조건 반복 | idempotent, 추가 unlockedEvidences 없음 확인. |
| 2026-06-05 | Demo Variant final-deduction smoke | HTTP 200과 result 화면/API 진입 확인. 점수/등급/정오 상세는 private 산출물에만 둔다. |
| 2026-06-05 | interrogation/final-deduction AI 호출 | AI 호출 완료, fallback/MockSolutionReader warn 없음 확인. |
| 2026-06-05 | Android 핵심 플레이 E2E | 앱 실행, 시나리오 목록, 세션 생성, 증거/용의자/심문, 최종 추리/결과 기본 flow PASS. |
| 2026-06-05 | Android 증거/잠금 표시 | 잠긴 증거 상세/이미지 미노출, 확보 증거 상세/이미지, 증거 제시 심문 flow PASS. |
| 2026-06-05 | 운영 AI fallback/balance grep | 해당 QA 구간에서 fallback, MockSolutionReader, AI 호출 실패, 잔액 오류 미검출. |
| 2026-06-15 | `coverUpText` 누락 허용 | `FinalDeductionRequest.coverUpText`를 필수로 전환하고 API/FE 문서 계약을 정정했다. |
| 2026-06-15 | concurrent create race `activeSessionId` 누락 가능 | duplicate fallback에서 별도 read-only 재조회로 `activeSessionId` 포함을 우선하도록 보강했다. 장애성 조회 실패 대비 `/active` fallback 계약은 유지한다. |
| 2026-06-15 | final-deduction 중 abandon 허용 | final-deduction in-flight lock 상태의 abandon을 `P003`으로 차단하도록 보강했다. |
| 2026-06-15 | YAML hint import 미지원 | official seed가 `hints`를 source로 관리하고 importer가 `hints` table에 저장하도록 보강했다. private official seed는 1.1.2로 version bump 후 public-safe hint 3개씩 준비했다. |

## 5. Re-smoke Checklist

### Backend / AI

```text
1. 능동 증거 제시 기반 해금
   - 특정 용의자에게 지정 증거 제시
   - EVIDENCE_PRESENTED 조건 증거 해금
   - InterrogationResponse.unlockedEvidences 포함
   - GET /evidences와 dashboard count 반영

2. 심문 요청 validation
   - locked evidence 제시 시 400
   - questionType / presentedEvidenceId 조합 validation 유지

3. final-deduction 입력/상태
   - coverUpText 필수 여부 결정 후 재검증
   - final-deduction in-flight 중 abandon 처리
   - abandon 후 final-deduction/result 메시지 정합성

4. validation API
   - 1.1.2 official seed import 후 hints=0 해소 여부 재검증
   - GET /hints 목록과 /hints/{hintId}/use 본문 노출 정책 확인
   - timelineEvents 포함 여부
   - active variants 전체 검증 여부
   - AI 호출 완료 로그 확인
```

### Android / Frontend

```text
1. Studio9 Playability
   - 라이브러리 진입
   - Studio9 상세 진입
   - 시작 버튼이 "준비 중"이 아니라 "조사 시작"인지 확인
   - 현장 화면 진입
   - mapImageUrl / 장소 / 증거 count 표시

2. P002 Active Recovery
   - active PLAYING 세션이 있는 상태에서 다시 시작
   - 409 P002 수신
   - error.details.activeSessionId 또는 GET /active fallback으로 기존 세션 진입

3. Timeline Rendering
   - timeline tab 진입
   - 서버 timeline event 목록 표시
   - isTrueEvent 필드를 기대하지 않음

4. Scenario Images
   - 목록 thumbnailUrl 표시
   - 상세 coverImageUrl 표시
   - 현장 mapImageUrl 표시

5. Hint Empty State
   - hints=[] 상태에서 명확한 빈 상태 문구와 닫기 동선 표시

6. Android Copy / Image / Search UX
   - 목록/상세 이미지가 API URL을 사용하는지 확인
   - 검색/필터가 실제 결과에 반영되거나 미지원 UI가 숨겨지는지 확인
   - 브리핑/결과 화면에 내부 용어와 하드코딩된 증거 개수가 남지 않는지 확인
```

### Ops / Privacy

```text
1. 운영 로그 privacy
   - 사용자 질문 원문 미노출
   - 최종 추리 원문 미노출
   - Hibernate bind TRACE 비활성
   - AI debug는 redacted preview / model / latency / status / token 중심

2. Prompt context logging check
   - `AI_CALL_CONTEXT`는 AI 심문 실호출 직전에 best-effort로 기록한다.
   - 원문 prompt 없음, 원문 answer 없음, 사용자 질문 전문 없음, sessionId/scenarioId/suspectId/npcCode 없음, block token estimate 숫자와 templateHash만 있음을 확인한다.
```

## 6. Next Absorption

다음 문서 다이어트 단계에서 처리한다.

```text
1. `MVP_PLAY_FLOW_QA_2026-06-04.md` 원문은 정답/variant/증거 ID/운영 marker 상세가 있어 커밋하지 않는다.
2. `docs/README.md`의 흡수/제거 이력을 최신 상태로 유지한다.
```

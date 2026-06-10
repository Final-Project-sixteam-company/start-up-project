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
| `MVP_PLAY_FLOW_QA_2026-06-04.md` | 현재 로컬 untracked 파일이다. 정답/variant 상세 포함 가능성이 있어 scrub 전까지 커밋/흡수하지 않는다. |
| PR #55의 2026-06-10 QA 문서 | PR #55가 아직 open 상태라 미흡수. 머지 후 이 문서로 흡수한다. |

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

## 2. 현재 결론

```text
운영 API 기준 MVP 기본 플레이 흐름은 동작한다.
다만 AI 심문 해금, validation/hint 정책, 일부 final-deduction 상태 전이, Android E2E UX는 계속 재검증이 필요하다.
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

## 3. Open Issues

### Backend / AI

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P1 | INTERROGATION 기반 증거 해금 E2E | `EVIDENCE_PRESENTED` 기반 unlock smoke는 PASS지만, 일반 심문 조건 해금은 별도 E2E가 필요하다. | INTERROGATION unlock rule seed/import 후 `InterrogationResponse.unlockedEvidences`와 evidence board count 재검증. |
| P1 | `coverUpText` 누락 허용 | 2026-06-05 local re-check 기준 여전히 200 허용. | 필수 필드면 `@NotBlank`, 선택 필드면 API Spec/Android 문서에 선택값으로 명시. |
| P1 | official scenario validation `NEEDS_FIX` | scenario validation이 hints=0 때문에 `NEEDS_FIX`, score 40으로 확인됨. | MVP에서 hint를 쓸지 결정. 미사용이면 validation rule 조정. |
| P1 | PHASE 해금이 시간 경과만으로 과도하게 열림 | Studio9 QA에서 15분 후 전체 증거 공개 흐름 확인. | MVP를 시간 공개 게임으로 갈지, 핵심 증거 일부를 INTERROGATION/EVIDENCE_PRESENTED/MANUAL로 전환할지 결정. |
| P1 | AI policy 반응 약함 | FREE와 EVIDENCE_PRESENTED 답변 차이가 약하고, topic/userIntent/stagePolicies 활용이 제한적이다. | questionType + presentedEvidenceId 기반 정책 강화 후 답변 차이 smoke. |
| P1 | concurrent create race의 `activeSessionId` 누락 가능 | 일반 duplicate create는 P002 details가 정상이나, race fallback에서는 누락 가능성이 기록됨. | `DataIntegrityViolationException` fallback에서 active session 재조회 후 details 포함. FE는 `/active` fallback 유지. |
| P1 | final-deduction 중 abandon 허용 | final-deduction 중 abandon 200, 이후 result 404 또는 부정확한 상태 메시지 확인. | final-deduction in-flight 중 abandon 차단 또는 상태 메시지 정정. |
| P2 | final-deduction 입력 품질 제한 | 긴 텍스트 제한, selectedEvidenceIds 중복 처리 등 UX/계약 정리가 필요하다. | API Spec과 validation 정책 확정. |
| P2 | suspect detail 데이터 품질 | `relationToVictim=null`, publicProfile/publicStatement 중복 기록. | seed/import 또는 response mapping 정리. |

### Android / Frontend

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P0/P1 | Studio9 앱 진입 차단 여부 | Android E2E에서 시작 버튼/준비 중 gate 확인 필요. | Studio9 상세 진입 후 실제 플레이 시작 smoke. |
| P1 | P002 active session 복구 계약 연동 | API는 P002와 `/active` fallback을 제공하나 앱 복구 동선 확인 필요. | `error.details.activeSessionId` 우선, 없으면 `GET /active` fallback 적용 확인. |
| P1 | Timeline API 화면 연동 | 서버 timeline API는 존재한다. 앱에서 placeholder 대신 목록 렌더링 확인 필요. | timeline 탭에서 서버 응답 표시, `isTrueEvent` 미기대 확인. |
| P1 | 시나리오 목록/상세 이미지 사용 | `thumbnailUrl`, `coverImageUrl`, map image 표시 확인 필요. | library card/detail/field map image smoke. |
| P1 | 라이브러리 검색/필터 반영 | 검색/필터 결과 반영 여부가 명확하지 않다. | 빈 상태와 결과 개수 UX 확인. |
| P1 | 브리핑 문구와 실제 시나리오 정보 불일치 | 하드코딩/placeholder copy 가능성. | 서버 scenario synopsis/briefing 사용 여부 확인. |
| P1 | 현장 지도/장소 이미지 발견성 | map/place image가 사용자에게 명확히 보이는지 확인 필요. | 장소 탭과 지도 화면 UX smoke. |
| P2 | Hint 빈 상태 UX | hints=0일 때 빈 바텀시트가 아니라 명확한 안내가 필요하다. | "현재 사용할 수 있는 힌트가 없습니다" 류의 empty state 적용. |

### Infra / Ops / Privacy

| Priority | 이슈 | 현재 판단 | 다음 액션 |
|---|---|---|---|
| P0 | 운영 로그에 사용자 입력 원문 노출 방지 | Docker/prod 기본 Hibernate bind log level은 warn으로 확인됐지만, 운영 marker 재확인이 필요하다. | Hibernate bind TRACE 비활성 상태와 AI 로그 redaction 기준 재확인. |
| P1 | AI/LLMOps 로그 privacy | AI 고도화용 로그는 필요하지만 prompt/answer/user question 원문 저장은 금지해야 한다. | `AI_CALL`, `AI_CALL_CONTEXT`에서 원문과 session/suspect 식별자 미포함 확인. |

## 4. Resolved / Verified

| 날짜 | 항목 | 결과 |
|---|---|---|
| 2026-06-05 | request body parse/type/enum 오류 | 400으로 내려오는 것으로 local re-check 완료. |
| 2026-06-05 | `EVIDENCE_PRESENTED + presentedEvidenceId=null` | 400으로 차단 확인. |
| 2026-06-05 | `FREE + presentedEvidenceId` | 400으로 차단 확인. |
| 2026-06-05 | locked evidence를 `presentedEvidenceId`로 전송 | 400 `AI009` 확인. |
| 2026-06-05 | `EVIDENCE_PRESENTED` 기반 unlock | `unlockedEvidences` 1건 반환과 target evidence detail 200 확인. |
| 2026-06-05 | 동일 해금 조건 반복 | idempotent, 추가 unlockedEvidences 없음 확인. |
| 2026-06-05 | Demo Variant final-deduction smoke | HTTP 200, result 200, score 100, grade S 확인. |
| 2026-06-05 | interrogation/final-deduction AI 호출 | AI 호출 완료, fallback/MockSolutionReader warn 없음 확인. |
| 2026-06-05 | Docker log privacy 기본값 | Hibernate bind log 원문, fallback, AI 호출 실패 미검출. |

## 5. Re-smoke Checklist

### Backend / AI

```text
1. INTERROGATION 기반 해금
   - 특정 용의자 심문
   - INTERROGATION 조건 증거 해금
   - InterrogationResponse.unlockedEvidences 포함
   - GET /evidences와 dashboard count 반영

2. EVIDENCE_PRESENTED 기반 해금
   - 제시 증거 조건으로 새 증거 해금
   - 중복 제시 idempotent
   - locked evidence 제시 시 400
   - questionType / presentedEvidenceId 조합 validation 유지

3. final-deduction 입력/상태
   - coverUpText 필수 여부 결정 후 재검증
   - final-deduction in-flight 중 abandon 처리
   - abandon 후 final-deduction/result 메시지 정합성

4. validation API
   - hints=0 rule 결정 후 재검증
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
```

### Ops / Privacy

```text
1. 운영 로그 privacy
   - 사용자 질문 원문 미노출
   - 최종 추리 원문 미노출
   - Hibernate bind TRACE 비활성
   - AI debug는 redacted preview / model / latency / status / token 중심

2. AI_CALL_CONTEXT
   - 원문 prompt 없음
   - 원문 answer 없음
   - 사용자 질문 전문 없음
   - sessionId / scenarioId / suspectId / npcCode 없음
   - block token estimate 숫자만 있음
```

## 6. Next Absorption

다음 문서 다이어트 단계에서 처리한다.

```text
1. PR #55 머지 후 2026-06-10 QA 문서들을 이 문서로 흡수한다.
2. `MVP_PLAY_FLOW_QA_2026-06-04.md`는 scrub 전까지 untracked 상태로 유지한다.
3. `docs/README.md`의 흡수/제거 이력을 최신 상태로 유지한다.
```

# MVP QA Issue Handoff - 2026-06-04

운영 API QA 결과를 팀 공유용으로 요약한 핸드오프 문서다.
상세 실행 로그 원문은 정답/variant 정보가 포함될 수 있어 PR 문서에는 포함하지 않는다.

## 결론

운영 API 기준 MVP 기본 플레이 흐름은 동작한다.

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

다만 MVP 완성도 기준으로 백엔드/AI 쪽에 반드시 정리할 이슈가 남아 있다.

## 백엔드 / AI 담당

담당자:

```text
배강혁
```

### P0/P1. 심문 기반 증거 해금 미구현

확인 결과:

```text
InterrogationResponse.unlockedEvidences=[]
InterrogationCompletedEvent 수신 구현 없음
private YAML unlockRules 전부 PHASE
```

영향:

```text
AI에게 증거를 제시하거나 심문해도 새 단서가 열리지 않는다.
```

필요 작업:

```text
InterrogationCompletedEventListener 또는 InterrogationUnlockService 구현
심문 전/후 unlocked evidence diff 계산
InterrogationResponse.unlockedEvidences 실제 반환
YAML에 INTERROGATION / EVIDENCE_PRESENTED unlock rule 추가
통합 테스트 추가
```

### P1. request body parse/type 오류가 500으로 떨어짐

재현:

```text
POST /final-deduction with malformed JSON -> 500 C010
POST /final-deduction selectedEvidenceIds 숫자 타입 -> 500 C010
POST /interrogations with malformed JSON -> 500 C010
POST /interrogations questionType invalid enum -> 500 C010
```

기대:

```text
400 Bad Request 계열
```

필요 작업:

```text
HttpMessageNotReadableException
InvalidFormatException / enum parse error
type mismatch 계열을 400으로 매핑
```

### P1. EVIDENCE_PRESENTED인데 presentedEvidenceId=null 허용

재현:

```json
{
  "suspectId": 53,
  "questionType": "EVIDENCE_PRESENTED",
  "question": "증거가 없는데 증거 질문입니다.",
  "presentedEvidenceId": null
}
```

실제:

```text
200 success
AI 호출 발생
interrogationCount 증가
```

기대:

```text
400 validation error
```

추가 확인:

```text
FREE + presentedEvidenceId도 200 허용
FREE + evidenceId와 EVIDENCE_PRESENTED + evidenceId 답변 차이가 약함
```

### P1. coverUpText 누락 허용

재현:

```json
{
  "selectedCulpritId": 53,
  "motiveText": "동기 테스트 입력입니다.",
  "methodText": "방법 테스트 입력입니다.",
  "selectedEvidenceIds": [261]
}
```

실제:

```text
200 success
```

확인:

```text
coverUpText가 필수 입력이면 @NotBlank 적용 필요
필수가 아니라면 API Spec / Android Mapping에 선택 필드로 명시 필요
```

### P1. official scenario validation 결과가 NEEDS_FIX

확인:

```text
POST /api/ai/scenarios/10/validate -> NEEDS_FIX, score=40
POST /api/ai/scenarios/11/validate -> NEEDS_FIX, score=40
```

원인:

```text
힌트 0개
```

로그:

```text
scenarioId=10, 용의자=5, 증거=25, 힌트=0
scenarioId=11, 용의자=7, 증거=35, 힌트=0
```

필요 결정:

```text
힌트를 MVP에 쓴다 -> hint seed/import/API 보강
힌트를 MVP에 안 쓴다 -> validation rule 조정
```

### P1. PHASE 해금이 시간 방치만으로 전체 증거 공개

스튜디오9 idle QA:

```text
0분: 8/35
5분: 14/35
10분: 29/35
15분: 35/35
```

영향:

```text
사용자가 아무 심문/조사를 하지 않아도 15분이면 모든 증거가 열린다.
```

필요 결정:

```text
MVP는 시간 공개 게임으로 간다
또는 결정타 일부를 INTERROGATION / EVIDENCE_PRESENTED / MANUAL 해금으로 변경한다
```

### P1. AI policy 반응 약함

확인:

```text
FREE 질문과 EVIDENCE_PRESENTED 질문의 답변 차이가 약함
ResponsePolicyResolver가 question text / userIntent / topic / stagePolicies를 보지 않음
NpcKnowledgeProfile directKnowledge/stagePoliciesJson도 prompt 경로에서 사용되지 않음
```

필요 작업:

```text
questionType + presentedEvidenceId 기반 정책 강화
추후 question topic / userIntent / stage 매칭 추가
```

### P1. concurrent create race에서 activeSessionId 누락 가능

확인:

```text
일반 duplicate create -> 409 P002 + details.activeSessionId 정상
동시 세션 생성 race -> 409 P002지만 activeSessionId 누락 가능
```

추가 QA:

```text
scenarioId=10 동시 5개 요청 -> 1개 201, 4개 P002, details.activeSessionId 0/4
scenarioId=11 동시 5개 요청 -> 1개 201, 4개 P002, details.activeSessionId 3/4
GET /active fallback은 정상
```

필요 작업:

```text
DataIntegrityViolationException fallback 경로에서 active session 재조회 후 details.activeSessionId 포함
실패 시 프론트가 GET /active로 복구 가능하긴 함
```

### P2

```text
용의자 detail relationToVictim=null
publicProfile/publicStatement 중복
abandon success response가 {"success": true}만 반환
abandon 후 final-deduction 메시지가 "이미 최종 추리를 제출했습니다"로 부정확
numeric string ID coercion 허용
final-deduction 긴 텍스트 제한 없음
final-deduction selectedEvidenceIds 중복은 15개 이하에서 200 허용
```

### P1. final-deduction 중 abandon 허용

확인:

```text
final-deduction 중 hint use -> 400 P003, final 정상 완료
final-deduction 중 evidence unlock -> 400 P003, final 정상 완료
final-deduction 중 abandon -> abandon 200, final-deduction 400 P003, result 404
```

판정:

```text
데이터 꼬임은 보이지 않음
다만 제출 중 포기/뒤로가기 UX 정책 필요
```

권장:

```text
Android에서 final 제출 중 abandon/navigation 차단
또는 Backend abandonSession에서도 finalDeductionLockManager.isLocked(sessionId) 차단
```

## 인프라 / 운영 담당

담당자:

```text
황도윤
```

### P0. 운영 로그에 사용자 입력 원문 노출

재현:

```text
QA marker:
QA_PRIVACY_MARKER_20260604_1409_TEXT_SHOULD_NOT_APPEAR_IN_LOGS

심문 질문 원문이 org.hibernate.orm.jdbc.bind TRACE에 노출
final-deduction motive/method/coverUp 원문이 org.hibernate.orm.jdbc.bind TRACE에 노출
```

현재 env:

```text
HIBERNATE_SQL_LOG=debug
HIBERNATE_SQL_PARAM_LOG=trace
```

필요 작업:

```text
운영에서 Hibernate bind TRACE 비활성화
HIBERNATE_SQL_PARAM_LOG=off 또는 info 이상으로 조정
org.hibernate.SQL DEBUG도 운영에서는 기본 비활성화 권장
장애 분석용 SQL 파라미터 로그는 staging/local에서만 사용
```

### 인프라 정상 확인

상태:

```text
S3 official/* public read 적용 완료
장소 이미지 21개 S3 업로드 완료
imageUrl 200 OK 확인
최신 scenarioId=10,11 노출 확인
구버전 scenarioId=1,2 hidden/unlisted 처리 완료
Blue-Green active upstream 정상
운영 로그 fallback / MockSolutionReader / AI 호출 실패 없음
```

## 팀 공유용 메시지

```md
운영 API 기준 MVP QA를 정상/비정상 플로우까지 돌렸습니다.

기본 플레이 흐름은 동작합니다.

- 시나리오 목록/상세
- 이미지 URL
- 세션 생성/active 복구
- 장소/타임라인/증거 조회
- 증거 잠금 마스킹
- 시간 기반 PHASE 해금
- 심문 기본 응답/로그 저장
- 최종 추리 제출/결과 조회
- 동시 final-deduction lock

다만 백엔드/AI 쪽에서 MVP 품질 기준으로 정리해야 할 이슈가 있습니다.

우선순위 높은 건 아래입니다.

1. 심문 기반 증거 해금 미구현
   - InterrogationCompletedEvent 수신 구현 없음
   - InterrogationResponse.unlockedEvidences=[] 고정
   - seed unlockRules도 전부 PHASE

2. request body parse/type 오류가 500 C010으로 떨어짐
   - malformed JSON
   - selectedEvidenceIds 숫자 타입
   - invalid questionType enum

3. EVIDENCE_PRESENTED인데 presentedEvidenceId=null이어도 AI 호출됨

4. coverUpText 누락이 final-deduction에서 허용됨

5. validate API 기준 공식 시나리오가 둘 다 NEEDS_FIX
   - 원인: hints=0

6. 스튜디오9는 아무 행동 없이 15분이면 모든 증거가 자동 해금됨
   - 기획상 시간 공개가 맞는지, 일부 결정타는 심문/증거 제시 해금으로 바꿀지 결정 필요

인프라 쪽은 S3 이미지, active upstream, 최신 scenario 노출은 정상 확인됐습니다.
다만 운영 Hibernate TRACE 로그에 사용자 질문 원문이 남는 부분은 로그 레벨 조정 검토가 필요합니다.

핸드오프 문서:
docs/MVP_QA_ISSUE_HANDOFF_2026-06-04.md
```

## 수정 후 재검증 대기 항목

현재 즉시 실행 가능한 추가 QA는 완료했다.

수정 이후 다시 봐야 할 항목:

```text
1. 심문 기반 해금 E2E
   - InterrogationResponse.unlockedEvidences 실제 반환
   - INTERROGATION / EVIDENCE_PRESENTED unlock rule 동작

2. request parse/type 오류
   - malformed JSON / invalid enum / type mismatch가 400으로 떨어지는지

3. variant별 final-deduction smoke
   - 서월채 4 variant
   - 스튜디오9 4 variant

4. validation API AI path
   - hints rule 정리 후 NEEDS_FIX 고정 해소
   - timelineEvents / active variants 검증 포함

5. Android full E2E
   - P002 fallback
   - locked evidence importance 미표시
   - abandon success-only 처리
   - imageUrl 표시

6. production log privacy
   - HIBERNATE_SQL_PARAM_LOG trace 비활성화 후 사용자 입력 marker 미검출
```

## QA Review Follow-up

2026-06-04 추가 QA 리뷰 기준으로, 백엔드 운영 API 단독 QA는 대부분 완료된 상태다.
남은 항목은 "지금 앱/운영에서 바로 확인할 것"과 "수정 후 재검증할 것"으로 분리한다.

### 지금 바로 확인할 항목

```text
1. Android / Frontend full E2E
   담당: 정채림
   - 시나리오 목록/상세 이미지 표시
   - 시작 버튼
   - P002 details.activeSessionId 누락 시 /active fallback
   - dashboard / locations / evidence / suspect / interrogation / result / abandon
   - timeline에서 isTrueEvent 미기대
   - abandon 응답이 {"success": true}만 와도 파서가 깨지지 않는지
   - imageUrl만 사용하고 assetKey 직접 조합하지 않는지

2. locked evidence 표시 정책 확인
   담당: 백엔드 + Android
   - YAML importance=CORE/FAKE는 내부 seed 값으로 유지
   - locked evidence 응답에서 importance를 내려줄지 백엔드 계약 확정
   - Android는 locked evidence에서 CORE/FAKE를 사용자에게 표시하지 않아야 함
   - title 노출은 진행 예고 UX로 허용할지 기획 확인 필요

3. 운영 로그 privacy
   담당: 황도윤 + 배강혁
   - AI 고도화용 로그는 필요하지만 Hibernate bind TRACE는 사용자 질문/최종추리 원문을 남기므로 운영 상시 로그로 부적절
   - AI debug는 redacted preview / model / latency / status / token 중심으로 분리
   - HIBERNATE_SQL_PARAM_LOG 조정 후 QA marker 미검출 재확인

4. final result 정답 공개 UX
   담당: Android + 기획
   - 제출 전 "정답 공개/되돌릴 수 없음" 안내
   - 결과 화면에서 범인/동기/방법/은폐/핵심증거 공개 정책 확인
   - 재도전 UX가 있으면 정답 노출 후 재도전 정책 확인
```

### 수정 후 반드시 재검증할 항목

```text
1. 심문 기반 해금 E2E
   담당: 배강혁
   - 특정 용의자 심문
   - INTERROGATION 조건 증거 해금
   - InterrogationResponse.unlockedEvidences 포함
   - GET /evidences와 dashboard count 반영

2. 증거 제시 기반 해금 E2E
   담당: 배강혁
   - EVIDENCE_PRESENTED 조건 증거 해금
   - 중복 제시 idempotent
   - EVIDENCE_PRESENTED + presentedEvidenceId=null => 400
   - FREE + presentedEvidenceId 정책 확정

3. request parse/type 오류 500 -> 400 재검증
   담당: 백엔드
   - final-deduction malformed JSON
   - selectedEvidenceIds type mismatch
   - invalid questionType enum
   - unlock/hint malformed body

4. variant별 final-deduction smoke
   담당: 배강혁 + 황도윤
   - 서월채 4 variants
   - 스튜디오9 4 variants
   - result 200, fallback warn 없음, explanation이 variant와 일치

5. validation API AI path
   담당: 배강혁
   - hints=0 rule 정리 후 재검증
   - timelineEvents 포함 여부
   - active variants 전체 검증 여부
   - AI 호출 완료 로그 확인
```

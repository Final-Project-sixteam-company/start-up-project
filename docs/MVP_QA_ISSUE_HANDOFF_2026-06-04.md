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

## Android / Frontend 담당

담당자:

```text
정채림
```

### P0. Studio9 앱 진입 차단

확인 결과:

```text
Backend /api/scenarios:
- Studio9 scenarioId=11
- canPlay=true
- evidenceCount=35
- suspectCount=7

Android:
- 목록에 Studio9 카드 노출
- 상세 화면에서 "CL-011 시나리오는 아직 준비 중입니다. 곧 플레이할 수 있어요." 표시
- 시작 버튼이 "준비 중"으로 비활성화
- Studio9 case briefing / session start / locations / evidence QA 진행 불가
```

영향:

```text
백엔드와 운영 seed는 Studio9를 플레이 가능 상태로 내려주지만,
Android 화이트리스트가 scenarioId=11을 차단하고 있어 앱에서 플레이할 수 없다.
발표/데모에서 Studio9를 사용할 계획이면 MVP 차단 이슈다.
```

소스 위치:

```text
start-up-fe/lib/screens/scenario_detail_screen.dart

const _kPlayableIds = {'1', '10'};
```

필요 작업:

```text
1. Studio9 운영 smoke가 완료된 기준으로 11을 _kPlayableIds에 추가
2. 또는 백엔드 canPlay를 신뢰하도록 전환
3. 임시 화이트리스트를 유지한다면 데모 대상 scenarioId 10/11을 모두 포함
```

### P1. P002 active session 복구 계약 미연동

테스트 조건:

```text
1. 서월채 active PLAYING 세션 생성
2. 앱 로컬 데이터 초기화로 SharedPreferences active_play_session 저장값 제거
3. 같은 사용자/같은 scenarioId=10으로 앱에서 다시 조사 시작
```

확인 결과:

```text
Android UI:
- "진행 중인 세션이 있습니다"
- "기존 세션 포기 후 새로 시작"
- 버튼 클릭 후 "이 기기에서 시작한 세션 기록이 없어 자동으로 정리할 수 없습니다..." 안내

Android 코드:
- P002 error.details.activeSessionId 사용 없음
- GET /api/play-sessions/active?scenarioId= fallback 호출 없음
- 로컬 SharedPreferences에 저장된 sessionId가 없으면 복구 불가
```

영향:

```text
새 설치, 앱 데이터 초기화, 다른 기기에서 이어가기 상황에서 기존 PLAYING 세션으로 복구할 수 없다.
백엔드 ST-53에서 추가한 activeSessionId / GET active 계약이 앱에 아직 연결되지 않았다.
```

소스 위치:

```text
start-up-fe/lib/controllers/game_session_controller.dart
start-up-fe/lib/repositories/play_session_repository.dart
```

필요 작업:

```text
1. POST /api/play-sessions 409 P002에서 error.details.activeSessionId가 있으면 해당 sessionId로 진입
2. details가 없으면 GET /api/play-sessions/active?scenarioId= fallback
3. UI 문구를 "진행 중인 수사 이어가기" 중심으로 변경
4. 정말 새로 시작할 때만 abandon 후 재시작 제공
```

### P1. Timeline API 미연동

확인 결과:

```text
백엔드 /api/play-sessions/{sessionId}/timeline:
- 서월채/스튜디오9 timelineEvents 존재

Android:
- 타임라인 탭에서 "타임라인 준비 중" 표시
- 실제 /timeline API 응답 렌더링 없음
```

소스 위치:

```text
start-up-fe/lib/screens/timeline_screen.dart
TODO: 백엔드 구현 시 controller.timeline을 읽도록 교체
```

필요 작업:

```text
MVP에서 타임라인 탭을 노출한다면 실제 /timeline API 응답을 렌더링해야 한다.
MVP에서 타임라인을 제외한다면 탭 숨김 또는 데모 범위에 맞는 준비 중 문구로 정리한다.
```

### P1. 시나리오 목록/상세 이미지 미사용

확인 결과:

```text
Backend:
- thumbnailUrl / coverImageUrl / mapImageUrl 정상
- S3 URL 200 OK

Android:
- 목록/상세 카드에서 API 이미지 대신 CL-010/CL-011 placeholder 표시
```

영향:

```text
S3와 백엔드는 정상이나, 앱 첫 화면에서 공식 시나리오 비주얼 완성도가 낮아 보인다.
```

필요 작업:

```text
목록: thumbnailUrl 사용
상세 hero: coverImageUrl 사용
assetKey로 URL 직접 조합 금지, 백엔드 응답 imageUrl 계열 사용
```

### P2. Hint 빈 상태 UX 부족

확인 결과:

```text
현장 힌트 버튼 -> 힌트 바텀시트 표시
하지만 "힌트 요청" 제목과 "힌트 사용 시 최종 점수가 감점됩니다." 안내만 보임
힌트 목록 / 사용 가능한 힌트 없음 / 닫기 액션이 명확히 보이지 않음
```

필요 작업:

```text
힌트 seed가 없는 MVP 상태라도 빈 상태 문구를 보여준다.
예: "현재 사용할 수 있는 힌트가 없습니다."
닫기/취소 액션을 명확히 제공한다.
```

### P1. 라이브러리 검색/필터가 결과에 반영되지 않음

확인 결과:

```text
Android:
- 라이브러리 검색창에 qatestnomatch 입력
- 엔터/검색 아이콘 후에도 시나리오 2건이 그대로 표시됨
- "커스텀" 필터 선택 후에도 공식 시나리오 2건이 그대로 표시됨

운영 API:
- GET /api/scenarios?keyword=qatestnomatch
- GET /api/scenarios?keyword=qatestnomatch&type=CUSTOM
- GET /api/scenarios?difficulty=EASY
위 요청 모두 기존 공식 시나리오 2건을 그대로 반환함
```

영향:

```text
사용자는 검색/필터 칩을 조작할 수 있지만 결과가 바뀌지 않아 기능이 고장난 것처럼 느낀다.
시나리오가 2개뿐인 MVP에서도 "커스텀", "쉬움" 같은 필터가 실제 의미와 다르게 보인다.
```

담당:

```text
백엔드 + Android
```

필요 작업:

```text
1. 백엔드 /api/scenarios 목록에서 keyword/type/difficulty 필터를 실제 적용
2. MVP에서 필터 API를 당장 지원하지 않을 경우 Android에서 미지원 필터 칩을 숨김
3. 검색 결과 0건 빈 상태를 실제로 노출
```

### P1. 브리핑 문구가 실제 시나리오 정보와 어긋남

확인 결과:

```text
서월채 브리핑:
- 사건 개요 본문에는 차민혁이 피해자로 명시됨
- 바로 아래 피해자 정보 카드는 "미상"으로 표시됨
- 탐정 목표에 "결정적 증거 3개를 수집하라"가 하드코딩되어 있음
```

영향:

```text
사용자 입장에서는 피해자가 공개된 것인지 숨겨진 것인지 헷갈린다.
증거 개수 하드코딩은 시드/채점 계약 변경 시 바로 틀어질 수 있다.
```

담당:

```text
Android + 기획
```

필요 작업:

```text
1. briefing/dashboard의 victimName/foundLocation이 있으면 해당 값을 표시
2. 없을 때만 "수사 시작 후 공개" 또는 "미상" 처리
3. "결정적 증거 3개"는 서버 keyEvidence 목표 수 또는 일반 문구로 교체
   예: "결정적 증거를 수집하라"
```

### P1. 현장 지도/장소 이미지 발견성이 약함

확인 결과:

```text
서월채 현장 탭:
- 상단 큰 지도 영역이 빈 패널처럼 보임
- 장소 카드를 눌러도 선택 테두리 외 즉시 피드백이 약함
- 선택된 장소 이미지는 카드 아래쪽에 붙는 구조라 사용자가 스크롤 위치에 따라 못 볼 수 있음
```

영향:

```text
API/S3 이미지가 정상이어도 앱 사용자는 지도나 방 사진이 연결됐는지 바로 알기 어렵다.
향후 "맵 특정 지점 클릭 -> 히든 증거" UX로 확장하려면 현재 지도 영역의 의미가 더 명확해야 한다.
```

담당:

```text
Android
```

필요 작업:

```text
1. mapImageUrl 로딩/실패/빈 상태를 명확히 표시
2. 장소 선택 시 해당 장소 설명과 이미지가 즉시 보이도록 배치 조정
3. 이미지가 있는 장소 카드에는 "사진 보기" 또는 썸네일 CTA를 더 명확하게 표시
```

### P2. 검색 빈 상태 요약 기준 모호

확인 결과:

```text
증거 검색 결과 없음: PASS
용의자 검색 결과 없음: PASS
다만 용의자 검색 결과가 0명이어도 상단 요약은 전체 기준 5명 / 0회 / 50%를 그대로 표시
```

판단:

```text
기능 오류는 아니지만, 검색 결과 기준 요약인지 전체 사건 기준 요약인지 혼동될 수 있다.
```

### Android Implementation Notes

위 이슈를 고칠 때 같이 확인할 코드 포인트다.

#### 1. Scenario canPlay / image fields

현재:

```text
lib/models/scenario.dart
- Scenario 모델에 thumbnailUrl만 있음
- canPlay 없음
- coverImageUrl 없음

lib/repositories/scenario_repository.dart
- summary thumbnailUrl은 파싱
- detail 응답의 coverImageUrl은 파싱하지 않음

lib/screens/scenario_detail_screen.dart
- _isPlayable이 backend canPlay가 아니라 _kPlayableIds만 봄
- _HeroArt가 coverImageUrl 없이 gradient + CL-XXX placeholder만 표시

lib/screens/scenario_library_screen.dart
- _CodeThumb가 thumbnailUrl 없이 CL-XXX placeholder만 표시
```

권장:

```text
Scenario 모델:
- bool canPlay
- String? thumbnailUrl
- String? coverImageUrl

ScenarioRepository:
- summary: canPlay / thumbnailUrl 파싱
- detail: canPlay / coverImageUrl / thumbnailUrl 파싱

ScenarioDetailScreen:
- 임시로는 _kPlayableIds에 11 추가
- 더 나은 방향은 _scenario.canPlay 사용
- 상세 hero는 coverImageUrl 사용

ScenarioLibraryScreen:
- 목록 thumb는 thumbnailUrl 사용
```

#### 2. P002 details / active fallback

현재:

```text
lib/core/api/api_exception.dart
- code/message/status만 있음
- details 없음

lib/core/api/api_client.dart
- error.details를 ApiException에 보존하지 않음

lib/repositories/play_session_repository.dart
- GET /api/play-sessions/active?scenarioId= 메서드 없음

lib/controllers/game_session_controller.dart
- 409 발생 시 sessionConflict=true만 설정
- error.details.activeSessionId를 사용하지 않음
- /active fallback 없음
```

권장:

```text
ApiException:
- Map<String, dynamic>? details 추가

ApiClient._parse:
- error['details']가 Map이면 ApiException.details에 보존

PlaySessionRepository:
- activeSession(int scenarioId) 추가
- GET /api/play-sessions/active?scenarioId=

GameSessionController.loadFromServer:
- createSession 409 P002 발생
- e.details['activeSessionId'] 있으면 그 sessionId로 _tryResume
- details 없으면 repo.activeSession(sid) fallback
- activeSessionId를 얻으면 _saveSession 후 _refreshAll
- 둘 다 실패할 때만 conflict 안내
```

#### 3. Timeline API

현재:

```text
lib/screens/timeline_screen.dart
- sampleCase.timeline만 사용
- CL-001 외 시나리오는 "타임라인 준비 중"

lib/repositories/play_session_repository.dart
- GET /api/play-sessions/{sessionId}/timeline 메서드 없음

lib/models/play_models.dart
- timeline list 응답 DTO 없음

lib/controllers/game_session_controller.dart
- _refreshAll에서 timeline을 로드하지 않음
```

권장:

```text
PlayTimelineEvent 모델 추가:
- time
- title
- description
- eventType
- relatedEvidenceId

PlaySessionRepository.timeline(sessionId) 추가
GameSessionController에 timeline 상태 추가
TimelineScreen이 context.sessionRead.timeline을 렌더링
기존 sampleCase timeline gate는 CL-001 fallback 전용으로만 유지하거나 제거
```

#### 4. Hint empty state

현재:

```text
힌트 API data=[] 상태에서 바텀시트가 제목/감점 안내만 보여줌
```

권장:

```text
data=[]이면 MSEmpty 또는 명확한 문구 표시
"현재 사용할 수 있는 힌트가 없습니다."
닫기/취소 액션 명확화
```

### Android Fix DoD / Re-smoke

Android 수정 후 아래 순서로 다시 확인한다.

#### 1. Studio9 Playability

```text
1. 앱 실행
2. 라이브러리 진입
3. Studio9 상세 진입
4. 시작 버튼이 "준비 중"이 아니라 "조사 시작"인지 확인
5. CASE BRIEFING 표시
6. 수사 시작하기
7. 현장 화면 진입
8. Studio9 mapImageUrl 표시
9. 장소 목록 10개 표시
10. 증거 탭에서 35개 기준 count 표시
```

PASS 기준:

```text
Studio9를 앱에서 실제 플레이 시작할 수 있다.
CL-011 준비 중 게이트가 더 이상 보이지 않는다.
```

#### 2. P002 Active Recovery

```text
1. 서월채 또는 Studio9에서 active PLAYING 세션 생성
2. 앱 데이터 삭제 또는 다른 기기 상태로 로컬 active session 저장값 제거
3. 같은 scenarioId로 다시 조사 시작
4. POST /play-sessions가 409 P002를 받는 상황 유도
5. 앱이 error.details.activeSessionId 또는 GET /active fallback으로 기존 세션에 진입하는지 확인
```

PASS 기준:

```text
"이 기기에서 시작한 세션 기록이 없어 자동으로 정리할 수 없습니다" 화면으로 막히지 않는다.
기존 세션 이어가기 또는 명확한 복구 동선이 제공된다.
```

#### 3. Timeline Rendering

```text
1. 서월채 세션 진입
2. 타임라인 탭 클릭
3. "타임라인 준비 중" 대신 서버 timeline event 목록 표시
4. Studio9 세션에서도 동일 확인
```

PASS 기준:

```text
GET /api/play-sessions/{sessionId}/timeline 응답이 앱 화면에 표시된다.
timeline에서 isTrueEvent 필드를 기대하지 않는다.
```

#### 4. Scenario Images

```text
1. 라이브러리 목록 진입
2. 서월채 / Studio9 카드 썸네일 확인
3. 상세 화면 hero 이미지 확인
```

PASS 기준:

```text
목록에서 thumbnailUrl 이미지 표시
상세에서 coverImageUrl 이미지 표시
CL-010 / CL-011 placeholder만 보이는 상태 해소
```

#### 5. Hint Empty State

```text
1. 현장 화면 진입
2. 힌트 버튼 클릭
3. 힌트 data=[] 상태 확인
```

PASS 기준:

```text
빈 바텀시트가 아니라 "현재 사용할 수 있는 힌트가 없습니다" 같은 명확한 빈 상태가 보인다.
닫기/취소 동선이 있다.
```

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

## 2026-06-05 Evidence Presented Smoke

운영 API 기준으로 `EVIDENCE_PRESENTED` 심문 스모크를 재확인했다.

결과:

```text
PASS:
- 문하연에게 제보 초안 제시 시 상태 로그 원시 데이터가 unlockedEvidences에 포함됨
- evidence board unlocked count가 20 -> 21로 증가
- locked evidence를 presentedEvidenceId로 보내면 400(AI009)
- FREE에 presentedEvidenceId를 보내면 400(C001)
- RECOMMENDED에 presentedEvidenceId를 보내면 400(C001)
- EVIDENCE_PRESENTED에 presentedEvidenceId가 없으면 400(C001)
- 같은 해금 조건 입력을 반복해도 추가 unlockedEvidences는 없음
```

담당 분류:

```text
인프라:
- 운영 seed/rule 수동 반영 완료
- 테스트 세션 정리 완료
```

## 2026-06-05 Demo Variant Final Deduction Smoke

운영 API 기준으로 데모용 단일 Variant 고정과 최종 추리 정채점 흐름을 확인했다.

준비:

```text
scenarioId: 10
sessionId: 75
selected variant: VARIANT_SECRETARY
variant active state: VARIANT_SECRETARY만 active, 나머지 inactive
phase elapsed: 16분 경과 상태로 조정
```

검증 결과:

```text
PASS:
- 새 세션이 VARIANT_SECRETARY로 생성됨
- 문하연에게 제보 초안 제시 후 상태 로그 원시 데이터 해금
- unlocked evidence count: 24 -> 25
- 정답 캐릭터 + 핵심 증거 전체로 final-deduction 제출
- POST /final-deduction HTTP 200
- GET /result HTTP 200
- score=100
- grade=S
- matched.culprit/motive/method/coverUp=true
- matched.keyEvidences=11
```

로그 확인:

```text
PASS:
- interrogation AI 호출 완료
- final-deduction AI 호출 완료
- AI 호출 실패 없음
- fallback / MockSolutionReader warn 없음
- AI012 / Exception 없음
```

담당 분류:

```text
인프라:
- 운영 seed hash 반영 완료
- 운영 variant active 상태 수동 반영 완료
- 데모 Variant smoke 완료

백엔드:
- final-deduction read-path / scoring path 정상 확인
- MockSolutionReader fallback 미발생 확인
```
# 2026-06-05 Codex Local Re-check

> 상세 handoff: `.review/FE_BE_MVP_CROSSCHECK_2026-06-05.md`

- BE/FE 모두 최신 `origin/develop`으로 fast-forward 후 재검증했다.
- Local Docker API는 `http://localhost:18080/api`로 확인했다.
- 로컬 DB에는 scenarioId=1만 존재해 운영 QA 문서의 scenarioId=10/11 flow는 동일 id로 재현하지 못했다.
- request body parse/type/enum 오류는 400으로 내려온다.
- `EVIDENCE_PRESENTED + presentedEvidenceId=null` 및 `FREE + presentedEvidenceId`는 400으로 차단된다.
- `EVIDENCE_PRESENTED` 기반 unlock은 로컬 smoke에서 `unlockedEvidences` 1건 반환 및 target evidence detail 200으로 확인했다.
- `coverUpText` 누락 final-deduction은 여전히 200으로 허용된다.
- validate는 로컬에서도 `NEEDS_FIX`, score 40, hints 0으로 재현됐다.
- abandon 응답은 `{"success":true}`이고, abandon 후 final-deduction은 여전히 409 AI010과 부정확한 메시지를 반환한다.
- Docker/prod 기본 Hibernate bind log level은 warn이며, Docker log tail 기준 원문 bind 로그/fallback/MockSolutionReader/AI 호출 실패는 확인되지 않았다.
- 현재 셸 PATH에서 `flutter`/`adb`가 탐지되지 않아 FE emulator UI smoke는 수행하지 못했다.

---

## 2026-06-10 Scenario Play QA - AI Interrogation / Difficulty Handoff

Android E2E 및 운영 API 기준으로 `스튜디오9(CL-011)`, `서월채(CL-010)`를 신규 유저 관점으로 플레이했다.

주의:

```text
이 섹션은 팀 전체 공유용이다.
정답 범인명, 정답 수법 원문, private seed, solution 원문은 포함하지 않는다.
정답 상세와 raw result는 private handoff / 로컬 QA 산출물에서만 확인한다.
```

진행 방식:

```text
1. 정답 문서/seed를 보지 않고 1차 플레이
2. 증거/용의자/타임라인을 읽고 직접 추리
3. 용의자별 동기/동선/접근권/증거 제시 심문 진행
4. 최종 추리 제출 후 결과 확인
5. 서월채는 오답 후 정답을 인지한 상태로 2차 정밀 심문 QA 진행
```

결론:

```text
기본 API와 AI 응답은 동작한다.
하지만 현재 심문 UX는 "유저가 범인을 확정할 때까지 몰아붙이는 게임"보다는
"시간이 지나 핵심 증거가 열릴 때까지 기다린 뒤 정답을 조립하는 게임"에 가깝다.
AI 답변은 자백하지 않는 방향은 맞지만, 핵심 사실 인정과 다음 단서 안내가 부족하다.
```

### 플레이 난이도 평가

#### 스튜디오9

체감 난이도:

```text
중상
```

이유:

```text
- 용의자 수가 7명이라 초반 정보량이 많다.
- MARK 9, 프롬프터/인이어, 조명, 하네스/잠금부 등 물리 단서가 여러 축으로 갈라진다.
- AI가 "모른다", "다른 증거와 비교해야 한다"로 답하는 경우가 많아 확정 질문을 만들기 어렵다.
- 타임라인 화면이 앱에서 충분히 연결되지 않아 시간대 비교가 어렵다.
```

권장 공략 흐름:

```text
1. 각 용의자에게 마지막으로 피해자를 본 시각/장소를 먼저 묻는다.
2. MARK 9, 프롬프터/인이어, 조명, 하네스/잠금부 접근권을 분리한다.
3. "누가 직접 만질 수 있었는가"와 "누가 유도할 수 있었는가"를 나눈다.
4. 핵심 증거를 제시한 뒤, 답변이 회피인지 사실 인정인지 기록한다.
5. 레드헤링 후보는 동기만 있고 실행 수단이 없는지 반증한다.
```

주요 문제:

```text
- AI가 시간대 질문에서 앞뒤 모순 답변을 만들 수 있다.
- 증거 설명이 "다른 증거와 함께 봐야 한다"로 끝나고, 어떤 증거와 비교해야 하는지 약하다.
- 이미지 속 작은 텍스트를 읽어야 하는데, 모바일에서는 판독값이 부족하다.
```

#### 서월채

체감 난이도:

```text
후반 핵심 증거가 열리기 전: 상
후반 핵심 증거가 열린 뒤: 중
```

이유:

```text
- 초반에는 물/약/와인/보안/배우자 문제 모두 그럴듯해 레드헤링이 강하다.
- 후반 핵심 단서가 실행 기회, 시간대, 기록 은폐, 동기 축으로 분산되어 있다.
- 하지만 이 중 후반 핵심 증거가 15분 경과 후에야 전부 열린다.
- 13/25 상태에서는 적극적으로 심문해도 확정 단서가 부족해 오답 후보로 굳어지기 쉽다.
```

권장 공략 흐름:

```text
1. 모든 용의자에게 동기보다 먼저 20:35~21:40 동선을 시간순으로 묻는다.
2. 물병/컵, 야간 약통, 와인잔/디캔터, CCTV 사각지대를 각각 다른 실행 루트로 나눈다.
3. 누가 "직접 준비/작성/접근"했는지 증거 제시로 확인한다.
4. 동기 증거는 레드헤링까지 모두 열어 놓고 비교한다.
5. 최종 후보는 "동기 + 실행 기회 + 은폐 기록"이 동시에 닫히는 사람으로 좁힌다.
```

서월채 정밀 QA 결과:

```text
1차 신규 유저 플레이:
- 최종 제출 D / 32점
- 범인 지목 오답
- 일부 핵심 증거는 맞혔지만, 정답 용의자를 특정하지 못함

2차 정답 인지 후 정밀 심문:
- 신규 session 86
- 심문 105회
- 증거 해금 7/25 -> 13/25 -> 20/25 -> 25/25
- 최종 제출 A / 88점
- 범인/방법/은폐는 맞음
- 동기 문구만 일부 감점
```

### 심문 패턴별 관찰

#### 1. 시간대 질문

질문 예:

```text
20:35 만찬 시작부터 21:40 발견까지 동선을 시간순으로 말해주세요.
마지막으로 피해자를 본 시각과 장소를 정확히 말해주세요.
```

좋은 답변:

```text
특정 시간대, 장소, 행동을 1~2문장으로 인정한다.
예: "21시경 침실 준비를 마치고 복도로 나왔다."
```

나쁜 답변:

```text
사건 기준 시각 이후의 불가능한 방문 시각을 새로 만든다.
예: 피해자 발견 이후 시간대를 사건 전 동선처럼 말함.
```

문제:

```text
유저가 시간대를 조금 잘못 물었을 때 AI가 타임라인 기준으로 정정하지 않고 새 사실을 생성한다.
이 답변이 나오면 유저가 엉뚱한 용의자를 강하게 의심하게 된다.
```

개선:

```text
AI prompt/context에 사건 기준 타임라인 hard guard 추가.
발견 시각 이후 질문에는 "그 시각에는 이미 발견 후입니다"로 정정.
설정에 없는 구체 시각 생성 금지.
```

#### 2. 증거 제시 질문

질문 예:

```text
이 증거 기준으로 누가 직접 접근할 수 있었나요?
이 기록은 당신 업무 기록인가요?
이 시각과 당신 진술이 맞나요?
```

좋은 답변:

```text
증거의 의미 일부를 인정하고 본인 방어를 한다.
예: "그 기록은 제 업무 영역이 맞지만, 사건과 직접 연결된다고 단정할 수는 없습니다."
```

나쁜 답변:

```text
"다른 로그와 함께 봐야 합니다."
"저도 정확히 알 수 없습니다."
"현재 증거만으로는 확인할 수 없습니다."
```

문제:

```text
회피 자체는 자연스럽지만, 너무 자주 반복되면 플레이어가 다음 질문을 만들 수 없다.
특히 "어떤 로그와 비교해야 하는지"를 말하지 않아 진행감이 끊긴다.
```

개선:

```text
각 증거 제시 답변은 최소 1개의 다음 비교 대상을 포함한다.
예:
- "호출 패널 로그와 비교해 보세요."
- "관련 기록 작성 시각을 확인해야 합니다."
- "상태 변화 시각과 맞춰 보세요."
```

#### 3. 레드헤링 반증 질문

질문 예:

```text
당신에게 동기는 있지만, 실제 실행 수단과 직접 연결되는 증거가 있나요?
이 증거는 동기 증거일 뿐 방법 증거는 아닌 것 아닌가요?
```

좋은 답변:

```text
동기는 인정하되 실행 증거가 약하다는 점을 구분해 준다.
예: "그 문서는 제 동기를 설명할 수 있지만, 물병 조작의 직접 증거는 아닙니다."
```

효과:

```text
레드헤링 후보를 논리적으로 닫는 데 도움이 된다.
서월채 정밀 QA에서는 이 방식으로 배우자/의사/보안/케어 계층의 오답 후보를 상당 부분 반증할 수 있었다.
```

개선:

```text
레드헤링 용의자에게 핵심 증거를 제시했을 때
"동기는 있지만 실행 루트가 약하다"는 식의 반증 가능 답변을 더 안정적으로 제공해야 한다.
```

#### 4. 직접 압박 질문

질문 예:

```text
정리하면 당신은 단독 접근했고, 기록도 없고, 물건을 만질 수 있었습니다. 이 중 틀린 말이 있나요?
```

좋은 답변:

```text
자백하지 않더라도, 사실관계는 인정한다.
예: "그 시간에 혼자 있었고 물건을 만질 수 있었던 점은 사실입니다."
```

문제:

```text
현재 AI는 여기서도 "단정할 수 없습니다"를 반복한다.
단정 회피는 맞지만, 인정한 사실과 부정하는 결론을 분리해서 말해야 유저가 논리적으로 확정할 수 있다.
```

개선:

```text
답변 구조를 고정한다.
1문장: 인정 가능한 사실
2문장: 범행 단정은 부인
```

예:

```text
그 시간에 제가 혼자 있었고 해당 물건을 만질 수 있었던 것은 사실입니다.
하지만 그것만으로 제가 사건을 저질렀다고 단정할 수는 없습니다.
```

### 권장 심문 횟수 / 밸런스

현재 관찰:

```text
서월채:
- 17회 심문, 2분대: 7/25
- 27회 심문, 3분대: 7/25
- 40회 심문, 5분대: 13/25
- 70회 심문, 8분대: 13/25
- 90회 심문, 15분대: 25/25
- 정답 확정까지 100회 이상 심문 필요

스튜디오9:
- 용의자 7명 구조라 전체 확인 질문만 해도 20회 이상 필요
- 핵심 증거 제시와 반증까지 포함하면 40회 이상 필요
```

현재 문제:

```text
심문 횟수가 많아지는 이유가 "추리가 깊어서"가 아니라
핵심 증거 해금 대기와 회피 답변 반복 때문이다.
```

권장 MVP 기준:

```text
서월채:
- 용의자당 기본 3~5회
- 핵심 후보 2명은 추가 5~8회
- 총 25~35회 안에 최종 후보를 1~2명으로 좁힐 수 있어야 함
- 총 40회 전후면 최종 제출 가능한 정보가 충분해야 함

스튜디오9:
- 용의자 7명 기준 용의자당 기본 2~3회
- 핵심 후보 3명은 추가 4~6회
- 총 35~50회 안에 최종 제출 가능한 정보가 충분해야 함
```

권장 제한 정책:

```text
MVP 데모/운영 초반:
- 하드 제한은 걸지 않는다.
- 대신 "무료/기본 심문 가이드"를 총 40회 내로 설계한다.
- 40회 이후에는 반복 회피 답변보다 힌트/추천 질문을 강화한다.

추후 rate limit 도입:
- 일반 유저: 시나리오당 AI 심문 50~70회/day 또는 session 기준 60회 권장
- 용의자별 soft cap: 10~15회
- 같은 용의자에게 반복 질문 시 "이미 답한 핵심 사실 요약" 제공
- ADMIN/QA 계정은 rate limit bypass 필요
```

중요:

```text
심문 제한은 게임 디자인 문제와 인프라 비용 문제를 분리해야 한다.
게임 디자인상으로는 30~50회 안에 풀려야 한다.
비용 방어상으로는 60회 전후 soft cap이 현실적이다.
```

### 증거 해금 / 진행감 문제

현재 문제:

```text
핵심 증거가 시간 PHASE 기반으로 열리는 비중이 높다.
유저가 핵심에 가까운 질문을 해도 시간 전이면 후반 증거에 접근하지 못한다.
반대로 시간이 지나면 별도 추리 행동 없이 핵심 증거가 열린다.
```

영향:

```text
추리 실력보다 대기 시간이 진행을 결정한다.
유저가 중간에 오답 후보로 굳어질 수 있다.
심문이 "증거를 여는 행위"로 느껴지지 않는다.
```

개선안:

```text
1. PHASE 해금은 유지하되, 행동 기반 조기 해금 경로를 추가한다.
2. 핵심 증거는 관련 증거 2~3개 확인 + 특정 용의자 증거 제시로 열리게 한다.
3. InterrogationResponse.unlockedEvidences를 UI에서 명확히 보여준다.
4. 증거 해금 toast에 "왜 열렸는지"를 표시한다.
```

예:

```text
서월채:
- 관련 물품 로그 + 알림 로그 + 호출/접근 로그를 모두 확인하면 후속 상태 로그 조기 해금
- 관련 물품 담당자에게 해당 증거를 제시하고 기록 부재를 물으면 후속 기록 증거 조기 해금
- 동기 단서 + 최종 후보 압박 질문 후 후속 동기 증거 조기 해금

스튜디오9:
- MARK 9 + 조명 + 하네스/잠금부 계열을 모두 확인하면 관련 후속 증거 조기 해금
- 프롬프터/인이어 증거를 관련 용의자에게 제시하면 유도 계열 후속 증거 조기 해금
```

### 증거 상세 UX 개선

현재 문제:

```text
이미지 속 텍스트가 작고 흐릿한데, description이 핵심 판독값을 충분히 반복하지 않는다.
유저가 이미지 확대 없이 중요한 시각/기록/필압 차이를 놓칠 수 있다.
```

개선안:

```text
증거 상세에 "판독 결과" 블록 추가.
증거 상세에 "비교할 증거" 블록 추가.
증거 상세에 "이 증거로 물어볼 질문" CTA 추가.
```

예:

```text
판독 결과:
- 21:15 복약 알림
- 21:20 스누즈
- 21:25 스누즈 2회

비교할 증거:
- VIP 케어 호출 패널 로그
- 관련 서비스 체크리스트
- 상태 로그 원시 데이터

추천 질문:
- 이 시각에 누가 관련 물품을 준비했나요?
- 이 기록과 당신의 동선이 맞나요?
```

### 추천 질문 / 힌트 문제

현재 상태:

```text
추천 질문 API 문서: 존재
운영 API 호출: 404
힌트 API: 빈 배열
```

영향:

```text
유저가 13/25 또는 20/25 상태에서 막히면 다음 질문을 찾기 어렵다.
AI 답변이 회피형일수록 추천 질문/힌트가 필요하다.
```

개선안:

```text
MVP 최소:
- FE local 추천 질문이라도 증거/용의자별로 확장
- 증거 상세에서 질문 chip 제공
- 힌트 API가 비어 있으면 FE에서 "아직 제공되는 힌트 없음" 대신 진행 가이드 노출

BE 권장:
- GET /api/play-sessions/{id}/recommended-questions 구현 또는 문서 제거
- suspectId + evidenceId 기준 추천 질문 반환
- 추천 질문은 정답 유도 문구가 아니라 비교/반증 질문 위주로 작성
```

### API / 스포일러 노출

확인된 문제:

```text
용의자 목록에 culpritEligible 노출
asset path에 WITNESS/CULPRIT/RED_HERRING 계열 역할성 이름 노출 가능
includeLocked=true로 잠긴 증거 제목 전체 노출
```

영향:

```text
정상 앱 UI에서는 안 보이더라도 네트워크 응답/로그/API 호출로 스포일러가 새어 나간다.
```

필요 작업:

```text
플레이어용 API에서 culpritEligible 제거
플레이어용 API에서 내부 importance/solution 계열 메타 제거
asset path 역할성 이름 제거 또는 signed alias 사용
includeLocked=true는 admin/debug 전용으로 분리
일반 유저 호출 시 locked evidence는 placeholder만 반환
```

### 담당별 액션 아이템

#### AI / 프롬프트

```text
담당: 배강혁

1. 사건 기준 타임라인 hard guard 추가
2. NPC 답변 구조를 "사실 인정 + 단정 부인"으로 정리
3. 증거별 allowedFacts에 다음 비교 대상 추가
4. 회피형 답변 반복 감소
5. 레드헤링 반증 답변 정책 추가
6. 발견 시각 이후의 사건 전 동선 생성 금지
```

#### 백엔드 / 게임 세션

```text
담당: 소수경 + 배강혁

1. 시간 PHASE 외 행동 기반 조기 해금 추가
2. InterrogationResponse.unlockedEvidences UI 활용 가능하게 검증
3. 추천 질문 API 구현 여부 확정
4. includeLocked / culpritEligible / asset path 스포일러 정리
5. P002 active session 이어하기/포기 후 새 시작 flow 재검증
```

#### Android

```text
담당: 정채림

1. active session 존재 시 상세 CTA를 "이어하기"로 전환
2. P002 수신 시 이어하기/포기 후 새 시작 모달 표시
3. 증거 상세에 판독 결과/비교 대상/추천 질문 표시
4. 증거 해금 toast 또는 modal 표시
5. 타임라인에서 관련 증거/용의자 이동 연결
```

#### 시나리오 / 인프라

```text
담당: 황도윤

1. 서월채/스튜디오9 핵심 증거별 판독 결과 텍스트 보강
2. 레드헤링 증거의 반증 단서 보강
3. 핵심 추리 경로 핵심 증거가 자연 플레이 30~40회 심문 안에 열리는지 확인
4. QA/Admin 계정 rate limit bypass 정책 auth 쪽과 연결
5. 공개 문서에는 정답/seed/solution 원문 이동 금지 유지
```

### 재QA 체크리스트

패치 후 확인:

```text
1. 서월채 신규 플레이에서 30~40회 심문 안에 최종 후보 1~2명으로 좁혀지는가
2. 스튜디오9 신규 플레이에서 40~50회 심문 안에 최종 제출 가능한가
3. AI가 발견 이후 시각을 사건 전 동선으로 생성하지 않는가
4. 증거 제시 답변이 다음 비교 대상 1개 이상을 제공하는가
5. 레드헤링 후보에게 핵심 증거를 제시하면 반증 가능 답변이 나오는가
6. 최종 후보 압박 시 자백은 안 하더라도 핵심 사실 인정이 나오는가
7. 행동 기반 조기 해금이 실제로 unlockedEvidences에 표시되는가
8. 앱에서 P002 active session 이어하기/포기 후 새 시작이 모두 되는가
9. 플레이어용 API에 정답성 메타데이터가 남아 있지 않은가
10. 최종 추리 결과가 timeout/network 후에도 result 화면에서 복구되는가
```

# FE(Flutter)↔BE(Spring) 통합 리뷰 — P0/P1/P2

- **작성일:** 2026-06-04
- **대상 HEAD:** BE `develop` / FE `project-fe`(현재 로컬 최신)
- **성격:** 읽기 전용 리뷰. **코드 변경 없음.** 모든 주장은 양측 실제 소스 `file:line`으로 추적 가능.
- **방법 (3-pass):**
  1. **정적 FE↔BE 계약 리뷰** — BE/FE/Docs 3축 인벤토리 → 6개 차원(coverage·dto·envelope·auth·aiflow·docsproc) 다중 에이전트 리뷰 → 발견별 적대적 검증(43건, INVALID 0건) + 리뷰어 독립 `file:line` 검증 net-new 3건(★). → **§1~§3, 부록 A**
  2. **private seed 검증(운영 동일본)** — 공식 시나리오 YAML(seowolchae/studio9) 구조 파싱으로 seed 의존 항목 확정(스포일러 제외). → **부록 B**
  3. **운영 MVP QA 대조(런타임)** — 팀장 운영 API QA(`docs/MVP_PLAY_FLOW_QA_2026-06-04.md`, 정상/비정상 플로우, BE/AI/Infra)를 정적 리뷰와 대조·통합. → **§4, 부록 C**

---

## 0. Codex 재검증 가이드

- **레포 경로**
  - BE: `C:/Users/Russell/Desktop/Workspace/start-up-project` (Spring Boot 4, Java 21, package `com.startup`)
  - FE: `C:/Users/Russell/Desktop/Workspace/project-fe` (**Flutter/Dart**, package `clueroom`) — ※ 문서엔 "Android/Kotlin"으로 오기(P2-6)
- **읽는 법**: 본문(§1~§3)은 중복제거된 "이슈" 단위 요약. 각 이슈 끝의 `근거 finding: <slug>`는 **부록 A**의 원자 검증 로그를 가리킨다. Codex는 본문에서 이슈를 잡고, 부록 A에서 해당 slug의 `검증 재현`/`정정` 전문을 보고 `file:line`을 직접 열어 확인하면 된다.
- **신뢰도 주의**: 부록 A의 `정정` 항목은 에이전트가 **자기 발견의 라인/문서 인용 오차를 스스로 교정**한 것이다. 본문 severity는 교정 반영본. 재검증 시 `정정`을 먼저 읽으면 위양성(false positive)을 줄일 수 있다.
- **공통 계약(정상 확인)**: 응답 엔벨로프 `ApiResponse{success,data,error}`, `ErrorResponse{code,message,status}`, `PageResponse{content,page,size,totalPages,totalElements,hasNext}` 파싱은 양측 정합(근거 `envelope-pagination-contract-ok`). 재플래그 불필요.

### 요약 카운트

**(A) 정적 FE↔BE 계약 리뷰** — §1~§3, 부록 A (중복제거 기준)

| 심각도 | 건수 | 핵심 |
|---|---|---|
| **P0** | 1 | 최종추리 채점 지연→재제출 409(AI010)→결과화면 영구 진입 불가 |
| **P1** | 11 | 세션복구 미연동·증인 판별필드 누락·타임라인/리뷰/북마크 미구현·검색필터 무시·증거해금 등 |
| **P2** | 22 | 인증/FCM/스펙드리프트/이미지/enum/날짜/버저닝 등 후순위·잠재 |

> 원자 발견 43건(부록 A) 기준 검증 후 분포: **P0=1, P1=11, P2=31**(중복 포함). 본문은 동일 이슈를 합쳐 P2를 22로 축약.

**(B) 운영 MVP QA 추가** — §4, 부록 C (런타임 / BE·AI 내부 / seed / 운영)

| 심각도 | 건수 | 핵심 |
|---|---|---|
| **P0/P1** | 9 | 심문 기반 증거해금 미구현·request 500 매핑·EVIDENCE_PRESENTED null·validate(hints=0)·idle 전체해금·AI policy 약함·history 5턴 cap·race activeSessionId 누락·coverUpText 허용 |
| **P2** | 9 | 텍스트 길이 무제한·string ID coercion·abandon 응답/에러·용의자 seed 품질·hints seed 부재·locked title 노출·**Hibernate TRACE 평문 로깅** 등 |

> 운영 QA 18건 중 **약 14건이 정적 리뷰에 없던 신규**(정적 계약 ↔ 런타임 서버 QA의 스코프 차이 — §4 머리말). 반대로 우리 정적 리뷰의 **FE측 발견은 QA에 없음**(QA는 서버만 실행, Flutter 앱 미검증 — 부록 C.2).

**출처 범례**: `[정적]` 계약 리뷰 / `[seed]` private 시드 검증 / `[런타임]` 운영 QA / `[코드확정]` 본 리뷰가 소스로 직접 재현.

---

## 1. P0 — 즉시 수정 권장 (1건)

### P0-1. 최종추리 채점 지연 + 재제출 409(AI010) → 결과화면 영구 진입 불가
- **현상**: BE `final-deduction`은 요청 스레드 내부에서 **동기 LLM 채점**(최대 1500토큰)을 수행. 이 호출이 FE 고정 타임아웃 20초를 초과하지만 BE는 **정상 완료**(세션 `COMPLETED`, `FinalDeduction` 저장)되는 경우, FE는 일반 오류로 처리한다. 사용자가 재시도하면 두 번째 호출이 **409 AI010(이미 제출됨)** 을 반환하는데, `submit_screen`은 `status >= 500`만 특수 처리하고 409는 "제출 실패" 스낵바만 띄운 뒤 **결과 화면으로 절대 이동하지 않는다.** 채점이 끝난 게임에서 결과를 못 보고 영구히 갇힌다.
- **FE 증거**: `lib/screens/submit_screen.dart:130-136`(성공 경로에서만 `ResultScreen` push), `:137-142`(>=500만 특수 분기, 409는 일반 실패); 타임아웃 `lib/core/api/api_config.dart:29`(20s 고정), 적용 `lib/core/api/api_client.dart:97`
- **BE 증거**: `domain/ai/service/AiDeductionScorer.java:82`(`ensureNotSubmitted`), `:141-146`(중복 제출→AI010), `:102`→`:258`(동기 `aiClient.chat`), `:125`(`saveResultAndComplete`=저장+완료 결합); `domain/ai/error/AiErrorCode.java:20`(AI010→`HttpStatus.CONFLICT`)
- **문서**: `docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:1240`("AI010 | 이미 최종 추리를 제출함 | 결과 화면으로 이동 시도"), `:1247-1251`("AI010 시 `GET /result`로 결과 확인")
- **영향**: 실 LLM 사용 시 느린 채점을 겪은 플레이어는 완료·채점된 세션의 결과에 도달 불가. 핵심 MVP 플레이 루프의 하드 데드엔드.
- **권장(코드변경 없음)**: 409 코드 AI010(및 채점 진행중 AI015) 수신 시, 문서 규정대로 `GET /api/play-sessions/{sessionId}/result`를 호출해 `ResultScreen`으로 `pushReplacement`. + AI 엔드포인트(interrogation/final-deduction)에 한해 타임아웃 상향 또는 제출을 비동기화(즉시 응답 후 결과 폴링).
- **재현 주의**: 로컬 Mock 모드(`SPRING_AI_MODEL_CHAT=none`)는 fallback 즉시 응답이라 미재현 → **실 OpenAI 연동 데모/운영에서만 발현.** 데모데이 기준 P0.
- **근거 finding**: `ai010-resubmit-not-recovered`(P0), `fe-final-deduction-409-stuck`, `ai-latency-vs-fe-timeout-no-retry`

---

## 2. P1 — MVP 품질 직접 영향 (11건)

### P1-1. 진행중 세션 복구를 FE가 BE의 `/active`·`409 details`에 연결하지 않음 *(FE팀이 원래 P0로 제기)*
- **BE는 요청(R1)을 두 방식 모두 충족**: `GET /api/play-sessions/active?scenarioId=`(`PlaySessionController.java:34-42` → `ActivePlaySessionResponse{hasActiveSession,activeSessionId,scenarioId,status,startedAt}`, `ActivePlaySessionResponse.java:8-14`) **및** 409 P002 바디의 `details.activeSessionId`(`PlaySessionService.java:129-135` → `GlobalExceptionHandler.java:181-191` → `ErrorResponse.java:22`, `@JsonInclude(NON_NULL)`).
- **FE는 둘 다 미사용**: `PlaySessionRepository`에 `/active` 호출 메서드 없음; `ApiException`에 `details` 필드 자체가 없어(`api_exception.dart:5-19`) `api_client._parse`가 code/message/status만 추출(`api_client.dart:137-143`); 409는 데드엔드 메시지만 설정(`game_session_controller.dart:100-105`). 복구는 로컬 SharedPreferences 캐시(`:67-83`, `_tryResume :114-128`)에만 의존.
- **영향**: 재설치·캐시 삭제·다른 기기 등으로 로컬 키가 없는데 서버에 PLAYING 세션이 남으면, 매 진입이 409 → **재개도 정리도 불가**(영구 차단).
- **권장**: 409(P002) 시 `GET /play-sessions/active?scenarioId=` 호출(또는 `error.details.activeSessionId` 파싱 — 단 FE 엔벨로프에 `details` 노출 추가 필요)로 기존 sessionId 확보 후 `/dashboard` 재개 또는 `/abandon` 정리.
- **근거 finding**: `fe-409-recovery-not-wired-to-active`, `active-session-recovery-not-wired-fe`, `fe-cannot-consume-409-activesessionid`

### P1-2. AI 응답 지연 vs FE 20s 고정 타임아웃 + 재시도 부재
- 모든 호출 공통 20s(`api_config.dart:29`→`api_client.dart:97`), 서버측 AI 클라이언트 타임아웃 미설정(`application.yml:98` deduction max-tokens 1500). 타임아웃은 `status==null`(네트워크 오류)로 분류(`api_exception.dart:22`)되어 자동 재시도 없음 → P0-1을 가중.
- **권장**: AI 엔드포인트 전용 타임아웃 또는 비동기화. 최소한 `TIMEOUT` 코드와 네트워크 실패를 구분하고, final-deduction은 타임아웃 후 `GET /result`로 "이미 제출됐는지" 탐침.
- **근거 finding**: `ai-latency-vs-fe-timeout-no-retry`

### P1-3. 심문 대화 이력이 서버에서 복원되지 않음
- 채팅 화면은 메모리 `_logs`(`game_session_controller.dart:240-241`, writer는 `addInterrogationLog :277-280`뿐)만 표시(`interrogation_chat_screen.dart:64-74`), `GET /interrogations`로 시드하지 않음(`_refreshAll :132-156`/`_tryResume`가 호출 안 함). 세션 재개 시 이전 대화 소실, 기본 멘트만 노출. (BE 이력은 정상: `InterrogationLogQueryService.java`, `AiInterrogationController.java:34-39`)
- **권장**: 세션 로드/재개 시 `GET /interrogations`로 채팅 이력을 시드.
- **근거 finding**: `chat-history-not-restored-from-server`

### P1-4. 용의자 "증인/지목가능" 판별 필드 누락 → 증인이 범인 지목 대상에 노출
- FE는 `isWitness` 또는 `characterType=='NEUTRAL_WITNESS'`로 지목 가능 용의자를 필터(`play_models.dart:236-254`, `game_session_controller.dart:39-40` `accusableSuspects`). `PlaySuspectResponse`는 **둘 다 미노출**(`PlaySuspectResponse.java:4-15`) — 엔티티 `Suspect`엔 `characterType`+`culpritEligible`이 **존재함에도** 매핑에서 누락(`PlaySessionService.java:582-592`). 결과: FE `isWitness` 항상 false → **증인을 범인으로 지목 가능**(절대 정답 불가한 id로 채점). 스펙 §9.7 예시에도 필드 없음.
- **권장**: `PlaySuspectResponse`에 `culpritEligible`(또는 `characterType`) 추가·매핑. 스펙 §9.7도 동반 갱신.
- **근거 finding**: `suspect-witness-discriminator-dropped`
- **✅ seed 검증(2026-06-04, 부록 B) — 확정·영향 구체화**: 두 공식 시나리오 모두 지목불가 캐릭터 보유 — **seowolchae 1명**(NEUTRAL_WITNESS), **studio9 3명**(NEUTRAL_WITNESS 1 + PERMANENT_RED_HERRING 2). 11개 seed 사본 전체 일관. `Suspect` 엔티티엔 `characterType`+`culpritEligible`이 import되나(`Suspect.java:35,38`) `PlaySuspectResponse`가 누락 → **현재 두 공식 시나리오 모두 지목불가 캐릭터가 범인 후보 드롭다운에 노출됨(확정 실버그).** FE의 `NEUTRAL_WITNESS`-only 판정은 studio9의 RED_HERRING 2명을 못 거름 → **`culpritEligible` 노출이 정답.**

### P1-5. 타임라인: BE 구현됨에도 FE는 미구현으로 간주(샘플/플레이스홀더)
- `GET /timeline` 정상 구현(`PlaySessionController.java:98-106` → `List<PlayTimelineResponse>{time,title,description,eventType,relatedEvidenceId}`, 서비스 `PlaySessionService.java:291-330`은 소유권·스포일러 필터 포함 실구현). FE는 repo 메서드 없음 + CL-001 샘플 하드코딩 + stale TODO(`timeline_screen.dart:33-42`), 그 외 시나리오는 "타임라인 준비 중"(`:50-61`). FE backend-request R2가 이미 해소됐으나 FE 미반영.
- **권장**: `PlaySessionRepository.timeline(sessionId)` 추가·연결, CL-001 게이트 제거. (단 FE 사본 스펙은 `isTrueEvent`를 기대하나 BE DTO엔 없음 — P2-7/P2-8 동반 확인)
- **근거 finding**: `fe-unaware-timeline-implemented`
- **✅ 런타임 QA(부록 C)**: 운영에서 timeline 엔드포인트 정상(서월채 11건, 스튜디오9 11→13건; locked evidence 연결 event 선노출 0). BE는 동작 — **FE 미연동이 유일한 갭.**

### P1-6. ★시나리오 검색·필터(keyword/type/difficulty)를 BE가 서버에서 무시 *(워크플로우 미탐지 · 리뷰어 독립 발견)*
- FE는 `keyword/type/difficulty/sort/page/size`를 전송(`scenario_repository.dart:53-65`). 그러나 `ScenarioService.getScenarios`는 `condition`을 사실상 버리고 **상태(PUBLISHED)+가시성(PUBLIC/OFFICIAL)으로만** 조회(`ScenarioService.java:42-73`), `:48`에 `// TODO: 세부 필터링(condition)은 나중에 QueryDSL 도입 시 추가`. `sort`만 `mapPageableSort`(`:113-144`)로 동작. `ScenarioSearchCondition`(`:11-20`)에 `keyword/type/difficulty` 필드는 있으나 미사용.
- **영향**: 라이브러리 검색창·필터칩이 **서버에서 무동작**(항상 전체 published 목록 반환). UI는 필터가 되는 것처럼 보이나 실제 미적용. (심각도는 카탈로그 규모 의존 — MVP 시나리오 수가 적으면 체감 낮음)
- **권장**: `ScenarioService`에서 `condition.keyword/type/difficulty`를 실제 쿼리 조건으로 반영(QueryDSL 도입 또는 파생 쿼리). 또는 필터 UI를 명시적으로 "추후" 비활성.
- **근거**: 본 리뷰어 독립 검증(부록 A 미수록 — Codex 직접 확인 권장: `ScenarioController.java:24-33`, `ScenarioService.java:42-73`, `ScenarioSearchCondition.java:11-20`, `scenario_repository.dart:53-65`).

### P1-7. 북마크 엔드포인트 미구현(스펙상 1차 MVP)
- 스펙 §12.1-12.2 `POST/DELETE /api/scenarios/{scenarioId}/bookmarks`(+ §4.2 `GET /api/scenarios/bookmarked`). **BE 컨트롤러 없음**(`ScenarioController.java:24-43`은 list+detail만). FE는 `shared_preferences`(`bookmark_<id>`)에만 저장(`scenario_detail_screen.dart:18,41-53`), `isBookmarked`는 BE도 `false` 하드코딩(`ScenarioService.java:96-98`, TODO). 기기간 동기화·집계 불가.
- **근거 finding**: `be-bookmark-endpoints-missing`

### P1-8. 리뷰 엔드포인트 미구현(스펙상 1차 MVP)
- 스펙 §12.3-12.4 `POST/GET /api/scenarios/{scenarioId}/reviews`. **BE 컨트롤러 없음**. FE는 메모리 `sampleReviews`에 read/write(`scenario_detail_screen.dart:59` 읽기, `:215` `sampleReviews.insert(0, review)`), `review_models.dart:24`가 가변 top-level 리스트 → 앱 재시작 시 소실, 서버 미도달. averageRating/ratingCount 구동 불가.
- **근거 finding**: `be-review-endpoints-missing`

### P1-9. 커스텀 시나리오 제작 플로우 부재 → validate 엔드포인트 소비자 없음
- validate는 스펙상 1차 MVP. BE `POST /api/ai/scenarios/{scenarioId}/validate`(+ `GET /api/scenarios/{scenarioId}/validation-result`)는 완전 구현(`AiScenarioValidationController.java:23,32`)이나, FE에 제작/편집/초안 화면 없음(grep `validate|validation|createScenario|draft` 0건; `ScenarioRepository`는 query/popular/detail만). **MVP 범위 확정 필요.**
- **근거 finding**: `no-custom-scenario-flow-validate-unused`

### P1-10. Android cleartext HTTP 미허용 → 로컬 개발 시 전체 API 호출 실패 가능
- 개발 base URL `http://10.0.2.2:8080`(Android 에뮬레이터) / `http://localhost:8080`(`api_config.dart:16-22`). `AndroidManifest`(main/debug/profile 전부)에 `usesCleartextTraffic`/`networkSecurityConfig` 없음, `network_security_config.xml` 부재. targetSdk 기본값(Flutter ^3.12.0 → API 34/35대)에서 cleartext 기본 차단 → 로컬 도커 백엔드 호출이 `Cleartext HTTP not permitted`로 막힐 수 있음. 운영 `https://api.clueroom.xyz`는 무관.
- **권장**: debug 한정 network-security-config(또는 debug 매니페스트 `usesCleartextTraffic=true`)로 10.0.2.2/localhost 허용. 운영은 https 유지.
- **근거 finding**: `cleartext-http-android-targetsdk`

### P1-11. 증거 해금 메커니즘 — MANUAL 미사용(반증) + ★심문 기반 해금 미구현(런타임 확정)
- **정적 발견(원안)**: `EvidenceUnlockType`(`:3-10`)={NONE,PHASE,TIME,INTERROGATION,EVIDENCE_PRESENTED,MANUAL}. `EvidenceUnlockPolicy.canAutoUnlock`(`:27-51`)은 TIME/PHASE만 자동 해금. FE는 `/unlock`(`PlaySessionController.java:119-129`)을 **어디서도 호출 안 함**(grep `/unlock` 0건).
- **✅ seed 검증(부록 B) → MANUAL 반증**: 두 공식 시나리오 unlockRule이 **전부 PHASE**(seowolchae 25, studio9 35; 22개 파일 MANUAL 0건). "MANUAL 증거 영구잠금"은 **현 콘텐츠에선 미발생** → 이 하위 항목 **P2/비이슈로 강등.**
- **🆕 런타임 QA → 진짜 문제는 INTERROGATION 해금 미구현(→ §4 QA-1) [코드확정]**: seed가 전부 PHASE라는 건 곧 **심문/증거제시로 단서가 열리는 경로가 콘텐츠에 없다**는 뜻. 코드상으로도 `InterrogationCompletedEvent`는 발행만 되고(`AiInterrogationService.java:117`) **수신 리스너가 0개**, `unlockedEvidences`는 항상 `[]`(`InterrogationResponse.java:26` 선언뿐, `AiInterrogationService.java:78` `List.of()`). 즉 "AI 심문으로 단서를 여는" 핵심 게임 루프가 데이터·코드 양쪽 미구현. **실질 심각도는 QA-1(P0/P1)로 승계.**
- **근거**: `EvidenceUnlockType.java`, `EvidenceUnlockPolicy.java:27-51`, `PlaySessionController.java:119-129`, 부록 B(seed), §4 QA-1.

---

## 3. P2 — 후순위 / 잠재 / 드리프트 (22건, 그룹별)

### 인증·아이덴티티 (post-MVP, 잠재 데이터 블리드)
- **P2-1** FE가 Authorization 헤더를 한 번도 보내지 않음 — `authTokenProvider`가 어디서도 할당 안 됨(`api_client.dart:61` 선언, `:86` 읽기뿐, grep으로 할당 0건), `AuthService.saveTokens` 호출자 없음, `init()`이 `'mock_jwt_token'` 기본값(`auth_service.dart:21`). BE `MockUserProvider`는 헤더 무시·userId 고정(`MockUserProvider.java:13-19`, 기본 1). → **모든 유저/기기가 동일 식별자 공유**(멀티유저 시 세션·로그·디바이스토큰·결과 블리드; 소유권 체크가 모두 userId=1로 통과). JWT 도입 시 헤더 미전송으로 전부 401. *근거: `fe-no-auth-header-shared-mock-user`*
- **P2-2** 로그인/회원가입 화면 없음, 스플래시는 `onboarding_complete` 플래그로만 분기(`splash_screen.dart:49-63`), `isLoggedIn` 영구 false(`auth_service.dart:41-42`). BE 인증 컨트롤러 부재(post-MVP). *근거: `login-onboarding-no-backend-auth`, `fe-no-auth-no-profile`*
- **P2-3** 403/P004(타세션 접근) FE 미처리 — `api_exception.dart:24-25`엔 401/404 헬퍼만. BE는 소유권 403을 적극 enforce(`PlaySessionService.java:156-157` 등, `AiSessionAccessDeniedTest` 통과). userId 고정 동안은 잠재; 실 식별자 도입 시 재개 경로에서 403이 조용히 세션 폐기로 마스킹될 수 있음. *근거: `fe-no-403-p004-handling`*

### FCM / 디바이스 토큰 (계약 불일치 + 미구현)
- **P2-4** FE가 FCM 토큰을 **등록조차 안 함** — `_registerFcmTokenWithBackend`(`main.dart:67-70`)는 `debugPrint`만 하는 스텁. TODO가 가리키는 `PATCH /api/users/me {fcmToken}`은 **BE에 없는 경로**이고, 실제 구현은 `POST /api/device-tokens {token, deviceType}`(`DeviceTokenController.java:29-37`, `DeviceTokenRegisterRequest.java:9-19`). 경로·바디·필드명 전부 불일치 → 푸시 타겟팅 불가. (BE는 MockUserProvider로 인증 전에도 등록 가능) *근거: `fcm-token-never-registered-wrong-endpoint`, `be-users-me-missing-fcm-stub`, `fcm-token-registration-wrong-endpoint`*

### 스펙/문서 드리프트 (단일 정본 부재)
- **P2-5** ★상세 응답 이미지/태그/추천 필드 — BE `ScenarioDetailResponse`는 `coverImageUrl`/`mapImageUrl`(`:14-15`) 전송, FE `_fromDetailJson`은 `thumbnailUrl`을 읽음(`scenario_repository.dart:134`) → **상세화면 커버 이미지 null.** 또 `tags`는 BE가 항상 빈 배열(`ScenarioDetailResponse.java:68` `Collections.emptyList()` TODO), `GET /result`의 `nextRecommendedScenarios`도 항상 빈 배열(`AiDeductionScorer.java:219` `List.of()`) → FE 태그/추천 섹션 공허. *(리뷰어 독립 발견 — Codex 직접 확인)*
- **P2-6** 모든 BE 문서·FE 자체 `api-spec.md`가 FE를 **Android/Kotlin으로 오기**(실제 Flutter/Dart; `pubspec.yaml:2`, `lib/**/*.dart`, MainActivity는 Java). `CLAUDE.md` 스택 블록, `CaseLab_AI_API_Spec.md:4`("기준 플랫폼: Android App"), `ANDROID_SCREEN_API_MAPPING.md:1` 등. *근거: `doc-drift-android-vs-flutter`*
- **P2-7** 두 스펙 사본 드리프트 — `/locations`가 BE 정본·DTO는 **객체**(`PlayLocationsResponse{...,locations:[...]}`), FE `api-spec.md` 사본은 **플랫 배열**(`[{locationId,name,...,evidences:[...]}]`). FE가 자기 사본대로 구현하면 `data as List` vs `Map` 캐스트 크래시 트랩. 현재 FE 미연동이라 잠재. *근거: `two-specs-diverged-locations-shape`*
- **P2-8** FE `api-spec.md`가 구버전 — `GET /active`·§9.1.1·409 P002 `details.activeSessionId`·`canPlay`·`portraitImageUrl`·P010/P011 누락, timeline `isTrueEvent` 잔존. 두 파일 모두 "MVP v0.1" 동일 버전 표기. *근거: `two-specs-diverged-missing-active-and-409`*
- **P2-9** API 버저닝 없음(`/v1` 또는 버전 헤더 부재; `apiPrefix`는 선언만 미사용 `api_config.dart:26`), 버전 문자열이 호환성 신호 역할 못함. *근거: `no-api-versioning`*

### 필드/계약 드리프트 (크래시 아님, 데이터 손실/무동작)
- **P2-10** 종합추리 설명(`_summaryCtrl`)을 FE가 **필수 입력**(min 10자 게이트)으로 받지만 `FinalDeductionRequest`(`:11-28`)에 필드 없어 **전송 안 됨**(`submit_screen.dart:31,70-71,121-128`). 헛수고 입력. → backend-requests **P2 R4** 결정 필요(필드 추가 vs 선택화). *근거: `summary-text-collected-required-not-sent`, `final-deduction-summary-not-sent`*
- **P2-11** `GET /interrogations`(`InterrogationLogResponse`: `presentedEvidence`+`questionType`)를 FE가 POST용 `InterrogationResult`로 파싱 → 두 필드 **무시·손실**(크래시 아님; `play_session_repository.dart:86-97`, `play_models.dart:346-359`). 현재 `_LogCard`가 question/answer만 렌더(`suspect_detail_screen.dart:357-365`)라 UI 영향 없음. *근거: `history-drops-questiontype-presentedevidence`, `interrogation-log-shape-mismatch`, `interrogation-log-dto-shape-mismatch`*
- **P2-12** FE가 `canPlay`/`isBookmarked` 미파싱(`scenario_repository.dart:95-136`, 모델에 필드 자체 없음 `scenario.dart:7-40`), 플레이 가능 여부를 `{'1','4','5'}` **하드코딩**(`scenario_detail_screen.dart:17,33`). 현재 `canPlay`는 항상 true(`ScenarioAccessService.canPlay` 고정 true)라 영향 제한적이나, id가 {1,4,5} 밖인 공개 시나리오가 "준비 중"으로 잘못 비활성. *근거: `fe-ignores-canplay-hardcoded-playability`, `fe-ignores-canplay-isbookmarked`*
- **P2-13** `Difficulty` enum: BE `{EASY,NORMAL,NORMAL_PLUS,HARD}`(`Difficulty.java:3-8`), FE `{easy,medium,hard}`(`scenario.dart:3`). FE는 EASY/HARD 외 전부 medium으로(`scenario_repository.dart:147-151`) → NORMAL_PLUS가 '보통'으로 뭉개지고 필터 불가. **주의(정정)**: 스펙 §3.4는 오히려 `{EASY,NORMAL,HARD}`만 명시 → NORMAL_PLUS는 **BE-only 미문서화 값**. **✅ seed 검증(부록 B): studio9가 실제 `difficulty: NORMAL_PLUS` 사용(전 사본) → 공식 시나리오 1개가 FE에서 '보통'으로 오표기·필터 불가(실영향 확정).** *근거: `difficulty-enum-coverage-mismatch`, `difficulty-enum-set-mismatch`*
- **P2-14** 증거 `categoryLabel`을 FE가 읽지만(`play_models.dart:175`) BE 미전송(항상 null), BE `oneLine`(`PlayEvidenceResponse.java:11`)은 FE 미사용. 엔티티 `evidenceType`을 `categoryLabel`로 노출하면 해결. *근거: `evidence-categorylabel-vs-oneline`*
- **P2-15** FE 주석이 `SESSION_ALREADY_EXISTS` 참조하나 실제 BE 코드 `P002`(`play_session_repository.dart:12`). 런타임은 status 기반이라 무해, 주석만 stale(backend-requests R1b). *근거: `session-already-exists-stale-code-comment`*

### 자원 / 전송 / 시간
- **P2-16** 이미지 자원 계약 — BE가 raw `imageAssetKey` + (로컬선 null인) `imageUrl` 전송(`PlaySessionService.java:263,268-269`; `ScenarioAssetUrlResolver.java:24-28`이 base URL 미설정 시 null; `application.yml:103-105` 기본 빈값). FE는 non-http 키를 번들 에셋으로 시도(`asset_image_widget.dart:50-72`)하나 `pubspec.yaml:27-36`에 `assets:` 섹션 없음 → **로컬에서 모든 이미지가 플레이스홀더로 폴백**. graceful. **✅ 런타임 QA(부록 C): 운영은 S3 base URL 설정 → imageUrl 200 OK(커버/맵/증거 정상). 이 갭은 로컬 한정, 운영 해소.** *근거: `asset-key-vs-url-contract`*
- **P2-17** `LocalDateTime` 타임존 오프셋 없이 직렬화, FE `DateTime.tryParse`는 로컬시간 해석(`play_models.dart:516-519`; startedAt/createdAt/usedAt/submittedAt). 현재 해당 필드 UI 표시 소비자 거의 없음 — 영향 미미. *근거: `localdatetime-no-timezone`*
- **P2-18** FE가 `ApiException.status`를 HTTP 상태줄이 아닌 **바디 `error.status`**에서 취득(`api_client.dart:142`, 없으면 `res.statusCode` 폴백) → 앱 핸들러를 안 거치는 프록시/LB 5xx에서 오분류 가능. 현재 단일 서비스·`GlobalExceptionHandler`가 일관되게 status 동기화(`:184`)라 잠재. *근거: `fe-status-from-body-not-http-line`*

### 커버리지 노트 (저위험)
- **P2-19** my-records(`GET /play-sessions/me`) 미구현 — 2차 MVP, FE 샘플 구동(`my_records_screen.dart`, `samplePlaySessions`). *근거: `be-my-records-endpoint-missing`*
- **P2-20** 로그인/회원가입/`users/me`/마이페이지 프로필 미구현 — post-MVP 정상. *근거: `fe-no-auth-no-profile`*
- **P2-21** 구현됐으나 FE 미소비 엔드포인트: 세션상세·`/active`·증거상세·증거 unlock·용의자상세·locations·timeline·validate·device-tokens(`PlaySessionController.java:34-140` 등). 화면 미연동/템플릿. 로드맵 대조용. (참고: `POST /api/notifications/test`는 `@Profile({"local","test"})` 한정) *근거: `be-implemented-unconsumed-play-endpoints`*
- ✅ **P2-22 (정상 확인)** 엔벨로프·에러 코드/메시지·`PageResponse` 페이지네이션·charset 계약 모두 정합(`api_client.dart:29-44,120-150` ↔ `ApiResponse/ErrorResponse/PageResponse`). ST-46 채점실패 500(AI014)도 FE `>=500` 분기와 정합, SOLUTION_NOT_FOUND는 404(AI011) 유지. **재플래그 불필요.** *근거: `envelope-pagination-contract-ok`*

---

## 4. 운영 MVP QA 추가 이슈 (런타임 — BE/AI/Infra)

> **출처**: 팀장 운영 API QA(`docs/MVP_PLAY_FLOW_QA_2026-06-04.md` / `docs/MVP_QA_ISSUE_HANDOFF_2026-06-04.md`, 정상+비정상 플로우, 대상 `https://api.clueroom.xyz`, scenarioId=10 서월채 / 11 스튜디오9).
> 위 §1~§3은 **정적 FE↔BE 계약**을, 이 절은 **운영 서버 런타임 + BE/AI 내부 + seed + 운영 로깅**을 다룬다 — 두 축은 상보적이며, 아래 18건 중 다수는 계약 diff로는 구조상 안 보이는 신규다(전체 대조 = **부록 C**).
> **운영에서 정상 통과 확인된 기본 흐름**: 시나리오 목록/상세·이미지 URL·세션 생성/active 복구·장소/타임라인/증거 조회·증거 잠금 마스킹·시간 기반 PHASE 해금·심문 기본 응답/로그 저장·최종추리 제출/결과 조회·동시 final-deduction lock.

### 4.1 P0/P1

#### QA-1. ★심문 기반 증거 해금 미구현 (핵심 게임 루프 결손) `[런타임]`+`[코드확정]`
- **현상**: 심문/증거 제시를 해도 `InterrogationResponse.unlockedEvidences`가 항상 `[]`. 새 단서가 열리지 않음.
- **BE 증거 [코드확정]**: `InterrogationCompletedEvent`는 발행만 됨(`AiInterrogationService.java:117`), `src/main/java` 전체에 `@EventListener`/`@TransactionalEventListener` **0개**(이벤트가 허공으로 발행). `unlockedEvidences`는 DTO 선언(`InterrogationResponse.java:26`)뿐, 서비스에서 채우는 코드 없음(`AiInterrogationService.java:78` `List.of()`). seed unlockRules 전부 PHASE(부록 B).
- **영향**: "AI 심문/증거 제시로 단서를 여는 추리게임" 핵심 루프가 데이터·코드 양쪽에서 미동작. 튜터 피드백 "증거를 제출해도 발뺌"과 동일 축. 정적 P1-11에서 승계.
- **권장(코드변경 없음 — 작업 항목)**: `InterrogationCompletedEvent` 리스너(또는 `InterrogationUnlockService`) 구현 → 심문 전/후 해금 diff 계산 → `unlockedEvidences` 실제 반환. YAML에 INTERROGATION/EVIDENCE_PRESENTED unlock rule 추가. 통합 테스트.
- **담당**: 배강혁(AI) / 소수경(seed)

#### QA-2. request body 파싱/타입/enum 오류가 500으로 떨어짐 `[런타임]`+`[코드확정]`
- **재현**: malformed JSON / `selectedEvidenceIds`에 숫자(배열 아님) / `questionType` invalid enum(`"HACK"`) → 전부 **500 C010**(기대: 400 계열). final-deduction·interrogations 양쪽 동일.
- **BE 증거 [코드확정]**: `GlobalExceptionHandler`에 `HttpMessageNotReadableException` 핸들러 **0개** → body 역직렬화/enum 파싱 실패가 generic `Exception`(500)으로 떨어짐.
- **영향**: 잘못된 클라이언트 입력이 서버 오류로 표시 → FE가 4xx로 구분 불가, 모니터링 노이즈.
- **권장**: `HttpMessageNotReadableException`/`InvalidFormatException`/enum·type mismatch를 400으로 매핑.
- **담당**: 배강혁/백엔드

#### QA-3. EVIDENCE_PRESENTED인데 presentedEvidenceId=null이어도 AI 호출됨 `[런타임]`
- **재현**: `{questionType:"EVIDENCE_PRESENTED", presentedEvidenceId:null}` → 200, AI 호출·interrogationCount 증가(기대: 400).
- **BE 증거**: `InterrogationRequest`(`:10-26`)에 cross-field 검증 없음(`presentedEvidenceId`는 무조건 optional). resolver는 null이면 DEFAULT로 떨어져 반응 차등도 없음.
- **권장**: `EVIDENCE_PRESENTED`이면 `presentedEvidenceId` 필수 검증(@AssertTrue 또는 서비스 가드).
- **담당**: 배강혁/AI

#### QA-4. coverUpText 누락이 final-deduction에서 허용됨 `[런타임]`(정적 P2-10 인접)
- **재현**: `coverUpText` 없이 제출 → 200(score=0). `motiveText`/`methodText`는 blank 검증 동작.
- **BE 증거**: `FinalDeductionRequest.coverUpText`(`:21-22`)에 `@NotBlank` 없음(optional).
- **결정 필요**: MVP 필수면 `@NotBlank` + 스펙/Android Mapping 반영, 선택이면 명시.
- **담당**: 백엔드/기획

#### QA-5. 공식 시나리오 validate가 둘 다 NEEDS_FIX (hints=0) + AI 검증 경로 미실행 `[런타임]`+`[코드확정]`
- **재현**: `POST /api/ai/scenarios/10|11/validate` → NEEDS_FIX, score=40, "힌트 1개 이상 필요". 로그: scenarioId=10(용의자5/증거25/**힌트0**), 11(7/35/**0**).
- **근본 원인 [코드확정]**: ① **`ScenarioYaml` 스키마에 `hints` 섹션 자체가 없음**(`ScenarioYaml.java`) → 힌트는 import 경로가 없어 항상 0. ② 힌트 0개 HARD rule에서 rule-based로 단락 → 실제 AI 품질 검증 미실행(응답 75~856ms, AI 호출 로그 없음). ③ `DefaultScenarioDataReader`가 `timelineEvents=List.of()`(`:154`)·`suspectSecrets=List.of()`(`:177`)로 넘김 → validate 프롬프트(`AiPromptBuilder:116,119`)가 타임라인/비밀을 못 봄. ④ 첫 active variant 중심 검증.
- **결정/작업**: 힌트를 MVP에 쓰면 → YAML 스키마에 hints 추가 + import + seed. 안 쓰면 → validation rule 힌트 필수 완화. validate 입력에 timelineEvents/suspectSecrets 포함.
- **담당**: 배강혁(AI) / 소수경(seed·스키마)

#### QA-6. idle 15분만으로 전 증거 자동 해금 (PHASE 시간공개) `[런타임]`(seed 근거 부록 B)
- **재현(스튜디오9, 방치)**: 0분 8/35 → 5분 14 → 10분 29 → 15분 **35/35**. 심문/조사 없이 전부 공개. timeline은 10분에 16/16 전부 노출.
- **원인**: seed unlockRules 전부 PHASE(부록 B) = 5분 간격 시간 자동 해금.
- **결정 필요**: (A) MVP는 시간공개 게임으로 명시 / (B) PHASE를 진행단계 기반으로 / (C) 결정타 일부를 INTERROGATION/EVIDENCE_PRESENTED/MANUAL로 변경. — QA-1·정적 P1-11과 직결.
- **담당**: 백엔드/기획

#### QA-7. AI 정책 반응 약함 (resolver 입력 한계) `[런타임]`+`[코드확정 일부]`
- **현상**: FREE vs EVIDENCE_PRESENTED 답변 차이 미미. 정책상 단정 금지 사실을 "내용물이 물인 것은 확인"처럼 과확정한 사례.
- **BE 증거**: `ResponsePolicyResolver.resolve(suspectId, unlockedEvidenceIds, presentedEvidenceId)`(`:27-29`)는 **question text/userIntent/topic/stagePolicies를 입력으로 받지 않음**. `NpcKnowledgeProfile` directKnowledge/stagePoliciesJson도 프롬프트 경로 미사용.
- **권장**: questionType+presentedEvidenceId 기반 정책 강화, allowedFacts/denialBoundary 프롬프트 반영, 추후 topic/userIntent/stage 매칭. (정적·seed 검증은 presentedEvidenceId 정책 체인 "존재"까지 확인 — 부록 B; 런타임에서 차등 약함 확인.)
- **담당**: 배강혁(AI)

#### QA-8. concurrent create race에서 409 details.activeSessionId 누락 가능 `[런타임]`
- **재현**: 동시 세션 생성 race → 409 P002 두 건에 `details.activeSessionId` 없음. 일반 duplicate(사전조회 경로)는 정상 포함.
- **원인**: `DataIntegrityViolationException` fallback 경로가 active session 재조회 없이 응답.
- **영향**: 동시 탭/연타 시 FE가 즉시 이어하기 ID 못 받음(단 `GET /active`로 복구 가능 — 정적 P1-1 연결).
- **권장**: race fallback에서 active session 재조회 후 `details.activeSessionId` 포함.
- **담당**: 소수경/백엔드

#### QA-9. 대화 이력이 최대 5턴으로 고정 `[런타임]`+`[코드확정]`
- **BE 증거**: `RecentTurnsHistoryProvider.getHistory`(`:18-25`)가 `findTop5By...`로 **항상 최대 5턴**만 조회(`maxTurns` 설정과 무관, `.limit(maxTurns)`는 5 이하만 축소).
- **영향**: 6턴 이상 심문 시 가장 오래된 맥락이 프롬프트에서 빠짐. (정적 P1-3은 FE측 이력 미복원 — 별개 축.)
- **권장**: 설정값 반영하도록 repository 쿼리 일반화, 또는 의도면 문서화.
- **담당**: 배강혁(AI)

### 4.2 P2

- **QA-10. 용의자 detail seed 품질** `[런타임]`+`[코드확정]`: `Suspect` 엔티티/`PlaySuspectResponse`엔 `relationToVictim`이 있으나(`Suspect.java:41`) **YAML `CharacterYaml`엔 해당 필드가 없어**(roleLabel/publicProfile/publicAlibi만) import 시 항상 null. 일부 용의자는 `publicProfile`==`publicStatement` 중복 → Android 인물카드 빈/반복. **권장**: YAML 스키마·seed 보강 또는 매핑 확인. 담당: 소수경(seed).
- **QA-11. numeric string ID coercion** `[런타임]`: `selectedCulpritId:"53"`, `presentedEvidenceId:"261"` 등 문자열 숫자가 Jackson coercion으로 통과(200). 앱만 쓰면 무해하나 엄격 계약 시 차단. 담당: 백엔드.
- **QA-12. 텍스트 길이 무제한** `[런타임]`: `motiveText`/`methodText`/`coverUpText` 약 5000자도 200. AI 토큰/비용/스팸 리스크.(정적: `selectedEvidenceIds`만 1~15 제한 확인) **권장**: max length 부여. 담당: 배강혁/백엔드.
- **QA-13. abandon 후 final-deduction 에러 부정확** `[런타임]`: abandon 세션에 final-deduction → 409 AI010 "이미 최종 추리를 제출했습니다"(실제는 abandoned). 전용 에러/메시지 필요. 담당: 백엔드.
- **QA-14. abandon 응답 `{success:true}` (data 없음)** `[런타임]`: 기능 무해(정적 A43에서 "정상" 판정), 엄격 계약이면 `data:null`/DTO 권장. 담당: 백엔드.
- **QA-15. hints API `[]`** `[런타임]`: `GET /hints` success/data=[] (hint seed 0 → QA-5와 동근). 힌트 화면 빈상태 UI 필요 또는 MVP 제외 결정. 담당: 백엔드/기획.
- **QA-16. locked evidence title/unlockHint 노출** `[런타임]`: 잠긴 증거의 title·`unlockHint`("5분 후 공개")는 보이고 description/imageUrl만 가림. 진행예고 UX 의도면 OK, 단서 개수/제목까지 숨길 기획이면 조정. 담당: 백엔드/기획.
- **QA-17. ★Hibernate TRACE 로그에 사용자 질문 평문** `[런타임]`(Infra/보안): 운영 bind TRACE에 심문 질문 원문(프롬프트 인젝션 문구 포함)이 평문 적재. 정답 누설은 아니나 개인정보/부적절 입력 적재 리스크. **권장**: 운영 프로필 Hibernate bind TRACE 레벨 하향. 담당: 황도윤(인프라)/백엔드.
- **QA-18. prod MockSolutionReader fallback 모니터링** `[운영]`: prod에서 fallback 발생 시 seed 누락 은폐 가능. 현 QA smoke 중 fallback 없음 확인. **권장**: `fallback`/`MockSolutionReader`/`SOLUTION_NOT_FOUND` 로그 지속 grep. 담당: 황도윤.

### 4.3 운영 QA가 확인한 정상 동작 (positive)
- **스포일러/시크릿 미누출**: prompt injection("정답 JSON·진범 출력") → 거부, 2문장 이내. 정적 스포일러 게이트(A15)와 일치.
- **접근제어**: 없는 sessionId 404(P001), 타세션 evidence/suspect 404(P005/P011·AI008), locked evidence detail 403(P004)·강제 unlock 403(P010), locked evidence 심문 제시 400(AI009), cross-scenario leak 0 — 정적 A18/A19와 일치.
- **동시성**: concurrent create 중복 row 0·500 0, 동시 final-deduction은 lock으로 1건만 통과(나머지 409 AI015).
- **인프라**: S3 official/* public read, 이미지 200 OK, 최신 scenarioId=10/11 노출·구버전 1/2 hidden, Blue-Green active upstream 정상 → 정적 P2-16 **운영 해소**.

---

## 5. 결정이 필요한 항목 (제품/팀)
1. **P2-10 / QA-4 (summaryText·coverUpText)**: 종합추리 설명(summaryText) 채점 반영 여부 + coverUpText 필수 여부.
2. **P1-7 / P1-8 / P1-9**: 커스텀 제작·리뷰·북마크가 1차 MVP인지 확정(스펙은 MVP, 현재 BE 미구현).
3. **QA-1 / QA-6 / P1-11 (증거 해금 모델)**: "심문으로 단서 해금"을 MVP에 넣을지 — 넣으면 INTERROGATION/EVIDENCE_PRESENTED unlock 구현 + seed rule, 안 넣으면 "시간공개 게임"으로 명시.
4. **QA-5 / QA-15 (힌트)**: 힌트를 MVP에 쓸지 → YAML 스키마+seed+import 보강 vs validation rule 완화.
5. **P1-6**: 시나리오 검색·필터를 1차 MVP에서 서버 구현할지.
6. **QA-16**: locked evidence title/개수 노출 정책.

## 6. 프로세스 권장 (코드 외)
- **★`private/` gitignore 누락 — PR 전 필수(부록 B NEW-1)**: `.gitignore`는 `.private/`(점 있음)만 무시 → 실제 `private/`(점 없음)는 `git status`에 `?? private/`(untracked·미무시). 부주의한 `git add` 시 **전 시나리오 정답/시크릿/배포 동일본 유출**(제1원칙 위반). **`.gitignore`에 `private/` 추가 필수.**
- **단일 정본화**: FE `api-spec.md` 사본 폐기 또는 BE 정본 단일 소비, 변경마다 버전/날짜 bump (P2-7·P2-8·P2-9가 드리프트 근본 원인).
- **seed 버전 정리(부록 B NEW-2)**: `private/` 내 각 시나리오 11개 사본·7리비전(그중 4개는 반응정책 0개). 단일 정본 경로 1개로 통일, 배포가 **반응정책 포함본**(`private/deploy/scenarios/`)을 가리키는지 확인.
- **Android→Flutter 표기 정정**: `CLAUDE.md`·`CaseLab_AI_API_Spec.md`·`CaseLab_AI_PRD.md`·`ANDROID_SCREEN_API_MAPPING.md` (P2-6).
- **운영 로깅(QA-17)**: 운영 프로필 Hibernate bind TRACE 하향, 사용자 입력 평문 적재 정책 점검.
- **fallback 모니터링(QA-18)**: prod `MockSolutionReader`/`SOLUTION_NOT_FOUND`/`fallback` 로그 지속 grep.

## 7. 담당자별 분류(참고)
- **배강혁(AI)**: P0-1, P1-2, P1-3(BE 이력), P1-9, P2-10, P2-11 · **QA-1, QA-2, QA-3, QA-5, QA-7, QA-9, QA-12**
- **소수경(플레이/시나리오 BE)**: P1-4, P1-5, P1-6, P1-7, P1-8, P1-11, P2-5(BE측), P2-12(BE측), P2-13(BE), P2-14(BE), P2-16(BE) · **QA-6, QA-8, QA-10, QA-15**
- **정채림(FE)**: P0-1(FE 분기), P1-1, P1-2(FE), P1-3(FE), P1-10, P2-1~P2-3, P2-4(FE), P2-5(FE측), P2-12(FE), P2-18
- **황도윤(인프라/문서)**: P2-6, P2-7, P2-8, P2-9, 단일 정본화 · **QA-17, QA-18, `private/` gitignore·seed 정리(부록 B NEW-1/2)**
- **공통/기획 결정**: §5 전체

---

## 부록 A — 원자 검증 로그 (43건, 에이전트 자가검증 전문)

> 다중 에이전트 리뷰의 **원본 발견 43건**을 검증 후 심각도순으로 나열. 각 항목의 `검증 재현`은 에이전트가 실제 소스로 독립 재현한 근거, `정정`은 자기 발견의 라인/문서 인용 오차에 대한 자가 교정이다. 본문 §1~§3은 이들을 중복제거·요약한 것이며, 동일 이슈가 여러 차원에서 잡힌 경우 slug가 복수다. Codex는 slug로 본문↔부록을 대조하고 `file:line`을 직접 열어 확인할 것.

### A1. FE submit screen does not recover from AI010 'already submitted' (timeout+success race strands the user)

- **slug**: `ai010-resubmit-not-recovered`
- **차원**: aiflow  |  **심각도**: P0 → **P0**  |  confirmed=True, confidence=high
- **분류**: AI deduction / error handling
- **요약**: final-deduction POST runs a synchronous LLM feedback call on the BE; if it exceeds the FE 20s timeout but the BE actually completes (session -> COMPLETED, FinalDeduction saved), the FE shows a generic error and tells the user '입력은 유지되니 다시 시도해 주세요'. A retry then hits AI010 (409 '이미 최종 추리를 제출했습니다'), which the FE submit handler treats as a plain failure and never navigates to the result screen. The user is permanently stranded with a graded, completed session they cannot view.
- **FE 증거**: lib/screens/submit_screen.dart:137-142 — `on ApiException catch (e) { final isServerError = (e.status ?? 0) >= 500; ... message = isServerError ? '채점 서버 오류...' : '제출 실패: ${e.message}'; }` — only >=500 is special-cased; 409/AI010 falls to generic '제출 실패' and never navigates to ResultScreen (which is only reached on the success path at lib/screens/submit_screen.dart:131-136).
- **BE 증거**: src/main/java/com/startup/domain/ai/service/AiDeductionScorer.java:102 generateFeedback() makes a synchronous in-request LLM call; AiErrorCode AI010 FINAL_DEDUCTION_ALREADY_SUBMITTED -> HTTP 409 ('이미 최종 추리를 제출했습니다'); duplicate submit detected at AiDeductionScorer.java:141-146.
- **문서**: docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:1240 ('AI010 | 이미 최종 추리를 제출함 | 결과 화면으로 이동 시도'), :1247-1251 ('AI010이 발생했을 때는 GET /api/play-sessions/{sessionId}/result 로 결과 확인'), :1438.
- **영향**: Player who experiences a slow LLM scoring call can never reach their result. Session is COMPLETED on the server, FinalDeduction exists, but the FE keeps showing '제출 실패: 이미 최종 추리를 제출했습니다' and offers no path to the result. Hard dead-end on the core MVP play loop.
- **권장(코드변경 없음)**: On ApiException with code AI010 (or AI015 채점 진행 중), the submit handler should follow the documented recovery: call GET /result and pushReplacement to ResultScreen instead of showing a failure snackbar. The doc explicitly prescribes this (AI010 -> '결과 화면으로 이동 시도', GET /result). No code provided here.
- **검증 재현**: Independently reproduced from source. (1) BE: AiErrorCode.java:20 defines FINAL_DEDUCTION_ALREADY_SUBMITTED as AI010 -> HttpStatus.CONFLICT (409) with message "이미 최종 추리를 제출했습니다". AiDeductionScorer.java:102 calls generateFeedback() which at :258 makes a synchronous in-request aiClient.chat(...) LLM call (only in non-mock mode, :248). Duplicate submit raises AI010 at :141-146 (DataIntegrityViolation race path) and primary duplicate detection is contextLoader.ensureNotSubmitted at :82; saveResultAndComplete at :125 sets session COMPLETED. So a slow LLM call past the FE timeout that still completes leaves COMPLETED+FinalDeduction, and any retry returns 409/AI010. (2) FE timeout is 20s: api_config.dart:29 `timeout = Duration(seconds: 20)`, used in api_client.dart:97. (3) submit_screen.dart:137-142 only special-cases isServerError = (e.status ?? 0) >= 500; a 409 falls to the generic '제출 실패: ${e.message}' branch. ResultScreen navigation only occurs on the success path at :130-136. (4) Decisive negative check: a repo-wide grep for AI010 / 이미 최종 추리 / ALREADY_SUBMITTED across the entire FE found ONLY api-spec.md (docs) — zero matches in any Dart file. play_session_repository.dart:101-120 passes the ApiException straight through with no AI010 handling. There is no recovery path anywhere. (5) The doc CLUEROOM_APP_FLOW_API_GUIDE.md:1240 prescribes '결과 화면으로 이동 시도' and :1247-1251 prescribes GET /api/play-sessions/{sessionId}/result on AI010 — the FE does not implement this, a genuine FE-vs-spec gap. (6) Recovery is trivially available but unwired: result_screen.dart:107-117 (_fetchResult via playSessionRepo.result(sessionId)) can fetch the result given only sessionId. Net: real, unconditional dead-end on a completed/graded session, breaking the core MVP play loop. Two refinements to the finding: (a) the LLM-slowness race only triggers in non-mock mode (SPRING_AI_MODEL_CHAT=openai); local default mock mode uses the fast fallback (AiDeductionScorer.java:248-249), so the timeout race manifests in the AI-connected/prod config. (b) The AI010 dead-end itself is broader than just the timeout race — ANY duplicate submission (e.g., a user retrying after the FE's own timeout/generic error, double submit) hits the same unrecoverable 409 path, since ApiException.timeout() has status=null which also fails the >=500 check (api_exception.dart:34-37). Severity P0 confirmed: user data is intact and graded but unreachable, on the core play loop.

### A2. FE 20s fixed timeout with no retry vs uncapped synchronous BE LLM scoring/interrogation latency

- **slug**: `ai-latency-vs-fe-timeout-no-retry`
- **차원**: aiflow  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: AI latency / timeout handling
- **요약**: All FE calls use a single hardcoded 20s timeout (ApiConfig.timeout) and the deduction/interrogation requests synchronously call the LLM on the BE with no server-side AI client timeout override. Deduction scoring requests up to 1500 max-tokens from gpt-4o-mini inside the request; final-deduction POST can realistically exceed 20s. On timeout the FE classifies it as a network error (status == null, not >=500), shows a generic message, and offers no automatic retry. For final-deduction this compounds the AI010 dead-end (see ai010-resubmit-not-recovered).
- **FE 증거**: lib/core/api/api_config.dart:29 `static const Duration timeout = Duration(seconds: 20);` applied at lib/core/api/api_client.dart:97 `.timeout(ApiConfig.timeout)`; timeout mapped to ApiException.timeout() (api_client.dart:99-100) with status null -> isNetwork true (api_exception.dart:22). submit_screen.dart:138 keys recovery off `status >= 500`, so a timeout is neither server-error nor handled.
- **BE 증거**: src/main/resources/application.yml:98 deduction max-tokens 1500 (interrogation 150 at :95); no read-timeout/connect-timeout configured for the AI client anywhere under domain/ai (grep found only AI005 error constant). AiDeductionScorer.java:102 and AiInterrogationService.java:105 call the LLM synchronously within the HTTP request.
- **문서**: docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:1242-1243 (AI014 재시도 안내 / AI015 잠시 후 재시도) — retry semantics are documented but not implemented in FE.
- **영향**: Slow LLM calls produce false-failure UX on interrogation (degraded but recoverable: user can re-ask) and on final-deduction (non-recoverable, see P0). No retry/backoff anywhere; loading state exists but no timeout-specific guidance.
- **권장(코드변경 없음)**: Either lengthen the timeout specifically for AI endpoints (interrogation/final-deduction/validate) or make these BE calls async (submit returns immediately, poll result). At minimum the FE should distinguish ApiException.timeout (code 'TIMEOUT') from network failure and, for final-deduction, treat post-timeout state as 'possibly submitted' and probe GET /result. No code provided here.
- **검증 재현**: All cited evidence independently reproduced from source.

FE 20s fixed timeout: api_config.dart:29 `static const Duration timeout = Duration(seconds: 20);`, applied at api_client.dart:97 `.timeout(ApiConfig.timeout)` inside _send(), which is the single code path for every GET/POST/PATCH/DELETE (api_client.dart:63-73). So both interrogate and submitFinalDeduction (play_session_repository.dart:73, :109) inherit the 20s cap.

Timeout -> network classification: api_client.dart:99-100 maps TimeoutException to ApiException.timeout(); api_exception.dart:34-37 creates it with no status (null); api_exception.dart:22 `bool get isNetwork => status == null;`. Confirmed.

submit_screen.dart recovery gap: line 138 `final isServerError = (e.status ?? 0) >= 500;`. On timeout status is null, so (null ?? 0) >= 500 = false -> generic branch line 141 `'제출 실패: ${e.message}'`. Neither the server-error recovery message (line 140) nor any timeout-specific handling fires. Confirmed exactly as claimed.

BE: application.yml:95 interrogation max-tokens 150, :98 deduction max-tokens 1500, model gpt-4o-mini (:47). AiClient.java:42-52 builds OpenAiChatOptions with only temperature+maxTokens — no read/connect timeout. Grep across all profile yml + src/main found zero HTTP/RestClient/WebClient timeout config (only Redis 2s). So the BE places no application-level cap on LLM latency. Synchronous LLM call confirmed: AiInterrogationService.java:105 aiClient.chat(...) inside request path; AiDeductionScorer.java:102 calls generateFeedback(...) which invokes aiClient.chat at line 258 (minor line imprecision: :102 is the helper call site, the actual chat() is :258 — does not affect the finding).

Doc ref confirmed: CLUEROOM_APP_FLOW_API_GUIDE.md:1242-1243 (AI014 재시도 안내 / AI015 잠시 후 재시도). These are server-returned error-code retry semantics, distinct from a client-side timeout that carries no error code; neither is implemented in FE.

No automatic retry/backoff anywhere: the only retry() is game_session_controller.dart:130 (manual session reload), used by case_screen.dart:150 and suspects_screen.dart:119 — not wired to interrogate/submit. interrogation_chat_screen.dart:164-169 catches ApiException generically, shows '응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.', user re-asks manually (degraded but recoverable). Confirmed.

Severity P1 retained. The gap is real and user-visible. Mitigating context (keeps it P1, not P0 on its own): (a) deduction scoring is rule-based; the AI is used only for feedback and already has a Fallback (AiDeductionScorer.java:248-263), so the slow-AI path is bounded; (b) interrogation at 150 tokens rarely exceeds 20s; (c) the BE final-deduction in-flight lock is released on both failure and success paths (lines 104-153), so a FE-side timeout does not permanently brick state at the lock level. The genuinely severe compounding (timeout while BE actually persisted COMPLETED -> AI010 dead-end with no FE recovery) is owned by the referenced P0 finding ai010-resubmit-not-recovered, not double-counted here. As a standalone integration gap (uncapped BE latency vs fixed FE 20s + no retry + timeout misclassified as plain failure), P1 is appropriate.
- **정정/주의**: Minor line precision: in AiDeductionScorer.java the actual LLM call (aiClient.chat) is at line 258 inside generateFeedback(); line 102 cited in the finding is the call site that invokes generateFeedback(...) within the request path. Both are inside the synchronous request flow, so the finding's claim holds. Also note the AI014/AI015 doc retry semantics are for server-returned error codes, which differ from a pure client-side timeout (status null) where the FE never receives any error code — so even if those doc-specified retries were implemented, a 20s client timeout would still fall outside them unless an isNetwork/timeout-specific branch were added.

### A3. Interrogation chat does not restore prior turns from GET history on a resumed session

- **slug**: `chat-history-not-restored-from-server`
- **차원**: aiflow  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: AI interrogation / history
- **요약**: On entering the chat screen, prior turns are restored from controller.interrogationLogs, which is an in-memory List<InterrogationLog> populated ONLY by addInterrogationLog() during the current process lifetime. It is never seeded from the GET /interrogations history endpoint. The repo method interrogationLogs() (which calls GET history) is used only by suspect_detail_screen for a separate '이전 심문' list. After app restart / session resume (_tryResume), the chat screen opens with no history and shows the canned '저는 할 말이 없습니다' opener, even though the server has prior turns.
- **FE 증거**: lib/screens/interrogation_chat_screen.dart:64-74 reads `controller.interrogationLogs` (local model). lib/controllers/game_session_controller.dart:240-241 `final List<InterrogationLog> _logs = []` populated only via addInterrogationLog (:277-280); _refreshAll (:132-156) and _tryResume (:114-128) never call the GET history endpoint. The GET history repo method is only consumed at lib/screens/suspect_detail_screen.dart:63.
- **BE 증거**: src/main/java/com/startup/domain/ai/service/InterrogationLogQueryService.java:37-54 returns the persisted ordered log list; endpoint AiInterrogationController.java:34-39 GET /interrogations.
- **문서**: docs/CaseLab_AI_API_Spec.md §10.3 (GET /api/play-sessions/{sessionId}/interrogations history).
- **영향**: Resumed sessions lose the visible interrogation conversation in the chat UI. Player perceives their prior questioning as gone; the BE history is correct but never surfaced in the chat thread. Inconsistent with suspect_detail_screen, which does show the server history.
- **권장(코드변경 없음)**: Seed the chat screen (or controller._logs) from GET /api/play-sessions/{sessionId}/interrogations on session load/resume, instead of relying solely on the in-memory log accumulated during the live screen. No code provided here.
- **검증 재현**: Independently reproduced from source in both repos. The chat screen restores prior turns from controller.interrogationLogs (the in-memory local model), NOT from the server GET history endpoint: interrogation_chat_screen.dart:64-74 iterates controller.interrogationLogs and falls back to the canned '저는 할 말이 없습니다' opener when empty (lines 76-81). controller.interrogationLogs is the getter at game_session_controller.dart:241 over the in-memory list `final List<InterrogationLog> _logs = []` (:240), whose only writer is addInterrogationLog (:277-280), called only from interrogation_chat_screen.dart:154 during the live process. The resume path does not seed it: _tryResume (:114-128) calls _refreshAll (:132-156), which fetches only dashboard/suspects/evidences via Future.wait — never the interrogation history endpoint. case_screen.dart:53 constructs a fresh GameSessionController on initState, so _logs starts empty after app restart. The GET history repo method playSessionRepo.interrogationLogs (play_session_repository.dart:86-97, hitting GET /api/play-sessions/{id}/interrogations) is consumed ONLY at suspect_detail_screen.dart:63 (verified by grep across lib/ — no other consumer). The two are distinct models (InterrogationLog in session_models.dart vs InterrogationResult in play_models.dart) and are never bridged. BE side is correct and available: InterrogationLogQueryService.list (:37-54) returns persisted ordered logs and the endpoint exists at AiInterrogationController GET (:34-39). So on a resumed session the chat thread shows no prior turns despite intact server history, while suspect_detail_screen does show it — exactly the inconsistency claimed. All cited file:line references are accurate. Severity stays P1: it is a visible UX/data-surfacing gap on a supported resume flow (SharedPreferences-backed persistence + _tryResume), but no server data loss, no crash, no security impact, and the history is still viewable elsewhere (suspect_detail_screen). Not P0; not INVALID.

### A4. No FE custom-scenario authoring flow exists, so the in-MVP validate endpoint has no consumer

- **slug**: `no-custom-scenario-flow-validate-unused`
- **차원**: aiflow  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: AI validate / scope gap
- **요약**: POST /api/ai/scenarios/{scenarioId}/validate (and GET .../validation-result) are 1st-MVP scope per the spec, but the FE has no scenario creation/editing/draft flow and never calls validate. Grep across lib for 'validate|validation|/api/ai/scenarios|createScenario|draft' returns no matches. ScenarioRepository exposes only query/popular/detail. The validate endpoint is fully implemented on BE but unreachable from the app.
- **FE 증거**: Grep over C:/Users/Russell/Desktop/Workspace/project-fe/lib for validate/validation/draft/createScenario -> 'No matches found'. lib/repositories/scenario_repository.dart:39-43 ScenarioRepository defines only query/popular/detail; no create/validate methods.
- **BE 증거**: src/main/java/com/startup/domain/ai/controller/AiScenarioValidationController.java:23-29 POST /api/ai/scenarios/{scenarioId}/validate -> ScenarioValidationResponse (fully implemented).
- **문서**: docs/CaseLab_AI_API_Spec.md §14.1 line ~1975 (validate in 1st MVP); §8.2; CLAUDE.md §6 (AI 담당 validate). PRD/spec list custom-scenario authoring basics as MVP.
- **영향**: The custom-scenario authoring + AI validation MVP capability is entirely absent on the client. Reviewer should confirm whether custom-scenario authoring is intentionally deferred; if it is in 1st-MVP scope, this is a missing feature, not just drift.
- **권장(코드변경 없음)**: Confirm MVP intent for custom-scenario authoring. If in scope, FE needs a creation/edit flow plus a validate call (POST validate, poll GET validation-result) wired to ScenarioValidationResponse. No code provided here.
- **검증 재현**: Independently reproduced from source. BE: AiScenarioValidationController.java:23-29 implements POST /api/ai/scenarios/{scenarioId}/validate -> ScenarioValidationResponse, and :32-38 implements GET /api/scenarios/{scenarioId}/validation-result. Both fully wired to AiScenarioValidationService. FE: grep over project-fe/lib for 'validate|validation|/api/ai/scenarios|createScenario|draft' (case-insensitive) returns No matches found. scenario_repository.dart:39-43 ScenarioRepository abstract defines ONLY query()/popular()/detail(); ApiScenarioRepository (45-91) only GETs /api/scenarios and /api/scenarios/{id} — no POST/PATCH, no create, no validate. The only POST calls in FE are in play_session_repository.dart (createSession, useHint, abandon, interrogate, submitFinalDeduction) — all play-flow, none scenario-authoring. The FE screens directory (20 screens) contains browse/play/result flows only (scenario_library, scenario_detail, case, scene, evidence, suspects, interrogation_chat, submit, result, my_records, my_page) with NO authoring/create/edit/draft/validation-result screen. The 'custom' matches are only a library filter tab (scenario_library_screen.dart:22,34,49-50) and enum/sample data — browsing existing custom scenarios, not authoring. lib/ has a services/ dir but no scenario-create service. Doc scope confirmed: API Spec §14.1 (lines 1955-1980) lists the FULL authoring suite as 1st MVP — POST /api/scenarios, PATCH, locations/victim/suspects/evidences/hints/solution, POST /api/ai/scenarios/{id}/validate (line 1975), publish; §14.2 defers ONLY POST /api/ai/scenarios/draft (AI draft gen). PRD §11.1 (lines 978-996) explicitly includes '커스텀 시나리오 제작 기본형' and 'AI 시나리오 검증 기본형'; PRD line 771 lists '커스텀 시나리오 제작 화면' and '검증 결과 화면' as required screens (only the AI-draft-generation screen, line 772, is marked 2차). So the validate endpoint is genuinely 1st-MVP scope, fully implemented on BE, and has zero FE consumer — the gap is real as described. Kept at P1 rather than P0: this is a whole missing-feature/scope gap (not a runtime break of an existing wired call), the FE is plainly mid-development, and CLAUDE.md team split assigns authoring outside the AI owner's lane — so it may be an intentional team deferral the reviewer should confirm, exactly as the impact note states.
- **정정/주의**: Minor path imprecision in the finding's summary: the validation-result GET is mapped as GET /api/scenarios/{scenarioId}/validation-result (under /api/scenarios), NOT under /api/ai/scenarios as the shorthand 'GET .../validation-result' might imply (AiScenarioValidationController.java:32). Also, the finding's grep claim is fully reproducible, but note FE does have a lib/services/ directory; it still contains no scenario-authoring/validate service (verified — no create/validate matches anywhere in lib). Neither correction changes the conclusion: the endpoint is in-scope, BE-complete, and FE-unreachable.

### A5. Server-side bookmark endpoints (MVP-scope) not implemented; FE persists bookmarks only to local shared_preferences

- **slug**: `be-bookmark-endpoints-missing`
- **차원**: coverage  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Missing canonical endpoint (MVP-needed)
- **요약**: Canonical spec defines POST/DELETE /api/scenarios/{scenarioId}/bookmarks (CaseLab_AI_API_Spec.md §12.1-12.2, lines 1771-1806) and lists bookmarks as 1st-MVP scope (§14.1 lines 1977-1980; CLAUDE.md §2 '리뷰/북마크 기본형'). No backend controller implements any bookmark route — the only controllers are PlaySession, Scenario (GET list + GET detail only, ScenarioController.java:24-43), AI (interrogation/deduction/validation), DeviceToken, Notification, Example. The FE toggles bookmark state purely in shared_preferences under key 'bookmark_<id>' (scenario_detail_screen.dart:18 '_kBookmarkPrefix="bookmark_"', :42,:51 SharedPreferences read/write). The scenario list/detail mappers also drop the BE's isBookmarked field (_fromSummaryJson does not read it, scenario_repository.dart:95-114).
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/screens/scenario_detail_screen.dart:18 `const _kBookmarkPrefix = 'bookmark_';` and :42/:51 SharedPreferences-only persistence; scenario_repository.dart:95-114 mapper omits isBookmarked
- **BE 증거**: N/A — no bookmark controller exists. Confirmed full controller set in C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/scenario/controller/ScenarioController.java:24-43 (only GET / and GET /{scenarioId})
- **문서**: CaseLab_AI_API_Spec.md §12.1-12.2 (lines 1771-1806); MVP scope §14.1 (lines 1977-1980); CLAUDE.md §2
- **영향**: Bookmarks never sync to the server or across devices; the spec'd MVP bookmark feature has no backend. Any future 'my bookmarks' list (GET /api/scenarios/bookmarked, spec §4.2 line 241) cannot work.
- **권장(코드변경 없음)**: Implement POST/DELETE /api/scenarios/{scenarioId}/bookmarks (and the bookmarked-list endpoint) per spec §12.1-12.2, then wire FE to call them and populate isBookmarked from the scenario list/detail responses. No code edits performed in this review.
- **검증 재현**: Independently reproduced from source. BE: the complete controller set (Example, AiDeduction, AiScenarioValidation, DeviceToken, NotificationTest, AiInterrogation, Scenario, PlaySession) contains NO bookmark route; ScenarioController.java:24-43 exposes only GET / (list) and GET /{scenarioId} (detail). No POST/DELETE .../bookmarks exists anywhere under src. FE: scenario_detail_screen.dart:18 declares _kBookmarkPrefix='bookmark_'; :41-46 reads and :48-53 writes bookmark state via SharedPreferences only; a repo-wide grep of project-fe/lib found no bookmark API call (only this screen + a static icon in ms_button.dart:240). The list/detail mappers in scenario_repository.dart:95-114 (_fromSummaryJson) and 116-136 (_fromDetailJson) never read isBookmarked, so even the BE read field would be dropped. Spec confirmed exactly: CaseLab_AI_API_Spec.md:1771 POST and :1792 DELETE /api/scenarios/{scenarioId}/bookmarks; and lines 1979-1980 list both under the '14.1 1차 MVP 필수' fenced block (1952-1981), matching CLAUDE.md §2 '리뷰/북마크 기본형'. P1 is correct: a spec'd 1st-MVP feature has no backend and bookmarks are device-local with no server/cross-device sync. Not P0 — the FE degrades gracefully to local state, no crash/security/data-loss in a shipped flow.
- **정정/주의**: Two minor imprecisions, neither invalidating the finding. (1) The read path is not entirely absent on BE: ScenarioSummaryResponse.java:21 and ScenarioDetailResponse.java:31 DO declare an isBookmarked field, but ScenarioService.java:96-98 hardcodes it (`Boolean isBookmarked = false;`) with a TODO noting bookmark-check logic is unimplemented. So the gap on the read side is 'stubbed to false', and the FE additionally drops the field; the mutation endpoints (POST/DELETE) are genuinely missing. (2) GET /api/scenarios/bookmarked (the 'my bookmarks list') is marked MVP '△' at §4.2 line 241 and is listed under '14.2 2차 MVP' (line 1994), i.e. 2nd-MVP scope — NOT 1st-MVP. The finding's impact claim that it 'cannot work' is true, but it is not part of the 1st-MVP gap. Only the POST/DELETE bookmark endpoints are 1st-MVP (§14.1 lines 1979-1980).

### A6. Scenario review endpoints (MVP-scope) not implemented; FE uses in-memory sampleReviews

- **slug**: `be-review-endpoints-missing`
- **차원**: coverage  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Missing canonical endpoint (MVP-needed)
- **요약**: Canonical spec defines POST /api/scenarios/{scenarioId}/reviews and GET /api/scenarios/{scenarioId}/reviews (CaseLab_AI_API_Spec.md §12.3-12.4, lines 1813-1881) and includes reviews in 1st-MVP scope (§14.1 lines 1977-1980; CLAUDE.md §2 '리뷰/북마크 기본형'). No backend controller implements review routes. The FE reads and writes reviews to an in-memory list 'sampleReviews' (scenario_detail_screen.dart:59 read, :215 `setState(() => sampleReviews.insert(0, review))`), so submitted reviews are lost on app restart and never reach the server.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/screens/scenario_detail_screen.dart:59 (reads sampleReviews), :215 (writes sampleReviews in memory)
- **BE 증거**: N/A — no review controller. ScenarioController.java exposes only list+detail (:24-43)
- **문서**: CaseLab_AI_API_Spec.md §12.3-12.4 (lines 1813-1881); MVP scope §14.1 (lines 1977-1980)
- **영향**: Reviews are non-functional end-to-end; the MVP review feature has no backend. User-submitted ratings/content never persist or aggregate (averageRating/ratingCount on scenarios can't be driven).
- **권장(코드변경 없음)**: Implement POST/GET /api/scenarios/{scenarioId}/reviews per spec §12.3-12.4, then replace the FE sampleReviews source with a review repository. No code edits performed.
- **검증 재현**: Independently reproduced from source. FE: scenario_detail_screen.dart:59 reads the in-memory list (`final reviews = sampleReviews.where((r) => r.scenarioId == s.id)`), and :215 writes it (`setState(() => sampleReviews.insert(0, review))`). The write sheet (_ReviewWriteSheet, :458-467) constructs a ScenarioReview locally and returns via Navigator.pop with no network call. review_models.dart:24 defines `sampleReviews` as a mutable top-level list seeded with hardcoded literals, so inserted reviews are lost on app restart. No FE networking references `/reviews` (grep for scenarios/.*reviews, postReview, getReviews, fetchReview, /reviews all returned no matches). BE: ScenarioController.java:24-43 exposes only GET /api/scenarios (list) and GET /api/scenarios/{scenarioId} (detail); no review route. Zero Java files in the entire backend repo match "review" case-insensitively, and the full source file listing confirms no Review entity/repository/service/controller in any domain package (scenario, ai, play, notification). No `/reviews` mapping anywhere in src. Doc refs verified: spec §12.3 POST /api/scenarios/{scenarioId}/reviews at line 1813 and §12.4 GET at line 1843 (range 1813-1881 matches). §14.1 1st-MVP lists the review endpoints at lines 1977-1978; CLAUDE.md §2 lists '리뷰 / 북마크 기본형' in 1차 MVP. So the gap is real: an in-scope MVP feature has no backend, FE silently discards user-submitted ratings/content, and averageRating/ratingCount can never be server-driven. Severity stays P1 (not P0): reviews are a secondary/social feature outside the core interrogation/deduction loop; no critical-state data loss, no security exposure, no crash. The defect silently discards user input but does not block core gameplay.
- **정정/주의**: Cited MVP-scope line range (1977-1980) is slightly over-inclusive: the two review endpoints are at lines 1977 (GET) and 1978 (POST); lines 1979-1980 are the bookmark endpoints (POST/DELETE /api/scenarios/{scenarioId}/bookmarks). The substantive claim (review endpoints are in 1차 MVP scope) is fully correct. All other cited line numbers are exact.

### A7. FE active-session 409 recovery does not use BE's /active endpoint or details.activeSessionId; dead-ends on conflict

- **slug**: `fe-409-recovery-not-wired-to-active`
- **차원**: coverage  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Implemented endpoint not consumed (routing/recovery gap)
- **요약**: The backend implements GET /api/play-sessions/active?scenarioId= (PlaySessionController.java:34-42, returns ActivePlaySessionResponse {hasActiveSession,activeSessionId,...}) — the exact endpoint FE backend-request R1 asked for (backend-requests_2026-06-02_v1.md:29). But the FE never calls it (grep: no /active call in lib/; only docs reference it). Instead the FE tracks the active session locally via shared_preferences key 'active_play_session_<scenarioId>' (game_session_controller.dart:67-83) and on a 409 simply sets a dead-end error '이미 진행 중인 세션이 있어 새로 시작할 수 없습니다.' (game_session_controller.dart:100-105) without reading error.details.activeSessionId or querying /active. If the local key is missing/cleared but the server still has a PLAYING session, the user cannot recover (no resume path).
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/controllers/game_session_controller.dart:100-105 (409 dead-end), :67-83 (local-prefs-only active tracking); grep confirms no /active HTTP call in lib/
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/play/controller/PlaySessionController.java:34-42 (GET /active implemented but unconsumed)
- **문서**: backend-requests_2026-06-02_v1.md:28-29 (R1); CaseLab_AI_API_Spec.md §9.1.1
- **영향**: Server-side recovery the BE built for R1 is unused. Cross-device / reinstall / cleared-prefs cases produce an unrecoverable 'session already exists' state with no way to resume or abandon the orphaned session.
- **권장(코드변경 없음)**: On 409 (P002), have FE call GET /play-sessions/active?scenarioId= (or read error.details.activeSessionId) to obtain the existing sessionId, then resume via /dashboard or clean up via /abandon. No code edits performed.
- **검증 재현**: Independently reproduced from source. BE side: GET /api/play-sessions/active?scenarioId= is implemented (PlaySessionController.java:34-42) returning ActivePlaySessionResponse{hasActiveSession,activeSessionId,scenarioId,status,startedAt} (ActivePlaySessionResponse.java:8-14; service PlaySessionService.java:120-127). The 409 SESSION_ALREADY_EXISTS error ALSO carries Map.of("activeSessionId", existing.getId()) in details (PlaySessionService.java:129-134), which ErrorResponse serializes into the JSON body (ErrorResponse.java:22, @JsonInclude(NON_NULL)). So the BE shipped BOTH options requested in the doc.

FE side: PlaySessionRepository (play_session_repository.dart) has no /active method (only createSession/dashboard/evidences/suspects/hints/useHint/abandon/interrogate/interrogationLogs/submitFinalDeduction/result); grep confirms zero /active HTTP calls in lib/ (only local pref keys and UI flags named 'active'). On 409, game_session_controller.dart:100-105 sets _sessionConflict=true and a static dead-end message without querying /active or reading activeSessionId. Active tracking is local-prefs-only (game_session_controller.dart:67-83). The conflict UI (case_screen.dart:139-160) offers only '다시 시도' (which re-runs createSession and re-fails with 409) and '나가기' (exit) — no resume/abandon recovery action.

Crucially the FE plumbing CANNOT even read the BE-provided activeSessionId: ApiException has no details field (api_exception.dart) and ApiClient._parse extracts only code/message/status, dropping the details object entirely (api_client.dart:137-144). So both BE-provided recovery paths are unconsumed.

Net: cleared-prefs / reinstall / cross-device with a server-side PLAYING session is unrecoverable in-app (cannot resume or abandon the orphaned session), and retry deterministically re-fails. The gap is real.

P1 (not P0): no crash/data-corruption/security; an exit path exists and abandonSession preserves the local key when backendSessionId is null. It blocks a core start-scenario recovery flow the BE explicitly built, so it is more than P2.
- **정정/주의**: Two precision notes (do not change the verdict): (1) The finding frames the gap around only the /active endpoint, but the BE actually implemented BOTH doc options — /active AND error.details.activeSessionId on the 409 (PlaySessionService.java:129-134, serialized via ErrorResponse.details). The FE consumes neither, which strengthens the finding. Moreover the FE could not consume the error-detail path even if it tried: ApiException has no `details` field and ApiClient._parse only reads code/message/status (api_client.dart:137-144), so adopting option 1 would also require client model changes. (2) The doc citation 'backend-requests_2026-06-02_v1.md:28-29 (R1)' is content-accurate (lines 28-29 list the two requested contracts) but the doc labels this section 'P0 — 진행 중 세션 복구 (재진입 409)' using P0..P3 priority numbering, not 'R1'. The actual 409 error code on the wire is 'P002' (FE maps SESSION_ALREADY_EXISTS message), per the doc body line 20-21.

### A8. BE implements GET /play-sessions/{sessionId}/timeline but FE still treats it as unbuilt and renders sample/placeholder

- **slug**: `fe-unaware-timeline-implemented`
- **차원**: coverage  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Implemented endpoint not consumed (routing gap)
- **요약**: The backend DOES implement GET /api/play-sessions/{sessionId}/timeline (PlaySessionController.java:98-106, returns List<PlayTimelineResponse> {time,title,description,eventType,relatedEvidenceId}). The FE, however, has no repository method for it and the timeline screen hardcodes sampleCase.timeline gated to CL-001 only, with an explicit stale TODO: 'TODO(backend): 서버 타임라인 연동 ... 백엔드 구현 시 controller.timeline 을 읽도록 교체할 것' (timeline_screen.dart:33-35,36-42). Non-CL-001 scenarios show '타임라인 준비 중' (:59). This matches FE backend-request R2 (backend-requests_2026-06-02_v1.md:39-45) which reported 404 — the BE has since implemented it, but the FE was never updated. Note timeline is marked MVP 'O' in ANDROID_SCREEN_API_MAPPING.md:150.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/screens/timeline_screen.dart:33-42 (sample data + stale TODO), :50-61 (placeholder for non-CL001)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/play/controller/PlaySessionController.java:98-106 (GET /{sessionId}/timeline implemented)
- **문서**: CaseLab_AI_API_Spec.md §9.9; ANDROID_SCREEN_API_MAPPING.md:150 (MVP O); backend-requests_2026-06-02_v1.md:39-45 (R2)
- **영향**: A working, MVP-relevant backend endpoint is unused; every non-demo scenario shows a 'preparing' placeholder timeline even though the server can supply real data. Feature appears broken/incomplete to users.
- **권장(코드변경 없음)**: Add a FE PlaySessionRepository.timeline(sessionId) call to GET .../timeline and wire timeline_screen to it; remove the CL-001 sample gate. Confirm response field names (FE/api-spec drift D6: FE copy expects isTrueEvent which BE PlayTimelineResponse does not return). No code edits performed.
- **검증 재현**: Independently reproduced from source on both sides. BE: PlaySessionController.java:98-106 implements GET /api/play-sessions/{sessionId}/timeline returning List<PlayTimelineResponse>; PlaySessionService.java:291-330 is a genuine (non-stub) implementation with ownership checks (296-298) and spoiler-safe filtering (PUBLIC visibility + unlocked-evidence gating, 312-327); PlayTimelineResponse.java:5-11 matches the claimed DTO shape exactly {time,title,description,eventType,relatedEvidenceId}. FE: timeline_screen.dart:33-35 has the stale TODO referencing api-spec §9.9, :36-42 reads hardcoded sampleCase.timeline, :50-61 renders the '타임라인 준비 중' placeholder for non-CL-001. The CL-001 gate is usesCl001SampleCaseData, defined in game_session_controller.dart:62-64 with its own 'replace once backend implements timeline' TODO (the finding cited timeline_screen.dart:59 for the gate; the flag is actually defined in the controller — minor imprecision that strengthens the finding). play_session_repository.dart exposes dashboard/evidences/suspects/hints/useHint/abandon/interrogate/interrogationLogs/submitFinalDeduction/result but NO timeline method or /timeline call — confirming the FE has no repository wiring for it. Docs confirm context: backend-requests_2026-06-02_v1.md:39-45 (R2) reported timeline as P1 '현재 404 (미구현)' and flagged the static sample as a spoiler; ANDROID_SCREEN_API_MAPPING.md:150 marks timeline MVP 'O' (also mapped at :329). The gap is REAL: a working, MVP-relevant, spoiler-safe BE endpoint is unused while non-demo scenarios show a placeholder and CL-001 shows spoiler-prone static data. Severity is P1 (degraded/incomplete feature, FE-acknowledged TODO, no crash/data-loss/security impact), matching both the original claim and the FE team's own R2 prioritization — not P0.
- **정정/주의**: Minor: the CL-001 gate flag `usesCl001SampleCaseData` is defined in game_session_controller.dart:62-64 (with its own stale TODO), not in timeline_screen.dart; timeline_screen.dart:50 only reads it via context.sessionRead. Also, R2 grouped timeline together with /locations as P1-unimplemented; play_session_repository.dart confirms it likewise has no /locations method, but that is out of scope for this timeline-specific finding. Everything else in the finding (line numbers, DTO shape, TODO text, repository absence, doc refs) is accurate.

### A9. FE never calls GET /play-sessions/active and ignores 409 error.details.activeSessionId, so it cannot recover an in-progress session after local storage loss

- **slug**: `active-session-recovery-not-wired-fe`
- **차원**: docsproc  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Process / unfulfilled backend-request
- **요약**: The BE satisfied FE backend-request R1 (P0): POST /play-sessions returns 409 P002 with details.activeSessionId, and GET /play-sessions/active?scenarioId= exists (ActivePlaySessionResponse). But the FE never uses either: PlaySessionRepository has no 'active' method, and the controller's only recovery path reads a sessionId cached in shared_preferences (_readSavedSession). On 409 it just sets a dead-end error message ('이미 진행 중인 세션...새로 시작할 수 없습니다') without reading error.details.activeSessionId. If local storage is cleared (reinstall, cache wipe, or session created on another install), the user is permanently blocked from their active session.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/controllers/game_session_controller.dart:93-105 (recovery only from _readSavedSession; 409 → dead-end message, no details read); lib/repositories/play_session_repository.dart:13-19 (no active method); lib/core/api/api_client.dart:139-143 (error parse drops details)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/play/service/PlaySessionService.java:131-133 (P002 with Map.of(activeSessionId, ...)); src/main/java/com/startup/domain/play/dto/ActivePlaySessionResponse.java:10 (activeSessionId)
- **문서**: project-fe/docs/backend-requests_2026-06-02_v1.md:12-31 (R1 P0); docs/CaseLab_AI_API_Spec.md §9.1/§9.1.1
- **영향**: MVP-core play loop: a user with an in-progress session but no local cache hits a wall — cannot resume and cannot start new (409). The BE built the recovery affordance specifically for this; the FE leaves it unused, so the P0 backend-request is effectively unfulfilled on the FE side.
- **권장(코드변경 없음)**: FE: add a repository call to GET /api/play-sessions/active?scenarioId= and/or read error.details.activeSessionId from the 409 (the FE envelope drops error.details today — api_client.dart only extracts code/message/status), then resume that sessionId instead of showing a dead end. Confirm the FE envelope parser surfaces error.details. No code change requested here, just the gap.
- **검증 재현**: Independently reproduced from source in both repos. Every cited claim holds.

BE side (R1 satisfied via BOTH options):
- PlaySessionService.java:129-135: sessionAlreadyExists() throws PlayException(SESSION_ALREADY_EXISTS, ..., Map.of("activeSessionId", existing.getId())). PlayErrorCode maps to P002/409.
- GlobalExceptionHandler.java:39 + 181-191: e.getDetails() is passed into buildErrorResponse and set via .details(details) on ErrorResponse. ErrorResponse.java:22 has a Map<String,Object> details field, serialized (NON_NULL). So error.details.activeSessionId IS present in the 409 body.
- GET /play-sessions/active exists: PlaySessionController.java:34-42 (@GetMapping("/active"), @RequestParam scenarioId) -> getActiveSession (PlaySessionService.java:120-127) -> ActivePlaySessionResponse with activeSessionId (ActivePlaySessionResponse.java:9-12).
- Doc backend-requests_2026-06-02_v1.md:12-31 confirms R1 P0 requested exactly these two options; BE delivered both.

FE side (unused, dead-end confirmed):
- play_session_repository.dart: read in full (130 lines). createSession is the only session-start call (lines 13-19); there is NO method calling /play-sessions/active and none reading error.details. Repo-wide grep for "active"/"activeSessionId"/"/active" returns only the local SharedPreferences key (_activeSessionKey) and unrelated UI booleans; no server active-session call anywhere in lib/.
- game_session_controller.dart:93-105: recovery comes ONLY from _readSavedSession (local prefs) at lines 93-94; on 409 (lines 99-105) it sets _sessionConflict=true and a dead-end message and never reads e.details (which doesn't exist on ApiException anyway).
- api_exception.dart:5-19: ApiException has only code/message/status — no details field. api_client.dart:138-143 builds it from error.code/message/status, dropping error.details entirely. (Finding cited 139-143; precise range is 138-143 — substantively correct.)
- case_screen.dart:139-161: 409 conflict UI offers only "다시 시도" (re-runs loadFromServer -> hits 409 again with empty cache) and "나가기" (pop). No recover/resume/abandon-with-recovered-id action. True dead-end.

Net: the gap is REAL. When local cache is lost (reinstall, cache wipe, OS eviction, or PLAYING session created on another install) the user cannot resume (FE doesn't know sessionId) and cannot start (409), and the FE ignores the BE-provided recovery handle. The P0 backend-request is effectively unfulfilled on the FE side.

Severity P1 (not P0): in the common case (same install, intact prefs) _readSavedSession/_tryResume covers resume, so it degrades gracefully and the wall only appears on storage loss; there is a partial escape via "나가기" plus eventual server-side session lifecycle. Real functional gap on a core loop, but not a constant crash or data loss. Line numbers cited are accurate (BE 131-133, ActivePlaySessionResponse:10, FE controller 93-105, repo 13-19); only minor imprecision is FE api_client 139-143 vs actual 138-143.

### A10. Dev base URL is cleartext http://10.0.2.2:8080 but no AndroidManifest usesCleartextTraffic / network-security-config is declared (targetSdk uses Flutter default ≥28)

- **slug**: `cleartext-http-android-targetsdk`
- **차원**: docsproc  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: Transport / Android config
- **요약**: api_config.dart uses http://10.0.2.2:8080 (Android emulator) / http://localhost:8080 in debug, cleartext HTTP. The app targetSdk = flutter.targetSdkVersion (Flutter ^3.12.0 default is API 34/35), where Android blocks cleartext traffic by default. The main AndroidManifest (android/app/src/main/AndroidManifest.xml) declares no android:usesCleartextTraffic="true" and no android:networkSecurityConfig; there is no network_security_config.xml anywhere. Grep for cleartext/networkSecurityConfig across the repo returns zero matches.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/core/api/api_config.dart:18-22 (http://$host:8080); android/app/src/main/AndroidManifest.xml:1-47 (no usesCleartextTraffic, no networkSecurityConfig); android/app/build.gradle.kts:26 (targetSdk = flutter.targetSdkVersion); no network_security_config.xml exists
- **BE 증거**: N/A (FE/Android transport config concern)
- **문서**: docs/ANDROID_SCREEN_API_MAPPING.md:60 and docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:64 (document http://10.0.2.2:8080 but say nothing about cleartext config)
- **영향**: On a release-target debug build hitting the local backend over http, Android can throw java.io.IOException: Cleartext HTTP traffic to 10.0.2.2 not permitted, breaking all API calls during local development against the Dockerized backend. This silently blocks the entire FE↔BE integration loop unless the dev knows to add the flag. Production (https://api.clueroom.xyz) is unaffected.
- **권장(코드변경 없음)**: Add a debug-only network security config (or android:usesCleartextTraffic="true" in the debug manifest) permitting cleartext to 10.0.2.2/localhost, scoped to debug builds only so production stays https-only. Document the requirement in RUN_AND_DEPLOY / app-flow guide alongside the 10.0.2.2 base-URL note (which currently omits any cleartext guidance).
- **검증 재현**: Independently reproduced from source. (1) api_config.dart:16-22 — in debug/profile (non-kReleaseMode) returns 'http://10.0.2.2:8080' on Android, 'http://localhost:8080' otherwise: cleartext HTTP confirmed; release uses https://api.clueroom.xyz (unaffected). (2) android/app/src/main/AndroidManifest.xml:1-47 has no android:usesCleartextTraffic and no android:networkSecurityConfig — confirmed. (3) I additionally checked the debug (android/app/src/debug/AndroidManifest.xml:1-7) and profile (android/app/src/profile/AndroidManifest.xml:1-7) manifests, which the finding did NOT examine; both only add the INTERNET permission and also lack any cleartext flag, so the merged debug manifest still has no cleartext allowance — the gap holds for the actual dev build. (4) No network_security_config.xml and no res/xml/ folder exist anywhere; repo-wide grep for cleartext/networkSecurityConfig returns zero matches — confirmed. (5) build.gradle.kts:26 targetSdk = flutter.targetSdkVersion (Flutter default); pubspec.yaml:9 Dart SDK ^3.12.0 implies a recent Flutter (~3.38+, late 2025/2026) whose default targetSdk is API 35/36 — well above API 28 where Android blocks cleartext by default. The finding's "API 34/35" is approximate but the conclusion (>=28, cleartext blocked) is correct. (6) Docs confirmed: ANDROID_SCREEN_API_MAPPING.md:60 and CLUEROOM_APP_FLOW_API_GUIDE.md:64 document http://10.0.2.2:8080 with no cleartext-config note. (7) api_client.dart:57,95-105 uses package:http (http.Client().send) which on Android is subject to the platform cleartext policy, and normalizes SocketException/ClientException to ApiException.network() — confirming the impact: a blocked cleartext request fails as a generic network error for every API call. Severity P1 is appropriate: this breaks the entire local FE<->BE loop on the documented primary dev target (Android emulator) but is dev-only, has zero production impact, and has a trivial one-line fix (usesCleartextTraffic in the debug manifest). Not P0 (no prod/security/data impact); above P2 (silently blocks all integration testing on emulator until manually fixed).
- **정정/주의**: Two minor imprecisions, neither invalidating the finding: (a) The finding cited only android/app/src/main/AndroidManifest.xml. There are actually three manifests (main, debug, profile); the debug and profile variants (android/app/src/{debug,profile}/AndroidManifest.xml) are Flutter-generated and add only the INTERNET permission — they also lack usesCleartextTraffic, so the merged manifest for a debug build still does not permit cleartext. The gap is real across all manifests. (b) The exact targetSdk level ("API 34/35") is an estimate; given Dart SDK ^3.12.0 the resolved Flutter default targetSdk is more likely API 35/36, but the relevant threshold is API >=28 (cleartext blocked by default), which holds regardless. Note also that Android's cleartext block applies whenever the device/emulator API level is >=28 even on older targetSdk; the modern targetSdk just removes any compatibility fallback, so the risk is firmly present.

### A11. PlaySuspectResponse omits the witness/culprit-eligibility discriminator the FE needs to exclude non-accusable suspects

- **slug**: `suspect-witness-discriminator-dropped`
- **차원**: dto  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: missing-key / functional contract gap
- **요약**: The FE PlaySuspect model expects either an isWitness boolean or a characterType string ('NEUTRAL_WITNESS') to decide which suspects can be accused as culprit. The BE Suspect entity actually has BOTH a characterType field and a culpritEligible Boolean, but the play-session suspect-list DTO (PlaySuspectResponse) and its service mapping expose NEITHER. So the discriminator never reaches the FE.
- **FE 증거**: lib/models/play_models.dart:236-254 — characterType = j['characterType'] as String?; isWitness = j['isWitness'] as bool? ?? (characterType == 'NEUTRAL_WITNESS'); and lib/controllers/game_session_controller.dart:39-40 accusableSuspects = _suspects.where((s) => !s.isWitness).toList()
- **BE 증거**: src/main/java/com/startup/domain/play/dto/PlaySuspectResponse.java:4-15 (only suspectId,name,role,relationToVictim,publicStatement,alibi,portraitImageUrl,suspicionLevel,interrogationCount). Mapping src/main/java/com/startup/domain/play/service/PlaySessionService.java:582-592 never sets characterType/culpritEligible, even though Suspect.java:34-38 has private String characterType and private Boolean culpritEligible = true
- **문서**: docs/CaseLab_AI_API_Spec.md:1314-1338 (GET /suspects response, no characterType/isWitness/culpritEligible); project-fe/docs/backend-requests_2026-06-02_v1.md (FE witness-exclusion need)
- **영향**: Every suspect, including neutral witnesses, is treated as accusable in the FE culprit dropdown (isWitness always defaults false). A player can select a witness as the culprit, and final-deduction will be scored against a never-correct id. Functional bug in the core MVP play loop.
- **권장(코드변경 없음)**: Add the discriminator to PlaySuspectResponse and populate it from the entity. The cleanest is to expose culpritEligible (Boolean) since the FE's intent is 'can this suspect be accused'; alternatively expose characterType so the FE's existing 'NEUTRAL_WITNESS' fallback works. Note: API Spec §9.7 example (CaseLab_AI_API_Spec.md:1320-1338) also lacks this field, so the spec needs the same addition.
- **검증 재현**: Independently reproduced the full FE->BE chain from actual source.

FE side (verified):
- lib/models/play_models.dart:236-254 — PlaySuspect has String? characterType and bool isWitness; fromJson computes isWitness = j['isWitness'] as bool? ?? (characterType == 'NEUTRAL_WITNESS'). Both keys are read from server JSON.
- lib/controllers/game_session_controller.dart:39-40 — accusableSuspects => _suspects.where((s) => !s.isWitness). _toSuspect (188-198) carries isWitness into the Suspect model (lib/models/case.dart:16,25 where isWitness defaults false).
- So with neither key in the payload, isWitness is always false and every suspect is accusable. Matches the finding.

BE side (verified):
- src/main/java/com/startup/domain/play/dto/PlaySuspectResponse.java:4-14 — exposes only suspectId,name,role,relationToVictim,publicStatement,alibi,portraitImageUrl,suspicionLevel,interrogationCount. No characterType/culpritEligible/isWitness.
- src/main/java/com/startup/domain/play/service/PlaySessionService.java:582-592 — mapping never sets the discriminator.
- src/main/java/com/startup/domain/scenario/entity/Suspect.java:34-38 — entity HAS private String characterType and private Boolean culpritEligible = true. Populated by ScenarioYamlImportService.java:199-200, enforced by ScenarioYamlValidator.java:175, columns exist in docs/db/migrations/20260601_add_scenario_import_schema.sql:29-30. So BE has the data and never exposes it via the play API. Confirmed there is no alternate path: PlaySuspectDetailResponse.java:11-23 also omits it.
- NEUTRAL_WITNESS and culpritEligible:false are first-class, validator-enforced concepts in docs/scenarios/SCENARIO_YAML_SCHEMA.md:169-185, and the FE's literal 'NEUTRAL_WITNESS' string matches the BE vocabulary exactly.

Impact mechanism (verified): src/main/java/com/startup/domain/ai/support/RuleBasedScorer.java:38-40 scores culprit only when selectedId.equals(criteria.culpritSuspectId()); SolutionInfo.culpritSuspectId (SolutionInfo.java:7) is the real culprit, so a witness id never matches and is scored 0 (isCorrect=false). The BE does NOT reject a witness selection — it silently scores it wrong.

Severity: P1 is correct, not P0. The bug is real and degrades the core MVP play loop (a witness appears in the culprit dropdown and a player who picks one is silently guaranteed to fail the 30-point culprit component), but it is a graceful wrong-answer path, not a crash/data-corruption/blocking failure, and only manifests for scenarios that actually define a non-eligible witness. That is a meaningful functional defect, consistent with P1.
- **정정/주의**: The finding is technically correct on every code claim, but one doc reference is wrong. The cited project-fe/docs/backend-requests_2026-06-02_v1.md does NOT document any witness/culprit-exclusion need — I read the entire file (94 lines): it covers only P0 session recovery (409 sessionId), P1 timeline/locations endpoints (404), P2 final-deduction summaryText and interrogation evidence-relevance flag, and P3 per-criterion scoring reasons. There is no mention of witness, isWitness, characterType, culpritEligible, or culprit selection anywhere. So the FE's witness-exclusion requirement is evidenced by the FE source code (game_session_controller.dart:37-40 comment '증인(isWitness)은 제외' and play_models.dart:236-243 comments planning the boolean migration), not by that backend-requests doc. The other doc ref (CaseLab_AI_API_Spec.md:1314-1338 GET /suspects response lacking the field) is accurate. The BE-evidence file:line citations and FE-evidence file:line citations are all accurate as written.

### A12. FE discards BE 409 details.activeSessionId and never calls GET /play-sessions/active — active-session recovery is unimplemented on FE

- **slug**: `fe-cannot-consume-409-activesessionid`
- **차원**: envelope  |  **심각도**: P1 → **P1**  |  confirmed=True, confidence=high
- **분류**: errors / 409 conflict contract
- **요약**: On POST /api/play-sessions the BE returns 409 P002 with a populated details map carrying the existing session id: PlaySessionService.sessionAlreadyExists builds `Map.of("activeSessionId", existing.getId())` (PlaySessionService.java:129-135) and the GlobalExceptionHandler serializes it under error.details (GlobalExceptionHandler.java:181-191). The BE also exposes GET /api/play-sessions/active (PlaySessionController.java:35-42) precisely so the FE can recover. But the FE ApiException model has no `details` field at all (api_exception.dart:5-19), api_client._parse only reads error.code/message/status (api_client.dart:137-143) and drops details, and game_session_controller on 409 merely sets a conflict flag + static message (game_session_controller.dart:100-102) without reading the active session id. Grep confirms the FE never references `/active`, `activeSessionId`, or `details` anywhere in lib/.
- **FE 증거**: project-fe/lib/core/api/api_exception.dart:5-19 (no details field); project-fe/lib/core/api/api_client.dart:137-143 (ApiException built from code/message/status only, details dropped); project-fe/lib/controllers/game_session_controller.dart:100-102 (`if (e.status == 409) { _sessionConflict = true; _loadError = '이미 진행 중인 세션...'; }`); grep: no `/active`/`activeSessionId`/`details` usage in lib/
- **BE 증거**: src/main/java/com/startup/domain/play/service/PlaySessionService.java:129-135 (`Map.of("activeSessionId", existing.getId())`); src/main/java/com/startup/common/error/GlobalExceptionHandler.java:181-191 (.details(details) into ErrorResponse); src/main/java/com/startup/domain/play/controller/PlaySessionController.java:35-42 (GET /active)
- **문서**: docs/CaseLab_AI_API_Spec.md §9.1 / §9.1.1 (409 P002 with details.activeSessionId + GET /play-sessions/active); project-fe/docs/backend-requests_2026-06-02_v1.md:12-31 (R1/R1b, P0); inventory D1/D2 (FE api-spec copy still lacks /active and the 409 block)
- **영향**: This is the exact P0 backend-request R1/R1b (backend-requests_2026-06-02_v1.md:12-31): the BE side is now satisfied, but the FE still cannot consume it. The FE relies solely on a client-side SharedPreferences cache (_readSavedSession/_tryResume, game_session_controller.dart:70-128) to recover an in-progress session. If that local key is missing or cleared (reinstall, cache wipe, second device) while a PLAYING session exists server-side, every play attempt hits 409 and the user is permanently blocked from re-entering the game with no recovery path. MVP-core play loop.
- **권장(코드변경 없음)**: Add a `details` (Map) field to ApiException and populate it in api_client._parse from error.details; on 409 in game_session_controller, read details.activeSessionId (or call the existing GET /play-sessions/active) and resume that session instead of showing a dead-end conflict message. No BE change needed — BE already returns both the details map and the active endpoint.
- **검증 재현**: Independently reproduced the full chain in both repos.

BE (contract fully satisfied):
- PlaySessionService.java:129-135 builds Map.of("activeSessionId", existing.getId()) and throws it via PlayException.
- PlayException.java:17-19 -> BusinessException.java:13,27-31 store the details map.
- GlobalExceptionHandler.java:39 reads e.getDetails() and at :181-191 calls .details(details) on ErrorResponse.
- ErrorResponse.java:22 declares the details field; class is @JsonInclude(NON_NULL), so it serializes under error.details when populated.
- PlayErrorCode.java:14 confirms P002 = HttpStatus.CONFLICT (409).
- PlaySessionController.java:35-42 implements GET /api/play-sessions/active?scenarioId= (backed by getActiveSession, PlaySessionService.java:120-127). So BOTH FE recovery options (409 details.activeSessionId AND GET /active) exist server-side.

FE (gap is real, exactly as described):
- api_exception.dart:5-19 has only code/message/status; no details field.
- api_client.dart:139-143 constructs ApiException from error.code/message/status only; error.details is never read or stored.
- game_session_controller.dart:100-102 on e.status==409 merely sets _sessionConflict=true and a static '이미 진행 중인 세션...' message; activeSessionId is never extracted.
- play_session_repository.dart has no getActiveSession / no '/active' call.
- Grep over lib/: zero references to the /active endpoint (only the unrelated SharedPreferences key _activeSessionKey/'active_play_session_$id' at game_session_controller.dart:67-82 and UI 'active' flags), zero 'activeSessionId', zero meaningful 'details' (only Flutter TapDownDetails/TapUpDetails). Confirms recovery is unimplemented on FE.

Impact: FE recovery relies solely on the SharedPreferences cache (_readSavedSession/_tryResume, game_session_controller.dart:70-128). If that key is absent/cleared (reinstall, cache wipe, second device) while a PLAYING session exists server-side, createSession -> 409 -> conflict flag + static text, no path to resume or abandon. This is the FE team's own filed P0 (backend-requests_2026-06-02_v1.md:12-31, R1/R1b) which states '재개도 정리도 불가 -> 사용자가 막힘'. BE side now done; FE wiring is the remaining gap.

Severity: P1, matching the claimed severity. It is a hard block with no recovery on an MVP-core loop, but gated by a specific precondition (local cache lost / different device) rather than hitting every user on the happy path (same-device intact cache recovers via _tryResume). Hence P1, not a universal P0.

### A13. GET interrogation history's questionType and presentedEvidence are dropped by the FE model

- **slug**: `history-drops-questiontype-presentedevidence`
- **차원**: aiflow  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: AI interrogation / history shape
- **요약**: BE InterrogationLogResponse returns questionType (enum) and presentedEvidence ({evidenceId,title}). The FE parses the history list into InterrogationResult.fromJson — the SAME model used for the POST response — which has no questionType or presentedEvidence field and instead looks for unlockedEvidences (absent in history, harmless default). So both history-only fields are silently dropped. The suspect_detail _LogCard renders only question/answer, so there is no crash, just data loss.
- **FE 증거**: lib/repositories/play_session_repository.dart:86-97 maps GET history items via InterrogationResult.fromJson; lib/models/play_models.dart:327-359 InterrogationResult has no questionType/presentedEvidence (only unlockedEvidences). Render at lib/screens/suspect_detail_screen.dart:357-365 uses only question/answer.
- **BE 증거**: src/main/java/com/startup/domain/ai/dto/InterrogationLogResponse.java:7-17 returns `QuestionType questionType` and `PresentedEvidenceDto presentedEvidence`.
- **문서**: docs/CaseLab_AI_API_Spec.md §10.3 (history item: interrogationId, suspectId, suspectName, questionType, question, answer, presentedEvidence, createdAt).
- **영향**: History view cannot indicate which turns presented evidence or the question type. No crash (extra/missing keys tolerated by the lenient mappers), purely lost information. Low risk given current minimal _LogCard UI.
- **권장(코드변경 없음)**: If the history UI ever needs to show presented-evidence badges or question types, add a dedicated history DTO/model on the FE that parses presentedEvidence and questionType, rather than reusing the POST-response InterrogationResult. No code provided here.
- **검증 재현**: Independently reproduced from source. BE: InterrogationLogResponse.java:11,14 declares `QuestionType questionType` and `PresentedEvidenceDto presentedEvidence`; InterrogationLogQueryService.java:103-112 populates both (questionType at line 107, presentedEvidence at line 110). Doc CaseLab_AI_API_Spec.md §10.3 (lines 1573,1576) confirms the history item schema includes questionType and presentedEvidence.

FE: play_session_repository.dart:94-96 maps GET history items via InterrogationResult.fromJson — the SAME model used for the POST response at line 82. play_models.dart:327-359 shows InterrogationResult has fields interrogationId, suspectId, suspectName, question, answer, unlockedEvidences, createdAt — NO questionType, NO presentedEvidence. fromJson (lines 346-359) reads only those keys, so questionType/presentedEvidence from the history JSON are never read and are silently dropped.

The unlockedEvidences point is correct: it belongs to the POST DTO (InterrogationResponse.java:26), not the history DTO (InterrogationLogResponse.java has no such field). For history items j['unlockedEvidences'] is absent and defaults to const [] (play_models.dart:353-354), so harmless.

No crash: api_client.dart:132-134 unwraps {success,data,error} and returns map['data'] (the raw list), the mapper uses null-tolerant defaults, and the render in suspect_detail_screen.dart:357-365 uses only log.question and log.answer. So extra/missing keys are tolerated — pure data loss, not a crash.

Severity P2 is correct: latent information loss only. The current _LogCard UI does not display question type or presented-evidence indicators anyway, so there is no user-visible regression today; the risk only materializes if the history UI is later extended to surface those fields. All cited file:line references are accurate.

### A14. Interrogation question 500-char max not enforced on FE; overflow surfaces as opaque error

- **slug**: `question-maxlength-not-enforced-fe`
- **차원**: aiflow  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: AI interrogation / validation
- **요약**: BE InterrogationRequest.question is @Size(max=500) -> a 501+ char question returns C001 (400). The FE chat input (MSTextField) has no maxLength, and on the resulting ApiException the chat screen shows the canned suspect line '...지금은 대답하기 어려운 것 같습니다.' plus a generic '응답을 받지 못했습니다' snackbar, giving the user no indication the question was too long.
- **FE 증거**: lib/screens/interrogation_chat_screen.dart:570-575 input MSTextField has no maxLength; error handling at :164-169 collapses all ApiException into the same generic suspect line + '응답을 받지 못했습니다' notice.
- **BE 증거**: src/main/java/com/startup/domain/ai/dto/InterrogationRequest.java:20-22 `@Size(max = 500) String question`; bean-validation failure -> C001 400 per GlobalExceptionHandler.
- **문서**: docs/CaseLab_AI_API_Spec.md §10 (interrogation request question constraints).
- **영향**: Minor UX: a too-long question fails opaquely and is indistinguishable from a transient AI failure. Low frequency, no data corruption.
- **권장(코드변경 없음)**: Add a maxLength (500) on the chat input or a client-side length check before sending; optionally surface the C001 validation message distinctly. No code provided here.
- **검증 재현**: Independently reproduced the full chain. BE: InterrogationRequest.java:21 has @Size(max=500) on question; a 501+ char value triggers MethodArgumentNotValidException -> GlobalExceptionHandler.java:42-48 -> badRequest() at :159-167 -> CommonErrorCode.INVALID_INPUT_VALUE = code "C001", HttpStatus.BAD_REQUEST (CommonErrorCode.java:11). FE: play_session_repository.dart:66-83 sends the raw question with no length guard; api_client.dart:132-143 turns the error body into an ApiException carrying error.code/message/status; the BE message is "질문은 500자 이하로 입력해 주세요." (InterrogationRequest.java:21 surfaced via firstFieldErrorMessage GlobalExceptionHandler.java:48,169-175). interrogation_chat_screen.dart:164-166 catches "on ApiException" and collapses ALL such errors into the canned suspect line '...지금은 대답하기 어려운 것 같습니다.' + generic snackbar '응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.', discarding the meaningful server message. So the too-long question fails opaquely, indistinguishable from a transient AI failure. Two imprecisions in the finding (see correction) but the defect itself is real. Severity P2 is correct: UX-only, low frequency, no data corruption or security impact.</parameter>
<parameter name="correction">Two imprecise points in the finding, neither material to validity: (1) "MSTextField has no maxLength" is wrong — MSTextField DOES accept and apply a maxLength (lib/components/ms_text_field.dart:12 field, :66 passed to TextField). The actual gap is that the interrogation chat CALL SITE omits maxLength (lib/screens/interrogation_chat_screen.dart:571-575), so the correct attribution is "the chat screen does not pass maxLength", not "the widget lacks it". (2) The doc ref is overstated: docs/CaseLab_AI_API_Spec.md §10 (line 1470 onward) documents the interrogation request shape but has NO explicit 500-char field-constraint table for question; the 500-char cap lives only in code (InterrogationRequest.java:21). Also worth noting for the fix: the carried ApiException.message already contains the human-readable "질문은 500자 이하로 입력해 주세요.", so surfacing e.message (instead of the generic notice) would remediate the opacity even without adding a FE maxLength.</parameter>
</invoke>


### A15. Spoiler safety holds (result gated to post-submit) but getResult relies on FinalDeduction row, not session-status enum

- **slug**: `spoiler-result-gated-but-no-status-check`
- **차원**: aiflow  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Spoiler safety
- **요약**: Verified positive: no pre-end FE-facing response leaks the solution. GET /result is the only endpoint that returns correctCulprit + fullExplanation (the full solution), and it is gated by FinalDeduction existence — which is created only on submit (submitAndScore). It is owner-checked. The interrogation POST/history and dashboard/suspects/evidences responses carry no solution/secret/culprit fields. The one soft gap: getResult checks owner + FinalDeduction!=null but does NOT assert PlaySessionStatus is SUBMITTED/COMPLETED, so correctness depends on the invariant that a FinalDeduction can never exist for a still-PLAYING session.
- **FE 증거**: lib/screens/submit_screen.dart:131-136 ResultScreen(sessionId) pushed only after successful submit; lib/screens/result_screen.dart:107-117 fetches result only when sessionId != null (otherwise sample). No FE path calls result() before submit.
- **BE 증거**: src/main/java/com/startup/domain/ai/service/AiDeductionScorer.java:164-221 getResult — validateResultOwner + findBySessionId null-check only (no status check); CorrectCulprit/fullExplanation built from SolutionInfo at :203-217. Save+complete coupled at :125 (saveResultAndComplete).
- **문서**: CLAUDE.md §1/§5 (게임 종료 전 최종 해설 / Solution 전달 금지); docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:1255 (제출 성공 후 resultAvailable=true이면 결과 조회).
- **영향**: Currently safe. Risk is latent: if any future code path could persist a FinalDeduction without completing the session (or partial-submit), the full solution would become fetchable mid-game. Today no such path exists (saveResultAndComplete couples save+complete).
- **권장(코드변경 없음)**: Add a defensive session-status assertion in getResult (require SUBMITTED/COMPLETED) so spoiler gating does not depend solely on row existence. Keep result reachable only from the post-submit FE flow (already the case). No code provided here.
- **검증 재현**: Independently reproduced every cited claim from source.

BE: getResult (AiDeductionScorer.java:164-221) gates ONLY on validateResultOwner (owner check, :223-229) + findBySessionId null-check (:167-170). No PlaySessionStatus/isPlaying assertion — confirmed. Notably PlaySessionReader.isPlaying(Long) exists (PlaySessionReader.java:13) but is unused in getResult. getResult is the only FE-facing endpoint returning the full solution: correctCulprit (culpritSuspectId/Name/Role, :203-206) and fullExplanation (:217), both built from SolutionInfo — confirmed. The controller exposes exactly two routes: POST /final-deduction and GET /result (AiDeductionController.java:29-43); /result maps to getResult.

Safety invariant is REAL and robustly enforced. saveResultAndComplete (DeductionContextLoader.java:31-42) is @Transactional and performs save(entity):34 -> saveAll(evidences):39 -> playSessionCompleter.complete(sessionId):40 in one TX. complete() -> completeSession() -> markCompleted() (PlaySessionService.java:705); completeSession (:697-707) is @Transactional (joins outer TX) and throws SESSION_NOT_PLAYING (:702) if not playing, rolling back the FinalDeduction save. There are also two earlier guards: ensureNotSubmitted (DeductionContextLoader.java:24-29, existsByPlaySessionId) and lockForFinalDeduction (DefaultPlaySessionCompleter.java:27-31, DB isPlaying check). So a FinalDeduction row cannot be committed for a still-PLAYING session via the current path. The finding's claim that save+complete are coupled is accurate.

No-leak claim verified: InterrogationResponse (InterrogationResponse.java:9-36) carries only interrogationId/suspectId/suspectName/question/answer/unlockedEvidences/createdAt — no solution/secret/culprit. DashboardResponse (DashboardResponse.java:6-25) carries only briefing (victimName/foundLocation/summary) — no solution fields.

FE: ResultScreen(sessionId:) is constructed ONLY at submit_screen.dart:134, after a successful await playSessionRepo.submitFinalDeduction (:121-128) and controller.completeSession (:131). The only playSessionRepo.result() call is in ResultScreen._fetchResult (result_screen.dart:117), invoked only when sessionId != null (initState :107-108); when sessionId is null it shows the local _sampleResult and never calls the API. Grep across lib confirms these are the sole call sites. No FE path fetches /result pre-submit.

Doc ref verified: CLUEROOM_APP_FLOW_API_GUIDE.md:1255 ('제출 성공 후 resultAvailable=true이면 결과 상세를 조회') and :1261 ('결과 응답은 게임이 끝난 뒤에만 표시한다') match the cited behavior. CLAUDE.md §1/§5 prohibit pre-end solution exposure.

Conclusion: This is a verified-positive finding — spoiler safety holds today and no exploitable leak exists. The reported issue is a latent defense-in-depth gap (getResult relies on the save+complete invariant rather than asserting status), exactly as described. P2 is appropriate; it is not INVALID because the missing status assertion genuinely exists and is accurately characterized, and it is not higher than P2 because there is no current code path that leaks the solution mid-game.

### A16. Final-deduction '종합 추리 설명' is mandatory in FE but never sent to BE (no field in contract)

- **slug**: `summary-text-collected-required-not-sent`
- **차원**: aiflow  |  **심각도**: P1 → **P2**  |  confirmed=True, confidence=high
- **분류**: AI deduction / request shape
- **요약**: The submit screen collects a 종합 추리 설명 (_summaryCtrl), enforces it as a required gate (min 10 chars) before the submit button enables, but it is never included in the final-deduction request body. The FE sends only selectedCulpritId/motiveText/methodText/coverUpText/selectedEvidenceIds, matching FinalDeductionRequest exactly. The user's mandatory comprehensive-reasoning input is silently discarded.
- **FE 증거**: lib/screens/submit_screen.dart:31 `_summaryCtrl`, :70-71 `_Requirement('종합 추리를 ${_minSummaryLen}자 이상 입력', _summary.length >= _minSummaryLen)` (required gate, _minSummaryLen=10 at :36), :121-128 submitFinalDeduction called WITHOUT any summary param.
- **BE 증거**: src/main/java/com/startup/domain/ai/dto/FinalDeductionRequest.java:11-28 — no reasoningText/summaryText field exists; only motiveText/methodText/coverUpText/selectedEvidenceIds.
- **문서**: project-fe/docs/backend-requests_2026-06-02_v1.md:60-66 (R4); docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:293 ('현재 API에는 별도의 reasoningText 또는 comprehensiveReasoning 필드가 없다').
- **영향**: Players are forced to write a comprehensive reasoning that has zero effect on scoring and is never persisted. Wasted effort and misleading UX; also a known open decision (backend-requests R4) that remains unresolved on the FE side (still required + dropped).
- **권장(코드변경 없음)**: Resolve R4: either (A) BE adds a reasoningText/summaryText field to FinalDeductionRequest and persists it, or (B) FE makes 종합 추리 optional/display-only (or removes it). Current state — required input that is dropped — is the worst of both. No code provided here.
- **검증 재현**: The mismatch is REAL and independently reproduced from source. FE collects a 종합 추리 설명 via _summaryCtrl (submit_screen.dart:31, :43, bound to MSTextField at :276) and enforces it as a mandatory submit gate of min 10 chars (_minSummaryLen=10 at :36; requirement at :70-71; gated through _allRequirementsMet/_canSubmit at :76-77 driving the button's onPressed at :291). Yet _onSubmit calls playSessionRepo.submitFinalDeduction at :121-128 passing ONLY selectedCulpritId/motiveText/methodText/coverUpText/selectedEvidenceIds — no summary argument. The repo method confirms it: submitFinalDeduction (play_session_repository.dart:101-120) has no summary/reasoning parameter and the POST body (:111-117) contains exactly those 5 keys. The BE contract matches and has no such field: FinalDeductionRequest.java:11-28 declares only selectedCulpritId/motiveText/methodText/coverUpText/selectedEvidenceIds, and AiDeductionController.java:30-32 binds the body strictly via @Valid @RequestBody FinalDeductionRequest (no extra @RequestParam, no wider model). So the user's mandatory comprehensive-reasoning input is silently discarded — accurate. Doc refs verified: CLUEROOM_APP_FLOW_API_GUIDE.md:293 confirms no reasoningText/comprehensiveReasoning field. CORRECTION on citation: the backend-requests doc labels this P2, not "R4" — header at backend-requests_2026-06-02_v1.md:58 ("## P2 — 추가 데이터 필드"), item at :60-66, summary table at :89 ("P2 | final-deduction summaryText | 수용 여부 결정 필요"). No "R4" identifier exists anywhere in the repos; that token appears fabricated. SEVERITY: Downgrading P1→P2. Impact is UX/contract-alignment only: wasted user effort + misleading UX from a mandatory gate whose value is dropped. No crash, no scoring-correctness defect (the 5 real scoring inputs are transmitted correctly), no security/spoiler issue. It is an explicitly-acknowledged OPEN product decision (the doc itself asks BE to pick option A add-field or B drop-requirement), and the team triaged it as P2 — consistent with my assessment.
- **정정/주의**: The finding's "(R4)" citation for the backend-requests doc is wrong/fabricated — the doc classifies this item as P2, not R4 (backend-requests_2026-06-02_v1.md:58 section header "## P2 — 추가 데이터 필드", item :60-66, summary table :89). All other cited file:line locations are exact and correct. Additionally, the BE controller (AiDeductionController.java:30-32) binds the body strictly to FinalDeductionRequest, so even if the FE sent an extra summaryText key it would be ignored — but the FE does not send it at all. Recommend tracking as P2: the team and the doc already flag it as an unresolved A/B decision (add summaryText field, or remove the FE required-gate).

### A17. FE never registers FCM token, and its TODO targets a non-existent endpoint (PATCH /api/users/me) instead of the implemented POST /api/device-tokens

- **slug**: `fcm-token-never-registered-wrong-endpoint`
- **차원**: auth  |  **심각도**: P1 → **P2**  |  confirmed=True, confidence=high
- **분류**: FCM / device token contract
- **요약**: FE obtains a Firebase Messaging token at startup and on refresh, then calls `_registerFcmTokenWithBackend(token)` (main.dart:38-45), but that function is a stub that only `debugPrint`s and never makes an HTTP call (main.dart:67-70). Its TODO comment says it will send the token via `PATCH /api/users/me` with `{fcmToken}` (main.dart:69). However the BE has NO `users/me` endpoint and no `fcmToken` field anywhere (grep for `users/me`, `/api/users`, `fcmToken`, `UserController` returns no matches in src/main/java). The actually-implemented BE contract is `POST /api/device-tokens` with body `DeviceTokenRegisterRequest { String token (@NotBlank, max 512), String deviceType (optional, defaults ANDROID) }` (DeviceTokenController.java:29-37; DeviceTokenRegisterRequest.java:9-19), returning `DeviceTokenResponse { Long deviceTokenId, boolean active }`. FE has no repository method for `/api/device-tokens` at all.
- **FE 증거**: lib/main.dart:67-70 `Future<void> _registerFcmTokenWithBackend(String token) async { debugPrint(...); // TODO: ... PATCH /api/users/me 로 fcmToken 전송 }` (no HTTP call); no device-tokens repository (grep `device-tokens` in lib = only this TODO is FCM-related)
- **BE 증거**: src/main/java/com/startup/domain/notification/controller/DeviceTokenController.java:29-37 `@PostMapping public ResponseEntity<ApiResponse<DeviceTokenResponse>> register(@Valid @RequestBody DeviceTokenRegisterRequest request)`; DeviceTokenRegisterRequest.java:9-18 fields `token`, `deviceType`; grep `users/me`/`fcmToken` in src/main/java = No matches
- **문서**: CaseLab_AI_API_Spec.md device-token / notification section; DeviceTokenController @Tag "FCM 디바이스 토큰 API"
- **영향**: Push notifications cannot work: no device token ever reaches the backend, so FCM sends from the server have no target. The FE's intended integration path (`PATCH /api/users/me {fcmToken}`) is doubly broken — the endpoint does not exist on BE, and the field name (`fcmToken`) differs from the BE's expected `token`/`deviceType` shape. Even after auth lands, following the current TODO would 404. The two sides have diverged on both the path and the request schema for the same capability.
- **권장(코드변경 없음)**: Point the FE FCM registration at the implemented endpoint: `POST /api/device-tokens` with body `{ "token": <fcmToken>, "deviceType": "ANDROID" }`, and add a corresponding repository method. Update the main.dart:69 TODO to drop the `PATCH /api/users/me` plan. Note BE `register` currently uses MockUserProvider for ownership, so token registration works pre-auth. No code change requested here — flag only.
- **검증 재현**: All cited facts independently reproduced from source.

FE side:
- lib/main.dart:67-70: _registerFcmTokenWithBackend(String token) only calls debugPrint and contains a TODO comment "AuthService 로그인 후 PATCH /api/users/me 로 fcmToken 전송". No HTTP call whatsoever. Confirmed verbatim.
- It is invoked at main.dart:39 (startup, after getToken) and main.dart:44 (onTokenRefresh). So FE does obtain the token but never transmits it.
- Grep for fcm/device/token/notification across lib/repositories and lib/services/auth_service.dart = no matches. Only two repositories exist (play_session_repository.dart, scenario_repository.dart); neither handles device tokens. All FCM references are confined to main.dart. So the finding's "handled elsewhere" possibility is ruled out — it is a genuine no-op stub.

BE side:
- Grep for users/me, fcmToken, UserController, /api/users in src/main/java = No matches. The only PatchMapping/PutMapping in the whole backend are in domain/example/controller/ExampleController.java (the package template), not a users endpoint. So PATCH /api/users/me genuinely does not exist.
- DeviceTokenController.java:18-37: @Tag "FCM 디바이스 토큰 API"; @RequestMapping("/api/device-tokens") (line 21); @PostMapping register(@Valid @RequestBody DeviceTokenRegisterRequest) (lines 29-31); resolves user via MockUserProvider.
- DeviceTokenRegisterRequest.java:9-19: record with String token (@NotBlank, @Size max=512) and String deviceType (@Size max=30, optional; comment line 16 confirms service defaults to ANDROID). Matches finding exactly.
- DeviceTokenResponse.java:8-14: record { Long deviceTokenId, boolean active }. Matches finding.

Conclusion: The contract divergence is real and double-broken — wrong path (FE TODO PATCH /api/users/me vs implemented POST /api/device-tokens) AND wrong request schema (FE intends {fcmToken} vs BE expects {token, deviceType}). The finding is accurate in every cited line.

Severity adjustment to P2: This is not a live runtime break. _registerFcmTokenWithBackend makes no call today, so there is no current 404 and no regression in existing functionality. The TODO is explicitly gated on auth ("AuthService 로그인 후"), and per CLAUDE.md the project deliberately defers JWT auth and push/notifications/credits to 후순위 (post-MVP). The gap blocks push notifications and would 404 if the TODO were implemented verbatim, but it affects an unimplemented, deferred (non-MVP) capability with zero impact on current behavior. P1 would be defensible if push were in-scope now; given it is explicitly deferred and the code path is an inert stub, P2 is the accurate severity. The mismatch itself is confirmed real.
- **정정/주의**: The finding is accurate. One refinement on impact: there is no current 404, because the FE never makes any HTTP call at all (the stub only debugPrints) — a 404 would only occur in the hypothetical future where the TODO is implemented as written. Today push notifications are simply non-functional (no token ever reaches BE), rather than actively erroring. Also note the BE already resolves the user via MockUserProvider (DeviceTokenController.java:34), so POST /api/device-tokens is callable even before auth lands — meaning the FE could already integrate against the correct endpoint without waiting for JWT, contrary to the TODO's "AuthService 로그인 후" gating premise.

### A18. FE has no handling for 403 / P004 session-access-denied; BE actively enforces foreign-session ownership

- **slug**: `fe-no-403-p004-handling`
- **차원**: auth  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Session ownership / access control
- **요약**: BE enforces session ownership on every play and AI path: PlaySessionService throws `PlayErrorCode.SESSION_ACCESS_DENIED` (P004, HTTP 403) when `session.getUserId() != userId` (PlaySessionService.java:156-157, 296-297, 338-339, 738-739); the AI services throw `CommonErrorCode.ACCESS_DENIED` (C004, HTTP 403) for foreign sessions (InterrogationContextLoader.java:44-45; AiDeductionScorer.java:77-78, 226-227; InterrogationLogQueryService.java:59-60). The dedicated test `AiSessionAccessDeniedTest` flips the mock user to a non-owner and asserts ACCESS_DENIED is raised for both interrogation and final-deduction (AiSessionAccessDeniedTest.java:81-107). On the FE side, `ApiException` exposes `isUnauthorized` (401) and `isNotFound` (404) helpers but no 403/Forbidden helper (api_exception.dart:24-25), and the only status-specific handling in the play flow is 409 -> session conflict (game_session_controller.dart:100) and >=500 -> server error (submit_screen.dart, per inventory). A 403/P004 falls through to the generic `_loadError = e.message` (game_session_controller.dart:104) showing the raw Korean BE message.
- **FE 증거**: lib/core/api/api_exception.dart:24-25 `bool get isNotFound => status == 404; bool get isUnauthorized => status == 401;` (no 403); lib/controllers/game_session_controller.dart:100-105 only handles 409, else `_loadError = e.message`
- **BE 증거**: src/main/java/com/startup/domain/play/service/PlaySessionService.java:156-157 `if (!session.getUserId().equals(userId)) throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);` (P004=403, PlayErrorCode.java:16); src/test/java/com/startup/domain/ai/controller/AiSessionAccessDeniedTest.java:90-92 asserts ACCESS_DENIED
- **문서**: CaseLab_AI_API_Spec.md §17 error table P004 (해당 세션에 접근할 수 없습니다.); CommonErrorCode C004 (403 Access is denied.)
- **영향**: Because all users currently resolve to mock userId=1, a 403 is effectively unreachable in the present demo, so this is not an active break. But it is a latent gap: the moment real identities exist (multi-device testing or JWT), a user re-entering another user's session, or a stale saved sessionId belonging to a different identity, will receive a 403 the FE does not recognize. `_tryResume` only special-cases network errors and otherwise clears the saved session (game_session_controller.dart:118-123), so a 403 on resume would silently discard the saved session and attempt a fresh create — masking the real cause. No spoiler/crash risk; degraded/confusing UX only.
- **권장(코드변경 없음)**: Add a 403/Forbidden branch to FE error handling (e.g. an `isForbidden => status == 403` helper plus a P004/C004 code check) so foreign-session access surfaces a clear, non-raw message rather than the generic BE string. Lower priority while userId is globally fixed. No code change requested here — flag only.
- **검증 재현**: Independently reproduced from source on both sides. BE enforces foreign-session 403 everywhere: PlaySessionService.java:156-157/296-297/338-339/738-739 throw PlayErrorCode.SESSION_ACCESS_DENIED, and PlayErrorCode.java:16 maps P004 to HttpStatus.FORBIDDEN (403). AI path throws BusinessException(CommonErrorCode.ACCESS_DENIED) at InterrogationContextLoader.java:44-45, AiDeductionScorer.java:77-78 and 226-227, InterrogationLogQueryService.java:59-60; CommonErrorCode.java:14 maps C004 to FORBIDDEN (403). AiSessionAccessDeniedTest.java:85/90-92/99/104-107 sets mockUserId to a non-owner (999L vs owner 100L) and asserts ACCESS_DENIED for both interrogation and final-deduction. FE has no 403 handling: api_exception.dart:24-25 only exposes isNotFound(404)/isUnauthorized(401); api_client.dart:142 populates status from error.status or res.statusCode, so a 403 IS received numerically but never branched on; game_session_controller.dart:100-105 only special-cases 409, everything else falls to _loadError=e.message (raw Korean BE message); _tryResume at game_session_controller.dart:118-123 rethrows only on isNetwork, otherwise clears the saved session and returns false, so a 403 on resume silently discards the saved session and triggers a fresh createSession; submit_screen.dart:137-142 dedicates a message only to >=500, so a 403 shows the raw 'submit 실패: ${e.message}'. A repo-wide grep for 403|forbidden|C004|P004 across all .dart files returns zero matches, confirming no handling anywhere. The gap is real and exactly as described. Severity is correctly P2: with all users resolving to mock userId=1 (authTokenProvider unset, api_client.dart:60-61) a 403 is currently unreachable, so this is a latent multi-identity/JWT-era gap, not an active break, and has no spoiler/crash/security impact — only degraded/confusing UX.
- **정정/주의**: Two minor imprecisions that do not affect the verdict: (1) The summary in one place attributes the AI-path 403 to PlayErrorCode.SESSION_ACCESS_DENIED, but the AI services (InterrogationContextLoader, AiDeductionScorer, InterrogationLogQueryService) actually throw CommonErrorCode.ACCESS_DENIED (C004), as the finding's own BE-evidence and the test correctly note. Both are HTTP 403, so the FE gap is identical. (2) The finding cites 'submit_screen.dart' without a path; the actual file is lib/screens/submit_screen.dart and its 403 handling is at lines 137-142 (only >=500 gets the channel-error branch).

### A19. FE never sends Authorization header; all sessions resolve to one shared mock user (data bleed in prod)

- **slug**: `fe-no-auth-header-shared-mock-user`
- **차원**: auth  |  **심각도**: P1 → **P2**  |  confirmed=True, confidence=high
- **분류**: Auth / identity
- **요약**: FE `ApiClient` declares `authTokenProvider` (api_client.dart:61) and would attach `Authorization: Bearer <token>` if it returned a non-empty value (:86-89), but the field is never assigned anywhere in the codebase (grep confirms only the declaration at :61 and the read at :86 exist). `AuthService.init()` loads the cached token and falls back to the literal `'mock_jwt_token'` (auth_service.dart:21), `saveTokens` has no caller, and the cached token is never bridged into `ApiClient.authTokenProvider`. BE intentionally has no auth: `MockUserProvider.currentUserId()` returns a single configured id (default 1) for every request (MockUserProvider.java:13-19), and every controller resolves the user this way (e.g. PlaySessionController.java:29; DeviceTokenController.java:34). So today FE and BE agree by accident (no header / mock user), but the identity of every caller is globally identical.
- **FE 증거**: lib/core/api/api_client.dart:61 `String? Function()? authTokenProvider;` (declared, never assigned — grep shows only :61 and :86); auth_service.dart:21 `_cachedToken ??= 'mock_jwt_token';` and :24 `saveTokens` has no caller
- **BE 증거**: src/main/java/com/startup/common/auth/MockUserProvider.java:13-19 `public MockUserProvider(@Value("${app.mock-user-id:1}") Long mockUserId)` / `public Long currentUserId() { return mockUserId; }`
- **문서**: CLAUDE.md §4 (userId 하드코딩 금지, MockUserProvider 사용); CaseLab_AI_API_Spec.md §4.1/§14.2 (auth/JWT is post-MVP); api_client.dart:59-60 comment acknowledging MockUserProvider(userId=1)
- **영향**: Within the current Mock-only MVP this is consistent and does not break the demo: every request maps to userId=1. The real risk is forward-looking and data-integrity oriented. (1) If two devices/users run the app against the same backend, they ALL share userId=1 — sessions, interrogation logs, device tokens, and final-deduction results bleed across users; e.g. user B can resume user A's active session because ownership checks pass (both are userId=1). (2) When BE introduces JWT (documented as post-MVP: spec §4.1/§14.2), the FE will still send no Authorization header, so every authenticated endpoint will 401 until the `authTokenProvider` wiring gap is closed. This is the seam the whole auth feature hinges on and it is currently a dangling, untested no-op.
- **권장(코드변경 없음)**: Track as a known pre-auth limitation. Before any multi-user testing or JWT rollout: assign `ApiClient.instance.authTokenProvider = () => AuthService.instance.token` during app init (main.dart, after AuthService.init()), and confirm BE swaps MockUserProvider for a SecurityContext-based provider at the same time. Do not ship to multiple real users while userId is globally fixed at 1. No code change requested here — flag only.
- **검증 재현**: All cited facts independently verified against source.

FE: api_client.dart:61 declares `String? Function()? authTokenProvider;` and :86-89 reads it (`final token = authTokenProvider?.call(); if (token != null && token.isNotEmpty) headers['Authorization'] = 'Bearer $token';`). Grep over project-fe confirms ONLY two occurrences of authTokenProvider (:61 declaration, :86 read) — it is never assigned anywhere, so the Authorization header is never sent. auth_service.dart:21 `_cachedToken ??= 'mock_jwt_token';` and :24 `saveTokens(...)` confirmed; grep confirms saveTokens has no caller and AuthService.token is never read by ApiClient (only AuthService.init() is called at main.dart:20). The bridge from AuthService into ApiClient.authTokenProvider genuinely does not exist — a dangling no-op.

BE: MockUserProvider.java:13-19 confirmed verbatim (`@Value("${app.mock-user-id:1}")`, `currentUserId()` returns the constant). PlaySessionController.java:29 and DeviceTokenController.java:34 both resolve user via `mockUserProvider.currentUserId()`. No spring-security dependency in any build.gradle (grep: no matches), and no SecurityContext/JwtFilter/SecurityFilterChain/@PreAuthorize anywhere — every request truly maps to userId=1.

Data-bleed mechanism confirmed: PlaySessionService has real ownership checks (`session.getUserId().equals(userId)` → SESSION_ACCESS_DENIED at :156-157, :296-297, :338-339; validateSessionOwner at :169/:214/:395/:466/:509/:567). Because currentUserId() is a global constant, two distinct physical devices both pass these checks against each other's sessions — the finding's claim that user B can access user A's session is accurate.

Severity downgraded P1 -> P2. The finding itself honestly scopes impact as forward-looking: within the current Mock-only MVP, FE and BE interoperate correctly (no header / single mock user), nothing is broken, and the demo works. This is the deliberate, documented MVP state — CLAUDE.md §2 lists JWT auth as post-MVP (후순위), and both code sites carry comments explicitly acknowledging the temporary MockUserProvider design (DeviceTokenController.java:33; MockUserProvider.java:7-8) and the deferred Phase-5 wiring (api_client.dart:59-60). The prod data-bleed is real but conditional on shipping the Mock build to a shared multi-user backend (an explicitly non-MVP scenario) and the 401-on-JWT risk is contingent on a future feature that does not yet exist. So this is a genuine latent wiring/data-integrity gap worth tracking, but not an active FE↔BE integration mismatch — P1 overstates current impact while INVALID would wrongly dismiss a real, well-evidenced latent defect. P2 fits.

### A20. Onboarding/splash flow has no login/signup and no backend auth endpoint; isLoggedIn is permanently false

- **slug**: `login-onboarding-no-backend-auth`
- **차원**: auth  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Auth / onboarding
- **요약**: The FE app boots Splash -> Onboarding/AppShell purely on a local `onboarding_complete` shared_preferences flag (splash_screen.dart:49-63); there is no login or signup screen and `saveTokens` is never called (auth_service.dart:24, no callers). `isLoggedIn` is defined as token present AND token != 'mock_jwt_token' (auth_service.dart:41-42), but since `init()` always defaults the token to `'mock_jwt_token'` (auth_service.dart:21) and nothing ever stores a real token, `isLoggedIn` is always false. Correspondingly, the BE has no auth controller — signup/login/refresh/users-me are documented as post-MVP only (spec §4.1/§14.2) and not implemented (no UserController/users/me found in src/main/java).
- **FE 증거**: lib/screens/splash_screen.dart:49-63 routes solely on `OnboardingFlag.isComplete()` (no auth check); lib/services/auth_service.dart:41-42 `isLoggedIn => _cachedToken != null && _cachedToken != 'mock_jwt_token'` always false given :21 default
- **BE 증거**: grep `users/me`/`/api/users`/`UserController` in src/main/java = No matches (no auth/user controller implemented); per docs spec §4.1/§14.2 auth is post-MVP
- **문서**: CLAUDE.md §2 후순위 'JWT 인증 완성'; CaseLab_AI_API_Spec.md §4.1 (auth endpoints '인증 도입 후'), §14.2 (post-MVP)
- **영향**: Consistent with the documented MVP scope (JWT auth is post-MVP per CLAUDE.md §2 후순위 and spec §14.2), so this is not an MVP break — it is drift between the auth scaffolding the FE carries (AuthService, token keys, Bearer wiring) and the complete absence of an auth flow on either side. The scaffolding is dead/dormant: it neither helps nor harms today, but it can mislead implementers into thinking auth is partially wired when in fact no token ever flows. Pairs with the authTokenProvider gap (separate finding) as the work remaining before auth functions.
- **권장(코드변경 없음)**: No action required for MVP. When auth is introduced, build login/signup screens that call the (future) `POST /api/auth/login`, call `AuthService.saveTokens`, and wire `authTokenProvider` to the cached token. Until then, treat AuthService/Bearer wiring as intentionally inert. Flag only.
- **검증 재현**: Independently reproduced every claim from source. FE: splash_screen.dart:49-63 routes purely on OnboardingFlag.isComplete() (a shared_preferences bool, lines 17-20) with no auth check — AppShell vs OnboardingScreen only. auth_service.dart:21 defaults _cachedToken ??= 'mock_jwt_token'; :41-42 isLoggedIn => _cachedToken != null && _cachedToken != 'mock_jwt_token', so it is permanently false. Grep confirms saveTokens (auth_service.dart:24) has zero callers and isLoggedIn (:41) has zero consumers (only their own definitions match). Case-insensitive grep for login/signup/LoginScreen across lib = no matches: no auth flow screen exists. BE: grep for UserController/AuthController/users/me/auth in src/main/java = no matches; the only controllers are example, ai (interrogation/deduction/validation), notification (device-token/test), scenario, play-session. Docs confirm post-MVP: CaseLab_AI_API_Spec.md:220-225 marks all /api/auth/* and /api/users/me as '인증 도입 후', and CLAUDE.md §2 lists 'JWT 인증 완성' under 후순위. The gap is real and matches the documented MVP scope, so it is drift/dead scaffolding rather than an MVP break — P2 is correct. The finding is if anything slightly conservative: the mock token would not even reach the wire because api_client.dart:61 authTokenProvider is declared but never assigned (grep shows only its declaration at :61 and self-call at :86), so the Bearer header at :88 is never set. The token chain is doubly dormant, reinforcing the dead-scaffolding conclusion.
- **정정/주의**: Finding is accurate. Minor strengthening: even if isLoggedIn were true or a token existed, no token would flow to the backend because ApiClient.authTokenProvider (api_client.dart:61) is never assigned anywhere — grep returns only the declaration and the internal call site (:86). Thus the Bearer header at api_client.dart:88 is never emitted, making the auth scaffolding doubly dormant (this overlaps with the separately-tracked authTokenProvider finding).

### A21. Several implemented BE play endpoints have no FE consumer (note-only)

- **slug**: `be-implemented-unconsumed-play-endpoints`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Implemented endpoint with no consumer (low severity note)
- **요약**: The BE implements these endpoints that no live FE call touches (grep of lib/ confirms FE only calls dashboard, evidences(list), suspects(list), hints(list+use), abandon, interrogations(GET+POST), final-deduction, result, scenarios(list+detail), createSession): GET /play-sessions/{id} session detail (PlaySessionController.java:44-52), GET /play-sessions/active (:34-42), GET /{id}/evidences/{evidenceId} detail (:108-117), POST /{id}/evidences/{evidenceId}/unlock (:119-129), GET /{id}/suspects/{suspectId} detail (:87-96), GET /{id}/locations (:132-140), GET /{id}/timeline (:98-106), plus AI validate POST /api/ai/scenarios/{id}/validate and GET /api/scenarios/{id}/validation-result (AiScenarioValidationController.java:23,32), and POST /api/device-tokens (DeviceTokenController.java). The Example controller (/api/examples) is a convention template, expected to have no consumer.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/repositories/play_session_repository.dart:13-125 and scenario_repository.dart:53-90 — the complete set of live FE HTTP calls; none hit the listed endpoints (grep confirmed)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/play/controller/PlaySessionController.java:34-140 (active/detail/evidence-detail/unlock/suspect-detail/locations/timeline); AiScenarioValidationController.java:23,32
- **문서**: CaseLab_AI_API_Spec.md §9.x (play endpoints), §8.2 (validate)
- **영향**: No integration break; these are either FE-features-not-yet-wired (timeline, locations, evidence detail/unlock, suspect detail, scenario validation for the custom-scenario authoring flow) or template code. Flagged so the team knows the surface the FE has not yet adopted.
- **권장(코드변경 없음)**: Cross-reference with the FE roadmap: wire evidence-detail/unlock, suspect-detail, locations, timeline, and (for custom-scenario authoring MVP) the AI validate endpoints when those FE screens go live. No code edits performed.
- **검증 재현**: Independently reproduced from source. Every BE controller line citation is exact:
- PlaySessionController.java: getActiveSession GET /active (:34-42), getSessionDetail GET /{sessionId} (:44-52), getSuspectDetail GET /{sessionId}/suspects/{suspectId} (:87-96), getTimeline GET /{sessionId}/timeline (:98-106), getEvidenceDetail GET /{sessionId}/evidences/{evidenceId} (:108-117), unlockEvidence POST /{sessionId}/evidences/{evidenceId}/unlock (:119-129), getLocations GET /{sessionId}/locations (:132-140) — all present and exactly as described.
- AiScenarioValidationController.java: POST /api/ai/scenarios/{scenarioId}/validate (:23), GET /api/scenarios/{scenarioId}/validation-result (:32) — exact.
- DeviceTokenController.java: POST /api/device-tokens (:21 mapping, :29 POST) — present.
- ExampleController.java: @RequestMapping("/api/examples") (:32) — confirmed template code, correctly excluded.

FE consumption set verified two ways: (1) reading play_session_repository.dart:13-125 and scenario_repository.dart:53-90, and (2) a comprehensive grep of every HTTP call site (_api.get/post/...) across all of project-fe/lib — the ONLY live call sites are in those two repository files, matching the finding's listed set exactly (dashboard, evidences list, suspects list, hints list+use, abandon, interrogations GET+POST, final-deduction, result, scenarios list+detail, createSession). None of the listed endpoints are touched. The interrogations GET (logs) and POST both exist on AiInterrogationController.java:34/42 and final-deduction/result on AiDeductionController.java:29/38, all of which the FE does consume, so the finding correctly excludes them from the unconsumed set.

The "FE-not-yet-wired" characterization is strongly corroborated: timeline_screen.dart:33-35 has an explicit TODO(backend) comment referencing GET /api/play-sessions/{sessionId}/timeline and currently uses hardcoded sampleCase.timeline data; suspect_detail_screen.dart receives the suspect via constructor (widget.suspect) and loads only interrogation logs, never calling the suspect-detail endpoint. fcmToken appears in FE only as a TODO comment (main.dart:69), so device-tokens is genuinely unconsumed.

No integration break — purely an informational surface-coverage note. P2 is appropriate; not INVALID since the gap is real, not P1 since nothing fails at runtime.
- **정정/주의**: Finding is accurate. One minor completeness gap (not an error): the BE also implements POST /api/notifications/test (NotificationTestController.java:34) which the FE does not consume either. It is omitted from the finding, but reasonably so because it is annotated @Profile({"local","test"}) (NotificationTestController.java:23) and thus only registered in local/test profiles — it is a dev-only verification endpoint, not a production FE-facing surface. Worth a one-line note for the team but does not change severity or the finding's conclusion.

### A22. My-Records screen needs play-history list (GET /play-sessions/me) which BE has not implemented; FE shows sample data

- **slug**: `be-my-records-endpoint-missing`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Missing canonical endpoint (post-MVP scope)
- **요약**: The My-Records screen reads samplePlaySessions and has a '추후 ... 교체 예정' comment (my_records_screen.dart:37,:55,:57,:59). Canonical spec defines GET /api/play-sessions/me?status,page,size (CaseLab_AI_API_Spec.md §11.3 lines 1725-1762) but classifies my-records/mypage as 2nd MVP (§14.2 lines 1992-1994; CLAUDE.md §2 후순위 '내 기록 / 마이페이지 완성'). No BE controller implements /play-sessions/me (PlaySessionController.java has no /me route).
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/screens/my_records_screen.dart:37,:55,:57 ('추후 ... 교체 예정'),:59 (samplePlaySessions)
- **BE 증거**: N/A — no /play-sessions/me route in PlaySessionController.java:16-172
- **문서**: CaseLab_AI_API_Spec.md §11.3 (lines 1725-1762); §14.2 (lines 1992-1994) post-MVP
- **영향**: My-Records is non-functional against the server; entirely sample-driven. Acceptable for 1st MVP per scope, but flagged so it is tracked.
- **권장(코드변경 없음)**: Defer to 2nd MVP per spec scope; when built, implement GET /play-sessions/me and replace samplePlaySessions. No code edits performed.
- **검증 재현**: Independently reproduced from source. FE: my_records_screen.dart is entirely sample-driven — it reads samplePlaySessions (defined sample_scenarios.dart:116) at lines 37, 55, 59 and has no API service call. A repo-wide grep of project-fe/lib found zero my-records API calls (the only /me reference is an unrelated FCM-token TODO at main.dart:69). BE: PlaySessionController.java (lines 16-172) declares 14 mappings (POST /, /active, /{sessionId}, /dashboard, /evidences, /suspects, /suspects/{suspectId}, /timeline, /evidences/{evidenceId}, .../unlock, /locations, /hints, .../use, /abandon) — none is /me; a grep for play-sessions/me|sessions/me across src returned no matches, so GET /api/play-sessions/me is not implemented. Doc refs verified: spec §11.3 (lines 1722-1762) defines GET /api/play-sessions/me with status/page/size; §14.2 line 1992 lists it explicitly under '2차 MVP' (post-MVP); CLAUDE.md §2 lists '내 기록 / 마이페이지 완성' as 후순위. The gap is real but is an explicitly deferred post-MVP item, not a 1st-MVP blocker, so P2 (tracking flag) is correct.
- **정정/주의**: Line-citation imprecision in FE evidence: lines 37 and 55 are sample-data filter usages (samplePlaySessions.where(...)), NOT comments. The '추후 ... 교체 예정' comment is actually at lines 56-57 (the finding cites :37,:55,:57). Also, that comment is scoped specifically to the _RecordsFilter.mine cross-filtering (authored scenario IDs), not a general server-data-replacement note — though the screen is fully sample-driven regardless, so the finding's substance holds. The samplePlaySessions reference is correctly at line 59 (and also 37, 55). Spec §11.3 starts at line 1722 (header) with the endpoint definition spanning 1724-1762; the finding's 1725-1762 range is acceptable.

### A23. No /api/users/me endpoint in BE; FE FCM token registration target (PATCH /api/users/me) is a TODO stub and uses the wrong route vs the implemented /api/device-tokens

- **slug**: `be-users-me-missing-fcm-stub`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Missing endpoint + route mismatch (post-MVP auth, but FCM relevant)
- **요약**: The FE intends to register the FCM token via PATCH /api/users/me {fcmToken} but the function is a debugPrint stub that never sends (main.dart:67-70 `// TODO: ... PATCH /api/users/me 로 fcmToken 전송`). The BE has NO user controller / /api/users/me route at all (canonical /api/users/me is post-MVP auth, spec §5.3/§14.2). Separately, the BE DOES implement device-token registration at a DIFFERENT path/shape: POST /api/device-tokens {token,deviceType} (DeviceTokenController.java:21 base, register endpoint), which the FE never calls. So the two sides disagree on both route (PATCH /api/users/me vs POST /api/device-tokens) and shape (fcmToken field vs token/deviceType).
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/main.dart:67-70 (`_registerFcmTokenWithBackend` debugPrint-only TODO targeting PATCH /api/users/me)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/notification/controller/DeviceTokenController.java:21 (base /api/device-tokens, POST register) — different path/shape; no /api/users/me controller exists
- **문서**: CaseLab_AI_API_Spec.md §5.3 / §4.1 (PATCH /api/users/me, post-MVP); §14.2 auth is 2nd MVP
- **영향**: Push notifications cannot be delivered to devices: FE never sends the token, and even when un-stubbed it targets a non-existent route with a mismatched body instead of the implemented /api/device-tokens. The implemented BE device-token endpoint has zero consumers.
- **권장(코드변경 없음)**: Decide the contract: either FE calls the existing POST /api/device-tokens {token,deviceType} (preferred, already built) or BE adds the spec'd PATCH /api/users/me path. Align field name (token vs fcmToken). Graded P2 because push and auth are post-MVP. No code edits performed.
- **검증 재현**: Independently reproduced from source. FE: main.dart:67-70 _registerFcmTokenWithBackend is a debugPrint-only stub with TODO targeting PATCH /api/users/me with an fcmToken field; it never issues an HTTP request. It is the ONLY FE file touching FCM token registration and never calls /api/device-tokens (grep over lib/ confirms). BE: no controller serves /api/users/me anywhere in src/ (grep for users/me and api/users RequestMapping = 0 matches; controller inventory has no UserController). The implemented endpoint is DeviceTokenController.java:21 base /api/device-tokens with @PostMapping register at :29, and DeviceTokenRegisterRequest.java:13,18 uses {token, deviceType} — different route AND different body shape than the FE TODO. Doc: CaseLab_AI_API_Spec.md:224-225 marks GET/PATCH /api/users/me as "인증 도입 후" (post-MVP, requires auth which is itself 2nd MVP). The spec contains zero references to fcmToken/deviceToken (grep = 0), so PATCH /api/users/me was never spec'd to carry an FCM token — the FE TODO invented that contract. Net: route mismatch (PATCH /api/users/me vs POST /api/device-tokens) and shape mismatch (fcmToken vs token/deviceType) are both real, and the implemented BE endpoint has zero FE consumers. Severity stays P2: this is a forward-looking, explicitly-stubbed (TODO) integration gap with no active runtime failure — the FE makes no call at all, push delivery is a known-incomplete feature, and the prerequisite (auth + /api/users/me) is post-MVP. No crash/data issue warranting P1/P0; not INVALID since the gap and mismatch are factual.
- **정정/주의**: Minor precision note: the finding's BE evidence cites only DeviceTokenController.java:21 (the base @RequestMapping). For completeness the actual register endpoint is the @PostMapping at DeviceTokenController.java:29-30 (method register), and the body shape is defined in DeviceTokenRegisterRequest.java:13 (token) and :18 (deviceType). Additionally, the BE/shared API spec (CaseLab_AI_API_Spec.md) never associates an fcmToken field with PATCH /api/users/me at all — that field/route pairing exists only in the FE TODO comment, so the FE's intended contract was never a documented BE contract in the first place. All other claims in the finding are accurate.

### A24. FE hardcodes playable scenario IDs {1,4,5} instead of consuming BE canPlay; start button gating disconnected from server

- **slug**: `fe-ignores-canplay-hardcoded-playability`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Field/contract drift (implemented field not consumed)
- **요약**: The BE returns canPlay on both scenario list (ScenarioSummaryResponse) and detail (ScenarioDetailResponse), populated from ScenarioAccessService (ScenarioService.java:68,99). The FE summary/detail mappers do NOT read canPlay (scenario_repository.dart:95-136 omit it). Instead the detail screen hardcodes playability to a static set: `const _kPlayableIds = {'1','4','5'}` and gates the start button on it (scenario_detail_screen.dart:17,:33 `_isPlayable`); others show '준비 중' and disable start.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/screens/scenario_detail_screen.dart:17 `const _kPlayableIds = {'1','4','5'};`, :33 `bool get _isPlayable => _kPlayableIds.contains(widget.scenario.id);`; scenario_repository.dart:95-114 omits canPlay
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/scenario/service/ScenarioService.java:68 (canPlay on list), :99 (canPlay on detail)
- **문서**: CaseLab_AI_API_Spec.md §6.1/§6.2 (canPlay field); FE-copy drift D4 (FE api-spec omits canPlay)
- **영향**: Playability shown to the user is decoupled from the server's actual access decision. New/official scenarios with IDs outside {1,4,5} are wrongly shown as not-playable, and any server-side access restriction is ignored. Lower severity because the current canPlay is effectively permissive (ScenarioAccessService returns true), but it is a latent correctness gap.
- **권장(코드변경 없음)**: Have the FE read and honor the BE canPlay field on list/detail responses and drive the start button from it; remove the hardcoded _kPlayableIds gate. No code edits performed.
- **검증 재현**: Independently reproduced from source in both repos. BE: ScenarioService.java:68 sets canPlay on the list response and :99 on detail, both from scenarioAccessService.canPlay(...). Both DTOs expose the field: ScenarioSummaryResponse.java:22 and ScenarioDetailResponse.java:32 (Boolean canPlay). FE: the summary mapper _fromSummaryJson (scenario_repository.dart:95-114) and detail mapper _fromDetailJson (:116-136) never read json['canPlay']; moreover the Scenario model (lib/models/scenario.dart:7-40) has no playability field at all, so canPlay is structurally dropped. Playability is instead hardcoded in scenario_detail_screen.dart:17 (const _kPlayableIds = {'1','4','5'}) and :33 (_isPlayable => _kPlayableIds.contains(widget.scenario.id)); the start button is gated on it (:90-98 onStart wired only when playable; :788 label '준비 중'; :795 onPressed disabled when not playable; :738-772 shows the not-ready notice). A repo-wide grep confirms 'canPlay' appears nowhere in lib/, so this hardcoded set is the only gate. The mismatch/gap is real exactly as described. Severity P2 is correct: ScenarioAccessService.canPlay returns true unconditionally (ScenarioAccessService.java:12-14, with comment at :8-9 that MVP allows all access), so the FE gate is strictly MORE restrictive than the server and cannot grant anything the server denies — no security hole. The defect is a latent UX/correctness decoupling: scenarios with IDs outside {1,4,5} are always shown not-playable, and any future server access policy is ignored. Two minor citation corrections below.
- **정정/주의**: FE repository file path in the finding is wrong: it is lib/repositories/scenario_repository.dart, NOT lib/data/scenario_repository.dart. The cited line ranges (95-114 / 95-136) are correct for that file. Also, the finding undercounts the work needed: the FE Scenario model (lib/models/scenario.dart:7-40) defines no canPlay/playability field, so wiring canPlay would require adding a model field plus mapper plumbing, not merely reading it in the mapper. The BE-side facts (ScenarioService.java:68,:99; both DTOs at line 22/32; ScenarioAccessService returning true) and the FE hardcoding facts (scenario_detail_screen.dart:17,:33, gating at :90-98/:788/:795) are all accurate.

### A25. No login/signup/profile endpoints consumed; FE auth is unbuilt (mock token), users/me unimplemented

- **slug**: `fe-no-auth-no-profile`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Missing canonical endpoint (post-MVP auth)
- **요약**: Canonical spec defines auth (POST /api/auth/signup, /login, /logout, /refresh) and GET /api/users/me (§5.1-5.3, §4.1 lines 220-225), but all are explicitly post-MVP ('JWT 인증 도입 후', §14.2 lines 1988-1990; CLAUDE.md §2 후순위 'JWT 인증 완성'). BE implements none (no auth/user controller; auth resolved via MockUserProvider). FE has no login/signup screen, AuthService.saveTokens has no caller, init() defaults to literal 'mock_jwt_token', and ApiClient.authTokenProvider is never assigned so no Authorization header is ever sent (api_client.dart:61,86-89). My-Page is fully static (no profile/credit call).
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/core/api/api_client.dart:61 (`authTokenProvider` never assigned), :86-89 (Authorization only if provider set)
- **BE 증거**: N/A — no auth/user controller; userId via MockUserProvider only (e.g. PlaySessionController.java:29)
- **문서**: CaseLab_AI_API_Spec.md §5.1-5.3, §4.1; §14.2 (lines 1988-1990) post-MVP; CLAUDE.md §2 후순위
- **영향**: No real identity end-to-end; everything runs as MockUserProvider userId. Correct for current MVP phase, but means My-Page profile and any per-user ownership are non-functional. Low risk now.
- **권장(코드변경 없음)**: Track as post-MVP. When auth lands, implement auth + users/me on BE and bridge AuthService token to ApiClient.authTokenProvider on FE. No code edits performed.
- **검증 재현**: Independently reproduced every load-bearing claim from source.

FE:
- api_client.dart:61 declares `String? Function()? authTokenProvider;` and a repo-wide grep for `authTokenProvider\s*=` returns NO matches — it is never assigned. Lines 86-89 only add the `Authorization: Bearer` header when `authTokenProvider?.call()` returns a non-empty token, so no Authorization header is ever sent. Confirmed.
- auth_service.dart:21 `_cachedToken ??= 'mock_jwt_token';` in init(). saveTokens() (lines 24-32) is defined but a grep for `saveTokens` finds only the definition, no caller. Confirmed.
- No login/signup/signin screen files exist (full lib/screens listing has splash, onboarding, my_page, my_records, etc., but none for auth). The only `users/me` reference is a TODO comment in main.dart:69. Confirmed.
- my_page_screen.dart is a StatelessWidget with hardcoded profile ('탐정견습생' line 97, 'detective@clueroom.xyz' line 102) and no-op menu items including 로그아웃 (onTap: () {} line 145). No profile/credit API call. Confirmed fully static.

BE:
- Full controller listing (Glob **/*Controller.java) contains NO auth or user controller; request mappings are /api/device-tokens, /api/notifications, /api/play-sessions, interrogations, deduction, examples, scenarios. Glob for {User,Auth,Login,SignUp,Signup,Member,Account}Controller.java returns No files found.
- PlaySessionController.java:29 `Long userId = mockUserProvider.currentUserId();` — exact line cited is correct. All endpoints resolve userId via MockUserProvider.

Doc: §4.1 (lines 216-226) is the actual auth/user table; lines 220-225 mark signup/login/logout/refresh/GET users/me all as '인증 도입 후'. §14.2 lines 1988-1990 list these as 2차 MVP (post-MVP). CLAUDE.md §2 lists 'JWT 인증 완성' as 후순위. All confirmed.

This is an intentional, documented MVP-phase gap (auth is explicitly post-MVP on both sides), not a defect. Per-user identity is non-functional but everything works under MockUserProvider. P2 is appropriate.
- **정정/주의**: Minor doc-citation imprecision: the finding cites 'CaseLab_AI_API_Spec.md §5.1-5.3' for the auth/users endpoints, but the auth & user API table is actually §4.1 (lines 216-226), and the post-MVP list is §14.2 (lines 1988-1990). The §4.1, §14.2, and the specific line numbers (220-225, 1988-1990) cited elsewhere in the finding are accurate; only the leading '§5.1-5.3' label is off. All FE:line and BE:line citations (api_client.dart:61/86-89, PlaySessionController.java:29) are exact and correct. Severity and confirmed status unaffected.

### A26. GET /interrogations returns InterrogationLogResponse (presentedEvidence, no unlockedEvidences); FE parses it as InterrogationResult expecting unlockedEvidences

- **slug**: `interrogation-log-shape-mismatch`
- **차원**: coverage  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Response-shape drift (same path, divergent DTO between GET/POST)
- **요약**: POST .../interrogations returns InterrogationResponse with unlockedEvidences[] (InterrogationResponse.java), but GET .../interrogations returns InterrogationLogResponse which has presentedEvidence (a single object) and NO unlockedEvidences (InterrogationLogResponse.java:7-17). The FE uses the SAME model InterrogationResult.fromJson for both (play_session_repository.dart:94-96 for the GET list), and that model only reads unlockedEvidences/answer/etc. (play_models.dart:346-359). Because unlockedEvidences defaults to [] when absent (:353-354) and presentedEvidence is simply ignored, this does NOT crash — but the interrogation-log history silently loses the 'evidence presented' info and never shows it.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/repositories/play_session_repository.dart:94-96 (GET list mapped with InterrogationResult.fromJson); lib/models/play_models.dart:346-359 (reads unlockedEvidences, no presentedEvidence)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/ai/dto/InterrogationLogResponse.java:7-17 (has presentedEvidence, no unlockedEvidences)
- **문서**: CaseLab_AI_API_Spec.md §10.3 (GET log shape) vs §10.1-10.2 (POST shape)
- **영향**: No crash, but log-history detail (which evidence the player presented) is dropped in the suspect detail screen. Minor UX/data-fidelity loss; also a latent trap if the FE model is later changed to require unlockedEvidences as non-null.
- **권장(코드변경 없음)**: Either align the GET response to include unlockedEvidences or add a dedicated FE log model that reads presentedEvidence + questionType. Document that GET and POST on this path return different DTOs. No code edits performed.
- **검증 재현**: The structural mismatch is REAL and independently reproduced from source.

BACKEND:
- AiInterrogationController.java:34-39 -> GET /interrogations returns List<InterrogationLogResponse>; :42-49 -> POST returns InterrogationResponse. Confirmed the two endpoints use different DTOs.
- InterrogationLogResponse.java:7-17 has presentedEvidence (PresentedEvidenceDto = evidenceId+title, a SINGLE nullable object) and NO unlockedEvidences. The presented-evidence value is the evidence the player presented during interrogation (InterrogationLogQueryService.java:94-101 maps log.getPresentedEvidenceId).
- InterrogationResponse.java:25-35 has unlockedEvidences (List) and NO presentedEvidence.
- API spec corroborates: GET log JSON (CaseLab_AI_API_Spec.md:1566-1581) has presentedEvidence, no unlockedEvidences; POST JSON (:1500, :1537) has unlockedEvidences.

FRONTEND:
- play_session_repository.dart:94-96 maps the GET list with InterrogationResult.fromJson (same model used for POST at :82). Confirmed.
- play_models.dart:346-359: InterrogationResult.fromJson reads interrogationId, suspectId, suspectName, question, answer, unlockedEvidences, createdAt. It does NOT read presentedEvidence. unlockedEvidences defaults to const [] when absent (:353-354).

NO CRASH: The only non-null-asserted fields in fromJson are interrogationId/suspectId via (j[...] as num).toInt() (:348-349), and InterrogationLogResponse always populates both (InterrogationLogQueryService.java:104-105). All other fields are null-safe with defaults. So parsing a GET log entry through this model does not throw, and unlockedEvidences simply comes back empty. Confirmed no-crash claim.

IMPACT CORRECTION (finding slightly overstates the UX loss): The finding says the history "silently loses the evidence-presented info and never shows it." The data IS dropped at parse time (presentedEvidence is ignored). BUT the consuming UI never displayed it regardless: the suspect-detail log card _LogCard (suspect_detail_screen.dart:339-369, fed by interrogationLogs() at :63) renders ONLY 'Q. {question}' and 'A. {answer}'. It does not render unlockedEvidences NOR presentedEvidence. So there is no visible regression in that screen today; the loss is purely at the model-fidelity level. Note also a separate, correctly-shaped InterrogationLog model exists (session_models.dart:18 has presentedEvidenceId) used by game_session_controller for the live chat replay, so the presented-evidence concept IS handled elsewhere in the live chat path - just not in the GET-log/InterrogationResult path.

Severity P2 is appropriate: no crash, no current visible UX bug, but a genuine data-shape coupling smell — one model reused for two divergent BE payloads. The finding's latent-trap note is valid: if InterrogationResult ever required a non-null presentedEvidence (or if the POST/GET were swapped), it would break. P2 stands; not P1 (no functional break) and not INVALID (the mismatch is genuinely present).
- **정정/주의**: No crash and no current visible UX regression: the suspect-detail log card (suspect_detail_screen.dart:339-369) only renders question/answer, so even before this change the presented-evidence info was never shown in that screen. The finding's "silently loses ... and never shows it" is technically true at the parse layer but implies a UX degradation that does not exist in the current UI. Also, presentedEvidence IS modeled elsewhere (session_models.dart:18 InterrogationLog.presentedEvidenceId, used by the live interrogation chat replay at interrogation_chat_screen.dart:71) — it is only absent from the InterrogationResult model used for the GET-log list. The real issue is model reuse across two divergent BE payloads (InterrogationResponse vs InterrogationLogResponse), a latent coupling risk, not a present-day data display bug.

### A27. Backend sends raw imageAssetKey + (often null) imageUrl; FE treats a non-http key as a bundled local asset, but the app bundles no assets, so images silently fall back

- **slug**: `asset-key-vs-url-contract`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Asset contract (remote vs bundled)
- **요약**: BE PlayEvidenceResponse carries both imageAssetKey (raw key, e.g. 'official/.../X.png') and imageUrl (resolved). ScenarioAssetUrlResolver.resolve returns null for non-absolute keys when clueroom.assets.public-base-url is empty — and application.yml defaults AWS_S3_PUBLIC_BASE_URL to empty. So locally imageUrl is null and only the raw key is sent. FE play_models.dart falls back to imageAssetKey when imageUrl is empty; AssetImageWidget then treats any non-'http' string as a local assets/ path via Image.asset. But pubspec.yaml declares NO assets: section (only fonts), so Image.asset always fails and renders the placeholder fallback.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/models/play_models.dart:170-174 (imageUrl else imageAssetKey); lib/components/asset_image_widget.dart:50-72 (non-http key → Image.asset local path); pubspec.yaml:27-36 (no assets: section)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/play/service/PlaySessionService.java:263,268-269 (sends raw imageAssetKey + resolved imageUrl); domain/scenario/support/ScenarioAssetUrlResolver.java:24-28 (returns null when publicBaseUrl blank); src/main/resources/application.yml:103-105 (AWS_S3_PUBLIC_BASE_URL default empty)
- **문서**: docs/CaseLab_AI_API_Spec.md §9.4 evidence fields; CLUEROOM_APP_FLOW_API_GUIDE.md asset fields
- **영향**: With no S3 base URL configured (the local/default state), every scenario/evidence/suspect image renders as a placeholder, because the FE tries to load a server asset key as a non-existent bundled asset. Graceful (fallback icon/initials), no crash — but the visual asset contract is effectively broken locally and the FE silently masks a misconfiguration. If the BE later sends a relative key with a base URL set, imageUrl will be a proper https URL and work; the risk is the raw-key fallback path masking the gap.
- **권장(코드변경 없음)**: Agree the contract: BE should send a fully-qualified imageUrl (set clueroom.assets.public-base-url / AWS_S3_PUBLIC_BASE_URL in non-prod too, or have the resolver return an absolute URL). FE should NOT treat a server-provided assetKey as a bundled local path (or should only do so for a known allowlist of bundled assets). Document that imageAssetKey is server-side-only and FE must rely on imageUrl.
- **검증 재현**: Independently reproduced the full chain from source in both repos.

BE side:
- PlaySessionService.java:263 sends raw imageAssetKey (evidence.getImageAssetKey()) and PlaySessionService.java:268-269,282 sends imageUrl resolved via scenarioAssetUrlResolver.resolve(evidence.getImageUrl(), evidence.getImageAssetKey()).
- ScenarioAssetUrlResolver.java:31-36: the two-arg resolve returns existingUrl if present, else falls to resolve(assetKey). resolve (lines 15-29): for a non-absolute key it returns null when publicBaseUrl is blank (lines 24-26).
- application.yml:105: clueroom.assets.public-base-url defaults to ${AWS_S3_PUBLIC_BASE_URL:} = empty. Constructor (ScenarioAssetUrlResolver.java:11-13) normalizes empty -> null, so hasText(publicBaseUrl) is false locally.
- Crucially confirmed evidence.getImageUrl() is null for imported scenarios: ScenarioYamlImportService.java:224-241 sets .imageAssetKey(evidenceYaml.imageAssetKey()) but NEVER sets .imageUrl(...). So locally resolve(null, key) -> resolve(key) -> null. imageUrl is null, only the raw key is sent. Confirmed.
- The key is always a relative path (e.g. official/seowolchae/v1/locations/LOC_DINING_ROOM.png per API spec line 1179). ScenarioYamlValidator.java:20 ASSET_KEY_PATTERN = ^[A-Za-z0-9._/-]+$ excludes ':' so a full http(s) URL can never be stored as imageAssetKey — reinforcing that the key is non-http.

FE side:
- play_models.dart:170-174: imageAssetKey = imageUrl if non-empty, else j['imageAssetKey']. With imageUrl null, the raw key is used. Confirmed.
- asset_image_widget.dart:48-73: null/empty -> fallback; startsWith('http') -> Image.network; else (line 63-72) Image.asset(key) as a local bundled asset, with errorBuilder -> _DefaultFallback (placeholder icon).
- pubspec.yaml:27-36 declares only fonts (Pretendard), NO assets: section. Verified assets/ dir contains only fonts/pretendard/ (no images). So Image.asset('official/...') always fails -> errorBuilder -> placeholder.
- This path is reached for evidence detail (evidence_detail_screen.dart:155,169,263), evidence thumbs, scene/location maps (scene_screen.dart), suspect portraits (suspect_card.dart:55, suspect_detail_screen.dart:105) and scenario covers — broad coverage as claimed.

Cited line numbers all accurate. The mismatch is REAL: in the local/default (no S3 base URL) state, imageUrl is null, FE falls back to the raw server key, treats it as a non-existent bundled asset, and every image silently renders as a placeholder.

Severity P2 is correct: failure mode is graceful (placeholder icon/initials, shimmer, no crash) and purely cosmetic, and it self-resolves once AWS_S3_PUBLIC_BASE_URL is configured (BE then emits a proper https imageUrl that hits the Image.network path). Not P1 — no data loss, no crash, no functional blocking; affects visual polish only and only in the unconfigured-asset-host environment.

### A28. BE Difficulty enum has 4 values (incl. NORMAL_PLUS) but FE models only 3; NORMAL_PLUS silently renders as '보통' and is unfilterable

- **slug**: `difficulty-enum-coverage-mismatch`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Data / enum drift
- **요약**: BE Difficulty enum = {EASY, NORMAL, NORMAL_PLUS, HARD}. FE Difficulty enum = {easy, medium, hard}. FE _difficultyFromApi maps anything not EASY/HARD to medium (so NORMAL and NORMAL_PLUS both become '보통'/medium), and _difficultyToApi can only emit EASY/NORMAL/HARD — it cannot request a NORMAL_PLUS filter.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/models/scenario.dart:3 (enum Difficulty { easy, medium, hard }); lib/repositories/scenario_repository.dart:147-157 (NORMAL_PLUS → medium; toApi only EASY/NORMAL/HARD)
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/scenario/enums/Difficulty.java:3-8 (EASY, NORMAL, NORMAL_PLUS, HARD)
- **문서**: docs/CaseLab_AI_API_Spec.md §3 Enum list (Difficulty)
- **영향**: A scenario authored with difficulty NORMAL_PLUS displays identically to NORMAL in the FE (loss of fidelity), and users cannot filter for NORMAL_PLUS. Degrades gracefully (no crash, defaults to medium), and impact depends on whether any official/MVP scenario actually uses NORMAL_PLUS. Lower risk.
- **권장(코드변경 없음)**: Decide whether NORMAL_PLUS is in MVP scope. If yes, add a 4th FE Difficulty value and map NORMAL_PLUS distinctly (label + filter). If not, drop NORMAL_PLUS from the BE enum/seed data or document it as never-emitted. Reconcile the spec §3 enum list with both sides.
- **검증 재현**: Reproduced all code-level claims directly from source.

BE: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java/com/startup/domain/scenario/enums/Difficulty.java:3-8 = {EASY, NORMAL, NORMAL_PLUS, HARD} (4 values).

FE: C:/Users/Russell/Desktop/Workspace/project-fe/lib/models/scenario.dart:3 = enum Difficulty { easy, medium, hard } (3 values).
- scenario_repository.dart:147-151 _difficultyFromApi: 'EASY'=>easy, 'HARD'=>hard, _ => medium. So both NORMAL and NORMAL_PLUS collapse to medium/'보통' (label at scenario.dart:44). Confirmed: NORMAL_PLUS renders identically to NORMAL.
- scenario_repository.dart:153-157 _difficultyToApi: only emits EASY/NORMAL/HARD; it is exhaustive over the 3-value FE enum and physically cannot produce NORMAL_PLUS. Confirmed unfilterable.

The 'unfilterable' claim is genuine because the BE DOES support difficulty filtering: ScenarioSearchCondition.java:14 exposes a Difficulty field (full enum incl. NORMAL_PLUS), and the FE wires the filter as a query param at scenario_repository.dart:59-60. So a user could never request NORMAL_PLUS from the app.

Severity stays P2 (graceful degradation, no crash) and is arguably borderline-low: a repo-wide search (grep NORMAL_PLUS across .java/.yaml/.yml/.sql/.json/.md) found NORMAL_PLUS ONLY in the enum definition — no seed, SQL, YAML, or test uses it. The official demo scenario uses difficulty: NORMAL (OFFICIAL_SCENARIO_DEMO_DAY.md:1183). So the impact is currently latent (zero live data triggers it).
- **정정/주의**: The doc reference in the finding is inaccurate and reverses the apparent root cause. docs/CaseLab_AI_API_Spec.md §3.4 (lines 148-154) lists the Difficulty enum as exactly {EASY, NORMAL, HARD} — it does NOT include NORMAL_PLUS. Therefore the FE actually CONFORMS to the documented API contract (3 values), and it is the BE enum (Difficulty.java) that has an undocumented 4th value diverging from the spec. The ERD (docs/CaseLab_AI_ERDCloud.sql:24) stores difficulty as VARCHAR(30) DEFAULT 'NORMAL' without enumerating allowed values, so it neither confirms nor denies NORMAL_PLUS. Recommended framing: this is a BE-vs-spec divergence (BE added NORMAL_PLUS without updating the API Spec) that incidentally creates an FE coverage gap — not a pure FE omission. Also note: currently no scenario data anywhere uses NORMAL_PLUS, so the defect is latent until/unless an author sets that difficulty.

### A29. All BE-side docs (and FE's own spec copy) describe an Android/Kotlin app; the FE is actually a Flutter/Dart project

- **slug**: `doc-drift-android-vs-flutter`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Docs/spec drift
- **요약**: Every reviewed doc identifies the frontend as 'Android App (Kotlin)' / 'Android 화면', but C:/Users/Russell/Desktop/Workspace/project-fe is a Flutter/Dart app (pubspec.yaml name: clueroom, sdk ^3.12.0, lib/**/*.dart, no iOS dir). The Android shell exists only as Flutter's generated android/ wrapper (MainActivity.java under xyz/clueroom, dev.flutter.flutter-gradle-plugin). The platform label, 'Kotlin', and the entire ANDROID_SCREEN_API_MAPPING.md framing are stale.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/pubspec.yaml:2-9 (name: clueroom, sdk ^3.12.0, flutter sdk); android/app/build.gradle.kts:4 (dev.flutter.flutter-gradle-plugin); android/app/src/main/java/xyz/clueroom/clueroom/MainActivity.java (Java, not Kotlin); no ios/ directory present
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/docs/ANDROID_SCREEN_API_MAPPING.md:1 'CaseLab AI - Android Screen API Mapping'; CaseLab_AI_API_Spec.md:4 '기준 플랫폼: Android App'; CLAUDE.md stack block 'Android App(Kotlin)'
- **문서**: CLAUDE.md header + §8; docs/CaseLab_AI_API_Spec.md:4; docs/ANDROID_SCREEN_API_MAPPING.md:1,54-61; docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:5,64; docs/CaseLab_AI_PRD.md:3
- **영향**: New contributors and reviewers are misled about the FE stack and toolchain. Android-specific guidance (e.g. ANDROID_SCREEN_API_MAPPING.md §12 'Android 개발 AI 규칙', MainActivity is Java not Kotlin) is mislabeled. Cross-platform concerns (FE config also targets iOS sim/desktop via localhost in api_config.dart:19-21, though no ios/ dir is committed) are not reflected in any doc. Purely documentation; no runtime break.
- **권장(코드변경 없음)**: Update CLAUDE.md (stack block 'Android App(Kotlin)'), CaseLab_AI_API_Spec.md:4 and project-fe/api-spec.md:4 ('기준 플랫폼: Android App'), CaseLab_AI_PRD.md, and ANDROID_SCREEN_API_MAPPING.md to state Flutter/Dart (Android target). Rename/retitle the Android-screen mapping doc to be platform-neutral or Flutter-screen oriented. Decide and document whether iOS is a supported target (FE config handles it; no ios/ dir is committed).
- **검증 재현**: Independently reproduced every cited line in both repos. FE is unambiguously Flutter/Dart, not native Android/Kotlin: project-fe/pubspec.yaml:2 (name: clueroom), :9 (sdk: ^3.12.0), :12-13 (flutter sdk dependency); 55 .dart files under lib/; no ios/ directory (Bash check confirmed); android/app/build.gradle.kts:4 applies id("dev.flutter.flutter-gradle-plugin") and :11/:22 namespace/applicationId xyz.clueroom.clueroom, marking the android/ dir as Flutter's generated wrapper; MainActivity.java (Java, not Kotlin) at android/app/src/main/java/xyz/clueroom/clueroom/MainActivity.java. BE docs uniformly frame the FE as Android: docs/ANDROID_SCREEN_API_MAPPING.md:1 title 'Android Screen API Mapping', :3 'Android 화면', :54 '§1 Android 공통 규칙', :560 '§12 Android 개발 AI 규칙'; docs/CaseLab_AI_API_Spec.md:4 '기준 플랫폼: Android App' and :12 'Android 추리게임 앱'; docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:5 'Android/Frontend' and :64 '로컬 Android Emulator'; docs/CaseLab_AI_PRD.md:3 'Android 기반'; CLAUDE.md header stack block 'Android App(Kotlin)' and §8 정채림 'Android UI'. The cross-platform claim is also accurate: lib/core/api/api_config.dart:6 comment and :19-21 code branch route non-Android platforms (iOS sim/desktop) to 'localhost' while only TargetPlatform.android uses '10.0.2.2'; no BE doc reflects this. Impact is purely documentation/onboarding drift with zero runtime break, so P2 is correct. Two precision notes that do not invalidate the finding: (1) 'Kotlin' as a literal label appears in CLAUDE.md's stack block, but ANDROID_SCREEN_API_MAPPING.md does not itself say 'Kotlin' — and the actual MainActivity is Java, so the 'Kotlin' label is doubly wrong (FE is Dart; even the generated shell's MainActivity is Java). (2) The api_config.dart explanatory comment is at line 6 while the cited '19-21' precisely covers the platform-branch logic, so the cite is accurate.
- **정정/주의**: Finding is accurate. Minor refinements: (a) The 'Kotlin' descriptor is specifically from CLAUDE.md's 'Android App(Kotlin)' stack block; ANDROID_SCREEN_API_MAPPING.md does not name a language. The Flutter wrapper's MainActivity is Java (not Kotlin), so 'Kotlin' is incorrect on two counts. (b) build.gradle.kts:40-44 does include a kotlin{} jvmTarget block (standard Flutter scaffolding), but no app Kotlin sources exist — reinforcing that any 'Kotlin' framing is stale boilerplate, not the actual stack. (c) api_config.dart platform comment is at line 6; the cited lines 19-21 correctly capture the localhost-vs-10.0.2.2 branch logic.

### A30. FE plans to register the FCM token via PATCH /api/users/me, but the BE exposes POST /api/device-tokens with a different body — and the FE call is an unimplemented stub

- **slug**: `fcm-token-registration-wrong-endpoint`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Process / transport contract drift
- **요약**: FE main.dart _registerFcmTokenWithBackend is a debugPrint-only stub whose TODO says it will call PATCH /api/users/me with fcmToken after login. The BE has no /api/users/me endpoint implemented (JWT-gated, post-MVP) and instead exposes POST /api/device-tokens with body {token, deviceType} → DeviceTokenResponse. The FE's intended contract does not match the BE's actual device-token contract.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/main.dart:67-70 (stub; TODO PATCH /api/users/me fcmToken); no device-tokens call in any repository
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project (per inventory) DeviceTokenController POST /api/device-tokens {token,deviceType} → DeviceTokenResponse; no /api/users/me implemented
- **문서**: docs/CaseLab_AI_API_Spec.md (PATCH /api/users/me table-only, JWT-gated post-MVP) vs BE DeviceTokenController POST /api/device-tokens
- **영향**: Push-notification token registration will not work as the FE doc/TODO describes. It is currently a no-op stub so nothing breaks at runtime, and device-token/FCM + JWT are post-MVP, so this is not an MVP blocker — but the documented integration path is wrong and will mislead whoever wires it.
- **권장(코드변경 없음)**: Align the FE on POST /api/device-tokens {token, deviceType:'ANDROID'} (the implemented endpoint) instead of PATCH /api/users/me {fcmToken}, or have the BE confirm the users/me approach for the post-JWT design and document one canonical path. Update the main.dart TODO accordingly.
- **검증 재현**: Independently reproduced all claims from source.

FE evidence (project-fe/lib/main.dart:67-70): _registerFcmTokenWithBackend is a debugPrint-only stub. Line 68 prints "백엔드에 FCM 토큰 등록 예정", line 69 TODO: "AuthService 로그인 후 PATCH /api/users/me 로 fcmToken 전송". It is called at lines 39 and 44 but performs no HTTP request. Grep across the entire FE repo for device-tokens/deviceToken/fcmToken/users/me returned only main.dart references plus api-spec.md doc lines — no repository call. FE has only two repositories (play_session_repository.dart, scenario_repository.dart), neither touching device tokens. So the FE call is genuinely an unimplemented stub.

BE evidence (DeviceTokenController.java:21,29): exposes POST /api/device-tokens. Request DeviceTokenRegisterRequest.java:13,18 = {token, deviceType}; Response DeviceTokenResponse.java:10,13 = {deviceTokenId, active}. This matches the finding's claimed BE contract.

BE /api/users/me: Grep of all Java source under src/main/java for users/me and /api/users returned NO matches; Glob for **/user/**/*Controller.java found no files. So there is no user controller at all — /api/users/me is unimplemented in code (not merely deferred). It appears only in the spec table (CaseLab_AI_API_Spec.md:224-225 and project-fe/api-spec.md:224-225) marked 인증 O / "인증 도입 후" (JWT-gated, post-MVP), table-only as claimed.

Mismatch is real: FE intended path PATCH /api/users/me {fcmToken} does not match BE actual POST /api/device-tokens {token, deviceType}.

Impact assessment accurate: no-op stub → no runtime breakage; FCM device-token + JWT are post-MVP → not an MVP blocker; but documented integration path is wrong and would mislead the implementer. P2 is appropriate. Minor precision notes (do not change conclusion): (a) BE deviceType is optional with ANDROID default applied in the service layer (DeviceTokenRegisterRequest.java:16 comment); (b) the BE API spec doc (CaseLab_AI_API_Spec.md) does not document /api/device-tokens or fcmToken at all — Grep for fcmToken/device-token there returned no matches — so DeviceTokenController exists in code beyond the documented spec, widening the doc-vs-implementation gap; (c) /api/users/me has never been implemented in any form, not just deferred.
- **정정/주의**: Refinements (core finding stands): (1) The BE API Spec doc itself does NOT document POST /api/device-tokens or any fcmToken field — DeviceTokenController exists in code only, so both the FE doc/TODO AND the BE spec are silent on the real device-token contract. (2) /api/users/me is not "implemented but JWT-gated" — there is no user controller/package in BE source at all; it is purely a spec-table placeholder. (3) BE deviceType is optional (defaults to ANDROID in the service layer per DeviceTokenRegisterRequest.java:16).

### A31. No API versioning on either side (no /v1/ path prefix or version header); both spec files share a fixed 'MVP v0.1' header despite having diverged

- **slug**: `no-api-versioning`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: API versioning
- **요약**: No endpoint on the BE or FE is versioned: all paths are /api/... with no /v1/ segment and no X-API-Version header (grep for /api/v1, /v1/, apiVersion across BE source returns nothing; FE ApiConfig.apiPrefix is '/api', unused). The two spec files both declare version 'MVP v0.1' verbatim yet have materially diverged (locations shape, active endpoint, error rows), so the version string conveys nothing about compatibility.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/lib/core/api/api_config.dart:26 (apiPrefix '/api', declared but unused); all repository paths are '/api/...' with no version segment
- **BE 증거**: C:/Users/Russell/Desktop/Workspace/start-up-project/src/main/java (no /api/v1, /v1/, or apiVersion match anywhere); controller bases are /api/play-sessions, /api/scenarios, etc.
- **문서**: docs/CaseLab_AI_API_Spec.md:3 ('버전: MVP v0.1') and project-fe/api-spec.md:3 (identical)
- **영향**: There is no contract-versioning mechanism to detect or manage breaking changes between FE and BE as the API evolves; today the only 'version' marker is duplicated and already inaccurate. Low immediate risk in MVP, but it removes the ability to gate FE/BE on a compatible contract revision and is why the spec drift went unnoticed.
- **권장(코드변경 없음)**: At minimum add a real, incremented version/date stamp to the canonical spec and bump it on every contract change; ideally adopt a single source-of-truth spec the FE imports. Consider a lightweight API version marker (header or /v1 prefix) before introducing the first breaking change. This is process/polish, not an MVP blocker.
- **검증 재현**: All cited facts independently reproduced from source.

FE evidence CONFIRMED:
- api_config.dart:26 declares `static const String apiPrefix = '/api';`. A repo-wide grep for `apiPrefix` across project-fe returns ONLY this definition line — it is declared but never referenced/used anywhere. Correct as stated.
- All FE repository paths use unversioned `/api/...` (scenario_repository.dart:55,76,88; play_session_repository.dart:15,22,32,41,48,56,61,74,91,110,123). No `/v1`.
- Grep for `X-API-Version|apiVersion|api_version|/v1` across project-fe/lib returns no matches — no version header on FE.

BE evidence CONFIRMED:
- Grep for `/api/v1|/v1/|apiVersion|X-API-Version|api-version` across src/main/java returns no matches.
- Every controller base is `/api/...` with no version segment: ScenarioController.java:17 `/api/scenarios`; PlaySessionController.java:17 `/api/play-sessions`; AiInterrogationController.java:27; AiDeductionController.java:23; AiScenarioValidationController.java:23 (`/api/ai/...` and `/api/scenarios/...`); plus notification/device-token/example controllers — all `/api/...`.
- No global path prefix: grep for `context-path|servlet.path|base-path` in src/main/resources returns no matches, so nothing injects a version at config level either.

Doc evidence CONFIRMED:
- CaseLab_AI_API_Spec.md:3 = `> 버전: MVP v0.1`. project-fe/api-spec.md:3 = identical `> 버전: MVP v0.1`.
- Spec drift between the two `MVP v0.1` files is real and material (diff confirms): BE spec table row 2 = `GET /api/play-sessions/active?scenarioId={scenarioId}` plus a full section 9.1.1 and P002 409 error block; the FE spec OMITS the /active row (table renumbered 2..13 instead of 2..14) and OMITS the P002 error section entirely. BE spec response includes `"canPlay": true` (line 484) which the FE spec omits (line 482). So the version string is duplicated verbatim yet conveys nothing about compatibility — accurate.
- Consistent corroboration: FE makes no call to `/active` or `/locations` (grep returns no matches), matching the dropped row in the FE spec.

Verdict: the gap is REAL exactly as described. Severity P2 is appropriate — no contract-versioning mechanism exists, but immediate risk is low for a pre-launch MVP built by one coordinated team with no external API consumers; nothing is broken at runtime today (FE/BE paths still match `/api/...`).
- **정정/주의**: Finding is accurate. One nuance on the impact statement: the claim that lack of versioning is "why the spec drift went unnoticed" is slightly overstated — the drift exists between two hand-maintained markdown files, and a /v1 path prefix or version header would not by itself have prevented two separately-edited spec docs from diverging (that needs a single source of truth / generated client, e.g. OpenAPI). The versioning gap and the spec drift are two independent symptoms of "no enforced contract," not strictly cause-and-effect. This does not change the P2 severity.

### A32. BE canonical spec and FE's api-spec.md clone diverged: /locations response is a wrapped object in BE but a flat array in the FE copy

- **slug**: `two-specs-diverged-locations-shape`
- **차원**: docsproc  |  **심각도**: P1 → **P2**  |  confirmed=True, confidence=high
- **분류**: Docs/spec drift
- **요약**: docs/CaseLab_AI_API_Spec.md §9.3 documents GET /play-sessions/{sessionId}/locations returning data as an OBJECT {sessionId, scenarioId, scenarioTitle, mapImageUrl, locations:[{locationId, locationCode, name, floor, description, imageAssetKey, imageUrl, mapX, mapY, totalEvidenceCount, unlockedEvidenceCount}]}. The actual BE DTO (PlayLocationsResponse) matches that object shape. The FE's stale copy project-fe/api-spec.md describes data as a flat ARRAY [{locationId, name, description, mapX, mapY, evidenceCount, evidences:[{evidenceId,title,isUnlocked}]}] with different fields (no mapImageUrl/locationCode/floor; adds nested evidences[]). The two spec files share the same version string 'MVP v0.1' yet contradict each other.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/api-spec.md:1104-1124 (data is a flat array with evidenceCount + nested evidences[]); FE locations not yet called live (timeline_screen/scene_screen sample-only)
- **BE 증거**: docs/CaseLab_AI_API_Spec.md:1164-1189 (object with mapImageUrl + locations[]); src/main/java/com/startup/domain/play/dto/PlayLocationsResponse.java (wrapped object per inventory)
- **문서**: docs/CaseLab_AI_API_Spec.md §9.3 (lines ~1158-1190) vs project-fe/api-spec.md ~1097-1125
- **영향**: If the FE implements its locations screen from its own committed spec, it will expect a JSON array and parse evidenceCount/evidences[] that the BE never sends, while the BE returns a Map — a runtime type-cast crash (data as List vs Map) plus missing mapImageUrl/locationCode/floor. Today the FE locations/scene screen is sample-only (no live call) so it does not crash yet, but the spec is a trap for the next implementer.
- **권장(코드변경 없음)**: Re-sync project-fe/api-spec.md to the BE canonical §9.3 wrapped-object shape, or delete the FE copy and have the FE consume the single canonical doc. Bump a version/date on both files so a drift is detectable. When the FE wires the locations screen, parse data as an object with a locations[] array.
- **검증 재현**: The divergence is REAL and independently reproduced from source. BE canonical spec docs/CaseLab_AI_API_Spec.md:1164-1189 documents GET /play-sessions/{sessionId}/locations returning data as an OBJECT {sessionId, scenarioId, scenarioTitle, mapImageUrl, locations:[...]} with location fields locationCode/floor/imageAssetKey/imageUrl/totalEvidenceCount/unlockedEvidenceCount. The actual BE DTO src/main/java/com/startup/domain/play/dto/PlayLocationsResponse.java:6-26 is a wrapped record matching that object shape verbatim (LocationDto fields identical). The FE's committed copy project-fe/api-spec.md:1103-1123 describes data as a flat ARRAY of {locationId,name,description,mapX,mapY,evidenceCount,evidences:[{evidenceId,title,isUnlocked}]} — no mapImageUrl/locationCode/floor, plus a nested evidences[] the BE never sends. Both files carry the identical version string '버전: MVP v0.1' at line 3 of each, confirming the contradictory-yet-same-version claim. The 'sample-only / no live call' claim is verified: the only locations consumer is scene_screen.dart:32 (const _locations hardcoded), gated by usesCl001SampleCaseData (line 101) with an explicit TODO comment at line 100 ('백엔드 locations 엔드포인트 구현 후 항상 서버 데이터로 교체한다'). No FE model/repository (play_models.dart, session_models.dart, scenario_repository.dart) parses the /locations response in either shape — the evidenceCount matches found are for the scenarios endpoint, not locations. Two minor inaccuracies in the finding, both non-material: (1) the FE is a Flutter/Dart app, not Kotlin/Android as org CLAUDE.md implies; the 'List vs Map cast crash' framing still holds for Dart (json['data'] as List vs as Map throws). (2) The finding cites 'timeline_screen/scene_screen sample-only' but timeline_screen.dart has NO locations reference at all — scene_screen.dart is the sole consumer. Severity adjusted P1->P2: zero live impact today (no FE code consumes the endpoint), it is a stale FE doc clone, and the next implementer would naturally validate against the canonical BE spec + DTO. It is a latent documentation-drift trap, not an active integration break, so P2 is the accurate severity.
- **정정/주의**: FE repo is Flutter/Dart (lib/screens/scene_screen.dart), not Kotlin/Android. The finding's 'timeline_screen' reference is incorrect — timeline_screen.dart contains no locations code; scene_screen.dart is the only (sample-only) consumer, using a const _locations list whose model (_Location: name/icon/clueCount/isIncident/imageAssetKey) matches NEITHER spec, so the next implementer must rewrite parsing regardless. No FE model parses the /locations response in either shape today, so there is no current runtime crash — the risk is purely a future trap from the stale committed FE spec. Spec divergence and identical 'MVP v0.1' version strings (line 3 of both files) are confirmed exactly as described.

### A33. FE's api-spec.md clone is an older revision missing GET /play-sessions/active, the 409/P002 active-session contract, canPlay, portraitImageUrl, and P010/P011 error rows

- **slug**: `two-specs-diverged-missing-active-and-409`
- **차원**: docsproc  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: Docs/spec drift
- **요약**: project-fe/api-spec.md is a stale fork of docs/CaseLab_AI_API_Spec.md. It lacks: (1) GET /api/play-sessions/active and §9.1.1 entirely (no hasActiveSession/activeSessionId block; play table renumbered 13 vs BE 14); (2) the POST /play-sessions 409 P002 body with details.activeSessionId; (3) canPlay on the scenario list item; (4) portraitImageUrl on suspect list/detail; (5) P010/P011 error rows. It also still carries isTrueEvent on timeline items that the BE spec dropped. Both files nonetheless claim version 'MVP v0.1'.
- **FE 증거**: C:/Users/Russell/Desktop/Workspace/project-fe/api-spec.md:281-296 (no active row), :482 (no canPlay), :1293-1294 (no portraitImageUrl), :2071-2072 (no P010/P011), :1326/1334 (isTrueEvent still present)
- **BE 증거**: docs/CaseLab_AI_API_Spec.md:1063-1119 (active endpoint + details.activeSessionId), :483-484 (canPlay), :1331/:1361 (portraitImageUrl), :2140-2141 (P010/P011)
- **문서**: docs/CaseLab_AI_API_Spec.md §9.1/§9.1.1/§17 vs project-fe/api-spec.md §9.1/§17
- **영향**: The FE team's reference contract is behind the implemented BE. Most acutely, the FE spec copy does not document the active-session recovery mechanism (active endpoint + 409 details) that the BE already ships, which is why the FE never implemented session recovery (see active-session finding). Drift compounds silently because nothing bumps the version.
- **권장(코드변경 없음)**: Make one canonical spec the single source of truth and have the FE consume it (or regenerate the FE copy). At minimum, sync the active endpoint, 409 P002 contract, canPlay, portraitImageUrl, P010/P011, and reconcile timeline isTrueEvent. Add a changelog/date header so future drift is visible.
- **검증 재현**: All five claimed gaps reproduce exactly from source.

(1) /active endpoint + §9.1.1: BE play table has 14 rows including GET /api/play-sessions/active as row 2 (CaseLab_AI_API_Spec.md:284); FE table has 13 rows with no active row (project-fe/api-spec.md:281-296). BE has full §9.1.1 block (BE:1083-1119) and the 409 P002 body with details.activeSessionId (BE:1061-1081, activeSessionId at :1077). FE §9.1 jumps straight from session-start Response (FE:1056) to §9.2 (FE:1061) — no 409 body and no §9.1.1. Grep confirms FE has §9.1 then §9.10 (FE:1030, :1344) with zero occurrences of 'active', '409', or 'P002' anywhere in the FE spec.

(2) canPlay: BE scenario list item has canPlay:true (BE:484); FE item ends at isBookmarked:false (FE:482) with no canPlay.

(3) portraitImageUrl: BE suspect list (BE:1331) and detail (BE:1361) both carry portraitImageUrl plus suspicionLevel; FE suspect list (FE:1265) and detail (FE:1287-1302) lack portraitImageUrl (FE detail also lacks suspicionLevel). Cited FE line :1293-1294 is the detail object's alibi/relatedEvidences boundary — correct location of the gap.

(4) P010/P011: BE error table has EVIDENCE_NOT_UNLOCKABLE/P010 (BE:2140) and SUSPECT_NOT_FOUND/P011 (BE:2141); FE table goes from EVIDENCE_LOCKED straight to HINT_NOT_AVAILABLE (FE:2071-2072), no P010/P011.

(5) isTrueEvent: FE timeline still has isTrueEvent:true (FE:1326, :1334); BE timeline dropped it (BE:1325, :1333).

Silent drift confirmed: both files declare identical '버전: MVP v0.1' at line 3.

Impact verified: FE lib/ never calls /active and never reads details.activeSessionId (grep for '/active|activeSessionId|details' in project-fe/lib returns no matches). game_session_controller.dart:99-105 handles 409 only by setting _sessionConflict and showing an error string; it does not recover via the BE contract. Its 'activeSession' references are a local SharedPreferences resume (_activeSessionKey/_readSavedSession/_tryResume, :67-128), not the BE endpoint.

Severity P2 is correct: this is spec-clone documentation drift with no direct runtime breakage. Its downstream consequence (FE lacking server-driven session recovery) is tracked separately. Minor imprecision in the finding: it says FE 'never implemented session recovery' — the FE has a partial local-only resume and surfaces the 409 conflict, but does not consume the documented server recovery mechanism. This does not change the verdict.
- **정정/주의**: Line-precision note: the FE 'no canPlay' gap is at FE:482 (item closes with isBookmarked:false). The FE suspect portraitImageUrl gap exists in BOTH the list (FE:1265) and detail (FE:1287-1302); the cited FE:1293-1294 points at the detail object, which is accurate. Impact wording slightly overstated: FE does have a local SharedPreferences-based resume (_tryResume) and shows a 409 conflict error, but genuinely does NOT implement the BE's server-side recovery (GET /play-sessions/active + 409 details.activeSessionId), confirmed by zero matches for '/active'/'activeSessionId' in project-fe/lib.

### A34. Difficulty enum value set differs: BE has NORMAL_PLUS, FE enum has only easy/medium/hard

- **slug**: `difficulty-enum-set-mismatch`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: enum-value mismatch
- **요약**: BE Difficulty = {EASY, NORMAL, NORMAL_PLUS, HARD}. FE Difficulty = {easy, medium, hard}. FE inbound mapping treats anything not EASY/HARD as medium, so a NORMAL_PLUS scenario is mislabeled '보통' (same as NORMAL). FE cannot send NORMAL_PLUS as a filter at all.
- **FE 증거**: lib/models/scenario.dart:3 enum Difficulty { easy, medium, hard }; lib/repositories/scenario_repository.dart:147-151 _difficultyFromApi: 'EASY'->easy, 'HARD'->hard, _ -> medium (NORMAL and NORMAL_PLUS both collapse to medium); :153-157 _difficultyToApi only emits EASY/NORMAL/HARD
- **BE 증거**: src/main/java/com/startup/domain/scenario/enums/Difficulty.java:3-8 (EASY, NORMAL, NORMAL_PLUS, HARD); bound in src/main/java/com/startup/domain/scenario/dto/ScenarioSearchCondition.java:14
- **문서**: docs/CaseLab_AI_API_Spec.md §3 Enums (Difficulty); FE-only difficulty enum noted in FE inventory casing section
- **영향**: Display-only mislabeling (no crash) for NORMAL_PLUS scenarios; difficulty filter cannot target NORMAL_PLUS. Low risk because the enum serializes as the constant name and the FE has a safe default.
- **권장(코드변경 없음)**: Add a NORMAL_PLUS (e.g. '보통+') case to the FE Difficulty enum and its from/to mappers, or confirm NORMAL_PLUS is unused for MVP official scenarios. No BE change needed.
- **검증 재현**: Independently reproduced all cited evidence from source.

BE: src/main/java/com/startup/domain/scenario/enums/Difficulty.java:3-8 defines {EASY, NORMAL, NORMAL_PLUS, HARD}. ScenarioSearchCondition.java:14 binds the full Difficulty enum for filtering. Scenario.java:57-59 persists it as @Enumerated(EnumType.STRING). ScenarioSummaryResponse.java:13 returns the raw Difficulty enum with no @JsonValue/custom serializer, so default Jackson serializes the constant name (e.g., "NORMAL_PLUS"). ScenarioDetailResponse also carries Difficulty.

FE: lib/models/scenario.dart:3 enum Difficulty { easy, medium, hard } — no NORMAL_PLUS analogue. lib/repositories/scenario_repository.dart:147-151 _difficultyFromApi maps 'EASY'->easy, 'HARD'->hard, wildcard _ -> medium with comment // NORMAL; therefore both NORMAL and NORMAL_PLUS collapse to Difficulty.medium. Lines 153-157 _difficultyToApi only emits EASY/NORMAL/HARD, so the FE can never send NORMAL_PLUS as a filter value. difficultyLabel (scenario.dart:42-46) renders medium as '보통', identical to a true NORMAL, confirming the mislabel. A repo-wide grep for NORMAL_PLUS/normalPlus across project-fe returned zero matches — the FE handles it nowhere else.

Impact confirmed as described: for a NORMAL_PLUS scenario the FE shows '보통' (same as NORMAL) with no crash (safe wildcard default), and the difficulty filter cannot target NORMAL_PLUS. Display/filter degradation only, no data loss or crash. P2 is appropriate.

One imprecision to note (does not change the verdict): the cited doc, docs/CaseLab_AI_API_Spec.md §3.4 (lines 148-154), lists Difficulty as only EASY/NORMAL/HARD and does NOT include NORMAL_PLUS. So the BE enum actually diverges from both the FE enum AND the API Spec — NORMAL_PLUS is an undocumented BE-only value. The finding's doc-ref framing slightly overstates spec coverage of the mismatch; the spec instead omits NORMAL_PLUS entirely.
- **정정/주의**: Doc reference is imprecise: docs/CaseLab_AI_API_Spec.md §3.4 (lines 148-154) documents Difficulty as exactly {EASY, NORMAL, HARD} and does NOT list NORMAL_PLUS. Thus NORMAL_PLUS is an undocumented backend-only enum value that diverges from BOTH the FE enum and the published API spec. Everything else in the finding (FE/BE line cites, mapping behavior, serialization-by-constant-name, impact) is accurate. The serialization-as-constant-name claim is also verified: ScenarioSummaryResponse.java:13 returns the raw enum with no @JsonValue and the entity uses @Enumerated(EnumType.STRING) (Scenario.java:57-59).

### A35. Evidence DTO field drift: FE reads categoryLabel (BE never sends it); BE sends oneLine (FE never reads it)

- **slug**: `evidence-categorylabel-vs-oneline`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: field-name drift / unused keys
- **요약**: FE PlayEvidence parses j['categoryLabel'] and renders it as a category chip, but PlayEvidenceResponse has no category/categoryLabel field, so it is always null. Conversely BE sends oneLine (one-line summary) which the FE never reads (it uses description). No crash either way due to FE null-tolerance.
- **FE 증거**: lib/models/play_models.dart:175 categoryLabel: j['categoryLabel'] as String?; consumed at lib/screens/evidence_image_viewer.dart:146-148 (renders categoryLabel.toUpperCase()); lib/screens/evidence_detail_screen.dart:158,266. No read of j['oneLine'] anywhere.
- **BE 증거**: src/main/java/com/startup/domain/play/dto/PlayEvidenceResponse.java:8-19 — has oneLine (line 11) but no category/categoryLabel field.
- **문서**: docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:187,651,676 documents evidence oneLine (not in api-spec §9.4); D-extra in docs inventory
- **영향**: Evidence category chip never appears (always null). BE oneLine payload is wasted. Cosmetic/degradation only.
- **권장(코드변경 없음)**: Align on one of: add a category field to PlayEvidenceResponse if the chip is wanted, or remove the FE categoryLabel UI; and decide whether the FE should consume oneLine (e.g. as the evidence subtitle) instead of description. No code edits here.
- **검증 재현**: Independently reproduced from source on both sides. BACKEND: PlayEvidenceResponse.java:8-19 declares oneLine (line 11) and has NO category/categoryLabel field; PlaySessionService.java:262,275 populates oneLine for unlocked evidence (it IS sent on the wire). A grep of the entire play domain (dto+service) for categoryLabel/category/evidenceType returns NO matches — the category data (Evidence.evidenceType, mapped from YAML 'category' at ScenarioYamlImportService.java:231) exists on the entity but is never serialized to this endpoint. FRONTEND: play_models.dart:175 parses categoryLabel: j['categoryLabel'] as String? (so always null from BE) and line 160 uses j['description'] for text; a repo-wide grep for 'oneLine' in the FE returns NO matches (FE never reads it). The categoryLabel flows _toEvidence (game_session_controller.dart:208) -> Evidence.categoryLabel (case.dart:43) -> EvidenceImageViewer, and is rendered as an uppercased chip at evidence_image_viewer.dart:146-148, gated by 'if (widget.categoryLabel != null)' (line 146) so it silently never appears, no crash. Passed in at evidence_detail_screen.dart:158,266. DOC: CLUEROOM_APP_FLOW_API_GUIDE.md lists oneLine (lines ~187,651,676) but a grep for categoryLabel/category in that doc returns NO matches; api-spec grep returns NO matches for oneLine either — confirming the D-extra classification (oneLine documented only in app-flow guide, categoryLabel undocumented). The mismatch is exactly as described: FE expects a categoryLabel chip the BE never sends; BE sends oneLine the FE never reads. Impact is purely cosmetic/degradation (missing chip + wasted payload), null-tolerant, no correctness or security impact. P2 is correct.
- **정정/주의**: Minor enrichment (not a defect in the finding): the BE DOES have category-equivalent data — Evidence.evidenceType (EvidenceType enum, Evidence.java; mapped from YAML 'category'). It is simply never added to PlayEvidenceResponse. So the fix path is to expose evidenceType as categoryLabel on the evidences endpoint, not to invent new data. The cited FE/BE line numbers all check out exactly.

### A36. FE Scenario model drops BE canPlay and isBookmarked fields; playability is hardcoded to ids {1,4,5}

- **slug**: `fe-ignores-canplay-isbookmarked`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: ignored-key / behavioral drift
- **요약**: Both ScenarioSummaryResponse and ScenarioDetailResponse send canPlay and isBookmarked, but the FE scenario mappers parse neither, and the FE Scenario model has no such fields. Playability is instead hardcoded to a static id allowlist {'1','4','5'} and bookmark state is kept only in shared_preferences.
- **FE 증거**: lib/repositories/scenario_repository.dart:95-114 (_fromSummaryJson) and :116-136 (_fromDetailJson) — no read of json['canPlay'] or json['isBookmarked']; lib/models/scenario.dart:7-40 has no canPlay/isBookmarked. lib/screens/scenario_detail_screen.dart:17 const _kPlayableIds = {'1','4','5'}; :33 _isPlayable = _kPlayableIds.contains(widget.scenario.id)
- **BE 증거**: src/main/java/com/startup/domain/scenario/dto/ScenarioSummaryResponse.java:21-22 (Boolean isBookmarked, Boolean canPlay); src/main/java/com/startup/domain/scenario/dto/ScenarioDetailResponse.java:31-32 (Boolean isBookmarked, Boolean canPlay)
- **문서**: docs/CaseLab_AI_API_Spec.md §6.1/§6.2 (list/detail include canPlay,isBookmarked); spec drift D4 in docs inventory (FE api-spec copy omits canPlay)
- **영향**: FE start-game gating and bookmark indicators ignore authoritative server state. A scenario the BE marks canPlay=true but whose id is outside {1,4,5} is shown as '준비 중' and disabled; conversely ids in the set are always enabled regardless of canPlay. Bookmark state diverges from server.
- **권장(코드변경 없음)**: Have the FE parse canPlay/isBookmarked into the Scenario model and drive playability/bookmark UI from them instead of the hardcoded id set. Note BE currently hardcodes isBookmarked=false in detail (ScenarioService.java:98), so server bookmark is itself a stub — sequence the change accordingly.
- **검증 재현**: All cited lines verified independently. FE: scenario_repository.dart:95-114 (_fromSummaryJson) and :116-136 (_fromDetailJson) read neither json['canPlay'] nor json['isBookmarked']; a full grep of project-fe/lib for canPlay|isBookmarked returned zero matches, confirming the fields are nowhere consumed. scenario.dart:7-40 has no such fields. scenario_detail_screen.dart:17 const _kPlayableIds = {'1','4','5'} and :33 _isPlayable = _kPlayableIds.contains(widget.scenario.id) gate the start button (:92,:795) and the "준비 중" disabled state (:759-760,:788). Bookmark is purely local: :41-53 use SharedPreferences key bookmark_<id> with no server round-trip. BE: ScenarioSummaryResponse.java:21-22 and ScenarioDetailResponse.java:31-32 declare both Booleans, and ScenarioService.java:68-69/:98-99 populate them. API Spec §6.1 (lines 483-484) and §6.2 (lines 532-533) include both fields. The contract mismatch and FE gap are real exactly as described.

Severity stays P2. Two precisions on the impact paragraph: (1) BE canPlay is currently a stub — ScenarioAccessService.java:12-14 hardcodes return true, so BE reports canPlay=true for ALL scenarios. Thus the real-world effect today is one-directional: BE-playable scenarios with id outside {1,4,5} are wrongly shown as 준비 중 and disabled (genuine, happening now). The converse case in the finding ("ids in set always enabled regardless of canPlay") cannot currently misfire because canPlay is never false; it is a latent bug that activates only once BE implements real access logic. (2) isBookmarked is also a stub (ScenarioService.java:69 passes false; :98 Boolean isBookmarked = false with a TODO at :96), so server bookmark state is not yet authoritative. Because MVP has no auth/authorization enforcement, this is a feature-correctness / contract-drift defect, not a security or data-integrity issue — consistent with P2, not higher.
- **정정/주의**: The finding is accurate but its impact framing slightly overstates current divergence. As of now the BE values are stubs, not authoritative state: ScenarioAccessService.canPlay() hardcodes return true (ScenarioAccessService.java:12-14) so every scenario reports canPlay=true, and isBookmarked is hardcoded false at both call sites (ScenarioService.java:69 and :98, with a TODO at :96). Therefore: (a) the "BE canPlay=true but id outside {1,4,5} shown as 준비 중" case is real and active today; (b) the "ids in set always enabled regardless of canPlay" case is currently impossible (canPlay is never false) and is a latent bug that only manifests once the BE implements real access logic; (c) the bookmark divergence is between FE-local prefs and a BE field that is itself not yet implemented. The core contract gap (FE never parses canPlay/isBookmarked; playability hardcoded to {1,4,5}) is fully confirmed.

### A37. FE collects a mandatory '종합 추리 설명' that has no field in FinalDeductionRequest and is never transmitted

- **slug**: `final-deduction-summary-not-sent`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: missing-key / request-body gap
- **요약**: The submit screen has a required free-text '종합 추리 설명' (_summaryCtrl) that gates submission (must be >= minSummaryLen), but the value is never placed in the final-deduction request body, and FinalDeductionRequest has no field to receive it. The user's comprehensive reasoning is silently discarded.
- **FE 증거**: lib/screens/submit_screen.dart:31 final _summaryCtrl; :70-71 _Requirement('종합 추리를 ...자 이상 입력', _summary.length >= _minSummaryLen) (mandatory); :124-127 body builds only motiveText/methodText/coverUpText/selectedEvidenceIds — _summary is omitted. Repository lib/repositories/play_session_repository.dart:111-117 sends no summary/reasoning key.
- **BE 증거**: src/main/java/com/startup/domain/ai/dto/FinalDeductionRequest.java:11-28 — fields are selectedCulpritId, motiveText, methodText, coverUpText, selectedEvidenceIds only; no reasoningText/summaryText.
- **문서**: project-fe/docs/backend-requests_2026-06-02_v1.md:60-66 (R4); docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md:293-294, 1138-1139 (states no reasoningText field exists)
- **영향**: No crash (BE simply never receives it), but a mandatory player input is dead weight and scoring cannot consider it. Product expectation mismatch.
- **권장(코드변경 없음)**: Decide per docs R4: either add reasoningText/summaryText to FinalDeductionRequest (and have the FE send _summary), or formally confirm it is display-only and relax the FE's mandatory requirement. No code change requested here — flag the open decision.
- **검증 재현**: Independently reproduced from source. FE: submit_screen.dart:31 declares _summaryCtrl; :36 _minSummaryLen=10; :70-71 adds _Requirement('종합 추리를 ...자 이상 입력', _summary.length >= _minSummaryLen) to _requirements, which gates _allRequirementsMet (every((r)=>r.met)) and thus _canSubmit (:76-77) — so it IS a mandatory submission gate. :121-128 the submitFinalDeduction call passes only selectedCulpritId/motiveText/methodText/coverUpText/selectedEvidenceIds; _summary is omitted. Grep confirms _summary* appears ONLY in submit_screen.dart (lifecycle dispose :58, gate :71, text field :276) and never reaches the repository/network layer. play_session_repository.dart:109-118 builds a body with exactly those 5 keys, no summary/reasoning key. BE: FinalDeductionRequest.java:11-28 has fields selectedCulpritId, motiveText, methodText, coverUpText, selectedEvidenceIds only — no reasoningText/summaryText. AiDeductionController.java:30-33 binds only @RequestBody FinalDeductionRequest (no extra summary param). AiDeductionScorer.java has zero summary/reasoning/종합 references, so scoring cannot consider it. Doc refs confirmed: backend-requests_2026-06-02_v1.md:60-66 raises this exact gap as P2 with A/B decision pending; CLUEROOM_APP_FLOW_API_GUIDE.md:293-294 and 1138-1139 both explicitly state no reasoningText/comprehensive-reasoning field exists and instruct keeping the input FE-display-only or splitting into motive/method/coverup. Net effect: no crash/error — BE simply never receives the value — but a mandatory player input is silently discarded and scoring cannot use it. Product-expectation/data-loss mismatch, not a functional break. P2 is correct: it does not block the flow or cause errors, the FE team itself classified it P2, and the line citations are all accurate.

### A38. BE LocalDateTime fields serialize as ISO-8601 with no timezone offset; FE parses as local time

- **slug**: `localdatetime-no-timezone`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: datetime format
- **요약**: All BE timestamp fields are java.time.LocalDateTime with no custom Jackson config, so under Spring Boot 4 defaults they serialize as e.g. '2026-06-04T12:34:56.789' with no zone/offset. FE uses DateTime.tryParse which interprets a zoneless ISO string as local device time. startedAt/createdAt/usedAt/submittedAt are affected.
- **FE 증거**: lib/models/play_models.dart:516-519 _parseDate uses DateTime.tryParse(v) (no zone handling); applied to startedAt (:56), createdAt (:358), usedAt (:321), submittedAt (:388).
- **BE 증거**: DTOs use LocalDateTime: PlaySessionCreateResponse.java:13 (startedAt), InterrogationResponse.java:29 (createdAt), HintUseResponse.java:15 (usedAt), FinalDeductionResponse.java:25 (submittedAt). No ObjectMapper/JsonFormat customization found (grep of common/* and application.yml empty for date config).
- **문서**: docs/CaseLab_AI_API_Spec.md §2 common response wrapper (no explicit datetime format spec)
- **영향**: Low: these timestamps are display-only in the FE and parsing does not crash. If server and device timezones differ, displayed times could be off; elapsed-time UI uses a client-side timer so is unaffected.
- **권장(코드변경 없음)**: If timezone-correct display matters, standardize on an offset/UTC format (e.g. switch BE to Instant/OffsetDateTime or add @JsonFormat with a zone, and have FE parse as UTC). Otherwise document that timestamps are server-local. No edits requested.
- **검증 재현**: Independently reproduced from source. BE: all four cited DTOs use java.time.LocalDateTime — PlaySessionCreateResponse.java:13 (startedAt), InterrogationResponse.java:29 (createdAt), HintUseResponse.java:15 (usedAt), FinalDeductionResponse.java:25 (submittedAt). No @JsonFormat on any DTO and no spring.jackson date-format/serialization config in any application*.yml (grep of src/main/resources returned only JDBC serverTimezone and Redis timeout — nothing touching HTTP date serialization). Spring Boot 4.0.6 (build.gradle:3) uses Jackson 3 (tools.jackson), whose default WRITE_DATES_AS_TIMESTAMPS=false serializes LocalDateTime as zoneless ISO-8601 (e.g. 2026-06-04T12:34:56.789). Confirmed. FE: _parseDate at play_models.dart:516-518 calls DateTime.tryParse(v) with no zone handling; applied to startedAt (:56), usedAt (:321), createdAt (:358), submittedAt (:388). Dart's DateTime.tryParse interprets a zoneless ISO string as local device time — accurate as described. So the mismatch (zoneless emit + local parse) is real and a genuine latent correctness gap. Severity P2 is appropriate: no crash, parsing succeeds, elapsed-time UI is a client-side timer (independently verified to be unaffected), and a device with timezone != server would only show display offsets.
- **정정/주의**: Two precisions on impact, both strengthening the "low severity" conclusion. (1) The four _parseDate-parsed fields are currently NOT rendered anywhere in the FE UI — grep for startedAt/usedAt/createdAt/submittedAt found the play_models.dart fields are parsed but have no UI consumer. The only timestamp actually displayed (my_records_screen.dart:259, session.startedAt via _formatDate) comes from a DIFFERENT model, scenario.dart PlaySession.startedAt, which is mock/sample-data backed (sample_scenarios.dart:124-157), not the BE response. So real user-facing impact today is effectively nil; this is latent. (2) The BE JDBC URL sets serverTimezone=Asia/Seoul (application-docker.yml:5, application-prod.yml:5), so the stored/serialized wall-clock is KST. The actual error magnitude when these fields are eventually displayed would be (device timezone offset minus KST). All claimed file:line citations are accurate.

### A39. FE repository doc/comment still references SESSION_ALREADY_EXISTS while BE returns error code P002 on 409

- **slug**: `session-already-exists-stale-code-comment`
- **차원**: dto  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: error-code drift (doc/comment only)
- **요약**: createSession's doc comment promises a 'SESSION_ALREADY_EXISTS' (409) ApiException, but the BE returns code P002 with HTTP 409. The FE conflict handling keys off e.status == 409 (not the code string), so runtime behavior is correct; only the stale comment is misleading.
- **FE 증거**: lib/repositories/play_session_repository.dart:12 comment '이미 진행 중이면 `SESSION_ALREADY_EXISTS`(409) ApiException'; actual handling lib/controllers/game_session_controller.dart:100-105 uses if (e.status == 409). ApiClient takes status from error.status body field (lib/core/api/api_client.dart:142).
- **BE 증거**: PlayErrorCode P002 (이미 진행 중인 플레이 세션이 존재합니다., HTTP 409) per BE inventory PlayErrorCode.java:13-23; ErrorResponse.code carries 'P002' not 'SESSION_ALREADY_EXISTS'.
- **문서**: project-fe/docs/backend-requests_2026-06-02_v1.md:31 (R1b error-code unification); docs/CaseLab_AI_API_Spec.md:1073 (P002)
- **영향**: No runtime break (FE matches on HTTP status). Risk is future-FE code that switches on error.code would never match. Documentation drift.
- **권장(코드변경 없음)**: Update the FE comment to reference P002 (or the canonical code). Confirm the BE never emits the legacy string SESSION_ALREADY_EXISTS anywhere (docs R1b).
- **검증 재현**: Independently reproduced every claim from source.

FE evidence:
- play_session_repository.dart:12 — comment verbatim: "게임 세션 시작. 이미 진행 중이면 `SESSION_ALREADY_EXISTS`(409) ApiException." Confirmed.
- game_session_controller.dart:100-105 — the only conflict handling keys off `if (e.status == 409)` (sets _sessionConflict + Korean message), NOT the code string. Confirmed.
- api_client.dart:138-143 — on failure, ApiException.status = (error['status'] as num?)?.toInt() ?? res.statusCode. So status comes from the body 'status' field. Confirmed.
- Grep over project-fe/lib for SESSION_ALREADY_EXISTS / P002 / `.code` comparisons: the ONLY SESSION_ALREADY_EXISTS occurrence is the doc comment at line 12. No FE branch switches on error.code for this case (other .code hits are scenario.code and the ApiException ctor field). So runtime is correct — no break.

BE evidence:
- PlayErrorCode.java:14 — SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "이미 진행 중인 플레이 세션이 존재합니다."). SESSION_ALREADY_EXISTS is the Java enum constant name; the serialized code value is "P002", status 409. Confirmed.
- GlobalExceptionHandler.java:184,186 — buildErrorResponse sets .status(errorCode.getStatus().value()) => 409 and .code(errorCode.getCode()) => "P002". The string "SESSION_ALREADY_EXISTS" is never placed in the response body. Confirmed.
- PlaySessionService.java:129-135 — sessionAlreadyExists throws PlayException(SESSION_ALREADY_EXISTS, ..., details{activeSessionId}). Confirmed it is the source of the 409.

Doc refs:
- project-fe/docs/backend-requests_2026-06-02_v1.md:31 — "에러 코드 불일치 — 문서/주석은 SESSION_ALREADY_EXISTS, 실제 응답은 P002. 하나로 통일 부탁드립니다." (also line 20-21 shows the actual body {"code":"P002",...,"status":409}). Confirmed.
- docs/CaseLab_AI_API_Spec.md:1073 — "code": "P002", status 409. Confirmed.

Conclusion: The mismatch is REAL exactly as described — the FE repository doc comment promises an error code string ("SESSION_ALREADY_EXISTS") that the BE never emits (BE emits "P002"), while the HTTP status (409) is consistent. No runtime malfunction because FE matches on e.status == 409, never on error.code. This is documentation/comment drift with a latent risk only for hypothetical future FE code that would switch on error.code. P2 is appropriate: real, low-impact, no current break.

### A40. Envelope (success/data/error), error code/message parsing, and PageResponse pagination contract match between BE and FE

- **slug**: `envelope-pagination-contract-ok`
- **차원**: envelope  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: response envelope / pagination (positive confirmation)
- **요약**: Verified the core dimension is sound. (1) Envelope: BE ApiResponse is {success, data?, error?} with @JsonInclude(NON_NULL) (ApiResponse.java:7-31); FE requires `map['success'] == true` strictly and returns raw `map['data']` (api_client.dart:132-134), matching success(data)/empty()/fail() shapes. Empty success {success:true} → data null → FE ignores (e.g. abandon, play_session_repository.dart:60-62). (2) Errors: BE ErrorResponse exposes code/message/status (ErrorResponse.java:15-23, populated GlobalExceptionHandler.java:181-191); FE reads error.code/message/status (api_client.dart:137-143) into ApiException — field names align exactly. (3) Pagination: BE PageResponse is {content, page, size, totalPages, totalElements, hasNext} (PageResponse.java:9-16); FE Page.fromJson reads the identical key set with null-tolerant defaults (api_client.dart:29-44). page is 0-based in code despite the BE comment (PageResponse.java:8 vs :18-26); FE only consumes content (scenario_repository.dart:66-70,79-83) so the 0/1-base ambiguity is harmless. (4) SliceResponse/CursorSliceResponse are declared but unused on BE and unparsed on FE — no cursor/nextCursor contract is in play. (5) Charset: FE decodes utf8.decode(res.bodyBytes) regardless of response Content-Type (api_client.dart:123), and BE has no produces/charset override, so charset mismatch cannot break parsing. The ST-46 change correctly surfaces scoring failure as 500 AI014 (AiDeductionScorer.java:147-153) which the FE's >=500 branch handles; SOLUTION_NOT_FOUND stays 404 AI011 via the BusinessException passthrough (AiDeductionScorer.java:136-140, validateSolutionForScoring :331-342).
- **FE 증거**: project-fe/lib/core/api/api_client.dart:29-44 (Page.fromJson), :120-150 (_parse success/data/error); project-fe/lib/core/api/api_exception.dart:5-25; project-fe/lib/repositories/scenario_repository.dart:66-70
- **BE 증거**: src/main/java/com/startup/common/dto/ApiResponse.java:7-31; src/main/java/com/startup/common/dto/ErrorResponse.java:15-23; src/main/java/com/startup/common/dto/PageResponse.java:9-26; src/main/java/com/startup/domain/ai/service/AiDeductionScorer.java:136-153,331-342
- **문서**: docs/CaseLab_AI_API_Spec.md §2 (common response wrapper) / §6.1 (page wrapper); CLAUDE.local.md ST-46 note (SCORING_FAILED 500); inventory §2 envelope shapes
- **영향**: No defect — documents that the primary envelope/error/pagination contract is correctly wired so reviewers do not re-flag it. Provided as a P2 confirmation, not an issue.
- **권장(코드변경 없음)**: No action. Keep page semantics consistent if list pagination navigation is ever added on the FE (currently only content is read). Fix the PageResponse.java:8 comment vs code 0-base disagreement opportunistically (BE-internal, non-blocking).
- **검증 재현**: Independently reproduced every sub-claim from source. (1) Envelope: ApiResponse.java:8-12 is exactly {success, data, error} with @JsonInclude(NON_NULL) at :7; factories success(data)/empty()/fail() at :13-30. api_client.dart:132 strictly checks map['success']==true and returns raw map['data'] at :134. Empty success (data null) is ignored by FE (play_session_repository.dart:60-62 awaits abandon without using result). (2) Errors: ErrorResponse.java:15-22 exposes timestamp/status/error/code/message/path/details; GlobalExceptionHandler.buildErrorResponse :181-190 populates code/message/status. api_client.dart:137-143 reads error.code/message/status into ApiException (api_exception.dart:5-19). AiException extends BusinessException (AiException.java:5), so it routes through handleBusinessException (GlobalExceptionHandler.java:30-40) emitting errorCode.getStatus(). (3) Pagination: PageResponse.java:9-15 is {content, page, size, totalPages, totalElements, hasNext}; Page.fromJson (api_client.dart:33-43) reads the identical keys with null-tolerant defaults. The 0/1-base ambiguity is real (comment PageResponse.java:8 says 1-base, but from() at :18-24 passes springPage.getNumber() = 0-based) and is harmless because FE consumes only .content (scenario_repository.dart:66-70, 79-83). (4) SliceResponse/CursorSliceResponse exist (SliceResponse.java, CursorSliceResponse.java) but grep across src/main/java shows references only inside their own definition files — unused on BE; no slice/cursor/nextCursor parsing exists in FE lib (only unrelated cursorColor UI matches). (5) Charset: api_client.dart:123 always utf8.decode(res.bodyBytes); BE sets no produces/charset, so charset cannot break parsing. ST-46: SCORING_FAILED = 500/AI014 (AiErrorCode.java:24) thrown at AiDeductionScorer.java:147-152; SOLUTION_NOT_FOUND = 404/AI011 (AiErrorCode.java:21) thrown in validateSolutionForScoring (:331-342) and passed through as BusinessException at :136-140; FE handles >=500 at submit_screen.dart:138. No defect — this is an accurate confirmation that the envelope/error/pagination contract is correctly wired. P2 confirmation is the right classification (not an issue requiring fix).
- **정정/주의**: Minor precision note: the FE's ">=500 branch" cited as handling SCORING_FAILED lives in submit_screen.dart:138 (`(e.status ?? 0) >= 500`), not in api_client.dart; api_client.dart:142 only propagates error.status into ApiException. This does not change the conclusion. Also, the finding's "page is 0-based in code despite the BE comment" is correct: PageResponse.from() (PageResponse.java:18-24) passes springPage.getNumber() unchanged (0-based), contradicting the 1-base conversion claim in the comment at line 8 — harmless given FE uses only content.

### A41. FE final-deduction handler only special-cases >=500; a 409 AI010 (already submitted) leaves the user stuck with no path to the result

- **slug**: `fe-final-deduction-409-stuck`
- **차원**: envelope  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: errors / final-deduction status handling
- **요약**: submit_screen._onSubmit branches only on `>=500` for the retry/keep-input message and otherwise shows a generic `'제출 실패: ${e.message}'` snackbar, and it navigates to ResultScreen ONLY on the success path (submit_screen.dart:131-141). The BE final-deduction can legitimately return 409 AI010 FINAL_DEDUCTION_ALREADY_SUBMITTED (AiErrorCode.java:20; thrown via DataIntegrityViolationException catch in AiDeductionScorer.java:141-146, and contextLoader.ensureNotSubmitted at :82). This 409 commonly happens when a first submit actually persisted but the FE saw a network blip and retried: the second call returns 409, the FE shows '제출 실패: 이미 최종 추리를 제출했습니다', and the user is dead-ended even though a valid result exists at GET /result.
- **FE 증거**: project-fe/lib/screens/submit_screen.dart:131-141 (success-only navigation; only `>=500` special-cased, else generic '제출 실패')
- **BE 증거**: src/main/java/com/startup/domain/ai/error/AiErrorCode.java:20 (AI010 CONFLICT 409); src/main/java/com/startup/domain/ai/service/AiDeductionScorer.java:82,141-146 (ensureNotSubmitted + DataIntegrityViolation → FINAL_DEDUCTION_ALREADY_SUBMITTED)
- **문서**: docs/CaseLab_AI_API_Spec.md §11.1 (final-deduction); error code AI010
- **영향**: Edge-case but reachable: after a flaky submit the player cannot reach their already-computed result from the submit screen. Not a crash; degrades the end-of-game flow. MVP-core (final deduction is in the 1st MVP play loop).
- **권장(코드변경 없음)**: On 409 (code AI010 / FINAL_DEDUCTION_ALREADY_SUBMITTED) in submit_screen, treat as 'already submitted' and navigate to ResultScreen(sessionId) instead of showing only an error snackbar. BE behavior is correct; FE handling needs to branch on the 409/AI010 code.
- **검증 재현**: Independently reproduced from source in both repos.

BE: AiErrorCode.java:20 defines FINAL_DEDUCTION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "AI010") = genuine 409. AiDeductionScorer.java:82 calls contextLoader.ensureNotSubmitted(sessionId); DeductionContextLoader.java:24-27 throws AiException(FINAL_DEDUCTION_ALREADY_SUBMITTED) when finalDeductionRepository.existsByPlaySessionId(sessionId) is true — this is the deterministic 409 path for a retry after a first submit already persisted. AiDeductionScorer.java:141-146 is a secondary concurrent-race path (DataIntegrityViolationException -> same AI010). Both yield HTTP 409. Documented at docs/CaseLab_AI_API_Spec.md:2143.

FE: api_client.dart:138-143 parses the error body into ApiException(code="AI010", status=409 via error['status'] or res.statusCode). submit_screen.dart:138 sets isServerError = (e.status ?? 0) >= 500; a 409 is NOT >=500, so line 141 shows the generic '제출 실패: ${e.message}' snackbar (= '제출 실패: 이미 최종 추리를 제출했습니다'). Navigation to ResultScreen occurs ONLY on the success path (submit_screen.dart:130-136); there is no 409/AI010 branch that routes the user to the result. result_screen.dart:107-114 confirms the result is independently fetchable via GET /result, so a valid computed result genuinely exists while the user is dead-ended on the submit screen.

The mismatch/gap is real exactly as described: FE only special-cases >=500, and a legitimate 409 AI010 leaves the user with only a snackbar and no navigation to their already-computed result.

Severity: P2 is correct. Not a crash, no data loss; degrades the end-of-game flow only under a specific timing (first submit persists, response lost/network blip, user retries). Final deduction is in the 1st MVP loop, but the trigger is an edge case, so it does not rise to P1.
- **정정/주의**: Two minor precisions (do not change the verdict): (1) FE line range is slightly off — the success-navigation block is submit_screen.dart:130-136 and the only-`>=500` special-casing is :137-142, not :131-141. (2) The primary/most-common 409 trigger is contextLoader.ensureNotSubmitted -> existsByPlaySessionId (DeductionContextLoader.java:24-27), invoked at AiDeductionScorer.java:82; the DataIntegrityViolationException catch at :141-146 is the narrower concurrent-double-submit race path. The finding cites both, which is accurate, but the retry-after-persist scenario it describes is handled by the existsBy check rather than the DIV catch.

### A42. FE derives ApiException.status from error.status in the JSON body, not the HTTP status line — all 5xx/409 branching depends on the BE echoing status inside the error object

- **slug**: `fe-status-from-body-not-http-line`
- **차원**: envelope  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: response envelope / HTTP status mapping
- **요약**: api_client._parse takes the status from `error['status']` in the body and only falls back to the real HTTP status code when error.status is absent: `status: (error['status'] as num?)?.toInt() ?? res.statusCode` (api_client.dart:142). The FE's two status-sensitive branches — submit_screen treating `(e.status ?? 0) >= 500` as a retryable scoring error (submit_screen.dart:138) and game_session_controller treating `e.status == 409` as a session conflict (game_session_controller.dart:100) — therefore trust the body field. This currently works because GlobalExceptionHandler.buildErrorResponse always sets `error.status = errorCode.getStatus().value()` (GlobalExceptionHandler.java:184) consistently with the ResponseEntity HTTP status. The risk is that any error NOT routed through this handler (e.g. a gateway/proxy 502/504, a Spring Boot default error response that lacks a nested `error` object, or an LB 500 HTML page) will not carry error.status, and if the body is not the standard envelope shape the FE collapses it to a generic UNKNOWN with the HTTP status (api_client.dart:145-149) — losing the 5xx-retry affordance the ST-46 change depends on.
- **FE 증거**: project-fe/lib/core/api/api_client.dart:142 (`status: (error['status'] as num?)?.toInt() ?? res.statusCode`); project-fe/lib/screens/submit_screen.dart:138 (`(e.status ?? 0) >= 500`); project-fe/lib/controllers/game_session_controller.dart:100 (`e.status == 409`)
- **BE 증거**: src/main/java/com/startup/common/error/GlobalExceptionHandler.java:184 (`.status(errorCode.getStatus().value())`) consistent with ResponseEntity.status(errorCode.getStatus()) at :37-39
- **문서**: project-fe/docs/backend-requests_2026-06-02_v1.md (ST-46 500-error scoring context); CLAUDE.local.md ST-46 note (final-deduction scoring → DB SolutionInfo SoT, 500 on failure)
- **영향**: As long as every error flows through GlobalExceptionHandler the behavior is correct. But for infrastructure-level 5xx (AI004/AI005 are 503/504 from the handler and are fine; the concern is non-app 502/504 from a reverse proxy or AI gateway), the FE may misclassify. Low likelihood in MVP single-service deploy, but it is a latent coupling between FE branching and BE body content rather than the transport status line.
- **권장(코드변경 없음)**: Prefer the HTTP status line as the source of truth for ApiException.status (use res.statusCode, optionally overriding with error.status only when present and equal). Keep error.code for business branching. No BE change required; BE already mirrors status correctly.
- **검증 재현**: All cited lines reproduce exactly from source. FE: api_client.dart:142 prefers the body field — `status: (error['status'] as num?)?.toInt() ?? res.statusCode`; submit_screen.dart:138 — `final isServerError = (e.status ?? 0) >= 500`; game_session_controller.dart:100 — `if (e.status == 409)`. ApiException.status is `int?` (api_exception.dart:9,19). BE: GlobalExceptionHandler.java:184 sets `.status(errorCode.getStatus().value())` inside buildErrorResponse, and every handler sets the ResponseEntity HTTP status from the same ErrorCode (e.g. lines 37-39 ResponseEntity.status(errorCode.getStatus())), so body status == transport status for all app errors. AI004=503 (AiErrorCode.java:14) and AI005=504 (AiErrorCode.java:15) are BusinessExceptions routed through this handler, so they are fine, as the finding states. I independently confirmed the latent-gap premise: there is NO custom ErrorController and NO server.error.* override in src/main/resources, so Spring's default BasicErrorController (and any reverse-proxy/LB) produces non-envelope bodies that lack a nested `error` Map. The coupling between FE branching and BE body content is therefore REAL as described, and currently benign only because all app errors flow through GlobalExceptionHandler in the MVP single-service deploy. P2 is correct: latent coupling, low likelihood, not a present defect.
- **정정/주의**: The mechanism and evidence are accurate, but the IMPACT is slightly overstated for one sub-case and should be split into two paths: (1) Non-app error returns a JSON body that is not the standard envelope (e.g. Spring whitelabel `{timestamp,status,error,message,path}` where `error` is a String reason phrase, or any JSON Map lacking a nested `error` object): FE `_parse` falls to api_client.dart:145-149 and builds `ApiException(code:'UNKNOWN', status: res.statusCode)`. Here `res.statusCode` IS the real HTTP 5xx, so submit_screen.dart:138 `(e.status ?? 0) >= 500` STILL fires correctly — the 5xx-retry affordance is PRESERVED; only `code` degrades to UNKNOWN. The `?? res.statusCode` fallback the finding cites actually rescues this case. (2) The 5xx-retry affordance is only genuinely LOST when the body is non-JSON (e.g. a reverse-proxy/LB 502/504 HTML page): jsonDecode throws FormatException → `ApiException.parse()` (api_client.dart:128-130) which has `status: null` (api_exception.dart:40-43), so `(e.status ?? 0) >= 500` → `0 >= 500` → false. That path does not even reach line 142, so the breakage is via the parse fallback, not via the body-status preference the finding headlines. The 409 conflict branch (game_session_controller.dart:100) is not realistically affected since infra/default errors are never legitimately 409. Net: confirmed latent coupling, P2; the headline mechanism (status from body, not HTTP line) is correct, but the concrete harm is narrower than stated — limited to non-JSON proxy 5xx, where the loss comes from the parse-fallback null status rather than from line 142.

### A43. GET /interrogations returns InterrogationLogResponse (questionType + presentedEvidence) but FE parses it as InterrogationResult (expects unlockedEvidences) — silent field loss, not a crash

- **slug**: `interrogation-log-dto-shape-mismatch`
- **차원**: envelope  |  **심각도**: P2 → **P2**  |  confirmed=True, confidence=high
- **분류**: response envelope / DTO shape drift
- **요약**: The list endpoint GET /api/play-sessions/{sessionId}/interrogations returns InterrogationLogResponse items: { interrogationId, suspectId, suspectName, questionType, question, answer, presentedEvidence{evidenceId,title}, createdAt } (InterrogationLogResponse.java:7-18). The FE maps every list item with InterrogationResult.fromJson (play_session_repository.dart:86-97), which is the POST-response shape: it reads interrogationId/suspectId/suspectName/question/answer/createdAt (all present in both) plus `unlockedEvidences` (absent in the log DTO → defaults to empty, play_models.dart:353-357) and ignores `presentedEvidence` and `questionType` entirely. No crash because the 5 required fields overlap, and the only consumer (_LogCard) renders just question + answer (suspect_detail_screen.dart:357-365). But the FE can never display which evidence was presented in a past interrogation, and the two distinct BE DTOs are conflated into one FE model.
- **FE 증거**: project-fe/lib/repositories/play_session_repository.dart:86-97 (interrogationLogs maps each item with InterrogationResult.fromJson); project-fe/lib/models/play_models.dart:346-359 (InterrogationResult.fromJson reads unlockedEvidences, no presentedEvidence/questionType); project-fe/lib/screens/suspect_detail_screen.dart:357-365 (_LogCard renders only question/answer)
- **BE 증거**: src/main/java/com/startup/domain/ai/dto/InterrogationLogResponse.java:7-18 (questionType + PresentedEvidenceDto, no unlockedEvidences)
- **문서**: docs/CaseLab_AI_API_Spec.md §10.3 (interrogation log list shape with presentedEvidence + questionType)
- **영향**: No functional break today (required fields overlap and presented-evidence isn't shown). It is a latent contract drift: if the FE later tries to show presentedEvidence from the log list it will always be null because it parses with the wrong model. Low risk, MVP-core screen but cosmetically complete.
- **권장(코드변경 없음)**: Either add a dedicated FE model matching InterrogationLogResponse (with questionType + presentedEvidence), or have the FE explicitly acknowledge it only consumes the overlapping fields. No BE change needed; the two BE DTOs are intentionally different (POST returns unlockedEvidences, GET returns presentedEvidence).
- **검증 재현**: Independently reproduced from source in both repos.

BE: AiInterrogationController.java:35-39 — GET /api/play-sessions/{sessionId}/interrogations returns ApiResponse<List<InterrogationLogResponse>>. InterrogationLogResponse.java:7-18 has fields {interrogationId, suspectId, suspectName, questionType, question, answer, presentedEvidence{evidenceId,title}, createdAt} and NO unlockedEvidences. The POST endpoint (controller:42-49) returns a DIFFERENT DTO, InterrogationResponse.java:9-36, which has unlockedEvidences and NO questionType/presentedEvidence. InterrogationLogQueryService.java:103-112 confirms the log DTO is populated with questionType + presentedEvidence.

FE: play_session_repository.dart:86-97 — interrogationLogs() maps every list item with InterrogationResult.fromJson (the POST shape). play_models.dart:346-359 — InterrogationResult.fromJson reads j['unlockedEvidences'] (absent in the log DTO -> defaults to const [], line 353-354) and never reads presentedEvidence or questionType. So those two BE fields are silently dropped at parse time and unlockedEvidences is always empty for log items.

No crash: api_client.dart:132-134 unwraps the {success,data} envelope and returns the raw data (the JSON array), so `data as List<dynamic>` at repository:94 succeeds; the 5 overlapping required fields (interrogationId/suspectId/suspectName/question/answer) plus createdAt are all present in both DTOs. Confirmed silent field loss, not a crash.

Sole consumer: suspect_detail_screen.dart:37,63-67,176 stores the result in _logs and renders each via _LogCard, which (lines 357-365) displays only log.question and log.answer. A full lib-wide grep shows the repository method interrogationLogs() has exactly one caller (suspect_detail_screen.dart:63). The interrogation_chat_screen.dart hits at lines 65-71 use a different in-memory controller.interrogationLogs (game_session_controller.dart:241), not the repository method — unrelated.

Doc ref verified: CaseLab_AI_API_Spec.md §10.3 (line 1551, response block lines 1565-1582) shows the log list shape with questionType + presentedEvidence and no unlockedEvidences, matching the BE DTO and contradicting the FE model.

Severity P2 is correct: no functional break today (required fields overlap, presented-evidence is not rendered), genuine latent contract drift on an MVP-core screen that is cosmetically complete. All cited file:line references in the finding are accurate.
- **정정/주의**: Minor precision only (does not change confirmed/severity): the finding's phrase "ignores presentedEvidence and questionType entirely" is accurate in effect — the FE never reads those keys off log-list items; they are silently discarded during InterrogationResult.fromJson. Also note the unrelated controller.interrogationLogs references in interrogation_chat_screen.dart:65-71 are a separate in-memory list (game_session_controller.dart:241), not the repository call, so they do not mitigate the finding. The repository method interrogationLogs() has exactly one consumer: suspect_detail_screen.dart:63.



---

## 부록 B — private seed 검증 결과 (운영 동일본 / 구조적 사실만 · 스포일러 제외)

> 팀장 제공 `private/` 폴더(운영 동일본)로 seed 의존 항목을 검증. **정답/범인/해설 등 스포일러 내용은 이 문서에 옮기지 않음** — 캐릭터 타입 개수·난이도·unlock 타입·정책 개수 등 비-정답 구조 사실만 기록. 검증 경로: `private/deploy/scenarios/{seowolchae,studio9}.v1.yaml`(PyYAML 파싱). 런타임 import 경로는 env `CLUEROOM_SCENARIO_IMPORT_PATHS`로 주입(로컬 기본 빈값).

### B.1 구조 요약 (정본 `private/deploy/scenarios/`)

| 시나리오 | difficulty | 캐릭터(타입) | 지목불가 | unlockRules | npcPolicies / 반응정책 entries |
|---|---|---|---|---|---|
| seowolchae | NORMAL | 5 (CULPRIT_ELIGIBLE 4 + NEUTRAL_WITNESS 1) | 1 | 25 (전부 PHASE) | 5 / 11 (dangling 0) |
| studio9 | **NORMAL_PLUS** | 7 (CULPRIT_ELIGIBLE 4 + PERMANENT_RED_HERRING 2 + NEUTRAL_WITNESS 1) | 3 | 35 (전부 PHASE) | 7 / 20 (dangling 0) |

### B.2 전 사본 일관성 (각 11개 사본)

- difficulty·캐릭터 타입·지목불가 수·unlockType(전부 PHASE)은 **11개 사본 전체 일관**.
- **단 반응정책: 7개 사본은 full(seowolchae 11 / studio9 20), 4개 사본은 0개** → 어떤 seed 리비전을 import하느냐로 "증거 제시 반응" 동작 여부가 갈림. 정본(`deploy/scenarios/`)은 full.

### B.3 항목별 판정

- **P1-4 (증인/지목불가) — 확정·영향 구체화**: 두 시나리오 모두 지목불가 캐릭터 존재(1, 3). `culpritEligible`이 정확한 판별자(FE의 NEUTRAL_WITNESS-only는 studio9 RED_HERRING 2명 미포착). `PlaySuspectResponse`에 `culpritEligible` 노출 필요.
- **P1-11 (MANUAL) — 반증**: 100% PHASE, MANUAL 0건 → 현 콘텐츠 비이슈(P2 강등). 진짜 문제는 §4 QA-1(심문 해금 미구현).
- **P2-13 (NORMAL_PLUS) — 확정 사용**: studio9가 실제 NORMAL_PLUS → FE에서 '보통'으로 오표기·필터 불가.
- **증거제시 반응 체인 — 검증**: importer `ScenarioYamlImportService.java:446-465`가 `evidenceReactionPolicies[].evidenceCode`를 Evidence PK로 resolve → `SuspectResponsePolicy(conditionKey="PRESENTED_<code>", presentedEvidenceId=PK, priority 100)` 저장(미존재 코드면 import 실패; 정본 dangling 0). resolver `ResponsePolicyResolver.java:61-64`가 presentedEvidenceId 정확 일치 매칭(priority 100 > DEFAULT 0). → **정본 seed에선 코드 체인 정상.** 단 (a) 반응정책 0개 리비전 import 시 무력화(B.2), (b) resolver 입력 한계로 런타임 차등 약함(§4 QA-7).

### B.4 private 폴더에서 발견한 추가 이슈

- **NEW-1 (치명적 · PR 전 필수)**: `.gitignore`는 `.private/`(점 있음, line 53)만 무시. 실제 폴더는 `private/`(점 없음) → `git status`에 `?? private/`(untracked, 미무시). 부주의한 `git add` 시 **전 시나리오 정답/시크릿/배포 동일본이 공개 레포로 유출** → 제1원칙 정면 위반. **`.gitignore`에 `private/`(및 `private.zip`) 추가 필수.** → §6 프로세스.
- **NEW-2 (P2 · 프로세스)**: seed 버전 난립 — `private/` 안에 각 시나리오 11개 사본·7리비전, 그중 4개는 반응정책 0개. 단일 정본 경로 통일 + 배포가 full-policy 본을 가리키는지 확인. → §6 프로세스.


---

## 부록 C — 운영 MVP QA 대조 (전체 · 생략 없음)

> 원본: `docs/MVP_PLAY_FLOW_QA_2026-06-04.md` + `docs/MVP_QA_ISSUE_HANDOFF_2026-06-04.md`. 각 QA 발견을 정적 리뷰와 대조해 **이미찾음 / 부분 / 신규**로 분류. 결론: QA 18건 중 약 14건이 정적 리뷰에 없던 신규 — 정적 계약 리뷰 ↔ 런타임 서버 QA의 **스코프 차이**(QA는 BE/AI 내부·seed·운영 로깅·런타임을 봄). 두 리뷰는 상보적이며 합쳐야 완전해진다.

### C.1 분류 표

| QA 발견 | 분류 | 본문 매핑 / 우리 근거 |
|---|---|---|
| seed unlockRules 전부 PHASE | 이미찾음 | 부록 B (P1-11 강등 근거) |
| active-session 복구 미연동 | 이미찾음 | P1-1 |
| prompt injection 미누출 | 이미찾음(positive) | A15 스포일러 게이트 |
| BE 403/404/ownership 방어 동작 | 이미찾음(positive) | A18 / A19 |
| S3/imageUrl 운영 정상 | 이미찾음(보완) | P2-16 — 운영 해소, 로컬 갭만 잔존 |
| abandon `{success:true}` (data 없음) | 이미찾음 | A43(정상 판정) = QA-14 |
| coverUpText optional | 이미찾음(사실)+신규(결정) | P2-10 인접 = QA-4 |
| 심문 기반 증거 해금 미구현 | **부분→신규(코드확정)** | P1-11/enum은 정적; 리스너 부재·`[]`는 QA-1 |
| idle 15분 전체 해금 | 부분 | P1-11(all PHASE) + QA-6(런타임 측정·기획) |
| 증거제시 정책 차등 약함 | 부분 | 부록 B(체인 존재) + QA-7(런타임·입력한계) |
| malformed/type/enum → 500 | **신규(코드확정)** | QA-2 |
| EVIDENCE_PRESENTED + null 허용 | **신규** | QA-3 |
| validate NEEDS_FIX(hints=0)·AI path 미실행·validate 입력 timeline/secret 누락 | **신규(코드확정)** | QA-5 |
| AI 답변 policy 초과 단정 | **신규** | QA-7 |
| ResponsePolicyResolver 입력 한계(question/topic/userIntent/stage 미사용) | **신규(코드확정)** | QA-7 |
| RecentTurnsHistoryProvider 5턴 cap | **신규(코드확정)** | QA-9 |
| concurrent race activeSessionId 누락 | **신규** | QA-8 |
| 용의자 relationToVictim=null / publicProfile==publicStatement | **신규(코드확정)** | QA-10 |
| numeric string ID coercion | **신규** | QA-11 |
| final-deduction/interrogation 텍스트 길이 무제한 | **신규** | QA-12 |
| abandon 후 final-deduction AI010 메시지 부정확 | **신규** | QA-13 |
| hints API `[]` (hint seed 0) | **신규** | QA-15 |
| locked evidence title/unlockHint 노출 | **신규(정책)** | QA-16 |
| Hibernate TRACE 사용자 질문 평문 로깅 | **신규(Infra/보안)** | QA-17 |
| prod MockSolutionReader fallback 모니터링 | **신규(운영)** | QA-18 |

### C.2 QA가 다루지 않은 우리 정적 FE 항목 (QA는 서버만 실행 · Flutter 앱 미검증)

P0-1(FE 409/AI010 후 결과화면 진입), P1-2(FE 20s 타임아웃), P1-3(FE 채팅 이력 미복원), P1-4(FE 지목 노출 — 부록 B로 확정), P1-10(cleartext), P2-1(FE 무인증 헤더), P2-4(FCM 스텁), P2-5(thumbnailUrl vs coverImageUrl), P2-12(canPlay/isBookmarked 무시·하드코딩) 등 — **여전히 FE측 실행 검증 필요.**

### C.3 주의

QA 문서의 "Backend Re-review Notes(Confirmed By Review)" 10항목은 **본 정적 FE↔BE 문서가 아니라 별도 BE 코드 재검토** 결과이며, 위 C.1의 신규 버킷과 겹친다(예: InterrogationCompletedEvent 리스너 부재, ResponsePolicyResolver 입력 한계, validate timeline 누락, RecentTurns 5턴, prod MockSolutionReader fallback). 본 §4는 이를 운영 QA와 함께 통합한 것이다.

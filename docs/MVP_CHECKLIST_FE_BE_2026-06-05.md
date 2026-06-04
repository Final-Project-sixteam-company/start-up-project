# MVP 체크리스트 — FE_BE 통합리뷰 + MVP QA + Play Flow (Codex 크로스체크용)

- **작성일:** 2026-06-05
- **목적:** `FE_BE_INTEGRATION_REVIEW_2026-06-04.md`(P0/P1/P2) + `MVP_QA_ISSUE_HANDOFF_2026-06-04.md` + `MVP_PLAY_FLOW_QA_2026-06-04.md`(QA-1~18, UX)를 단일 체크리스트로 통합. **현재 코드가 각 항목을 통과하는지** Codex가 독립 대조한다.
- **저장소:** BE `C:/Users/Russell/Desktop/Workspace/start-up-project` (Spring Boot4/Java21, `com.startup`) · FE `C:/Users/Russell/Desktop/Workspace/project-fe` (**Flutter/Dart**, `clueroom`)
- **판정 표기:** `PASS`(코드가 해결) / `FAIL`(미해결·미구현) / `PARTIAL`(일부·조건부) / `NA`(post-MVP·운영/인프라라 코드 점검 대상 아님)
- **사용법:** 각 표의 `Codex 판정` 칸을 비워뒀다. Codex는 `확인 위치`의 file:line/엔드포인트를 직접 열어 `PASS 기준` 충족 여부를 판정하고 근거 file:line을 적는다. `세션 변경`은 **이번 작업 세션(Phase1/2 + C-scope 보정)에서 손댄 항목** 표시 — 변경됐다고 PASS를 전제하지 말고 **특히 의심해서** 재확인할 것.
- **라인 번호 주의:** 출처 리뷰의 file:line은 2026-06-04 기준. 이번 세션 변경으로 일부 라인이 이동했을 수 있으니 심볼/메서드명 기준으로 확인.

> **검증 가능한 기준만 PASS로 친다.** "구현 의도"가 아니라 "코드가 실제로 그 동작을 하는가"로 판정.

---

## 1. P0 — 즉시 수정 (1)

| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P0-1 | 최종추리 채점지연+재제출 409(AI010)→결과화면 영구 진입불가 | final-deduction이 409 **AI010/AI015** 또는 타임아웃을 받으면 `GET /api/play-sessions/{id}/result`로 결과화면에 **도달**한다(데드엔드 아님) | FE `lib/screens/submit_screen.dart` `_onSubmit`/복구 분기, `lib/screens/result_screen.dart` `_fetchResult` 폴링, `api_config.dart` aiTimeout | ✅ Phase2 (submit 복구 + result 폴링) | ☐ |

---

## 2. P1 — MVP 품질 직접 영향 (11)

| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P1-1 | 진행중 세션 복구를 FE가 `/active`·409 details에 미연결 | FE가 409(P002) 시 `GET /play-sessions/active?scenarioId=` 호출 **또는** `error.details.activeSessionId` 파싱으로 기존 세션을 재개/정리 | FE `PlaySessionRepository`(active 메서드 유무), `api_exception.dart`(details 필드), `game_session_controller.dart` 409 분기 | — (미변경) | ☐ |
| P1-2 | AI 응답 지연 vs FE 고정 타임아웃 + 재시도 부재 | AI 엔드포인트(interrogation/final-deduction)에 **전용(긴) 타임아웃** + 타임아웃 후 `/result` 탐침 또는 재시도 | FE `api_config.dart` aiTimeout, `play_session_repository.dart` interrogate/submitFinalDeduction timeout 인자 | ✅ aiTimeout 60s + /result 탐침(부분) | ☐ |
| P1-3 | 심문 대화 이력이 서버에서 복원 안 됨 | 세션 로드/재개 시 `GET /interrogations`로 채팅 이력 시드 | FE `game_session_controller.dart` `_refreshAll`/`_tryResume`, `interrogation_chat_screen.dart` | — (미변경) | ☐ |
| P1-4 | 용의자 증인/지목가능 판별필드 누락 → 증인이 범인 후보 노출 | `PlaySuspectResponse`에 `culpritEligible`(또는 characterType) 노출·매핑 + FE가 그 값으로 지목후보 필터(isWitness 하드코딩 아님) | BE `PlaySuspectResponse.java`, `PlaySessionService` getSuspects 매핑 · FE `play_models.dart`, `game_session_controller.dart` accusableSuspects, `suspect_detail_screen.dart` | ✅ Phase1 (BE+FE) | ☐ |
| P1-5 | 타임라인 BE 구현됐으나 FE 미연동(샘플) | FE가 `GET /timeline`을 호출·렌더, CL-001 샘플 하드코딩 제거 | FE `lib/screens/timeline_screen.dart`, `PlaySessionRepository`(timeline 메서드 유무) | — (미변경) | ☐ |
| P1-6 | 시나리오 검색·필터(keyword/type/difficulty)를 BE가 서버에서 무시 | `ScenarioService.getScenarios`가 condition.keyword/type/difficulty를 **실제 쿼리 조건**으로 반영 | BE `ScenarioService.java` getScenarios, `ScenarioSearchCondition.java` | — (미변경) | ☐ |
| P1-7 | 북마크 엔드포인트 미구현(스펙 1차 MVP) | `POST/DELETE /api/scenarios/{id}/bookmarks` + `GET /api/scenarios/bookmarked` BE 구현 | BE `ScenarioController.java` | — (미변경) | ☐ |
| P1-8 | 리뷰 엔드포인트 미구현(스펙 1차 MVP) | `POST/GET /api/scenarios/{id}/reviews` BE 구현 | BE `ScenarioController.java` | — (미변경) | ☐ |
| P1-9 | 커스텀 시나리오 제작 플로우 부재 → validate 소비자 없음 | FE에 제작/편집/검증 화면 존재 **또는** MVP 범위에서 명시적 제외 결정 문서화 | FE 제작 화면(grep create/draft/validate), BE `AiScenarioValidationController.java`(구현됨) | — (미변경, 범위결정 대기) | ☐ |
| P1-10 | Android cleartext HTTP 미허용 → 로컬 API 전체 실패 가능 | debug 빌드에 `usesCleartextTraffic`(또는 network-security-config), 릴리스는 https 유지 | FE `android/app/src/debug/AndroidManifest.xml` | ✅ B2 | ☐ |
| P1-11 | 증거 해금 — 심문/증거제시 기반 해금 미구현(unlockedEvidences 항상 []) | EVIDENCE_PRESENTED 제시 시 조건 충족하면 `InterrogationResponse.unlockedEvidences`가 실제로 채워짐(+멱등). 순수 INTERROGATION topic/count 기반은 별도 | BE `AiInterrogationService` resolveUnlockedEvidences, `domain/play/service/InterrogationEvidenceUnlockService.java`, seed unlockRules | ✅ C3+seed (EVIDENCE_PRESENTED만; INTERROGATION은 phase2) | ☐ |

---

## 3. P2 — 후순위 / 잠재 / 드리프트 (22)

### 인증·아이덴티티 (post-MVP)
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-1 | FE Authorization 헤더 미전송 + 공유 Mock 유저 | FE가 JWT 헤더 전송 + BE가 헤더 기반 userId 식별 (post-MVP면 NA) | FE `api_client.dart` authTokenProvider, `auth_service.dart` · BE `MockUserProvider.java` | — | ☐ |
| P2-2 | 로그인/회원가입 화면·BE 인증 부재 | post-MVP — 범위 결정 | FE `splash_screen.dart`, `auth_service.dart` | — | ☐ |
| P2-3 | 403/P004(타세션 접근) FE 미처리 | FE가 403/P004를 재개경로에서 명시 처리 | FE `api_exception.dart` | — | ☐ |

### FCM / 디바이스 토큰
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-4 | FCM 토큰 미등록 + 잘못된 엔드포인트 | FE가 `POST /api/device-tokens {token,deviceType}`로 등록 | FE `main.dart` `_registerFcmTokenWithBackend` · BE `DeviceTokenController.java` | — | ☐ |

### 스펙/문서 드리프트
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-5 | 상세 이미지/태그/추천 필드 드리프트 | FE가 `coverImageUrl` 읽음(현 thumbnailUrl), BE tags/nextRecommendedScenarios 채움 | FE `scenario_repository.dart` `_fromDetailJson` · BE `ScenarioDetailResponse.java`, `AiDeductionScorer.java` | — | ☐ |
| P2-6 | 문서가 FE를 Android/Kotlin로 오기(실제 Flutter) | 문서 스택 표기 정정 | `CLAUDE.md`, `CaseLab_AI_API_Spec.md`, `ANDROID_SCREEN_API_MAPPING.md` | — | ☐ |
| P2-7 | `/locations` 스펙 사본 드리프트(객체 vs 배열) | FE가 BE 정본(객체 `{...,locations:[...]}`)대로 파싱 | FE `play_session_repository.dart` locations · BE `PlayLocationsResponse.java`, FE `api-spec.md` | ✅ B1(FE locations 객체 파싱 추가) | ☐ |
| P2-8 | FE `api-spec.md` 구버전(/active·409 details·canPlay 누락) | FE 스펙 사본 최신화 | FE `api-spec.md` | — | ☐ |
| P2-9 | API 버저닝 없음 | `/v1` 또는 버전 헤더 (선택) | `api_config.dart` apiPrefix | — | ☐ |

### 필드/계약 드리프트
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-10 | 종합추리 설명(summary) 수집하나 미전송 | FE summary가 `FinalDeductionRequest`에 포함·전송 **또는** 선택화 결정 | FE `submit_screen.dart` `_summaryCtrl` · BE `FinalDeductionRequest.java` | — | ☐ |
| P2-11 | `GET /interrogations` 응답 questionType/presentedEvidence 손실 | FE 로그 파싱이 두 필드 보존(UI 영향 시) | FE `play_session_repository.dart` interrogationLogs, `play_models.dart` | — | ☐ |
| P2-12 | canPlay/isBookmarked 미파싱, 플레이가능 {1,4,5} 하드코딩 | FE가 canPlay 파싱·사용(하드코딩 제거) | FE `scenario_repository.dart`, `scenario.dart`, `scenario_detail_screen.dart` | — | ☐ |
| P2-13 | Difficulty enum 불일치(BE NORMAL_PLUS ↔ FE easy/medium/hard) | FE가 NORMAL_PLUS 포함 매핑(또는 BE/스펙 정합) | FE `scenario.dart`, `scenario_repository.dart` · BE `Difficulty.java`, seed | — | ☐ |
| P2-14 | 증거 categoryLabel BE 미전송(FE 읽음) | BE evidenceType→categoryLabel 노출 | BE `PlayEvidenceResponse.java` · FE `play_models.dart` | — | ☐ |
| P2-15 | FE 주석 `SESSION_ALREADY_EXISTS` stale(실제 P002) | 주석 정정(무해) | FE `play_session_repository.dart` | — | ☐ |

### 자원/전송/시간
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-16 | 로컬 이미지 플레이스홀더 폴백 | graceful 폴백(운영 S3 200) — 로컬 한정 NA | FE `asset_image_widget.dart`, `pubspec.yaml` · BE `ScenarioAssetUrlResolver.java` | — | ☐ |
| P2-17 | LocalDateTime 타임존 없이 직렬화 | UI 표시 소비자 있으면 ISO offset (영향 미미) | FE `play_models.dart` 날짜 파싱 | — | ☐ |
| P2-18 | FE status를 바디 error.status에서 취득 | 프록시 5xx 오분류 잠재 (단일 서비스면 잠재) | FE `api_client.dart` | — | ☐ |

### 커버리지 노트 (저위험)
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| P2-19 | my-records(`GET /play-sessions/me`) 미구현 | 2차 MVP — NA 가능 | FE `my_records_screen.dart` | — | ☐ |
| P2-20 | 로그인/프로필/users-me 미구현 | post-MVP — NA 가능 | FE auth/profile | — | ☐ |
| P2-21 | 구현됐으나 FE 미소비 엔드포인트 | 세션상세/active/증거상세/unlock/용의자상세/locations/timeline/validate/device-tokens 소비 현황(B1 후 locations는 소비) | BE `PlaySessionController.java` ↔ FE repo | ✅ locations 일부(B1) | ☐ |
| P2-22 | 엔벨로프·에러코드·페이지네이션 계약 | ✅ 정상 확인(재플래그 불필요) | FE `api_client.dart` ↔ `ApiResponse/ErrorResponse/PageResponse` | — | ☐ PASS(기준선) |

---

## 4. 운영 MVP QA (런타임 — BE/AI/Infra) (18)

### 4.1 P0/P1
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| QA-1 | ★심문 기반 증거 해금 미구현 | 제시/심문 조건 충족 시 unlockedEvidences 실제 반환 + DB unlocked_evidences 기록(멱등). (P1-11과 동축) | BE `AiInterrogationService`, `InterrogationEvidenceUnlockService.java`, seed unlockRules | ✅ EVIDENCE_PRESENTED 경로 (INTERROGATION topic/count는 phase2) | ☐ |
| QA-2 | request body 파싱/타입/enum 오류가 500 | malformed JSON / 타입오류 / invalid enum → **400** | BE `GlobalExceptionHandler.java` `HttpMessageNotReadableException` 핸들러 | ✅ Phase1 | ☐ |
| QA-3 | EVIDENCE_PRESENTED인데 presentedEvidenceId=null이어도 AI 호출 | EVIDENCE_PRESENTED+null → **400**(AI 미호출) | BE `InterrogationRequest.java` `@AssertTrue` | ✅ Phase1 + 보정 A1(양방향) | ☐ |
| QA-4 | coverUpText 누락이 허용됨 | MVP 필수면 `@NotBlank`, 선택이면 명시 결정 | BE `FinalDeductionRequest.java` | — (결정 대기) | ☐ |
| QA-5 | 공식 시나리오 validate 둘 다 NEEDS_FIX(hints=0) + AI경로 미실행 | hints 사용 시 YAML 스키마+import+seed, 미사용 시 validation rule 완화. validate 입력에 timeline/secrets 포함 | BE `ScenarioYaml.java`(hints 섹션), `DefaultScenarioDataReader`, `AiScenarioValidator` | — (미변경) | ☐ |
| QA-6 | idle 15분만으로 전 증거 자동 해금(전부 PHASE) | 결정타 일부를 INTERROGATION/EVIDENCE_PRESENTED/MANUAL로 또는 시간공개 게임 명시 | seed unlockRules unlockType 분포 (WEARABLE=EVIDENCE_PRESENTED화 됐는지) | ⚠️ seed에서 WEARABLE만 EVIDENCE_PRESENTED화(부분); 나머지 PHASE | ☐ |
| QA-7 | AI 정책 반응 약함(resolver 입력 한계) | `ResponsePolicyResolver`가 questionType/topic/userIntent/stage 반영, allowedFacts/denialBoundary 프롬프트 반영 | BE `ResponsePolicyResolver.java` resolve 시그니처, `AiPromptBuilder` | — (미변경) | ☐ |
| QA-8 | concurrent create race에서 409 details.activeSessionId 누락 | DataIntegrityViolation fallback에서 active session 재조회 후 details 포함 | BE `PlaySessionService.java` create race fallback | — (미변경) | ☐ |
| QA-9 | 대화 이력 5턴 하드캡 | 설정값(maxTurns) 반영하는 쿼리(findTop5 아님) | BE `RecentTurnsHistoryProvider.java`, `InterrogationLogRepository.java` | ✅ Phase1 | ☐ |

### 4.2 P2
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| QA-10 | 용의자 detail seed 품질(relationToVictim=null, profile==statement) | YAML CharacterYaml에 relationToVictim 추가·import 또는 매핑 보강 | BE `ScenarioYaml.CharacterYaml`, `Suspect.java`, seed | — | ☐ |
| QA-11 | numeric string ID coercion 허용 | 엄격 계약 시 문자열 숫자 차단(선택) | BE Jackson 설정 | — | ☐ |
| QA-12 | 텍스트 길이 무제한 | motive/method/coverUp에 max length | BE `FinalDeductionRequest.java` `@Size` | ✅ Phase1 | ☐ |
| QA-13 | abandon 후 final-deduction 에러 부정확(AI010) | abandoned 전용 에러/메시지 | BE final-deduction 상태 검증 경로 | — | ☐ |
| QA-14 | abandon 응답 `{success:true}`(data 없음) | data:null/DTO (엄격 계약 시) | BE abandon 컨트롤러/응답 | — | ☐ |
| QA-15 | hints API `[]`(hint seed 0) | hint seed/import 또는 MVP 제외 결정 | BE hints 경로, seed | — | ☐ |
| QA-16 | locked evidence title/unlockHint 노출 | 진행예고 의도면 OK, 숨김 기획이면 마스킹 | BE 증거 마스킹 로직 | — (기획 결정) | ☐ |
| QA-17 | ★Hibernate TRACE에 사용자 질문 평문 로깅 | 운영 프로필 Hibernate bind TRACE 레벨 하향 | BE `application.yml`/`application-prod.yml` logging 레벨 | — (미변경; 보정 A6은 앱 로그만) | ☐ |
| QA-18 | prod MockSolutionReader fallback 모니터링 | 운영 로그 grep — 인프라 NA | 운영/인프라 | — (NA) | ☐ |

---

## 5. UX / 콘텐츠 QA (Play Flow QA)
| ID | 항목 | PASS 기준 | 확인 위치 | 세션 변경 | Codex 판정 |
|---|---|---|---|---|---|
| UX-P1 | 시나리오 제목이 첫인상에 너무 짧음(서월채/스튜디오9) | 정식 제목 보강(예: "서월채의 마지막 처방") 또는 shortTitle 분리 | seed scenario.title, FE 카드 UI | — (seed/콘텐츠) | ☐ |
| UX-P2 | 목록 description 한 문장으로 다소 빽빽 | Android 카드 2줄 말줄임 | FE 카드 UI | — | ☐ |

---

## 6. 이번 세션(C-scope 보정) 신규/검증 항목

> 위 표의 "세션 변경 ✅"와 매핑. Codex는 이 항목들을 **특히** 적대적으로 재확인.

| ID | 항목 | PASS 기준 | 확인 위치 | Codex 판정 |
|---|---|---|---|---|
| B1 | 현장 화면 `/locations` 연동 | scene_screen이 서버 장소 렌더, 서버 위치 없을 때만 정적 샘플/빈 상태 폴백 | FE `scene_screen.dart`, `game_session_controller.dart` locations 로드, `play_session_repository.dart` locations, `play_models.dart` PlayLocation | ☐ |
| A1 | 심문 요청 presentedEvidenceId 양방향 검증 | FREE/RECOMMENDED+id → 400, EVIDENCE_PRESENTED+null → 400 | BE `InterrogationRequest.java` `@AssertTrue` 2개 | ☐ |
| A2 | 해금 호출 서비스단 이중 방어 | resolveUnlockedEvidences가 questionType==EVIDENCE_PRESENTED && id!=null 일 때만 해금 호출 | BE `AiInterrogationService.java` resolveUnlockedEvidences | ☐ |
| A3 | EVIDENCE_PRESENTED unlock 트리거 필수 검증 | requiredPresentedEvidenceCode 비면 import validation 위반 | BE `ScenarioYamlValidator.java` | ☐ |
| A4 | importer→condition_json 저장 | requiredPresentedEvidenceCode가 evidence_unlock_rules.condition_json에 저장 | BE `ScenarioYamlImportServiceTest` + importer | ☐ |
| A5 | 해금 시나리오 소속 방어 | 제시 증거·용의자·대상 증거가 session.scenarioId 소속일 때만 해금 | BE `InterrogationEvidenceUnlockService.java` | ☐ |
| A6 | 민감 로그 축소 | conditionJson 파싱 실패/요청본문 파싱 실패 로그에 전문 대신 예외 클래스만 | BE `InterrogationEvidenceUnlockService.java`, `GlobalExceptionHandler.java` | ☐ |

---

## 7. 집계

### 7.1 우리 자동 감사 (2026-06-05, 멀티에이전트 + 적대적 재검증)
> raw finalVerdict 기준. **해석 주의:** descriptive/post-MVP 항목 일부(P2-1·P2-6 등)는 에이전트가 "현상 그대로 확인됨"을 `PASS`로 표기 — 이는 **"해결"이 아니라 "미해결 상태 확인"**이다. 따라서 raw PASS 수 ≠ 해결 수. §7.2의 MVP 관점 재버킷을 함께 볼 것.

| 심각도 | 총 | PASS | PARTIAL | FAIL | NA |
|---|---|---|---|---|---|
| P0 | 1 | 1 | 0 | 0 | 0 |
| P1 | 11 | 3 | 3 | 5 | 0 |
| P2 | 22 | 8* | 3 | 11 | 0 |
| QA | 18 | 10 | 3 | 4 | 1 |
| 보정 | 7 | 7 | 0 | 0 | 0 |
| UX | 2 | (미감사) | | | |

`*` P2 PASS 8건 중 P2-1·P2-6은 "미해결 확인"의 오라벨(실제 FAIL/NA 성격) — §7.2 참조.

### 7.2 MVP 관점 재버킷 (해결됐나? 남은 갭인가? post-MVP인가?)

**A. 해결됨/수용 가능 (MVP 문제 없음) — 23**
P0-1, P1-2, P1-4, P1-10 / QA-2, QA-3, QA-9, QA-12, QA-14, QA-16, QA-17 / A1·A2·A3·A4·A5·A6·B1 / P1-1(BE), B1(BE) / P2-7(런타임OK), P2-16(graceful), P2-18(OK), P2-22(기준선)

**B. 남은 MVP 갭 (조치 필요) — 18**
- **핵심 루프/플로우**: P1-1(FE 서버기반 세션복구 미연동), P1-3(채팅이력 서버복원 미구현), P1-5(타임라인 FE 미연동), P1-11/QA-1(순수 INTERROGATION topic·count 해금 미구현; EVIDENCE_PRESENTED만 됨), QA-7(AI 정책 반응 약함)
- **제출/상세 UX**: P2-5(상세 커버이미지 null + tags/추천 빈값), P2-10(종합추리 텍스트 미전송), P2-12(canPlay/북마크 미파싱·{1,4,5} 하드코딩), P2-11(로그 questionType/presentedEvidence 미파싱)
- **세션/에러 견고성**: QA-8(race 시 activeSessionId 누락 가능), QA-13(abandon 후 AI010 부정확)
- **seed/검증**: QA-5(validate hints=0, 스키마에 hints 없음), QA-10(relationToVictim null), QA-15(hint seed 0), P2-13(seed NORMAL_PLUS 미사용), QA-6(WEARABLE 1건만 EVIDENCE_PRESENTED; 나머지 PHASE — 시간공개 잔존), P2-14(evidence categoryLabel 미전송)
- **계약**: QA-11(FE가 ID를 strict 캐스트 → BE가 숫자 직렬화 보장해야)

**C. 1차 MVP 스펙이나 미구현(범위 결정 필요) — 4**
P1-6(검색·필터 BE 무시), P1-7(북마크 엔드포인트), P1-8(리뷰 엔드포인트), P1-9(커스텀 시나리오 제작 플로우)

**D. post-MVP / NA / 무해 — 12**
P2-1(auth 헤더), P2-2(로그인), P2-3(403 핸들링), P2-4(FCM 등록), P2-6(문서 Android 오기), P2-8(FE api-spec 구버전), P2-9(버저닝), P2-15(stale 주석), P2-17(tz), P2-19(my-records), P2-20(프로필), P2-21(미소비 엔드포인트), QA-18(fallback 모니터링/ops)

> 전체 항목별 verdict + file:line 근거 전문: 워크플로 출력 `tasks/wzgmucwmr.output` (56항목).

### 머지 게이트(C-scope 합의)
- **닫혀야 머지 가능:** P1-1(보정 A1)·P1-2(보정 A3)·P1-3(보정 A4 = QA-2/3 계열 + import 체인) + curl negative(FREE+id→400 / RECOMMENDED+id→400 / EVIDENCE_PRESENTED+null→400 / 정상해금→재발동 멱등).
- **seed 2건(배포 트랙):** WEARABLE unlock의 EVIDENCE_PRESENTED화 + variant SECRETARY 단일 활성. **gitignored라 코드 PR 미포함** — 운영/데모 배포 seed에 별도 반영 필수(미반영 시 박재민 fallback + 시간 자동해금 재현).

---

## 부록. 출처 매핑
- P0/P1/P2 → `docs/FE_BE_INTEGRATION_REVIEW_2026-06-04.md` §1~§3 (+ 부록 A 원자 발견, 부록 B seed, 부록 C 런타임 대조)
- QA-1~18 → 위 리뷰 §4 = `docs/MVP_PLAY_FLOW_QA_2026-06-04.md` / `MVP_QA_ISSUE_HANDOFF_2026-06-04.md` Findings 통합
- UX-P1/P2 → `MVP_PLAY_FLOW_QA_2026-06-04.md` UX/Content QA
- 보정 A1~A6/B1 → 이번 세션 C-scope 보정 커밋(BE/FE `fix/caselab-c-scope-phase1`)

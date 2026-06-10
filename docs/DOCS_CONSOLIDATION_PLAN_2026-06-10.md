# ClueRoom Docs Consolidation Plan - 2026-06-10

## 0. 목적

현재 `docs/`에는 `develop` 기준 tracked Markdown 문서가 30개, 약 17,422줄 있다.
문서 수가 늘면서 같은 API, 운영 절차, 인프라 계획, QA 결과가 여러 문서에 중복 기록되고 있고, 일부 문서는 코드 SoT와 drift가 있다.

이번 문서 다이어트의 목적은 단순 삭제가 아니라 **흡수 기반 정리**다.

```text
원칙:
1. 중복 문서는 바로 삭제하지 않는다.
2. 먼저 살아남을 정본 문서에 핵심 내용을 흡수한다.
3. 흡수된 문서는 docs index에 "어디로 흡수됐는지"를 남긴 뒤 제거한다.
4. 오래된 실행 기록은 필요한 결론과 재현 정보만 보존하고, 원문 전체 보존이 필요한 경우 별도 archive 여부를 먼저 결정한다.
5. 코드와 다른 문서는 문서끼리 합치기 전에 코드 SoT 기준으로 먼저 정정한다.
```

## 1. 현재 기준

```text
작업 브랜치:
chore/docs-consolidation-plan-20260610

분기 기준:
origin/develop

tracked Markdown 문서:
30개

tracked Markdown 총 라인 수:
약 17,422줄
```

현재 로컬에는 미추적 문서가 하나 있다.
이 파일은 이번 계획서 작성 중 건드리지 않는다.

```text
untracked:
docs/MVP_PLAY_FLOW_QA_2026-06-04.md
```

PR #55에 포함된 2026-06-10 QA 문서들은 아직 `develop` 기준 tracked 문서가 아니다.
해당 PR이 머지된 뒤 문서 다이어트 브랜치를 리베이스/머지하면 QA 흡수 대상에 포함한다.

## 2. 정리 방식

문서는 아래 4단계로 줄인다.

1. **정본 지정**
   내용 범위별로 살아남을 문서를 먼저 정한다.

2. **drift 정정**
   코드와 다른 내용은 흡수 전에 수정한다.
   오래된 내용을 그대로 합치면 큰 문서 하나 안에 틀린 정보가 남는다.

3. **흡수**
   중복 문서의 고유 내용만 정본 문서에 옮긴다.
   같은 설명은 한 곳만 남기고, 다른 문서에는 링크만 둔다.

4. **제거 또는 얇은 링크화**
   흡수가 끝난 문서는 제거한다.
   단, 팀이 자주 여는 진입점 파일은 1차에서는 5~10줄짜리 링크 문서로 남길 수 있다.
   최종적으로 문서 수를 줄이려면 링크 문서도 `docs/README.md`의 흡수 이력 표로 대체한다.

## 3. 목표 구조

1차 목표는 active docs를 30개에서 약 16~18개 수준으로 줄이는 것이다.
문서 수 자체보다 중요한 기준은 "한 주제의 정본이 하나인지"다.

### 유지 또는 정본화할 문서

| 범위 | 정본 후보 | 처리 방향 |
|---|---|---|
| 문서 진입점 | `docs/README.md` | 새로 만든다. 문서 지도, 정본 목록, 흡수 이력, 스포일러/secret 경계를 관리한다. |
| 제품/요구사항 | `docs/CaseLab_AI_PRD.md` | 제품 목적, MVP 범위, 역할만 남긴다. API/인프라 상세는 링크로 이동한다. |
| API 계약 | `docs/CaseLab_AI_API_Spec.md` | API request/response 정본으로 유지한다. 화면별 사용법은 frontend 문서로 보낸다. |
| Android/Frontend 연동 | `docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` | `ANDROID_SCREEN_API_MAPPING.md` 흡수 완료. 화면 흐름 + API 사용 순서를 여기에 집중한다. |
| 백엔드 구현 규칙 | `docs/BACKEND_IMPLEMENTATION_GUIDE.md` | 패키지, 계층, 트랜잭션, 예외, MockUser 규칙 정본으로 유지한다. |
| AI 프롬프트 정책 | `docs/AI_NPC_PROMPT_POLICY.md` | AI 지식 경계, prompt 구조, injection 방어, ResponsePolicy 규칙 정본으로 유지한다. |
| DB/ERD | `docs/CaseLab_AI_ERD_Design.md` | 코드 SoT 기준 drift를 먼저 정정한 뒤 DB 설계 정본으로 유지한다. |
| ADR | `docs/ADR.md` | 짧은 결정 목록으로 유지한다. 상세 설명은 각 정본 문서로 링크한다. |
| 공식 시나리오 | `docs/OFFICIAL_SCENARIO_DEMO_DAY.md` | 스포일러 포함 정본으로 유지한다. 공개/비공개 경계 주석을 강화한다. |
| 시나리오 스키마 | `docs/scenarios/SCENARIO_YAML_SCHEMA.md` | `docs/scenarios/README.md`의 정책 내용을 흡수한다. |
| 로컬 실행/배포 요약 | `docs/RUN_AND_DEPLOY.md` | 개발자 빠른 실행, Android 연결, 배포 요약만 유지한다. 상세 운영은 runbook으로 링크한다. |
| 운영 runbook | `docs/infra/OPS_RUNBOOK.md` | 실제 명령어, 장애 대응, 백업/복구, rate limit dry-run 실행 절차를 흡수한다. |
| 인프라 전략 | `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 현재 구조와 선택 이유 정본. roadmap/scale-out 계획을 흡수한다. |
| 보안/트래픽/알림 정책 | `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | 새 문서 후보. GeoIP, Rate Limit, Grafana alert 정책을 하나로 합친다. |
| Infra Agent 운영 | `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` | Codex playbook, snapshot spec, monitoring agent 계획 중 공통 운영 원칙을 흡수한다. |
| LLMOps 운영 | `docs/infra/agent/LLMOPS_OPERATING_GUIDE.md` | LLMOps plan, PromQL, smoke runbook을 하나로 합친다. |
| QA handoff | `docs/QA_HANDOFF.md` 또는 `docs/qa/MVP_QA_HANDOFF.md` | 날짜별 QA 문서의 결론/미해결/재검증 결과를 흡수한다. |

## 4. 흡수 후보 매핑

### 4.1 Frontend / Android 중복 정리

| 현재 문서 | 흡수처 | 이유 |
|---|---|---|
| `docs/ANDROID_SCREEN_API_MAPPING.md` | `docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/CaseLab_AI_API_Spec.md`의 화면별 사용 설명 | `docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` | 흡수 완료. API Spec은 계약만, 화면 흐름은 frontend 문서가 맡는다. |

흡수 시 주의할 drift:

```text
- timeline / evidence detail / suspect detail API가 실제 구현됐는지 상태 정정
- GET /api/play-sessions/active를 구현 API 표에 반영
- POST interrogation 응답과 GET log 응답 필드 혼동 제거
```

### 4.2 QA 문서 정리

| 현재 문서 | 흡수처 | 이유 |
|---|---|---|
| `docs/MVP_QA_ISSUE_HANDOFF_2026-06-04.md` | `docs/QA_HANDOFF.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/MVP_PLAY_FLOW_QA_2026-06-04.md` | `docs/QA_HANDOFF.md` | scrub 완료. 정답/variant/증거 ID/운영 marker 상세가 있어 원문은 커밋하지 않고 public-safe 결론만 흡수한다. |
| PR #55의 2026-06-10 QA 문서들 | `docs/QA_HANDOFF.md` | PR #55가 아직 open 상태라 미흡수. 머지 후 최신 QA 결과를 같은 문서에 누적한다. |

흡수 방식:

```text
- P0/P1/P2 이슈는 현재 상태 기준으로 하나의 표에 모은다.
- 해결된 이슈는 "Resolved"로 이동하고 재검증 근거를 남긴다.
- 날짜별 긴 실행 로그는 본문에 반복하지 않는다.
- 중요한 curl, 재현 절차, 기대/실제 결과만 보존한다.
```

주의할 drift:

```text
- 500 parse 오류와 EVIDENCE_PRESENTED + null 이슈는 코드상 400 처리로 해결됨.
- 상단 미해결 목록과 하단 재검증 노트가 충돌하지 않게 정리한다.
```

### 4.3 인프라 문서 정리

| 현재 문서 | 흡수처 | 이유 |
|---|---|---|
| `docs/infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md` | `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/SCALE_OUT_POC_PLAN.md` | `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/MYSQL_BACKUP_AND_RESTORE_POLICY.md` | `docs/infra/OPS_RUNBOOK.md` + `docs/infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md` | `docs/infra/OPS_RUNBOOK.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/RATE_LIMIT_POLICY.md` | `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/GEOIP_BOT_TRAFFIC_POLICY.md` | `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/GRAFANA_ALERT_POLICY.md` | `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/CLUEROOM_SECRET_INPUT_GUIDE.md` | 유지 | 작고 보안 민감한 팀원 handoff 문서라 당장 합치지 않는다. 필요 시 OPS_RUNBOOK의 secret 섹션에서 링크한다. |

흡수 전 정정할 drift:

```text
- external-data cutover 상태를 현재 스크립트 기준으로 통일
- Prometheus job_name: clueroom-app-blue / clueroom-app-green
- Loki/Alloy가 미도입이라는 stale 서술 정정
- Swagger 정본 경로 /swagger-ui.html 병기
- backup cron/log가 스크립트 기능인지 운영 crontab 설정인지 분리
```

### 4.4 Infra Agent / LLMOps 문서 정리

| 현재 문서 | 흡수처 | 이유 |
|---|---|---|
| `docs/infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md` | `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/agent/OPS_SNAPSHOT_SPEC.md` | `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/agent/MONITORING_AGENT_PLAN.md` | `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/agent/LLMOPS_AGENT_PLAN.md` | `docs/infra/agent/LLMOPS_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/agent/LLMOPS_PROMQL_QUERIES.md` | `docs/infra/agent/LLMOPS_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/infra/agent/LLMOPS_SMOKE_RUNBOOK.md` | `docs/infra/agent/LLMOPS_OPERATING_GUIDE.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |

### 4.5 시나리오 문서 정리

| 현재 문서 | 흡수처 | 이유 |
|---|---|---|
| `docs/scenarios/README.md` | `docs/scenarios/SCENARIO_YAML_SCHEMA.md` + `docs/README.md` | 흡수 완료. 최종 링크 정리 후 파일 제거 완료. |
| `docs/scenarios/SCENARIO_YAML_SCHEMA.md` | 유지 | YAML 구조 정본이다. |

정정할 drift:

```text
- CLUEROOM_SCENARIO_IMPORT_PATH -> CLUEROOM_SCENARIO_IMPORT_PATHS
- scoring 예시가 실제 기본 배점과 다른 경우 "shape only"인지 실제값인지 명확히 한다.
```

상태:

```text
완료. SCENARIO_YAML_SCHEMA.md에 공개 범위/private seed 경계와 MVP scoring bridge note를 흡수했고, scenarios/README.md는 최종 링크 정리 후 제거했다.
```

### 4.6 ERD / API / Backend / AI 정책 정리

이 그룹은 합치기보다 역할을 선명하게 나누는 편이 낫다.
문서 수를 줄이기 위해 억지로 합치면 오히려 정본성이 깨진다.

| 문서 | 처리 방향 |
|---|---|
| `docs/CaseLab_AI_ERD_Design.md` | 코드 SoT 기준으로 엔티티/컬럼 drift를 정정한다. |
| `docs/CaseLab_AI_API_Spec.md` | 미구현 API를 명확히 표시하고, 화면 흐름 설명은 frontend 문서로 링크한다. |
| `docs/BACKEND_IMPLEMENTATION_GUIDE.md` | 구현 규칙만 유지하고 API/ERD 상세 중복은 링크로 대체한다. |
| `docs/AI_NPC_PROMPT_POLICY.md` | 실제 prompt template과 ResponsePolicyResolver 기준으로 drift를 정정한다. |
| `docs/ADR.md` | 결정 목록만 유지한다. 상세 설명은 각 정본 문서로 링크한다. |

주요 drift 정정 후보:

```text
- SuspectSecret / SolutionEvidence / ScenarioVersion / AiGenerationLog 등 미구현 엔티티 표기 정리
- NpcKnowledgeProfile, ScenarioVariant, VariantSolution, EvidenceUnlockRule, ScenarioAsset 반영
- ai_generation_logs -> ai_call_logs(raw JDBC 기록) 정정
- ResponsePolicyResolver 입력값 정정
- prompt template의 publicAlibi/history/allowedFacts 반영
- system prompt 규칙 수와 injection 방어 설명 정정
- /api/ai/logs, /recommended-questions, draft API 등 미구현 상태 명확화
```

상태:

```text
완료. ERD/API/Backend/AI 정책 문서는 삭제하지 않고 역할을 유지하며 코드 SoT와 충돌하던 서비스명, 로그 테이블명, prompt 파일명, 정책 선택 조건만 정정했다.
```

## 5. 작업 순서 제안

### Phase 1. 문서 index와 정본 지도 생성

작업:

```text
- docs/README.md 생성
- 문서별 정본 범위 표 작성
- 흡수 예정 문서와 흡수처 표 작성
- 스포일러/secret/public repo 경계 표시
```

완료 기준:

```text
팀원이 어느 문서를 봐야 하는지 docs/README.md만 보고 판단 가능해야 한다.
```

### Phase 2. drift 먼저 정정

작업:

```text
- docs_vs_code_drift.md의 확정 항목을 정본 문서에 반영
- 코드 SoT와 충돌하는 오래된 서술 제거 또는 미구현 표기
- 문서 간 상호 모순 제거
```

완료 기준:

```text
문서 다이어트 전에 잘못된 내용이 큰 정본 문서로 흡수되지 않아야 한다.
```

### Phase 3. QA 문서 흡수

작업:

```text
- docs/QA_HANDOFF.md 생성
- MVP_QA_ISSUE_HANDOFF_2026-06-04.md의 미해결/해결 이슈 흡수 완료
- MVP_QA_ISSUE_HANDOFF_2026-06-04.md는 최종 링크 정리 후 제거
- 로컬 untracked MVP_PLAY_FLOW_QA_2026-06-04.md는 scrub 후 public-safe 결론만 흡수, 원문은 미커밋 유지
- PR #55 머지 후 2026-06-10 QA 문서들도 최신 상태로 흡수
```

완료 기준:

```text
날짜별 QA 문서를 열지 않아도 현재 미해결 이슈와 재검증 상태를 확인할 수 있어야 한다.
```

### Phase 4. Frontend/API 문서 흡수

작업:

```text
- ANDROID_SCREEN_API_MAPPING.md 내용을 frontend guide로 흡수
- API Spec은 계약 중심으로 정리
- frontend guide는 화면 흐름과 호출 순서 중심으로 정리
- ANDROID_SCREEN_API_MAPPING.md는 최종 링크 정리 후 제거
```

완료 기준:

```text
API 필드 확인은 API Spec, 화면 구현 순서는 frontend guide로 역할이 분리되어야 한다.
```

### Phase 5. Infra 문서 흡수

작업:

```text
- roadmap + scale-out 계획을 infrastructure strategy에 흡수
- rate limit dry-run과 backup/restore 실행 절차를 OPS_RUNBOOK에 흡수
- GeoIP/rate limit/Grafana alert 정책을 SECURITY_TRAFFIC_ALERT_POLICY로 통합
- 기존 세부 문서는 흡수 완료 후 최종 링크 정리 단계에서 제거
```

완료 기준:

```text
운영자가 명령어가 필요하면 OPS_RUNBOOK, 설계 판단이 필요하면 STRATEGY, 정책 기준이 필요하면 SECURITY_TRAFFIC_ALERT_POLICY를 보면 된다.
```

상태:

```text
완료. redirect 문서는 최종 링크 정리 단계에서 제거했다.
```

### Phase 6. Agent/LLMOps 문서 흡수

작업:

```text
- INFRA_CODEX_AGENT_PLAYBOOK + OPS_SNAPSHOT_SPEC + MONITORING_AGENT_PLAN을 INFRA_AGENT_OPERATING_GUIDE에 흡수
- LLMOPS_AGENT_PLAN + LLMOPS_PROMQL_QUERIES + LLMOPS_SMOKE_RUNBOOK을 LLMOPS_OPERATING_GUIDE로 통합
- 기존 세부 문서는 흡수 완료 후 최종 링크 정리 단계에서 제거
```

완료 기준:

```text
agent 운영 문서와 LLMOps 운영 문서가 각각 하나의 진입점만 가져야 한다.
```

상태:

```text
완료. redirect 문서는 최종 링크 정리 단계에서 제거했다.
```

### Phase 7. 링크/참조 정리

작업:

```text
- AGENTS.md 참조 문서 목록 갱신
- CLAUDE.md 참조 문서 목록 갱신
- 문서 내부 상대 링크 수정
- 제거된 파일명 검색 후 전부 새 위치로 교체
```

검증:

```powershell
git grep -n "ANDROID_SCREEN_API_MAPPING\|MVP_QA_ISSUE_HANDOFF\|RATE_LIMIT_DRY_RUN_RUNBOOK\|LLMOPS_PROMQL_QUERIES" -- .
```

완료 기준:

```text
AGENTS.md, CLAUDE.md, root README.md는 active 정본 문서만 가리킨다.
예전 문서명 검색 결과는 흡수 이력이나 정본 문서의 "기존 문서 흡수" 설명에만 남는다.
```

상태:

```text
완료. active 진입 문서는 새 정본 링크로 교체했고, 예전 파일명은 흡수 이력 맥락에만 남긴다.
```

## 6. 예상 결과

### 문서 수

```text
정리 전:
tracked docs Markdown 30개

정리 후:
active docs Markdown 19개

감소:
16개 redirect 문서 제거
```

### 흡수 후 제거 완료 문서

```text
- docs/ANDROID_SCREEN_API_MAPPING.md
- docs/MVP_QA_ISSUE_HANDOFF_2026-06-04.md
- docs/scenarios/README.md
- docs/infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md
- docs/infra/SCALE_OUT_POC_PLAN.md
- docs/infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md
- docs/infra/RATE_LIMIT_POLICY.md
- docs/infra/GEOIP_BOT_TRAFFIC_POLICY.md
- docs/infra/GRAFANA_ALERT_POLICY.md
- docs/infra/MYSQL_BACKUP_AND_RESTORE_POLICY.md
- docs/infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md
- docs/infra/agent/OPS_SNAPSHOT_SPEC.md
- docs/infra/agent/MONITORING_AGENT_PLAN.md
- docs/infra/agent/LLMOPS_AGENT_PLAN.md
- docs/infra/agent/LLMOPS_PROMQL_QUERIES.md
- docs/infra/agent/LLMOPS_SMOKE_RUNBOOK.md
```

위 목록은 고유 내용을 정본 문서에 흡수한 뒤 제거한 문서다.
원문 전체가 필요하면 Git history에서 이전 revision을 확인한다.

## 7. 리뷰 기준

문서 정리 PR은 다음 기준으로 리뷰한다.

```text
1. 삭제된 파일의 고유 내용이 어디로 흡수됐는가?
2. 새 정본 문서가 코드 SoT와 맞는가?
3. 같은 API/운영 절차가 두 문서에 서로 다르게 남아 있지 않은가?
4. 스포일러/secret/private seed가 public 문서로 새로 노출되지 않았는가?
5. AGENTS.md / CLAUDE.md / 문서 내부 링크가 깨지지 않았는가?
```

## 8. 권장 진행 방식

한 번에 전부 합치면 diff가 너무 커진다.
다음 순서로 작은 PR 또는 작은 커밋 단위로 진행하는 편이 안전하다.

```text
1. docs/README.md + QA_HANDOFF.md 생성
2. drift 정정만 별도 커밋
3. Frontend/API 문서 흡수
4. Infra 문서 흡수
5. Agent/LLMOps 문서 흡수
6. 링크 정리 + 제거 후보 파일 삭제
```

단, 실제 파일 삭제는 마지막 커밋에서 한다.
그 전 커밋에서는 흡수처 문서가 먼저 완성되어 있어야 한다.

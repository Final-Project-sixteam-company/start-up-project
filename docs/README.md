# ClueRoom Docs Index

## 0. 문서 사용 원칙

이 디렉터리는 ClueRoom 백엔드, Android 연동, AI 정책, 운영, QA 문서를 관리한다.
문서가 늘어나면서 같은 내용이 여러 파일에 반복되기 시작했으므로, 앞으로는 아래 원칙을 따른다.

```text
1. 한 주제의 정본 문서는 하나만 둔다.
2. 다른 문서에는 중복 설명 대신 정본 링크를 둔다.
3. 오래된 문서는 삭제 전에 정본 문서로 내용을 흡수한다.
4. 코드와 문서가 다르면 코드 SoT 기준으로 문서를 먼저 정정한다.
5. secret, private seed, 정답 상세, 운영 비밀은 public 문서에 추가하지 않는다.
```

문서 다이어트 계획은 `DOCS_CONSOLIDATION_PLAN_2026-06-10.md`를 기준으로 진행한다.

## 1. 빠른 진입점

| 목적 | 문서 |
|---|---|
| 전체 문서 지도와 흡수/제거 이력 확인 | `README.md` |
| 제품 범위와 MVP 목표 확인 | `CaseLab_AI_PRD.md` |
| API request/response 계약 확인 | `CaseLab_AI_API_Spec.md` |
| Android 화면 흐름과 API 호출 순서 확인 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` |
| 백엔드 패키지/계층/트랜잭션 규칙 확인 | `BACKEND_IMPLEMENTATION_GUIDE.md` |
| AI NPC 프롬프트와 정답 누설 방지 정책 확인 | `AI_NPC_PROMPT_POLICY.md` |
| DB/엔티티 설계 확인 | `CaseLab_AI_ERD_Design.md` |
| 로컬 실행, Android 연결, 배포 요약 확인 | `RUN_AND_DEPLOY.md` |
| 운영 명령어와 장애 대응 확인 | `infra/OPS_RUNBOOK.md` |
| 인프라 구조와 고도화 방향 확인 | `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` |
| 현재 QA 이슈와 재검증 항목 확인 | `QA_HANDOFF.md` |
| 공식 데모 시나리오 확인 | `OFFICIAL_SCENARIO_DEMO_DAY.md` |
| 시나리오 YAML 스키마 확인 | `scenarios/SCENARIO_YAML_SCHEMA.md` |

## 2. 정본 문서 지도

### 제품 / API / 구현

| 문서 | 정본 범위 | 비고 |
|---|---|---|
| `CaseLab_AI_PRD.md` | 제품 목적, MVP 범위, 사용자 가치, 팀 역할 | API/운영 상세는 링크로만 둔다. |
| `CaseLab_AI_API_Spec.md` | API path, method, request, response, error contract | 화면별 사용 순서는 frontend 문서로 분리 완료. |
| `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` | Android/Frontend 화면 흐름, 호출 순서, E2E 사용법 | 화면 흐름과 API 사용 순서 정본이다. |
| `BACKEND_IMPLEMENTATION_GUIDE.md` | 백엔드 계층, 패키지, 예외, 트랜잭션, MockUser 규칙 | API/ERD 상세는 정본 링크를 둔다. |
| `CaseLab_AI_ERD_Design.md` | 엔티티, 테이블, 관계, 컬럼 설계 | 코드 SoT와 drift 정정 필요. |
| `ADR.md` | 이미 결정된 주요 아키텍처 결정 목록 | 상세 설명은 각 정본 문서로 링크한다. |

### AI / 시나리오

| 문서 | 정본 범위 | 비고 |
|---|---|---|
| `AI_NPC_PROMPT_POLICY.md` | AI 지식 경계, prompt 구조, ResponsePolicy, injection 방어 | 실제 prompt template과 drift 정정 필요. |
| `OFFICIAL_SCENARIO_DEMO_DAY.md` | 공식 데모 시나리오 정본, 채점 기준 | 스포일러 포함 문서다. 공개 범위 주의. |
| `scenarios/SCENARIO_YAML_SCHEMA.md` | YAML import schema, 공개 범위, private seed 경계, MVP scoring bridge | 시나리오 문서 정본이다. |

### 실행 / 운영 / 인프라

| 문서 | 정본 범위 | 비고 |
|---|---|---|
| `RUN_AND_DEPLOY.md` | 로컬 실행, Docker, Android 연결, 배포 요약 | 상세 운영 명령은 runbook으로 링크한다. |
| `infra/OPS_RUNBOOK.md` | 운영 명령어, Blue-Green, 장애 대응, 백업/복구, rate limit dry-run 실행 절차 | 운영 절차 정본이다. |
| `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 인프라 선택 이유, 현재 구조, 확장 방향, roadmap, scale-out PoC, backup strategy | 인프라 전략 정본이다. |
| `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | rate limit, GeoIP/bot traffic, Grafana alert 정책 | 보안/트래픽/알림 정책 정본이다. |
| `infra/CLUEROOM_SECRET_INPUT_GUIDE.md` | 팀원 secret 입력 절차 | 민감 주제라 당장 유지한다. |

### Agent / LLMOps

| 문서 | 정본 범위 | 비고 |
|---|---|---|
| `infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` | 인프라 agent 권한, 금지 작업, 승인 기준, Codex playbook, Ops Snapshot, Monitoring Agent 운영 모델 | infra agent 문서군 정본이다. |
| `infra/agent/LLMOPS_OPERATING_GUIDE.md` | LLMOps 계획, telemetry safety, smoke 절차, PromQL 후보, agent 역할 | LLMOps 문서군 정본이다. |

### QA / Handoff

| 문서 | 정본 범위 | 비고 |
|---|---|---|
| `QA_HANDOFF.md` | 현재 미해결 QA 이슈, 해결된 이슈, 재검증 항목 | 새 정본이다. 날짜별 QA 문서의 결론을 흡수한다. |

## 3. 흡수/제거 이력

아래 문서는 고유 내용을 정본 문서에 흡수한 뒤 제거했다.
원문 전체가 필요하면 Git history에서 이전 revision을 확인한다.

| 제거 문서 | 흡수처 |
|---|---|
| `ANDROID_SCREEN_API_MAPPING.md` | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` |
| `MVP_QA_ISSUE_HANDOFF_2026-06-04.md` | `QA_HANDOFF.md` |
| `scenarios/README.md` | `scenarios/SCENARIO_YAML_SCHEMA.md`, `README.md` |
| `infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md` | `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` |
| `infra/SCALE_OUT_POC_PLAN.md` | `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` |
| `infra/MYSQL_BACKUP_AND_RESTORE_POLICY.md` | `infra/OPS_RUNBOOK.md`, `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` |
| `infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md` | `infra/OPS_RUNBOOK.md`, `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` |
| `infra/RATE_LIMIT_POLICY.md` | `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` |
| `infra/GEOIP_BOT_TRAFFIC_POLICY.md` | `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` |
| `infra/GRAFANA_ALERT_POLICY.md` | `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` |
| `infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md` | `infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` |
| `infra/agent/OPS_SNAPSHOT_SPEC.md` | `infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` |
| `infra/agent/MONITORING_AGENT_PLAN.md` | `infra/agent/INFRA_AGENT_OPERATING_GUIDE.md` |
| `infra/agent/LLMOPS_AGENT_PLAN.md` | `infra/agent/LLMOPS_OPERATING_GUIDE.md` |
| `infra/agent/LLMOPS_PROMQL_QUERIES.md` | `infra/agent/LLMOPS_OPERATING_GUIDE.md` |
| `infra/agent/LLMOPS_SMOKE_RUNBOOK.md` | `infra/agent/LLMOPS_OPERATING_GUIDE.md` |

## 4. 공개 범위와 스포일러 기준

```text
public 문서에 넣어도 되는 것:
- API 계약
- 일반 QA 결론
- 재현 절차
- 운영 명령어 중 secret을 포함하지 않는 것
- prompt 구조와 정책 원칙

public 문서에 넣으면 안 되는 것:
- private seed 원문
- 정답/범인/트릭/해설 전문 중 공개가 결정되지 않은 내용
- 실제 secret 값, token, key, PEM
- 운영 서버 민감 정보 전체 dump
- 사용자 질문/AI 응답 원문 로그
```

공식 시나리오 문서는 스포일러를 포함할 수 있으므로 링크 시 공개 범위를 함께 표시한다.

## 5. 문서 정리 검증

문서 정리 PR은 최소 아래를 확인한다.

```powershell
git diff --check
git grep -n "ANDROID_SCREEN_API_MAPPING\|MVP_QA_ISSUE_HANDOFF\|RATE_LIMIT_DRY_RUN_RUNBOOK\|LLMOPS_PROMQL_QUERIES" -- .
```

검색 결과가 나오면 흡수 이력 또는 정본 문서의 "기존 문서 흡수" 설명인지 확인한다.
active 정본을 안내해야 하는 문서에서 예전 파일명을 정본처럼 가리키면 안 된다.

문서를 삭제하는 PR에서는 삭제 파일의 고유 내용이 어느 정본 문서로 흡수됐는지 PR 본문에 적는다.

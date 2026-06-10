# Android Screen API Mapping

## Status

이 문서의 고유 내용은 `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md`로 흡수됐다.
Android/Frontend 화면별 API 호출 기준은 아래 정본 문서를 사용한다.

```text
docs/frontend/CLUEROOM_APP_FLOW_API_GUIDE.md
```

새 화면/API 연동 기준, 구현 우선순위, DTO 필드명 규칙, loading/empty/error 처리 기준은 이 파일이 아니라 정본 문서에 추가한다.

## Absorbed Content

| 기존 섹션 | 현재 위치 |
|---|---|
| Android 공통 규칙 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 2장 |
| ID 필드명 규칙 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 2.4 |
| Android 화면 구조 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 2.5 |
| 화면별 API 매핑 요약 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 3~9장 |
| 공식 시나리오 플레이 네비게이션 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 6~9장 |
| 이어하기 / active session 복구 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 6.6, 10.4, 10.5 |
| Android 구현 우선순위 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 10.9 |
| Android 개발 AI 규칙 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 10.10 |
| 체크리스트 | `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` 10.8~10.11 |

## Why This File Remains

문서 다이어트는 삭제가 아니라 흡수를 기본으로 한다.
이 파일은 기존 참조 링크가 깨지지 않도록 임시 redirect로 유지한다.
최종 링크 정리 단계에서 `AGENTS.md`, `CLAUDE.md`, 문서 내부 링크가 모두 정본 문서로 바뀌면 제거 후보로 전환한다.

## Original History

원문 전체가 필요하면 Git history에서 이 파일의 이전 revision을 확인한다.
단, 원문에는 미구현 API와 구현 API가 섞여 있으므로 현재 연동 기준으로 사용하지 않는다.

# ClueRoom QA Handoff

> 상태: 이 문서의 실행 기준과 현재 이슈 board는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)로 흡수됐다.
> 기존 링크 호환을 위해 파일은 유지한다.

## 정본 위치

QA를 새로 돌리거나 현재 미해결 이슈를 확인할 때는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)를 사용한다.

```text
흡수된 내용:
- MVP QA 현재 결론
- open/resolved/re-smoke checklist
- Backend/AI, Android/Frontend, Infra/Ops/Privacy owner별 follow-up
- 2026-06-10~2026-06-15 날짜별 QA 보고서의 반복 이슈
- public-safe/private artifact 기록 원칙
- #67 이후 public gameplay metadata 제거, coverUpText, activeSessionId, abandon, hints/import 재검증 항목
```

## 문서 역할

이 파일은 더 이상 정본 handoff가 아니다.

```text
- 새 QA 실행 지시: QA_OPERATING_GUIDE.md
- 새 보고서 템플릿: QA_OPERATING_GUIDE.md
- 현재 QA board: QA_OPERATING_GUIDE.md
- 날짜별 원문 판단: docs/qa/archive/*
```

## 흡수 원칙

날짜별 QA 결과에서 새로 살아남은 이슈는 이 파일이 아니라 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)의 `Current QA Board`에 반영한다.
public 문서에는 범인명, 정답 수법, 점수/등급, 정오/성공 여부, rejected-candidate rationale, raw sessionId/token을 남기지 않는다.

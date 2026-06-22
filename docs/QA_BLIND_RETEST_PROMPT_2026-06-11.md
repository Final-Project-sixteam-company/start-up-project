# ClueRoom Blind Retest Prompt - 2026-06-11

> 상태: 이 프롬프트는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)의 `복붙용 QA 프롬프트`와 `보고서 템플릿`으로 흡수됐다.
> 기존 링크 호환을 위해 파일은 유지한다.

## 정본 위치

새 blind QA를 시작할 때 QA operator는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)를 사용한다.
blind tester/agent에게는 `QA_OPERATING_GUIDE.md` 전체를 전달하지 않고, 7장의 fenced block만 복사해 전달한다.

## QA 계정 / 로그인

QA 계정 식별자는 public 문서에 직접 쓰지 않는다.
운영자는 blind tester/agent에게 private handoff로 QA 계정 접근 방법만 전달한다.

웹 QA에서 QA 로그인 버튼이 보이지 않으면 임의 개인 계정으로 우회하지 않고, 아래를 환경 blocker로 기록한다.

```text
QA login unavailable:
- frontend build env: VITE_ENABLE_QA_LOGIN / VITE_QA_LOGIN_EMAIL 확인 필요
- backend env: AUTH_DEV_LOGIN_ENABLED 확인 필요
```

정확한 실행 문구는 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)의 `복붙용 QA 프롬프트` 안의 `계정 / 로그인` 섹션을 사용한다.

이전 긴 프롬프트에서 반복되던 아래 규칙은 정본 문서로 이동했다.

```text
- Android 우선, API-only 보조 원칙
- API-only spoiler metadata masking 및 blind invalid 표기
- Swagger/API 문서의 solution/result 예시 열람 금지
- fresh session 생성과 P002/abandon fallback
- suggested-question chip prefill-only 기준
- 점수/등급/result breakdown private artifact 분리
- rejected candidate rationale private artifact 분리
- 10턴 단위 broad summary 기록 방식
- privacy spot check 필수 기록
```

## 사용 금지

이 파일의 이전 revision을 새 QA prompt로 복사하지 않는다.
긴 프롬프트가 필요하면 Git history가 아니라 [QA_OPERATING_GUIDE.md](QA_OPERATING_GUIDE.md)의 최신 프롬프트 fenced block만 복사한다.
Current QA Board, 역사 보고서, 기존 QA 결과 요약은 blind tester에게 전달하지 않는다.

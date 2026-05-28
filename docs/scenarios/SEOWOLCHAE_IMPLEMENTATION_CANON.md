# SEOWOLCHAE_IMPLEMENTATION_CANON_v1.1

> INTERNAL SPOILER DOCUMENT.  
> 이 문서는 서버 seed, 채점, 검증, Android UI 구현 기준을 맞추기 위한 내부 정본이다.  
> 범인, Variant Truth Layer, Solution, Proof Dimension이 포함되므로 AI NPC 프롬프트 입력값이나 `resources/prompts/` 템플릿으로 사용하지 않는다.

> 문서 목적: `CLUEROOM_SCENARIO_WORKING_BRIEF_v20.md`의 작업 히스토리/패치 로그를 걷어내고, **DB Seed / Android UI / AI 심문 프롬프트 / 최종 추리 채점**에 바로 연결하기 위한 서월채 구현 정본을 고정한다.  
> v1.1 목적: 레드팀/디렉터 피드백의 P0/P1 수정사항을 반영하여 **Role Matrix ↔ Proof Dimension 정합성, EvidenceVariantState 범위, MVP Fallback, AI 프롬프트 최적화 정책**을 확정한다.  
> 상태: `IMPLEMENTATION_CANON_V1.1`  
> 기준: 서월채의 마지막 처방 / ClueRoom 공식 시나리오 1번  
> 주의: 이 문서는 Working Brief를 대체하는 “구현 정본”이다. 이후 코드/DB/JSON/프롬프트에서는 이 문서의 canonical code만 사용한다.

---

## 0. 이 문서에서 고정하는 것

레드팀 피드백 반영 결과, 서월채 Working Brief는 더 이상 설정을 추가하는 문서가 아니라 **정본화 / 압축 / 구현 고정** 단계로 들어간다.

이 문서에서 고정하는 항목은 다음이다.

```text
1. 사건 정본 요약
2. 인물 5명 정본
3. 장소/위치 코드 정본
4. 공통 타임라인 정본
5. 4개 Variant Truth Table
6. 증거 25개 Canonical Code Table
7. Evidence Variant State 정책
8. AI NPC Knowledge Boundary
9. 최종 추리 채점 Proof Dimension
10. Android UI / DB 구현 기준
```

### v1.1 반영 요약

```text
P0 반영:
1. Role Matrix와 Proof Dimension의 관계를 분리 정의
2. SUPPORT_KEY role 추가
3. SECRETARY / SPOUSE / DOCTOR proof mismatch 정리
4. EvidenceVariantState 적용 범위 명확화
5. ScenarioVariant 구현 부담에 대한 MVP Fallback Plan 추가

P1 반영:
6. oldAliases 자기 자신 참조 제거
7. 케어매니저 오답 피드백 문구 추가
8. Doctor Variant의 수정된 야간 처방 메모 playerText 보강
9. Wearable Vital Log 해금 조건의 MVP 단순화 정책 추가
10. AI Prompt Spec에서 RAG식 evidenceReactionPolicy 주입 권장
```

---

## 1. 사건 정본 요약

```yaml
scenarioCode: SCENARIO_SEOWOLCHAE_LAST_PRESCRIPTION
title: "서월채의 마지막 처방"
genre: "폐쇄 저택 독살 미스터리"
tone: "의료재단 권력형 스캔들 / 고급 VIP 별장 / 심리전 / 증거 조합"
estimatedPlayTimeMinutes: 30
difficulty: NORMAL
culpritMode: RANDOM_REQUIRED
culpritCandidateCount: 4
neutralWitnessCount: 1
interviewableCharacterCount: 5
mapModeMvp: REFERENCE_ONLY
evidenceModeMvp: PHASE_BASED_EVIDENCE_TAB
```

### 사건 개요

서광의료재단 이사장 **차민혁**은 이사회 전날, 강원도 산중의 VIP 별장동 **서월채**로 핵심 관계자들을 부른다. 겉으로는 내부 감사 대응과 책임 정리를 위한 비공식 조정 회의였지만, 실제로는 재단 위기에서 자신을 보호하기 위해 주변 인물들에게 책임을 떠넘기려는 자리였다.

회의와 만찬 이후 차민혁은 2층 이사장 침실로 올라간다. 얼마 지나지 않아 VIP 케어 호출 패널이 작동하고, 현장에 있던 사람들은 그가 지병 또는 약물 부작용으로 쓰러졌다고 주장한다.

그러나 침실의 물병과 컵, 야간 약통, 와인 디캔터, 약품 보관함 로그, 보안 서버 재동기화 로그, 복약 알림/스누즈 로그, 웨어러블 바이탈 원시 데이터는 단순한 급사가 아님을 보여준다.

### 핵심 미스터리

```text
1. 차민혁은 무엇을 통해 치명적인 독성 반응에 이르렀는가?
2. 그 물건 또는 시스템 지시에 접근한 사람은 누구인가?
3. 각 인물이 숨긴 행동은 살인 은폐인가, 자기 비밀 은폐인가?
4. 웨어러블 바이탈 로그의 세 시간대 중 어떤 변화가 진짜 치명 경로와 맞물리는가?
5. 누가 피해자의 죽음으로 가장 직접적인 이득을 보는가?
```

---

## 2. 인물 정본

### 2-1. 피해자

```yaml
victimCode: VICTIM_CHA_MINHYUK
name: 차민혁
age: 58
role: 서광의료재단 이사장
publicProfile: "공익 의료사업과 기부로 알려진 의료재단 권력자"
hiddenProfile: "주변 사람들의 약점과 책임 문서를 쥐고 통제하던 인물"
deathLocationCode: LOC_DIRECTOR_SUITE
surfaceCause: "심장질환 또는 약물 부작용처럼 보이는 급성 쇼크"
actualCauseCategory: "섭취물 조작 또는 케어/처방 지시 조작으로 인한 독성 반응"
```

### 2-2. 범인 가능 인물 4명

| characterCode | 역할 우선 표기 | 이름 | 범인 가능 | 핵심 비밀 |
|---|---|---:|---:|---|
| `SUSPECT_SPOUSE` | 배우자 / 재단 홍보이사 | 윤서하 | true | 갤러리 운영권 약속과 재산분할 협상 뒤에 숨은 횡령 고발 준비 |
| `SUSPECT_SECRETARY` | 비서실장 / 이사장실 실무 책임자 | 한지오 | true | 비밀 장부와 내부 감사 책임을 독박으로 떠안게 될 위기 |
| `SUSPECT_DOCTOR` | 예비병원장 / 주치의 | 서태준 | true | 과거 VIP 의료사고 및 처방 기록 조작 은폐 |
| `SUSPECT_SECURITY` | 서월채 특수보안팀장 | 오민석 | true | 비공식 녹취/영상 관리 책임을 단독 책임으로 떠넘겨질 위기 |

### 2-3. 중립 참고인 1명

| characterCode | 역할 우선 표기 | 이름 | 범인 가능 | 설계 역할 |
|---|---|---:|---:|---|
| `WITNESS_CARE_MANAGER` | 케어매니저 / 야간 건강 체크 담당 | 문하연 | false | 바이탈 로그 해금 담당, 시간축 보정, 수상한 중립 참고인 |

### 2-4. 최종 선택 UI 정책

```yaml
culpritRandomPool:
  - SUSPECT_SPOUSE
  - SUSPECT_SECRETARY
  - SUSPECT_DOCTOR
  - SUSPECT_SECURITY

finalDeductionSelectableCharacters:
  - SUSPECT_SPOUSE
  - SUSPECT_SECRETARY
  - SUSPECT_DOCTOR
  - SUSPECT_SECURITY
  - WITNESS_CARE_MANAGER

neutralWitnessAnswerPolicy:
  WITNESS_CARE_MANAGER:
    selectableInFinalDeduction: true
    canBeCorrectCulprit: false
    selectingAsCulprit: "오답 처리"
```

---

## 3. 장소 / Location Code 정본

MVP에서는 맵을 직접 탐색하지 않는다. 평면도는 참고용이며, 증거는 증거 탭에서 공개된다.  
`locationCode`는 다음 용도로만 사용한다.

```text
1. 증거 출처 표시
2. 장소별 필터
3. 타임라인/동선 검증
4. 향후 맵 터치 탐색 고도화의 기준점
```

| locationCode | 표시명 | 층 | MVP 용도 |
|---|---|---:|---|
| `LOC_ENTRANCE_HALL` | 현관홀 | 1F | 공통 동선 배경 |
| `LOC_MEETING_ROOM` | 회의실 | 1F | 회의 안건/동기 문서 출처 |
| `LOC_DINING_ROOM` | 만찬장 | 1F | 좌석표, 디캔터/와인잔 출처 |
| `LOC_KITCHEN_PREP` | 주방 / 보조 준비실 | 1F | 물병 서비스 체크리스트 출처 |
| `LOC_SECURITY_ROOM` | 보안실 | 1F | 보안 서버 로그, 책임전가 문서 출처 |
| `LOC_WINE_CELLAR` | 지하 와인셀러 | B1 | 와인셀러 로그, 실링 조각 출처 |
| `LOC_DIRECTOR_SUITE` | 이사장 침실 | 2F | 현장 사진, 물병/컵, 약통, 호출 패널 |
| `LOC_MEDICAL_ROOM` | 간이진료실 / 약품 보관실 | 2F | 약품 보관함 로그, 처방 메모, VIP 사고 파일 |
| `LOC_CARE_STATION` | 케어 스테이션 | 2F | 케어 로그, 바이탈 로그, 제보 초안 |
| `LOC_SECOND_FLOOR_CORRIDOR` | 2층 중앙 복도 | 2F | CCTV 사각지대, 부분 CCTV, 상호 목격 |
| `LOC_GUEST_ROOM_SPOUSE` | 배우자 게스트룸 | 2F | 찢어진 약포장 |

---

## 4. 공통 공개 타임라인 정본

이 타임라인은 모든 Variant에서 공통으로 공개되는 큰 사건 흐름이다.  
세부 진실은 activeVariant에 따라 달라진다.

| 시간 | 공개 사건 |
|---:|---|
| 18:30 | 특수보안팀장 오민석이 서월채에 선도착해 보안/출입 시스템을 점검한다. |
| 18:40 | 비서실장 한지오가 회의 자료와 만찬 준비 상태를 확인한다. |
| 18:50 | 케어매니저 문하연이 2층 케어 스테이션 야간 근무를 시작한다. |
| 19:00 | 배우자 윤서하와 예비병원장 서태준이 도착한다. |
| 19:10 | 차민혁 주재 비공식 조정 회의가 시작된다. |
| 19:45 | 차민혁이 각 인물에게 불리한 책임 정리 또는 인사 결정을 암시한다. |
| 20:20 | 회의가 종료되고 인물들이 만찬장으로 이동한다. |
| 20:35 | 만찬이 시작된다. |
| 20:48 | 관리자 권한 카드로 와인셀러 출입 기록이 남는다. |
| 21:02 | 차민혁이 2층 이사장 침실로 이동한다. |
| 21:05~21:35 | 핵심 공백 구간. 여러 인물이 각자 숨기는 행동을 한다. |
| 21:15 | 침실 복약 알림이 울린다. 실제 복용 완료 시각은 아니다. |
| 21:37 | VIP 케어 호출 패널이 작동한다. |
| 21:40 | 차민혁이 침실에서 의식 저하 상태로 발견된다. |
| 21:55 | 외부 신고 여부를 두고 현장 인물들이 충돌한다. |

### 바이탈 원시 데이터 공통 규칙

`EVIDENCE_WEARABLE_VITAL_RAW_LOG`는 범인을 직접 알려주는 정답표가 아니다. 시간축 필터다.

| 시간 | 원시 데이터 |
|---:|---|
| 20:58 | 혈압 미세 하락 / 1차 부정맥성 흔들림. 알코올, 스트레스, 초기 반응 모두 가능. |
| 21:18 | 호흡수 불안정 / 체온 상승 추세. 물병, 복약 루틴, 진행 중 반응 모두 가능. |
| 21:29 | 급성 빈맥 / 생체 신호 급강하. 약통, 처방 지시, 지연 복약, 케어 실패와 연결 가능. |

---

## 5. Variant Truth Table 정본

> 주의: 이 섹션은 **게임 엔진 / 채점 / 정답 해설용 Truth Layer**다.  
> AI NPC 프롬프트에 activeVariant, culpritCode, fullSolution을 직접 넣지 않는다.

---

### 5-1. `VARIANT_SECURITY`

```yaml
variantCode: VARIANT_SECURITY
culpritCode: SUSPECT_SECURITY
culpritRole: "서월채 특수보안팀장"
culpritName: "오민석"
mainMethodLayer: "와인셀러 / 이사장 전용 디캔터 / 와인잔"
coreTimeWindow:
  access: "20:48 관리자 권한 카드로 와인셀러 접근"
  earlySignal: "20:58 바이탈 1차 이상 반응"
  coverUp: "21:24 보안 서버 재동기화"
motive: "차민혁이 비공식 녹취/영상 관리 책임을 오민석의 독단적 일탈로 넘기려 했고, 오민석은 형사 책임과 경력 파탄을 피하려 했다."
coverUp: "보안 서버 재동기화를 시스템 점검처럼 위장해 1층/B1 동선 메타데이터를 흐린다."
```

#### 핵심 증거

```text
TIME_PROOF:
- EVIDENCE_WEARABLE_VITAL_RAW_LOG

METHOD_PROOF:
- EVIDENCE_DECANTER_SEAL_FRAGMENT
- EVIDENCE_DIRECTOR_DECANTER_AND_WINE_GLASS

ACCESS_PROOF:
- EVIDENCE_WINE_CELLAR_ACCESS_LOG

COVERUP_PROOF:
- EVIDENCE_SECURITY_SERVER_RESYNC_LOG

MOTIVE_PROOF:
- EVIDENCE_SECURITY_SCAPEGOAT_DRAFT
```

#### 오답 유도 증거

```text
- EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP
- EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE
- EVIDENCE_MEDICAL_CABINET_ACCESS_LOG
- EVIDENCE_DIVORCE_PROPERTY_DRAFT
```

#### 다른 인물이 아닌 이유

```text
배우자:
- 약통/스누즈 의심은 강하지만 20:58의 초기 이상 반응과 직접 맞물리지 않는다.

비서실장:
- 물병 흔적은 수상하지만 핵심 이상 반응이 물병 섭취 전부터 시작된다.

예비병원장:
- 약품 보관함 개봉은 과거 의료사고 은폐와 연결되지만, 와인셀러 접근/디캔터 흔적과 시간축이 더 강하다.

케어매니저:
- 바이탈 로그를 숨긴 중립 참고인이지만, 치명 물건이나 와인셀러 접근과 연결되지 않는다.
```

#### 최종 해설 5문장

```text
차민혁의 첫 이상 반응은 만찬 후반인 20:58경 이미 바이탈 원시 데이터에 나타났다.
이 시간대는 침실 물병, 야간 약통, 처방 메모 조작보다 앞선다.
와인셀러 관리자 권한 출입 기록과 디캔터 실링 조각은 이사장 전용 디캔터가 먼저 조작되었음을 보여준다.
21:24 보안 서버 재동기화는 살인 자체보다 자신의 1층/B1 동선을 흐리기 위한 은폐 행동이었다.
따라서 이 Variant에서 진범은 특수보안팀장 오민석이다.
```

---

### 5-2. `VARIANT_SECRETARY`

```yaml
variantCode: VARIANT_SECRETARY
culpritCode: SUSPECT_SECRETARY
culpritRole: "비서실장 / 이사장실 실무 책임자"
culpritName: "한지오"
mainMethodLayer: "침실 물병 / 컵 / 서비스 체크리스트"
coreTimeWindow:
  manipulation: "21:07 침실 물병/컵 확인 및 서비스 기록 조작"
  reminder: "21:15 복약 알림"
  ingestionLikely: "21:17~21:19 물 섭취"
  reaction: "21:18 호흡수/체온 이상 시작"
motive: "차민혁이 비자금 흐름과 비밀 장부 책임을 한지오 개인 책임으로 몰아 내부 감사의 희생양으로 삼으려 했다."
coverUp: "침실 물병 정리를 업무상 루틴으로 위장하고, 체크리스트 시간을 사후 정리 기록처럼 보이게 한다."
```

#### 핵심 증거

```text
TIME_PROOF:
- EVIDENCE_WEARABLE_VITAL_RAW_LOG
- EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG

METHOD_PROOF:
- EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP
- EVIDENCE_WATER_SERVICE_CHECKLIST

MOTIVE_PROOF:
- EVIDENCE_SECRET_LEDGER_AUDIT_NOTICE

ACCESS_PROOF:
- EVIDENCE_CARE_CALL_PANEL_LOG
- EVIDENCE_SECOND_FLOOR_CCTV_STILL
```

#### 오답 유도 증거

```text
- EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE
- EVIDENCE_MEDICAL_CABINET_ACCESS_LOG
- EVIDENCE_DIVORCE_PROPERTY_DRAFT
- EVIDENCE_SECURITY_SERVER_RESYNC_LOG
```

#### 다른 인물이 아닌 이유

```text
배우자:
- 2층 접근과 약통 의심은 강하지만, 21:18의 호흡수/체온 이상은 물 섭취 구간과 더 잘 맞는다.

예비병원장:
- 약품 보관함 개봉은 존재하지만, 수정된 처방 메모가 결정적으로 치명 경로와 맞지 않는다.

특수보안팀장:
- 와인셀러 로그는 수상하지만 20:58 신호가 결정타가 되지 않고, 물병/체크리스트 조합이 더 직접적이다.

케어매니저:
- 케어 로그를 숨겼지만 치명 물건인 물병/컵을 조작한 흔적은 없다.
```

#### 최종 해설 5문장

```text
차민혁은 21:15 복약 알림 직후 물을 마신 것으로 추정된다.
21:18의 바이탈 변화는 만찬 후반의 미세 이상보다 침실 물병 경로와 더 밀접하다.
침실 물병의 닦인 흔적과 서비스 체크리스트의 수정 흔적은 비서실장의 21:07 행동과 연결된다.
비밀 장부 내부 감사 통보는 한지오가 차민혁에게 책임을 뒤집어쓸 위기에 있었다는 동기를 보강한다.
따라서 이 Variant에서 진범은 비서실장 한지오다.
```

---

### 5-3. `VARIANT_SPOUSE`

```yaml
variantCode: VARIANT_SPOUSE
culpritCode: SUSPECT_SPOUSE
culpritRole: "배우자 / 재단 홍보이사"
culpritName: "윤서하"
mainMethodLayer: "개인 야간 약통 / 복약 알림 스누즈 / 찢어진 약포장"
coreTimeWindow:
  reminder: "21:15 복약 알림"
  hiddenAccess: "21:17~21:23 배우자 2층 접근"
  delayedMedication: "21:25 전후 지연 복약"
  reaction: "21:29 급성 악화"
motive: "차민혁이 쇼윈도 부부 유지와 갤러리 운영권을 약속했지만, 뒤로는 윤서하의 갤러리 관련 비리 자료를 외부 제출용으로 정리하고 있었다."
coverUp: "침실 접근을 갤러리 USB를 찾기 위한 행동으로 축소하고, 약포장 흔적을 게스트룸 안에 숨긴다."
```

#### 핵심 증거

```text
TIME_PROOF:
- EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG
- EVIDENCE_WEARABLE_VITAL_RAW_LOG

METHOD_PROOF:
- EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE
- EVIDENCE_NIGHT_PILL_PACKAGE_TEAR

MOTIVE_PROOF:
- EVIDENCE_DIVORCE_PROPERTY_DRAFT

ACCESS_PROOF:
- EVIDENCE_SECOND_FLOOR_CCTV_STILL
- EVIDENCE_CROSS_WITNESS_STATEMENT_CARD
```

#### 오답 유도 증거

```text
- EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP
- EVIDENCE_MEDICAL_CABINET_ACCESS_LOG
- EVIDENCE_SECRET_LEDGER_AUDIT_NOTICE
- EVIDENCE_WINE_CELLAR_ACCESS_LOG
```

#### 다른 인물이 아닌 이유

```text
비서실장:
- 물병 흔적은 수상하지만, 21:25 지연 복약과 21:29 급성 악화가 약통 루트와 더 강하게 맞는다.

예비병원장:
- 처방/약품 보관함 접근은 수상하지만, 직접 약통 접촉과 찢어진 약포장 상태가 배우자 쪽으로 더 강하다.

특수보안팀장:
- 20:58 신호는 미세하고 불명확하며, 최종 급강하는 지연 복약 이후에 발생한다.

케어매니저:
- 스누즈/바이탈 로그를 숨겼지만 약통 또는 약포장 조작 주체는 아니다.
```

#### 최종 해설 5문장

```text
21:15 알림은 실제 복용 완료 시각이 아니라 복약 알림 발생 시각이었다.
스누즈 기록은 차민혁이 약을 뒤늦게 복용했음을 보여준다.
배우자의 21:17~21:23 2층 접근과 찢어진 약포장 흔적은 이 지연 복약 구간과 맞물린다.
이혼·재산분할 문서는 윤서하가 차민혁에게 속고 있었다는 동기를 보여준다.
따라서 이 Variant에서 진범은 배우자 윤서하다.
```

---

### 5-4. `VARIANT_DOCTOR`

```yaml
variantCode: VARIANT_DOCTOR
culpritCode: SUSPECT_DOCTOR
culpritRole: "예비병원장 / 주치의"
culpritName: "서태준"
mainMethodLayer: "수정된 야간 처방 메모 / 케어 지시 계층 / 약품 보관함"
coreTimeWindow:
  access: "21:13 약품 보관함 개봉"
  instructionLayer: "야간 처방 메모 수정"
  reaction: "21:29 급성 악화"
motive: "차민혁이 과거 VIP 의료사고 책임을 서태준에게 넘기고, 의사 경력과 병원장 승계를 무너뜨릴 외부 제출용 문서를 준비했다."
coverUp: "약품 보관함 개봉을 의학적 확인 루틴으로 설명하고, 처방 메모 수정은 과거 기록 정리였다고 축소한다."
```

#### 핵심 증거

```text
TIME_PROOF:
- EVIDENCE_WEARABLE_VITAL_RAW_LOG

METHOD_PROOF:
- EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO
- EVIDENCE_MEDICAL_CABINET_ACCESS_LOG

MOTIVE_PROOF:
- EVIDENCE_VIP_PATIENT_INCIDENT_FILE

SUPPORT_PROOF:
- EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG
- EVIDENCE_CARE_CALL_PANEL_LOG
```

#### 오답 유도 증거

```text
- EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP
- EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE
- EVIDENCE_DECANTER_SEAL_FRAGMENT
- EVIDENCE_DIVORCE_PROPERTY_DRAFT
```

#### 다른 인물이 아닌 이유

```text
배우자:
- 약통 의심은 강하지만, 이 Variant에서는 직접 약통 조작보다 의료 지시 계층 조작이 치명 경로다.

비서실장:
- 물병 흔적은 수상하지만, 수정된 처방 메모와 21:13 약품 보관함 개봉이 더 강하다.

특수보안팀장:
- 보안/와인셀러 흔적은 은폐성 페이크로 남지만, 치명 반응의 마지막 급강하와 처방 지시가 더 밀접하다.

케어매니저:
- 바이탈 로그를 숨긴 중립 참고인이지만 처방 메모를 수정할 권한이나 동기가 없다.
```

#### 최종 해설 5문장

```text
서태준의 범행은 특정 약품 설명이 아니라 케어 지시 계층을 바꾼 방식으로 이해해야 한다.
21:13 약품 보관함 개봉과 수정된 야간 처방 메모는 피해자의 야간 복약 루틴에 개입한 흔적이다.
21:29 급성 악화는 이 수정된 지시가 실제 상황에 영향을 준 시점과 맞물린다.
과거 VIP 환자 사고 파일은 서태준이 차민혁에게 버려질 위기에 있었다는 동기를 보여준다.
따라서 이 Variant에서 진범은 예비병원장 서태준이다.
```

---

## 6. Evidence Canonical Code Table 정본

### 6-1. Evidence Category Enum

```java
enum EvidenceCategory {
    SCENE,
    PHYSICAL,
    DOCUMENT,
    DIGITAL_LOG,
    TESTIMONY,
    MAP
}
```

### 6-2. Evidence Unlock Phase Enum

| internalPhase | UI 표시 권장명 |
|---|---|
| `PHASE_0_OPENING` | 초기 현장 자료 |
| `PHASE_1_BASIC_OBJECTS` | 현장 물건 증거 |
| `PHASE_2_SYSTEM_LOGS` | 시스템 기록 |
| `PHASE_3_MOTIVE_AND_CONTRADICTION` | 관계/동기 문서 |
| `PHASE_4_KILLING_BLOW` | 결정적 기록 |

> UI에 `PHASE_1`, `PHASE_2` 같은 내부명을 직접 노출하지 않는다.

### 6-3. Canonical Evidence Table

> 코드/DB/JSON/AI 프롬프트에서는 **canonicalCode만 사용**한다. oldAliases는 문서 히스토리 추적용이다.

| canonicalCode | title | phase | category | locationCode | oldAliases |
|---|---|---|---|---|---|
| `EVIDENCE_DIRECTOR_SUITE_SCENE_PHOTO` | 이사장 침실 현장 사진 | PHASE_0_OPENING | SCENE | `LOC_DIRECTOR_SUITE` | - |
| `EVIDENCE_PRIVATE_ADJUSTMENT_AGENDA` | 비공식 조정 회의 안건 | PHASE_0_OPENING | DOCUMENT | `LOC_MEETING_ROOM` | `EVIDENCE_PRIVATE_MEETING_AGENDA` |
| `EVIDENCE_DINING_SEATING_CHART` | 만찬 좌석 배치표 | PHASE_1_BASIC_OBJECTS | DOCUMENT | `LOC_DINING_ROOM` | - |
| `EVIDENCE_SECOND_FLOOR_CCTV_BLIND_MAP` | 2층 CCTV 사각지대 평면도 | PHASE_0_OPENING | MAP | `LOC_SECOND_FLOOR_CORRIDOR` | `EVIDENCE_CCTV_BLIND_SPOT_MAP` |
| `EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP` | 침실 협탁의 물병과 컵 | PHASE_1_BASIC_OBJECTS | PHYSICAL | `LOC_DIRECTOR_SUITE` | `EVIDENCE_WATER_BOTTLE_CUP`, `EVIDENCE_BEDSIDE_WATER_BOTTLE` |
| `EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE` | 이사장 개인 야간 약통 | PHASE_1_BASIC_OBJECTS | PHYSICAL | `LOC_DIRECTOR_SUITE` | `EVIDENCE_NIGHT_PILL_CASE`, `EVIDENCE_DIRECTOR_NIGHT_MEDICINE_CASE` |
| `EVIDENCE_DIRECTOR_DECANTER_AND_WINE_GLASS` | 이사장 전용 디캔터와 와인잔 | PHASE_1_BASIC_OBJECTS | PHYSICAL | `LOC_DINING_ROOM` | `EVIDENCE_DECANTER_WINE_GLASS` |
| `EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG` | 침실 복약 알림/스누즈 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_CARE_STATION` | `EVIDENCE_NIGHT_MEDICATION_REMINDER` |
| `EVIDENCE_CARE_CALL_PANEL_LOG` | VIP 케어 호출 패널 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_CARE_STATION` | `EVIDENCE_VIP_CARE_CALL_PANEL_LOG` |
| `EVIDENCE_MEDICAL_CABINET_ACCESS_LOG` | 약품 보관함 개봉 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_MEDICAL_ROOM` | - |
| `EVIDENCE_WINE_CELLAR_ACCESS_LOG` | 와인셀러 관리자 권한 카드 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_WINE_CELLAR` | `EVIDENCE_WINE_CELLAR_CARDKEY_LOG` |
| `EVIDENCE_SECURITY_SERVER_RESYNC_LOG` | 보안 서버 재동기화 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_SECURITY_ROOM` | - |
| `EVIDENCE_CARE_STATION_ACCESS_LOG` | 케어 스테이션 접근 로그 | PHASE_2_SYSTEM_LOGS | DIGITAL_LOG | `LOC_CARE_STATION` | - |
| `EVIDENCE_SECOND_FLOOR_CCTV_STILL` | 2층 복도 부분 CCTV 스틸컷 | PHASE_3_MOTIVE_AND_CONTRADICTION | DIGITAL_LOG | `LOC_SECOND_FLOOR_CORRIDOR` | - |
| `EVIDENCE_CROSS_WITNESS_STATEMENT_CARD` | 상호 목격 진술 카드 | PHASE_3_MOTIVE_AND_CONTRADICTION | TESTIMONY | `LOC_SECOND_FLOOR_CORRIDOR` | `EVIDENCE_CROSS_WITNESS_CARD` |
| `EVIDENCE_DIVORCE_PROPERTY_DRAFT` | 이혼·재산분할 합의서 초안 | PHASE_3_MOTIVE_AND_CONTRADICTION | DOCUMENT | `LOC_MEETING_ROOM` | `EVIDENCE_DIVORCE_ASSET_DRAFT` |
| `EVIDENCE_SECRET_LEDGER_AUDIT_NOTICE` | 비밀 장부 내부 감사 통보 | PHASE_3_MOTIVE_AND_CONTRADICTION | DOCUMENT | `LOC_MEETING_ROOM` | - |
| `EVIDENCE_VIP_PATIENT_INCIDENT_FILE` | 과거 VIP 환자 사고 파일 | PHASE_3_MOTIVE_AND_CONTRADICTION | DOCUMENT | `LOC_MEDICAL_ROOM` | `EVIDENCE_VIP_PATIENT_ACCIDENT_FILE` |
| `EVIDENCE_SECURITY_SCAPEGOAT_DRAFT` | 특수보안팀장 책임전가 지시서 | PHASE_3_MOTIVE_AND_CONTRADICTION | DOCUMENT | `LOC_SECURITY_ROOM` | - |
| `EVIDENCE_CARE_MANAGER_WHISTLEBLOWER_DRAFT` | 케어매니저 제보 초안 | PHASE_3_MOTIVE_AND_CONTRADICTION | DOCUMENT | `LOC_CARE_STATION` | - |
| `EVIDENCE_WEARABLE_VITAL_RAW_LOG` | 웨어러블 바이탈 원시 데이터 | PHASE_4_KILLING_BLOW | DIGITAL_LOG | `LOC_CARE_STATION` | `EVIDENCE_WEARABLE_VITAL_LOG` |
| `EVIDENCE_NIGHT_PILL_PACKAGE_TEAR` | 배우자 게스트룸의 찢어진 약포장 | PHASE_4_KILLING_BLOW | PHYSICAL | `LOC_GUEST_ROOM_SPOUSE` | `EVIDENCE_TORN_PILL_PACKAGING` |
| `EVIDENCE_WATER_SERVICE_CHECKLIST` | 침실 물병 서비스 체크리스트 | PHASE_4_KILLING_BLOW | DOCUMENT | `LOC_KITCHEN_PREP` | - |
| `EVIDENCE_DECANTER_SEAL_FRAGMENT` | 와인셀러 선반의 디캔터 실링 조각 | PHASE_4_KILLING_BLOW | PHYSICAL | `LOC_WINE_CELLAR` | - |
| `EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO` | 수정된 야간 처방 메모 | PHASE_4_KILLING_BLOW | DOCUMENT | `LOC_MEDICAL_ROOM` | - |

### 6-4. Player-facing Text 보강 대상

`EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO`는 Doctor Variant의 핵심 증거지만, “처방 지시 계층 조작”이 다른 물리 증거보다 추상적이다.  
따라서 카드 상세 텍스트는 다음 수준까지 명확해야 한다.

```text
간이진료실 데스크에서 발견된 이사장의 야간 케어 지시서.
메모의 마지막 줄, '21:15 야간 약 복용 전후 투여 금지' 항목 위로
다른 잉크 농도와 다급한 필압이 덧씌워져 있다.
수정된 문장은 기존 야간 케어 루틴의 순서를 무력화하는 내용이며,
하단에는 약품 보관함 개봉 시각과 가까운 시간대의 확인 표시가 남아 있다.
```

표현 금지:

```text
- 특정 약품명, 용량, 실제 범죄 수법에 가까운 설명
- “의사가 죽이려고 수정했다” 같은 추론형 문장
- 범인 확정 표현
```

---

## 7. Evidence Role Matrix 정본

### 7-1. Role Enum

```text
COMMON
KEY
TIME_KEY
METHOD_KEY
ACCESS_KEY
MOTIVE_KEY
COVERUP_KEY
UNLOCK_KEY
DIRTY_FAKE
SUPPORT
SUPPORT_KEY
EXCLUSION
```

#### Role Enum 사용 기준

```text
KEY / *_KEY:
- 해당 Variant에서 정답 추론의 주 증거.
- 채점 Proof Dimension의 primary evidence로 인정 가능.

SUPPORT_KEY:
- 단독으로 정답 차원을 만점 처리하지는 않지만,
  primary evidence와 함께 선택되면 특정 Proof Dimension을 보강하는 보조 핵심 증거.
- 예: 비서실장 Variant의 2층 CCTV 스틸컷, 배우자 Variant의 상호 목격 진술 카드.

SUPPORT / COMMON:
- 배경/맥락/해금/보조 증거.
- 채점에서 secondary evidence로 인정 가능하지만,
  단독으로는 Proof Dimension 만점 처리하지 않는다.

DIRTY_FAKE:
- 해당 Variant에서는 최종 정답 증거가 아니지만,
  플레이어에게 충분히 수상하게 보여야 하는 오답 유도 증거.

EXCLUSION:
- 특정 가설을 제외하거나 시간축을 좁히는 데 사용되는 증거.
```

#### Role Matrix와 Proof Dimension의 관계

```text
Role Matrix = 게임 내 체감/기획 역할
Proof Dimension = 최종 채점에서 이 증거가 무엇을 증명하는지

둘은 1:1 매핑이 아니다.
SUPPORT_KEY / SUPPORT / COMMON 증거도 Proof Dimension의 secondary evidence가 될 수 있다.
단, ACCESS_KEY / METHOD_KEY / TIME_KEY 같은 primary evidence 없이
SUPPORT 계열 증거만으로 해당 dimension을 만점 처리하지 않는다.
```

### 7-2. Variant별 역할 매트릭스

| evidenceCode | SECURITY | SECRETARY | SPOUSE | DOCTOR |
|---|---|---|---|---|
| `EVIDENCE_DIRECTOR_SUITE_SCENE_PHOTO` | COMMON | COMMON | COMMON | COMMON |
| `EVIDENCE_PRIVATE_ADJUSTMENT_AGENDA` | COMMON | COMMON | COMMON | COMMON |
| `EVIDENCE_DINING_SEATING_CHART` | COMMON | COMMON | COMMON | COMMON |
| `EVIDENCE_SECOND_FLOOR_CCTV_BLIND_MAP` | SUPPORT | SUPPORT | SUPPORT | SUPPORT |
| `EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP` | DIRTY_FAKE | METHOD_KEY | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE` | DIRTY_FAKE | DIRTY_FAKE | METHOD_KEY | DIRTY_FAKE |
| `EVIDENCE_DIRECTOR_DECANTER_AND_WINE_GLASS` | METHOD_KEY | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG` | EXCLUSION | TIME_KEY | TIME_KEY | SUPPORT_KEY |
| `EVIDENCE_CARE_CALL_PANEL_LOG` | COMMON | SUPPORT_KEY | COMMON | SUPPORT_KEY |
| `EVIDENCE_MEDICAL_CABINET_ACCESS_LOG` | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE | ACCESS_KEY |
| `EVIDENCE_WINE_CELLAR_ACCESS_LOG` | ACCESS_KEY | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_SECURITY_SERVER_RESYNC_LOG` | COVERUP_KEY | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_CARE_STATION_ACCESS_LOG` | UNLOCK_KEY | UNLOCK_KEY | UNLOCK_KEY | UNLOCK_KEY |
| `EVIDENCE_SECOND_FLOOR_CCTV_STILL` | SUPPORT | SUPPORT_KEY | ACCESS_KEY | SUPPORT |
| `EVIDENCE_CROSS_WITNESS_STATEMENT_CARD` | SUPPORT | SUPPORT | SUPPORT_KEY | SUPPORT |
| `EVIDENCE_DIVORCE_PROPERTY_DRAFT` | DIRTY_FAKE | DIRTY_FAKE | MOTIVE_KEY | DIRTY_FAKE |
| `EVIDENCE_SECRET_LEDGER_AUDIT_NOTICE` | DIRTY_FAKE | MOTIVE_KEY | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_VIP_PATIENT_INCIDENT_FILE` | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE | MOTIVE_KEY |
| `EVIDENCE_SECURITY_SCAPEGOAT_DRAFT` | MOTIVE_KEY | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_CARE_MANAGER_WHISTLEBLOWER_DRAFT` | UNLOCK_KEY | UNLOCK_KEY | UNLOCK_KEY | UNLOCK_KEY |
| `EVIDENCE_WEARABLE_VITAL_RAW_LOG` | TIME_KEY | TIME_KEY | TIME_KEY | TIME_KEY |
| `EVIDENCE_NIGHT_PILL_PACKAGE_TEAR` | DIRTY_FAKE | DIRTY_FAKE | METHOD_KEY | DIRTY_FAKE |
| `EVIDENCE_WATER_SERVICE_CHECKLIST` | DIRTY_FAKE | METHOD_KEY | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_DECANTER_SEAL_FRAGMENT` | METHOD_KEY | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE |
| `EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO` | DIRTY_FAKE | DIRTY_FAKE | DIRTY_FAKE | METHOD_KEY |

---

## 8. Evidence Variant State 정책

레드팀 피드백 반영 사항:

```text
같은 증거를 단순히 다르게 해석하는 수준이면 유저가 불공정하다고 느낄 수 있다.
따라서 각 Variant마다 결정타 증거의 실제 상태 차이가 있어야 한다.
```

### 8-1. MVP 최소 구현 기준

모든 증거의 이미지/카드를 Variant별로 완전히 다르게 만들 필요는 없다.

#### 구현 원칙: 증거 데이터의 상태 관리

```text
Default Evidence 20개:
- 4개 Variant에서 title / oneLine / baseDetail / image는 100% 동일하게 출력한다.
- 화면에 보이는 기본 텍스트는 변하지 않는다.
- Variant별 역할 차이는 EvidenceRoleMatrix / VariantSolution / ProofDimension에서만 관리한다.

Variant Evidence 5개:
- 기본 title / image / baseDetail은 유지한다.
- activeVariant에 따라 visibleDetailModifier와 internalRole이 동적으로 덧씌워질 수 있다.
- 이 5개만 “이번 회차에서 실제 상태가 조금 다른 증거”로 취급한다.
```

아래 5개 결정타 증거는 `EvidenceVariantState`를 둔다.

```text
1. EVIDENCE_WEARABLE_VITAL_RAW_LOG
2. EVIDENCE_NIGHT_PILL_PACKAGE_TEAR
3. EVIDENCE_WATER_SERVICE_CHECKLIST
4. EVIDENCE_DECANTER_SEAL_FRAGMENT
5. EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO
```

### 8-2. 권장 DB 구조

```json
{
  "evidenceCode": "EVIDENCE_WATER_SERVICE_CHECKLIST",
  "variantStates": [
    {
      "variantCode": "VARIANT_SECRETARY",
      "role": "METHOD_KEY",
      "visibleDetailModifier": "21:07 이후 침실 물병 교체 확인란이 다른 필압으로 덧씌워져 있다.",
      "scoringProofDimensions": ["METHOD_PROOF", "ACCESS_PROOF"]
    },
    {
      "variantCode": "VARIANT_SPOUSE",
      "role": "DIRTY_FAKE",
      "visibleDetailModifier": "체크리스트에 수정 흔적은 있으나 바이탈 로그의 급강하 시점과 직접 맞지 않는다.",
      "scoringProofDimensions": []
    }
  ]
}
```

### 8-3. 결정타 증거별 Variant State 요약

| evidenceCode | 진짜 KEY Variant | KEY일 때 상태 | 다른 Variant 상태 |
|---|---|---|---|
| `EVIDENCE_DECANTER_SEAL_FRAGMENT` | SECURITY | 디캔터 실링 조각이 와인셀러 출입 시간과 맞물림 | 수상하지만 바이탈 주요 시점과 어긋남 |
| `EVIDENCE_WATER_SERVICE_CHECKLIST` | SECRETARY | 21:07 물병 서비스 기록 수정이 21:18 반응과 맞물림 | 수상하지만 최종 치명 경로와 어긋남 |
| `EVIDENCE_NIGHT_PILL_PACKAGE_TEAR` | SPOUSE | 찢어진 약포장과 21:25 지연 복약이 맞물림 | 약 관련 의심은 남지만 타임라인 불충분 |
| `EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO` | DOCTOR | 21:13 약품 보관함 개봉과 처방 메모 수정이 맞물림 | 의사 비리 은폐처럼 보일 뿐 치명 경로 아님 |
| `EVIDENCE_WEARABLE_VITAL_RAW_LOG` | ALL | 시간 필터. 다른 증거와 결합해야만 KEY | 단독 정답표 아님 |

---

## 9. 증거 해금 정책

### 9-1. 기본 Phase 해금

```json
{
  "unlockCondition": {
    "type": "PHASE",
    "phase": "PHASE_2_SYSTEM_LOGS"
  }
}
```

### 9-2. 웨어러블 바이탈 원시 데이터 해금

`EVIDENCE_WEARABLE_VITAL_RAW_LOG`는 자동 시간 해금이 아니라, 케어매니저 심문을 통해 얻는 전리품이다.

```json
{
  "evidenceCode": "EVIDENCE_WEARABLE_VITAL_RAW_LOG",
  "unlockCondition": {
    "type": "ACTIVE_INVESTIGATION",
    "requiredPhase": "PHASE_4_KILLING_BLOW",
    "requiredEvidenceCodes": [
      "EVIDENCE_CARE_STATION_ACCESS_LOG",
      "EVIDENCE_CARE_MANAGER_WHISTLEBLOWER_DRAFT"
    ],
    "requiredNpcPressure": {
      "characterCode": "WITNESS_CARE_MANAGER",
      "minimumStage": "STAGE_2_EVIDENCE_COMBINATION"
    },
    "fallback": {
      "hintUnlockAllowed": true,
      "hintScorePenalty": 5,
      "systemUnlockAllowed": true,
      "systemScorePenalty": 10
    }
  }
}
```

#### MVP Simple Unlock 정책

구현 일정상 위 조건을 모두 세분화하기 어렵다면, 아래 단순 정책을 우선 적용한다.

```text
1. PHASE_4 진입 시 바이탈 로그 카드 슬롯은 잠금 상태로 표시한다.
2. 플레이어가 케어매니저에게 아래 증거 2개를 제시하면 해금한다.
   - EVIDENCE_CARE_STATION_ACCESS_LOG
   - EVIDENCE_CARE_MANAGER_WHISTLEBLOWER_DRAFT
3. 해금 실패 시 힌트 버튼으로 강제 해금 가능하다.
4. MVP에서는 세부 점수 패널티 대신 hintUsed=true만 기록해도 된다.
5. 고도화 버전에서는 hintScorePenalty / systemScorePenalty를 적용한다.
```

### 9-3. 상호 목격 진술 해금

상호 목격 진술은 AI가 먼저 말하지 않는다. 관련 증거를 제시받은 뒤 일부만 열린다.

```json
{
  "type": "EVIDENCE_TRIGGERED_AI_DISCLOSURE",
  "targetNpc": "SUSPECT_SECRETARY",
  "requiredEvidenceCodes": [
    "EVIDENCE_MEDICAL_CABINET_ACCESS_LOG"
  ],
  "unlockedDisclosure": "2층에 있을 때 간이진료실 쪽에서 금속 서랍이 닫히는 소리를 들었다.",
  "disclosureType": "DIRECT_HEARD",
  "mustNotSay": [
    "예비병원장이 약품을 조작했다",
    "예비병원장이 범인이다"
  ]
}
```

---

## 10. AI NPC Knowledge Boundary

### 10-1. AI에게 절대 주면 안 되는 Truth Layer

AI NPC 프롬프트에는 아래 정보를 넣지 않는다.

```text
activeVariant
culpritCode
variantSolution
fullTruthTimeline
evidenceRoleByVariant 전체
정답 해설
채점 정답
다른 NPC의 hiddenAction 전체
```

### 10-2. AI에게 줄 수 있는 것

```text
characterProfile
publicAlibi
hiddenSelfAction
directKnowledge
inferredKnowledge
forbiddenKnowledge
evidenceReactionPolicy
stageResponsePolicy
```

#### AI 프롬프트 토큰 최적화 정책

AI 심문 프롬프트에는 25개 증거의 모든 반응 정책을 한 번에 넣지 않는다.

```text
기본 프롬프트:
- characterProfile
- publicAlibi
- hiddenSelfAction
- directKnowledge
- inferredKnowledge
- forbiddenKnowledge
- stageResponsePolicy

증거 제시 시 동적 주입:
- 플레이어가 특정 evidenceCode를 제시한 경우에만
  해당 evidenceCode에 연결된 evidenceReactionPolicy를 추가한다.
```

즉, RAG 또는 동적 프롬프트 조립 방식으로 구현한다.

```text
금지:
- 모든 evidenceReactionPolicy를 매번 통째로 넣기
- activeVariant / culpritCode / fullTruthTimeline을 넣기

권장:
- NPC 기본 성격 + 현재 질문 + 제시된 증거 관련 반응 정책만 주입
- 응답 후 금칙어/정답 누설 검증
```

### 10-3. DIRECT / INFERRED 규칙

```yaml
DIRECT:
  definition: "직접 본 것 / 직접 들은 것 / 직접 한 것"
  answerStyle: "단정 가능하되, 목격 범위를 넘지 않는다."

INFERRED:
  definition: "정황상 추정하는 것"
  answerStyle: "확실하진 않지만, 제가 보기엔, 소리만 들었습니다 같은 표현 필수"
```

### 10-4. NPC별 지식 조각

| characterCode | 직접 아는 것 | 추정하는 것 | 금지 |
|---|---|---|---|
| `SUSPECT_SPOUSE` | 21:17~21:23 2층 접근, 갤러리 USB 탐색, 침실 근처 접촉 | 진료실 문 닫히는 소리의 주체 | 비서실장/의사의 실제 조작 단정 |
| `SUSPECT_SECRETARY` | 물병/컵 확인, 체크리스트 작성/수정, 회의 자료 정리 | 진료실 쪽 금속 서랍 소리 | 예비병원장이 약품을 조작했다고 단정 |
| `SUSPECT_DOCTOR` | 약품 보관함 개봉, 처방 메모 확인, VIP 사고 파일 위험 | 복도에 다른 인기척 | 배우자/비서의 물건 조작 단정 |
| `SUSPECT_SECURITY` | 보안 권한 구조, 재동기화 작업, 와인셀러 권한자 범위 | 누가 관리자 권한을 썼을 가능성 | CCTV 사각지대 세부와 activeVariant 누설 |
| `WITNESS_CARE_MANAGER` | 케어 로그 접근, 바이탈 로그 사본 보관, 제보 초안 작성 | 바이탈 시점상 일부 경로가 더 그럴듯함 | 범인 단정, 정답 해설 |

---

## 11. AI 심문 Stage 정책

```text
STAGE_0_NO_EVIDENCE
- 공개 알리바이와 기본 부정만 가능

STAGE_1_SINGLE_EVIDENCE
- 단일 증거 제시
- 자기 행동 일부 인정, 축소, 변명

STAGE_2_EVIDENCE_COMBINATION
- 증거 조합 제시
- 숨긴 행동 일부 인정
- 다른 인물 관련 DIRECT/INFERRED 조각 일부 공개 가능

STAGE_3_CONTRADICTION_LOCKED
- 동기 + 물리 증거 + 시간 모순이 동시에 제시됨
- 감정 붕괴 / 책임 회피 / 물귀신 작전
- 그래도 살인 자백과 범인 단정은 금지
```

### 11-1. 공통 금지 응답

```text
"제가 범인입니다."
"그 사람이 범인입니다."
"실제로는 X가 조작했습니다."
"정답은 Y입니다."
"이번 Variant에서는..."
"바이탈 로그상 범인은..."
```

---

## 12. 최종 추리 채점 기준

### 12-1. 기본 점수

```yaml
total: 100
culpritSelection: 30
evidenceProofCoverage: 20
methodExplanation: 20
motiveExplanation: 15
coverUpOrAlibiExplanation: 10
reasoningConsistency: 5
```

### 12-2. 증거 선택은 ID 단일 정답이 아니라 Proof Dimension 기준으로 채점

최종 제출에서 플레이어는 증거 3개를 선택한다.  
채점은 단순히 `keyEvidenceIds` 3개 일치 여부만 보지 않는다.

```text
TIME_PROOF
METHOD_PROOF
ACCESS_PROOF
MOTIVE_PROOF
COVERUP_PROOF
SUPPORT_PROOF
```

선택한 증거 3개가 위 차원을 얼마나 커버하는지 평가한다.

#### 채점 인정 규칙

```text
primary evidence:
- 해당 proof dimension을 직접 증명하는 핵심 증거
- 선택 시 해당 dimension의 높은 점수 부여 가능

secondary evidence:
- primary evidence와 함께 선택될 때 dimension을 보강하는 증거
- 단독 선택만으로는 dimension 만점 처리하지 않는다

partial evidence:
- 방향은 맞지만 정보가 부족한 증거
- 부분 점수만 부여한다
```

`SUPPORT_KEY`, `SUPPORT`, `COMMON`은 Role Matrix에서의 게임 내 역할이다.  
이 증거들도 Proof Dimension의 `secondary` 또는 `partial`로 인정될 수 있으나, primary evidence 없이 단독으로 정답 차원을 닫지는 못한다.

### 12-3. Proof Dimension 정본

#### SECURITY

```yaml
TIME_PROOF:
  primary:
    - EVIDENCE_WEARABLE_VITAL_RAW_LOG
METHOD_PROOF:
  primary:
    - EVIDENCE_DECANTER_SEAL_FRAGMENT
  secondary:
    - EVIDENCE_DIRECTOR_DECANTER_AND_WINE_GLASS
ACCESS_PROOF:
  primary:
    - EVIDENCE_WINE_CELLAR_ACCESS_LOG
COVERUP_PROOF:
  primary:
    - EVIDENCE_SECURITY_SERVER_RESYNC_LOG
MOTIVE_PROOF:
  primary:
    - EVIDENCE_SECURITY_SCAPEGOAT_DRAFT
```

#### SECRETARY

```yaml
TIME_PROOF:
  primary:
    - EVIDENCE_WEARABLE_VITAL_RAW_LOG
  secondary:
    - EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG
METHOD_PROOF:
  primary:
    - EVIDENCE_WATER_SERVICE_CHECKLIST
  secondary:
    - EVIDENCE_BEDROOM_WATER_BOTTLE_AND_CUP
ACCESS_PROOF:
  primaryCombination:
    - EVIDENCE_CARE_CALL_PANEL_LOG
    - EVIDENCE_SECOND_FLOOR_CCTV_STILL
  partial:
    - EVIDENCE_CARE_CALL_PANEL_LOG
    - EVIDENCE_SECOND_FLOOR_CCTV_STILL
MOTIVE_PROOF:
  primary:
    - EVIDENCE_SECRET_LEDGER_AUDIT_NOTICE
```

비서실장 Variant의 ACCESS_PROOF는 단일 CCTV만으로 만점 처리하지 않는다.  
`CARE_CALL_PANEL_LOG`와 `SECOND_FLOOR_CCTV_STILL`이 함께 있을 때 물병 접근 가능성이 충분히 닫힌다.

#### SPOUSE

```yaml
TIME_PROOF:
  primary:
    - EVIDENCE_WEARABLE_VITAL_RAW_LOG
  secondary:
    - EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG
METHOD_PROOF:
  primary:
    - EVIDENCE_NIGHT_PILL_PACKAGE_TEAR
  secondary:
    - EVIDENCE_PERSONAL_NIGHT_MEDICATION_CASE
ACCESS_PROOF:
  primary:
    - EVIDENCE_SECOND_FLOOR_CCTV_STILL
  secondary:
    - EVIDENCE_CROSS_WITNESS_STATEMENT_CARD
MOTIVE_PROOF:
  primary:
    - EVIDENCE_DIVORCE_PROPERTY_DRAFT
```

배우자 Variant의 `CROSS_WITNESS_STATEMENT_CARD`는 ACCESS_PROOF 보조 인정 증거다.  
단독으로는 만점 처리하지 않고, CCTV 스틸컷 또는 스누즈 로그와 결합될 때 강해진다.

#### DOCTOR

```yaml
TIME_PROOF:
  primary:
    - EVIDENCE_WEARABLE_VITAL_RAW_LOG
METHOD_PROOF:
  primary:
    - EVIDENCE_MODIFIED_NIGHT_PRESCRIPTION_MEMO
  secondary:
    - EVIDENCE_MEDICAL_CABINET_ACCESS_LOG
MOTIVE_PROOF:
  primary:
    - EVIDENCE_VIP_PATIENT_INCIDENT_FILE
SUPPORT_PROOF:
  secondary:
    - EVIDENCE_NIGHT_MEDICATION_SNOOZE_LOG
    - EVIDENCE_CARE_CALL_PANEL_LOG
```

의사 Variant에서는 `MODIFIED_NIGHT_PRESCRIPTION_MEMO`가 primary evidence다.  
`NIGHT_MEDICATION_SNOOZE_LOG`와 `CARE_CALL_PANEL_LOG`는 처방/케어 지시가 실제 야간 루틴과 연결되었다는 보조 증거다.

### 12-4. 진행도 기반 Score Cap

```yaml
scoreCapByMaxUnlockedPhase:
  PHASE_0_OPENING: 45
  PHASE_1_BASIC_OBJECTS: 60
  PHASE_2_SYSTEM_LOGS: 75
  PHASE_3_MOTIVE_AND_CONTRADICTION: 85
  PHASE_4_KILLING_BLOW: 100

noVitalLogSubmission:
  allowed: true
  maxScoreCap: 85
```

### 12-5. 케어매니저 오답 피드백

케어매니저 `WITNESS_CARE_MANAGER`는 최종 선택 UI에서 선택 가능하지만, 어떤 Variant에서도 범인이 아니다.  
플레이어가 케어매니저를 범인으로 선택한 경우 아래 피드백을 사용한다.

```text
문하연은 바이탈 로그를 숨기고 제보를 망설인 수상한 인물이 맞습니다.
하지만 그녀가 치명 경로가 된 물병, 약통, 디캔터, 처방 메모 중 어느 하나를 직접 조작했다는 증거는 없습니다.
그녀가 숨긴 것은 범행이 아니라, 재단 비리를 제보하기 위해 보존해 둔 객관 기록과 그 제출 시점이었습니다.
진범은 문하연이 숨긴 로그가 가리키는 시간표 안에서, 실제 치명 물건에 접근한 인물입니다.
```

---

## 13. Android UI 기준

### 13-1. 증거 목록

MVP 추천:

```text
기본: 전체 증거 목록
보조: 카테고리 필터 칩
보조: 장소 필터 칩
정렬: 최근 해금 순 우선
```

### 13-2. 카테고리 필터

```text
전체
현장
물리
문서
로그
진술
맵
```

### 13-3. 장소 필터

```text
1F
2F
B1
침실
진료실
보안실
케어스테이션
와인셀러
```

### 13-4. Phase 표시

내부 phase명을 UI에 직접 노출하지 않는다.

```text
PHASE_0_OPENING → 초기 현장 자료
PHASE_1_BASIC_OBJECTS → 현장 물건 증거
PHASE_2_SYSTEM_LOGS → 시스템 기록
PHASE_3_MOTIVE_AND_CONTRADICTION → 관계/동기 문서
PHASE_4_KILLING_BLOW → 결정적 기록
```

---

## 14. DB / ERD 반영 기준

### 14-1. Solution 1:1 폐기

서월채는 `Scenario : Solution = 1 : 1`이 아니다.

```text
Scenario 1 : N ScenarioVariant
ScenarioVariant 1 : 1 VariantSolution
PlaySession N : 1 ScenarioVariant
```

### 14-2. 권장 도메인 구조

```text
Scenario
ScenarioVariant
VariantSolution
Character
ScenarioCharacter
Location
Evidence
EvidenceVariantState
PlaySession
UnlockedEvidence
InterrogationRecord
FinalDeduction
```

### 14-3. PlaySession 시작 시점

```text
1. 사용자가 서월채 시나리오 시작
2. 서버가 Variant 4개 중 하나를 선택
3. PlaySession.activeVariantCode 저장
4. EvidenceVariantState는 activeVariant 기준으로 조회
5. AI 심문 프롬프트에는 activeVariant를 직접 넣지 않음
6. FinalDeduction 채점 시에만 activeVariant의 VariantSolution 사용
```

---


### 14-4. MVP 서버 아키텍처 Fallback 정책

공식 목표는 `RANDOM_REQUIRED`, 즉 4개 Variant 전체 구현이다.  
다만 일정상 `Scenario 1 : N ScenarioVariant` 구조를 MVP에 완전 반영하기 어렵다면 아래 fallback을 허용한다.

```text
Official Target:
- DB ERD는 Scenario 1 : N Variant 확장을 전제로 설계한다.
- PlaySession.activeVariantCode를 저장할 수 있어야 한다.
- VariantSolution / EvidenceVariantState / ProofDimension 구조를 유지한다.

Fallback MVP Plan:
- 시간상 4개 Variant 전체 seed와 랜덤 할당이 어렵다면,
  서버단에서 activeVariant를 VARIANT_SECRETARY 1개로 하드코딩하여 공식 시연 안정성을 우선한다.
- 단, DB 구조와 canonical evidenceCode, scoring dimension은 4 Variant 확장이 가능하도록 유지한다.
- 이후 업데이트에서 RANDOM Variant 할당 로직을 활성화한다.
```

이 fallback은 랜덤 범인 기획을 포기한다는 뜻이 아니다.  
백엔드 구조 변경 비용이 예상보다 클 때, 시연 안정성을 확보하기 위한 위험관리 정책이다.

## 15. 구현 전 P0 체크리스트

```text
[ ] Evidence canonicalCode가 DB/JSON/AI/Android에서 하나로 통일되었는가?
[ ] oldAliases에 자기 자신 canonicalCode가 들어가 있지 않은가?
[ ] ScenarioVariant / VariantSolution 구조가 ERD에 반영되었는가?
[ ] 일정 리스크가 있는 경우 activeVariant fallback 정책이 결정되었는가?
[ ] EvidenceVariantState 적용 범위가 Default Evidence 20개 / Variant Evidence 5개로 구분되었는가?
[ ] Role Matrix와 Proof Dimension의 관계가 구현자에게 명확한가?
[ ] SUPPORT_KEY / SUPPORT / COMMON의 채점 인정 범위가 정의되었는가?
[ ] SECRETARY ACCESS_PROOF가 CARE_CALL_PANEL_LOG + CCTV_STILL 조합형으로 구현되는가?
[ ] SPOUSE의 CROSS_WITNESS_STATEMENT_CARD가 ACCESS_PROOF 보조 증거로 처리되는가?
[ ] DOCTOR의 NIGHT_MEDICATION_SNOOZE_LOG / CARE_CALL_PANEL_LOG가 SUPPORT_PROOF 보조 증거로 처리되는가?
[ ] AI 프롬프트에서 activeVariant와 culpritCode가 빠져 있는가?
[ ] NPC 지식은 DIRECT/INFERRED/FORBIDDEN으로 나뉘는가?
[ ] evidenceReactionPolicy가 RAG/동적 주입 방식으로 들어가는가?
[ ] 최종 추리 채점이 proof dimension 기반으로 작동하는가?
[ ] Android 증거 UI에 카테고리/장소/최근 해금 필터가 있는가?
[ ] Phase 내부명 대신 자연어 UI명이 사용되는가?
[ ] 바이탈 로그 없이도 제출 가능하되 score cap 또는 hintUsed 정책이 적용되는가?
[ ] 케어매니저를 범인으로 선택하면 전용 오답 피드백이 출력되는가?
[ ] Doctor Variant의 수정된 처방 메모 카드가 player-facing으로 충분히 직관적인가?
```

---

## 16. 이후 문서 분리 계획

`SEOWOLCHAE_IMPLEMENTATION_CANON_v1.1.md`는 구현 정본이다.  
이후 개발팀 작업이 시작되면 아래 산출물로 분리한다.

### 16-1. Seed Data Spec

```text
fileName: SEOWOLCHAE_SEED_SPEC_v1.md

필요 seed:
- scenario seed
- characters seed
- locations seed
- evidences seed
- scenario_variants seed
- variant_solutions seed
- evidence_variant_states seed
- unlock policies seed
```

### 16-2. NPC Prompt Spec

```text
fileName: SEOWOLCHAE_NPC_PROMPT_SPEC_v1.md

구성:
- 공통 시스템 프롬프트
- NPC별 characterProfile
- NPC별 publicAlibi
- NPC별 hiddenSelfAction
- NPC별 directKnowledge
- NPC별 inferredKnowledge
- NPC별 forbiddenKnowledge
- stageResponsePolicy
- evidenceReactionPolicy
- output JSON format
- 정답 누설 검증 규칙
- RAG/동적 evidenceReactionPolicy 주입 규칙
```

### 16-3. Scoring Criteria JSON

```text
fileName: SEOWOLCHAE_SCORING_CRITERIA_v1.json

구성:
- scenarioCode
- variantCode
- culpritCode
- proofDimensions
- primary/secondary/partial evidence mapping
- scoreCapByMaxUnlockedPhase
- noVitalLogSubmission policy
- careManagerWrongAnswerFeedback
```

### 16-4. Android Evidence UI Spec

```text
fileName: SEOWOLCHAE_ANDROID_EVIDENCE_UI_SPEC_v1.md

구성:
- evidence category enum
- unlock phase display name
- location filter
- recent unlock sort
- locked evidence placeholder
- vital log locked/unlocked UI
```

---

## 17. v1.1 최종 판정

```text
상태:
GREENLIGHT WITH FIXES → FIXES APPLIED

구현팀 전달 가능 여부:
가능

단, 다음 개발 문서는 반드시 seed / prompt / scoring / Android UI로 분리한다.
이 문서는 더 이상 세계관이나 증거를 추가하는 창작 문서가 아니라,
구현 정본의 기준점으로 유지한다.
```

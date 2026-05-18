# CaseLab AI 인증/거래 확장 계획

문서명: `AUTH_TRANSACTION_EXPANSION_PLAN.md`  
목적: CaseLab AI의 **초기 MVP 개발 속도**를 유지하면서, 이후 과제 요구사항 및 실제 서비스화를 위해 **인증/인가, 크레딧, 시나리오 구매/언락 거래 시스템**을 자연스럽게 확장할 수 있도록 기준을 정리한다.

---

## 1. 현재 개발 전략

CaseLab AI는 초기 MVP에서 다음 흐름을 우선 완성한다.

```text
시나리오 목록
→ 시나리오 상세
→ 게임 시작
→ 증거 확인
→ 용의자 심문
→ 최종 추리 제출
→ 결과 해설
```

따라서 초기에는 로그인, 결제, 크레딧, 구매/환불을 바로 구현하지 않는다.

다만 나중에 인증/거래 기능을 붙일 수 있도록 아래 구조는 처음부터 열어둔다.

```text
user_id
creator_id
visibility
scenario_type
price_credit
AccessService
MockUserProvider
```

핵심 원칙:

> **기능은 먼저 만들고, 인증/거래는 점진적으로 확장한다.  
> 단, 후반에 구조를 갈아엎지 않도록 확장 지점은 초반부터 유지한다.**

---

## 2. 현재 단계와 향후 단계

### 2.1 Phase 1: MVP Mock 단계

목표:

```text
핵심 게임 플레이 완성
AI 용의자 심문 완성
공식 시나리오 1개 플레이 가능
커스텀 시나리오 기본 제작 가능
```

인증 처리:

```text
로그인 없음
MockUserProvider 사용
기본 userId = 1L
모든 시나리오 접근 허용
```

거래 처리:

```text
모든 시나리오는 무료로 간주
price_credit = 0
구매/환불/정산 없음
AccessService는 항상 true 반환
```

---

### 2.2 Phase 2: 인증 도입 단계

목표:

```text
회원가입 / 로그인
JWT 기반 사용자 식별
내 시나리오 / 내 플레이 기록 / 내 북마크 구분
```

인증 처리:

```text
MockUserProvider 제거 또는 SecurityUserProvider로 교체
SecurityContext에서 userId 추출
creator_id 기반 수정 권한 적용
```

인가 처리:

```text
내가 만든 시나리오만 수정 가능
PRIVATE 시나리오는 작성자만 접근 가능
UNLISTED 시나리오는 링크가 있는 사용자만 접근 가능
PUBLIC 시나리오는 모두 접근 가능
```

---

### 2.3 Phase 3: 크레딧/구매 도입 단계

목표:

```text
무료/유료 시나리오 구분
크레딧 기반 시나리오 구매
구매한 시나리오 접근 권한 생성
구매 내역 조회
```

거래 처리:

```text
wallet 생성
mock credit 충전
credit transaction 기록
scenario purchase 기록
scenario access 생성
```

---

### 2.4 Phase 4: 거래 고도화 단계

목표:

```text
환불 정책
중복 구매 방지
제작자 수익 정산 Mock
유료 시나리오 랭킹
거래 로그 관리
```

고도화 기능:

```text
refund
creator settlement mock
purchase status
transaction audit
concurrency control
```

---

## 3. 인증/인가 확장을 위한 현재 유지 필드

아래 필드는 MVP 초반부터 유지한다.

### users

```text
id
email
nickname
password
role
created_at
updated_at
```

초기 MVP에서는 실제 로그인 없이 1번 유저를 사용한다.

---

### scenarios

```text
id
creator_id
title
description
scenario_type
visibility
status
difficulty
price_credit
created_at
updated_at
```

`price_credit`는 현재 ERD에 없다면 추가를 권장한다.

```text
price_credit = 0
→ 무료 시나리오

price_credit > 0
→ 유료 시나리오
```

초기에는 모든 시나리오를 `price_credit = 0`으로 둔다.

---

### play_sessions

```text
id
user_id
scenario_id
status
started_at
ended_at
score
grade
```

초기에는 `user_id = 1L`로 저장한다.

---

### scenario_reviews / scenario_bookmarks

```text
user_id
scenario_id
```

초기에는 실제 로그인 없이 1번 유저를 사용한다.  
JWT 도입 후에는 로그인 사용자 기준으로 전환한다.

---

## 4. MockUserProvider 설계

### 4.1 목적

초기 MVP에서 인증 구현 없이도 다음 도메인을 개발할 수 있게 한다.

```text
내 플레이 세션
내 커스텀 시나리오
내 북마크
내 리뷰
```

### 4.2 구현 예시

```java
@Component
public class MockUserProvider {

    public Long currentUserId() {
        return 1L;
    }
}
```

나중에 JWT 도입 시:

```java
@Component
public class SecurityUserProvider {

    public Long currentUserId() {
        // SecurityContext에서 userId 추출
    }
}
```

초기에는 서비스 계층에서 직접 `1L`을 박지 말고 반드시 Provider를 통해 가져온다.

```java
Long userId = mockUserProvider.currentUserId();
```

금지:

```java
Long userId = 1L; // Service 곳곳에 직접 하드코딩 금지
```

---

## 5. ScenarioAccessService 설계

### 5.1 목적

시나리오 접근 권한 정책을 한 곳에서 관리한다.

초기에는 모든 접근을 허용하지만, 나중에는 아래 조건을 반영한다.

```text
무료 시나리오
공식 시나리오
내가 만든 시나리오
구매한 시나리오
공개 상태
차단/신고 상태
```

### 5.2 초기 MVP 구현

```java
@Service
public class ScenarioAccessService {

    public boolean canPlay(Long userId, Long scenarioId) {
        return true;
    }

    public boolean canEdit(Long userId, Long scenarioId) {
        return true;
    }

    public boolean canView(Long userId, Long scenarioId) {
        return true;
    }
}
```

### 5.3 인증 도입 후 정책

```java
public boolean canEdit(Long userId, Long scenarioId) {
    Scenario scenario = scenarioRepository.findByIdOrThrow(scenarioId);
    return scenario.getCreatorId().equals(userId);
}
```

### 5.4 거래 도입 후 정책

```java
public boolean canPlay(Long userId, Long scenarioId) {
    Scenario scenario = scenarioRepository.findByIdOrThrow(scenarioId);

    if (scenario.isOfficial()) return true;
    if (scenario.isFree()) return true;
    if (scenario.isCreator(userId)) return true;
    if (scenarioAccessRepository.existsByUserIdAndScenarioId(userId, scenarioId)) return true;

    return false;
}
```

---

## 6. 시나리오 공개 범위 정책

### 6.1 Visibility 종류

```text
PRIVATE
UNLISTED
PUBLIC
OFFICIAL
```

### 6.2 정책

| Visibility | 설명 | 접근 가능 사용자 |
|---|---|---|
| PRIVATE | 작성자만 볼 수 있음 | 작성자 |
| UNLISTED | 링크가 있는 사용자만 접근 가능 | 링크 보유자 |
| PUBLIC | 시나리오 라이브러리에 노출 | 모든 사용자 |
| OFFICIAL | 운영진/제작진 검수 완료 공식 시나리오 | 모든 사용자 |

초기 MVP에서는 실제 인가를 강하게 적용하지 않더라도, 필드는 유지한다.

---

## 7. 거래 도메인 확장 구조

거래를 붙일 때 추가할 핵심 도메인은 다음과 같다.

```text
CreditWallet
CreditTransaction
ScenarioPurchase
ScenarioAccess
CreatorSettlementMock
```

---

## 8. CreditWallet

### 8.1 역할

사용자의 보유 크레딧을 관리한다.

### 8.2 필드 예시

```text
id
user_id
balance
created_at
updated_at
```

### 8.3 정책

```text
1명의 유저는 1개의 지갑을 가진다.
구매 시 balance에서 차감한다.
잔액 부족 시 구매 실패 처리한다.
```

---

## 9. CreditTransaction

### 9.1 역할

크레딧의 증가/감소 이력을 저장한다.

### 9.2 필드 예시

```text
id
user_id
transaction_type
amount
balance_after
description
reference_type
reference_id
created_at
```

### 9.3 transaction_type

```text
CHARGE
PURCHASE
REFUND
BONUS
SETTLEMENT
```

### 9.4 예시

```text
CHARGE:
Mock 충전으로 +1000 credit

PURCHASE:
유료 시나리오 구매로 -300 credit

REFUND:
환불로 +300 credit
```

---

## 10. ScenarioPurchase

### 10.1 역할

사용자가 유료 시나리오를 구매한 기록을 저장한다.

### 10.2 필드 예시

```text
id
user_id
scenario_id
price_credit
purchase_status
purchased_at
refunded_at
```

### 10.3 purchase_status

```text
COMPLETED
REFUNDED
CANCELED
FAILED
```

---

## 11. ScenarioAccess

### 11.1 역할

사용자가 특정 시나리오를 플레이할 수 있는 권한을 저장한다.

### 11.2 필드 예시

```text
id
user_id
scenario_id
access_type
granted_at
expired_at
```

### 11.3 access_type

```text
OWNER
PURCHASED
FREE
OFFICIAL
ADMIN_GRANTED
```

구매 완료 시 `ScenarioAccess`를 생성한다.

---

## 12. CreatorSettlementMock

### 12.1 역할

유저가 만든 유료 시나리오가 판매되었을 때 제작자 정산 정보를 Mock으로 관리한다.

실제 결제 정산이 아니라, 프로젝트 과제용 거래 흐름을 보여주는 용도다.

### 12.2 필드 예시

```text
id
creator_id
scenario_id
purchase_id
gross_credit
platform_fee_credit
settlement_credit
settlement_status
created_at
```

### 12.3 settlement_status

```text
PENDING
CONFIRMED
CANCELED
```

---

## 13. 거래 API 확장 계획

초기 MVP 이후 아래 API를 추가한다.

### 13.1 지갑 조회

```http
GET /api/wallet
```

응답 예시:

```json
{
  "userId": 1,
  "balance": 1200
}
```

---

### 13.2 Mock 크레딧 충전

```http
POST /api/wallet/charge-mock
```

요청 예시:

```json
{
  "amount": 1000
}
```

응답 예시:

```json
{
  "transactionId": 10,
  "balance": 2000
}
```

---

### 13.3 시나리오 구매

```http
POST /api/scenarios/{scenarioId}/purchase
```

정책:

```text
이미 구매한 경우 실패 또는 기존 Access 반환
잔액 부족 시 실패
무료 시나리오는 구매 없이 접근 가능
작성자는 자신의 시나리오를 구매하지 않음
```

응답 예시:

```json
{
  "purchaseId": 5,
  "scenarioId": 12,
  "priceCredit": 300,
  "balanceAfter": 700,
  "accessGranted": true
}
```

---

### 13.4 구매 내역 조회

```http
GET /api/users/me/purchases
```

응답 예시:

```json
[
  {
    "purchaseId": 5,
    "scenarioId": 12,
    "scenarioTitle": "데모데이 전야 살인사건",
    "priceCredit": 300,
    "purchaseStatus": "COMPLETED",
    "purchasedAt": "2026-05-18T12:00:00"
  }
]
```

---

### 13.5 접근 권한 확인

```http
GET /api/scenarios/{scenarioId}/access
```

응답 예시:

```json
{
  "scenarioId": 12,
  "canPlay": true,
  "accessType": "PURCHASED"
}
```

---

### 13.6 환불

```http
POST /api/scenarios/{scenarioId}/refund
```

초기 정책:

```text
아직 플레이를 시작하지 않은 구매 건만 환불 가능
플레이 세션이 생성되었으면 환불 불가
환불 시 ScenarioAccess 비활성화
CreditTransaction REFUND 생성
ScenarioPurchase 상태 REFUNDED 변경
```

---

## 14. 거래 동시성 고려 사항

거래 기능 도입 시 다음 동시성 문제가 발생할 수 있다.

```text
같은 사용자가 같은 시나리오를 동시에 두 번 구매
동시에 크레딧 차감 요청 발생
환불과 플레이 시작이 동시에 발생
구매 완료 전 접근 권한 요청
```

### 14.1 기본 방어 전략

```text
scenario_purchases에 user_id + scenario_id Unique 제약
wallet 차감은 Transaction 안에서 처리
잔액 검증 후 차감
필요 시 Redis Lock 적용
```

### 14.2 추천 Unique 제약

```sql
UNIQUE (user_id, scenario_id)
```

### 14.3 Redis Lock 적용 후보

```text
purchase:scenario:{scenarioId}:user:{userId}
wallet:user:{userId}
```

---

## 15. 인증 도입 API 계획

### 15.1 회원가입

```http
POST /api/auth/signup
```

요청:

```json
{
  "email": "user@example.com",
  "password": "password1234",
  "nickname": "탐정도윤"
}
```

---

### 15.2 로그인

```http
POST /api/auth/login
```

응답:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token"
}
```

---

### 15.3 내 정보 조회

```http
GET /api/users/me
```

---

## 16. 인증 도입 후 인가 정책

### 16.1 시나리오 수정

```text
creator_id == currentUserId
```

작성자만 수정 가능하다.

---

### 16.2 시나리오 삭제

```text
creator_id == currentUserId
또는 ADMIN
```

---

### 16.3 비공개 시나리오 조회

```text
PRIVATE:
작성자만 조회 가능

UNLISTED:
링크 또는 초대 토큰 보유자만 조회 가능

PUBLIC/OFFICIAL:
모두 조회 가능
```

---

### 16.4 플레이 시작

```text
무료 시나리오:
모두 가능

유료 시나리오:
구매/언락 사용자만 가능

내가 만든 시나리오:
항상 가능
```

---

## 17. 현재 MVP에서 지켜야 할 구현 규칙

### 17.1 Service에 userId 하드코딩 금지

금지:

```java
Long userId = 1L;
```

권장:

```java
Long userId = userProvider.currentUserId();
```

초기에는 MockUserProvider가 1L을 반환하고, 나중에 SecurityUserProvider로 교체한다.

---

### 17.2 접근 권한 판단을 Controller에서 하지 않기

금지:

```java
if (scenario.getCreatorId().equals(userId)) {
    ...
}
```

권장:

```java
scenarioAccessService.validateCanEdit(userId, scenarioId);
```

---

### 17.3 구매 여부 판단 로직을 여러 곳에 흩뿌리지 않기

나중에 거래 기능을 붙이면 구매/접근 판단이 복잡해진다.

모든 접근 판단은 `ScenarioAccessService`로 모은다.

---

### 17.4 시나리오 가격 필드는 미리 유지

현재 모든 시나리오가 무료여도 아래 필드는 유지한다.

```text
price_credit
```

초기값:

```text
0
```

---

### 17.5 거래 확장 전까지는 모든 canPlay true

초기 MVP에서는 아래처럼 처리 가능하다.

```java
public void validateCanPlay(Long userId, Long scenarioId) {
    return;
}
```

단, 메서드 자체는 반드시 존재해야 한다.

---

## 18. 추천 개발 순서

### 18.1 현재 우선순위

```text
1. 공식 시나리오 플레이
2. 증거/용의자 조회
3. AI 심문
4. 최종 추리 제출
5. 결과 해설
6. 커스텀 시나리오 제작
```

---

### 18.2 인증 도입 시점

아래 기능이 어느 정도 완성된 후 도입한다.

```text
시나리오 목록/상세
게임 세션 시작
AI 심문
최종 추리 제출
```

도입 이유:

```text
내 기록
내 시나리오
북마크
리뷰
커스텀 제작자 구분
```

---

### 18.3 거래 도입 시점

아래 기능이 완성된 후 도입한다.

```text
인증
커스텀 시나리오 공개
시나리오 라이브러리
리뷰/평점
```

도입 이유:

```text
유료 시나리오 판매
크레딧 구매
접근 권한 관리
제작자 정산 Mock
```

---

## 19. Android 확장 영향

인증/거래 도입 전 Android는 다음처럼 동작한다.

```text
앱 실행
→ 시나리오 목록 조회
→ 시나리오 상세
→ 게임 시작
```

인증 도입 후 추가 화면:

```text
로그인
회원가입
마이페이지
내가 만든 시나리오
내 플레이 기록
```

거래 도입 후 추가 화면:

```text
크레딧 지갑
Mock 충전
시나리오 구매
구매 완료
구매 내역
환불 요청
```

---

## 20. API 응답 확장 필드

초기 시나리오 API 응답에도 아래 필드는 포함하는 것을 권장한다.

```json
{
  "scenarioId": 1,
  "title": "데모데이 전야 살인사건",
  "scenarioType": "OFFICIAL",
  "visibility": "PUBLIC",
  "priceCredit": 0,
  "isPaid": false,
  "canPlay": true,
  "isPurchased": false,
  "isOwner": false
}
```

초기에는 값이 고정될 수 있다.

```text
priceCredit = 0
isPaid = false
canPlay = true
isPurchased = false
isOwner = false
```

나중에 거래/인증 도입 시 그대로 확장한다.

---

## 21. 현재 ERD에 추가 권장 필드

현재 ERD 기준으로 아래 필드가 없다면 추가를 권장한다.

### scenarios

```text
price_credit INT NOT NULL DEFAULT 0
```

### play_sessions

이미 있다면 유지:

```text
user_id
```

### scenarios

이미 있다면 유지:

```text
creator_id
visibility
scenario_type
```

---

## 22. 추후 추가 테이블 SQL 초안

아래 SQL은 거래 확장 시 참고용이다.  
초기 MVP에서 바로 생성하지 않아도 된다.

```sql
CREATE TABLE credit_wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credit_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    description VARCHAR(255),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scenario_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    price_credit INT NOT NULL,
    purchase_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    purchased_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at DATETIME,
    UNIQUE (user_id, scenario_id)
);

CREATE TABLE scenario_accesses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    access_type VARCHAR(30) NOT NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME,
    UNIQUE (user_id, scenario_id)
);

CREATE TABLE creator_settlement_mocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    gross_credit INT NOT NULL,
    platform_fee_credit INT NOT NULL,
    settlement_credit INT NOT NULL,
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 23. 개발 AI에게 주는 지시

IntelliJ/Android Studio AI에게 이 문서를 먹일 때 다음 지시를 함께 준다.

```text
현재 CaseLab AI는 인증/거래를 바로 구현하지 않는다.
하지만 user_id, creator_id, visibility, price_credit, AccessService 구조는 유지해야 한다.

Service 내부에 userId를 직접 하드코딩하지 말고 UserProvider를 통해 가져와라.
현재는 MockUserProvider가 1L을 반환한다.

시나리오 접근 권한 판단은 ScenarioAccessService에 모아라.
초기 MVP에서는 canPlay/canView/canEdit이 true를 반환해도 된다.

나중에 JWT 인증과 크레딧 기반 시나리오 구매 기능으로 확장할 수 있도록 구조를 깨지 마라.
```

---

## 24. 최종 요약

CaseLab AI는 초기 MVP에서 인증/거래를 바로 구현하지 않는다.

현재 우선순위는 다음과 같다.

```text
게임 플레이
AI 용의자 심문
최종 추리 제출
커스텀 시나리오 제작
```

그러나 나중에 다음 기능을 붙일 수 있도록 구조를 유지한다.

```text
JWT 인증
사용자별 플레이 기록
내 시나리오 관리
크레딧 지갑
시나리오 구매
시나리오 접근 권한
제작자 정산 Mock
```

핵심 원칙:

> **지금은 MockUser + 무료 접근으로 빠르게 개발한다.  
> 하지만 모든 도메인은 사용자, 접근 권한, 가격, 구매 가능성을 전제로 설계한다.**

-- 시나리오 목록 조회 (홈 화면) 성능 개선을 위한 복합 인덱스 추가
-- 조건 필터링 (status, visibility) 및 정렬 (createdAt DESC) 최적화를 통해 Filesort를 제거합니다.
-- (관련 PR: 성능 병목 개선 - 부하 테스트 k6)

CREATE INDEX idx_scenarios_status_visibility_created 
ON scenarios (status, visibility, created_at DESC);

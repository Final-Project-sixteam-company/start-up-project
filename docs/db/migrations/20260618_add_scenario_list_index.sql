-- 시나리오 목록 조회 (홈 화면) 성능 개선을 위한 복합 인덱스 추가
-- 조건 필터링 (status, visibility) 및 정렬 (createdAt DESC) 최적화를 통해 Filesort를 제거합니다.
-- (관련 PR: 성능 병목 개선 - 부하 테스트 k6)

-- 안전장치(Idempotency): 이미 인덱스가 존재하는 경우 중복 실행 에러를 방지하기 위해 
-- INFORMATION_SCHEMA를 확인 후 동적으로 생성합니다.

DELIMITER $$
CREATE PROCEDURE CreateScenarioListIndexIfNotExists()
BEGIN
    DECLARE indexCount INT;
    
    SELECT COUNT(1) INTO indexCount
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = 'scenarios'
      AND index_name = 'idx_scenarios_status_visibility_created';
      
    IF indexCount = 0 THEN
        SET @sql = 'CREATE INDEX idx_scenarios_status_visibility_created ON scenarios (status, visibility, created_at DESC, id DESC)';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL CreateScenarioListIndexIfNotExists();
DROP PROCEDURE CreateScenarioListIndexIfNotExists;

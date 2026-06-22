-- 시나리오 목록 조회 (홈 화면) 성능 개선을 위한 복합 인덱스 추가
-- 조건 필터링 (status, visibility) 및 정렬 (createdAt DESC) 최적화를 통해 Filesort를 제거합니다.
-- (관련 PR: 성능 병목 개선 - 부하 테스트 k6)

-- [주의] 이 마이그레이션 스크립트는 내부적으로 `DELIMITER` 구문과 Stored Procedure를 사용합니다.
-- Flyway, Liquibase 등 JDBC 기반의 자동 마이그레이션 툴에서는 문법 에러가 발생할 수 있으므로,
-- 가급적 MySQL CLI 환경에서 수동으로 실행(예: `mysql < 20260618_add_scenario_list_index.sql`)해 주세요.

-- 안전장치(Idempotency): 이미 인덱스가 존재하는 경우 중복 실행 에러를 방지하기 위해 
-- INFORMATION_SCHEMA를 확인 후 동적으로 생성합니다.

DELIMITER $$
CREATE PROCEDURE CreateScenarioListIndexIfNotExists()
BEGIN
    DECLARE indexCols INT;
    
    -- 인덱스가 존재하는 경우 최대 컬럼 수를 가져옵니다 (없으면 NULL)
    SELECT MAX(SEQ_IN_INDEX) INTO indexCols
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = 'scenarios'
      AND index_name = 'idx_scenarios_status_visibility_created';
      
    -- 인덱스가 아예 없거나, 예전 버전(컬럼 3개)인 경우
    IF indexCols IS NULL OR indexCols < 4 THEN
        -- 예전 버전 인덱스가 있다면 삭제
        IF indexCols IS NOT NULL THEN
            SET @sql_drop = 'DROP INDEX idx_scenarios_status_visibility_created ON scenarios';
            PREPARE stmt_drop FROM @sql_drop;
            EXECUTE stmt_drop;
            DEALLOCATE PREPARE stmt_drop;
        END IF;
        
        -- 최신 버전(컬럼 4개) 인덱스 생성
        SET @sql_create = 'CREATE INDEX idx_scenarios_status_visibility_created ON scenarios (status, visibility, created_at DESC, id DESC)';
        PREPARE stmt_create FROM @sql_create;
        EXECUTE stmt_create;
        DEALLOCATE PREPARE stmt_create;
    END IF;
END $$
DELIMITER ;

CALL CreateScenarioListIndexIfNotExists();
DROP PROCEDURE CreateScenarioListIndexIfNotExists;

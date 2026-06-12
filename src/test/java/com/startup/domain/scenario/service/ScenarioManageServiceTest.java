package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.community.entity.ScenarioBookmark;
import com.startup.domain.community.repository.ScenarioBookmarkRepository;
import com.startup.domain.scenario.dto.ScenarioDeleteResponse;
import com.startup.domain.scenario.dto.ScenarioHideResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ScenarioManageServiceTest {

    @Autowired private ScenarioManageService scenarioManageService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private ScenarioBookmarkRepository bookmarkRepository;

    private static final Long OWNER_ID = 100L;
    private static final Long OTHER_ID = 200L;

    private Scenario draftScenario;
    private Scenario publishedScenario;
    private Scenario hiddenScenario;
    private Scenario deletedScenario;

    @BeforeEach
    void setUp() {
        // 상태별로 시나리오 1개씩 미리 셋팅
        draftScenario = scenarioRepository.save(createScenario(ScenarioStatus.DRAFT));
        publishedScenario = scenarioRepository.save(createScenario(ScenarioStatus.PUBLISHED));
        hiddenScenario = scenarioRepository.save(createScenario(ScenarioStatus.HIDDEN));
        deletedScenario = scenarioRepository.save(createScenario(ScenarioStatus.DELETED));
    }

    @AfterEach
    void tearDown() {
        bookmarkRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    private Scenario createScenario(ScenarioStatus status) {
        return Scenario.builder()
                .title("테스트 시나리오 " + status)
                .description("설명")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .creatorId(OWNER_ID)
                .status(status)
                .build();
    }

    // ──────────────────────────────────────────────
    // 1. DELETE API 테스트
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("DELETE: 정상 삭제 (Soft Delete 확인)")
    void deleteScenario_success() {
        ScenarioDeleteResponse response = scenarioManageService.deleteScenario(OWNER_ID, publishedScenario.getId());

        assertThat(response.scenarioId()).isEqualTo(publishedScenario.getId());
        assertThat(response.status()).isEqualTo(ScenarioStatus.DELETED);

        Scenario updated = scenarioRepository.findById(publishedScenario.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ScenarioStatus.DELETED);
    }

    @Test
    @DisplayName("DELETE: 권한 없음 (타인 시나리오 삭제 시도)")
    void deleteScenario_unauthorized() {
        assertThatThrownBy(() -> scenarioManageService.deleteScenario(OTHER_ID, publishedScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SCENARIO_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("DELETE: 이미 삭제됨 (삭제된 것 또 삭제 시도)")
    void deleteScenario_alreadyDeleted() {
        assertThatThrownBy(() -> scenarioManageService.deleteScenario(OWNER_ID, deletedScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SCENARIO_ALREADY_DELETED.getMessage());
    }

    // ──────────────────────────────────────────────
    // 2. HIDE API 테스트
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("HIDE: 정상 숨김")
    void hideScenario_success() {
        ScenarioHideResponse response = scenarioManageService.hideScenario(OWNER_ID, publishedScenario.getId());

        assertThat(response.scenarioId()).isEqualTo(publishedScenario.getId());
        assertThat(response.status()).isEqualTo(ScenarioStatus.HIDDEN);

        Scenario updated = scenarioRepository.findById(publishedScenario.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ScenarioStatus.HIDDEN);
    }

    @Test
    @DisplayName("HIDE: 상태 전이 위반 (DRAFT 상태를 HIDDEN으로 변경 불가)")
    void hideScenario_invalidStatus() {
        assertThatThrownBy(() -> scenarioManageService.hideScenario(OWNER_ID, draftScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SCENARIO_CANNOT_HIDE.getMessage());
    }

    // ──────────────────────────────────────────────
    // 3. GET /me (내가 만든 시나리오) API 테스트
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("GET /me: 정상 조회 (DELETED는 제외, DRAFT/PUBLISHED/HIDDEN만 반환)")
    void getMyScenarios_success() {
        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getMyScenarios(OWNER_ID, PageRequest.of(0, 10));

        // ScenarioSummaryResponse::id -> ScenarioSummaryResponse::scenarioId
        List<Long> ids = response.content().stream().map(ScenarioSummaryResponse::scenarioId).toList();

        assertThat(ids).containsExactlyInAnyOrder(draftScenario.getId(), publishedScenario.getId(), hiddenScenario.getId());
        assertThat(ids).doesNotContain(deletedScenario.getId());
    }

    @Test
    @DisplayName("GET /me: 타인의 시나리오는 목록에 노출되지 않음")
    void getMyScenarios_onlyMine() {
        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getMyScenarios(OTHER_ID, PageRequest.of(0, 10));
        assertThat(response.content()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // 4. GET /bookmarked (북마크한 시나리오) API 테스트
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("GET /bookmarked: 정상 조회 (PUBLISHED 상태인 것만 포함)")
    void getBookmarkedScenarios_success() {
        // given: 4가지 상태의 시나리오를 전부 북마크함 (Builder 패턴으로 수정)
        bookmarkRepository.save(ScenarioBookmark.builder().userId(OWNER_ID).scenarioId(draftScenario.getId()).build());
        bookmarkRepository.save(ScenarioBookmark.builder().userId(OWNER_ID).scenarioId(publishedScenario.getId()).build());
        bookmarkRepository.save(ScenarioBookmark.builder().userId(OWNER_ID).scenarioId(hiddenScenario.getId()).build());
        bookmarkRepository.save(ScenarioBookmark.builder().userId(OWNER_ID).scenarioId(deletedScenario.getId()).build());

        // when
        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getBookmarkedScenarios(OWNER_ID, PageRequest.of(0, 10));

        // then: ScenarioSummaryResponse::id -> ScenarioSummaryResponse::scenarioId
        List<Long> ids = response.content().stream().map(ScenarioSummaryResponse::scenarioId).toList();

        // DRAFT, HIDDEN, DELETED는 북마크했더라도 목록에서 제외되어야 함
        assertThat(ids).containsExactly(publishedScenario.getId());
    }
}

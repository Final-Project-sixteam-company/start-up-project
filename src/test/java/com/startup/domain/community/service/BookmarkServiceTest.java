package com.startup.domain.community.service;

import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioBookmarkRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class BookmarkServiceTest {

    @Autowired private BookmarkService bookmarkService;
    @Autowired private ScenarioBookmarkRepository bookmarkRepository;
    @Autowired private ScenarioRepository scenarioRepository;

    private static final Long USER_ID = 1L;
    private Scenario publishedScenario;
    private Scenario draftScenario;

    @BeforeEach
    void setUp() {
        publishedScenario = scenarioRepository.save(Scenario.builder()
                .title("배포된 시나리오")
                .description("설명")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .creatorId(USER_ID)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        draftScenario = scenarioRepository.save(Scenario.builder()
                .title("초안 시나리오")
                .description("설명")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PRIVATE)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .creatorId(USER_ID)
                .status(ScenarioStatus.DRAFT)
                .build());
    }

    @AfterEach
    void tearDown() {
        bookmarkRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("북마크 등록 성공")
    void addBookmark_success() {
        bookmarkService.addBookmark(USER_ID, publishedScenario.getId());

        assertThat(bookmarkRepository.existsByUserIdAndScenarioId(USER_ID, publishedScenario.getId())).isTrue();
    }

    @Test
    @DisplayName("북마크 등록 실패: 권한 없는 시나리오는 북마크 불가 (Negative)")
    void addBookmark_fail_accessDenied() {
        Long otherUserId = 2L;
        assertThatThrownBy(() -> bookmarkService.addBookmark(otherUserId, draftScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SCENARIO_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("북마크 등록 실패: 중복 북마크 방어 (Negative)")
    void addBookmark_fail_alreadyBookmarked() {
        bookmarkService.addBookmark(USER_ID, publishedScenario.getId());

        assertThatThrownBy(() -> bookmarkService.addBookmark(USER_ID, publishedScenario.getId()))
                .isInstanceOf(CommunityException.class)
                .hasMessageContaining(CommunityErrorCode.ALREADY_BOOKMARKED.getMessage());
    }

    @Test
    @DisplayName("북마크 취소 성공 및 멱등성 보장")
    void removeBookmark_success_and_idempotent() {
        // given
        bookmarkService.addBookmark(USER_ID, publishedScenario.getId());
        assertThat(bookmarkRepository.existsByUserIdAndScenarioId(USER_ID, publishedScenario.getId())).isTrue();

        // when (최초 삭제)
        bookmarkService.removeBookmark(USER_ID, publishedScenario.getId());
        
        // then
        assertThat(bookmarkRepository.existsByUserIdAndScenarioId(USER_ID, publishedScenario.getId())).isFalse();

        // when (이미 지워진 상태에서 다시 삭제 - 에러가 나면 안 됨)
        bookmarkService.removeBookmark(USER_ID, publishedScenario.getId());
    }
}

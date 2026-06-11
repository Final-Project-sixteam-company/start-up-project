package com.startup.domain.community.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.dto.ReviewResponse;
import com.startup.domain.community.dto.ReviewUpdateRequest;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioReviewRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ReviewServiceTest {

    @Autowired private ReviewService reviewService;
    @Autowired private ScenarioReviewRepository reviewRepository;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private UserRepository userRepository;

    private User reviewer;
    private User creator;
    private Scenario publishedScenario;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(User.builder()
                .email("creator@test.com")
                .nickname("제작자")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        reviewer = userRepository.save(User.builder()
                .email("reviewer@test.com")
                .nickname("리뷰어")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        publishedScenario = scenarioRepository.save(Scenario.builder()
                .title("리뷰 대상 시나리오")
                .description("설명")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .creatorId(creator.getId())
                .status(ScenarioStatus.PUBLISHED)
                .build());
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("리뷰 등록 성공 및 시나리오 평점 갱신 확인")
    void addReview_success() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "정말 재밌어요", false);
        
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), request);

        assertThat(reviewRepository.existsByUserIdAndScenarioId(reviewer.getId(), publishedScenario.getId())).isTrue();
        
        Scenario updatedScenario = scenarioRepository.findById(publishedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getRatingCount()).isEqualTo(1);
        assertThat(updatedScenario.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("리뷰 등록 실패: 자신의 시나리오")
    void addReview_fail_ownScenario() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "자화자찬", false);
        
        assertThatThrownBy(() -> reviewService.addReview(creator.getId(), publishedScenario.getId(), request))
                .isInstanceOf(CommunityException.class)
                .hasMessageContaining(CommunityErrorCode.CANNOT_REVIEW_OWN.getMessage());
    }

    @Test
    @DisplayName("리뷰 등록 실패: 중복 작성 방어")
    void addReview_fail_alreadyReviewed() {
        ReviewCreateRequest request1 = new ReviewCreateRequest(5, "첫 리뷰", false);
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), request1);

        ReviewCreateRequest request2 = new ReviewCreateRequest(1, "또 쓰기", false);
        assertThatThrownBy(() -> reviewService.addReview(reviewer.getId(), publishedScenario.getId(), request2))
                .isInstanceOf(CommunityException.class)
                .hasMessageContaining(CommunityErrorCode.ALREADY_REVIEWED.getMessage());
    }

    @Test
    @DisplayName("리뷰 목록 페이징 조회 성공 (FETCH JOIN)")
    void getReviews_success() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "정말 재밌어요", false);
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), request);

        PageResponse<ReviewResponse> response = reviewService.getReviews(publishedScenario.getId(), true, PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("리뷰어"); // User FETCH JOIN 검증
        assertThat(response.content().get(0).rating()).isEqualTo(5);
    }

    @Test
    @DisplayName("리뷰 수정 성공 및 평점 재계산")
    void updateReview_success() {
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), new ReviewCreateRequest(5, "좋아요", false));
        Long reviewId = reviewRepository.findAll().get(0).getId();

        ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(1, "생각해보니 별로네요", null);
        ReviewResponse response = reviewService.updateReview(reviewer.getId(), reviewId, updateRequest);

        assertThat(response.rating()).isEqualTo(1);
        assertThat(response.content()).isEqualTo("생각해보니 별로네요");

        Scenario updatedScenario = scenarioRepository.findById(publishedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getAverageRating()).isEqualTo(1.0); // 5점에서 1점으로 재계산됨
    }

    @Test
    @DisplayName("리뷰 삭제 성공 및 평점 롤백")
    void deleteReview_success() {
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), new ReviewCreateRequest(5, "좋아요", false));
        Long reviewId = reviewRepository.findAll().get(0).getId();

        reviewService.deleteReview(reviewer.getId(), reviewId);

        assertThat(reviewRepository.findById(reviewId)).isEmpty();

        Scenario updatedScenario = scenarioRepository.findById(publishedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getRatingCount()).isEqualTo(0);
        assertThat(updatedScenario.getAverageRating()).isEqualTo(0.0); // 전부 지워지면 0.0
    }

    @Test
    @DisplayName("리뷰 삭제 실패: 소유권 불일치")
    void deleteReview_fail_notOwner() {
        reviewService.addReview(reviewer.getId(), publishedScenario.getId(), new ReviewCreateRequest(5, "좋아요", false));
        Long reviewId = reviewRepository.findAll().get(0).getId();

        // 작성자(creator)가 리뷰어(reviewer)의 리뷰를 삭제 시도
        assertThatThrownBy(() -> reviewService.deleteReview(creator.getId(), reviewId))
                .isInstanceOf(CommunityException.class)
                .hasMessageContaining(CommunityErrorCode.NOT_REVIEW_OWNER.getMessage());
    }
}

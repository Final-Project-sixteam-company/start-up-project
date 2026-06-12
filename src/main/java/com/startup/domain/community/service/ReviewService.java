package com.startup.domain.community.service;

import com.startup.common.dto.PageResponse;
import com.startup.common.error.CommonErrorCode;
import com.startup.common.error.BusinessException;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.dto.ReviewResponse;
import com.startup.domain.community.dto.ReviewUpdateRequest;
import com.startup.domain.community.entity.ScenarioReview;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioReviewRepository;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ScenarioReviewRepository reviewRepository;
    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final com.startup.domain.scenario.service.ScenarioAccessService scenarioAccessService;
    private final PlaySessionRepository playSessionRepository;

    // 시나리오 리뷰 작성
    @Transactional
    public Long addReview(Long userId, Long scenarioId, ReviewCreateRequest request) {
        Scenario scenario = getAccessibleScenario(userId, scenarioId);

        if (scenario.getCreatorId() != null && scenario.getCreatorId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.CANNOT_REVIEW_OWN);
        }

        if (!playSessionRepository.existsByUserIdAndScenarioIdAndStatus(userId, scenarioId, com.startup.domain.play.enums.PlaySessionStatus.COMPLETED)) {
            throw new CommunityException(CommunityErrorCode.MUST_PLAY_BEFORE_REVIEW);
        }

        if (reviewRepository.existsByUserIdAndScenarioId(userId, scenarioId)) {
            throw new CommunityException(CommunityErrorCode.ALREADY_REVIEWED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));

        try {
            ScenarioReview review = ScenarioReview.builder()
                    .scenarioId(scenario.getId())
                    .user(user)
                    .rating(request.rating())
                    .content(request.content())
                    .isSpoiler(request.isSpoiler())
                    .build();
            
            ScenarioReview savedReview = reviewRepository.save(review);
            
            // 리뷰 추가 후 즉시 평균 평점 및 리뷰 개수 동기화
            reviewRepository.flush(); // 실제 반영
            scenarioRepository.recalculateRating(scenario.getId());

            return savedReview.getId();
        } catch (DataIntegrityViolationException e) {
            log.warn("리뷰 중복 작성 감지: userId={}, scenarioId={}", userId, scenarioId);
            throw new CommunityException(CommunityErrorCode.ALREADY_REVIEWED);
        }
    }

    // 시나리오 리뷰 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(Long scenarioId, boolean includeSpoiler, Pageable pageable) {
        scenarioAccessService.validateViewable(null, scenarioId);

        Page<ScenarioReview> reviewPage;
        if (includeSpoiler) {
            reviewPage = reviewRepository.findAllByScenarioId(scenarioId, pageable);
        } else {
            reviewPage = reviewRepository.findAllByScenarioIdAndIsSpoilerFalse(scenarioId, pageable);
        }
        
        return PageResponse.from(
            reviewPage.map(ReviewResponse::from)
        );
    }

    // 시나리오 리뷰 수정
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        ScenarioReview review = reviewRepository.findWithUserById(reviewId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.NOT_REVIEW_OWNER);
        }

        review.updateReview(request.rating(), request.content(), request.isSpoiler());
        
        // flush 후 평점 재계산 (수정 시 별점이 변경되었을 수 있으므로)
        reviewRepository.flush();
        scenarioRepository.recalculateRating(review.getScenarioId());
        
        return ReviewResponse.from(review);
    }

    // 시나리오 리뷰 삭제
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        ScenarioReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.NOT_REVIEW_OWNER);
        }

        Long scenarioId = review.getScenarioId();
        reviewRepository.delete(review);
        
        // flush 후 평점 재계산
        reviewRepository.flush();
        scenarioRepository.recalculateRating(scenarioId);
    }

    // 인증 및 접근 권한 검증 후 시나리오 조회
    private Scenario getAccessibleScenario(Long userId, Long scenarioId) {
        scenarioAccessService.validateViewable(userId, scenarioId);
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));
    }
}

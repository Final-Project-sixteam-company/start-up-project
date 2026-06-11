package com.startup.domain.community.service;

import com.startup.common.error.CommonErrorCode;
import com.startup.common.error.BusinessException;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.entity.ScenarioReview;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioReviewRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ScenarioReviewRepository reviewRepository;
    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;

    // 시나리오 리뷰 작성
    @Transactional
    public void addReview(Long userId, Long scenarioId, ReviewCreateRequest request) {
        Scenario scenario = findPublishedScenario(scenarioId);

        if (scenario.getCreatorId() != null && scenario.getCreatorId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.CANNOT_REVIEW_OWN);
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
            reviewRepository.save(review);
            
            // 리뷰 저장 후 부모 시나리오의 평점을 원자적으로 갱신 (Lost Update 방어)
            scenarioRepository.addRating(scenarioId, request.rating());
            
        } catch (DataIntegrityViolationException e) {
            log.warn("리뷰 중복 작성 감지 (따닥 방어): userId={}, scenarioId={}", userId, scenarioId);
            throw new CommunityException(CommunityErrorCode.ALREADY_REVIEWED);
        }
    }

    private Scenario findPublishedScenario(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getStatus() != ScenarioStatus.PUBLISHED) {
            throw new CommunityException(CommunityErrorCode.SCENARIO_NOT_PUBLISHED);
        }

        return scenario;
    }
}

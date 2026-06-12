package com.startup.domain.community.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.entity.ScenarioBookmark;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioBookmarkRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.service.ScenarioAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final ScenarioBookmarkRepository bookmarkRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final UserRepository userRepository;

    // 시나리오 북마크 등록
    @Transactional
    public void addBookmark(Long userId, Long scenarioId) {
        Scenario scenario = getAccessibleScenario(userId, scenarioId);

        // 중복 체크
        if (bookmarkRepository.existsByUserIdAndScenarioId(userId, scenarioId)) {
            throw new CommunityException(CommunityErrorCode.ALREADY_BOOKMARKED);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));

        // DB UNIQUE 제약조건 위반 시 catch
        try {
            ScenarioBookmark bookmark = ScenarioBookmark.builder()
                    .userId(userId)
                    .scenarioId(scenario.getId())
                    .build();
            bookmarkRepository.save(bookmark);
        } catch (DataIntegrityViolationException e) {
            log.warn("북마크 중복 삽입 감지: userId={}, scenarioId={}", userId, scenarioId);
            throw new CommunityException(CommunityErrorCode.ALREADY_BOOKMARKED);
        }
    }

    // 시나리오 북마크 취소
    @Transactional
    public void removeBookmark(Long userId, Long scenarioId) {
        bookmarkRepository.findByUserIdAndScenarioId(userId, scenarioId)
                .ifPresent(bookmarkRepository::delete);
    }

    // 인증 및 접근 권한 검증 후 시나리오 조회
    private Scenario getAccessibleScenario(Long userId, Long scenarioId) {
        scenarioAccessService.validateViewable(userId, scenarioId);
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));
    }
}

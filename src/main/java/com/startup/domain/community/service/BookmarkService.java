package com.startup.domain.community.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
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
import com.startup.domain.scenario.enums.ScenarioStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.startup.domain.scenario.service.RedisScenarioService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final ScenarioBookmarkRepository bookmarkRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final UserRepository userRepository;
    private final RedisScenarioService redisScenarioService;

    // 시나리오 북마크 등록
    @Transactional
    public void addBookmark(Long userId, Long scenarioId) {
        Scenario scenario = getAccessibleScenario(userId, scenarioId);

        // 중복 체크
        if (bookmarkRepository.existsByUserIdAndScenarioId(userId, scenarioId)) {
            throw new CommunityException(CommunityErrorCode.ALREADY_BOOKMARKED);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // DB UNIQUE 제약조건 위반 시 catch
        try {
            ScenarioBookmark bookmark = ScenarioBookmark.builder()
                    .userId(userId)
                    .scenarioId(scenario.getId())
                    .build();
            bookmarkRepository.save(bookmark);
            
            // 캐시 무효화: 북마크 상태 변경 즉시 반영
            redisScenarioService.evictUserListCache(userId);

        } catch (DataIntegrityViolationException e) {
            log.warn("북마크 중복 삽입 감지: userId={}, scenarioId={}", userId, scenarioId);
            throw new CommunityException(CommunityErrorCode.ALREADY_BOOKMARKED);
        }
    }

    // 시나리오 북마크 취소
    @Transactional
    public void removeBookmark(Long userId, Long scenarioId) {
        bookmarkRepository.findByUserIdAndScenarioId(userId, scenarioId)
                .ifPresent(bookmark -> {
                    bookmarkRepository.delete(bookmark);
                    // 캐시 무효화: 북마크 상태 변경 즉시 반영
                    redisScenarioService.evictUserListCache(userId);
                });
    }

    // 인증 및 접근 권한 검증 후 시나리오 조회 (북마크는 PUBLISHED 상태에서만 허용)
    private Scenario getAccessibleScenario(Long userId, Long scenarioId) {
        // 비관적 락을 먼저 획득하여 상태 변경과의 Race Condition 방어
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));
        
        // 락이 걸린 상태에서 권한 검증
        scenarioAccessService.validateViewable(userId, scenarioId);
        
        if (scenario.getStatus() != ScenarioStatus.PUBLISHED) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
        
        return scenario;
    }
}

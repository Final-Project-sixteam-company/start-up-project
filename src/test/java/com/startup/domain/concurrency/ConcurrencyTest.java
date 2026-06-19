package com.startup.domain.concurrency;

import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.repository.ScenarioReviewRepository;
import com.startup.domain.community.service.ReviewService;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 동시성 제어 검증 테스트.
// 멀티스레드 환경에서 비관적 락(Pessimistic Lock)과 DB 원자적 업데이트가
// 데이터 정합성을 올바르게 보장하는지 증명한다.
@SpringBootTest
public class ConcurrencyTest {

    @Autowired private ReviewService reviewService;
    @Autowired private ScenarioReviewRepository reviewRepository;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlaySessionRepository playSessionRepository;

    private Scenario scenario;
    private final List<User> users = new ArrayList<>();

    private static final int CONCURRENT_USERS = 100;

    @BeforeEach
    void setUp() {
        // 시나리오 생성 (creatorId를 null로 두어 자기 시나리오 리뷰 제한을 우회)
        scenario = scenarioRepository.save(Scenario.builder()
                .title("동시성 테스트 시나리오")
                .description("100명이 동시에 리뷰를 작성하는 시나리오")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        // 100명의 유저 생성 + 각각 플레이 완료 상태 세팅
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            User user = userRepository.save(User.builder()
                    .email("concurrent_user_" + i + "@test.com")
                    .nickname("동시유저" + i)
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build());
            users.add(user);

            // 리뷰 작성 전제조건: 해당 시나리오를 플레이 완료해야 함
            PlaySession session = PlaySession.builder()
                    .userId(user.getId())
                    .scenarioId(scenario.getId())
                    .build();
            session.markCompleted();
            playSessionRepository.save(session);
        }
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        users.clear();
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 리뷰를 작성해도 평점과 리뷰 카운트가 정확히 반영된다")
    void concurrent_review_rating_consistency() throws InterruptedException {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명의 유저가 동시에 리뷰를 작성한다
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final User user = users.get(i);
            final int rating = (i % 5) + 1; // 1~5점 고르게 분포

            executor.submit(() -> {
                readyLatch.countDown();    // "나 준비됐어" 신호
                try {
                    startLatch.await();    // 모든 스레드가 준비될 때까지 대기
                    ReviewCreateRequest request = new ReviewCreateRequest(rating, "동시성 테스트 리뷰", false);
                    reviewService.addReview(user.getId(), scenario.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();  // 모든 스레드가 준비될 때까지 대기
        startLatch.countDown();  // 동시에 출발!
        doneLatch.await();       // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();

        // then: 100명 모두 성공, 리뷰 100개, 평점 정확히 계산
        assertThat(successCount.get()).isEqualTo(CONCURRENT_USERS);
        assertThat(failCount.get()).isEqualTo(0);

        // DB에 리뷰가 정확히 100개 저장되었는지 확인
        long reviewCount = reviewRepository.count();
        assertThat(reviewCount).isEqualTo(CONCURRENT_USERS);

        // 시나리오의 ratingCount가 정확히 100인지 확인 (유실 없음)
        Scenario updatedScenario = scenarioRepository.findById(scenario.getId()).orElseThrow();
        assertThat(updatedScenario.getRatingCount()).isEqualTo(CONCURRENT_USERS);

        // 평균 평점 검증: 1~5점이 각 20명씩 → 평균 = (1+2+3+4+5) * 20 / 100 = 3.0
        assertThat(updatedScenario.getAverageRating()).isEqualTo(3.0);
    }

    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 플레이를 시작해도 playCount가 정확히 100 증가한다")
    void concurrent_playCount_increment_consistency() throws InterruptedException {
        // given: 초기 playCount 확인
        int initialPlayCount = scenarioRepository.findById(scenario.getId()).orElseThrow().getPlayCount();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 100명이 동시에 incrementPlayCount를 호출한다
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    // @Modifying 쿼리는 트랜잭션이 필수이므로 TransactionTemplate으로 감싼다
                    transactionTemplate.executeWithoutResult(status -> {
                        scenarioRepository.incrementPlayCount(scenario.getId());
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // DB 원자적 업데이트이므로 락 충돌이나 데드락 없이 모두 성공해야 한다
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then: playCount가 정확히 100 증가했는지 확인 (Lost Update 없음)
        assertThat(successCount.get()).isEqualTo(CONCURRENT_USERS);

        Scenario updatedScenario = scenarioRepository.findById(scenario.getId()).orElseThrow();
        assertThat(updatedScenario.getPlayCount()).isEqualTo(initialPlayCount + CONCURRENT_USERS);
    }
}

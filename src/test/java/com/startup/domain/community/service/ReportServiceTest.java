package com.startup.domain.community.service;

import com.startup.domain.community.dto.ReportCreateRequest;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioReportRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
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
public class ReportServiceTest {

    @Autowired private ReportService reportService;
    @Autowired private ScenarioReportRepository reportRepository;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private UserRepository userRepository;

    private Long REPORTER_ID;
    private Scenario publishedScenario;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("reporter@test.com")
                .nickname("리포터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        REPORTER_ID = user.getId();

        publishedScenario = scenarioRepository.save(Scenario.builder()
                .title("신고 대상 시나리오")
                .description("설명")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .creatorId(999L)
                .status(ScenarioStatus.PUBLISHED)
                .build());
    }

    @AfterEach
    void tearDown() {
        reportRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("신고 등록 성공")
    void addReport_success() {
        ReportCreateRequest request = new ReportCreateRequest("스포일러", "스포일러가 있어요");

        reportService.addReport(REPORTER_ID, publishedScenario.getId(), request);

        assertThat(reportRepository.existsByReporterIdAndScenarioId(REPORTER_ID, publishedScenario.getId())).isTrue();
    }

    @Test
    @DisplayName("신고 등록 실패: 중복 신고 방어 (Negative)")
    void addReport_fail_alreadyReported() {
        ReportCreateRequest request = new ReportCreateRequest("스포일러", "스포일러가 있어요");
        reportService.addReport(REPORTER_ID, publishedScenario.getId(), request);

        assertThatThrownBy(() -> reportService.addReport(REPORTER_ID, publishedScenario.getId(), request))
                .isInstanceOf(CommunityException.class)
                .hasMessageContaining(CommunityErrorCode.ALREADY_REPORTED.getMessage());
    }
}

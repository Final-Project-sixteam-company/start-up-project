package com.startup.domain.community.service;

import com.startup.domain.community.dto.ReportCreateRequest;
import com.startup.domain.community.dto.ReportStatusResponse;
import com.startup.domain.community.entity.ScenarioReport;
import com.startup.domain.community.error.CommunityErrorCode;
import com.startup.domain.community.error.CommunityException;
import com.startup.domain.community.repository.ScenarioReportRepository;
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
public class ReportService {

    private final ScenarioReportRepository reportRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;

    // 시나리오 신고 접수
    @Transactional
    public ReportStatusResponse addReport(Long reporterId, Long scenarioId, ReportCreateRequest request) {
        Scenario scenario = getAccessibleScenario(reporterId, scenarioId);

        // 애플리케이션 레벨 중복 신고 체크
        if (reportRepository.existsByReporterIdAndScenarioId(reporterId, scenarioId)) {
            throw new CommunityException(CommunityErrorCode.ALREADY_REPORTED);
        }

        try {
            ScenarioReport report = ScenarioReport.builder()
                    .reporterId(reporterId)
                    .scenarioId(scenario.getId())
                    .reason(request.reason())
                    .detail(request.detail())
                    .build();
            ScenarioReport savedReport = reportRepository.save(report);
            
            // TODO: 신고 누적 N건 이상 자동 블라인드 등 후속 로직 (기획 확정 시 추가)
            
            return new ReportStatusResponse(savedReport.getId(), savedReport.getStatus());
        } catch (DataIntegrityViolationException e) {
            log.warn("신고 중복 삽입 감지 (따닥 방어): reporterId={}, scenarioId={}", reporterId, scenarioId);
            throw new CommunityException(CommunityErrorCode.ALREADY_REPORTED);
        }
    }

    // 인증 및 접근 권한 검증 후 시나리오 조회
    private Scenario getAccessibleScenario(Long userId, Long scenarioId) {
        scenarioAccessService.validateViewable(userId, scenarioId);
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));
    }
}

package com.startup.domain.play.service;

import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.play.dto.EvidenceUnlockResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceSuspect;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.enums.RelationType;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceSuspectRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "clueroom.assets.public-base-url=https://assets.example.com")
class PlaySessionMvpApiTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private UnlockedEvidenceRepository unlockedEvidenceRepository;

    @Autowired
    private SuspectRepository suspectRepository;

    @Autowired
    private EvidenceSuspectRepository evidenceSuspectRepository;

    @Autowired
    private InterrogationLogRepository interrogationLogRepository;

    @AfterEach
    void tearDown() {
        interrogationLogRepository.deleteAllInBatch();
        evidenceSuspectRepository.deleteAllInBatch();
        unlockedEvidenceRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    void unlockEvidence_allowsManualEvidenceAndKeepsRepeatedCallIdempotent() {
        Scenario scenario = saveScenario();
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .title("manual evidence")
                .description("manual evidence detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.MANUAL)
                .sortOrder(1)
                .build());
        PlaySession session = saveSession(101L, scenario.getId());

        EvidenceUnlockResponse first =
                playSessionService.unlockEvidence(101L, session.getId(), evidence.getId(), null);
        EvidenceUnlockResponse second =
                playSessionService.unlockEvidence(101L, session.getId(), evidence.getId(), null);

        assertThat(first.evidenceId()).isEqualTo(evidence.getId());
        assertThat(first.isUnlocked()).isTrue();
        assertThat(first.unlockedAt()).isNotNull();
        assertThat(second.unlockedAt()).isEqualTo(first.unlockedAt());
        assertThat(unlockedEvidenceRepository.findAllByPlaySessionId(session.getId())).hasSize(1);
    }

    @Test
    void unlockEvidence_rejectsEvidenceWhenUnlockConditionIsNotSatisfied() {
        Scenario scenario = saveScenario();
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .title("locked evidence")
                .description("locked evidence detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.NONE)
                .sortOrder(1)
                .build());
        PlaySession session = saveSession(102L, scenario.getId());

        assertThatThrownBy(() -> playSessionService.unlockEvidence(102L, session.getId(), evidence.getId(), null))
                .isInstanceOf(PlayException.class)
                .extracting("errorCode")
                .isEqualTo(PlayErrorCode.EVIDENCE_NOT_UNLOCKABLE);
    }

    @Test
    void getSuspectDetail_returnsOnlyPublicDataAndUnlockedRelatedEvidence() {
        Scenario scenario = saveScenario();
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(scenario.getId())
                .name("한지오")
                .role("비서실장")
                .relationToVictim("측근")
                .publicProfile("이사장의 일정을 관리한다.")
                .publicStatement("회의실에 있었다고 주장한다.")
                .alibi("20시 이후 1층에 있었다고 말한다.")
                .portraitAssetKey("official/seowolchae/v1/characters/SUSPECT_SECRETARY.png")
                .suspicionLevel(70)
                .sortOrder(1)
                .build());
        Evidence unlockedEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .title("해금된 관련 증거")
                .description("related unlocked detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.MANUAL)
                .sortOrder(1)
                .build());
        Evidence lockedEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .title("잠긴 관련 증거")
                .description("related locked detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.NONE)
                .sortOrder(2)
                .build());
        evidenceSuspectRepository.save(EvidenceSuspect.builder()
                .evidenceId(unlockedEvidence.getId())
                .suspectId(suspect.getId())
                .relationType(RelationType.RELATED)
                .build());
        evidenceSuspectRepository.save(EvidenceSuspect.builder()
                .evidenceId(lockedEvidence.getId())
                .suspectId(suspect.getId())
                .relationType(RelationType.RELATED)
                .build());
        PlaySession session = saveSession(103L, scenario.getId());
        unlockedEvidenceRepository.save(UnlockedEvidence.builder()
                .playSessionId(session.getId())
                .evidenceId(unlockedEvidence.getId())
                .unlockedReason("TEST")
                .build());
        interrogationLogRepository.save(InterrogationLog.builder()
                .playSessionId(session.getId())
                .suspectId(suspect.getId())
                .questionType(QuestionType.FREE)
                .question("어디에 있었습니까?")
                .answer("회의실에 있었습니다.")
                .aiModel("test-model")
                .build());

        var response = playSessionService.getSuspectDetail(103L, session.getId(), suspect.getId());

        assertThat(response.suspectId()).isEqualTo(suspect.getId());
        assertThat(response.publicProfile()).isEqualTo("이사장의 일정을 관리한다.");
        assertThat(response.portraitImageUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/characters/SUSPECT_SECRETARY.png");
        assertThat(response.relatedEvidences())
                .extracting("evidenceId")
                .containsExactly(unlockedEvidence.getId());
        assertThat(response.interrogationLogs()).hasSize(1);
    }

    @Test
    void getEvidenceDetail_returnsGuidanceAndMasksLockedCompareEvidence() {
        Scenario scenario = saveScenario();
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(scenario.getId())
                .code("SUSPECT_TEST")
                .name("테스트 용의자")
                .role("용의자")
                .publicProfile("공개 프로필")
                .publicStatement("공개 진술")
                .alibi("공개 알리바이")
                .suspicionLevel(50)
                .sortOrder(1)
                .build());
        Evidence currentEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_CURRENT")
                .title("현재 증거")
                .description("현재 증거 상세")
                .oneLine("현재 증거 요약")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(true)
                .unlockType(EvidenceUnlockType.PHASE)
                .guidanceJson("""
                        {
                          "readingPoints": ["현재 증거의 시간대를 비교한다."],
                          "compareWithEvidenceCodes": ["EVIDENCE_UNLOCKED", "EVIDENCE_LOCKED"],
                          "suggestedQuestions": [
                            {
                              "targetCharacterCode": "SUSPECT_TEST",
                              "question": "이 증거와 다른 기록의 차이를 설명할 수 있나요?"
                            }
                          ]
                        }
                        """)
                .sortOrder(1)
                .build());
        Evidence unlockedCompare = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_UNLOCKED")
                .title("해금 비교 증거")
                .description("해금 비교 증거 상세")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(true)
                .unlockType(EvidenceUnlockType.PHASE)
                .sortOrder(2)
                .build());
        Evidence lockedCompare = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_LOCKED")
                .title("잠긴 비교 증거")
                .description("잠긴 비교 증거 상세")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.PHASE)
                .unlockPhase("PHASE_2_SYSTEM_LOGS")
                .sortOrder(3)
                .build());
        PlaySession session = saveSession(104L, scenario.getId());

        var response = playSessionService.getEvidenceDetail(104L, session.getId(), currentEvidence.getId());

        assertThat(response.guidance()).isNotNull();
        assertThat(response.guidance().readingPoints()).containsExactly("현재 증거의 시간대를 비교한다.");
        assertThat(response.guidance().compareEvidences())
                .extracting("evidenceId", "isUnlocked")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(unlockedCompare.getId(), true),
                        org.assertj.core.groups.Tuple.tuple(lockedCompare.getId(), false)
                );
        assertThat(response.guidance().compareEvidences().get(1).title()).isEqualTo("잠긴 비교 증거");
        assertThat(response.guidance().compareEvidences().get(1).unlockHint()).isNotBlank();
        assertThat(response.guidance().suggestedQuestions()).hasSize(1);
        assertThat(response.guidance().suggestedQuestions().getFirst().targetSuspectId()).isEqualTo(suspect.getId());
        assertThat(response.guidance().suggestedQuestions().getFirst().presentedEvidenceId()).isEqualTo(currentEvidence.getId());
        assertThat(response.guidance().suggestedQuestions().getFirst().questionType()).isEqualTo("EVIDENCE_PRESENTED");
    }

    private Scenario saveScenario() {
        return scenarioRepository.save(Scenario.builder()
                .title("mvp api test")
                .description("mvp api test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());
    }

    private PlaySession saveSession(Long userId, Long scenarioId) {
        return playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .build());
    }
}

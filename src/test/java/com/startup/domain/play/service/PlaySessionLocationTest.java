package com.startup.domain.play.service;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.ScenarioLocation;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "clueroom.assets.public-base-url=https://assets.example.com")
class PlaySessionLocationTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private UnlockedEvidenceRepository unlockedEvidenceRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private ScenarioLocationRepository scenarioLocationRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @AfterEach
    void tearDown() {
        unlockedEvidenceRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        scenarioLocationRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    void getLocations_resolvesMapAndLocationImagesAndEvidenceCounts() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("location image scenario")
                .description("location image test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .mapAssetKey("official/seowolchae/v1/scenario/SCENARIO.map.png")
                .status(ScenarioStatus.PUBLISHED)
                .build());

        ScenarioLocation bedroom = scenarioLocationRepository.save(ScenarioLocation.builder()
                .scenarioId(scenario.getId())
                .code("LOC_BEDROOM")
                .name("침실")
                .floor("2F")
                .description("피해자가 발견된 방")
                .imageAssetKey("official/seowolchae/v1/locations/LOC_BEDROOM.png")
                .mapX(120)
                .mapY(80)
                .sortOrder(1)
                .build());

        ScenarioLocation diningRoom = scenarioLocationRepository.save(ScenarioLocation.builder()
                .scenarioId(scenario.getId())
                .code("LOC_DINING_ROOM")
                .name("다이닝룸")
                .floor("1F")
                .description("만찬이 열린 장소")
                .imageAssetKey("official/seowolchae/v1/locations/LOC_DINING_ROOM.png")
                .mapX(40)
                .mapY(160)
                .sortOrder(2)
                .build());

        Evidence unlockedBedroomEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .locationId(bedroom.getId())
                .title("해금 증거")
                .description("해금 증거 설명")
                .evidenceType(EvidenceType.VISUAL)
                .importance(EvidenceImportance.NORMAL)
                .unlockType(EvidenceUnlockType.NONE)
                .isInitialPublic(false)
                .sortOrder(1)
                .build());

        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .locationId(bedroom.getId())
                .title("잠긴 증거")
                .description("잠긴 증거 설명")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .unlockType(EvidenceUnlockType.NONE)
                .isInitialPublic(false)
                .sortOrder(2)
                .build());

        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .locationId(diningRoom.getId())
                .title("다이닝룸 증거")
                .description("다이닝룸 증거 설명")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .unlockType(EvidenceUnlockType.NONE)
                .isInitialPublic(false)
                .sortOrder(3)
                .build());

        Long userId = 1001L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());
        unlockedEvidenceRepository.save(UnlockedEvidence.builder()
                .playSessionId(session.getId())
                .evidenceId(unlockedBedroomEvidence.getId())
                .unlockedReason("TEST")
                .build());

        var response = playSessionService.getLocations(userId, session.getId());

        assertThat(response.sessionId()).isEqualTo(session.getId());
        assertThat(response.scenarioId()).isEqualTo(scenario.getId());
        assertThat(response.scenarioTitle()).isEqualTo("location image scenario");
        assertThat(response.mapImageUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/scenario/SCENARIO.map.png");
        assertThat(response.locations()).hasSize(2);

        var bedroomResponse = response.locations().getFirst();
        assertThat(bedroomResponse.locationId()).isEqualTo(bedroom.getId());
        assertThat(bedroomResponse.locationCode()).isEqualTo("LOC_BEDROOM");
        assertThat(bedroomResponse.imageUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/locations/LOC_BEDROOM.png");
        assertThat(bedroomResponse.mapX()).isEqualTo(120);
        assertThat(bedroomResponse.mapY()).isEqualTo(80);
        assertThat(bedroomResponse.totalEvidenceCount()).isEqualTo(2);
        assertThat(bedroomResponse.unlockedEvidenceCount()).isEqualTo(1);

        var diningRoomResponse = response.locations().get(1);
        assertThat(diningRoomResponse.locationId()).isEqualTo(diningRoom.getId());
        assertThat(diningRoomResponse.totalEvidenceCount()).isEqualTo(1);
        assertThat(diningRoomResponse.unlockedEvidenceCount()).isZero();
    }
}

package com.startup.domain.play.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.ScenarioLocation;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.entity.TimelineEvent;
import lombok.Builder;

import java.util.List;

@Builder
public record PlayEvidenceDetailResponse(
        Long evidenceId,
        String title,
        String description,
        String imageUrl,
        LocationInfo location,
        List<SuspectInfo> relatedSuspects,
        List<TimelineInfo> relatedTimelineEvents,
        EvidenceGuidanceInfo guidance
) {
    // JSON 중첩 객체를 위한 내부 record 선언
    public record LocationInfo(Long locationId, String name) {}
    public record SuspectInfo(Long suspectId, String name) {}
    public record TimelineInfo(String time, String title) {}
    public record EvidenceGuidanceInfo(
            List<String> readingPoints,
            List<CompareEvidenceInfo> compareEvidences,
            List<SuggestedQuestionInfo> suggestedQuestions
    ) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompareEvidenceInfo(
            Long evidenceId,
            String evidenceCode,
            String title,
            Boolean isUnlocked,
            String unlockHint
    ) {}
    public record SuggestedQuestionInfo(
            Long targetSuspectId,
            String targetName,
            String question,
            Long presentedEvidenceId,
            String questionType
    ) {}

    public static PlayEvidenceDetailResponse of(
            Evidence evidence,
            String resolvedDescription,
            String resolvedImageUrl,
            ScenarioLocation location,
            List<Suspect> relatedSuspects,
            List<TimelineEvent> timelineEvents,
            EvidenceGuidanceInfo guidance
    ) {
        LocationInfo locationInfo = location != null ?
                new LocationInfo(location.getId(), location.getName()) : null;

        List<SuspectInfo> suspectInfos = relatedSuspects.stream()
                .map(s -> new SuspectInfo(s.getId(), s.getName()))
                .toList();

        List<TimelineInfo> timelineInfos = timelineEvents.stream()
                .map(t -> new TimelineInfo(t.getEventTime(), t.getTitle()))
                .toList();

        return PlayEvidenceDetailResponse.builder()
                .evidenceId(evidence.getId())
                .title(evidence.getTitle())
                .description(resolvedDescription)
                .imageUrl(resolvedImageUrl)
                .location(locationInfo)
                .relatedSuspects(suspectInfos)
                .relatedTimelineEvents(timelineInfos)
                .guidance(guidance)
                .build();
    }
}

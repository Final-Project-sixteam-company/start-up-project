package com.startup.domain.play.dto;

import com.startup.domain.scenario.entity.TimelineEvent;

public record PlayTimelineResponse(
        String time,
        String title,
        String description,
        String eventType,
        Boolean isTrueEvent,
        Long relatedEvidenceId
) {
    public static PlayTimelineResponse from(TimelineEvent event) {
        return new PlayTimelineResponse(
                event.getEventTime(),
                event.getTitle(),
                event.getDescription(),
                event.getEventType(),
                event.getIsTrueEvent(),
                event.getRelatedEvidenceId()
        );
    }
}

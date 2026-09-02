package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.MeetingTypeOption;
import java.util.UUID;

public record MeetingTypeSummary(UUID id, String name) {

    public static MeetingTypeSummary from(MeetingTypeOption option) {
        return new MeetingTypeSummary(option.getId(), option.getName());
    }
}

package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.AgendaItem;
import java.util.UUID;

public record AgendaItemDto(UUID id, int position, String title, String description) {

    public static AgendaItemDto from(AgendaItem item) {
        return new AgendaItemDto(item.getId(), item.getPosition(), item.getTitle(), item.getDescription());
    }
}

package com.fris.boardportal.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaItemRepository extends JpaRepository<AgendaItem, UUID> {

    List<AgendaItem> findByMeetingIdOrderByPositionAsc(UUID meetingId);

    Optional<AgendaItem> findByIdAndMeetingId(UUID id, UUID meetingId);

    int countByMeetingId(UUID meetingId);
}

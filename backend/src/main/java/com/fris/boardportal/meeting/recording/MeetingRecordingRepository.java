package com.fris.boardportal.meeting.recording;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, UUID> {

    List<MeetingRecording> findByMeetingIdOrderByCreatedAtDesc(UUID meetingId);

    Optional<MeetingRecording> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

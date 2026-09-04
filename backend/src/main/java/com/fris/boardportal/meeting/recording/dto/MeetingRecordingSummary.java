package com.fris.boardportal.meeting.recording.dto;

import com.fris.boardportal.meeting.recording.TranscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record MeetingRecordingSummary(
        UUID id,
        UUID meetingId,
        String fileName,
        String contentType,
        long fileSize,
        String recordedByName,
        Instant createdAt,
        String transcriptText,
        TranscriptionStatus transcriptionStatus) {
}

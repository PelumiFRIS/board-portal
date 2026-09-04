package com.fris.boardportal.meeting.recording;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meeting_recordings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRecording {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "transcript_text")
    private String transcriptText;

    @Enumerated(EnumType.STRING)
    @Column(name = "transcription_status", nullable = false)
    private TranscriptionStatus transcriptionStatus;

    public static MeetingRecording create(UUID organizationId, UUID meetingId, String fileName, String contentType,
            byte[] fileData, UUID recordedBy) {
        MeetingRecording recording = new MeetingRecording();
        recording.setId(UUID.randomUUID());
        recording.setOrganizationId(organizationId);
        recording.setMeetingId(meetingId);
        recording.setFileName(fileName);
        recording.setContentType(contentType);
        recording.setFileSize(fileData.length);
        recording.setFileData(fileData);
        recording.setRecordedBy(recordedBy);
        recording.setCreatedAt(Instant.now());
        recording.setTranscriptionStatus(TranscriptionStatus.NONE);
        return recording;
    }
}

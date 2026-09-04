package com.fris.boardportal.meeting.recording;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.meeting.recording.dto.MeetingRecordingSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MeetingRecordingService {

    private final MeetingRecordingRepository meetingRecordingRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public MeetingRecordingService(MeetingRecordingRepository meetingRecordingRepository,
            MeetingRepository meetingRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.meetingRecordingRepository = meetingRecordingRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<MeetingRecordingSummary> listForMeeting(AppUserPrincipal principal, UUID meetingId) {
        findMeetingInOrg(principal, meetingId);
        Map<UUID, User> usersById = userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return meetingRecordingRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
                .map(r -> toSummary(r, usersById))
                .toList();
    }

    @Transactional
    public MeetingRecordingSummary upload(AppUserPrincipal admin, UUID meetingId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A recording file is required");
        }
        findMeetingInOrg(admin, meetingId);

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded recording", e);
        }

        MeetingRecording recording = MeetingRecording.create(
                admin.getOrganizationId(),
                meetingId,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "recording",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                fileData,
                admin.getUserId());
        meetingRecordingRepository.save(recording);

        auditLogService.record(admin, AuditAction.RECORDING_UPLOADED, AuditEntityType.MEETING_RECORDING,
                recording.getId(), "Uploaded a recording for this meeting");

        Map<UUID, User> usersById = userRepository.findByOrganizationId(admin.getOrganizationId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return toSummary(recording, usersById);
    }

    public MeetingRecording getContent(AppUserPrincipal principal, UUID meetingId, UUID recordingId) {
        MeetingRecording recording = findInOrg(principal, recordingId);
        if (!recording.getMeetingId().equals(meetingId)) {
            throw ApiException.notFound("Recording not found");
        }
        return recording;
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID meetingId, UUID recordingId) {
        MeetingRecording recording = findInOrg(admin, recordingId);
        if (!recording.getMeetingId().equals(meetingId)) {
            throw ApiException.notFound("Recording not found");
        }
        meetingRecordingRepository.delete(recording);

        auditLogService.record(admin, AuditAction.RECORDING_DELETED, AuditEntityType.MEETING_RECORDING,
                recording.getId(), "Deleted a recording from this meeting");
    }

    private void findMeetingInOrg(AppUserPrincipal principal, UUID meetingId) {
        meetingRepository.findByIdAndOrganizationId(meetingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting not found"));
    }

    private MeetingRecording findInOrg(AppUserPrincipal principal, UUID recordingId) {
        return meetingRecordingRepository.findByIdAndOrganizationId(recordingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Recording not found"));
    }

    private MeetingRecordingSummary toSummary(MeetingRecording recording, Map<UUID, User> usersById) {
        User recordedBy = usersById.get(recording.getRecordedBy());
        String recordedByName = recordedBy != null
                ? recordedBy.getFirstName() + " " + recordedBy.getLastName()
                : "Unknown";
        return new MeetingRecordingSummary(recording.getId(), recording.getMeetingId(), recording.getFileName(),
                recording.getContentType(), recording.getFileSize(), recordedByName, recording.getCreatedAt());
    }
}

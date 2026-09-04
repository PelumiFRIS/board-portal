package com.fris.boardportal.meeting.recording;

import com.fris.boardportal.meeting.recording.dto.MeetingRecordingSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meetings/{meetingId}/recordings")
public class MeetingRecordingController {

    private final MeetingRecordingService meetingRecordingService;

    public MeetingRecordingController(MeetingRecordingService meetingRecordingService) {
        this.meetingRecordingService = meetingRecordingService;
    }

    @GetMapping
    public List<MeetingRecordingSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID meetingId) {
        return meetingRecordingService.listForMeeting(principal, meetingId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<MeetingRecordingSummary> upload(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID meetingId, @RequestParam("file") MultipartFile file) {
        MeetingRecordingSummary created = meetingRecordingService.upload(principal, meetingId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{recordingId}/content")
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID meetingId, @PathVariable UUID recordingId) {
        MeetingRecording recording = meetingRecordingService.getContent(principal, meetingId, recordingId);
        String encodedName = URLEncoder.encode(recording.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recording.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(recording.getFileData());
    }

    @DeleteMapping("/{recordingId}")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID meetingId, @PathVariable UUID recordingId) {
        meetingRecordingService.delete(principal, meetingId, recordingId);
        return ResponseEntity.noContent().build();
    }
}

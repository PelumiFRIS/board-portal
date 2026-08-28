package com.fris.boardportal.meeting;

import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MatterArisingItem;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.UpdateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.UpdateMeetingRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public List<MeetingSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) UUID committeeId) {
        return meetingService.listForOrganization(principal, committeeId);
    }

    @GetMapping("/{id}")
    public MeetingDetail detail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return meetingService.getDetail(principal, id);
    }

    @GetMapping("/{id}/matters-arising")
    public List<MatterArisingItem> mattersArising(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return meetingService.getMattersArising(principal, id);
    }

    @GetMapping("/{id}/ics")
    public ResponseEntity<byte[]> ics(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        byte[] ics = meetingService.generateIcs(principal, id);
        String encodedName = URLEncoder.encode(id + ".ics", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(ics);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        byte[] html = meetingService.exportRecordHtml(principal, id);
        String encodedName = URLEncoder.encode(id + "-record.html", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(html);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeetingSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateMeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.create(principal, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MeetingDetail update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateMeetingRequest request) {
        return meetingService.update(principal, id, request);
    }

    @PostMapping("/{id}/agenda-items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgendaItemDto> addAgendaItem(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody CreateAgendaItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.addAgendaItem(principal, id, request));
    }

    @PatchMapping("/{id}/agenda-items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AgendaItemDto updateAgendaItem(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID itemId, @RequestBody UpdateAgendaItemRequest request) {
        return meetingService.updateAgendaItem(principal, id, itemId, request);
    }

    @DeleteMapping("/{id}/agenda-items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAgendaItem(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID itemId) {
        meetingService.deleteAgendaItem(principal, id, itemId);
        return ResponseEntity.noContent().build();
    }
}

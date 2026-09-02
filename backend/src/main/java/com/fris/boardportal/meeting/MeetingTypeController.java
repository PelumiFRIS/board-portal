package com.fris.boardportal.meeting;

import com.fris.boardportal.meeting.dto.CreateMeetingTypeRequest;
import com.fris.boardportal.meeting.dto.MeetingTypeSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meeting-types")
public class MeetingTypeController {

    private final MeetingTypeService meetingTypeService;

    public MeetingTypeController(MeetingTypeService meetingTypeService) {
        this.meetingTypeService = meetingTypeService;
    }

    @GetMapping
    public List<MeetingTypeSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return meetingTypeService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<MeetingTypeSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateMeetingTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingTypeService.create(principal, request.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        meetingTypeService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

package com.fris.boardportal.publicapi;

import com.fris.boardportal.actionitem.ActionItemService;
import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.document.DocumentService;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.MeetingService;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.resolution.ResolutionService;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublicApiController {

    private final MeetingService meetingService;
    private final ResolutionService resolutionService;
    private final ActionItemService actionItemService;
    private final DocumentService documentService;

    public PublicApiController(MeetingService meetingService, ResolutionService resolutionService,
            ActionItemService actionItemService, DocumentService documentService) {
        this.meetingService = meetingService;
        this.resolutionService = resolutionService;
        this.actionItemService = actionItemService;
        this.documentService = documentService;
    }

    @GetMapping("/meetings")
    public List<MeetingSummary> meetings(@AuthenticationPrincipal AppUserPrincipal principal) {
        return meetingService.listForOrganization(principal, null);
    }

    @GetMapping("/resolutions")
    public List<ResolutionSummary> resolutions(@AuthenticationPrincipal AppUserPrincipal principal) {
        return resolutionService.listForOrganization(principal, null);
    }

    @GetMapping("/action-items")
    public List<ActionItemSummary> actionItems(@AuthenticationPrincipal AppUserPrincipal principal) {
        return actionItemService.listForOrganization(principal, null);
    }

    @GetMapping("/documents")
    public List<DocumentSummary> documents(@AuthenticationPrincipal AppUserPrincipal principal) {
        return documentService.listForOrganization(principal, null, null, null);
    }
}

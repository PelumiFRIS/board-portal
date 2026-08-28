package com.fris.boardportal.resolution;

import com.fris.boardportal.resolution.dto.CastVoteRequest;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionDetail;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/resolutions")
public class ResolutionController {

    private final ResolutionService resolutionService;

    public ResolutionController(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @GetMapping
    public List<ResolutionSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) UUID meetingId) {
        return resolutionService.listForOrganization(principal, meetingId);
    }

    @GetMapping("/{id}")
    public ResolutionDetail detail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return resolutionService.getDetail(principal, id);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AppUserPrincipal principal) {
        byte[] csv = resolutionService.exportCsv(principal);
        String filename = "resolutions-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolutionSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateResolutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resolutionService.create(principal, request));
    }

    @PatchMapping("/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResolutionSummary open(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return resolutionService.open(principal, id);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResolutionSummary close(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return resolutionService.close(principal, id);
    }

    @PostMapping("/{id}/votes")
    public ResolutionSummary castVote(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody CastVoteRequest request) {
        return resolutionService.castVote(principal, id, request.choice());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        resolutionService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

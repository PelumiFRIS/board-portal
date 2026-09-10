package com.fris.boardportal.organization;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.organization.dto.OrganizationOnboardRequest;
import com.fris.boardportal.organization.dto.OrganizationOnboardResponse;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody OrganizationSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.signup(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationOnboardResponse> onboardClient(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody OrganizationOnboardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.onboardClient(principal, request));
    }
}

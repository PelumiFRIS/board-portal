package com.fris.boardportal.organization;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}

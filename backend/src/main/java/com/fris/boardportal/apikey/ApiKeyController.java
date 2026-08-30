package com.fris.boardportal.apikey;

import com.fris.boardportal.apikey.dto.ApiKeySummary;
import com.fris.boardportal.apikey.dto.CreateApiKeyRequest;
import com.fris.boardportal.apikey.dto.CreateApiKeyResponse;
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
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiKeySummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return apiKeyService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateApiKeyResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(principal, request.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        apiKeyService.revoke(principal, id);
        return ResponseEntity.noContent().build();
    }
}

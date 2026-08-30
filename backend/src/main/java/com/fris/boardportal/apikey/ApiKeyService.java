package com.fris.boardportal.apikey;

import com.fris.boardportal.apikey.dto.ApiKeySummary;
import com.fris.boardportal.apikey.dto.CreateApiKeyResponse;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.security.AppUserPrincipal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "bpk_";
    private static final int RAW_KEY_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = 12;

    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AuditLogService auditLogService) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditLogService = auditLogService;
    }

    public List<ApiKeySummary> listForOrganization(AppUserPrincipal admin) {
        return apiKeyRepository.findByOrganizationIdOrderByCreatedAtDesc(admin.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public CreateApiKeyResponse create(AppUserPrincipal admin, String name) {
        String rawKey = generateRawKey();
        String hash = ApiKeyHasher.hash(rawKey);
        String displayPrefix = rawKey.substring(0, DISPLAY_PREFIX_LENGTH);

        ApiKey apiKey = ApiKey.create(admin.getOrganizationId(), name, hash, displayPrefix, admin.getUserId());
        apiKeyRepository.save(apiKey);

        auditLogService.record(admin, AuditAction.API_KEY_CREATED, AuditEntityType.API_KEY, apiKey.getId(),
                "Created API key \"" + apiKey.getName() + "\"");

        return new CreateApiKeyResponse(toSummary(apiKey), rawKey);
    }

    @Transactional
    public void revoke(AppUserPrincipal admin, UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndOrganizationId(apiKeyId, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("API key not found"));
        apiKeyRepository.delete(apiKey);

        auditLogService.record(admin, AuditAction.API_KEY_REVOKED, AuditEntityType.API_KEY, apiKey.getId(),
                "Revoked API key \"" + apiKey.getName() + "\"");
    }

    private String generateRawKey() {
        byte[] bytes = new byte[RAW_KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKeySummary toSummary(ApiKey apiKey) {
        return new ApiKeySummary(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(), apiKey.getCreatedAt(),
                apiKey.getLastUsedAt());
    }
}

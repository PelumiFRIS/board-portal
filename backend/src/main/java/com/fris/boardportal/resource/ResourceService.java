package com.fris.boardportal.resource;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AuditLogService auditLogService;

    public ResourceService(ResourceRepository resourceRepository, AuditLogService auditLogService) {
        this.resourceRepository = resourceRepository;
        this.auditLogService = auditLogService;
    }

    public List<ResourceSummary> listForOrganization(AppUserPrincipal principal) {
        return resourceRepository.findByOrganizationIdOrderByTitleAsc(principal.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ResourceSummary create(AppUserPrincipal admin, ResourceCategory category, String title, String body) {
        Resource resource = Resource.create(admin.getOrganizationId(), category, title, body, admin.getUserId());
        resourceRepository.save(resource);

        auditLogService.record(admin, AuditAction.RESOURCE_CREATED, AuditEntityType.RESOURCE, resource.getId(),
                "Created resource \"" + resource.getTitle() + "\"");

        return toSummary(resource);
    }

    @Transactional
    public ResourceSummary update(AppUserPrincipal admin, UUID resourceId, ResourceCategory category, String title,
            String body) {
        Resource resource = findInOrg(admin, resourceId);
        if (category != null) {
            resource.setCategory(category);
        }
        if (title != null) {
            resource.setTitle(title);
        }
        if (body != null) {
            resource.setBody(body);
        }
        resource.setUpdatedAt(Instant.now());
        resourceRepository.save(resource);

        auditLogService.record(admin, AuditAction.RESOURCE_UPDATED, AuditEntityType.RESOURCE, resource.getId(),
                "Updated resource \"" + resource.getTitle() + "\"");

        return toSummary(resource);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID resourceId) {
        Resource resource = findInOrg(admin, resourceId);
        resourceRepository.delete(resource);

        auditLogService.record(admin, AuditAction.RESOURCE_DELETED, AuditEntityType.RESOURCE, resource.getId(),
                "Deleted resource \"" + resource.getTitle() + "\"");
    }

    private Resource findInOrg(AppUserPrincipal principal, UUID resourceId) {
        return resourceRepository.findByIdAndOrganizationId(resourceId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Resource not found"));
    }

    private ResourceSummary toSummary(Resource resource) {
        return new ResourceSummary(resource.getId(), resource.getCategory(), resource.getTitle(), resource.getBody(),
                resource.getCreatedAt(), resource.getUpdatedAt());
    }
}

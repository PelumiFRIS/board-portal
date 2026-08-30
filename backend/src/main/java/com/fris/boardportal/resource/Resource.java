package com.fris.boardportal.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Resource create(UUID organizationId, ResourceCategory category, String title, String body,
            UUID createdBy) {
        Resource resource = new Resource();
        resource.setId(UUID.randomUUID());
        resource.setOrganizationId(organizationId);
        resource.setCategory(category);
        resource.setTitle(title);
        resource.setBody(body);
        resource.setCreatedBy(createdBy);
        Instant now = Instant.now();
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return resource;
    }
}

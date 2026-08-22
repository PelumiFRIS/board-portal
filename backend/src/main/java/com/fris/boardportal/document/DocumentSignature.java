package com.fris.boardportal.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_signatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignature {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private Instant signedAt;

    public static DocumentSignature create(UUID documentId, UUID organizationId, UUID userId, String userName) {
        DocumentSignature signature = new DocumentSignature();
        signature.setId(UUID.randomUUID());
        signature.setDocumentId(documentId);
        signature.setOrganizationId(organizationId);
        signature.setUserId(userId);
        signature.setUserName(userName);
        signature.setSignedAt(Instant.now());
        return signature;
    }
}

package com.fris.boardportal.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, UUID> {

    List<DocumentSignature> findByDocumentIdOrderBySignedAtAsc(UUID documentId);

    boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId);

    long countByDocumentId(UUID documentId);
}

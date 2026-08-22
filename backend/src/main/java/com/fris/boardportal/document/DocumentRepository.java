package com.fris.boardportal.document;

import com.fris.boardportal.document.dto.DocumentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("""
            select new com.fris.boardportal.document.dto.DocumentSummary(
                d.id, d.title, d.description, d.category, d.fileName, d.contentType, d.fileSize, d.meetingId,
                d.createdAt, d.retentionUntil, 0L, false)
            from Document d
            where d.organizationId = :organizationId
            order by d.createdAt desc
            """)
    List<DocumentSummary> findSummariesByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            select new com.fris.boardportal.document.dto.DocumentSummary(
                d.id, d.title, d.description, d.category, d.fileName, d.contentType, d.fileSize, d.meetingId,
                d.createdAt, d.retentionUntil, 0L, false)
            from Document d
            where d.meetingId = :meetingId
            order by d.createdAt desc
            """)
    List<DocumentSummary> findSummariesByMeetingId(@Param("meetingId") UUID meetingId);

    Optional<Document> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

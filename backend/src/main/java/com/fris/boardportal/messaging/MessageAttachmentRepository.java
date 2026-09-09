package com.fris.boardportal.messaging;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageIdIn(List<UUID> messageIds);

    Optional<MessageAttachment> findByMessageId(UUID messageId);
}

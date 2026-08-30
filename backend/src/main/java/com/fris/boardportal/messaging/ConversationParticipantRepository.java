package com.fris.boardportal.messaging;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findByUserId(UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}

package com.fris.boardportal.messaging;

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
@Table(name = "conversation_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    public static ConversationParticipant create(UUID conversationId, UUID userId, Instant lastReadAt) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setId(UUID.randomUUID());
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participant.setLastReadAt(lastReadAt);
        return participant;
    }
}

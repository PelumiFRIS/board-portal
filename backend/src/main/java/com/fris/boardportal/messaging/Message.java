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
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Message create(UUID conversationId, UUID senderId, String senderName, String body) {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setBody(body);
        message.setCreatedAt(Instant.now());
        return message;
    }
}

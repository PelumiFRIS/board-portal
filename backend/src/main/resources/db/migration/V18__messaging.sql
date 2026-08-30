CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    is_group BOOLEAN NOT NULL DEFAULT false,
    title VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_organization_id ON conversations (organization_id);

CREATE TABLE conversation_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    last_read_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user_id ON conversation_participants (user_id);
CREATE INDEX idx_conversation_participants_conversation_id ON conversation_participants (conversation_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    sender_id UUID NOT NULL REFERENCES users (id),
    sender_name VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);

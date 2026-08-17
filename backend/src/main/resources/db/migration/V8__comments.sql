CREATE TABLE comments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    entity_type VARCHAR(30) NOT NULL,
    entity_id UUID NOT NULL,
    author_id UUID NOT NULL REFERENCES users (id),
    author_name VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_entity ON comments (entity_type, entity_id);
CREATE INDEX idx_comments_organization_id ON comments (organization_id);

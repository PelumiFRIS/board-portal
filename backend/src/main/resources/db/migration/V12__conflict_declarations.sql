CREATE TABLE conflict_declarations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    user_name VARCHAR(255) NOT NULL,
    declared_by UUID NOT NULL REFERENCES users (id),
    declared_by_name VARCHAR(255) NOT NULL,
    has_conflict BOOLEAN NOT NULL,
    details TEXT,
    declared_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_conflict_declarations_organization_id ON conflict_declarations (organization_id);
CREATE INDEX idx_conflict_declarations_user_id ON conflict_declarations (user_id);

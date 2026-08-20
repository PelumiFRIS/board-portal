ALTER TABLE users DROP COLUMN committees;

CREATE TABLE committees (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_committees_organization_id ON committees (organization_id);

CREATE TABLE committee_memberships (
    id UUID PRIMARY KEY,
    committee_id UUID NOT NULL REFERENCES committees (id),
    user_id UUID NOT NULL REFERENCES users (id),
    is_chair BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (committee_id, user_id)
);

CREATE INDEX idx_committee_memberships_committee_id ON committee_memberships (committee_id);
CREATE INDEX idx_committee_memberships_user_id ON committee_memberships (user_id);

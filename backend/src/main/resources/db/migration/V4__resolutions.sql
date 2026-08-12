CREATE TABLE resolutions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    meeting_id UUID NOT NULL REFERENCES meetings (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    outcome VARCHAR(20),
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    opened_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_resolutions_organization_id ON resolutions (organization_id);
CREATE INDEX idx_resolutions_meeting_id ON resolutions (meeting_id);

CREATE TABLE votes (
    id UUID PRIMARY KEY,
    resolution_id UUID NOT NULL REFERENCES resolutions (id),
    user_id UUID NOT NULL REFERENCES users (id),
    choice VARCHAR(20) NOT NULL,
    cast_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (resolution_id, user_id)
);

CREATE INDEX idx_votes_resolution_id ON votes (resolution_id);

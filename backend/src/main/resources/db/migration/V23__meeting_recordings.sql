CREATE TABLE meeting_recordings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    meeting_id UUID NOT NULL REFERENCES meetings (id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    recorded_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_recordings_meeting_id ON meeting_recordings (meeting_id);
CREATE INDEX idx_meeting_recordings_organization_id ON meeting_recordings (organization_id);

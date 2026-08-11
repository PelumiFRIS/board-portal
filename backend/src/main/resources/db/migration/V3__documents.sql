CREATE TABLE documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    meeting_id UUID REFERENCES meetings (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_organization_id ON documents (organization_id);
CREATE INDEX idx_documents_meeting_id ON documents (meeting_id);

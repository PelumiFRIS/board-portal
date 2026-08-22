ALTER TABLE documents ADD COLUMN retention_until DATE;

CREATE TABLE document_signatures (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    user_name VARCHAR(255) NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (document_id, user_id)
);
CREATE INDEX idx_document_signatures_document_id ON document_signatures (document_id);

ALTER TABLE documents ADD COLUMN root_document_id UUID;
ALTER TABLE documents ADD COLUMN version_number INT NOT NULL DEFAULT 1;
UPDATE documents SET root_document_id = id WHERE root_document_id IS NULL;
ALTER TABLE documents ALTER COLUMN root_document_id SET NOT NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_root_document FOREIGN KEY (root_document_id) REFERENCES documents (id);
CREATE INDEX idx_documents_root_document_id ON documents (root_document_id);

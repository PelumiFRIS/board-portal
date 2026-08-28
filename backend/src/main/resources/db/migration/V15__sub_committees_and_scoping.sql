ALTER TABLE committees ADD COLUMN parent_committee_id UUID REFERENCES committees (id);
CREATE INDEX idx_committees_parent_committee_id ON committees (parent_committee_id);

ALTER TABLE meetings ADD COLUMN committee_id UUID REFERENCES committees (id) ON DELETE SET NULL;
CREATE INDEX idx_meetings_committee_id ON meetings (committee_id);

ALTER TABLE documents ADD COLUMN committee_id UUID REFERENCES committees (id) ON DELETE SET NULL;
CREATE INDEX idx_documents_committee_id ON documents (committee_id);

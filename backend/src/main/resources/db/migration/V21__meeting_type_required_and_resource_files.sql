ALTER TABLE meetings ALTER COLUMN meeting_type TYPE VARCHAR(30);
UPDATE meetings SET meeting_type = 'BOARD' WHERE meeting_type IS NULL;
ALTER TABLE meetings ALTER COLUMN meeting_type SET NOT NULL;

UPDATE resources SET category = 'OTHER' WHERE category = 'ONBOARDING';

ALTER TABLE resources ADD COLUMN file_name VARCHAR(255);
ALTER TABLE resources ADD COLUMN content_type VARCHAR(255);
ALTER TABLE resources ADD COLUMN file_size BIGINT;
ALTER TABLE resources ADD COLUMN file_data BYTEA;

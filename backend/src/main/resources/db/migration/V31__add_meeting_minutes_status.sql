ALTER TABLE meetings ADD COLUMN minutes_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

UPDATE meetings SET minutes_status = 'APPROVED' WHERE status = 'COMPLETED';

ALTER TABLE meeting_recordings ADD COLUMN transcript_text TEXT;
ALTER TABLE meeting_recordings ADD COLUMN transcription_status VARCHAR(20) NOT NULL DEFAULT 'NONE';

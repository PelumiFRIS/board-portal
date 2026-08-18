CREATE TABLE user_photos (
    user_id UUID PRIMARY KEY REFERENCES users (id),
    content_type VARCHAR(100) NOT NULL,
    photo_data BYTEA NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

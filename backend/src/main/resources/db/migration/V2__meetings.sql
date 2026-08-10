CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    minutes_content TEXT,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_meetings_organization_id ON meetings (organization_id);

CREATE TABLE agenda_items (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES meetings (id),
    position INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_agenda_items_meeting_id ON agenda_items (meeting_id);

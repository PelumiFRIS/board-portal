CREATE TABLE action_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    meeting_id UUID NOT NULL REFERENCES meetings (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assignee_id UUID NOT NULL REFERENCES users (id),
    due_date DATE,
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_action_items_organization_id ON action_items (organization_id);
CREATE INDEX idx_action_items_meeting_id ON action_items (meeting_id);
CREATE INDEX idx_action_items_assignee_id ON action_items (assignee_id);

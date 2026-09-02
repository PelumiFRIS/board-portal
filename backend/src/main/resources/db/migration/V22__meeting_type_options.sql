CREATE TABLE meeting_type_options (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (organization_id, name)
);

CREATE INDEX idx_meeting_type_options_organization_id ON meeting_type_options (organization_id);

INSERT INTO meeting_type_options (id, organization_id, name)
SELECT gen_random_uuid(), o.id, label
FROM organizations o
CROSS JOIN (VALUES
    ('Annual General Meeting (AGM)'),
    ('Extra-Ordinary General Meeting (EGM)'),
    ('Court-Ordered Meeting (COM)'),
    ('Board Meeting'),
    ('Committee Meeting'),
    ('Executive/Management Meeting'),
    ('General Staff Meeting')
) AS labels(label);

ALTER TABLE meetings ADD COLUMN meeting_type_id UUID;

UPDATE meetings m
SET meeting_type_id = o.id
FROM meeting_type_options o
WHERE o.organization_id = m.organization_id
  AND o.name = CASE m.meeting_type
      WHEN 'AGM' THEN 'Annual General Meeting (AGM)'
      WHEN 'EGM' THEN 'Extra-Ordinary General Meeting (EGM)'
      WHEN 'COM' THEN 'Court-Ordered Meeting (COM)'
      WHEN 'BOARD' THEN 'Board Meeting'
      WHEN 'COMMITTEE' THEN 'Committee Meeting'
      WHEN 'EXECUTIVE_MANAGEMENT' THEN 'Executive/Management Meeting'
      WHEN 'GENERAL_STAFF' THEN 'General Staff Meeting'
  END;

ALTER TABLE meetings ALTER COLUMN meeting_type_id SET NOT NULL;
ALTER TABLE meetings ADD CONSTRAINT fk_meetings_meeting_type FOREIGN KEY (meeting_type_id) REFERENCES meeting_type_options (id);
ALTER TABLE meetings DROP COLUMN meeting_type;

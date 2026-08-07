-- Renames user-facing "Cadence" branding to "HirePilot" across every seeded email
-- template. Scoped to visible copy only -- no backend package/service names, no
-- database/column names, no localStorage keys are touched by this change anywhere
-- in the platform.
UPDATE notification_template
SET body_html = REPLACE(body_html, 'CADence AI Hiring Platform', 'HirePilot');

UPDATE notification_template
SET subject = REPLACE(REPLACE(REPLACE(subject,
    'on Cadence', 'on HirePilot'),
    'Welcome to Cadence', 'Welcome to HirePilot'),
    'Reset your Cadence password', 'Reset your HirePilot password');

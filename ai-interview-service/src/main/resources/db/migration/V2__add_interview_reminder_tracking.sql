-- Tracks whether a reminder email has already been sent for a not-yet-started
-- interview invite, so the reminder sweep job never sends the same reminder twice.
ALTER TABLE interview_session ADD COLUMN reminded_at TIMESTAMP NULL;

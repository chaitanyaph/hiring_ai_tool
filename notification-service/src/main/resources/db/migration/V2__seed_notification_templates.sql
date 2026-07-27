-- =====================================================================
-- Seeds the 21 templates the text spec names + INTERVIEW_RESCHEDULED
-- and INTERVIEW_CANCELLED (added -- justified by real Kafka events
-- with no literal-list template, see README "Architecture Decisions").
-- trigger_event = 'NONE' where research confirmed no real publisher
-- exists anywhere in the platform yet -- these rows exist so the
-- Figma's "Email templates" tab and the admin CRUD API have real
-- content to manage today, ready the moment an upstream event exists.
-- =====================================================================

INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('Recruiter Invitation', 'TEAM_INVITATION_CREATED', 'RECRUITER_INVITATION',
 'You''re invited to join {{company_name}} on Cadence',
 '<p>Hi {{recipient_name}},</p><p>You''ve been invited to join <b>{{company_name}}</b> as a {{role}}. <a href="{{invite_link}}">Accept invitation</a></p>',
 '{{recipient_name}}, {{company_name}}, {{role}}, {{invite_link}}', TRUE),

('Candidate Registration', 'USER_REGISTERED', 'CANDIDATE_REGISTRATION',
 'Welcome to Cadence, {{candidate_name}}!',
 '<p>Hi {{candidate_name}},</p><p>Your candidate account has been created. Start exploring open roles today.</p>',
 '{{candidate_name}}', TRUE),

('Welcome Email', 'USER_REGISTERED', 'WELCOME_EMAIL',
 'Welcome to Cadence, {{recipient_name}}!',
 '<p>Hi {{recipient_name}},</p><p>Your account at <b>{{company_name}}</b> is ready to go.</p>',
 '{{recipient_name}}, {{company_name}}', TRUE),

('Email Verification', 'USER_REGISTERED', 'EMAIL_VERIFICATION',
 'Verify your email address',
 '<p>Hi {{recipient_name}},</p><p>Please verify your email: <a href="{{verification_link}}">Verify email</a></p>',
 '{{recipient_name}}, {{verification_link}}', TRUE),

('Password Reset', 'PASSWORD_RESET_REQUESTED', 'PASSWORD_RESET',
 'Reset your Cadence password',
 '<p>Hi,</p><p>Click below to reset your password. This link expires soon.</p><p><a href="{{reset_link}}">Reset password</a></p>',
 '{{reset_link}}', TRUE),

('Application Received', 'APPLICATION_SUBMITTED', 'APPLICATION_RECEIVED',
 'We''ve received your application for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Thanks for applying to <b>{{job_title}}</b> at <b>{{company_name}}</b>. We''ll be in touch.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE),

('Resume Uploaded', 'RESUME_UPLOADED', 'RESUME_UPLOADED',
 'Your resume has been uploaded successfully',
 '<p>Hi {{candidate_name}},</p><p>Your resume has been uploaded and is now part of your profile.</p>',
 '{{candidate_name}}', TRUE),

('Resume Shortlisted', 'CANDIDATE_SHORTLISTED', 'RESUME_SHORTLISTED',
 'Great news about your application for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>You''ve been shortlisted for <b>{{job_title}}</b> at <b>{{company_name}}</b>. Next steps coming soon.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE),

('Resume Rejected', 'CANDIDATE_SHORTLISTED', 'RESUME_REJECTED',
 'Update on your application for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Thank you for your interest in <b>{{job_title}}</b> at <b>{{company_name}}</b>. We''ve decided to move forward with other candidates.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE),

('AI Interview Invitation', 'NONE', 'AI_INTERVIEW_INVITATION',
 'You''re invited to an AI interview for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Please complete your AI-led interview for <b>{{job_title}}</b> by {{expiry_date}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{expiry_date}}', TRUE),

('AI Interview Reminder', 'NONE', 'AI_INTERVIEW_REMINDER',
 'Reminder: your AI interview for {{job_title}} is expiring soon',
 '<p>Hi {{candidate_name}},</p><p>Your AI interview for <b>{{job_title}}</b> expires {{expiry_date}}. Complete it now.</p>',
 '{{candidate_name}}, {{job_title}}, {{expiry_date}}', TRUE),

('Coding Assessment Invitation', 'NONE', 'CODING_ASSESSMENT_INVITATION',
 'Coding assessment for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>You''ve been invited to a coding assessment for <b>{{job_title}}</b>.</p>',
 '{{candidate_name}}, {{job_title}}', TRUE),

('Coding Assessment Reminder', 'NONE', 'CODING_ASSESSMENT_REMINDER',
 'Reminder: complete your coding assessment for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Your coding assessment for <b>{{job_title}}</b> is still pending.</p>',
 '{{candidate_name}}, {{job_title}}', TRUE),

('Technical Interview Invitation', 'INTERVIEW_SCHEDULED', 'TECHNICAL_INTERVIEW_INVITATION',
 'You''re invited to interview for {{job_title}} at {{company_name}}',
 '<p>Hi {{candidate_name}},</p><p>Your technical interview for <b>{{job_title}}</b> is scheduled on {{interview_date}} at {{interview_time}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}, {{interview_date}}, {{interview_time}}', TRUE),

('Technical Interview Reminder', 'NONE', 'TECHNICAL_INTERVIEW_REMINDER',
 'Reminder: your interview for {{job_title}} is coming up',
 '<p>Hi {{candidate_name}},</p><p>Your technical interview for <b>{{job_title}}</b> starts soon: {{interview_date}} at {{interview_time}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{interview_date}}, {{interview_time}}', TRUE),

('HR Interview Invitation', 'INTERVIEW_SCHEDULED', 'HR_INTERVIEW_INVITATION',
 'HR round scheduled for {{job_title}} at {{company_name}}',
 '<p>Hi {{candidate_name}},</p><p>Your HR interview for <b>{{job_title}}</b> is scheduled on {{interview_date}} at {{interview_time}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}, {{interview_date}}, {{interview_time}}', TRUE),

('HR Interview Reminder', 'NONE', 'HR_INTERVIEW_REMINDER',
 'Reminder: your HR round for {{job_title}} is coming up',
 '<p>Hi {{candidate_name}},</p><p>Your HR interview for <b>{{job_title}}</b> starts soon: {{interview_date}} at {{interview_time}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{interview_date}}, {{interview_time}}', TRUE),

('Interview Rescheduled', 'INTERVIEW_RESCHEDULED', 'INTERVIEW_RESCHEDULED',
 'Your interview for {{job_title}} has been rescheduled',
 '<p>Hi {{candidate_name}},</p><p>Your interview has been moved to {{interview_date}} at {{interview_time}}.{{reschedule_reason}}</p>',
 '{{candidate_name}}, {{job_title}}, {{interview_date}}, {{interview_time}}, {{reschedule_reason}}', TRUE),

('Interview Cancelled', 'INTERVIEW_CANCELLED', 'INTERVIEW_CANCELLED',
 'Your interview for {{job_title}} has been cancelled',
 '<p>Hi {{candidate_name}},</p><p>Your interview for <b>{{job_title}}</b> has been cancelled.{{cancel_reason}}</p>',
 '{{candidate_name}}, {{job_title}}, {{cancel_reason}}', TRUE),

('Offer Letter', 'NONE', 'OFFER_LETTER',
 'Your offer from {{company_name}} — {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Congratulations! Please find your offer letter for <b>{{job_title}}</b> at <b>{{company_name}}</b> attached.</p>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE),

('Offer Accepted', 'OFFER_ACCEPTED', 'OFFER_ACCEPTED',
 'Offer accepted — {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>We''re thrilled you''ve accepted the offer for <b>{{job_title}}</b>. Welcome aboard!</p>',
 '{{candidate_name}}, {{job_title}}', TRUE),

('Offer Rejected', 'OFFER_REJECTED', 'OFFER_REJECTED',
 'Offer response received — {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>We''ve received your decision to decline the offer for <b>{{job_title}}</b>. Thank you for your time.</p>',
 '{{candidate_name}}, {{job_title}}', TRUE),

('Background Verification', 'NONE', 'BACKGROUND_VERIFICATION',
 'Background verification update for {{job_title}}',
 '<p>Hi {{candidate_name}},</p><p>Your background verification for <b>{{job_title}}</b> is {{verification_status}}.</p>',
 '{{candidate_name}}, {{job_title}}, {{verification_status}}', TRUE);

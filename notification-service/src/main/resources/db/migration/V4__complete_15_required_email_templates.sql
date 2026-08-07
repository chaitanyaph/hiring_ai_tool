-- =====================================================================
-- Completes the platform's 15 required candidate lifecycle emails.
-- Every template below gets the same 7-field bar: Company Name, Job
-- Title, Candidate Name, Current Stage, Next Step, Important
-- Instructions, Support Contact -- the Support Contact footer in
-- particular was previously absent from every template in the system.
--
-- Six of these categories did not exist at all before this migration:
-- AI_INTERVIEW_EXPIRED, AI_INTERVIEW_PASSED, AI_INTERVIEW_REJECTED,
-- CODING_ASSESSMENT_PASSED, CODING_ASSESSMENT_REJECTED, FINAL_REJECTION.
-- The rest already existed as stub <p>-tag templates and/or had
-- trigger_event = 'NONE' (no real publisher wired) -- both are fixed here.
-- =====================================================================

-- ---- 1. Application Submitted ----
UPDATE notification_template
SET subject = 'We''ve received your application for {{job_title}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Thanks for applying to <b>{{job_title}}</b> at <b>{{company_name}}</b>. We''ve received your application and it is now in our system.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Position</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{job_title}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Company</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{company_name}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Application Received</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Resume Screening</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Your resume will be automatically screened against this role''s requirements.</li>
<li>You''ll be notified by email as soon as a decision is made.</li>
<li>You can track your application status any time from your candidate dashboard.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Thank you for your interest!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'APPLICATION_RECEIVED';

-- ---- 2. Resume Shortlisted ----
UPDATE notification_template
SET subject = 'Great news about your application for {{job_title}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;"><strong>Congratulations!</strong> Your resume has been reviewed and you''ve been shortlisted for <b>{{job_title}}</b> at <b>{{company_name}}</b>.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Resume Shortlisted</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Interview Invitation</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>You''ll receive a separate email with your AI Interview invitation shortly.</li>
<li>No action is needed from you right now.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Best of luck!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'RESUME_SHORTLISTED';

-- ---- 3. Resume Rejected ----
UPDATE notification_template
SET subject = 'Update on your application for {{job_title}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Thank you for your interest in <b>{{job_title}}</b> at <b>{{company_name}}</b>. After reviewing your application, we''ve decided to move forward with other candidates at this time.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Resume Review Complete</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">None -- this application is closed</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:14px;color:#4b5563;line-height:1.6;margin:0 0 24px;">We encourage you to apply for other open roles at {{company_name}} that match your background.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;">We wish you the best in your job search.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'RESUME_REJECTED';

-- ---- 4. AI Interview Invitation: already wired + good HTML from V3, just add Support Contact ----
UPDATE notification_template
SET body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;"><strong>Congratulations!</strong></p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Based on your resume screening results, you have successfully qualified for the next stage of the recruitment process. You have been invited to attend the AI Interview.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Interview Details</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Position</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{job_title}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Company</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{company_name}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Interview Invitation</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Complete Your AI Interview</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Interview Type</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Voice AI Interview</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Valid From</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{valid_from}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Valid Until</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{valid_until}}</td></tr>
</table>
</td></tr>
</table>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0;"><tr><td align="center">
<a href="{{interview_link}}" style="display:inline-block;background-color:#372f84;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:14px 32px;border-radius:6px;">Start Your AI Interview</a>
</td></tr></table>
<p style="font-size:13px;color:#6b7280;text-align:center;margin:0 0 24px;">Or copy this link: <a href="{{interview_link}}" style="color:#372f84;">{{interview_link}}</a></p>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Complete the interview before the expiry time.</li>
<li>The interview can only be attempted once.</li>
<li>Ensure a stable internet connection.</li>
<li>Use microphone permission when prompted.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Best of Luck!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'AI_INTERVIEW_INVITATION';

-- ---- 5. AI Interview Reminder: wire real trigger + flesh out ----
UPDATE notification_template
SET trigger_event = 'AI_INTERVIEW_REMINDER_DUE',
    subject = 'Reminder: your AI interview for {{job_title}} is expiring soon',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{interview_link}}, {{expiry_date}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">This is a reminder that your AI Interview for <b>{{job_title}}</b> expires on <b>{{expiry_date}}</b>. Please complete it before then.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0;"><tr><td align="center">
<a href="{{interview_link}}" style="display:inline-block;background-color:#372f84;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:14px 32px;border-radius:6px;">Start Your AI Interview</a>
</td></tr></table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>The interview can only be attempted once, so set aside enough uninterrupted time.</li>
<li>Once the deadline passes, this invitation link will no longer work.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'AI_INTERVIEW_REMINDER';

-- ---- 6. AI Interview Expired (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('AI Interview Expired', 'AI_INTERVIEW_EXPIRED', 'AI_INTERVIEW_EXPIRED',
 'Your AI interview invitation for {{job_title}} has expired',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Your AI Interview invitation for <b>{{job_title}}</b> at <b>{{company_name}}</b> has expired because it was not completed in time.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Interview Expired</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Contact your recruiter to request a new invitation</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE);

-- ---- 7. AI Interview Passed (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('AI Interview Passed', 'AI_INTERVIEW_EVALUATED', 'AI_INTERVIEW_PASSED',
 'Congratulations! You passed the AI Interview for {{job_title}}',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;"><strong>Great news!</strong> You''ve successfully passed the AI Interview for <b>{{job_title}}</b> at <b>{{company_name}}</b>.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Overall Score</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{overall_score}}%</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Interview Passed</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Coding Assessment Invitation</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>You''ll receive a separate email with your Coding Assessment invitation shortly.</li>
<li>No action is needed from you right now.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Keep up the great work!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}, {{overall_score}}', TRUE);

-- ---- 8. AI Interview Rejected (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('AI Interview Rejected', 'AI_INTERVIEW_EVALUATED', 'AI_INTERVIEW_REJECTED',
 'Update on your AI Interview for {{job_title}}',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Thank you for completing the AI Interview for <b>{{job_title}}</b> at <b>{{company_name}}</b>. After careful review, we''ve decided not to move forward with your application at this time.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">AI Interview Complete</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">None -- this application is closed</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:15px;color:#1c1b29;margin:0;">We wish you the best in your job search.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE);

-- ---- 9. Coding Assessment Invitation: already wired + good HTML from V3, just add Support Contact ----
UPDATE notification_template
SET body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Great news -- you have progressed to the Coding Assessment stage for <b>{{job_title}}</b> at <b>{{company_name}}</b>.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Assessment Details</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Position</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{job_title}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Company</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{company_name}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Coding Assessment Invitation</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Complete Your Coding Assessment</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Assessment</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{assessment_name}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Duration</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{duration_minutes}} minutes</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Passing Marks</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{passing_score_percent}}%</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Expires</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{expiry_date}}</td></tr>
</table>
</td></tr>
</table>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0;"><tr><td align="center">
<a href="{{assessment_link}}" style="display:inline-block;background-color:#372f84;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:14px 32px;border-radius:6px;">Start Your Coding Assessment</a>
</td></tr></table>
<p style="font-size:13px;color:#6b7280;text-align:center;margin:0 0 24px;">Or copy this link: <a href="{{assessment_link}}" style="color:#372f84;">{{assessment_link}}</a></p>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Complete the assessment before the expiry time.</li>
<li>Ensure a stable internet connection throughout.</li>
<li>Do not refresh or close the browser tab once started.</li>
<li>Your code is auto-saved as you work through each question.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Best of Luck!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'CODING_ASSESSMENT_INVITATION';

-- ---- 10. Coding Assessment Reminder: wire real trigger + flesh out ----
UPDATE notification_template
SET trigger_event = 'CODING_ASSESSMENT_REMINDER_DUE',
    subject = 'Reminder: complete your coding assessment for {{job_title}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{assessment_link}}, {{expiry_date}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">This is a reminder that your Coding Assessment for <b>{{job_title}}</b> expires on <b>{{expiry_date}}</b>. Please complete it before then.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0;"><tr><td align="center">
<a href="{{assessment_link}}" style="display:inline-block;background-color:#372f84;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:14px 32px;border-radius:6px;">Start Your Coding Assessment</a>
</td></tr></table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Once the deadline passes, this invitation link will no longer work.</li>
<li>Your code is auto-saved as you work, so you can pace yourself within the time limit.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'CODING_ASSESSMENT_REMINDER';

-- ---- 11. Coding Assessment Passed (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('Coding Assessment Passed', 'CODING_ASSESSMENT_COMPLETED', 'CODING_ASSESSMENT_PASSED',
 'Congratulations! You passed the coding assessment for {{job_title}}',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;"><strong>Great news!</strong> You''ve successfully passed the coding assessment for <b>{{job_title}}</b> at <b>{{company_name}}</b>.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Score</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{score}}%</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Coding Assessment Passed</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Technical Interview</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>A recruiter will reach out separately to schedule your Technical Interview.</li>
<li>No action is needed from you right now.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Keep up the great work!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}, {{score}}', TRUE);

-- ---- 12. Coding Assessment Rejected (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('Coding Assessment Rejected', 'CODING_ASSESSMENT_COMPLETED', 'CODING_ASSESSMENT_REJECTED',
 'Update on your coding assessment for {{job_title}}',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Thank you for completing the coding assessment for <b>{{job_title}}</b> at <b>{{company_name}}</b>. After review, your score did not meet the passing threshold for this role.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Score</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{score}}%</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Coding Assessment Complete</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">None -- this application is closed</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:15px;color:#1c1b29;margin:0;">We wish you the best in your job search.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}, {{score}}', TRUE);

-- ---- 13. Technical Interview Invitation: flesh out + honest round_type wording ----
UPDATE notification_template
SET subject = 'You''re invited to interview for {{job_title}} at {{company_name}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}, {{round_type}}, {{interview_date}}, {{interview_time}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Congratulations on qualifying for the next round! Your {{round_type}} interview for <b>{{job_title}}</b> at <b>{{company_name}}</b> has been scheduled.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Position</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{job_title}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Company</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{company_name}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Technical Interview Scheduled</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Date</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{interview_date}}</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Time</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">{{interview_time}}</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Join a few minutes early to allow time for any technical setup.</li>
<li>Your recruiter will follow up with joining details if needed.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Best of luck!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'TECHNICAL_INTERVIEW_INVITATION';

-- ---- 14. Offer Letter: wire real trigger (offer-management-service''s OfferSentEvent) + flesh out ----
UPDATE notification_template
SET trigger_event = 'OFFER_SENT',
    subject = 'Your offer from {{company_name}} — {{job_title}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;"><strong>Congratulations!</strong> We''re delighted to offer you the role of <b>{{job_title}}</b> at <b>{{company_name}}</b>.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Offer Sent</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Review and Respond to Your Offer</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:13px;text-transform:uppercase;letter-spacing:0.04em;color:#6b7280;margin:0 0 12px;font-weight:600;">Important Instructions</p>
<ul style="font-size:14px;color:#4b5563;line-height:1.7;margin:0 0 24px;padding-left:20px;">
<li>Full offer details are available in your candidate dashboard.</li>
<li>Please review and respond by the deadline noted in your dashboard.</li>
</ul>
<p style="font-size:15px;color:#1c1b29;margin:0;">Welcome to the team!</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>'
WHERE category = 'OFFER_LETTER';

-- ---- 15. Final Rejection (new) ----
INSERT INTO notification_template (name, trigger_event, category, subject, body_html, variables_hint, active) VALUES
('Final Rejection', 'APPLICATION_REJECTED', 'FINAL_REJECTION',
 'Update on your application for {{job_title}}',
 '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
<style>@media only screen and (max-width:620px){.email-padding{padding:20px !important;}}</style>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background-color:#372f84;padding:24px 32px;text-align:center;"><span style="color:#ffffff;font-size:20px;font-weight:700;">CADence AI Hiring Platform</span></td></tr>
<tr><td class="email-padding" style="padding:32px;">
<p style="font-size:16px;color:#1c1b29;margin:0 0 16px;">Dear {{candidate_name}},</p>
<p style="font-size:15px;color:#4b5563;margin:0 0 16px;line-height:1.5;">Thank you for your time and interest in <b>{{job_title}}</b> at <b>{{company_name}}</b>. After careful consideration, we''ve decided to move forward with other candidates.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8f8fb;border-radius:6px;margin:24px 0;">
<tr><td style="padding:20px 24px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0">
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;width:40%;">Current Stage</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">Application Closed</td></tr>
<tr><td style="padding:6px 0;font-size:14px;color:#6b7280;">Next Step</td><td style="padding:6px 0;font-size:14px;color:#1c1b29;font-weight:600;">None -- this application is closed</td></tr>
</table>
</td></tr>
</table>
<p style="font-size:14px;color:#4b5563;line-height:1.6;margin:0 0 24px;">We encourage you to apply for other open roles at {{company_name}} that match your background.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;">We wish you the best in your job search.</p>
<p style="font-size:15px;color:#1c1b29;margin:0;font-weight:600;">CADence AI Hiring Platform</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;"><tr><td style="border-top:1px solid #e5e7eb;padding-top:16px;text-align:center;">
<p style="font-size:12px;color:#9ca3af;margin:0;">Need help? Contact us at <a href="mailto:support@cadence-hiring.com" style="color:#372f84;">support@cadence-hiring.com</a></p>
</td></tr></table>
</td></tr>
</table>
</div>',
 '{{candidate_name}}, {{job_title}}, {{company_name}}', TRUE);

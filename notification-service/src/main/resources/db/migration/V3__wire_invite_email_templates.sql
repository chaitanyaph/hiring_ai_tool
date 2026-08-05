-- =====================================================================
-- The AI Interview Invitation and Coding Assessment Invitation
-- templates were seeded in V2 with trigger_event = 'NONE' -- no
-- publisher existed for either at the time. Both are now wired to
-- real events (ai-interview.interview.invited / assessment.coding.invited,
-- published the moment InterviewSessionServiceImpl.inviteCandidate() /
-- CandidateAssessmentServiceImpl.inviteCandidate() actually create the
-- invite row with its real link/validity window), and rewritten as
-- responsive HTML instead of the bare unstyled <p> tags they had.
-- =====================================================================

UPDATE notification_template
SET trigger_event = 'AI_INTERVIEW_INVITED',
    subject = 'Congratulations! You have been shortlisted for the AI Interview',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}, {{valid_from}}, {{valid_until}}, {{interview_link}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
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
</td></tr>
</table>
</div>'
WHERE category = 'AI_INTERVIEW_INVITATION';

UPDATE notification_template
SET trigger_event = 'CODING_ASSESSMENT_INVITED',
    subject = 'Coding Assessment Invitation - {{job_title}} at {{company_name}}',
    variables_hint = '{{candidate_name}}, {{job_title}}, {{company_name}}, {{assessment_name}}, {{duration_minutes}}, {{passing_score_percent}}, {{assessment_link}}, {{expiry_date}}',
    body_html = '<div style="background-color:#f4f4f7;padding:24px 0;font-family:Arial,Helvetica,sans-serif;">
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
</td></tr>
</table>
</div>'
WHERE category = 'CODING_ASSESSMENT_INVITATION';

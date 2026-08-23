-- V13__demo_seed_data.sql
-- Seeds a dedicated demo user and realistic demo tickets for the
-- "Try Demo" one-click login feature.
--
-- The demo user belongs to the existing Acme Corp tenant (aaaa...).
-- Tickets span all categories, priorities, and statuses to showcase
-- the full triage pipeline, state machine, and analytics dashboard.
--
-- Idempotent: uses ON CONFLICT DO NOTHING so re-running is safe.

-- ─── Demo User ─────────────────────────────────────────────────────
-- BCrypt hash for 'demo123' (cost 10):
-- $2a$10$dXJ3SW6G7P50lGmMQgel6uVktDQPLJ0Xt2Yeb5w3VBzgHPKtOe.FO

INSERT INTO users (id, tenant_id, email, password_hash, name) VALUES
    ('dddd0000-0000-0000-0000-000000000001',
     'aaaa0000-0000-0000-0000-000000000001',
     'demo@nexus.dev',
     '$2a$10$dXJ3SW6G7P50lGmMQgel6uVktDQPLJ0Xt2Yeb5w3VBzgHPKtOe.FO',
     'Demo Agent')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role) VALUES
    ('dddd0000-0000-0000-0000-000000000001', 'ROLE_AGENT')
ON CONFLICT (user_id, role) DO NOTHING;

-- Also give demo user ADMIN so they can see analytics dashboard
INSERT INTO user_roles (user_id, role) VALUES
    ('dddd0000-0000-0000-0000-000000000001', 'ROLE_ADMIN')
ON CONFLICT (user_id, role) DO NOTHING;

-- ─── Demo Tickets ──────────────────────────────────────────────────
-- 10 tickets across all categories and statuses for a realistic demo.
-- Deterministic UUIDs for reproducibility.

INSERT INTO tickets (id, tenant_id, subject, description, status, priority, category, confidence_score, ai_response, created_at) VALUES

-- 1. NEW ticket — password reset (unprocessed)
('eeee0000-0000-0000-0000-000000000001',
 'aaaa0000-0000-0000-0000-000000000001',
 'Cannot reset my password',
 'I have been trying to reset my password for the last hour but the reset email never arrives. I have checked spam folders. My account email is john.doe@company.com. This is urgent as I cannot access my dashboard for client deliverables due tomorrow.',
 'NEW', NULL, NULL, NULL, NULL,
 now() - interval '2 hours'),

-- 2. CLASSIFIED ticket — billing dispute
('eeee0000-0000-0000-0000-000000000002',
 'aaaa0000-0000-0000-0000-000000000001',
 'Unexpected charge on my invoice',
 'I noticed a $49.99 charge on my latest invoice that I do not recognize. I am on the Starter plan which should be $19.99/month. I did not authorize any upgrades. Please investigate and reverse this charge immediately.',
 'CLASSIFIED', 'HIGH', 'BILLING', 0.72, NULL,
 now() - interval '1 day'),

-- 3. AI_DRAFTED — API integration issue
('eeee0000-0000-0000-0000-000000000003',
 'aaaa0000-0000-0000-0000-000000000001',
 'API returns 429 Too Many Requests',
 'Our integration started getting 429 errors this morning around 9 AM EST. We are on the Professional plan and our request volume has not changed. We are making approximately 500 requests per minute which should be well within our limit of 1000/min.',
 'AI_DRAFTED', 'HIGH', 'TECHNICAL', 0.85,
 'Thank you for reporting this issue. I can see that your account is on the Professional plan with a 1000 req/min limit. The 429 errors you are experiencing may be caused by a recent rate limiting configuration update. I have escalated this to our engineering team for immediate investigation. In the meantime, you can implement exponential backoff in your integration to handle transient rate limit responses gracefully.',
 now() - interval '5 hours'),

-- 4. AUTO_RESOLVED — 2FA setup help (high confidence)
('eeee0000-0000-0000-0000-000000000004',
 'aaaa0000-0000-0000-0000-000000000001',
 'How do I enable two-factor authentication?',
 'Hi, I would like to set up 2FA on my account for better security. Can you walk me through the steps?',
 'AUTO_RESOLVED', 'LOW', 'ACCOUNT', 0.95,
 'Great question! To enable Two-Factor Authentication: 1) Go to Settings > Security > Two-Factor Authentication 2) Click "Enable" 3) Choose your preferred method: authenticator app (recommended) or SMS 4) Scan the QR code with your authenticator app (Google Authenticator or Authy) 5) Enter the verification code to confirm. Important: Save your backup codes in a safe place — you will need them if you lose access to your authenticator. Let me know if you need further help!',
 now() - interval '3 hours'),

-- 5. ESCALATED — data export not working
('eeee0000-0000-0000-0000-000000000005',
 'aaaa0000-0000-0000-0000-000000000001',
 'CSV export generates empty file',
 'When I try to export my ticket data as CSV from the analytics dashboard, it downloads a file but the file is completely empty (0 bytes). I have tried in Chrome and Firefox with the same result. I need this data for our quarterly review meeting on Friday.',
 'ESCALATED', 'MEDIUM', 'TECHNICAL', 0.45,
 'I apologize for the inconvenience. I was not able to determine the root cause with high confidence. This has been escalated to a human agent for investigation.',
 now() - interval '8 hours'),

-- 6. IN_PROGRESS — subscription cancellation
('eeee0000-0000-0000-0000-000000000006',
 'aaaa0000-0000-0000-0000-000000000001',
 'Cancel my subscription effective end of month',
 'Please cancel my Professional subscription at the end of the current billing cycle (August 31). I do not want to be charged for September. Please confirm the cancellation and any remaining balance.',
 'IN_PROGRESS', 'MEDIUM', 'BILLING', 0.78,
 'I understand you would like to cancel your Professional subscription. I can confirm that your cancellation will take effect at the end of your current billing cycle (August 31). You will retain full access until then and will not be charged for September.',
 now() - interval '12 hours'),

-- 7. RESOLVED — dashboard analytics question
('eeee0000-0000-0000-0000-000000000007',
 'aaaa0000-0000-0000-0000-000000000001',
 'How to filter analytics by date range?',
 'I want to see my support metrics for just the last 7 days but I cannot figure out how to change the date range on the dashboard. The default seems to show all time data.',
 'RESOLVED', 'LOW', 'GENERAL', 0.92,
 'You can filter analytics by date range using the date picker in the top-right corner of the Analytics dashboard. Click on it and select "Last 7 Days" from the preset options, or set a custom range. The dashboard will update immediately. You can also export the filtered data using the Export button.',
 now() - interval '2 days'),

-- 8. CLOSED — refund processed
('eeee0000-0000-0000-0000-000000000008',
 'aaaa0000-0000-0000-0000-000000000001',
 'Refund for duplicate charge in July',
 'I was charged twice on July 15 for my monthly subscription. Order IDs: ORD-7891 and ORD-7892. Both are for $29.99. Please refund the duplicate charge.',
 'CLOSED', 'HIGH', 'BILLING', 0.88,
 'I have verified the duplicate charge and processed a refund of $29.99 for order ORD-7892. The refund should appear on your statement within 5-7 business days. I apologize for the inconvenience.',
 now() - interval '5 days'),

-- 9. NEW — feature request (CRITICAL from customer perspective)
('eeee0000-0000-0000-0000-000000000009',
 'aaaa0000-0000-0000-0000-000000000001',
 'System completely down — cannot access anything',
 'Our entire team of 50 agents cannot log into the platform. We are getting a 503 Service Unavailable error on every page. This is a production-critical issue as we have over 200 customer tickets waiting. Started approximately 15 minutes ago.',
 'NEW', NULL, NULL, NULL, NULL,
 now() - interval '15 minutes'),

-- 10. AUTO_RESOLVED — simple account question
('eeee0000-0000-0000-0000-000000000010',
 'aaaa0000-0000-0000-0000-000000000001',
 'Where can I update my billing email address?',
 'I need to change the email address where invoices are sent. Currently they go to accounting@oldcompany.com but we need them at finance@newcompany.com.',
 'AUTO_RESOLVED', 'LOW', 'ACCOUNT', 0.93,
 'You can update your billing email address by going to Settings > Billing > Billing Contact. Click "Edit" next to the email field, enter your new email (finance@newcompany.com), and click Save. A confirmation email will be sent to the new address. Future invoices will be sent there starting with your next billing cycle.',
 now() - interval '1 day')

ON CONFLICT (id) DO NOTHING;

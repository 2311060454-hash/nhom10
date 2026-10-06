ALTER TABLE users ADD COLUMN marketing_consent_at DATETIME(6) NULL;
UPDATE users SET marketing_consent_at=CURRENT_TIMESTAMP(6) WHERE marketing_consent=1;

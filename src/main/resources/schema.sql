-- MySQL script to add missing column in users table
-- We use a simple ALTER statement. If it fails because the column exists, it won't crash the whole startup unless configured strictly.
-- MySQL 8.0.29+ alternative: ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATE DEFAULT (CURDATE());

ALTER TABLE users ADD COLUMN created_at DATE DEFAULT (CURDATE());

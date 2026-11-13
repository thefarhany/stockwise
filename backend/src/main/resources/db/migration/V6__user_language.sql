-- Bahasa tampilan per akun. Akun lama memakai English (tampilan sebelum ada pilihan bahasa).
ALTER TABLE app_user
    ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'EN',
    ADD CONSTRAINT ck_app_user_language CHECK (language IN ('EN', 'ID'));

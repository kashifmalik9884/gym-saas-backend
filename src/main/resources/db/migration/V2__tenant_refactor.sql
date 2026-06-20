SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS gyms (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    owner_name VARCHAR(120) NOT NULL,
    owner_email VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    access_start_date DATE NOT NULL,
    access_end_date DATE NOT NULL,
    last_renewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gyms_slug (slug),
    UNIQUE KEY uk_gyms_owner_email (owner_email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS gym_renewal_logs (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                gym_id BIGINT NOT NULL,
                                                previous_end_date DATE NOT NULL,
                                                new_end_date DATE NOT NULL,
                                                renewed_by_email VARCHAR(150) NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_gym_renewal_logs_gym_id (gym_id),
    CONSTRAINT fk_gym_renewal_logs_gym
    FOREIGN KEY (gym_id) REFERENCES gyms(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN gym_id BIGINT NULL AFTER id;

ALTER TABLE members
    ADD COLUMN gym_id BIGINT NULL AFTER id;

ALTER TABLE attendance
    ADD COLUMN gym_id BIGINT NULL AFTER id,
    ADD COLUMN member_id BIGINT NULL AFTER gym_id;

INSERT INTO gyms (
    name, slug, owner_name, owner_email, status,
    access_start_date, access_end_date, last_renewed_at, created_at, updated_at
)
SELECT
    'Default Gym',
    'default-gym',
    COALESCE((SELECT full_name FROM users ORDER BY id ASC LIMIT 1), 'Admin'),
    COALESCE((SELECT email FROM users ORDER BY id ASC LIMIT 1), 'admin@example.com'),
    'active',
    CURDATE(),
    DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
    NULL,
    NOW(),
    NOW()
    WHERE NOT EXISTS (SELECT 1 FROM gyms);

UPDATE users
SET gym_id = (SELECT id FROM gyms ORDER BY id ASC LIMIT 1)
WHERE gym_id IS NULL;

UPDATE members
SET gym_id = (SELECT id FROM gyms ORDER BY id ASC LIMIT 1)
WHERE gym_id IS NULL;

UPDATE attendance
SET gym_id = (SELECT id FROM gyms ORDER BY id ASC LIMIT 1)
WHERE gym_id IS NULL;

UPDATE attendance a
    JOIN members m
ON m.name = a.member_name
    AND m.gym_id = a.gym_id
    SET a.member_id = m.id
WHERE a.member_id IS NULL;

CREATE INDEX idx_users_gym_id ON users(gym_id);
CREATE INDEX idx_members_gym_id ON members(gym_id);
CREATE INDEX idx_attendance_gym_id ON attendance(gym_id);
CREATE INDEX idx_attendance_member_id ON attendance(member_id);
CREATE INDEX idx_attendance_gym_date ON attendance(gym_id, date);

ALTER TABLE users
    ADD CONSTRAINT fk_users_gym
        FOREIGN KEY (gym_id) REFERENCES gyms(id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE members
    ADD CONSTRAINT fk_members_gym
        FOREIGN KEY (gym_id) REFERENCES gyms(id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_gym
        FOREIGN KEY (gym_id) REFERENCES gyms(id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_member
        FOREIGN KEY (member_id) REFERENCES members(id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE users
    MODIFY COLUMN gym_id BIGINT NOT NULL;

ALTER TABLE members
    MODIFY COLUMN gym_id BIGINT NOT NULL;

ALTER TABLE attendance
    MODIFY COLUMN gym_id BIGINT NOT NULL,
    MODIFY COLUMN member_id BIGINT NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
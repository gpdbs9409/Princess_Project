-- =========================================================
-- Incremental migration for the admin site feature (role/cohort on users,
-- weekly_refunds table). Safe to run once against the EXISTING live
-- database - unlike schema.sql, this does NOT drop anything.
--
-- Run this manually against the production DB (schema.sql is the
-- from-scratch reference and starts with DROP DATABASE - do not re-run it
-- against a database that already has real data).
-- =========================================================

USE princess_project;

ALTER TABLE users
    ADD COLUMN role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' AFTER profile_image_url,
    ADD COLUMN cohort VARCHAR(20) NULL AFTER role;

CREATE TABLE IF NOT EXISTS weekly_refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    week_start DATE NOT NULL,

    paid BOOLEAN NOT NULL DEFAULT FALSE,
    amount DECIMAL(10, 2) NULL,
    paid_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_weekly_refunds_user_week UNIQUE (user_id, week_start),
    CONSTRAINT fk_weekly_refunds_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS weekly_mvp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    cohort VARCHAR(20) NOT NULL,
    week_start DATE NOT NULL,

    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_weekly_mvp_cohort_week UNIQUE (cohort, week_start),
    CONSTRAINT fk_weekly_mvp_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS score_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    week_start DATE NULL,
    stat_type_code VARCHAR(20) NULL,

    points DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(500) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_score_adjustments_user (user_id),
    CONSTRAINT fk_score_adjustments_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

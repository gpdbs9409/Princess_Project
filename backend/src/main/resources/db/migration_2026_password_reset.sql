-- Password reset / SMTP migration (2026-08)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- "비밀번호 찾기"를 위한 이메일 컬럼 + 1회용 재설정 토큰 테이블.

USE princess_project;

-- MySQL은 ADD COLUMN/ADD CONSTRAINT에 IF NOT EXISTS를 지원하지 않는다 (MariaDB 전용 문법).
-- 이 스크립트를 이미 한 번 실행했다면 아래 두 ALTER TABLE에서 각각
-- "Duplicate column name 'email'" / "Duplicate key name 'uk_users_email'" 에러가 나는데,
-- 그 경우 이미 적용된 것이니 그 두 줄만 건너뛰고 CREATE TABLE부터 이어서 실행하면 된다.
ALTER TABLE users
    ADD COLUMN email VARCHAR(255) NULL AFTER cohort;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    token VARCHAR(128) NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    used BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_password_reset_tokens_token
        UNIQUE (token),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

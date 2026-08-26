-- Email verification-before-signup (2026-08-26)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- 주의: 아래 USE 문은 기존 migration 파일들과 형식을 맞추려고 남겨뒀지만, Railway MySQL
-- 플러그인의 실제 DB 이름은 princess_project가 아니라 railway다 (2026-08 prod->dev 데이터
-- 이전 작업 중 확인됨). 이 파일을 그대로 mysql < 로 실행하면 USE 단계에서 DB가 없다는 에러가
-- 날 수 있으니, 그럴 경우 USE 줄만 지우거나 "USE railway;"로 바꿔서(혹은 mysql 접속 시
-- 이미 DB를 지정했다면 USE 줄 자체를 건너뛰고) 이어서 실행하면 된다.
USE princess_project;

CREATE TABLE IF NOT EXISTS email_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    email VARCHAR(255) NOT NULL,

    code VARCHAR(6) NOT NULL,

    verified_token VARCHAR(128) NULL,

    verified BOOLEAN NOT NULL DEFAULT FALSE,

    expires_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_email_verifications_email (
        email
    ),

    CONSTRAINT uk_email_verifications_verified_token
        UNIQUE (verified_token)
) ENGINE = InnoDB;

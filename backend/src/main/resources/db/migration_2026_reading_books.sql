-- Reading books migration (2026-09)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- "독서 인증 시 이전 페이지 이어쓰기" + "완독 후 마이페이지에서 새 책 등록 -> 자동 활성화"
-- 기능을 위한 테이블. 병렬독서는 지원하지 않는다 - 사용자당 status=ACTIVE인 행은 항상 최대
-- 1개만 존재하도록 애플리케이션(ReadingBookService)이 보장한다 (MySQL은 부분 유니크 인덱스를
-- 지원하지 않아 DB 제약으로는 강제하지 않는다).

USE princess_project;

CREATE TABLE IF NOT EXISTS reading_books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATE NOT NULL,
    completed_at DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reading_books_user_status (user_id, status),
    CONSTRAINT fk_reading_books_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reading_books_project FOREIGN KEY (project_id) REFERENCES user_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

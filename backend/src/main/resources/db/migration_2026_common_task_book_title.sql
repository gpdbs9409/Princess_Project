-- Common task book title migration (2026-08)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- 독서 공통 과제에 책 제목(선택 입력) 컬럼을 추가하기 위한 마이그레이션
-- (2026-08-26 QA 반영: 독서 기록에 책 제목도 같이 남기면 좋겠다는 요청).

USE princess_project;

-- MySQL은 ADD COLUMN에 IF NOT EXISTS를 지원하지 않는다 (MariaDB 전용 문법).
-- 이미 한 번 실행했다면 "Duplicate column name 'book_title'" 에러가 나는데, 그 경우 이미
-- 적용된 것이니 그냥 넘어가면 된다.
ALTER TABLE common_task_records
    ADD COLUMN book_title VARCHAR(200) NULL AFTER record_date;

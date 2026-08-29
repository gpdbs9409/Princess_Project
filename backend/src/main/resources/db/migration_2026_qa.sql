-- QA 반영 마이그레이션 (2026-08)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.

-- 1) "나의 외적 추구미" 필드 추가
--    이미 실행했다면 Duplicate column 에러가 나므로 재실행하지 말 것.
ALTER TABLE user_projects
    ADD COLUMN goal_appearance VARCHAR(500) NULL AFTER goal_human;

-- 2) 인스타그램 핸들 (선택 입력)
--    이미 실행했다면 Duplicate column 에러가 나므로 재실행하지 말 것.
ALTER TABLE users
    ADD COLUMN instagram VARCHAR(30) NULL AFTER profile_image_url;

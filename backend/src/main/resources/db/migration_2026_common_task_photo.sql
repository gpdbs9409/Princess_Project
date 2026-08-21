-- Common task photo verification migration (2026-08)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- 독서/공부 공통 과제에 타 습관 카드(MissionCard)와 동일한 사진 인증란을 추가하기 위한 컬럼
-- (2026-08-21 정책: 사진인증 정책 변경). 주간회고에는 쓰이지 않지만, 이 테이블은 3개 타입이
-- 한 테이블을 공유하는 구조라 다른 타입 전용 컬럼들(start_page 등)과 마찬가지로 nullable로 둔다.

USE princess_project;

-- MySQL은 ADD COLUMN에 IF NOT EXISTS를 지원하지 않는다 (MariaDB 전용 문법).
-- 이미 한 번 실행했다면 "Duplicate column name 'photo_url'" 에러가 나는데, 그 경우 이미
-- 적용된 것이니 그냥 넘어가면 된다.
ALTER TABLE common_task_records
    ADD COLUMN photo_url VARCHAR(500) NULL AFTER retro_next_week_plan;

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

-- 3) WEEKLY 미션 스펙아웃 (2026-08-29)
--    매주 미션은 채점·환급 어디에도 반영되지 않는다. 이미 만들어진 WEEKLY 미션이 있으면
--    화면에서 보이지도 채점되지도 않는 유령이 되므로 DAILY로 전환한다.
--    (목표값은 그대로 남으므로 "주 3회"였던 미션은 "하루 3회"가 된다 - 참가자에게 안내 필요)
UPDATE user_missions SET mission_type = 'DAILY' WHERE mission_type = 'WEEKLY';
UPDATE mission_definitions SET mission_type = 'DAILY' WHERE mission_type = 'WEEKLY';

-- 운영자가 AI 오판 여부를 최종 검토해 인증을 무효화할 수 있도록 한다.
-- 무효화된 기록은 보존되지만 점수와 환급 계산에서는 제외된다.
ALTER TABLE daily_records
    ADD COLUMN admin_invalidated BOOLEAN NOT NULL DEFAULT FALSE AFTER ai_verified;

ALTER TABLE daily_common_task_records
    ADD COLUMN admin_invalidated BOOLEAN NOT NULL DEFAULT FALSE AFTER ai_verified;

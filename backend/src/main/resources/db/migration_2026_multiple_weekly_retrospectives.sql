-- 주간회고를 저장할 때마다 새 카드로 누적할 수 있도록 기존 일자별 유일 제약을 제거한다.
-- 독서/공부의 하루 한 건 정책은 CommonTaskService의 upsert 로직으로 계속 유지된다.
ALTER TABLE common_task_records
    DROP INDEX uk_common_task_records_user_type_date;

CREATE INDEX idx_common_task_records_user_type_date
    ON common_task_records (user_id, task_type, record_date);

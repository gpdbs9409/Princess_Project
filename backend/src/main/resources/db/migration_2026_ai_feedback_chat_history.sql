-- 레오집사 피드백을 하루 한 건 덮어쓰기에서 실제 채팅처럼 여러 건 누적으로 전환한다.
-- dev/prod DB마다 한 번만 실행한다.
ALTER TABLE ai_feedbacks
    DROP INDEX uk_ai_feedbacks_project_date_type,
    ADD INDEX idx_ai_feedbacks_conversation
        (user_id, project_id, feedback_date, feedback_type, created_at);

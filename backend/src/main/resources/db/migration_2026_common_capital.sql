-- 최초 설정의 공통자본 목표와 새 공부 인증 필드를 추가한다.
ALTER TABLE user_projects
    ADD COLUMN common_reading_book_title VARCHAR(200) NULL AFTER goal_ending,
    ADD COLUMN common_reading_total_pages INT NULL AFTER common_reading_book_title,
    ADD COLUMN common_study_youtube_topic VARCHAR(300) NULL AFTER common_reading_total_pages;

ALTER TABLE daily_common_task_records
    ADD COLUMN study_youtube_url VARCHAR(1000) NULL AFTER study_completed_amount,
    ADD COLUMN study_takeaway VARCHAR(1000) NULL AFTER study_youtube_url;

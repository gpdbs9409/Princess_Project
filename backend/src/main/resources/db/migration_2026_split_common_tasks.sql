-- 독서/공부(일일·점수/환급 대상)와 주간 회고(선택·점수/환급 제외)를 물리적으로 분리한다.
-- 앱 배포 전에 기존 DB에 1회 실행한다.

CREATE TABLE weekly_retrospectives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    week_start DATE NOT NULL,
    retro_daily_life TEXT NULL,
    retro_week_review TEXT NULL,
    retro_next_week_plan TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_weekly_retrospectives_user_week (user_id, week_start),
    INDEX idx_weekly_retrospectives_project (project_id),
    CONSTRAINT fk_weekly_retrospectives_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_weekly_retrospectives_project FOREIGN KEY (project_id) REFERENCES user_projects(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- 과거 중복 회고가 있으면 같은 사용자/주 중 가장 최신 id만 옮긴다.
INSERT INTO weekly_retrospectives (
    user_id, project_id, week_start, retro_daily_life, retro_week_review,
    retro_next_week_plan, created_at, updated_at
)
SELECT c.user_id, c.project_id, c.record_date, c.retro_daily_life, c.retro_week_review,
       c.retro_next_week_plan, c.created_at, c.updated_at
FROM common_task_records c
JOIN (
    SELECT user_id, record_date, MAX(id) AS id
    FROM common_task_records
    WHERE task_type = 'WEEKLY_RETROSPECTIVE'
    GROUP BY user_id, record_date
) latest ON latest.id = c.id;

DELETE FROM common_task_records WHERE task_type = 'WEEKLY_RETROSPECTIVE';

-- unique 제약이 없던 환경의 일일 중복은 최신 행만 유지한다.
DELETE older
FROM common_task_records older
JOIN common_task_records newer
  ON older.user_id = newer.user_id
 AND older.task_type = newer.task_type
 AND older.record_date = newer.record_date
 AND older.id < newer.id;

RENAME TABLE common_task_records TO daily_common_task_records;
ALTER TABLE daily_common_task_records
    MODIFY task_type ENUM('READING', 'STUDY') NOT NULL;
ALTER TABLE daily_common_task_records
    ADD CONSTRAINT uk_daily_common_tasks_user_type_date UNIQUE (user_id, task_type, record_date);

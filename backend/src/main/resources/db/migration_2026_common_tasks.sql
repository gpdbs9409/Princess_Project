-- Common tasks migration (2026-08)
-- 운영 DB에 이미 데이터가 있는 상태에서 안전하게 돌릴 수 있는 증분 스크립트다.
-- schema.sql은 DROP DATABASE로 시작하므로 운영 DB에 절대 실행하지 말 것.
--
-- 독서/공부/주간회고 "공통 과제 3종"을 기록할 곳이 앱 어디에도 없던 문제를 고치기 위한 테이블.
-- 아비투스(GoalType)로 선택하는 목표가 아니라 모든 참가자가 공통으로 하는 과제라서, 가중치
-- 합계 100% 규칙이 있는 user_goals/user_stats/user_missions 트리에 억지로 끼워넣지 않고
-- 별도 테이블로 분리했다.

USE princess_project;

CREATE TABLE IF NOT EXISTS common_task_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,

    task_type ENUM(
        'READING',
        'STUDY',
        'WEEKLY_RETROSPECTIVE'
    ) NOT NULL,

    -- READING/STUDY: 그 날짜. WEEKLY_RETROSPECTIVE: 그 주의 월요일.
    record_date DATE NOT NULL,

    -- READING
    start_page INT NULL,
    end_page INT NULL,

    -- STUDY
    study_planned_amount DECIMAL(10, 2) NULL,
    study_completed_amount DECIMAL(10, 2) NULL,

    -- WEEKLY_RETROSPECTIVE (PART1/2/3)
    retro_daily_life TEXT NULL,
    retro_week_review TEXT NULL,
    retro_next_week_plan TEXT NULL,

    memo VARCHAR(1000) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_common_task_records_user_type_date
        UNIQUE (user_id, task_type, record_date),

    INDEX idx_common_task_records_project (project_id),

    CONSTRAINT fk_common_task_records_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_common_task_records_project
        FOREIGN KEY (project_id) REFERENCES user_projects(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_common_task_records_start_page
        CHECK (start_page IS NULL OR start_page >= 0),

    CONSTRAINT chk_common_task_records_end_page
        CHECK (end_page IS NULL OR end_page >= 0),

    CONSTRAINT chk_common_task_records_study_planned
        CHECK (study_planned_amount IS NULL OR study_planned_amount >= 0),

    CONSTRAINT chk_common_task_records_study_completed
        CHECK (study_completed_amount IS NULL OR study_completed_amount >= 0)
) ENGINE = InnoDB;

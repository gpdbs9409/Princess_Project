-- =========================================================
-- 0. DATABASE
-- =========================================================

DROP DATABASE IF EXISTS princess_project;

CREATE DATABASE princess_project
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE princess_project;


-- =========================================================
-- 1. USERS
-- 사용자 기본 정보
-- =========================================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    nickname VARCHAR(50) NOT NULL,

    -- BCrypt hash, never the raw password
    password_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    last_login_at TIMESTAMP NULL,

    CONSTRAINT uk_users_nickname
        UNIQUE (nickname)
) ENGINE = InnoDB;


-- =========================================================
-- 2. USER_PROJECTS
-- 사용자의 Princess Project 진행 단위
-- =========================================================

CREATE TABLE user_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    title VARCHAR(255) NOT NULL,

    -- 사용자가 원하는 최종 인간상
    goal_human VARCHAR(500),

    -- 최종적으로 도달하고 싶은 엔딩 또는 행동양식 설명
    goal_ending TEXT,

    start_date DATE NOT NULL,
    end_date DATE NULL,

    status ENUM(
        'ACTIVE',
        'COMPLETED',
        'PAUSED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_projects_user_id (user_id),

    INDEX idx_user_projects_user_status (
        user_id,
        status
    ),

    CONSTRAINT fk_user_projects_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT chk_user_projects_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        )
) ENGINE = InnoDB;


-- =========================================================
-- 3. GOAL_TYPES
-- 사용자가 선택할 수 있는 아비투스 7자본 전체 풀
-- =========================================================

CREATE TABLE goal_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    code ENUM(
        'PHYSICAL',
        'ECONOMY',
        'CULTURE',
        'KNOWLEDGE',
        'LANGUAGE',
        'PSYCHOLOGY',
        'SYMBOL'
    ) NOT NULL,

    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),

    display_order INT NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_goal_types_code
        UNIQUE (code),

    CONSTRAINT uk_goal_types_display_order
        UNIQUE (display_order)
) ENGINE = InnoDB;


-- =========================================================
-- 4. USER_GOALS
-- 특정 프로젝트에서 사용자가 선택한 아비투스
--
-- user_projects와 goal_types의 N:M 중간 테이블 역할
-- 예: 신체 70%, 경제 30%
-- =========================================================

CREATE TABLE user_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    project_id BIGINT NOT NULL,
    goal_type_id BIGINT NOT NULL,

    -- 1순위, 2순위 등의 표시 순서
    priority INT NULL,

    -- 프로젝트 총점에서 해당 아비투스가 차지하는 비중
    weight_percent INT NOT NULL,

    -- 해당 아비투스에 대한 사용자의 구체적인 목표
    custom_goal_text VARCHAR(500),

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_goals_project_goal_type
        UNIQUE (
            project_id,
            goal_type_id
        ),

    CONSTRAINT uk_user_goals_project_priority
        UNIQUE (
            project_id,
            priority
        ),

    INDEX idx_user_goals_goal_type_id (
        goal_type_id
    ),

    CONSTRAINT fk_user_goals_project
        FOREIGN KEY (project_id)
        REFERENCES user_projects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_goals_goal_type
        FOREIGN KEY (goal_type_id)
        REFERENCES goal_types(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_user_goals_priority
        CHECK (
            priority IS NULL
            OR priority > 0
        ),

    CONSTRAINT chk_user_goals_weight
        CHECK (
            weight_percent BETWEEN 1 AND 100
        )
) ENGINE = InnoDB;


-- =========================================================
-- 5. STAT_TYPES
-- 각 아비투스에 속하는 행동양식 전체 풀
--
-- 예:
-- 신체 → 운동, 식단, 자세/스트레칭
-- 경제 → 가계부, 소비 관리, 경제 콘텐츠
-- =========================================================

CREATE TABLE stat_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    goal_type_id BIGINT NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    display_order INT NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_stat_types_goal_code
        UNIQUE (
            goal_type_id,
            code
        ),

    CONSTRAINT uk_stat_types_goal_order
        UNIQUE (
            goal_type_id,
            display_order
        ),

    INDEX idx_stat_types_goal_type_id (
        goal_type_id
    ),

    CONSTRAINT fk_stat_types_goal_type
        FOREIGN KEY (goal_type_id)
        REFERENCES goal_types(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 6. USER_STATS
-- 사용자가 선택한 행동양식
--
-- user_goals와 stat_types의 N:M 중간 테이블 역할
--
-- 예:
-- 사용자 목표 '신체' → 운동, 식단 선택
-- 사용자 목표 '경제' → 가계부, 소비 관리 선택
-- =========================================================

CREATE TABLE user_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_goal_id BIGINT NOT NULL,
    stat_type_id BIGINT NOT NULL,

    priority INT NULL,

    -- 같은 아비투스 내부에서 행동양식별 비중이 필요할 때 사용
    -- MVP에서 비중을 적용하지 않으면 NULL 가능
    weight_percent INT NULL,

    -- 기본 행동양식 대신 별도의 표시명을 사용할 때
    custom_stat_name VARCHAR(100),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_stats_goal_stat
        UNIQUE (
            user_goal_id,
            stat_type_id
        ),

    CONSTRAINT uk_user_stats_goal_priority
        UNIQUE (
            user_goal_id,
            priority
        ),

    INDEX idx_user_stats_stat_type_id (
        stat_type_id
    ),

    INDEX idx_user_stats_active (
        user_goal_id,
        active
    ),

    CONSTRAINT fk_user_stats_user_goal
        FOREIGN KEY (user_goal_id)
        REFERENCES user_goals(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_stats_stat_type
        FOREIGN KEY (stat_type_id)
        REFERENCES stat_types(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_user_stats_priority
        CHECK (
            priority IS NULL
            OR priority > 0
        ),

    CONSTRAINT chk_user_stats_weight
        CHECK (
            weight_percent IS NULL
            OR weight_percent BETWEEN 1 AND 100
        )
) ENGINE = InnoDB;


-- =========================================================
-- 7. MISSION_DEFINITIONS
-- 행동양식별 기본 미션 전체 풀
--
-- 예:
-- 운동 → 운동 30분, 만 보 걷기
-- 식단 → 식단 기록, 야식 참기
-- 가계부 → 하루 지출 기록
-- =========================================================

CREATE TABLE mission_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    stat_type_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    mission_type ENUM(
        'DAILY',
        'WEEKLY',
        'TOTAL'
    ) NOT NULL DEFAULT 'DAILY',

    default_target_value DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(50) NOT NULL,

    default_assigned_points DECIMAL(10, 2) NOT NULL,

    requires_photo BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_mission_definitions_stat_type (
        stat_type_id
    ),

    INDEX idx_mission_definitions_active (
        stat_type_id,
        active
    ),

    CONSTRAINT fk_mission_definitions_stat_type
        FOREIGN KEY (stat_type_id)
        REFERENCES stat_types(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_mission_definitions_target
        CHECK (
            default_target_value > 0
        ),

    CONSTRAINT chk_mission_definitions_points
        CHECK (
            default_assigned_points >= 0
        )
) ENGINE = InnoDB;


-- =========================================================
-- 8. USER_MISSIONS
-- 사용자가 실제 수행 대상으로 선택하거나 직접 만든 미션
--
-- 기본 미션 선택:
-- mission_definition_id에 값 존재
--
-- 커스텀 미션:
-- mission_definition_id = NULL
-- custom_name에 값 존재
-- =========================================================

CREATE TABLE user_missions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_stat_id BIGINT NOT NULL,

    mission_definition_id BIGINT NULL,

    custom_name VARCHAR(255),

    target_value DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    assigned_points DECIMAL(10, 2) NOT NULL,

    mission_type ENUM(
        'DAILY',
        'WEEKLY',
        'TOTAL'
    ) NOT NULL DEFAULT 'DAILY',

    priority INT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    /*
      동일한 user_stat 안에서 동일한 기본 미션을
      중복으로 선택할 수 없도록 한다.

      mission_definition_id가 NULL인 커스텀 미션은
      MySQL UNIQUE 특성상 여러 개 생성할 수 있다.
    */
    CONSTRAINT uk_user_missions_stat_definition
        UNIQUE (
            user_stat_id,
            mission_definition_id
        ),

    CONSTRAINT uk_user_missions_stat_priority
        UNIQUE (
            user_stat_id,
            priority
        ),

    INDEX idx_user_missions_definition_id (
        mission_definition_id
    ),

    INDEX idx_user_missions_active (
        user_stat_id,
        active
    ),

    CONSTRAINT fk_user_missions_user_stat
        FOREIGN KEY (user_stat_id)
        REFERENCES user_stats(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_missions_definition
        FOREIGN KEY (mission_definition_id)
        REFERENCES mission_definitions(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,

    CONSTRAINT chk_user_missions_target
        CHECK (
            target_value > 0
        ),

    CONSTRAINT chk_user_missions_points
        CHECK (
            assigned_points >= 0
        ),

    CONSTRAINT chk_user_missions_priority
        CHECK (
            priority IS NULL
            OR priority > 0
        ),

    CONSTRAINT chk_user_missions_name
        CHECK (
            mission_definition_id IS NOT NULL
            OR (
                custom_name IS NOT NULL
                AND CHAR_LENGTH(TRIM(custom_name)) > 0
            )
        )
) ENGINE = InnoDB;


-- =========================================================
-- 9. DAILY_RECORDS
-- 사용자의 실제 미션 수행 기록 및 계산된 점수
-- =========================================================

CREATE TABLE daily_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_mission_id BIGINT NOT NULL,

    record_date DATE NOT NULL,

    -- 사용자가 입력한 실제 수행량
    input_value DECIMAL(10, 2) NOT NULL,

    photo_url VARCHAR(1000),
    memo VARCHAR(1000),

    /*
      미션 설정이 나중에 변경되더라도
      과거 점수가 변하지 않도록 계산 당시 값을 저장한다.
    */
    target_value_snapshot DECIMAL(10, 2) NOT NULL,
    assigned_points_snapshot DECIMAL(10, 2) NOT NULL,

    -- 예: 0.5000 = 50%
    achievement_rate DECIMAL(8, 4) NOT NULL,

    earned_score DECIMAL(10, 2) NOT NULL,

    /*
      현재 구조에서는 user_goals가 사용자가 선택한 목표이므로
      대부분 MAIN이 된다.

      추후 선택하지 않은 목표의 사이드 미션을 허용할 경우
      BONUS로 저장할 수 있다.
    */
    score_type ENUM(
        'MAIN',
        'BONUS'
    ) NOT NULL DEFAULT 'MAIN',

    verification_status ENUM(
        'NOT_REQUIRED',
        'PENDING',
        'APPROVED',
        'REJECTED'
    ) NOT NULL DEFAULT 'NOT_REQUIRED',

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_daily_records_mission_date
        UNIQUE (
            user_id,
            user_mission_id,
            record_date
        ),

    INDEX idx_daily_records_user_date (
        user_id,
        record_date
    ),

    INDEX idx_daily_records_project_date (
        project_id,
        record_date
    ),

    INDEX idx_daily_records_user_mission (
        user_mission_id
    ),

    CONSTRAINT fk_daily_records_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_daily_records_project
        FOREIGN KEY (project_id)
        REFERENCES user_projects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_daily_records_user_mission
        FOREIGN KEY (user_mission_id)
        REFERENCES user_missions(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_daily_records_input
        CHECK (
            input_value >= 0
        ),

    CONSTRAINT chk_daily_records_target_snapshot
        CHECK (
            target_value_snapshot > 0
        ),

    CONSTRAINT chk_daily_records_points_snapshot
        CHECK (
            assigned_points_snapshot >= 0
        ),

    CONSTRAINT chk_daily_records_achievement
        CHECK (
            achievement_rate >= 0
        ),

    CONSTRAINT chk_daily_records_earned_score
        CHECK (
            earned_score >= 0
        )
) ENGINE = InnoDB;


-- =========================================================
-- 10. AI_FEEDBACKS
-- 일일, 주간, 최종 AI 피드백
-- =========================================================

CREATE TABLE ai_feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,

    feedback_date DATE NOT NULL,

    feedback_type ENUM(
        'DAILY',
        'WEEKLY',
        'FINAL'
    ) NOT NULL DEFAULT 'DAILY',

    summary TEXT,
    praise TEXT,
    improvement TEXT,
    tomorrow TEXT,
    cheer TEXT,

    model VARCHAR(100),

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_ai_feedbacks_project_date_type
        UNIQUE (
            user_id,
            project_id,
            feedback_date,
            feedback_type
        ),

    INDEX idx_ai_feedbacks_project (
        project_id
    ),

    INDEX idx_ai_feedbacks_user_date (
        user_id,
        feedback_date
    ),

    CONSTRAINT fk_ai_feedbacks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_ai_feedbacks_project
        FOREIGN KEY (project_id)
        REFERENCES user_projects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

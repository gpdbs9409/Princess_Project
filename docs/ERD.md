# Princess Project ERD (MySQL)

Hibernate `ddl-auto=update` 기준으로 실제 생성되는 테이블/컬럼을 그대로 반영했습니다.
엔티티 코드에는 없지만 논리적으로 필요한 유니크 제약(예: 사용자+날짜+미션 조합)은 **권장 제약**으로 표시했습니다 (현재는 애플리케이션 로직의 upsert로만 보장됨).

## Mermaid 소스

```mermaid
erDiagram
    USERS ||--o{ USER_STAT_FOCUS : "has"
    USERS ||--o{ DAILY_RECORDS : "writes"
    USERS ||--o{ DAILY_SCORES : "has"
    USERS ||--o{ DAILY_STAT_SCORES : "has"
    USERS ||--o{ AI_FEEDBACKS : "has"
    MISSION_DEFINITIONS ||--o{ DAILY_RECORDS : "recorded as"

    USERS {
        bigint id PK
        varchar nickname "논리적 unique (DB 제약 없음)"
        varchar goal_human
        varchar goal_ending
        datetime created_at
    }

    USER_STAT_FOCUS {
        bigint id PK
        bigint user_id FK
        varchar stat_type "enum string"
        int weight_percent
    }

    MISSION_DEFINITIONS {
        bigint id PK
        varchar name
        varchar mission_type "enum: DAILY/WEEKLY/TOTAL"
        varchar stat_type "enum string"
        int assigned_points
        double target_value
        varchar unit
        boolean common
    }

    DAILY_RECORDS {
        bigint id PK
        bigint user_id FK
        bigint mission_id FK
        date date
        double input_value
        varchar photo_url
        varchar memo
        datetime created_at
        "권장: UNIQUE(user_id, mission_id, date)" note
    }

    DAILY_SCORES {
        bigint id PK
        bigint user_id FK
        date date
        double mission_score
        double behavior_score
        double stat_score
        double bonus_score
        double total_score
        double progress_percent
        "권장: UNIQUE(user_id, date)" note
    }

    DAILY_STAT_SCORES {
        bigint id PK
        bigint user_id FK
        date date
        varchar stat_type "enum string"
        double score
        "권장: UNIQUE(user_id, date, stat_type)" note
    }

    AI_FEEDBACKS {
        bigint id PK
        bigint user_id FK
        date date
        text prompt
        text summary
        text praise
        text improvement
        text tomorrow
        text cheer
        varchar model
        datetime created_at
        "권장: UNIQUE(user_id, date)" note
    }
```

## 테이블 설명

| 테이블 | 역할 |
|---|---|
| `users` | 사용자(닉네임 기반, 비밀번호 없음), 목표 문구(`goal_human`/`goal_ending`) 보유 |
| `user_stat_focus` | 사용자가 선택한 스탯 비중 (0~100%, 여러 stat 가능) |
| `mission_definitions` | 미션 카탈로그(공용, `MissionSeeder`로 앱 시작 시 5개 시딩) |
| `daily_records` | 사용자가 특정 날짜에 특정 미션에 입력한 실제 행동값(예: 독서 15분) + 사진/메모 |
| `daily_scores` | 하루 단위 집계 점수(총점/진행률). 현재 MVP는 `mission_score == behavior_score == stat_score == total_score` (구분 미구현, 코드 주석에 명시된 알려진 단순화) |
| `daily_stat_scores` | 하루 단위 스탯별 점수 (레이더차트/스탯 누적용) |
| `ai_feedbacks` | 그날의 집계 데이터를 바탕으로 생성된 AI 피드백 5종 텍스트 |

## 관계 요약

- `users 1 : N user_stat_focus` — 사용자 1명이 여러 스탯에 비중 설정
- `users 1 : N daily_records / daily_scores / daily_stat_scores / ai_feedbacks`
- `mission_definitions 1 : N daily_records` — 미션 하나에 여러 사용자/날짜의 기록이 달림

## MySQL 생성 스크립트 (참고용, Hibernate가 실제로는 자동 생성)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(255) NOT NULL,
    goal_human VARCHAR(255),
    goal_ending VARCHAR(255),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uq_users_nickname (nickname) -- 권장 추가 (현재 코드엔 없음)
);

CREATE TABLE user_stat_focus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stat_type VARCHAR(20) NOT NULL,
    weight_percent INT NOT NULL,
    CONSTRAINT fk_usf_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE mission_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mission_type VARCHAR(20) NOT NULL,
    stat_type VARCHAR(20) NOT NULL,
    assigned_points INT NOT NULL,
    target_value DOUBLE NOT NULL,
    unit VARCHAR(50),
    common BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE daily_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mission_id BIGINT NOT NULL,
    date DATE NOT NULL,
    input_value DOUBLE NOT NULL,
    photo_url VARCHAR(500),
    memo VARCHAR(1000),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_dr_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_dr_mission FOREIGN KEY (mission_id) REFERENCES mission_definitions(id),
    UNIQUE KEY uq_dr_user_mission_date (user_id, mission_id, date) -- 권장 추가
);

CREATE TABLE daily_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    mission_score DOUBLE NOT NULL,
    behavior_score DOUBLE NOT NULL,
    stat_score DOUBLE NOT NULL,
    bonus_score DOUBLE NOT NULL,
    total_score DOUBLE NOT NULL,
    progress_percent DOUBLE NOT NULL,
    CONSTRAINT fk_ds_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uq_ds_user_date (user_id, date) -- 권장 추가
);

CREATE TABLE daily_stat_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    stat_type VARCHAR(20) NOT NULL,
    score DOUBLE NOT NULL,
    CONSTRAINT fk_dss_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uq_dss_user_date_stat (user_id, date, stat_type) -- 권장 추가
);

CREATE TABLE ai_feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    prompt TEXT,
    summary TEXT,
    praise TEXT,
    improvement TEXT,
    tomorrow TEXT,
    cheer TEXT,
    model VARCHAR(100),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_af_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uq_af_user_date (user_id, date) -- 권장 추가
);
```

시각적 이미지 버전은 `docs/erd.svg` (또는 프로젝트 아티팩트 링크) 참고.

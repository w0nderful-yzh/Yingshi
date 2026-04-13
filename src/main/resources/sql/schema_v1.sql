CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NULL,
    role VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
);

CREATE TABLE IF NOT EXISTS video_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) NULL,
    source_type VARCHAR(32) NOT NULL,
    file_name VARCHAR(255) NULL,
    file_url VARCHAR(512) NULL,
    size BIGINT NULL,
    external_url VARCHAR(512) NULL,
    device_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_video_source_device (device_id),
    KEY idx_video_source_type (source_type)
);

CREATE TABLE IF NOT EXISTS device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    stream_url VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
    remark VARCHAR(255) NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_device_status (status),
    KEY idx_device_source_type (source_type)
);

CREATE TABLE IF NOT EXISTS zone_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    zone_name VARCHAR(128) NOT NULL,
    zone_type VARCHAR(32) NOT NULL,
    coordinates JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_zone_device_id (device_id),
    KEY idx_zone_type (zone_type)
);

CREATE TABLE IF NOT EXISTS video_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(128) NOT NULL,
    device_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    file_id VARCHAR(64) NULL,
    video_url VARCHAR(512) NULL,
    frame_interval_sec INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'INIT',
    progress INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_video_task_status (status),
    KEY idx_video_task_device (device_id),
    KEY idx_video_task_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS frame_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    frame_time DATETIME NOT NULL,
    image_url VARCHAR(512) NULL,
    pet_type VARCHAR(32) NULL,
    bbox JSON NULL,
    center_x INT NULL,
    center_y INT NULL,
    confidence DECIMAL(5,4) NULL,
    zone_type VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_frame_result_task (task_id),
    KEY idx_frame_result_frame_time (frame_time)
);

CREATE TABLE IF NOT EXISTS behavior_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_level VARCHAR(16) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    description VARCHAR(1024) NOT NULL,
    snapshot_url VARCHAR(512) NULL,
    rule_detail JSON NULL,
    ack_status TINYINT NOT NULL DEFAULT 0,
    ack_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_behavior_event_task (task_id),
    KEY idx_behavior_event_type (event_type),
    KEY idx_behavior_event_ack_status (ack_status)
);

CREATE TABLE IF NOT EXISTS task_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    frame_count INT NOT NULL DEFAULT 0,
    event_count INT NOT NULL DEFAULT 0,
    stillness_count INT NOT NULL DEFAULT 0,
    pacing_count INT NOT NULL DEFAULT 0,
    danger_zone_count INT NOT NULL DEFAULT 0,
    movement_score DECIMAL(5,2) NULL,
    zone_stay_stats JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_summary_task_id (task_id)
);

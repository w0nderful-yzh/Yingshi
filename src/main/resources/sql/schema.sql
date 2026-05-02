CREATE DATABASE IF NOT EXISTS yingshi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yingshi;

-- ============================================================
-- 萤石智宠 Demo 核心表
-- 保留：sys_user, device, pet, alarm_message
-- ============================================================

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password_hash   VARCHAR(100) NOT NULL COMMENT '加密密码',
    nickname        VARCHAR(50)  COMMENT '昵称',
    role_code       VARCHAR(20)  DEFAULT 'ADMIN' COMMENT '角色: ADMIN/OPERATOR/VIEWER',
    avatar_url      VARCHAR(255) COMMENT '头像',
    status          TINYINT      DEFAULT 1 COMMENT '状态(1正常 0禁用)',
    last_login_at   DATETIME     COMMENT '最后登录时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 设备管理表
CREATE TABLE IF NOT EXISTS device (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_serial   VARCHAR(64)  COMMENT '萤石设备序列号',
    channel_no      INT          DEFAULT 1 COMMENT '通道号，摄像头一般为1',
    device_name     VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_type     VARCHAR(100) COMMENT '设备型号/类型',
    source_type     VARCHAR(20)  DEFAULT 'RTSP' COMMENT '视频源类型: UPLOAD/RTSP/EZVIZ',
    stream_url      VARCHAR(500) COMMENT '拉流地址',
    status          VARCHAR(20)  DEFAULT 'OFFLINE' COMMENT '状态: ONLINE/OFFLINE/DISABLED',
    remark          VARCHAR(255) COMMENT '备注',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除(1已删)',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    UNIQUE INDEX uk_device_serial (device_serial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理表';

-- 3. 宠物管理表
CREATE TABLE IF NOT EXISTS pet (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '所属用户ID',
    pet_name    VARCHAR(50)  NOT NULL COMMENT '宠物名',
    pet_type    VARCHAR(20)  NOT NULL COMMENT '宠物类型: DOG/CAT/OTHER',
    age         INT          COMMENT '年龄(月)',
    gender      VARCHAR(10)  COMMENT '性别: MALE/FEMALE/UNKNOWN',
    remark      VARCHAR(255) COMMENT '备注',
    avatar_url  VARCHAR(500) COMMENT '头像URL',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物管理表';

-- 4. 告警消息表
CREATE TABLE IF NOT EXISTS alarm_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT COMMENT '本地设备ID',
    device_serial   VARCHAR(64) NOT NULL COMMENT '萤石设备序列号',
    channel_no      INT DEFAULT 1 COMMENT '通道号',
    alarm_id        VARCHAR(128) COMMENT '萤石告警唯一标识/UUID',
    alarm_type      VARCHAR(100) COMMENT '告警类型',
    alarm_name      VARCHAR(100) COMMENT '告警名称',
    alarm_time      DATETIME COMMENT '告警时间',
    alarm_pic_url   VARCHAR(1000) COMMENT '告警图片地址',
    alarm_content   VARCHAR(500) COMMENT '告警内容',
    read_status     TINYINT DEFAULT 0 COMMENT '本地已读状态：0未读，1已读',
    source          VARCHAR(20) DEFAULT 'EZVIZ' COMMENT '告警来源',
    raw_json        TEXT COMMENT '萤石原始告警JSON',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_alarm_device_id (device_id),
    INDEX idx_alarm_device_serial (device_serial),
    INDEX idx_alarm_time (alarm_time),
    INDEX idx_alarm_read_status (read_status),
    UNIQUE INDEX uk_alarm_unique (device_serial, alarm_type, alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简化告警消息表';

-- 5. 宠物检测配置表
CREATE TABLE IF NOT EXISTS pet_detection_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT NOT NULL COMMENT '所属用户ID',
    pet_id          BIGINT NOT NULL COMMENT '宠物ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID',
    enabled         TINYINT DEFAULT 1 COMMENT '是否启用检测: 0关闭 1开启',
    cooldown_seconds INT DEFAULT 300 COMMENT '告警冷却时间(秒), 避免重复告警',
    remark          VARCHAR(255) COMMENT '备注',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_pet (pet_id),
    INDEX idx_device (device_id),
    UNIQUE INDEX uk_pet_device (pet_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物检测配置表';

-- 6. 宠物安全区域表
CREATE TABLE IF NOT EXISTS pet_safe_zone (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    detection_config_id BIGINT NOT NULL COMMENT '关联的检测配置ID',
    zone_name       VARCHAR(100) COMMENT '区域名称',
    zone_type       VARCHAR(20) DEFAULT 'RECTANGLE' COMMENT '区域类型: RECTANGLE(矩形)/POLYGON(多边形)',
    -- 矩形区域 (百分比坐标 0-100)
    rect_left       DOUBLE COMMENT '矩形左边界(x1), 百分比',
    rect_top        DOUBLE COMMENT '矩形上边界(y1), 百分比',
    rect_right      DOUBLE COMMENT '矩形右边界(x2), 百分比',
    rect_bottom     DOUBLE COMMENT '矩形下边界(y2), 百分比',
    -- 多边形区域 (JSON数组, 格式: [{"x":10.5,"y":20.3}, ...], 百分比坐标)
    polygon_points  TEXT COMMENT '多边形顶点坐标JSON',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config (detection_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物安全区域表';

-- 7. 宠物检测记录表
CREATE TABLE IF NOT EXISTS pet_detection_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    detection_config_id BIGINT NOT NULL COMMENT '检测配置ID',
    pet_id          BIGINT NOT NULL COMMENT '宠物ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID',
    device_serial   VARCHAR(64) COMMENT '设备序列号',
    detect_time     DATETIME COMMENT '检测时间',
    pet_coord_x     DOUBLE COMMENT '宠物检测坐标X(百分比)',
    pet_coord_y     DOUBLE COMMENT '宠物检测坐标Y(百分比)',
    pet_width       DOUBLE COMMENT '宠物检测区域宽度(百分比)',
    pet_height      DOUBLE COMMENT '宠物检测区域高度(百分比)',
    in_safe_zone    TINYINT DEFAULT 1 COMMENT '是否在安全区域内: 1在区域内 0在区域外',
    alarm_triggered TINYINT DEFAULT 0 COMMENT '是否触发告警: 0未触发 1已触发',
    snapshot_url    VARCHAR(1000) COMMENT '检测截图URL',
    ai_result_json  TEXT COMMENT 'AI检测原始结果JSON',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_config (detection_config_id),
    INDEX idx_pet (pet_id),
    INDEX idx_device (device_id),
    INDEX idx_detect_time (detect_time),
    INDEX idx_alarm (alarm_triggered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物检测记录表';

-- ============================================================
-- 数据初始化
-- ============================================================

-- 默认管理员账户（密码: 123456）
INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, role_code, status)
VALUES (1, 'admin', '$2a$10$gFcFjASQAPmUr8QHeCfBz.rlfDUyhlw7DUhPDXVIYVF7a1DyJ/coe', '超级管理员', 'ADMIN', 1);

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

-- ============================================================
-- 数据初始化
-- ============================================================

-- 默认管理员账户（密码: 123456）
INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, role_code, status)
VALUES (1, 'admin', '$2a$10$gFcFjASQAPmUr8QHeCfBz.rlfDUyhlw7DUhPDXVIYVF7a1DyJ/coe', '超级管理员', 'ADMIN', 1);

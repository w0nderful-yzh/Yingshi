CREATE DATABASE IF NOT EXISTS yingshi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yingshi;

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role_code VARCHAR(20) DEFAULT 'ADMIN' COMMENT '角色',
    avatar_url VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态(1正常 0禁用)',
    last_login_at DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 设备管理表
CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_sn VARCHAR(100) UNIQUE COMMENT '设备序列号',
    status VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '状态: ONLINE, OFFLINE, DISABLED',
    location VARCHAR(200) COMMENT '安装位置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理表';

-- 3. 视频源表
CREATE TABLE IF NOT EXISTS video_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT COMMENT '关联设备ID（上传视频非必填）',
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型: UPLOAD, RTSP, EZVIZ',
    source_url VARCHAR(500) NOT NULL COMMENT '视频地址或流地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频源表';

-- 4. 区域配置表
CREATE TABLE IF NOT EXISTS zone_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '关联设备ID',
    zone_name VARCHAR(100) NOT NULL COMMENT '区域名称',
    zone_type VARCHAR(20) NOT NULL COMMENT '区域类型: NORMAL, REST, FEED, DANGER',
    coordinates JSON NOT NULL COMMENT '多边形坐标点JSON数组',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域配置表';

-- 5. 分析任务管理表
CREATE TABLE IF NOT EXISTS video_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL COMMENT '视频源ID',
    status VARCHAR(20) DEFAULT 'INIT' COMMENT '任务状态: INIT, WAITING, PROCESSING, FINISHED, FAILED, CANCELED',
    error_message TEXT COMMENT '失败原因',
    start_time DATETIME COMMENT '开始处理时间',
    end_time DATETIME COMMENT '结束处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务表';

-- 6. 帧检测结果表
CREATE TABLE IF NOT EXISTS frame_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    frame_index INT NOT NULL COMMENT '帧序号',
    frame_timestamp BIGINT COMMENT '帧对应的视频时间戳(毫秒)',
    detect_data JSON COMMENT '检测结果(目标框、类别等)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帧检测结果表';

-- 7. 异常行为事件表
CREATE TABLE IF NOT EXISTS behavior_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型: STILLNESS, PACING, DANGER_ZONE',
    description VARCHAR(255) COMMENT '事件描述摘要',
    start_time DATETIME COMMENT '起始时间',
    end_time DATETIME COMMENT '结束时间',
    thumbnail_url VARCHAR(500) COMMENT '抓拍封面图片URL',
    clip_url VARCHAR(500) COMMENT '短片段URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常行为事件表';

-- 8. 任务摘要统计表
CREATE TABLE IF NOT EXISTS task_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE COMMENT '关联任务ID',
    total_events INT DEFAULT 0 COMMENT '异常事件总数',
    stillness_count INT DEFAULT 0 COMMENT '静止事件数',
    pacing_count INT DEFAULT 0 COMMENT '来回踱步事件数',
    danger_zone_count INT DEFAULT 0 COMMENT '危险区域事件数',
    summary_text TEXT COMMENT 'AI总结分析文本(如需)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务摘要表';

-- ==========================================
-- 数据初始化: 插入默认 admin 账户 (密码: 123456)
-- ==========================================
INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, role_code, status) 
VALUES (1, 'admin', '123456', '超级管理员', 'ADMIN', 1);

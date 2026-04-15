CREATE DATABASE IF NOT EXISTS yingshi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yingshi;
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
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
INSERT INTO sys_user (username, password_hash, nickname, role_code, status) VALUES ('admin', '\\\./jZ8gYI5c.K4c.wT8pQ1B8h1Jb3.4fO', '超级管理员', 'ADMIN', 1);

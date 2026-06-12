USE yingshi;

-- 仅供本地 Docker Compose 演示，生产环境不要导入。
-- 默认管理员：admin / 123456
INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, role_code, status)
VALUES (1, 'admin', '$2a$10$gFcFjASQAPmUr8QHeCfBz.rlfDUyhlw7DUhPDXVIYVF7a1DyJ/coe', '超级管理员', 'ADMIN', 1);

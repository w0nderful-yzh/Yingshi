USE yingshi;

-- 已有数据库只执行一次。新部署会直接通过 schema.sql 创建正确索引。
ALTER TABLE alarm_message
    DROP INDEX uk_alarm_unique,
    ADD INDEX idx_alarm_type_time (device_serial, alarm_type, alarm_time),
    ADD UNIQUE INDEX uk_alarm_message_id (device_serial, alarm_id);

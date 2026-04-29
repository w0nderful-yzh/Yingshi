  CREATE DATABASE IF NOT EXISTS yingshi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  USE yingshi;

  -- ============================================================
  -- 一、基础表（与现有实体类完全对应）
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

  -- 2. 设备管理表（匹配 Device.java 实体）
  CREATE TABLE IF NOT EXISTS device (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_name     VARCHAR(100) NOT NULL COMMENT '设备名称',
      source_type     VARCHAR(20)  DEFAULT 'RTSP' COMMENT '视频源类型: UPLOAD/RTSP/EZVIZ',
      stream_url      VARCHAR(500) COMMENT '拉流地址(rtsp://... 或 http://...)',
      status          VARCHAR(20)  DEFAULT 'OFFLINE' COMMENT '状态: ONLINE/OFFLINE/DISABLED',
      remark          VARCHAR(255) COMMENT '备注',
      deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除(1已删)',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_status (status)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理表';

  -- 3. 视频源表（匹配 VideoSource.java 实体）
  CREATE TABLE IF NOT EXISTS video_source (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       COMMENT '关联设备ID',
      source_type     VARCHAR(20)  NOT NULL COMMENT '来源类型: UPLOAD/RTSP/EZVIZ',
      source_url      VARCHAR(500) NOT NULL COMMENT '视频地址或流地址',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device (device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频源表';

  -- 4. 区域配置表（匹配 ZoneConfig.java 实体）
  CREATE TABLE IF NOT EXISTS zone_config (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '关联设备ID',
      zone_name       VARCHAR(100) NOT NULL COMMENT '区域名称',
      zone_type       VARCHAR(20)  NOT NULL COMMENT '区域类型: NORMAL/REST/FEED/DANGER',
      coordinates     JSON         NOT NULL COMMENT '多边形坐标点JSON数组 [[x,y],[x,y],...]',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device (device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域配置表';

  -- 5. 分析任务表（匹配 VideoTask.java 实体）
  CREATE TABLE IF NOT EXISTS video_task (
      id                BIGINT AUTO_INCREMENT PRIMARY KEY,
      task_name         VARCHAR(100) NOT NULL COMMENT '任务名称',
      device_id         BIGINT       COMMENT '关联设备ID',
      source_type       VARCHAR(20)  NOT NULL COMMENT '视频源类型: UPLOAD/RTSP/EZVIZ',
      file_id           VARCHAR(100) COMMENT '上传文件ID（source_type=UPLOAD时关联video_source.id）',
      video_url         VARCHAR(500) COMMENT '视频URL地址',
      frame_interval_sec INT         DEFAULT 2 COMMENT '抽帧间隔(秒)',
      status            VARCHAR(20)  DEFAULT 'INIT' COMMENT '任务状态:
  INIT/WAITING/PROCESSING/FINISHED/FAILED/CANCELED',
      progress          INT          DEFAULT 0 COMMENT '进度百分比(0-100)',
      created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      started_at        DATETIME     COMMENT '开始处理时间',
      finished_at       DATETIME     COMMENT '结束处理时间',
      updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device  (device_id),
      INDEX idx_status  (status)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务表';

  -- 6. 帧检测结果表（匹配 FrameResult.java 实体）
  CREATE TABLE IF NOT EXISTS frame_result (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      task_id         BIGINT       NOT NULL COMMENT '关联任务ID',
      frame_time      DATETIME     COMMENT '帧对应视频时间',
      image_url       VARCHAR(500) COMMENT '帧截图URL',
      pet_type        VARCHAR(50)  COMMENT '宠物类型: dog/cat/...',
      bbox            VARCHAR(100) COMMENT '检测框坐标 [x1,y1,x2,y2] JSON',
      center_x        INT          COMMENT '中心点X坐标',
      center_y        INT          COMMENT '中心点Y坐标',
      confidence      DECIMAL(5,4) COMMENT '检测置信度(0.0000~1.0000)',
      zone_type       VARCHAR(20)  COMMENT '所处区域类型: NORMAL/REST/FEED/DANGER',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_task  (task_id),
      INDEX idx_zone  (zone_type)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帧检测结果表';

  -- 7. 异常行为事件表（匹配 BehaviorEvent.java 实体）
  CREATE TABLE IF NOT EXISTS behavior_event (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      task_id         BIGINT       NOT NULL COMMENT '关联任务ID',
      event_type      VARCHAR(50)  NOT NULL COMMENT '事件类型: STILLNESS/PACING/DANGER_ZONE',
      event_level     VARCHAR(20)  NOT NULL DEFAULT 'WARN' COMMENT '事件级别: INFO/WARN/CRITICAL',
      start_time      DATETIME     COMMENT '事件起始时间',
      end_time        DATETIME     COMMENT '事件结束时间',
      description     VARCHAR(255) COMMENT '事件描述摘要',
      snapshot_url    VARCHAR(500) COMMENT '抓拍图片URL',
      rule_detail     JSON         COMMENT '触发规则详情JSON',
      ack_status      TINYINT      DEFAULT 0 COMMENT '确认状态(0未确认 1已确认)',
      ack_at          DATETIME     COMMENT '确认时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_task  (task_id),
      INDEX idx_type  (event_type),
      INDEX idx_ack   (ack_status)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常行为事件表';

  -- 8. 任务摘要统计表（匹配 TaskSummary.java 实体）
  CREATE TABLE IF NOT EXISTS task_summary (
      id                BIGINT AUTO_INCREMENT PRIMARY KEY,
      task_id           BIGINT       NOT NULL UNIQUE COMMENT '关联任务ID（唯一）',
      frame_count       INT          DEFAULT 0 COMMENT '总帧数',
      event_count       INT          DEFAULT 0 COMMENT '异常事件总数',
      stillness_count   INT          DEFAULT 0 COMMENT '静止事件数',
      pacing_count      INT          DEFAULT 0 COMMENT '踱步事件数',
      danger_zone_count INT          DEFAULT 0 COMMENT '危险区域事件数',
      movement_score    DECIMAL(5,2) COMMENT '活动量评分(0~100)',
      zone_stay_stats   JSON         COMMENT '各区域停留统计JSON [{"zoneType":"REST","seconds":120},...]',
      created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务摘要统计表';

  -- 宠物管理表
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

  -- ============================================================
  -- 二、物联网设备接入与管理
  -- ============================================================

  -- 9. 物联网设备凭证表
  CREATE TABLE IF NOT EXISTS iot_device_credential (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL UNIQUE COMMENT '关联设备ID',
      protocol        VARCHAR(20)  NOT NULL DEFAULT 'MQTT' COMMENT '通信协议: MQTT/COAP/HTTP/WEBSOCKET',
      client_id       VARCHAR(100) NOT NULL COMMENT '客户端标识',
      username        VARCHAR(100) COMMENT 'MQTT认证用户名',
      password_enc    VARCHAR(200) COMMENT '加密后的认证密码',
      topic_pub       VARCHAR(255) COMMENT '上行Topic(设备→服务端)',
      topic_sub       VARCHAR(255) COMMENT '下行Topic(服务端→设备)',
      qos             TINYINT      DEFAULT 1 COMMENT 'MQTT QoS(0/1/2)',
      keep_alive_sec  INT          DEFAULT 60 COMMENT '心跳间隔(秒)',
      cert_fingerprint VARCHAR(128) COMMENT 'TLS证书指纹(可选)',
      last_online_at  DATETIME     COMMENT '最后上线时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device (device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物联网设备凭证表';

  -- 10. 设备连接日志表（心跳/上下线记录）
  CREATE TABLE IF NOT EXISTS device_connectivity_log (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '关联设备ID',
      event           VARCHAR(20)  NOT NULL COMMENT '事件: ONLINE/OFFLINE/HEARTBEAT/RECONNECT',
      source_ip       VARCHAR(45)  COMMENT '来源IP',
      latency_ms      INT          COMMENT '延迟(毫秒)',
      detail          VARCHAR(500) COMMENT '详情/错误信息',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
      INDEX idx_device_time (device_id, created_at)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备连接日志表';

  -- 11. 设备遥测数据点表
  CREATE TABLE IF NOT EXISTS device_data_point (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '关联设备ID',
      metric_key      VARCHAR(50)  NOT NULL COMMENT '指标名: cpu_usage/mem_usage/temperature/bandwidth/...',
      metric_value    DECIMAL(12,4) COMMENT '指标值',
      unit            VARCHAR(20)  COMMENT '单位: %/°C/Mbps/...',
      tags            JSON         COMMENT '附加标签JSON',
      reported_at     DATETIME     COMMENT '设备上报时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
      INDEX idx_device_metric (device_id, metric_key, reported_at)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备遥测数据点表';

  -- 12. 设备指令表（下发给设备的命令定义）
  CREATE TABLE IF NOT EXISTS device_command (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '目标设备ID',
      command_code    VARCHAR(50)  NOT NULL COMMENT '指令编码: RESTART/SET_INTERVAL/PTZ_CTRL/OTA_UPGRADE/...',
      command_params  JSON         COMMENT '指令参数JSON',
      status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '执行状态: PENDING/SENT/DELIVERED/EXECUTED/FAILED/TIMEOUT',
      timeout_sec     INT          DEFAULT 30 COMMENT '超时时间(秒)',
      result_data     JSON         COMMENT '设备返回结果JSON',
      created_by      BIGINT       COMMENT '下发人用户ID',
      sent_at         DATETIME     COMMENT '发送时间',
      executed_at     DATETIME     COMMENT '执行完成时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device_status (device_id, status),
      INDEX idx_status      (status)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指令表';

  -- 13. OTA固件管理表
  CREATE TABLE IF NOT EXISTS device_ota_firmware (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       COMMENT '设备ID（NULL表示通用固件）',
      version         VARCHAR(50)  NOT NULL COMMENT '固件版本号',
      file_url        VARCHAR(500) NOT NULL COMMENT '固件下载地址',
      file_size       BIGINT       COMMENT '文件大小(字节)',
      checksum_md5    VARCHAR(32)  COMMENT 'MD5校验值',
      change_log      TEXT         COMMENT '更新日志',
      status          VARCHAR(20)  DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/DEPRECATED',
      published_at    DATETIME     COMMENT '发布时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device_version (device_id, version)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA固件管理表';

  -- 14. OTA升级任务表
  CREATE TABLE IF NOT EXISTS device_ota_task (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      firmware_id     BIGINT       NOT NULL COMMENT '固件ID',
      device_id       BIGINT       NOT NULL COMMENT '目标设备ID',
      status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:
  PENDING/DOWNLOADING/INSTALLING/SUCCESS/FAILED/ROLLBACK',
      progress        INT          DEFAULT 0 COMMENT '升级进度(0-100)',
      error_message   TEXT         COMMENT '失败原因',
      started_at      DATETIME     COMMENT '开始时间',
      finished_at     DATETIME     COMMENT '完成时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device (device_id),
      INDEX idx_firmware (firmware_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA升级任务表';


  -- ============================================================
  -- 三、视频预览与回放
  -- ============================================================

  -- 15. 视频录制存储表
  CREATE TABLE IF NOT EXISTS video_record (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '关联设备ID',
      task_id         BIGINT       COMMENT '关联分析任务ID（可为空）',
      record_type     VARCHAR(20)  NOT NULL COMMENT '录制类型: CONTINUOUS/EVENT/SCHEDULE/MANUAL',
      stream_type     VARCHAR(10)  DEFAULT 'MAIN' COMMENT '码流类型: MAIN/SUB',
      start_time      DATETIME     NOT NULL COMMENT '录制起始时间',
      end_time        DATETIME     COMMENT '录制结束时间',
      duration_sec    INT          COMMENT '时长(秒)',
      file_url        VARCHAR(500) COMMENT '录像文件地址(OSS/COS/NFS)',
      file_size       BIGINT       COMMENT '文件大小(字节)',
      thumbnail_url   VARCHAR(500) COMMENT '封面缩略图',
      record_status   VARCHAR(20)  DEFAULT 'RECORDING' COMMENT '状态: RECORDING/COMPLETED/EXPIRED/DELETED',
      expire_at       DATETIME     COMMENT '过期删除时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device_time (device_id, start_time),
      INDEX idx_task       (task_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频录制存储表';

  -- 16. 播放会话表（记录用户的视频播放行为）
  CREATE TABLE IF NOT EXISTS video_playback_session (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      user_id         BIGINT       NOT NULL COMMENT '用户ID',
      record_id       BIGINT       COMMENT '关联录像记录ID（回放）',
      device_id       BIGINT       COMMENT '关联设备ID（直播）',
      play_type       VARCHAR(20)  NOT NULL COMMENT '播放类型: LIVE/REPLAY',
      protocol        VARCHAR(20)  DEFAULT 'HLS' COMMENT '播放协议: HLS/RTMP/WEBRTC/RTSP',
      play_url        VARCHAR(500) COMMENT '播放地址',
      start_time      DATETIME     COMMENT '开始播放时间',
      end_time        DATETIME     COMMENT '结束播放时间',
      seek_count      INT          DEFAULT 0 COMMENT '拖动次数',
      pause_count     INT          DEFAULT 0 COMMENT '暂停次数',
      buffer_count    INT          DEFAULT 0 COMMENT '缓冲次数',
      first_frame_ms  INT          COMMENT '首帧耗时(毫秒)',
      total_play_sec  INT          COMMENT '总播放时长(秒)',
      client_ip       VARCHAR(45)  COMMENT '客户端IP',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_user   (user_id),
      INDEX idx_record (record_id),
      INDEX idx_device (device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放会话表';

  -- 17. 视频快照表（手动/自动截图）
  CREATE TABLE IF NOT EXISTS video_snapshot (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       COMMENT '关联设备ID',
      task_id         BIGINT       COMMENT '关联任务ID',
      frame_result_id BIGINT       COMMENT '关联帧检测结果ID',
      snapshot_url    VARCHAR(500) NOT NULL COMMENT '快照图片地址',
      snapshot_time   DATETIME     COMMENT '快照对应视频时间',
      snapshot_type   VARCHAR(20)  DEFAULT 'AUTO' COMMENT '类型: AUTO/MANUAL/EVENT',
      created_by      BIGINT       COMMENT '手动截图人',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_device_time (device_id, snapshot_time),
      INDEX idx_task       (task_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频快照表';

  -- 18. 实时流会话表（直播预览会话）
  CREATE TABLE IF NOT EXISTS live_stream_session (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      device_id       BIGINT       NOT NULL COMMENT '设备ID',
      user_id         BIGINT       COMMENT '发起人ID',
      stream_id       VARCHAR(64)  NOT NULL COMMENT '流会话唯一标识(UUID)',
      protocol        VARCHAR(20)  DEFAULT 'WEBRTC' COMMENT '传输协议: WEBRTC/RTMP/HLS/RTSP',
      status          VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/PAUSED/CLOSED',
      bitrate_kbps    INT          COMMENT '实时码率(Kbps)',
      fps             INT          COMMENT '实时帧率',
      resolution      VARCHAR(20)  COMMENT '分辨率: 1920x1080/...',
      viewer_count    INT          DEFAULT 1 COMMENT '当前观看人数',
      started_at      DATETIME     COMMENT '开始时间',
      closed_at       DATETIME     COMMENT '关闭时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_device (device_id),
      INDEX idx_stream (stream_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时流会话表';


  -- ============================================================
  -- 四、消息警告推送
  -- ============================================================

  -- 19. 告警规则表
  CREATE TABLE IF NOT EXISTS alert_rule (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      rule_name       VARCHAR(100) NOT NULL COMMENT '规则名称',
      device_id       BIGINT       COMMENT '适用设备(NULL=全局)',
      event_type      VARCHAR(50)  NOT NULL COMMENT '触发事件类型: STILLNESS/PACING/DANGER_ZONE',
      event_level     VARCHAR(20)  DEFAULT 'WARN' COMMENT '触发事件级别: INFO/WARN/CRITICAL',
      condition_json  JSON         COMMENT '触发条件JSON: {"duration_gt_sec":60, "zone_type":"DANGER", ...}',
      cooldown_sec    INT          DEFAULT 300 COMMENT '冷却时间(秒)，同一事件重复告警间隔',
      enabled         TINYINT      DEFAULT 1 COMMENT '是否启用(1启用 0禁用)',
      priority        INT          DEFAULT 3 COMMENT '优先级(1最高~5最低)',
      created_by      BIGINT       COMMENT '创建人用户ID',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_device_type (device_id, event_type)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

  -- 20. 告警记录表（告警事件历史）
  CREATE TABLE IF NOT EXISTS alert_record (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      rule_id         BIGINT       COMMENT '触发的告警规则ID',
      event_id        BIGINT       NOT NULL COMMENT '关联行为事件ID',
      device_id       BIGINT       NOT NULL COMMENT '关联设备ID',
      event_type      VARCHAR(50)  NOT NULL COMMENT '事件类型',
      event_level     VARCHAR(20)  NOT NULL COMMENT '事件级别',
      title           VARCHAR(200) NOT NULL COMMENT '告警标题',
      content         TEXT         COMMENT '告警内容详情',
      snapshot_url    VARCHAR(500) COMMENT '抓拍图片',
      handle_status   VARCHAR(20)  DEFAULT 'PENDING' COMMENT '处理状态: PENDING/HANDLED/IGNORED/ESCALATED',
      handled_by      BIGINT       COMMENT '处理人用户ID',
      handled_at      DATETIME     COMMENT '处理时间',
      handle_remark   VARCHAR(500) COMMENT '处理备注',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
      INDEX idx_device_time (device_id, created_at),
      INDEX idx_status    (handle_status),
      INDEX idx_event     (event_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

  -- 21. 通知消息模板表
  CREATE TABLE IF NOT EXISTS notification_template (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      template_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '模板编码',
      template_name   VARCHAR(100) NOT NULL COMMENT '模板名称',
      channel_type    VARCHAR(20)  NOT NULL COMMENT '适用渠道: SMS/EMAIL/WECHAT/APP_PUSH/WEBHOOK/IN_APP',
      title_tpl       VARCHAR(200) COMMENT '标题模板(支持变量 {{var}})',
      content_tpl     TEXT         NOT NULL COMMENT '内容模板(支持变量 {{var}})',
      enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息模板表';

  -- 22. 通知渠道配置表
  CREATE TABLE IF NOT EXISTS notification_channel (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      channel_name    VARCHAR(50)  NOT NULL COMMENT '渠道名称',
      channel_type    VARCHAR(20)  NOT NULL COMMENT '渠道类型: SMS/EMAIL/WECHAT/APP_PUSH/WEBHOOK/DINGTALK/FEISHU',
      config_json     JSON         NOT NULL COMMENT '渠道配置JSON（API密钥/Webhook地址/签名等）',
      rate_limit_per_min INT       DEFAULT 10 COMMENT '每分钟发送上限',
      enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道配置表';

  -- 23. 通知推送日志表
  CREATE TABLE IF NOT EXISTS notification_log (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      alert_record_id BIGINT       COMMENT '关联告警记录ID',
      channel_id      BIGINT       NOT NULL COMMENT '通知渠道ID',
      user_id         BIGINT       COMMENT '接收用户ID',
      recipient       VARCHAR(200) NOT NULL COMMENT '接收人地址（手机号/邮箱/openid/设备token）',
      title           VARCHAR(200) COMMENT '推送标题',
      content         TEXT         COMMENT '推送内容',
      send_status     VARCHAR(20)  DEFAULT 'PENDING' COMMENT '发送状态: PENDING/SENDING/SUCCESS/FAILED/RETRY',
      error_message   TEXT         COMMENT '失败原因',
      retry_count     INT          DEFAULT 0 COMMENT '重试次数',
      sent_at         DATETIME     COMMENT '发送成功时间',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      INDEX idx_alert  (alert_record_id),
      INDEX idx_status (send_status),
      INDEX idx_user   (user_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知推送日志表';

  -- 24.

● 继续，后半部分：

  -- 24. 用户订阅表（用户订阅哪些设备/事件的告警通知）
  CREATE TABLE IF NOT EXISTS user_subscription (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      user_id         BIGINT       NOT NULL COMMENT '用户ID',
      device_id       BIGINT       COMMENT '设备ID(NULL=全部设备)',
      event_type      VARCHAR(50)  COMMENT '订阅事件类型(NULL=全部类型)',
      channel_ids     JSON         COMMENT '通知渠道ID列表 [1,2,3]',
      push_interval_sec INT        DEFAULT 0 COMMENT '静默间隔(秒)，0=实时推送',
      enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      INDEX idx_user  (user_id),
      INDEX idx_device (device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅表';

  -- 25. 用户设备关系表（用户与设备的绑定关系）
  CREATE TABLE IF NOT EXISTS user_device_relation (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      user_id         BIGINT       NOT NULL COMMENT '用户ID',
      device_id       BIGINT       NOT NULL COMMENT '设备ID',
      relation_type   VARCHAR(20)  DEFAULT 'OWNER' COMMENT '关系: OWNER/OPERATOR/VIEWER',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      UNIQUE KEY uk_user_device (user_id, device_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设备关系表';

  -- 26. 系统操作日志表
  CREATE TABLE IF NOT EXISTS sys_operation_log (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      user_id         BIGINT       COMMENT '操作用户ID',
      username        VARCHAR(50)  COMMENT '操作用户名',
      module          VARCHAR(50)  COMMENT '操作模块',
      action          VARCHAR(50)  COMMENT '操作动作',
      target_type     VARCHAR(50)  COMMENT '操作对象类型',
      target_id       VARCHAR(100) COMMENT '操作对象ID',
      detail          TEXT         COMMENT '操作详情JSON',
      client_ip       VARCHAR(45)  COMMENT '客户端IP',
      user_agent      VARCHAR(500) COMMENT 'User-Agent',
      cost_ms         INT          COMMENT '耗时(毫秒)',
      status          VARCHAR(20)  DEFAULT 'SUCCESS' COMMENT '结果: SUCCESS/FAIL',
      created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
      INDEX idx_user_time (user_id, created_at),
      INDEX idx_module   (module, action)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';


  -- ============================================================
  -- 数据初始化
  -- ============================================================

  -- 默认管理员账户（密码: 123456）
  INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, role_code, status)
  VALUES (1, 'admin', '$2a$10$gFcFjASQAPmUr8QHeCfBz.rlfDUyhlw7DUhPDXVIYVF7a1DyJ/coe', '超级管理员', 'ADMIN', 1);

  -- 默认通知模板
  INSERT IGNORE INTO notification_template (template_code, template_name, channel_type, title_tpl, content_tpl) VALUES
  ('EVENT_STILLNESS', '宠物静止异常告警', 'IN_APP',
   '【{{eventLevel}}】宠物静止异常告警',
   '设备 {{deviceName}} 检测到宠物持续静止超过 {{durationSec}} 秒，请及时查看。'),
  ('EVENT_PACING', '宠物踱步异常告警', 'IN_APP',
   '【{{eventLevel}}】宠物踱步异常告警',
   '设备 {{deviceName}} 检测到宠物频繁踱步，持续 {{durationSec}} 秒，可能存在焦虑行为。'),
  ('EVENT_DANGER_ZONE', '宠物进入危险区域告警', 'IN_APP',
   '【{{eventLevel}}】宠物进入危险区域告警',
   '设备 {{deviceName}} 检测到宠物进入危险区域 "{{zoneName}}"，请立即处理。'),
  ('DEVICE_OFFLINE', '设备离线通知', 'IN_APP',
   '【WARN】设备离线通知',
   '设备 {{deviceName}} 已离线，最后在线时间 {{lastOnlineTime}}，请检查设备状态。');

  -- 默认通知渠道（站内消息）
  INSERT IGNORE INTO notification_channel (id, channel_name, channel_type, config_json, enabled)
  VALUES (1, '站内消息', 'IN_APP', '{}', 1);

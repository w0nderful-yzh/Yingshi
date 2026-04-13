# GitHub Copilot Instructions for Yingshi

## 项目定位
这是一个 **Spring Boot 3 + Java 17 + Maven** 的后端项目，项目名为 **Yingshi**，包名 **com.yzh.yingshi**。  
目标是实现“宠物异常行为检测系统”的第一阶段 MVP 后端。

第一阶段只做系统闭环，不做模型训练，不做真正的视频推理。  
检测与规则分析由外部分析服务完成，本项目负责：

- 用户登录与鉴权
- 文件上传
- 设备管理
- 区域配置
- 分析任务管理
- 帧检测结果存储与查询
- 异常事件存储与查询
- 任务摘要统计
- 提供 internal 接口接收分析服务回写的数据

## 当前阶段范围
第一阶段只围绕以下 3 类异常：
- `STILLNESS`
- `PACING`
- `DANGER_ZONE`

不要扩展到抓挠识别、情绪识别、健康诊断、语音识别等更复杂能力。

## 技术与依赖偏好
优先使用：
- Spring Boot 3
- Maven
- Java 17
- MyBatis-Plus
- MySQL 8
- JWT
- Spring Validation
- Lombok
- springdoc-openapi

默认不要引入：
- Redis
- Kafka
- RocketMQ
- Elasticsearch
- Docker 配置
- 微服务拆分
- 复杂工作流引擎

除非用户明确要求，否则保持单体后端、最小依赖、最小复杂度。

## 包结构约束
优先采用清晰、稳定的模块化结构：

- `com.yzh.yingshi.common`
  - `api`
  - `config`
  - `enums`
  - `exception`
  - `util`
- `com.yzh.yingshi.entity`
- `com.yzh.yingshi.dto`
- `com.yzh.yingshi.vo`
- `com.yzh.yingshi.mapper`
- `com.yzh.yingshi.modules.auth`
- `com.yzh.yingshi.modules.file`
- `com.yzh.yingshi.modules.device`
- `com.yzh.yingshi.modules.zone`
- `com.yzh.yingshi.modules.task`
- `com.yzh.yingshi.modules.frame`
- `com.yzh.yingshi.modules.event`
- `com.yzh.yingshi.modules.summary`

每个业务模块内部建议包含：
- controller
- service
- service.impl
- dto / vo（若采用模块内细分）
- convert（可选）
- mapper（若采用模块内细分）

但不要把结构做得过深，避免影响开发效率。

## API 设计约束
统一前缀：
- `/api/v1`

统一返回结构：
```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "xxx",
  "timestamp": "2026-04-09T14:35:21+08:00"
}
```

统一原则：
- 成功 `code = 0`
- 失败使用明确业务码
- Controller 中不要直接返回裸对象
- 分页接口统一返回 `total + records`
- 列表查询优先支持基础筛选条件
- 查询接口尽量幂等
- 状态变更接口要检查状态流转是否合法

## 数据库与实体约束
第一阶段核心表：
- `sys_user`
- `video_source`
- `device`
- `zone_config`
- `video_task`
- `frame_result`
- `behavior_event`
- `task_summary`

要求：
- 使用 `LocalDateTime`
- 主键优先使用 `Long`
- 枚举字段用 `String` 存储，保持可读性
- 先不要强依赖外键约束，避免联调阶段受阻
- 必要索引要补上
- 不要为了“规范”写出过于复杂的表关系

## 核心枚举固定值
任务状态：
- `INIT`
- `WAITING`
- `PROCESSING`
- `FINISHED`
- `FAILED`
- `CANCELED`

区域类型：
- `NORMAL`
- `REST`
- `FEED`
- `DANGER`

事件类型：
- `STILLNESS`
- `PACING`
- `DANGER_ZONE`

设备状态：
- `ONLINE`
- `OFFLINE`
- `DISABLED`

视频来源：
- `UPLOAD`
- `RTSP`
- `EZVIZ`

不要随意改名，不要另起一套拼写。

## 鉴权与安全约束
第一阶段只做轻量鉴权：
- 用户登录返回 JWT
- 后续业务接口使用 `Authorization: Bearer <token>`
- `logout` 可以做轻量实现，不强依赖黑名单
- 密码必须使用 BCrypt
- 不要一上来引入 OAuth2、SSO、复杂 RBAC

## 业务边界约束
本项目不是分析引擎本体。  
遇到“视频抽帧、YOLO 检测、行为规则判定”相关需求时，默认做法是：

- 在 Java 后端中保留任务与结果表结构
- 通过 internal API 接收外部分析服务上报
- 不要在当前项目中硬写复杂推理流程
- 不要伪造“AI 已完成”但没有实际数据流的复杂逻辑

可做的最小实现：
- `POST /internal/tasks/{taskId}/frames:batch-report`
- `POST /internal/tasks/{taskId}/events:report`

## 代码风格约束
请始终遵守：
- Controller 薄，Service 承担业务逻辑
- DTO / VO / Entity 分离
- 参数校验用 `@Valid`
- 尽量显式命名，避免模糊缩写
- 公共逻辑下沉到 common
- 不要把所有类堆到一个包
- 不要输出无法编译的半成品代码
- 优先提交“能跑起来”的最小闭环实现
- 避免过度抽象、过度泛型、过度设计模式

## 实现优先级
实现代码时优先顺序如下：
1. `pom.xml` 和基础配置
2. 通用返回体、异常处理、枚举
3. 数据库脚本与实体
4. 登录鉴权
5. 设备管理
6. 区域配置
7. 任务管理
8. internal 上报接口
9. 帧结果 / 事件 / 摘要查询
10. OpenAPI 文档与必要初始化数据

## 建议实现风格
当生成代码时：
- 优先生成完整可运行类，而不是只给片段
- 优先补全最小必要的 import、注解、构造、配置
- 对于新增接口，同时补 DTO、Service、Controller、Mapper
- 对于分页查询，同时给出分页请求对象或必要参数
- 对于枚举与状态，给出统一定义，不要在代码中散落硬编码字符串
- 对于查询和新增接口，尽量给出基础校验和合理异常提示

## 非目标
当前阶段不要默认实现：
- 真正的视频解码和抽帧
- 真正的模型推理
- Redis 缓存
- WebSocket 实时推送
- 多租户
- 审计日志中心
- 分布式任务调度
- 云存储对接
- 前端代码

## 你在生成代码时的思维原则
- 先跑通闭环，再谈扩展
- 先保证字段、接口、状态一致
- 先做稳定 CRUD 和任务流转
- 遇到 AI 相关环节，默认采用“外部服务上报结果”的方式
- 代码要服务于当前阶段开发，而不是想象未来所有版本

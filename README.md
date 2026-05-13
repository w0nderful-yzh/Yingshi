# Yingshi 萤石智宠 — 智能宠物管理系统

基于萤石开放平台 API 和 AI 能力构建的智能宠物管理系统，通过摄像头实时监控宠物活动，利用 AI 算法检测宠物异常行为并自动报警，同时集成大语言模型提供宠物健康建议与智能问答。

## 功能特性

### 已实现

- **用户认证** — JWT 无状态认证，BCrypt 密码加密，角色体系（ADMIN / OPERATOR / VIEWER）
- **设备管理** — 从萤石云同步设备，支持列表筛选、启停用、编辑、删除
- **视频服务** — 直播预览（HLS / FLV / RTMP / EZOPEN）、云录像回放
- **宠物管理** — 宠物档案 CRUD（名称、类型、年龄、性别、头像）
- **安全区域编辑器** — 可视化画布编辑矩形 / 多边形安全区域，百分比坐标适配不同分辨率
- **AI 宠物检测** — 定时截取摄像头画面，调用萤石 AI 宠物检测算法，判断宠物是否在安全区域内，越界自动报警
- **异常行为分析** — 三种异常模式检测：宠物消失、异常活跃、长时间静止，各自独立冷却机制
- **报警管理** — 萤石云端报警 + 本地 AI 检测报警双来源，支持已读标记、筛选、删除
- **AI 宠物助手** — 基于 DeepSeek 大模型的宠物行为分析、健康建议、自由问答

### TODO — 功能需求

- [ ] 设备分组与房间管理
- [ ] 多宠物同时检测与追踪
- [ ] 报警推送通知（WebSocket / 短信 / 邮件）
- [ ] 宠物活动数据统计与可视化图表
- [ ] 宠物喂食 / 用药提醒与日程管理
- [ ] 多用户家庭成员协作与权限细粒度控制
- [ ] C 端设备托管/授权模式 — 用户通过萤石 OAuth 授权页绑定自己的设备，后端用 auth_code 换取托管 token，建立用户-设备权限关系（开发阶段使用 ngrok 穿透实现萤石回调）
      ```
      用户登录系统 → 点击"绑定萤石设备" → 后端生成萤石授权页 URL
        → 用户跳转萤石授权页登录并选择设备
        → 萤石回调后端（携带 auth_code / state / deviceTrustId / deviceSerials）
        → 后端用 auth_code 换取该用户的托管 token
        → 存储用户-托管账号-设备-权限关系到数据库
        → 用户仅可管理自己授权过的设备
      ```
- [ ] 云端录像自动回放关联报警事件
- [ ] 检测算法参数自动调优
- [ ] **AI 优化**
  - [ ] 拆分 system prompt — 行为分析、健康建议、自由问答各自独立 prompt，行为分析要求返回结构化 JSON（状态 / 风险等级 / 建议）
  - [ ] 修正行为分析的误导描述 — 当前 prompt 让 LLM "分析监控截图" 但实际只传了 URL 字符串，LLM 无法访问图片；改为基于检测数据（坐标、轨迹、行为模式）做文本分析，或接入多模态模型传图片 base64
  - [ ] prompt 外部化 — 将 system prompt 从 Java 硬编码移至 application.yml 或数据库，支持热更新无需重新编译
  - [ ] 添加 token 控制 — 配置 maxTokens 限制输出长度；对 `/chat` 和 `/health-advice` 接口添加输入长度校验，防止单次请求打满上下文窗口
  - [ ] LLM 调用超时与重试 — 为 DeepSeek API 调用设置 connect/read timeout；对 429（限流）和 503（服务不可用）做指数退避重试
  - [ ] 接口频率限制 — 为 `/chat` 等 LLM 接口添加频率限制，防止滥用消耗 API 额度
  - [ ] 错误信息脱敏 — `LlmClient.mapError` 当前会把原始异常 message 暴露给前端，需改为返回通用错误提示，内部详情仅写日志
  - [ ] 日志级别调整 — LLM 请求/响应日志当前为 INFO 级别且截断 100 字符，生产环境应降为 DEBUG
- [ ] 国际化（i18n）支持
- [ ] Docker 容器化部署方案
- [ ] CI/CD 流水线配置
- [ ] 完善 `docs/` 目录下的产品说明、部署文档、数据库设计文档

### TODO — 安全加固

- [ ] 敏感配置外部化 — 数据库密码、JWT 密钥、萤石 AppKey/AppSecret 当前明文硬编码在 `application.yml`，需移至环境变量或配置中心
- [ ] JWT 密钥强度提升 — 当前密钥为可读英文短语（36 字节），熵值过低易被暴力破解；应使用 64 字节以上密码学随机密钥
- [ ] 全局异常处理器脱敏 — `GlobalExceptionHandler` 将 `exception.getMessage()` 直接返回给客户端，可能泄露 SQL 错误、内部类名等信息；应返回固定提示，详情仅写服务端日志
- [ ] 生产环境关闭 SQL 日志 — `application.yml` 中 MyBatis-Plus 配置了 `StdOutImpl`，每条 SQL 均打印到 stdout，影响性能且可能泄露敏感数据
- [ ] 生产环境禁用 Swagger — `springdoc.swagger-ui.enabled` 未按环境区分，生产环境暴露全部 API 接口信息
- [ ] 接口数据隔离（IDOR 修复） — `listDevices()`、`listAlarms()`、`countUnread()`、`markAllRead()` 等接口未按当前用户过滤数据，任意用户可访问全部设备和报警；宠物详情 `getById()` 未校验归属权
- [ ] 认证接口限流 — `/api/auth/login` 和 `/api/auth/register` 无限流措施，存在暴力破解和批量注册风险
- [ ] JWT 令牌撤销机制 — `logout()` 为空操作，令牌签发后无法主动失效；考虑引入 Redis 令牌黑名单
- [ ] 注册接口权限控制 — 当前任何人可自行注册账号，生产环境应限制为管理员创建或增加邮箱验证
- [ ] CORS 配置 — `WebMvcConfig` 未配置跨域策略，前后端分离部署时会出问题

### TODO — 性能优化

- [ ] 统一 RestTemplate Bean — 5 处 `new RestTemplate()` 分散在各服务中，无超时配置、无连接池；应注册为 Spring Bean 并配置 connect/read timeout
- [ ] 列表接口分页 — 所有列表接口（设备、报警、检测记录、宠物、检测配置）均无分页，`selectList()` 返回全量数据；`MybatisPlusConfig` 已配置分页插件但从未使用
- [ ] EzvizTokenService 线程安全 — `cachedToken` 和 `expireTime` 为普通字段无同步保护，并发场景下可能重复刷新令牌；应使用 `volatile` 或 `AtomicReference`
- [ ] 定时任务线程池 — `@EnableScheduling` 默认单线程，三个定时任务（检测 30s / 报警同步 60s / 异常分析 60s）互相阻塞；需配置 `SchedulingConfigurer` 多线程池
- [ ] 外部 API 熔断重试 — 萤石 API 调用无重试、无熔断器、无降级，萤石宕机时所有相关接口和定时任务持续报错
- [ ] 报警同步 N+1 查询 — `AlarmServiceImpl.saveIfAbsent()` 对每条报警逐条查重再逐条插入，应改为批量操作
- [ ] 异常分析 N+1 查询 — `PetAbnormalAnalysisServiceImpl.analyzeAll` 对每个检测配置分别查设备、宠物、记录，配置多时数据库压力大；改为批量查询后内存关联
- [ ] 检测记录表增长控制 — `pet_detection_record` 每个配置每天新增约 2880 行，无 TTL / 分区 / 归档策略，长期运行将成性能瓶颈
- [ ] 前端列表无服务端分页 — 所有列表页面使用 Ant Design 客户端分页，API 一次返回全部数据
- [ ] 前端报警轮询优化 — 后台标签页仍在每 30s 轮询未读数，应使用 `document.visibilitychange` 暂停不可见时的请求
- [ ] 前端 SafeZoneCanvas 性能 — `handleMouseMove` 每次鼠标移动触发 Canvas 重绘，应使用 `requestAnimationFrame` 节流
- [ ] 前端路由懒加载 — 所有页面组件在入口文件同步导入，初始包体积过大；应使用 `React.lazy()` 按路由拆分

### TODO — 代码质量

- [ ] 生产环境异常日志规范 — `GlobalExceptionHandler` 使用 `exception.printStackTrace()` 而非日志框架，应改为 `log.error()`
- [ ] 逻辑删除一致性 — `Device` 实体有 `@TableLogic`，但 `syncFromEzviz()` 可能导致已逻辑删除的设备被重新同步复活；`AlarmServiceImpl.listAlarms()` 未过滤已逻辑删除的记录
- [ ] VO 构建方式统一 — 部分手写 setter，无 MapStruct；实体新增字段时容易遗漏映射
- [ ] userId 类型安全 — 多处 `(Long) request.getAttribute("userId")` 强转，无类型保护；若中间件改为 Integer 会抛 ClassCastException
- [ ] 异常检测阈值可配置化 — 移动活跃阈值（10%）和长时间静止阈值（2%）硬编码在代码中，应移入配置表
- [ ] 冷却机制重构 — `isAlarmInCooldown` 用 `LIKE` 匹配 `alarmContent` 字符串，格式变化即失效；应增加专门字段或用 Redis
- [ ] 日期类型规范化 — `AlarmQueryDTO`、`CloudRecordQueryDTO` 等用 String 接收时间参数，无格式校验，运行时才报错
- [ ] 前端消除 `any` 类型 — 所有 API 模块 `request.get<any, T>(...)` 第一个泛参均为 `any`，丧失类型检查
- [ ] 前端错误处理统一 — 部分页面 `.catch(() => [])` 静默吞错，部分用 `message.error()`，无全局 Error Boundary
- [ ] 前端清理死代码 — `App.tsx` 返回 null 未使用；`BusinessCode` 枚举已定义但从未引用；`LivePreviewPage` 残留 `console.log`
- [ ] 前端类型定义一致性 — `PetDetectionRecordVO.inSafeZone` 为 number，`PetDetectionResultVO` 中为 boolean，同一概念类型不一致

### TODO — 架构改进

- [ ] 多环境配置分离 — 仅一个 `application.yml`，无 dev/staging/prod profile，所有环境共享同一配置
- [ ] 分布式锁支持 — 定时任务无分布式锁，多实例部署导致重复检测和重复报警
- [ ] 定时任务协调 — 检测任务（30s）和异常分析任务（60s）无执行顺序保证
- [ ] 跨服务事务管理 — `triggerDetection()` 执行多次 DB 插入但无 `@Transactional`，部分失败会导致数据不一致
- [ ] 自定义健康检查 — 已引入 Actuator 但无萤石连通性、MySQL 连通性等自定义 Health Indicator
- [ ] 前端全局 Error Boundary — 无 React Error Boundary，渲染异常直接白屏
- [ ] 前端实体数据共享 — 宠物列表在 PetAiPage（2 次）、DetectionConfigFormPage、DetectionRecordsPage、PetListPage 各自独立请求，无共享缓存
- [ ] 前端 404 页面 — 不存在的路径静默重定向到首页，用户无感知
- [ ] 前端聊天记录持久化 — ChatTab 切换标签页后聊天记录丢失
- [ ] 数据库复合索引 — `pet_detection_record` 缺少 `(detection_config_id, detect_time)` 复合索引；`alarm_message` 缺少 `(device_id, read_status, alarm_time)` 复合索引
- [ ] 数据库字段约束 — `device.status`、`pet.pet_type` 使用 VARCHAR 无约束，应改为 ENUM 或 CHECK

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.12 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 持久层 |
| MySQL | 8.x | 关系型数据库 |
| Spring AI | 1.0.0 | LLM 集成（OpenAI 兼容协议，当前接入 DeepSeek） |
| JJWT | 0.12.6 | JWT 令牌签发与验证 |
| SpringDoc OpenAPI | 2.6.0 | Swagger API 文档 |
| Spring Security Crypto | — | BCrypt 密码哈希 |
| Lombok | — | 代码简化 |
| Maven | — | 构建工具 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 18.3 | UI 框架 |
| TypeScript | 5.5 | 类型安全 |
| Vite | 5.4 | 构建工具 |
| Ant Design | 5.21 | 组件库（中文 locale） |
| React Router | 6.26 | 客户端路由 |
| Zustand | 4.5 | 状态管理 |
| Axios | 1.7 | HTTP 客户端 |
| Tailwind CSS | 3.4 | 原子化 CSS |
| FLV.js / HLS.js | 1.6 / 1.6.16 | 视频流播放 |

### 外部服务

- **萤石开放平台** — 设备管理、视频直播/回放、云录像、AI 宠物检测算法、截图
- **DeepSeek API** — 大语言模型，用于宠物行为分析与智能问答

## 项目结构

```
Yingshi/
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/yzh/yingshi/
│       ├── YingshiApplication.java       # 启动类
│       ├── common/                       # 公共基础设施
│       │   ├── api/                      #   统一响应 ApiResponse、业务码 BusinessCode
│       │   ├── config/                   #   WebMvcConfig（JWT 拦截器注册）
│       │   ├── exception/                #   BusinessException
│       │   ├── interceptor/              #   JwtAuthInterceptor
│       │   └── util/                     #   JwtUtil
│       ├── config/                       # 萤石 / 宠物检测配置属性类
│       ├── constant/                     # 常量定义
│       ├── controller/                   # 7 个 REST 控制器（41 个接口）
│       ├── dto/                          # 请求 DTO（14 个）
│       ├── entity/                       # 数据库实体（7 个）
│       ├── mapper/                       # MyBatis-Plus Mapper（7 个）
│       ├── service/                      # 业务逻辑层
│       │   ├── impl/                     #   Service 实现（11 个）
│       │   ├── PetDetectTask.java        #   定时宠物检测任务（30s）
│       │   └── PetAbnormalTask.java      #   定时异常行为分析任务（60s）
│       └── vo/                           # 视图对象（13 个）
│
├── frontend/                             # React + TypeScript 前端
│   ├── vite.config.ts                    # Vite 配置（开发代理 /api → :8080）
│   ├── tailwind.config.ts
│   └── src/
│       ├── main.tsx                      # 入口文件
│       ├── router/index.tsx              # 路由定义 + AuthGuard
│       ├── api/                          # API 模块（7 个）
│       ├── types/                        # TypeScript 类型定义
│       ├── utils/                        # 工具函数、常量、格式化
│       ├── store/                        # Zustand 状态管理（auth / app / alarm）
│       ├── layouts/                      # 布局组件（BasicLayout / AuthLayout）
│       ├── pages/                        # 页面组件（16 个）
│       │   ├── login/                    #   登录 / 注册
│       │   ├── dashboard/                #   仪表盘
│       │   ├── device/                   #   设备列表 / 设备详情
│       │   ├── video/                    #   直播预览 / 云录像回放
│       │   ├── pet/                      #   宠物管理
│       │   ├── pet-ai/                   #   AI 宠物助手（聊天 / 行为分析 / 健康建议）
│       │   ├── detection/                #   检测配置 / 安全区域编辑 / 检测记录
│       │   ├── alarm/                    #   报警列表
│       │   └── settings/                 #   用户设置
│       └── components/                   # 可复用组件
│           ├── VideoPlayer/              #   视频播放器（HLS / FLV 自适应）
│           ├── SafeZoneEditor/           #   安全区域画布编辑器
│           └── ...
│
├── docs/                                 # 文档（待补充）
├── API-DOC.md                            # 完整 API 接口文档
└── README.md
```

## 数据库设计

系统共 7 张核心表：

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户（用户名、密码哈希、角色） |
| `device` | 设备信息（萤石设备序列号、通道、状态） |
| `pet` | 宠物档案（名称、类型、年龄、性别） |
| `alarm_message` | 报警消息（来源：萤石云端 / 本地 AI 检测） |
| `pet_detection_config` | 宠物检测配置（关联宠物与设备，设定阈值参数） |
| `pet_safe_zone` | 安全区域（矩形 / 多边形，百分比坐标） |
| `pet_detection_record` | 检测记录（坐标、是否在安全区内、快照、AI 原始结果） |

建表 SQL 位于 `backend/src/main/resources/sql/schema.sql`。

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- 萤石开放平台账号（获取 AppKey / AppSecret）
- DeepSeek API Key（可选，用于 AI 助手功能）

### 后端启动

```bash
cd backend

# 1. 创建数据库并导入表结构
mysql -u root -p < src/main/resources/sql/schema.sql

# 2. 修改数据库连接和萤石 / AI 配置
vim src/main/resources/application.yml

# 3. 启动
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8080`，Swagger 文档地址：`http://localhost:8080/swagger-ui.html`

### 前端启动

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`，自动代理 `/api` 请求到后端。

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin  | 123456 | ADMIN |

## API 文档

完整的接口文档见 [API-DOC.md](./API-DOC.md)，涵盖全部 7 个 Controller、41 个接口的请求/响应格式、错误码及认证说明。

## 架构说明

- **统一响应格式** — 所有接口返回 `ApiResponse<T>`（code / message / data / requestId / timestamp）
- **DTO / VO 分离** — 请求用 DTO 做参数校验，响应用 VO 控制输出字段
- **定时任务驱动检测** — `PetDetectTask`（30s）负责截帧 + AI 检测 + 越界判断，`PetAbnormalTask`（60s）负责异常行为模式分析
- **安全区域判定** — 矩形区域用边界比较，多边形区域用射线法（Ray Casting）判断点是否在区域内
- **报警冷却机制** — 每种异常类型独立冷却时间，避免短时间内重复报警

## 许可证

本项目仅供学习与个人使用。

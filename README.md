# Yingshi 萤石智宠 — 智能宠物管理系统

基于萤石开放平台 API 和 AI 能力构建的智能宠物管理系统，通过摄像头实时监控宠物活动，利用 AI 算法检测宠物异常行为并自动报警，同时集成大语言模型提供宠物健康建议与智能问答。

## 功能特性

- **用户认证** — JWT 无状态认证，BCrypt 密码加密，角色体系（ADMIN / OPERATOR / VIEWER）
- **设备管理** — 从萤石云同步设备，支持列表筛选、启停用、编辑、删除
- **视频服务** — 直播预览（HLS / FLV / RTMP / EZOPEN）、云录像回放
- **宠物管理** — 宠物档案 CRUD（名称、类型、年龄、性别、头像）
- **安全区域编辑器** — 可视化画布编辑矩形 / 多边形安全区域，百分比坐标适配不同分辨率
- **AI 宠物检测** — 定时截取摄像头画面，调用萤石 AI 宠物检测算法，判断宠物是否在安全区域内，越界自动报警
- **异常行为分析** — 三种异常模式检测：宠物消失、异常活跃、长时间静止，各自独立冷却机制
- **报警管理** — 萤石云端报警 + 本地 AI 检测报警双来源，支持已读标记、筛选、删除
- **AI 宠物助手** — 基于 MiMo 多模态模型分析监控图片，使用 DeepSeek 提供健康建议与自由问答

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
- **小米 MiMo API** — 多模态模型，用于监控图片与事件证据分析
- **DeepSeek API** — 大语言模型，用于健康建议、周期总结与智能问答

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- 萤石开放平台账号（获取 AppKey / AppSecret）
- DeepSeek API Key（当前启动配置必需，用于 AI 助手功能）

### 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. clone 项目
git clone <repo-url>
cd Yingshi

# 2. 复制环境变量模板并填入 API Key
cp .env.example .env
vim .env   # 填入 LLM_API_KEY、MIMO_API_KEY 和萤石配置

# 3. 一键构建并启动
docker compose up -d --build
```

启动后访问 `http://localhost`，本地演示编排可使用默认账号登录。

> MySQL 数据库、表结构、默认管理员账号均自动初始化，无需手动操作。

`.env` 可选配置项：

```bash
LLM_API_KEY=sk-xxx              # DeepSeek API Key，AI 功能必须
MIMO_API_KEY=your_mimo_key      # 小米 MiMo Open Platform，多模态图片分析
MYSQL_ROOT_PASSWORD=root123     # MySQL root 密码，默认 root123
MYSQL_PASSWORD=123456           # MySQL 应用账号密码，默认 123456
JWT_SECRET=your_secret_key      # JWT 签名密钥
EZVIZ_APP_KEY=your_app_key      # 萤石 AppKey
EZVIZ_APP_SECRET=your_app_secret # 萤石 AppSecret
```

### 方式二：本地开发启动

#### 后端启动

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

#### 前端启动

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`，自动代理 `/api` 请求到后端。

### 本地演示默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin  | 123456 | ADMIN |

该账号只由 `demo-data.sql` 注入本地开发环境。生产编排不会创建默认账号，公网部署步骤见 [docs/deploy.md](./docs/deploy.md)。

## 角色与权限

当前版本提供三类角色：

- `ADMIN` / `OPERATOR`：拥有业务读写权限
- `VIEWER`：只读角色，只能查看设备、视频、告警和检测结果，不能执行同步、编辑、删除、授权等写操作

系统同时按当前用户绑定的设备做数据隔离，设备、视频、告警、检测记录不会再按全局数据直接返回。

> 自注册用户默认角色为 `OPERATOR`，方便当前 Demo 自助体验。

## 开发说明

- 首次使用前，请先在“绑定萤石设备”完成 OAuth 授权，再执行设备同步
- 前端质量检查：`cd frontend && npm run lint && npm run build`
- 后端最小测试入口：`cd backend && ./mvnw test`
- GitHub Actions 已内置基础 CI，见 `.github/workflows/ci.yml`

### 开发期设备访问开关

为了方便本地摄像头和非萤石回调场景调试，当前后端默认开启了未绑定设备访问兜底：

- 配置项：`app.auth.allow-unbound-device-access=true`
- 配置位置：`backend/src/main/resources/application.yml`
- 当前行为：当用户还没有绑定任何萤石设备时，允许直接访问数据库中已有的设备记录，便于本地联调设备管理、视频、检测和告警功能

生产 profile 已强制关闭该兜底。手动部署时也应将下面配置设为 `false`：

```yml
app:
  auth:
    allow-unbound-device-access: false
```

改回 `false` 后的行为：

- 用户必须先完成萤石设备绑定
- 设备、视频、告警、检测记录将重新严格按 `user_device` 绑定关系做访问控制

### 萤石回调说明

- 已实现：设备托管 OAuth 授权回调 `/api/ezviz/oauth/callback`
- 已实现：云信令实时告警 Webhook `/api/ezviz/webhook`，包含 HMAC-SHA1 验签、时间戳防重放、消息幂等、设备归属校验和用户定向 SSE
- 默认保留每 60 秒轮询作为兜底；确认真实推送稳定后可设置 `ALARM_SYNC_ENABLED=false`
- 公网回调必须配置域名和 HTTPS，具体环境变量、反向代理和验收步骤见 [docs/deploy.md](./docs/deploy.md)

## 项目结构

```
Yingshi/
├── backend/                              # Spring Boot 后端
│   ├── Dockerfile                        # 后端容器镜像
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
│       ├── controller/                   # REST 控制器
│       ├── dto/                          # 请求 DTO
│       ├── entity/                       # 数据库实体
│       ├── mapper/                       # MyBatis-Plus Mapper
│       ├── service/                      # 业务逻辑层
│       └── vo/                           # 视图对象
│
├── frontend/                             # React + TypeScript 前端
│   ├── Dockerfile                        # 前端容器镜像
│   ├── nginx.conf                        # Nginx 配置（SPA + API 代理）
│   ├── vite.config.ts                    # Vite 配置（开发代理 /api → :8080）
│   └── src/
│       ├── main.tsx                      # 入口文件
│       ├── router/index.tsx              # 路由定义 + AuthGuard
│       ├── api/                          # API 模块
│       ├── types/                        # TypeScript 类型定义
│       ├── utils/                        # 工具函数、常量
│       ├── store/                        # Zustand 状态管理
│       ├── layouts/                      # 布局组件
│       ├── pages/                        # 页面组件
│       └── components/                   # 可复用组件
│
├── docker-compose.yml                    # Docker 编排配置
├── docs/                                 # 产品、数据库、部署与答辩文档
├── API-DOC.md                            # 完整 API 接口文档
└── README.md
```

## 数据库设计

系统共 10 张表：

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户（用户名、密码哈希、角色） |
| `device` | 设备信息（萤石设备序列号、通道、状态） |
| `pet` | 宠物档案（名称、类型、年龄、性别） |
| `alarm_message` | 报警消息（来源：萤石云端 / 本地 AI 检测） |
| `pet_detection_config` | 宠物检测配置（关联宠物与设备，设定阈值参数） |
| `pet_safe_zone` | 安全区域（矩形 / 多边形，百分比坐标） |
| `pet_detection_record` | 检测记录（坐标、是否在安全区内、快照、AI 原始结果） |
| `pet_ai_report` | AI 事件分析报告与周期总结 |
| `user_ezviz_account` | 用户萤石 OAuth 授权账户 |
| `user_device` | 用户设备绑定关系 |

建表 SQL 位于 `backend/src/main/resources/sql/schema.sql`。

## 架构说明

- **统一响应格式** — 所有接口返回 `ApiResponse<T>`（code / message / data / requestId / timestamp）
- **DTO / VO 分离** — 请求用 DTO 做参数校验，响应用 VO 控制输出字段
- **定时任务驱动检测** — `PetDetectTask`（30s）负责截帧 + AI 检测 + 越界判断，`PetAbnormalTask`（60s）负责异常行为模式分析
- **安全区域判定** — 矩形区域用边界比较，多边形区域用射线法（Ray Casting）判断点是否在区域内
- **报警冷却机制** — 每种异常类型独立冷却时间，避免短时间内重复报警

## API 文档

完整的接口文档见 [API-DOC.md](./API-DOC.md)，涵盖全部 Controller 的请求/响应格式、错误码及认证说明。

## 答辩准备

答辩前的环境检查、推荐演示主线、外部服务故障预案和常见追问见
[docs/defense-checklist.md](./docs/defense-checklist.md)。

## 许可证

本项目仅供学习与个人使用。

---

## 当前状态与后续规划

当前 Demo 已完成 OAuth 设备绑定、角色与设备级数据隔离、生产 profile、
Swagger 环境隔离、前端路由懒加载、全局渲染错误兜底、基础 CI 与部署文档。

下一阶段优先级：

1. 增加前端组件测试和关键业务端到端测试。
2. 为登录、注册和 AI 接口增加限流，完善 JWT 主动撤销。
3. 为外部 API 增加统一超时、重试、熔断与连接池配置。
4. 为检测记录增加分页、归档和数据增长控制。
5. 增加宠物活动趋势图、消息通知和报警关联录像。
6. 多实例部署时为定时任务增加分布式锁。

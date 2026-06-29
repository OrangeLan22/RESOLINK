# RESOLINK

RESOLINK 是一个智能资源预约管理系统，集成了 AI 助手、飞书开放平台等多种能力，为用户提供智能化的资源预约和管理服务。

## 项目架构

```
RESOLINK/
├── ai-agent/           # AI 代理服务
│   └── resolink-agent/ # 基于 DeepSeek API 的智能助手
└── back_end/           # 后端服务
    └── resolink-server/# 核心业务服务
```

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2.x | 应用框架 |
| Spring Data JPA | 3.2.x | 数据访问层 |
| Spring Data Redis | 3.2.x | Redis 数据访问 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存服务 |

### AI 能力

| 服务 | 说明 |
|------|------|
| DeepSeek API | 提供智能对话和工具调用能力 |
| WebSocket | 支持实时流式响应 |

### 第三方集成

| 平台 | 说明 |
|------|------|
| 飞书开放平台 | 用户认证、消息推送、卡片消息 |

## 核心功能

### 1. AI 智能助手 (`ai-agent/resolink-agent`)

- **智能对话**: 基于 DeepSeek 大语言模型实现自然语言交互
- **工具调用**: 支持多种工具能力，包括：
  - 资源查询 (`ResourceQueryToolService`)
  - 可用性检查 (`AvailabilityCheckToolService`)
  - 预约创建 (`AddAppointmentToolService`)
  - 预约统计 (`AppointmentCountToolService`)
  - 时间服务 (`TimeToolService`)
- **流式响应**: 通过 WebSocket 实现实时消息推送
- **Token 验证**: 集成 JWT 令牌验证机制

### 2. 资源管理 (`back_end/resolink-server`)

- **空间资源管理**: 会议室、工位等空间资源的增删改查
- **实体资源管理**: 设备、器材等实体资源的管理
- **预约管理**: 预约创建、取消、查询、历史记录
- **用户管理**: 用户账号、部门、权限管理
- **飞书集成**: 
  - 飞书 OAuth2 认证登录
  - 飞书消息卡片推送（绑定、解绑、预约成功、取消预约等）

## 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 7.0+

### 配置说明

#### 后端服务配置 (`back_end/resolink-server/src/main/resources/application.yml`)

```yaml
# 数据库配置 - 请根据实际环境修改
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/resolink?useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username    # 替换为你的数据库用户名
    password: your_password    # 替换为你的数据库密码

# JWT配置 - 生产环境请使用安全的密钥
jwt:
  secret-key: "your-secret-key"       # 替换为安全的签名密钥
  expiration-time: 86400000           # Token有效期（毫秒）

# 飞书API配置 - 替换为你的飞书应用信息
lark:
  appId: "your-app-id"                # 替换为你的飞书应用ID
  appSecret: "your-app-secret"        # 替换为你的飞书应用密钥
  redirectUri: "http://localhost:2307/api/lark/authCallback"

# 飞书卡片ID配置 - 替换为你的飞书消息卡片ID
feishu:
  card_id:
    bind: "your-bind-card-id"         # 绑定成功卡片ID
    unbind: "your-unbind-card-id"     # 解绑卡片ID
    appointment: "your-appointment-card-id"  # 预约成功卡片ID
    cancel: "your-cancel-card-id"     # 取消预约卡片ID
    check: "your-check-card-id"       # 待检查卡片ID
    start: "your-start-card-id"       # 资源即将开始卡片ID
```

#### AI 代理配置 (`ai-agent/resolink-agent/src/main/resources/application.yml`)

```yaml
# 后端服务地址
back-end-server:
  url: http://localhost:2307          # 后端服务地址

# DeepSeek API配置 - 替换为你的API密钥
deepseek:
  api:
    url: https://api.deepseek.com/v1/chat/completions
    key: "your-deepseek-api-key"       # 替换为你的DeepSeek API密钥
    model: deepseek-chat
    max-tokens: 2000
    temperature: 0.1

# 数据库配置 - 与后端服务使用同一数据库
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/resolink
    username: your_username            # 替换为你的数据库用户名
    password: your_password            # 替换为你的数据库密码

# Redis配置
spring:
  data:
    redis:
      host: localhost                  # Redis主机地址
      port: 6379                       # Redis端口
      password:                        # Redis密码（如有）
```

### 启动步骤

1. **启动数据库服务**
   ```bash
   # 启动 MySQL
   systemctl start mysql
   
   # 启动 Redis
   systemctl start redis
   ```

2. **启动后端服务**
   ```bash
   cd back_end/resolink-server
   mvn spring-boot:run
   ```
   服务将在 `http://localhost:2307` 启动

3. **启动 AI 代理服务**
   ```bash
   cd ai-agent/resolink-agent
   mvn spring-boot:run
   ```
   服务将在 `http://localhost:2970` 启动

## API 接口

### 后端服务 (`resolink-server`)

| 模块 | 路径 | 说明 |
|------|------|------|
| 登录 | `/api/login` | 用户登录 |
| 飞书认证 | `/api/lark/auth` | 飞书OAuth认证 |
| 预约管理 | `/api/appointment` | 预约CRUD操作 |
| 资源管理 | `/api/resource` | 资源管理 |
| 用户管理 | `/api/account` | 用户账号管理 |
| 历史记录 | `/api/history` | 预约历史 |

### AI 代理服务 (`resolink-agent`)

| 模块 | 路径 | 说明 |
|------|------|------|
| 聊天接口 | `/api/agent/chat` | 同步聊天接口 |
| 流式聊天 | `/api/agent/stream` | 流式响应接口 |
| WebSocket | `/ws/chat` | WebSocket实时聊天 |
| 模型状态 | `/api/agent/status` | 模型状态查询 |

## 项目结构

```
ai-agent/resolink-agent/
├── src/main/java/com/orangelan/resolinkagent/
│   ├── controller/     # REST API 控制层
│   ├── service/        # 业务逻辑层（含工具服务）
│   ├── repository/     # 数据访问层
│   ├── entity/         # 数据库实体
│   ├── dto/            # 数据传输对象
│   ├── config/         # 配置类
│   ├── websocket/      # WebSocket 相关
│   └── interceptor/    # 拦截器
└── src/main/resources/
    ├── application.yml # 配置文件
    └── system-prompt.md # 系统提示词

back_end/resolink-server/
├── src/main/java/com/orangelan/resolinkserver/
│   ├── controller/     # REST API 控制层
│   ├── entity/         # 数据库实体
│   ├── dto/            # 数据传输对象
│   ├── config/         # 配置类（含拦截器）
│   └── ResolinkServerApplication.java
└── src/main/resources/
    └── application.yml # 配置文件
```

## 注意事项

1. **API 密钥安全**: DeepSeek API Key 和飞书应用密钥属于敏感信息，请勿提交到代码仓库
2. **数据库初始化**: 首次启动前请确保已创建数据库 `resolink`，你可以在根目录找到`resolink.sql`数据库文件
3. **Redis 连接**: 确保 Redis 服务正常运行且配置正确
4. **飞书配置**: 需在飞书开放平台正确配置应用权限和回调地址

## 许可证

MIT License

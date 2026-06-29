# WebSocket AI聊天服务

## 概述

本项目已实现WebSocket连接功能，允许客户端通过WebSocket协议直接与AI聊天服务进行实时通信。

## WebSocket端点

- **WebSocket连接地址**: `ws://localhost:8080/ws/chat`
- **HTTP状态检查**: `GET /ws/status`
- **会话信息**: `GET /ws/sessions`

## 消息格式

### 客户端发送消息格式

```json
{
  "type": "CHAT",
  "content": {
    "message": "用户消息内容",
    "session_id": "会话ID（可选）",
    "token": "认证令牌（可选）"
  },
  "timestamp": 1640995200000
}
```

### 服务端响应消息格式

```json
{
  "type": "CHAT",
  "content": {
    "response": "AI回复内容",
    "session_id": "会话ID",
    "is_new": true
  },
  "session_id": "当前会话ID",
  "timestamp": 1640995200000
}
```

### 消息类型

- `CONNECT`: 连接建立消息
- `CHAT`: 聊天消息
- `DISCONNECT`: 断开连接请求
- `ERROR`: 错误消息

## 使用示例

### JavaScript客户端示例

```javascript
// 连接WebSocket
const websocket = new WebSocket('ws://localhost:8080/ws/chat');

// 连接建立
websocket.onopen = function(event) {
    console.log('WebSocket连接已建立');
};

// 接收消息
websocket.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('收到消息:', message);
    
    switch (message.type) {
        case 'CONNECT':
            console.log('连接成功，会话ID:', message.session_id);
            break;
        case 'CHAT':
            console.log('AI回复:', message.content.response);
            break;
        case 'ERROR':
            console.error('错误:', message.content.error);
            break;
    }
};

// 发送聊天消息
function sendChatMessage(messageText) {
    const chatMessage = {
        type: 'CHAT',
        content: {
            message: messageText,
            session_id: '自定义会话ID',
            token: '认证令牌'
        },
        timestamp: Date.now()
    };
    websocket.send(JSON.stringify(chatMessage));
}

// 断开连接
function disconnect() {
    const disconnectMessage = {
        type: 'DISCONNECT',
        session_id: '当前会话ID'
    };
    websocket.send(JSON.stringify(disconnectMessage));
    websocket.close();
}
```

### 测试页面

项目包含一个测试页面，可以通过以下地址访问：

```
http://localhost:8080/websocket-test.html
```

该页面提供了完整的WebSocket连接、聊天和状态监控功能。

## 连接管理

### 检查连接状态

```bash
# 获取WebSocket连接状态
curl http://localhost:8080/ws/status

# 响应示例
{
  "activeConnections": 3,
  "endpoint": "/ws/chat",
  "protocol": "WebSocket",
  "timestamp": 1640995200000
}
```

### 查看活跃会话

```bash
# 获取活跃会话信息
curl http://localhost:8080/ws/sessions

# 响应示例
{
  "totalSessions": 3,
  "sessionIds": ["session1", "session2", "session3"],
  "timestamp": 1640995200000
}
```

## 配置说明

### WebSocket配置

- **允许的来源**: 所有来源（生产环境应配置具体域名）
- **端点路径**: `/ws/chat`
- **会话管理**: 自动管理会话生命周期
- **错误处理**: 完整的错误消息和重连机制

### 跨域配置

项目已配置CORS支持，允许所有来源的跨域请求：

```java
registry.addMapping("/**")
    .allowedOrigins("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(false);
```

## 安全性考虑

1. **生产环境配置**: 在生产环境中，应限制允许的来源域名
2. **认证机制**: 可以通过token验证机制增强安全性
3. **会话管理**: 支持会话隔离和超时管理
4. **错误处理**: 完整的异常处理和错误消息返回

## 故障排除

### 常见问题

1. **连接失败**: 检查服务器是否正常运行，端口是否被占用
2. **消息格式错误**: 确保消息格式符合规范
3. **跨域问题**: 检查CORS配置是否正确
4. **会话管理**: 检查会话ID是否正确传递

### 日志查看

WebSocket连接和消息处理的相关日志可以在应用日志中查看：

```
WebSocket连接建立: session-id
收到WebSocket消息: message from session-id
WebSocket连接关闭: session-id, 状态: status
```

## 扩展功能

当前实现支持以下扩展功能：

- 实时聊天通信
- 会话状态管理
- 错误处理和重连机制
- 连接状态监控
- 多客户端支持

可以根据需要进一步扩展功能，如：
- 消息广播
- 用户认证
- 消息历史记录
- 文件传输支持
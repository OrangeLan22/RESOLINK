package com.orangelan.resolinkagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.orangelan.resolinkagent.websocket.ChatWebSocketHandler;
import com.orangelan.resolinkagent.websocket.WebSocketTokenInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketTokenInterceptor webSocketTokenInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, 
                          WebSocketTokenInterceptor webSocketTokenInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.webSocketTokenInterceptor = webSocketTokenInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(webSocketTokenInterceptor) // 添加token拦截器
                .setAllowedOrigins("*"); // 允许所有来源，生产环境应配置具体域名
    }
}
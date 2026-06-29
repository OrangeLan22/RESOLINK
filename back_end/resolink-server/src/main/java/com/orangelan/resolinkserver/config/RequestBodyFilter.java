package com.orangelan.resolinkserver.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 请求体过滤器
 * 用于读取并保存请求体内容，以便在拦截器和控制器中都能使用
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 仅处理HttpServletRequest
        if (request instanceof HttpServletRequest httpServletRequest) {
            // 如果是POST请求，读取请求体
            if ("POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
                // 创建可重复读取的请求包装器
                RepeatedlyReadRequestWrapper requestWrapper = new RepeatedlyReadRequestWrapper(httpServletRequest);
                // 继续处理请求
                chain.doFilter(requestWrapper, response);
                return;
            }
        }
        // 其他请求直接处理
        chain.doFilter(request, response);
    }
    
    /**
     * 可重复读取的请求包装器
     * 用于保存请求体内容，以便多次读取
     */
    public static class RepeatedlyReadRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        
        private final byte[] body;
        
        public RepeatedlyReadRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            // 读取请求体
            String bodyString = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            body = bodyString.getBytes(StandardCharsets.UTF_8);
        }
        
        /**
         * 获取请求体内容
         * @return 请求体内容
         */
        public String getRequestBody() {
            return new String(body, StandardCharsets.UTF_8);
        }
        
        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ByteArrayServletInputStream(body);
        }
        
        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
        
        /**
         * 字节数组Servlet输入流
         * 用于重复读取请求体
         */
        private static class ByteArrayServletInputStream extends ServletInputStream {
            
            private final ByteArrayInputStream inputStream;
            
            public ByteArrayServletInputStream(byte[] buffer) {
                this.inputStream = new ByteArrayInputStream(buffer);
            }
            
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }
            
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }
            
            @Override
            public boolean isReady() {
                return true;
            }
            
            @Override
            public void setReadListener(ReadListener readListener) {
                // 不支持异步读取
            }
        }
    }
}
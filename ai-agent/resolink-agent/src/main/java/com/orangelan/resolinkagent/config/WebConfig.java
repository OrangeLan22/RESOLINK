package com.orangelan.resolinkagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.interceptor.TokenValidationInterceptor;
import com.orangelan.resolinkagent.service.TokenValidationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;
    
    public WebConfig(TokenValidationService tokenValidationService, ObjectMapper objectMapper) {
        this.tokenValidationService = tokenValidationService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenValidationInterceptor(tokenValidationService, objectMapper))
                .addPathPatterns("/agent/chat");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
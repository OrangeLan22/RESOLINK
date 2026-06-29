package com.orangelan.resolinkserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    private final PublicInfoInterceptor publicInfoInterceptor;
    private final AccountMgmInterceptor accountMgmInterceptor;
    private final ResourceMgmInterceptor resourceMgmInterceptor;
    private final AppointmentInterceptor appointmentInterceptor;
    private final HistoryInterceptor historyInterceptor;
    private final TokenInterceptor tokenInterceptor;
    private final CheckInterceptor checkInterceptor;
    
    public CorsConfig(PublicInfoInterceptor publicInfoInterceptor, AccountMgmInterceptor accountMgmInterceptor, ResourceMgmInterceptor resourceMgmInterceptor, AppointmentInterceptor appointmentInterceptor, HistoryInterceptor historyInterceptor, TokenInterceptor tokenInterceptor, CheckInterceptor checkInterceptor) {
        this.publicInfoInterceptor = publicInfoInterceptor;
        this.accountMgmInterceptor = accountMgmInterceptor;
        this.resourceMgmInterceptor = resourceMgmInterceptor;
        this.appointmentInterceptor = appointmentInterceptor;
        this.historyInterceptor = historyInterceptor;
        this.tokenInterceptor = tokenInterceptor;
        this.checkInterceptor = checkInterceptor;
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*") // 使用allowedOriginPatterns代替allowedOrigins
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置Token验证拦截器，拦截/api/lark下的所有请求
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/lark/**")
                .excludePathPatterns("/api/lark/authCallback") // 排除飞书授权回调请求
                .excludePathPatterns("/api/lark/**/options"); // 排除OPTIONS请求
        
        // 配置公共信息拦截器，拦截/api/public-info下的所有请求
        registry.addInterceptor(publicInfoInterceptor)
                .addPathPatterns("/api/public-info/**")
                .excludePathPatterns("/api/public-info/options"); // 排除OPTIONS请求
        
        // 配置账号管理拦截器，拦截/api/account-mgm下的所有请求
        registry.addInterceptor(accountMgmInterceptor)
                .addPathPatterns("/api/account-mgm/**")
                .excludePathPatterns("/api/account-mgm/options"); // 排除OPTIONS请求
        
        // 配置资源管理拦截器，拦截/api/resource-mgm下的所有请求
        registry.addInterceptor(resourceMgmInterceptor)
                .addPathPatterns("/api/resource-mgm/**")
                .excludePathPatterns("/api/resource-mgm/options"); // 排除OPTIONS请求
        
        // 配置预约管理拦截器，拦截/api/appointment下的所有请求
        registry.addInterceptor(appointmentInterceptor)
                .addPathPatterns("/api/appointment/**")
                .excludePathPatterns("/api/appointment/options"); // 排除OPTIONS请求
        
        // 配置历史记录拦截器，拦截/api/history下的所有请求
        registry.addInterceptor(historyInterceptor)
                .addPathPatterns("/api/history/**")
                .excludePathPatterns("/api/history/options"); // 排除OPTIONS请求
        
        // 配置资源检查拦截器，拦截/api/check下的所有请求
        registry.addInterceptor(checkInterceptor)
                .addPathPatterns("/api/check/**")
                .excludePathPatterns("/api/check/options"); // 排除OPTIONS请求
    }
}
package com.orangelan.resolinkagent.config;

import com.orangelan.resolinkagent.interceptor.RequestCachingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    
    @Bean
    public FilterRegistrationBean<RequestCachingFilter> requestCachingFilterRegistration(RequestCachingFilter requestCachingFilter) {
        FilterRegistrationBean<RequestCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(requestCachingFilter);
        registrationBean.addUrlPatterns("/agent/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
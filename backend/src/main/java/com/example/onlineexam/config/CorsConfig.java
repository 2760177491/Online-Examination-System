package com.example.onlineexam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS配置类
 * 解决跨域资源共享问题
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的来源，包括前端运行的端口
        config.addAllowedOrigin("http://localhost:8081");
        config.addAllowedOrigin("http://127.0.0.1:8081");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        
        // 允许所有HTTP方法
        config.addAllowedMethod("*");
        
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 允许携带认证信息（如cookies）
        config.setAllowCredentials(true);
        
        // 设置预检请求的有效期，单位为秒
        config.setMaxAge(3600L);
        
        // 2. 创建CORS源配置对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);
        
        // 3. 返回CORS过滤器
        return new CorsFilter(source);
    }
}
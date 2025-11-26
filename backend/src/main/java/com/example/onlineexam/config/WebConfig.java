package com.example.onlineexam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器，让Spring Boot能够访问Vue构建后的静态文件
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("file:../frontend/dist/static/");

        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("file:../frontend/dist/");

        // 添加对index.html的映射
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("file:../frontend/dist/");

        // 添加对CSS和JS文件的映射
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .addResourceLocations("file:../frontend/dist/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .addResourceLocations("file:../frontend/dist/js/");
    }
}

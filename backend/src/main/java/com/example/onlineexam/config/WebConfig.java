package com.example.onlineexam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器，让Spring Boot能够访问项目根目录下的HTML文件
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("file:../frontend/")
                .addResourceLocations("file:../frontend/pages/")
                .addResourceLocations("file:./")
                .addResourceLocations("file:E:/IDEA/Online-Examination-System/frontend/")
                .addResourceLocations("file:E:/IDEA/Online-Examination-System/frontend/pages/");
    }
}
package com.example.onlineexam.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class PageController {

    @Value("${app.frontend.url:http://localhost:8081}")
    private String frontendUrl;

    // 处理所有前端路由，在开发环境下重定向到前端开发服务器
    @GetMapping({"/", "/index", "/login", "/register", "/teacher-dashboard", "/student-dashboard", "/exam/**", "/question/**", "/result/**"})
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 构建重定向URL，保持原始路径
        String redirectUrl = frontendUrl + request.getRequestURI();
        if (request.getQueryString() != null) {
            redirectUrl += "?" + request.getQueryString();
        }
        response.sendRedirect(redirectUrl);
    }

    // 处理测试仪表板页面
    @GetMapping("/test-dashboard")
    public String testDashboard() {
        return "forward:/test-dashboard.html";
    }
    
    // 处理API测试页面
    @GetMapping("/api-test")
    public String apiTest() {
        return "forward:/api-test.html";
    }
}
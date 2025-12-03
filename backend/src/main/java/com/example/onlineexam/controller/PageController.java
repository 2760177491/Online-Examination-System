package com.example.onlineexam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 处理所有前端路由，返回Vue应用的index.html
    @GetMapping({"/", "/index", "/login", "/register", "/teacher-dashboard", "/student-dashboard", "/exam/**", "/question/**", "/result/**"})
    public String index() {
        return "forward:/index.html";
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
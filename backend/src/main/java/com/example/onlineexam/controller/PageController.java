package com.example.onlineexam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 处理登录页面请求
    @GetMapping("/login")
    public String login() {
        return "forward:/pages/login.html";
    }

    // 处理首页请求
    @GetMapping({"/", "/index"})
    public String index() {
        return "forward:/index.html";
    }

    // 处理教师仪表板页面
    @GetMapping("/teacher-dashboard")
    public String teacherDashboard() {
        return "forward:/pages/teacher-dashboard.html";
    }

    // 处理学生仪表板页面
    @GetMapping("/student-dashboard")
    public String studentDashboard() {
        return "forward:/pages/student-dashboard.html";
    }

    // 处理测试仪表板页面
    @GetMapping("/test-dashboard")
    public String testDashboard() {
        return "forward:/test-dashboard.html";
    }
}
package com.example.onlineexam.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "在线考试系统后端启动成功！当前时间：" + System.currentTimeMillis();
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\": \"OK\", \"service\": \"Online Exam System\"}";
    }
}
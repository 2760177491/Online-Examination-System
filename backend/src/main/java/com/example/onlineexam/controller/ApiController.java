package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("")
    public ApiResponse apiInfo() {
        return ApiResponse.success("在线考试系统API", "API已就绪，请访问具体端点");
    }
}

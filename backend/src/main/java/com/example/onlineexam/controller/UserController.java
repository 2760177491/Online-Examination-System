package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.LoginRequest;
import com.example.onlineexam.entity.User;
import com.example.onlineexam.entity.UserRole;
import com.example.onlineexam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody User user) {
        try {
            User savedUser = userService.register(user);
            return ApiResponse.success("注册成功", savedUser);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        try {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

            // 验证角色 - 直接比较枚举值
            UserRole loginRole = UserRole.valueOf(loginRequest.getRole());
            if (!user.getRole().equals(loginRole)) {
                return ApiResponse.error("角色选择错误");
            }

            // 保存用户信息到session - 使用枚举的name()方法
            session.setAttribute("currentUser", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userRole", user.getRole().name());

            return ApiResponse.success("登录成功", user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success("退出成功", null);
    }

    @GetMapping("/current")
    public ApiResponse getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            return ApiResponse.success("获取用户信息成功", user);
        }
        return ApiResponse.error("用户未登录");
    }

    /**
     * 教师端：获取学生列表（用于“考试分配”弹窗）
     *
     * 安全说明：
     * - 仅允许 TEACHER 调用
     * - 不返回密码字段（前端也不需要）
     */
    @GetMapping("/students")
    public ApiResponse listStudents(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以获取学生列表");
        }

        // 兼容：不依赖 UserService 新方法，直接拉全量再过滤（数据量小，且更稳）
        List<User> all = userService.getAllUsers();
        List<User> students = all.stream().filter(u -> u != null && u.getRole() == UserRole.STUDENT).toList();

        // 简单脱敏：密码不返回
        for (User u : students) {
            u.setPassword(null);
        }
        return ApiResponse.success("查询成功", students);
    }
}
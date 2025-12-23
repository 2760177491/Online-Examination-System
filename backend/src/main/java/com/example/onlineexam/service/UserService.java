package com.example.onlineexam.service;

import com.example.onlineexam.entity.User;
import com.example.onlineexam.entity.UserRole;
import com.example.onlineexam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User register(User user) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt.get();
        }
        throw new RuntimeException("用户名或密码错误");
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 按角色查询用户列表
     *
     * 中文说明：
     * - 用于教师端“分配考试给学生”时拉取学生列表
     */
    public List<User> listUsersByRole(UserRole role) {
        if (role == null) {
            return List.of();
        }
        return userRepository.findByRole(role);
    }
}
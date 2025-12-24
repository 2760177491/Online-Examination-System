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

    /**
     * 修改用户名
     * @param userId 当前登录用户ID
     * @param newUsername 新用户名
     */
    public User updateUsername(Long userId, String newUsername) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new RuntimeException("新用户名不能为空");
        }
        String u = newUsername.trim();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 如果用户名没变，直接返回
        if (u.equals(user.getUsername())) {
            return user;
        }

        // 唯一性校验
        if (userRepository.findByUsername(u).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        user.setUsername(u);
        return userRepository.save(user);
    }

    /**
     * 修改密码
     * @param userId 当前登录用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public User updatePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new RuntimeException("旧密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("新密码长度至少 6 位");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("旧密码不正确");
        }

        user.setPassword(newPassword);
        return userRepository.save(user);
    }
}
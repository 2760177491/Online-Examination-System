package com.example.onlineexam.repository;

import com.example.onlineexam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // ============================
    // 教师端“分配考试给学生”需要：拉取学生列表
    // ============================
    java.util.List<User> findByRole(com.example.onlineexam.entity.UserRole role);
}
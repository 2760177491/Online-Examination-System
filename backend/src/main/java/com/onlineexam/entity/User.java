package com.onlineexam.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String realName;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role; // STUDENT, TEACHER, ADMIN

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum UserRole {
        STUDENT, TEACHER, ADMIN
    }
}
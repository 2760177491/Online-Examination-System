package com.example.onlineexam.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private String role; // STUDENT, TEACHER, ADMIN

    @Column(name = "create_time")
    private java.time.LocalDateTime createTime;
}
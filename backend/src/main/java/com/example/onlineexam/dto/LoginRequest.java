package com.example.onlineexam.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role; // TEACHER or STUDENT
}
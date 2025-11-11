package com.onlineexam.dto;

import com.onlineexam.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private User user;

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }
}
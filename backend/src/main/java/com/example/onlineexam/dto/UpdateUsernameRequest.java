package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 修改用户名请求
 */
@Data
public class UpdateUsernameRequest {
    private String newUsername;
    private String captchaId;
    private String captchaCode;
}


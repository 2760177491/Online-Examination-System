package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class UpdatePasswordRequest {
    private String oldPassword;
    private String newPassword;
    private String captchaId;
    private String captchaCode;
}


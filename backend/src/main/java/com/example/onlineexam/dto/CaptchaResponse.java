package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 返回给前端的验证码数据（算术题版）
 *
 * 字段说明：
 * - captchaId：提交修改请求时必须带回，用于服务端校验
 * - question：验证码题目，例如："22 + 11 = ?"
 */
@Data
public class CaptchaResponse {
    private String captchaId;
    private String question;

    public CaptchaResponse() {
    }

    public CaptchaResponse(String captchaId, String question) {
        this.captchaId = captchaId;
        this.question = question;
    }
}

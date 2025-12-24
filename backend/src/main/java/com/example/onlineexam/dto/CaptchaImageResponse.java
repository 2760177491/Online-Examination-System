package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 图片验证码响应
 *
 * 中文说明：
 * - captchaId：提交时必须带回
 * - imageBase64：base64 图片（不含 data:image/png;base64, 前缀，前端可自行拼接）
 */
@Data
public class CaptchaImageResponse {
    private String captchaId;
    private String imageBase64;

    public CaptchaImageResponse() {
    }

    public CaptchaImageResponse(String captchaId, String imageBase64) {
        this.captchaId = captchaId;
        this.imageBase64 = imageBase64;
    }
}


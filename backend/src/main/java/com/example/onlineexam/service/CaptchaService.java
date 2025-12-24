package com.example.onlineexam.service;

import com.example.onlineexam.dto.CaptchaImageResponse;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片验证码服务（纯 Java2D 实现，不依赖第三方库）
 *
 * 中文说明：
 * - GET /api/users/captcha 返回 captchaId + imageBase64
 * - 前端直接用 <img src="data:image/png;base64,..."> 展示
 * - 校验时不区分大小写，成功后一次性消费
 */
@Service
public class CaptchaService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(5);

    // 图片尺寸
    private static final int WIDTH = 140;
    private static final int HEIGHT = 48;

    // 验证码长度与字符集（去掉易混淆字符）
    private static final int LEN = 4;
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static class CaptchaItem {
        String answer;
        Instant expireAt;

        CaptchaItem(String answer, Instant expireAt) {
            this.answer = answer;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, CaptchaItem> store = new ConcurrentHashMap<>();

    private String newCaptchaId() {
        return UUID.randomUUID().toString();
    }

    private String randomText() {
        StringBuilder sb = new StringBuilder(LEN);
        for (int i = 0; i < LEN; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成图片验证码（base64）并缓存答案
     */
    public CaptchaImageResponse createImageCaptcha() {
        String captchaId = newCaptchaId();
        String text = randomText();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // 背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线（简单几条即可）
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80)));
                int x1 = RANDOM.nextInt(WIDTH);
                int y1 = RANDOM.nextInt(HEIGHT);
                int x2 = RANDOM.nextInt(WIDTH);
                int y2 = RANDOM.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 文字
            g.setFont(new Font("Arial", Font.BOLD, 32));
            FontMetrics fm = g.getFontMetrics();

            int baseY = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            int x = 10;
            for (int i = 0; i < text.length(); i++) {
                // 每个字符轻微随机颜色/偏移
                g.setColor(new Color(RANDOM.nextInt(120), RANDOM.nextInt(120), RANDOM.nextInt(120)));
                int y = baseY + (RANDOM.nextInt(7) - 3);
                g.drawString(String.valueOf(text.charAt(i)), x, y);
                x += 28;
            }

            // 噪点
            for (int i = 0; i < 80; i++) {
                g.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
                int px = RANDOM.nextInt(WIDTH);
                int py = RANDOM.nextInt(HEIGHT);
                g.fillRect(px, py, 1, 1);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            store.put(captchaId, new CaptchaItem(text, Instant.now().plus(TTL)));
            return new CaptchaImageResponse(captchaId, base64);
        } catch (Exception e) {
            throw new RuntimeException("生成验证码失败");
        } finally {
            g.dispose();
        }
    }

    /**
     * 校验验证码（不区分大小写），成功后一次性消费
     */
    public boolean verifyAndConsume(String captchaId, String input) {
        if (captchaId == null || captchaId.isBlank() || input == null || input.isBlank()) {
            return false;
        }
        CaptchaItem item = store.get(captchaId);
        if (item == null) {
            return false;
        }
        if (item.expireAt.isBefore(Instant.now())) {
            store.remove(captchaId);
            return false;
        }

        boolean ok = item.answer.equalsIgnoreCase(input.trim());
        if (ok) {
            store.remove(captchaId);
        }
        return ok;
    }
}

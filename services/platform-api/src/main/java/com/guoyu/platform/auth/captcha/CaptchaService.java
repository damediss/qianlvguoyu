package com.guoyu.platform.auth.captcha;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CaptchaService {

  private static final int BG_WIDTH = 320;
  private static final int BG_HEIGHT = 160;
  private static final int PIECE_SIZE = 44;
  private static final int BUMP_R = 6;
  private static final int PIECE_IMG_SIZE = PIECE_SIZE + 2 * BUMP_R; // 56
  private static final int TOLERANCE = 5;
  private static final long POS_TTL_SECONDS = 120;
  private static final long TOKEN_TTL_SECONDS = 300;
  private static final int MAX_FAIL_COUNT = 6;

  private static final String KEY_POS = "captcha:pos:";
  private static final String KEY_TOKEN = "captcha:token:";
  private static final String KEY_FAIL = "captcha:fail:";

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom random = new SecureRandom();

  public CaptchaService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  // ─── Generate ──────────────────────────────────────────────

  public CaptchaGenerateVO generate() {
    String captchaId = UUID.randomUUID().toString();

    // random position: x in [80, BG_WIDTH - PIECE_IMG_SIZE - 10], y in [10, BG_HEIGHT - PIECE_IMG_SIZE - 10]
    int targetX = 80 + random.nextInt(BG_WIDTH - PIECE_IMG_SIZE - 90);
    int targetY = 10 + random.nextInt(BG_HEIGHT - PIECE_IMG_SIZE - 20);

    // 1. Draw background (gradient + geometric patterns)
    BufferedImage bgImg = drawBackground();

    // 2. Build puzzle piece shape at (targetX, targetY)
    Shape pieceShape = buildPieceShape(targetX, targetY);

    // 3. Clip puzzle piece from the CLEAN background (before gap overlay)
    BufferedImage sliderImg = new BufferedImage(PIECE_IMG_SIZE, PIECE_IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D sg = sliderImg.createGraphics();
    sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Shape localShape = buildPieceShape(0, 0);
    sg.setClip(localShape);
    sg.drawImage(bgImg, -targetX, -targetY, null);
    sg.setClip(null);
    sg.setColor(new Color(255, 255, 255, 200));
    sg.setStroke(new BasicStroke(2f));
    sg.draw(localShape);
    sg.dispose();

    // 4. Fill gap on background (dark overlay) — drawn AFTER piece extraction
    Graphics2D bgG = bgImg.createGraphics();
    bgG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    bgG.setComposite(AlphaComposite.SrcAtop);
    bgG.setColor(new Color(0, 0, 0, 100));
    bgG.fill(pieceShape);
    bgG.setComposite(AlphaComposite.SrcOver);
    bgG.setColor(new Color(255, 255, 255, 140));
    bgG.setStroke(new BasicStroke(1.5f));
    bgG.draw(pieceShape);
    bgG.dispose();

    // 5. Store correct X in Redis
    redisTemplate.opsForValue().set(
        KEY_POS + captchaId, String.valueOf(targetX), POS_TTL_SECONDS, TimeUnit.SECONDS);

    // 6. Encode to Base64
    String bgBase64 = toDataUrl(bgImg);
    String sliderBase64 = toDataUrl(sliderImg);

    return new CaptchaGenerateVO(captchaId, bgBase64, sliderBase64, targetY);
  }

  // ─── Verify ────────────────────────────────────────────────

  public CaptchaVerifyVO verify(String captchaId, int submittedX) {
    String posKey = KEY_POS + captchaId;
    String xStr = redisTemplate.opsForValue().get(posKey);

    if (xStr == null) {
      return CaptchaVerifyVO.fail("验证已过期，请重新验证");
    }

    int correctX = Integer.parseInt(xStr);

    if (Math.abs(submittedX - correctX) > TOLERANCE) {
      // Track failure count
      String failKey = KEY_FAIL + captchaId;
      Long failCount = redisTemplate.opsForValue().increment(failKey);
      if (failCount != null && failCount == 1) {
        redisTemplate.expire(failKey, POS_TTL_SECONDS, TimeUnit.SECONDS);
      }
      if (failCount != null && failCount >= MAX_FAIL_COUNT) {
        redisTemplate.delete(posKey);
        redisTemplate.delete(failKey);
      }
      return CaptchaVerifyVO.fail("验证失败，请重试");
    }

    // Success — consume position key
    redisTemplate.delete(posKey);
    redisTemplate.delete(KEY_FAIL + captchaId);

    // Generate one-time token
    String captchaToken = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(
        KEY_TOKEN + captchaToken, "verified", TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

    log.info("Captcha verified: captchaId={}", captchaId);
    return CaptchaVerifyVO.ok(captchaToken);
  }

  // ─── Consume token (called during login) ───────────────────

  public void consumeToken(String captchaToken) {
    String tokenKey = KEY_TOKEN + captchaToken;
    Boolean existed = redisTemplate.delete(tokenKey);
    if (existed == null || !existed) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已过期，请重新验证");
    }
  }

  // ─── Image drawing helpers ─────────────────────────────────

  /**
   * Build puzzle piece shape: rounded rectangle body + two semicircle bumps (right & bottom).
   *
   * @param x left of the main body
   * @param y top of the main body
   */
  private Shape buildPieceShape(int x, int y) {
    Area area = new Area(new RoundRectangle2D.Double(x, y, PIECE_SIZE, PIECE_SIZE, 4, 4));
    // Right bump
    area.add(new Area(new Ellipse2D.Double(
        x + PIECE_SIZE - BUMP_R, y + PIECE_SIZE / 2.0 - BUMP_R,
        BUMP_R * 2, BUMP_R * 2)));
    // Bottom bump
    area.add(new Area(new Ellipse2D.Double(
        x + PIECE_SIZE / 2.0 - BUMP_R, y + PIECE_SIZE - BUMP_R,
        BUMP_R * 2, BUMP_R * 2)));
    return area;
  }

  private BufferedImage drawBackground() {
    BufferedImage img = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Gradient fill
    Color c1 = randomPastel();
    Color c2 = randomPastel();
    g.setPaint(new GradientPaint(0, 0, c1, BG_WIDTH, BG_HEIGHT, c2));
    g.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);

    // Random circles
    for (int i = 0; i < 20; i++) {
      g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 35));
      int r = 8 + random.nextInt(50);
      g.fillOval(random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT), r, r);
    }

    // Random rectangles
    for (int i = 0; i < 10; i++) {
      g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 25));
      g.fillRect(random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT),
          10 + random.nextInt(60), 10 + random.nextInt(40));
    }

    // Random lines
    for (int i = 0; i < 12; i++) {
      g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 30));
      g.setStroke(new BasicStroke(1 + random.nextFloat() * 2));
      g.drawLine(random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT),
          random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT));
    }

    g.dispose();
    return img;
  }

  private Color randomPastel() {
    return new Color(
        120 + random.nextInt(136),
        120 + random.nextInt(136),
        120 + random.nextInt(136));
  }

  private String toDataUrl(BufferedImage img) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(img, "png", baos);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to encode captcha image");
    }
  }
}

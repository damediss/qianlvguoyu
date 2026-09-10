package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** OnlyOffice HS256 JWT（无第三方依赖）。 */
public final class OnlyOfficeJwtSupport {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private OnlyOfficeJwtSupport() {}

  public static String signHs256(Map<String, Object> payload, String secret) {
    try {
      String header = b64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
      String body = b64Url(MAPPER.writeValueAsBytes(payload));
      String data = header + "." + body;
      String sig = b64Url(hmac(data, secret));
      return data + "." + sig;
    } catch (Exception ex) {
      throw new IllegalStateException("OnlyOffice JWT sign failed", ex);
    }
  }

  public static Map<String, Object> verifyHs256(String token, String secret) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid JWT");
      }
      String data = parts[0] + "." + parts[1];
      String expected = b64Url(hmac(data, secret));
      if (!constantTimeEquals(expected, parts[2])) {
        throw new IllegalArgumentException("Invalid JWT signature");
      }
      byte[] json = Base64.getUrlDecoder().decode(pad(parts[1]));
      return MAPPER.readValue(json, new TypeReference<>() {});
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid JWT", ex);
    }
  }

  private static byte[] hmac(String data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
  }

  private static String b64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String pad(String s) {
    int mod = s.length() % 4;
    if (mod == 0) {
      return s;
    }
    return s + "====".substring(mod);
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }
    int r = 0;
    for (int i = 0; i < a.length(); i++) {
      r |= a.charAt(i) ^ b.charAt(i);
    }
    return r == 0;
  }
}

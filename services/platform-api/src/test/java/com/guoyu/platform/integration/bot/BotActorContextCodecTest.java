package com.guoyu.platform.integration.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class BotActorContextCodecTest {

  private static final String SECRET = "01234567890123456789012345678901";

  @Test
  void verifiesSignedContextAndPreservesExternalActor() throws Exception {
    String json = "{"
        + "\"tenant_id\":\"1\","
        + "\"bot_id\":\"bot-001\","
        + "\"wecom_user_id\":\"zhangsan\","
        + "\"chat_id\":\"chat-001\","
        + "\"chat_type\":\"private\","
        + "\"channel_session_id\":\"channel-001\","
        + "\"weknora_session_id\":\"session-001\","
        + "\"request_id\":\"req-001\""
        + "}";
    String encoded = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature = HexFormat.of().formatHex(mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8)));

    BotActorContext context = new BotActorContextCodec(new ObjectMapper(), SECRET)
        .decodeAndVerify(encoded, signature);

    assertEquals(1L, context.tenantId());
    assertEquals("zhangsan", context.wecomUserId());
    assertEquals("req-001", context.requestId());
    assertEquals(true, context.isPrivateChat());
  }

  @Test
  void rejectsTamperedContext() throws Exception {
    String json = "{\"tenant_id\":\"1\",\"bot_id\":\"bot\",\"wecom_user_id\":\"user\","
        + "\"chat_id\":\"chat\",\"chat_type\":\"private\",\"channel_session_id\":\"channel\","
        + "\"weknora_session_id\":\"session\",\"request_id\":\"request\"}";
    String encoded = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    assertThrows(BusinessException.class, () -> new BotActorContextCodec(new ObjectMapper(), SECRET)
        .decodeAndVerify(encoded + "x", "00"));
  }
}

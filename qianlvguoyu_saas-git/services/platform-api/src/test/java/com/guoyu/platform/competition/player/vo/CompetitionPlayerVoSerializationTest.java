package com.guoyu.platform.competition.player.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationVerifyReq;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CompetitionPlayerVoSerializationTest {

  private static final long SNOWFLAKE_ID = 2080130297284501506L;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void serializesChallengeIdAsStringForJavaScriptClients() throws Exception {
    var value = new CompetitionPlayerSmsChallengeVO(SNOWFLAKE_ID, LocalDateTime.now().plusMinutes(5));

    var json = objectMapper.readTree(objectMapper.writeValueAsBytes(value));

    assertThat(json.get("challengeId").isTextual()).isTrue();
    assertThat(json.get("challengeId").asText()).isEqualTo(Long.toString(SNOWFLAKE_ID));
  }

  @Test
  void deserializesStringChallengeIdFromH5() throws Exception {
    String json = """
        {
          "entryCode": "event-entry",
          "mobile": "18107708559",
          "challengeId": "2080130297284501506",
          "code": "123456"
        }
        """;

    var request = objectMapper.readValue(json, CompetitionPlayerActivationVerifyReq.class);

    assertThat(request.challengeId()).isEqualTo(SNOWFLAKE_ID);
  }

  @Test
  void serializesOtherPlayerSnowflakeIdsAsStrings() throws Exception {
    var session = new CompetitionPlayerSessionVO(
        SNOWFLAKE_ID, SNOWFLAKE_ID, "token", "Bearer", 7200L);
    var entry = new CompetitionPlayerEntryVO(
        SNOWFLAKE_ID, SNOWFLAKE_ID, "event", "published");

    var sessionJson = objectMapper.readTree(objectMapper.writeValueAsBytes(session));
    var entryJson = objectMapper.readTree(objectMapper.writeValueAsBytes(entry));

    assertThat(sessionJson.get("playerId").isTextual()).isTrue();
    assertThat(sessionJson.get("tenantId").isTextual()).isTrue();
    assertThat(entryJson.get("eventId").isTextual()).isTrue();
  }
}

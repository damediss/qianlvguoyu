package com.guoyu.platform.competition.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompetitionScoreVoSerializationTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void scoreAndRankingSnowflakeIdsAreSerializedAsStrings() throws Exception {
    long id = 2084569567062556674L;
    CompetitionScoreSourceVO source = new CompetitionScoreSourceVO(id, id, id, id, id, id,
        "theory", BigDecimal.TEN, "confirmed", "competition-formal-exam", "ref", id,
        1, 1, null, null, id, null);
    CompetitionRankingEntryVO entry = new CompetitionRankingEntryVO(id, id, id, id,
        BigDecimal.TEN, 1, "10|10|10|10", 0, "completed", 1);

    assertThat(objectMapper.writeValueAsString(source))
        .contains("\"id\":\"2084569567062556674\"")
        .contains("\"scoreRuleId\":\"2084569567062556674\"")
        .contains("\"confirmedBy\":\"2084569567062556674\"");
    assertThat(objectMapper.writeValueAsString(entry))
        .contains("\"rankingSnapshotId\":\"2084569567062556674\"")
        .contains("\"totalScoreSnapshotId\":\"2084569567062556674\"");
  }
}

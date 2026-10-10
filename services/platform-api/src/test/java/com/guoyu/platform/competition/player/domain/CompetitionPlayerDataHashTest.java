package com.guoyu.platform.competition.player.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompetitionPlayerDataHashTest {

  @Test
  void buildsDeterministicSha256LookupHashWithoutConfiguration() {
    CompetitionPlayerDataHash dataHash = new CompetitionPlayerDataHash();

    assertThat(dataHash.lookupHash("13800138000")).hasSize(64)
        .isEqualTo(dataHash.lookupHash("13800138000"));
  }
}

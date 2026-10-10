package com.guoyu.platform.competition.player.security;

import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompetitionPlayerStpConfiguration {

  @Bean("competitionPlayerStpLogic")
  public StpLogic competitionPlayerStpLogic() {
    return new StpLogic("competition-player");
  }
}

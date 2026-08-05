package com.guoyu.platform.competition.exam.security;

import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompetitionExamStpConfiguration {
  @Bean("competitionExamStpLogic")
  public StpLogic competitionExamStpLogic() {
    return new StpLogic("competition-exam");
  }
}

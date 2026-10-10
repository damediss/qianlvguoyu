package com.guoyu.platform.competition.exam.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CompetitionTheoryExamConfiguration {
  @Bean
  public Clock competitionTheoryExamClock() {
    return Clock.systemDefaultZone();
  }
}

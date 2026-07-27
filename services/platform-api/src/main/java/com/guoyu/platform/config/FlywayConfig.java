package com.guoyu.platform.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

  /**
   * Schema-history repair is an explicit maintenance operation, never an application-start side effect.
   * Automatic repair can delete history entries and unexpectedly run unrelated out-of-order migrations.
   */
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> flyway.migrate();
  }
}

package com.guoyu.platform.competition.messaging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CompetitionMessagingProperties.class,
    CompetitionScoringMessagingProperties.class})
public class CompetitionMessagingPropertiesConfiguration {
}

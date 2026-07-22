package com.guoyu.platform.competition.player.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CompetitionPlayerCryptoProperties.class, CompetitionPlayerSmsProperties.class})
public class CompetitionPlayerConfiguration {
}

package com.guoyu.platform.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * 启动前先 repair，清除失败迁移记录并修复 checksum，再执行 migrate。
     * 解决迁移脚本修复后仍因 flyway_schema_history 中 success=0 记录而无法启动的问题。
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}

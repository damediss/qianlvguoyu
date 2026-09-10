package com.guoyu.platform.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallFilter;
import java.util.List;
import javax.sql.DataSource;
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
  public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
    return flyway -> {
      WallFilter wallFilter = findWallFilter(dataSource);
      boolean previousNoneBaseStatementAllow = false;
      if (wallFilter != null) {
        previousNoneBaseStatementAllow = wallFilter.getConfig().isNoneBaseStatementAllow();
        // Repository-owned Flyway migrations may use trusted metadata-guarded PREPARE/EXECUTE
        // statements. Keep this exception scoped to migration startup and restore the runtime
        // SQL-injection protection before the application accepts requests.
        wallFilter.getConfig().setNoneBaseStatementAllow(true);
      }
      try {
        flyway.migrate();
      } finally {
        if (wallFilter != null) {
          wallFilter.getConfig().setNoneBaseStatementAllow(previousNoneBaseStatementAllow);
        }
      }
    };
  }

  private WallFilter findWallFilter(DataSource dataSource) {
    if (!(dataSource instanceof DruidDataSource druidDataSource)) {
      return null;
    }
    List<Filter> filters = druidDataSource.getProxyFilters();
    if (filters == null) {
      return null;
    }
    return filters.stream()
        .filter(WallFilter.class::isInstance)
        .map(WallFilter.class::cast)
        .findFirst()
        .orElse(null);
  }
}

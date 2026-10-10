package com.guoyu.platform.competition.exam.observability;

import com.alibaba.druid.pool.DruidDataSource;
import com.guoyu.platform.competition.exam.config.FormalExamLoadtestIsolationGuard;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Publishes sanitized JDBC host/schema and node identity for load-test isolation gates.
 * Never includes passwords, tokens, or player PII.
 */
@Component
@Profile("loadtest")
public class FormalExamLoadtestInfoContributor implements InfoContributor {

  private final DataSource dataSource;
  private final Environment environment;

  public FormalExamLoadtestInfoContributor(DataSource dataSource, Environment environment) {
    this.dataSource = dataSource;
    this.environment = environment;
  }

  @Override
  public void contribute(Info.Builder builder) {
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("profile", "loadtest");
    detail.put("requiredSchema", FormalExamLoadtestIsolationGuard.REQUIRED_SCHEMA);
    detail.put("nodeId", environment.getProperty("COMPETITION_FORMAL_EXAM_NODE_ID", ""));
    detail.put("redisHost", firstNonBlank(
        environment.getProperty("spring.data.redis.host"),
        environment.getProperty("REDIS_HOST")));
    detail.put("rabbitHost", firstNonBlank(
        environment.getProperty("spring.rabbitmq.host"),
        environment.getProperty("RABBITMQ_HOST")));
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData meta = connection.getMetaData();
      String url = meta == null ? "" : meta.getURL();
      String catalog = connection.getCatalog();
      detail.put("jdbcUrlSanitized", FormalExamLoadtestIsolationGuard.sanitizeJdbcUrl(url));
      detail.put("jdbcHost", FormalExamLoadtestIsolationGuard.hostName(url));
      detail.put("jdbcSchema", catalog == null || catalog.isBlank()
          ? FormalExamLoadtestIsolationGuard.schemaName(url)
          : catalog);
      detail.put("jdbcUser", meta == null ? "" : meta.getUserName());
    } catch (Exception ex) {
      detail.put("jdbcError", ex.getClass().getSimpleName());
    }
    if (dataSource instanceof DruidDataSource druid) {
      detail.put("druidMaxActive", druid.getMaxActive());
      detail.put("druidActiveCount", druid.getActiveCount());
      detail.put("druidPoolingCount", druid.getPoolingCount());
      detail.put("druidWaitThreadCount", druid.getWaitThreadCount());
    }
    builder.withDetail("formalExamLoadtest", detail);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}

package com.guoyu.platform.competition.exam.config;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * Fail fast when the loadtest profile would still point at a shared or production database.
 * Runs before Flyway so a misconfigured process cannot migrate or write the SaaS schema.
 */
public class FormalExamLoadtestIsolationGuard implements EnvironmentPostProcessor {

  public static final String REQUIRED_SCHEMA = "gjjddb_load";
  public static final String[] BLOCKED_FRAGMENTS = {
      "43.138.163.7",
      "gjjddb2026",
      "gjjddbpro"
  };

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (environment == null || !environment.acceptsProfiles(Profiles.of("loadtest"))) {
      return;
    }
    String url = firstNonBlank(
        environment.getProperty("MYSQL_URL"),
        environment.getProperty("spring.datasource.url"));
    assertJdbcUrl(url);
  }

  public static void assertJdbcUrl(String jdbcUrl) {
    if (!StringUtils.hasText(jdbcUrl)) {
      throw new IllegalStateException(
          "SPRING_PROFILES_ACTIVE=loadtest requires MYSQL_URL (or spring.datasource.url) "
              + "pointing at isolated schema " + REQUIRED_SCHEMA);
    }
    String normalized = jdbcUrl.toLowerCase(Locale.ROOT);
    for (String fragment : BLOCKED_FRAGMENTS) {
      if (normalized.contains(fragment.toLowerCase(Locale.ROOT))) {
        throw new IllegalStateException(
            "loadtest refused shared/prod JDBC fragment '" + fragment + "'; "
                + "sanitizedUrl=" + sanitizeJdbcUrl(jdbcUrl));
      }
    }
    String schema = schemaName(jdbcUrl);
    if (!REQUIRED_SCHEMA.equalsIgnoreCase(schema)) {
      throw new IllegalStateException(
          "loadtest schema must be " + REQUIRED_SCHEMA + ", got '" + schema
              + "'; sanitizedUrl=" + sanitizeJdbcUrl(jdbcUrl));
    }
  }

  public static boolean containsBlockedFragment(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    return Arrays.stream(BLOCKED_FRAGMENTS)
        .anyMatch(fragment -> normalized.contains(fragment.toLowerCase(Locale.ROOT)));
  }

  public static String schemaName(String jdbcUrl) {
    if (!StringUtils.hasText(jdbcUrl)) {
      return "";
    }
    String withoutPrefix = jdbcUrl;
    int scheme = withoutPrefix.indexOf("://");
    if (scheme >= 0) {
      withoutPrefix = withoutPrefix.substring(scheme + 3);
    }
    int slash = withoutPrefix.indexOf('/');
    if (slash < 0 || slash == withoutPrefix.length() - 1) {
      return "";
    }
    String afterHost = withoutPrefix.substring(slash + 1);
    int query = afterHost.indexOf('?');
    String schema = query >= 0 ? afterHost.substring(0, query) : afterHost;
    int extraSlash = schema.indexOf('/');
    if (extraSlash >= 0) {
      schema = schema.substring(0, extraSlash);
    }
    return schema.trim();
  }

  public static String hostName(String jdbcUrl) {
    if (!StringUtils.hasText(jdbcUrl)) {
      return "";
    }
    String withoutPrefix = jdbcUrl;
    int scheme = withoutPrefix.indexOf("://");
    if (scheme >= 0) {
      withoutPrefix = withoutPrefix.substring(scheme + 3);
    }
    int slash = withoutPrefix.indexOf('/');
    String hostPort = slash >= 0 ? withoutPrefix.substring(0, slash) : withoutPrefix;
    int at = hostPort.lastIndexOf('@');
    if (at >= 0) {
      hostPort = hostPort.substring(at + 1);
    }
    int comma = hostPort.indexOf(',');
    if (comma >= 0) {
      hostPort = hostPort.substring(0, comma);
    }
    int colon = hostPort.lastIndexOf(':');
    if (colon >= 0 && !hostPort.startsWith("[")) {
      hostPort = hostPort.substring(0, colon);
    }
    return hostPort.trim();
  }

  public static String sanitizeJdbcUrl(String jdbcUrl) {
    if (!StringUtils.hasText(jdbcUrl)) {
      return "";
    }
    return jdbcUrl.replaceAll("(?i)(password=)[^&;]*", "$1***");
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return "";
  }
}

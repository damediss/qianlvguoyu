package com.guoyu.platform.competition.exam.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * When the hotspot Vite proxy is enabled, expose {@code /formal-exam} at the HTTP root so a Pad
 * can load H5 from {@code :8080} while API stays at {@code /api/**}.
 */
public class FormalExamHotspotEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String target = firstNonBlank(
        environment.getProperty("formal-exam.hotspot-vite-proxy-target"),
        System.getProperty("formal-exam.hotspot-vite-proxy-target"),
        System.getenv("FORMAL_EXAM_HOTSPOT_VITE_PROXY_TARGET"));
    if (!StringUtils.hasText(target)) {
      return;
    }
    Map<String, Object> properties = new HashMap<>();
    properties.put("server.servlet.context-path", "/");
    properties.put("spring.mvc.servlet.path", "/api");
    environment.getPropertySources().addFirst(
        new MapPropertySource("formalExamHotspotViteProxy", properties));
    System.err.println("Formal exam hotspot Vite proxy: context-path=/ servlet.path=/api target=" + target.trim());
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

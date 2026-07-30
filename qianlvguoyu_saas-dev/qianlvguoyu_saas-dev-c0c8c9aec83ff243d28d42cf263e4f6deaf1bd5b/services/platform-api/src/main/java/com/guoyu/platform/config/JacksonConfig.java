package com.guoyu.platform.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
    return builder -> builder.serializers(new LocalDateTimeSerializer(FORMATTER));
  }

  /** Avoid JavaScript Number precision loss for MyBatis-Plus snowflake IDs. */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
    return builder -> builder
        .serializerByType(Long.class, ToStringSerializer.instance)
        .serializerByType(long.class, ToStringSerializer.instance);
  }
}

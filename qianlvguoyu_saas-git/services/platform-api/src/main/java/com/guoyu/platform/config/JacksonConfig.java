package com.guoyu.platform.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
    return builder -> {
      builder.serializers(new LocalDateTimeSerializer(FORMATTER));
      builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<>() {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
          String value = parser.getValueAsString();
          if (value == null || value.isBlank()) {
            return null;
          }
          return parseLocalDateTime(value.trim(), context);
        }
      });
    };
  }

  private LocalDateTime parseLocalDateTime(String value, DeserializationContext context) throws IOException {
    try {
      return LocalDateTime.parse(value, FORMATTER);
    } catch (DateTimeParseException ignored) {
      // Continue with ISO formats used by browsers and Android bridges.
    }
    try {
      return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException ignored) {
      // Continue with offset-aware ISO formats.
    }
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (DateTimeParseException ex) {
      throw context.weirdStringException(
          value,
          LocalDateTime.class,
          "Expected yyyy-MM-dd HH:mm:ss or ISO datetime"
      );
    }
  }
}

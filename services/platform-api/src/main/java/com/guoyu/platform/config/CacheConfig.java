package com.guoyu.platform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * 带 JSR310（LocalDateTime 等）支持 + 默认类型标记的 JSON 序列化器。
   *
   * <p>关键点（踩过的两个坑，见 BUG-2026-06-17-001）：
   * <ol>
   *   <li>必须显式 register {@link JavaTimeModule}，否则 VO 里的
   *       {@code LocalDateTime}/{@code LocalDate} 字段写缓存就抛 SerializationException</li>
   *   <li>必须显式 {@code activateDefaultTyping(... As.PROPERTY)}，
   *       {@link GenericJackson2JsonRedisSerializer#GenericJackson2JsonRedisSerializer(ObjectMapper)}
   *       构造器不会自动加 default typing，不加的话 read 时拿不到目标类型</li>
   * </ol>
   */
  private static GenericJackson2JsonRedisSerializer jsonSerializer() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Object.class)
        .build();
    mapper.activateDefaultTyping(ptv,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);

    return new GenericJackson2JsonRedisSerializer(mapper);
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(jsonSerializer()))
        .disableCachingNullValues()
        .prefixCacheNameWith("saas:");

    return RedisCacheManager.builder(cf)
        .cacheDefaults(base)
        .withCacheConfiguration("menuTree", base.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("workTypeTree", base.entryTtl(Duration.ofHours(1)))
        .build();
  }
}

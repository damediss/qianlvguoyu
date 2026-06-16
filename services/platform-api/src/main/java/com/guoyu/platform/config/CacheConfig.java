package com.guoyu.platform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
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
   * GenericJackson2JsonRedisSerializer 的无参构造内部 ObjectMapper 不带 JavaTimeModule，
   * 缓存含 java.time 字段的 VO（如 ExamWorkTypeVO.createTime）时会抛 SerializationException。
   */
  private static GenericJackson2JsonRedisSerializer jsonSerializer() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
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

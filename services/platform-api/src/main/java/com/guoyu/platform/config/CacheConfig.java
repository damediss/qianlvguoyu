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
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

  /**
   * 带 JSR310（LocalDateTime 等）支持 + 默认类型标记的 JSON 序列化器。
   *
   * <p>关键点（踩过的四个坑）：
   * <ol>
   *   <li>必须显式 register {@link JavaTimeModule}，否则 VO 里的
   *       {@code LocalDateTime}/{@code LocalDate} 字段写缓存就抛 SerializationException</li>
   *   <li>必须显式 {@code activateDefaultTyping(... As.WRAPPER_ARRAY)}，
   *       {@link GenericJackson2JsonRedisSerializer#GenericJackson2JsonRedisSerializer(ObjectMapper)}
   *       构造器不会自动加 default typing，不加的话 read 时拿不到目标类型</li>
   *   <li><b>必须用 {@code As.WRAPPER_ARRAY} 而不是 {@code As.PROPERTY}！</b>
   *       BUG-2026-06-18-002 验证：{@link GenericJackson2JsonRedisSerializer#deserialize}
   *       内部调 {@code ObjectMapper.readValue(bytes, Object.class)}，Jackson
   *       对 {@code Object.class} 作为 root 类型会走 polymorphic 路径，期望 type id
   *       在顶层 {@code [typeId, ...]}（即 WRAPPER_ARRAY 格式）。如果配成
   *       {@code As.PROPERTY}（{@code @class} 在 object 内部字段），写时是
   *       {@code [{"@class":...,...}]}，读时 Jackson 把根 array 当成
   *       {@code AsArrayTypeDeserializer} 处理，期望 {@code [typeId, {...}]}，
   *       但读到的是 {@code {{...} （START_OBJECT），报 "Unexpected token
   *       (START_OBJECT), expected VALUE_STRING"。</li>
   *   <li><b>{@code DefaultTyping.NON_FINAL} 不会给 final collection 加 type id!</b>
   *       BUG-2026-06-18-003 验证：{@code Stream.toList()} 返回
   *       {@code java.util.ImmutableCollections$ListN}（继承自 final 类
   *       {@code AbstractImmutableList}）；{@code List.of()} / {@code Map.of()} /
   *       {@code Set.of()} 同理。如果 cacheable 方法返回这些 immutable collection，
   *       Jackson 写出来是 {@code [[item1], [item2], ...]}（顶层 array 没 type id），
   *       read 时 polymorphic 反序列化找不到 type id，报
   *       "Unexpected token (START_ARRAY), expected VALUE_STRING"。
   *       <b>约束：所有 {@code @Cacheable} 方法的返回值必须是 mutable collection
   *       （如 {@code ArrayList}/{@code HashMap}/{@code HashSet}）</b>，推荐用
   *       {@code stream.collect(Collectors.toCollection(ArrayList::new))} 替代
   *       {@code stream.toList()}。把 DefaultTyping 改成 {@code EVERYTHING_BUT_PRIMITIVES}
   *       可以修这个问题，但会把 {@code String}/{@code Integer} 也 wrap 成
   *       {@code ["String", "value"]}，Redis 数据膨胀 ~2x，Map<String,?> 的 key 也全膨胀，
   *       所以不推荐。</li>
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
    // 关键：WRAPPER_ARRAY，不能是 PROPERTY
    mapper.activateDefaultTyping(ptv,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.WRAPPER_ARRAY);

    return new GenericJackson2JsonRedisSerializer(mapper);
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    RedisSerializer<Object> valueSerializer = jsonSerializer();

    RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(valueSerializer))
        .disableCachingNullValues()
        .prefixCacheNameWith("saas:");

    RedisCacheManager cm = RedisCacheManager.builder(cf)
        .cacheDefaults(base)
        .withCacheConfiguration("menuTree", base.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("workTypeTree", base.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("questionBankTree", base.entryTtl(Duration.ofMinutes(30)))
        .build();

    log.info("[CacheConfig] menuTree TTL=30min, workTypeTree TTL=1h, questionBankTree TTL=30min, 其他 cache TTL=10min, "
        + "key prefix=saas:, default typing=WRAPPER_ARRAY (NON_FINAL)。"
        + "约束:@Cacheable 返回值必须是 mutable collection（ArrayList/HashMap/HashSet），"
        + "不能用 Stream.toList() / List.of() / Map.of() / Set.of()（final 类不会被 wrap,read 会炸）。");
    return cm;
  }
}
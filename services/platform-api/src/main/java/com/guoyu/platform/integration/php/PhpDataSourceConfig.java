package com.guoyu.platform.integration.php;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PHP 库（gjjddb）只读第二数据源配置 —— 供三表同步任务直读
 * platform_user / platform_user_token / platform_user_detail。
 *
 * <p><strong>多数据源两道防线（2026-08-04 两次冒烟事故换来的教训）</strong>：
 * <ol>
 *   <li>严禁注册 {@code DataSource} 类型的 Bean —— 会令 DataSourceAutoConfiguration 退让，
 *       主数据源消失，Flyway/主库指向 PHP 库（详见 {@link PhpDataSourceHolder} 类注释）；</li>
 *   <li>注册了第二 JdbcTemplate 后，JdbcTemplateAutoConfiguration 同样退让，
 *       导致未标注 {@code @Qualifier} 的 JdbcTemplate 注入点全部拿到 PHP 模板 ——
 *       因此必须显式提供 {@code @Primary} 的主 JdbcTemplate Bean 钉住默认注入语义。</li>
 * </ol>
 */
@Configuration
public class PhpDataSourceConfig {

  /** 主库（SaaS）JdbcTemplate：显式 @Primary，防止 phpJdbcTemplate 存在期间默认注入漂移 */
  @Bean
  @Primary
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean(destroyMethod = "close")
  public PhpDataSourceHolder phpDataSourceHolder(
      @Value("${platform.sync.php.url:}") String url,
      @Value("${platform.sync.php.username:}") String username,
      @Value("${platform.sync.php.password:}") String password,
      @Value("${platform.sync.php.max-active:3}") int maxActive,
      @Value("${platform.sync.php.max-wait-ms:3000}") long maxWaitMs,
      @Value("${platform.sync.php.query-timeout-seconds:10}") int queryTimeoutSeconds) {
    return new PhpDataSourceHolder(url, username, password, maxActive, maxWaitMs, queryTimeoutSeconds);
  }

  @Bean(name = "phpJdbcTemplate")
  public JdbcTemplate phpJdbcTemplate(PhpDataSourceHolder phpDataSourceHolder) {
    return phpDataSourceHolder.getJdbcTemplate();
  }
}

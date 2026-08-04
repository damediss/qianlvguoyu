package com.guoyu.platform.integration.php;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PHP 库（gjjddb）只读数据源持有器。
 *
 * <p><strong>为什么不是 {@code DataSource} 类型的 Bean</strong>：一旦向容器注册任何 DataSource Bean，
 * Spring Boot 的 DataSourceAutoConfiguration 会整体退让，主数据源（SaaS 库）随之消失，
 * Flyway/MyBatis/JdbcTemplate 会错误地指向 PHP 库（2026-08-04 冒烟事故：Flyway 差点把全量
 * 迁移打到 PHP 生产库）。本持有器内部自建 DruidDataSource 但不以 DataSource 类型暴露，
 * 主数据源自动配置完全不受影响；{@code close()} 由 Bean 生命周期（destroyMethod）回调。
 *
 * <p>《手机端调用SaaS鉴权方案》§3.5.3④：专用账号、小连接池、超时必配、断线自动重连。
 * 本数据源<strong>不挂载 MyBatis-Plus 多租户拦截器</strong>（PHP 库 tenant_id=10159 旧口径，
 * 且三表本身无 tenant_id 列），仅以 JdbcTemplate 直读；后续《AI同步学》的 PHP 业务表查询层复用本数据源。
 */
public class PhpDataSourceHolder implements AutoCloseable {

  private final DruidDataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public PhpDataSourceHolder(String url, String username, String password,
      int maxActive, long maxWaitMs, int queryTimeoutSeconds) {
    this.dataSource = new DruidDataSource();
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    // 小连接池：同步任务每周期仅几条 SQL，2~3 连接足够
    dataSource.setInitialSize(0);
    dataSource.setMinIdle(0);
    dataSource.setMaxActive(maxActive);
    dataSource.setMaxWait(maxWaitMs);
    // 跨公网读取：借出必校验，宁可慢不可拿断线连接；断线后靠 testWhileIdle 重建
    dataSource.setValidationQuery("SELECT 1");
    dataSource.setTestOnBorrow(true);
    dataSource.setTestWhileIdle(true);
    dataSource.setTimeBetweenEvictionRunsMillis(60_000);
    dataSource.setMinEvictableIdleTimeMillis(300_000);

    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
  }

  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  @Override
  public void close() {
    dataSource.close();
  }
}

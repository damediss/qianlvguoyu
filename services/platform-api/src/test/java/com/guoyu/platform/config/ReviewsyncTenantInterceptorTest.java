package com.guoyu.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.guoyu.platform.common.tenant.TenantContextHolder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 单库化 §7.1 拦截器行为断言：主数据源查询对 AI 同步学新依赖的 8 张合并表
 * 自动拼 tenant_id=10159（不在 ignoreTable 白名单）；白名单表不拼。
 * 直接使用生产 MybatisPlusConfig 的真实拦截器链（parserSingle 为 beforeQuery 的实际解析入口）。
 */
class ReviewsyncTenantInterceptorTest {

  private TenantLineInnerInterceptor tenantInterceptor;

  @BeforeEach
  void setUp() {
    MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();
    tenantInterceptor = interceptor.getInterceptors().stream()
        .filter(i -> i instanceof TenantLineInnerInterceptor)
        .map(TenantLineInnerInterceptor.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("TenantLineInnerInterceptor 未注册"));
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  private String parse(String sql) {
    return tenantInterceptor.parserSingle(sql, null);
  }

  /** §4-B 新查询层依赖的 8 张合并表：必须全部被注入 tenant_id=10159（即不在白名单） */
  @Test
  void reviewSyncTablesGetTenantConditionInjected() {
    String[] queries = {
        "SELECT id, title, source FROM train_exam_list WHERE id = 440",
        "SELECT id, exam_id, work_type_id FROM train_exam_work_type_list WHERE exam_id = 440",
        "SELECT id, exam_id, platform_user_id FROM train_exam_people_list WHERE exam_id = 440",
        "SELECT id, work_type_id, level FROM exam_course WHERE work_type_id = 10",
        "SELECT id, parent_id, title FROM exam_knowledge_structure WHERE course_id = 55",
        "SELECT id, course_id, type FROM exam_knowledge_rate WHERE course_id = 55",
        "SELECT id, knowledge_rate_id, rate FROM exam_knowledge_rate_detail WHERE knowledge_rate_id = 5",
        "SELECT id, knowledge_id, type FROM exam_exam_detail WHERE final_check_status = 1"
    };
    for (String q : queries) {
      String parsed = parse(q);
      assertThat(parsed).as("应注入租户条件: %s", q).contains("tenant_id = 10159");
      assertThat(parsed).as("不应出现旧租户: %s", q).doesNotContain("tenant_id = 1 ").doesNotContain("tenant_id = 1)");
    }
  }

  /** §6.2 白名单表（PHP schema 无 tenant_id 列）：拦截器不得拼条件 */
  @Test
  void whitelistTablesNotInjected() {
    assertThat(parse("SELECT id, user_id FROM platform_user_token WHERE token = 'x'"))
        .doesNotContain("tenant_id");
    assertThat(parse("SELECT id, realname FROM platform_user WHERE idcard = 'x' LIMIT 1"))
        .doesNotContain("tenant_id");
    assertThat(parse("SELECT user_id, phone FROM platform_user_detail LIMIT 1"))
        .doesNotContain("tenant_id");
  }

  /** Worker/调度线程未 set 时：getTenantId() 回落 DEFAULT_TENANT_ID=10159（坑 #15/#17 语义） */
  @Test
  void unsetThreadContextFallsBackToDefault() {
    assertThat(TenantContextHolder.getTenantId()).isNull();
    assertThat(parse("SELECT id FROM train_exam_list WHERE id = 440"))
        .contains("tenant_id = 10159");
  }

  /** 若上游漏网 set 了旧租户 1：拦截器忠实按 ThreadLocal 拼——证明 TenantContextFilter 强制覆盖是唯一防线 */
  @Test
  void legacyTenantValueIsInjectedAsIs() {
    TenantContextHolder.setTenantId(1L);
    assertThat(parse("SELECT id FROM train_exam_list WHERE id = 440"))
        .endsWith("tenant_id = 1");
  }
}

package com.guoyu.platform.config;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MybatisPlusConfig {

  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
      @Override
      public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
          tenantId = TenantContextHolder.DEFAULT_TENANT_ID;
        }
        return new LongValue(tenantId);
      }

      @Override
      public String getTenantIdColumn() {
        return "tenant_id";
      }

      @Override
      public boolean ignoreTable(String tableName) {
        return "sys_tenant".equalsIgnoreCase(tableName)
            || "flyway_schema_history".equalsIgnoreCase(tableName)
            || "course_kejian".equalsIgnoreCase(tableName)
            || "train_info_class".equalsIgnoreCase(tableName)
            || "train_base_data".equalsIgnoreCase(tableName)
            || "train_task_log".equalsIgnoreCase(tableName)
            || "train_sign_up_self".equalsIgnoreCase(tableName)
            || "train_sign_up_people_self".equalsIgnoreCase(tableName)
            || "train_people_list".equalsIgnoreCase(tableName)
            // 工种目录按 id 反查导出；勿按当前租户过滤导致 title 查空
            || "exam_work_type".equalsIgnoreCase(tableName)
            || "platform_user_detail".equalsIgnoreCase(tableName)
            || "platform_user".equalsIgnoreCase(tableName)
            || "platform_user_token".equalsIgnoreCase(tableName)
            || "platform_sms".equalsIgnoreCase(tableName)
            || "cms_article".equalsIgnoreCase(tableName)
            || "cms_article_category".equalsIgnoreCase(tableName)
            || "cms_article_content".equalsIgnoreCase(tableName)
            || "sys_index_swiper".equalsIgnoreCase(tableName)
            // platform-shared subsidy pack recipes (tenant_id=0 seeds + tenant overlays)
            || "train_subsidy_pack_profile".equalsIgnoreCase(tableName)
            || "train_subsidy_pack_slot".equalsIgnoreCase(tableName)
            // 多级模板库：platform tenant_id 可空，由业务 SQL 自行过滤可见范围
            || "train_subsidy_material_template".equalsIgnoreCase(tableName);
      }
    }));
    return interceptor;
  }

}
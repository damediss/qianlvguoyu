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
          tenantId = 1L;
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
            || "platform_user_detail".equalsIgnoreCase(tableName)
            || "platform_user".equalsIgnoreCase(tableName)
            || "platform_user_token".equalsIgnoreCase(tableName)
            || "platform_sms".equalsIgnoreCase(tableName);
      }
    }));
    return interceptor;
  }

}
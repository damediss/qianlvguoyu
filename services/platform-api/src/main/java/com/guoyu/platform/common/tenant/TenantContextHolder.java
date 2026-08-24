package com.guoyu.platform.common.tenant;

public final class TenantContextHolder {

  /**
   * SaaS 默认租户（千铝工投）。
   * 口径变迁：分库+sync 时代 SaaS 库内用 1（鉴权方案 v1.2 Q2，2026-08-03）；
   * 2026-08-24 决策 7（数据库合并方案）改为与 PHP 库一致的 10159——
   * 合并后同库无转换层，PHP 代码硬编码 10159 过滤，SaaS 侧租户 id 数据驱动，统一向 PHP 口径看齐。
   */
  public static final Long DEFAULT_TENANT_ID = 10159L;

  private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

  private TenantContextHolder() {
  }

  public static void setTenantId(Long tenantId) {
    CURRENT_TENANT.set(tenantId);
  }

  public static Long getTenantId() {
    return CURRENT_TENANT.get();
  }

  public static void clear() {
    CURRENT_TENANT.remove();
  }
}

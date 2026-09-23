package com.guoyu.platform.security;

/**
 * 标记「带有确定租户归属」的认证主体。
 *
 * <p>链内会写入 SecurityContext 的主体不止一种：管理端 Sa-Token 主体（{@link PlatformUserPrincipal}）、
 * App 学生主体（同源）、竞赛选手与考评主体。租户一致性校验（TenantConsistencyFilter）需要以统一方式
 * 取出主体租户，因此抽取该接口，避免校验逻辑对具体主体类型做 if-else 分支而漏掉新增主体。
 */
public interface TenantScopedPrincipal {

  /** 主体所属租户；为 null 表示未知，校验应放行而不是拒绝。 */
  Long getTenantId();
}

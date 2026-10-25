package com.guoyu.platform.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

  String module();

  String action();

  String targetType();

  /** Whether the returned command result should be included in the audit detail. */
  boolean includeResult() default false;

  /**
   * Whether method args should be recorded in the audit detail. Default true keeps the
   * historical behavior. Endpoints whose args contain full document bodies (e.g. textbook
   * section autosave W3) must set false so contentMd never lands in the audit log.
   */
  boolean includeArgs() default true;
}

package com.guoyu.platform.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Optional handling for audit records whose request data is sensitive. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogOptions {

  /** Whether request arguments should be included in the audit detail. */
  boolean includeArguments() default true;

  /** Whether the authenticated actor is also the audit target. */
  boolean targetIdFromActor() default false;
}

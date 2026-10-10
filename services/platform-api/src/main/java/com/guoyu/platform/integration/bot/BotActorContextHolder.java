package com.guoyu.platform.integration.bot;

/** Request-thread holder populated only after the signed context is verified. */
public final class BotActorContextHolder {

  private static final ThreadLocal<BotActorContext> CONTEXT = new ThreadLocal<>();

  private BotActorContextHolder() {
  }

  public static BotActorContext get() {
    return CONTEXT.get();
  }

  public static void set(BotActorContext context) {
    CONTEXT.set(context);
  }

  public static void clear() {
    CONTEXT.remove();
  }
}

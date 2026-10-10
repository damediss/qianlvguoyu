package com.guoyu.platform.config;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token {@code SaBeanInject#setStpLogic} 按类型注入唯一/主 {@link StpLogic}。
 * 竞赛侧另有 competition-exam / competition-player 多账号体系 Bean，须用 {@code @Qualifier} 注入；
 * 此处钉死默认 login，避免启动时歧义。
 */
@Configuration
public class SaTokenStpConfiguration {

  @Bean
  @Primary
  public StpLogic stpLogic() {
    return StpUtil.getStpLogic();
  }
}

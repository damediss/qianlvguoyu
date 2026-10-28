package com.guoyu.platform.exam.answercheck.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 客观题答案 AI 检查业务开关与阈值（方案 §9.1）。不含 JEV 密钥配置。
 *
 * <p>16 个选项上限、每块 5 个多选子问题是固定常量，不开放环境配置。
 * 配置非法必须在启动期拒绝，不能运行期静默降级。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.objectiveanswercheck")
public class ObjectiveAnswerCheckProperties {

  private boolean enabled = false;

  @Min(1)
  @Max(2000)
  private int batchMaxSize = 2000;

  @Min(1)
  @Max(20)
  private int concurrency = 5;

  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private double confidenceThreshold = 0.80;

  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private double marginThreshold = 0.20;

  /** 过期批次恢复扫描开关（测试上下文关闭，避免定时任务干扰断言）。 */
  private boolean recoveryEnabled = true;

  @Min(100)
  private int maxStemChars = 12000;

  @Min(10)
  private int maxOptionChars = 2000;

  /** effectiveMaxBatchSize=min(2000, 配置值)；试点 200 时绝不允许创建 201 道。 */
  public int effectiveMaxBatchSize() {
    return Math.min(2000, batchMaxSize);
  }
}

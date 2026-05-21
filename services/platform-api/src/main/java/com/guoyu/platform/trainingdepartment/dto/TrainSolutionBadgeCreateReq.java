package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** train_solution_badge 创建请求 */
public class TrainSolutionBadgeCreateReq {

  @Size(max = 255)
  /** 方案ID */
  private Long solutionId;

  @NotBlank
  @Size(max = 255)
  /** 费用项目名称 */
  private String title;

  @Size(max = 255)
  /** 费用金额 */
  private BigDecimal badge;

  @Size(max = 255)
  /** 支出金额 */
  private BigDecimal expend;

  public Long getSolutionId() {
    return solutionId;
  }

  public void setSolutionId(Long solutionId) {
    this.solutionId = solutionId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public BigDecimal getBadge() {
    return badge;
  }

  public void setBadge(BigDecimal badge) {
    this.badge = badge;
  }

  public BigDecimal getExpend() {
    return expend;
  }

  public void setExpend(BigDecimal expend) {
    this.expend = expend;
  }
}

package com.guoyu.platform.exam.vo;

import java.io.Serializable;

/** 专家提交后返回的统计结果 (count/total/isdone)。 */
public class ExamTaskSubmitResultVO implements Serializable {
  private Long count;
  private Long total;
  private Integer isdone;

  public Long getCount() { return count; }
  public void setCount(Long count) { this.count = count; }
  public Long getTotal() { return total; }
  public void setTotal(Long total) { this.total = total; }
  public Integer getIsdone() { return isdone; }
  public void setIsdone(Integer isdone) { this.isdone = isdone; }
}

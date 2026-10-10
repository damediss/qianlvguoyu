package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamshicaoDailyVO implements Serializable {
  private String ymd;
  private Long nums;

  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
  public Long getNums() { return nums; }
  public void setNums(Long nums) { this.nums = nums; }
}

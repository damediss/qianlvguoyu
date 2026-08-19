package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FollowTeacherPaySaveReq {

  private List<Item> items = new ArrayList<>();

  public List<Item> getItems() {
    return items;
  }

  public void setItems(List<Item> items) {
    this.items = items == null ? new ArrayList<>() : items;
  }

  public static class Item {
    private String teacherName;
    private String teachForm;
    private BigDecimal ratePerDay;
    private String remark;

    public String getTeacherName() {
      return teacherName;
    }

    public void setTeacherName(String teacherName) {
      this.teacherName = teacherName;
    }

    public String getTeachForm() {
      return teachForm;
    }

    public void setTeachForm(String teachForm) {
      this.teachForm = teachForm;
    }

    public BigDecimal getRatePerDay() {
      return ratePerDay;
    }

    public void setRatePerDay(BigDecimal ratePerDay) {
      this.ratePerDay = ratePerDay;
    }

    public String getRemark() {
      return remark;
    }

    public void setRemark(String remark) {
      this.remark = remark;
    }
  }
}

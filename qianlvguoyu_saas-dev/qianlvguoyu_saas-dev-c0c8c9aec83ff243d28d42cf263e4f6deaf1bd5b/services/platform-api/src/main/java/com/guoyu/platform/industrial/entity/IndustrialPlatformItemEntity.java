package com.guoyu.platform.industrial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;

@TableName("industrial_platform_item")
public class IndustrialPlatformItemEntity extends BaseEntity {

  private String category;
  private String company;
  private String name;
  private Integer nums;
  private LocalDate itemDate;
  private String state;
  private Integer sortOrder;

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getNums() {
    return nums;
  }

  public void setNums(Integer nums) {
    this.nums = nums;
  }

  public LocalDate getItemDate() {
    return itemDate;
  }

  public void setItemDate(LocalDate itemDate) {
    this.itemDate = itemDate;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}

package com.guoyu.platform.industrial.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("industrial_talent_training")
public class IndustrialTalentTrainingEntity extends BaseEntity {

  private String name;
  private String level;
  @TableField("work_type")
  private String workType;
  private String zhiwei;
  private String education;
  private Integer sortOrder;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getWorkType() {
    return workType;
  }

  public void setWorkType(String workType) {
    this.workType = workType;
  }

  public String getZhiwei() {
    return zhiwei;
  }

  public void setZhiwei(String zhiwei) {
    this.zhiwei = zhiwei;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}

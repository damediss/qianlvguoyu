package com.guoyu.platform.industrial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("industrial_college")
public class IndustrialCollegeEntity extends BaseEntity {

  private String name;
  private String tag;
  private String businessScope;
  private String description;
  private String trainingQualification;
  private String certificationQualification;
  private String address;
  private String phone;
  private String status;
  private Integer sortOrder;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getBusinessScope() {
    return businessScope;
  }

  public void setBusinessScope(String businessScope) {
    this.businessScope = businessScope;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTrainingQualification() {
    return trainingQualification;
  }

  public void setTrainingQualification(String trainingQualification) {
    this.trainingQualification = trainingQualification;
  }

  public String getCertificationQualification() {
    return certificationQualification;
  }

  public void setCertificationQualification(String certificationQualification) {
    this.certificationQualification = certificationQualification;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}

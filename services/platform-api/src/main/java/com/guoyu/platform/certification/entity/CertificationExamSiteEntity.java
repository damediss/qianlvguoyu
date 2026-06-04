package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_site")
public class CertificationExamSiteEntity extends BaseEntity {
  private String siteName;
  private String siteType;
  private String address;
  private Integer seatCount;
  private String filingNo;
  private String contactName;
  private String contactPhone;
  private String status;
  private String equipmentCondition;
  private String remark;
}

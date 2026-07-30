package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_staff")
public class CertificationExamStaffEntity extends BaseEntity {
  private Long taskId;
  private Long categoryId;
  private String categoryCode;
  private String categoryName;
  private String staffName;
  private String idCardNo;
  private String maskedIdCardNo;
  private String phone;
  private String maskedPhone;
  private String orgName;
  private String duty;
  private Long examSiteId;
  private String examSiteName;
  private Long userId;
  private Long assessorId;
  private Long taskAssessorId;
  private String certificateNo;
  private String workTypeScope;
  private String status;
  private Integer locked;
  private String remark;
}

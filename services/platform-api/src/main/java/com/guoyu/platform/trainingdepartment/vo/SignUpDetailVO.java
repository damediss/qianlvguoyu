package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 报名计划详情（小程序/H5 扫码后展示） */
@Data
@Builder
public class SignUpDetailVO {
  private Long signUpId;
  private String title;
  private String cover;
  private String trainTime;
  private String examTime;
  private Long orgId;
  private Long trainInfoId;
  private Long trainClassId;
  private String collectionSource;
  /** 采集计划所属租户（App 调 OCR 等接口时带 X-Tenant-Id） */
  private Long tenantId;
  /** class_prep 时为 true：提交前须通过花名册校验 */
  private Boolean requireRoster;
  /** 传入 idCardNumber 时返回：是否在本班学员花名册 */
  private Boolean inRoster;
  private Integer peopleCount;
  private List<String> files;
}

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
  private Integer peopleCount;
  private List<String> files;
}

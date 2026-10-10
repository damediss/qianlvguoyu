package com.guoyu.platform.trainingdepartment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignUpRegisterResultVO {
  private Long id;
  private Long signUpId;
  private String name;
  private String phone;
  private String createTime;
}

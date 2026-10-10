package com.guoyu.platform.trainingdepartment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GraduationStudentUpdateReq {
  private String realname;
  private String phone;
  private String idCard;
  private String ticketNo;
  /** 证件照（一寸）存储路径或可访问 URL */
  private String idCardHeader;
  /** 身份证正面 */
  private String idCardFront;
  /** 身份证反面 */
  private String idCardBack;
  /** 证书文件路径（PDF/图片） */
  private String certificateFilePath;
}

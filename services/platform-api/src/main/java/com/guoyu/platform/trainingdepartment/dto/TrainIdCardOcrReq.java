package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** App 证件照/OCR 请求（对齐 AppuserController::idcardorcAction） */
public class TrainIdCardOcrReq {
  private Long signUpId;
  /** 正面上传时可为空（OCR 提取），反面/证件照必填 */
  private String idcard;
  /** front / back / header */
  @NotBlank
  private String type;
  /** CDN 相对路径，如 upload/xxx.jpg */
  @NotBlank
  private String pic;

  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public String getIdcard() { return idcard; }
  public void setIdcard(String idcard) { this.idcard = idcard; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
}

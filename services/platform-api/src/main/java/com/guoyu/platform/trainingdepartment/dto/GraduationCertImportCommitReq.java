package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** 毕业典礼证书文件夹导入 · 确认回填 */
public class GraduationCertImportCommitReq {

  @NotBlank
  private String previewId;

  public String getPreviewId() {
    return previewId;
  }

  public void setPreviewId(String previewId) {
    this.previewId = previewId;
  }
}

package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 教材节正文图片归属（textbook_section_asset）。
 * Markdown 中以 textbook-asset://{id} 引用；file_id 指向 file 模块受保护（protected/**）文件。
 */
@TableName("textbook_section_asset")
public class TextbookSectionAssetEntity extends BaseEntity {

  private Long projectId;
  private Long sectionId;
  private Long fileId;
  /** 本期仅 inline_image */
  private String assetType;
  private Integer sortNo;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getSectionId() {
    return sectionId;
  }

  public void setSectionId(Long sectionId) {
    this.sectionId = sectionId;
  }

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

  public String getAssetType() {
    return assetType;
  }

  public void setAssetType(String assetType) {
    this.assetType = assetType;
  }

  public Integer getSortNo() {
    return sortNo;
  }

  public void setSortNo(Integer sortNo) {
    this.sortNo = sortNo;
  }
}

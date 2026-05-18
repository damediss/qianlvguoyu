package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 课程课件创建请求
 */
public class CourseKejianCreateReq {

  /** 课件编号 */
  @NotBlank
  private String sno;

  /** 课件名称 */
  @NotBlank
  @Size(max = 50)
  private String title;

  /** 课时 */
  private Integer keshi;

  /** 封面 */
  private String cover;

  /** 获取方式:0免费,1收费 */
  private Integer type;

  /** 单价 */
  private BigDecimal price;

  /** 描述 */
  private String description;

  /** 内容 */
  private String content;

  /** 文件路径 */
  private String filePath;

  /** 工种id */
  private Long workTypeId;

  /** 0平台,1企业 */
  private Integer tag;

  /** 机构id */
  private Long jigouId;

  /** 拥有者id */
  private Long ownerId;

  public String getSno() { return sno; }
  public void setSno(String sno) { this.sno = sno; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getKeshi() { return keshi; }
  public void setKeshi(Integer keshi) { this.keshi = keshi; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getTag() { return tag; }
  public void setTag(Integer tag) { this.tag = tag; }
  public Long getJigouId() { return jigouId; }
  public void setJigouId(Long jigouId) { this.jigouId = jigouId; }
  public Long getOwnerId() { return ownerId; }
  public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}

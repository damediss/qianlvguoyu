package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_sign_up_self
 */
@TableName("train_sign_up_self")
public class TrainSignUpSelfEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 标题 */
  private String title;

  /** 封面图 */
  private String cover;

  /** 培训时间 */
  private String trainTime;

  /** 考试时间 */
  private String examTime;

  /** 组织ID */
  private Long orgId;

  /** 关联企业调研 */
  private Long researchOrgId;

  /** 采集来源 train_info / research */
  private String collectionSource;

  /** 公开采集 token */
  private String qrcodeToken;

  /** 培训信息ID */
  private Long trainInfoId;

  /** 排序号 */
  private Integer sortNumber;

  /** 方案附件 JSON 数组（相对 CDN 路径） */
  private String files;

  private Long tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public String getTrainTime() { return trainTime; }
  public void setTrainTime(String trainTime) { this.trainTime = trainTime; }
  public String getExamTime() { return examTime; }
  public void setExamTime(String examTime) { this.examTime = examTime; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getResearchOrgId() { return researchOrgId; }
  public void setResearchOrgId(Long researchOrgId) { this.researchOrgId = researchOrgId; }
  public String getCollectionSource() { return collectionSource; }
  public void setCollectionSource(String collectionSource) { this.collectionSource = collectionSource; }
  public String getQrcodeToken() { return qrcodeToken; }
  public void setQrcodeToken(String qrcodeToken) { this.qrcodeToken = qrcodeToken; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getFiles() { return files; }
  public void setFiles(String files) { this.files = files; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}

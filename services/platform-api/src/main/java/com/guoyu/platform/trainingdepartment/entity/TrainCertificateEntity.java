package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训证书实体
 */
@TableName("train_certificate")
public class TrainCertificateEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 项目ID */
  private Long projectId;
  /** 班级ID */
  private Long classId;
  /** 花名册学员ID */
  private Long peopleId;
  /** 模板ID */
  private Long templateId;
  /** 用户ID */
  private Long userId;
  private String userName;
  /** 证件类型（证面原文，如居民身份证） */
  private String idDocType;
  /** 证书编号 */
  private String certificateNo;
  private String certificateType;
  /** 工种 */
  private String workType;
  /** 职业名称（证面） */
  private String occupation;
  /** 等级 */
  private String level;
  /** 等级五档归一（报表用；证面原文在 level） */
  private String levelNormalized;
  /** 颁发日期 */
  private String issueDate;
  private String expireDate;
  private String issuingAuthority;
  /** 评价机构/落款机构（证面） */
  private String evaluationOrg;
  /** 证面查询地址原文 JSON 数组 */
  private String queryUrls;
  private String filePath;
  /** 证书文件 ID */
  private Long fileId;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  private Long createdBy;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getPeopleId() { return peopleId; }
  public void setPeopleId(Long peopleId) { this.peopleId = peopleId; }
  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getIdDocType() { return idDocType; }
  public void setIdDocType(String idDocType) { this.idDocType = idDocType; }
  public String getCertificateNo() { return certificateNo; }
  public void setCertificateNo(String certificateNo) { this.certificateNo = certificateNo; }
  public String getCertificateType() { return certificateType; }
  public void setCertificateType(String certificateType) { this.certificateType = certificateType; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getOccupation() { return occupation; }
  public void setOccupation(String occupation) { this.occupation = occupation; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getLevelNormalized() { return levelNormalized; }
  public void setLevelNormalized(String levelNormalized) { this.levelNormalized = levelNormalized; }
  public String getIssueDate() { return issueDate; }
  public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
  public String getExpireDate() { return expireDate; }
  public void setExpireDate(String expireDate) { this.expireDate = expireDate; }
  public String getIssuingAuthority() { return issuingAuthority; }
  public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
  public String getEvaluationOrg() { return evaluationOrg; }
  public void setEvaluationOrg(String evaluationOrg) { this.evaluationOrg = evaluationOrg; }
  public String getQueryUrls() { return queryUrls; }
  public void setQueryUrls(String queryUrls) { this.queryUrls = queryUrls; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}

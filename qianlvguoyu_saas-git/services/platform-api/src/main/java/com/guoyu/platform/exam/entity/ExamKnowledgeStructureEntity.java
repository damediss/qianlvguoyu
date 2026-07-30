package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_knowledge_structure")
public class ExamKnowledgeStructureEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer type;
  private Integer useType;
  private Integer courseId;
  private Integer parentId;
  private String cover;
  private String intro;
  private String title;
  private String seriesNo;
  private Integer level;
  private Integer important;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private Integer orgId;
  private String orgName;
  private Integer amount;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getUseType() { return useType; }
  public void setUseType(Integer useType) { this.useType = useType; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getParentId() { return parentId; }
  public void setParentId(Integer parentId) { this.parentId = parentId; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public String getIntro() { return intro; }
  public void setIntro(String intro) { this.intro = intro; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSeriesNo() { return seriesNo; }
  public void setSeriesNo(String seriesNo) { this.seriesNo = seriesNo; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Integer getImportant() { return important; }
  public void setImportant(Integer important) { this.important = important; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Integer getOrgId() { return orgId; }
  public void setOrgId(Integer orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Integer getAmount() { return amount; }
  public void setAmount(Integer amount) { this.amount = amount; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}

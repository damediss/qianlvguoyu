package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_work_type")
public class ExamWorkTypeEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private String title;
  private String seriesNo;
  private String levelsNo;
  private Integer parentId;
  private Integer userId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSeriesNo() { return seriesNo; }
  public void setSeriesNo(String seriesNo) { this.seriesNo = seriesNo; }
  public String getLevelsNo() { return levelsNo; }
  public void setLevelsNo(String levelsNo) { this.levelsNo = levelsNo; }
  public Integer getParentId() { return parentId; }
  public void setParentId(Integer parentId) { this.parentId = parentId; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

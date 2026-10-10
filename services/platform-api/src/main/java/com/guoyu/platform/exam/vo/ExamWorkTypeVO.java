package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import java.time.LocalDateTime;
import java.util.List;

public class ExamWorkTypeVO {

  private Integer id;
  private String title;
  private String seriesNo;
  private String levelsNo;
  private Integer parentId;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;
  private List<ExamWorkTypeVO> children;

  public static ExamWorkTypeVO from(ExamWorkTypeEntity entity) {
    ExamWorkTypeVO vo = new ExamWorkTypeVO();
    vo.setId(entity.getId());
    vo.setTitle(entity.getTitle());
    vo.setSeriesNo(entity.getSeriesNo());
    vo.setLevelsNo(entity.getLevelsNo());
    vo.setParentId(entity.getParentId());
    vo.setUserId(entity.getUserId());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    return vo;
  }

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
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public List<ExamWorkTypeVO> getChildren() { return children; }
  public void setChildren(List<ExamWorkTypeVO> children) { this.children = children; }
}
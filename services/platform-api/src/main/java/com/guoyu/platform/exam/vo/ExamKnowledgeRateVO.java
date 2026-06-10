package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamKnowledgeRateEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ExamKnowledgeRateVO {

  private Integer id;
  private Integer courseId;
  private Integer type;
  private Integer isStandard;
  private String title;
  private Integer sortNumber;
  private String comments;
  private Integer status;
  private Integer userId;
  private Integer tenantId;
  private LocalDateTime createTime;
  private List<ExamKnowledgeRateDetailVO> detailList;

  public static ExamKnowledgeRateVO from(ExamKnowledgeRateEntity entity) {
    ExamKnowledgeRateVO vo = new ExamKnowledgeRateVO();
    vo.setId(entity.getId());
    vo.setCourseId(entity.getCourseId());
    vo.setType(entity.getType());
    vo.setIsStandard(entity.getIsStandard());
    vo.setTitle(entity.getTitle());
    vo.setSortNumber(entity.getSortNumber());
    vo.setComments(entity.getComments());
    vo.setStatus(entity.getStatus());
    vo.setUserId(entity.getUserId());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    if (entity.getDetailList() != null) {
      vo.setDetailList(entity.getDetailList().stream()
          .map(ExamKnowledgeRateDetailVO::from)
          .collect(Collectors.toList()));
    }
    return vo;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getIsStandard() { return isStandard; }
  public void setIsStandard(Integer isStandard) { this.isStandard = isStandard; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public List<ExamKnowledgeRateDetailVO> getDetailList() { return detailList; }
  public void setDetailList(List<ExamKnowledgeRateDetailVO> detailList) { this.detailList = detailList; }
}

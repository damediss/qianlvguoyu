package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import java.time.LocalDateTime;
import java.util.List;

public class ExamKnowledgeStructureVO {

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
  private Integer amount;
  private LocalDateTime createTime;
  private List<ExamKnowledgeStructureVO> children;

  public static ExamKnowledgeStructureVO from(ExamKnowledgeStructureEntity entity) {
    ExamKnowledgeStructureVO vo = new ExamKnowledgeStructureVO();
    vo.setId(entity.getId());
    vo.setType(entity.getType());
    vo.setUseType(entity.getUseType());
    vo.setCourseId(entity.getCourseId());
    vo.setParentId(entity.getParentId());
    vo.setCover(entity.getCover());
    vo.setIntro(entity.getIntro());
    vo.setTitle(entity.getTitle());
    vo.setSeriesNo(entity.getSeriesNo());
    vo.setLevel(entity.getLevel());
    vo.setImportant(entity.getImportant());
    vo.setUserId(entity.getUserId());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setAmount(entity.getAmount());
    vo.setCreateTime(entity.getCreateTime());
    return vo;
  }

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
  public Integer getAmount() { return amount; }
  public void setAmount(Integer amount) { this.amount = amount; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public List<ExamKnowledgeStructureVO> getChildren() { return children; }
  public void setChildren(List<ExamKnowledgeStructureVO> children) { this.children = children; }
}

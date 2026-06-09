package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamExamDetailVO {

  private Integer id;
  private Integer examId;
  private Integer taskDetailId;
  private Integer source;
  private Integer orgId;
  private String orgName;
  private Integer knowledgeId;
  private String knowledgeTitle;
  private Integer isPrivate;
  private Integer difficulty;
  private Integer userId;
  private String creatorName;
  private Integer checkUserId;
  private Integer finalCheckUserId;
  private String type;
  private String keyword;
  private String audio;
  private String video;
  private String content;
  private String answer;
  private BigDecimal score;
  private String resolve;
  private Integer checkStatus;
  private Integer finalCheckStatus;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private LocalDateTime checkTime;
  private LocalDateTime finalCheckTime;

  public static ExamExamDetailVO from(ExamExamDetailEntity entity) {
    ExamExamDetailVO vo = new ExamExamDetailVO();
    vo.setId(entity.getId());
    vo.setExamId(entity.getExamId());
    vo.setTaskDetailId(entity.getTaskDetailId());
    vo.setSource(entity.getSource());
    vo.setOrgId(entity.getOrgId());
    vo.setOrgName(entity.getOrgName());
    vo.setKnowledgeId(entity.getKnowledgeId());
    vo.setIsPrivate(entity.getIsPrivate());
    vo.setDifficulty(entity.getDifficulty());
    vo.setUserId(entity.getUserId());
    vo.setCheckUserId(entity.getCheckUserId());
    vo.setFinalCheckUserId(entity.getFinalCheckUserId());
    vo.setType(entity.getType());
    vo.setKeyword(entity.getKeyword());
    vo.setAudio(entity.getAudio());
    vo.setVideo(entity.getVideo());
    vo.setContent(entity.getContent());
    vo.setAnswer(entity.getAnswer());
    vo.setScore(entity.getScore());
    vo.setResolve(entity.getResolve());
    vo.setCheckStatus(entity.getCheckStatus());
    vo.setFinalCheckStatus(entity.getFinalCheckStatus());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    vo.setUpdateTime(entity.getUpdateTime());
    vo.setCheckTime(entity.getCheckTime());
    vo.setFinalCheckTime(entity.getFinalCheckTime());
    return vo;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public Integer getTaskDetailId() { return taskDetailId; }
  public void setTaskDetailId(Integer taskDetailId) { this.taskDetailId = taskDetailId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public Integer getOrgId() { return orgId; }
  public void setOrgId(Integer orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getKnowledgeTitle() { return knowledgeTitle; }
  public void setKnowledgeTitle(String knowledgeTitle) { this.knowledgeTitle = knowledgeTitle; }
  public Integer getIsPrivate() { return isPrivate; }
  public void setIsPrivate(Integer isPrivate) { this.isPrivate = isPrivate; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public String getCreatorName() { return creatorName; }
  public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
  public Integer getCheckUserId() { return checkUserId; }
  public void setCheckUserId(Integer checkUserId) { this.checkUserId = checkUserId; }
  public Integer getFinalCheckUserId() { return finalCheckUserId; }
  public void setFinalCheckUserId(Integer finalCheckUserId) { this.finalCheckUserId = finalCheckUserId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public String getAudio() { return audio; }
  public void setAudio(String audio) { this.audio = audio; }
  public String getVideo() { return video; }
  public void setVideo(String video) { this.video = video; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public String getResolve() { return resolve; }
  public void setResolve(String resolve) { this.resolve = resolve; }
  public Integer getCheckStatus() { return checkStatus; }
  public void setCheckStatus(Integer checkStatus) { this.checkStatus = checkStatus; }
  public Integer getFinalCheckStatus() { return finalCheckStatus; }
  public void setFinalCheckStatus(Integer finalCheckStatus) { this.finalCheckStatus = finalCheckStatus; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public LocalDateTime getCheckTime() { return checkTime; }
  public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
  public LocalDateTime getFinalCheckTime() { return finalCheckTime; }
  public void setFinalCheckTime(LocalDateTime finalCheckTime) { this.finalCheckTime = finalCheckTime; }
}

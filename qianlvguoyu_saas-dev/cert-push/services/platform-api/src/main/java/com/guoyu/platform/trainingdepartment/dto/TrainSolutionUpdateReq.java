package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/**
 * 培训方案更新请求
 */
public class TrainSolutionUpdateReq {
  private Long trainInfoId;
  private Long companyId;
  @Size(max = 255) private String companyName;
  /** 标题 */
  @Size(max = 255) private String title;
  private String nationPolicy;
  private String industrySituationAnalysis;
  private String companyDevelopmentAnalysis;
  private String researchDataAnalysis;
  private String projectDevelopmentIdeas;
  private String mainObjectives;
  private String mainInitiatives;
  private String developmentPlan;
  private String division;
  private String risk;
  private String communication;
  private String checkpoint;
  private String results;
  private String teacherIntro;
  private String requestAnalysis;
  private String trainContent;
  private String trainMethod;
  private String course;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;
  /** 内容 */
  private String content;
  private String filePath;
  private String rejectReason;

  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getNationPolicy() { return nationPolicy; }
  public void setNationPolicy(String nationPolicy) { this.nationPolicy = nationPolicy; }
  public String getIndustrySituationAnalysis() { return industrySituationAnalysis; }
  public void setIndustrySituationAnalysis(String industrySituationAnalysis) { this.industrySituationAnalysis = industrySituationAnalysis; }
  public String getCompanyDevelopmentAnalysis() { return companyDevelopmentAnalysis; }
  public void setCompanyDevelopmentAnalysis(String companyDevelopmentAnalysis) { this.companyDevelopmentAnalysis = companyDevelopmentAnalysis; }
  public String getResearchDataAnalysis() { return researchDataAnalysis; }
  public void setResearchDataAnalysis(String researchDataAnalysis) { this.researchDataAnalysis = researchDataAnalysis; }
  public String getProjectDevelopmentIdeas() { return projectDevelopmentIdeas; }
  public void setProjectDevelopmentIdeas(String projectDevelopmentIdeas) { this.projectDevelopmentIdeas = projectDevelopmentIdeas; }
  public String getMainObjectives() { return mainObjectives; }
  public void setMainObjectives(String mainObjectives) { this.mainObjectives = mainObjectives; }
  public String getMainInitiatives() { return mainInitiatives; }
  public void setMainInitiatives(String mainInitiatives) { this.mainInitiatives = mainInitiatives; }
  public String getDevelopmentPlan() { return developmentPlan; }
  public void setDevelopmentPlan(String developmentPlan) { this.developmentPlan = developmentPlan; }
  public String getDivision() { return division; }
  public void setDivision(String division) { this.division = division; }
  public String getRisk() { return risk; }
  public void setRisk(String risk) { this.risk = risk; }
  public String getCommunication() { return communication; }
  public void setCommunication(String communication) { this.communication = communication; }
  public String getCheckpoint() { return checkpoint; }
  public void setCheckpoint(String checkpoint) { this.checkpoint = checkpoint; }
  public String getResults() { return results; }
  public void setResults(String results) { this.results = results; }
  public String getTeacherIntro() { return teacherIntro; }
  public void setTeacherIntro(String teacherIntro) { this.teacherIntro = teacherIntro; }
  public String getRequestAnalysis() { return requestAnalysis; }
  public void setRequestAnalysis(String requestAnalysis) { this.requestAnalysis = requestAnalysis; }
  public String getTrainContent() { return trainContent; }
  public void setTrainContent(String trainContent) { this.trainContent = trainContent; }
  public String getTrainMethod() { return trainMethod; }
  public void setTrainMethod(String trainMethod) { this.trainMethod = trainMethod; }
  public String getCourse() { return course; }
  public void setCourse(String course) { this.course = course; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}

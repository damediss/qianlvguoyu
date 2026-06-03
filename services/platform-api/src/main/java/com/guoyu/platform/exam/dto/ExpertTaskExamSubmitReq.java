package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 专家端 - 提交理论题审核。
 * items 是题目集合(支持一次提交多道),逐条写入 exam_task_exam_audit_records。
 */
public class ExpertTaskExamSubmitReq {

  @NotNull
  private Integer taskId;
  @NotNull
  private Integer role;     // 0=初审 1=终审
  private Integer id;       // 单题时使用 (兼容 PHP 端 task-exam-submit 单题模式)
  private String examType;
  private String examContent;
  private String examA;
  private String examB;
  private String examC;
  private String examD;
  private String examE;
  private String examF;
  private String examG;
  private String examH;
  private String examAnswer;
  private java.util.List<ExamItem> items;

  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getRole() { return role; }
  public void setRole(Integer role) { this.role = role; }
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getExamA() { return examA; }
  public void setExamA(String examA) { this.examA = examA; }
  public String getExamB() { return examB; }
  public void setExamB(String examB) { this.examB = examB; }
  public String getExamC() { return examC; }
  public void setExamC(String examC) { this.examC = examC; }
  public String getExamD() { return examD; }
  public void setExamD(String examD) { this.examD = examD; }
  public String getExamE() { return examE; }
  public void setExamE(String examE) { this.examE = examE; }
  public String getExamF() { return examF; }
  public void setExamF(String examF) { this.examF = examF; }
  public String getExamG() { return examG; }
  public void setExamG(String examG) { this.examG = examG; }
  public String getExamH() { return examH; }
  public void setExamH(String examH) { this.examH = examH; }
  public String getExamAnswer() { return examAnswer; }
  public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
  public java.util.List<ExamItem> getItems() { return items; }
  public void setItems(java.util.List<ExamItem> items) { this.items = items; }

  public static class ExamItem {
    private Integer id;
    private String examContent;
    private String examA;
    private String examB;
    private String examC;
    private String examD;
    private String examE;
    private String examF;
    private String examG;
    private String examH;
    private String examAnswer;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getExamContent() { return examContent; }
    public void setExamContent(String examContent) { this.examContent = examContent; }
    public String getExamA() { return examA; }
    public void setExamA(String examA) { this.examA = examA; }
    public String getExamB() { return examB; }
    public void setExamB(String examB) { this.examB = examB; }
    public String getExamC() { return examC; }
    public void setExamC(String examC) { this.examC = examC; }
    public String getExamD() { return examD; }
    public void setExamD(String examD) { this.examD = examD; }
    public String getExamE() { return examE; }
    public void setExamE(String examE) { this.examE = examE; }
    public String getExamF() { return examF; }
    public void setExamF(String examF) { this.examF = examF; }
    public String getExamG() { return examG; }
    public void setExamG(String examG) { this.examG = examG; }
    public String getExamH() { return examH; }
    public void setExamH(String examH) { this.examH = examH; }
    public String getExamAnswer() { return examAnswer; }
    public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
  }
}

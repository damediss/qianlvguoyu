package com.guoyu.platform.trainingdepartment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 保存班级练习范围（科目 + 知识点白名单 + 可选刷题时间窗） */
public class ClassPracticeScopeSaveReq {

  /** 1 启用 / 0 停用；默认启用 */
  private Integer enabled = 1;
  private String remark;
  /** 刷题开放开始；null 表示不限开始（可清空） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime practiceStartTime;
  /** 刷题截止；null 表示不过期（可清空） */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime practiceEndTime;
  private List<Integer> courseIds = new ArrayList<>();
  /** 知识点 ID；可空表示所选科目下不限知识点 */
  private List<Integer> knowledgeIds = new ArrayList<>();

  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer enabled) { this.enabled = enabled; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getPracticeStartTime() { return practiceStartTime; }
  public void setPracticeStartTime(LocalDateTime practiceStartTime) { this.practiceStartTime = practiceStartTime; }
  public LocalDateTime getPracticeEndTime() { return practiceEndTime; }
  public void setPracticeEndTime(LocalDateTime practiceEndTime) { this.practiceEndTime = practiceEndTime; }
  public List<Integer> getCourseIds() { return courseIds; }
  public void setCourseIds(List<Integer> courseIds) { this.courseIds = courseIds; }
  public List<Integer> getKnowledgeIds() { return knowledgeIds; }
  public void setKnowledgeIds(List<Integer> knowledgeIds) { this.knowledgeIds = knowledgeIds; }
}

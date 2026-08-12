package com.guoyu.platform.trainingdepartment.dto;

import java.util.ArrayList;
import java.util.List;

/** App 题库智能抽题请求。 */
public class FollowBankDrawReq {

  private Integer courseId;
  private Integer knowledgeId;
  /** 单选题抽取数量 */
  private Integer singleCount = 0;
  /** 多选题抽取数量 */
  private Integer multiCount = 0;
  /** 判断题抽取数量 */
  private Integer judgeCount = 0;
  /** 已选题目，抽取时排除 */
  private List<Integer> excludeIds = new ArrayList<>();
  /** 模拟考配卷：排除本班其他 mock 任务已占用的题库题 */
  private Boolean excludeOtherMock;
  /** 编辑当前草稿时不排除本任务已选题 */
  private Long excludeTaskId;

  public Integer getCourseId() {
    return courseId;
  }

  public void setCourseId(Integer courseId) {
    this.courseId = courseId;
  }

  public Integer getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Integer knowledgeId) {
    this.knowledgeId = knowledgeId;
  }

  public Integer getSingleCount() {
    return singleCount;
  }

  public void setSingleCount(Integer singleCount) {
    this.singleCount = singleCount;
  }

  public Integer getMultiCount() {
    return multiCount;
  }

  public void setMultiCount(Integer multiCount) {
    this.multiCount = multiCount;
  }

  public Integer getJudgeCount() {
    return judgeCount;
  }

  public void setJudgeCount(Integer judgeCount) {
    this.judgeCount = judgeCount;
  }

  public List<Integer> getExcludeIds() {
    return excludeIds;
  }

  public void setExcludeIds(List<Integer> excludeIds) {
    this.excludeIds = excludeIds;
  }

  public Boolean getExcludeOtherMock() {
    return excludeOtherMock;
  }

  public void setExcludeOtherMock(Boolean excludeOtherMock) {
    this.excludeOtherMock = excludeOtherMock;
  }

  public Long getExcludeTaskId() {
    return excludeTaskId;
  }

  public void setExcludeTaskId(Long excludeTaskId) {
    this.excludeTaskId = excludeTaskId;
  }
}

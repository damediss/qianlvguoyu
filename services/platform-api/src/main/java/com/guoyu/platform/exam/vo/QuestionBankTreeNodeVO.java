package com.guoyu.platform.exam.vo;

import java.util.List;

/**
 * 题库分类导航聚合树节点。
 *
 * <p>完整树: 工种 → 课程 → 知识点 L0 → L1 → L2 → L3 (最多 6 级)。每个节点带
 * {@code questionCount},表示以该节点为根的子树中所有"挂载在 L3 知识点上
 * 且未逻辑删除"的试题总数(递归汇总自身+所有后代)。
 *
 * <p>字段命名按前端 QuestionBankPage 的消费习惯: {@code nodeType} 区分节点类型,
 * {@code key/id} 稳定用作 React key, {@code children} 始终是数组(空数组而非 null,
 * 避免前端 undefined 处理)。
 */
public class QuestionBankTreeNodeVO {

  /** 节点唯一 key,前端 React key 用,值形如 "workType-1" / "course-2" / "ks-3" */
  private String key;

  /** 节点原始主键 */
  private Integer id;

  /** 节点类型: workType / course / knowledge */
  private String nodeType;

  /** 显示标题 */
  private String title;

  /** 排序号 (小到大) */
  private Integer sortNumber;

  /** 业务状态 (0=停用, 1=启用),仅 knowledge 节点有意义 */
  private Integer status;

  /** 知识点节点专用: 树层级 (0/1/2/3) */
  private Integer level;

  /** 课程节点专用: 关联工种 ID */
  private Integer workTypeId;

  /** 知识点节点专用: 关联课程 ID */
  private Integer courseId;

  /** 题目数量递归汇总: 自身 L3 节点挂载的题 + 所有后代节点的题 */
  private long questionCount;

  /** 子节点(始终为数组,空叶节点是 [],不会是 null) */
  private List<QuestionBankTreeNodeVO> children;

  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getNodeType() { return nodeType; }
  public void setNodeType(String nodeType) { this.nodeType = nodeType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public long getQuestionCount() { return questionCount; }
  public void setQuestionCount(long questionCount) { this.questionCount = questionCount; }
  public List<QuestionBankTreeNodeVO> getChildren() { return children; }
  public void setChildren(List<QuestionBankTreeNodeVO> children) { this.children = children; }
}

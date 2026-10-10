package com.guoyu.platform.exam.knowledgefix.dto;

import java.time.LocalDateTime;

/** exam_exam_detail 候选原始行（去重清单生成的 Java 侧输入） */
public class DetailRawRow {
  private Integer id;
  private String type;
  private String content;
  private String answer;
  private Integer knowledgeId;
  private LocalDateTime createTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

package com.guoyu.platform.exam.dto;

import java.math.BigDecimal;

/** Immutable source data captured when a pre-practice task selects a course question. */
public class ExamPracticeQuestionResourceRow {

  private Long questionId;
  private Integer sourceCourseId;
  private Integer sourceWorkTypeId;
  private Integer sourceLevel;
  private Integer knowledgeId;
  private String questionType;
  private String content;
  private String answer;
  private String resolve;
  private String audio;
  private String video;
  private Integer difficulty;
  private BigDecimal score;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Integer getSourceCourseId() { return sourceCourseId; }
  public void setSourceCourseId(Integer sourceCourseId) { this.sourceCourseId = sourceCourseId; }
  public Integer getSourceWorkTypeId() { return sourceWorkTypeId; }
  public void setSourceWorkTypeId(Integer sourceWorkTypeId) { this.sourceWorkTypeId = sourceWorkTypeId; }
  public Integer getSourceLevel() { return sourceLevel; }
  public void setSourceLevel(Integer sourceLevel) { this.sourceLevel = sourceLevel; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getResolve() { return resolve; }
  public void setResolve(String resolve) { this.resolve = resolve; }
  public String getAudio() { return audio; }
  public void setAudio(String audio) { this.audio = audio; }
  public String getVideo() { return video; }
  public void setVideo(String video) { this.video = video; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
}

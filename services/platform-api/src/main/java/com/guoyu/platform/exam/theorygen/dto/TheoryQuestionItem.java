package com.guoyu.platform.exam.theorygen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** LLM 生成试题条目（JSON 解析目标，零信任：全部字段可空，由 Validator 判定） */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheoryQuestionItem {

  private String questionType;
  private String typeCode;
  private Integer difficulty;
  private String content;
  private List<Option> options;
  private String answer;
  private String analysis;
  private String referenceAnswer;
  private String scorePoints;
  private String scoringStandard;
  private java.math.BigDecimal score;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Option {
    private String label;
    private String content;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
  }

  public List<Option> getOptionList() { return options; }
  public void setOptions(List<Option> options) { this.options = options; }

  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getTypeCode() { return typeCode; }
  public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getAnalysis() { return analysis; }
  public void setAnalysis(String analysis) { this.analysis = analysis; }
  public String getReferenceAnswer() { return referenceAnswer; }
  public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
  public String getScorePoints() { return scorePoints; }
  public void setScorePoints(String scorePoints) { this.scorePoints = scorePoints; }
  public String getScoringStandard() { return scoringStandard; }
  public void setScoringStandard(String scoringStandard) { this.scoringStandard = scoringStandard; }
  public java.math.BigDecimal getScore() { return score; }
  public void setScore(java.math.BigDecimal score) { this.score = score; }
}

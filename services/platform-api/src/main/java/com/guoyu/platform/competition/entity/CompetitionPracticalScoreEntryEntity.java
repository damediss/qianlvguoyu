package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_score_entry")
public class CompetitionPracticalScoreEntryEntity extends BaseEntity {
  private Long scoreSheetId;
  private Long ruleItemId;
  private Long paperQuestionId;
  private Integer paperItemNo;
  private BigDecimal score;
  private BigDecimal deduction;
  private String remark;
  private String status;
  public Long getScoreSheetId() { return scoreSheetId; }
  public void setScoreSheetId(Long value) { scoreSheetId = value; }
  public Long getRuleItemId() { return ruleItemId; }
  public void setRuleItemId(Long value) { ruleItemId = value; }
  public Long getPaperQuestionId() { return paperQuestionId; }
  public void setPaperQuestionId(Long value) { paperQuestionId = value; }
  public Integer getPaperItemNo() { return paperItemNo; }
  public void setPaperItemNo(Integer value) { paperItemNo = value; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal value) { score = value; }
  public BigDecimal getDeduction() { return deduction; }
  public void setDeduction(BigDecimal value) { deduction = value; }
  public String getRemark() { return remark; }
  public void setRemark(String value) { remark = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}

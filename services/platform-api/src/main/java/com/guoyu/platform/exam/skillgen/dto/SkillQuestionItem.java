package com.guoyu.platform.exam.skillgen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * LLM 生成的操作技能试题条目（JSON 解析目标，零信任：全部字段可空，由 Validator 判定）。
 * 结构对应方案 §7.2：每题 = 准备要求（考场含设备设施表 + 考生准备）+ 考核试题（要求/否定项）+ 评分标准。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillQuestionItem {

  private String name;
  @JsonAlias("duration_minutes")
  private Integer durationMinutes;
  @JsonAlias("exam_form")
  private String examForm;
  @JsonAlias("site_prepare")
  private SitePrepare sitePrepare;
  @JsonAlias("examinee_prepare")
  private String examineePrepare;
  private String requirements;
  private String negatives;
  private List<ScoringItem> scoring;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SitePrepare {
    private String note;
    private List<Equipment> equipment;
    @JsonAlias("fault_setup")
    private String faultSetup;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<Equipment> getEquipment() { return equipment; }
    public void setEquipment(List<Equipment> equipment) { this.equipment = equipment; }
    public String getFaultSetup() { return faultSetup; }
    public void setFaultSetup(String faultSetup) { this.faultSetup = faultSetup; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Equipment {
    private String name;
    private String spec;
    private String unit;
    private String qty;
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getQty() { return qty; }
    public void setQty(String qty) { this.qty = qty; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
  }

  /** 评分记录表行（item/score/scoreDesc/maxDeduct/step，与 pingfen JSON 五键一致） */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ScoringItem {
    private String item;
    private String score;
    @JsonAlias("score_desc")
    private String scoreDesc;
    @JsonAlias("max_deduct")
    private String maxDeduct;
    private String step;

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }
    public String getScoreDesc() { return scoreDesc; }
    public void setScoreDesc(String scoreDesc) { this.scoreDesc = scoreDesc; }
    public String getMaxDeduct() { return maxDeduct; }
    public void setMaxDeduct(String maxDeduct) { this.maxDeduct = maxDeduct; }
    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public String getExamForm() { return examForm; }
  public void setExamForm(String examForm) { this.examForm = examForm; }
  public SitePrepare getSitePrepare() { return sitePrepare; }
  public void setSitePrepare(SitePrepare sitePrepare) { this.sitePrepare = sitePrepare; }
  public String getExamineePrepare() { return examineePrepare; }
  public void setExamineePrepare(String examineePrepare) { this.examineePrepare = examineePrepare; }
  public String getRequirements() { return requirements; }
  public void setRequirements(String requirements) { this.requirements = requirements; }
  public String getNegatives() { return negatives; }
  public void setNegatives(String negatives) { this.negatives = negatives; }
  public List<ScoringItem> getScoring() { return scoring; }
  public void setScoring(List<ScoringItem> scoring) { this.scoring = scoring; }
}

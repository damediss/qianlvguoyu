package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * Tenant-level referee qualification.  The work type points to the global
 * exam_work_type catalogue; it is deliberately not scoped to a competition.
 */
@TableName("competition_referee_qualification")
public class CompetitionRefereeQualificationEntity extends BaseEntity {

  private Long refereeProfileId;
  private Long workTypeId;
  /**
   * The UI-facing range is ordered from the lower skill level to the higher
   * skill level (for example, 初级工 -> 高级工).  The numeric level codes are
   * intentionally kept in their existing competition convention, where 5 is
   * 初级工 and 1 is 高级技师.
   */
  private String levelFromCode;
  private String levelToCode;
  /** Legacy single-level column retained for compatibility with V20260918_004. */
  private String levelCode;

  public Long getRefereeProfileId() { return refereeProfileId; }
  public void setRefereeProfileId(Long refereeProfileId) { this.refereeProfileId = refereeProfileId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getLevelFromCode() { return levelFromCode; }
  public void setLevelFromCode(String levelFromCode) { this.levelFromCode = levelFromCode; }
  public String getLevelToCode() { return levelToCode; }
  public void setLevelToCode(String levelToCode) { this.levelToCode = levelToCode; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
}

package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_staff_category")
public class CertificationExamStaffCategoryEntity extends BaseEntity {
  private Long taskId;
  private Long templateId;
  private String categoryCode;
  private String categoryName;
  private Integer required;
  private Integer minimumCount;
  private Integer currentCount;
  private String completeStatus;
  private String completedByName;
  private LocalDateTime completedAt;
  private String reviewStatus;
  private String reviewedByName;
  private LocalDateTime reviewedAt;
  private String reviewComment;
  private Integer locked;
  private String remark;
}

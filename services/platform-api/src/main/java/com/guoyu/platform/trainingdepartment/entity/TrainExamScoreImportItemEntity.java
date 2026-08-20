package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_exam_score_import_item")
public class TrainExamScoreImportItemEntity extends BaseEntity {

  private Long batchId;
  private Integer rowNo;
  private String idCard;
  private String name;
  private String ticketNo;
  private BigDecimal theoryScore;
  private BigDecimal skillScore;
  private BigDecimal comprehensiveScore;
  private Long peopleId;
  private Long classId;
  private String resultCode;
  private BigDecimal oldExamScore;
  private BigDecimal newExamScore;
  private BigDecimal oldRealScore;
  private BigDecimal newRealScore;
  private Integer theoryFallback;
}

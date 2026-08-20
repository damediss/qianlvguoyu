package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_exam_score_import_batch")
public class TrainExamScoreImportBatchEntity extends BaseEntity {

  private Long operatorUserId;
  private String operatorUsername;
  private String fileName;
  private String filePath;
  private String planName;
  private String planCode;
  private String examTimeText;
  private String layout;
  private String classIdsJson;
  private Long trainInfoId;
  private Integer syncExamPlan;
  private Integer totalRows;
  private Integer matchedCount;
  private Integer writtenCount;
  private Integer overwrittenCount;
  private Integer skippedCount;
  private Integer unmatchedCount;
}

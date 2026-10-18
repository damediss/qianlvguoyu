package com.guoyu.platform.exam.zhenti.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/** 批量导入记录（exam_zhenti_import_log）；进度即状态，D6 直接读表。 */
@TableName("exam_zhenti_import_log")
public class ExamZhentiImportLogEntity extends BaseEntity {

  private Long examId;
  private Long operatorId;
  private Integer total;
  private Integer finished;
  private String status;
  private String resultsJson;

  public Long getExamId() {
    return examId;
  }

  public void setExamId(Long examId) {
    this.examId = examId;
  }

  public Long getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(Long operatorId) {
    this.operatorId = operatorId;
  }

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public Integer getFinished() {
    return finished;
  }

  public void setFinished(Integer finished) {
    this.finished = finished;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResultsJson() {
    return resultsJson;
  }

  public void setResultsJson(String resultsJson) {
    this.resultsJson = resultsJson;
  }
}

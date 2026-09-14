package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_formal_exam_realtime_cursor")
public class CompetitionFormalExamRealtimeCursorEntity extends BaseEntity {

  private Long examSessionId;
  private Long lastSeq;
  private Long dispatchSeq;

  public Long getExamSessionId() {
    return examSessionId;
  }

  public void setExamSessionId(Long examSessionId) {
    this.examSessionId = examSessionId;
  }

  public Long getLastSeq() {
    return lastSeq;
  }

  public void setLastSeq(Long lastSeq) {
    this.lastSeq = lastSeq;
  }

  public Long getDispatchSeq() {
    return dispatchSeq;
  }

  public void setDispatchSeq(Long dispatchSeq) {
    this.dispatchSeq = dispatchSeq;
  }
}

package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_collection_queue")
public class PracticalCollectionQueueEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long statusId;
  private Integer queueNo;
  private String queueStatus;
  private String matchMethod;
  private LocalDateTime joinedAt;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private LocalDateTime removedAt;
  private Long operatorId;
  private String removeReason;
}

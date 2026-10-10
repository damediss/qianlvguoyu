package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_collection_exception")
public class PracticalCollectionExceptionEntity extends BaseEntity {
  private Long taskId;
  private Long examSessionId;
  private Long candidateId;
  private String exceptionType;
  private String exceptionStatus;
  private String releaseScope;
  private String reason;
  private String deviceSn;
  private String deviceStatus;
  private Long handledBy;
  private LocalDateTime handledAt;
  private String requestId;
}

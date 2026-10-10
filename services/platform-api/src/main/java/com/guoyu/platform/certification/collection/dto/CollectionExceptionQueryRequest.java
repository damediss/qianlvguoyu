package com.guoyu.platform.certification.collection.dto;

import com.guoyu.platform.common.api.PageQuery;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionExceptionQueryRequest extends PageQuery {
  private Long taskId;
  private Long examSessionId;
  private Long candidateId;

  @Size(max = 32)
  private String exceptionType;

  @Size(max = 32)
  private String exceptionStatus;

  @Size(max = 100)
  private String keyword;
}

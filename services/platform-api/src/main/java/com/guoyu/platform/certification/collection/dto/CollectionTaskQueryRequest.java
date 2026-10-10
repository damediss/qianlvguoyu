package com.guoyu.platform.certification.collection.dto;

import com.guoyu.platform.common.api.PageQuery;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionTaskQueryRequest extends PageQuery {
  @Size(max = 100)
  private String keyword;

  @Size(max = 40)
  private String status;

  @Size(max = 80)
  private String workTypeName;

  @Size(max = 40)
  private String levelCode;
}

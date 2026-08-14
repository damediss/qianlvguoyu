package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record SubsidyPackProfileVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String regionCode,
    String regionName,
    String trainMode,
    Integer policyYear,
    String profileVersion,
    String title,
    String status,
    Integer isDefault,
    String remark,
    List<SubsidyPackSlotVO> slots
) {
  public record SubsidyPackSlotVO(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      String slotCode,
      String title,
      boolean required,
      Integer sortOrder,
      String renderer,
      @JsonSerialize(using = ToStringSerializer.class) Long templateId,
      String dataBinding,
      String folderPath,
      String remark
  ) {}
}

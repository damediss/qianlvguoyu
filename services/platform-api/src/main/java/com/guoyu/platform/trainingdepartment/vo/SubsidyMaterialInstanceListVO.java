package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record SubsidyMaterialInstanceListVO(
    @JsonSerialize(using = ToStringSerializer.class) Long subsidyId,
    int total,
    int requiredTotal,
    int confirmedRequired,
    int hasFileCount,
    List<SubsidyMaterialGroupVO> groups,
    List<SubsidyMaterialInstanceVO> items) {}

package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record ExamScoreImportClassVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String title,
    @JsonSerialize(using = ToStringSerializer.class) Long companyId,
    String companyName,
    @JsonSerialize(using = ToStringSerializer.class) Long trainInfoId,
    String trainTitle,
    @JsonSerialize(using = ToStringSerializer.class) Long workTypeId,
    String workTypeTitle,
    Integer workTypeLevel) {}

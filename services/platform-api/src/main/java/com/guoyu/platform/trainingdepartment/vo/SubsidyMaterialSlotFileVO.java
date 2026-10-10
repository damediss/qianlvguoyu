package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record SubsidyMaterialSlotFileVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String fileName,
    String filePath,
    @JsonSerialize(using = ToStringSerializer.class) Long fileId) {}

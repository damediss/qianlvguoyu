package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record SubsidyMaterialInstanceVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long subsidyId,
    String slotCode,
    String materialType,
    String materialTypeLabel,
    String title,
    String status,
    String renderer,
    boolean required,
    @JsonSerialize(using = ToStringSerializer.class) Long currentVersionId,
    Integer versionNo,
    @JsonSerialize(using = ToStringSerializer.class) Long fileId,
    String filePath,
    String fileName,
    boolean hasFile,
    boolean photoPackReady,
    String photoPackPath,
    String photoPackTitle,
    int slotFileCount,
    List<SubsidyMaterialSlotFileVO> slotFiles) {}

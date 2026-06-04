package com.guoyu.platform.certification.dto;

public record PreparationMaterialItemSaveReq(
    Long candidateId,
    String itemCode,
    String itemName,
    Boolean required,
    Long fileId,
    String fileName,
    String remark
) {
}

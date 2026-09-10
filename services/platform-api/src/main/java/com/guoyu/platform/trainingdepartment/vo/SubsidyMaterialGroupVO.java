package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record SubsidyMaterialGroupVO(
    String materialType,
    String label,
    int count,
    List<SubsidyMaterialInstanceVO> items) {}

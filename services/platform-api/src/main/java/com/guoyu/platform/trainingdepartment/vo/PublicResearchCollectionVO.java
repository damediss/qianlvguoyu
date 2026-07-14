package com.guoyu.platform.trainingdepartment.vo;

import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;

public record PublicResearchCollectionVO(
    String title,
    String researchOrgTitle,
    String companyName,
    ResearchCollectionFieldOptions fieldOptions,
    boolean active
) {}

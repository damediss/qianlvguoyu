package com.guoyu.platform.trainingdepartment.vo;

import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;
import java.time.LocalDateTime;

public record TrainResearchCollectionVO(
    Long researchOrgId,
    String researchOrgTitle,
    Long targetOrgId,
    String targetOrgName,
    Long signUpId,
    String collectionStatus,
    Integer personnelCount,
    String qrcodeToken,
    boolean qrcodeReady,
    String collectionPageUrl,
    LocalDateTime collectionStartTime,
    ResearchCollectionFieldOptions fieldOptions
) {}

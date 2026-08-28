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
    ResearchCollectionFieldOptions fieldOptions,
    /** 禁用态：仅可查看，不可写入采集 */
    boolean readOnly,
    /** 调研企业 status=0 */
    boolean disabled
) {}

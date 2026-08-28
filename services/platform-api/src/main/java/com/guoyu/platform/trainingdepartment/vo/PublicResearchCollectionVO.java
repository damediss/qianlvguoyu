package com.guoyu.platform.trainingdepartment.vo;

import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;

public record PublicResearchCollectionVO(
    String title,
    String researchOrgTitle,
    String companyName,
    ResearchCollectionFieldOptions fieldOptions,
    /** 与 collectable 一致，兼容旧 H5 */
    boolean active,
    /** 是否允许提交/更新 */
    boolean collectable,
    /** collectable=false 时的说明 */
    String disabledReason
) {}

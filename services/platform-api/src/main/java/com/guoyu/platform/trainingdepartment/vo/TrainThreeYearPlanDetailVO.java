package com.guoyu.platform.trainingdepartment.vo;

import com.guoyu.platform.common.api.PageResponse;
import java.util.List;

public record TrainThreeYearPlanDetailVO(
    TrainThreeYearPlanVO plan,
    PageResponse<TrainThreeYearPlanItemVO> items,
    List<TrainThreeYearPlanMatrixVO> matrix,
    List<TrainThreeYearPlanDistributionVO> distribution
) {}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainBudgetVO;

public interface TrainBudgetService {
    PageResponse<TrainBudgetVO> page(TrainBudgetQueryReq req);
    TrainBudgetVO getById(Long id);
    TrainBudgetVO create(TrainBudgetCreateReq req);
    TrainBudgetVO update(Long id, TrainBudgetUpdateReq req);
    void delete(Long id);
}

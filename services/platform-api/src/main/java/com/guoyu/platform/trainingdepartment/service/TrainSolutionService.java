package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;

public interface TrainSolutionService {
    PageResponse<TrainSolutionVO> page(TrainSolutionQueryReq req);
    TrainSolutionVO getById(Long id);
    TrainSolutionVO create(TrainSolutionCreateReq req);
    TrainSolutionVO update(Long id, TrainSolutionUpdateReq req);
    void delete(Long id);
}

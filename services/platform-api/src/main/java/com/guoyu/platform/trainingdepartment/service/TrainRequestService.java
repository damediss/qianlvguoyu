package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainRequestVO;

public interface TrainRequestService {
    PageResponse<TrainRequestVO> page(TrainRequestQueryReq req);
    TrainRequestVO getById(Long id);
    TrainRequestVO create(TrainRequestCreateReq req);
    TrainRequestVO update(Long id, TrainRequestUpdateReq req);
    void delete(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainDeclareVO;

public interface TrainDeclareService {
    PageResponse<TrainDeclareVO> page(TrainDeclareQueryReq req);
    TrainDeclareVO getById(Long id);
    TrainDeclareVO create(TrainDeclareCreateReq req);
    TrainDeclareVO update(Long id, TrainDeclareUpdateReq req);
    void delete(Long id);
}

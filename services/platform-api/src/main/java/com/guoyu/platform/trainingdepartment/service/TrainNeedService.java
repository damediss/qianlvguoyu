package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainNeedVO;

public interface TrainNeedService {
    PageResponse<TrainNeedVO> page(TrainNeedQueryReq req);
    TrainNeedVO getById(Long id);
    TrainNeedVO create(TrainNeedCreateReq req);
    TrainNeedVO update(Long id, TrainNeedUpdateReq req);
    void delete(Long id);
}

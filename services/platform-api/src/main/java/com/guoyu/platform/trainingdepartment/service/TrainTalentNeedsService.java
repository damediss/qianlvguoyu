package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTalentNeedsVO;

public interface TrainTalentNeedsService {
    PageResponse<TrainTalentNeedsVO> page(TrainTalentNeedsQueryReq req);
    TrainTalentNeedsVO getById(Long id);
    TrainTalentNeedsVO create(TrainTalentNeedsCreateReq req);
    TrainTalentNeedsVO update(Long id, TrainTalentNeedsUpdateReq req);
    void delete(Long id);
}

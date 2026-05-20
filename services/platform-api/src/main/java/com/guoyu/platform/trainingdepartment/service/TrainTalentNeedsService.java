package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTalentNeedsVO;

/**
 * 人才需求管理服务接口
 */
public interface TrainTalentNeedsService {
    /** 分页查询人才需求 */
    PageResponse<TrainTalentNeedsVO> page(TrainTalentNeedsQueryReq req);
    /** 根据ID查询人才需求 */
    TrainTalentNeedsVO getById(Long id);
    /** 创建人才需求 */
    TrainTalentNeedsVO create(TrainTalentNeedsCreateReq req);
    /** 更新人才需求 */
    TrainTalentNeedsVO update(Long id, TrainTalentNeedsUpdateReq req);
    /** 删除人才需求 */
    void delete(Long id);
}

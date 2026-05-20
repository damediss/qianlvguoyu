package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainSummaryVO;

/**
 * 培训总结管理服务接口
 */
public interface TrainSummaryService {
    /** 分页查询培训总结 */
    PageResponse<TrainSummaryVO> page(TrainSummaryQueryReq req);
    /** 根据ID查询培训总结 */
    TrainSummaryVO getById(Long id);
    /** 创建培训总结 */
    TrainSummaryVO create(TrainSummaryCreateReq req);
    /** 更新培训总结 */
    TrainSummaryVO update(Long id, TrainSummaryUpdateReq req);
    /** 删除培训总结 */
    void delete(Long id);
}

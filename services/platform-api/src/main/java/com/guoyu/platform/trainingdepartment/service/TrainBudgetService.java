package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainBudgetVO;

/**
 * 培训预算管理服务接口
 */
public interface TrainBudgetService {
    /** 分页查询培训预算 */
    PageResponse<TrainBudgetVO> page(TrainBudgetQueryReq req);
    /** 根据ID查询培训预算 */
    TrainBudgetVO getById(Long id);
    /** 创建培训预算 */
    TrainBudgetVO create(TrainBudgetCreateReq req);
    /** 更新培训预算 */
    TrainBudgetVO update(Long id, TrainBudgetUpdateReq req);
    /** 删除培训预算 */
    void delete(Long id);
}

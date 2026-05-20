package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;

/**
 * 培训方案管理服务接口
 */
public interface TrainSolutionService {
    /** 分页查询培训方案 */
    PageResponse<TrainSolutionVO> page(TrainSolutionQueryReq req);
    /** 根据ID查询培训方案 */
    TrainSolutionVO getById(Long id);
    /** 创建培训方案 */
    TrainSolutionVO create(TrainSolutionCreateReq req);
    /** 更新培训方案 */
    TrainSolutionVO update(Long id, TrainSolutionUpdateReq req);
    /** 删除培训方案 */
    void delete(Long id);
    /** 提交培训方案审批 */
    TrainSolutionVO submit(Long id);
    /** 审批通过培训方案 */
    TrainSolutionVO approve(Long id);
    /** 驳回培训方案 */
    TrainSolutionVO reject(Long id, String reason);
}

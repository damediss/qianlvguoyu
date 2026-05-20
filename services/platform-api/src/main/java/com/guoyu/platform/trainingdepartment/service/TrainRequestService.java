package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainRequestVO;

/**
 * 培训申请管理服务接口
 */
public interface TrainRequestService {
    /** 分页查询培训申请 */
    PageResponse<TrainRequestVO> page(TrainRequestQueryReq req);
    /** 根据ID查询培训申请 */
    TrainRequestVO getById(Long id);
    /** 创建培训申请 */
    TrainRequestVO create(TrainRequestCreateReq req);
    /** 更新培训申请 */
    TrainRequestVO update(Long id, TrainRequestUpdateReq req);
    /** 删除培训申请 */
    void delete(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectVO;

/**
 * 培训项目管理服务接口
 */
public interface TrainProjectService {
    /** 分页查询培训项目 */
    PageResponse<TrainProjectVO> page(TrainProjectQueryReq req);
    /** 根据ID查询培训项目 */
    TrainProjectVO getById(Long id);
    /** 创建培训项目 */
    TrainProjectVO create(TrainProjectCreateReq req);
    /** 更新培训项目 */
    TrainProjectVO update(Long id, TrainProjectUpdateReq req);
    /** 删除培训项目 */
    void delete(Long id);
}

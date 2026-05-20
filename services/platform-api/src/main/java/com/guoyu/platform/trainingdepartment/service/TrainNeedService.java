package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainNeedVO;

/**
 * 培训需求管理服务接口
 */
public interface TrainNeedService {
    /** 分页查询培训需求 */
    PageResponse<TrainNeedVO> page(TrainNeedQueryReq req);
    /** 根据ID查询培训需求 */
    TrainNeedVO getById(Long id);
    /** 创建培训需求 */
    TrainNeedVO create(TrainNeedCreateReq req);
    /** 更新培训需求 */
    TrainNeedVO update(Long id, TrainNeedUpdateReq req);
    /** 删除培训需求 */
    void delete(Long id);
}

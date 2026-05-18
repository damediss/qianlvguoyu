package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserLearnLogVO;

/**
 * 用户学习日志管理服务接口
 */
public interface TrainUserLearnLogService {

    /**
     * 分页查询用户学习日志
     */
    PageResponse<TrainUserLearnLogVO> page(TrainUserLearnLogQueryReq req);

    /**
     * 根据ID查询用户学习日志
     */
    TrainUserLearnLogVO getById(Long id);

    /**
     * 创建用户学习日志
     */
    TrainUserLearnLogVO create(TrainUserLearnLogCreateReq req);

    /**
     * 更新用户学习日志
     */
    TrainUserLearnLogVO update(Long id, TrainUserLearnLogUpdateReq req);

    /**
     * 删除用户学习日志
     */
    void delete(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserExamLogVO;

/**
 * 用户考试日志管理服务接口
 */
public interface TrainUserExamLogService {

    /**
     * 分页查询用户考试日志
     */
    PageResponse<TrainUserExamLogVO> page(TrainUserExamLogQueryReq req);

    /**
     * 根据ID查询用户考试日志
     */
    TrainUserExamLogVO getById(Long id);

    /**
     * 创建用户考试日志
     */
    TrainUserExamLogVO create(TrainUserExamLogCreateReq req);

    /**
     * 更新用户考试日志
     */
    TrainUserExamLogVO update(Long id, TrainUserExamLogUpdateReq req);

    /**
     * 删除用户考试日志
     */
    void delete(Long id);
}

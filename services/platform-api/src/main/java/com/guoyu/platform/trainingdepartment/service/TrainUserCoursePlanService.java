package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePlanVO;

/**
 * 用户课程计划管理服务接口
 */
public interface TrainUserCoursePlanService {

    /**
     * 分页查询用户课程计划
     */
    PageResponse<TrainUserCoursePlanVO> page(TrainUserCoursePlanQueryReq req);

    /**
     * 根据ID查询用户课程计划
     */
    TrainUserCoursePlanVO getById(Long id);

    /**
     * 创建用户课程计划
     */
    TrainUserCoursePlanVO create(TrainUserCoursePlanCreateReq req);

    /**
     * 更新用户课程计划
     */
    TrainUserCoursePlanVO update(Long id, TrainUserCoursePlanUpdateReq req);

    /**
     * 删除用户课程计划
     */
    void delete(Long id);
}

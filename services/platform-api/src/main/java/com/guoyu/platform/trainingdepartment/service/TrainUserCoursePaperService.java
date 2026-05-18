package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePaperVO;

/**
 * 用户课程论文管理服务接口
 */
public interface TrainUserCoursePaperService {

    /**
     * 分页查询用户课程论文
     */
    PageResponse<TrainUserCoursePaperVO> page(TrainUserCoursePaperQueryReq req);

    /**
     * 根据ID查询用户课程论文
     */
    TrainUserCoursePaperVO getById(Long id);

    /**
     * 创建用户课程论文
     */
    TrainUserCoursePaperVO create(TrainUserCoursePaperCreateReq req);

    /**
     * 更新用户课程论文
     */
    TrainUserCoursePaperVO update(Long id, TrainUserCoursePaperUpdateReq req);

    /**
     * 删除用户课程论文
     */
    void delete(Long id);
}

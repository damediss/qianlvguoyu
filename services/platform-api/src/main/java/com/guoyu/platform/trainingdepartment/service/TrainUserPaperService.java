package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPaperVO;

/**
 * 用户论文管理服务接口
 */
public interface TrainUserPaperService {

    /**
     * 分页查询用户论文
     */
    PageResponse<TrainUserPaperVO> page(TrainUserPaperQueryReq req);

    /**
     * 根据ID查询用户论文
     */
    TrainUserPaperVO getById(Long id);

    /**
     * 创建用户论文
     */
    TrainUserPaperVO create(TrainUserPaperCreateReq req);

    /**
     * 更新用户论文
     */
    TrainUserPaperVO update(Long id, TrainUserPaperUpdateReq req);

    /**
     * 删除用户论文
     */
    void delete(Long id);
}

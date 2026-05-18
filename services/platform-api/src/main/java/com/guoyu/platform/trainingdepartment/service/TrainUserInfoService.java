package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserInfoVO;

/**
 * 用户信息管理服务接口
 */
public interface TrainUserInfoService {

    /**
     * 分页查询用户信息
     */
    PageResponse<TrainUserInfoVO> page(TrainUserInfoQueryReq req);

    /**
     * 根据ID查询用户信息
     */
    TrainUserInfoVO getById(Long id);

    /**
     * 创建用户信息
     */
    TrainUserInfoVO create(TrainUserInfoCreateReq req);

    /**
     * 更新用户信息
     */
    TrainUserInfoVO update(Long id, TrainUserInfoUpdateReq req);

    /**
     * 删除用户信息
     */
    void delete(Long id);
}

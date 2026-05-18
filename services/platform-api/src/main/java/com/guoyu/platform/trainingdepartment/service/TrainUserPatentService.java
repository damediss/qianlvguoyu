package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPatentVO;

/**
 * 用户专利管理服务接口
 */
public interface TrainUserPatentService {

    /**
     * 分页查询用户专利
     */
    PageResponse<TrainUserPatentVO> page(TrainUserPatentQueryReq req);

    /**
     * 根据ID查询用户专利
     */
    TrainUserPatentVO getById(Long id);

    /**
     * 创建用户专利
     */
    TrainUserPatentVO create(TrainUserPatentCreateReq req);

    /**
     * 更新用户专利
     */
    TrainUserPatentVO update(Long id, TrainUserPatentUpdateReq req);

    /**
     * 删除用户专利
     */
    void delete(Long id);
}

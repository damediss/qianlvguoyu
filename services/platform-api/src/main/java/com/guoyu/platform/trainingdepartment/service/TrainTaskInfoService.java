package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoVO;

/**
 * 培训任务信息管理服务接口
 */
public interface TrainTaskInfoService {

    /**
     * 分页查询培训任务信息
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainTaskInfoVO> page(TrainTaskInfoQueryReq req);

    /**
     * 根据ID查询培训任务信息
     *
     * @param id 主键ID
     * @return 培训任务信息VO
     */
    TrainTaskInfoVO getById(Long id);

    /**
     * 创建培训任务信息
     *
     * @param req 创建请求参数
     * @return 创建的培训任务信息VO
     */
    TrainTaskInfoVO create(TrainTaskInfoCreateReq req);

    /**
     * 更新培训任务信息
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训任务信息VO
     */
    TrainTaskInfoVO update(Long id, TrainTaskInfoUpdateReq req);

    /**
     * 删除培训任务信息（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

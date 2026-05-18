package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoDetailVO;

/**
 * 培训任务明细管理服务接口
 */
public interface TrainTaskInfoDetailService {

    /**
     * 分页查询培训任务明细
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainTaskInfoDetailVO> page(TrainTaskInfoDetailQueryReq req);

    /**
     * 根据ID查询培训任务明细
     *
     * @param id 主键ID
     * @return 培训任务明细VO
     */
    TrainTaskInfoDetailVO getById(Long id);

    /**
     * 创建培训任务明细
     *
     * @param req 创建请求参数
     * @return 创建的培训任务明细VO
     */
    TrainTaskInfoDetailVO create(TrainTaskInfoDetailCreateReq req);

    /**
     * 更新培训任务明细
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训任务明细VO
     */
    TrainTaskInfoDetailVO update(Long id, TrainTaskInfoDetailUpdateReq req);

    /**
     * 删除培训任务明细（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

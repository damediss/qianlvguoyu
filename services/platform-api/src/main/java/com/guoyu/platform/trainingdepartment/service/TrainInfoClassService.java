package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;

/**
 * 培训信息班级管理服务接口
 */
public interface TrainInfoClassService {

    /**
     * 分页查询培训信息班级列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainInfoClassVO> page(TrainInfoClassQueryReq req);

    /**
     * 根据ID查询培训信息班级
     *
     * @param id 主键ID
     * @return 培训信息班级VO
     */
    TrainInfoClassVO getById(Long id);

    /**
     * 创建培训信息班级
     *
     * @param req 创建请求参数
     * @return 创建后的培训信息班级VO
     */
    TrainInfoClassVO create(TrainInfoClassCreateReq req);

    /**
     * 更新培训信息班级
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训信息班级VO
     */
    TrainInfoClassVO update(Long id, TrainInfoClassUpdateReq req);

    /**
     * 删除培训信息班级（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

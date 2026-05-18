package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainResourceVO;

/**
 * 培训资源管理服务接口
 */
public interface TrainResourceService {

    /**
     * 分页查询培训资源列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainResourceVO> page(TrainResourceQueryReq req);

    /**
     * 根据ID查询培训资源
     *
     * @param id 主键ID
     * @return 培训资源VO
     */
    TrainResourceVO getById(Long id);

    /**
     * 创建培训资源
     *
     * @param req 创建请求参数
     * @return 创建后的培训资源VO
     */
    TrainResourceVO create(TrainResourceCreateReq req);

    /**
     * 更新培训资源
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训资源VO
     */
    TrainResourceVO update(Long id, TrainResourceUpdateReq req);

    /**
     * 删除培训资源（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

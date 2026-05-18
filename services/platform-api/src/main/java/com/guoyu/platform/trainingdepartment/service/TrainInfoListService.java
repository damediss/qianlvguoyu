package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoListVO;

/**
 * 培训信息列表管理服务接口
 */
public interface TrainInfoListService {

    /**
     * 分页查询培训信息列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainInfoListVO> page(TrainInfoListQueryReq req);

    /**
     * 根据ID查询培训信息
     *
     * @param id 主键ID
     * @return 培训信息VO
     */
    TrainInfoListVO getById(Long id);

    /**
     * 创建培训信息
     *
     * @param req 创建请求参数
     * @return 创建的培训信息VO
     */
    TrainInfoListVO create(TrainInfoListCreateReq req);

    /**
     * 更新培训信息
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训信息VO
     */
    TrainInfoListVO update(Long id, TrainInfoListUpdateReq req);

    /**
     * 删除培训信息（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

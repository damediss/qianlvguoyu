package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainFileDetailVO;

/**
 * 文件详情管理服务接口
 */
public interface TrainFileDetailService {

    /**
     * 分页查询文件详情列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainFileDetailVO> page(TrainFileDetailQueryReq req);

    /**
     * 根据ID查询文件详情
     *
     * @param id 主键ID
     * @return 文件详情VO
     */
    TrainFileDetailVO getById(Long id);

    /**
     * 创建文件详情
     *
     * @param req 创建请求参数
     * @return 创建后的文件详情VO
     */
    TrainFileDetailVO create(TrainFileDetailCreateReq req);

    /**
     * 更新文件详情
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的文件详情VO
     */
    TrainFileDetailVO update(Long id, TrainFileDetailUpdateReq req);

    /**
     * 删除文件详情
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

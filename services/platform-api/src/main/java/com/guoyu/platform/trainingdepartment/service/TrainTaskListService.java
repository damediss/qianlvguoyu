package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskListVO;

/**
 * 培训任务列表管理服务接口
 */
public interface TrainTaskListService {

    /**
     * 分页查询培训任务列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainTaskListVO> page(TrainTaskListQueryReq req);

    /**
     * 根据ID查询培训任务
     *
     * @param id 主键ID
     * @return 培训任务VO
     */
    TrainTaskListVO getById(Long id);

    /**
     * 创建培训任务
     *
     * @param req 创建请求参数
     * @return 创建的培训任务VO
     */
    TrainTaskListVO create(TrainTaskListCreateReq req);

    /**
     * 更新培训任务
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训任务VO
     */
    TrainTaskListVO update(Long id, TrainTaskListUpdateReq req);

    /**
     * 删除培训任务（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

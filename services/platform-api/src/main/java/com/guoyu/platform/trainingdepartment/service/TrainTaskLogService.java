package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskLogVO;

/**
 * 培训任务日志管理服务接口
 */
public interface TrainTaskLogService {

    /**
     * 分页查询培训任务日志
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainTaskLogVO> page(TrainTaskLogQueryReq req);

    /**
     * 根据ID查询培训任务日志
     *
     * @param id 主键ID
     * @return 培训任务日志VO
     */
    TrainTaskLogVO getById(Long id);

    /**
     * 创建培训任务日志
     *
     * @param req 创建请求参数
     * @return 创建的培训任务日志VO
     */
    TrainTaskLogVO create(TrainTaskLogCreateReq req);

    /**
     * 更新培训任务日志
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训任务日志VO
     */
    TrainTaskLogVO update(Long id, TrainTaskLogUpdateReq req);

    /**
     * 删除培训任务日志
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

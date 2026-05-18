package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;

/**
 * 考试计划管理服务接口
 */
public interface TrainExamListService {

    /**
     * 分页查询考试计划
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainExamListVO> page(TrainExamListQueryReq req);

    /**
     * 根据ID查询考试计划
     *
     * @param id 主键ID
     * @return 考试计划VO
     */
    TrainExamListVO getById(Long id);

    /**
     * 创建考试计划
     *
     * @param req 创建请求参数
     * @return 创建后的考试计划VO
     */
    TrainExamListVO create(TrainExamListCreateReq req);

    /**
     * 更新考试计划
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的考试计划VO
     */
    TrainExamListVO update(Long id, TrainExamListUpdateReq req);

    /**
     * 删除考试计划（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

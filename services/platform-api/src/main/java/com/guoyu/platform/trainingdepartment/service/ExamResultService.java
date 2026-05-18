package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.ExamResultCreateReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultQueryReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.ExamResultVO;

/**
 * 考试结果管理服务接口
 */
public interface ExamResultService {

    /**
     * 分页查询考试结果
     *
     * @param req 查询请求
     * @return 分页结果
     */
    PageResponse<ExamResultVO> page(ExamResultQueryReq req);

    /**
     * 根据ID查询考试结果
     *
     * @param id 主键
     * @return 考试结果
     */
    ExamResultVO getById(Long id);

    /**
     * 创建考试结果
     *
     * @param req 创建请求
     * @return 创建后的考试结果
     */
    ExamResultVO create(ExamResultCreateReq req);

    /**
     * 更新考试结果
     *
     * @param id 主键
     * @param req 更新请求
     * @return 更新后的考试结果
     */
    ExamResultVO update(Long id, ExamResultUpdateReq req);

    /**
     * 删除考试结果（软删除）
     *
     * @param id 主键
     */
    void delete(Long id);
}

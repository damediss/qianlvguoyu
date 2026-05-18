package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainCourseVO;

/**
 * 培训课程管理服务接口
 */
public interface TrainCourseService {

    /**
     * 分页查询培训课程列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainCourseVO> page(TrainCourseQueryReq req);

    /**
     * 根据ID查询培训课程
     *
     * @param id 主键ID
     * @return 培训课程VO
     */
    TrainCourseVO getById(Long id);

    /**
     * 创建培训课程
     *
     * @param req 创建请求参数
     * @return 创建后的培训课程VO
     */
    TrainCourseVO create(TrainCourseCreateReq req);

    /**
     * 更新培训课程
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训课程VO
     */
    TrainCourseVO update(Long id, TrainCourseUpdateReq req);

    /**
     * 删除培训课程（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

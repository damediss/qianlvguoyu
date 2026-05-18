package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianCreateReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianQueryReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.CourseKejianVO;

/**
 * 课程课件管理服务接口
 */
public interface CourseKejianService {

    /**
     * 分页查询课程课件
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<CourseKejianVO> page(CourseKejianQueryReq req);

    /**
     * 根据ID查询课程课件
     *
     * @param id 主键ID
     * @return 课程课件VO
     */
    CourseKejianVO getById(Long id);

    /**
     * 创建课程课件
     *
     * @param req 创建请求参数
     * @return 创建的课程课件VO
     */
    CourseKejianVO create(CourseKejianCreateReq req);

    /**
     * 更新课程课件
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的课程课件VO
     */
    CourseKejianVO update(Long id, CourseKejianUpdateReq req);

    /**
     * 删除课程课件
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.AssignClassReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainClassListVO;

/**
 * 培训班级列表管理服务接口
 */
public interface TrainClassListService {

    /**
     * 分页查询培训班级列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainClassListVO> page(TrainClassListQueryReq req);

    /**
     * 根据ID查询培训班级
     *
     * @param id 主键ID
     * @return 培训班级VO
     */
    TrainClassListVO getById(Long id);

    /**
     * 创建培训班级
     *
     * @param req 创建请求参数
     * @return 创建后的培训班级VO
     */
    TrainClassListVO create(TrainClassListCreateReq req);

    /**
     * 更新培训班级
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训班级VO
     */
    TrainClassListVO update(Long id, TrainClassListUpdateReq req);

    /**
     * 删除培训班级（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 批量分配学员到班级
     *
     * @param req 分配请求（classId + userIds）
     */
    void batchAssign(AssignClassReq req);

    /**
     * 开班
     *
     * @param id 班级ID
     * @return 更新后的班级VO
     */
    TrainClassListVO openClass(Long id);

    /**
     * 结课
     *
     * @param id 班级ID
     * @return 更新后的班级VO
     */
    TrainClassListVO closeClass(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;

/**
 * 培训人员列表管理服务接口
 */
public interface TrainPeopleListService {

    /**
     * 分页查询培训人员列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainPeopleListVO> page(TrainPeopleListQueryReq req);

    /**
     * 根据ID查询培训人员
     *
     * @param id 主键ID
     * @return 培训人员VO
     */
    TrainPeopleListVO getById(Long id);

    /**
     * 创建培训人员
     *
     * @param req 创建请求参数
     * @return 创建后的培训人员VO
     */
    TrainPeopleListVO create(TrainPeopleListCreateReq req);

    /**
     * 更新培训人员
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训人员VO
     */
    TrainPeopleListVO update(Long id, TrainPeopleListUpdateReq req);

    /**
     * 删除培训人员（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 批量审核报名人员
     *
     * @param req 审核请求（ids + status）
     */
    void batchAudit(SignupAuditReq req);
}

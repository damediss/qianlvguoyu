package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankVO;

/**
 * 培训师资库管理服务接口
 */
public interface TrainTeacherBankService {

    /**
     * 分页查询培训师资库
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainTeacherBankVO> page(TrainTeacherBankQueryReq req);

    /**
     * 根据ID查询培训师资
     *
     * @param id 主键ID
     * @return 培训师资VO
     */
    TrainTeacherBankVO getById(Long id);

    /**
     * 创建培训师资
     *
     * @param req 创建请求参数
     * @return 创建的培训师资VO
     */
    TrainTeacherBankVO create(TrainTeacherBankCreateReq req);

    /**
     * 更新培训师资
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的培训师资VO
     */
    TrainTeacherBankVO update(Long id, TrainTeacherBankUpdateReq req);

    /**
     * 删除培训师资（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

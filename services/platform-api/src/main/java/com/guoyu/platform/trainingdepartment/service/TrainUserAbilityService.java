package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainUserAbilityVO;

/**
 * 用户能力管理服务接口
 */
public interface TrainUserAbilityService {

    /**
     * 分页查询用户能力
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainUserAbilityVO> page(TrainUserAbilityQueryReq req);

    /**
     * 根据ID查询用户能力
     *
     * @param id 主键ID
     * @return 用户能力VO
     */
    TrainUserAbilityVO getById(Long id);

    /**
     * 创建用户能力
     *
     * @param req 创建请求参数
     * @return 创建的用户能力VO
     */
    TrainUserAbilityVO create(TrainUserAbilityCreateReq req);

    /**
     * 更新用户能力
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的用户能力VO
     */
    TrainUserAbilityVO update(Long id, TrainUserAbilityUpdateReq req);

    /**
     * 删除用户能力（逻辑删除）
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainDeclareVO;

/**
 * 培训申报管理服务接口
 */
public interface TrainDeclareService {
    /** 分页查询培训申报 */
    PageResponse<TrainDeclareVO> page(TrainDeclareQueryReq req);
    /** 根据ID查询培训申报 */
    TrainDeclareVO getById(Long id);
    /** 创建培训申报 */
    TrainDeclareVO create(TrainDeclareCreateReq req);
    /** 更新培训申报 */
    TrainDeclareVO update(Long id, TrainDeclareUpdateReq req);
    /** 删除培训申报 */
    void delete(Long id);
}

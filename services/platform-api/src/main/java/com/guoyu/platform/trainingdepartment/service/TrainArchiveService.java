package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainArchiveVO;

/**
 * 培训档案管理服务接口
 */
public interface TrainArchiveService {
    /** 分页查询培训档案 */
    PageResponse<TrainArchiveVO> page(TrainArchiveQueryReq req);
    /** 根据ID查询培训档案 */
    TrainArchiveVO getById(Long id);
    /** 创建培训档案 */
    TrainArchiveVO create(TrainArchiveCreateReq req);
    /** 更新培训档案 */
    TrainArchiveVO update(Long id, TrainArchiveUpdateReq req);
    /** 删除培训档案 */
    void delete(Long id);
}

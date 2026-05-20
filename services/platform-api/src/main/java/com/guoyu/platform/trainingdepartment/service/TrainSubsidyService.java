package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;

/**
 * 培训补贴管理服务接口
 */
public interface TrainSubsidyService {
    /** 分页查询补贴记录 */
    PageResponse<TrainSubsidyVO> page(TrainSubsidyQueryReq req);
    /** 根据ID查询补贴记录 */
    TrainSubsidyVO getById(Long id);
    /** 创建补贴记录 */
    TrainSubsidyVO create(TrainSubsidyCreateReq req);
    /** 更新补贴记录 */
    TrainSubsidyVO update(Long id, TrainSubsidyUpdateReq req);
    /** 删除补贴记录 */
    void delete(Long id);
    /** 提交补贴审批 */
    TrainSubsidyVO submit(Long id);
    /** 驳回补贴 */
    TrainSubsidyVO reject(Long id, String reason);
    /** 审批通过补贴 */
    TrainSubsidyVO approve(Long id);
}

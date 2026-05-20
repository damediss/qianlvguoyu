package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainSettlementVO;

/**
 * 培训结算管理服务接口
 */
public interface TrainSettlementService {
    /** 分页查询结算记录 */
    PageResponse<TrainSettlementVO> page(TrainSettlementQueryReq req);
    /** 根据ID查询结算记录 */
    TrainSettlementVO getById(Long id);
    /** 创建结算记录 */
    TrainSettlementVO create(TrainSettlementCreateReq req);
    /** 更新结算记录 */
    TrainSettlementVO update(Long id, TrainSettlementUpdateReq req);
    /** 删除结算记录 */
    void delete(Long id);
    /** 提交结算审批 */
    TrainSettlementVO submit(Long id);
    /** 审批通过结算 */
    TrainSettlementVO approve(Long id);
    /** 驳回结算 */
    TrainSettlementVO reject(Long id, String reason);
}

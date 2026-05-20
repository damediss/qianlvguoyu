package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainProtocolVO;

/**
 * 培训协议管理服务接口
 */
public interface TrainProtocolService {
    /** 分页查询培训协议 */
    PageResponse<TrainProtocolVO> page(TrainProtocolQueryReq req);
    /** 根据ID查询培训协议 */
    TrainProtocolVO getById(Long id);
    /** 创建培训协议 */
    TrainProtocolVO create(TrainProtocolCreateReq req);
    /** 更新培训协议 */
    TrainProtocolVO update(Long id, TrainProtocolUpdateReq req);
    /** 删除培训协议 */
    void delete(Long id);
    /** 提交审核 */
    TrainProtocolVO submitReview(Long id);
    /** 办公室审批 */
    TrainProtocolVO officeApprove(Long id);
    /** 总经理审批 */
    TrainProtocolVO gmApprove(Long id);
    /** 企业确认 */
    TrainProtocolVO enterpriseConfirm(Long id);
    /** 签署协议 */
    TrainProtocolVO sign(Long id);
    /** 驳回协议 */
    TrainProtocolVO reject(Long id, String reason);
}

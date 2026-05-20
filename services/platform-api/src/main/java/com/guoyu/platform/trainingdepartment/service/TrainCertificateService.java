package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainCertificateVO;

import java.util.List;

/**
 * 培训证书管理服务接口
 */
public interface TrainCertificateService {
    /** 分页查询证书 */
    PageResponse<TrainCertificateVO> page(TrainCertificateQueryReq req);
    /** 根据ID查询证书 */
    TrainCertificateVO getById(Long id);
    /** 创建证书 */
    TrainCertificateVO create(TrainCertificateCreateReq req);
    /** 更新证书 */
    TrainCertificateVO update(Long id, TrainCertificateUpdateReq req);
    /** 删除证书 */
    void delete(Long id);
    /** 批量颁发证书 */
    List<TrainCertificateVO> batchIssue(List<Long> studentIds, Long projectId);
}

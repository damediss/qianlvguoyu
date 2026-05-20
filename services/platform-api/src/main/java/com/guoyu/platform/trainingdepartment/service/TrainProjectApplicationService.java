package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectVO;

import java.util.List;

/**
 * 培训项目申请流程服务接口
 */
public interface TrainProjectApplicationService {
    /** 发起项目申请 */
    TrainProjectVO launch(Long id);
    /** 审批通过项目 */
    TrainProjectVO approve(Long id);
    /** 驳回项目申请 */
    TrainProjectVO reject(Long id, String reason);
    /** 流转到指定阶段 */
    TrainProjectVO transit(Long id, String targetStage);
    /** 获取项目关联表单 */
    List<TrainProjectTableVO> getProjectTables(Long projectId);
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import java.util.List;

/**
 * 培训排课管理服务接口
 */
public interface TrainScheduleService {
    /** 检查排课冲突 */
    List<TrainScheduleConflictVO> checkConflicts(Long classId);
}

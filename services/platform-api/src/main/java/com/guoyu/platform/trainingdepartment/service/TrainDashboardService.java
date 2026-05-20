package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.trainingdepartment.vo.TrainDashboardVO;

import java.util.List;
import java.util.Map;

/**
 * 培训数据看板服务接口
 */
public interface TrainDashboardService {
    /** 获取看板汇总数据 */
    TrainDashboardVO getDashboard();
    /** 获取里程碑列表 */
    List<TrainDashboardVO.MilestoneItem> getMilestones();
    /** 获取阶段分布统计 */
    Map<String, Long> getStageDistribution();
}

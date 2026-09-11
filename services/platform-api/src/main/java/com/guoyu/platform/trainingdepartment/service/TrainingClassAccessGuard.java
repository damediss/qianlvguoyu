package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import org.springframework.stereotype.Service;

/**
 * 培训班级可达性：加载班级并按组织层 Scope 校验（企业强制本企业）。
 * 供考勤 / 补贴 / 结业等过程模块复用，避免各服务重复 select + assert。
 */
@Service
public class TrainingClassAccessGuard {

  private final TrainInfoClassMapper classMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public TrainingClassAccessGuard(
      TrainInfoClassMapper classMapper, TrainingOperatorScopeService operatorScopeService) {
    this.classMapper = classMapper;
    this.operatorScopeService = operatorScopeService;
  }

  public TrainInfoClassEntity requireAccessibleClass(Long classId) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级 ID 无效");
    }
    TrainInfoClassEntity clazz = classMapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, classId)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(clazz);
    return clazz;
  }

  public void assertClassIdAccessible(Long classId) {
    if (classId == null) {
      return;
    }
    requireAccessibleClass(classId);
  }
}

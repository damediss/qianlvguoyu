package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;

/** 从报名人员复制基础字段到开班人员列表（遗留表结构仅保留核心列） */
public final class TrainPeopleListSignUpFieldMapper {

  private TrainPeopleListSignUpFieldMapper() {}

  public static void applySignUpFields(TrainPeopleListEntity entity, TrainSignUpPeopleSelfEntity signUp) {
    if (entity == null || signUp == null) {
      return;
    }
    entity.setUserId(signUp.getUserId());
  }
}

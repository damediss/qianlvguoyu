package com.guoyu.platform.exam.reviewsync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AiReviewPlanDayMapper extends BaseMapper<AiReviewPlanDayEntity> {

  /**
   * 当前读：并发物化撞唯一键 uk_ai_review_day_plan_day 后取回已提交的当天任务行。
   * 必须在持有计划行锁（{@link AiReviewPlanMapper#selectByIdForUpdate}）之后调用，保证加锁顺序一致。
   */
  @Select("""
      SELECT *
      FROM ai_review_plan_day
      WHERE plan_id = #{planId}
        AND day_index = #{dayIndex}
        AND deleted = 0
      LIMIT 1
      FOR UPDATE
      """)
  AiReviewPlanDayEntity selectByPlanAndDayForUpdate(@Param("planId") long planId,
      @Param("dayIndex") int dayIndex);
}

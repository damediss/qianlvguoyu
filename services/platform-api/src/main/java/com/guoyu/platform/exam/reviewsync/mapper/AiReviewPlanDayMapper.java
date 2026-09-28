package com.guoyu.platform.exam.reviewsync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
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

  /**
   * 当前读：指定日期起（含）未开始的日任务行（新题/复盘零作答），供考试日期变更重排
   * 先取 dayIndex 失效鼓励语缓存、再物理删除。必须在持有计划行锁之后调用。
   */
  @Select("""
      SELECT *
      FROM ai_review_plan_day
      WHERE plan_id = #{planId}
        AND plan_date >= #{fromDate}
        AND new_done = 0
        AND review_done = 0
        AND deleted = 0
      FOR UPDATE
      """)
  List<AiReviewPlanDayEntity> selectUnstartedFromForUpdate(@Param("planId") long planId,
      @Param("fromDate") LocalDate fromDate);

  /**
   * 物理删除指定日期起（含）未开始的日任务行（考试日期变更重排专用）。
   * 不走 @TableLogic 逻辑删：uk_ai_review_day_plan_day 不含 deleted，逻辑删会挡住同
   * (plan_id, day_index) 重物化；且被删行零作答（无 answer 挂靠），物理删不丢学员数据。
   * 与 {@link #selectUnstartedFromForUpdate} 同条件，须在持有计划行锁之后调用。
   *
   * @return 实际删除行数
   */
  @Delete("""
      DELETE FROM ai_review_plan_day
      WHERE plan_id = #{planId}
        AND plan_date >= #{fromDate}
        AND new_done = 0
        AND review_done = 0
      """)
  int deleteUnstartedFrom(@Param("planId") long planId, @Param("fromDate") LocalDate fromDate);
}

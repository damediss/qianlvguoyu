package com.guoyu.platform.exam.reviewsync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AiReviewPlanMapper extends BaseMapper<AiReviewPlanEntity> {

  /**
   * 行锁 + 当前读（REPEATABLE READ 下普通 SELECT 走旧快照，看不到并发已提交行）。
   * 用途：串行化同一计划的物化与指针推进，读到的也是最新已提交状态。
   */
  @Select("SELECT * FROM ai_review_plan WHERE id = #{id} AND deleted = 0 FOR UPDATE")
  AiReviewPlanEntity selectByIdForUpdate(@Param("id") long id);

  /** 当前读：并发建计划撞唯一键 uk_ai_review_plan_user_exam 后取回已提交行 */
  @Select("""
      SELECT *
      FROM ai_review_plan
      WHERE platform_user_id = #{platformUserId}
        AND exam_id = #{examId}
        AND deleted = 0
      LIMIT 1
      FOR UPDATE
      """)
  AiReviewPlanEntity selectByUserAndExamForUpdate(@Param("platformUserId") long platformUserId,
      @Param("examId") long examId);
}

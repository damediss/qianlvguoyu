package com.guoyu.platform.exam.theorygen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TheoryGenBatchMapper extends BaseMapper<TheoryGenBatchEntity> {

  /** 原子递增已完成考评点计数（并累计题数） */
  @Update("UPDATE exam_theory_gen_batch SET done_points = done_points + 1, "
      + "total_questions = total_questions + #{questions}, "
      + "token_usage = token_usage + #{tokens}, updated_at = NOW() WHERE id = #{batchId}")
  int incrementDone(@Param("batchId") Long batchId, @Param("questions") int questions,
      @Param("tokens") long tokens);

  /** 原子递增失败考评点计数 */
  @Update("UPDATE exam_theory_gen_batch SET failed_points = failed_points + 1, "
      + "updated_at = NOW() WHERE id = #{batchId}")
  int incrementFailed(@Param("batchId") Long batchId);
}

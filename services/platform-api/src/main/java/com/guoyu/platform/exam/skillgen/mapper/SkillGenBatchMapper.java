package com.guoyu.platform.exam.skillgen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkillGenBatchMapper extends BaseMapper<SkillGenBatchEntity> {

  /** 原子递增已完成考评点计数（并累计题数与 usage 明细；并发块下 SQL 原子，无丢计） */
  @Update("UPDATE exam_skill_gen_batch SET done_points = done_points + 1, "
      + "total_questions = total_questions + #{questions}, "
      + "token_usage = token_usage + #{totalTokens}, "
      + "prompt_tokens = prompt_tokens + #{promptTokens}, "
      + "completion_tokens = completion_tokens + #{completionTokens}, "
      + "cached_tokens = cached_tokens + #{cachedTokens}, "
      + "updated_at = NOW() WHERE id = #{batchId}")
  int incrementDone(@Param("batchId") Long batchId, @Param("questions") int questions,
      @Param("totalTokens") long totalTokens, @Param("promptTokens") long promptTokens,
      @Param("completionTokens") long completionTokens, @Param("cachedTokens") long cachedTokens);

  /** 原子递增失败考评点计数（失败块的已产生 usage 同样累计——那是真实花费） */
  @Update("UPDATE exam_skill_gen_batch SET failed_points = failed_points + 1, "
      + "token_usage = token_usage + #{totalTokens}, "
      + "prompt_tokens = prompt_tokens + #{promptTokens}, "
      + "completion_tokens = completion_tokens + #{completionTokens}, "
      + "cached_tokens = cached_tokens + #{cachedTokens}, "
      + "updated_at = NOW() WHERE id = #{batchId}")
  int incrementFailed(@Param("batchId") Long batchId, @Param("totalTokens") long totalTokens,
      @Param("promptTokens") long promptTokens, @Param("completionTokens") long completionTokens,
      @Param("cachedTokens") long cachedTokens);
}

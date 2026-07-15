package com.guoyu.platform.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.entity.ExamFaqConversionBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExamFaqConversionBatchMapper extends BaseMapper<ExamFaqConversionBatchEntity> {

  /** 原子递增成功计数 */
  @Update("UPDATE exam_faq_conversion_batch SET success_count = success_count + 1, updated_at = NOW() WHERE id = #{batchId}")
  int incrementSuccess(@Param("batchId") Long batchId);

  /** 原子递增失败计数 */
  @Update("UPDATE exam_faq_conversion_batch SET failed_count = failed_count + 1, updated_at = NOW() WHERE id = #{batchId}")
  int incrementFailed(@Param("batchId") Long batchId);

  /** 原子递增跳过计数 */
  @Update("UPDATE exam_faq_conversion_batch SET skipped_count = skipped_count + 1, updated_at = NOW() WHERE id = #{batchId}")
  int incrementSkipped(@Param("batchId") Long batchId);

  /** 原子递减待转换计数 */
  @Update("UPDATE exam_faq_conversion_batch SET pending_count = GREATEST(pending_count - 1, 0), updated_at = NOW() WHERE id = #{batchId}")
  int decrementPending(@Param("batchId") Long batchId);
}

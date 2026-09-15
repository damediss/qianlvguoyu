package com.guoyu.platform.exam.knowledgefix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 知识归属修正分类批次 Mapper */
@Mapper
public interface ExamKnowledgeFixBatchMapper extends BaseMapper<ExamKnowledgeFixBatchEntity> {

  /** 批次计数推进（done+1；outcome 计数同步 +1） */
  @Update("""
      <script>UPDATE exam_knowledge_fix_batch SET done = done + 1,
      <choose><when test="outcome == 'apply'">apply_cnt = apply_cnt + 1</when>
      <when test="outcome == 'keep'">keep_cnt = keep_cnt + 1</when>
      <otherwise>flag_cnt = flag_cnt + 1</otherwise></choose>,
      updated_at = NOW() WHERE batch_no = #{batchNo} AND deleted = 0</script>
      """)
  int incrementClassified(@Param("batchNo") Long batchNo, @Param("outcome") String outcome);

  /** 批次失败计数推进（失败行待补跑轮重试） */
  @Update("UPDATE exam_knowledge_fix_batch SET failed = failed + 1, updated_at = NOW() "
      + "WHERE batch_no = #{batchNo} AND deleted = 0")
  int incrementFailed(@Param("batchNo") Long batchNo);

  /**
   * 未分类行计数（FIX-1.3）：只累加 flag_cnt，**不计入 done**——done 定义为"进过模型且有结论"的行，
   * 未分类行数由 total - done - failed 推导（页面"未分类"口径）。
   */
  @Update("UPDATE exam_knowledge_fix_batch SET flag_cnt = flag_cnt + #{count}, updated_at = NOW() "
      + "WHERE batch_no = #{batchNo} AND deleted = 0")
  int incrementUnclassified(@Param("batchNo") Long batchNo, @Param("count") int count);
}

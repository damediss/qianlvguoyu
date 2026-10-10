package com.guoyu.platform.exam.knowledgefix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 知识归属修正 LLM 调用审计 Mapper */
@Mapper
public interface ExamKnowledgeFixLlmLogMapper extends BaseMapper<ExamKnowledgeFixLlmLogEntity> {

  /**
   * 批次累计用量（FIX-10/v1.7）：进度接口透出，供"费用超预期就暂停"的现场判断；走 idx_kfll_batch。
   * cachedTokens = 命中前缀缓存的输入 token（智谱 prompt_tokens_details.cached_tokens /
   * DeepSeek prompt_cache_hit_tokens，已在 LlmChatCompletionResult 归一）。
   */
  @Select("""
      SELECT COUNT(*) AS calls,
             COALESCE(SUM(prompt_tokens), 0) AS promptTokens,
             COALESCE(SUM(completion_tokens), 0) AS completionTokens,
             COALESCE(SUM(total_tokens), 0) AS totalTokens,
             COALESCE(SUM(cached_tokens), 0) AS cachedTokens
      FROM exam_knowledge_fix_llm_log
      WHERE batch_no = #{batchNo} AND deleted = 0
      """)
  Map<String, Object> sumUsageByBatch(@Param("batchNo") Long batchNo);
}

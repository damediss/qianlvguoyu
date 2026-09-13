package com.guoyu.platform.exam.knowledgefix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity;
import org.apache.ibatis.annotations.Mapper;

/** 知识归属修正 LLM 调用审计 Mapper */
@Mapper
public interface ExamKnowledgeFixLlmLogMapper extends BaseMapper<ExamKnowledgeFixLlmLogEntity> {
}

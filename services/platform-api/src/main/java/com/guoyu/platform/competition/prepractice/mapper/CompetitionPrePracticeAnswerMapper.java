package com.guoyu.platform.competition.prepractice.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeAnswerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompetitionPrePracticeAnswerMapper
    extends BaseMapper<CompetitionPrePracticeAnswerEntity> {

  @InterceptorIgnore(tenantLine = "true")
  @Insert("""
      INSERT INTO competition_pre_practice_answer (
        id, tenant_id, delivery_id, task_id, task_question_id, player_id, answer_json,
        answered_at, deleted, created_by, updated_by, created_at, updated_at
      ) VALUES (
        #{answer.id}, #{answer.tenantId}, #{answer.deliveryId}, #{answer.taskId},
        #{answer.taskQuestionId}, #{answer.playerId}, #{answer.answerJson}, #{answer.answeredAt},
        0, #{answer.createdBy}, #{answer.updatedBy}, #{answer.createdAt}, #{answer.updatedAt}
      )
      ON DUPLICATE KEY UPDATE
        answer_json = VALUES(answer_json), answered_at = VALUES(answered_at),
        updated_by = VALUES(updated_by), updated_at = VALUES(updated_at), deleted = 0
      """)
  int upsert(@Param("answer") CompetitionPrePracticeAnswerEntity answer);
}

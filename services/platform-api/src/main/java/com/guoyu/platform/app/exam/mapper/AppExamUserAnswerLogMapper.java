package com.guoyu.platform.app.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.app.exam.entity.AppExamUserAnswerLogEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppExamUserAnswerLogMapper extends BaseMapper<AppExamUserAnswerLogEntity> {

  /** 按 knowledge_id 聚合错题数/答题数，用于考点预测 */
  @Select("""
      SELECT knowledge_id AS knowledgeId,
             SUM(CASE WHEN is_right = 0 THEN 1 ELSE 0 END) AS wrongCount,
             COUNT(*) AS totalCount
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND user_id = #{userId}
        AND knowledge_id IS NOT NULL
      GROUP BY knowledge_id
      """)
  List<Map<String, Object>> wrongStatsByKnowledge(@Param("tenantId") Long tenantId,
                                                   @Param("userId") Long userId);

  /** 错题 id 列表（按错题时间倒序，可选限定考点） */
  @Select("""
      <script>
      SELECT DISTINCT question_id
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND is_right = 0
      <if test="knowledgeId != null"> AND knowledge_id = #{knowledgeId} </if>
      ORDER BY id DESC
      LIMIT #{limit}
      </script>
      """)
  List<Integer> findWrongQuestionIds(@Param("tenantId") Long tenantId,
                                     @Param("userId") Long userId,
                                     @Param("knowledgeId") Integer knowledgeId,
                                     @Param("limit") Integer limit);
}

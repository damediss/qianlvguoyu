package com.guoyu.platform.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamExamDetailMapper extends BaseMapper<ExamExamDetailEntity> {

  /**
   * 聚合:按 knowledge_id 统计本租户的题目数量,只统计有效题目(逻辑删除=0 + knowledge_id 非空)。
   *
   * <p>直接走 MySQL 端 GROUP BY,不再把 (id, knowledge_id) 全部行拉到 Java 内存聚合。
   * 配套索引:V20260610_005 迁移的 idx_ed_tenant_knowledge_deleted(tenant_id, knowledge_id, deleted),
   * 该 SELECT 仅用索引列,可走覆盖扫描(Using index),无需回表。
   *
   * <p>为什么不走 MyBatis-Plus 多租户插件 + 自动 LambdaQueryWrapper:
   * 多租户插件会自动加 tenant_id 条件,但本题需要聚合且只 select knowledge_id + COUNT,
   * 手动写 SQL 更精准。deleted 由 @TableLogic 自动加 deleted=0,这里显式写出来更直观。
   *
   * @return [{ knowledge_id=123, cnt=42 }, ...],空租户返回 []
   */
  @Select("""
      SELECT knowledge_id AS knowledgeId, COUNT(*) AS cnt
      FROM exam_exam_detail
      WHERE tenant_id = #{tenantId}
        AND deleted = 0
        AND knowledge_id IS NOT NULL
      GROUP BY knowledge_id
      """)
  List<Map<String, Object>> countByKnowledgeIdGrouped(@Param("tenantId") Integer tenantId);
}

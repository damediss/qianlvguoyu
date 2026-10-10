package com.guoyu.platform.exam.standard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ExamStandardNodeMapper extends BaseMapper<ExamStandardNodeEntity> {

    /**
     * 重概括清理（C 方案 B）：物理删除本任务未审定的 LLM 生成叶子，腾空唯一键。
     * 只删 source=llm 且 review_status=pending 的行；已 approved 的与人工节点不动。
     */
    @Delete("DELETE FROM exam_standard_node WHERE task_id = #{taskId} "
        + "AND source = 'llm' AND review_status = 'pending'")
    int purgePendingLlmLeaves(@Param("taskId") Long taskId);

    /**
     * 同键查询（C 方案 A）：含软删行（绕过 @TableLogic）。
     * uk_standard_node_key = (task_id, level_code, direction, dimension, node_key)，
     * 软删行仍占用唯一键，裸 INSERT 同键必撞（踩坑 #22 同类）。
     */
    @Select("SELECT * FROM exam_standard_node WHERE task_id = #{taskId} "
        + "AND level_code = #{levelCode} AND direction = #{direction} "
        + "AND dimension = #{dimension} AND node_key = #{nodeKey} LIMIT 1")
    ExamStandardNodeEntity selectAnyByKey(@Param("taskId") Long taskId,
        @Param("levelCode") Integer levelCode, @Param("direction") String direction,
        @Param("dimension") String dimension, @Param("nodeKey") String nodeKey);

    /**
     * 复活软删叶子（C 方案 A）：物理恢复 deleted=0 并覆盖为新内容。
     * 实体 @TableLogic 使 updateById 自动追加 deleted=0 条件，无法更新软删行，须原生 SQL。
     */
    @Update("UPDATE exam_standard_node SET deleted = 0, title = #{title}, important = #{important}, "
        + "sort_number = #{sortNumber}, source = 'llm', review_status = 'pending', updated_at = NOW() "
        + "WHERE id = #{id}")
    int reviveLeaf(@Param("id") Long id, @Param("title") String title,
        @Param("important") String important, @Param("sortNumber") Integer sortNumber);
}

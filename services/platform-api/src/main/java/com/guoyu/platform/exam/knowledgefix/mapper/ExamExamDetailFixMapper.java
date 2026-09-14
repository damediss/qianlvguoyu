package com.guoyu.platform.exam.knowledgefix.mapper;

import com.guoyu.platform.exam.knowledgefix.dto.DetailRawRow;
import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 知识修正对 exam_exam_detail / exam_knowledge_structure / exam_user_answer_log 的读写通道。
 * 仅方案限定的语句；规范化口径与去重清单生成逐字符一致（防通用题干陷阱回归）。
 */
@Mapper
public interface ExamExamDetailFixMapper {

  /** 跨课程同名 title 集合（同名碰撞圈定依据） */
  @Select("""
      SELECT title FROM exam_knowledge_structure
      WHERE deleted = 0 GROUP BY title HAVING COUNT(DISTINCT course_id) > 1
      """)
  List<String> selectDuplicateTitles();

  /** 全部知识点节点（构建课程树快照与 id→course 映射） */
  @Select("""
      SELECT id, course_id AS courseId, parent_id AS parentId, title, level, important
      FROM exam_knowledge_structure WHERE deleted = 0
      """)
  List<KnowledgeNodeRow> selectAllNodes();

  /** 5-01 以来导入的候选原始行（exam_id=0；规范化在 Java 侧完成以便单测） */
  @Select("""
      SELECT id, type, content, answer, knowledge_id AS knowledgeId, create_time AS createTime
      FROM exam_exam_detail
      WHERE create_time >= '2026-05-01' AND deleted = 0 AND exam_id = 0
      """)
  List<DetailRawRow> selectMayCandidateRows();

  /** 有答题日志引用的候选题 id（防御过滤） */
  @Select("""
      SELECT DISTINCT l.question_id FROM exam_user_answer_log l
      JOIN exam_exam_detail e ON e.id = l.question_id
      WHERE e.create_time >= '2026-05-01' AND e.deleted = 0 AND e.exam_id = 0
      """)
  List<Integer> selectAnswerLogRefIds();

  /** 分类范围量化：去重后幸存集合中命中同名塌缩节点的行数 */
  @Select("""
      SELECT COUNT(*) FROM exam_exam_detail e
      JOIN exam_knowledge_structure k ON k.id = e.knowledge_id
      WHERE e.deleted = 0 AND k.title IN (
        SELECT title FROM exam_knowledge_structure
        WHERE deleted = 0 GROUP BY title HAVING COUNT(DISTINCT course_id) > 1)
      """)
  long countSurvivorSuspects();

  /** 创建备份表（整表结构；表名服务端生成，非用户输入） */
  @Update("CREATE TABLE IF NOT EXISTS ${backupTable} LIKE exam_exam_detail")
  int createBackupTable(@Param("backupTable") String backupTable);

  /** 备份指定行（幂等：已备份的行跳过） */
  @Insert("INSERT IGNORE INTO ${backupTable} "
      + "SELECT * FROM exam_exam_detail WHERE id IN (SELECT detail_id FROM ${listTable} WHERE status = 'approved' AND deleted = 0)")
  int backupRows(@Param("backupTable") String backupTable, @Param("listTable") String listTable);

  /** 按备份表回滚 deleted */
  @Update("UPDATE exam_exam_detail e JOIN ${backupTable} b ON b.id = e.id "
      + "SET e.deleted = b.deleted, e.update_time = NOW() WHERE e.deleted = 1 AND b.deleted = 0")
  int rollbackSoftDelete(@Param("backupTable") String backupTable);

  /** 按备份表回滚 knowledge_id */
  @Update("UPDATE exam_exam_detail e JOIN ${backupTable} b ON b.id = e.id "
      + "SET e.knowledge_id = b.knowledge_id, e.update_time = NOW() WHERE e.knowledge_id <> b.knowledge_id")
  int rollbackKnowledge(@Param("backupTable") String backupTable);

  /** 单行知识修正（PK 更新；方案 §6.4 仅 apply 行调用） */
  @Update("UPDATE exam_exam_detail SET knowledge_id = #{newKnowledgeId}, update_time = NOW() "
      + "WHERE id = #{detailId} AND deleted = 0")
  int updateKnowledgeId(@Param("detailId") int detailId, @Param("newKnowledgeId") int newKnowledgeId);

  /** 5-01 以来导入行计数（统计卡"总行数"） */
  @Select("SELECT COUNT(*) FROM exam_exam_detail "
      + "WHERE create_time >= '2026-05-01' AND deleted = 0 AND exam_id = 0")
  long selectMayRowCount();

  /** 幸存集合中命中同名塌缩节点的行 → 装入知识修正清单（幂等：已有行跳过） */
  @Insert("""
      INSERT INTO exam_detail_knowledge_fix
        (id, tenant_id, detail_id, old_knowledge_id, course_id, status,
         created_at, updated_at, created_by, updated_by, deleted)
      SELECT UUID_SHORT(), 10159, e.id, e.knowledge_id, k.course_id, 'pending',
             NOW(), NOW(), 0, 0, 0
      FROM exam_exam_detail e
      JOIN exam_knowledge_structure k ON k.id = e.knowledge_id
      WHERE e.deleted = 0 AND e.exam_id = 0
        AND k.title IN (
          SELECT title FROM exam_knowledge_structure
          WHERE deleted = 0 GROUP BY title HAVING COUNT(DISTINCT course_id) > 1)
        AND e.id NOT IN (SELECT detail_id FROM exam_detail_knowledge_fix WHERE deleted = 0)
      """)
  int populateFixList();
}

package com.guoyu.platform.exam.knowledgefix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailDedupListEntity;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 去重清单 Mapper。清单生成 = 单条 INSERT...SELECT（服务端集合运算，零应用层往返）；
 * 判重键与方案 §1.2 逐字符一致（含选项答案，防通用题干陷阱）。
 */
@Mapper
public interface ExamDetailDedupListMapper extends BaseMapper<ExamDetailDedupListEntity> {

  /** 清空未执行的 pending 清单行（幂等重生成；approved/done/rolled_back 行不动） */
  @Delete("DELETE FROM exam_detail_dedup_list WHERE status = 'pending'")
  int clearPending();

  /**
   * 生成去重清单（方案 §6.3；v1.5 按 FIX-7 裁定重写）：
   * 判重域 = **同课程**（判重键前置 course_id，取自 knowledge_id → exam_knowledge_structure，取 deleted=0 节点）；
   * 跨课程同题**不算重复**（需求方 2026-09-15 裁定）；无课程行（knowledge_id 为空/节点已删）归入 0 号"无课程桶"内部判重。
   * 候选 = 5-01 以来导入 + exam_id=0（去掉旧的"跨课程同名节点"限定，DEF-7）；
   * 历史已有同题（同课程/同桶）的组 → 全部 5 月副本软删（is_keeper=0）；仅 5 月的组 → 保留最早一份（rn=1 不入清单）。
   */
  @Insert("""
      INSERT INTO exam_detail_dedup_list
        (id, tenant_id, detail_id, group_key, is_keeper, keeper_id, create_wave, status,
         created_at, updated_at, created_by, updated_by, deleted)
      SELECT UUID_SHORT(), 10159, r.detail_id, r.group_key, 0, r.keeper_id, r.wave, 'pending',
             NOW(), NOW(), 0, 0, 0
      FROM (
        SELECT k.detail_id, k.group_key, k.wave, k.create_time,
               ROW_NUMBER() OVER (PARTITION BY k.group_key ORDER BY k.create_time, k.detail_id) AS rn,
               FIRST_VALUE(k.detail_id) OVER (PARTITION BY k.group_key ORDER BY k.create_time, k.detail_id) AS keeper_id
        FROM (
          SELECT e.id AS detail_id, e.create_time,
            MD5(CONCAT(COALESCE(ks.course_id, 0), '|', e.type, '|',
              COALESCE(REGEXP_REPLACE(REGEXP_REPLACE(REGEXP_REPLACE(e.content,'<[^>]+>',''),'&nbsp;',''),'[[:space:]　]+',''), ''),
              '|', COALESCE(REPLACE(REPLACE(REPLACE(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(e.answer,''),'[[:space:]　]+',''),'[{]"content":"",[^}]*[}]',''),',]',']'),',[','['),',,',','), ''))) AS group_key,
            CASE DATE(e.create_time)
              WHEN '2026-05-14' THEN '2026-05-14'
              WHEN '2026-07-30' THEN '2026-07-30'
              WHEN '2026-07-31' THEN '2026-07-31'
              ELSE 'other' END AS wave
          FROM exam_exam_detail e
          LEFT JOIN exam_knowledge_structure ks ON ks.id = e.knowledge_id AND ks.deleted = 0
          WHERE e.create_time >= '2026-05-01' AND e.deleted = 0 AND e.exam_id = 0
        ) k
      ) r
      JOIN (
        SELECT DISTINCT MD5(CONCAT(COALESCE(ks.course_id, 0), '|', e.type, '|',
          COALESCE(REGEXP_REPLACE(REGEXP_REPLACE(REGEXP_REPLACE(e.content,'<[^>]+>',''),'&nbsp;',''),'[[:space:]　]+',''), ''),
          '|', COALESCE(REPLACE(REPLACE(REPLACE(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(e.answer,''),'[[:space:]　]+',''),'[{]"content":"",[^}]*[}]',''),',]',']'),',[','['),',,',','), ''))) AS group_key
        FROM exam_exam_detail e
        LEFT JOIN exam_knowledge_structure ks ON ks.id = e.knowledge_id AND ks.deleted = 0
        WHERE e.create_time < '2026-05-01' AND e.deleted = 0
      ) h ON h.group_key = r.group_key
      WHERE (h.group_key IS NOT NULL) OR (h.group_key IS NULL AND r.rn > 1)
      """)
  int generateDedupList();

  /** 有答题日志引用的行 → 移入人工复核（防御过滤，KF-05） */
  @Update("""
      UPDATE exam_detail_dedup_list l
      JOIN exam_user_answer_log al ON al.question_id = l.detail_id
      SET l.status = 'manual_review', l.updated_at = NOW()
      WHERE l.status = 'pending'
      """)
  int markManualReviewRows();

  /**
   * 统计（FIX-5 口径修正；排除已回滚批次行，避免回滚后重复计数）：
   * dup_groups=重复题组数（同课程口径）、delete_rows=待执行删除行（pending/approved）、
   * deleted_rows=已软删行（done）、manual_rows=人工复核行。
   */
  @Select("""
      SELECT COUNT(DISTINCT CASE WHEN status IN ('pending','approved') THEN group_key END) AS dup_groups,
             SUM(status IN ('pending','approved')) AS delete_rows,
             SUM(status = 'done') AS deleted_rows,
             SUM(status = 'manual_review') AS manual_rows
      FROM exam_detail_dedup_list WHERE deleted = 0
      """)
  Map<String, Object> selectDedupStatsRaw();

  /** 分批取待软删行 id（每批 1,000，按 id 升序游标推进） */
  @Select("SELECT detail_id FROM exam_detail_dedup_list "
      + "WHERE is_keeper = 0 AND status = 'approved' AND detail_id > #{lastId} AND deleted = 0 "
      + "ORDER BY detail_id LIMIT #{limit}")
  java.util.List<Integer> selectApprovedDeleteIds(@Param("lastId") int lastId, @Param("limit") int limit);

  /** 按批次 id 集合软删除（返回实际影响行数；已删行不重复计） */
  @Update("""
      <script>UPDATE exam_exam_detail SET deleted = 1, update_time = NOW()
      WHERE deleted = 0 AND id IN
      <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>
      """)
  int softDeleteByIds(@Param("ids") java.util.List<Integer> ids);

  /** 清单行标记完成 */
  @Update("""
      UPDATE exam_detail_dedup_list SET status = 'done', updated_at = NOW()
      WHERE is_keeper = 0 AND status = 'approved' AND deleted = 0
      """)
  int markDedupDone();

  /** 去重回滚：按备份表还原 deleted */
  @Update("UPDATE exam_exam_detail e JOIN ${backupTable} b ON b.id = e.id "
      + "SET e.deleted = b.deleted, e.update_time = NOW() WHERE e.deleted = 1 AND b.deleted = 0")
  int rollbackSoftDelete(@Param("backupTable") String backupTable);

  /** pending → approved（执行按钮触发） */
  @Update("UPDATE exam_detail_dedup_list SET status = 'approved', updated_at = NOW() "
      + "WHERE status = 'pending' AND is_keeper = 0 AND deleted = 0")
  int approvePending();

  /** 人工复核行计数 */
  @Select("SELECT COUNT(*) FROM exam_detail_dedup_list "
      + "WHERE status = 'manual_review' AND deleted = 0")
  long selectManualReviewCount();

  /** 去重回滚后清单标记 */
  @Update("UPDATE exam_detail_dedup_list SET status = 'rolled_back', updated_at = NOW() "
      + "WHERE status = 'done' AND deleted = 0")
  int markDedupRolledBack();
}

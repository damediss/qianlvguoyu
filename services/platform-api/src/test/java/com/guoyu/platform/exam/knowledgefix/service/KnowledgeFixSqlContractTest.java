package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailDedupListMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

/**
 * 判重键/范围的 SQL 契约测试（KF-27/29/30）。
 *
 * <p>判重与范围由服务端单条 INSERT...SELECT 完成（零应用层往返），语义无法用纯 Java 单测覆盖；
 * 本类对 SQL 文本做契约断言，防止"跨课程同题不算重复"的口径被回退。
 * 线上正确性由演练库与生产执行的复核 SQL 验证（方案 §11.8 D）。
 */
class KnowledgeFixSqlContractTest {

  private static String insertSql(Class<?> mapper, String methodName) {
    Method m = Arrays.stream(mapper.getMethods())
        .filter(x -> x.getName().equals(methodName))
        .findFirst()
        .orElseThrow();
    Insert insert = m.getAnnotation(Insert.class);
    if (insert != null) {
      return String.join("\n", insert.value());
    }
    org.apache.ibatis.annotations.Update update = m.getAnnotation(
        org.apache.ibatis.annotations.Update.class);
    if (update != null) {
      return String.join("\n", update.value());
    }
    org.apache.ibatis.annotations.Select select = m.getAnnotation(
        org.apache.ibatis.annotations.Select.class);
    return select == null ? "" : String.join("\n", select.value());
  }

  /** KF-29：去重清单 SQL 必须带课程维度（跨课程同题不算重复，2026-09-15 裁定） */
  @Test
  void kf29_dedupKeyCarriesCourseDimension() {
    String sql = insertSql(ExamDetailDedupListMapper.class, "generateDedupList");
    assertTrue(sql.contains("COALESCE(ks.course_id, 0)"),
        "判重键必须前置课程维度（无课程行归 0 号桶）");
    assertTrue(sql.contains("LEFT JOIN exam_knowledge_structure ks"),
        "课程维度的来源：knowledge_id → exam_knowledge_structure（deleted=0）");
  }

  /** KF-30：去重候选不得再退回 DEF-7 的"跨课程同名节点"收窄口径 */
  @Test
  void kf30_dedupCandidateIsNotNarrowedByDuplicateTitleJoin() {
    String sql = insertSql(ExamDetailDedupListMapper.class, "generateDedupList");
    assertFalse(sql.contains("COUNT(DISTINCT course_id) > 1"),
        "候选范围不得再限定为跨课程同名节点（DEF-7）");
    assertTrue(sql.contains("e.create_time >= '2026-05-01'"), "候选仍限定 5 月以来");
    assertTrue(sql.contains("e.exam_id = 0"), "候选仍保留引用面防御过滤（exam_id=0）");
  }

  /** KF-27：分类范围收敛——装清单只装 5 月以来行（DEF-4 回归） */
  @Test
  void kf27_populateFixListFiltersMayRowsOnly() {
    String sql = insertSql(ExamExamDetailFixMapper.class, "populateFixList");
    assertTrue(sql.contains("e.create_time >= '2026-05-01'"),
        "populateFixList 必须加 create_time 过滤，不得改动 5 月前历史行");
  }

  /** KF-34：分类阶段范围加固（FIX-9/v1.7）——"装入时拦、运行时漏"的下游三处 SQL 均须带 5 月过滤 */
  @Test
  void kf34_classifyScopeGuardsMayRowsOnly() {
    for (String method : java.util.List.of(
        "selectPendingWithContent", "resetFlagRows", "countByStatus")) {
      String sql = insertSql(ExamDetailKnowledgeFixMapper.class, method);
      assertTrue(sql.contains("e.create_time >= '2026-05-01'"),
          method + " 必须带 5 月以来过滤（历史行绝不送模型、绝不被复位拉回）");
    }
  }

  /** KF-35：备份过滤条件必须参数化（FIX-8/v1.7）——去重=approved、写回=apply，杜绝空备份表 */
  @Test
  void kf35_backupRowsStatusFilterIsParameterized() {
    String sql = insertSql(ExamExamDetailFixMapper.class, "backupRows");
    assertTrue(sql.contains("#{statusFilter}"),
        "备份过滤条件必须参数化：去重侧传 approved、写回侧传 apply"
            + "（修复知识修正清单不存在 approved 导致的空备份表/回滚失效）");
  }
}

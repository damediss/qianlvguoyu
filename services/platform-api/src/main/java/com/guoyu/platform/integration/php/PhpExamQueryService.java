package com.guoyu.platform.integration.php;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guoyu.platform.integration.php.dto.PhpCourseWeights;
import com.guoyu.platform.integration.php.dto.PhpExamPlanInfo;
import com.guoyu.platform.integration.php.dto.PhpKnowledgeNode;
import com.guoyu.platform.integration.php.dto.PhpQuestion;
import com.guoyu.platform.integration.php.dto.PhpQuestionDetail;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * PHP 库（gjjddb）考试业务只读查询层 —— 《AI同步学（学员复习）实现方案》§3。
 *
 * <p>取题链路复刻 PHP {@code appreview/sortlist}（§3.3）：
 * train_exam_list.source → train_exam_work_type_list → exam_course →
 * 三级知识点（level=3,type=0）→ exam_exam_detail（source 一致 + knowledge_id IN + final_check_status=1 + deleted=0）。
 *
 * <p>约束：全部 SQL 显式列名、禁 SELECT *；返回值均做 null 检查；
 * 题目详情走 Caffeine 读穿缓存（TTL 6h），缓存 miss 且 PHP 不可达 → 抛业务异常（不静默吞）。
 */
@Slf4j
@Service
public class PhpExamQueryService {

  private final JdbcTemplate phpJdbcTemplate;
  private final Cache<Long, PhpQuestionDetail> detailCache;

  public PhpExamQueryService(@Qualifier("phpJdbcTemplate") JdbcTemplate phpJdbcTemplate) {
    this.phpJdbcTemplate = phpJdbcTemplate;
    this.detailCache = Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterWrite(Duration.ofHours(6))
        .build();
  }

  // ------------------------------------------------------------------
  // §3.3 题目全集解析
  // ------------------------------------------------------------------

  /** 考试计划与工种绑定（步骤 1-3）。计划不存在返回 null（调用方转业务异常） */
  public PhpExamPlanInfo loadExamPlanInfo(long examId) {
    List<Map<String, Object>> plans = phpJdbcTemplate.queryForList(
        "SELECT id, title, source, exam_date, exam_date_list FROM train_exam_list WHERE deleted = 0 AND id = ?",
        examId);
    if (plans.isEmpty()) {
      return null;
    }
    Map<String, Object> plan = plans.get(0);
    List<Map<String, Object>> bindings = phpJdbcTemplate.queryForList(
        "SELECT work_type_id, level, course_id FROM train_exam_work_type_list WHERE deleted = 0 AND exam_id = ?",
        examId);
    List<PhpExamPlanInfo.WorkTypeBinding> bindingList = new ArrayList<>();
    for (Map<String, Object> binding : bindings) {
      Long workTypeId = asLong(binding.get("work_type_id"));
      if (workTypeId == null) {
        continue;
      }
      Long courseId = asLong(binding.get("course_id"));
      Integer level = asInt(binding.get("level"));
      if (courseId == null || courseId == 0) {
        courseId = findCourseId(workTypeId, level);
      }
      bindingList.add(new PhpExamPlanInfo.WorkTypeBinding(workTypeId, level, courseId));
    }
    return new PhpExamPlanInfo(
        asLong(plan.get("id")),
        (String) plan.get("title"),
        asInt(plan.get("source")),
        (String) plan.get("exam_date"),
        (String) plan.get("exam_date_list"),
        bindingList);
  }

  /** 按（工种, 等级）补查科目（步骤 3）：exam_course.level 0=初级工 … 4=高级技师 */
  private Long findCourseId(long workTypeId, Integer level) {
    if (level == null) {
      return null;
    }
    List<Long> ids = phpJdbcTemplate.queryForList(
        "SELECT id FROM exam_course WHERE deleted = 0 AND work_type_id = ? AND level = ? ORDER BY id LIMIT 1",
        Long.class, workTypeId, level);
    return ids.isEmpty() ? null : ids.get(0);
  }

  /** 三级知识点 id（步骤 4）：level=3 AND type=0 */
  public List<Long> loadL3KnowledgeIds(List<Long> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(", ", courseIds.stream().map(id -> "?").toList());
    return phpJdbcTemplate.queryForList(
        "SELECT id FROM exam_knowledge_structure WHERE deleted = 0 AND level = 3 AND type = 0"
            + " AND course_id IN (" + placeholders + ")",
        Long.class, courseIds.toArray());
  }

  /** 题目全集（步骤 5）：source 一致 + knowledge_id IN + final_check_status=1 + deleted=0 */
  public List<PhpQuestion> loadQuestionScope(int source, List<Long> l3KnowledgeIds) {
    if (l3KnowledgeIds == null || l3KnowledgeIds.isEmpty()) {
      return List.of();
    }
    List<PhpQuestion> result = new ArrayList<>();
    for (List<Long> chunk : chunked(l3KnowledgeIds, 500)) {
      String placeholders = String.join(", ", chunk.stream().map(id -> "?").toList());
      List<Object> args = new ArrayList<>();
      args.add(source);
      args.addAll(chunk);
      List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
          "SELECT id, knowledge_id, type, difficulty, score FROM exam_exam_detail"
              + " WHERE deleted = 0 AND final_check_status = 1 AND source = ?"
              + " AND knowledge_id IN (" + placeholders + ")",
          args.toArray());
      for (Map<String, Object> row : rows) {
        Long id = asLong(row.get("id"));
        if (id == null) {
          continue;
        }
        result.add(new PhpQuestion(
            id,
            asLong(row.get("knowledge_id")),
            (String) row.get("type"),
            asInt(row.get("difficulty")),
            asBigDecimal(row.get("score"))));
      }
    }
    return result;
  }

  /** 按 id 批量查题目轻量元数据（排题/组卷用，不查 content/answer 大字段） */
  public Map<Long, PhpQuestion> loadQuestionMetaByIds(List<Long> questionIds) {
    Map<Long, PhpQuestion> result = new HashMap<>();
    if (questionIds == null || questionIds.isEmpty()) {
      return result;
    }
    for (List<Long> chunk : chunked(questionIds, 500)) {
      String placeholders = String.join(", ", chunk.stream().map(id -> "?").toList());
      List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
          "SELECT id, knowledge_id, type, difficulty, score FROM exam_exam_detail"
              + " WHERE deleted = 0 AND id IN (" + placeholders + ")",
          chunk.toArray());
      for (Map<String, Object> row : rows) {
        Long id = asLong(row.get("id"));
        if (id == null) {
          continue;
        }
        result.put(id, new PhpQuestion(
            id,
            asLong(row.get("knowledge_id")),
            (String) row.get("type"),
            asInt(row.get("difficulty")),
            asBigDecimal(row.get("score"))));
      }
    }
    return result;
  }

  // ------------------------------------------------------------------
  // §3.4 题目详情（读穿缓存，下发前剥离答案标记由调用方处理）
  // ------------------------------------------------------------------

  public PhpQuestionDetail loadQuestionDetail(long questionId) {
    PhpQuestionDetail cached = detailCache.getIfPresent(questionId);
    if (cached != null) {
      return cached;
    }
    List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
        "SELECT id, knowledge_id, type, difficulty, score, content, answer, resolve"
            + " FROM exam_exam_detail WHERE deleted = 0 AND id = ?",
        questionId);
    if (rows.isEmpty()) {
      return null;
    }
    PhpQuestionDetail detail = toDetail(rows.get(0));
    detailCache.put(questionId, detail);
    return detail;
  }

  public Map<Long, PhpQuestionDetail> loadQuestionDetails(List<Long> questionIds) {
    Map<Long, PhpQuestionDetail> result = new HashMap<>();
    List<Long> misses = new ArrayList<>();
    for (Long id : questionIds) {
      if (id == null) {
        continue;
      }
      PhpQuestionDetail cached = detailCache.getIfPresent(id);
      if (cached != null) {
        result.put(id, cached);
      } else {
        misses.add(id);
      }
    }
    for (List<Long> chunk : chunked(misses, 200)) {
      String placeholders = String.join(", ", chunk.stream().map(id -> "?").toList());
      List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
          "SELECT id, knowledge_id, type, difficulty, score, content, answer, resolve"
              + " FROM exam_exam_detail WHERE deleted = 0 AND id IN (" + placeholders + ")",
          chunk.toArray());
      for (Map<String, Object> row : rows) {
        PhpQuestionDetail detail = toDetail(row);
        detailCache.put(detail.id(), detail);
        result.put(detail.id(), detail);
      }
    }
    return result;
  }

  private PhpQuestionDetail toDetail(Map<String, Object> row) {
    return new PhpQuestionDetail(
        asLong(row.get("id")),
        asLong(row.get("knowledge_id")),
        (String) row.get("type"),
        asInt(row.get("difficulty")),
        asBigDecimal(row.get("score")),
        (String) row.get("content"),
        (String) row.get("answer"),
        (String) row.get("resolve"));
  }

  // ------------------------------------------------------------------
  // 学员名单与知识结构
  // ------------------------------------------------------------------

  /** 学员是否在计划名单中（train_exam_people_list） */
  public boolean isUserInExam(long examId, long platformUserId) {
    Integer count = phpJdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM train_exam_people_list WHERE deleted = 0 AND exam_id = ? AND platform_user_id = ?",
        Integer.class, examId, platformUserId);
    return count != null && count > 0;
  }

  /** 课程知识结构全树（权重 L1→L3 细分与重要度用） */
  public List<PhpKnowledgeNode> loadKnowledgeTree(long courseId) {
    List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
        "SELECT id, parent_id, title, level, important FROM exam_knowledge_structure"
            + " WHERE deleted = 0 AND course_id = ?",
        courseId);
    List<PhpKnowledgeNode> nodes = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Long id = asLong(row.get("id"));
      if (id == null) {
        continue;
      }
      nodes.add(new PhpKnowledgeNode(
          id, asLong(row.get("parent_id")), (String) row.get("title"),
          asInt(row.get("level")), (String) row.get("important")));
    }
    return nodes;
  }

  // ------------------------------------------------------------------
  // §5.7 权重配置：type=3 国标 → type=0 人工 → null（调用方兜底）
  // ------------------------------------------------------------------

  public PhpCourseWeights loadCourseWeights(long courseId) {
    PhpCourseWeights standard = loadWeightsByType(courseId, 3, "standard");
    if (standard != null) {
      return standard;
    }
    PhpCourseWeights custom = loadWeightsByType(courseId, 0, "custom");
    if (custom != null) {
      return custom;
    }
    return new PhpCourseWeights(null, List.of());
  }

  private PhpCourseWeights loadWeightsByType(long courseId, int type, String sourceType) {
    List<Long> rateIds = phpJdbcTemplate.queryForList(
        "SELECT id FROM exam_knowledge_rate WHERE deleted = 0 AND status = 1 AND course_id = ? AND type = ?"
            + " ORDER BY id DESC LIMIT 1",
        Long.class, courseId, type);
    if (rateIds.isEmpty()) {
      return null;
    }
    List<Map<String, Object>> details = phpJdbcTemplate.queryForList(
        "SELECT d.knowledge_id, d.rate, k.title FROM exam_knowledge_rate_detail d"
            + " LEFT JOIN exam_knowledge_structure k ON k.id = d.knowledge_id"
            + " WHERE d.deleted = 0 AND d.knowledge_rate_id = ? ORDER BY d.sort_number",
        rateIds.get(0));
    List<PhpCourseWeights.WeightItem> items = new ArrayList<>();
    for (Map<String, Object> detail : details) {
      Long knowledgeId = asLong(detail.get("knowledge_id"));
      Integer rate = asInt(detail.get("rate"));
      if (knowledgeId == null || rate == null || rate <= 0) {
        continue;
      }
      items.add(new PhpCourseWeights.WeightItem(knowledgeId, (String) detail.get("title"), rate));
    }
    if (items.isEmpty()) {
      return null;
    }
    if (rateIds.size() > 1) {
      log.warn("[AI复习] course={} type={} 权重配置存在多条，取 id 最大者 {}", courseId, type, rateIds.get(0));
    }
    return new PhpCourseWeights(sourceType, items);
  }

  // ------------------------------------------------------------------
  // §4.7 回写 exam_result（仅 INSERT，exam_type=5 AI 同步练习）
  // ------------------------------------------------------------------

  public void insertExamResult(String questionType, long examPlanId, long questionId, long platformUserId,
      String userAnswer, BigDecimal getScore) {
    phpJdbcTemplate.update(
        "INSERT INTO exam_result (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id,"
            + " platform_user_id, user_answer, tenant_id, create_time, deleted)"
            + " VALUES (?, ?, 0, 0, ?, 5, ?, ?, ?, 10159, NOW(), 0)",
        questionType, examPlanId, getScore, questionId, platformUserId, userAnswer);
  }

  // ------------------------------------------------------------------
  // 工具
  // ------------------------------------------------------------------

  private static List<List<Long>> chunked(List<Long> ids, int size) {
    List<List<Long>> chunks = new ArrayList<>();
    for (int i = 0; i < ids.size(); i += size) {
      chunks.add(ids.subList(i, Math.min(i + size, ids.size())));
    }
    return chunks;
  }

  private static Long asLong(Object value) {
    if (value instanceof Boolean bool) {
      return bool ? 1L : 0L; // 防御：tinyInt1isBit=true 时 TINYINT(1) 被映射为 Boolean（仅 0/1 语义列可用）
    }
    return value instanceof Number number ? number.longValue() : null;
  }

  private static Integer asInt(Object value) {
    if (value instanceof Boolean bool) {
      return bool ? 1 : 0; // 同上；level/difficulty 等真实数值列依赖 URL tinyInt1isBit=false 返回 Integer
    }
    return value instanceof Number number ? number.intValue() : null;
  }

  private static BigDecimal asBigDecimal(Object value) {
    return value instanceof BigDecimal bd ? bd : null;
  }
}

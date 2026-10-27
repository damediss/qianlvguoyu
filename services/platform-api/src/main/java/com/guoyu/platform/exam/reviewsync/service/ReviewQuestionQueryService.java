package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights;
import com.guoyu.platform.exam.reviewsync.dto.ReviewExamPlanInfo;
import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.exam.service.OfficialResolveResolver;
import com.guoyu.platform.exam.service.OfficialResolveResolver.OfficialResolve;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 同步学题库/计划/知识树主数据源查询层（单库化改造后替代 PhpExamQueryService）。
 *
 * <p>取题链路复刻 PHP {@code appreview/sortlist}（复习方案 §3.3）：
 * train_exam_list.source → train_exam_work_type_list → exam_course →
 * 三级知识点（level=3,type=0）→ exam_exam_detail（source 一致 + knowledge_id IN + final_check_status=1）。
 *
 * <p>单库口径：全部走 MyBatis-Plus 主数据源 Mapper，租户拦截器自动拼 tenant_id（App 链路经
 * TenantContextFilter 强制 10159），逻辑删除由 @TableLogic 自动拼 deleted=0；复用的合并表实体
 * 均不继承 BaseEntity、主键 IdType.AUTO（合并表只有 create_time/update_time）。
 * 题目详情走 Caffeine 读穿缓存（TTL 6h），缓存 miss 回源主库；返回值均做 null 检查。
 * 解析走统一官方读取器（§4.1）；缓存命中但 resolve 为空时回源当前行刷新，
 * 新解析非空才更新缓存，避免"生成了却读不到"被空解析缓存 6 小时。
 */
@Slf4j
@Service
public class ReviewQuestionQueryService {

  private final TrainExamListMapper trainExamListMapper;
  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final TrainExamPeopleListMapper peopleListMapper;
  private final ExamCourseMapper examCourseMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamKnowledgeRateMapper knowledgeRateMapper;
  private final ExamKnowledgeRateDetailMapper knowledgeRateDetailMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final OfficialResolveResolver officialResolveResolver;
  private final Cache<Long, ReviewQuestionDetail> detailCache;

  public ReviewQuestionQueryService(
      TrainExamListMapper trainExamListMapper,
      TrainExamWorkTypeListMapper workTypeListMapper,
      TrainExamPeopleListMapper peopleListMapper,
      ExamCourseMapper examCourseMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamKnowledgeRateMapper knowledgeRateMapper,
      ExamKnowledgeRateDetailMapper knowledgeRateDetailMapper,
      ExamExamDetailMapper examDetailMapper,
      OfficialResolveResolver officialResolveResolver) {
    this.trainExamListMapper = trainExamListMapper;
    this.workTypeListMapper = workTypeListMapper;
    this.peopleListMapper = peopleListMapper;
    this.examCourseMapper = examCourseMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.knowledgeRateMapper = knowledgeRateMapper;
    this.knowledgeRateDetailMapper = knowledgeRateDetailMapper;
    this.examDetailMapper = examDetailMapper;
    this.officialResolveResolver = officialResolveResolver;
    this.detailCache = Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterWrite(Duration.ofHours(6))
        .build();
  }

  // ------------------------------------------------------------------
  // §3.3 题目全集解析
  // ------------------------------------------------------------------

  /** 考试计划与工种绑定（步骤 1-3）。计划不存在返回 null（调用方转业务异常） */
  public ReviewExamPlanInfo loadExamPlanInfo(long examId) {
    TrainExamListEntity plan = trainExamListMapper.selectById(examId);
    if (plan == null) {
      return null;
    }
    List<TrainExamWorkTypeListEntity> bindings = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .eq(TrainExamWorkTypeListEntity::getExamId, examId));
    List<ReviewExamPlanInfo.WorkTypeBinding> bindingList = new ArrayList<>();
    for (TrainExamWorkTypeListEntity binding : bindings) {
      if (binding == null || binding.getWorkTypeId() == null) {
        continue;
      }
      Long courseId = binding.getCourseId();
      Integer level = parseLevel(binding.getLevel());
      if (courseId == null || courseId == 0) {
        courseId = findCourseId(binding.getWorkTypeId(), level);
      }
      bindingList.add(new ReviewExamPlanInfo.WorkTypeBinding(binding.getWorkTypeId(), level, courseId));
    }
    return new ReviewExamPlanInfo(
        plan.getId(),
        plan.getTitle(),
        plan.getSource(),
        plan.getExamDate(),
        plan.getExamDateList(),
        bindingList);
  }

  /**
   * 考试计划头部字段（漂移检测专用，轻量：只查计划行不查工种绑定）。
   * 计划不存在返回 null（调用方保留原计划照常学习）。
   */
  public ReviewExamPlanInfo.ExamPlanHead loadExamPlanHead(long examId) {
    TrainExamListEntity plan = trainExamListMapper.selectById(examId);
    if (plan == null) {
      return null;
    }
    return new ReviewExamPlanInfo.ExamPlanHead(plan.getExamDate(), plan.getExamDateList(),
        plan.getSource());
  }

  /** 按（工种, 等级）补查科目（步骤 3）：exam_course.level 0=初级工 … 4=高级技师 */
  private Long findCourseId(long workTypeId, Integer level) {    if (level == null) {
      return null;
    }
    List<ExamCourseEntity> courses = examCourseMapper.selectList(new LambdaQueryWrapper<ExamCourseEntity>()
        .select(ExamCourseEntity::getId)
        .eq(ExamCourseEntity::getWorkTypeId, workTypeId)
        .eq(ExamCourseEntity::getLevel, level)
        .orderByAsc(ExamCourseEntity::getId)
        .last("LIMIT 1"));
    return courses.isEmpty() || courses.get(0) == null ? null : courses.get(0).getId().longValue();
  }

  /** 三级知识点 id（步骤 4）：level=3 AND type=0 */
  public List<Long> loadL3KnowledgeIds(List<Long> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return List.of();
    }
    List<ExamKnowledgeStructureEntity> nodes = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .select(ExamKnowledgeStructureEntity::getId)
            .eq(ExamKnowledgeStructureEntity::getLevel, 3)
            .eq(ExamKnowledgeStructureEntity::getType, 0)
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds));
    List<Long> ids = new ArrayList<>(nodes.size());
    for (ExamKnowledgeStructureEntity node : nodes) {
      if (node != null && node.getId() != null) {
        ids.add(node.getId().longValue());
      }
    }
    return ids;
  }

  /** 入池题型白名单（《简答题作答与AI判分方案》§3.4 选题硬过滤）：口径收敛在
   * {@link QuestionTypes#SCOPE_TYPES}（客观三型 + 主观题 8 白名单，唯一来源）。
   * 空 type（存量 16,918 题）与未知题型不入池——这些题被选中即卡死，排除是修 bug */
  private static final List<String> QUESTION_SCOPE_TYPES = QuestionTypes.SCOPE_TYPES;

  /** 题目全集（步骤 5）：source 一致 + knowledge_id IN + final_check_status=1 + 题型白名单 */
  public List<ReviewQuestion> loadQuestionScope(int source, List<Long> l3KnowledgeIds) {
    if (l3KnowledgeIds == null || l3KnowledgeIds.isEmpty()) {
      return List.of();
    }
    List<ReviewQuestion> result = new ArrayList<>();
    for (List<Long> chunk : chunked(l3KnowledgeIds, 500)) {
      for (ExamExamDetailEntity row : examDetailMapper.selectList(questionMetaWrapper()
          .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
          .eq(ExamExamDetailEntity::getSource, source)
          .in(ExamExamDetailEntity::getKnowledgeId, chunk)
          .in(ExamExamDetailEntity::getType, QUESTION_SCOPE_TYPES))) {
        ReviewQuestion question = toQuestion(row);
        if (question != null) {
          result.add(question);
        }
      }
    }
    return result;
  }

  /** 按 id 批量查题目轻量元数据（排题/组卷用，不查 content/answer 大字段） */
  public Map<Long, ReviewQuestion> loadQuestionMetaByIds(List<Long> questionIds) {
    Map<Long, ReviewQuestion> result = new HashMap<>();
    if (questionIds == null || questionIds.isEmpty()) {
      return result;
    }
    for (List<Long> chunk : chunked(questionIds, 500)) {
      for (ExamExamDetailEntity row : examDetailMapper.selectList(questionMetaWrapper()
          .in(ExamExamDetailEntity::getId, chunk))) {
        ReviewQuestion question = toQuestion(row);
        if (question != null) {
          result.put(question.id(), question);
        }
      }
    }
    return result;
  }

  // ------------------------------------------------------------------
  // §3.4 题目详情（读穿缓存，下发前剥离答案标记由调用方处理）
  // ------------------------------------------------------------------

  public ReviewQuestionDetail loadQuestionDetail(long questionId) {
    ReviewQuestionDetail cached = detailCache.getIfPresent(questionId);
    if (cached != null) {
      if (hasText(cached.resolve())) {
        return cached;
      }
      // 命中缓存但 resolve 为空（解析后生成/回写）：回源当前行刷新，非空才更新缓存
      ExamExamDetailEntity refreshed = examDetailMapper.selectOne(new LambdaQueryWrapper<ExamExamDetailEntity>()
          .eq(ExamExamDetailEntity::getId, questionId)
          .last("LIMIT 1"));
      if (refreshed == null) {
        return cached;
      }
      ReviewQuestionDetail detail = toDetail(refreshed);
      if (detail != null && hasText(detail.resolve())) {
        detailCache.put(questionId, detail);
        return detail;
      }
      return detail != null ? detail : cached;
    }
    ExamExamDetailEntity entity = examDetailMapper.selectOne(new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getId, questionId)
        .last("LIMIT 1"));
    if (entity == null) {
      return null;
    }
    ReviewQuestionDetail detail = toDetail(entity);
    if (detail != null) {
      detailCache.put(questionId, detail);
    }
    return detail;
  }

  /** 供生成回写后主动失效缓存（§5.11：AI 回写 resolve 立即对同步学可见）。 */
  public void evictQuestionDetail(long questionId) {
    detailCache.invalidate(questionId);
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }

  public Map<Long, ReviewQuestionDetail> loadQuestionDetails(List<Long> questionIds) {
    Map<Long, ReviewQuestionDetail> result = new HashMap<>();
    List<Long> misses = new ArrayList<>();
    List<Long> emptyResolveHits = new ArrayList<>();
    for (Long id : questionIds) {
      if (id == null) {
        continue;
      }
      ReviewQuestionDetail cached = detailCache.getIfPresent(id);
      if (cached != null && hasText(cached.resolve())) {
        // 非空缓存继续直接使用
        result.put(id, cached);
      } else if (cached != null) {
        // 缓存了空解析：先放旧值兜底，随后回源刷新
        result.put(id, cached);
        emptyResolveHits.add(id);
      } else {
        misses.add(id);
      }
    }
    for (List<Long> chunk : chunked(misses, 200)) {
      for (ExamExamDetailEntity row : examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>()
              .in(ExamExamDetailEntity::getId, chunk))) {
        ReviewQuestionDetail detail = toDetail(row);
        if (detail != null) {
          detailCache.put(detail.id(), detail);
          result.put(detail.id(), detail);
        }
      }
    }
    // 缓存命中但 resolve 为空的 id：回源当前行，新解析非空则更新缓存与返回值
    for (List<Long> chunk : chunked(emptyResolveHits, 200)) {
      for (ExamExamDetailEntity row : examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>()
              .in(ExamExamDetailEntity::getId, chunk))) {
        ReviewQuestionDetail detail = toDetail(row);
        if (detail != null && hasText(detail.resolve())) {
          detailCache.put(detail.id(), detail);
          result.put(detail.id(), detail);
        }
      }
    }
    return result;
  }

  // ------------------------------------------------------------------
  // 学员名单与知识结构
  // ------------------------------------------------------------------

  /** 学员是否在计划名单中（train_exam_people_list） */
  public boolean isUserInExam(long examId, long platformUserId) {
    Long count = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getExamId, examId)
        .eq(TrainExamPeopleListEntity::getPlatformUserId, platformUserId));
    return count != null && count > 0;
  }

  /** 课程知识结构全树（权重 L1→L3 细分与重要度用） */
  public List<ReviewKnowledgeNode> loadKnowledgeTree(long courseId) {
    List<ExamKnowledgeStructureEntity> rows = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .select(ExamKnowledgeStructureEntity::getId, ExamKnowledgeStructureEntity::getParentId,
                ExamKnowledgeStructureEntity::getTitle, ExamKnowledgeStructureEntity::getLevel,
                ExamKnowledgeStructureEntity::getImportant)
            .eq(ExamKnowledgeStructureEntity::getCourseId, courseId));
    List<ReviewKnowledgeNode> nodes = new ArrayList<>();
    for (ExamKnowledgeStructureEntity row : rows) {
      if (row == null || row.getId() == null) {
        continue;
      }
      nodes.add(new ReviewKnowledgeNode(
          row.getId().longValue(),
          row.getParentId() != null ? row.getParentId().longValue() : null,
          row.getTitle(),
          row.getLevel(),
          row.getImportant()));
    }
    return nodes;
  }

  // ------------------------------------------------------------------
  // §5.7 权重配置：type=3 国标 → type=0 人工 → 空（调用方兜底）
  // ------------------------------------------------------------------

  public ReviewCourseWeights loadCourseWeights(long courseId) {
    ReviewCourseWeights standard = loadWeightsByType(courseId, 3, "standard");
    if (standard != null) {
      return standard;
    }
    ReviewCourseWeights custom = loadWeightsByType(courseId, 0, "custom");
    if (custom != null) {
      return custom;
    }
    return new ReviewCourseWeights(null, List.of());
  }

  private ReviewCourseWeights loadWeightsByType(long courseId, int type, String sourceType) {
    LambdaQueryWrapper<ExamKnowledgeRateEntity> rateWrapper = new LambdaQueryWrapper<ExamKnowledgeRateEntity>()
        .eq(ExamKnowledgeRateEntity::getCourseId, courseId)
        .eq(ExamKnowledgeRateEntity::getType, type)
        .eq(ExamKnowledgeRateEntity::getStatus, 1);
    Long rateCount = knowledgeRateMapper.selectCount(rateWrapper);
    List<ExamKnowledgeRateEntity> rates = knowledgeRateMapper.selectList(rateWrapper
        .select(ExamKnowledgeRateEntity::getId)
        .orderByDesc(ExamKnowledgeRateEntity::getId)
        .last("LIMIT 1"));
    if (rates.isEmpty() || rates.get(0) == null) {
      return null;
    }
    if (rateCount != null && rateCount > 1) {
      log.warn("[AI复习] course={} type={} 权重配置存在 {} 条，取 id 最大者 {}", courseId, type, rateCount,
          rates.get(0).getId());
    }
    List<ExamKnowledgeRateDetailEntity> details = knowledgeRateDetailMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeRateDetailEntity>()
            .eq(ExamKnowledgeRateDetailEntity::getKnowledgeRateId, rates.get(0).getId())
            .orderByAsc(ExamKnowledgeRateDetailEntity::getSortNumber));
    // 标题单独批量查（不 JOIN）：已删除/缺失节点标题按 null 处理，与原 LEFT JOIN 语义一致
    List<Long> knowledgeIds = new ArrayList<>();
    for (ExamKnowledgeRateDetailEntity detail : details) {
      if (detail != null && detail.getKnowledgeId() != null) {
        knowledgeIds.add(detail.getKnowledgeId().longValue());
      }
    }
    Map<Long, String> titleById = new HashMap<>();
    if (!knowledgeIds.isEmpty()) {
      for (ExamKnowledgeStructureEntity node : knowledgeStructureMapper.selectList(
          new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
              .select(ExamKnowledgeStructureEntity::getId, ExamKnowledgeStructureEntity::getTitle)
              .in(ExamKnowledgeStructureEntity::getId, knowledgeIds))) {
        if (node != null && node.getId() != null) {
          titleById.put(node.getId().longValue(), node.getTitle());
        }
      }
    }
    List<ReviewCourseWeights.WeightItem> items = new ArrayList<>();
    for (ExamKnowledgeRateDetailEntity detail : details) {
      if (detail == null || detail.getKnowledgeId() == null
          || detail.getRate() == null || detail.getRate() <= 0) {
        continue;
      }
      long knowledgeId = detail.getKnowledgeId().longValue();
      items.add(new ReviewCourseWeights.WeightItem(knowledgeId, titleById.get(knowledgeId), detail.getRate()));
    }
    if (items.isEmpty()) {
      return null;
    }
    return new ReviewCourseWeights(sourceType, items);
  }

  // ------------------------------------------------------------------
  // 工具
  // ------------------------------------------------------------------

  private static LambdaQueryWrapper<ExamExamDetailEntity> questionMetaWrapper() {
    return new LambdaQueryWrapper<ExamExamDetailEntity>()
        .select(ExamExamDetailEntity::getId, ExamExamDetailEntity::getKnowledgeId,
            ExamExamDetailEntity::getType, ExamExamDetailEntity::getDifficulty,
            ExamExamDetailEntity::getScore);
  }

  private static ReviewQuestion toQuestion(ExamExamDetailEntity row) {
    if (row == null || row.getId() == null) {
      return null;
    }
    return new ReviewQuestion(
        row.getId().longValue(),
        row.getKnowledgeId() != null ? row.getKnowledgeId().longValue() : null,
        row.getType(),
        row.getDifficulty(),
        row.getScore());
  }

  private ReviewQuestionDetail toDetail(ExamExamDetailEntity row) {
    if (row == null || row.getId() == null) {
      return null;
    }
    // resolve 统一走官方读取器（resolve 列 → answer JSON 官方解析 → empty），§4.1
    OfficialResolve official = officialResolveResolver.resolve(row);
    return new ReviewQuestionDetail(
        row.getId().longValue(),
        row.getKnowledgeId() != null ? row.getKnowledgeId().longValue() : null,
        row.getType(),
        row.getDifficulty(),
        row.getScore(),
        row.getContent(),
        row.getAnswer(),
        official.text());
  }

  /** train_exam_work_type_list.level 实体为 String（合并库实为 tinyint）：数字串/数值表示均容错解析 */
  private static Integer parseLevel(String level) {
    if (level == null || level.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(level.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static List<List<Long>> chunked(List<Long> ids, int size) {
    List<List<Long>> chunks = new ArrayList<>();
    for (int i = 0; i < ids.size(); i += size) {
      chunks.add(ids.subList(i, Math.min(i + size, ids.size())));
    }
    return chunks;
  }
}

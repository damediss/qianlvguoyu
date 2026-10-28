package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamPracticeCourseScope;
import com.guoyu.platform.exam.dto.ExamPracticeQuestionResourceRow;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamPracticeResourceMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Course-question-bank adapter shared by the competition pre-practice configuration use case. */
@Service
public class ExamPracticeResourceApplicationService {

  private final ExamPracticeResourceMapper resourceMapper;
  private final ExamWorkTypeMapper workTypeMapper;

  public ExamPracticeResourceApplicationService(
      ExamPracticeResourceMapper resourceMapper,
      ExamWorkTypeMapper workTypeMapper) {
    this.resourceMapper = resourceMapper;
    this.workTypeMapper = workTypeMapper;
  }

  @Transactional(readOnly = true)
  public Map<String, Long> countAvailable(Long tenantId, List<ExamPracticeCourseScope> scopes) {
    Map<String, Long> counts = new LinkedHashMap<>();
    QuestionTypes.SCOPE_TYPES.forEach(type -> counts.put(type, 0L));
    if (scopes == null || scopes.isEmpty()) {
      return counts;
    }
    for (Map<String, Object> row : resourceMapper.countAvailableQuestions(
        tenantId, scopes, QuestionTypes.SCOPE_TYPES)) {
      if (row == null || row.get("questionType") == null || row.get("availableCount") == null) {
        continue;
      }
      String type = QuestionTypes.normalize(String.valueOf(row.get("questionType")));
      if (!counts.containsKey(type)) {
        continue;
      }
      Object rawCount = row.get("availableCount");
      long count = rawCount instanceof Number number ? number.longValue() : 0L;
      counts.merge(type, count, Long::sum);
    }
    return counts;
  }

  @Transactional(readOnly = true)
  public List<ExamPracticeQuestionResourceRow> sample(
      Long tenantId,
      List<ExamPracticeCourseScope> scopes,
      String questionType,
      int questionCount,
      long availableCount) {
    if (!QuestionTypes.SCOPE_TYPES.contains(questionType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的试题类型");
    }
    if (questionCount <= 0 || scopes == null || scopes.isEmpty()) {
      return List.of();
    }
    if (questionCount > availableCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选题型的可用试题数量已变化，请刷新后重试");
    }
    long maxOffset = availableCount - questionCount;
    long offset = maxOffset <= 0 ? 0 : ThreadLocalRandom.current().nextLong(maxOffset + 1);
    List<ExamPracticeQuestionResourceRow> rows = resourceMapper.selectQuestionWindow(
        tenantId, scopes, questionType, offset, questionCount);
    return rows == null ? List.of() : rows;
  }

  @Transactional(readOnly = true)
  public Map<Integer, String> loadWorkTypeNames(Long tenantId, List<Integer> workTypeIds) {
    if (workTypeIds == null || workTypeIds.isEmpty()) {
      return Map.of();
    }
    List<ExamWorkTypeEntity> rows = workTypeMapper.selectList(
        new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .select(ExamWorkTypeEntity::getId, ExamWorkTypeEntity::getTitle)
            .eq(ExamWorkTypeEntity::getTenantId, tenantId)
            .eq(ExamWorkTypeEntity::getStatus, 1)
            .in(ExamWorkTypeEntity::getId, workTypeIds));
    if (rows == null) {
      return Map.of();
    }
    return rows.stream().filter(row -> row != null && row.getId() != null)
        .collect(Collectors.toMap(
            ExamWorkTypeEntity::getId,
            row -> row.getTitle() == null ? "" : row.getTitle(),
            (left, right) -> left,
            LinkedHashMap::new));
  }

  public static List<ExamPracticeCourseScope> toCourseScopes(
      List<Integer> workTypeIds, List<Integer> nationalGrades) {
    if (workTypeIds == null || nationalGrades == null) {
      return List.of();
    }
    List<ExamPracticeCourseScope> scopes = new ArrayList<>();
    for (Integer workTypeId : workTypeIds) {
      if (workTypeId == null || workTypeId <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择有效的试题资源工种");
      }
      for (Integer grade : nationalGrades) {
        if (grade == null || grade < 1 || grade > 5) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "课程题库等级必须是1至5级");
        }
        scopes.add(new ExamPracticeCourseScope(workTypeId, 5 - grade));
      }
    }
    return scopes;
  }
}

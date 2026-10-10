package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.vo.CompetitionTheoryCourseSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSourceVO;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamMakeExamPlanEntity;
import com.guoyu.platform.exam.entity.ExamPaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamPaperEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamMakeExamPlanMapper;
import com.guoyu.platform.exam.mapper.ExamPaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamPaperMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class CompetitionTheoryPaperSourceAdapter {

  private final ExamCourseMapper courseMapper;
  private final ExamMakeExamPlanMapper planMapper;
  private final ExamPaperMapper paperMapper;
  private final ExamPaperDetailMapper paperDetailMapper;
  private final ExamExamDetailMapper examDetailMapper;

  public CompetitionTheoryPaperSourceAdapter(
      ExamCourseMapper courseMapper,
      ExamMakeExamPlanMapper planMapper,
      ExamPaperMapper paperMapper,
      ExamPaperDetailMapper paperDetailMapper,
      ExamExamDetailMapper examDetailMapper) {
    this.courseMapper = courseMapper;
    this.planMapper = planMapper;
    this.paperMapper = paperMapper;
    this.paperDetailMapper = paperDetailMapper;
    this.examDetailMapper = examDetailMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryCourseSourceVO> listCourses(Long tenantId, Long workTypeId) {
    int sourceWorkTypeId = toSourceId(workTypeId, "工种");
    List<ExamCourseEntity> courses = courseMapper.selectList(
        new LambdaQueryWrapper<ExamCourseEntity>()
            .eq(ExamCourseEntity::getTenantId, tenantId)
            .eq(ExamCourseEntity::getWorkTypeId, sourceWorkTypeId)
            .eq(ExamCourseEntity::getStatus, 1)
            .eq(ExamCourseEntity::getDeleted, 0)
            .orderByAsc(ExamCourseEntity::getSortNumber)
            .orderByAsc(ExamCourseEntity::getId));
    if (courses == null) {
      log.warn("Course mapper returned null for competition theory source, tenantId={}, workTypeId={}",
          tenantId, workTypeId);
      return Collections.emptyList();
    }
    List<CompetitionTheoryCourseSourceVO> result = new ArrayList<>();
    for (ExamCourseEntity course : courses) {
      result.add(new CompetitionTheoryCourseSourceVO(
          course.getId().longValue(),
          course.getWorkTypeId().longValue(),
          course.getTitle(),
          course.getLevel(),
          course.getVersion()));
    }
    return result;
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryPaperSourceVO> listCompletedPapers(
      Long tenantId, Long workTypeId, Long courseId) {
    ExamCourseEntity course = requireCourse(tenantId, workTypeId, courseId);
    int sourceTenantId = toSourceId(tenantId, "租户");
    List<ExamMakeExamPlanEntity> plans = planMapper.selectList(
        new LambdaQueryWrapper<ExamMakeExamPlanEntity>()
            .eq(ExamMakeExamPlanEntity::getTenantId, sourceTenantId)
            .eq(ExamMakeExamPlanEntity::getCourseId, course.getId())
            .eq(ExamMakeExamPlanEntity::getExamType, 0)
            .eq(ExamMakeExamPlanEntity::getStatus, 1)
            .orderByDesc(ExamMakeExamPlanEntity::getCreateTime)
            .orderByDesc(ExamMakeExamPlanEntity::getId));
    if (plans == null) {
      log.warn("Make-exam-plan mapper returned null, tenantId={}, courseId={}", tenantId, courseId);
      return Collections.emptyList();
    }
    List<CompetitionTheoryPaperSourceVO> result = new ArrayList<>();
    for (ExamMakeExamPlanEntity plan : plans) {
      List<ExamPaperEntity> papers = paperMapper.selectList(
          new LambdaQueryWrapper<ExamPaperEntity>()
              .eq(ExamPaperEntity::getTenantId, sourceTenantId)
              .eq(ExamPaperEntity::getMakeId, plan.getId())
              .eq(ExamPaperEntity::getStatus, 1)
              .orderByDesc(ExamPaperEntity::getCreateTime)
              .orderByDesc(ExamPaperEntity::getId));
      if (papers == null) {
        log.warn("Paper mapper returned null, tenantId={}, planId={}", tenantId, plan.getId());
        continue;
      }
      for (ExamPaperEntity paper : papers) {
        Long count = paperDetailMapper.selectCount(
            new LambdaQueryWrapper<ExamPaperDetailEntity>()
                .eq(ExamPaperDetailEntity::getTenantId, sourceTenantId)
                .eq(ExamPaperDetailEntity::getPaperId, paper.getId())
                .eq(ExamPaperDetailEntity::getStatus, 1));
        if (count == null || count <= 0) {
          // 课程试卷只有题目明细落库后，才算可被竞赛导入的已完成组卷。
          continue;
        }
        result.add(new CompetitionTheoryPaperSourceVO(
            paper.getId().longValue(),
            plan.getId().longValue(),
            course.getId().longValue(),
            plan.getTitle(),
            paper.getTitle(),
            paper.getTime(),
            decimal(paper.getTotalPoint()),
            decimal(paper.getPassPoint()),
            Math.toIntExact(count)));
      }
    }
    return result;
  }

  @Transactional(readOnly = true)
  public SourcePaper loadCompletedPaper(
      Long tenantId, Long workTypeId, Long courseId, Long paperId) {
    ExamCourseEntity course = requireCourse(tenantId, workTypeId, courseId);
    int sourceTenantId = toSourceId(tenantId, "租户");
    int sourcePaperId = toSourceId(paperId, "课程试卷");
    ExamPaperEntity paper = paperMapper.selectOne(
        new LambdaQueryWrapper<ExamPaperEntity>()
            .eq(ExamPaperEntity::getTenantId, sourceTenantId)
            .eq(ExamPaperEntity::getId, sourcePaperId)
            .eq(ExamPaperEntity::getStatus, 1));
    if (paper == null || paper.getMakeId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "课程试卷不存在或尚未组卷完成");
    }
    ExamMakeExamPlanEntity plan = planMapper.selectOne(
        new LambdaQueryWrapper<ExamMakeExamPlanEntity>()
            .eq(ExamMakeExamPlanEntity::getTenantId, sourceTenantId)
            .eq(ExamMakeExamPlanEntity::getId, paper.getMakeId())
            .eq(ExamMakeExamPlanEntity::getCourseId, course.getId())
            .eq(ExamMakeExamPlanEntity::getExamType, 0)
            .eq(ExamMakeExamPlanEntity::getStatus, 1));
    if (plan == null) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "课程试卷不属于当前工种课程或不是已完成理论组卷");
    }
    List<ExamPaperDetailEntity> details = paperDetailMapper.selectList(
        new LambdaQueryWrapper<ExamPaperDetailEntity>()
            .eq(ExamPaperDetailEntity::getTenantId, sourceTenantId)
            .eq(ExamPaperDetailEntity::getPaperId, sourcePaperId)
            .eq(ExamPaperDetailEntity::getStatus, 1)
            .orderByAsc(ExamPaperDetailEntity::getSortNumber)
            .orderByAsc(ExamPaperDetailEntity::getId));
    if (details == null || details.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "课程试卷没有可用题目");
    }
    List<SourceQuestion> questions = new ArrayList<>();
    for (ExamPaperDetailEntity detail : details) {
      ExamExamDetailEntity source = detail.getExamId() == null ? null
          : examDetailMapper.selectById(detail.getExamId());
      questions.add(new SourceQuestion(
          detail.getId().longValue(),
          detail.getExamId() == null ? null : detail.getExamId().longValue(),
          detail.getType(),
          detail.getContent(),
          detail.getAnswer(),
          detail.getScore(),
          detail.getResolve(),
          detail.getDiff(),
          source == null ? null : source.getOrgId(),
          source == null ? null : source.getOrgName(),
          source == null || source.getUserId() == null ? null : source.getUserId().longValue(),
          source == null ? null : source.getUpdateTime()));
    }
    return new SourcePaper(
        paper.getId().longValue(),
        plan.getId().longValue(),
        course.getId().longValue(),
        paper.getTitle(),
        paper.getTime(),
        decimal(paper.getTotalPoint()),
        decimal(paper.getPassPoint()),
        questions);
  }

  private ExamCourseEntity requireCourse(Long tenantId, Long workTypeId, Long courseId) {
    int sourceWorkTypeId = toSourceId(workTypeId, "工种");
    int sourceCourseId = toSourceId(courseId, "课程");
    ExamCourseEntity course = courseMapper.selectOne(
        new LambdaQueryWrapper<ExamCourseEntity>()
            .eq(ExamCourseEntity::getTenantId, tenantId)
            .eq(ExamCourseEntity::getId, sourceCourseId)
            .eq(ExamCourseEntity::getWorkTypeId, sourceWorkTypeId)
            .eq(ExamCourseEntity::getStatus, 1)
            .eq(ExamCourseEntity::getDeleted, 0));
    if (course == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "课程不存在或不属于当前赛事工种");
    }
    return course;
  }

  private int toSourceId(Long value, String name) {
    try {
      return Math.toIntExact(Objects.requireNonNull(value));
    } catch (ArithmeticException | NullPointerException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, name + "ID不合法");
    }
  }

  private BigDecimal decimal(Integer value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }

  public record SourcePaper(
      Long paperId,
      Long planId,
      Long courseId,
      String title,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      List<SourceQuestion> questions
  ) {
  }

  public record SourceQuestion(
      Long paperDetailId,
      Long sourceQuestionId,
      String type,
      String content,
      String answerJson,
      BigDecimal score,
      String resolveContent,
      Integer difficulty,
      Long sourceOrgId,
      String sourceOrgName,
      Long sourceAuthorId,
      LocalDateTime sourceUpdatedAt
  ) {
  }
}

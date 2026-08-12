package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanCreateReq;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanDetailReq;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanQueryReq;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanUpdateReq;
import com.guoyu.platform.exam.entity.*;
import com.guoyu.platform.exam.mapper.*;
import com.guoyu.platform.exam.vo.ExamMakeExamPlanVO;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamMakeExamPlanApplicationService {

  private static final String[] LEVEL_NAMES = {
      "初级工/五级", "中级工/四级", "高级工/三级", "技师/二级",
      "高级技师/一级", "特级技师", "首席技师"
  };

  private final ExamMakeExamPlanMapper planMapper;
  private final ExamMakeExamPlanDetailMapper planDetailMapper;
  private final ExamKnowledgeRateDetailMapper knowledgeRateDetailMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final ExamPaperMapper paperMapper;
  private final ExamPaperDetailMapper paperDetailMapper;
  private final ExamOperatePaperMapper operatePaperMapper;
  private final ExamOperatePaperDetailMapper operatePaperDetailMapper;
  private final ExamOperateDetailMapper operateDetailMapper;
  private final ExamCourseMapper courseMapper;
  private final ExamKnowledgeStructureApplicationService knowledgeService;

  public ExamMakeExamPlanApplicationService(
      ExamMakeExamPlanMapper planMapper,
      ExamMakeExamPlanDetailMapper planDetailMapper,
      ExamKnowledgeRateDetailMapper knowledgeRateDetailMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamExamDetailMapper examDetailMapper,
      ExamPaperMapper paperMapper,
      ExamPaperDetailMapper paperDetailMapper,
      ExamOperatePaperMapper operatePaperMapper,
      ExamOperatePaperDetailMapper operatePaperDetailMapper,
      ExamOperateDetailMapper operateDetailMapper,
      ExamCourseMapper courseMapper,
      ExamKnowledgeStructureApplicationService knowledgeService) {
    this.planMapper = planMapper;
    this.planDetailMapper = planDetailMapper;
    this.knowledgeRateDetailMapper = knowledgeRateDetailMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.examDetailMapper = examDetailMapper;
    this.paperMapper = paperMapper;
    this.paperDetailMapper = paperDetailMapper;
    this.operatePaperMapper = operatePaperMapper;
    this.operatePaperDetailMapper = operatePaperDetailMapper;
    this.operateDetailMapper = operateDetailMapper;
    this.courseMapper = courseMapper;
    this.knowledgeService = knowledgeService;
  }

  // ==================== CRUD ====================

  @Transactional(readOnly = true)
  public PageResponse<ExamMakeExamPlanVO> page(ExamMakeExamPlanQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamMakeExamPlanEntity> wrapper = new LambdaQueryWrapper<ExamMakeExamPlanEntity>()
        .eq(ExamMakeExamPlanEntity::getTenantId, tenantId)
        .eq(req.getCourseId() != null, ExamMakeExamPlanEntity::getCourseId, req.getCourseId())
        .eq(req.getExamType() != null, ExamMakeExamPlanEntity::getExamType, req.getExamType())
        .like(StringUtils.hasText(req.getKeyword()), ExamMakeExamPlanEntity::getTitle, req.getKeyword())
        .orderByDesc(ExamMakeExamPlanEntity::getSortNumber)
        .orderByDesc(ExamMakeExamPlanEntity::getId);

    Page<ExamMakeExamPlanEntity> page = planMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamMakeExamPlanVO> vos = page.getRecords().stream()
        .map(ExamMakeExamPlanVO::from).toList();
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<ExamMakeExamPlanVO> list(Integer courseId, Integer examType) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamMakeExamPlanEntity> wrapper = new LambdaQueryWrapper<ExamMakeExamPlanEntity>()
        .eq(ExamMakeExamPlanEntity::getTenantId, tenantId)
        .eq(courseId != null, ExamMakeExamPlanEntity::getCourseId, courseId)
        .eq(examType != null, ExamMakeExamPlanEntity::getExamType, examType)
        .orderByDesc(ExamMakeExamPlanEntity::getSortNumber)
        .orderByDesc(ExamMakeExamPlanEntity::getId);

    return planMapper.selectList(wrapper).stream()
        .map(ExamMakeExamPlanVO::from).toList();
  }

  @Transactional(readOnly = true)
  public ExamMakeExamPlanVO getById(Integer id) {
    ExamMakeExamPlanEntity entity = findExisting(id);

    // 加载明细列表
    LambdaQueryWrapper<ExamMakeExamPlanDetailEntity> detailWrapper = new LambdaQueryWrapper<ExamMakeExamPlanDetailEntity>()
        .eq(ExamMakeExamPlanDetailEntity::getMakeId, id)
        .orderByAsc(ExamMakeExamPlanDetailEntity::getSortNumber)
        .orderByAsc(ExamMakeExamPlanDetailEntity::getId);
    List<ExamMakeExamPlanDetailEntity> details = planDetailMapper.selectList(detailWrapper);
    entity.setPlanDetailList(details);

    return ExamMakeExamPlanVO.from(entity);
  }

  @Transactional
  public ExamMakeExamPlanVO create(ExamMakeExamPlanCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    // 1. 保存主记录
    ExamMakeExamPlanEntity plan = new ExamMakeExamPlanEntity();
    plan.setTenantId(tenantId);
    plan.setTitle(req.title());
    plan.setCourseId(req.courseId());
    plan.setRateId(req.rateId());
    plan.setType(req.type());
    plan.setExamType(req.examType());
    plan.setTime(req.time());
    plan.setIsComputer(req.isComputer());
    plan.setIsPrint(req.isPrint());
    plan.setOpenRate(req.openRate());
    plan.setTotalPoint(req.totalPoint());
    plan.setPassPoint(req.passPoint());
    plan.setIsAll(req.isAll());
    plan.setComments(req.comments());
    plan.setStatus(1);
    plan.setUserId(userId.intValue());
    plan.setCreateTime(LocalDateTime.now());

    planMapper.insert(plan);

    // 2. 构建明细列表
    List<ExamMakeExamPlanDetailEntity> detailEntities = new ArrayList<>();

    if (req.examType() != null && req.examType() == 0) {
      // 理论组卷
      detailEntities = buildTheoryPlanDetails(plan, req.detailList(), tenantId, userId.intValue());
    } else if (req.examType() != null && req.examType() == 1) {
      // 实操组卷 - 明细来自前端
      if (req.detailList() != null) {
        detailEntities = saveDetailEntities(plan.getId(), plan.getCourseId(), req.detailList(), tenantId, userId.intValue());
      }
    }

    // 3. 生成试卷
    if (req.examType() != null && req.examType() == 0) {
      makeTheoryPaper(plan, detailEntities, userId.intValue());
    } else if (req.examType() != null && req.examType() == 1) {
      makeOperatePaper(plan, detailEntities, userId.intValue());
    }

    plan.setPlanDetailList(detailEntities);
    return ExamMakeExamPlanVO.from(plan);
  }

  @Transactional
  public ExamMakeExamPlanVO update(Integer id, ExamMakeExamPlanUpdateReq req) {
    ExamMakeExamPlanEntity plan = findExisting(id);
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    plan.setTitle(req.title());
    plan.setCourseId(req.courseId());
    plan.setRateId(req.rateId());
    plan.setType(req.type());
    plan.setExamType(req.examType());
    plan.setTime(req.time());
    plan.setIsComputer(req.isComputer());
    plan.setIsPrint(req.isPrint());
    plan.setOpenRate(req.openRate());
    plan.setTotalPoint(req.totalPoint());
    plan.setPassPoint(req.passPoint());
    plan.setIsAll(req.isAll());
    plan.setComments(req.comments());

    planMapper.updateById(plan);

    // 删旧明细
    LambdaQueryWrapper<ExamMakeExamPlanDetailEntity> delWrapper = new LambdaQueryWrapper<ExamMakeExamPlanDetailEntity>()
        .eq(ExamMakeExamPlanDetailEntity::getMakeId, id);
    planDetailMapper.delete(delWrapper);

    // 重建明细 + 试卷
    List<ExamMakeExamPlanDetailEntity> detailEntities = new ArrayList<>();

    if (req.examType() != null && req.examType() == 0) {
      detailEntities = buildTheoryPlanDetails(plan, req.detailList(), tenantId, userId.intValue());
      makeTheoryPaper(plan, detailEntities, userId.intValue());
    } else if (req.examType() != null && req.examType() == 1) {
      if (req.detailList() != null) {
        detailEntities = saveDetailEntities(plan.getId(), plan.getCourseId(), req.detailList(), tenantId, userId.intValue());
      }
      makeOperatePaper(plan, detailEntities, userId.intValue());
    }

    plan.setPlanDetailList(detailEntities);
    return ExamMakeExamPlanVO.from(plan);
  }

  @Transactional
  public void delete(Integer id) {
    findExisting(id);

    // 删除明细
    LambdaQueryWrapper<ExamMakeExamPlanDetailEntity> delWrapper = new LambdaQueryWrapper<ExamMakeExamPlanDetailEntity>()
        .eq(ExamMakeExamPlanDetailEntity::getMakeId, id);
    planDetailMapper.delete(delWrapper);

    // 删除关联试卷及其详情
    LambdaQueryWrapper<ExamPaperEntity> paperWrapper = new LambdaQueryWrapper<ExamPaperEntity>()
        .eq(ExamPaperEntity::getMakeId, id);
    List<ExamPaperEntity> papers = paperMapper.selectList(paperWrapper);
    for (ExamPaperEntity paper : papers) {
      paperDetailMapper.delete(new LambdaQueryWrapper<ExamPaperDetailEntity>()
          .eq(ExamPaperDetailEntity::getPaperId, paper.getId()));
      paperMapper.deleteById(paper.getId());
    }

    // 删除关联实操试卷及其详情
    LambdaQueryWrapper<ExamOperatePaperEntity> opWrapper = new LambdaQueryWrapper<ExamOperatePaperEntity>()
        .eq(ExamOperatePaperEntity::getMakeId, id);
    List<ExamOperatePaperEntity> opPapers = operatePaperMapper.selectList(opWrapper);
    for (ExamOperatePaperEntity opPaper : opPapers) {
      operatePaperDetailMapper.delete(new LambdaQueryWrapper<ExamOperatePaperDetailEntity>()
          .eq(ExamOperatePaperDetailEntity::getPaperId, opPaper.getId()));
      operatePaperMapper.deleteById(opPaper.getId());
    }

    planMapper.deleteById(id);
  }

  // ==================== 核心组卷逻辑 ====================

  /**
   * 构建理论组卷的计划明细，并持久化。
   * 全选模式(isAll=1)时自动统计题型+数量；自定义模式使用前端传入的 detailList。
   */
  private List<ExamMakeExamPlanDetailEntity> buildTheoryPlanDetails(
      ExamMakeExamPlanEntity plan,
      List<ExamMakeExamPlanDetailReq> inputDetails,
      Integer tenantId,
      Integer userId) {

    if (plan.getIsAll() != null && plan.getIsAll() == 1) {
      // 全选模式：查 L3 知识点 + 按题型分组统计
      List<Integer> l3Ids = knowledgeService.getLevel3IdsByCourse(plan.getCourseId());
      if (l3Ids.isEmpty()) {
        return new ArrayList<>();
      }

      // 按题型分组统计终审通过的试题
      LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
          .select(ExamExamDetailEntity::getType, ExamExamDetailEntity::getScore)
          .in(ExamExamDetailEntity::getKnowledgeId, l3Ids)
          .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
          .eq(ExamExamDetailEntity::getTenantId, tenantId)
          .groupBy(ExamExamDetailEntity::getType);

      // MyBatis-Plus select with groupBy: 使用 SQL 片段
      // 用 selectList + groupBy 不够精确，改用手动统计
      List<ExamExamDetailEntity> allDetails = examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>()
              .in(ExamExamDetailEntity::getKnowledgeId, l3Ids)
              .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
              .eq(ExamExamDetailEntity::getTenantId, tenantId));

      // 按题型分组
      var grouped = allDetails.stream()
          .collect(java.util.stream.Collectors.groupingBy(ExamExamDetailEntity::getType));

      List<ExamMakeExamPlanDetailEntity> result = new ArrayList<>();
      for (var entry : grouped.entrySet()) {
        String type = entry.getKey();
        List<ExamExamDetailEntity> items = entry.getValue();
        int num = items.size();
        BigDecimal point = items.isEmpty() ? BigDecimal.ZERO : items.get(0).getScore();

        ExamMakeExamPlanDetailEntity detail = new ExamMakeExamPlanDetailEntity();
        detail.setMakeId(plan.getId());
        detail.setCourseId(plan.getCourseId());
        detail.setType(type);
        detail.setNum(num);
        detail.setPoint(point);
        detail.setStartPoint(0);
        detail.setEndPoint(100);
        detail.setDifficultyStart(0);
        detail.setDifficultyEnd(4);
        detail.setTotalPoint(point.multiply(BigDecimal.valueOf(num)));
        detail.setUserId(userId);
        detail.setStatus(1);
        detail.setTenantId(tenantId);
        detail.setCreateTime(LocalDateTime.now());

        planDetailMapper.insert(detail);
        result.add(detail);
      }
      return result;
    } else {
      // 自定义模式
      if (inputDetails != null) {
        return saveDetailEntities(plan.getId(), plan.getCourseId(), inputDetails, tenantId, userId);
      }
      return new ArrayList<>();
    }
  }

  private List<ExamMakeExamPlanDetailEntity> saveDetailEntities(
      Integer makeId, Integer courseId,
      List<ExamMakeExamPlanDetailReq> reqList,
      Integer tenantId, Integer userId) {

    List<ExamMakeExamPlanDetailEntity> result = new ArrayList<>();
    int sortNum = 1;
    for (ExamMakeExamPlanDetailReq req : reqList) {
      ExamMakeExamPlanDetailEntity detail = new ExamMakeExamPlanDetailEntity();
      detail.setMakeId(makeId);
      detail.setCourseId(courseId);
      detail.setType(req.type());
      detail.setNum(req.num());
      detail.setPoint(req.point());
      detail.setStartPoint(req.startPoint());
      detail.setEndPoint(req.endPoint());
      detail.setDifficultyStart(req.difficultyStart());
      detail.setDifficultyEnd(req.difficultyEnd());
      if (req.point() != null && req.num() != null) {
        detail.setTotalPoint(req.point().multiply(BigDecimal.valueOf(req.num())));
      }
      detail.setUserId(userId);
      detail.setStatus(1);
      detail.setSortNumber(sortNum++);
      detail.setTenantId(tenantId);
      detail.setCreateTime(LocalDateTime.now());

      planDetailMapper.insert(detail);
      result.add(detail);
    }
    return result;
  }

  /**
   * 理论组卷：创建 exam_paper + exam_paper_detail
   */
  private void makeTheoryPaper(ExamMakeExamPlanEntity plan,
                               List<ExamMakeExamPlanDetailEntity> detailList,
                               Integer userId) {
    // 查找或创建试卷
    ExamPaperEntity existingPaper = paperMapper.selectOne(
        new LambdaQueryWrapper<ExamPaperEntity>()
            .eq(ExamPaperEntity::getMakeId, plan.getId())
            .last("LIMIT 1"));

    ExamPaperEntity paper;
    if (existingPaper != null) {
      paper = existingPaper;
      paper.setTitle(plan.getTitle());
      paper.setTime(plan.getTime());
      paper.setTotalPoint(plan.getTotalPoint());
      paper.setPassPoint(plan.getPassPoint());
      paper.setIsComputer(plan.getIsComputer());
      paper.setIsPrint(plan.getIsPrint());
      paperMapper.updateById(paper);

      // 清除旧详情
      paperDetailMapper.delete(new LambdaQueryWrapper<ExamPaperDetailEntity>()
          .eq(ExamPaperDetailEntity::getPaperId, paper.getId()));
    } else {
      paper = new ExamPaperEntity();
      paper.setMakeId(plan.getId());
      paper.setUserId(userId);
      paper.setTitle(plan.getTitle());
      paper.setTime(plan.getTime());
      paper.setTotalPoint(plan.getTotalPoint());
      paper.setPassPoint(plan.getPassPoint());
      paper.setIsComputer(plan.getIsComputer());
      paper.setIsPrint(plan.getIsPrint());
      paper.setCode(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));
      paper.setStatus(1);
      paper.setTenantId(plan.getTenantId());
      paper.setCreateTime(LocalDateTime.now());
      paperMapper.insert(paper);
    }

    // 获取 L3 知识点
    List<Integer> l3Ids = knowledgeService.getLevel3IdsByCourse(plan.getCourseId());
    if (l3Ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "该科目下无可用三级知识点，无法组卷；请先在题库配置知识点树");
    }

    boolean isAll = plan.getIsAll() != null && plan.getIsAll() == 1;
    if (!isAll && (detailList == null || detailList.isEmpty())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "自定义组卷请至少添加一种题型配置，或改用「全选(自动统计)」模式");
    }

    // 加载比重详情 (如果有 rateId)
    List<ExamKnowledgeRateDetailEntity> rateDetailList = new ArrayList<>();
    if (plan.getRateId() != null && !isAll) {
      rateDetailList = knowledgeRateDetailMapper.selectList(
          new LambdaQueryWrapper<ExamKnowledgeRateDetailEntity>()
              .eq(ExamKnowledgeRateDetailEntity::getKnowledgeRateId, plan.getRateId()));
    }

    List<ExamPaperDetailEntity> paperDetails = new ArrayList<>();
    List<Integer> addedExamIds = new ArrayList<>();
    final Integer finalPaperId = paper.getId();
    final Integer tenantId = plan.getTenantId();

    if (isAll) {
      // 全选模式：抽取所有终审通过的试题
      List<ExamExamDetailEntity> examList = examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>()
              .in(ExamExamDetailEntity::getKnowledgeId, l3Ids)
              .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
              .eq(ExamExamDetailEntity::getTenantId, tenantId)
              .last("ORDER BY RAND()"));

      for (ExamExamDetailEntity exam : examList) {
        paperDetails.add(buildPaperDetail(finalPaperId, exam, tenantId));
      }
    } else {
      // 自定义模式
      for (ExamMakeExamPlanDetailEntity detail : detailList) {
        List<ExamPaperDetailEntity> tmpList = new ArrayList<>();
        double num = detail.getNum() != null ? detail.getNum() : 0;
        String type = normalizePlanQuestionType(detail.getType());
        int diffStart = detail.getDifficultyStart() != null ? detail.getDifficultyStart() : 0;
        int diffEnd = detail.getDifficultyEnd() != null ? detail.getDifficultyEnd() : 4;
        int scoreStart = detail.getStartPoint() != null ? detail.getStartPoint() : 0;
        int scoreEnd = detail.getEndPoint() != null ? detail.getEndPoint() : 100;

        boolean isSubjectiveType = isSubjectiveType(type);

        if (isSubjectiveType) {
          // 主观题：直接从 L3 知识点中按题型+难度+分值抽
          List<ExamExamDetailEntity> examList = examDetailMapper.selectList(
              applyDrawFilters(
                  new LambdaQueryWrapper<ExamExamDetailEntity>()
                      .in(ExamExamDetailEntity::getKnowledgeId, l3Ids)
                      .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
                      .eq(ExamExamDetailEntity::getTenantId, tenantId)
                      .eq(ExamExamDetailEntity::getType, type),
                  diffStart, diffEnd, scoreStart, scoreEnd, addedExamIds));

          for (int i = 0; i < num && i < examList.size(); i++) {
            ExamExamDetailEntity exam = examList.get(i);
            addedExamIds.add(exam.getId());
            ExamPaperDetailEntity pd = buildPaperDetail(finalPaperId, exam, tenantId);
            if (detail.getPoint() != null) pd.setScore(detail.getPoint());
            tmpList.add(pd);
          }
        } else if (!rateDetailList.isEmpty()) {
          // 有比重：按知识点比重分配题数
          for (ExamKnowledgeRateDetailEntity rateDetail : rateDetailList) {
            List<Integer> subL3Ids = knowledgeService.getLevel3DescendantIds(rateDetail.getKnowledgeId());
            if (subL3Ids.isEmpty()) continue;

            double subNum = Math.ceil((double) detail.getNum() * rateDetail.getRate() / 100);

            List<ExamExamDetailEntity> examList = examDetailMapper.selectList(
                applyDrawFilters(
                    new LambdaQueryWrapper<ExamExamDetailEntity>()
                        .in(ExamExamDetailEntity::getKnowledgeId, subL3Ids)
                        .eq(ExamExamDetailEntity::getType, type)
                        .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
                        .eq(ExamExamDetailEntity::getTenantId, tenantId),
                    diffStart, diffEnd, scoreStart, scoreEnd, addedExamIds));
            for (int i = 0; i < subNum && i < examList.size(); i++) {
              ExamExamDetailEntity exam = examList.get(i);
              addedExamIds.add(exam.getId());
              ExamPaperDetailEntity pd = buildPaperDetail(finalPaperId, exam, tenantId);
              if (detail.getPoint() != null) pd.setScore(detail.getPoint());
              tmpList.add(pd);
            }
          }
        } else {
          // 无比重：直接从 L3 中按题型+难度+分值抽
          List<ExamExamDetailEntity> examList = examDetailMapper.selectList(
              applyDrawFilters(
                  new LambdaQueryWrapper<ExamExamDetailEntity>()
                      .in(ExamExamDetailEntity::getKnowledgeId, l3Ids)
                      .eq(ExamExamDetailEntity::getType, type)
                      .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
                      .eq(ExamExamDetailEntity::getTenantId, tenantId),
                  diffStart, diffEnd, scoreStart, scoreEnd, addedExamIds));
          for (int i = 0; i < num && i < examList.size(); i++) {
            ExamExamDetailEntity exam = examList.get(i);
            addedExamIds.add(exam.getId());
            ExamPaperDetailEntity pd = buildPaperDetail(finalPaperId, exam, tenantId);
            if (detail.getPoint() != null) pd.setScore(detail.getPoint());
            tmpList.add(pd);
          }
        }

        // 限制为 detail.getNum() 条
        for (int i = 0; i < detail.getNum() && i < tmpList.size(); i++) {
          paperDetails.add(tmpList.get(i));
        }
      }
    }

    if (paperDetails.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          isAll
              ? "该科目下暂无终审通过的试题，无法生成正式卷"
              : "按当前题型/难度/分值条件未抽到试题，请调整配置或改用全选模式");
    }

    // 批量插入试卷详情
    for (ExamPaperDetailEntity pd : paperDetails) {
      paperDetailMapper.insert(pd);
    }
  }

  private LambdaQueryWrapper<ExamExamDetailEntity> applyDrawFilters(
      LambdaQueryWrapper<ExamExamDetailEntity> wrapper,
      int diffStart,
      int diffEnd,
      int scoreStart,
      int scoreEnd,
      List<Integer> addedExamIds) {
    wrapper.between(ExamExamDetailEntity::getDifficulty, diffStart, diffEnd)
        .between(ExamExamDetailEntity::getScore, scoreStart, scoreEnd);
    if (addedExamIds != null && !addedExamIds.isEmpty()) {
      wrapper.notIn(ExamExamDetailEntity::getId, addedExamIds);
    }
    wrapper.last("ORDER BY RAND()");
    return wrapper;
  }

  /** 表单历史值 multiple → 题库实际存储 multi */
  private String normalizePlanQuestionType(String type) {
    if (type == null) {
      return null;
    }
    String t = type.trim().toLowerCase();
    if ("multiple".equals(t) || "duoxuan".equals(t) || "多选".equals(type)) {
      return "multi";
    }
    if ("danxuan".equals(t) || "单选".equals(type)) {
      return "single";
    }
    if ("pandan".equals(t) || "判断".equals(type)) {
      return "judge";
    }
    return type;
  }

  /**
   * 实操组卷：创建 exam_operate_paper + exam_operate_paper_detail
   */
  private void makeOperatePaper(ExamMakeExamPlanEntity plan,
                                List<ExamMakeExamPlanDetailEntity> detailList,
                                Integer userId) {
    ExamCourseEntity course = courseMapper.selectById(plan.getCourseId());
    if (course == null) return;

    String levelName = (course.getLevel() != null && course.getLevel() >= 0
        && course.getLevel() < LEVEL_NAMES.length)
        ? LEVEL_NAMES[course.getLevel()] : "未知等级";

    // 查找或创建实操试卷
    ExamOperatePaperEntity existingPaper = operatePaperMapper.selectOne(
        new LambdaQueryWrapper<ExamOperatePaperEntity>()
            .eq(ExamOperatePaperEntity::getMakeId, plan.getId())
            .last("LIMIT 1"));

    ExamOperatePaperEntity opPaper;
    if (existingPaper != null) {
      opPaper = existingPaper;
      opPaper.setTitle(plan.getTitle());
      opPaper.setTotalPoint(plan.getTotalPoint());
      opPaper.setPassPoint(plan.getPassPoint());
      opPaper.setTime(plan.getTime());
      opPaper.setNum(plan.getNum());
      opPaper.setLevel(levelName);
      opPaper.setCode(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
          + String.format("%04d", new Random().nextInt(10000)));
      operatePaperMapper.updateById(opPaper);

      operatePaperDetailMapper.delete(new LambdaQueryWrapper<ExamOperatePaperDetailEntity>()
          .eq(ExamOperatePaperDetailEntity::getPaperId, opPaper.getId()));
    } else {
      opPaper = new ExamOperatePaperEntity();
      opPaper.setMakeId(plan.getId());
      opPaper.setWorkTypeId(course.getWorkTypeId());
      opPaper.setCourseId(plan.getCourseId());
      opPaper.setNum(plan.getNum());
      opPaper.setLevel(levelName);
      opPaper.setTitle(plan.getTitle());
      opPaper.setTotalPoint(plan.getTotalPoint());
      opPaper.setPassPoint(plan.getPassPoint());
      opPaper.setTime(plan.getTime());
      opPaper.setCode(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
          + String.format("%04d", new Random().nextInt(10000)));
      opPaper.setUserId(userId);
      opPaper.setStatus(1);
      opPaper.setTenantId(plan.getTenantId());
      opPaper.setCreateTime(LocalDateTime.now());
      operatePaperMapper.insert(opPaper);
    }

    // 抽题逻辑
    final Integer paperId = opPaper.getId();
    final Integer tenantId = plan.getTenantId();

    if (detailList != null && !detailList.isEmpty()) {
      // 有计划明细时：按知识点的 L3 后代抽题
      for (ExamMakeExamPlanDetailEntity detail : detailList) {
        if (detail.getKnowledgeId() == null) continue;

        List<Integer> l3Ids = knowledgeService.getLevel3DescendantIds(detail.getKnowledgeId());
        if (l3Ids.isEmpty()) continue;

        List<ExamOperateDetailEntity> opDetails = operateDetailMapper.selectList(
            new LambdaQueryWrapper<ExamOperateDetailEntity>()
                .eq(ExamOperateDetailEntity::getCourseId, plan.getCourseId())
                .in(ExamOperateDetailEntity::getKnowledgeId, l3Ids)
                .eq(ExamOperateDetailEntity::getTenantId, tenantId)
                .last("ORDER BY RAND() LIMIT " + detail.getNum()));

        for (ExamOperateDetailEntity od : opDetails) {
          ExamOperatePaperDetailEntity opd = new ExamOperatePaperDetailEntity();
          opd.setPaperId(paperId);
          opd.setTitle(od.getTitle());
          opd.setStudentPrepare(od.getStudentPrepare());
          opd.setContent(od.getContent());
          opd.setScore(od.getScore());
          opd.setWeight(od.getWeight());
          opd.setEquipPrepare(od.getEquipPrepare());
          opd.setExamContent(od.getExamContent());
          opd.setScoringTemplateJson(od.getScoringTemplateJson());
          opd.setWeight(100);
          opd.setUserId(userId);
          opd.setStatus(1);
          opd.setTenantId(tenantId);
          opd.setCreateTime(LocalDateTime.now());
          operatePaperDetailMapper.insert(opd);
        }
      }
    } else {
      // 无明细时：直接从科目中随机抽 num 道题
      int num = plan.getNum() != null ? plan.getNum() : 0;
      if (num > 0) {
        List<ExamOperateDetailEntity> opDetails = operateDetailMapper.selectList(
            new LambdaQueryWrapper<ExamOperateDetailEntity>()
                .eq(ExamOperateDetailEntity::getCourseId, plan.getCourseId())
                .eq(ExamOperateDetailEntity::getTenantId, tenantId)
                .last("ORDER BY RAND() LIMIT " + num));

        for (ExamOperateDetailEntity od : opDetails) {
          ExamOperatePaperDetailEntity opd = new ExamOperatePaperDetailEntity();
          opd.setPaperId(paperId);
          opd.setTitle(od.getTitle());
          opd.setStudentPrepare(od.getStudentPrepare());
          opd.setContent(od.getContent());
          opd.setScore(od.getScore());
          opd.setWeight(od.getWeight());
          opd.setEquipPrepare(od.getEquipPrepare());
          opd.setExamContent(od.getExamContent());
          opd.setScoringTemplateJson(od.getScoringTemplateJson());
          opd.setWeight(100);
          opd.setUserId(userId);
          opd.setStatus(1);
          opd.setTenantId(tenantId);
          opd.setCreateTime(LocalDateTime.now());
          operatePaperDetailMapper.insert(opd);
        }
      }
    }
  }

  // ==================== 辅助方法 ====================

  private ExamPaperDetailEntity buildPaperDetail(Integer paperId, ExamExamDetailEntity exam, Integer tenantId) {
    ExamPaperDetailEntity pd = new ExamPaperDetailEntity();
    pd.setPaperId(paperId);
    pd.setExamId(exam.getId());
    pd.setType(exam.getType());
    pd.setDiff(exam.getDifficulty());
    pd.setContent(exam.getContent());
    pd.setAnswer(exam.getAnswer());
    pd.setScore(exam.getScore());
    pd.setResolve(exam.getResolve());
    pd.setStatus(1);
    pd.setTenantId(tenantId);
    pd.setCreateTime(LocalDateTime.now());
    return pd;
  }

  private boolean isSubjectiveType(String type) {
    if (type == null) return false;
    return type.equals("essay") || type.equals("explain") || type.equals("cal")
        || type.equals("simple_fill") || type.equals("answer") || type.equals("analysis");
  }

  private ExamMakeExamPlanEntity findExisting(Integer id) {
    ExamMakeExamPlanEntity entity = planMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "组卷计划不存在");
    }
    return entity;
  }
}

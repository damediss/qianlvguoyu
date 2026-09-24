package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainExamWorkTypeListVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamListApplicationService {

  private final TrainExamListMapper mapper;
  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final TrainExamPeopleListMapper peopleListMapper;
  private final ExamCourseMapper examCourseMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainPeopleListMapper trainPeopleListMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public TrainExamListApplicationService(TrainExamListMapper mapper,
      TrainExamWorkTypeListMapper workTypeListMapper,
      TrainExamPeopleListMapper peopleListMapper,
      ExamCourseMapper examCourseMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamExamDetailMapper examDetailMapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainPeopleListMapper trainPeopleListMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.workTypeListMapper = workTypeListMapper;
    this.peopleListMapper = peopleListMapper;
    this.examCourseMapper = examCourseMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.examDetailMapper = examDetailMapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.trainPeopleListMapper = trainPeopleListMapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamListVO> page(TrainExamListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamListEntity> wrapper = new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getDeleted, 0)
        .orderByDesc(TrainExamListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainExamListEntity::getTrainId, req.getTrainId());
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(TrainExamListEntity::getOrgId, companyFilter);
    }
    if (req.getType() != null) {
      wrapper.eq(TrainExamListEntity::getType, req.getType());
    }
    if (req.getSource() != null) {
      wrapper.eq(TrainExamListEntity::getSource, req.getSource());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainExamListEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainExamListEntity::getOrgName, req.getKeyword()));
    }
    Page<TrainExamListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypeList(
        page.getRecords().stream().map(TrainExamListEntity::getId).toList());
    return new PageResponse<>(page.getRecords().stream()
            .map(e -> toVO(e, wtMap.get(e.getId()))).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamListVO getById(Long id) {
    TrainExamListEntity entity = getExisting(id);
    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypeList(List.of(id));
    return toVO(entity, wtMap.get(id));
  }

  @Transactional
  public TrainExamListVO create(TrainExamListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainExamListEntity entity = new TrainExamListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setLevel(req.getLevel());
    entity.setTrainId(req.getTrainId());
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setType(req.getType());
    entity.setSource(req.getSource());
    entity.setInvigilateUserId(req.getInvigilateUserId());
    entity.setInvigilateUsername(req.getInvigilateUsername());
    entity.setEvaluateUserId(req.getEvaluateUserId());
    entity.setEvaluateUsername(req.getEvaluateUsername());
    entity.setWatcherUserId(req.getWatcherUserId());
    entity.setWatcherUsername(req.getWatcherUsername());
    entity.setStaffUserId(req.getStaffUserId());
    entity.setStaffUsername(req.getStaffUsername());
    entity.setPropositionUserId(req.getPropositionUserId());
    entity.setPropositionUsername(req.getPropositionUsername());
    entity.setMarkingUserId(req.getMarkingUserId());
    entity.setMarkingUsername(req.getMarkingUsername());
    entity.setExamId(req.getExamId());
    entity.setExamDate(req.getExamDate());
    entity.setExamDateMode(req.getExamDateMode());
    entity.setExamDateList(req.getExamDateList());
    entity.setExamTime(req.getExamTime());
    entity.setNeedFinishAdventure(req.getNeedFinishAdventure());
    entity.setNeedFinishNormal(req.getNeedFinishNormal());
    entity.setJigouId(req.getJigouId());
    entity.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : 1);
    entity.setCreateTime(LocalDateTime.now());
    mapper.insert(entity);

    if (req.getCourseIdList() != null && !req.getCourseIdList().isEmpty()) {
      saveWorkTypeList(entity.getId(), req.getCourseIdList(), tenantId);
    }

    // 创建考试计划时，若选择了培训，自动从培训导入考生
    if (req.getTrainId() != null) {
      importPeopleFromTrain(entity.getId(), req.getTrainId());
    }

    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypeList(List.of(entity.getId()));
    return toVO(entity, wtMap.get(entity.getId()));
  }

  /**
   * 试题配置自动开通同步学：创建本班考试计划。
   * 不导入全培训考生（由班级 enroll-roster 按花名册写入）。
   * <p>同培训下若已存在标题「{班级名}-同步学」则直接复用，禁止重复插入。
   */
  @Transactional
  public Long createForClassReviewSync(TrainInfoClassEntity clazz, List<Integer> courseIds) {
    if (clazz == null || clazz.getId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级无效，无法创建同步学考试计划");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    String classTitle = StringUtils.hasText(clazz.getTitle()) ? clazz.getTitle().trim() : ("班级" + clazz.getId());
    String title = classTitle + "-同步学";
    if (title.length() > 255) {
      title = title.substring(0, 255);
    }

    // 幂等：同培训同标题已存在则复用最早一条，避免保存试题配置反复新建
    if (clazz.getTrainInfoId() != null && clazz.getTrainInfoId() > 0) {
      TrainExamListEntity existed = mapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
          .eq(TrainExamListEntity::getTrainId, clazz.getTrainInfoId())
          .eq(TrainExamListEntity::getTitle, title)
          .eq(TrainExamListEntity::getDeleted, 0)
          .orderByAsc(TrainExamListEntity::getId)
          .last("LIMIT 1"));
      if (existed != null && existed.getId() != null) {
        List<Long> courseIdList = courseIds == null ? List.of() : courseIds.stream()
            .filter(id -> id != null && id > 0)
            .map(Integer::longValue)
            .distinct()
            .toList();
        if (!courseIdList.isEmpty()) {
          try {
            syncCoursesForReviewSync(existed.getId(), courseIds);
          } catch (Exception e) {
            log.warn("[班级同步学] 复用已有计划时同步科目失败 examId={}: {}", existed.getId(), e.getMessage());
          }
        }
        log.info("[班级同步学] 复用已有考试计划 examId={} title={} classId={}",
            existed.getId(), title, clazz.getId());
        return existed.getId();
      }
    }

    Long orgId = clazz.getCompanyId() != null && clazz.getCompanyId() > 0 ? clazz.getCompanyId() : null;
    String orgName = null;
    if (clazz.getTrainInfoId() != null && clazz.getTrainInfoId() > 0) {
      TrainInfoListEntity info = trainInfoListMapper.selectById(clazz.getTrainInfoId());
      if (info != null) {
        if (orgId == null && info.getCompanyId() != null && info.getCompanyId() > 0) {
          orgId = info.getCompanyId();
        }
        if (StringUtils.hasText(info.getCompanyName())) {
          orgName = info.getCompanyName().trim();
        }
      }
    }
    if (orgId == null) {
      Long scopeOrgId = operatorScopeService.currentScope().orgId();
      if (scopeOrgId != null && scopeOrgId > 0) {
        orgId = scopeOrgId;
      }
    }
    if (orgId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "班级未关联企业(org_id)，无法自动创建同步学考试计划，请先完善班级/培训企业信息");
    }

    TrainExamListEntity entity = new TrainExamListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(title);
    if (clazz.getWorkTypeLevel() != null && clazz.getWorkTypeLevel() > 0) {
      entity.setLevel(String.valueOf(clazz.getWorkTypeLevel()));
    }
    entity.setTrainId(clazz.getTrainInfoId());
    entity.setOrgId(orgId);
    if (StringUtils.hasText(orgName)) {
      entity.setOrgName(orgName);
    }
    entity.setType(0);
    List<Long> courseIdList = courseIds == null ? List.of() : courseIds.stream()
        .filter(id -> id != null && id > 0)
        .map(Integer::longValue)
        .distinct()
        .toList();
    Integer detectedSource = detectDominantQuestionSource(courseIdList);
    entity.setSource(detectedSource != null ? detectedSource : 0);
    entity.setSortNumber(1);
    entity.setCreateTime(LocalDateTime.now());
    mapper.insert(entity);

    if (!courseIdList.isEmpty()) {
      saveWorkTypeList(entity.getId(), courseIdList, tenantId);
    } else if (clazz.getWorkTypeId() != null && clazz.getWorkTypeId() > 0) {
      TrainExamWorkTypeListEntity wte = new TrainExamWorkTypeListEntity();
      wte.setExamId(entity.getId());
      wte.setWorkTypeId(clazz.getWorkTypeId());
      wte.setTenantId(tenantId);
      if (clazz.getWorkTypeLevel() != null) {
        wte.setLevel(String.valueOf(clazz.getWorkTypeLevel()));
      }
      wte.setCreateTime(LocalDateTime.now());
      wte.setDeleted(0);
      workTypeListMapper.insert(wte);
    }
    log.info("[班级同步学] 已为 classId={} 创建考试计划 examId={} source={}",
        clazz.getId(), entity.getId(), entity.getSource());
    return entity.getId();
  }

  /**
   * 把刷题试题配置的科目写回考试计划工种绑定，并校正 source 与题库一致。
   * 解决：已绑定/匹配到的考试计划无 courseId 或 source 与题不符 → App「该计划未绑定有效题库」。
   */
  @Transactional
  public void syncCoursesForReviewSync(Long examId, List<Integer> courseIds) {
    if (examId == null || examId <= 0 || courseIds == null || courseIds.isEmpty()) {
      return;
    }
    TrainExamListEntity exam = getExisting(examId);
    Long tenantId = exam.getTenantId() != null ? exam.getTenantId() : SecurityContext.currentUser().getTenantId();
    List<Long> courseIdList = courseIds.stream()
        .filter(id -> id != null && id > 0)
        .map(Integer::longValue)
        .distinct()
        .toList();
    if (courseIdList.isEmpty()) {
      return;
    }
    saveWorkTypeList(examId, courseIdList, tenantId);
    Integer detectedSource = detectDominantQuestionSource(courseIdList);
    if (detectedSource != null && !Objects.equals(exam.getSource(), detectedSource)) {
      exam.setSource(detectedSource);
      mapper.updateById(exam);
      log.info("[班级同步学] examId={} source 校正为 {}（与科目题库一致）", examId, detectedSource);
    }
  }

  /**
   * 在科目下 L3 知识点中，取 final_check_status=1 题目数量最多的 source。
   * 与同步学建计划口径一致；无题返回 null。
   */
  Integer detectDominantQuestionSource(List<Long> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return null;
    }
    List<Integer> courseInts = courseIds.stream().map(Long::intValue).toList();
    List<ExamKnowledgeStructureEntity> l3Nodes = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .select(ExamKnowledgeStructureEntity::getId)
            .eq(ExamKnowledgeStructureEntity::getLevel, 3)
            .eq(ExamKnowledgeStructureEntity::getType, 0)
            .in(ExamKnowledgeStructureEntity::getCourseId, courseInts));
    if (l3Nodes.isEmpty()) {
      return null;
    }
    List<Integer> knowledgeIds = l3Nodes.stream()
        .map(ExamKnowledgeStructureEntity::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (knowledgeIds.isEmpty()) {
      return null;
    }
    Map<Integer, Long> sourceCounts = new LinkedHashMap<>();
    final int chunk = 500;
    for (int i = 0; i < knowledgeIds.size(); i += chunk) {
      List<Integer> part = knowledgeIds.subList(i, Math.min(i + chunk, knowledgeIds.size()));
      List<ExamExamDetailEntity> rows = examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>()
              .select(ExamExamDetailEntity::getSource)
              .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
              .eq(ExamExamDetailEntity::getDeleted, 0)
              .in(ExamExamDetailEntity::getKnowledgeId, part));
      for (ExamExamDetailEntity row : rows) {
        if (row == null || row.getSource() == null) {
          continue;
        }
        sourceCounts.merge(row.getSource(), 1L, Long::sum);
      }
    }
    return sourceCounts.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  @Transactional
  public TrainExamListVO update(Long id, TrainExamListUpdateReq req) {
    TrainExamListEntity entity = getExisting(id);
    Long userId = SecurityContext.currentUser().getUserId();
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getLevel())) { entity.setLevel(req.getLevel()); }
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getOrgName())) { entity.setOrgName(req.getOrgName()); }
    if (req.getType() != null) { entity.setType(req.getType()); }
    if (req.getSource() != null) { entity.setSource(req.getSource()); }
    if (req.getInvigilateUserId() != null) { entity.setInvigilateUserId(req.getInvigilateUserId()); }
    if (StringUtils.hasText(req.getInvigilateUsername())) { entity.setInvigilateUsername(req.getInvigilateUsername()); }
    if (req.getEvaluateUserId() != null) { entity.setEvaluateUserId(req.getEvaluateUserId()); }
    if (StringUtils.hasText(req.getEvaluateUsername())) { entity.setEvaluateUsername(req.getEvaluateUsername()); }
    if (req.getWatcherUserId() != null) { entity.setWatcherUserId(req.getWatcherUserId()); }
    if (StringUtils.hasText(req.getWatcherUsername())) { entity.setWatcherUsername(req.getWatcherUsername()); }
    if (req.getStaffUserId() != null) { entity.setStaffUserId(req.getStaffUserId()); }
    if (StringUtils.hasText(req.getStaffUsername())) { entity.setStaffUsername(req.getStaffUsername()); }
    if (req.getPropositionUserId() != null) { entity.setPropositionUserId(req.getPropositionUserId()); }
    if (StringUtils.hasText(req.getPropositionUsername())) { entity.setPropositionUsername(req.getPropositionUsername()); }
    if (req.getMarkingUserId() != null) { entity.setMarkingUserId(req.getMarkingUserId()); }
    if (StringUtils.hasText(req.getMarkingUsername())) { entity.setMarkingUsername(req.getMarkingUsername()); }
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (StringUtils.hasText(req.getExamDate())) { entity.setExamDate(req.getExamDate()); }
    if (req.getExamDateMode() != null) { entity.setExamDateMode(req.getExamDateMode()); }
    if (StringUtils.hasText(req.getExamDateList())) { entity.setExamDateList(req.getExamDateList()); }
    if (req.getExamTime() != null) { entity.setExamTime(req.getExamTime()); }
    if (req.getNeedFinishAdventure() != null) { entity.setNeedFinishAdventure(req.getNeedFinishAdventure()); }
    if (req.getNeedFinishNormal() != null) { entity.setNeedFinishNormal(req.getNeedFinishNormal()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    mapper.updateById(entity);

    if (req.getCourseIdList() != null && !req.getCourseIdList().isEmpty()) {
      saveWorkTypeList(id, req.getCourseIdList(), entity.getTenantId());
    }

    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypeList(List.of(id));
    return toVO(entity, wtMap.get(id));
  }

  @Transactional
  public void delete(Long id) {
    TrainExamListEntity entity = getExisting(id);
    mapper.deleteById(entity.getId());
  }

  // ======== People management (考生管理) ========

  @Transactional(readOnly = true)
  public PageResponse<TrainExamPeopleListVO> getPeopleGradeList(Long examId, String keyword, Integer page, Integer pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleListEntity::getExamId, examId)
        .eq(TrainExamPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainExamPeopleListEntity::getId);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(TrainExamPeopleListEntity::getRealname, keyword)
          .or()
          .like(TrainExamPeopleListEntity::getPhone, keyword));
    }
    Page<TrainExamPeopleListEntity> result = peopleListMapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10), wrapper);
    return new PageResponse<>(result.getRecords().stream().map(this::toPeopleVO).toList(),
        result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public void updatePeopleGrade(Long id, Integer inWhiteList, BigDecimal normalScore,
      BigDecimal score, BigDecimal totalScore) {
    TrainExamPeopleListEntity entity = getPeopleExisting(id);
    if (inWhiteList != null) {
      entity.setInWhiteList(inWhiteList);
    }
    if (normalScore != null && totalScore != null) {
      entity.setNormalScore(normalScore);
      entity.setTotalScore(totalScore);
    }
    if (score != null && totalScore != null) {
      entity.setScore(score);
      entity.setTotalScore(totalScore);
    }
    peopleListMapper.updateById(entity);
  }

  @Transactional
  public int importPeopleByIdCard(Long examId, String idCardNumbers) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (examId == null || examId == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试ID不能为空");
    }
    if (!StringUtils.hasText(idCardNumbers)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有要导入的身份证号");
    }

    String[] idcards = idCardNumbers.split("[\\r\\n]+");
    int count = 0;
    for (String idcard : idcards) {
      idcard = idcard.trim();
      if (idcard.isEmpty() || idcard.length() != 18) {
        continue;
      }
      Long existingCount = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
          .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
          .eq(TrainExamPeopleListEntity::getExamId, examId)
          .eq(TrainExamPeopleListEntity::getIdCardNum, idcard)
          .eq(TrainExamPeopleListEntity::getDeleted, 0));
      if (existingCount > 0) {
        continue;
      }
      TrainExamPeopleListEntity entity = new TrainExamPeopleListEntity();
      entity.setExamId(examId);
      entity.setIdCardNum(idcard);
      entity.setTenantId(tenantId);
      entity.setSortNumber(1);
      entity.setLearnTime(0);
      entity.setLearnUseTime(0);
      peopleListMapper.insert(entity);
      count++;
    }
    return count;
  }

  @Transactional
  public void batchDeletePeople(List<Long> ids) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (ids == null || ids.isEmpty()) {
      return;
    }
    // 拆分：先按 tenant_id 过滤出当前租户可删的 id，再交给 MyBatis-Plus 的逻辑删除
    List<TrainExamPeopleListEntity> existingEntities = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainExamPeopleListEntity>()
            .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
            .in(TrainExamPeopleListEntity::getId, ids)
            .eq(TrainExamPeopleListEntity::getDeleted, 0));
    List<Long> existing = existingEntities.stream().map(TrainExamPeopleListEntity::getId).toList();
    if (!existing.isEmpty()) {
      peopleListMapper.deleteByIds(existing);
    }
  }

  /**
   * 从培训导入考生到考试计划。
   * 跳过已存在的（按 examId + idCardNum 判重），返回实际新增数量。
   */
  @Transactional
  public int importPeopleFromTrain(Long examId, Long trainId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (examId == null || trainId == null) {
      return 0;
    }
    // 取培训信息以获得 workTypeId / workTypeLevel
    TrainInfoListEntity trainInfo = trainInfoListMapper.selectById(trainId);
    Long workTypeId = trainInfo != null ? trainInfo.getWorkTypeId() : null;
    Integer workTypeLevel = trainInfo != null ? trainInfo.getWorkTypeLevel() : null;

    List<TrainPeopleListEntity> trainPeople = trainPeopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainId, trainId)
            .eq(TrainPeopleListEntity::getDeleted, 0));

    if (trainPeople.isEmpty()) {
      return 0;
    }

    // 已存在的 idCardNum，避免逐条 selectCount
    List<String> idCards = trainPeople.stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(Objects::nonNull)
        .filter(c -> !c.isEmpty())
        .toList();
    java.util.Set<String> existingIdCards = idCards.isEmpty() ? java.util.Set.of() :
        peopleListMapper.selectList(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
                .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
                .eq(TrainExamPeopleListEntity::getExamId, examId)
                .in(TrainExamPeopleListEntity::getIdCardNum, idCards)
                .eq(TrainExamPeopleListEntity::getDeleted, 0))
            .stream().map(TrainExamPeopleListEntity::getIdCardNum).collect(Collectors.toSet());

    int count = 0;
    for (TrainPeopleListEntity p : trainPeople) {
      if (p.getIdCard() == null || p.getIdCard().isEmpty()) {
        continue;
      }
      if (existingIdCards.contains(p.getIdCard())) {
        continue;
      }
      TrainExamPeopleListEntity entity = new TrainExamPeopleListEntity();
      entity.setExamId(examId);
      entity.setPlatformUserId(p.getPlatformUserId());
      entity.setWorkTypeId(workTypeId);
      entity.setWorkTypeLevel(workTypeLevel != null ? String.valueOf(workTypeLevel) : null);
      entity.setRealname(p.getRealname());
      entity.setIdCardNum(p.getIdCard());
      entity.setPhone(p.getPhone());
      if (p.getNormalScore() != null) {
        entity.setNormalScore(p.getNormalScore());
      }
      entity.setTenantId(tenantId);
      entity.setSortNumber(1);
      entity.setLearnTime(0);
      entity.setLearnUseTime(0);
      peopleListMapper.insert(entity);
      count++;
    }
    return count;
  }

  // ======== Private helpers ========

  /**
   * 删除已有 workTypeList 并按新课程列表重建。
   * 通过查 Course 补全 workTypeId / level。
   */
  private void saveWorkTypeList(Long examId, List<Long> courseIdList, Long tenantId) {
    // 先查现有记录拿到 id（用逻辑删除）
    List<Long> existingIds = workTypeListMapper.selectList(
            new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
                .eq(TrainExamWorkTypeListEntity::getExamId, examId)
                .eq(TrainExamWorkTypeListEntity::getDeleted, 0))
        .stream().map(TrainExamWorkTypeListEntity::getId).toList();
    if (!existingIds.isEmpty()) {
      workTypeListMapper.deleteByIds(existingIds);
    }
    for (Long courseId : courseIdList) {
      TrainExamWorkTypeListEntity wte = new TrainExamWorkTypeListEntity();
      wte.setExamId(examId);
      wte.setCourseId(courseId);
      wte.setTenantId(tenantId);
      // ExamCourseEntity.id 是 Integer，courseId 是 Long
      ExamCourseEntity course = examCourseMapper.selectById(courseId.intValue());
      if (course != null) {
        if (course.getWorkTypeId() != null) {
          wte.setWorkTypeId(course.getWorkTypeId().longValue());
        }
        if (course.getLevel() != null) {
          wte.setLevel(String.valueOf(course.getLevel()));
        }
      }
      workTypeListMapper.insert(wte);
    }
  }

  /** 批量加载若干 examId 对应的 workTypeList，按 examId 分组 */
  private Map<Long, List<TrainExamWorkTypeListEntity>> batchLoadWorkTypeList(List<Long> examIds) {
    if (examIds == null || examIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<TrainExamWorkTypeListEntity> all = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .in(TrainExamWorkTypeListEntity::getExamId, examIds)
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    Map<Long, List<TrainExamWorkTypeListEntity>> map = new HashMap<>();
    for (TrainExamWorkTypeListEntity w : all) {
      map.computeIfAbsent(w.getExamId(), k -> new ArrayList<>()).add(w);
    }
    return map;
  }

  private TrainExamPeopleListEntity getPeopleExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleListEntity entity = peopleListMapper.selectOne(
        new LambdaQueryWrapper<TrainExamPeopleListEntity>()
            .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
            .eq(TrainExamPeopleListEntity::getId, id)
            .eq(TrainExamPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getId, id)
        .eq(TrainExamListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    operatorScopeService.assertCompanyAccessible(entity.getOrgId());
    return entity;
  }

  private TrainExamListVO toVO(TrainExamListEntity entity, List<TrainExamWorkTypeListEntity> workTypeLists) {
    List<TrainExamWorkTypeListVO> wtVOs = null;
    List<Long> courseIds = null;
    if (workTypeLists != null && !workTypeLists.isEmpty()) {
      wtVOs = workTypeLists.stream()
          .map(w -> new TrainExamWorkTypeListVO(
              w.getId(), w.getExamId(), w.getWorkTypeId(), w.getCourseId(),
              w.getLevel(), w.getTenantId(), w.getCreateTime(), w.getUpdateTime()))
          .toList();
      courseIds = workTypeLists.stream().map(TrainExamWorkTypeListEntity::getCourseId).toList();
    }
    return new TrainExamListVO(
        entity.getId(), entity.getTitle(), entity.getLevel(), entity.getTrainId(),
        entity.getOrgId(), entity.getOrgName(), entity.getType(), entity.getSource(),
        entity.getInvigilateUserId(), entity.getInvigilateUsername(),
        entity.getEvaluateUserId(), entity.getEvaluateUsername(),
        entity.getWatcherUserId(), entity.getWatcherUsername(),
        entity.getStaffUserId(), entity.getStaffUsername(),
        entity.getPropositionUserId(), entity.getPropositionUsername(),
        entity.getMarkingUserId(), entity.getMarkingUsername(),
        entity.getExamId(), entity.getExamDate(), entity.getExamDateMode(),
        entity.getExamDateList(), entity.getExamTime(),
        entity.getNeedFinishAdventure(), entity.getNeedFinishNormal(),
        entity.getUserId(), entity.getSortNumber(), entity.getTenantId(),
        entity.getJigouId(), entity.getCreateTime(),
        wtVOs, courseIds
    );
  }

  private TrainExamPeopleListVO toPeopleVO(TrainExamPeopleListEntity e) {
    return new TrainExamPeopleListVO(
        e.getId(), e.getExamId(), e.getPlatformUserId(), e.getWorkTypeId(),
        e.getWorkTypeLevel(), e.getRealname(), e.getIdCardNum(), e.getPhone(),
        e.getEducation(), e.getRoomId(), e.getInWhiteList(), e.getNormalScore(),
        e.getScore(), e.getTotalScore(), e.getLearnTime(), e.getLearnUseTime(),
        e.getUserId(), e.getSortNumber(), e.getTenantId(), e.getCreateTime(), e.getUpdateTime()
    );
  }
}

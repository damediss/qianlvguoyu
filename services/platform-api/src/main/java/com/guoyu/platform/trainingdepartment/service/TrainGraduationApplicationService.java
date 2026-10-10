package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.GraduationStudentUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.support.TrainingIdCardHeaderResolver;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 结业与毕业典礼（O3）：班级列表、学员详情、证书回填关联。
 */
@Service
@Slf4j
public class TrainGraduationApplicationService {

  private static final int STATUS_PENDING = 0;
  private static final int STATUS_PASSED = 1;
  private static final int STATUS_FAILED = 2;
  private static final Set<Integer> PROCESSED = Set.of(STATUS_PASSED, STATUS_FAILED);
  private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainFileTempMapper fileTempMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainingIdCardHeaderResolver idCardHeaderResolver;
  private final TrainingClassAccessGuard classAccessGuard;
  private final TrainingOperatorScopeService operatorScopeService;
  private final String cdnPath;

  public TrainGraduationApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainCertificateMapper certificateMapper,
      TrainFileTempMapper fileTempMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainingIdCardHeaderResolver idCardHeaderResolver,
      TrainingClassAccessGuard classAccessGuard,
      TrainingOperatorScopeService operatorScopeService,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.certificateMapper = certificateMapper;
    this.fileTempMapper = fileTempMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.idCardHeaderResolver = idCardHeaderResolver;
    this.classAccessGuard = classAccessGuard;
    this.operatorScopeService = operatorScopeService;
    this.cdnPath = cdnPath;
  }

  private static final String EMPTY_FILTER = "__EMPTY__";

  @Transactional
  public Map<String, Object> graduationCandidates(
      Integer page,
      Integer pageSize,
      String keyword,
      String graduateStatus,
      List<String> trainTitles,
      List<String> companyNames,
      List<String> workTypeNames) {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    LambdaQueryWrapper<TrainInfoClassEntity> base = new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getDeleted, 0)
        .orderByDesc(TrainInfoClassEntity::getId);
    Long companyFilter = operatorScopeService.resolveCompanyFilter(null);
    if (companyFilter != null) {
      base.eq(TrainInfoClassEntity::getCompanyId, companyFilter);
    }
    if (StringUtils.hasText(keyword)) {
      base.like(TrainInfoClassEntity::getTitle, keyword.trim());
    }
    if (StringUtils.hasText(graduateStatus)) {
      base.eq(TrainInfoClassEntity::getGraduateStatus, graduateStatus.trim());
    }

    // 筛选项：基于关键词/结业状态范围（不含列筛选），最多取 2000 班
    Map<String, Object> facets = buildGraduationFacets(base);

    LambdaQueryWrapper<TrainInfoClassEntity> q = base.clone();
    applyGraduationColumnFilters(q, trainTitles, companyNames, workTypeNames);

    Page<TrainInfoClassEntity> mpPage = classMapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 20), q);

    List<Long> pageClassIds = mpPage.getRecords().stream().map(TrainInfoClassEntity::getId).toList();
    // 已获证（证书回填）学员自动结业，并刷新班级结业进度/状态
    syncGraduationFromCertificates(pageClassIds);
    Map<Long, TrainInfoClassEntity> classAfterSync = new HashMap<>();
    if (!pageClassIds.isEmpty()) {
      for (TrainInfoClassEntity refreshed : classMapper.selectBatchIds(pageClassIds)) {
        classAfterSync.put(refreshed.getId(), refreshed);
      }
    }

    List<Long> companyIds = new ArrayList<>();
    List<Long> workTypeIds = new ArrayList<>();
    List<Long> trainInfoIds = new ArrayList<>();
    for (TrainInfoClassEntity c : mpPage.getRecords()) {
      if (c.getCompanyId() != null) companyIds.add(c.getCompanyId());
      if (c.getWorkTypeId() != null) workTypeIds.add(c.getWorkTypeId());
      if (c.getTrainInfoId() != null) trainInfoIds.add(c.getTrainInfoId());
    }

    Map<Long, String> companyNameMap = loadCompanyNames(companyIds);
    Map<Long, String> workTypeNameMap = loadWorkTypeNames(workTypeIds);
    Map<Long, TrainInfoListEntity> infoMap = loadInfoMap(trainInfoIds);

    Map<Long, int[]> certStatsByClass = loadCertBackfillStats(pageClassIds);

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainInfoClassEntity raw : mpPage.getRecords()) {
      TrainInfoClassEntity c = classAfterSync.getOrDefault(raw.getId(), raw);
      List<TrainPeopleListEntity> students = listStudents(c.getId());
      int total = students.size();
      int graduated = (int) students.stream()
          .filter(s -> PROCESSED.contains(nzStatus(s.getGraduateStatus())))
          .count();
      int[] certStats = certStatsByClass.getOrDefault(c.getId(), new int[] {0, 0});
      int certBackfillCount = certStats[0];
      int certAnyCount = certStats[1];
      TrainInfoListEntity info = infoMap.get(c.getTrainInfoId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", c.getId());
      item.put("title", c.getTitle());
      item.put("trainInfoId", c.getTrainInfoId());
      item.put("trainTitle", info != null ? info.getTitle() : "");
      item.put("companyId", c.getCompanyId());
      item.put("companyName", companyNameMap.getOrDefault(c.getCompanyId(), ""));
      item.put("workTypeId", c.getWorkTypeId());
      item.put("workTypeName", workTypeNameMap.getOrDefault(c.getWorkTypeId(), ""));
      item.put("workTypeLevel", c.getWorkTypeLevel());
      item.put("studentCount", total);
      item.put("graduatedCount", graduated);
      item.put("certBackfillCount", certBackfillCount);
      item.put("certAnyCount", certAnyCount);
      item.put("hasCertBackfill", certBackfillCount > 0);
      item.put("graduateStatus", normalizeClassStatus(c.getGraduateStatus()));
      item.put("graduateAt", c.getGraduateAt());
      item.put("certificateTempId", c.getCertificateTempId());
      item.put("dateStart", c.getDateStart());
      item.put("dateEnd", c.getDateEnd());
      item.put("tenantId", tenantId);
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    result.put("facets", facets);
    return result;
  }

  private Map<String, Object> buildGraduationFacets(LambdaQueryWrapper<TrainInfoClassEntity> base) {
    LambdaQueryWrapper<TrainInfoClassEntity> facetQ = base.clone();
    facetQ.select(
        TrainInfoClassEntity::getId,
        TrainInfoClassEntity::getTrainInfoId,
        TrainInfoClassEntity::getCompanyId,
        TrainInfoClassEntity::getWorkTypeId);
    facetQ.last("LIMIT 2000");
    List<TrainInfoClassEntity> rows = classMapper.selectList(facetQ);
    List<Long> companyIds = new ArrayList<>();
    List<Long> workTypeIds = new ArrayList<>();
    List<Long> trainInfoIds = new ArrayList<>();
    boolean emptyTrain = false;
    boolean emptyCompany = false;
    boolean emptyWorkType = false;
    for (TrainInfoClassEntity c : rows) {
      if (c.getTrainInfoId() == null || c.getTrainInfoId() == 0L) {
        emptyTrain = true;
      } else {
        trainInfoIds.add(c.getTrainInfoId());
      }
      if (c.getCompanyId() == null || c.getCompanyId() == 0L) {
        emptyCompany = true;
      } else {
        companyIds.add(c.getCompanyId());
      }
      if (c.getWorkTypeId() == null || c.getWorkTypeId() == 0L) {
        emptyWorkType = true;
      } else {
        workTypeIds.add(c.getWorkTypeId());
      }
    }
    Map<Long, TrainInfoListEntity> infoMap = loadInfoMap(trainInfoIds);
    Map<Long, String> companyNameMap = loadCompanyNames(companyIds);
    Map<Long, String> workTypeNameMap = loadWorkTypeNames(workTypeIds);
    TreeSet<String> trainTitles = new TreeSet<>();
    TreeSet<String> companies = new TreeSet<>();
    TreeSet<String> workTypes = new TreeSet<>();
    for (TrainInfoListEntity info : infoMap.values()) {
      if (StringUtils.hasText(info.getTitle())) {
        trainTitles.add(info.getTitle().trim());
      }
    }
    companies.addAll(companyNameMap.values().stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.toSet()));
    workTypes.addAll(workTypeNameMap.values().stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.toSet()));
    Map<String, Object> facets = new LinkedHashMap<>();
    List<String> trainFacet = new ArrayList<>(trainTitles);
    if (emptyTrain) {
      trainFacet.add(EMPTY_FILTER);
    }
    List<String> companyFacet = new ArrayList<>(companies);
    if (emptyCompany) {
      companyFacet.add(EMPTY_FILTER);
    }
    List<String> workTypeFacet = new ArrayList<>(workTypes);
    if (emptyWorkType) {
      workTypeFacet.add(EMPTY_FILTER);
    }
    facets.put("trainTitles", trainFacet);
    facets.put("companyNames", companyFacet);
    facets.put("workTypeNames", workTypeFacet);
    return facets;
  }

  private void applyGraduationColumnFilters(
      LambdaQueryWrapper<TrainInfoClassEntity> q,
      List<String> trainTitles,
      List<String> companyNames,
      List<String> workTypeNames) {
    applyTrainTitleFilter(q, trainTitles);
    applyCompanyNameFilter(q, companyNames);
    applyWorkTypeNameFilter(q, workTypeNames);
  }

  private void applyTrainTitleFilter(LambdaQueryWrapper<TrainInfoClassEntity> q, List<String> trainTitles) {
    if (trainTitles == null || trainTitles.isEmpty()) {
      return;
    }
    boolean wantEmpty = trainTitles.stream().anyMatch(EMPTY_FILTER::equals);
    List<String> names = trainTitles.stream()
        .filter(t -> !EMPTY_FILTER.equals(t) && StringUtils.hasText(t))
        .map(String::trim)
        .distinct()
        .toList();
    List<Long> ids = names.isEmpty()
        ? List.of()
        : infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
                .in(TrainInfoListEntity::getTitle, names)
                .eq(TrainInfoListEntity::getDeleted, 0))
            .stream()
            .map(TrainInfoListEntity::getId)
            .toList();
    q.and(w -> {
      boolean started = false;
      if (!ids.isEmpty()) {
        w.in(TrainInfoClassEntity::getTrainInfoId, ids);
        started = true;
      }
      if (wantEmpty) {
        if (started) {
          w.or();
        }
        w.and(x -> x.isNull(TrainInfoClassEntity::getTrainInfoId)
            .or()
            .eq(TrainInfoClassEntity::getTrainInfoId, 0L));
        started = true;
      }
      if (!started) {
        w.eq(TrainInfoClassEntity::getId, -1L);
      }
    });
  }

  private void applyCompanyNameFilter(LambdaQueryWrapper<TrainInfoClassEntity> q, List<String> companyNames) {
    if (companyNames == null || companyNames.isEmpty()) {
      return;
    }
    boolean wantEmpty = companyNames.stream().anyMatch(EMPTY_FILTER::equals);
    List<String> names = companyNames.stream()
        .filter(t -> !EMPTY_FILTER.equals(t) && StringUtils.hasText(t))
        .map(String::trim)
        .distinct()
        .toList();
    List<Long> ids = names.isEmpty()
        ? List.of()
        : orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>().in(OrgEntity::getName, names))
            .stream()
            .map(OrgEntity::getId)
            .toList();
    q.and(w -> {
      boolean started = false;
      if (!ids.isEmpty()) {
        w.in(TrainInfoClassEntity::getCompanyId, ids);
        started = true;
      }
      if (wantEmpty) {
        if (started) {
          w.or();
        }
        w.and(x -> x.isNull(TrainInfoClassEntity::getCompanyId)
            .or()
            .eq(TrainInfoClassEntity::getCompanyId, 0L));
        started = true;
      }
      if (!started) {
        w.eq(TrainInfoClassEntity::getId, -1L);
      }
    });
  }

  private void applyWorkTypeNameFilter(LambdaQueryWrapper<TrainInfoClassEntity> q, List<String> workTypeNames) {
    if (workTypeNames == null || workTypeNames.isEmpty()) {
      return;
    }
    boolean wantEmpty = workTypeNames.stream().anyMatch(EMPTY_FILTER::equals);
    List<String> names = workTypeNames.stream()
        .filter(t -> !EMPTY_FILTER.equals(t) && StringUtils.hasText(t))
        .map(String::trim)
        .distinct()
        .toList();
    List<Long> ids = names.isEmpty()
        ? List.of()
        : examWorkTypeMapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
                .in(ExamWorkTypeEntity::getTitle, names)
                .eq(ExamWorkTypeEntity::getDeleted, 0))
            .stream()
            .map(e -> e.getId().longValue())
            .toList();
    q.and(w -> {
      boolean started = false;
      if (!ids.isEmpty()) {
        w.in(TrainInfoClassEntity::getWorkTypeId, ids);
        started = true;
      }
      if (wantEmpty) {
        if (started) {
          w.or();
        }
        w.and(x -> x.isNull(TrainInfoClassEntity::getWorkTypeId)
            .or()
            .eq(TrainInfoClassEntity::getWorkTypeId, 0L));
        started = true;
      }
      if (!started) {
        w.eq(TrainInfoClassEntity::getId, -1L);
      }
    });
  }

  @Transactional
  public Map<String, Object> graduationStudents(Long classId) {
    syncGraduationFromCertificates(List.of(classId));
    TrainInfoClassEntity clazz = requireClass(classId);
    List<TrainPeopleListEntity> students = listStudents(classId);

    String templateTitle = null;
    if (clazz.getCertificateTempId() != null) {
      TrainFileTempEntity temp = fileTempMapper.selectById(clazz.getCertificateTempId());
      if (temp != null && (temp.getDeleted() == null || temp.getDeleted() == 0)) {
        templateTitle = temp.getTitle();
      }
    }

    Map<Long, TrainCertificateEntity> certByPeople = loadCertsByPeople(classId);

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      boolean qualified = isQualified(s);
      int st = nzStatus(s.getGraduateStatus());
      TrainCertificateEntity cert = certByPeople.get(s.getId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", s.getId());
      item.put("realname", s.getRealname());
      item.put("phone", s.getPhone());
      item.put("idCard", s.getIdCard());
      item.put("ticketNo", s.getTicketNo());
      // 与考试模块（人社成绩）对齐：理论 / 技能 / 综合 / 工作业绩
      item.put("theoryScore", s.getTheoryScore());
      item.put("skillScore", s.getRealScore());
      item.put("examScore", s.getExamScore());
      item.put("performanceScore", s.getPerformanceScore());
      item.put("normalScore", s.getNormalScore());
      item.put("realScore", s.getRealScore());
      item.put("isLeader", s.getIsLeader());
      item.put("isCommittee", s.getIsCommittee());
      item.put("graduateStatus", st);
      item.put("graduateStatusLabel", statusLabel(st));
      item.put("graduateAt", s.getGraduateAt());
      item.put("qualified", qualified);
      item.put("qualifyHint", qualifyHint(s, qualified));
      item.put("examResultStatus", resolveExamResultStatus(s));
      item.put("templateTitle", templateTitle);
      item.put("certificateTempId", clazz.getCertificateTempId());
      item.put("certificateNo", cert != null ? cert.getCertificateNo() : null);
      item.put("certificateFilePath", cert != null ? cert.getFilePath() : null);
      item.put("certificateOccupation", cert != null ? cert.getOccupation() : null);
      item.put("certificateWorkType", cert != null ? cert.getWorkType() : null);
      item.put("certificateLevel", cert != null ? cert.getLevel() : null);
      item.put("certificateIssuingAuthority", cert != null ? cert.getIssuingAuthority() : null);
      item.put("certificateEvaluationOrg", cert != null ? cert.getEvaluationOrg() : null);
      item.put("certificateIssueDate", cert != null ? cert.getIssueDate() : null);
      item.put("certStatus", cert != null ? cert.getStatus() : null);
      item.put("hasCertificate", cert != null);
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classTitle", clazz.getTitle());
    result.put("graduateStatus", normalizeClassStatus(clazz.getGraduateStatus()));
    result.put("certificateTempId", clazz.getCertificateTempId());
    result.put("templateTitle", templateTitle);
    result.put("list", list);
    result.put("total", list.size());
    return result;
  }

  @Transactional
  public Map<String, Object> graduationStudentDetail(Long classId, Long peopleId) {
    syncGraduationFromCertificates(List.of(classId));
    requireClass(classId);
    TrainPeopleListEntity student = requireStudent(classId, peopleId);
    PlatformUserDetailEntity platformDetail = loadPlatformDetail(student.getIdCard());
    TrainSignUpPeopleSelfEntity signUp = loadSignUpProfile(student.getIdCard());

    String templateTitle = null;
    TrainInfoClassEntity clazz = requireClass(classId);
    if (clazz.getCertificateTempId() != null) {
      TrainFileTempEntity temp = fileTempMapper.selectById(clazz.getCertificateTempId());
      if (temp != null && (temp.getDeleted() == null || temp.getDeleted() == 0)) {
        templateTitle = temp.getTitle();
      }
    }

    TrainCertificateEntity cert = loadCertsByPeople(classId).get(student.getId());
    boolean qualified = isQualified(student);
    int st = nzStatus(student.getGraduateStatus());

    String detailHeader = platformDetail != null ? platformDetail.getIdcardHeader() : null;
    String signUpHeader = signUp != null ? signUp.getIdCardHeader() : null;
    String header = idCardHeaderResolver.resolveForStudent(student.getIdCard(), detailHeader, signUpHeader);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", student.getId());
    result.put("classId", classId);
    result.put("classTitle", clazz.getTitle());
    result.put("realname", firstNonBlank(student.getRealname(), platformDetail != null ? platformDetail.getRealname() : null, signUp != null ? signUp.getName() : null));
    result.put("phone", firstNonBlank(student.getPhone(), platformDetail != null ? platformDetail.getPhone() : null, signUp != null ? signUp.getPhone() : null));
    result.put("idCard", firstNonBlank(student.getIdCard(), platformDetail != null ? platformDetail.getIdcard() : null, signUp != null ? signUp.getIdCardNumber() : null));
    result.put("ticketNo", student.getTicketNo());
    result.put("gender", resolveGenderLabel(platformDetail, signUp));
    result.put("nation", firstNonBlank(platformDetail != null ? platformDetail.getNation() : null, signUp != null ? signUp.getNation() : null));
    result.put("education", firstNonBlank(platformDetail != null ? platformDetail.getEducation() : null, signUp != null ? signUp.getEducation() : null));
    result.put("political", firstNonBlank(platformDetail != null ? platformDetail.getPolitical() : null, signUp != null ? signUp.getPolitical() : null));
    result.put("address", firstNonBlank(platformDetail != null ? platformDetail.getAddress() : null, signUp != null ? signUp.getAddress() : null));
    result.put("theoryScore", student.getTheoryScore());
    result.put("skillScore", student.getRealScore());
    result.put("examScore", student.getExamScore());
    result.put("performanceScore", student.getPerformanceScore());
    result.put("normalScore", student.getNormalScore());
    result.put("qualified", qualified);
    result.put("qualifyHint", qualifyHint(student, qualified));
    result.put("graduateStatus", st);
    result.put("graduateStatusLabel", statusLabel(st));
    result.put("graduateAt", student.getGraduateAt());
    result.put("templateTitle", templateTitle);
    result.put("certificateNo", cert != null ? cert.getCertificateNo() : null);
    result.put("certificateFilePath", cert != null ? cert.getFilePath() : null);
    result.put("certificateOccupation", cert != null ? cert.getOccupation() : null);
    result.put("certificateWorkType", cert != null ? cert.getWorkType() : null);
    result.put("certificateLevel", cert != null ? cert.getLevel() : null);
    result.put("certificateIssuingAuthority", cert != null ? cert.getIssuingAuthority() : null);
    result.put("certificateEvaluationOrg", cert != null ? cert.getEvaluationOrg() : null);
    result.put("certificateIssueDate", cert != null ? cert.getIssueDate() : null);
    result.put("certificateIdDocType", cert != null ? cert.getIdDocType() : null);
    result.put("hasCertificate", cert != null);
    result.put("idCardHeader", header);
    result.put("idCardFront", resolvePhoto(
        platformDetail != null ? platformDetail.getIdcardFront() : null,
        signUp != null ? signUp.getIdCardFront() : null));
    result.put("idCardBack", resolvePhoto(
        platformDetail != null ? platformDetail.getIdcardBack() : null,
        signUp != null ? signUp.getIdCardBack() : null));
    result.put("idCardHeader2", resolvePhoto(signUp != null ? signUp.getIdCardHeader2() : null, null));
    return result;
  }

  @Transactional
  public Map<String, Object> updateGraduationStudent(Long classId, Long peopleId, GraduationStudentUpdateReq req) {
    TrainInfoClassEntity clazz = requireClass(classId);
    TrainPeopleListEntity student = requireStudent(classId, peopleId);
    if (req != null) {
      if (StringUtils.hasText(req.getRealname())) {
        student.setRealname(req.getRealname().trim());
      }
      if (req.getPhone() != null) {
        student.setPhone(req.getPhone().trim());
      }
      if (req.getIdCard() != null) {
        student.setIdCard(req.getIdCard().trim());
      }
      if (req.getTicketNo() != null) {
        student.setTicketNo(req.getTicketNo().trim());
      }
    }
    peopleListMapper.updateById(student);

    boolean touchPhotos = req != null && (req.getIdCardHeader() != null
        || req.getIdCardFront() != null
        || req.getIdCardBack() != null);
    if (touchPhotos) {
      upsertStudentPhotos(student, req);
    }
    if (req != null && req.getCertificateFilePath() != null) {
      upsertStudentCertificateFile(clazz, student, req.getCertificateFilePath().trim());
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", student.getId());
    result.put("realname", student.getRealname());
    result.put("phone", student.getPhone());
    result.put("idCard", student.getIdCard());
    result.put("ticketNo", student.getTicketNo());
    result.put("idCardHeader", req != null ? blankToNull(req.getIdCardHeader()) : null);
    result.put("idCardFront", req != null ? blankToNull(req.getIdCardFront()) : null);
    result.put("idCardBack", req != null ? blankToNull(req.getIdCardBack()) : null);
    result.put("certificateFilePath", req != null ? blankToNull(req.getCertificateFilePath()) : null);
    return result;
  }

  /** 写入 platform_user_detail + 报名档案；证件照更新时清本地 header 缓存文件，确保详情优先展示新图。 */
  private void upsertStudentPhotos(TrainPeopleListEntity student, GraduationStudentUpdateReq req) {
    String idCard = student.getIdCard() != null ? student.getIdCard().trim() : "";
    if (!StringUtils.hasText(idCard)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先填写身份证号后再上传证件照片");
    }

    if (req.getIdCardHeader() != null && StringUtils.hasText(req.getIdCardHeader())) {
      clearLocalHeaderFiles(idCard);
    }

    PlatformUserDetailEntity detail = loadPlatformDetail(idCard);
    if (detail == null) {
      detail = new PlatformUserDetailEntity();
      detail.setIdcard(idCard);
      detail.setRealname(student.getRealname());
      detail.setPhone(student.getPhone());
      detail.setUserId(student.getPlatformUserId() != null ? student.getPlatformUserId() : student.getUserId());
      detail.setDeleted(0);
      if (req.getIdCardHeader() != null) {
        detail.setIdcardHeader(blankToNull(req.getIdCardHeader()));
      }
      if (req.getIdCardFront() != null) {
        detail.setIdcardFront(blankToNull(req.getIdCardFront()));
      }
      if (req.getIdCardBack() != null) {
        detail.setIdcardBack(blankToNull(req.getIdCardBack()));
      }
      platformUserDetailMapper.insert(detail);
    } else {
      if (req.getIdCardHeader() != null) {
        detail.setIdcardHeader(blankToNull(req.getIdCardHeader()));
      }
      if (req.getIdCardFront() != null) {
        detail.setIdcardFront(blankToNull(req.getIdCardFront()));
      }
      if (req.getIdCardBack() != null) {
        detail.setIdcardBack(blankToNull(req.getIdCardBack()));
      }
      if (!StringUtils.hasText(detail.getRealname()) && StringUtils.hasText(student.getRealname())) {
        detail.setRealname(student.getRealname());
      }
      if (!StringUtils.hasText(detail.getPhone()) && StringUtils.hasText(student.getPhone())) {
        detail.setPhone(student.getPhone());
      }
      platformUserDetailMapper.updateById(detail);
    }

    TrainSignUpPeopleSelfEntity signUp = loadSignUpProfile(idCard);
    if (signUp != null) {
      if (req.getIdCardHeader() != null) {
        signUp.setIdCardHeader(blankToNull(req.getIdCardHeader()));
      }
      if (req.getIdCardFront() != null) {
        signUp.setIdCardFront(blankToNull(req.getIdCardFront()));
      }
      if (req.getIdCardBack() != null) {
        signUp.setIdCardBack(blankToNull(req.getIdCardBack()));
      }
      signUpPeopleSelfMapper.updateById(signUp);
    }
  }

  private void upsertStudentCertificateFile(
      TrainInfoClassEntity clazz, TrainPeopleListEntity student, String filePath) {
    Long tenantId = SecurityContext.currentUser() != null
        ? SecurityContext.currentUser().getTenantId()
        : null;
    Long userId = SecurityContext.currentUser() != null
        ? SecurityContext.currentUser().getUserId()
        : null;
    TrainCertificateEntity existing = certificateMapper.selectOne(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .eq(TrainCertificateEntity::getClassId, clazz.getId())
            .eq(TrainCertificateEntity::getPeopleId, student.getId())
            .eq(TrainCertificateEntity::getDeleted, 0)
            .orderByDesc(TrainCertificateEntity::getId)
            .last("LIMIT 1"));
    LocalDateTime now = LocalDateTime.now();
    if (existing == null) {
      if (!StringUtils.hasText(filePath)) {
        return;
      }
      TrainCertificateEntity cert = new TrainCertificateEntity();
      cert.setTenantId(tenantId);
      cert.setProjectId(clazz.getTrainInfoId());
      cert.setClassId(clazz.getId());
      cert.setPeopleId(student.getId());
      cert.setUserId(student.getPlatformUserId() != null ? student.getPlatformUserId() : student.getUserId());
      cert.setUserName(student.getRealname());
      cert.setCertificateType("skill_level_cert");
      cert.setFilePath(filePath);
      cert.setStatus(1);
      cert.setCreatedBy(userId);
      cert.setCreateTime(now);
      cert.setUpdateTime(now);
      cert.setDeleted(0);
      certificateMapper.insert(cert);
      syncGraduationFromCertificates(List.of(clazz.getId()));
      return;
    }
    existing.setFilePath(StringUtils.hasText(filePath) ? filePath : null);
    existing.setUpdateTime(now);
    certificateMapper.updateById(existing);
    if (StringUtils.hasText(filePath)) {
      syncGraduationFromCertificates(List.of(clazz.getId()));
    }
  }

  /**
   * 根据证书回填情况自动结业：已回填真实证书的未结业学员标为已结业，并刷新班级结业状态。
   * 不覆盖「不合格结业」(2)。
   */
  @Transactional
  public void syncGraduationFromCertificates(Collection<Long> classIds) {
    if (classIds == null || classIds.isEmpty()) {
      return;
    }
    List<Long> ids = classIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    if (ids.isEmpty()) {
      return;
    }
    Map<Long, Set<Long>> backfillPeopleByClass = loadBackfillPeopleByClass(ids);
    Long userId = SecurityContext.currentUser() != null
        ? SecurityContext.currentUser().getUserId()
        : null;
    LocalDateTime now = LocalDateTime.now();
    for (Long classId : ids) {
      Set<Long> peopleIds = backfillPeopleByClass.getOrDefault(classId, Set.of());
      if (!peopleIds.isEmpty()) {
        for (TrainPeopleListEntity person : peopleListMapper.selectBatchIds(peopleIds)) {
          if (person == null || (person.getDeleted() != null && person.getDeleted() != 0)) {
            continue;
          }
          if (!classId.equals(person.getTrainClassId())) {
            continue;
          }
          if (nzStatus(person.getGraduateStatus()) != STATUS_PENDING) {
            continue;
          }
          person.setGraduateStatus(STATUS_PASSED);
          person.setGraduateAt(now);
          person.setGraduateBy(userId);
          if (!StringUtils.hasText(person.getGraduateRemark())) {
            person.setGraduateRemark("证书回填自动结业");
          }
          peopleListMapper.updateById(person);
        }
      }
      refreshClassGraduateStatus(classId, now);
    }
  }

  private Map<Long, Set<Long>> loadBackfillPeopleByClass(List<Long> classIds) {
    Map<Long, Set<Long>> result = new HashMap<>();
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .in(TrainCertificateEntity::getClassId, classIds)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .isNotNull(TrainCertificateEntity::getPeopleId));
    for (TrainCertificateEntity c : rows) {
      if (!isBackfilledCertificate(c)) {
        continue;
      }
      Long classId = c.getClassId();
      Long peopleId = c.getPeopleId();
      if (classId == null || peopleId == null) {
        continue;
      }
      result.computeIfAbsent(classId, k -> new HashSet<>()).add(peopleId);
    }
    return result;
  }

  private void refreshClassGraduateStatus(Long classId, LocalDateTime now) {
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    if (clazz == null || (clazz.getDeleted() != null && clazz.getDeleted() != 0)) {
      return;
    }
    List<TrainPeopleListEntity> students = listStudents(classId);
    int total = students.size();
    int graduated = (int) students.stream()
        .filter(s -> PROCESSED.contains(nzStatus(s.getGraduateStatus())))
        .count();
    String status;
    if (total == 0 || graduated == 0) {
      status = "pending";
    } else if (graduated >= total) {
      status = "done";
    } else {
      status = "partial";
    }
    String old = normalizeClassStatus(clazz.getGraduateStatus());
    boolean needAt = "done".equals(status) && clazz.getGraduateAt() == null;
    if (!status.equals(old) || needAt) {
      clazz.setGraduateStatus(status);
      if (needAt) {
        clazz.setGraduateAt(now);
      }
      classMapper.updateById(clazz);
    }
  }

  /** 清除证件照本地缓存文件，避免盖住新上传的 platform_user_detail URL。 */
  private void clearLocalHeaderFiles(String idCard) {
    if (!StringUtils.hasText(idCard) || !StringUtils.hasText(cdnPath)) {
      return;
    }
    String id = idCard.trim();
    Path root = Paths.get(cdnPath).toAbsolutePath().normalize();
    Path[] candidates = new Path[] {
        root.resolve("idCard/old/headerOrigin").resolve(id + "_full.png"),
        root.resolve("idCard/headerOrigin").resolve(id + "_header2.jpg"),
        root.resolve("idCard/headerOrigin").resolve(id + "_header.jpg"),
    };
    for (Path path : candidates) {
      try {
        Files.deleteIfExists(path);
      } catch (Exception ex) {
        log.warn("[graduation] 清理本地证件照失败: {}", path, ex);
      }
    }
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * 与考试模块一致：理论≥60 且 技能≥60 为达标；
   * 理论与技能皆无时本期不拦结业（仍标「暂无成绩」）。
   */
  private boolean isQualified(TrainPeopleListEntity s) {
    BigDecimal theory = s.getTheoryScore();
    BigDecimal skill = s.getRealScore();
    if (theory == null && skill == null) {
      return true;
    }
    return theory != null
        && skill != null
        && theory.compareTo(PASS_SCORE) >= 0
        && skill.compareTo(PASS_SCORE) >= 0;
  }

  private String resolveExamResultStatus(TrainPeopleListEntity s) {
    BigDecimal theory = s.getTheoryScore();
    BigDecimal skill = s.getRealScore();
    if (theory == null && skill == null) {
      return "ABSENT";
    }
    if (theory != null
        && skill != null
        && theory.compareTo(PASS_SCORE) >= 0
        && skill.compareTo(PASS_SCORE) >= 0) {
      return "PASS";
    }
    return "FAIL";
  }

  private String qualifyHint(TrainPeopleListEntity s, boolean qualified) {
    if (s.getTheoryScore() == null && s.getRealScore() == null) {
      return "暂无成绩（本期不强制）";
    }
    if (qualified) {
      return "达标";
    }
    return "未达标（理论与技能需均≥60）";
  }

  private Map<Long, TrainCertificateEntity> loadCertsByPeople(Long classId) {
    Map<Long, TrainCertificateEntity> map = new HashMap<>();
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .eq(TrainCertificateEntity::getClassId, classId)
            .eq(TrainCertificateEntity::getDeleted, 0));
    for (TrainCertificateEntity c : rows) {
      if (c.getPeopleId() != null) {
        map.putIfAbsent(c.getPeopleId(), c);
      }
    }
    return map;
  }

  /**
   * 班级证书回填统计：key=classId，value=[回填人数(有文件或非GC假号), 任意证书人数]。
   */
  private Map<Long, int[]> loadCertBackfillStats(List<Long> classIds) {
    Map<Long, int[]> result = new HashMap<>();
    if (classIds == null || classIds.isEmpty()) {
      return result;
    }
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .in(TrainCertificateEntity::getClassId, classIds)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .isNotNull(TrainCertificateEntity::getPeopleId));
    Map<Long, Set<Long>> backfillPeople = new HashMap<>();
    Map<Long, Set<Long>> anyPeople = new HashMap<>();
    for (TrainCertificateEntity c : rows) {
      Long classId = c.getClassId();
      Long peopleId = c.getPeopleId();
      if (classId == null || peopleId == null) {
        continue;
      }
      anyPeople.computeIfAbsent(classId, k -> new HashSet<>()).add(peopleId);
      if (isBackfilledCertificate(c)) {
        backfillPeople.computeIfAbsent(classId, k -> new HashSet<>()).add(peopleId);
      }
    }
    for (Long classId : classIds) {
      result.put(
          classId,
          new int[] {
            backfillPeople.getOrDefault(classId, Set.of()).size(),
            anyPeople.getOrDefault(classId, Set.of()).size()
          });
    }
    return result;
  }

  private static boolean isBackfilledCertificate(TrainCertificateEntity c) {
    if (c == null) {
      return false;
    }
    if (StringUtils.hasText(c.getFilePath())) {
      return true;
    }
    String type = c.getCertificateType();
    if ("skill_level_cert".equalsIgnoreCase(type) || "renshe".equalsIgnoreCase(type)) {
      return true;
    }
    String no = c.getCertificateNo();
    return StringUtils.hasText(no) && !no.startsWith("GC-");
  }

  private List<TrainPeopleListEntity> listStudents(Long classId) {
    return peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByAsc(TrainPeopleListEntity::getId));
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    return classAccessGuard.requireAccessibleClass(classId);
  }

  private TrainPeopleListEntity requireStudent(Long classId, Long peopleId) {
    TrainPeopleListEntity student = peopleListMapper.selectOne(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getId, peopleId)
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    if (student == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "学员不存在或不属于该班级");
    }
    return student;
  }

  private PlatformUserDetailEntity loadPlatformDetail(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return null;
    }
    return platformUserDetailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idCard.trim())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .orderByDesc(PlatformUserDetailEntity::getId)
            .last("LIMIT 1"));
  }

  private TrainSignUpPeopleSelfEntity loadSignUpProfile(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return null;
    }
    return signUpPeopleSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard.trim())
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .orderByDesc(TrainSignUpPeopleSelfEntity::getId)
            .last("LIMIT 1"));
  }

  private String resolvePhoto(String primary, String fallback) {
    String resolved = idCardHeaderResolver.resolveStoredHeader(primary);
    if (StringUtils.hasText(resolved)) {
      return resolved;
    }
    return idCardHeaderResolver.resolveStoredHeader(fallback);
  }

  private String resolveGenderLabel(PlatformUserDetailEntity platformDetail, TrainSignUpPeopleSelfEntity signUp) {
    if (platformDetail != null && platformDetail.getGender() != null) {
      return platformDetail.getGender() > 0 ? "女" : "男";
    }
    if (signUp != null && StringUtils.hasText(signUp.getGender())) {
      String g = signUp.getGender().trim();
      if ("女".equals(g) || "2".equals(g)) {
        return "女";
      }
      if ("男".equals(g) || "1".equals(g)) {
        return "男";
      }
      return g;
    }
    return "";
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return "";
  }

  private Map<Long, String> loadCompanyNames(List<Long> companyIds) {
    Map<Long, String> companyNames = new HashMap<>();
    if (!companyIds.isEmpty()) {
      for (OrgEntity o : orgMapper.selectBatchIds(companyIds)) {
        companyNames.put(o.getId().longValue(), o.getName());
      }
    }
    return companyNames;
  }

  private Map<Long, String> loadWorkTypeNames(List<Long> workTypeIds) {
    Map<Long, String> workTypeNames = new HashMap<>();
    if (!workTypeIds.isEmpty()) {
      for (ExamWorkTypeEntity w : examWorkTypeMapper.selectBatchIds(workTypeIds)) {
        workTypeNames.put(w.getId().longValue(), w.getTitle());
      }
    }
    return workTypeNames;
  }

  private Map<Long, TrainInfoListEntity> loadInfoMap(List<Long> trainInfoIds) {
    Map<Long, TrainInfoListEntity> map = new HashMap<>();
    if (trainInfoIds.isEmpty()) {
      return map;
    }
    for (TrainInfoListEntity i : infoListMapper.selectBatchIds(trainInfoIds)) {
      map.put(i.getId(), i);
    }
    return map;
  }

  private static int nzStatus(Integer status) {
    return status == null ? STATUS_PENDING : status;
  }

  private static String statusLabel(int status) {
    return switch (status) {
      case STATUS_PASSED -> "已结业";
      case STATUS_FAILED -> "不合格结业";
      default -> "未结业";
    };
  }

  private static String normalizeClassStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "pending";
    }
    return status.trim();
  }

  private static String nullToDash(String v) {
    return StringUtils.hasText(v) ? v : "";
  }

  private static String scoreStr(BigDecimal v) {
    return v == null ? "" : v.stripTrailingZeros().toPlainString();
  }

  private static String trimToNull(String v) {
    if (!StringUtils.hasText(v)) {
      return null;
    }
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}

package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.CourseKejianEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.CourseKejianMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.support.TrainPeopleListSignUpFieldMapper;
import com.guoyu.platform.trainingdepartment.support.TrainingIdCardHeaderResolver;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训人员列表管理服务实现
 */
@Service
@Slf4j
public class TrainPeopleListApplicationService {

  private final TrainPeopleListMapper mapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainTeacherBankMapper teacherBankMapper;
  private final CourseKejianMapper courseKejianMapper;
  private final TrainInfoClassMapper trainInfoClassMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final TrainingIdCardHeaderResolver idCardHeaderResolver;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final PlatformUserMapper platformUserMapper;
  private final PlatformUserEnsureService platformUserEnsureService;
  private final UserMapper userMapper;
  private final TrainClassRoleApplicationService classRoleApplicationService;
  private final TrainFollowTaskApplicationService followTaskApplicationService;
  private final ClassReviewSyncApplicationService classReviewSyncApplicationService;
  private final PlatformUserApplicationService platformUserApplicationService;

  public TrainPeopleListApplicationService(TrainPeopleListMapper mapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainTeacherBankMapper teacherBankMapper,
      CourseKejianMapper courseKejianMapper,
      TrainInfoClassMapper trainInfoClassMapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainingOperatorScopeService operatorScopeService,
      TrainingIdCardHeaderResolver idCardHeaderResolver,
      PlatformUserDetailMapper platformUserDetailMapper,
      PlatformUserMapper platformUserMapper,
      PlatformUserEnsureService platformUserEnsureService,
      UserMapper userMapper,
      @Lazy TrainClassRoleApplicationService classRoleApplicationService,
      @Lazy TrainFollowTaskApplicationService followTaskApplicationService,
      @Lazy ClassReviewSyncApplicationService classReviewSyncApplicationService,
      @Lazy PlatformUserApplicationService platformUserApplicationService) {
    this.mapper = mapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.teacherBankMapper = teacherBankMapper;
    this.courseKejianMapper = courseKejianMapper;
    this.trainInfoClassMapper = trainInfoClassMapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.operatorScopeService = operatorScopeService;
    this.idCardHeaderResolver = idCardHeaderResolver;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.platformUserMapper = platformUserMapper;
    this.platformUserEnsureService = platformUserEnsureService;
    this.userMapper = userMapper;
    this.classRoleApplicationService = classRoleApplicationService;
    this.followTaskApplicationService = followTaskApplicationService;
    this.classReviewSyncApplicationService = classReviewSyncApplicationService;
    this.platformUserApplicationService = platformUserApplicationService;
  }

  // ==================== 基础CRUD ====================

  @Transactional(readOnly = true)
  public PageResponse<TrainPeopleListVO> page(TrainPeopleListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long companyFilter = operatorScopeService.resolveCompanyFilter(null);
    if (req.getTrainClassId() != null) {
      assertClassViewable(req.getTrainClassId());
    }
    LambdaQueryWrapper<TrainPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainPeopleListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainId, req.getTrainId());
    }
    if (req.getTrainClassId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainClassId, req.getTrainClassId());
    }
    if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainPeopleListEntity::getType, req.getType());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainPeopleListEntity::getRealname, req.getKeyword())
          .or()
          .like(TrainPeopleListEntity::getPhone, req.getKeyword()));
    }
    applyEnterprisePeopleScope(wrapper, companyFilter);
    IPage<TrainPeopleListEntity> page;
    if (StringUtils.hasText(req.getApplyWorkTypeName()) && req.getApplyLevel() != null) {
      page = mapper.selectCertificationImportPage(
          new Page<>(req.getPage(), req.getPageSize()),
          tenantId,
          req.getApplyWorkTypeName(),
          req.getApplyLevel(),
          StringUtils.hasText(req.getKeyword()) ? req.getKeyword() : null,
          companyFilter);
    } else {
      page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    }
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainPeopleListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainPeopleListVO create(TrainPeopleListCreateReq req) {
    assertClassViewable(req.getTrainClassId());
    if (req.getTrainClassId() == null) {
      assertTrainInfoCompanyAccessible(req.getTrainId());
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity entity = new TrainPeopleListEntity();
    entity.setTenantId(tenantId);
    entity.setTrainId(req.getTrainId());
    entity.setTrainClassId(req.getTrainClassId());
    entity.setType(req.getType());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    entity.setIdCard(req.getIdCard());
    entity.setDeleted(0);
    insertPeople(entity);
    if ("student".equals(entity.getType()) && entity.getTrainClassId() != null) {
      classReviewSyncApplicationService.syncRosterIfBound(entity.getTrainClassId());
    }
    return toVO(entity);
  }

  @Transactional
  public TrainPeopleListVO update(Long id, TrainPeopleListUpdateReq req) {
    TrainPeopleListEntity entity = getExisting(id);
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainPeopleListEntity entity = getExisting(id);
    Long classId = entity.getTrainClassId();
    boolean student = "student".equals(entity.getType());
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
    if (student && classId != null) {
      classReviewSyncApplicationService.unenrollRemovedStudents(classId, List.of(entity));
    }
  }

  @Transactional
  public void batchAudit(SignupAuditReq req) {
    throw new BusinessException(ErrorCode.BAD_REQUEST, "遗留 train_people_list 表无 status 字段，不支持批量审核");
  }

  // ==================== 班级学员 ====================

  /** 班级学员列表（type=student, by classId） */
  @Transactional(readOnly = true)
  public Map<String, Object> classStudentList(Long classId, int page, int pageSize) {
    assertClassViewable(classId);
    LambdaQueryWrapper<TrainPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .orderByDesc(TrainPeopleListEntity::getId);
    Page<TrainPeopleListEntity> mpPage = mapper.selectPage(new Page<>(page, pageSize), wrapper);

    List<String> idCards = mpPage.getRecords().stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    Map<String, String> headerFromDetail = new HashMap<>();
    Map<String, String> headerFromSignUp = new HashMap<>();
    Map<String, String> nameFromDetail = new HashMap<>();
    Map<String, String> nameFromSignUp = new HashMap<>();
    Map<Long, String> nameFromPlatformUser = new HashMap<>();
    if (!idCards.isEmpty()) {
      platformUserDetailMapper.selectList(
          new LambdaQueryWrapper<PlatformUserDetailEntity>()
              .in(PlatformUserDetailEntity::getIdcard, idCards)
              .eq(PlatformUserDetailEntity::getDeleted, 0))
          .forEach(p -> {
            if (StringUtils.hasText(p.getIdcard()) && StringUtils.hasText(p.getIdcardHeader())) {
              headerFromDetail.put(p.getIdcard(), p.getIdcardHeader());
            }
            if (StringUtils.hasText(p.getIdcard()) && StringUtils.hasText(p.getRealname())
                && !nameFromDetail.containsKey(p.getIdcard())) {
              nameFromDetail.put(p.getIdcard(), p.getRealname().trim());
            }
          });
      // 仅取头像/姓名字段；勿 SELECT *（level 为 varchar 混中文，整行映射曾导致 Integer 反序列化 500）
      signUpPeopleSelfMapper.selectList(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .select(
                  TrainSignUpPeopleSelfEntity::getIdCardNumber,
                  TrainSignUpPeopleSelfEntity::getIdCardHeader,
                  TrainSignUpPeopleSelfEntity::getName)
              .in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
              .orderByDesc(TrainSignUpPeopleSelfEntity::getId))
          .forEach(p -> {
            if (StringUtils.hasText(p.getIdCardNumber())
                && StringUtils.hasText(p.getIdCardHeader())
                && !headerFromSignUp.containsKey(p.getIdCardNumber())) {
              headerFromSignUp.put(p.getIdCardNumber(), p.getIdCardHeader());
            }
            if (StringUtils.hasText(p.getIdCardNumber())
                && StringUtils.hasText(p.getName())
                && !nameFromSignUp.containsKey(p.getIdCardNumber())) {
              nameFromSignUp.put(p.getIdCardNumber(), p.getName().trim());
            }
          });
    }
    List<Long> platformUserIds = mpPage.getRecords().stream()
        .map(TrainPeopleListEntity::getPlatformUserId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (!platformUserIds.isEmpty()) {
      platformUserMapper.selectList(
          new LambdaQueryWrapper<PlatformUserEntity>()
              .in(PlatformUserEntity::getId, platformUserIds)
              .eq(PlatformUserEntity::getDeleted, 0))
          .forEach(u -> {
            if (u.getId() != null && StringUtils.hasText(u.getRealname())) {
              nameFromPlatformUser.put(u.getId(), u.getRealname().trim());
            }
          });
    }

    List<Map<String, Object>> list = mpPage.getRecords().stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      String idCard = e.getIdCard();
      // 花名册 realname 历史可能为空：与 PHP 开班导出一致，回退 detail / 账号 / 报名姓名
      String displayName = e.getRealname();
      if (!StringUtils.hasText(displayName) && StringUtils.hasText(idCard)) {
        displayName = nameFromDetail.get(idCard);
      }
      if (!StringUtils.hasText(displayName) && e.getPlatformUserId() != null) {
        displayName = nameFromPlatformUser.get(e.getPlatformUserId());
      }
      if (!StringUtils.hasText(displayName) && StringUtils.hasText(idCard)) {
        displayName = nameFromSignUp.get(idCard);
      }
      item.put("realname", displayName);
      item.put("phone", e.getPhone());
      item.put("train_id", e.getTrainId());
      item.put("train_class_id", e.getTrainClassId());
      item.put("id_card", e.getIdCard());
      item.put("is_leader", e.getIsLeader());
      item.put("is_leader_cn", e.getIsLeader() != null && e.getIsLeader() == 1 ? "是" : "否");
      item.put("is_committee", e.getIsCommittee());
      item.put("is_committee_cn", e.getIsCommittee() != null && e.getIsCommittee() == 1 ? "是" : "否");
      item.put("exam_score", e.getExamScore());
      item.put("real_score", e.getRealScore());
      item.put("normal_score", e.getNormalScore());
      item.put("header", idCardHeaderResolver.resolveForStudent(
          idCard,
          idCard != null ? headerFromDetail.get(idCard) : null,
          idCard != null ? headerFromSignUp.get(idCard) : null));
      return item;
    }).toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    result.put("limit", pageSize);
    return result;
  }

  /** 编辑学员信息 */
  @Transactional
  public Map<String, Object> saveStudent(Map<String, Object> params) {
    operatorScopeService.requireOrgOperator();
    Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : null;
    if (id == null || id <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持编辑已有学员");
    }
    TrainPeopleListEntity entity = getExisting(id);
    assertClassViewable(entity.getTrainClassId());
    if (params.get("realname") != null) entity.setRealname(params.get("realname").toString());
    if (params.get("phone") != null) entity.setPhone(params.get("phone").toString());
    if (params.get("id_card") != null) entity.setIdCard(params.get("id_card").toString());
    if (params.get("normal_score") != null) entity.setNormalScore(new java.math.BigDecimal(params.get("normal_score").toString()));
    if (params.get("is_leader") != null) entity.setIsLeader(Integer.valueOf(params.get("is_leader").toString()));
    if (params.get("is_committee") != null) entity.setIsCommittee(Integer.valueOf(params.get("is_committee").toString()));
    mapper.updateById(entity);
    if (params.get("is_leader") != null) {
      classRoleApplicationService.syncMonitorFromStudent(entity);
    }
    platformUserApplicationService.syncFromExternalProfile(
        entity.getPlatformUserId(), entity.getIdCard(), entity.getRealname(), entity.getPhone());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", entity.getId());
    result.put("realname", entity.getRealname());
    return result;
  }

  /** 批量删除学员 */
  @Transactional
  public Map<String, Object> batchDeleteStudents(List<Long> ids) {
    operatorScopeService.requireOrgOperator();
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要删除的学员");
    }
    int count = 0;
    java.util.Set<Long> classIds = new java.util.HashSet<>();
    Map<Long, List<TrainPeopleListEntity>> removedByClass = new LinkedHashMap<>();
    for (Long id : ids) {
      TrainPeopleListEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        assertClassViewable(entity.getTrainClassId());
        if (entity.getTrainClassId() != null) {
          classIds.add(entity.getTrainClassId());
          if ("student".equals(entity.getType())) {
            removedByClass.computeIfAbsent(entity.getTrainClassId(), k -> new ArrayList<>()).add(entity);
          }
        }
        if (entity.getIsLeader() != null && entity.getIsLeader() == 1) {
          entity.setIsLeader(0);
          classRoleApplicationService.syncMonitorFromStudent(entity);
        }
        entity.setDeleted(1);
        mapper.deleteById(entity.getId());
        count++;
      }
    }
    for (Long classId : classIds) {
      syncClassStudentNums(classId);
    }
    for (Map.Entry<Long, List<TrainPeopleListEntity>> entry : removedByClass.entrySet()) {
      classReviewSyncApplicationService.unenrollRemovedStudents(entry.getKey(), entry.getValue());
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    return result;
  }

  /** 可导入的报名人员列表 */
  @Transactional(readOnly = true)
  public Map<String, Object> signUpSelfList(Long traininfoId, String keyword, Long classId) {
    TrainSignUpSelfEntity signUp = resolveSignUpForImport(traininfoId, classId);
    if (signUp == null) {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("lists", List.of());
      return result;
    }
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(TrainSignUpPeopleSelfEntity::getName, keyword)
          .or()
          .like(TrainSignUpPeopleSelfEntity::getPhone, keyword));
    }
    List<TrainSignUpPeopleSelfEntity> people = signUpPeopleSelfMapper.selectList(wrapper);
    List<Map<String, Object>> lists = people.stream().map(p -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", p.getId());
      item.put("name", p.getName());
      item.put("phone", p.getPhone());
      return item;
    }).toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("lists", lists);
    result.put("signUpId", signUp.getId());
    result.put("collectionSource", signUp.getCollectionSource());
    return result;
  }

  private TrainSignUpSelfEntity resolveSignUpForImport(Long traininfoId, Long classId) {
    if (classId != null) {
      TrainSignUpSelfEntity byClass = signUpSelfMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpSelfEntity>()
              .eq(TrainSignUpSelfEntity::getTrainClassId, classId)
              .eq(TrainSignUpSelfEntity::getCollectionSource, ClassCollectionApplicationService.SOURCE_CLASS_PREP)
              .eq(TrainSignUpSelfEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (byClass != null) {
        return byClass;
      }
    }
    if (traininfoId == null) {
      return null;
    }
    return signUpSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTrainInfoId, traininfoId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0)
            .and(w -> w
                .eq(TrainSignUpSelfEntity::getCollectionSource, "train_info")
                .or()
                .isNull(TrainSignUpSelfEntity::getCollectionSource))
            .last("LIMIT 1"));
  }

  /** 从报名人员导入学员（同步开 App / 学员管理账号） */
  @Transactional
  public Map<String, Object> importStudents(Long traininfoId, Long classId, List<Long> studentIds) {
    operatorScopeService.requireOrgOperator();
    assertClassViewable(classId);
    Long companyId = resolveCompanyId(traininfoId, classId);
    Long orgId = resolveImportPlatformOrgId();
    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();

    List<TrainSignUpPeopleSelfEntity> signUpPeople = signUpPeopleSelfMapper.selectBatchIds(studentIds);
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (Long sid : studentIds) {
      TrainSignUpPeopleSelfEntity sp = signUpPeople.stream()
          .filter(p -> p.getId().equals(sid)).findFirst().orElse(null);
      if (sp == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", sid);
        f.put("reason", "报名人员不存在");
        failed.add(f);
        continue;
      }
      ImportOneResult one = importOneFromSignUp(traininfoId, classId, sp, companyId, orgId, syncStats);
      if (one.ok()) {
        count++;
      } else {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", sid);
        f.put("name", sp.getName());
        f.put("reason", one.failReason());
        failed.add(f);
      }
    }
    syncClassStudentNums(classId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    result.put("platformUserSync", syncStats.toMap());
    return result;
  }

  /** 按身份证号导入学员（同步开 App / 学员管理账号） */
  @Transactional
  public Map<String, Object> importStudentsByIdCard(Long traininfoId, Long classId, String idCardsText) {
    operatorScopeService.requireOrgOperator();
    assertClassViewable(classId);
    TrainSignUpSelfEntity signUp = resolveSignUpForImport(traininfoId, classId);
    if (signUp == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录未找到");
    }
    Long companyId = resolveCompanyId(traininfoId, classId);
    Long orgId = resolveImportPlatformOrgId();
    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();

    String[] lines = idCardsText.split("[\\n,，\\s]+");
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (String line : lines) {
      String idCard = line.trim();
      if (idCard.isEmpty()) continue;
      if (idCard.length() != 18) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("reason", "身份证号格式错误");
        failed.add(f);
        continue;
      }
      TrainSignUpPeopleSelfEntity sp = signUpPeopleSelfMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard));
      if (sp == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("reason", "未在报名记录中找到该身份证号");
        failed.add(f);
        continue;
      }
      ImportOneResult one = importOneFromSignUp(traininfoId, classId, sp, companyId, orgId, syncStats);
      if (one.ok()) {
        count++;
      } else {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("name", sp.getName());
        f.put("reason", one.failReason());
        failed.add(f);
      }
    }
    syncClassStudentNums(classId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    result.put("platformUserSync", syncStats.toMap());
    return result;
  }

  /**
   * 班级维度补开 / 对齐 App 账号：platform_user_id 为空或 SaaS 高位孤儿。
   */
  @Transactional
  public Map<String, Object> syncAppAccountsForClass(Long classId) {
    operatorScopeService.requireOrgOperator();
    assertClassViewable(classId);
    TrainInfoClassEntity clazz = trainInfoClassMapper.selectById(classId);
    if (clazz == null || (clazz.getDeleted() != null && clazz.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    Long companyId = resolveCompanyId(clazz.getTrainInfoId(), classId);
    Long orgId = resolveImportPlatformOrgId();

    List<TrainPeopleListEntity> people = mapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .isNull(TrainPeopleListEntity::getPlatformUserId));

    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();
    int linked = 0;
    for (TrainPeopleListEntity row : people) {
      PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
          row.getRealname(),
          row.getIdCard(),
          row.getPhone(),
          companyId,
          orgId);
      syncStats.accept(ensure, row.getRealname(), PlatformUserEnsureService.normalizeIdCard(row.getIdCard()));
      if (ensure.platformUserId() != null) {
        Long oldId = row.getPlatformUserId();
        if (oldId == null || !oldId.equals(ensure.platformUserId())) {
          row.setPlatformUserId(ensure.platformUserId());
          mapper.updateById(row);
          linked++;
        }
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("candidates", people.size());
    result.put("linked", linked);
    result.put("platformUserSync", syncStats.toMap());
    classReviewSyncApplicationService.syncRosterIfBound(classId);
    return result;
  }

  private record ImportOneResult(boolean ok, String failReason) {
    static ImportOneResult success() {
      return new ImportOneResult(true, null);
    }

    static ImportOneResult fail(String reason) {
      return new ImportOneResult(false, reason);
    }
  }

  /**
   * 开账号 → 写花名册 → 回写报名 platformUserId。
   * 与分班确认共用 {@link PlatformUserEnsureService#ensureFromDivisionPerson}。
   */
  private ImportOneResult importOneFromSignUp(
      Long traininfoId,
      Long classId,
      TrainSignUpPeopleSelfEntity sp,
      Long companyId,
      Long orgId,
      PlatformUserEnsureService.SyncStats syncStats) {
    PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
        sp.getName(),
        sp.getIdCardNumber(),
        sp.getPhone(),
        companyId,
        orgId);
    syncStats.accept(ensure, sp.getName(), PlatformUserEnsureService.normalizeIdCard(sp.getIdCardNumber()));

    if (ensure.platformUserId() != null
        && (sp.getPlatformUserId() == null || !ensure.platformUserId().equals(sp.getPlatformUserId()))) {
      // 回写报名人员，便于下次导入直接带上账号
      sp.setPlatformUserId(ensure.platformUserId());
      signUpPeopleSelfMapper.updateById(sp);
    }

    String idCard = PlatformUserEnsureService.normalizeIdCard(sp.getIdCardNumber());
    LambdaQueryWrapper<TrainPeopleListEntity> existQ = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .eq(TrainPeopleListEntity::getTrainId, traininfoId)
        .eq(TrainPeopleListEntity::getTrainClassId, classId);
    if (StringUtils.hasText(idCard)) {
      existQ.eq(TrainPeopleListEntity::getIdCard, idCard);
    } else if (ensure.platformUserId() != null) {
      existQ.eq(TrainPeopleListEntity::getPlatformUserId, ensure.platformUserId());
    } else if (sp.getPlatformUserId() != null) {
      existQ.eq(TrainPeopleListEntity::getPlatformUserId, sp.getPlatformUserId());
    } else {
      existQ.eq(TrainPeopleListEntity::getPhone, sp.getPhone());
    }
    Long existCount = mapper.selectCount(existQ);
    if (existCount != null && existCount > 0) {
      return ImportOneResult.fail("已存在该学员");
    }

    TrainPeopleListEntity entity = buildStudentFromSignUp(traininfoId, classId, sp);
    if (ensure.platformUserId() != null) {
      entity.setPlatformUserId(ensure.platformUserId());
    }
    insertPeople(entity);
    return ImportOneResult.success();
  }

  private Long resolveCompanyId(Long traininfoId, Long classId) {
    if (traininfoId != null) {
      TrainInfoListEntity ti = trainInfoListMapper.selectById(traininfoId);
      if (ti != null && ti.getCompanyId() != null && ti.getCompanyId() > 0) {
        return ti.getCompanyId();
      }
    }
    if (classId != null) {
      TrainInfoClassEntity clazz = trainInfoClassMapper.selectById(classId);
      if (clazz != null && clazz.getCompanyId() != null && clazz.getCompanyId() > 0) {
        return clazz.getCompanyId();
      }
    }
    return null;
  }

  /** 与学员管理列表过滤同源：优先当前操作者所属培训机构 orgId */
  private Long resolveImportPlatformOrgId() {
    TrainingOperatorScopeService.Scope scope = operatorScopeService.currentScope();
    if (scope.isOrgOperator() && scope.orgId() != null && scope.orgId() > 0) {
      return scope.orgId();
    }
    UserEntity user = userMapper.selectById(SecurityContext.currentUser().getUserId());
    if (user != null && user.getOrgId() != null && user.getOrgId() > 0) {
      return user.getOrgId();
    }
    return SecurityContext.currentUser().getTenantId() != null
        ? SecurityContext.currentUser().getTenantId()
        : 0L;
  }

  private TrainPeopleListEntity buildStudentFromSignUp(
      Long traininfoId, Long classId, TrainSignUpPeopleSelfEntity signUp) {
    TrainPeopleListEntity entity = new TrainPeopleListEntity();
    entity.setType("student");
    entity.setTrainId(traininfoId);
    entity.setTrainClassId(classId);
    entity.setRealname(signUp.getName());
    entity.setPhone(signUp.getPhone());
    entity.setIdCard(signUp.getIdCardNumber());
    entity.setPlatformUserId(signUp.getPlatformUserId());
    entity.setDeleted(0);
    TrainPeopleListSignUpFieldMapper.applySignUpFields(entity, signUp);
    return entity;
  }

  // ==================== 教务/负责人 ====================

  /** 查询教务/负责人列表 */
  @Transactional(readOnly = true)
  public Map<String, Object> classOtherList(Long trainInfoId, Long classId, String tag) {
    assertClassViewable(classId);
    String type = tag.equals("teacher") ? "staff" : "inCharge";
    List<TrainPeopleListEntity> records = mapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .eq(TrainPeopleListEntity::getType, type)
            .eq(TrainPeopleListEntity::getTrainId, trainInfoId)
            .eq(TrainPeopleListEntity::getTrainClassId, classId));
    List<Map<String, Object>> list = records.stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("train_id", e.getTrainId());
      item.put("train_class_id", e.getTrainClassId());
      item.put("realname", e.getRealname());
      item.put("phone", e.getPhone());
      item.put("id_card", e.getIdCard());
      return item;
    }).toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    return result;
  }

  /** 新增/编辑教务/负责人 */
  @Transactional
  public Map<String, Object> saveOther(Map<String, Object> params) {
    operatorScopeService.requireOrgOperator();
    Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : 0L;
    String type = params.get("type") != null ? params.get("type").toString() : null;
    if (id > 0) {
      TrainPeopleListEntity entity = getExisting(id);
      assertClassViewable(entity.getTrainClassId());
      if (params.get("realname") != null) entity.setRealname(params.get("realname").toString());
      if (params.get("phone") != null) entity.setPhone(params.get("phone").toString());
      if (params.get("id_card") != null) entity.setIdCard(params.get("id_card").toString());
      mapper.updateById(entity);
      if ("staff".equals(entity.getType()) || "inCharge".equals(entity.getType())) {
        classRoleApplicationService.syncFromLegacyPeople(entity);
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      return result;
    } else {
      Long classId = params.get("classid") != null ? Long.valueOf(params.get("classid").toString()) : null;
      assertClassViewable(classId);
      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType(type);
      entity.setTrainId(params.get("train_id") != null ? Long.valueOf(params.get("train_id").toString()) : null);
      entity.setTrainClassId(classId);
      entity.setRealname(params.get("realname") != null ? params.get("realname").toString() : null);
      entity.setPhone(params.get("phone") != null ? params.get("phone").toString() : null);
      entity.setIdCard(params.get("id_card") != null ? params.get("id_card").toString() : null);
      entity.setDeleted(0);
      insertPeople(entity);
      if ("staff".equals(entity.getType()) || "inCharge".equals(entity.getType())) {
        classRoleApplicationService.syncFromLegacyPeople(entity);
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      return result;
    }
  }

  /** 批量删除教务/负责人 */
  @Transactional
  public Map<String, Object> batchDeleteOthers(List<Long> ids) {
    operatorScopeService.requireOrgOperator();
    int count = 0;
    for (Long id : ids) {
      TrainPeopleListEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        assertClassViewable(entity.getTrainClassId());
        classRoleApplicationService.endFromLegacyPeople(entity);
        entity.setDeleted(1);
        mapper.deleteById(entity.getId());
        count++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    return result;
  }

  /** 可导入的教务数据源（教师库） */
  @Transactional(readOnly = true)
  public Map<String, Object> otherImportData(String tag) {
    List<Map<String, Object>> list = List.of();
    if ("teacher".equals(tag)) {
      list = teacherBankMapper.selectList(
              new LambdaQueryWrapper<TrainTeacherBankEntity>()
                  .eq(TrainTeacherBankEntity::getDeleted, 0))
          .stream().map(t -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("name", t.getName());
            item.put("phone", t.getPhone());
            return item;
          }).toList();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    return result;
  }

  /** 从教师库导入教务人员 */
  @Transactional
  public Map<String, Object> importOthers(Long traininfoId, Long classId, List<Long> ids) {
    operatorScopeService.requireOrgOperator();
    assertClassViewable(classId);
    List<TrainTeacherBankEntity> teachers = teacherBankMapper.selectBatchIds(ids);
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (Long tid : ids) {
      TrainTeacherBankEntity t = teachers.stream()
          .filter(x -> x.getId().equals(tid)).findFirst().orElse(null);
      if (t == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", tid);
        f.put("reason", "教师不存在");
        failed.add(f);
        continue;
      }
      Long existCount = mapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
          .eq(TrainPeopleListEntity::getType, "staff")
          .eq(TrainPeopleListEntity::getDeleted, 0)
          .eq(TrainPeopleListEntity::getTrainId, traininfoId)
          .eq(TrainPeopleListEntity::getTrainClassId, classId)
          .eq(TrainPeopleListEntity::getUserId, t.getId()));
      if (existCount != null && existCount > 0) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", tid);
        f.put("reason", "已存在该教务人员");
        failed.add(f);
        continue;
      }

      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType("staff");
      entity.setTrainId(traininfoId);
      entity.setTrainClassId(classId);
      entity.setUserId(t.getId());
      entity.setRealname(t.getName());
      entity.setPhone(t.getPhone());
      entity.setIdCard(t.getIdCardNumber());
      entity.setDeleted(0);
      insertPeople(entity);
      classRoleApplicationService.syncFromLegacyPeople(entity);
      count++;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    return result;
  }

  private void insertPeople(TrainPeopleListEntity entity) {
    if (entity.getTenantId() == null) {
      entity.setTenantId(SecurityContext.currentUser().getTenantId());
    }
    if (entity.getCreateTime() == null) {
      entity.setCreateTime(LocalDateTime.now());
    }
    mapper.insert(entity);
    if ("student".equals(entity.getType())
        && entity.getTrainClassId() != null
        && entity.getId() != null) {
      try {
        followTaskApplicationService.issuePendingItemsForNewStudent(
            entity.getTrainClassId(), entity.getId());
      } catch (Exception ex) {
        log.warn("插班补发作业失败 classId={} peopleId={}: {}",
            entity.getTrainClassId(), entity.getId(), ex.getMessage());
      }
    }
  }

  /** 将班级 nums 同步为花名册学员实时人数，供开班申报材料等使用 */
  private void syncClassStudentNums(Long classId) {
    if (classId == null) {
      return;
    }
    Long count = mapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    TrainInfoClassEntity clazz = trainInfoClassMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      return;
    }
    int nums = count == null ? 0 : count.intValue();
    if (clazz.getNums() == null || clazz.getNums() != nums) {
      clazz.setNums(nums);
      trainInfoClassMapper.updateById(clazz);
    }
    classReviewSyncApplicationService.syncRosterIfBound(classId);
  }

  private void assertClassViewable(Long classId) {
    if (classId == null) {
      return;
    }
    TrainInfoClassEntity clazz = trainInfoClassMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(clazz);
  }

  /** 企业账号：花名册仅可见本企业班级（或无班级时本企业培训信息下）人员 */
  private void applyEnterprisePeopleScope(
      LambdaQueryWrapper<TrainPeopleListEntity> wrapper, Long companyFilter) {
    if (companyFilter == null) {
      return;
    }
    wrapper.and(w -> w
        .apply(
            "train_class_id IN (SELECT id FROM train_info_class WHERE company_id = {0} AND deleted = 0)",
            companyFilter)
        .or(w2 -> w2.isNull(TrainPeopleListEntity::getTrainClassId)
            .apply(
                "(train_id IN (SELECT id FROM train_info_list WHERE company_id = {0} AND deleted = 0))",
                companyFilter)));
  }

  private void assertTrainInfoCompanyAccessible(Long trainInfoId) {
    if (trainInfoId == null || !operatorScopeService.isEnterpriseScoped()) {
      return;
    }
    TrainInfoListEntity info = trainInfoListMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getId, trainInfoId)
            .eq(TrainInfoListEntity::getDeleted, 0));
    if (info == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训信息不存在");
    }
    operatorScopeService.assertCompanyAccessible(info.getCompanyId());
  }

  private TrainPeopleListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getId, id)
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    assertClassViewable(entity.getTrainClassId());
    if (entity.getTrainClassId() == null) {
      assertTrainInfoCompanyAccessible(entity.getTrainId());
    }
    return entity;
  }

  private String resolveHeaderFromStored(String stored) {
    return idCardHeaderResolver.resolveStoredHeader(stored);
  }

  private TrainPeopleListVO toVO(TrainPeopleListEntity entity) {
    return new TrainPeopleListVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getTrainClassId(),
        entity.getType(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getRealname(),
        entity.getPhone(),
        entity.getIsLeader(),
        entity.getIsCommittee(),
        entity.getExamScore(),
        entity.getRealScore(),
        entity.getPaper(),
        entity.getNormalScore(),
        entity.getNationalPaper(),
        entity.getCurrentLevel(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getIdCard()
    );
  }
}

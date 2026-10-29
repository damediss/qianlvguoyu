package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ParsedRow;
import com.guoyu.platform.trainingdepartment.vo.SignUpPeopleImportResultVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 开班准备资料收集 · 学徒制花名册导入 */
@Service
@Slf4j
public class ClassPrepRosterImportService {

  static final String DATA_SOURCE = "class_roster_xlsx";

  private final ClassPrepRosterParseService parseService;
  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final TrainInfoClassMapper classMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final PlatformUserMapper platformUserMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final PlatformUserEnsureService platformUserEnsureService;
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private final TrainingOperatorScopeService operatorScopeService;

  public ClassPrepRosterImportService(
      ClassPrepRosterParseService parseService,
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      TrainInfoClassMapper classMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      PlatformUserMapper platformUserMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      PlatformUserEnsureService platformUserEnsureService,
      ClassCollectionApplicationService classCollectionApplicationService,
      TrainingOperatorScopeService operatorScopeService) {
    this.parseService = parseService;
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.classMapper = classMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.platformUserMapper = platformUserMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.platformUserEnsureService = platformUserEnsureService;
    this.classCollectionApplicationService = classCollectionApplicationService;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional
  public SignUpPeopleImportResultVO importExcel(Long signUpId, MultipartFile file) {
    if (signUpId == null || signUpId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参数不完整");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不合法");
    }
    String originalName = file.getOriginalFilename();
    if (!StringUtils.hasText(originalName)
        || (!originalName.toLowerCase(Locale.ROOT).endsWith(".xlsx")
            && !originalName.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能上传 xlsx/xls 文件");
    }

    operatorScopeService.requireTrainingOperator();
    TrainSignUpSelfEntity signUp = getClassPrepSignUp(signUpId);
    TrainInfoClassEntity clazz = getClassAccessible(signUp.getTrainClassId());

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
    }
    var roster = parseService.parse(bytes);

    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long platformOrgId = resolvePlatformOrgId();
    Long companyId = clazz.getCompanyId() != null ? clazz.getCompanyId() : signUp.getOrgId();
    String companyName = resolveCompanyName(companyId);
    ClassWorkType classWorkType = resolveClassWorkType(clazz);

    int inserted = 0;
    int skipped = 0;
    int failed = 0;
    List<String> errors = new ArrayList<>();
    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();

    Set<String> seenCards = new HashSet<>();
    LocalDateTime now = LocalDateTime.now();

    for (ParsedRow row : roster.rows()) {
      try {
        String idCard = ClassPrepRosterParseService.normalizeIdCard(row.idCard());
        if (idCard == null) {
          idCard = ClassPrepRosterParseService.normalizeIdCard(row.idCardRaw());
        }
        String phone = ClassPrepRosterParseService.normalizePhone(row.phone());
        if (phone == null) {
          phone = ClassPrepRosterParseService.normalizePhone(row.phoneRaw());
        }

        if (!StringUtils.hasText(row.name())) {
          failed++;
          errors.add("第" + row.rowNo() + "行：缺少姓名");
          continue;
        }
        if (!StringUtils.hasText(idCard) || idCard.length() != 18) {
          failed++;
          errors.add("第" + row.rowNo() + "行：" + safeName(row.name()) + " 身份证号不是18位");
          continue;
        }
        if (!StringUtils.hasText(phone) || phone.length() != 11) {
          failed++;
          errors.add("第" + row.rowNo() + "行：" + safeName(row.name()) + " 手机号不是11位");
          continue;
        }
        if (!seenCards.add(idCard)) {
          failed++;
          errors.add("第" + row.rowNo() + "行：" + safeName(row.name()) + " 表内身份证重复");
          continue;
        }

        PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
            row.name(), idCard, phone, companyId, platformOrgId);
        syncStats.accept(ensure, row.name(), idCard);
        if (StringUtils.hasText(ensure.warning())) {
          errors.add("第" + row.rowNo() + "行 " + safeName(row.name()) + ": " + ensure.warning());
        }

        TrainSignUpPeopleSelfEntity exists = peopleMapper.selectOne(
            new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
                .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
                .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard)
                .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
                .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
                .last("LIMIT 1"));

        if (exists != null) {
          boolean changed = fillEmptyFromRoster(exists, row, idCard, companyId, companyName);
          if (ensure.platformUserId() != null
              && (exists.getPlatformUserId() == null
                  || !ensure.platformUserId().equals(exists.getPlatformUserId()))) {
            exists.setPlatformUserId(ensure.platformUserId());
            changed = true;
          }
          if (changed) {
            exists.setUpdateTime(now);
            peopleMapper.updateById(exists);
          }
          enrichDetailFromRoster(ensure.platformUserId(), row.name(), idCard, phone, row);
          classCollectionApplicationService.syncPersonToRoster(exists);
          skipped++;
          continue;
        }

        if (ensure.outcome() == PlatformUserEnsureService.Outcome.SKIPPED
            || ensure.platformUserId() == null) {
          failed++;
          if (!StringUtils.hasText(ensure.warning())) {
            errors.add("第" + row.rowNo() + "行 " + safeName(row.name()) + ": 未能开 App 账号，未写入名单");
          }
          continue;
        }

        TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
        entity.setSignUpId(signUpId);
        entity.setTenantId(tenantId);
        entity.setPlatformUserId(ensure.platformUserId());
        entity.setName(row.name());
        entity.setPhone(phone);
        entity.setIdCardNumber(idCard);
        entity.setAge(calcAgeFromIdCard(idCard));
        entity.setGender(firstNonBlank(row.gender(), genderLabelFromIdCard(idCard)));
        entity.setNation(row.nation());
        entity.setPolitical(row.political());
        entity.setAddress(row.address());
        entity.setEducation(row.education());
        entity.setSocialSecurity(row.socialSecurity());
        entity.setWorkDateStart(row.workDateStart());
        entity.setWorkDateEnd(row.workDateEnd());
        entity.setWorkTime(row.workTime());
        entity.setComments(joinComments(row.personCategory(), row.remark()));
        entity.setCompanyId(companyId);
        entity.setCompanyName(companyName);
        entity.setReqWorkTypeId(classWorkType.workTypeId());
        entity.setReqWorkTypeName(classWorkType.workTypeName());
        entity.setReqLevel(classWorkType.reqLevel());
        entity.setStatus(ClassCollectionApplicationService.STATUS_PENDING);
        entity.setCompanyStatus(0);
        entity.setHasPay(0);
        entity.setSchoolPaperHas(0);
        entity.setWorkTypePaperHas(0);
        entity.setDataSource(DATA_SOURCE);
        entity.setDeleted(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        if (ensure.platformUserId() != null) {
          PlatformUserEntity platformUser = platformUserMapper.selectById(ensure.platformUserId());
          if (platformUser != null) {
            if (StringUtils.hasText(platformUser.getRealname())) {
              entity.setName(platformUser.getRealname());
            }
            if (StringUtils.hasText(platformUser.getPhone())) {
              entity.setPhone(platformUser.getPhone());
            }
          }
          enrichDetailFromRoster(ensure.platformUserId(), row.name(), idCard, phone, row);
        }

        peopleMapper.insert(entity);
        classCollectionApplicationService.syncPersonToRoster(entity);
        inserted++;
      } catch (Exception ex) {
        log.warn("class prep roster row failed rowNo={}", row.rowNo(), ex);
        failed++;
        errors.add("第" + row.rowNo() + "行：" + safeName(row.name()) + " " + rootMessage(ex));
      }
    }

    return new SignUpPeopleImportResultVO(inserted, skipped, failed, null, errors, syncStats.toMap());
  }

  private TrainSignUpSelfEntity getClassPrepSignUp(Long signUpId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = signUpMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, signUpId)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "资料收集计划不存在");
    }
    if (!classCollectionApplicationService.isClassPrep(entity) || entity.getTrainClassId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持开班准备资料收集导入花名册");
    }
    if (entity.getOrgId() != null) {
      operatorScopeService.assertCompanyAccessible(entity.getOrgId());
    }
    return entity;
  }

  private TrainInfoClassEntity getClassAccessible(Long classId) {
    TrainInfoClassEntity entity = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(entity);
    return entity;
  }

  private ClassWorkType resolveClassWorkType(TrainInfoClassEntity clazz) {
    Long workTypeId = clazz.getWorkTypeId();
    String workTypeName = null;
    if (workTypeId != null && workTypeId > 0) {
      ExamWorkTypeEntity type = examWorkTypeMapper.selectById(workTypeId.intValue());
      if (type != null && StringUtils.hasText(type.getTitle())) {
        workTypeName = type.getTitle();
      }
    }
    Integer reqLevel = classLevelToPersonReqLevel(clazz.getWorkTypeLevel());
    return new ClassWorkType(workTypeId, workTypeName, reqLevel);
  }

  static Integer classLevelToPersonReqLevel(Integer classLevel) {
    if (classLevel == null || classLevel < 1 || classLevel > 5) {
      return null;
    }
    return 5 - classLevel;
  }

  private String resolveCompanyName(Long companyId) {
    if (companyId == null || companyId <= 0) {
      return null;
    }
    OrgEntity org = orgMapper.selectById(companyId);
    return org != null && StringUtils.hasText(org.getName()) ? org.getName() : null;
  }

  private Long resolvePlatformOrgId() {
    UserEntity user = userMapper.selectById(SecurityContext.currentUser().getUserId());
    if (user != null && user.getOrgId() != null) {
      return user.getOrgId();
    }
    return 0L;
  }

  private boolean fillEmptyFromRoster(
      TrainSignUpPeopleSelfEntity exists,
      ParsedRow row,
      String idCard,
      Long companyId,
      String companyName) {
    boolean changed = false;
    changed |= fillIfBlank(exists.getGender(), row.gender(), exists::setGender);
    if (!StringUtils.hasText(exists.getGender())) {
      changed |= fillIfBlank(exists.getGender(), genderLabelFromIdCard(idCard), exists::setGender);
    }
    changed |= fillIfBlank(exists.getNation(), row.nation(), exists::setNation);
    changed |= fillIfBlank(exists.getEducation(), row.education(), exists::setEducation);
    changed |= fillIfBlank(exists.getPolitical(), row.political(), exists::setPolitical);
    changed |= fillIfBlank(exists.getAddress(), row.address(), exists::setAddress);
    changed |= fillIfBlank(exists.getSocialSecurity(), row.socialSecurity(), exists::setSocialSecurity);
    changed |= fillIfBlank(exists.getWorkDateStart(), row.workDateStart(), exists::setWorkDateStart);
    changed |= fillIfBlank(exists.getWorkDateEnd(), row.workDateEnd(), exists::setWorkDateEnd);
    changed |= fillIfBlank(exists.getWorkTime(), row.workTime(), exists::setWorkTime);
    changed |= fillIfBlank(exists.getComments(), joinComments(row.personCategory(), row.remark()), exists::setComments);
    if (exists.getCompanyId() == null && companyId != null) {
      exists.setCompanyId(companyId);
      changed = true;
    }
    changed |= fillIfBlank(exists.getCompanyName(), companyName, exists::setCompanyName);
    if (exists.getAge() == null) {
      Integer age = calcAgeFromIdCard(idCard);
      if (age != null) {
        exists.setAge(age);
        changed = true;
      }
    }
    return changed;
  }

  private void enrichDetailFromRoster(
      Long userId, String realname, String idCard, String phone, ParsedRow row) {
    if (userId == null || !StringUtils.hasText(idCard) || idCard.length() < 14) {
      return;
    }
    PlatformUserDetailEntity exists = platformUserDetailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idCard)
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (exists == null) {
      PlatformUserDetailEntity detail = new PlatformUserDetailEntity();
      detail.setUserId(userId);
      detail.setRealname(realname);
      detail.setPhone(phone);
      detail.setIdcard(idCard);
      detail.setBirthday(idCard.substring(6, 14));
      detail.setGender(genderFromIdCard(idCard));
      detail.setNation(row.nation());
      detail.setAddress(row.address());
      detail.setPolitical(row.political());
      detail.setEducation(row.education());
      detail.setDeleted(0);
      platformUserDetailMapper.insert(detail);
      return;
    }
    boolean changed = false;
    if (exists.getUserId() == null) {
      exists.setUserId(userId);
      changed = true;
    }
    changed |= fillIfBlank(exists.getNation(), row.nation(), exists::setNation);
    changed |= fillIfBlank(exists.getAddress(), row.address(), exists::setAddress);
    changed |= fillIfBlank(exists.getPolitical(), row.political(), exists::setPolitical);
    changed |= fillIfBlank(exists.getEducation(), row.education(), exists::setEducation);
    if (changed) {
      platformUserDetailMapper.updateById(exists);
    }
  }

  private static boolean fillIfBlank(String current, String incoming, java.util.function.Consumer<String> setter) {
    if (!StringUtils.hasText(current) && StringUtils.hasText(incoming)) {
      setter.accept(incoming);
      return true;
    }
    return false;
  }

  static String joinComments(String category, String remark) {
    if (StringUtils.hasText(category) && StringUtils.hasText(remark)) {
      return category + "；" + remark;
    }
    if (StringUtils.hasText(category)) {
      return category;
    }
    return StringUtils.hasText(remark) ? remark : null;
  }

  private static String firstNonBlank(String a, String b) {
    if (StringUtils.hasText(a)) {
      return a;
    }
    return StringUtils.hasText(b) ? b : null;
  }

  private static String safeName(String name) {
    return StringUtils.hasText(name) ? name : "";
  }

  private static String rootMessage(Throwable ex) {
    if (ex instanceof BusinessException be && StringUtils.hasText(be.getMessage())) {
      return be.getMessage();
    }
    return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
  }

  private static Integer calcAgeFromIdCard(String idCard) {
    try {
      if (!StringUtils.hasText(idCard) || idCard.length() < 14) {
        return null;
      }
      String birthRaw = idCard.length() == 18 ? idCard.substring(6, 14) : "19" + idCard.substring(6, 12);
      LocalDate birth = LocalDate.parse(birthRaw, DateTimeFormatter.BASIC_ISO_DATE);
      return Period.between(birth, LocalDate.now()).getYears();
    } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
      return null;
    }
  }

  private static Integer genderFromIdCard(String idCard) {
    if (!StringUtils.hasText(idCard) || idCard.length() < 2) {
      return 0;
    }
    int digit = Character.getNumericValue(idCard.charAt(idCard.length() - 2));
    return digit % 2 > 0 ? 0 : 1;
  }

  private static String genderLabelFromIdCard(String idCard) {
    Integer g = genderFromIdCard(idCard);
    if (g == null) {
      return null;
    }
    return g == 0 ? "男" : "女";
  }

  private record ClassWorkType(Long workTypeId, String workTypeName, Integer reqLevel) {}
}

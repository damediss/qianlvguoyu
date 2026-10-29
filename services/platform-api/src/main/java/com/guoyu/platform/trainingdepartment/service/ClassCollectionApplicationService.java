package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.TrainPeopleListSignUpFieldMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 开班准备 · 按班资料收集（collection_source=class_prep）
 */
@Service
@Slf4j
public class ClassCollectionApplicationService {

  public static final String SOURCE_CLASS_PREP = "class_prep";
  public static final int STATUS_PENDING = 0;
  public static final int STATUS_APPROVED = 1;

  private final TrainInfoClassMapper classMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final JdbcTemplate jdbcTemplate;

  public ClassCollectionApplicationService(
      TrainInfoClassMapper classMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainingOperatorScopeService operatorScopeService,
      JdbcTemplate jdbcTemplate) {
    this.classMapper = classMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleMapper = signUpPeopleMapper;
    this.peopleListMapper = peopleListMapper;
    this.operatorScopeService = operatorScopeService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public Map<String, Object> ensureCollection(Long classId) {
    operatorScopeService.requireTrainingOperator();
    TrainInfoClassEntity clazz = getClassAccessible(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();

    TrainSignUpSelfEntity existing = findClassPrepSignUp(tenantId, classId);
    if (existing == null) {
      existing = createClassPrepSignUp(tenantId, clazz);
    }
    int seeded = seedFromRoster(tenantId, clazz, existing.getId());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("signUpId", existing.getId());
    result.put("title", existing.getTitle());
    result.put("trainClassId", classId);
    result.put("trainInfoId", existing.getTrainInfoId());
    result.put("collectionSource", SOURCE_CLASS_PREP);
    result.put("seeded", seeded);
    result.putAll(profileProgressForSignUp(existing.getId(), classId));
    return result;
  }

  /** 报名人员创建/审核通过后同步花名册 */
  @Transactional
  public void syncPersonToRoster(TrainSignUpPeopleSelfEntity person) {
    if (person == null || person.getSignUpId() == null) {
      return;
    }
    TrainSignUpSelfEntity signUp = signUpSelfMapper.selectById(person.getSignUpId());
    if (signUp == null
        || signUp.getDeleted() != null && signUp.getDeleted() == 1
        || !SOURCE_CLASS_PREP.equals(signUp.getCollectionSource())
        || signUp.getTrainClassId() == null) {
      return;
    }
    upsertRosterStudent(signUp.getTrainInfoId(), signUp.getTrainClassId(), person, signUp.getTenantId());
  }

  /**
   * class_prep（或已绑定班级）计划：身份证必须已在本班学员花名册中。
   * 其它计划直接放行。
   */
  @Transactional(readOnly = true)
  public void assertIdCardInClassRoster(TrainSignUpSelfEntity plan, String idCardNumber) {
    if (!requiresRoster(plan)) {
      return;
    }
    if (!isIdCardInClassRoster(plan, idCardNumber)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "您不在本班学员名单中，无法填写资料");
    }
  }

  /** 是否需要花名册门禁（开班准备 / 已绑定班级） */
  public boolean requiresRoster(TrainSignUpSelfEntity plan) {
    if (plan == null) {
      return false;
    }
    return SOURCE_CLASS_PREP.equals(plan.getCollectionSource()) || plan.getTrainClassId() != null;
  }

  public boolean isClassPrep(TrainSignUpSelfEntity plan) {
    return requiresRoster(plan);
  }

  /**
   * 扫码验证用：返回是否需要花名册、是否在名单内、是否允许进入填写。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> checkRosterAccess(TrainSignUpSelfEntity plan, String idCardNumber) {
    boolean requireRoster = requiresRoster(plan);
    boolean inRoster = !requireRoster || isIdCardInClassRoster(plan, idCardNumber);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("requireRoster", requireRoster);
    m.put("inRoster", inRoster);
    m.put("allowed", !requireRoster || inRoster);
    m.put("trainClassId", plan != null ? plan.getTrainClassId() : null);
    m.put("collectionSource", plan != null ? plan.getCollectionSource() : null);
    return m;
  }

  private boolean isIdCardInClassRoster(TrainSignUpSelfEntity plan, String idCardNumber) {
    if (plan == null || plan.getTrainClassId() == null) {
      return false;
    }
    if (!StringUtils.hasText(idCardNumber)) {
      return false;
    }
    String want = idCardNumber.trim().toUpperCase();
    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, plan.getTrainClassId())
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .select(TrainPeopleListEntity::getId, TrainPeopleListEntity::getIdCard));
    for (TrainPeopleListEntity s : students) {
      if (s.getIdCard() != null && want.equals(s.getIdCard().trim().toUpperCase())) {
        return true;
      }
    }
    return false;
  }

  /** 批量审核通过后同步 */
  @Transactional
  public void syncApprovedPeopleToRoster(List<Long> peopleIds) {
    if (peopleIds == null || peopleIds.isEmpty()) {
      return;
    }
    List<TrainSignUpPeopleSelfEntity> people = signUpPeopleMapper.selectBatchIds(peopleIds);
    for (TrainSignUpPeopleSelfEntity person : people) {
      if (person == null || person.getDeleted() != null && person.getDeleted() == 1) {
        continue;
      }
      if (person.getStatus() != null && person.getStatus() == STATUS_APPROVED) {
        syncPersonToRoster(person);
      }
    }
  }

  public Map<Long, Map<String, Object>> batchProfileProgress(List<Long> classIds) {
    Map<Long, Map<String, Object>> out = new HashMap<>();
    if (classIds == null || classIds.isEmpty()) {
      return out;
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainSignUpSelfEntity> signUps = signUpSelfMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0)
            .eq(TrainSignUpSelfEntity::getCollectionSource, SOURCE_CLASS_PREP)
            .in(TrainSignUpSelfEntity::getTrainClassId, classIds));
    Map<Long, Long> classToSignUp = new HashMap<>();
    for (TrainSignUpSelfEntity s : signUps) {
      if (s.getTrainClassId() != null) {
        classToSignUp.put(s.getTrainClassId(), s.getId());
      }
    }
    for (Long classId : classIds) {
      Long signUpId = classToSignUp.get(classId);
      out.put(classId, profileProgressForSignUp(signUpId, classId));
    }
    return out;
  }

  private Map<String, Object> profileProgressForSignUp(Long signUpId, Long classId) {
    long rosterCount = peopleListMapper.selectCount(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    long total = 0;
    long approved = 0;
    long pending = 0;
    if (signUpId != null) {
      List<TrainSignUpPeopleSelfEntity> people = signUpPeopleMapper.selectList(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
              .select(TrainSignUpPeopleSelfEntity::getId, TrainSignUpPeopleSelfEntity::getStatus));
      total = people.size();
      for (TrainSignUpPeopleSelfEntity p : people) {
        int st = p.getStatus() != null ? p.getStatus() : STATUS_PENDING;
        if (st == STATUS_APPROVED) {
          approved++;
        } else if (st == STATUS_PENDING) {
          pending++;
        }
      }
    }
    boolean ready = rosterCount > 0 && approved >= rosterCount;
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("signUpId", signUpId);
    m.put("profile_total", total);
    m.put("profile_approved", approved);
    m.put("profile_pending", pending);
    m.put("roster_count", rosterCount);
    m.put("profile_ready", ready);
    return m;
  }

  private TrainSignUpSelfEntity findClassPrepSignUp(Long tenantId, Long classId) {
    return signUpSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0)
            .eq(TrainSignUpSelfEntity::getCollectionSource, SOURCE_CLASS_PREP)
            .eq(TrainSignUpSelfEntity::getTrainClassId, classId)
            .last("LIMIT 1"));
  }

  private TrainSignUpSelfEntity createClassPrepSignUp(Long tenantId, TrainInfoClassEntity clazz) {
    LocalDate today = LocalDate.now();
    String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
    TrainSignUpSelfEntity entity = new TrainSignUpSelfEntity();
    entity.setTenantId(tenantId);
    entity.setTitle((clazz.getTitle() != null ? clazz.getTitle() : "班级") + "-资料收集");
    entity.setTrainTime(dateStr + " ~ " + dateStr);
    entity.setExamTime(dateStr);
    entity.setOrgId(clazz.getCompanyId());
    entity.setTrainInfoId(clazz.getTrainInfoId());
    entity.setTrainClassId(clazz.getId());
    entity.setCollectionSource(SOURCE_CLASS_PREP);
    entity.setSortNumber(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    signUpSelfMapper.insert(entity);
    return entity;
  }

  private int seedFromRoster(Long tenantId, TrainInfoClassEntity clazz, Long signUpId) {
    List<TrainPeopleListEntity> roster = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, clazz.getId())
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    if (roster.isEmpty()) {
      return 0;
    }
    // 含已删除记录：用户删过的人当作墓碑，再次打开资料收集时不得从花名册种回
    Set<String> existingCards = new HashSet<>();
    List<String> cards = jdbcTemplate.query(
        """
            SELECT UPPER(TRIM(id_card_number))
            FROM train_sign_up_people_self
            WHERE sign_up_id = ?
              AND id_card_number IS NOT NULL
              AND TRIM(id_card_number) <> ''
            """,
        (rs, rowNum) -> rs.getString(1),
        signUpId);
    for (String c : cards) {
      if (StringUtils.hasText(c)) {
        existingCards.add(c.trim().toUpperCase());
      }
    }
    Set<String> existingNames = new HashSet<>();
    List<String> names = jdbcTemplate.query(
        """
            SELECT TRIM(name)
            FROM train_sign_up_people_self
            WHERE sign_up_id = ?
              AND name IS NOT NULL
              AND TRIM(name) <> ''
            """,
        (rs, rowNum) -> rs.getString(1),
        signUpId);
    for (String n : names) {
      if (StringUtils.hasText(n)) {
        existingNames.add(n.trim());
      }
    }
    int seeded = 0;
    for (TrainPeopleListEntity student : roster) {
      String idCard = student.getIdCard() != null ? student.getIdCard().trim() : "";
      String realname = student.getRealname() != null ? student.getRealname().trim() : "";
      if (StringUtils.hasText(idCard) && existingCards.contains(idCard.toUpperCase())) {
        continue;
      }
      // 花名册占位身份证与扫码真实身份证不一致时，按姓名避免重复种入；已删除同名也视为墓碑
      if (StringUtils.hasText(realname) && existingNames.contains(realname)) {
        continue;
      }
      TrainSignUpPeopleSelfEntity row = new TrainSignUpPeopleSelfEntity();
      row.setTenantId(tenantId);
      row.setSignUpId(signUpId);
      row.setPlatformUserId(student.getPlatformUserId());
      row.setName(student.getRealname());
      row.setPhone(student.getPhone());
      row.setIdCardNumber(StringUtils.hasText(idCard) ? idCard : null);
      row.setStatus(STATUS_PENDING);
      row.setCompanyStatus(0);
      row.setHasPay(0);
      row.setSchoolPaperHas(0);
      row.setWorkTypePaperHas(0);
      row.setDataSource("roster_seed");
      row.setDeleted(0);
      row.setCreateTime(LocalDateTime.now());
      row.setUpdateTime(LocalDateTime.now());
      signUpPeopleMapper.insert(row);
      if (StringUtils.hasText(idCard)) {
        existingCards.add(idCard.toUpperCase());
      }
      if (StringUtils.hasText(realname)) {
        existingNames.add(realname);
      }
      seeded++;
    }
    return seeded;
  }

  private void upsertRosterStudent(
      Long trainInfoId, Long classId, TrainSignUpPeopleSelfEntity person, Long tenantId) {
    String idCard = person.getIdCardNumber() != null ? person.getIdCardNumber().trim() : null;
    TrainPeopleListEntity existing = null;
    if (StringUtils.hasText(idCard)) {
      existing = peopleListMapper.selectOne(
          new LambdaQueryWrapper<TrainPeopleListEntity>()
              .eq(TrainPeopleListEntity::getTrainClassId, classId)
              .eq(TrainPeopleListEntity::getType, "student")
              .eq(TrainPeopleListEntity::getDeleted, 0)
              .eq(TrainPeopleListEntity::getIdCard, idCard)
              .last("LIMIT 1"));
    }
    if (existing == null && person.getPlatformUserId() != null) {
      existing = peopleListMapper.selectOne(
          new LambdaQueryWrapper<TrainPeopleListEntity>()
              .eq(TrainPeopleListEntity::getTrainClassId, classId)
              .eq(TrainPeopleListEntity::getType, "student")
              .eq(TrainPeopleListEntity::getDeleted, 0)
              .eq(TrainPeopleListEntity::getPlatformUserId, person.getPlatformUserId())
              .last("LIMIT 1"));
    }
    if (existing != null) {
      if (StringUtils.hasText(person.getName())) {
        existing.setRealname(person.getName());
      }
      if (StringUtils.hasText(person.getPhone())) {
        existing.setPhone(person.getPhone());
      }
      if (StringUtils.hasText(idCard)) {
        existing.setIdCard(idCard);
      }
      if (person.getPlatformUserId() != null) {
        existing.setPlatformUserId(person.getPlatformUserId());
      }
      TrainPeopleListSignUpFieldMapper.applySignUpFields(existing, person);
      peopleListMapper.updateById(existing);
      return;
    }
    TrainPeopleListEntity entity = new TrainPeopleListEntity();
    entity.setType("student");
    entity.setTrainId(trainInfoId);
    entity.setTrainClassId(classId);
    entity.setTenantId(tenantId);
    entity.setRealname(person.getName());
    entity.setPhone(person.getPhone());
    entity.setIdCard(idCard);
    entity.setPlatformUserId(person.getPlatformUserId());
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    TrainPeopleListSignUpFieldMapper.applySignUpFields(entity, person);
    peopleListMapper.insert(entity);
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

  private static boolean eqIgnoreCase(String a, String b) {
    if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
      return false;
    }
    return a.trim().equalsIgnoreCase(b.trim());
  }

  /** 供花名册导入按班级定位收集计划 */
  @Transactional(readOnly = true)
  public TrainSignUpSelfEntity requireClassPrepSignUp(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity signUp = findClassPrepSignUp(tenantId, classId);
    if (signUp == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "请先打开班级资料收集（将自动创建收集计划）");
    }
    return signUp;
  }

  @Transactional(readOnly = true)
  public TrainSignUpSelfEntity findByClassId(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return findClassPrepSignUp(tenantId, classId);
  }

  public Map<Long, Long> mapClassIdsToSignUpIds(List<Long> classIds) {
    if (classIds == null || classIds.isEmpty()) {
      return Map.of();
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return signUpSelfMapper.selectList(
            new LambdaQueryWrapper<TrainSignUpSelfEntity>()
                .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
                .eq(TrainSignUpSelfEntity::getDeleted, 0)
                .eq(TrainSignUpSelfEntity::getCollectionSource, SOURCE_CLASS_PREP)
                .in(TrainSignUpSelfEntity::getTrainClassId, classIds))
        .stream()
        .filter(s -> s.getTrainClassId() != null)
        .collect(Collectors.toMap(TrainSignUpSelfEntity::getTrainClassId, TrainSignUpSelfEntity::getId, (a, b) -> a));
  }
}

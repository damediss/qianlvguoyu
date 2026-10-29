package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainClassRoleAssignReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassRoleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpEventCode;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogCommand;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainClassRoleVO;
import com.guoyu.platform.trainingdepartment.vo.TrainMyClassVO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 班级角色（train_class_role）应用服务 — 《培训班级五角色落地实现方案》R1。
 */
@Slf4j
@Service
public class TrainClassRoleApplicationService {

  public static final String HEAD_TEACHER = "head_teacher";
  public static final String FOLLOW_STAFF = "follow_staff";
  public static final String LEAD_TEACHER = "lead_teacher";
  public static final String ASSISTANT = "assistant";
  public static final String MONITOR = "monitor";

  private static final Set<String> ROLE_CODES = Set.of(
      HEAD_TEACHER, FOLLOW_STAFF, LEAD_TEACHER, ASSISTANT, MONITOR);

  private static final Map<String, Integer> ROLE_CAPS = Map.of(
      HEAD_TEACHER, 2,
      FOLLOW_STAFF, 2,
      LEAD_TEACHER, 3,
      ASSISTANT, 5,
      MONITOR, 20);

  private static final Map<String, String> ROLE_NAMES = Map.of(
      HEAD_TEACHER, "教务人员",
      FOLLOW_STAFF, "教务人员",
      LEAD_TEACHER, "理论老师",
      ASSISTANT, "实操老师",
      MONITOR, "班长");

  private final TrainClassRoleMapper roleMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleMapper;
  private final TrainTeacherBankMapper teacherBankMapper;
  private final PlatformUserMapper platformUserMapper;
  private final PlatformUserEnsureService platformUserEnsureService;
  private final TrainingOperatorScopeService operatorScopeService;
  private final FollowOpLogWriter opLogWriter;

  public TrainClassRoleApplicationService(
      TrainClassRoleMapper roleMapper,
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleMapper,
      TrainTeacherBankMapper teacherBankMapper,
      PlatformUserMapper platformUserMapper,
      PlatformUserEnsureService platformUserEnsureService,
      TrainingOperatorScopeService operatorScopeService,
      FollowOpLogWriter opLogWriter) {
    this.roleMapper = roleMapper;
    this.classMapper = classMapper;
    this.peopleMapper = peopleMapper;
    this.teacherBankMapper = teacherBankMapper;
    this.platformUserMapper = platformUserMapper;
    this.platformUserEnsureService = platformUserEnsureService;
    this.operatorScopeService = operatorScopeService;
    this.opLogWriter = opLogWriter;
  }

  @Transactional(readOnly = true)
  public List<TrainClassRoleVO> listByClass(Long classId) {
    assertClassViewable(classId);
    return roleMapper.selectList(activeWrapper(classId)).stream().map(this::toVO).toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> roleSummary(Long classId) {
    List<TrainClassRoleVO> roles = listByClass(classId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("roles", roles);
    result.put("unboundCount", roles.stream().filter(TrainClassRoleVO::unboundWarning).count());
    return result;
  }

  /**
   * App：当前登录 platform_user 可见班级（角色表活跃岗位 ∪ 花名册学员）。
   */
  @Transactional(readOnly = true)
  public List<TrainMyClassVO> listMyClasses(Long platformUserId) {
    if (platformUserId == null || platformUserId <= 0) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
    }
    Map<Long, LinkedHashSet<String>> rolesByClass = new LinkedHashMap<>();

    List<TrainClassRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getPlatformUserId, platformUserId)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active"));
    for (TrainClassRoleEntity r : roles) {
      if (r.getClassId() == null || !StringUtils.hasText(r.getRoleCode())) {
        continue;
      }
      rolesByClass.computeIfAbsent(r.getClassId(), k -> new LinkedHashSet<>()).add(r.getRoleCode());
    }

    List<TrainPeopleListEntity> students = peopleMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .isNotNull(TrainPeopleListEntity::getTrainClassId));
    for (TrainPeopleListEntity p : students) {
      rolesByClass.computeIfAbsent(p.getTrainClassId(), k -> new LinkedHashSet<>()).add("student");
    }

    if (rolesByClass.isEmpty()) {
      return List.of();
    }

    Map<Long, TrainInfoClassEntity> classMap = classMapper.selectBatchIds(rolesByClass.keySet()).stream()
        .filter(c -> c.getDeleted() == null || c.getDeleted() == 0)
        .collect(Collectors.toMap(TrainInfoClassEntity::getId, c -> c, (a, b) -> a));

    List<TrainMyClassVO> result = new ArrayList<>();
    for (Map.Entry<Long, LinkedHashSet<String>> e : rolesByClass.entrySet()) {
      TrainInfoClassEntity clazz = classMap.get(e.getKey());
      if (clazz == null) {
        continue;
      }
      List<String> codes = new ArrayList<>(e.getValue());
      String title = StringUtils.hasText(clazz.getTitle()) ? clazz.getTitle() : ("班级#" + clazz.getId());
      result.add(new TrainMyClassVO(clazz.getId(), title, codes, pickPrimaryRole(codes), false));
    }
    result.sort(Comparator.comparing(TrainMyClassVO::classId).reversed());
    return result;
  }

  /**
   * App：班内角色摘要（须为该班成员）。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> roleSummaryForMember(Long platformUserId, Long classId) {
    assertClassMember(platformUserId, classId);
    List<TrainClassRoleVO> roles = roleMapper.selectList(activeWrapper(classId)).stream()
        .map(this::toVO)
        .toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("roles", roles);
    result.put("unboundCount", roles.stream().filter(TrainClassRoleVO::unboundWarning).count());
    return result;
  }

  public void assertClassMember(Long platformUserId, Long classId) {
    if (platformUserId == null || platformUserId <= 0) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
    }
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "classId 无效");
    }
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    if (clazz == null || (clazz.getDeleted() != null && clazz.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    long roleHit = roleMapper.selectCount(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, classId)
        .eq(TrainClassRoleEntity::getPlatformUserId, platformUserId)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active"));
    if (roleHit > 0) {
      return;
    }
    long studentHit = peopleMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (studentHit > 0) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "无本班权限");
  }

  /**
   * App：当前用户在本班的主角色（与 listMyClasses 优先级一致）。
   * 仅读，不影响现有角色分配逻辑。
   */
  @Transactional(readOnly = true)
  public String primaryRoleOfMember(Long platformUserId, Long classId) {
    assertClassMember(platformUserId, classId);
    List<String> codes = new ArrayList<>();
    List<TrainClassRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, classId)
        .eq(TrainClassRoleEntity::getPlatformUserId, platformUserId)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active"));
    for (TrainClassRoleEntity r : roles) {
      if (StringUtils.hasText(r.getRoleCode())) {
        codes.add(r.getRoleCode());
      }
    }
    long studentHit = peopleMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (studentHit > 0 && !codes.contains("student")) {
      codes.add("student");
    }
    return pickPrimaryRole(codes);
  }

  /**
   * App：须持有本班任一指定活跃角色，返回命中的角色码（按 allowed 顺序优先）。
   */
  public String requireAnyActiveRole(Long platformUserId, Long classId, String... allowedRoleCodes) {
    assertClassMember(platformUserId, classId);
    if (allowedRoleCodes == null || allowedRoleCodes.length == 0) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
    }
    List<TrainClassRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, classId)
        .eq(TrainClassRoleEntity::getPlatformUserId, platformUserId)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active"));
    Set<String> held = roles.stream()
        .map(TrainClassRoleEntity::getRoleCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    for (String code : allowedRoleCodes) {
      if (held.contains(code)) {
        return code;
      }
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行此操作");
  }

  private static String pickPrimaryRole(List<String> roleCodes) {
    String[] priority = {
        MONITOR, HEAD_TEACHER, FOLLOW_STAFF, LEAD_TEACHER, ASSISTANT, "student"
    };
    for (String code : priority) {
      if (roleCodes.contains(code)) {
        return code;
      }
    }
    return roleCodes.isEmpty() ? "student" : roleCodes.get(0);
  }

  @Transactional
  public TrainClassRoleVO assign(Long classId, TrainClassRoleAssignReq req) {
    operatorScopeService.requireTrainingOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    String roleCode = normalizeRole(req.getRoleCode());
    if (MONITOR.equals(roleCode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班长请在花名册设置「是否班长」，系统自动同步");
    }

    String subjectType = StringUtils.hasText(req.getSubjectType())
        ? req.getSubjectType().trim()
        : inferSubjectType(roleCode, req);
    Long subjectId = req.getSubjectId();
    String displayName = req.getDisplayName();
    String phone = req.getPhone();
    String idCard = req.getIdCard();
    Long peopleId = req.getPeopleId();
    Long existingPlatformUserId = null;
    Long teacherCompanyId = null;

    if ("teacher_bank".equals(subjectType)) {
      TrainTeacherBankEntity teacher = teacherBankMapper.selectById(subjectId);
      if (teacher == null || (teacher.getDeleted() != null && teacher.getDeleted() == 1)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "师资库人员不存在");
      }
      displayName = firstNonBlank(displayName, teacher.getName());
      phone = firstNonBlank(phone, teacher.getPhone());
      idCard = firstNonBlank(idCard, teacher.getIdCardNumber());
      teacherCompanyId = teacher.getCompanyId();
    } else if ("people".equals(subjectType)) {
      TrainPeopleListEntity people = peopleMapper.selectById(subjectId);
      if (people == null || (people.getDeleted() != null && people.getDeleted() == 1)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "花名册人员不存在");
      }
      if (!classId.equals(people.getTrainClassId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不属于本班");
      }
      peopleId = people.getId();
      displayName = firstNonBlank(displayName, people.getRealname());
      phone = firstNonBlank(phone, people.getPhone());
      idCard = firstNonBlank(idCard, people.getIdCard());
      existingPlatformUserId = people.getPlatformUserId();
    }

    // 履职角色强制开户绑定；bindPlatformUser=false 忽略（见《班级角色老师App开户》）
    AccountBind bind = resolveDutyAccount(
        displayName, phone, idCard, existingPlatformUserId, clazz, teacherCompanyId);

    TrainClassRoleEntity existing = findActive(classId, roleCode, subjectType, subjectId);
    if (existing != null) {
      existing.setDisplayName(displayName);
      existing.setPhone(phone);
      existing.setPeopleId(peopleId);
      existing.setPlatformUserId(bind.platformUserId());
      roleMapper.updateById(existing);
      dualWriteLegacy(classId, roleCode, existing, false);
      emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_ASSIGN, existing,
          "更新角色指派：" + roleLabel(roleCode) + " · " + nullToDash(displayName), false);
      return toVO(existing, bind.bindStatus());
    }

    assertUnderCap(classId, roleCode);

    TrainClassRoleEntity entity = new TrainClassRoleEntity();
    entity.setClassId(classId);
    entity.setRoleCode(roleCode);
    entity.setSubjectType(subjectType);
    entity.setSubjectId(subjectId);
    entity.setPeopleId(peopleId);
    entity.setDisplayName(displayName);
    entity.setPhone(phone);
    entity.setPlatformUserId(bind.platformUserId());
    entity.setStatus("active");
    entity.setStartDate(LocalDate.now());
    entity.setTenantId(resolveTenantId(classId));
    roleMapper.insert(entity);

    dualWriteLegacy(classId, roleCode, entity, false);
    emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_ASSIGN, entity,
        "指派" + roleLabel(roleCode) + "：" + nullToDash(displayName), false);
    return toVO(entity, bind.bindStatus());
  }

  /**
   * 单行补开 App 账号并回写 platform_user_id（历史 unbound）。
   */
  @Transactional
  public TrainClassRoleVO ensureAppAccount(Long classId, Long roleId) {
    operatorScopeService.requireTrainingOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    TrainClassRoleEntity entity = requireActiveDutyRole(classId, roleId);
    if (entity.getPlatformUserId() != null && entity.getPlatformUserId() > 0) {
      return toVO(entity, "bound");
    }
    SubjectContact contact = resolveSubjectContact(entity);
    AccountBind bind = resolveDutyAccount(
        contact.displayName(), contact.phone(), contact.idCard(), null, clazz, contact.companyId());
    entity.setDisplayName(firstNonBlank(entity.getDisplayName(), contact.displayName()));
    entity.setPhone(firstNonBlank(entity.getPhone(), contact.phone()));
    entity.setPlatformUserId(bind.platformUserId());
    roleMapper.updateById(entity);
    emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_ASSIGN, entity,
        "补开 App 账号：" + nullToDash(entity.getDisplayName()), false);
    return toVO(entity, bind.bindStatus());
  }

  /**
   * 本班未绑定履职角色批量补开。
   */
  @Transactional
  public Map<String, Object> ensureAppAccountsForClass(Long classId) {
    operatorScopeService.requireTrainingOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    PlatformUserEnsureService.SyncStats stats = new PlatformUserEnsureService.SyncStats();
    List<TrainClassRoleEntity> unbound = roleMapper.selectList(activeWrapper(classId)).stream()
        .filter(r -> !MONITOR.equals(r.getRoleCode()))
        .filter(r -> r.getPlatformUserId() == null || r.getPlatformUserId() <= 0)
        .toList();
    for (TrainClassRoleEntity entity : unbound) {
      SubjectContact contact = resolveSubjectContact(entity);
      String name = firstNonBlank(entity.getDisplayName(), contact.displayName());
      String phone = firstNonBlank(entity.getPhone(), contact.phone());
      String idCard = contact.idCard();
      try {
        AccountBind bind = resolveDutyAccount(name, phone, idCard, null, clazz, contact.companyId());
        entity.setDisplayName(name);
        entity.setPhone(phone);
        entity.setPlatformUserId(bind.platformUserId());
        roleMapper.updateById(entity);
        stats.accept(
            new PlatformUserEnsureService.EnsureResult(
                "ensured".equals(bind.bindStatus())
                    ? PlatformUserEnsureService.Outcome.CREATED
                    : PlatformUserEnsureService.Outcome.REUSED,
                bind.platformUserId(),
                null),
            name,
            PlatformUserEnsureService.normalizeIdCard(idCard));
      } catch (BusinessException ex) {
        stats.accept(
            PlatformUserEnsureService.EnsureResult.skipped(ex.getMessage()),
            name,
            PlatformUserEnsureService.normalizeIdCard(idCard));
      }
    }
    Map<String, Object> out = stats.toMap();
    out.put("classId", classId);
    out.put("attempted", unbound.size());
    return out;
  }

  @Transactional
  public void endRole(Long classId, Long roleId) {
    operatorScopeService.requireTrainingOperator();
    assertClassViewable(classId);
    TrainClassRoleEntity entity = roleMapper.selectById(roleId);
    if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "角色记录不存在");
    }
    if (!classId.equals(entity.getClassId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不属于本班");
    }
    if (MONITOR.equals(entity.getRoleCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班长请在花名册取消「是否班长」");
    }
    entity.setStatus("ended");
    entity.setEndDate(LocalDate.now());
    roleMapper.updateById(entity);
    dualWriteLegacy(classId, entity.getRoleCode(), entity, true);
    emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_UNBIND, entity,
        "解除" + roleLabel(entity.getRoleCode()) + "：" + nullToDash(entity.getDisplayName()), true);
  }

  /** 花名册班长开关同步 monitor */
  @Transactional
  public void syncMonitorFromStudent(TrainPeopleListEntity student) {
    if (student == null || student.getTrainClassId() == null || !"student".equals(student.getType())) {
      return;
    }
    Long classId = student.getTrainClassId();
    boolean isLeader = student.getIsLeader() != null && student.getIsLeader() == 1;
    TrainClassRoleEntity existing = roleMapper.selectOne(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, classId)
        .eq(TrainClassRoleEntity::getRoleCode, MONITOR)
        .eq(TrainClassRoleEntity::getPeopleId, student.getId())
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active")
        .last("LIMIT 1"));
    if (isLeader) {
      if (existing != null) {
        existing.setDisplayName(student.getRealname());
        existing.setPhone(student.getPhone());
        existing.setPlatformUserId(student.getPlatformUserId());
        roleMapper.updateById(existing);
        return;
      }
      TrainClassRoleEntity entity = new TrainClassRoleEntity();
      entity.setClassId(classId);
      entity.setRoleCode(MONITOR);
      entity.setSubjectType("people");
      entity.setSubjectId(student.getId());
      entity.setPeopleId(student.getId());
      entity.setDisplayName(student.getRealname());
      entity.setPhone(student.getPhone());
      entity.setPlatformUserId(student.getPlatformUserId());
      entity.setStatus("active");
      entity.setStartDate(LocalDate.now());
      entity.setTenantId(resolveTenantId(classId));
      roleMapper.insert(entity);
      emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_ASSIGN, entity,
          "设置班长：" + nullToDash(student.getRealname()), false);
    } else if (existing != null) {
      existing.setStatus("ended");
      existing.setEndDate(LocalDate.now());
      roleMapper.updateById(existing);
      emitRoleOp(classId, FollowOpEventCode.CLASS_ROLE_UNBIND, existing,
          "取消班长：" + nullToDash(existing.getDisplayName()), true);
    }
  }

  /** 遗留 staff/inCharge 写入后同步角色表 */
  @Transactional
  public void syncFromLegacyPeople(TrainPeopleListEntity people) {
    if (people == null || people.getTrainClassId() == null) {
      return;
    }
    String roleCode;
    String subjectType;
    Long subjectId;
    if ("staff".equals(people.getType())) {
      roleCode = FOLLOW_STAFF;
      if (people.getUserId() != null && people.getUserId() > 0) {
        subjectType = "teacher_bank";
        subjectId = people.getUserId();
      } else {
        subjectType = "people";
        subjectId = people.getId();
      }
    } else if ("inCharge".equals(people.getType())) {
      roleCode = HEAD_TEACHER;
      subjectType = "people";
      subjectId = people.getId();
    } else {
      return;
    }
    TrainClassRoleEntity existing = findActive(people.getTrainClassId(), roleCode, subjectType, subjectId);
    if (existing == null && people.getId() != null) {
      existing = roleMapper.selectOne(new LambdaQueryWrapper<TrainClassRoleEntity>()
          .eq(TrainClassRoleEntity::getClassId, people.getTrainClassId())
          .eq(TrainClassRoleEntity::getRoleCode, roleCode)
          .eq(TrainClassRoleEntity::getPeopleId, people.getId())
          .eq(TrainClassRoleEntity::getDeleted, 0)
          .eq(TrainClassRoleEntity::getStatus, "active")
          .last("LIMIT 1"));
    }
    Long platformUserId = findPlatformUserId(people.getPhone(), people.getIdCard());
    if (existing != null) {
      existing.setDisplayName(people.getRealname());
      existing.setPhone(people.getPhone());
      existing.setPeopleId(people.getId());
      if (platformUserId != null) {
        existing.setPlatformUserId(platformUserId);
      }
      roleMapper.updateById(existing);
      return;
    }
    long activeCount = roleMapper.selectCount(activeWrapper(people.getTrainClassId())
        .eq(TrainClassRoleEntity::getRoleCode, roleCode));
    Integer cap = ROLE_CAPS.get(roleCode);
    if (cap != null && activeCount >= cap) {
      log.warn("[班级角色] 同步遗留人员超上限 classId={} role={} count={}",
          people.getTrainClassId(), roleCode, activeCount);
      return;
    }
    TrainClassRoleEntity entity = new TrainClassRoleEntity();
    entity.setClassId(people.getTrainClassId());
    entity.setRoleCode(roleCode);
    entity.setSubjectType(subjectType);
    entity.setSubjectId(subjectId);
    entity.setPeopleId(people.getId());
    entity.setDisplayName(people.getRealname());
    entity.setPhone(people.getPhone());
    entity.setPlatformUserId(platformUserId);
    entity.setStatus("active");
    entity.setStartDate(LocalDate.now());
    entity.setTenantId(resolveTenantId(people.getTrainClassId()));
    roleMapper.insert(entity);
  }

  /** 遗留人员删除时结束对应角色 */
  @Transactional
  public void endFromLegacyPeople(TrainPeopleListEntity people) {
    if (people == null || people.getTrainClassId() == null) {
      return;
    }
    String roleCode = "staff".equals(people.getType()) ? FOLLOW_STAFF
        : "inCharge".equals(people.getType()) ? HEAD_TEACHER : null;
    if (roleCode == null) {
      return;
    }
    List<TrainClassRoleEntity> list = roleMapper.selectList(new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, people.getTrainClassId())
        .eq(TrainClassRoleEntity::getRoleCode, roleCode)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active")
        .and(w -> w.eq(TrainClassRoleEntity::getPeopleId, people.getId())
            .or()
            .eq(people.getUserId() != null, TrainClassRoleEntity::getSubjectId, people.getUserId())));
    for (TrainClassRoleEntity entity : list) {
      entity.setStatus("ended");
      entity.setEndDate(LocalDate.now());
      roleMapper.updateById(entity);
    }
  }

  private void dualWriteLegacy(Long classId, String roleCode, TrainClassRoleEntity role, boolean ending) {
    if (FOLLOW_STAFF.equals(roleCode) || HEAD_TEACHER.equals(roleCode)) {
      // 过渡期：角色表为权威；遗留 staff/inCharge 由旧 UI 继续维护，新 UI 指派时补写 people 行
      if (ending) {
        if (role.getPeopleId() != null) {
          TrainPeopleListEntity people = peopleMapper.selectById(role.getPeopleId());
          if (people != null && people.getDeleted() == 0) {
            people.setDeleted(1);
            peopleMapper.deleteById(people.getId());
          }
        }
        return;
      }
      if (role.getPeopleId() != null) {
        return;
      }
      String type = FOLLOW_STAFF.equals(roleCode) ? "staff" : "inCharge";
      TrainInfoClassEntity clazz = classMapper.selectById(classId);
      TrainPeopleListEntity people = new TrainPeopleListEntity();
      people.setType(type);
      people.setTrainClassId(classId);
      people.setTrainId(clazz != null ? clazz.getTrainInfoId() : null);
      people.setRealname(role.getDisplayName());
      people.setPhone(role.getPhone());
      if ("teacher_bank".equals(role.getSubjectType())) {
        people.setUserId(role.getSubjectId());
      }
      people.setTenantId(role.getTenantId());
      people.setDeleted(0);
      people.setCreateTime(java.time.LocalDateTime.now());
      peopleMapper.insert(people);
      role.setPeopleId(people.getId());
      if ("people".equals(role.getSubjectType())) {
        role.setSubjectId(people.getId());
      }
      roleMapper.updateById(role);
    }
  }

  private void assertUnderCap(Long classId, String roleCode) {
    long count = roleMapper.selectCount(activeWrapper(classId).eq(TrainClassRoleEntity::getRoleCode, roleCode));
    Integer cap = ROLE_CAPS.get(roleCode);
    if (cap != null && count >= cap) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          ROLE_NAMES.getOrDefault(roleCode, roleCode) + "每班最多 " + cap + " 人");
    }
  }

  private TrainClassRoleEntity findActive(Long classId, String roleCode, String subjectType, Long subjectId) {
    return roleMapper.selectOne(activeWrapper(classId)
        .eq(TrainClassRoleEntity::getRoleCode, roleCode)
        .eq(TrainClassRoleEntity::getSubjectType, subjectType)
        .eq(TrainClassRoleEntity::getSubjectId, subjectId)
        .last("LIMIT 1"));
  }

  private LambdaQueryWrapper<TrainClassRoleEntity> activeWrapper(Long classId) {
    return new LambdaQueryWrapper<TrainClassRoleEntity>()
        .eq(TrainClassRoleEntity::getClassId, classId)
        .eq(TrainClassRoleEntity::getDeleted, 0)
        .eq(TrainClassRoleEntity::getStatus, "active")
        .orderByAsc(TrainClassRoleEntity::getRoleCode)
        .orderByAsc(TrainClassRoleEntity::getId);
  }

  private String normalizeRole(String roleCode) {
    if (!StringUtils.hasText(roleCode) || !ROLE_CODES.contains(roleCode.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的角色码: " + roleCode);
    }
    return roleCode.trim();
  }

  private String inferSubjectType(String roleCode, TrainClassRoleAssignReq req) {
    if (LEAD_TEACHER.equals(roleCode) || ASSISTANT.equals(roleCode) || FOLLOW_STAFF.equals(roleCode)) {
      return "teacher_bank";
    }
    return "people";
  }

  private Long findPlatformUserId(String phone, String idCard) {
    if (StringUtils.hasText(idCard)) {
      PlatformUserEntity byCard = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
          .eq(PlatformUserEntity::getDeleted, 0)
          .eq(PlatformUserEntity::getIdcard, idCard.trim())
          .last("LIMIT 1"));
      if (byCard != null) {
        return byCard.getId();
      }
    }
    if (StringUtils.hasText(phone)) {
      PlatformUserEntity byPhone = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
          .eq(PlatformUserEntity::getDeleted, 0)
          .eq(PlatformUserEntity::getPhone, phone.trim())
          .last("LIMIT 1"));
      if (byPhone != null) {
        return byPhone.getId();
      }
    }
    return null;
  }

  private void assertClassViewable(Long classId) {
    requireClass(classId);
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    if (clazz == null || (clazz.getDeleted() != null && clazz.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(clazz);
    return clazz;
  }

  private TrainClassRoleEntity requireActiveDutyRole(Long classId, Long roleId) {
    TrainClassRoleEntity entity = roleMapper.selectById(roleId);
    if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "角色记录不存在");
    }
    if (!classId.equals(entity.getClassId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不属于本班");
    }
    if (!"active".equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "角色已卸任");
    }
    if (MONITOR.equals(entity.getRoleCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班长请走学员开户，不在此补开");
    }
    return entity;
  }

  private record AccountBind(Long platformUserId, String bindStatus) {}

  private record SubjectContact(String displayName, String phone, String idCard, Long companyId) {}

  private SubjectContact resolveSubjectContact(TrainClassRoleEntity entity) {
    String name = entity.getDisplayName();
    String phone = entity.getPhone();
    String idCard = null;
    Long companyId = null;
    if ("teacher_bank".equals(entity.getSubjectType()) && entity.getSubjectId() != null) {
      TrainTeacherBankEntity teacher = teacherBankMapper.selectById(entity.getSubjectId());
      if (teacher != null && (teacher.getDeleted() == null || teacher.getDeleted() == 0)) {
        name = firstNonBlank(name, teacher.getName());
        phone = firstNonBlank(phone, teacher.getPhone());
        idCard = teacher.getIdCardNumber();
        companyId = teacher.getCompanyId();
      }
    } else if (entity.getPeopleId() != null) {
      TrainPeopleListEntity people = peopleMapper.selectById(entity.getPeopleId());
      if (people != null && (people.getDeleted() == null || people.getDeleted() == 0)) {
        name = firstNonBlank(name, people.getRealname());
        phone = firstNonBlank(phone, people.getPhone());
        idCard = people.getIdCard();
      }
    } else if ("people".equals(entity.getSubjectType()) && entity.getSubjectId() != null) {
      TrainPeopleListEntity people = peopleMapper.selectById(entity.getSubjectId());
      if (people != null && (people.getDeleted() == null || people.getDeleted() == 0)) {
        name = firstNonBlank(name, people.getRealname());
        phone = firstNonBlank(phone, people.getPhone());
        idCard = people.getIdCard();
      }
    }
    return new SubjectContact(name, phone, idCard, companyId);
  }

  /**
   * 履职角色：必有 platform_user_id。优先沿用已有绑定；否则按身份证 ensure（同学员规则）。
   */
  private AccountBind resolveDutyAccount(
      String displayName,
      String phone,
      String idCard,
      Long existingPlatformUserId,
      TrainInfoClassEntity clazz,
      Long teacherCompanyId) {
    if (existingPlatformUserId != null && existingPlatformUserId > 0) {
      PlatformUserEntity existing = platformUserMapper.selectById(existingPlatformUserId);
      if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
        return new AccountBind(existingPlatformUserId, "bound");
      }
    }

    String normalizedId = PlatformUserEnsureService.normalizeIdCard(idCard);
    String normalizedPhone = phone == null ? "" : phone.replace(" ", "").trim();
    if (!StringUtils.hasText(normalizedId) || normalizedId.length() != 18) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先在师资库补全 18 位身份证后再指派");
    }
    if (!StringUtils.hasText(normalizedPhone) || normalizedPhone.length() != 11) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先在师资库补全 11 位手机号后再指派");
    }
    if (!StringUtils.hasText(displayName)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "姓名不能为空");
    }

    Long companyId = clazz != null && clazz.getCompanyId() != null && clazz.getCompanyId() > 0
        ? clazz.getCompanyId()
        : teacherCompanyId;
    Long orgId = companyId;

    PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
        displayName.trim(), normalizedId, normalizedPhone, companyId, orgId);
    if (ensure.outcome() == PlatformUserEnsureService.Outcome.SKIPPED
        || ensure.platformUserId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, mapEnsureWarning(ensure.warning()));
    }
    String bindStatus = switch (ensure.outcome()) {
      case CREATED -> "ensured";
      case REUSED, UPDATED -> "reused";
      default -> "bound";
    };
    log.info("[班级角色] ensure App账号 source=class_role outcome={} userId={} name={}",
        ensure.outcome(), ensure.platformUserId(), displayName);
    return new AccountBind(ensure.platformUserId(), bindStatus);
  }

  private static String mapEnsureWarning(String warning) {
    if (!StringUtils.hasText(warning)) {
      return "无法开通 App 账号，请核对手机号与身份证";
    }
    if (warning.contains("手机号已被其他学员占用") || warning.contains("手机号已被")) {
      return "该手机号已绑定其他身份证账号，请核对后重试";
    }
    if (warning.contains("身份证")) {
      return "请先在师资库补全 18 位身份证后再指派";
    }
    if (warning.contains("手机")) {
      return "请先在师资库补全 11 位手机号后再指派";
    }
    return warning;
  }

  private Long resolveTenantId(Long classId) {
    Long ctx = TenantContextHolder.getTenantId();
    if (ctx != null && ctx > 0) {
      return ctx;
    }
    try {
      Long tid = SecurityContext.currentUser().getTenantId();
      if (tid != null && tid > 0) {
        return tid;
      }
    } catch (Exception ignored) {
      // 双写/迁移路径可能无登录上下文
    }
    // train_info_class 无 tenant_id；无上下文时回退默认租户
    return 1L;
  }

  private TrainClassRoleVO toVO(TrainClassRoleEntity e) {
    return toVO(e, e.getPlatformUserId() != null && e.getPlatformUserId() > 0 ? "bound" : null);
  }

  private TrainClassRoleVO toVO(TrainClassRoleEntity e, String bindStatus) {
    boolean unbound = e.getPlatformUserId() == null || e.getPlatformUserId() <= 0;
    return new TrainClassRoleVO(
        String.valueOf(e.getId()),
        e.getClassId(),
        e.getRoleCode(),
        ROLE_NAMES.getOrDefault(e.getRoleCode(), e.getRoleCode()),
        e.getSubjectType(),
        e.getSubjectId(),
        e.getPlatformUserId(),
        e.getPeopleId(),
        e.getDisplayName(),
        e.getPhone(),
        e.getStatus(),
        unbound,
        bindStatus);
  }

  private void emitRoleOp(
      Long classId,
      FollowOpEventCode event,
      TrainClassRoleEntity entity,
      String summary,
      boolean warn) {
    if (classId == null || entity == null || event == null) {
      return;
    }
    try {
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("roleCode", entity.getRoleCode());
      detail.put("roleName", roleLabel(entity.getRoleCode()));
      detail.put("displayName", entity.getDisplayName());
      detail.put("subjectType", entity.getSubjectType());
      detail.put("subjectId", entity.getSubjectId());
      detail.put("peopleId", entity.getPeopleId());
      detail.put("platformUserId", entity.getPlatformUserId());
      detail.put("source", "class_role");
      opLogWriter.write(FollowOpLogCommand.of(classId, event)
          .title(event.defaultTitle())
          .summary(summary)
          .target("class_role", entity.getId())
          .severity(warn ? "warn" : "info")
          .detail(detail));
    } catch (Exception ex) {
      log.warn("[班级角色] 写过程日志失败 classId={} roleId={} err={}",
          classId, entity.getId(), ex.getMessage());
    }
  }

  private static String roleLabel(String roleCode) {
    if (!StringUtils.hasText(roleCode)) {
      return "角色";
    }
    return ROLE_NAMES.getOrDefault(roleCode, roleCode);
  }

  private static String nullToDash(String s) {
    return StringUtils.hasText(s) ? s.trim() : "—";
  }

  private static String firstNonBlank(String a, String b) {
    if (StringUtils.hasText(a)) {
      return a.trim();
    }
    if (StringUtils.hasText(b)) {
      return b.trim();
    }
    return null;
  }
}

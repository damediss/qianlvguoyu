package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamTaskExamAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.enums.TaskAuditStatus;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
import com.guoyu.platform.exam.mapper.ExamTaskExamAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskExamDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamDetailChildVO;
import com.guoyu.platform.exam.vo.ExamTaskExamDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamVO;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.exam.vo.ExpertVO;
import com.guoyu.platform.exam.vo.PublicDataVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.entity.UserRoleEntity;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端 - 题库理论审核指派 (菜单 4710)。
 * 沿用 ExamTaskXimuApplicationService 写法,key 不同点:tag 统一用 "exam"。
 */
@Service
@Slf4j
public class ExamTaskExamApplicationService {

  private static final String TAG = "exam";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ExamTaskExamMapper taskMapper;
  private final ExamTaskExamDetailMapper detailMapper;
  private final ExamTaskExamAuditRecordMapper auditMapper;
  private final ExamTaskExpertMapper expertMapper;
  private final UserMapper userMapper;
  private final UserRoleRelationMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final TaskAuditAssignHelper helper;
  private final ExamWorkTypeApplicationService workTypeService;

  public ExamTaskExamApplicationService(
      ExamTaskExamMapper taskMapper,
      ExamTaskExamDetailMapper detailMapper,
      ExamTaskExamAuditRecordMapper auditMapper,
      ExamTaskExpertMapper expertMapper,
      UserMapper userMapper,
      UserRoleRelationMapper userRoleMapper,
      RoleMapper roleMapper,
      TaskAuditAssignHelper helper,
      ExamWorkTypeApplicationService workTypeService) {
    this.taskMapper = taskMapper;
    this.detailMapper = detailMapper;
    this.auditMapper = auditMapper;
    this.expertMapper = expertMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMapper = roleMapper;
    this.helper = helper;
    this.workTypeService = workTypeService;
  }

  // ─── page ───
  // 不要 @Transactional(readOnly = true):helper.autoCompleteStatus 内部会调 UPDATE,
  // 读连接会拒绝写入并把这个事务标记成 rollback-only,然后 commit 暴露 UnexpectedRollbackException。
  @Transactional
  public PageResponse<ExamTaskExamVO> page(ExamTaskExamQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_exam page, tenant={}, page={}", tenantId, req.getPage());
    TaskAuditAssignHelper.autoCompleteStatus(tenantId, taskMapper, auditMapper);

    LambdaQueryWrapper<ExamTaskExamEntity> wrapper = new LambdaQueryWrapper<ExamTaskExamEntity>()
        .eq(ExamTaskExamEntity::getTenantId, tenantId)
        .orderByDesc(ExamTaskExamEntity::getId);
    if (req.getStatus() != null) {
      wrapper.eq(ExamTaskExamEntity::getStatus, req.getStatus());
    }
    if (req.getWorkTypeId() != null) {
      wrapper.eq(ExamTaskExamEntity::getWorkTypeId, req.getWorkTypeId());
    }
    if (req.getLevel() != null) {
      wrapper.eq(ExamTaskExamEntity::getLevel, req.getLevel());
    }

    Page<ExamTaskExamEntity> page = taskMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<ExamTaskExamEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    Set<Integer> workTypeIds = records.stream()
        .map(ExamTaskExamEntity::getWorkTypeId).collect(Collectors.toSet());
    Set<Integer> expertIds = new java.util.HashSet<>();
    records.forEach(e -> {
      if (e.getExpertStart() != null && e.getExpertStart() > 0) expertIds.add(e.getExpertStart());
      if (e.getExpertEnd() != null && e.getExpertEnd() > 0)     expertIds.add(e.getExpertEnd());
    });

    Map<Integer, String> workTypeNames = helper.loadWorkTypeNames(workTypeIds);
    Map<Integer, String> expertNames = helper.loadUserDisplayNames(expertIds);

    List<ExamTaskExamVO> vos = records.stream().map(e -> toVO(e, workTypeNames, expertNames)).toList();
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  // ─── details ───
  @Transactional(readOnly = true)
  public List<ExamTaskExamDetailVO> getDetails(Integer taskId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_exam details, tenant={}, taskId={}", tenantId, taskId);

    List<ExamTaskExamDetailEntity> details = detailMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamDetailEntity>()
            .eq(ExamTaskExamDetailEntity::getTenantId, tenantId)
            .eq(ExamTaskExamDetailEntity::getTaskId, taskId));
    List<ExamTaskExamAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
            .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamAuditRecordEntity::getTaskId, taskId));

    Map<Integer, Map<Integer, ExamTaskExamAuditRecordEntity>> startByExam = new LinkedHashMap<>();
    Map<Integer, Map<Integer, ExamTaskExamAuditRecordEntity>> endByExam = new LinkedHashMap<>();
    for (ExamTaskExamAuditRecordEntity a : audits) {
      boolean isEnd = a.getAuditRole() != null && a.getAuditRole() > 0;
      var target = isEnd ? endByExam : startByExam;
      target.computeIfAbsent(a.getExamId(), k -> new LinkedHashMap<>()).put(a.getExpertId(), a);
    }

    List<ExamTaskExamDetailVO> vos = new ArrayList<>();
    for (ExamTaskExamDetailEntity d : details) {
      ExamTaskExamDetailVO vo = toDetailVO(d);
      Map<Integer, ExamTaskExamAuditRecordEntity> sMap = startByExam.getOrDefault(d.getId(), Map.of());
      Map<Integer, ExamTaskExamAuditRecordEntity> eMap = endByExam.getOrDefault(d.getId(), Map.of());
      vo.setChildren(buildChildren(sMap, eMap));
      vos.add(vo);
    }
    return vos;
  }

  // ─── delete ───
  @Transactional
  public void delete(Integer id) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_exam delete, tenant={}, id={}", tenantId, id);
    ExamTaskExamEntity entity = findExisting(tenantId, id);
    taskMapper.deleteById(entity.getId());
    detailMapper.delete(new LambdaQueryWrapper<ExamTaskExamDetailEntity>()
        .eq(ExamTaskExamDetailEntity::getTenantId, tenantId)
        .eq(ExamTaskExamDetailEntity::getTaskId, id));
    auditMapper.delete(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
        .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
        .eq(ExamTaskExamAuditRecordEntity::getTaskId, id));
    expertMapper.delete(new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId)
        .eq(ExamTaskExpertEntity::getTaskId, id)
        .eq(ExamTaskExpertEntity::getTaskType, TAG));
  }

  // ─── assignExpert ───
  @Transactional
  public void assignExpert(ExamTaskExamAssignExpertReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("task_exam assign, tenant={}, taskId={}, expertId={}, role={}, by={}",
        tenantId, req.taskId(), req.expertId(), req.role(), userId);

    ExamTaskExamEntity entity = findExisting(tenantId, req.taskId());
    // 互斥
    if (req.role() == 1 && req.expertId().equals(entity.getExpertStart())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不能和初审是同一人");
    }
    if (req.role() == 0 && req.expertId().equals(entity.getExpertEnd())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不能和终审是同一人");
    }
    if (req.role() > 0) {
      entity.setExpertEnd(req.expertId());
    } else {
      entity.setExpertStart(req.expertId());
    }
    entity.setStatus(1);
    taskMapper.updateById(entity);

    // 查重
    if (expertMapper.exists(new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId)
        .eq(ExamTaskExpertEntity::getTaskId, req.taskId())
        .eq(ExamTaskExpertEntity::getTaskType, TAG)
        .eq(ExamTaskExpertEntity::getExpertId, req.expertId())
        .eq(ExamTaskExpertEntity::getTaskRole, req.role()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已经指派过了");
    }

    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setTenantId(tenantId);
    rec.setTaskId(req.taskId());
    rec.setTaskType(TAG);
    rec.setExpertId(req.expertId());
    rec.setTaskRole(req.role());
    rec.setTag(TAG);
    rec.setAddtime(LocalDateTime.now());
    rec.setCreatedBy(userId.intValue());
    rec.setUpdatedBy(userId.intValue());
    expertMapper.insert(rec);
  }

  // ─── daily ───
  @Transactional(readOnly = true)
  public List<ExamTaskExamDailyVO> getDaily(Integer taskId, Integer role) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_exam daily, tenant={}, taskId={}, role={}", tenantId, taskId, role);
    // 注:不再用 .select(getYmd) — 该方法会触发 MyBatis-Plus lambda 缓存,
    // 在 test 环境下不可用。改为全字段拉取后在 Java 端聚合,数据量不大,无明显性能影响。
    List<ExamTaskExamAuditRecordEntity> records = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
            .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
    Map<String, Long> cmap = records.stream()
        .collect(Collectors.groupingBy(ExamTaskExamAuditRecordEntity::getYmd, LinkedHashMap::new, Collectors.counting()));
    List<ExamTaskExamDailyVO> out = new ArrayList<>();
    cmap.forEach((k, v) -> {
      ExamTaskExamDailyVO vo = new ExamTaskExamDailyVO();
      vo.setYmd(k);
      vo.setNums(v);
      out.add(vo);
    });
    return out;
  }

  // ─── publicData ───
  @Transactional(readOnly = true)
  public PublicDataVO getPublicData() {
    log.info("task_exam public-data");
    PublicDataVO vo = new PublicDataVO();
    vo.setWorkTypeList(loadWorkTypeTree());
    vo.setExpertList(loadExpertList());
    return vo;
  }

  // ─── helpers ───
  private List<ExamWorkTypeVO> loadWorkTypeTree() {
    return workTypeService.tree();
  }

  private List<ExpertVO> loadExpertList() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getCode, "EXPERT")
        .eq(RoleEntity::getStatus, "enabled"));
    if (role == null) return List.of();
    List<UserRoleEntity> userRoles = userRoleMapper.selectList(
        new LambdaQueryWrapper<UserRoleEntity>()
            .eq(UserRoleEntity::getTenantId, tenantId)
            .eq(UserRoleEntity::getRoleId, role.getId()));
    if (userRoles.isEmpty()) return List.of();
    Set<Long> uids = userRoles.stream().map(UserRoleEntity::getUserId).collect(Collectors.toSet());
    List<UserEntity> users = userMapper.selectBatchIds(uids);
    return users.stream()
        .filter(u -> "enabled".equals(u.getStatus()))
        .map(u -> new ExpertVO(u.getId().intValue(), u.getDisplayName()))
        .collect(Collectors.toList());
  }

  private ExamTaskExamEntity findExisting(Integer tenantId, Integer id) {
    ExamTaskExamEntity e = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamEntity>()
            .eq(ExamTaskExamEntity::getTenantId, tenantId)
            .eq(ExamTaskExamEntity::getId, id));
    if (e == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }
    return e;
  }

  static ExamTaskExamVO toVO(ExamTaskExamEntity e,
                             Map<Integer, String> workTypeNames,
                             Map<Integer, String> expertNames) {
    ExamTaskExamVO vo = new ExamTaskExamVO();
    vo.setId(e.getId());
    vo.setWorkTypeId(e.getWorkTypeId());
    vo.setWorkTypeCn(workTypeNames.getOrDefault(e.getWorkTypeId(), "-"));
    vo.setLevel(e.getLevel());
    int lvl = e.getLevel() != null ? e.getLevel() : 0;
    vo.setLevelCn(lvl >= 1 && lvl < LEVELS.length ? LEVELS[lvl] : "-");
    vo.setNums(e.getNums());
    vo.setStatus(e.getStatus());
    vo.setStatusCn(TaskAuditStatus.cnOf(e.getStatus()));
    vo.setExpertStart(e.getExpertStart());
    vo.setExpertStartName(e.getExpertStart() != null && e.getExpertStart() > 0
        ? expertNames.getOrDefault(e.getExpertStart(), "***") : "待指派");
    vo.setExpertEnd(e.getExpertEnd());
    vo.setExpertEndName(e.getExpertEnd() != null && e.getExpertEnd() > 0
        ? expertNames.getOrDefault(e.getExpertEnd(), "***") : "待指派");
    vo.setFilePath(e.getFilePath());
    vo.setAddtime(e.getAddtime());
    vo.setAddtimeCn(e.getAddtime() != null ? e.getAddtime().format(DATE_FMT) : "-");
    return vo;
  }

  static ExamTaskExamDetailVO toDetailVO(ExamTaskExamDetailEntity d) {
    ExamTaskExamDetailVO vo = new ExamTaskExamDetailVO();
    vo.setId(d.getId());
    vo.setTaskId(d.getTaskId());
    vo.setXingwei(d.getXingwei());
    vo.setKaohe1(d.getKaohe1());
    vo.setKaohe2(d.getKaohe2());
    vo.setKaohe3(d.getKaohe3());
    vo.setKaohe4(d.getKaohe4());
    vo.setJianding(d.getJianding());
    vo.setLevel(d.getLevel());
    vo.setNanyicd(d.getNanyicd());
    vo.setExamType(d.getExamType());
    vo.setExamContent(d.getExamContent());
    vo.setExamA(d.getExamA());
    vo.setExamB(d.getExamB());
    vo.setExamC(d.getExamC());
    vo.setExamD(d.getExamD());
    vo.setExamE(d.getExamE());
    vo.setExamF(d.getExamF());
    vo.setExamG(d.getExamG());
    vo.setExamH(d.getExamH());
    vo.setExamAnswer(d.getExamAnswer());
    vo.setIsblod(d.getIsblod());
    return vo;
  }

  static List<ExamTaskExamDetailChildVO> buildChildren(
      Map<Integer, ExamTaskExamAuditRecordEntity> sMap,
      Map<Integer, ExamTaskExamAuditRecordEntity> eMap) {
    List<ExamTaskExamDetailChildVO> out = new ArrayList<>();
    ExamTaskExamAuditRecordEntity s = sMap.isEmpty() ? null : sMap.values().iterator().next();
    ExamTaskExamAuditRecordEntity en = eMap.isEmpty() ? null : eMap.values().iterator().next();
    ExamTaskExamDetailChildVO sc = new ExamTaskExamDetailChildVO();
    sc.setId("初审");
    if (s != null) {
      sc.setExamId(s.getExamId());
      sc.setExamContent(s.getExamContent());
      sc.setExamAnswer(s.getExamAnswer());
    }
    out.add(sc);
    ExamTaskExamDetailChildVO ec = new ExamTaskExamDetailChildVO();
    ec.setId("终审");
    if (en != null) {
      ec.setExamId(en.getExamId());
      ec.setExamContent(en.getExamContent());
      ec.setExamAnswer(en.getExamAnswer());
    }
    out.add(ec);
    return out;
  }
}

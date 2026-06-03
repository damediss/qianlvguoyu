package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamTaskExamsubAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamsubQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.enums.TaskAuditStatus;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskExamsubDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamsubDetailChildVO;
import com.guoyu.platform.exam.vo.ExamTaskExamsubDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamsubVO;
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
 * 管理端 - 题库主观审核指派 (菜单 4712)。tag = "examsub"。
 */
@Service
@Slf4j
public class ExamTaskExamsubApplicationService {

  private static final String TAG = "examsub";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ExamTaskExamsubMapper taskMapper;
  private final ExamTaskExamsubDetailMapper detailMapper;
  private final ExamTaskExamsubAuditRecordMapper auditMapper;
  private final ExamTaskExpertMapper expertMapper;
  private final UserMapper userMapper;
  private final UserRoleRelationMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final TaskAuditAssignHelper helper;
  private final ExamWorkTypeApplicationService workTypeService;

  public ExamTaskExamsubApplicationService(
      ExamTaskExamsubMapper taskMapper,
      ExamTaskExamsubDetailMapper detailMapper,
      ExamTaskExamsubAuditRecordMapper auditMapper,
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

  // 不要 @Transactional(readOnly = true):helper 会做 UPDATE
  @Transactional
  public PageResponse<ExamTaskExamsubVO> page(ExamTaskExamsubQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examsub page, tenant={}, page={}", tenantId, req.getPage());
    TaskAuditAssignHelper.autoCompleteStatusExamsub(tenantId, taskMapper, auditMapper);

    LambdaQueryWrapper<ExamTaskExamsubEntity> wrapper = new LambdaQueryWrapper<ExamTaskExamsubEntity>()
        .eq(ExamTaskExamsubEntity::getTenantId, tenantId)
        .orderByDesc(ExamTaskExamsubEntity::getId);
    if (req.getStatus() != null) wrapper.eq(ExamTaskExamsubEntity::getStatus, req.getStatus());
    if (req.getWorkTypeId() != null) wrapper.eq(ExamTaskExamsubEntity::getWorkTypeId, req.getWorkTypeId());
    if (req.getLevel() != null) wrapper.eq(ExamTaskExamsubEntity::getLevel, req.getLevel());

    Page<ExamTaskExamsubEntity> page = taskMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<ExamTaskExamsubEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }
    Set<Integer> wtIds = records.stream().map(ExamTaskExamsubEntity::getWorkTypeId).collect(Collectors.toSet());
    Set<Integer> eIds = new java.util.HashSet<>();
    records.forEach(e -> {
      if (e.getExpertStart() != null && e.getExpertStart() > 0) eIds.add(e.getExpertStart());
      if (e.getExpertEnd() != null && e.getExpertEnd() > 0)     eIds.add(e.getExpertEnd());
    });
    Map<Integer, String> wtNames = helper.loadWorkTypeNames(wtIds);
    Map<Integer, String> exNames = helper.loadUserDisplayNames(eIds);

    List<ExamTaskExamsubVO> vos = records.stream().map(e -> toVO(e, wtNames, exNames)).toList();
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<ExamTaskExamsubDetailVO> getDetails(Integer taskId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examsub details, tenant={}, taskId={}", tenantId, taskId);
    List<ExamTaskExamsubDetailEntity> details = detailMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubDetailEntity>()
            .eq(ExamTaskExamsubDetailEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubDetailEntity::getTaskId, taskId));
    List<ExamTaskExamsubAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
            .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, taskId));

    Map<Integer, Map<Integer, ExamTaskExamsubAuditRecordEntity>> sMap = new LinkedHashMap<>();
    Map<Integer, Map<Integer, ExamTaskExamsubAuditRecordEntity>> eMap = new LinkedHashMap<>();
    for (ExamTaskExamsubAuditRecordEntity a : audits) {
      boolean isEnd = a.getAuditRole() != null && a.getAuditRole() > 0;
      var target = isEnd ? eMap : sMap;
      target.computeIfAbsent(a.getExamId(), k -> new LinkedHashMap<>()).put(a.getExpertId(), a);
    }
    List<ExamTaskExamsubDetailVO> vos = new ArrayList<>();
    for (ExamTaskExamsubDetailEntity d : details) {
      ExamTaskExamsubDetailVO vo = toDetailVO(d);
      vo.setChildren(buildChildren(sMap.getOrDefault(d.getId(), Map.of()), eMap.getOrDefault(d.getId(), Map.of())));
      vos.add(vo);
    }
    return vos;
  }

  @Transactional
  public void delete(Integer id) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examsub delete, tenant={}, id={}", tenantId, id);
    ExamTaskExamsubEntity entity = findExisting(tenantId, id);
    taskMapper.deleteById(entity.getId());
    detailMapper.delete(new LambdaQueryWrapper<ExamTaskExamsubDetailEntity>()
        .eq(ExamTaskExamsubDetailEntity::getTenantId, tenantId)
        .eq(ExamTaskExamsubDetailEntity::getTaskId, id));
    auditMapper.delete(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
        .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
        .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, id));
    expertMapper.delete(new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId)
        .eq(ExamTaskExpertEntity::getTaskId, id)
        .eq(ExamTaskExpertEntity::getTaskType, TAG));
  }

  @Transactional
  public void assignExpert(ExamTaskExamsubAssignExpertReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("task_examsub assign, tenant={}, taskId={}, expertId={}, role={}, by={}",
        tenantId, req.taskId(), req.expertId(), req.role(), userId);

    ExamTaskExamsubEntity entity = findExisting(tenantId, req.taskId());
    if (req.role() == 1 && req.expertId().equals(entity.getExpertStart())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不能和初审是同一人");
    }
    if (req.role() == 0 && req.expertId().equals(entity.getExpertEnd())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不能和终审是同一人");
    }
    if (req.role() > 0) entity.setExpertEnd(req.expertId());
    else entity.setExpertStart(req.expertId());
    entity.setStatus(1);
    taskMapper.updateById(entity);

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

  @Transactional(readOnly = true)
  public List<ExamTaskExamsubDailyVO> getDaily(Integer taskId, Integer role) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examsub daily, tenant={}, taskId={}, role={}", tenantId, taskId, role);
    List<ExamTaskExamsubAuditRecordEntity> records = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
            .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
    Map<String, Long> cmap = records.stream()
        .collect(Collectors.groupingBy(ExamTaskExamsubAuditRecordEntity::getYmd, LinkedHashMap::new, Collectors.counting()));
    List<ExamTaskExamsubDailyVO> out = new ArrayList<>();
    cmap.forEach((k, v) -> {
      ExamTaskExamsubDailyVO vo = new ExamTaskExamsubDailyVO();
      vo.setYmd(k);
      vo.setNums(v);
      out.add(vo);
    });
    return out;
  }

  @Transactional(readOnly = true)
  public PublicDataVO getPublicData() {
    PublicDataVO vo = new PublicDataVO();
    vo.setWorkTypeList(workTypeService.tree());
    vo.setExpertList(loadExpertList());
    return vo;
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

  private ExamTaskExamsubEntity findExisting(Integer tenantId, Integer id) {
    ExamTaskExamsubEntity e = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamsubEntity>()
            .eq(ExamTaskExamsubEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubEntity::getId, id));
    if (e == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    return e;
  }

  static ExamTaskExamsubVO toVO(ExamTaskExamsubEntity e,
                                Map<Integer, String> wtNames,
                                Map<Integer, String> exNames) {
    ExamTaskExamsubVO vo = new ExamTaskExamsubVO();
    vo.setId(e.getId());
    vo.setWorkTypeId(e.getWorkTypeId());
    vo.setWorkTypeCn(wtNames.getOrDefault(e.getWorkTypeId(), "-"));
    vo.setLevel(e.getLevel());
    int lvl = e.getLevel() != null ? e.getLevel() : 0;
    vo.setLevelCn(lvl >= 1 && lvl < LEVELS.length ? LEVELS[lvl] : "-");
    vo.setNums(e.getNums());
    vo.setStatus(e.getStatus());
    vo.setStatusCn(TaskAuditStatus.cnOf(e.getStatus()));
    vo.setExpertStart(e.getExpertStart());
    vo.setExpertStartName(e.getExpertStart() != null && e.getExpertStart() > 0
        ? exNames.getOrDefault(e.getExpertStart(), "***") : "待指派");
    vo.setExpertEnd(e.getExpertEnd());
    vo.setExpertEndName(e.getExpertEnd() != null && e.getExpertEnd() > 0
        ? exNames.getOrDefault(e.getExpertEnd(), "***") : "待指派");
    vo.setFilePath(e.getFilePath());
    vo.setAddtime(e.getAddtime());
    vo.setAddtimeCn(e.getAddtime() != null ? e.getAddtime().format(DATE_FMT) : "-");
    return vo;
  }

  static ExamTaskExamsubDetailVO toDetailVO(ExamTaskExamsubDetailEntity d) {
    ExamTaskExamsubDetailVO vo = new ExamTaskExamsubDetailVO();
    vo.setId(d.getId());
    vo.setTaskId(d.getTaskId());
    vo.setZhishidian(d.getZhishidian());
    vo.setContent(d.getContent());
    vo.setAnswer(d.getAnswer());
    vo.setIsJisuan(d.getIsJisuan());
    vo.setNanyicd(d.getNanyicd());
    vo.setIsblod(d.getIsblod());
    vo.setPic(d.getPic());
    return vo;
  }

  static List<ExamTaskExamsubDetailChildVO> buildChildren(
      Map<Integer, ExamTaskExamsubAuditRecordEntity> sMap,
      Map<Integer, ExamTaskExamsubAuditRecordEntity> eMap) {
    List<ExamTaskExamsubDetailChildVO> out = new ArrayList<>();
    ExamTaskExamsubAuditRecordEntity s = sMap.isEmpty() ? null : sMap.values().iterator().next();
    ExamTaskExamsubAuditRecordEntity en = eMap.isEmpty() ? null : eMap.values().iterator().next();
    ExamTaskExamsubDetailChildVO sc = new ExamTaskExamsubDetailChildVO();
    sc.setId("初审");
    if (s != null) {
      sc.setExamId(s.getExamId());
      sc.setContent(s.getContent());
      sc.setAnswer(s.getAnswer());
      sc.setPic(s.getPic());
    }
    out.add(sc);
    ExamTaskExamsubDetailChildVO ec = new ExamTaskExamsubDetailChildVO();
    ec.setId("终审");
    if (en != null) {
      ec.setExamId(en.getExamId());
      ec.setContent(en.getContent());
      ec.setAnswer(en.getAnswer());
      ec.setPic(en.getPic());
    }
    out.add(ec);
    return out;
  }
}

package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.enums.TaskAuditStatus;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDetailChildVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoVO;
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
 * 管理端 - 题库实操审核指派 (菜单 4711)。tag = "examshicao"。
 */
@Service
@Slf4j
public class ExamTaskExamshicaoApplicationService {

  private static final String TAG = "examshicao";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ExamTaskExamshicaoMapper taskMapper;
  private final ExamTaskExamshicaoDetailMapper detailMapper;
  private final ExamTaskExamshicaoAuditRecordMapper auditMapper;
  private final ExamTaskExpertMapper expertMapper;
  private final UserMapper userMapper;
  private final UserRoleRelationMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final TaskAuditAssignHelper helper;
  private final ExamWorkTypeApplicationService workTypeService;

  public ExamTaskExamshicaoApplicationService(
      ExamTaskExamshicaoMapper taskMapper,
      ExamTaskExamshicaoDetailMapper detailMapper,
      ExamTaskExamshicaoAuditRecordMapper auditMapper,
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
  public PageResponse<ExamTaskExamshicaoVO> page(ExamTaskExamshicaoQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examshicao page, tenant={}, page={}", tenantId, req.getPage());
    TaskAuditAssignHelper.autoCompleteStatusExamshicao(tenantId, taskMapper, auditMapper);

    LambdaQueryWrapper<ExamTaskExamshicaoEntity> wrapper = new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
        .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
        .orderByDesc(ExamTaskExamshicaoEntity::getId);
    if (req.getStatus() != null) wrapper.eq(ExamTaskExamshicaoEntity::getStatus, req.getStatus());
    if (req.getWorkTypeId() != null) wrapper.eq(ExamTaskExamshicaoEntity::getWorkTypeId, req.getWorkTypeId());
    if (req.getLevel() != null) wrapper.eq(ExamTaskExamshicaoEntity::getLevel, req.getLevel());

    Page<ExamTaskExamshicaoEntity> page = taskMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<ExamTaskExamshicaoEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }
    Set<Integer> wtIds = records.stream().map(ExamTaskExamshicaoEntity::getWorkTypeId).collect(Collectors.toSet());
    Set<Integer> eIds = new java.util.HashSet<>();
    records.forEach(e -> {
      if (e.getExpertStart() != null && e.getExpertStart() > 0) eIds.add(e.getExpertStart());
      if (e.getExpertEnd() != null && e.getExpertEnd() > 0)     eIds.add(e.getExpertEnd());
    });
    Map<Integer, String> wtNames = helper.loadWorkTypeNames(wtIds);
    Map<Integer, String> exNames = helper.loadUserDisplayNames(eIds);

    List<ExamTaskExamshicaoVO> vos = records.stream().map(e -> toVO(e, wtNames, exNames)).toList();
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<ExamTaskExamshicaoDetailVO> getDetails(Integer taskId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examshicao details, tenant={}, taskId={}", tenantId, taskId);
    List<ExamTaskExamshicaoDetailEntity> details = detailMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
            .eq(ExamTaskExamshicaoDetailEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoDetailEntity::getTaskId, taskId));
    List<ExamTaskExamshicaoAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId));

    Map<Integer, Map<Integer, ExamTaskExamshicaoAuditRecordEntity>> sMap = new LinkedHashMap<>();
    Map<Integer, Map<Integer, ExamTaskExamshicaoAuditRecordEntity>> eMap = new LinkedHashMap<>();
    for (ExamTaskExamshicaoAuditRecordEntity a : audits) {
      boolean isEnd = a.getAuditRole() != null && a.getAuditRole() > 0;
      var target = isEnd ? eMap : sMap;
      target.computeIfAbsent(a.getExamId(), k -> new LinkedHashMap<>()).put(a.getExpertId(), a);
    }
    List<ExamTaskExamshicaoDetailVO> vos = new ArrayList<>();
    for (ExamTaskExamshicaoDetailEntity d : details) {
      ExamTaskExamshicaoDetailVO vo = toDetailVO(d);
      vo.setChildren(buildChildren(sMap.getOrDefault(d.getId(), Map.of()), eMap.getOrDefault(d.getId(), Map.of())));
      vos.add(vo);
    }
    return vos;
  }

  @Transactional
  public void delete(Integer id) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examshicao delete, tenant={}, id={}", tenantId, id);
    ExamTaskExamshicaoEntity entity = findExisting(tenantId, id);
    taskMapper.deleteById(entity.getId());
    detailMapper.delete(new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
        .eq(ExamTaskExamshicaoDetailEntity::getTenantId, tenantId)
        .eq(ExamTaskExamshicaoDetailEntity::getTaskId, id));
    auditMapper.delete(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
        .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
        .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, id));
    expertMapper.delete(new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId)
        .eq(ExamTaskExpertEntity::getTaskId, id)
        .eq(ExamTaskExpertEntity::getTaskType, TAG));
  }

  @Transactional
  public void assignExpert(ExamTaskExamshicaoAssignExpertReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("task_examshicao assign, tenant={}, taskId={}, expertId={}, role={}, by={}",
        tenantId, req.taskId(), req.expertId(), req.role(), userId);

    ExamTaskExamshicaoEntity entity = findExisting(tenantId, req.taskId());
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
  public List<ExamTaskExamshicaoDailyVO> getDaily(Integer taskId, Integer role) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examshicao daily, tenant={}, taskId={}, role={}", tenantId, taskId, role);
    List<ExamTaskExamshicaoAuditRecordEntity> records = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
    Map<String, Long> cmap = records.stream()
        .collect(Collectors.groupingBy(ExamTaskExamshicaoAuditRecordEntity::getYmd, LinkedHashMap::new, Collectors.counting()));
    List<ExamTaskExamshicaoDailyVO> out = new ArrayList<>();
    cmap.forEach((k, v) -> {
      ExamTaskExamshicaoDailyVO vo = new ExamTaskExamshicaoDailyVO();
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

  // ─── helpers ───
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

  private ExamTaskExamshicaoEntity findExisting(Integer tenantId, Integer id) {
    ExamTaskExamshicaoEntity e = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
            .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoEntity::getId, id));
    if (e == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    return e;
  }

  static ExamTaskExamshicaoVO toVO(ExamTaskExamshicaoEntity e,
                                   Map<Integer, String> wtNames,
                                   Map<Integer, String> exNames) {
    ExamTaskExamshicaoVO vo = new ExamTaskExamshicaoVO();
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

  static ExamTaskExamshicaoDetailVO toDetailVO(ExamTaskExamshicaoDetailEntity d) {
    ExamTaskExamshicaoDetailVO vo = new ExamTaskExamshicaoDetailVO();
    vo.setId(d.getId());
    vo.setTaskId(d.getTaskId());
    vo.setXingwei(d.getXingwei());
    vo.setKaohe1(d.getKaohe1());
    vo.setKaohe2(d.getKaohe2());
    vo.setKaohe3(d.getKaohe3());
    vo.setKaohe4(d.getKaohe4());
    vo.setJianding(d.getJianding());
    vo.setNanyicd(d.getNanyicd());
    vo.setContent(d.getContent());
    vo.setPingfen(d.getPingfen());
    vo.setKaochang(d.getKaochang());
    vo.setKaosheng(d.getKaosheng());
    vo.setIsblod(d.getIsblod());
    return vo;
  }

  static List<ExamTaskExamshicaoDetailChildVO> buildChildren(
      Map<Integer, ExamTaskExamshicaoAuditRecordEntity> sMap,
      Map<Integer, ExamTaskExamshicaoAuditRecordEntity> eMap) {
    List<ExamTaskExamshicaoDetailChildVO> out = new ArrayList<>();
    ExamTaskExamshicaoAuditRecordEntity s = sMap.isEmpty() ? null : sMap.values().iterator().next();
    ExamTaskExamshicaoAuditRecordEntity en = eMap.isEmpty() ? null : eMap.values().iterator().next();
    ExamTaskExamshicaoDetailChildVO sc = new ExamTaskExamshicaoDetailChildVO();
    sc.setId("初审");
    if (s != null) {
      sc.setExamId(s.getExamId());
      sc.setContent(s.getContent());
      sc.setPingfen(s.getPingfen());
      sc.setKaochang(s.getKaochang());
      sc.setKaosheng(s.getKaosheng());
    }
    out.add(sc);
    ExamTaskExamshicaoDetailChildVO ec = new ExamTaskExamshicaoDetailChildVO();
    ec.setId("终审");
    if (en != null) {
      ec.setExamId(en.getExamId());
      ec.setContent(en.getContent());
      ec.setPingfen(en.getPingfen());
      ec.setKaochang(en.getKaochang());
      ec.setKaosheng(en.getKaosheng());
    }
    out.add(ec);
    return out;
  }
}

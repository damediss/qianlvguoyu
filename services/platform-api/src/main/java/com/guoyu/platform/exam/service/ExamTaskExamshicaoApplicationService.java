package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.guoyu.platform.exam.vo.ExportResultDTO;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
  private final ObjectMapper objectMapper;

  // 为保留旧版数据,id>41 之前的是老数据,新导入要避开。
  private static final int LEGACY_TASK_ID_LIMIT = 41;

  @Value("${platform.cdn.path:./data/cdn}")
  private String cdnPath;
  @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}")
  private String cdnUrl;

  public ExamTaskExamshicaoApplicationService(
      ExamTaskExamshicaoMapper taskMapper,
      ExamTaskExamshicaoDetailMapper detailMapper,
      ExamTaskExamshicaoAuditRecordMapper auditMapper,
      ExamTaskExpertMapper expertMapper,
      UserMapper userMapper,
      UserRoleRelationMapper userRoleMapper,
      RoleMapper roleMapper,
      TaskAuditAssignHelper helper,
      ExamWorkTypeApplicationService workTypeService,
      ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.detailMapper = detailMapper;
    this.auditMapper = auditMapper;
    this.expertMapper = expertMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMapper = roleMapper;
    this.helper = helper;
    this.workTypeService = workTypeService;
    this.objectMapper = objectMapper;
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
    Set<Integer> expertIds = new HashSet<>();
    for (ExamTaskExamshicaoAuditRecordEntity a : audits) {
      boolean isEnd = a.getAuditRole() != null && a.getAuditRole() > 0;
      var target = isEnd ? eMap : sMap;
      target.computeIfAbsent(a.getExamId(), k -> new LinkedHashMap<>()).put(a.getExpertId(), a);
      if (a.getExpertId() != null) expertIds.add(a.getExpertId());
    }
    Map<Integer, String> expertNames = helper.loadUserDisplayNames(expertIds);

    List<ExamTaskExamshicaoDetailVO> vos = new ArrayList<>();
    for (ExamTaskExamshicaoDetailEntity d : details) {
      ExamTaskExamshicaoDetailVO vo = toDetailVO(d);
      vo.setChildren(buildChildren(
          sMap.getOrDefault(d.getId(), Map.of()),
          eMap.getOrDefault(d.getId(), Map.of()),
          expertNames));
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

  // ─── importExcel ───
  // 对应老 PHP /yuntiku/upload?type=examshicao → examshicaoUpload()
  // Excel 列:0 鉴定点 / 1 题目内容 / 2 考场准备 / 3 考生准备 / 4 评分要素 / 5 配分 / 6 评分标准 / 7 扣分 / 8 补偿
  // 多行评分要素可挂在同一鉴定点下,会被合并为 pingfen JSON 数组。
  // 查重要带 id>41 限制(老 PHP 行为),防止跟老版数据冲突。
  @Transactional
  public int importExcel(MultipartFile file, Integer workTypeId, Integer level) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("task_examshicao import, tenant={}, workTypeId={}, level={}, by={}",
        tenantId, workTypeId, level, userId);

    if (workTypeId == null || workTypeId <= 0 || level == null || level <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择工种和等级");
    }

    // 查重:PHP 老逻辑是 work_type_id + level + id>41(老 id<=41 的数据不算)
    LambdaQueryWrapper<ExamTaskExamshicaoEntity> dup = new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
        .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
        .eq(ExamTaskExamshicaoEntity::getWorkTypeId, workTypeId)
        .eq(ExamTaskExamshicaoEntity::getLevel, level)
        .gt(ExamTaskExamshicaoEntity::getId, LEGACY_TASK_ID_LIMIT);
    if (taskMapper.exists(dup)) {
      throw new BusinessException(ErrorCode.CONFLICT, "该工种等级已存在");
    }

    String filePath;
    try {
      filePath = saveFile(file, tenantId);
    } catch (IOException e) {
      log.error("Failed to save uploaded file", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
    }

    Path fullPath = Paths.get(cdnPath, filePath);
    List<List<String>> rows;
    try (InputStream is = Files.newInputStream(fullPath)) {
      Workbook workbook = WorkbookFactory.create(is);
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter fmt = new DataFormatter();
      int lastRow = sheet.getLastRowNum();
      rows = new ArrayList<>();
      for (int i = 0; i <= lastRow; i++) {
        Row row = sheet.getRow(i);
        if (row == null) { rows.add(List.of()); continue; }
        List<String> cells = new ArrayList<>();
        int lastCol = row.getLastCellNum();
        for (int j = 0; j < lastCol; j++) {
          var cell = row.getCell(j);
          cells.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
        }
        rows.add(cells);
      }
      workbook.close();
    } catch (BusinessException e) {
      delSilent(fullPath);
      throw e;
    } catch (Exception e) {
      delSilent(fullPath);
      log.error("Excel parse failed", e);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel解析失败: " + e.getMessage());
    }

    // 创建任务(参考 PHP:先 insertRow,拿到 task_id 再写 details,最后 updateRow nums)
    ExamTaskExamshicaoEntity task = new ExamTaskExamshicaoEntity();
    task.setTenantId(tenantId);
    task.setWorkTypeId(workTypeId);
    task.setLevel(level);
    task.setNums(0);
    task.setStatus(0);
    task.setFilePath(filePath);
    task.setAddtime((int) (System.currentTimeMillis() / 1000));
    task.setDeleted(0);
    taskMapper.insert(task);

    // 解析 rows(行 0 是表头,跳过)
    // 重构:同一鉴定点下的多行评分数据合并到一个 pingfen JSON 数组中(PHP 行为)
    int count = 0;
    int rowIdx = 0;
    String lastJianding = null;
    List<LinkedHashMap<String, String>> pendingPingfen = null; // 当前鉴定点累积的评分项
    ExamTaskExamshicaoDetailEntity currentDetail = null;       // 当前鉴定点的 detail 记录

    for (List<String> el : rows) {
      rowIdx++;
      if (rowIdx == 1) continue;                                   // skip header
      if (el.isEmpty() || el.stream().allMatch(String::isEmpty)) continue;

      String jianding = cell(el, 0);
      String content  = cell(el, 1);
      String kaochang = cell(el, 2);
      String kaosheng = cell(el, 3);
      String yaosu    = cell(el, 4);
      String peifen   = cell(el, 5);
      String biaozhun = cell(el, 6);
      String koufen   = cell(el, 7);
      String buchang  = cell(el, 8);

      if (!jianding.isEmpty()) {
        // 新鉴定点:先 flush 上一个鉴定点的 detail
        flushDetail(currentDetail, pendingPingfen);

        // content / yaosu / peifen / biaozhun / koufen / buchang 都必须有效
        if (content.isEmpty() || yaosu.isEmpty() || parseIntOrZero(peifen) == 0
            || biaozhun.isEmpty() || parseIntOrZero(koufen) == 0 || parseIntOrZero(buchang) == 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "第" + rowIdx + "行数据有列为空或为0");
        }
        lastJianding = jianding;
        pendingPingfen = new ArrayList<>();
        pendingPingfen.add(buildPingfenItem(yaosu, peifen, biaozhun, koufen, buchang));

        currentDetail = new ExamTaskExamshicaoDetailEntity();
        currentDetail.setTenantId(tenantId);
        currentDetail.setTaskId(task.getId());
        currentDetail.setJianding(jianding);
        currentDetail.setContent(content);
        currentDetail.setKaochang(kaochang);
        currentDetail.setKaosheng(kaosheng);
        currentDetail.setDeleted(0);
        count++;
      } else {
        // 续行:yaosu 空表示文件结束(PHP 老逻辑 break)
        if (yaosu.isEmpty()) break;
        // yaosu / peifen / biaozhun / koufen / buchang 必须有效
        if (parseIntOrZero(peifen) == 0 || biaozhun.isEmpty()
            || parseIntOrZero(koufen) == 0 || parseIntOrZero(buchang) == 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "第" + rowIdx + "行数据有列为空或为0");
        }
        if (lastJianding == null || pendingPingfen == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "第" + rowIdx + "行缺少鉴定点,请检查数据");
        }
        // 追加评分项到当前鉴定点的 pingfen 数组
        pendingPingfen.add(buildPingfenItem(yaosu, peifen, biaozhun, koufen, buchang));
      }
    }

    // flush 最后一个鉴定点
    flushDetail(currentDetail, pendingPingfen);

    if (count > 0) {
      task.setNums(count);
      taskMapper.updateById(task);
    }
    log.info("task_examshicao import done, taskId={}, count={}", task.getId(), count);
    return count;
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

  ExamTaskExamshicaoVO toVO(ExamTaskExamshicaoEntity e,
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
    if (e.getFilePath() != null && !e.getFilePath().isEmpty()) {
      vo.setFileUrl(cdnUrl + "/" + e.getFilePath());
      String ext = e.getFilePath();
      int dotIdx = ext.lastIndexOf('.');
      if (dotIdx >= 0) {
        ext = ext.substring(dotIdx + 1).toLowerCase();
        if ("xls".equals(ext)) ext = "xlsx";
        vo.setFileType(ext);
      }
    }
    vo.setAddtime(e.getAddtime());
    vo.setAddtimeCn(e.getAddtime() != null
        ? ExamTaskExamApplicationService.toLocalDateTime(e.getAddtime()).format(DATE_FMT) : "-");
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
      Map<Integer, ExamTaskExamshicaoAuditRecordEntity> eMap,
      Map<Integer, String> expertNames) {
    List<ExamTaskExamshicaoDetailChildVO> out = new ArrayList<>();
    ExamTaskExamshicaoAuditRecordEntity s = sMap.isEmpty() ? null : sMap.values().iterator().next();
    ExamTaskExamshicaoAuditRecordEntity en = eMap.isEmpty() ? null : eMap.values().iterator().next();
    ExamTaskExamshicaoDetailChildVO sc = new ExamTaskExamshicaoDetailChildVO();
    sc.setId("初审");
    if (s != null) {
      sc.setExamId(s.getExamId());
      sc.setExpertId(s.getExpertId());
      sc.setExpertName(s.getExpertId() != null ? expertNames.getOrDefault(s.getExpertId(), "-") : null);
      sc.setAddtimeCn(formatAddtime(s.getAddtime()));
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
      ec.setExpertId(en.getExpertId());
      ec.setExpertName(en.getExpertId() != null ? expertNames.getOrDefault(en.getExpertId(), "-") : null);
      ec.setAddtimeCn(formatAddtime(en.getAddtime()));
      ec.setContent(en.getContent());
      ec.setPingfen(en.getPingfen());
      ec.setKaochang(en.getKaochang());
      ec.setKaosheng(en.getKaosheng());
    }
    out.add(ec);
    return out;
  }

  // 复用于 3 个 buildChildren:addtime(Unix 秒,PHP 口径) → yyyy-MM-dd HH:mm:ss
  static String formatAddtime(Integer epoch) {
    java.time.LocalDateTime t = ExamTaskExamApplicationService.toLocalDateTime(epoch);
    if (t == null) return null;
    return t.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  // ─── import helpers ───
  private static String cell(List<String> row, int idx) {
    return idx < row.size() ? row.get(idx) : "";
  }

  private static Integer parseIntOrNull(String s) {
    if (s == null || s.isEmpty()) return null;
    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int parseIntOrZero(String s) {
    Integer v = parseIntOrNull(s);
    return v == null ? 0 : v;
  }

  /**
   * 构建单个评分项的 Map(不序列化,等待累积完成后统一序列化)
   */
  private static LinkedHashMap<String, String> buildPingfenItem(
      String yaosu, String peifen, String biaozhun,
      String koufen, String buchang) {
    LinkedHashMap<String, String> item = new LinkedHashMap<>();
    item.put("yaosu", yaosu);
    item.put("peifen", peifen);
    item.put("biaozhun", biaozhun);
    item.put("koufen", koufen);
    item.put("buchang", buchang);
    return item;
  }

  /**
   * 把累积的评分项列表序列化为 JSON 并写入 detail 记录,然后 insert 入库。
   * 与老 PHP json_encode(..., JSON_UNESCAPED_UNICODE) 兼容。
   */
  private void flushDetail(ExamTaskExamshicaoDetailEntity detail,
                           List<LinkedHashMap<String, String>> pingfenItems) {
    if (detail == null || pingfenItems == null || pingfenItems.isEmpty()) return;
    try {
      detail.setPingfen(objectMapper.writeValueAsString(pingfenItems));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize pingfen JSON", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分标准序列化失败");
    }
    detailMapper.insert(detail);
  }

  private String saveFile(MultipartFile file, Integer tenantId) throws IOException {
    Path dir = Paths.get(cdnPath).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    String orig = file.getOriginalFilename();
    String ext = "";
    if (orig != null && orig.contains(".")) {
      ext = orig.substring(orig.lastIndexOf('.'));
    }
    String name = UUID.randomUUID().toString() + ext;
    Path target = dir.resolve(name);
    file.transferTo(target);
    return name;
  }

  private void delSilent(Path path) {
    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
  }

  /**
   * 导出审核任务对应的原始 Excel 文件。
   */
  @Transactional(readOnly = true)
  public ExportResultDTO exportExcel(Integer taskId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    log.info("task_examshicao export, tenant={}, taskId={}", tenantId, taskId);
    ExamTaskExamshicaoEntity entity = findExisting(tenantId, taskId);

    if (entity.getFilePath() == null || entity.getFilePath().isBlank()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "该任务没有关联的原始文件");
    }

    Path filePath = Paths.get(cdnPath, entity.getFilePath());
    if (!Files.exists(filePath)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "原文件不存在");
    }

    try {
      byte[] data = Files.readAllBytes(filePath);
      String filename = buildExportFilename(entity.getId(), entity.getWorkTypeId(),
          entity.getLevel(), "实操");
      return new ExportResultDTO(data, filename);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败: " + e.getMessage());
    }
  }

  private String buildExportFilename(Integer id, Integer workTypeId, Integer level, String questionType) {
    String workTypeCn = helper.loadWorkTypeNames(
        java.util.Set.of(workTypeId != null ? workTypeId : 0))
        .getOrDefault(workTypeId, "-");
    int lvl = level != null ? level : 0;
    String levelCn = lvl >= 1 && lvl < LEVELS.length ? LEVELS[lvl] : "-";
    String ts = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return id + "_" + workTypeCn + "_" + levelCn + "_" + questionType + "_" + ts + ".xlsx";
  }
}

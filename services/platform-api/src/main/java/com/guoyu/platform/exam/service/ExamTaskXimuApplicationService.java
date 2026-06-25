package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.support.CdnFileServeHelper;
import com.guoyu.platform.exam.dto.ExamTaskXimuAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskXimuQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskXimuAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskXimuAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskXimuDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskXimuDetailChildVO;
import com.guoyu.platform.exam.vo.ExamTaskXimuDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskXimuVO;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.exam.vo.ExpertVO;
import com.guoyu.platform.exam.vo.PublicDataVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.entity.UserRoleEntity;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ExamTaskXimuApplicationService {

  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final String[] STATUS_NAMES = {"待审核", "初审审核中", "终审审核中", "完成"};
  private static final String[] TYPES = {"理论", "实操"};
  private static final String[] EXCEL_COLUMNS = {
      "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
      "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
      "U", "V", "W", "X", "Y", "Z"
  };
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ExamTaskXimuMapper ximuMapper;
  private final ExamTaskXimuDetailMapper detailMapper;
  private final ExamTaskXimuAuditRecordMapper auditMapper;
  private final ExamTaskExpertMapper expertMapper;
  private final UserMapper userMapper;
  private final UserRoleRelationMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final ExamWorkTypeApplicationService workTypeService;
  private final ObjectMapper objectMapper;
  private final CdnFileServeHelper cdnFileServeHelper;

  @Value("${platform.cdn.path:./data/cdn}")
  private String cdnPath;
  @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}")
  private String cdnUrl;

  public ExamTaskXimuApplicationService(
      ExamTaskXimuMapper ximuMapper,
      ExamTaskXimuDetailMapper detailMapper,
      ExamTaskXimuAuditRecordMapper auditMapper,
      ExamTaskExpertMapper expertMapper,
      UserMapper userMapper,
      UserRoleRelationMapper userRoleMapper,
      RoleMapper roleMapper,
      ExamWorkTypeApplicationService workTypeService,
      ObjectMapper objectMapper,
      CdnFileServeHelper cdnFileServeHelper
  ) {
    this.ximuMapper = ximuMapper;
    this.detailMapper = detailMapper;
    this.auditMapper = auditMapper;
    this.expertMapper = expertMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMapper = roleMapper;
    this.workTypeService = workTypeService;
    this.objectMapper = objectMapper;
    this.cdnFileServeHelper = cdnFileServeHelper;
  }

  // ── public methods ──

  @Transactional(readOnly = true)
  public PageResponse<ExamTaskXimuVO> page(ExamTaskXimuQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu page, tenant={}, page={}", tenantId, req.getPage());
    autoCompleteStatus(tenantId);

    LambdaQueryWrapper<ExamTaskXimuEntity> wrapper = new LambdaQueryWrapper<ExamTaskXimuEntity>()
        .eq(ExamTaskXimuEntity::getTenantId, tenantId.intValue())
        .orderByDesc(ExamTaskXimuEntity::getId);

    Page<ExamTaskXimuEntity> page = ximuMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamTaskXimuEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    Set<Integer> workTypeIds = records.stream()
        .map(ExamTaskXimuEntity::getWorkTypeId).collect(Collectors.toSet());
    Set<Integer> expertIds = records.stream()
        .map(ExamTaskXimuEntity::getExpertStart).filter(id -> id != null && id > 0)
        .collect(Collectors.toSet());
    records.stream()
        .map(ExamTaskXimuEntity::getExpertEnd).filter(id -> id != null && id > 0)
        .forEach(expertIds::add);

    Map<Integer, String> workTypeNames = loadWorkTypeNames(workTypeIds);
    Map<Integer, String> expertNames = loadUserDisplayNames(expertIds);

    List<ExamTaskXimuVO> vos = records.stream().map(entity -> {
      ExamTaskXimuVO vo = new ExamTaskXimuVO();
      vo.setId(entity.getId());
      vo.setWorkTypeId(entity.getWorkTypeId());
      vo.setLevel(entity.getLevel());
      vo.setTypes(entity.getTypes());
      vo.setNums(entity.getNums());
      vo.setStatus(entity.getStatus());
      vo.setExpertStart(entity.getExpertStart());
      vo.setExpertEnd(entity.getExpertEnd());
      vo.setFilePath(entity.getFilePath());
      vo.setAddtime(entity.getAddtime());

      vo.setWorkTypeCn(workTypeNames.getOrDefault(entity.getWorkTypeId(), "-"));
      int level = entity.getLevel() != null ? entity.getLevel() : 0;
      vo.setLevelCn(level >= 1 && level < LEVELS.length ? LEVELS[level] : "-");
      int types = entity.getTypes() != null ? entity.getTypes() : 0;
      vo.setTypesCn(types >= 0 && types < TYPES.length ? TYPES[types] : "-");
      int status = entity.getStatus() != null ? entity.getStatus() : 0;
      vo.setStatusCn(status >= 0 && status < STATUS_NAMES.length ? STATUS_NAMES[status] : "-");

      if (entity.getExpertStart() != null && entity.getExpertStart() > 0) {
        vo.setExpertStartName(expertNames.getOrDefault(entity.getExpertStart(), "***"));
      } else {
        vo.setExpertStartName("待指派");
      }
      if (entity.getExpertEnd() != null && entity.getExpertEnd() > 0) {
        vo.setExpertEndName(expertNames.getOrDefault(entity.getExpertEnd(), "***"));
      } else {
        vo.setExpertEndName("待指派");
      }

      vo.setAddtimeCn(entity.getAddtime() != null ? entity.getAddtime().format(DATE_FMT) : "-");

      if (entity.getFilePath() != null && !entity.getFilePath().isEmpty()) {
        vo.setFileUrl(cdnUrl + "/" + entity.getFilePath());
        String ext = entity.getFilePath();
        int dotIdx = ext.lastIndexOf('.');
        if (dotIdx >= 0) {
          ext = ext.substring(dotIdx + 1).toLowerCase();
          if ("xls".equals(ext)) ext = "xlsx";
          if ("doc".equals(ext)) ext = "docx";
          vo.setFileType(ext);
        }
      }
      return vo;
    }).toList();

    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public void delete(Integer id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu delete, tenant={}, id={}", tenantId, id);
    ExamTaskXimuEntity entity = findExisting(tenantId, id);
    ximuMapper.deleteById(entity.getId());

    LambdaQueryWrapper<ExamTaskXimuDetailEntity> dw = new LambdaQueryWrapper<ExamTaskXimuDetailEntity>()
        .eq(ExamTaskXimuDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuDetailEntity::getTaskId, id);
    detailMapper.delete(dw);

    LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity> aw = new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
        .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuAuditRecordEntity::getTaskId, id);
    auditMapper.delete(aw);
  }

  @Transactional(readOnly = true)
  public List<ExamTaskXimuDetailVO> getDetails(Integer taskId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu details, tenant={}, taskId={}", tenantId, taskId);

    List<ExamTaskXimuDetailEntity> details = detailMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuDetailEntity>()
            .eq(ExamTaskXimuDetailEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuDetailEntity::getTaskId, taskId));

    List<ExamTaskXimuAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
            .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId));

    Map<Integer, Map<String, ExamTaskXimuAuditRecordEntity>> startLogs = new LinkedHashMap<>();
    Map<Integer, Map<String, ExamTaskXimuAuditRecordEntity>> endLogs = new LinkedHashMap<>();
    for (ExamTaskXimuAuditRecordEntity a : audits) {
      boolean isEnd = a.getAuditRole() != null && a.getAuditRole() > 0;
      var target = isEnd ? endLogs : startLogs;
      target.computeIfAbsent(a.getXimuId(), k -> new LinkedHashMap<>()).put(a.getSnoItem(), a);
    }

    List<ExamTaskXimuDetailVO> vos = new ArrayList<>();
    for (ExamTaskXimuDetailEntity d : details) {
      ExamTaskXimuDetailVO vo = toDetailVO(d);
      ExamTaskXimuDetailChildVO sc = new ExamTaskXimuDetailChildVO();
      sc.setId("初审");
      sc.setSnoItem(d.getSnoItem());
      Map<String, ExamTaskXimuAuditRecordEntity> sm = startLogs.getOrDefault(d.getId(), Map.of());
      sc.setSnoTitle(sm.containsKey(d.getSnoItem()) ? sm.get(d.getSnoItem()).getSnoTitle() : "-");
      ExamTaskXimuDetailChildVO ec = new ExamTaskXimuDetailChildVO();
      ec.setId("终审");
      ec.setSnoItem(d.getSnoItem());
      Map<String, ExamTaskXimuAuditRecordEntity> em = endLogs.getOrDefault(d.getId(), Map.of());
      ec.setSnoTitle(em.containsKey(d.getSnoItem()) ? em.get(d.getSnoItem()).getSnoTitle() : "-");
      vo.setChildren(List.of(sc, ec));
      vos.add(vo);
    }
    return vos;
  }

  @Transactional
  public void assignExpert(ExamTaskXimuAssignExpertReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu assign expert, tenant={}, taskId={}, expertId={}, role={}",
        tenantId, req.taskId(), req.expertId(), req.role());
    ExamTaskXimuEntity entity = findExisting(tenantId, req.taskId());
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
    ximuMapper.updateById(entity);

    // check if expert already assigned for this task+role
    LambdaQueryWrapper<ExamTaskExpertEntity> expertQuery = new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExpertEntity::getTaskId, req.taskId())
        .eq(ExamTaskExpertEntity::getExpertId, req.expertId())
        .eq(ExamTaskExpertEntity::getTaskRole, req.role())
        .eq(ExamTaskExpertEntity::getTag, "ximu");
    if (expertMapper.exists(expertQuery)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已经指派过了");
    }

    // record assignment
    ExamTaskExpertEntity expertRecord = new ExamTaskExpertEntity();
    expertRecord.setTaskId(req.taskId());
    expertRecord.setTaskType("ximu");
    expertRecord.setExpertId(req.expertId());
    expertRecord.setTaskRole(req.role());
    expertRecord.setTag("ximu");
    expertRecord.setAddtime(LocalDateTime.now());
    expertRecord.setTenantId(tenantId.intValue());
    expertMapper.insert(expertRecord);
  }

  @Transactional(readOnly = true)
  public List<ExamTaskXimuDailyVO> getDaily(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu daily, tenant={}, taskId={}, role={}", tenantId, taskId, role);
    List<ExamTaskXimuAuditRecordEntity> records = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
            .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskXimuAuditRecordEntity::getYmd));
    Map<String, Long> cmap = new LinkedHashMap<>();
    for (ExamTaskXimuAuditRecordEntity r : records) {
      cmap.merge(r.getYmd(), 1L, Long::sum);
    }
    return cmap.entrySet().stream().map(e -> {
      ExamTaskXimuDailyVO vo = new ExamTaskXimuDailyVO();
      vo.setYmd(e.getKey());
      vo.setNums(e.getValue());
      return vo;
    }).collect(Collectors.toList());
  }

  @Transactional
  public int importExcel(MultipartFile file, Integer workTypeId, Integer level, Integer types) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu import, tenant={}, workTypeId={}, level={}, types={}",
        tenantId, workTypeId, level, types);

    LambdaQueryWrapper<ExamTaskXimuEntity> dup = new LambdaQueryWrapper<ExamTaskXimuEntity>()
        .eq(ExamTaskXimuEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuEntity::getWorkTypeId, workTypeId)
        .eq(ExamTaskXimuEntity::getLevel, level)
        .eq(ExamTaskXimuEntity::getTypes, types);
    if (ximuMapper.exists(dup)) {
      throw new BusinessException(ErrorCode.CONFLICT, "该工种等级类型已存在");
    }

    String filePath;
    try {
      filePath = saveFile(file, tenantId);
    } catch (IOException e) {
      log.error("Failed to save uploaded file", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
    }

    Path fullPath = Paths.get(cdnPath, filePath);
    List<Map<String, Object>> parsedData;
    try (InputStream is = Files.newInputStream(fullPath)) {
      Workbook workbook = WorkbookFactory.create(is);
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> excelRows = readSheet(sheet);
      workbook.close();
      parsedData = types > 0 ? parseShicao(excelRows, fullPath.toString()) : parseLilun(excelRows, fullPath.toString());
    } catch (BusinessException e) {
      delSilent(fullPath);
      throw e;
    } catch (Exception e) {
      delSilent(fullPath);
      log.error("Excel parse failed", e);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel解析失败: " + e.getMessage());
    }

    ExamTaskXimuEntity task = new ExamTaskXimuEntity();
    task.setTenantId(tenantId.intValue());
    task.setWorkTypeId(workTypeId);
    task.setLevel(level);
    task.setTypes(types);
    task.setNums(parsedData.size());
    task.setStatus(0);
    task.setFilePath(filePath);
    task.setAddtime(LocalDateTime.now());
    task.setDeleted(0);
    ximuMapper.insert(task);

    int count = 0;
    for (Map<String, Object> item : parsedData) {
      String sno = (String) item.get("sno");
      @SuppressWarnings("unchecked")
      Map<String, String> sno0 = (Map<String, String>) item.get("sno_0");
      @SuppressWarnings("unchecked")
      Map<String, String> sno1 = (Map<String, String>) item.get("sno_1");
      String title = (String) item.get("title");
      @SuppressWarnings("unchecked")
      List<Map<String, String>> items = (List<Map<String, String>>) item.get("list");
      for (Map<String, String> el : items) {
        ExamTaskXimuDetailEntity detail = new ExamTaskXimuDetailEntity();
        detail.setTenantId(tenantId.intValue());
        detail.setTaskId(task.getId());
        detail.setSno(sno);
        try {
          detail.setSno0(objectMapper.writeValueAsString(sno0));
          detail.setSno1(objectMapper.writeValueAsString(sno1));
        } catch (JsonProcessingException e) {
          log.error("JSON serialization failed", e);
          continue;
        }
        detail.setTitle(title);
        detail.setSnoItem(el.get("sno"));
        detail.setSnoTitle(el.get("title"));
        detail.setRowColumn(el.get("row_column"));
        detail.setDeleted(0);
        detailMapper.insert(detail);
        count++;
      }
    }
    log.info("task_ximu import done, taskId={}, count={}", task.getId(), count);
    return count;
  }

  @Transactional(readOnly = true)
  public ExportResultDTO exportExcel(Integer taskId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    log.info("task_ximu export, tenant={}, taskId={}", tenantId, taskId);
    ExamTaskXimuEntity entity = findExisting(tenantId, taskId);

    Path filePath = Paths.get(cdnPath, entity.getFilePath());
    if (!Files.exists(filePath)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "原文件不存在");
    }

    try (FileInputStream fis = new FileInputStream(filePath.toFile());
         Workbook workbook = WorkbookFactory.create(fis)) {
      Sheet sheet = workbook.getSheetAt(0);

      List<ExamTaskXimuDetailEntity> details = detailMapper.selectList(
          new LambdaQueryWrapper<ExamTaskXimuDetailEntity>()
              .eq(ExamTaskXimuDetailEntity::getTenantId, tenantId.intValue())
              .eq(ExamTaskXimuDetailEntity::getTaskId, taskId));

      List<ExamTaskXimuAuditRecordEntity> audits = auditMapper.selectList(
          new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
              .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
              .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
              .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, 1)
              .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1));

      Map<Integer, ExamTaskXimuAuditRecordEntity> auditMap = new LinkedHashMap<>();
      for (ExamTaskXimuAuditRecordEntity a : audits) {
        auditMap.put(a.getXimuId(), a);
      }

      for (ExamTaskXimuDetailEntity d : details) {
        ExamTaskXimuAuditRecordEntity audit = auditMap.get(d.getId());
        if (audit != null && audit.getSnoTitle() != null && d.getRowColumn() != null) {
          String colRef = d.getRowColumn();
          try {
            int rowNum = Integer.parseInt(colRef.replaceAll("[A-Z]", "")) - 1;
            Row row = sheet.getRow(rowNum);
            if (row != null) {
              String colLetter = colRef.replaceAll("[0-9]", "");
              int colIdx = colIndex(colLetter);
              if (colIdx >= 0) {
                var cell = row.getCell(colIdx);
                if (cell == null) cell = row.createCell(colIdx);
                cell.setCellValue(audit.getSnoTitle());
              }
            }
          } catch (Exception e) {
            log.warn("Failed to write audit to cell {}: {}", d.getRowColumn(), e.getMessage());
          }
        }
      }

      File tmp = File.createTempFile("ximu_export_", ".xlsx");
      try (FileOutputStream fos = new FileOutputStream(tmp)) {
        workbook.write(fos);
      }
      byte[] bytes = Files.readAllBytes(tmp.toPath());
      tmp.delete();

      String filename = buildExportFilename(entity.getId(), entity.getWorkTypeId(),
          entity.getLevel());
      return new ExportResultDTO(bytes, filename);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Export failed", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败");
    }
  }

  private String buildExportFilename(Integer id, Integer workTypeId, Integer level) {
    String workTypeCn = "-";
    if (workTypeId != null) {
      try {
        ExamWorkTypeVO vo = workTypeService.getById(workTypeId);
        if (vo != null && vo.getTitle() != null) workTypeCn = vo.getTitle();
      } catch (Exception ignored) {}
    }
    int lvl = level != null ? level : 0;
    String levelCn = lvl >= 1 && lvl < LEVELS.length ? LEVELS[lvl] : "-";
    String ts = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return id + "_" + workTypeCn + "_" + levelCn + "_细目_" + ts + ".xlsx";
  }

  @Transactional(readOnly = true)
  public PublicDataVO getPublicData() {
    log.info("task_ximu public-data");
    PublicDataVO vo = new PublicDataVO();
    vo.setWorkTypeList(workTypeService.tree());

    Long tenantId = SecurityContext.currentUser().getTenantId();
    RoleEntity expertRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getCode, "EXPERT")
        .eq(RoleEntity::getStatus, "enabled"));
    if (expertRole == null) {
      vo.setExpertList(List.of());
      return vo;
    }

    List<UserRoleEntity> userRoles = userRoleMapper.selectList(
        new LambdaQueryWrapper<UserRoleEntity>()
            .eq(UserRoleEntity::getTenantId, tenantId)
            .eq(UserRoleEntity::getRoleId, expertRole.getId()));
    if (userRoles.isEmpty()) {
      vo.setExpertList(List.of());
      return vo;
    }

    Set<Long> userIds = userRoles.stream().map(UserRoleEntity::getUserId).collect(Collectors.toSet());
    List<UserEntity> users = userMapper.selectBatchIds(userIds);
    List<ExpertVO> experts = users.stream()
        .filter(u -> "enabled".equals(u.getStatus()))
        .map(u -> new ExpertVO(u.getId().intValue(), u.getDisplayName()))
        .collect(Collectors.toList());
    vo.setExpertList(experts);
    return vo;
  }

  @Transactional(readOnly = true)
  public File resolveFile(String fp) {
    return cdnFileServeHelper.resolveLocalPath(fp)
        .map(Path::toFile)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));
  }

  // ── private helpers ──

  private void autoCompleteStatus(Long tenantId) {
    List<ExamTaskXimuEntity> inProgress = ximuMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuEntity>()
            .eq(ExamTaskXimuEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuEntity::getStatus, 2));
    if (inProgress.isEmpty()) return;

    Set<Integer> taskIds = inProgress.stream().map(ExamTaskXimuEntity::getId).collect(Collectors.toSet());
    List<ExamTaskXimuAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
            .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
            .in(ExamTaskXimuAuditRecordEntity::getTaskId, taskIds)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, 1)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1));

    Map<Integer, Long> completed = audits.stream()
        .collect(Collectors.groupingBy(ExamTaskXimuAuditRecordEntity::getTaskId, Collectors.counting()));

    for (ExamTaskXimuEntity e : inProgress) {
      long cnt = completed.getOrDefault(e.getId(), 0L);
      if (cnt >= e.getNums()) {
        e.setStatus(3);
        ximuMapper.updateById(e);
        log.info("Auto-completed task_ximu id={}", e.getId());
      }
    }
  }

  private ExamTaskXimuEntity findExisting(Long tenantId, Integer id) {
    ExamTaskXimuEntity entity = ximuMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskXimuEntity>()
            .eq(ExamTaskXimuEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表任务不存在");
    }
    return entity;
  }

  private Map<Integer, String> loadWorkTypeNames(Set<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    Map<Integer, String> result = new LinkedHashMap<>();
    for (Integer id : ids) {
      try {
        result.put(id, workTypeService.getById(id).getTitle());
      } catch (Exception e) {
        result.put(id, "-");
      }
    }
    return result;
  }

  private Map<Integer, String> loadUserDisplayNames(Set<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    Map<Integer, String> result = new LinkedHashMap<>();
    List<UserEntity> users = userMapper.selectBatchIds(
        ids.stream().map(Long::valueOf).collect(Collectors.toSet()));
    for (UserEntity user : users) {
      result.put(user.getId().intValue(),
          user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
    }
    return result;
  }

  private String saveFile(MultipartFile file, Long tenantId) throws IOException {
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

  private List<List<String>> readSheet(Sheet sheet) {
    List<List<String>> rows = new ArrayList<>();
    DataFormatter fmt = new DataFormatter();
    int lastRow = sheet.getLastRowNum();
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
    return rows;
  }

  private int colIndex(String letter) {
    for (int i = 0; i < EXCEL_COLUMNS.length; i++) {
      if (EXCEL_COLUMNS[i].equals(letter)) return i;
    }
    return -1;
  }

  private List<Map<String, Object>> parseLilun(List<List<String>> data, String filePath) {
    if (data.size() < 5) throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel行数不足");
    List<String> header = data.get(4);
    if (header.size() < 10 || isEmpty(header, 10))
      throw new BusinessException(ErrorCode.BAD_REQUEST, "表头不正确,请检查");

    LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<>();
    String[] letterRef = {"", "", "", ""};
    String lastSno = null;

    for (int k = 4; k < data.size(); k++) {
      List<String> row = data.get(k);
      if (row.isEmpty() || row.stream().allMatch(String::isEmpty)) continue;

      String a = getCell(row, 0);
      String c = getCell(row, 2);
      String e = getCell(row, 4);
      String g = getCell(row, 6);
      String h = getCell(row, 7);
      String i = getCell(row, 8);

      if (!a.isEmpty()) {
        String[] pa = splitCell(a);
        String l1 = checkLetter(pa, k + 1, "A");
        String[] pc = splitCell(c);
        String l2 = checkLetter(pc, k + 1, "C");
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = l1 + "-" + l2 + "-" + l3;
        lastSno = sno;
        letterRef = new String[]{l1, l2, l3, ""};
        Map<String, Object> item = makeItem(sno, Map.of(l1, pa[0]), Map.of(l1 + "-" + l2, pc[0]), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (!c.isEmpty()) {
        String[] pc = splitCell(c);
        String l2 = checkLetter(pc, k + 1, "C");
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = letterRef[0] + "-" + l2 + "-" + l3;
        lastSno = sno;
        letterRef[1] = l2; letterRef[2] = l3;
        Map<String, Object> item = makeItem(sno, Map.of(letterRef[0], ""), Map.of(letterRef[0] + "-" + l2, pc[0]), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (!e.isEmpty()) {
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = letterRef[0] + "-" + letterRef[1] + "-" + l3;
        lastSno = sno;
        letterRef[2] = l3;
        Map<String, Object> item = makeItem(sno, Map.of(letterRef[0], ""), Map.of(letterRef[0] + "-" + letterRef[1], ""), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (lastSno != null && !h.isEmpty()) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> list = (List<Map<String, String>>) result.get(lastSno).get("list");
        list.add(makeEl(sno(lastSno, h), i, k));
      }
    }

    return new ArrayList<>(result.values());
  }

  private List<Map<String, Object>> parseShicao(List<List<String>> data, String filePath) {
    if (data.size() < 5) throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel行数不足");
    List<String> header = data.get(4);
    if (header.size() < 13 || isEmpty(header, 13))
      throw new BusinessException(ErrorCode.BAD_REQUEST, "表头不正确,请检查");

    LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<>();
    String[] letterRef = {"", "", "", ""};
    String lastSno = null;

    for (int k = 4; k < data.size(); k++) {
      List<String> row = data.get(k);
      if (row.isEmpty() || row.stream().allMatch(String::isEmpty)) continue;

      String a = getCell(row, 0);
      String c = getCell(row, 2);
      String e = getCell(row, 4);
      String g = getCell(row, 6);
      String h = getCell(row, 7);
      String i = getCell(row, 8);

      if (!a.isEmpty()) {
        String[] pa = splitCell(a);
        String l1 = checkLetter(pa, k + 1, "A");
        String[] pc = splitCell(c);
        String l2 = checkLetter(pc, k + 1, "C");
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = l1 + "-" + l2 + "-" + l3;
        lastSno = sno;
        letterRef = new String[]{l1, l2, l3, ""};
        Map<String, Object> item = makeItem(sno, Map.of(l1, pa[0]), Map.of(l1 + "-" + l2, pc[0]), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (!c.isEmpty()) {
        String[] pc = splitCell(c);
        String l2 = checkLetter(pc, k + 1, "C");
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = letterRef[0] + "-" + l2 + "-" + l3;
        lastSno = sno;
        letterRef[1] = l2; letterRef[2] = l3;
        Map<String, Object> item = makeItem(sno, Map.of(letterRef[0], ""), Map.of(letterRef[0] + "-" + l2, pc[0]), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (!e.isEmpty()) {
        String[] pe = splitCell(e);
        String l3 = checkLetter(pe, k + 1, "E");
        String sno = letterRef[0] + "-" + letterRef[1] + "-" + l3;
        lastSno = sno;
        letterRef[2] = l3;
        Map<String, Object> item = makeItem(sno, Map.of(letterRef[0], ""), Map.of(letterRef[0] + "-" + letterRef[1], ""), g);
        result.put(sno, item);
        if (!h.isEmpty()) addEl(item, sno, h, i, k);
      } else if (lastSno != null && !h.isEmpty()) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> list = (List<Map<String, String>>) result.get(lastSno).get("list");
        list.add(makeEl(sno(lastSno, h), i, k));
      }
    }

    return new ArrayList<>(result.values());
  }

  private String getCell(List<String> row, int idx) {
    return idx < row.size() ? row.get(idx) : "";
  }

  private String[] splitCell(String val) {
    String[] parts = val.split("\n");
    String text = parts[0];
    String letter = parts.length > 1 ? parts[1].replace(" ", "") : "";
    return new String[]{text, letter};
  }

  private String checkLetter(String[] parts, int lineNum, String col) {
    if (parts.length < 2 || parts[1].isEmpty() || colIndex(parts[1]) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          lineNum + "行" + col + "列数据结构不正确,获取不到字母");
    }
    return parts[1];
  }

  private Map<String, Object> makeItem(String sno, Map<String, String> sno0,
                                        Map<String, String> sno1, String title) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("sno", sno);
    item.put("sno_0", new LinkedHashMap<>(sno0));
    item.put("sno_1", new LinkedHashMap<>(sno1));
    item.put("title", title);
    item.put("list", new ArrayList<Map<String, String>>());
    return item;
  }

  @SuppressWarnings("unchecked")
  private void addEl(Map<String, Object> item, String sno, String h, String i, int k) {
    ((List<Map<String, String>>) item.get("list")).add(makeEl(sno(sno, h), i, k));
  }

  private Map<String, String> makeEl(String fullSno, String title, int k) {
    Map<String, String> el = new LinkedHashMap<>();
    el.put("sno", fullSno);
    el.put("title", title);
    el.put("row_column", (k + 1) + EXCEL_COLUMNS[8]);
    return el;
  }

  private String sno(String base, String h) {
    return base + "-" + h;
  }

  private boolean isEmpty(List<String> row, int cols) {
    for (int i = 0; i < Math.min(cols, row.size()); i++) {
      if (!row.get(i).isEmpty()) return false;
    }
    return true;
  }

  private ExamTaskXimuDetailVO toDetailVO(ExamTaskXimuDetailEntity d) {
    ExamTaskXimuDetailVO vo = new ExamTaskXimuDetailVO();
    vo.setId(d.getId());
    vo.setTaskId(d.getTaskId());
    vo.setSno(d.getSno());
    try {
      if (d.getSno0() != null) {
        @SuppressWarnings("unchecked")
        Map<String, String> m0 = objectMapper.readValue(d.getSno0(), LinkedHashMap.class);
        vo.setSno0(String.join(",", m0.values()));
      }
      if (d.getSno1() != null) {
        @SuppressWarnings("unchecked")
        Map<String, String> m1 = objectMapper.readValue(d.getSno1(), LinkedHashMap.class);
        vo.setSno1(String.join(",", m1.values()));
      }
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse sno JSON for detail id={}", d.getId());
    }
    vo.setTitle(d.getTitle());
    vo.setSnoItem(d.getSnoItem());
    vo.setSnoTitle(d.getSnoTitle());
    vo.setRowColumn(d.getRowColumn());
    return vo;
  }
}

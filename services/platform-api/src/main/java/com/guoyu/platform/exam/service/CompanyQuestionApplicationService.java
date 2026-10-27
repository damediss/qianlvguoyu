package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.CompanyQuestionCreateReq;
import com.guoyu.platform.exam.dto.CompanyQuestionQueryReq;
import com.guoyu.platform.exam.dto.CompanyQuestionUpdateReq;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CompanyQuestionApplicationService {

  private final ExamExamDetailMapper mapper;
  private final UserMapper userMapper;

  public CompanyQuestionApplicationService(
      ExamExamDetailMapper mapper,
      UserMapper userMapper) {
    this.mapper = mapper;
    this.userMapper = userMapper;
  }

  // ── 查询 ──

  @Transactional(readOnly = true)
  public PageResponse<ExamExamDetailVO> page(CompanyQuestionQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .isNotNull(ExamExamDetailEntity::getOrgId)
        .eq(req.getOrgId() != null, ExamExamDetailEntity::getOrgId, req.getOrgId())
        .eq(StringUtils.hasText(req.getType()), ExamExamDetailEntity::getType, req.getType())
        .eq(req.getDifficulty() != null, ExamExamDetailEntity::getDifficulty, req.getDifficulty())
        .eq(req.getStatus() != null, ExamExamDetailEntity::getStatus, req.getStatus())
        .like(StringUtils.hasText(req.getKeyword()), ExamExamDetailEntity::getContent, req.getKeyword())
        .orderByDesc(ExamExamDetailEntity::getSortNumber)
        .orderByDesc(ExamExamDetailEntity::getId);

    Page<ExamExamDetailEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamExamDetailEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    // 批量加载创建人名称
    Set<Integer> userIds = records.stream()
        .map(ExamExamDetailEntity::getUserId)
        .filter(uid -> uid != null && uid > 0)
        .collect(Collectors.toSet());
    Map<Integer, String> userNameMap = loadUserNames(userIds);

    List<ExamExamDetailVO> vos = records.stream().map(entity -> {
      ExamExamDetailVO vo = ExamExamDetailVO.from(entity);
      if (entity.getUserId() != null) {
        vo.setCreatorName(userNameMap.getOrDefault(entity.getUserId(), ""));
      }
      return vo;
    }).toList();

    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public ExamExamDetailVO getById(Integer id) {
    ExamExamDetailEntity entity = findExisting(id);
    ExamExamDetailVO vo = ExamExamDetailVO.from(entity);
    if (entity.getUserId() != null) {
      UserEntity user = userMapper.selectById((long) entity.getUserId());
      if (user != null) {
        vo.setCreatorName(user.getDisplayName());
      }
    }
    return vo;
  }

  // ── 写操作 ──

  @Transactional
  public ExamExamDetailVO create(CompanyQuestionCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamExamDetailEntity entity = new ExamExamDetailEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.type());
    entity.setContent(req.content());
    entity.setAnswer(compactAnswer(req.answer()));
    entity.setOrgId(req.orgId());
    entity.setOrgName(req.orgName());
    entity.setKnowledgeId(null);
    entity.setDifficulty(req.difficulty() != null ? req.difficulty() : 2);
    entity.setScore(req.score());
    // §5.8 第三道闸门：手工编辑解析 ≤2000 字
    ResolvePlainText.assertEditableLength(req.resolve());
    entity.setResolve(req.resolve());
    entity.setKeyword(req.keyword());
    entity.setSource(0);
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 1);
    entity.setUserId(userId.intValue());
    entity.setCheckStatus(0);
    entity.setFinalCheckStatus(0);
    entity.setIsPrivate(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());

    mapper.insert(entity);
    return ExamExamDetailVO.from(entity);
  }

  @Transactional
  public ExamExamDetailVO update(Integer id, CompanyQuestionUpdateReq req) {
    ExamExamDetailEntity entity = findExisting(id);

    if (req.type() != null) entity.setType(req.type());
    if (req.content() != null) entity.setContent(req.content());
    if (req.answer() != null) entity.setAnswer(compactAnswer(req.answer()));
    if (req.orgId() != null) entity.setOrgId(req.orgId());
    if (req.orgName() != null) entity.setOrgName(req.orgName());
    if (req.difficulty() != null) entity.setDifficulty(req.difficulty());
    if (req.score() != null) entity.setScore(req.score());
    if (req.resolve() != null) {
      ResolvePlainText.assertEditableLength(req.resolve());
      entity.setResolve(req.resolve());
    }
    if (req.keyword() != null) entity.setKeyword(req.keyword());
    if (req.status() != null) entity.setStatus(req.status());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());

    mapper.updateById(entity);
    return ExamExamDetailVO.from(entity);
  }

  @Transactional
  public void delete(Integer id) {
    findExisting(id);
    mapper.deleteById(id);
  }

  @Transactional
  public int batchDelete(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .in(ExamExamDetailEntity::getId, ids);
    return mapper.delete(wrapper);
  }

  // ── 导入 ──

  @Transactional
  public int importQuestions(MultipartFile file, Long orgId, String orgName, Integer source) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
    }

    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    List<ExamExamDetailEntity> entities = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      int lastRow = sheet.getLastRowNum();

      for (int i = 1; i <= lastRow; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String type = getCellString(row.getCell(0));
        if (!StringUtils.hasText(type)) continue;

        String typeCode = mapTypeLabel(type);
        if (typeCode == null) {
          log.warn("Row {}: unknown type '{}', skipping", i, type);
          continue;
        }

        String stem = getCellString(row.getCell(1));
        String optA = getCellString(row.getCell(2));
        String optB = getCellString(row.getCell(3));
        String optC = getCellString(row.getCell(4));
        String optD = getCellString(row.getCell(5));
        String answer = getCellString(row.getCell(6));
        String resolve = getCellString(row.getCell(7));
        try {
          ResolvePlainText.assertEditableLength(resolve);
        } catch (BusinessException ex) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "第 " + (i + 1) + " 行：" + ex.getMessage());
        }
        Integer difficulty = parseDifficulty(getCellString(row.getCell(8)));
        String keyword = getCellString(row.getCell(9));

        String content = buildContent(stem, optA, optB, optC, optD);

        ExamExamDetailEntity entity = new ExamExamDetailEntity();
        entity.setTenantId(tenantId);
        entity.setType(typeCode);
        entity.setContent(content);
        entity.setAnswer(compactAnswer(answer));
        entity.setResolve(resolve);
        entity.setKeyword(keyword);
        entity.setDifficulty(difficulty);
        entity.setOrgId(orgId);
        entity.setOrgName(orgName);
        entity.setKnowledgeId(null);
        entity.setSource(source != null ? source : 0);
        entity.setScore(null);
        entity.setIsPrivate(0);
        entity.setStatus(1);
        entity.setCheckStatus(0);
        entity.setFinalCheckStatus(0);
        entity.setSortNumber(1);
        entity.setUserId(userId.intValue());
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());

        entities.add(entity);
      }
    } catch (IOException e) {
      log.error("Failed to parse Excel file", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 文件解析失败: " + e.getMessage());
    }

    if (!entities.isEmpty()) {
      for (ExamExamDetailEntity entity : entities) {
        mapper.insert(entity);
      }
    }

    log.info("Imported {} company questions for orgId={}", entities.size(), orgId);
    return entities.size();
  }

  // ── 导出 ──

  @Transactional(readOnly = true)
  public ExportResultDTO exportByOrg(Long orgId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .isNotNull(ExamExamDetailEntity::getOrgId)
        .eq(orgId != null, ExamExamDetailEntity::getOrgId, orgId)
        .orderByDesc(ExamExamDetailEntity::getSortNumber)
        .orderByDesc(ExamExamDetailEntity::getId);

    List<ExamExamDetailEntity> records = mapper.selectList(wrapper);

    if (records.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该条件下的公司试题数据");
    }

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("试题");

      Row header = sheet.createRow(0);
      String[] headers = {"题型", "题干", "选项A", "选项B", "选项C", "选项D",
          "答案", "解析", "难度", "关键词", "公司名称"};
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 1;
      for (ExamExamDetailEntity entity : records) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(mapTypeCode(entity.getType()));

        String[] parts = splitContent(entity.getContent());
        row.createCell(1).setCellValue(parts[0]);
        if (parts.length > 1) row.createCell(2).setCellValue(parts[1]);
        if (parts.length > 2) row.createCell(3).setCellValue(parts[2]);
        if (parts.length > 3) row.createCell(4).setCellValue(parts[3]);
        if (parts.length > 4) row.createCell(5).setCellValue(parts[4]);

        row.createCell(6).setCellValue(nvl(entity.getAnswer()));
        row.createCell(7).setCellValue(nvl(entity.getResolve()));
        row.createCell(8).setCellValue(mapDifficultyLabel(entity.getDifficulty()));
        row.createCell(9).setCellValue(nvl(entity.getKeyword()));
        row.createCell(10).setCellValue(nvl(entity.getOrgName()));
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      byte[] data = out.toByteArray();
      String filename = buildExportFilename(orgId);
      return new ExportResultDTO(data, filename);
    } catch (IOException e) {
      log.error("Failed to generate Excel", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 文件生成失败: " + e.getMessage());
    }
  }

  // ── Excel helper methods ──

  private String buildExportFilename(Long orgId) {
    String prefix = "公司试题";
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return prefix + "_" + (orgId != null ? orgId : "all") + "_" + ts + ".xlsx";
  }

  private Map<Integer, String> loadUserNames(Set<Integer> userIds) {
    if (userIds.isEmpty()) return Map.of();
    List<UserEntity> users = userMapper.selectBatchIds(
        userIds.stream().map(Integer::longValue).toList());
    return users.stream().collect(Collectors.toMap(
        u -> u.getId().intValue(),
        u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
        (a, b) -> a));
  }

  private String mapTypeLabel(String label) {
    if (label == null) return null;
    return switch (label.trim()) {
      case "单选题" -> "danxuan";
      case "多选题" -> "duoxuan";
      case "判断题" -> "pandan";
      case "填空题" -> "tiankong";
      case "简答题" -> "jianda";
      case "论述题" -> "lunshu";
      case "案例分析题" -> "anli";
      default -> null;
    };
  }

  private String mapTypeCode(String code) {
    if (code == null) return "未知";
    return switch (code) {
      case "danxuan" -> "单选题";
      case "duoxuan" -> "多选题";
      case "pandan" -> "判断题";
      case "tiankong" -> "填空题";
      case "jianda" -> "简答题";
      case "lunshu" -> "论述题";
      case "anli" -> "案例分析题";
      default -> code;
    };
  }

  private Integer parseDifficulty(String s) {
    if (s == null || s.isBlank()) return 2;
    return switch (s.trim()) {
      case "简单" -> 1;
      case "中等" -> 2;
      case "困难" -> 3;
      default -> {
        try { yield Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { yield 2; }
      }
    };
  }

  private String mapDifficultyLabel(Integer difficulty) {
    if (difficulty == null) return "中等";
    return switch (difficulty) {
      case 1 -> "简单";
      case 3 -> "困难";
      default -> "中等";
    };
  }

  private String buildContent(String stem, String optA, String optB, String optC, String optD) {
    StringBuilder sb = new StringBuilder(nvl(stem));
    if (StringUtils.hasText(optA)) sb.append("\nA. ").append(optA);
    if (StringUtils.hasText(optB)) sb.append("\nB. ").append(optB);
    if (StringUtils.hasText(optC)) sb.append("\nC. ").append(optC);
    if (StringUtils.hasText(optD)) sb.append("\nD. ").append(optD);
    return sb.toString();
  }

  private String[] splitContent(String content) {
    if (content == null) return new String[]{""};
    String[] lines = content.split("\n");
    if (lines.length <= 1) return new String[]{content};
    String stem = lines[0];
    String optA = extractOption(lines, "A.");
    String optB = extractOption(lines, "B.");
    String optC = extractOption(lines, "C.");
    String optD = extractOption(lines, "D.");
    return new String[]{stem, optA, optB, optC, optD};
  }

  private String extractOption(String[] lines, String prefix) {
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith(prefix)) {
        return trimmed.substring(prefix.length()).trim();
      }
    }
    return "";
  }

  private String getCellString(Cell cell) {
    if (cell == null) return "";
    if (cell.getCellType() == CellType.NUMERIC) {
      return String.valueOf((int) cell.getNumericCellValue());
    }
    if (cell.getCellType() == CellType.BOOLEAN) {
      return String.valueOf(cell.getBooleanCellValue());
    }
    return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
  }

  private String nvl(String s) {
    return s != null ? s : "";
  }

  private ExamExamDetailEntity findExisting(Integer id) {
    ExamExamDetailEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "试题不存在");
    }
    return entity;
  }

  /**
   * 若 answer 内容为 JSON（以 { 或 [ 开头），则去除换行符、制表符并压缩连续空格，
   * 减少存储体积；非 JSON 内容原样返回。
   */
  private String compactAnswer(String answer) {
    if (answer == null || answer.isBlank()) return answer;
    String trimmed = answer.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      return trimmed
              .replace("\r", "")
              .replace("\n", "")
              .replace("\t", "")
              .replaceAll(" {2,}", " ");
    }
    return answer;
  }
}

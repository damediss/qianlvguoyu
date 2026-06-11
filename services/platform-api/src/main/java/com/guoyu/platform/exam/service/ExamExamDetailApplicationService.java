package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
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
public class ExamExamDetailApplicationService {

  private final ExamExamDetailMapper mapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final ExamCourseMapper courseMapper;
  private final UserMapper userMapper;
  private final ExamKnowledgeStructureApplicationService knowledgeService;

  public ExamExamDetailApplicationService(
      ExamExamDetailMapper mapper,
      ExamKnowledgeStructureMapper knowledgeMapper,
      ExamCourseMapper courseMapper,
      UserMapper userMapper,
      ExamKnowledgeStructureApplicationService knowledgeService) {
    this.mapper = mapper;
    this.knowledgeMapper = knowledgeMapper;
    this.courseMapper = courseMapper;
    this.userMapper = userMapper;
    this.knowledgeService = knowledgeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamExamDetailVO> page(ExamExamDetailQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId);

    // 知识点展开逻辑: 根据 knowledgeId 或 courseId 获取 L3 ID 列表
    List<Integer> knowledgeIds = resolveKnowledgeIds(req);
    if (knowledgeIds != null) {
      if (knowledgeIds.isEmpty()) {
        return new PageResponse<>(List.of(), req.getPage(), req.getPageSize(), 0L);
      }
      wrapper.in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds);
    }

    wrapper.eq(StringUtils.hasText(req.getType()), ExamExamDetailEntity::getType, req.getType())
        .eq(req.getSource() != null, ExamExamDetailEntity::getSource, req.getSource())
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

    // 批量加载关联数据: 创建人用户名 + 知识点名称
    Set<Integer> userIds = records.stream()
        .map(ExamExamDetailEntity::getUserId)
        .filter(uid -> uid != null && uid > 0)
        .collect(Collectors.toSet());
    Set<Integer> kIds = records.stream()
        .map(ExamExamDetailEntity::getKnowledgeId)
        .filter(kid -> kid != null && kid > 0)
        .collect(Collectors.toSet());

    Map<Integer, String> userNameMap = loadUserNames(userIds);
    Map<Integer, String> knowledgeTitleMap = loadKnowledgeTitles(kIds);

    List<ExamExamDetailVO> vos = records.stream().map(entity -> {
      ExamExamDetailVO vo = ExamExamDetailVO.from(entity);
      if (entity.getUserId() != null) {
        vo.setCreatorName(userNameMap.getOrDefault(entity.getUserId(), ""));
      }
      if (entity.getKnowledgeId() != null) {
        vo.setKnowledgeTitle(knowledgeTitleMap.getOrDefault(entity.getKnowledgeId(), ""));
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
    if (entity.getKnowledgeId() != null) {
      ExamKnowledgeStructureEntity ks = knowledgeMapper.selectById(entity.getKnowledgeId());
      if (ks != null) {
        vo.setKnowledgeTitle(ks.getTitle());
      }
    }
    return vo;
  }

  @Transactional
  public ExamExamDetailVO create(ExamExamDetailCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamExamDetailEntity entity = new ExamExamDetailEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.type());
    entity.setContent(req.content());
    entity.setAnswer(req.answer());
    entity.setKnowledgeId(req.knowledgeId());
    entity.setDifficulty(req.difficulty() != null ? req.difficulty() : 2);
    entity.setScore(req.score());
    entity.setResolve(req.resolve());
    entity.setKeyword(req.keyword());
    entity.setSource(req.source() != null ? req.source() : 0);
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
  public ExamExamDetailVO update(Integer id, ExamExamDetailUpdateReq req) {
    ExamExamDetailEntity entity = findExisting(id);

    if (req.type() != null) entity.setType(req.type());
    if (req.content() != null) entity.setContent(req.content());
    if (req.answer() != null) entity.setAnswer(req.answer());
    if (req.knowledgeId() != null) entity.setKnowledgeId(req.knowledgeId());
    if (req.difficulty() != null) entity.setDifficulty(req.difficulty());
    if (req.score() != null) entity.setScore(req.score());
    if (req.resolve() != null) entity.setResolve(req.resolve());
    if (req.keyword() != null) entity.setKeyword(req.keyword());
    if (req.source() != null) entity.setSource(req.source());
    if (req.status() != null) entity.setStatus(req.status());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());
    if (req.checkStatus() != null) {
      entity.setCheckStatus(req.checkStatus());
      entity.setCheckTime(LocalDateTime.now());
      entity.setCheckUserId(SecurityContext.currentUser().getUserId().intValue());
    }
    if (req.finalCheckStatus() != null) {
      entity.setFinalCheckStatus(req.finalCheckStatus());
      entity.setFinalCheckTime(LocalDateTime.now());
      entity.setFinalCheckUserId(SecurityContext.currentUser().getUserId().intValue());
    }

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
    // 限定 tenant 范围,避免跨租户删除
    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .in(ExamExamDetailEntity::getId, ids);
    return mapper.delete(wrapper);
  }

  /**
   * 根据查询条件解析 L3 知识点ID列表。
   * 返回 null 表示不需要知识点过滤; 返回空列表表示无匹配。
   */
  private List<Integer> resolveKnowledgeIds(ExamExamDetailQueryReq req) {
    if (req.getKnowledgeId() != null && req.getKnowledgeId() > 0) {
      return knowledgeService.getLevel3DescendantIds(req.getKnowledgeId());
    }
    if (req.getCourseId() != null && req.getCourseId() > 0) {
      return knowledgeService.getLevel3IdsByCourse(req.getCourseId());
    }
    return null;
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

  private Map<Integer, String> loadKnowledgeTitles(Set<Integer> kIds) {
    if (kIds.isEmpty()) return Map.of();
    List<ExamKnowledgeStructureEntity> list = knowledgeMapper.selectBatchIds(kIds.stream().toList());
    return list.stream().collect(Collectors.toMap(
        ExamKnowledgeStructureEntity::getId,
        ExamKnowledgeStructureEntity::getTitle,
        (a, b) -> a));
  }

  // ── Import from Excel ──
  @Transactional
  public int importExamDetails(MultipartFile file, Integer courseId, Integer source) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
    }

    List<Integer> knowledgeIds = knowledgeService.getLevel3IdsByCourse(courseId);

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
        Integer difficulty = parseDifficulty(getCellString(row.getCell(8)));
        String keyword = getCellString(row.getCell(9));

        String content = buildContent(stem, optA, optB, optC, optD);

        ExamExamDetailEntity entity = new ExamExamDetailEntity();
        entity.setTenantId(tenantId);
        entity.setType(typeCode);
        entity.setContent(content);
        entity.setAnswer(answer);
        entity.setResolve(resolve);
        entity.setKeyword(keyword);
        entity.setDifficulty(difficulty);
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

        // Assign knowledgeId: pick a random L3 under the course
        if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
          int idx = entities.size() % knowledgeIds.size();
          entity.setKnowledgeId(knowledgeIds.get(idx));
        }

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

    log.info("Imported {} exam details for courseId={}", entities.size(), courseId);
    return entities.size();
  }

  // ── Export to Excel ──
  @Transactional(readOnly = true)
  public ExportResultDTO exportByCourse(Integer courseId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    List<Integer> knowledgeIds = knowledgeService.getLevel3IdsByCourse(courseId);
    if (knowledgeIds == null || knowledgeIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该科目下无知识点，无法导出");
    }

    List<ExamExamDetailEntity> records = mapper.selectList(
        new LambdaQueryWrapper<ExamExamDetailEntity>()
            .eq(ExamExamDetailEntity::getTenantId, tenantId)
            .in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds)
            .orderByDesc(ExamExamDetailEntity::getSortNumber)
            .orderByDesc(ExamExamDetailEntity::getId));

    if (records.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该科目下无试题数据");
    }

    // Load knowledge title map for knowledge column
    Set<Integer> kIds = records.stream()
        .map(ExamExamDetailEntity::getKnowledgeId)
        .filter(kid -> kid != null && kid > 0)
        .collect(Collectors.toSet());
    Map<Integer, String> knowledgeTitleMap = loadKnowledgeTitles(kIds);

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("试题");

      // Header row
      Row header = sheet.createRow(0);
      String[] headers = {"题型", "题干", "选项A", "选项B", "选项C", "选项D",
          "答案", "解析", "难度", "关键词", "知识点"};
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }

      int rowIdx = 1;
      for (ExamExamDetailEntity entity : records) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(mapTypeCode(entity.getType()));

        // Split content into stem + options
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

        String kTitle = entity.getKnowledgeId() != null
            ? knowledgeTitleMap.getOrDefault(entity.getKnowledgeId(), "") : "";
        row.createCell(10).setCellValue(kTitle);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      byte[] data = out.toByteArray();
      String filename = buildExportFilename(courseId, records.get(0).getType());
      return new ExportResultDTO(data, filename);
    } catch (IOException e) {
      log.error("Failed to generate Excel", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 文件生成失败: " + e.getMessage());
    }
  }

  private String buildExportFilename(Integer courseId, String type) {
    String courseTitle = "题库";
    if (courseId != null) {
      ExamCourseEntity course = courseMapper.selectById(courseId);
      if (course != null) courseTitle = course.getTitle();
    }
    String typeLabel = mapTypeCode(type);
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return courseTitle + "_" + typeLabel + "_" + ts + ".xlsx";
  }

  // ── Excel helper methods ──

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
}

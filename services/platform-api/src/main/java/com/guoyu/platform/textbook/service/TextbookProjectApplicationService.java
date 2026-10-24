package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.textbook.domain.TextbookDirectionResolver;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder;
import com.guoyu.platform.textbook.dto.ProjectCreateReq;
import com.guoyu.platform.textbook.dto.ProjectQueryReq;
import com.guoyu.platform.textbook.dto.ProjectUpdateReq;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import com.guoyu.platform.textbook.vo.ParsePreviewVO;
import com.guoyu.platform.textbook.vo.TextbookProjectVO;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教材工程编排（设计 §2.3/§2.4）：解析预览、创建（同步 解析→方向过滤→骨架→配比）、
 * 列表/详情、更新（结构变化自动重建）、删除、重建、导出。
 *
 * <p>一期全同步无异步；levelCode 全链路 API 口径（5=五级 … 1=一级），
 * 与解析器（0=五级 … 4=一级）的换算只发生在本类与重建入口处（5 - code）。
 */
@Service
public class TextbookProjectApplicationService {

  private static final Logger log = LoggerFactory.getLogger(TextbookProjectApplicationService.class);

  public static final String STATUS_PARSED = "parsed";
  public static final String STATUS_OUTLINED = "outlined";
  public static final String STATUS_REVIEWED = "reviewed";
  private static final int DEFAULT_WORDS_PER_HOUR = 2500;

  private final TextbookProjectMapper projectMapper;
  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookOutlineApplicationService outlineService;
  private final FileApplicationService fileApplicationService;
  private final FileMapper fileMapper;
  private final ObjectMapper objectMapper;

  public TextbookProjectApplicationService(TextbookProjectMapper projectMapper,
      TextbookOutlineNodeMapper nodeMapper,
      TextbookOutlineApplicationService outlineService,
      FileApplicationService fileApplicationService,
      FileMapper fileMapper,
      ObjectMapper objectMapper) {
    this.projectMapper = projectMapper;
    this.nodeMapper = nodeMapper;
    this.outlineService = outlineService;
    this.fileApplicationService = fileApplicationService;
    this.fileMapper = fileMapper;
    this.objectMapper = objectMapper;
  }

  // ============================================================
  // 解析预览（接口 1，同步不上库）
  // ============================================================

  @Transactional(readOnly = true)
  public ParsePreviewVO parsePreview(byte[] content, String originalName) {
    ParsedStandard parsed = parseAndValidate(content, originalName);

    // 方向列表：解析出多方向映射时以映射为准（铝电解工权重表无方向行，parsed.directions 只有"默认"）
    Map<String, List<Integer>> firstLevelMapping = new LinkedHashMap<>();
    Map<Integer, Map<String, List<Integer>>> mappingByLevel = new LinkedHashMap<>();
    List<String> directions = new ArrayList<>(parsed.getDirections());
    List<String> extraWarnings = new ArrayList<>();
    for (StandardLevelDoc level : parsed.getLevels()) {
      Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(level,
          parsed.getDirections());
      if (!mapping.containsKey(ParsedStandard.DEFAULT_DIRECTION)) {
        mappingByLevel.put(5 - level.getLevelCode(), mapping);
        if (firstLevelMapping.isEmpty()) {
          firstLevelMapping = mapping;
        }
      }
    }
    if (!firstLevelMapping.isEmpty()) {
      directions = new ArrayList<>(firstLevelMapping.keySet());
    } else if (parsed.getDirections().size() > 1) {
      extraWarnings.add("WARN: 方向映射未识别，请人工登记职业功能序号");
    }

    Map<String, List<Integer>> directionLevels = new LinkedHashMap<>();
    parsed.getDirectionLevels().forEach((direction, codes) ->
        directionLevels.put(direction, codes.stream().map(c -> 5 - c).sorted().toList()));

    Map<Integer, Integer> trainingHours = new LinkedHashMap<>();
    parsed.getTrainingHoursByLevel().forEach((code, hours) -> trainingHours.put(5 - code, hours));

    // 扁平猜测值：首等级首方向的序号数组（前端勾选预选；解析失败为空数组 → 人工勾选）
    List<Integer> guessedFlat = firstLevelMapping.isEmpty()
        ? List.of()
        : List.copyOf(firstLevelMapping.values().iterator().next());

    // 各等级职业功能序号→名称（工作要求表 L1），供前端勾选项显示真实功能名
    Map<Integer, Map<Integer, String>> functionNamesByLevel = new LinkedHashMap<>();
    for (StandardLevelDoc level : parsed.getLevels()) {
      Map<Integer, String> names = new LinkedHashMap<>();
      for (StandardNodeDoc func : level.getWorkFunctions()) {
        Integer no = TextbookOutlineBuilder.parseFunctionNo(func.getNumber());
        if (no != null && func.getTitle() != null && !func.getTitle().isBlank()) {
          names.put(no, func.getTitle());
        }
      }
      functionNamesByLevel.put(5 - level.getLevelCode(), names);
    }

    List<String> warnings = new ArrayList<>(parsed.getParseWarnings());
    warnings.addAll(extraWarnings);
    return new ParsePreviewVO(
        parsed.getWorkTypeTitle(), parsed.getSeriesNo(), parsed.getVersion(),
        directions, directionLevels, trainingHours,
        guessedFlat, mappingByLevel, functionNamesByLevel, warnings);
  }

  // ============================================================
  // 创建（接口 2，同步全链路，失败整体回滚）
  // ============================================================

  /**
   * multipart 直传创建：前端上传原始 docx（不走 file 模块预传），
   * 后端存文件后走与 {@link #create(ProjectCreateReq)} 相同的 解析→方向过滤→骨架→配比 链路。
   * 文件存储与工程插入同事务，创建失败回滚时物理文件由 file 模块的 rollback cleanup 删除。
   * 注意：文件内容直接用内存 bytes 解析，不能经 {@link FileApplicationService#readFileBytesById}
   * 回读——它是 NOT_SUPPORTED 传播，会挂起本事务导致读不到尚未提交的文件行。
   */
  @Transactional
  public TextbookProjectVO createFromUpload(byte[] content, String originalName,
      ProjectCreateReq req) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    FileVO file = fileApplicationService.storeFromBytes(content,
        originalName != null ? originalName : "standard.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "private");
    return createInternal(new ProjectCreateReq(file.id(), req.direction(), req.levelCode(),
        req.functionNos(), req.trainingHours(), req.wordsPerHour()), content);
  }

  @Transactional
  public TextbookProjectVO create(ProjectCreateReq req) {
    // JSON 分支：fileId 来自先前已提交的上传事务，跨事务回读可见
    return createInternal(req, fileApplicationService.readFileBytesById(req.fileId()));
  }

  private TextbookProjectVO createInternal(ProjectCreateReq req, byte[] content) {
    ParsedStandard parsed = parseAndValidate(content, null);

    String workTypeTitle = parsed.getWorkTypeTitle();
    if (workTypeTitle == null || workTypeTitle.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "未解析到职业名称，请检查标准文档 1.1 节");
    }
    int parserLevelCode = 5 - req.levelCode();
    StandardLevelDoc level = parsed.levelOf(parserLevelCode);
    if (level == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "标准中不存在所选等级");
    }

    // 方向过滤：解析映射优先，人工登记 functionNos 次之（设计 §2.4-②）
    Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(level,
        parsed.getDirections());
    String direction = req.direction() == null ? "" : req.direction().trim();
    List<Integer> functionNos;
    if (mapping.containsKey(direction)) {
      functionNos = mapping.get(direction);
    } else if (mapping.size() == 1 && mapping.containsKey(ParsedStandard.DEFAULT_DIRECTION)) {
      if (!ParsedStandard.DEFAULT_DIRECTION.equals(direction)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "该标准为单方向工种，方向应为「" + ParsedStandard.DEFAULT_DIRECTION + "」");
      }
      functionNos = req.functionNos() != null && !req.functionNos().isEmpty()
          ? req.functionNos() : mapping.get(ParsedStandard.DEFAULT_DIRECTION);
    } else {
      if (mapping.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "方向映射未识别，请人工登记职业功能序号（functionNos）");
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "方向「" + direction + "」不在标准方向列表中：" + mapping.keySet());
    }
    if (req.functionNos() != null && !req.functionNos().isEmpty()) {
      functionNos = req.functionNos(); // 人工登记优先于解析
    }
    validateFunctionNos(functionNos, level.getWorkFunctions().size());

    Integer parsedHours = parsed.getTrainingHoursByLevel().get(parserLevelCode);
    int trainingHours = req.trainingHours() != null ? req.trainingHours()
        : (parsedHours == null ? -1 : parsedHours);
    if (trainingHours <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "未解析到该等级培训参考学时（1.8），请手工填写 trainingHours");
    }
    int wordsPerHour = req.wordsPerHour() != null ? req.wordsPerHour() : DEFAULT_WORDS_PER_HOUR;

    // 同分册查重（唯一键兜底 409）
    Long exists = projectMapper.selectCount(new LambdaQueryWrapper<TextbookProjectEntity>()
        .eq(TextbookProjectEntity::getWorkTypeTitle, workTypeTitle)
        .eq(TextbookProjectEntity::getDirection, direction)
        .eq(TextbookProjectEntity::getLevelCode, req.levelCode()));
    if (exists != null && exists > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该分册已存在（同工种×方向×等级），请勿重复创建");
    }

    List<String> warnings = new ArrayList<>(parsed.getParseWarnings());
    if (parsed.getSkillWeights() == null) {
      warnings.add("WARN: 未解析到技能要求权重表（一期配比不依赖，可忽略）");
    }

    TextbookProjectEntity project = new TextbookProjectEntity();
    project.setWorkTypeTitle(workTypeTitle);
    project.setSeriesNo(parsed.getSeriesNo());
    project.setStandardVersion(parsed.getVersion());
    project.setFileId(req.fileId());
    project.setFileHash(sha256(content));
    project.setDirection(direction);
    project.setLevelCode(req.levelCode());
    project.setFunctionNosJson(writeJson(new LinkedHashSet<>(functionNos)));
    project.setTrainingHours(trainingHours);
    project.setWordsPerHour(wordsPerHour);
    project.setTotalWords(trainingHours * wordsPerHour);
    project.setStatus(STATUS_PARSED);
    project.setParseWarnings(writeJson(warnings));
    try {
      projectMapper.insert(project);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.CONFLICT, "该分册已存在（同工种×方向×等级），请勿重复创建");
    }

    List<String> rebuildWarnings = outlineService.regenerate(project, parsed, false);
    if (!rebuildWarnings.isEmpty()) {
      warnings.addAll(rebuildWarnings);
      project.setParseWarnings(writeJson(warnings));
    }
    project.setStatus(STATUS_OUTLINED);
    projectMapper.updateById(project);
    log.info("[教材] 创建工程：projectId={} 工种={} 方向={} 等级={} 总字数={}",
        project.getId(), workTypeTitle, direction, req.levelCode(), project.getTotalWords());
    return detail(project.getId());
  }

  // ============================================================
  // 列表 / 详情
  // ============================================================

  @Transactional(readOnly = true)
  public PageResponse<TextbookProjectVO> page(ProjectQueryReq req) {
    LambdaQueryWrapper<TextbookProjectEntity> wrapper =
        new LambdaQueryWrapper<TextbookProjectEntity>()
            .like(req.getWorkTypeTitle() != null && !req.getWorkTypeTitle().isBlank(),
                TextbookProjectEntity::getWorkTypeTitle, req.getWorkTypeTitle())
            .eq(req.getDirection() != null && !req.getDirection().isBlank(),
                TextbookProjectEntity::getDirection, req.getDirection())
            .eq(req.getLevelCode() != null, TextbookProjectEntity::getLevelCode,
                req.getLevelCode())
            .eq(req.getStatus() != null && !req.getStatus().isBlank(),
                TextbookProjectEntity::getStatus, req.getStatus())
            .orderByDesc(TextbookProjectEntity::getId);
    Page<TextbookProjectEntity> result = projectMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, String> fileNames = fileNames(result.getRecords());
    List<TextbookProjectVO> items = new ArrayList<>();
    for (TextbookProjectEntity project : result.getRecords()) {
      items.add(toVO(project, fileNames, false));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public TextbookProjectVO detail(Long id) {
    TextbookProjectEntity project = findProject(id);
    return toVO(project, fileNames(List.of(project)), true);
  }

  // ============================================================
  // 更新（接口 5）：名称/系数/功能映射；结构变化自动重建骨架
  // ============================================================

  @Transactional
  public TextbookProjectVO update(Long id, ProjectUpdateReq req) {
    TextbookProjectEntity project = findProject(id);
    if (req.workTypeTitle() != null && !req.workTypeTitle().isBlank()) {
      project.setWorkTypeTitle(req.workTypeTitle().trim());
    }
    boolean structural = req.trainingHours() != null || req.wordsPerHour() != null
        || (req.functionNos() != null && !req.functionNos().isEmpty());
    List<String> warnings = readWarnings(project.getParseWarnings());
    if (structural) {
      ParsedStandard parsed = parseAndValidate(
          fileApplicationService.readFileBytesById(project.getFileId()), null);
      StandardLevelDoc level = parsed.levelOf(5 - project.getLevelCode());
      if (level == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "标准中不存在该工程等级");
      }
      if (req.functionNos() != null && !req.functionNos().isEmpty()) {
        validateFunctionNos(req.functionNos(), level.getWorkFunctions().size());
        project.setFunctionNosJson(writeJson(new LinkedHashSet<>(req.functionNos())));
      }
      if (req.trainingHours() != null) {
        project.setTrainingHours(req.trainingHours());
      }
      if (req.wordsPerHour() != null) {
        project.setWordsPerHour(req.wordsPerHour());
      }
      project.setTotalWords(project.getTrainingHours() * project.getWordsPerHour());
      warnings.addAll(outlineService.regenerate(project, parsed, true));
      project.setParseWarnings(writeJson(warnings));
      project.setStatus(STATUS_OUTLINED);
    }
    applyStatusTransition(project, req.status());
    try {
      projectMapper.updateById(project);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.CONFLICT, "该分册已存在（同工种×方向×等级）");
    }
    return detail(id);
  }

  // ============================================================
  // 删除（接口 6）/ 重建（接口 7）
  // ============================================================

  @Transactional
  public void delete(Long id) {
    TextbookProjectEntity project = findProject(id);
    List<Long> nodeIds = nodeMapper.selectList(
            new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
                .eq(TextbookOutlineNodeEntity::getProjectId, id))
        .stream().map(TextbookOutlineNodeEntity::getId).toList();
    if (!nodeIds.isEmpty()) {
      nodeMapper.deleteBatchIds(nodeIds);
    }
    projectMapper.deleteById(project.getId());
    log.info("[教材] 删除工程：projectId={} 级联删除大纲节点={}", id, nodeIds.size());
  }

  @Transactional
  public OutlineSummaryVO rebuild(Long id) {
    TextbookProjectEntity project = findProject(id);
    ParsedStandard parsed = parseAndValidate(
        fileApplicationService.readFileBytesById(project.getFileId()), null);
    List<String> warnings = readWarnings(project.getParseWarnings());
    warnings.add("INFO: 手动重建骨架（未人工调整的字数已被覆盖）");
    warnings.addAll(outlineService.regenerate(project, parsed, true));
    project.setParseWarnings(writeJson(warnings));
    project.setStatus(STATUS_OUTLINED);
    projectMapper.updateById(project);
    OutlineSummaryVO summary = outlineService.summary(project);
    log.info("[教材] 重建骨架：projectId={} balanced={}", id, summary.balanced());
    return summary;
  }

  // ============================================================
  // 导出（接口 12）
  // ============================================================

  @Transactional(readOnly = true)
  public ExportFile export(Long id, String format) {
    TextbookProjectEntity project = findProject(id);
    List<TextbookOutlineNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, id)
            .orderByAsc(TextbookOutlineNodeEntity::getParentId)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    OutlineSummaryVO summary = outlineService.summary(project);
    return new TextbookOutlineExporter().export(project, nodes, summary, format);
  }

  // ============================================================
  // 内部工具
  // ============================================================

  /** docx 校验 + 解析；理论权重表是配比依据，缺失即 400（设计 §2.4-①） */
  private ParsedStandard parseAndValidate(byte[] content, String originalName) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容为空");
    }
    if (originalName != null && !originalName.toLowerCase().endsWith(".docx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 .docx 文件");
    }
    if (content.length >= 4 && (content[0] & 0xFF) == 0xD0 && (content[1] & 0xFF) == 0xCF
        && (content[2] & 0xFF) == 0x11 && (content[3] & 0xFF) == 0xE0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "该文件实为 Word 97-2003（.doc）旧格式，请另存为 .docx 后重新上传");
    }
    ParsedStandard parsed;
    try {
      parsed = new DocxStandardParser().parse(new ByteArrayInputStream(content));
    } catch (java.io.IOException e) {
      log.warn("[教材] docx 解析失败: {}", e.getMessage());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "docx 解析失败：" + e.getMessage());
    } catch (POIXMLException e) {
      log.warn("[教材] docx 文件损坏或格式不受支持: {}", e.getMessage());
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "docx 文件损坏或格式不受支持，请用 Word/WPS 另存为标准 .docx 后重试");
    }
    boolean hasError = parsed.getParseWarnings().stream().anyMatch(w -> w.startsWith("ERROR"));
    if (hasError || parsed.getLevels().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "文档不符合国家职业技能标准格式：" + String.join("；", parsed.getParseWarnings()));
    }
    // 理论权重表是配比依据，缺失即阻断（设计 §2.4-①：禁止进入下一步，返回 400 + warnings）
    if (parsed.getTheoryWeights() == null) {
      List<String> warnings = new ArrayList<>(parsed.getParseWarnings());
      warnings.add("ERROR: 未解析到理论知识权重表，无法生成篇幅配比");
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "未解析到理论知识权重表，无法生成篇幅配比", warnings);
    }
    return parsed;
  }

  private void validateFunctionNos(List<Integer> functionNos, int functionCount) {
    if (functionNos == null || functionNos.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "方向映射未识别，请人工登记职业功能序号（functionNos）");
    }
    for (Integer no : functionNos) {
      if (no == null || no < 1 || no > functionCount) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "职业功能序号越界：" + no + "（该等级实际存在 1~" + functionCount + "）");
      }
    }
  }

  private void applyStatusTransition(TextbookProjectEntity project, String status) {
    if (status == null || status.isBlank()) {
      return;
    }
    switch (status) {
      case STATUS_REVIEWED -> {
        if (!STATUS_OUTLINED.equals(project.getStatus())
            && !STATUS_PARSED.equals(project.getStatus())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "当前状态不允许流转为 reviewed：" + project.getStatus());
        }
        project.setStatus(STATUS_REVIEWED);
      }
      case STATUS_OUTLINED -> project.setStatus(STATUS_OUTLINED);
      // parsed 为管理端"打回重建"兜底态（骨架未建的创建失败态），允许从 outlined/reviewed 回退
      case STATUS_PARSED -> {
        if (!STATUS_OUTLINED.equals(project.getStatus())
            && !STATUS_REVIEWED.equals(project.getStatus())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "当前状态不允许流转为 parsed：" + project.getStatus());
        }
        project.setStatus(STATUS_PARSED);
      }
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 status：" + status);
    }
  }

  private TextbookProjectEntity findProject(Long id) {
    TextbookProjectEntity project = projectMapper.selectOne(
        new LambdaQueryWrapper<TextbookProjectEntity>()
            .eq(TextbookProjectEntity::getId, id));
    if (project == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "教材工程不存在");
    }
    return project;
  }

  private Map<Long, String> fileNames(List<TextbookProjectEntity> projects) {
    List<Long> fileIds = projects.stream()
        .map(TextbookProjectEntity::getFileId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    if (fileIds.isEmpty()) {
      return Map.of();
    }
    // 直查 FileMapper（与细目表 batchFileNames 同范式）：同事务可见未提交的文件行，
    // 且不经 FileApplicationService.getById 的 NOT_SUPPORTED 边界——后者会挂起当前事务
    // 重取连接（弱网/池紧张时显著拖慢），并在创建事务内必然读不到刚 insert 的文件行。
    Map<Long, String> names = new LinkedHashMap<>();
    for (FileEntity file : fileMapper.selectBatchIds(fileIds)) {
      names.put(file.getId(), file.getOriginalName());
    }
    return names;
  }

  private TextbookProjectVO toVO(TextbookProjectEntity project, Map<Long, String> fileNames,
      boolean withStats) {
    Integer chapterCount = null;
    Integer sectionCount = null;
    Integer pointCount = null;
    OutlineSummaryVO summary = null;
    if (withStats) {
      chapterCount = countOf(project.getId(), TextbookOutlineApplicationService.LEVEL_CHAPTER);
      sectionCount = countOf(project.getId(), TextbookOutlineApplicationService.LEVEL_SECTION);
      pointCount = countOf(project.getId(), TextbookOutlineApplicationService.LEVEL_POINT);
      summary = outlineService.summary(project);
    }
    return new TextbookProjectVO(
        project.getId() == null ? null : String.valueOf(project.getId()),
        project.getWorkTypeTitle(),
        project.getSeriesNo(),
        project.getStandardVersion(),
        project.getFileId() == null ? null : String.valueOf(project.getFileId()),
        project.getFileId() == null ? null : fileNames.get(project.getFileId()),
        project.getDirection(),
        project.getLevelCode(),
        outlineService.readFunctionNos(project.getFunctionNosJson()),
        project.getTrainingHours(),
        project.getWordsPerHour(),
        project.getTotalWords(),
        project.getStatus(),
        project.getParseWarnings(),
        chapterCount,
        sectionCount,
        pointCount,
        summary,
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private Integer countOf(Long projectId, int nodeLevel) {
    Long count = nodeMapper.selectCount(new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
        .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
        .eq(TextbookOutlineNodeEntity::getNodeLevel, nodeLevel));
    return count == null ? 0 : count.intValue();
  }

  private List<String> readWarnings(String json) {
    if (json == null || json.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return new ArrayList<>(objectMapper.readValue(json,
          new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
          }));
    } catch (Exception e) {
      log.warn("[教材] parse_warnings 解析失败: {}, json={}", e.getMessage(), json);
      return new ArrayList<>();
    }
  }

  private String writeJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JSON 序列化失败", e);
    }
  }

  private static String sha256(byte[] content) {
    try {
      return java.util.HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}

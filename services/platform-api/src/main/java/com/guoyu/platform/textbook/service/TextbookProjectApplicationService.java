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
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionAssetEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentVersionEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionReviewRecordEntity;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionAssetMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentVersionMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionReviewRecordMapper;
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
import java.util.Set;
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
  /** 二期新增工程状态（方案 §3.1）：首个有效章分配创建 → writing；教研完成 → done。 */
  public static final String STATUS_WRITING = "writing";
  public static final String STATUS_DONE = "done";
  private static final int DEFAULT_WORDS_PER_HOUR = 2500;

  private final TextbookProjectMapper projectMapper;
  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookOutlineApplicationService outlineService;
  private final FileApplicationService fileApplicationService;
  private final FileMapper fileMapper;
  private final ObjectMapper objectMapper;
  private final TextbookProjectLockService lockService;
  private final TextbookStructureGuard structureGuard;
  private final TextbookChapterAssignmentMapper assignmentMapper;
  private final TextbookSectionContentMapper sectionContentMapper;
  private final TextbookSectionContentVersionMapper sectionVersionMapper;
  private final TextbookSectionReviewRecordMapper sectionReviewRecordMapper;
  private final TextbookSectionAssetMapper sectionAssetMapper;

  public TextbookProjectApplicationService(TextbookProjectMapper projectMapper,
      TextbookOutlineNodeMapper nodeMapper,
      TextbookOutlineApplicationService outlineService,
      FileApplicationService fileApplicationService,
      FileMapper fileMapper,
      ObjectMapper objectMapper,
      TextbookProjectLockService lockService,
      TextbookStructureGuard structureGuard,
      TextbookChapterAssignmentMapper assignmentMapper,
      TextbookSectionContentMapper sectionContentMapper,
      TextbookSectionContentVersionMapper sectionVersionMapper,
      TextbookSectionReviewRecordMapper sectionReviewRecordMapper,
      TextbookSectionAssetMapper sectionAssetMapper) {
    this.projectMapper = projectMapper;
    this.nodeMapper = nodeMapper;
    this.outlineService = outlineService;
    this.fileApplicationService = fileApplicationService;
    this.fileMapper = fileMapper;
    this.objectMapper = objectMapper;
    this.lockService = lockService;
    this.structureGuard = structureGuard;
    this.assignmentMapper = assignmentMapper;
    this.sectionContentMapper = sectionContentMapper;
    this.sectionVersionMapper = sectionVersionMapper;
    this.sectionReviewRecordMapper = sectionReviewRecordMapper;
    this.sectionAssetMapper = sectionAssetMapper;
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

    List<String> rebuildWarnings =
        outlineService.regenerate(project, parsed, false).warnings();
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
    Map<Long, CollaborationStats> stats = collaborationStats(result.getRecords());
    List<TextbookProjectVO> items = new ArrayList<>();
    for (TextbookProjectEntity project : result.getRecords()) {
      items.add(toVO(project, fileNames, false, stats.get(project.getId())));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public TextbookProjectVO detail(Long id) {
    TextbookProjectEntity project = findProject(id);
    return toVO(project, fileNames(List.of(project)), true,
        collaborationStats(List.of(project)).get(id));
  }

  /** 二期列表批量统计（方案 §10.2）：禁止逐工程 N+1；内容查询只选 project/status 两列，不拉 LONGTEXT 正文。 */
  private Map<Long, CollaborationStats> collaborationStats(List<TextbookProjectEntity> projects) {
    if (projects == null || projects.isEmpty()) {
      return Map.of();
    }
    List<Long> projectIds = projects.stream()
        .map(TextbookProjectEntity::getId).distinct().toList();
    Map<Long, Integer> assignedByProject = new LinkedHashMap<>();
    for (TextbookChapterAssignmentEntity assignment : assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .select(TextbookChapterAssignmentEntity::getProjectId)
            .in(TextbookChapterAssignmentEntity::getProjectId, projectIds)
            .eq(TextbookChapterAssignmentEntity::getActive, 1))) {
      assignedByProject.merge(assignment.getProjectId(), 1, Integer::sum);
    }
    Map<Long, Integer> approvedByProject = new LinkedHashMap<>();
    Set<Long> projectsWithContent = new LinkedHashSet<>();
    for (TextbookSectionContentEntity content : sectionContentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .select(TextbookSectionContentEntity::getProjectId,
                TextbookSectionContentEntity::getContentStatus)
            .in(TextbookSectionContentEntity::getProjectId, projectIds))) {
      projectsWithContent.add(content.getProjectId());
      if (TextbookSectionContentApplicationService.STATUS_APPROVED
          .equals(content.getContentStatus())) {
        approvedByProject.merge(content.getProjectId(), 1, Integer::sum);
      }
    }
    Map<Long, CollaborationStats> result = new LinkedHashMap<>();
    for (Long projectId : projectIds) {
      int assigned = assignedByProject.getOrDefault(projectId, 0);
      boolean hasContent = projectsWithContent.contains(projectId);
      result.put(projectId, new CollaborationStats(assigned,
          approvedByProject.getOrDefault(projectId, 0), hasContent, hasContent || assigned > 0));
    }
    return result;
  }

  private record CollaborationStats(int assignedChapterCount, int approvedSectionCount,
                                    boolean hasContent, boolean structureLocked) {
  }

  // ============================================================
  // 更新（接口 5）：名称/系数/功能映射；结构变化自动重建骨架
  // 二期收紧（方案 §3.5/§5.7）：状态白名单 + 结构锁 + 同请求结构/状态互斥
  // ============================================================

  @Transactional
  public TextbookProjectVO update(Long id, ProjectUpdateReq req) {
    // 统一工程行锁（方案 §7.1）：旧 PUT 与 C3-C6/重建/删除互斥
    TextbookProjectEntity project = lockService.lockProject(id);
    boolean structural = req.trainingHours() != null || req.wordsPerHour() != null
        || (req.functionNos() != null && !req.functionNos().isEmpty());
    boolean statusChange = req.status() != null && !req.status().isBlank();
    if (structural && statusChange) {
      // 结构变更后必须单独发起评审，不得同一请求重建并立即 reviewed（方案 §3.5）
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "同一请求不允许同时携带结构字段与 status，请先保存结构变化再单独发起评审");
    }
    if (req.workTypeTitle() != null && !req.workTypeTitle().isBlank()) {
      // 工程名称：非 done 允许（方案 §5.7）
      structureGuard.assertSafeEditAllowed(project, "修改工程名称");
      project.setWorkTypeTitle(req.workTypeTitle().trim());
    }
    List<String> warnings = readWarnings(project.getParseWarnings());
    if (structural) {
      // 改 functionNos/学时/系数触发 regenerate：仅 outlined 且未结构锁定（§5.7）；
      // writing/done 一律 409；reviewed 必须先显式撤回为 outlined
      structureGuard.assertIdentityChangeAllowed(project, "修改学时/系数/功能映射触发的重建");
      if (!STATUS_OUTLINED.equals(project.getStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "结构重建仅 outlined 工程允许，当前：" + project.getStatus());
      }
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
      warnings.addAll(applyRegenerate(project, parsed));
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

  /**
   * 旧 PUT 状态白名单（方案 §3.5）：
   * <ul>
   *   <li>outlined → reviewed：一期「大纲评审通过」唯一入口</li>
   *   <li>reviewed → outlined：仅未结构锁定时允许（撤回大纲评审）</li>
   *   <li>writing → done / done → writing：只允许 C5/C6，旧 PUT 一律 409</li>
   *   <li>writing/done → outlined/parsed/reviewed：一律 409</li>
   * </ul>
   */
  private void applyStatusTransition(TextbookProjectEntity project, String status) {
    if (status == null || status.isBlank()) {
      return;
    }
    String current = project.getStatus();
    switch (status) {
      case STATUS_REVIEWED -> {
        if (!STATUS_OUTLINED.equals(current)) {
          throw new BusinessException(ErrorCode.CONFLICT,
              "仅 outlined 工程可评审通过为 reviewed，当前：" + current
                  + "（writing/done 请走编写管理）");
        }
        project.setStatus(STATUS_REVIEWED);
      }
      case STATUS_OUTLINED -> {
        if (STATUS_DONE.equals(current)) {
          throw new BusinessException(ErrorCode.CONFLICT,
              "工程已完成（done），旧接口不允许回退，请先重新打开为 writing");
        }
        if (structureGuard.isLocked(project.getId())) {
          throw new BusinessException(ErrorCode.CONFLICT,
              (STATUS_REVIEWED.equals(current) ? "已存在章节分配或正文，不能撤回大纲评审（结构已锁定）"
                  : "工程仍有有效分配或正文，不能从 writing 撤回为 outlined；如需重建请先取消全部分配且确认无正文"));
        }
        project.setStatus(STATUS_OUTLINED);
      }
      // parsed 为管理端"打回重建"兜底态；writing/done 禁止经旧 PUT 越级
      case STATUS_PARSED -> {
        if (STATUS_WRITING.equals(current) || STATUS_DONE.equals(current)) {
          throw new BusinessException(ErrorCode.CONFLICT,
              "工程已进入编写阶段（" + current + "），旧接口不允许回退为 parsed");
        }
        if (structureGuard.isLocked(project.getId())) {
          throw new BusinessException(ErrorCode.CONFLICT,
              "已存在章节分配或正文，不允许回退为 parsed（结构已锁定）");
        }
        project.setStatus(STATUS_PARSED);
      }
      case STATUS_WRITING, STATUS_DONE -> throw new BusinessException(ErrorCode.CONFLICT,
          "writing/done 流转仅允许编写管理接口（complete/reopen），旧 PUT 禁止");
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 status：" + status);
    }
  }

  /** 重建后的分配行处理（方案 §5.7 分配唯一键规则）：保留章保留 active=0 行，移除章软删。 */
  private List<String> applyRegenerate(TextbookProjectEntity project, ParsedStandard parsed) {
    TextbookOutlineApplicationService.RegenerateResult result =
        outlineService.regenerate(project, parsed, true);
    if (!result.removedChapterIds().isEmpty()) {
      List<TextbookChapterAssignmentEntity> removed = assignmentMapper.selectList(
          new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
              .in(TextbookChapterAssignmentEntity::getChapterId, result.removedChapterIds()));
      if (!removed.isEmpty()) {
        assignmentMapper.deleteBatchIds(
            removed.stream().map(TextbookChapterAssignmentEntity::getId).toList());
      }
    }
    // 保留章：active=0 分配行原样保留，供下次分配复用（防 (tenant_id,chapter_id,deleted) 唯一键冲突）
    return result.warnings();
  }

  // ============================================================
  // 删除（接口 6）/ 重建（接口 7）
  // ============================================================

  /**
   * 删除工程：done 先由教研重开（§5.7）；任务、正文、快照、审定记录、资产元数据级联软删，
   * 资产文件按 file 模块提交后清理。
   */
  @Transactional
  public void delete(Long id) {
    TextbookProjectEntity project = lockService.lockProject(id);
    if (STATUS_DONE.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程已完成（done），请先重新打开为 writing 再删除");
    }
    List<Long> nodeIds = nodeMapper.selectList(
            new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
                .eq(TextbookOutlineNodeEntity::getProjectId, id))
        .stream().map(TextbookOutlineNodeEntity::getId).toList();
    if (!nodeIds.isEmpty()) {
      nodeMapper.deleteBatchIds(nodeIds);
    }
    cascadeDeleteCollaboration(id, project.getTenantId());
    projectMapper.deleteById(project.getId());
    log.info("[教材] 删除工程：projectId={} 级联删除大纲节点={}", id, nodeIds.size());
  }

  /** 二期新表级联软删 + 资产文件提交后清理（方案 §5.7 删除行）。 */
  private void cascadeDeleteCollaboration(Long projectId, Long tenantId) {
    List<TextbookSectionAssetEntity> assets = sectionAssetMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionAssetEntity>()
            .eq(TextbookSectionAssetEntity::getProjectId, projectId));
    if (!assets.isEmpty()) {
      sectionAssetMapper.deleteBatchIds(
          assets.stream().map(TextbookSectionAssetEntity::getId).toList());
      for (TextbookSectionAssetEntity asset : assets) {
        fileApplicationService.deleteAfterCommitForTenant(tenantId, asset.getFileId());
      }
    }
    // content/version/review/assignment 都带 project_id 或可由 content 推导
    List<TextbookSectionContentEntity> contents = sectionContentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .eq(TextbookSectionContentEntity::getProjectId, projectId));
    if (!contents.isEmpty()) {
      List<Long> contentIds = contents.stream()
          .map(TextbookSectionContentEntity::getId).toList();
      List<Long> sectionIds = contents.stream()
          .map(TextbookSectionContentEntity::getSectionId).toList();
      sectionVersionMapper.delete(new LambdaQueryWrapper<TextbookSectionContentVersionEntity>()
          .in(TextbookSectionContentVersionEntity::getContentId, contentIds));
      sectionReviewRecordMapper.delete(new LambdaQueryWrapper<TextbookSectionReviewRecordEntity>()
          .in(TextbookSectionReviewRecordEntity::getSectionId, sectionIds));
      sectionContentMapper.deleteBatchIds(contentIds);
    }
    List<TextbookChapterAssignmentEntity> assignments = assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getProjectId, projectId));
    if (!assignments.isEmpty()) {
      assignmentMapper.deleteBatchIds(
          assignments.stream().map(TextbookChapterAssignmentEntity::getId).toList());
    }
  }

  /** 手动重建：仅 parsed/outlined 且未锁定（§5.7）；writing/done/reviewed 一律 409。 */
  @Transactional
  public OutlineSummaryVO rebuild(Long id) {
    TextbookProjectEntity project = lockService.lockProject(id);
    structureGuard.assertIdentityChangeAllowed(project, "手动重建骨架");
    if (!STATUS_OUTLINED.equals(project.getStatus())
        && !STATUS_PARSED.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "手动重建仅 parsed/outlined 工程允许，当前：" + project.getStatus());
    }
    ParsedStandard parsed = parseAndValidate(
        fileApplicationService.readFileBytesById(project.getFileId()), null);
    List<String> warnings = readWarnings(project.getParseWarnings());
    warnings.add("INFO: 手动重建骨架（未人工调整的字数已被覆盖）");
    warnings.addAll(applyRegenerate(project, parsed));
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
      boolean withStats, CollaborationStats collaborationStats) {
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
    CollaborationStats stats = collaborationStats == null
        ? new CollaborationStats(0, 0, false, false) : collaborationStats;
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
        stats.assignedChapterCount(),
        stats.approvedSectionCount(),
        stats.hasContent(),
        stats.structureLocked(),
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

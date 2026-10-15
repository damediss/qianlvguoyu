package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.helper.ExamCourseLevels;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ExamExamDetailApplicationService {

  private static final String PROPOSITION_TEMPLATE = "template/试题导入模板-命题.xlsx";
  /** 命题模板答案区起始列（G 列，0-based） */
  private static final int PROPOSITION_ANSWER_START_COL = 6;
  /** 最多横向展开的选项列数（G 起） */
  private static final int PROPOSITION_MAX_CHOICE_OPTIONS = 8;
  /** 命题模板数字题型码 → 题库题型码（与导出 mapTypeToPropositionCode 一一对应） */
  private static final Map<Integer, String> PROPOSITION_TYPE_BY_CODE = Map.of(
      1, "judge", 2, "single", 3, "fill", 4, "short", 5, "compute",
      6, "essay", 7, "multi", 8, "name", 9, "drawing", 10, "case");
  /** 命题模板选项单元格"内容、分值"后缀（分值可缺省） */
  private static final Pattern PROPOSITION_OPTION_SCORE_PATTERN =
      Pattern.compile("(.+)、([0-9]+(?:\\.[0-9]+)?)\\s*$");

  private final ExamExamDetailMapper mapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final ExamCourseMapper courseMapper;
  private final ExamWorkTypeMapper workTypeMapper;
  private final UserMapper userMapper;
  private final ExamKnowledgeStructureApplicationService knowledgeService;
  private final ObjectMapper objectMapper;

  public ExamExamDetailApplicationService(
      ExamExamDetailMapper mapper,
      ExamKnowledgeStructureMapper knowledgeMapper,
      ExamCourseMapper courseMapper,
      ExamWorkTypeMapper workTypeMapper,
      UserMapper userMapper,
      ExamKnowledgeStructureApplicationService knowledgeService,
      ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.knowledgeMapper = knowledgeMapper;
    this.courseMapper = courseMapper;
    this.workTypeMapper = workTypeMapper;
    this.userMapper = userMapper;
    this.knowledgeService = knowledgeService;
    this.objectMapper = objectMapper;
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
    Map<Integer, String> knowledgeTitleMap = buildKnowledgePaths(tenantId, kIds);

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
      Map<Integer, String> pathMap = buildKnowledgePaths(entity.getTenantId(), Set.of(entity.getKnowledgeId()));
      vo.setKnowledgeTitle(pathMap.getOrDefault(entity.getKnowledgeId(), ""));
    }
    return vo;
  }

  @CacheEvict(value = "questionBankTree", allEntries = true)
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

  @CacheEvict(value = "questionBankTree", allEntries = true)
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

  @CacheEvict(value = "questionBankTree", allEntries = true)
  @Transactional
  public void delete(Integer id) {
    findExisting(id);
    mapper.deleteById(id);
  }

  @CacheEvict(value = "questionBankTree", allEntries = true)
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
      return knowledgeService.getDescendantIds(req.getKnowledgeId());
    }
    if (req.getCourseId() != null && req.getCourseId() > 0) {
      return knowledgeService.getNodeIdsByCourse(req.getCourseId());
    }
    // workTypeId → 查该工种下所有科目 → 再查全部知识点节点（不限层级，含 L2 直挂题）
    if (req.getWorkTypeId() != null && req.getWorkTypeId() > 0) {
      List<Integer> courseIds = courseMapper.selectList(
          new LambdaQueryWrapper<ExamCourseEntity>()
              .eq(ExamCourseEntity::getTenantId, SecurityContext.currentUser().getTenantId().intValue())
              .eq(ExamCourseEntity::getWorkTypeId, req.getWorkTypeId())
              .select(ExamCourseEntity::getId))
          .stream().map(ExamCourseEntity::getId).toList();
      if (courseIds.isEmpty()) {
        return List.of();
      }
      // 批量查所有科目下的全部知识点节点
      List<Integer> allKnowledgeIds = new ArrayList<>();
      for (Integer cid : courseIds) {
        allKnowledgeIds.addAll(knowledgeService.getNodeIdsByCourse(cid));
      }
      return allKnowledgeIds.isEmpty() ? List.of() : allKnowledgeIds;
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

  /**
   * 构建知识点完整路径: 工种链/课程标题/知识点链(L0→L1→L2→L3)。
   * 例如: "母液蒸发工/母液蒸发工初级/基本要求/职业道德/职业道德基本知识/工匠精神培养"
   */
  private Map<Integer, String> buildKnowledgePaths(Integer tenantId, Set<Integer> kIds) {
    if (kIds.isEmpty()) return Map.of();

    // 1) 查当前租户全部知识点, 构建 id→entity Map
    List<ExamKnowledgeStructureEntity> allKs = knowledgeMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId));
    Map<Integer, ExamKnowledgeStructureEntity> ksMap = allKs.stream()
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, e -> e, (a, b) -> a));

    // 2) 收集涉及的 courseId, 批量查课程
    Set<Integer> courseIds = allKs.stream()
        .map(ExamKnowledgeStructureEntity::getCourseId)
        .filter(cid -> cid != null && cid > 0)
        .collect(Collectors.toSet());
    Map<Integer, ExamCourseEntity> courseMap = courseIds.isEmpty() ? Map.of()
        : courseMapper.selectBatchIds(courseIds.stream().toList()).stream()
            .collect(Collectors.toMap(ExamCourseEntity::getId, e -> e, (a, b) -> a));

    // 3) 查当前租户全部工种, 构建 id→entity Map (用于工种父链回溯)
    List<ExamWorkTypeEntity> allWt = workTypeMapper.selectList(
        new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .eq(ExamWorkTypeEntity::getTenantId, tenantId));
    Map<Integer, ExamWorkTypeEntity> wtMap = allWt.stream()
        .collect(Collectors.toMap(ExamWorkTypeEntity::getId, e -> e, (a, b) -> a));

    // 4) 逐个 kId 组装路径
    Map<Integer, String> result = new LinkedHashMap<>();
    for (Integer kId : kIds) {
      ExamKnowledgeStructureEntity ksNode = ksMap.get(kId);
      if (ksNode == null) {
        result.put(kId, "");
        continue;
      }

      // 知识点父链: 从当前节点回溯到根, 反转为 L0→L1→...→Ln
      List<String> ksChain = walkKsAncestors(ksNode, ksMap);

      // 课程标题
      String courseTitle = "";
      String workTypePath = "";
      if (ksNode.getCourseId() != null) {
        ExamCourseEntity course = courseMap.get(ksNode.getCourseId());
        if (course != null) {
          courseTitle = course.getTitle() != null ? course.getTitle() : "";
          // 工种父链
          if (course.getWorkTypeId() != null) {
            ExamWorkTypeEntity wtNode = wtMap.get(course.getWorkTypeId());
            if (wtNode != null) {
              workTypePath = walkWorkTypeAncestors(wtNode, wtMap);
            }
          }
        }
      }

      // 拼接: 工种链/课程/知识点链
      StringBuilder sb = new StringBuilder();
      if (!workTypePath.isEmpty()) sb.append(workTypePath).append('/');
      if (!courseTitle.isEmpty()) sb.append(courseTitle).append('/');
      sb.append(String.join("/", ksChain));
      result.put(kId, sb.toString());
    }
    return result;
  }

  /**
   * 知识点父链回溯: 从当前节点沿 parentId 走到根, 返回正序 [L0, L1, ..., Ln]。
   */
  private List<String> walkKsAncestors(ExamKnowledgeStructureEntity node,
      Map<Integer, ExamKnowledgeStructureEntity> ksMap) {
    List<String> chain = new ArrayList<>();
    ExamKnowledgeStructureEntity cur = node;
    int maxDepth = 10; // 防环
    while (cur != null && maxDepth-- > 0) {
      if (cur.getTitle() != null) chain.add(cur.getTitle());
      Integer pid = cur.getParentId();
      if (pid == null || pid <= 0) break;
      cur = ksMap.get(pid);
    }
    Collections.reverse(chain);
    return chain;
  }

  /**
   * 工种父链回溯: 从当前节点沿 parentId 走到根, 返回 "根/.../当前" 格式字符串。
   */
  private String walkWorkTypeAncestors(ExamWorkTypeEntity node,
      Map<Integer, ExamWorkTypeEntity> wtMap) {
    List<String> chain = new ArrayList<>();
    ExamWorkTypeEntity cur = node;
    int maxDepth = 10; // 防环
    while (cur != null && maxDepth-- > 0) {
      if (cur.getTitle() != null) chain.add(cur.getTitle());
      Integer pid = cur.getParentId();
      if (pid == null || pid <= 0) break;
      cur = wtMap.get(pid);
    }
    Collections.reverse(chain);
    return String.join("/", chain);
  }

  // ── Import from Excel ──
  @CacheEvict(value = "questionBankTree", allEntries = true)
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
      if (lastRow < 1) {
        log.info("Import: empty sheet for courseId={}", courseId);
        return 0;
      }

      // 按表头列名解析，兼容"序号"前置列、列顺序变化、选项 A-H、
      // 以及"参考答案"/"答案"等不同命名。不再硬编码列索引。
      Map<String, Integer> colMap = buildColumnMap(sheet.getRow(0));
      Integer typeCol = colMap.get("题型");
      Integer stemCol = colMap.get("题干");
      if (typeCol == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "Excel 表头缺少必需列“题型”。请使用系统导出的模板，或确保表头包含该列");
      }
      // 选项列 A-H：normalizeHeader 把 "A"/"选项A" 都归一为 "a"，
      // 故按 "a".."h" 查列索引
      Integer[] optCols = new Integer[8];
      for (int k = 0; k < 8; k++) {
        optCols[k] = colMap.get(String.valueOf((char) ('a' + k)));
      }
      Integer answerCol = firstNonNull(colMap.get("参考答案"), colMap.get("答案"));
      Integer resolveCol = firstNonNull(colMap.get("答案解析"), colMap.get("解析"));
      Integer difficultyCol = colMap.get("难度");
      Integer keywordCol = colMap.get("关键词");

      for (int i = 1; i <= lastRow; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String type = getCellString(row.getCell(typeCol));
        if (!StringUtils.hasText(type)) continue;

        String typeCode;
        String content;
        String answer;
        String resolve;
        Integer difficulty;
        String keyword;
        BigDecimal score = null;
        Integer knowledgeIdOverride = null;

        Integer propositionCode = parsePropositionTypeCode(type);
        if (propositionCode != null) {
          // 命题模板格式（与导出同款）：A=知识点编码 C=数字题型码 D=题干 E=难度 F=分值
          // G..N=选项（"内容、分值"），下一行在正确选项列写"是"。
          // 与平台标准对齐：题型存英文码，answer 存 JSON 选项数组（判分消费端口径）。
          typeCode = PROPOSITION_TYPE_BY_CODE.get(propositionCode);
          String seriesNo = getCellString(row.getCell(0));
          String stem = getCellString(row.getCell(3));
          difficulty = parseDifficulty(getCellString(row.getCell(4)));
          score = getCellDecimal(row.getCell(5));
          resolve = null;
          keyword = null;
          if (isChoicePropositionCode(propositionCode)) {
            answer = buildPropositionAnswerJson(
                readPropositionOptions(row), readPropositionMarkers(sheet.getRow(i + 1)));
          } else {
            // 非选择题：答案文本直接写在 G 列
            answer = getCellString(row.getCell(PROPOSITION_ANSWER_START_COL));
          }
          content = stem;
          knowledgeIdOverride = resolveKnowledgeBySeriesNo(seriesNo, courseId);
        } else {
          typeCode = mapTypeLabel(type);
          if (typeCode == null) {
            log.warn("Row {}: unknown type '{}', skipping", i, type);
            continue;
          }
          if (stemCol == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "Excel 表头缺少必需列“题干”。请使用系统导出的模板，或确保表头包含该列");
          }

          String stem = getCellString(row.getCell(stemCol));
          String[] opts = new String[8];
          for (int k = 0; k < 8; k++) {
            opts[k] = optCols[k] != null ? getCellString(row.getCell(optCols[k])) : null;
          }
          answer = answerCol != null ? getCellString(row.getCell(answerCol)) : null;
          resolve = resolveCol != null ? getCellString(row.getCell(resolveCol)) : null;
          difficulty = parseDifficulty(
              difficultyCol != null ? getCellString(row.getCell(difficultyCol)) : null);
          keyword = keywordCol != null ? getCellString(row.getCell(keywordCol)) : null;

          content = buildContent(stem, opts);
        }

        ExamExamDetailEntity entity = new ExamExamDetailEntity();
        entity.setTenantId(tenantId);
        entity.setType(typeCode);
        entity.setContent(content);
        entity.setAnswer(answer);
        entity.setResolve(resolve);
        entity.setKeyword(keyword);
        entity.setDifficulty(difficulty);
        entity.setSource(source != null ? source : 0);
        entity.setScore(score);
        entity.setIsPrivate(0);
        entity.setStatus(1);
        // 导入即通过两级审核（用户口径）：审核人/时间一并落库，直接进入练习/同步学题库
        LocalDateTime now = LocalDateTime.now();
        entity.setCheckStatus(1);
        entity.setCheckTime(now);
        entity.setCheckUserId(userId.intValue());
        entity.setFinalCheckStatus(1);
        entity.setFinalCheckTime(now);
        entity.setFinalCheckUserId(userId.intValue());
        entity.setSortNumber(1);
        entity.setUserId(userId.intValue());
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());

        // 命题模板按"知识点编码"列精确挂载；匹配不到或旧格式回退随机 L3
        if (knowledgeIdOverride != null) {
          entity.setKnowledgeId(knowledgeIdOverride);
        } else if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
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

  // ── Export to Excel (命题模板) ──
  @Transactional(readOnly = true)
  public ExportResultDTO exportByCourse(Integer courseId, List<Integer> ids) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    List<Integer> knowledgeIds = knowledgeService.getNodeIdsByCourse(courseId);
    if (knowledgeIds == null || knowledgeIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该科目下无知识点，无法导出");
    }

    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds)
        .orderByDesc(ExamExamDetailEntity::getSortNumber)
        .orderByDesc(ExamExamDetailEntity::getId);
    // 勾选导出：仅导出勾选的试题（勾选可跨页）；未勾选/空数组保持全量导出
    if (ids != null && !ids.isEmpty()) {
      wrapper.in(ExamExamDetailEntity::getId, ids);
    }
    List<ExamExamDetailEntity> records = mapper.selectList(wrapper);

    if (records.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该科目下无试题数据");
    }

    return buildPropositionExport(courseMapper.selectById(courseId), records, "");
  }

  /**
   * 按题型配额随机抽题导出（竞赛题库等场景）：仅从指定来源抽审核通过的题，
   * 不产生试卷/考试数据，导出格式与全量导出一致。
   * quotas 键为题型（single/multi/judge），值为配额；实际抽到数量经 sampledCounts 回传。
   */
  @Transactional(readOnly = true)
  public SampledExport exportSampledByQuotas(Integer courseId, Map<String, Integer> quotas, int source) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    List<Integer> knowledgeIds = knowledgeService.getNodeIdsByCourse(courseId);
    if (knowledgeIds == null || knowledgeIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该科目下无知识点，无法导出");
    }

    List<ExamExamDetailEntity> records = new ArrayList<>();
    StringBuilder sampledCounts = new StringBuilder();
    for (String type : List.of("single", "multi", "judge")) {
      Integer quota = quotas.get(type);
      if (quota == null || quota <= 0) {
        continue;
      }
      LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
          .eq(ExamExamDetailEntity::getTenantId, tenantId)
          .in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds)
          .eq(ExamExamDetailEntity::getType, type)
          .eq(ExamExamDetailEntity::getSource, source)
          .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
          .last("ORDER BY RAND() LIMIT " + quota);
      List<ExamExamDetailEntity> part = mapper.selectList(wrapper);
      records.addAll(part);
      if (sampledCounts.length() > 0) {
        sampledCounts.append(';');
      }
      sampledCounts.append(type).append('=').append(part.size());
    }

    if (records.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "按所选来源与题型未抽到任何试题，请调整配额或来源");
    }

    return new SampledExport(
        buildPropositionExport(courseMapper.selectById(courseId), records, "_抽题"),
        sampledCounts.toString());
  }

  /** 抽题导出结果：文件 + 实际抽到数量（形如 single=345;multi=230;judge=228，回传 X-Export-Sampled 响应头） */
  public record SampledExport(ExportResultDTO result, String sampledCounts) {
  }

  private ExportResultDTO buildPropositionExport(ExamCourseEntity course,
      List<ExamExamDetailEntity> records, String filenameSuffix) {
    Set<Integer> kIds = records.stream()
        .map(ExamExamDetailEntity::getKnowledgeId)
        .filter(kid -> kid != null && kid > 0)
        .collect(Collectors.toSet());
    Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap = kIds.isEmpty()
        ? Map.of()
        : knowledgeMapper.selectBatchIds(kIds.stream().toList()).stream()
            .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, e -> e, (a, b) -> a));

    try (InputStream in = openPropositionTemplate();
        Workbook workbook = WorkbookFactory.create(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.getSheetAt(0);
      clearRowsFrom(sheet, 1);

      int rowIdx = 1;
      for (ExamExamDetailEntity entity : records) {
        List<Integer> correctIndices = writePropositionQuestionRow(
            sheet, rowIdx, entity, course, knowledgeMap);
        rowIdx++;
        if (needsAnswerMarkerRow(entity.getType())) {
          writePropositionAnswerMarkerRow(sheet, rowIdx, correctIndices);
          rowIdx++;
        }
      }

      workbook.write(out);
      return new ExportResultDTO(out.toByteArray(), buildPropositionFilename(course, filenameSuffix));
    } catch (BusinessException e) {
      throw e;
    } catch (IOException e) {
      log.error("Failed to generate proposition Excel", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 文件生成失败: " + e.getMessage());
    }
  }

  private InputStream openPropositionTemplate() {
    ClassPathResource resource = new ClassPathResource(PROPOSITION_TEMPLATE);
    if (!resource.exists()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "命题导出模板不存在");
    }
    try {
      return resource.getInputStream();
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "命题导出模板读取失败");
    }
  }

  private void clearRowsFrom(Sheet sheet, int startRow) {
    int lastRow = sheet.getLastRowNum();
    for (int r = startRow; r <= lastRow; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      for (int c = 0; c <= row.getLastCellNum(); c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          cell.setBlank();
        }
      }
    }
  }

  /**
   * SaaS 题型码 → 命题模板数字码。
   * 库中实际存英文码（single/multi/judge…），导入兼容拼音码（danxuan/pandan…）。
   */
  private Integer mapTypeToPropositionCode(String type) {
    if (type == null) {
      return null;
    }
    return switch (type.trim()) {
      case "judge", "pandan" -> 1;
      case "single", "danxuan" -> 2;
      case "fill", "tiankong" -> 3;
      case "short", "jianda" -> 4;
      case "compute" -> 5;
      case "essay", "lunshu", "composite" -> 6;
      case "multi", "duoxuan" -> 7;
      case "name" -> 8;
      case "drawing", "drawing2" -> 9;
      case "case", "anli" -> 10;
      default -> null;
    };
  }

  private String mapDifficultyToPropositionLabel(Integer difficulty) {
    if (difficulty == null) {
      return "中";
    }
    return switch (difficulty) {
      case 1 -> "易";
      case 3 -> "难";
      default -> "中";
    };
  }

  private String resolveKnowledgeSeriesNo(
      Integer knowledgeId, Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap) {
    if (knowledgeId == null || knowledgeId <= 0) {
      return "";
    }
    ExamKnowledgeStructureEntity ks = knowledgeMap.get(knowledgeId);
    if (ks == null || !StringUtils.hasText(ks.getSeriesNo())) {
      return "";
    }
    return ks.getSeriesNo().trim();
  }

  private boolean needsAnswerMarkerRow(String type) {
    if (type == null) {
      return false;
    }
    return switch (type.trim()) {
      case "judge", "pandan", "single", "danxuan", "multi", "duoxuan" -> true;
      default -> false;
    };
  }

  /**
   * 写入命题模板题目行，返回正确选项索引（0-based，相对 G 列）。
   */
  private List<Integer> writePropositionQuestionRow(
      Sheet sheet,
      int rowIdx,
      ExamExamDetailEntity entity,
      ExamCourseEntity course,
      Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
      row = sheet.createRow(rowIdx);
    }

    setCellString(row, 0, resolveKnowledgeSeriesNo(entity.getKnowledgeId(), knowledgeMap));
    // 模板 B 列表头为"级别（5：初级…1：高级技师）"，写国标等级；course.level 为 0~4 序数需换算
    Integer nationalGrade = ExamCourseLevels.toNationalGrade(course != null ? course.getLevel() : null);
    if (nationalGrade != null) {
      setCellNumeric(row, 1, nationalGrade);
    }

    Integer typeCode = mapTypeToPropositionCode(entity.getType());
    if (typeCode == null) {
      log.warn("Proposition export: unknown type '{}' for exam detail id={}",
          entity.getType(), entity.getId());
    } else {
      setCellNumeric(row, 2, typeCode);
    }

    String[] parts = splitContent(stripHtml(entity.getContent()));
    setCellString(row, 3, parts[0]);
    setCellString(row, 4, mapDifficultyToPropositionLabel(entity.getDifficulty()));

    BigDecimal score = entity.getScore();
    if (score != null) {
      setCellNumeric(row, 5, score.doubleValue());
    }

    return writePropositionAnswerCells(row, entity.getType(), parts, entity.getAnswer());
  }

  private List<Integer> writePropositionAnswerCells(
      Row row, String type, String[] parts, String answer) {
    String normalized = type != null ? type.trim() : "";

    // 优先解析 answer JSON 选项数组：[{content, isCorrect, score}, ...]
    List<Map<String, Object>> jsonOptions = parseAnswerOptionsJson(answer);
    if (!jsonOptions.isEmpty()) {
      List<Integer> correctIndices = new ArrayList<>();
      int limit = Math.min(jsonOptions.size(), PROPOSITION_MAX_CHOICE_OPTIONS);
      for (int i = 0; i < limit; i++) {
        Map<String, Object> opt = jsonOptions.get(i);
        setCellString(row, PROPOSITION_ANSWER_START_COL + i, optionContentText(opt.get("content")));
        if (isOptionCorrect(opt.get("isCorrect"))) {
          correctIndices.add(i);
        }
      }
      return correctIndices;
    }

    if ("judge".equals(normalized) || "pandan".equals(normalized)) {
      String judgment = resolveJudgmentAnswer(answer);
      setCellString(row, PROPOSITION_ANSWER_START_COL, judgment);
      return List.of(0);
    }
    if ("single".equals(normalized) || "danxuan".equals(normalized)
        || "multi".equals(normalized) || "duoxuan".equals(normalized)) {
      // 兼容旧格式：content 中 A./B./C./D. 拆分 + answer 字母/数字
      List<Integer> correctIndices = parseChoiceAnswerIndices(answer);
      boolean wroteAny = false;
      for (int i = 0; i < PROPOSITION_MAX_CHOICE_OPTIONS; i++) {
        String optionText = parts.length > i + 1 ? nvl(parts[i + 1]) : "";
        if (!StringUtils.hasText(optionText)) {
          continue;
        }
        setCellString(row, PROPOSITION_ANSWER_START_COL + i, optionText);
        wroteAny = true;
      }
      if (!wroteAny && StringUtils.hasText(answer)) {
        setCellString(row, PROPOSITION_ANSWER_START_COL, answer);
      }
      return correctIndices;
    }
    setCellString(row, PROPOSITION_ANSWER_START_COL, nvl(answer));
    return List.of();
  }

  private List<Map<String, Object>> parseAnswerOptionsJson(String answerJson) {
    if (!StringUtils.hasText(answerJson)) {
      return List.of();
    }
    String trimmed = answerJson.trim();
    if (!trimmed.startsWith("[")) {
      return List.of();
    }
    try {
      List<Map<String, Object>> options = objectMapper.readValue(
          trimmed, new TypeReference<List<Map<String, Object>>>() {});
      return options != null ? options : List.of();
    } catch (Exception e) {
      log.debug("answer is not options JSON, fallback. err={}", e.getMessage());
      return List.of();
    }
  }

  private String optionContentText(Object content) {
    if (content == null) {
      return "";
    }
    if (content instanceof Number n) {
      // 避免 85.0 这类浮点尾巴；整数写整数字符串
      if (n instanceof Double d && d == Math.rint(d)) {
        return String.valueOf(d.longValue());
      }
      if (n instanceof Float f && f == Math.rint(f)) {
        return String.valueOf(f.longValue());
      }
      return String.valueOf(content);
    }
    return stripHtml(String.valueOf(content));
  }

  private String stripHtml(String html) {
    if (!StringUtils.hasText(html)) {
      return "";
    }
    return Jsoup.parse(html).text().trim();
  }

  private boolean isOptionCorrect(Object isCorrect) {
    if (isCorrect instanceof Boolean b) {
      return b;
    }
    if (isCorrect instanceof Number n) {
      return n.intValue() != 0;
    }
    if (isCorrect == null) {
      return false;
    }
    String s = String.valueOf(isCorrect).trim();
    return "true".equalsIgnoreCase(s) || "1".equals(s) || "是".equals(s);
  }

  private void writePropositionAnswerMarkerRow(
      Sheet sheet, int rowIdx, List<Integer> correctIndices) {
    if (correctIndices == null || correctIndices.isEmpty()) {
      return;
    }
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
      row = sheet.createRow(rowIdx);
    }
    for (Integer idx : correctIndices) {
      if (idx != null && idx >= 0 && idx < PROPOSITION_MAX_CHOICE_OPTIONS) {
        setCellString(row, PROPOSITION_ANSWER_START_COL + idx, "是");
      }
    }
  }

  private String resolveJudgmentAnswer(String answer) {
    if (!StringUtils.hasText(answer)) {
      return "";
    }
    String normalized = answer.trim().toUpperCase();
    if ("A".equals(normalized) || "对".equals(normalized) || "正确".equals(normalized)
        || "是".equals(normalized) || "TRUE".equals(normalized) || "T".equals(normalized)
        || "1".equals(normalized)) {
      return "对";
    }
    if ("B".equals(normalized) || "错".equals(normalized) || "错误".equals(normalized)
        || "否".equals(normalized) || "FALSE".equals(normalized) || "F".equals(normalized)
        || "0".equals(normalized)) {
      return "错";
    }
    return answer.trim();
  }

  private List<Integer> parseChoiceAnswerIndices(String answer) {
    if (!StringUtils.hasText(answer)) {
      return List.of();
    }
    Set<Integer> indices = new LinkedHashSet<>();
    String normalized = answer.trim().toUpperCase();
    for (char c : normalized.toCharArray()) {
      if (c >= 'A' && c <= 'H') {
        indices.add(c - 'A');
      } else if (c >= '1' && c <= '8') {
        indices.add(c - '1');
      }
    }
    if (!indices.isEmpty()) {
      return new ArrayList<>(indices);
    }
    for (String token : normalized.split("[,，、\\s]+")) {
      if (token.isBlank()) {
        continue;
      }
      if (token.length() == 1) {
        char c = token.charAt(0);
        if (c >= 'A' && c <= 'H') {
          indices.add(c - 'A');
        } else if (c >= '1' && c <= '8') {
          indices.add(c - '1');
        }
      }
    }
    return new ArrayList<>(indices);
  }

  private String buildPropositionFilename(ExamCourseEntity course, String suffix) {
    String courseTitle = course != null && StringUtils.hasText(course.getTitle())
        ? course.getTitle() : "题库";
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return courseTitle + "_命题导出" + suffix + "_" + ts + ".xlsx";
  }

  private void setCellString(Row row, int col, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    cell.setCellValue(value);
  }

  private void setCellNumeric(Row row, int col, double value) {
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    cell.setCellValue(value);
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

  private Integer parseDifficulty(String s) {
    if (s == null || s.isBlank()) return 2;
    return switch (s.trim()) {
      case "简单", "易" -> 1;
      case "中等", "中" -> 2;
      case "困难", "难" -> 3;
      default -> {
        try { yield Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { yield 2; }
      }
    };
  }

  private String buildContent(String stem, String[] opts) {
    StringBuilder sb = new StringBuilder(nvl(stem));
    for (int k = 0; k < opts.length; k++) {
      if (StringUtils.hasText(opts[k])) {
        sb.append("\n").append((char) ('A' + k)).append(". ").append(opts[k]);
      }
    }
    return sb.toString();
  }

  /**
   * 读表头行，建立 归一化列名 -> 列索引 的映射。
   *
   * <p>归一化规则：trim + 去掉"选项"前缀 + 转小写。
   * 这样 "A"/"选项A"/"选项 A" 都归一为 "a"，兼容不同模板的列命名与顺序。
   * 同名列取第一个出现的（防止重复表头）。
   */
  private Map<String, Integer> buildColumnMap(Row headerRow) {
    Map<String, Integer> map = new LinkedHashMap<>();
    if (headerRow == null) return map;
    int last = headerRow.getLastCellNum();
    for (int c = 0; c <= last; c++) {
      String name = normalizeHeader(getCellString(headerRow.getCell(c)));
      if (name != null && !map.containsKey(name)) {
        map.put(name, c);
      }
    }
    return map;
  }

  private String normalizeHeader(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.startsWith("选项")) t = t.substring(2).trim();
    return t.toLowerCase();
  }

  private Integer firstNonNull(Integer... vals) {
    for (Integer v : vals) {
      if (v != null) return v;
    }
    return null;
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

  /** 命题模板题型列：数字码 1-10 识别为命题格式，返回码值；其余（中文标签）返回 null 走旧解析 */
  private Integer parsePropositionTypeCode(String type) {
    if (type == null) return null;
    String trimmed = type.trim();
    if (!trimmed.matches("[0-9]+")) return null;
    int code = Integer.parseInt(trimmed);
    return PROPOSITION_TYPE_BY_CODE.containsKey(code) ? code : null;
  }

  private boolean isChoicePropositionCode(int code) {
    return code == 1 || code == 2 || code == 7;
  }

  /** 命题模板选项行：解析 G..N 的"内容、分值"单元格，空单元格保留槽位以对齐标记列 */
  private PropositionOption[] readPropositionOptions(Row row) {
    PropositionOption[] options = new PropositionOption[PROPOSITION_MAX_CHOICE_OPTIONS];
    if (row == null) return options;
    for (int k = 0; k < PROPOSITION_MAX_CHOICE_OPTIONS; k++) {
      String text = getCellString(row.getCell(PROPOSITION_ANSWER_START_COL + k));
      if (!StringUtils.hasText(text)) continue;
      Matcher m = PROPOSITION_OPTION_SCORE_PATTERN.matcher(text);
      if (m.matches()) {
        options[k] = new PropositionOption(m.group(1).trim(), new BigDecimal(m.group(2)));
      } else {
        options[k] = new PropositionOption(text, null);
      }
    }
    return options;
  }

  /** 命题模板标记行：正确选项列写"是" */
  private boolean[] readPropositionMarkers(Row markerRow) {
    boolean[] marks = new boolean[PROPOSITION_MAX_CHOICE_OPTIONS];
    if (markerRow == null) return marks;
    for (int k = 0; k < PROPOSITION_MAX_CHOICE_OPTIONS; k++) {
      String v = getCellString(markerRow.getCell(PROPOSITION_ANSWER_START_COL + k));
      marks[k] = "是".equals(v) || "√".equals(v) || "Y".equalsIgnoreCase(v);
    }
    return marks;
  }

  /**
   * 组装平台标准答案 JSON：[{"content","isCorrect","score"}]（判分/练习消费端口径）。
   * 选择题必须有正确项标记，否则返回 null（留空待审核补录，避免产生全错判分数据）。
   */
  private String buildPropositionAnswerJson(PropositionOption[] options, boolean[] marks) {
    List<Map<String, Object>> answerOptions = new ArrayList<>();
    boolean anyMark = false;
    for (int k = 0; k < options.length; k++) {
      PropositionOption opt = options[k];
      if (opt == null) continue;
      anyMark |= marks[k];
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("content", opt.content());
      item.put("isCorrect", marks[k]);
      if (opt.score() != null) {
        item.put("score", opt.score());
      }
      answerOptions.add(item);
    }
    if (answerOptions.isEmpty() || !anyMark) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(answerOptions);
    } catch (Exception e) {
      log.warn("Failed to serialize proposition answer options: {}", e.getMessage());
      return null;
    }
  }

  /** 按"知识点编码"列（series_no）在目标科目下精确挂载；查不到返回 null 回退随机 L3 */
  private Integer resolveKnowledgeBySeriesNo(String seriesNo, Integer courseId) {
    if (!StringUtils.hasText(seriesNo) || courseId == null) return null;
    List<ExamKnowledgeStructureEntity> nodes = knowledgeMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getCourseId, courseId)
            .eq(ExamKnowledgeStructureEntity::getSeriesNo, seriesNo.trim())
            .last("LIMIT 1"));
    return nodes.isEmpty() ? null : nodes.get(0).getId();
  }

  private BigDecimal getCellDecimal(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.NUMERIC) {
      return BigDecimal.valueOf(cell.getNumericCellValue());
    }
    String s = getCellString(cell);
    if (!StringUtils.hasText(s)) return null;
    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 命题模板选项槽位 */
  private record PropositionOption(String content, BigDecimal score) {
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

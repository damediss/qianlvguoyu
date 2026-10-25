package com.guoyu.platform.textbook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.textbook.dto.TextbookSectionSaveReq;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionAssetEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentVersionEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionReviewRecordEntity;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionAssetMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentVersionMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionReviewRecordMapper;
import com.guoyu.platform.textbook.service.TextbookAccessService.SectionAccess;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedSection;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 节正文保存（方案 §12.1-TextbookSectionContentApplicationServiceTest）：
 * 空正文契约、首存 version=1、显式版本条件更新、409 冲突、状态禁写、非法 Markdown/资产引用拒绝。
 */
class TextbookSectionContentApplicationServiceTest {

  private static final long TENANT = 10159L;
  private static final long WRITER = 880_101L;
  private static final long REVIEWER = 880_202L;
  private static final long PROJECT = 1L;
  private static final long CHAPTER = 9001L;
  private static final long SECTION = 9101L;

  private TextbookOutlineNodeMapper nodeMapper;
  private TextbookSectionContentMapper contentMapper;
  private TextbookSectionContentVersionMapper versionMapper;
  private TextbookSectionReviewRecordMapper reviewRecordMapper;
  private TextbookSectionAssetMapper assetMapper;
  private TextbookProjectLockService lockService;
  private TextbookAccessService accessService;
  private TextbookSectionContentApplicationService service;

  private final Map<Long, TextbookSectionContentEntity> contentsDb = new HashMap<>();
  private final Map<Long, TextbookSectionContentVersionEntity> versionsDb = new HashMap<>();
  private TextbookProjectEntity project;
  private TextbookChapterAssignmentEntity assignment;

  @BeforeAll
  static void initMybatisPlusCache() {
    org.apache.ibatis.builder.MapperBuilderAssistant assistant =
        new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TextbookOutlineNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookSectionContentEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookSectionContentVersionEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookSectionAssetEntity.class);
  }

  @BeforeEach
  void setUp() {
    nodeMapper = mock(TextbookOutlineNodeMapper.class);
    contentMapper = mock(TextbookSectionContentMapper.class);
    versionMapper = mock(TextbookSectionContentVersionMapper.class);
    reviewRecordMapper = mock(TextbookSectionReviewRecordMapper.class);
    assetMapper = mock(TextbookSectionAssetMapper.class);
    lockService = mock(TextbookProjectLockService.class);
    accessService = mock(TextbookAccessService.class);
    service = new TextbookSectionContentApplicationService(nodeMapper, contentMapper,
        versionMapper, reviewRecordMapper, assetMapper, lockService, accessService,
        mock(TextbookProgressService.class), new ObjectMapper());

    project = new TextbookProjectEntity();
    project.setId(PROJECT);
    project.setTenantId(TENANT);
    project.setStatus(TextbookProjectApplicationService.STATUS_WRITING);
    assignment = new TextbookChapterAssignmentEntity();
    assignment.setId(7001L);
    assignment.setChapterId(CHAPTER);
    assignment.setWriterId(WRITER);
    assignment.setReviewerId(REVIEWER);
    assignment.setActive(1);
    assignment.setAssignmentVersion(1);

    when(nodeMapper.selectById(anyLong())).thenAnswer(inv -> section());
    when(nodeMapper.selectList(any())).thenReturn(List.of());
    when(assetMapper.selectList(any())).thenReturn(List.of());
    when(reviewRecordMapper.selectOne(any())).thenReturn(null);
    when(accessService.displayNames(anyCollection())).thenReturn(Map.of());
    when(contentMapper.selectOne(any())).thenAnswer(
        inv -> contentsDb.isEmpty() ? null : contentsDb.values().iterator().next());
    when(contentMapper.selectById(anyLong()))
        .thenAnswer(inv -> contentsDb.get(inv.getArgument(0, Long.class)));
    when(contentMapper.insert(any(TextbookSectionContentEntity.class))).thenAnswer(inv -> {
      TextbookSectionContentEntity entity = inv.getArgument(0);
      entity.setId(IdWorker.getId());
      contentsDb.put(entity.getId(), entity);
      return 1;
    });
    // update(null, wrapper)：按 set 子句落到假库（section 唯一，直接改写行）
    when(contentMapper.update(any(), any())).thenAnswer(inv -> {
      LambdaUpdateWrapper<?> wrapper = inv.getArgument(1);
      Map<String, Object> sets = setParams(wrapper);
      Long expectVersion = (Long) null;
      Object baseVersion = sets.get("__baseVersion");
      if (contentsDb.isEmpty()) {
        return 0;
      }
      TextbookSectionContentEntity row = contentsDb.values().iterator().next();
      // wrapper eq(version, base) 的值在 paramNameValuePairs 里，用提取的 base 校验
      Map<String, Object> eqs = eqParams(wrapper);
      Object base = eqs.get("version");
      if (base != null && !Objects.equals(row.getVersion(), ((Number) base).intValue())) {
        return 0;
      }
      applySets(row, sets);
      return 1;
    });
    when(versionMapper.insert(any(TextbookSectionContentVersionEntity.class))).thenAnswer(inv -> {
      TextbookSectionContentVersionEntity entity = inv.getArgument(0);
      entity.setId(IdWorker.getId());
      versionsDb.put(entity.getId(), entity);
      return 1;
    });
    when(versionMapper.selectList(any())).thenReturn(List.copyOf(versionsDb.values()));
    when(lockService.lockBySection(anyLong())).thenAnswer(inv -> new LockedSection(
        project, chapter(), section(), assignment,
        contentsDb.isEmpty() ? null : contentsDb.values().iterator().next()));

    PlatformUserPrincipal principal = writerPrincipal();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static PlatformUserPrincipal writerPrincipal() {
    return new PlatformUserPrincipal(WRITER, TENANT, "writer1", "主笔一", "admin",
        List.of("TEXTBOOK_WRITER"),
        List.of("textbook:task:view", "textbook:content:write", "textbook:project:view"), null);
  }

  private static TextbookOutlineNodeEntity chapter() {
    TextbookOutlineNodeEntity chapter = new TextbookOutlineNodeEntity();
    chapter.setId(CHAPTER);
    chapter.setTenantId(TENANT);
    chapter.setProjectId(PROJECT);
    chapter.setParentId(0L);
    chapter.setNodeLevel(2);
    chapter.setTitle("铝电解槽操作");
    return chapter;
  }

  private static TextbookOutlineNodeEntity section() {
    TextbookOutlineNodeEntity section = new TextbookOutlineNodeEntity();
    section.setId(SECTION);
    section.setTenantId(TENANT);
    section.setProjectId(PROJECT);
    section.setParentId(CHAPTER);
    section.setNodeLevel(3);
    section.setTitle("更换阳极");
    section.setTargetWords(5000);
    return section;
  }

  private SectionAccess readAccess() {
    return new SectionAccess(project, chapter(), section(), assignment, writerPrincipal(),
        true, false);
  }

  @Test
  @DisplayName("W2 无正文记录：200 契约 contentExists=false/version=0/contentStatus=null")
  void emptyContentContract() {
    when(accessService.requireSectionRead(anyLong())).thenReturn(readAccess());
    TextbookSectionContentVO vo = service.getContent(SECTION);
    assertTrue(!vo.contentExists());
    assertEquals(0, vo.version());
    assertEquals(null, vo.contentStatus());
    assertEquals("", vo.contentMd());
    assertEquals(0, vo.wordCount());
  }

  @Test
  @DisplayName("首存 baseVersion=0 → version=1/draft；再次保存 version=2")
  void firstSaveThenSecondSave() {
    TextbookSectionContentVO first =
        service.save(SECTION, new TextbookSectionSaveReq("第一段正文", 0));
    assertTrue(first.contentExists());
    assertEquals(1, first.version());
    assertEquals(TextbookSectionContentApplicationService.STATUS_DRAFT, first.contentStatus());

    TextbookSectionContentVO second =
        service.save(SECTION, new TextbookSectionSaveReq("第一段正文，再补充", 1));
    assertEquals(2, second.version());
    assertTrue(second.wordCount() > first.wordCount(),
        "后续保存的 word_count 必须随正文更新落库");
    assertTrue(second.checkReport() != null && second.checkReport().wordCount() == second.wordCount(),
        "检查报告必须随正文更新落库并回读");
    assertEquals(WRITER, contentsDb.values().iterator().next().getUpdatedBy(),
        "wrapper 更新也应写入 updated_by（BaseEntity 审计字段）");
  }

  @Test
  @DisplayName("旧 baseVersion 保存 → 409 VERSION_CONFLICT，服务端内容不被覆盖")
  void staleBaseVersionRejected() {
    service.save(SECTION, new TextbookSectionSaveReq("服务端版本", 0));
    service.save(SECTION, new TextbookSectionSaveReq("服务端版本2", 1)); // version=2

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.save(SECTION, new TextbookSectionSaveReq("旧页面内容", 1)));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) ex.getData();
    assertEquals("VERSION_CONFLICT", data.get("reason"));
    assertEquals(2, ((Number) data.get("currentVersion")).intValue());
    assertEquals("服务端版本2", data.get("currentContentMd"));
    assertEquals("服务端版本2", contentsDb.values().iterator().next().getContentMd(),
        "服务端正文仍为先保存内容");
  }

  @Test
  @DisplayName("submitted/approved 状态主笔保存 → 409（须先打回/撤销）")
  void submittedOrApprovedSaveRejected() {
    service.save(SECTION, new TextbookSectionSaveReq("草稿", 0));
    TextbookSectionContentEntity row = contentsDb.values().iterator().next();
    row.setContentStatus(TextbookSectionContentApplicationService.STATUS_SUBMITTED);
    row.setVersion(5);
    BusinessException submitted = assertThrows(BusinessException.class,
        () -> service.save(SECTION, new TextbookSectionSaveReq("越权修改", 5)));
    assertEquals(ErrorCode.CONFLICT, submitted.getCode());

    row.setContentStatus(TextbookSectionContentApplicationService.STATUS_APPROVED);
    BusinessException approved = assertThrows(BusinessException.class,
        () -> service.save(SECTION, new TextbookSectionSaveReq("越权修改", 5)));
    assertEquals(ErrorCode.CONFLICT, approved.getCode());
    assertTrue(approved.getMessage().contains("已通过"));
  }

  @Test
  @DisplayName("非当前主笔（含改派后原主笔）禁写：403/409 由 AccessService 统一裁决")
  void nonWriterRejectedByAccessService() {
    doThrow(new BusinessException(ErrorCode.FORBIDDEN, "仅当前主笔可执行该操作"))
        .when(accessService).requireCurrentWriter(any(), any(), any());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.save(SECTION, new TextbookSectionSaveReq("内容", 0)));
    assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
  }

  @Test
  @DisplayName("非法 Markdown（script/危险协议/外链图片）拒绝，不保存")
  void illegalMarkdownRejected() {
    BusinessException script = assertThrows(BusinessException.class,
        () -> service.save(SECTION, new TextbookSectionSaveReq("<script>x</script>", 0)));
    assertEquals(ErrorCode.BAD_REQUEST, script.getCode());

    BusinessException jsLink = assertThrows(BusinessException.class,
        () -> service.save(SECTION,
            new TextbookSectionSaveReq("[a](javascript:alert(1))", 0)));
    assertEquals(ErrorCode.BAD_REQUEST, jsLink.getCode());

    BusinessException extImage = assertThrows(BusinessException.class,
        () -> service.save(SECTION,
            new TextbookSectionSaveReq("![x](https://evil.com/a.png)", 0)));
    assertEquals(ErrorCode.BAD_REQUEST, extImage.getCode());
    assertTrue(contentsDb.isEmpty(), "校验失败不得落库");
  }

  @Test
  @DisplayName("引用其他节的资产 → 拒绝（资产归属校验）")
  void foreignAssetReferenceRejected() {
    TextbookSectionAssetEntity own = new TextbookSectionAssetEntity();
    own.setId(9901L);
    own.setSectionId(SECTION);
    when(assetMapper.selectList(any())).thenReturn(List.of(own));

    // 本节资产引用 OK
    service.save(SECTION,
        new TextbookSectionSaveReq("![own](textbook-asset://9901)", 0));

    // 其他节资产（9902 不在本节资产集）拒绝
    contentsDb.clear();
    BusinessException foreign = assertThrows(BusinessException.class,
        () -> service.save(SECTION,
            new TextbookSectionSaveReq("![foreign](textbook-asset://9902)", 0)));
    assertEquals(ErrorCode.BAD_REQUEST, foreign.getCode());
    assertTrue(foreign.getMessage().contains("不属于本节"));
  }

  @Test
  @DisplayName("W4 提交：生成 submit 快照与 submit 记录，version+1")
  void submitCreatesSnapshotAndRecord() {
    service.save(SECTION, new TextbookSectionSaveReq("可提交正文", 0));
    TextbookSectionContentEntity row = contentsDb.values().iterator().next();
    int versionBefore = row.getVersion();

    ArgumentCaptor<TextbookSectionContentVersionEntity> snapshotCaptor =
        ArgumentCaptor.forClass(TextbookSectionContentVersionEntity.class);
    ArgumentCaptor<TextbookSectionReviewRecordEntity> recordCaptor =
        ArgumentCaptor.forClass(TextbookSectionReviewRecordEntity.class);

    TextbookSectionContentVO vo = service.submit(SECTION, versionBefore);

    assertEquals(TextbookSectionContentApplicationService.STATUS_SUBMITTED, vo.contentStatus());
    assertEquals(versionBefore + 1, vo.version());
    verify(versionMapper).insert(snapshotCaptor.capture());
    TextbookSectionContentVersionEntity snapshot = snapshotCaptor.getValue();
    assertEquals(TextbookSectionContentApplicationService.SNAPSHOT_SUBMIT,
        snapshot.getSnapshotType());
    assertEquals(1, snapshot.getSubmitRevision());
    assertEquals(1, snapshot.getSnapshotNo());
    verify(reviewRecordMapper).insert(recordCaptor.capture());
    assertEquals("submit", recordCaptor.getValue().getAction());
    assertEquals(WRITER, row.getSubmittedBy());
    // 空白正文（仅图片、无 alt 文本）不能提交
    contentsDb.values().iterator().next()
        .setContentStatus(TextbookSectionContentApplicationService.STATUS_RETURNED);
    contentsDb.values().iterator().next().setContentMd("![](textbook-asset://1)");
    BusinessException empty = assertThrows(BusinessException.class,
        () -> service.submit(SECTION, contentsDb.values().iterator().next().getVersion()));
    assertTrue(empty.getMessage().contains("仅图片不算正文"));
  }

  // ============================================================
  // 假库工具
  // ============================================================

  private static Map<String, Object> eqParams(Wrapper<?> wrapper) {
    AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
    String segment = aw.getSqlSegment();
    Map<String, Object> pairs = aw.getParamNameValuePairs();
    Map<String, Object> out = new LinkedHashMap<>();
    Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}")
        .matcher(segment);
    while (m.find()) {
      out.put(m.group(1), pairs.get(m.group(2)));
    }
    return out;
  }

  private static Map<String, Object> setParams(LambdaUpdateWrapper<?> wrapper) {
    String sqlSet = wrapper.getSqlSet();
    Map<String, Object> pairs = wrapper.getParamNameValuePairs();
    Map<String, Object> out = new LinkedHashMap<>();
    if (sqlSet == null) {
      return out;
    }
    Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}")
        .matcher(sqlSet);
    while (m.find()) {
      out.put(m.group(1), pairs.get(m.group(2)));
    }
    return out;
  }

  private static void applySets(TextbookSectionContentEntity row, Map<String, Object> sets) {
    Object md = sets.get("content_md");
    if (md instanceof String s) {
      row.setContentMd(s);
    }
    Object status = sets.get("content_status");
    if (status instanceof String s) {
      row.setContentStatus(s);
    }
    Object version = sets.get("version");
    if (version instanceof Number n) {
      row.setVersion(n.intValue());
    }
    Object wordCount = sets.get("word_count");
    if (wordCount instanceof Number n) {
      row.setWordCount(n.intValue());
    }
    Object report = sets.get("check_report_json");
    if (report instanceof String s) {
      row.setCheckReportJson(s);
    }
    Object updatedBy = sets.get("updated_by");
    if (updatedBy instanceof Number n) {
      row.setUpdatedBy(n.longValue());
    }
    Object submittedBy = sets.get("submitted_by");
    if (submittedBy instanceof Number n) {
      row.setSubmittedBy(n.longValue());
    } else if (sets.containsKey("submitted_by") && submittedBy == null) {
      row.setSubmittedBy(null);
    }
    Object reviewedBy = sets.get("reviewed_by");
    if (reviewedBy instanceof Number n) {
      row.setReviewedBy(n.longValue());
    } else if (sets.containsKey("reviewed_by") && reviewedBy == null) {
      row.setReviewedBy(null);
    }
  }
}

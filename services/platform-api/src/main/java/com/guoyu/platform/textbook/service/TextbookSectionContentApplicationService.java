package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.domain.TextbookContentChecker;
import com.guoyu.platform.textbook.domain.TextbookContentChecker.StandardPoint;
import com.guoyu.platform.textbook.domain.TextbookMarkdownSupport;
import com.guoyu.platform.textbook.dto.TextbookSectionRestoreReq;
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
import com.guoyu.platform.textbook.vo.TextbookCheckReportVO;
import com.guoyu.platform.textbook.vo.TextbookReviewRecordVO;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO.AssetRef;
import com.guoyu.platform.textbook.vo.TextbookVersionDetailVO;
import com.guoyu.platform.textbook.vo.TextbookVersionVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 节正文应用服务（方案 §6.3 W2/W3/W4/W9、W6/W7/W8）。
 *
 * <p>写路径统一按 §7.1 锁序（project → assignment → content FOR UPDATE），锁后重新校验
 * 主笔/状态/baseVersion；正文写使用显式版本条件更新并检查影响行数（不依赖 MP 乐观锁插件）。
 * 自动保存不生成快照；submit 生成 submit 快照；恢复前先存 before_restore。
 */
@Service
public class TextbookSectionContentApplicationService {

  private static final Logger log =
      LoggerFactory.getLogger(TextbookSectionContentApplicationService.class);

  public static final String STATUS_DRAFT = "draft";
  public static final String STATUS_SUBMITTED = "submitted";
  public static final String STATUS_APPROVED = "approved";
  public static final String STATUS_RETURNED = "returned";
  public static final String SNAPSHOT_SUBMIT = "submit";
  public static final String SNAPSHOT_BEFORE_RESTORE = "before_restore";
  public static final String ACTION_SUBMIT = "submit";

  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookSectionContentVersionMapper versionMapper;
  private final TextbookSectionReviewRecordMapper reviewRecordMapper;
  private final TextbookSectionAssetMapper assetMapper;
  private final TextbookProjectLockService lockService;
  private final TextbookAccessService accessService;
  private final TextbookProgressService progressService;
  private final ObjectMapper objectMapper;

  public TextbookSectionContentApplicationService(
      TextbookOutlineNodeMapper nodeMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookSectionContentVersionMapper versionMapper,
      TextbookSectionReviewRecordMapper reviewRecordMapper,
      TextbookSectionAssetMapper assetMapper,
      TextbookProjectLockService lockService,
      TextbookAccessService accessService,
      TextbookProgressService progressService,
      ObjectMapper objectMapper) {
    this.nodeMapper = nodeMapper;
    this.contentMapper = contentMapper;
    this.versionMapper = versionMapper;
    this.reviewRecordMapper = reviewRecordMapper;
    this.assetMapper = assetMapper;
    this.lockService = lockService;
    this.accessService = accessService;
    this.progressService = progressService;
    this.objectMapper = objectMapper;
  }

  // ============================================================
  // W2 读取（空正文 200 contentExists=false，不用 404）
  // ============================================================

  @Transactional(readOnly = true)
  public TextbookSectionContentVO getContent(Long sectionId) {
    SectionAccess access = accessService.requireSectionRead(sectionId);
    return buildContentVO(access, contentOf(sectionId));
  }

  // ============================================================
  // W3 保存草稿（自动/手动）
  // ============================================================

  @Transactional
  public TextbookSectionContentVO save(Long sectionId, TextbookSectionSaveReq req) {
    LockedSection locked = lockService.lockBySection(sectionId);
    accessService.requireCurrentWriter(locked.project(), locked.assignment(),
        SecurityContext.currentUser());

    String contentMd = req.contentMd() == null ? "" : req.contentMd();
    if (contentMd.length() > TextbookMarkdownSupport.MAX_CONTENT_LENGTH) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "正文超过上限 " + TextbookMarkdownSupport.MAX_CONTENT_LENGTH + " 字符");
    }
    validateMarkdown(contentMd, sectionId);

    TextbookSectionContentEntity content = locked.content();
    if (content == null) {
      if (req.baseVersion() != 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "该节尚无正文记录，baseVersion 必须为 0");
      }
      content = insertFirstContent(locked, contentMd);
    } else {
      requireEditable(content, locked.project(), locked.assignment());
      requireVersionMatch(content, req.baseVersion());
      updateContent(content, contentMd);
    }
    progressService.refreshChapterSummary(locked.chapter().getId());
    log.info("[教材] 保存正文：sectionId={} version={} status={}",
        sectionId, content.getVersion(), content.getContentStatus());
    return buildContentVO(loadAccess(sectionId, locked), contentMapper.selectById(content.getId()));
  }

  // ============================================================
  // W4 提交审定（生成 submit 快照与 submit 记录）
  // ============================================================

  @Transactional
  public TextbookSectionContentVO submit(Long sectionId, Integer baseVersion) {
    LockedSection locked = lockService.lockBySection(sectionId);
    accessService.requireCurrentWriter(locked.project(), locked.assignment(),
        SecurityContext.currentUser());
    TextbookSectionContentEntity content = locked.content();
    if (content == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "正文为空，不能提交审定");
    }
    requireEditable(content, locked.project(), locked.assignment());
    requireVersionMatch(content, baseVersion);
    // 正文经 Markdown 转纯文本后必须非空白；只有图片不算可提交正文（方案 §7.3-2）
    String plain = TextbookMarkdownSupport.toPlainText(content.getContentMd());
    if (plain.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正文为空（仅图片不算正文），不能提交审定");
    }

    int submitRevision = nextSubmitRevision(content.getId());
    insertSnapshot(content, SNAPSHOT_SUBMIT, submitRevision, content.getContentMd());
    int newVersion = content.getVersion() + 1;
    int updated = contentMapper.update(null, new LambdaUpdateWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getId, content.getId())
        .eq(TextbookSectionContentEntity::getVersion, baseVersion)
        .set(TextbookSectionContentEntity::getContentStatus, STATUS_SUBMITTED)
        .set(TextbookSectionContentEntity::getVersion, newVersion)
        .set(TextbookSectionContentEntity::getSubmittedBy,
            SecurityContext.currentUser().getUserId())
        .set(TextbookSectionContentEntity::getSubmittedAt, LocalDateTime.now())
        .set(TextbookSectionContentEntity::getReviewedBy, null)
        .set(TextbookSectionContentEntity::getReviewedAt, null)
        .set(TextbookSectionContentEntity::getUpdatedBy,
            SecurityContext.currentUser().getUserId()));
    if (updated == 0) {
      throw versionConflict(content);
    }
    insertReviewRecord(content.getId(), sectionId, submitRevision, ACTION_SUBMIT,
        content.getContentStatus(), STATUS_SUBMITTED, null);
    progressService.refreshChapterSummary(locked.chapter().getId());
    log.info("[教材] 提交审定：sectionId={} submitRevision={} version={}",
        sectionId, submitRevision, newVersion);
    return buildContentVO(loadAccess(sectionId, locked), contentMapper.selectById(content.getId()));
  }

  // ============================================================
  // W9 恢复历史版本（先存 before_restore，保持 draft/returned 原状态）
  // ============================================================

  @Transactional
  public TextbookSectionContentVO restore(Long sectionId, TextbookSectionRestoreReq req) {
    LockedSection locked = lockService.lockBySection(sectionId);
    accessService.requireCurrentWriter(locked.project(), locked.assignment(),
        SecurityContext.currentUser());
    TextbookSectionContentEntity content = locked.content();
    if (content == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "尚无正文记录，无可恢复内容");
    }
    requireEditable(content, locked.project(), locked.assignment());
    requireVersionMatch(content, req.baseVersion());

    long snapshotId = parseId(req.snapshotId(), "快照 ID 非法");
    TextbookSectionContentVersionEntity snapshot = versionMapper.selectById(snapshotId);
    if (snapshot == null || !sectionId.equals(snapshot.getSectionId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "快照不存在或不属于本节，不能恢复");
    }
    // 先把当前内容保存为 before_restore（防恢复操作覆盖唯一未提交副本）
    insertSnapshot(content, SNAPSHOT_BEFORE_RESTORE, null, content.getContentMd());
    int newVersion = content.getVersion() + 1;
    int updated = contentMapper.update(null, new LambdaUpdateWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getId, content.getId())
        .eq(TextbookSectionContentEntity::getVersion, req.baseVersion())
        .set(TextbookSectionContentEntity::getContentMd, snapshot.getContentMd())
        .set(TextbookSectionContentEntity::getVersion, newVersion)
        // 清空当前轮提交/审定信息（历史仍在 review_record），状态保持 draft/returned
        .set(TextbookSectionContentEntity::getSubmittedBy, null)
        .set(TextbookSectionContentEntity::getSubmittedAt, null)
        .set(TextbookSectionContentEntity::getReviewedBy, null)
        .set(TextbookSectionContentEntity::getReviewedAt, null)
        .set(TextbookSectionContentEntity::getUpdatedBy,
            SecurityContext.currentUser().getUserId()));
    if (updated == 0) {
      throw versionConflict(content);
    }
    recomputeCheckReport(sectionId, content.getId(), snapshot.getContentMd(), newVersion,
        locked);
    progressService.refreshChapterSummary(locked.chapter().getId());
    log.info("[教材] 恢复快照：sectionId={} snapshotId={} before_restore 已生成", sectionId, snapshotId);
    return buildContentVO(loadAccess(sectionId, locked), contentMapper.selectById(content.getId()));
  }

  // ============================================================
  // W6 审定时间线 / W7 快照分页 / W8 快照详情
  // ============================================================

  @Transactional(readOnly = true)
  public PageResponse<TextbookReviewRecordVO> reviews(Long sectionId, long page, long pageSize) {
    accessService.requireSectionRead(sectionId);
    Page<TextbookSectionReviewRecordEntity> result = reviewRecordMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<TextbookSectionReviewRecordEntity>()
            .eq(TextbookSectionReviewRecordEntity::getSectionId, sectionId)
            .orderByDesc(TextbookSectionReviewRecordEntity::getId));
    List<Long> actorIds = result.getRecords().stream()
        .map(TextbookSectionReviewRecordEntity::getCreatedBy).toList();
    Map<Long, String> names = accessService.displayNames(actorIds);
    List<TextbookReviewRecordVO> items = new ArrayList<>();
    for (TextbookSectionReviewRecordEntity record : result.getRecords()) {
      items.add(toReviewRecordVO(record, names));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public PageResponse<TextbookVersionVO> versions(Long sectionId, long page, long pageSize) {
    accessService.requireSectionRead(sectionId);
    Page<TextbookSectionContentVersionEntity> result = versionMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<TextbookSectionContentVersionEntity>()
            .eq(TextbookSectionContentVersionEntity::getSectionId, sectionId)
            .orderByDesc(TextbookSectionContentVersionEntity::getSnapshotNo));
    List<Long> actorIds = result.getRecords().stream()
        .map(TextbookSectionContentVersionEntity::getCreatedBy).toList();
    Map<Long, String> names = accessService.displayNames(actorIds);
    List<TextbookVersionVO> items = new ArrayList<>();
    for (TextbookSectionContentVersionEntity version : result.getRecords()) {
      items.add(new TextbookVersionVO(
          String.valueOf(version.getId()), version.getSnapshotNo(), version.getSnapshotType(),
          version.getSubmitRevision(), version.getWordCount(),
          names.getOrDefault(version.getCreatedBy(), ""), version.getCreatedAt()));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public TextbookVersionDetailVO versionDetail(Long versionId) {
    TextbookSectionContentVersionEntity version = versionMapper.selectById(versionId);
    if (version == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "快照不存在");
    }
    accessService.requireSectionRead(version.getSectionId());
    Map<Long, String> names = accessService.displayNames(List.of(version.getCreatedBy()));
    return new TextbookVersionDetailVO(String.valueOf(version.getId()),
        String.valueOf(version.getSectionId()), version.getSnapshotNo(),
        version.getSnapshotType(), version.getSubmitRevision(), version.getWordCount(),
        version.getContentMd(), names.getOrDefault(version.getCreatedBy(), ""),
        version.getCreatedAt());
  }

  // ============================================================
  // VO 组装（W2/W3/W4/W5/W9 共用）
  // ============================================================

  TextbookSectionContentVO buildContentVO(SectionAccess access,
      TextbookSectionContentEntity content) {
    TextbookOutlineNodeEntity section = access.section();
    TextbookChapterAssignmentEntity assignment = access.assignment();
    TextbookProjectEntity project = access.project();
    boolean writer = access.writerRelation();
    boolean reviewer = access.reviewerRelation();

    // 检查报告（无正文时为 null）
    TextbookCheckReportVO checkReport = null;
    if (content != null && content.getCheckReportJson() != null) {
      try {
        checkReport = objectMapper.readValue(content.getCheckReportJson(),
            TextbookCheckReportVO.class);
      } catch (Exception e) {
        log.warn("[教材] check_report_json 解析失败: {}", e.getMessage());
      }
    }

    // 资产引用（不暴露 objectKey/URL）
    List<AssetRef> assets = new ArrayList<>();
    for (TextbookSectionAssetEntity asset : assetMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionAssetEntity>()
            .eq(TextbookSectionAssetEntity::getSectionId, section.getId())
            .orderByAsc(TextbookSectionAssetEntity::getSortNo)
            .orderByAsc(TextbookSectionAssetEntity::getId))) {
      assets.add(new AssetRef(String.valueOf(asset.getId()),
          TextbookMarkdownSupport.ASSET_URI_PREFIX + asset.getId()));
    }

    // 最近一条时间线记录
    TextbookSectionReviewRecordEntity latestRecord = reviewRecordMapper.selectOne(
        new LambdaQueryWrapper<TextbookSectionReviewRecordEntity>()
            .eq(TextbookSectionReviewRecordEntity::getSectionId, section.getId())
            .orderByDesc(TextbookSectionReviewRecordEntity::getId)
            .last("LIMIT 1"));
    Map<Long, String> names = new HashMap<>();
    Set<Long> nameIds = new HashSet<>();
    if (content != null && content.getSubmittedBy() != null) {
      nameIds.add(content.getSubmittedBy());
    }
    if (content != null && content.getReviewedBy() != null) {
      nameIds.add(content.getReviewedBy());
    }
    if (latestRecord != null && latestRecord.getCreatedBy() != null) {
      nameIds.add(latestRecord.getCreatedBy());
    }
    if (assignment != null) {
      nameIds.add(assignment.getReviewerId());
    }
    if (!nameIds.isEmpty()) {
      names.putAll(accessService.displayNames(nameIds));
    }

    // 当前内容对应的提交快照：仅在 submitted/approved 下非空（方案 §6.3）
    Integer submitRevision = null;
    String currentSnapshotId = null;
    if (content != null && (STATUS_SUBMITTED.equals(content.getContentStatus())
        || STATUS_APPROVED.equals(content.getContentStatus()))) {
      TextbookSectionContentVersionEntity latestSubmit = latestSubmitSnapshot(content.getId());
      if (latestSubmit != null) {
        submitRevision = latestSubmit.getSubmitRevision();
        currentSnapshotId = String.valueOf(latestSubmit.getId());
      }
    }

    boolean writing = TextbookProjectApplicationService.STATUS_WRITING.equals(project.getStatus());
    boolean active = assignment != null && Integer.valueOf(1).equals(assignment.getActive());
    boolean editableStatus = content == null
        || STATUS_DRAFT.equals(content.getContentStatus())
        || STATUS_RETURNED.equals(content.getContentStatus());
    boolean canWrite = writing && active && writer && editableStatus
        && TextbookAccessService.has(access.principal(), TextbookAccessService.PERM_CONTENT_WRITE);
    boolean canReview = writing && active && reviewer
        && (content != null && (STATUS_SUBMITTED.equals(content.getContentStatus())
            || STATUS_APPROVED.equals(content.getContentStatus())))
        && TextbookAccessService.has(access.principal(), TextbookAccessService.PERM_CONTENT_REVIEW);

    boolean readOnly = !canWrite;
    String readOnlyReason = null;
    if (readOnly) {
      readOnlyReason = readOnlyReason(access, content, assignment, project, writer, writing,
          editableStatus);
    }

    return new TextbookSectionContentVO(
        String.valueOf(section.getId()),
        String.valueOf(section.getParentId()),
        String.valueOf(project.getId()),
        section.getTitle(),
        nodeTitle(section.getParentId()),
        section.getTargetWords(),
        content != null,
        content == null ? "" : content.getContentMd(),
        content == null ? null : content.getContentStatus(),
        content == null ? 0 : content.getWordCount(),
        content == null ? 0 : content.getVersion(),
        access.relations(),
        canWrite, canReview, readOnly, readOnlyReason,
        checkReport, assets,
        content == null || content.getSubmittedBy() == null ? null
            : String.valueOf(content.getSubmittedBy()),
        content == null ? null : names.get(content.getSubmittedBy()),
        content == null ? null : content.getSubmittedAt(),
        submitRevision, currentSnapshotId,
        content == null ? null : names.get(content.getReviewedBy()),
        content == null ? null : content.getReviewedAt(),
        latestRecord == null ? null : toReviewRecordVO(latestRecord, names));
  }

  private String readOnlyReason(SectionAccess access, TextbookSectionContentEntity content,
      TextbookChapterAssignmentEntity assignment, TextbookProjectEntity project,
      boolean writer, boolean writing, boolean editableStatus) {
    String status = content == null ? null : content.getContentStatus();
    if (STATUS_SUBMITTED.equals(status)) {
      String reviewer = assignment == null ? "审定人"
          : accessService.displayNames(List.of(assignment.getReviewerId()))
              .getOrDefault(assignment.getReviewerId(), "审定人");
      return "本节已提交，等待「" + reviewer + "」审定";
    }
    if (STATUS_APPROVED.equals(status)) {
      return "本节已通过，如需修改需审定人撤销通过或教研重新打开工程";
    }
    if (!writing) {
      return "工程当前状态为 " + project.getStatus() + "，不可编辑";
    }
    if (!editableStatus) {
      return "当前状态不可编辑";
    }
    if (!writer) {
      return "仅当前主笔可编辑本节";
    }
    return null;
  }

  private String nodeTitle(Long nodeId) {
    if (nodeId == null) {
      return null;
    }
    TextbookOutlineNodeEntity node = nodeMapper.selectById(nodeId);
    return node == null ? null : node.getTitle();
  }

  TextbookReviewRecordVO toReviewRecordVO(TextbookSectionReviewRecordEntity record,
      Map<Long, String> names) {
    return new TextbookReviewRecordVO(
        String.valueOf(record.getId()), record.getSubmitRevision(), record.getAction(),
        record.getFromStatus(), record.getToStatus(), record.getComment(),
        names.getOrDefault(record.getCreatedBy(), ""), record.getCreatedAt());
  }

  // ============================================================
  // 写路径共享
  // ============================================================

  private TextbookSectionContentEntity insertFirstContent(LockedSection locked, String contentMd) {
    TextbookSectionContentEntity content = new TextbookSectionContentEntity();
    content.setProjectId(locked.project().getId());
    content.setChapterId(locked.chapter().getId());
    content.setSectionId(locked.section().getId());
    content.setContentMd(contentMd);
    content.setContentStatus(STATUS_DRAFT);
    content.setVersion(1);
    applyCheckReport(content, contentMd, locked.section());
    try {
      contentMapper.insert(content);
    } catch (DuplicateKeyException e) {
      // 唯一键 (tenant_id, section_id, deleted) 兜底并发首存
      throw new BusinessException(ErrorCode.CONFLICT, "该节正文已被并发创建，请刷新后重试");
    }
    return content;
  }

  private void updateContent(TextbookSectionContentEntity content, String contentMd) {
    // 先重算确定性检查，再与正文/版本同一条件 UPDATE 落库（word_count/check_report 不能只改内存）
    applyCheckReport(content, contentMd, sectionOf(content.getSectionId()));
    int newVersion = content.getVersion() + 1;
    int updated = contentMapper.update(null, new LambdaUpdateWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getId, content.getId())
        .eq(TextbookSectionContentEntity::getVersion, content.getVersion())
        .set(TextbookSectionContentEntity::getContentMd, contentMd)
        .set(TextbookSectionContentEntity::getVersion, newVersion)
        .set(TextbookSectionContentEntity::getWordCount, content.getWordCount())
        .set(TextbookSectionContentEntity::getCheckReportJson, content.getCheckReportJson())
        .set(TextbookSectionContentEntity::getUpdatedBy,
            SecurityContext.currentUser().getUserId()));
    if (updated == 0) {
      throw versionConflict(content);
    }
  }

  private void recomputeCheckReport(Long sectionId, Long contentId, String contentMd,
      int newVersion, LockedSection locked) {
    TextbookSectionContentEntity fresh = contentMapper.selectById(contentId);
    if (fresh == null) {
      return;
    }
    applyCheckReport(fresh, contentMd, locked.section());
    contentMapper.update(null, new LambdaUpdateWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getId, contentId)
        .set(TextbookSectionContentEntity::getWordCount, fresh.getWordCount())
        .set(TextbookSectionContentEntity::getCheckReportJson, fresh.getCheckReportJson()));
  }

  private void applyCheckReport(TextbookSectionContentEntity content, String contentMd,
      TextbookOutlineNodeEntity section) {
    List<StandardPoint> points = standardPointsOf(section.getId());
    TextbookCheckReportVO report = TextbookContentChecker.check(contentMd,
        section.getTargetWords(), points);
    content.setWordCount(report.wordCount());
    content.setCheckReportJson(writeJson(report));
  }

  private List<StandardPoint> standardPointsOf(Long sectionId) {
    List<StandardPoint> points = new ArrayList<>();
    for (TextbookOutlineNodeEntity point : nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, sectionId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_POINT)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId))) {
      points.add(new StandardPoint(point.getId(),
          point.getTitleRaw() != null ? point.getTitleRaw() : point.getTitle(),
          point.getDimension()));
    }
    return points;
  }

  private void requireEditable(TextbookSectionContentEntity content,
      TextbookProjectEntity project, TextbookChapterAssignmentEntity assignment) {
    String status = content.getContentStatus();
    if (STATUS_SUBMITTED.equals(status) || STATUS_APPROVED.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          STATUS_SUBMITTED.equals(status) ? "本节已提交审定，等待审定人处理，主笔暂不能修改"
              : "本节已通过，先由审定人撤销通过或教研重新打开工程");
    }
    if (!STATUS_DRAFT.equals(status) && !STATUS_RETURNED.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前正文状态不允许编辑：" + status);
    }
  }

  private void requireVersionMatch(TextbookSectionContentEntity content, Integer baseVersion) {
    if (baseVersion == null || !baseVersion.equals(content.getVersion())) {
      throw versionConflict(content);
    }
  }

  /** 409 data 结构（方案 §6 通用约定）：reason + 服务端当前版本/内容/更新人。 */
  BusinessException versionConflict(TextbookSectionContentEntity content) {
    Long updatedBy = content.getUpdatedBy() == null ? content.getCreatedBy()
        : content.getUpdatedBy();
    Map<Long, String> names = updatedBy == null ? Map.of()
        : accessService.displayNames(List.of(updatedBy));
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("reason", "VERSION_CONFLICT");
    data.put("currentVersion", content.getVersion());
    data.put("currentContentMd", content.getContentMd());
    data.put("currentUpdatedAt", content.getUpdatedAt());
    data.put("currentUpdatedByName", updatedBy == null ? "" : names.getOrDefault(updatedBy, ""));
    return new BusinessException(ErrorCode.CONFLICT, "内容版本冲突，请加载最新内容后再操作", data);
  }

  /** W5 审定状态落库：显式版本条件更新 + 影响行数检查；返回影响行数。 */
  int updateForReview(Long contentId, int baseVersion, String toStatus, int newVersion,
      Long reviewerId) {
    return contentMapper.update(null, new LambdaUpdateWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getId, contentId)
        .eq(TextbookSectionContentEntity::getVersion, baseVersion)
        .set(TextbookSectionContentEntity::getContentStatus, toStatus)
        .set(TextbookSectionContentEntity::getVersion, newVersion)
        .set(TextbookSectionContentEntity::getReviewedBy, reviewerId)
        .set(TextbookSectionContentEntity::getReviewedAt, LocalDateTime.now())
        .set(TextbookSectionContentEntity::getUpdatedBy, reviewerId));
  }

  /** 写操作完成后基于锁内上下文组装最新 W2（复用授权结果，不再重复走读取授权）。 */
  TextbookSectionContentVO buildContentVOForLocked(Long sectionId, LockedSection locked) {
    return buildContentVO(loadAccess(sectionId, locked),
        locked.content() == null ? contentOf(sectionId)
            : contentMapper.selectById(locked.content().getId()));
  }

  void validateMarkdown(String contentMd, Long sectionId) {
    Set<Long> allowedAssetIds = new HashSet<>();
    for (TextbookSectionAssetEntity asset : assetMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionAssetEntity>()
            .eq(TextbookSectionAssetEntity::getSectionId, sectionId))) {
      allowedAssetIds.add(asset.getId());
    }
    List<String> violations = TextbookMarkdownSupport.validate(contentMd, allowedAssetIds);
    if (!violations.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "Markdown 校验失败：" + String.join("；", violations));
    }
  }

  int nextSubmitRevision(Long contentId) {
    TextbookSectionContentVersionEntity latest = latestSubmitSnapshot(contentId);
    return latest == null || latest.getSubmitRevision() == null ? 1
        : latest.getSubmitRevision() + 1;
  }

  TextbookSectionContentVersionEntity latestSubmitSnapshot(Long contentId) {
    return versionMapper.selectOne(new LambdaQueryWrapper<TextbookSectionContentVersionEntity>()
        .eq(TextbookSectionContentVersionEntity::getContentId, contentId)
        .eq(TextbookSectionContentVersionEntity::getSnapshotType, SNAPSHOT_SUBMIT)
        .orderByDesc(TextbookSectionContentVersionEntity::getSubmitRevision)
        .last("LIMIT 1"));
  }

  void insertSnapshot(TextbookSectionContentEntity content, String snapshotType,
      Integer submitRevision, String contentMd) {
    Integer maxNo = versionMapper.selectList(new LambdaQueryWrapper<
        TextbookSectionContentVersionEntity>()
            .eq(TextbookSectionContentVersionEntity::getContentId, content.getId())
            .orderByDesc(TextbookSectionContentVersionEntity::getSnapshotNo)
            .last("LIMIT 1"))
        .stream().findFirst().map(TextbookSectionContentVersionEntity::getSnapshotNo).orElse(0);
    TextbookSectionContentVersionEntity snapshot = new TextbookSectionContentVersionEntity();
    snapshot.setContentId(content.getId());
    snapshot.setSectionId(content.getSectionId());
    snapshot.setSnapshotNo(maxNo == null ? 1 : maxNo + 1);
    snapshot.setSubmitRevision(submitRevision);
    snapshot.setSnapshotType(snapshotType);
    snapshot.setContentMd(contentMd);
    snapshot.setWordCount(TextbookMarkdownSupport.wordCount(contentMd));
    try {
      versionMapper.insert(snapshot);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "快照序号冲突（并发提交），请重试；不会产生重复版本", Map.of("reason", "SNAPSHOT_CONFLICT"));
    }
  }

  void insertReviewRecord(Long contentId, Long sectionId, Integer submitRevision, String action,
      String fromStatus, String toStatus, String comment) {
    TextbookSectionReviewRecordEntity record = new TextbookSectionReviewRecordEntity();
    record.setContentId(contentId);
    record.setSectionId(sectionId);
    record.setSubmitRevision(submitRevision);
    record.setAction(action);
    record.setFromStatus(fromStatus);
    record.setToStatus(toStatus);
    record.setComment(comment);
    reviewRecordMapper.insert(record);
  }

  private TextbookOutlineNodeEntity sectionOf(Long sectionId) {
    return nodeMapper.selectById(sectionId);
  }

  TextbookSectionContentEntity contentOf(Long sectionId) {
    return contentMapper.selectOne(new LambdaQueryWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getSectionId, sectionId));
  }

  /** 锁内已重读实体，避免 requireSectionRead 再查一遍锁外数据；直接复用上下文。 */
  private SectionAccess loadAccess(Long sectionId, LockedSection locked) {
    return new SectionAccess(locked.project(), locked.chapter(), locked.section(),
        locked.assignment(), SecurityContext.currentUser(),
        TextbookAccessService.isWriter(SecurityContext.currentUser(), locked.assignment()),
        TextbookAccessService.isReviewer(SecurityContext.currentUser(), locked.assignment()));
  }

  private long parseId(String raw, String message) {
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
  }

  private String writeJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JSON 序列化失败", e);
    }
  }
}

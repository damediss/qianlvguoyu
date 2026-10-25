package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.dto.TextbookAssignmentBatchReq;
import com.guoyu.platform.textbook.dto.TextbookAssignmentBatchReq.Item;
import com.guoyu.platform.textbook.dto.TextbookTaskQueryReq;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionReviewRecordEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookCollaboratorQueryMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionReviewRecordMapper;
import com.guoyu.platform.textbook.service.TextbookAccessService.ChapterAccess;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedChapter;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import com.guoyu.platform.textbook.vo.TextbookAssignmentVO;
import com.guoyu.platform.textbook.vo.TextbookChapterWorkspaceVO;
import com.guoyu.platform.textbook.vo.TextbookChapterWorkspaceVO.PointVO;
import com.guoyu.platform.textbook.vo.TextbookChapterWorkspaceVO.WorkspaceSectionVO;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.ChapterCollaboration;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.Completion;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.Progress;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.ProjectBrief;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.SectionProgress;
import com.guoyu.platform.textbook.vo.TextbookCollaboratorVO;
import com.guoyu.platform.textbook.vo.TextbookReviewRecordVO;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import com.guoyu.platform.textbook.vo.TextbookTaskSummaryVO;
import com.guoyu.platform.textbook.vo.TextbookTaskSummaryVO.ReviewTaskSummaryVO;
import com.guoyu.platform.textbook.vo.TextbookTaskSummaryVO.WritingTaskSummaryVO;
import com.guoyu.platform.textbook.vo.TextbookTaskVO;
import com.guoyu.platform.textbook.vo.TextbookVersionDetailVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工程编写管理与我的任务（方案 §6.1 C1-C6、§6.2 T1-T4、§6.3 W1）。
 *
 * <p>分配/改派/取消/完成/重开全部在工程行锁内执行（§7.1）；批量分配单事务、全量预检、
 * 全部成功才提交；改派更新原记录并 assignment_version+1，不插新行；取消置 active=0，
 * 不删正文/快照/审定记录。完成条件与 C2 共用 {@link TextbookCompletionChecker}。
 */
@Service
public class TextbookAssignmentApplicationService {

  private static final Logger log =
      LoggerFactory.getLogger(TextbookAssignmentApplicationService.class);

  private static final List<String> WRITER_PERMS = List.of(
      TextbookAccessService.PERM_TASK_VIEW, TextbookAccessService.PERM_CONTENT_WRITE);
  private static final List<String> REVIEWER_PERMS = List.of(
      TextbookAccessService.PERM_REVIEW_VIEW, TextbookAccessService.PERM_CONTENT_REVIEW);

  private final TextbookProjectMapper projectMapper;
  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookChapterAssignmentMapper assignmentMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookSectionReviewRecordMapper reviewRecordMapper;
  private final TextbookCollaboratorQueryMapper collaboratorMapper;
  private final TextbookProjectLockService lockService;
  private final TextbookAccessService accessService;
  private final TextbookProgressService progressService;
  private final TextbookCompletionChecker completionChecker;
  private final TextbookOutlineApplicationService outlineService;
  private final TextbookSectionContentApplicationService contentService;

  public TextbookAssignmentApplicationService(
      TextbookProjectMapper projectMapper,
      TextbookOutlineNodeMapper nodeMapper,
      TextbookChapterAssignmentMapper assignmentMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookSectionReviewRecordMapper reviewRecordMapper,
      TextbookCollaboratorQueryMapper collaboratorMapper,
      TextbookProjectLockService lockService,
      TextbookAccessService accessService,
      TextbookProgressService progressService,
      TextbookCompletionChecker completionChecker,
      TextbookOutlineApplicationService outlineService,
      TextbookSectionContentApplicationService contentService) {
    this.projectMapper = projectMapper;
    this.nodeMapper = nodeMapper;
    this.assignmentMapper = assignmentMapper;
    this.contentMapper = contentMapper;
    this.reviewRecordMapper = reviewRecordMapper;
    this.collaboratorMapper = collaboratorMapper;
    this.lockService = lockService;
    this.accessService = accessService;
    this.progressService = progressService;
    this.completionChecker = completionChecker;
    this.outlineService = outlineService;
    this.contentService = contentService;
  }

  // ============================================================
  // C1 候选人
  // ============================================================

  @Transactional(readOnly = true)
  public PageResponse<TextbookCollaboratorVO> collaborators(String role, String keyword,
      long page, long pageSize) {
    List<String> requiredPermissions = requiredPermissionsOf(role);
    Page<TextbookCollaboratorVO> result = new Page<>(page, pageSize);
    List<TextbookCollaboratorVO> records = collaboratorMapper.selectCollaborators(result,
        requiredPermissions, requiredPermissions.size(), keyword == null ? "" : keyword.trim());
    result.setRecords(records);
    return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
  }

  private List<String> requiredPermissionsOf(String role) {
    if ("writer".equals(role)) {
      return WRITER_PERMS;
    }
    if ("reviewer".equals(role)) {
      return REVIEWER_PERMS;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "role 仅支持 writer/reviewer");
  }

  // ============================================================
  // C2 工程编写管理（一次返回全部）
  // ============================================================

  @Transactional(readOnly = true)
  public TextbookCollaborationVO collaboration(Long projectId) {
    TextbookProjectEntity project = accessService.requireProjectView(projectId).project();
    List<TextbookOutlineNodeEntity> chapters = chaptersOf(projectId);
    List<TextbookOutlineNodeEntity> sections = sectionsOf(projectId);
    Map<Long, List<TextbookOutlineNodeEntity>> sectionsByChapter = sections.stream()
        .collect(Collectors.groupingBy(TextbookOutlineNodeEntity::getParentId));
    Map<Long, TextbookSectionContentEntity> contentBySection = contentBySection(sections);
    Map<Long, TextbookChapterAssignmentEntity> assignmentByChapter = new LinkedHashMap<>();
    for (TextbookChapterAssignmentEntity assignment : assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getProjectId, projectId))) {
      assignmentByChapter.put(assignment.getChapterId(), assignment);
    }
    Map<Long, TextbookSectionReviewRecordEntity> latestRecordBySection =
        latestReviewRecords(sections);
    NameIndex names = nameIndex(assignmentByChapter.values(), contentBySection.values(),
        latestRecordBySection.values());

    List<ChapterCollaboration> chapterVos = new ArrayList<>();
    int assignedChapterCount = 0;
    int sectionCount = sections.size();
    int approvedSectionCount = 0;
    long actualWords = 0;
    for (TextbookOutlineNodeEntity chapter : chapters) {
      TextbookChapterAssignmentEntity assignment = assignmentByChapter.get(chapter.getId());
      if (assignment != null && Integer.valueOf(1).equals(assignment.getActive())) {
        assignedChapterCount++;
      }
      List<SectionProgress> sectionVos = new ArrayList<>();
      for (TextbookOutlineNodeEntity section
          : sectionsByChapter.getOrDefault(chapter.getId(), List.of())) {
        TextbookSectionContentEntity content = contentBySection.get(section.getId());
        if (content != null) {
          actualWords += content.getWordCount() == null ? 0 : content.getWordCount();
          if (TextbookSectionContentApplicationService.STATUS_APPROVED
              .equals(content.getContentStatus())) {
            approvedSectionCount++;
          }
        }
        sectionVos.add(new SectionProgress(
            String.valueOf(section.getId()), section.getTitle(), section.getSourceNo(),
            section.getSortNo(), section.getTargetWords(),
            content != null,
            content == null ? null : content.getContentStatus(),
            content == null ? 0 : content.getWordCount(),
            content == null ? 0 : content.getVersion(),
            content == null ? null : content.getSubmittedAt(),
            toReviewRecordVO(latestRecordBySection.get(section.getId()), names)));
      }
      chapterVos.add(new ChapterCollaboration(String.valueOf(chapter.getId()),
          chapter.getTitle(), chapter.getSourceNo(), chapter.getTargetWords(),
          assignment == null ? null : toAssignmentVO(assignment, names), sectionVos));
    }
    OutlineSummaryVO outlineSummary = outlineService.summary(project);
    Completion completion = completionChecker.check(project);
    Progress progress = new Progress(chapters.size(), assignedChapterCount, sectionCount,
        approvedSectionCount, actualWords,
        project.getTotalWords() == null ? 0 : project.getTotalWords());
    return new TextbookCollaborationVO(
        new ProjectBrief(String.valueOf(project.getId()), project.getStatus(),
            project.getWorkTypeTitle(), project.getDirection(), project.getLevelCode()),
        outlineSummary, progress, completion, chapterVos);
  }

  // ============================================================
  // C3 批量分配 / 改派（单事务、全量预检、全部成功才提交）
  // ============================================================

  @Transactional
  public List<TextbookAssignmentVO> assignBatch(Long projectId, TextbookAssignmentBatchReq req) {
    TextbookProjectEntity project = lockService.lockProject(projectId);
    requireAssignable(project);

    // 全量预检（排序后按章升序处理，锁序一致防死锁）
    List<Item> items = req.items().stream()
        .sorted(Comparator.comparing(Item::chapterId)).toList();
    LocalDate today = LocalDate.now();
    Map<String, ParsedItem> parsed = new LinkedHashMap<>();
    for (Item item : items) {
      if (parsed.containsKey(item.chapterId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一章在请求中出现多次：" + item.chapterId());
      }
      ParsedItem p = precheckItem(project, item, today);
      parsed.put(item.chapterId(), p);
    }

    Long operatorId = SecurityContext.currentUser().getUserId();
    List<TextbookAssignmentVO> result = new ArrayList<>();
    boolean anyActive = false;
    for (ParsedItem p : parsed.values()) {
      TextbookChapterAssignmentEntity existing = lockService.lockAssignmentByChapter(p.chapterId);
      if (existing == null) {
        if (p.assignmentVersion() != null) {
          throw assignmentConflict(null,
              "该章在服务端从未有分配记录，assignmentVersion 应为 null");
        }
        TextbookChapterAssignmentEntity created = new TextbookChapterAssignmentEntity();
        created.setProjectId(projectId);
        created.setChapterId(p.chapterId());
        created.setWriterId(p.writerId());
        created.setReviewerId(p.reviewerId());
        created.setDueDate(p.dueDate());
        created.setInstruction(p.instruction());
        created.setActive(1);
        created.setTaskStatus(TextbookProgressService.TASK_ASSIGNED);
        created.setAssignedBy(operatorId);
        created.setAssignedAt(LocalDateTime.now());
        created.setAssignmentVersion(1);
        assignmentMapper.insert(created);
        anyActive = true;
        progressService.refreshChapterSummary(p.chapterId());
        result.add(toAssignmentVO(created, null));
      } else {
        if (p.assignmentVersion() == null
            || !p.assignmentVersion().equals(existing.getAssignmentVersion())) {
          throw assignmentConflict(existing, "分配版本冲突，请刷新后重试");
        }
        existing.setWriterId(p.writerId());
        existing.setReviewerId(p.reviewerId());
        existing.setDueDate(p.dueDate());
        existing.setInstruction(p.instruction());
        existing.setActive(1);
        existing.setAssignedBy(operatorId);
        existing.setAssignedAt(LocalDateTime.now());
        existing.setAssignmentVersion(existing.getAssignmentVersion() + 1);
        assignmentMapper.updateById(existing);
        anyActive = true;
        progressService.refreshChapterSummary(p.chapterId());
        result.add(toAssignmentVO(existing, null));
      }
    }
    // 首个有效分配创建后 → writing（方案 §3.1）
    if (anyActive && TextbookProjectApplicationService.STATUS_REVIEWED
        .equals(project.getStatus())) {
      projectMapper.update(null, new LambdaUpdateWrapper<TextbookProjectEntity>()
          .eq(TextbookProjectEntity::getId, projectId)
          .eq(TextbookProjectEntity::getStatus,
              TextbookProjectApplicationService.STATUS_REVIEWED)
          .set(TextbookProjectEntity::getStatus,
              TextbookProjectApplicationService.STATUS_WRITING));
    }
    log.info("[教材] 批量分配：projectId={} 章数={}", projectId, result.size());
    // 全部落库后统一回读并装配名称（含同事务内新插入的行）
    Map<Long, TextbookChapterAssignmentEntity> fresh = new LinkedHashMap<>();
    for (TextbookChapterAssignmentEntity row : assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getProjectId, projectId)
            .in(TextbookChapterAssignmentEntity::getChapterId, parsed.keySet().stream()
                .map(s -> parseId(s, "chapterId 非法")).toList()))) {
      fresh.put(row.getChapterId(), row);
    }
    NameIndex names = nameIndex(fresh.values(), null, null);
    return result.stream()
        .map(vo -> {
          TextbookChapterAssignmentEntity row = fresh.get(parseId(vo.chapterId(), "chapterId"));
          return row == null ? vo : toAssignmentVO(row, names);
        })
        .toList();
  }

  private record ParsedItem(Long chapterId, Long writerId, Long reviewerId, LocalDate dueDate,
      String instruction, Integer assignmentVersion, String chapterTitle) {
  }

  private ParsedItem precheckItem(TextbookProjectEntity project, Item item, LocalDate today) {
    long chapterId = parseId(item.chapterId(), "chapterId 非法");
    long writerId = parseId(item.writerId(), "writerId 非法");
    long reviewerId = parseId(item.reviewerId(), "reviewerId 非法");
    if (writerId == reviewerId) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一章的主笔与审定人不能是同一人");
    }
    TextbookOutlineNodeEntity chapter = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, chapterId)
            .eq(TextbookOutlineNodeEntity::getProjectId, project.getId())
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_CHAPTER));
    if (chapter == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "章 " + item.chapterId() + " 不属于该工程或不是章级节点");
    }
    Long sectionCount = nodeMapper.selectCount(new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
        .eq(TextbookOutlineNodeEntity::getParentId, chapterId)
        .eq(TextbookOutlineNodeEntity::getNodeLevel,
            TextbookOutlineApplicationService.LEVEL_SECTION));
    if (sectionCount == null || sectionCount == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "章「" + chapter.getTitle() + "」下没有任何节，不能分配（数据异常请先修正大纲）");
    }
    // 主笔/审定人：同租户、enabled 且同时拥有对应权限（方案 §3.4-3）
    if (collaboratorMapper.countUserPermissions(writerId, WRITER_PERMS) < WRITER_PERMS.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "主笔（" + item.writerId() + "）不是启用中的教材编写专家"
              + "（需绑定角色并拥有 textbook:task:view + textbook:content:write）");
    }
    if (collaboratorMapper.countUserPermissions(reviewerId, REVIEWER_PERMS)
        < REVIEWER_PERMS.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "审定人（" + item.reviewerId() + "）不是启用中的教材审定专家"
              + "（需绑定角色并拥有 textbook:review:view + textbook:content:review）");
    }
    LocalDate dueDate = null;
    if (item.dueDate() != null && !item.dueDate().isBlank()) {
      try {
        dueDate = LocalDate.parse(item.dueDate().trim());
      } catch (RuntimeException e) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "截止日期格式应为 yyyy-MM-dd：" + item.dueDate());
      }
    }
    return new ParsedItem(chapterId, writerId, reviewerId, dueDate,
        item.instruction() == null ? null : item.instruction().trim(),
        item.assignmentVersion(), chapter.getTitle());
  }

  private void requireAssignable(TextbookProjectEntity project) {
    String status = project.getStatus();
    if (TextbookProjectApplicationService.STATUS_DONE.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "工程已完成（done），请先重新打开再分配");
    }
    if (!TextbookProjectApplicationService.STATUS_REVIEWED.equals(status)
        && !TextbookProjectApplicationService.STATUS_WRITING.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "仅 reviewed/writing 工程可分配，当前：" + status);
    }
  }

  // ============================================================
  // C4 取消分配
  // ============================================================

  @Transactional
  public TextbookAssignmentVO cancelAssignment(Long chapterId, Integer assignmentVersion) {
    LockedChapter locked = lockService.lockByChapter(chapterId);
    requireAssignable(locked.project());
    TextbookChapterAssignmentEntity assignment = locked.assignment();
    if (assignment == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "该章没有分配记录");
    }
    if (assignmentVersion == null
        || !assignmentVersion.equals(assignment.getAssignmentVersion())) {
      throw assignmentConflict(assignment, "分配版本冲突，请刷新后重试");
    }
    assignment.setActive(0);
    assignment.setAssignmentVersion(assignment.getAssignmentVersion() + 1);
    assignment.setAssignedBy(SecurityContext.currentUser().getUserId());
    assignment.setAssignedAt(LocalDateTime.now());
    assignmentMapper.updateById(assignment);
    progressService.refreshChapterSummary(chapterId);
    log.info("[教材] 取消分配：chapterId={} 新版本={}", chapterId,
        assignment.getAssignmentVersion());
    NameIndex names = nameIndex(List.of(assignment), null, null);
    return toAssignmentVO(assignment, names);
  }

  /** 409 data：reason=ASSIGNMENT_VERSION_CONFLICT + currentAssignment（方案 §6 通用约定）。 */
  private BusinessException assignmentConflict(TextbookChapterAssignmentEntity current,
      String message) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("reason", "ASSIGNMENT_VERSION_CONFLICT");
    data.put("currentAssignment",
        current == null ? null : toAssignmentVO(current, nameIndex(List.of(current), null, null)));
    return new BusinessException(ErrorCode.CONFLICT, message, data);
  }

  // ============================================================
  // C5 完成 / C6 重新打开（done 的唯一进出门）
  // ============================================================

  @Transactional
  public Completion complete(Long projectId) {
    TextbookProjectEntity project = lockService.lockProject(projectId);
    if (!TextbookProjectApplicationService.STATUS_WRITING.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "仅 writing 工程可完成编写，当前：" + project.getStatus());
    }
    // 工程锁内重新查询所有章/节/分配/占位，不只用缓存（方案 §7.1-5）
    Completion completion = completionChecker.check(project);
    if (!completion.canComplete()) {
      throw new BusinessException(ErrorCode.CONFLICT, "完成条件不满足，请先处理阻塞项", completion);
    }
    int updated = projectMapper.update(null, new LambdaUpdateWrapper<TextbookProjectEntity>()
        .eq(TextbookProjectEntity::getId, projectId)
        .eq(TextbookProjectEntity::getStatus,
            TextbookProjectApplicationService.STATUS_WRITING)
        .set(TextbookProjectEntity::getStatus, TextbookProjectApplicationService.STATUS_DONE));
    if (updated == 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "并发流转冲突，请刷新后重试");
    }
    log.info("[教材] 完成编写：projectId={}", projectId);
    return completion;
  }

  @Transactional
  public void reopen(Long projectId) {
    TextbookProjectEntity project = lockService.lockProject(projectId);
    if (!TextbookProjectApplicationService.STATUS_DONE.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "仅 done 工程可重新打开，当前：" + project.getStatus());
    }
    int updated = projectMapper.update(null, new LambdaUpdateWrapper<TextbookProjectEntity>()
        .eq(TextbookProjectEntity::getId, projectId)
        .eq(TextbookProjectEntity::getStatus, TextbookProjectApplicationService.STATUS_DONE)
        .set(TextbookProjectEntity::getStatus,
            TextbookProjectApplicationService.STATUS_WRITING));
    if (updated == 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "并发流转冲突，请刷新后重试");
    }
    log.info("[教材] 重新打开：projectId={}", projectId);
  }

  // ============================================================
  // T1/T2 我的任务
  // ============================================================

  @Transactional(readOnly = true)
  public PageResponse<TextbookTaskVO> myTasks(boolean review, TextbookTaskQueryReq req) {
    Long userId = SecurityContext.currentUser().getUserId();
    LambdaQueryWrapper<TextbookChapterAssignmentEntity> wrapper =
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getActive, 1)
            .eq(review ? TextbookChapterAssignmentEntity::getReviewerId
                : TextbookChapterAssignmentEntity::getWriterId, userId);
    String status = req.getStatus() == null || req.getStatus().isBlank()
        ? (review ? "pending" : null) : req.getStatus();
    if (review) {
      switch (status) {
        case "pending" -> wrapper.apply("submitted_count > 0");
        case "returned" -> wrapper.apply("returned_count > 0");
        case "approved" -> wrapper.eq(TextbookChapterAssignmentEntity::getTaskStatus,
            TextbookProgressService.TASK_APPROVED);
        case "all" -> {
        }
        default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
            "review status 仅支持 pending/returned/approved/all");
      }
    } else if (status != null) {
      switch (status) {
        case "overdue" -> wrapper.apply(
            "due_date IS NOT NULL AND due_date < CURDATE() AND task_status <> {0}",
            TextbookProgressService.TASK_APPROVED);
        case "returned" -> wrapper.apply("returned_count > 0");
        case "assigned", "writing", "reviewing", "approved" ->
            wrapper.eq(TextbookChapterAssignmentEntity::getTaskStatus, status);
        default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
            "writing status 仅支持 assigned/writing/reviewing/returned/approved/overdue");
      }
    }
    if (Boolean.TRUE.equals(req.getOverdueOnly())) {
      wrapper.apply("due_date IS NOT NULL AND due_date < CURDATE() AND task_status <> {0}",
          TextbookProgressService.TASK_APPROVED);
    }
    String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim();
    if (!keyword.isEmpty()) {
      // 参数化 EXISTS 子查询：关键字走 {0} 绑定参数，不拼接 SQL 字符串
      wrapper.exists("""
          SELECT 1 FROM textbook_outline o
          WHERE o.id = textbook_chapter_assignment.chapter_id
            AND o.title LIKE CONCAT('%', {0}, '%')
          """, keyword)
          .or(keywordScope -> keywordScope.exists("""
              SELECT 1 FROM textbook_project p
              WHERE p.id = textbook_chapter_assignment.project_id
                AND (p.work_type_title LIKE CONCAT('%', {0}, '%')
                     OR p.direction LIKE CONCAT('%', {0}, '%'))
              """, keyword));
    }
    // 默认排序：逾期优先 → 最近节更新倒序（NULL 最后）→ assignmentId 倒序（方案 §6.2）
    String latestUpdate = "(SELECT MAX(c.updated_at) FROM textbook_section_content c WHERE "
        + "c.chapter_id = textbook_chapter_assignment.chapter_id AND c.deleted = 0)";
    wrapper.last("ORDER BY (due_date IS NOT NULL AND due_date < CURDATE() "
        + "AND task_status <> 'approved') DESC, " + latestUpdate + " IS NULL ASC, "
        + latestUpdate + " DESC, id DESC");

    Page<TextbookChapterAssignmentEntity> result =
        assignmentMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<TextbookTaskVO> items = enrichTasks(result.getRecords());
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  private List<TextbookTaskVO> enrichTasks(List<TextbookChapterAssignmentEntity> assignments) {
    if (assignments.isEmpty()) {
      return List.of();
    }
    List<Long> chapterIds = assignments.stream()
        .map(TextbookChapterAssignmentEntity::getChapterId).toList();
    List<Long> projectIds = assignments.stream()
        .map(TextbookChapterAssignmentEntity::getProjectId).distinct().toList();
    Map<Long, TextbookOutlineNodeEntity> chapters = new HashMap<>();
    for (TextbookOutlineNodeEntity node : nodeMapper.selectBatchIds(chapterIds)) {
      chapters.put(node.getId(), node);
    }
    Map<Long, TextbookProjectEntity> projects = new HashMap<>();
    for (TextbookProjectEntity project : projectMapper.selectBatchIds(projectIds)) {
      projects.put(project.getId(), project);
    }
    Map<Long, TextbookSectionContentEntity> contentByChapter = new HashMap<>();
    for (TextbookSectionContentEntity content : contentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .in(TextbookSectionContentEntity::getChapterId, chapterIds))) {
      contentByChapter.merge(content.getChapterId(), content, (a, b) ->
          a.getUpdatedAt() != null && b.getUpdatedAt() != null
              && a.getUpdatedAt().isAfter(b.getUpdatedAt()) ? a : b);
    }
    Map<Long, Long> wordSumByChapter = new HashMap<>();
    Map<Long, LocalDateTime> latestUpdateByChapter = new HashMap<>();
    for (TextbookSectionContentEntity content : contentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .in(TextbookSectionContentEntity::getChapterId, chapterIds))) {
      wordSumByChapter.merge(content.getChapterId(),
          content.getWordCount() == null ? 0L : content.getWordCount(), Long::sum);
      if (content.getUpdatedAt() != null) {
        latestUpdateByChapter.merge(content.getChapterId(), content.getUpdatedAt(),
            (a, b) -> a.isAfter(b) ? a : b);
      }
    }
    NameIndex names = nameIndex(assignments, null, null);
    LocalDate today = LocalDate.now();
    List<TextbookTaskVO> items = new ArrayList<>();
    for (TextbookChapterAssignmentEntity assignment : assignments) {
      TextbookOutlineNodeEntity chapter = chapters.get(assignment.getChapterId());
      TextbookProjectEntity project = projects.get(assignment.getProjectId());
      if (chapter == null || project == null) {
        continue;
      }
      boolean overdue = assignment.getDueDate() != null
          && assignment.getDueDate().isBefore(today)
          && !TextbookProgressService.TASK_APPROVED.equals(assignment.getTaskStatus());
      Integer chapterWords = chapter.getTargetWords();
      items.add(new TextbookTaskVO(
          toAssignmentVO(assignment, names),
          String.valueOf(project.getId()), project.getWorkTypeTitle(), project.getDirection(),
          project.getLevelCode(), chapter.getTitle(), chapter.getSourceNo(),
          project.getStatus(), latestUpdateByChapter.get(assignment.getChapterId()),
          wordSumByChapter.getOrDefault(assignment.getChapterId(), 0L).intValue(),
          chapterWords, overdue));
    }
    return items;
  }

  // ============================================================
  // T3/T4 统计卡（当前用户 + active 全量，不受筛选/分页影响）
  // ============================================================

  @Transactional(readOnly = true)
  public TextbookTaskSummaryVO summaries() {
    Long userId = SecurityContext.currentUser().getUserId();
    List<TextbookChapterAssignmentEntity> writingRows = assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getActive, 1)
            .eq(TextbookChapterAssignmentEntity::getWriterId, userId));
    List<TextbookChapterAssignmentEntity> reviewRows = assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getActive, 1)
            .eq(TextbookChapterAssignmentEntity::getReviewerId, userId));
    return new TextbookTaskSummaryVO(
        writingSummary(writingRows), reviewSummary(reviewRows));
  }

  /** T3：/my-writing-tasks/summary 的扁平契约（前端直接读 assignedCount 等字段）。 */
  @Transactional(readOnly = true)
  public WritingTaskSummaryVO writingSummaryForCurrentUser() {
    Long userId = SecurityContext.currentUser().getUserId();
    return writingSummary(assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getActive, 1)
            .eq(TextbookChapterAssignmentEntity::getWriterId, userId)));
  }

  /** T4：/my-review-tasks/summary 的扁平契约。 */
  @Transactional(readOnly = true)
  public ReviewTaskSummaryVO reviewSummaryForCurrentUser() {
    Long userId = SecurityContext.currentUser().getUserId();
    return reviewSummary(assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getActive, 1)
            .eq(TextbookChapterAssignmentEntity::getReviewerId, userId)));
  }

  WritingTaskSummaryVO writingSummary(List<TextbookChapterAssignmentEntity> rows) {
    long assigned = 0;
    long writing = 0;
    long pendingReview = 0;
    long returned = 0;
    long approved = 0;
    for (TextbookChapterAssignmentEntity row : rows) {
      if (TextbookProgressService.TASK_ASSIGNED.equals(row.getTaskStatus())) {
        assigned++;
      }
      if (TextbookProgressService.TASK_WRITING.equals(row.getTaskStatus())) {
        writing++;
      }
      if (nz(row.getSubmittedCount()) > 0) {
        pendingReview++;
      }
      if (nz(row.getReturnedCount()) > 0) {
        returned++;
      }
      if (TextbookProgressService.TASK_APPROVED.equals(row.getTaskStatus())) {
        approved++;
      }
    }
    return new WritingTaskSummaryVO(assigned, writing, pendingReview, returned, approved);
  }

  ReviewTaskSummaryVO reviewSummary(List<TextbookChapterAssignmentEntity> rows) {
    long pending = 0;
    long returned = 0;
    long partialApproved = 0;
    long approved = 0;
    for (TextbookChapterAssignmentEntity row : rows) {
      if (nz(row.getSubmittedCount()) > 0) {
        pending++;
      }
      if (nz(row.getReturnedCount()) > 0) {
        returned++;
      }
      if (nz(row.getApprovedCount()) > 0
          && !TextbookProgressService.TASK_APPROVED.equals(row.getTaskStatus())) {
        partialApproved++;
      }
      if (TextbookProgressService.TASK_APPROVED.equals(row.getTaskStatus())) {
        approved++;
      }
    }
    return new ReviewTaskSummaryVO(pending, returned, partialApproved, approved);
  }

  // ============================================================
  // W1 章工作台
  // ============================================================

  @Transactional(readOnly = true)
  public TextbookChapterWorkspaceVO workspace(Long chapterId) {
    ChapterAccess access = accessService.requireChapterRead(chapterId);
    List<TextbookOutlineNodeEntity> sections = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, chapterId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    Map<Long, List<TextbookOutlineNodeEntity>> pointsBySection = new HashMap<>();
    if (!sections.isEmpty()) {
      for (TextbookOutlineNodeEntity point : nodeMapper.selectList(
          new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
              .in(TextbookOutlineNodeEntity::getParentId,
                  sections.stream().map(TextbookOutlineNodeEntity::getId).toList())
              .eq(TextbookOutlineNodeEntity::getNodeLevel,
                  TextbookOutlineApplicationService.LEVEL_POINT)
              .orderByAsc(TextbookOutlineNodeEntity::getSortNo))) {
        pointsBySection.computeIfAbsent(point.getParentId(), k -> new ArrayList<>()).add(point);
      }
    }
    Map<Long, TextbookSectionContentEntity> contentBySection = contentBySection(sections);
    Map<Long, TextbookSectionReviewRecordEntity> latestRecords = latestReviewRecords(sections);
    NameIndex names = nameIndex(access.assignment() == null ? List.of() : List.of(access.assignment()),
        contentBySection.values(), latestRecords.values());

    boolean writing = TextbookProjectApplicationService.STATUS_WRITING
        .equals(access.project().getStatus());
    boolean active = access.assignment() != null
        && Integer.valueOf(1).equals(access.assignment().getActive());
    boolean canWrite = writing && active && access.writerRelation()
        && TextbookAccessService.has(access.principal(), TextbookAccessService.PERM_CONTENT_WRITE);
    boolean canReview = writing && active && access.reviewerRelation()
        && TextbookAccessService.has(access.principal(), TextbookAccessService.PERM_CONTENT_REVIEW);

    List<WorkspaceSectionVO> sectionVos = new ArrayList<>();
    for (TextbookOutlineNodeEntity section : sections) {
      TextbookSectionContentEntity content = contentBySection.get(section.getId());
      Integer submitRevision = null;
      String currentSnapshotId = null;
      if (content != null && (TextbookSectionContentApplicationService.STATUS_SUBMITTED
          .equals(content.getContentStatus())
          || TextbookSectionContentApplicationService.STATUS_APPROVED
              .equals(content.getContentStatus()))) {
        var snapshot = contentService.latestSubmitSnapshot(content.getId());
        if (snapshot != null) {
          submitRevision = snapshot.getSubmitRevision();
          currentSnapshotId = String.valueOf(snapshot.getId());
        }
      }
      List<PointVO> points = new ArrayList<>();
      for (TextbookOutlineNodeEntity point
          : pointsBySection.getOrDefault(section.getId(), List.of())) {
        points.add(new PointVO(String.valueOf(point.getId()),
            point.getTitleRaw() != null ? point.getTitleRaw() : point.getTitle(),
            point.getDimension(), point.getSortNo()));
      }
      sectionVos.add(new WorkspaceSectionVO(
          String.valueOf(section.getId()), section.getTitle(), section.getSourceNo(),
          section.getSortNo(), section.getTargetWords(),
          content != null,
          content == null ? null : content.getContentStatus(),
          content == null ? 0 : content.getWordCount(),
          content == null ? 0 : content.getVersion(),
          content == null ? null : content.getSubmittedAt(),
          content == null || content.getSubmittedBy() == null ? null
              : names.of(content.getSubmittedBy()),
          submitRevision, currentSnapshotId,
          toReviewRecordVO(latestRecords.get(section.getId()), names), points));
    }
    List<String> relations = new ArrayList<>();
    if (access.writerRelation()) {
      relations.add("writer");
    }
    if (access.reviewerRelation()) {
      relations.add("reviewer");
    }
    String readOnlyReason = null;
    if (!canWrite && !canReview) {
      readOnlyReason = access.assignment() == null ? "该章未分配或分配已取消"
          : TextbookProjectApplicationService.STATUS_DONE.equals(access.project().getStatus())
              ? "工程已完成（done），全页只读"
              : "您不是该章的当前主笔或审定人";
    }
    return new TextbookChapterWorkspaceVO(
        String.valueOf(access.project().getId()), access.project().getStatus(),
        access.project().getWorkTypeTitle(), access.project().getDirection(),
        access.project().getLevelCode(),
        String.valueOf(access.chapter().getId()), access.chapter().getTitle(),
        access.chapter().getSourceNo(),
        access.assignment() == null ? null : toAssignmentVO(access.assignment(), names),
        relations, canWrite, canReview, readOnlyReason, sectionVos);
  }

  // ============================================================
  // 内部工具
  // ============================================================

  private static int nz(Integer value) {
    return value == null ? 0 : value;
  }

  private long parseId(String raw, String message) {
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
  }

  private List<TextbookOutlineNodeEntity> chaptersOf(Long projectId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
        .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
        .eq(TextbookOutlineNodeEntity::getNodeLevel,
            TextbookOutlineApplicationService.LEVEL_CHAPTER)
        .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
        .orderByAsc(TextbookOutlineNodeEntity::getId));
  }

  private List<TextbookOutlineNodeEntity> sectionsOf(Long projectId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
        .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
        .eq(TextbookOutlineNodeEntity::getNodeLevel,
            TextbookOutlineApplicationService.LEVEL_SECTION)
        .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
        .orderByAsc(TextbookOutlineNodeEntity::getId));
  }

  private Map<Long, TextbookSectionContentEntity> contentBySection(
      List<TextbookOutlineNodeEntity> sections) {
    if (sections.isEmpty()) {
      return Map.of();
    }
    Map<Long, TextbookSectionContentEntity> bySection = new HashMap<>();
    for (TextbookSectionContentEntity content : contentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .in(TextbookSectionContentEntity::getSectionId,
                sections.stream().map(TextbookOutlineNodeEntity::getId).toList()))) {
      bySection.put(content.getSectionId(), content);
    }
    return bySection;
  }

  private Map<Long, TextbookSectionReviewRecordEntity> latestReviewRecords(
      List<TextbookOutlineNodeEntity> sections) {
    if (sections.isEmpty()) {
      return Map.of();
    }
    Map<Long, TextbookSectionReviewRecordEntity> latest = new HashMap<>();
    for (TextbookSectionReviewRecordEntity record : reviewRecordMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionReviewRecordEntity>()
            .in(TextbookSectionReviewRecordEntity::getSectionId,
                sections.stream().map(TextbookOutlineNodeEntity::getId).toList())
            .orderByDesc(TextbookSectionReviewRecordEntity::getId))) {
      latest.putIfAbsent(record.getSectionId(), record);
    }
    return latest;
  }

  /** 名称索引：一次批量取齐 writer/reviewer/assignedBy/submittedBy/reviewedBy/记录操作人。 */
  private NameIndex nameIndex(Iterable<TextbookChapterAssignmentEntity> assignments,
      Iterable<TextbookSectionContentEntity> contents,
      Iterable<TextbookSectionReviewRecordEntity> records) {
    Set<Long> ids = new HashSet<>();
    if (assignments != null) {
      for (TextbookChapterAssignmentEntity assignment : assignments) {
        ids.add(assignment.getWriterId());
        ids.add(assignment.getReviewerId());
        ids.add(assignment.getAssignedBy());
      }
    }
    if (contents != null) {
      for (TextbookSectionContentEntity content : contents) {
        if (content.getSubmittedBy() != null) {
          ids.add(content.getSubmittedBy());
        }
        if (content.getReviewedBy() != null) {
          ids.add(content.getReviewedBy());
        }
      }
    }
    if (records != null) {
      for (TextbookSectionReviewRecordEntity record : records) {
        if (record.getCreatedBy() != null) {
          ids.add(record.getCreatedBy());
        }
      }
    }
    return new NameIndex(accessService.displayNames(ids));
  }

  private record NameIndex(Map<Long, String> names) {

    String of(Long id) {
      return id == null ? "" : names.getOrDefault(id, "");
    }
  }

  TextbookAssignmentVO toAssignmentVO(TextbookChapterAssignmentEntity assignment,
      NameIndex names) {
    NameIndex index = names == null ? new NameIndex(Map.of()) : names;
    int sectionCount = nz(assignment.getSectionCount());
    int withContent = sectionCount - nz(assignment.getDraftCount())
        - nz(assignment.getSubmittedCount()) - nz(assignment.getApprovedCount())
        - nz(assignment.getReturnedCount());
    // withContent 由各状态缓存合计推导；首次分配后刷新前 counts 可能为 0，untouched 按 sectionCount 兜底
    int untouched = Math.max(withContent, 0);
    if (sectionCount == 0) {
      untouched = 0;
    }
    return new TextbookAssignmentVO(
        String.valueOf(assignment.getChapterId()),
        String.valueOf(assignment.getWriterId()), index.of(assignment.getWriterId()),
        String.valueOf(assignment.getReviewerId()), index.of(assignment.getReviewerId()),
        assignment.getDueDate(), assignment.getInstruction(),
        Integer.valueOf(1).equals(assignment.getActive()),
        assignment.getTaskStatus(), assignment.getAssignmentVersion(),
        assignment.getSectionCount(), assignment.getApprovedCount(),
        assignment.getSubmittedCount(), assignment.getReturnedCount(),
        assignment.getDraftCount(), untouched,
        index.of(assignment.getAssignedBy()), assignment.getAssignedAt());
  }

  private TextbookReviewRecordVO toReviewRecordVO(TextbookSectionReviewRecordEntity record,
      NameIndex names) {
    if (record == null) {
      return null;
    }
    return new TextbookReviewRecordVO(String.valueOf(record.getId()),
        record.getSubmitRevision(), record.getAction(), record.getFromStatus(),
        record.getToStatus(), record.getComment(), names.of(record.getCreatedBy()),
        record.getCreatedAt());
  }
}

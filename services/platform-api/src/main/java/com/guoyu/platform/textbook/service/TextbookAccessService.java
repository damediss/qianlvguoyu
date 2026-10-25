package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 三层授权统一入口（方案 §4.4，深模块）：Controller 不各自查询 assignment 后拼 if。
 *
 * <p>三层依次：功能权限（@PreAuthorize）→ 工程可见性（project:view 或该章有效分配归属）
 * → 章节归属与状态（写=当前主笔、审定=当前审定人）。
 *
 * <p>读取授权与写能力计算分离：requireSectionRead 只判定「能读」并装载 relations
 * （writer/reviewer 归属布尔，供 VO 组装），不裁决 canWrite/canReview；写/审定方法必须
 * 再分别比较当前用户、对应内容权限、工程/节状态（requireCurrentWriter/Reviewer）。
 *
 * <p>对象级无权访问统一 404（防探测）；有身份但缺功能权限由 Security 层 403。
 * 仅 HTTP 请求线程使用（二期无 Worker 调用）；不从前端接收 tenantId/userId 做授权依据。
 */
@Service
public class TextbookAccessService {

  public static final String PERM_PROJECT_VIEW = "textbook:project:view";
  public static final String PERM_PROJECT_MANAGE = "textbook:project:manage";
  public static final String PERM_TASK_VIEW = "textbook:task:view";
  public static final String PERM_CONTENT_WRITE = "textbook:content:write";
  public static final String PERM_REVIEW_VIEW = "textbook:review:view";
  public static final String PERM_CONTENT_REVIEW = "textbook:content:review";

  private final TextbookProjectMapper projectMapper;
  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookChapterAssignmentMapper assignmentMapper;
  private final UserMapper userMapper;

  public TextbookAccessService(TextbookProjectMapper projectMapper,
      TextbookOutlineNodeMapper nodeMapper,
      TextbookChapterAssignmentMapper assignmentMapper,
      UserMapper userMapper) {
    this.projectMapper = projectMapper;
    this.nodeMapper = nodeMapper;
    this.assignmentMapper = assignmentMapper;
    this.userMapper = userMapper;
  }

  // ============================================================
  // 上下文记录（一次装载，后续 Service 不重复查询）
  // ============================================================

  public record ProjectAccess(TextbookProjectEntity project, PlatformUserPrincipal principal,
                              boolean viewer) {
  }

  public record ChapterAccess(TextbookProjectEntity project,
                              TextbookOutlineNodeEntity chapter,
                              TextbookChapterAssignmentEntity assignment,
                              PlatformUserPrincipal principal,
                              boolean writerRelation, boolean reviewerRelation) {
  }

  public record SectionAccess(TextbookProjectEntity project,
                              TextbookOutlineNodeEntity chapter,
                              TextbookOutlineNodeEntity section,
                              TextbookChapterAssignmentEntity assignment,
                              PlatformUserPrincipal principal,
                              boolean writerRelation, boolean reviewerRelation) {

    public List<String> relations() {
      List<String> relations = new ArrayList<>();
      if (writerRelation) {
        relations.add("writer");
      }
      if (reviewerRelation) {
        relations.add("reviewer");
      }
      return relations;
    }
  }

  // ============================================================
  // 工程级
  // ============================================================

  /** 工程可见性：project:view 可只读全部；对象不存在/他租户统一 404。 */
  public ProjectAccess requireProjectView(Long projectId) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    TextbookProjectEntity project = findProject(projectId);
    return new ProjectAccess(project, principal, has(principal, PERM_PROJECT_VIEW));
  }

  /** 管理操作上下文（@PreAuthorize 已挡功能权限；此处装载对象并 404）。 */
  public ProjectAccess requireProjectManage(Long projectId) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    TextbookProjectEntity project = findProject(projectId);
    return new ProjectAccess(project, principal, has(principal, PERM_PROJECT_VIEW));
  }

  // ============================================================
  // 章 / 节读取
  // ============================================================

  /** 章读取（W1/章节导出）：project:view 或该章有效分配的主笔/审定人。 */
  public ChapterAccess requireChapterRead(Long chapterId) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    TextbookOutlineNodeEntity chapter = findChapter(chapterId);
    TextbookProjectEntity project = findProject(chapter.getProjectId());
    TextbookChapterAssignmentEntity assignment = activeAssignmentOf(chapter.getId());
    boolean writer = isWriter(principal, assignment);
    boolean reviewer = isReviewer(principal, assignment);
    if (!has(principal, PERM_PROJECT_VIEW) && !writer && !reviewer) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在或无权访问");
    }
    return new ChapterAccess(project, chapter, assignment, principal, writer, reviewer);
  }

  /** 节读取（W2/W6/W7/W8/节导出/A2）。 */
  public SectionAccess requireSectionRead(Long sectionId) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    TextbookOutlineNodeEntity section = findSection(sectionId);
    TextbookOutlineNodeEntity chapter = findChapter(section.getParentId());
    TextbookProjectEntity project = findProject(section.getProjectId());
    TextbookChapterAssignmentEntity assignment = activeAssignmentOf(chapter.getId());
    boolean writer = isWriter(principal, assignment);
    boolean reviewer = isReviewer(principal, assignment);
    if (!has(principal, PERM_PROJECT_VIEW) && !writer && !reviewer) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在或无权访问");
    }
    return new SectionAccess(project, chapter, section, assignment, principal, writer, reviewer);
  }

  // ============================================================
  // 写 / 审定（在工程行锁内、用锁后实体调用）
  // ============================================================

  /**
   * 当前主笔校验（W3/W4/W9、A1/A3）：task:view + content:write + 分配 active +
   * 当前用户==writerId + 工程 writing。
   * 分配缺失/取消 → 409；非当前主笔 → 404（对象级无权访问统一 404，防探测，
   * 含改派后的原主笔）；缺功能权限 → 403（正常入口由 @PreAuthorize 先拦）；工程非 writing → 409。
   */
  public void requireCurrentWriter(TextbookProjectEntity project,
      TextbookChapterAssignmentEntity assignment, PlatformUserPrincipal principal) {
    if (assignment == null || !Integer.valueOf(1).equals(assignment.getActive())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该章尚未分配或分配已取消，不能编写");
    }
    if (!assignment.getWriterId().equals(principal.getUserId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在或无权访问");
    }
    if (!has(principal, PERM_TASK_VIEW) || !has(principal, PERM_CONTENT_WRITE)) {
      throw new BusinessException(ErrorCode.FORBIDDEN,
          "缺少教材编写权限（textbook:task:view + textbook:content:write）");
    }
    requireProjectWriting(project);
  }

  /**
   * 当前审定人校验（W5）：review:view + content:review + 分配 active +
   * 当前用户==reviewerId + 工程 writing。非当前审定人 → 404（对象级统一 404）。
   */
  public void requireCurrentReviewer(TextbookProjectEntity project,
      TextbookChapterAssignmentEntity assignment, PlatformUserPrincipal principal) {
    if (assignment == null || !Integer.valueOf(1).equals(assignment.getActive())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该章尚未分配或分配已取消，不能审定");
    }
    if (!assignment.getReviewerId().equals(principal.getUserId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在或无权访问");
    }
    if (!has(principal, PERM_REVIEW_VIEW) || !has(principal, PERM_CONTENT_REVIEW)) {
      throw new BusinessException(ErrorCode.FORBIDDEN,
          "缺少教材审定权限（textbook:review:view + textbook:content:review）");
    }
    requireProjectWriting(project);
  }

  /** done 下一切写操作 409（方案 §3.1-5）；唯一重开入口是 C6。 */
  public void requireProjectWriting(TextbookProjectEntity project) {
    if (TextbookProjectApplicationService.STATUS_DONE.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程已完成（done），需教研先重新打开为 writing");
    }
    if (!TextbookProjectApplicationService.STATUS_WRITING.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程当前状态不允许该操作：" + project.getStatus());
    }
  }

  // ============================================================
  // 内部工具
  // ============================================================

  public static boolean has(PlatformUserPrincipal principal, String permission) {
    return principal != null && principal.getPermissionCodes() != null
        && principal.getPermissionCodes().contains(permission);
  }

  public static boolean isWriter(PlatformUserPrincipal principal,
      TextbookChapterAssignmentEntity assignment) {
    return assignment != null && Integer.valueOf(1).equals(assignment.getActive())
        && principal != null && assignment.getWriterId() != null
        && assignment.getWriterId().equals(principal.getUserId())
        && has(principal, PERM_TASK_VIEW);
  }

  public static boolean isReviewer(PlatformUserPrincipal principal,
      TextbookChapterAssignmentEntity assignment) {
    return assignment != null && Integer.valueOf(1).equals(assignment.getActive())
        && principal != null && assignment.getReviewerId() != null
        && assignment.getReviewerId().equals(principal.getUserId())
        && has(principal, PERM_REVIEW_VIEW);
  }

  /** 该章有效分配（active=1）；无则 null。 */
  public TextbookChapterAssignmentEntity activeAssignmentOf(Long chapterId) {
    return assignmentMapper.selectOne(new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
        .eq(TextbookChapterAssignmentEntity::getChapterId, chapterId)
        .eq(TextbookChapterAssignmentEntity::getActive, 1)
        .orderByDesc(TextbookChapterAssignmentEntity::getId)
        .last("LIMIT 1"));
  }

  private TextbookProjectEntity findProject(Long projectId) {
    TextbookProjectEntity project = projectMapper.selectOne(
        new LambdaQueryWrapper<TextbookProjectEntity>()
            .eq(TextbookProjectEntity::getId, projectId));
    if (project == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "教材工程不存在");
    }
    return project;
  }

  private TextbookOutlineNodeEntity findChapter(Long chapterId) {
    TextbookOutlineNodeEntity chapter = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, chapterId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_CHAPTER));
    if (chapter == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章不存在");
    }
    return chapter;
  }

  private TextbookOutlineNodeEntity findSection(Long sectionId) {
    TextbookOutlineNodeEntity section = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, sectionId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION));
    if (section == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "节不存在");
    }
    return section;
  }

  /** 批量取用户显示名（Assignment/Task/Review VO 组装；缺行回退空串）。 */
  public Map<Long, String> displayNames(Collection<Long> userIds) {
    List<Long> ids = userIds == null ? List.of()
        : userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> names = new LinkedHashMap<>();
    for (UserEntity user : userMapper.selectBatchIds(ids)) {
      names.put(user.getId(),
          user.getDisplayName() == null || user.getDisplayName().isBlank()
              ? user.getUsername() : user.getDisplayName());
    }
    return names;
  }
}

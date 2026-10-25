package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import org.springframework.stereotype.Service;

/**
 * 教材工程统一行锁（方案 §7.1）：project → assignment → content 固定顺序 FOR UPDATE。
 *
 * <p>所有相关写操作（C3/C4 分配、W3/W4/W5/W9、A1/A3、C5/C6、一期工程 update/status、
 * 结构重建/工程删除）都必须先锁同一个 project 行，锁后重新校验 active、writer/reviewer、
 * 工程状态、内容状态与 baseVersion，消除「授权检查后管理员完成改派，原主笔仍写入」的
 * TOCTOU。多章批量按 chapterId 升序锁分配，避免死锁。
 *
 * <p>锁语义沿用 {@code CompetitionStageLifecycleGuard.lockStage} 的既定范式：
 * wrapper 显式 tenant + {@code .last("FOR UPDATE")}，MyBatis-Plus 租户插件仍会追加
 * tenant 条件（幂等不冲突）。仅 HTTP 请求线程使用（二期无 Worker 调用）。
 */
@Service
public class TextbookProjectLockService {

  private final TextbookProjectMapper projectMapper;
  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookChapterAssignmentMapper assignmentMapper;
  private final TextbookSectionContentMapper contentMapper;

  public TextbookProjectLockService(TextbookProjectMapper projectMapper,
      TextbookOutlineNodeMapper nodeMapper,
      TextbookChapterAssignmentMapper assignmentMapper,
      TextbookSectionContentMapper contentMapper) {
    this.projectMapper = projectMapper;
    this.nodeMapper = nodeMapper;
    this.assignmentMapper = assignmentMapper;
    this.contentMapper = contentMapper;
  }

  /** 工程行锁（本工程全部写操作的粗粒度互斥点）。404：工程不存在/他租户。 */
  public TextbookProjectEntity lockProject(Long projectId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TextbookProjectEntity project = projectMapper.selectOne(
        new LambdaQueryWrapper<TextbookProjectEntity>()
            .eq(TextbookProjectEntity::getTenantId, tenantId)
            .eq(TextbookProjectEntity::getId, projectId)
            .eq(TextbookProjectEntity::getDeleted, 0)
            .last("FOR UPDATE"));
    if (project == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "教材工程不存在");
    }
    return project;
  }

  public TextbookChapterAssignmentEntity lockAssignmentByChapter(Long chapterId) {
    return assignmentMapper.selectOne(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getChapterId, chapterId)
            .last("FOR UPDATE"));
  }

  public TextbookSectionContentEntity lockContentBySection(Long sectionId) {
    return contentMapper.selectOne(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .eq(TextbookSectionContentEntity::getSectionId, sectionId)
            .last("FOR UPDATE"));
  }

  /** 同一事务内按固定顺序锁定 project → assignment → content；行不存在时对应槽位为 null。 */
  public LockedChain lockProjectChapterContent(Long projectId, Long chapterId, Long sectionId) {
    TextbookProjectEntity project = lockProject(projectId);
    TextbookChapterAssignmentEntity assignment =
        chapterId == null ? null : lockAssignmentByChapter(chapterId);
    TextbookSectionContentEntity content =
        sectionId == null ? null : lockContentBySection(sectionId);
    return new LockedChain(project, assignment, content);
  }

  /** 由节节点推导工程/章并按固定顺序加锁（W 组接口主入口）。 */
  public LockedSection lockBySection(Long sectionId) {
    TextbookOutlineNodeEntity section = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, sectionId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION));
    if (section == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "节不存在");
    }
    TextbookOutlineNodeEntity chapter = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, section.getParentId())
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_CHAPTER));
    if (chapter == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章不存在");
    }
    LockedChain chain = lockProjectChapterContent(section.getProjectId(), chapter.getId(),
        sectionId);
    return new LockedSection(chain.project(), chapter, section, chain.assignment(),
        chain.content());
  }

  /** 由章节点推导工程并按固定顺序加锁（C 组按章操作的入口）。 */
  public LockedChapter lockByChapter(Long chapterId) {
    TextbookOutlineNodeEntity chapter = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, chapterId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_CHAPTER));
    if (chapter == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "章不存在");
    }
    LockedChain chain = lockProjectChapterContent(chapter.getProjectId(), chapterId, null);
    return new LockedChapter(chain.project(), chapter, chain.assignment());
  }

  public record LockedChain(TextbookProjectEntity project,
                            TextbookChapterAssignmentEntity assignment,
                            TextbookSectionContentEntity content) {
  }

  public record LockedChapter(TextbookProjectEntity project,
                              TextbookOutlineNodeEntity chapter,
                              TextbookChapterAssignmentEntity assignment) {
  }

  public record LockedSection(TextbookProjectEntity project,
                              TextbookOutlineNodeEntity chapter,
                              TextbookOutlineNodeEntity section,
                              TextbookChapterAssignmentEntity assignment,
                              TextbookSectionContentEntity content) {
  }
}

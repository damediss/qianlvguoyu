package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 章进度唯一重算（方案 §3.3）：正文状态是事实源，assignment 上的 task_status 与 *_count
 * 是列表查询缓存。每次正文保存/提交/审定/恢复、分配/取消后，必须在<b>同一事务</b>内调用
 * {@link #refreshChapterSummary(Long)}，禁止各 Controller 自行拼状态。
 *
 * <p>统计口径：先取章下全部存活节（不是只统计已有正文记录的节）。
 * 计算优先级：sectionCount=0 → assigned 兜底；approvedCount=sectionCount → approved；
 * returnedCount>0 → returned；submitted+approved=sectionCount → reviewing；
 * startedCount>0 → writing；否则 assigned。进度刷新不递增 assignment_version。
 */
@Service
public class TextbookProgressService {

  private static final Logger log = LoggerFactory.getLogger(TextbookProgressService.class);

  public static final String TASK_ASSIGNED = "assigned";
  public static final String TASK_WRITING = "writing";
  public static final String TASK_REVIEWING = "reviewing";
  public static final String TASK_RETURNED = "returned";
  public static final String TASK_APPROVED = "approved";

  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookChapterAssignmentMapper assignmentMapper;

  public TextbookProgressService(TextbookOutlineNodeMapper nodeMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookChapterAssignmentMapper assignmentMapper) {
    this.nodeMapper = nodeMapper;
    this.contentMapper = contentMapper;
    this.assignmentMapper = assignmentMapper;
  }

  /** 同事务重算章任务状态缓存；无分配行时静默跳过（写路径都先保证了分配存在）。 */
  public void refreshChapterSummary(Long chapterId) {
    List<TextbookOutlineNodeEntity> sections = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, chapterId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    Map<Long, TextbookSectionContentEntity> contentBySection = contentsBySection(sections);

    int sectionCount = sections.size();
    int approved = 0;
    int submitted = 0;
    int returned = 0;
    int draft = 0;
    int started = 0;
    int withContent = 0;
    for (TextbookOutlineNodeEntity section : sections) {
      TextbookSectionContentEntity content = contentBySection.get(section.getId());
      if (content == null) {
        continue;
      }
      withContent++;
      String status = content.getContentStatus();
      if (TextbookSectionContentApplicationService.STATUS_APPROVED.equals(status)) {
        approved++;
        started++;
      } else if (TextbookSectionContentApplicationService.STATUS_SUBMITTED.equals(status)) {
        submitted++;
        started++;
      } else if (TextbookSectionContentApplicationService.STATUS_RETURNED.equals(status)) {
        returned++;
        started++;
      } else {
        draft++;
        if (content.getContentMd() != null && !content.getContentMd().isBlank()) {
          started++;
        }
      }
    }
    String taskStatus = computeTaskStatus(sectionCount, approved, submitted, returned, started);

    TextbookChapterAssignmentEntity assignment = assignmentMapper.selectOne(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getChapterId, chapterId));
    if (assignment == null) {
      log.debug("[教材] 章无分配行，跳过进度刷新：chapterId={}", chapterId);
      return;
    }
    // 只更新缓存列，不触碰 assignment_version / active / 人员 / 截止（方案 §5.1）
    assignmentMapper.update(null, new LambdaUpdateWrapper<TextbookChapterAssignmentEntity>()
        .eq(TextbookChapterAssignmentEntity::getId, assignment.getId())
        .set(TextbookChapterAssignmentEntity::getTaskStatus, taskStatus)
        .set(TextbookChapterAssignmentEntity::getSectionCount, sectionCount)
        .set(TextbookChapterAssignmentEntity::getApprovedCount, approved)
        .set(TextbookChapterAssignmentEntity::getSubmittedCount, submitted)
        .set(TextbookChapterAssignmentEntity::getReturnedCount, returned)
        .set(TextbookChapterAssignmentEntity::getDraftCount, draft));
  }

  /** §3.3 计算优先级（纯函数，供单测直接覆盖）。 */
  static String computeTaskStatus(int sectionCount, int approvedCount, int submittedCount,
      int returnedCount, int startedCount) {
    if (sectionCount <= 0) {
      return TASK_ASSIGNED;
    }
    if (approvedCount == sectionCount) {
      return TASK_APPROVED;
    }
    if (returnedCount > 0) {
      return TASK_RETURNED;
    }
    if (submittedCount + approvedCount == sectionCount) {
      return TASK_REVIEWING;
    }
    if (startedCount > 0) {
      return TASK_WRITING;
    }
    return TASK_ASSIGNED;
  }

  /**
   * untouched = sectionCount - 已有正文记录节数（方案 §6.1 AssignmentVO.untouchedCount）。
   */
  public int untouchedCount(Long chapterId) {
    List<TextbookOutlineNodeEntity> sections = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, chapterId)
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION));
    if (sections.isEmpty()) {
      return 0;
    }
    Map<Long, TextbookSectionContentEntity> contentBySection = contentsBySection(sections);
    int withContent = 0;
    for (TextbookOutlineNodeEntity section : sections) {
      if (contentBySection.containsKey(section.getId())) {
        withContent++;
      }
    }
    return sections.size() - withContent;
  }

  private Map<Long, TextbookSectionContentEntity> contentsBySection(
      List<TextbookOutlineNodeEntity> sections) {
    if (sections.isEmpty()) {
      return Map.of();
    }
    List<Long> sectionIds = new ArrayList<>(sections.size());
    for (TextbookOutlineNodeEntity section : sections) {
      sectionIds.add(section.getId());
    }
    Map<Long, TextbookSectionContentEntity> bySection = new HashMap<>();
    for (TextbookSectionContentEntity content : contentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .in(TextbookSectionContentEntity::getSectionId, sectionIds))) {
      bySection.put(content.getSectionId(), content);
    }
    return bySection;
  }
}

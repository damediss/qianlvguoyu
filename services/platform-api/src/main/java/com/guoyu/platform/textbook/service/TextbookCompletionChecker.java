package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.Blocker;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.Completion;
import com.guoyu.platform.textbook.domain.TextbookMarkdownSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 完成条件/阻塞项唯一判定（方案 §3.1-3/§6.1）：C2 展示与 C5 校验共用。
 *
 * <p>完成必须同时满足：chapterCount&gt;0、sectionCount&gt;0、每章至少 1 节、每章有有效分配、
 * 全部节 approved、无 [待专家补充] 占位。禁止用「空集合全称条件」把零章/零节工程判为完成
 * （先判 NO_CHAPTER/NO_SECTION）。C5 在工程行锁内重新查询，不只用缓存。
 */
@Component
public class TextbookCompletionChecker {

  public static final String CODE_NO_CHAPTER = "NO_CHAPTER";
  public static final String CODE_NO_SECTION = "NO_SECTION";
  public static final String CODE_CHAPTER_UNASSIGNED = "CHAPTER_UNASSIGNED";
  public static final String CODE_EMPTY_CHAPTER = "EMPTY_CHAPTER";
  public static final String CODE_SECTION_NOT_APPROVED = "SECTION_NOT_APPROVED";
  public static final String CODE_PLACEHOLDER_PRESENT = "PLACEHOLDER_PRESENT";

  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookChapterAssignmentMapper assignmentMapper;

  public TextbookCompletionChecker(TextbookOutlineNodeMapper nodeMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookChapterAssignmentMapper assignmentMapper) {
    this.nodeMapper = nodeMapper;
    this.contentMapper = contentMapper;
    this.assignmentMapper = assignmentMapper;
  }

  /** 重新查询全部章/节/分配/占位并产出阻塞项（须在工程行锁内调用，方案 §7.1-5）。 */
  public Completion check(TextbookProjectEntity project) {
    List<Blocker> blockers = new ArrayList<>();
    List<TextbookOutlineNodeEntity> chapters = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, project.getId())
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_CHAPTER)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    if (chapters.isEmpty()) {
      blockers.add(new Blocker(CODE_NO_CHAPTER, null, null, "工程没有任何章，不能完成"));
      return new Completion(false, blockers);
    }
    List<TextbookOutlineNodeEntity> sections = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, project.getId())
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION));
    if (sections.isEmpty()) {
      blockers.add(new Blocker(CODE_NO_SECTION, null, null, "工程没有任何节，不能完成"));
      return new Completion(false, blockers);
    }

    Map<Long, List<TextbookOutlineNodeEntity>> sectionsByChapter = new HashMap<>();
    for (TextbookOutlineNodeEntity section : sections) {
      sectionsByChapter.computeIfAbsent(section.getParentId(), k -> new ArrayList<>()).add(section);
    }
    Set<Long> activeChapters = new HashSet<>();
    for (TextbookChapterAssignmentEntity assignment : assignmentMapper.selectList(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getProjectId, project.getId())
            .eq(TextbookChapterAssignmentEntity::getActive, 1))) {
      activeChapters.add(assignment.getChapterId());
    }
    List<Long> sectionIds = sections.stream().map(TextbookOutlineNodeEntity::getId).toList();
    Map<Long, TextbookSectionContentEntity> contentBySection = new HashMap<>();
    if (!sectionIds.isEmpty()) {
      for (TextbookSectionContentEntity content : contentMapper.selectList(
          new LambdaQueryWrapper<TextbookSectionContentEntity>()
              .in(TextbookSectionContentEntity::getSectionId, sectionIds))) {
        contentBySection.put(content.getSectionId(), content);
      }
    }

    for (TextbookOutlineNodeEntity chapter : chapters) {
      String chapterId = String.valueOf(chapter.getId());
      if (!activeChapters.contains(chapter.getId())) {
        blockers.add(new Blocker(CODE_CHAPTER_UNASSIGNED, chapterId, null,
            "章「" + chapter.getTitle() + "」尚无有效分配"));
        continue;
      }
      List<TextbookOutlineNodeEntity> chapterSections =
          sectionsByChapter.getOrDefault(chapter.getId(), List.of());
      if (chapterSections.isEmpty()) {
        blockers.add(new Blocker(CODE_EMPTY_CHAPTER, chapterId, null,
            "章「" + chapter.getTitle() + "」下没有任何节"));
        continue;
      }
      for (TextbookOutlineNodeEntity section : chapterSections) {
        String sectionId = String.valueOf(section.getId());
        TextbookSectionContentEntity content = contentBySection.get(section.getId());
        if (content == null
            || !TextbookSectionContentApplicationService.STATUS_APPROVED.equals(
                content.getContentStatus())) {
          blockers.add(new Blocker(CODE_SECTION_NOT_APPROVED, chapterId, sectionId,
              "「" + section.getTitle() + "」尚未通过"));
        } else if (content.getContentMd() != null
            && content.getContentMd().contains(TextbookMarkdownSupport.PLACEHOLDER)) {
          blockers.add(new Blocker(CODE_PLACEHOLDER_PRESENT, chapterId, sectionId,
              "「" + section.getTitle() + "」仍含 [待专家补充] 占位"));
        }
      }
    }
    return new Completion(blockers.isEmpty(), blockers);
  }
}

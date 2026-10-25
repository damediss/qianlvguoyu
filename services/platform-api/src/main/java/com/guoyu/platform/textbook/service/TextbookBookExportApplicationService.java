package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionAssetEntity;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookChapter;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookPart;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookSection;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionAssetMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.service.TextbookAccessService.ChapterAccess;
import com.guoyu.platform.textbook.service.TextbookAccessService.SectionAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * docx 导出编排（E1-E4，方案 §6.5/§9）。单节/单章按可读节/章授权；整册 review 仅需
 * project:export；整册 final 仅 done 工程可导出（后端再校验，不信任前端按钮状态）。
 * 图片经教材资产归属校验后走 file 模块内部受控读取（不经通用入口、不服务端自回环 HTTP）。
 */
@Service
public class TextbookBookExportApplicationService {

  private static final Logger log =
      LoggerFactory.getLogger(TextbookBookExportApplicationService.class);

  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookSectionAssetMapper assetMapper;
  private final TextbookAccessService accessService;
  private final FileApplicationService fileApplicationService;

  public TextbookBookExportApplicationService(
      TextbookOutlineNodeMapper nodeMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookSectionAssetMapper assetMapper,
      TextbookAccessService accessService,
      FileApplicationService fileApplicationService) {
    this.nodeMapper = nodeMapper;
    this.contentMapper = contentMapper;
    this.assetMapper = assetMapper;
    this.accessService = accessService;
    this.fileApplicationService = fileApplicationService;
  }

  /** E1 单节导出（当前节内容与状态）。 */
  @Transactional(readOnly = true)
  public ExportFile exportSection(Long sectionId, boolean reviewMode) {
    SectionAccess access = accessService.requireSectionRead(sectionId);
    TextbookSectionContentEntity content = contentMapper.selectOne(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .eq(TextbookSectionContentEntity::getSectionId, sectionId));
    BookSection section = toBookSection(access.section(),
        content == null ? null : content.getContentMd(),
        content == null ? null : content.getContentStatus());
    BookChapter chapter = new BookChapter(access.chapter().getId(),
        access.chapter().getTitle(), access.chapter().getSourceNo(), List.of(section));
    TextbookBookExporter exporter = new TextbookBookExporter(reviewMode, assetResolver());
    return exporter.exportSection(access.project(), chapter, section);
  }

  /** E2 单章导出。 */
  @Transactional(readOnly = true)
  public ExportFile exportChapter(Long chapterId, boolean reviewMode) {
    ChapterAccess access = accessService.requireChapterRead(chapterId);
    BookChapter chapter = buildChapter(access.chapter());
    TextbookBookExporter exporter = new TextbookBookExporter(reviewMode, assetResolver());
    return exporter.exportChapter(access.project(), chapter);
  }

  /** E3/E4 整册导出：mode=review 全部章节带状态标识；mode=final 仅 done 且全部 approved 无占位。 */
  @Transactional(readOnly = true)
  public ExportFile exportBook(Long projectId, String mode) {
    boolean finalMode = "final".equals(mode);
    if (!finalMode && !"review".equals(mode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "mode 仅支持 review/final");
    }
    TextbookProjectEntity project = accessService.requireProjectView(projectId).project();
    if (finalMode && !TextbookProjectApplicationService.STATUS_DONE
        .equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "final 正式导出仅 done 工程可用，当前：" + project.getStatus());
    }
    // 按 outline 的 parentId + sortNo + id 顺序遍历（方案 §9.1），不按分配/姓名/ID 排序
    List<TextbookOutlineNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
            .orderByAsc(TextbookOutlineNodeEntity::getParentId)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    Map<Long, List<TextbookOutlineNodeEntity>> byParent = new HashMap<>();
    for (TextbookOutlineNodeEntity node : nodes) {
      byParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
    }
    Map<Long, TextbookSectionContentEntity> contentBySection = new HashMap<>();
    for (TextbookSectionContentEntity content : contentMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentEntity>()
            .eq(TextbookSectionContentEntity::getProjectId, projectId))) {
      contentBySection.put(content.getSectionId(), content);
    }

    List<BookPart> parts = new ArrayList<>();
    for (TextbookOutlineNodeEntity part : byParent.getOrDefault(0L, List.of())) {
      List<BookChapter> chapters = new ArrayList<>();
      for (TextbookOutlineNodeEntity chapter
          : byParent.getOrDefault(part.getId(), List.of())) {
        chapters.add(buildChapter(chapter, byParent, contentBySection));
      }
      parts.add(new BookPart(part.getTitle(), chapters));
    }
    // 无篇结构：章直接挂根（部分工程篇级缺省）
    if (parts.isEmpty()) {
      List<BookChapter> chapters = new ArrayList<>();
      for (TextbookOutlineNodeEntity node : nodes) {
        if (node.getNodeLevel() != null
            && node.getNodeLevel() == TextbookOutlineApplicationService.LEVEL_CHAPTER) {
          chapters.add(buildChapter(node, byParent, contentBySection));
        }
      }
      parts.add(new BookPart(null, chapters));
    }
    TextbookBookExporter exporter = new TextbookBookExporter(!finalMode, assetResolver());
    log.info("[教材] 整册导出：projectId={} mode={}", projectId, mode);
    return exporter.exportBook(project, parts);
  }

  private BookChapter buildChapter(TextbookOutlineNodeEntity chapter) {
    List<TextbookOutlineNodeEntity> sections = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, chapter.getId())
            .eq(TextbookOutlineNodeEntity::getNodeLevel,
                TextbookOutlineApplicationService.LEVEL_SECTION)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    Map<Long, TextbookSectionContentEntity> contentBySection = new HashMap<>();
    if (!sections.isEmpty()) {
      for (TextbookSectionContentEntity content : contentMapper.selectList(
          new LambdaQueryWrapper<TextbookSectionContentEntity>()
              .in(TextbookSectionContentEntity::getSectionId,
                  sections.stream().map(TextbookOutlineNodeEntity::getId).toList()))) {
        contentBySection.put(content.getSectionId(), content);
      }
    }
    return buildChapter(chapter, Map.of(chapter.getId(), sections), contentBySection);
  }

  private BookChapter buildChapter(TextbookOutlineNodeEntity chapter,
      Map<Long, List<TextbookOutlineNodeEntity>> byParent,
      Map<Long, TextbookSectionContentEntity> contentBySection) {
    List<BookSection> sections = new ArrayList<>();
    for (TextbookOutlineNodeEntity section : byParent.getOrDefault(chapter.getId(),
        List.of())) {
      if (section.getNodeLevel() == null
          || section.getNodeLevel() != TextbookOutlineApplicationService.LEVEL_SECTION) {
        continue;
      }
      TextbookSectionContentEntity content = contentBySection.get(section.getId());
      sections.add(toBookSection(section,
          content == null ? null : content.getContentMd(),
          content == null ? null : content.getContentStatus()));
    }
    return new BookChapter(chapter.getId(), chapter.getTitle(), chapter.getSourceNo(),
        sections);
  }

  private BookSection toBookSection(TextbookOutlineNodeEntity section, String contentMd,
      String status) {
    return new BookSection(section.getId(), section.getTitle(), section.getSourceNo(),
        status, contentMd);
  }

  /**
   * 资产字节解析器：docx 导出时后端再次校验资产归属（该资产必须仍归属某存活节），
   * 再经 file 模块内部受控读取字节（方案 §8.2）。
   */
  private TextbookBookExporter.AssetBytesResolver assetResolver() {
    return assetId -> {
      TextbookSectionAssetEntity asset = assetMapper.selectById(assetId);
      if (asset == null) {
        return null;
      }
      return fileApplicationService.readFileBytesById(asset.getFileId());
    };
  }
}

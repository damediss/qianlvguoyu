package com.guoyu.platform.trainingdepartment.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialInstanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialTemplateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialVersionEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialInstanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialTemplateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 为填数/在线编辑挑选「带边框」的 Excel 底稿。
 *
 * <p>优先：原始 .xls 上传 → 模板库文件 → 边框最多的历史版本；避免误用被 SheetJS 另存后丢边框的 xlsx。
 */
@Component
@Slf4j
public class SubsidyMaterialStyleBaseResolver {

  public record StyleBase(
      Long fileId, String fileName, byte[] bytes, String label, int borderCount) {}

  private final TrainSubsidyMaterialVersionMapper versionMapper;
  private final TrainSubsidyMaterialInstanceMapper instanceMapper;
  private final TrainSubsidyMaterialTemplateMapper templateMapper;
  private final FileApplicationService fileApplicationService;

  public SubsidyMaterialStyleBaseResolver(
      TrainSubsidyMaterialVersionMapper versionMapper,
      TrainSubsidyMaterialInstanceMapper instanceMapper,
      TrainSubsidyMaterialTemplateMapper templateMapper,
      FileApplicationService fileApplicationService) {
    this.versionMapper = versionMapper;
    this.instanceMapper = instanceMapper;
    this.templateMapper = templateMapper;
    this.fileApplicationService = fileApplicationService;
  }

  public StyleBase resolveForInstance(Long instanceId) {
    TrainSubsidyMaterialInstanceEntity instance = instanceMapper.selectById(instanceId);
    if (instance == null) {
      return null;
    }
    List<Candidate> candidates = new ArrayList<>();

    // 1) 上传的 .xls（通常带完整边框）
    List<TrainSubsidyMaterialVersionEntity> uploads =
        versionMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
                .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
                .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
                .eq(TrainSubsidyMaterialVersionEntity::getSource, "upload")
                .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo));
    for (TrainSubsidyMaterialVersionEntity v : uploads) {
      if (isXlsName(v.getFileName())) {
        addCandidate(candidates, v.getFileId(), v.getFileName(), "upload-xls#" + v.getId());
      }
    }

    // 2) 模板库原文件
    if (instance.getTemplateId() != null) {
      TrainSubsidyMaterialTemplateEntity tpl = templateMapper.selectById(instance.getTemplateId());
      if (tpl != null && tpl.getTemplateFileId() != null) {
        String name =
            StringUtils.hasText(tpl.getTitle()) ? tpl.getTitle() + ".xls" : "library-template.xls";
        addCandidate(candidates, tpl.getTemplateFileId(), name, "library#" + tpl.getId());
      }
    }

    // 3) 其它上传 / 填数产物（按边框数打分）
    for (TrainSubsidyMaterialVersionEntity v : uploads) {
      if (!isXlsName(v.getFileName())) {
        addCandidate(candidates, v.getFileId(), v.getFileName(), "upload#" + v.getId());
      }
    }
    List<TrainSubsidyMaterialVersionEntity> fills =
        versionMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
                .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
                .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
                .in(
                    TrainSubsidyMaterialVersionEntity::getSource,
                    "auto_fill",
                    "auto_fill_current",
                    "class_sync")
                .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
                .last("LIMIT 8"));
    for (TrainSubsidyMaterialVersionEntity v : fills) {
      addCandidate(candidates, v.getFileId(), v.getFileName(), v.getSource() + "#" + v.getId());
    }

    // 4) 当前版本兜底
    if (instance.getCurrentVersionId() != null) {
      TrainSubsidyMaterialVersionEntity cur = versionMapper.selectById(instance.getCurrentVersionId());
      if (cur != null) {
        addCandidate(candidates, cur.getFileId(), cur.getFileName(), "current#" + cur.getId());
      }
    }

    Candidate best = null;
    for (Candidate c : candidates) {
      if (best == null
          || c.borderCount > best.borderCount
          || (c.borderCount == best.borderCount && c.xlsBonus > best.xlsBonus)) {
        best = c;
      }
    }
    if (best == null || best.bytes == null) {
      return null;
    }
    log.info(
        "style base picked instanceId={} label={} borders={} fileId={}",
        instanceId,
        best.label,
        best.borderCount,
        best.fileId);
    return new StyleBase(best.fileId, best.fileName, best.bytes, best.label, best.borderCount);
  }

  private void addCandidate(List<Candidate> out, Long fileId, String fileName, String label) {
    if (fileId == null) {
      return;
    }
    for (Candidate c : out) {
      if (fileId.equals(c.fileId)) {
        return;
      }
    }
    try {
      byte[] bytes = fileApplicationService.readFileBytesById(fileId);
      int borders = SubsidyExcelStyleSupport.countBorderedCells(bytes);
      int xlsBonus = "xls".equals(SubsidyExcelStyleSupport.sniffFormat(bytes)) ? 1 : 0;
      out.add(new Candidate(fileId, fileName, bytes, label, borders, xlsBonus));
    } catch (Exception ex) {
      log.warn("style base candidate skip fileId={} label={}: {}", fileId, label, ex.getMessage());
    }
  }

  private static boolean isXlsName(String fileName) {
    if (!StringUtils.hasText(fileName)) {
      return false;
    }
    String n = fileName.toLowerCase(Locale.ROOT);
    return n.endsWith(".xls") && !n.endsWith(".xlsx");
  }

  private static final class Candidate {
    final Long fileId;
    final String fileName;
    final byte[] bytes;
    final String label;
    final int borderCount;
    final int xlsBonus;

    Candidate(
        Long fileId,
        String fileName,
        byte[] bytes,
        String label,
        int borderCount,
        int xlsBonus) {
      this.fileId = fileId;
      this.fileName = fileName;
      this.bytes = bytes;
      this.label = label;
      this.borderCount = borderCount;
      this.xlsBonus = xlsBonus;
    }
  }
}

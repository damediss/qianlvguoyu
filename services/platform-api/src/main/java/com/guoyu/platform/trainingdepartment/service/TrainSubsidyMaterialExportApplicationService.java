package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialInstanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialVersionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.support.SubsidyMaterialTypes;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSubsidyMaterialExportApplicationService {

  private final TrainSubsidyMapper subsidyMapper;
  private final TrainSubsidyMaterialVersionMapper versionMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final TrainSubsidyMaterialInstanceApplicationService instanceService;
  private final FileApplicationService fileApplicationService;

  public TrainSubsidyMaterialExportApplicationService(
      TrainSubsidyMapper subsidyMapper,
      TrainSubsidyMaterialVersionMapper versionMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainSubsidyEventMapper eventMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      TrainSubsidyMaterialInstanceApplicationService instanceService,
      FileApplicationService fileApplicationService) {
    this.subsidyMapper = subsidyMapper;
    this.versionMapper = versionMapper;
    this.slotFileMapper = slotFileMapper;
    this.eventMapper = eventMapper;
    this.photoPackMapper = photoPackMapper;
    this.instanceService = instanceService;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional
  public Map<String, Object> exportSelected(Long subsidyId, List<Long> instanceIds) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    List<TrainSubsidyMaterialInstanceEntity> instances = instanceService.listInstancesForExport(subsidyId, instanceIds);
    instances.sort(Comparator
        .comparingInt((TrainSubsidyMaterialInstanceEntity i) -> SubsidyMaterialTypes.materialTypeOrder(i.getMaterialType()))
        .thenComparing(i -> i.getSortOrder() != null ? i.getSortOrder() : 0));

    TrainFollowPhotoPackEntity photoPack = latestPhotoPack(subsidy);
    Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot = loadSlotFiles(subsidyId).stream()
        .collect(java.util.stream.Collectors.groupingBy(TrainSubsidySlotFileEntity::getSlotCode));

    List<String> exported = new ArrayList<>();
    List<String> skipped = new ArrayList<>();

    try {
      byte[] zipBytes = buildZip(subsidy, instances, filesBySlot, photoPack, exported, skipped);
      String fileName = "subsidy-" + subsidy.getId() + "-export-"
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";
      FileVO stored = fileApplicationService.storeFromBytes(zipBytes, fileName, "application/zip", "private");
      String path = "/api/files/" + stored.id() + "/download";
      subsidy.setMaterialPackagePath(path);
      subsidy.setMaterialStatus(1);
      subsidyMapper.updateById(subsidy);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("ok", true);
      result.put("fileId", stored.id());
      result.put("filePath", path);
      // private 对象不可用 COS 裸链（浏览器直链 403），统一返回鉴权下载路径
      result.put("url", path);
      result.put("exported", exported);
      result.put("skipped", skipped);
      result.put("message", "已导出 " + exported.size() + " 项材料");
      addEvent(subsidy, "export_selected", "勾选导出材料包", result);
      return result;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("补贴勾选导出失败 subsidyId={}", subsidyId, ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + ex.getMessage());
    }
  }

  private byte[] buildZip(
      TrainSubsidyEntity subsidy,
      List<TrainSubsidyMaterialInstanceEntity> instances,
      Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot,
      TrainFollowPhotoPackEntity photoPack,
      List<String> exported,
      List<String> skipped) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(bos)) {
      StringBuilder readme = new StringBuilder();
      readme.append("补贴材料包（勾选导出）\n")
          .append("subsidyId=").append(subsidy.getId()).append('\n')
          .append("classId=").append(subsidy.getClassId()).append('\n')
          .append("exportedAt=").append(LocalDateTime.now()).append('\n')
          .append("items=").append(instances.size()).append('\n');
      zos.putNextEntry(new ZipEntry("README.txt"));
      zos.write(readme.toString().getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      for (TrainSubsidyMaterialInstanceEntity instance : instances) {
        String folder = SubsidyMaterialTypes.exportFolder(instance.getMaterialType());
        if ("follow_photo".equals(instance.getRenderer())) {
          if (photoPack != null && photoPack.getFileId() != null) {
            byte[] bytes = fileApplicationService.readFileBytesById(photoPack.getFileId());
            String name = folder + (StringUtils.hasText(photoPack.getTitle()) ? photoPack.getTitle() : "培训照片") + ".docx";
            putEntry(zos, name, bytes);
            exported.add(instance.getTitle() + " -> " + name);
          } else if (writeCurrentVersion(zos, folder, instance, exported)) {
            // ok
          } else {
            skipped.add(instance.getTitle() + "(无照片文件)");
          }
          continue;
        }

        List<TrainSubsidySlotFileEntity> slotFiles = StringUtils.hasText(instance.getSlotCode())
            ? filesBySlot.getOrDefault(instance.getSlotCode(), List.of())
            : List.of();
        if (!slotFiles.isEmpty()) {
          int i = 1;
          for (TrainSubsidySlotFileEntity f : slotFiles) {
            if (f.getFileId() == null) continue;
            byte[] bytes = fileApplicationService.readFileBytesById(f.getFileId());
            String fname = StringUtils.hasText(f.getFileName()) ? f.getFileName() : ("file-" + i);
            String entry = folder + fname;
            putEntry(zos, entry, bytes);
            exported.add(instance.getTitle() + " -> " + entry);
            i++;
          }
          if (slotFiles.isEmpty()) {
            skipped.add(instance.getTitle());
          }
          continue;
        }

        if (!writeCurrentVersion(zos, folder, instance, exported)) {
          skipped.add(instance.getTitle() + "(无文件)");
        }
      }
    }
    return bos.toByteArray();
  }

  private boolean writeCurrentVersion(
      ZipOutputStream zos, String folder, TrainSubsidyMaterialInstanceEntity instance, List<String> exported)
      throws Exception {
    if (instance.getCurrentVersionId() == null) {
      return false;
    }
    TrainSubsidyMaterialVersionEntity version = versionMapper.selectById(instance.getCurrentVersionId());
    if (version == null || version.getFileId() == null) {
      return false;
    }
    byte[] bytes = fileApplicationService.readFileBytesById(version.getFileId());
    String fname = StringUtils.hasText(version.getFileName()) ? version.getFileName() : instance.getTitle();
    String entry = folder + fname;
    putEntry(zos, entry, bytes);
    exported.add(instance.getTitle() + " -> " + entry);
    return true;
  }

  private void putEntry(ZipOutputStream zos, String name, byte[] bytes) throws Exception {
    zos.putNextEntry(new ZipEntry(name));
    zos.write(bytes);
    zos.closeEntry();
  }

  private List<TrainSubsidySlotFileEntity> loadSlotFiles(Long subsidyId) {
    return slotFileMapper.selectList(new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
        .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidyId)
        .eq(TrainSubsidySlotFileEntity::getDeleted, 0)
        .orderByAsc(TrainSubsidySlotFileEntity::getId));
  }

  private TrainFollowPhotoPackEntity latestPhotoPack(TrainSubsidyEntity subsidy) {
    if (subsidy.getClassId() == null) {
      return null;
    }
    return photoPackMapper.selectOne(new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
        .eq(TrainFollowPhotoPackEntity::getClassId, subsidy.getClassId())
        .eq(TrainFollowPhotoPackEntity::getTenantId, subsidy.getTenantId())
        .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
        .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
        .last("LIMIT 1"));
  }

  private TrainSubsidyEntity requireSubsidy(Long subsidyId) {
    TrainSubsidyEntity entity = subsidyMapper.selectById(subsidyId);
    if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "补贴单不存在");
    }
    return entity;
  }

  private void addEvent(TrainSubsidyEntity subsidy, String action, String remark, Map<String, Object> payload) {
    TrainSubsidyEventEntity event = new TrainSubsidyEventEntity();
    event.setSubsidyId(subsidy.getId());
    event.setAction(action);
    event.setFromPhase(subsidy.getPhase());
    event.setToPhase(subsidy.getPhase());
    event.setRemark(remark);
    try {
      event.setPayloadJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
    } catch (Exception ignored) {
      event.setPayloadJson(null);
    }
    event.setOperatorId(SecurityContext.currentUser().getUserId());
    event.setTenantId(subsidy.getTenantId());
    event.setDeleted(0);
    eventMapper.insert(event);
  }
}

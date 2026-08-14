package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackSlotEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * P2 试点：按锁定配方槽位打包 ZIP；必填缺项则拒绝出包。
 */
@Service
@Slf4j
public class TrainSubsidyMaterialPackService {

  private final TrainSubsidyPackProfileApplicationService profileService;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TrainSubsidyMaterialPackService(
      TrainSubsidyPackProfileApplicationService profileService,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      FileApplicationService fileApplicationService) {
    this.profileService = profileService;
    this.slotFileMapper = slotFileMapper;
    this.photoPackMapper = photoPackMapper;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional
  public Map<String, Object> generate(TrainSubsidyEntity subsidy) {
    if (subsidy.getProfileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定配方");
    }
    List<TrainSubsidyPackSlotEntity> slots = profileService.listSlots(subsidy.getProfileId());
    if (slots.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配方无槽位");
    }

    Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot = new LinkedHashMap<>();
    slotFileMapper.selectList(new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidy.getId())
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0))
        .forEach(f -> filesBySlot.computeIfAbsent(f.getSlotCode(), k -> new ArrayList<>()).add(f));

    TrainFollowPhotoPackEntity followPack = null;
    if (subsidy.getClassId() != null) {
      followPack = photoPackMapper.selectOne(new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
          .eq(TrainFollowPhotoPackEntity::getClassId, subsidy.getClassId())
          .eq(TrainFollowPhotoPackEntity::getTenantId, subsidy.getTenantId())
          .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
          .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
          .last("LIMIT 1"));
    }

    List<Map<String, Object>> checklist = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    for (TrainSubsidyPackSlotEntity slot : slots) {
      boolean required = slot.getRequiredFlag() != null && slot.getRequiredFlag() == 1;
      boolean ok = false;
      String source = null;
      if ("follow_photo".equals(slot.getRenderer())) {
        if (followPack != null && followPack.getFileId() != null) {
          ok = true;
          source = "follow_photo_pack:" + followPack.getId();
        } else if (filesBySlot.containsKey(slot.getSlotCode()) && !filesBySlot.get(slot.getSlotCode()).isEmpty()) {
          ok = true;
          source = "manual_upload";
        }
      } else {
        ok = filesBySlot.containsKey(slot.getSlotCode()) && !filesBySlot.get(slot.getSlotCode()).isEmpty();
        source = ok ? "manual_upload" : null;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("slotCode", slot.getSlotCode());
      item.put("title", slot.getTitle());
      item.put("required", required);
      item.put("ok", ok);
      item.put("source", source);
      item.put("renderer", slot.getRenderer());
      checklist.add(item);
      if (required && !ok) {
        missing.add(slot.getTitle() + "(" + slot.getSlotCode() + ")");
      }
    }

    try {
      subsidy.setChecklistJson(objectMapper.writeValueAsString(checklist));
    } catch (Exception e) {
      subsidy.setChecklistJson(null);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("checklist", checklist);
    result.put("missing", missing);
    if (!missing.isEmpty()) {
      result.put("ok", false);
      result.put("message", "必填材料缺项：" + String.join("、", missing));
      subsidy.setMaterialStatus(0);
      return result;
    }

    try {
      byte[] zipBytes = buildZip(subsidy, slots, filesBySlot, followPack);
      String fileName = "subsidy-" + subsidy.getId() + "-"
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";
      FileVO stored = fileApplicationService.storeFromBytes(
          zipBytes, fileName, "application/zip", "private");
      String path = "/api/files/" + stored.id() + "/download";
      subsidy.setMaterialPackagePath(path);
      subsidy.setMaterialStatus(1);
      subsidy.setProfileLocked(1);
      result.put("ok", true);
      result.put("fileId", stored.id());
      result.put("filePath", path);
      result.put("url", stored.url());
      result.put("message", "材料包已生成");
      return result;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("补贴材料打包失败 subsidyId={}", subsidy.getId(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "材料打包失败: " + ex.getMessage());
    }
  }

  private byte[] buildZip(
      TrainSubsidyEntity subsidy,
      List<TrainSubsidyPackSlotEntity> slots,
      Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot,
      TrainFollowPhotoPackEntity followPack) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(bos)) {
      StringBuilder readme = new StringBuilder();
      readme.append("补贴材料包\n")
          .append("subsidyId=").append(subsidy.getId()).append('\n')
          .append("region=").append(subsidy.getRegionName()).append('\n')
          .append("trainMode=").append(subsidy.getTrainMode()).append('\n')
          .append("policyYear=").append(subsidy.getPolicyYear()).append('\n')
          .append("profileId=").append(subsidy.getProfileId())
          .append(" @").append(subsidy.getProfileVersion()).append('\n')
          .append("generatedAt=").append(LocalDateTime.now()).append('\n');
      zos.putNextEntry(new ZipEntry("README.txt"));
      zos.write(readme.toString().getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      for (TrainSubsidyPackSlotEntity slot : slots) {
        String folder = StringUtils.hasText(slot.getFolderPath())
            ? (slot.getFolderPath().endsWith("/") ? slot.getFolderPath() : slot.getFolderPath() + "/")
            : (slot.getSlotCode() + "/");
        if ("follow_photo".equals(slot.getRenderer()) && followPack != null && followPack.getFileId() != null) {
          byte[] bytes = fileApplicationService.readFileBytesById(followPack.getFileId());
          String name = folder + "培训照片.docx";
          zos.putNextEntry(new ZipEntry(name));
          zos.write(bytes);
          zos.closeEntry();
          continue;
        }
        List<TrainSubsidySlotFileEntity> files = filesBySlot.getOrDefault(slot.getSlotCode(), List.of());
        int i = 1;
        for (TrainSubsidySlotFileEntity f : files) {
          if (f.getFileId() == null) continue;
          byte[] bytes = fileApplicationService.readFileBytesById(f.getFileId());
          String fname = StringUtils.hasText(f.getFileName()) ? f.getFileName() : ("file-" + i);
          zos.putNextEntry(new ZipEntry(folder + fname));
          zos.write(bytes);
          zos.closeEntry();
          i++;
        }
      }
    }
    return bos.toByteArray();
  }
}

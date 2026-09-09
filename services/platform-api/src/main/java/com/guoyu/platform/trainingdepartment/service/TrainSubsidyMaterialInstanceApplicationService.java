package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialInstanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialVersionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackSlotEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialInstanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.support.SubsidyMaterialTypes;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialGroupVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialInstanceListVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialInstanceVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialSlotFileVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainSubsidyMaterialInstanceApplicationService {

  private final TrainSubsidyMapper subsidyMapper;
  private final TrainSubsidyMaterialInstanceMapper instanceMapper;
  private final TrainSubsidyMaterialVersionMapper versionMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final TrainSubsidyPackProfileApplicationService profileService;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final FileApplicationService fileApplicationService;
  private final TrainSubsidyClassMaterialService classMaterialService;
  private final com.guoyu.platform.trainingdepartment.support.SubsidyMaterialStyleBaseResolver styleBaseResolver;

  public TrainSubsidyMaterialInstanceApplicationService(
      TrainSubsidyMapper subsidyMapper,
      TrainSubsidyMaterialInstanceMapper instanceMapper,
      TrainSubsidyMaterialVersionMapper versionMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainSubsidyEventMapper eventMapper,
      TrainSubsidyPackProfileApplicationService profileService,
      TrainFollowPhotoPackMapper photoPackMapper,
      FileApplicationService fileApplicationService,
      @org.springframework.context.annotation.Lazy TrainSubsidyClassMaterialService classMaterialService,
      com.guoyu.platform.trainingdepartment.support.SubsidyMaterialStyleBaseResolver styleBaseResolver) {
    this.subsidyMapper = subsidyMapper;
    this.instanceMapper = instanceMapper;
    this.versionMapper = versionMapper;
    this.slotFileMapper = slotFileMapper;
    this.eventMapper = eventMapper;
    this.profileService = profileService;
    this.photoPackMapper = photoPackMapper;
    this.fileApplicationService = fileApplicationService;
    this.classMaterialService = classMaterialService;
    this.styleBaseResolver = styleBaseResolver;
  }

  @Transactional
  public SubsidyMaterialInstanceListVO listInstances(Long subsidyId) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    pruneNonDefaultEmptyInstances(subsidy);
    ensureInstancesFromProfile(subsidy);
    tryAutoSyncUpstream(subsidy);
    refreshInstancesFromSlotFiles(subsidy);
    return buildListVO(subsidy);
  }

  @Transactional
  public SubsidyMaterialInstanceVO replaceFile(Long instanceId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择文件");
    }
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    assertEditable(subsidy);

    FileVO stored = fileApplicationService.upload(file, "private");
    String path = "/api/files/" + stored.id() + "/download";
    String fileName = StringUtils.hasText(stored.originalName()) ? stored.originalName() : file.getOriginalFilename();

    TrainSubsidyMaterialVersionEntity version = newVersion(instance, stored.id(), path, fileName, "upload", subsidy.getTenantId());
    version.setConfirmed(1);
    version.setConfirmedBy(SecurityContext.currentUser().getUserId());
    version.setConfirmedAt(LocalDateTime.now());
    versionMapper.insert(version);

    instance.setCurrentVersionId(version.getId());
    instance.setStatus("upload_only");
    instanceMapper.updateById(instance);

    if (StringUtils.hasText(instance.getSlotCode())) {
      TrainSubsidySlotFileEntity slotFile = new TrainSubsidySlotFileEntity();
      slotFile.setSubsidyId(subsidy.getId());
      slotFile.setSlotCode(instance.getSlotCode());
      slotFile.setInstanceId(instance.getId());
      slotFile.setVersionId(version.getId());
      slotFile.setFileId(stored.id());
      slotFile.setFilePath(path);
      slotFile.setFileName(fileName);
      slotFile.setUploadedBy(SecurityContext.currentUser().getUserId());
      slotFile.setTenantId(subsidy.getTenantId());
      slotFile.setDeleted(0);
      slotFileMapper.insert(slotFile);
    }

    addEvent(subsidy, "instance_replace", "替换材料 " + instance.getTitle(),
        Map.of("instanceId", instance.getId(), "versionId", version.getId()));
    return toVO(instance, subsidy, loadSlotFiles(subsidy.getId()), latestPhotoPack(subsidy));
  }

  /**
   * 在线编辑保存：按边框密度挑选底稿（原始 xls / 模板库优先），POI 只改单元格值。
   */
  @Transactional
  public SubsidyMaterialInstanceVO patchCells(
      Long instanceId, com.guoyu.platform.trainingdepartment.dto.SubsidyMaterialPatchCellsReq req) {
    if (req == null || req.getCells() == null || req.getCells().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可保存的单元格");
    }
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    assertEditable(subsidy);

    var styleBase = styleBaseResolver.resolveForInstance(instanceId);
    if (styleBase == null || styleBase.bytes() == null || styleBase.bytes().length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，无法保存编辑");
    }
    if (styleBase.borderCount() < 20) {
      log.warn(
          "style base has few borders instanceId={} label={} borders={}",
          instanceId,
          styleBase.label(),
          styleBase.borderCount());
    }

    byte[] patched;
    try {
      patched =
          com.guoyu.platform.trainingdepartment.support.SubsidyXlsxCellPatcher.patch(
              styleBase.bytes(), req.getCells());
    } catch (Exception ex) {
      log.warn("patch cells failed instanceId={}", instanceId, ex);
      String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      throw new BusinessException(ErrorCode.BAD_REQUEST, "保存编辑失败: " + detail);
    }

    String sniffed =
        com.guoyu.platform.trainingdepartment.support.SubsidyExcelStyleSupport.sniffFormat(patched);
    boolean keepXls = "xls".equals(sniffed);
    String baseName =
        StringUtils.hasText(styleBase.fileName()) ? styleBase.fileName() : "material.xls";
    String outName = baseName;
    if (!outName.startsWith("[编辑]")) {
      outName = "[编辑]" + outName.replaceFirst("^\\[(填数|模板|编辑)\\]", "");
    }
    outName = outName.replaceAll("(?i)\\.xlsx?$", "");
    outName = outName + (keepXls ? ".xls" : ".xlsx");
    String contentType =
        keepXls
            ? "application/vnd.ms-excel"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    FileVO stored =
        fileApplicationService.storeFromBytes(patched, outName, contentType, "private");
    String path = "/api/files/" + stored.id() + "/download";

    TrainSubsidyMaterialVersionEntity version =
        newVersion(instance, stored.id(), path, outName, "inline_edit", subsidy.getTenantId());
    versionMapper.insert(version);
    instance.setCurrentVersionId(version.getId());
    if (!"confirmed".equals(instance.getStatus())) {
      instance.setStatus("filled");
    }
    instanceMapper.updateById(instance);

    addEvent(
        subsidy,
        "instance_edit",
        "在线编辑材料 " + instance.getTitle(),
        Map.of(
            "instanceId",
            instance.getId(),
            "versionId",
            version.getId(),
            "cellCount",
            req.getCells().size(),
            "styleBase",
            styleBase.label(),
            "styleBorders",
            styleBase.borderCount()));
    return toVO(instance, subsidy, loadSlotFiles(subsidy.getId()), latestPhotoPack(subsidy));
  }

  /** Word 轻量编辑初始化：解析当前版本可编辑段落（标题 / 元信息 / 图注） */
  @Transactional
  public com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialDocxMetaVO getDocxMeta(
      Long instanceId) {
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyMaterialVersionEntity version = currentVersionOf(instance);
    byte[] base = fileApplicationService.readFileBytesById(version.getFileId());
    com.guoyu.platform.trainingdepartment.support.SubsidyDocxTextPatcher.ParseResult parsed;
    try {
      parsed =
          com.guoyu.platform.trainingdepartment.support.SubsidyDocxTextPatcher.parse(base);
    } catch (Exception ex) {
      log.warn("parse docx meta failed instanceId={}", instanceId, ex);
      String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析 Word 内容: " + detail);
    }
    List<com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialDocxMetaVO.Line> lines =
        parsed.lines().stream()
            .map(
                l ->
                    new com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialDocxMetaVO.Line(
                        l.index(), l.kind(), l.label(), l.value(), l.text()))
            .toList();
    return new com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialDocxMetaVO(
        instance.getId(), version.getId(), version.getFileName(), lines);
  }

  /** Word 轻量编辑保存：按段落索引写回整段文本（POI 保留图片与段落样式） */
  @Transactional
  public SubsidyMaterialInstanceVO patchDocx(
      Long instanceId, com.guoyu.platform.trainingdepartment.dto.SubsidyMaterialPatchDocxReq req) {
    if (req == null || req.getParagraphs() == null || req.getParagraphs().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可保存的字段");
    }
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    assertEditable(subsidy);
    TrainSubsidyMaterialVersionEntity version = currentVersionOf(instance);

    byte[] base = fileApplicationService.readFileBytesById(version.getFileId());
    byte[] patched;
    try {
      patched =
          com.guoyu.platform.trainingdepartment.support.SubsidyDocxTextPatcher.patch(
              base, req.getParagraphs());
    } catch (Exception ex) {
      log.warn("patch docx failed instanceId={}", instanceId, ex);
      String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      throw new BusinessException(ErrorCode.BAD_REQUEST, "保存编辑失败: " + detail);
    }

    String baseName =
        StringUtils.hasText(version.getFileName()) ? version.getFileName() : "material.docx";
    String outName = baseName;
    if (!outName.startsWith("[编辑]")) {
      outName = "[编辑]" + outName.replaceFirst("^\\[(填数|模板|编辑)\\]", "");
    }
    outName = outName.replaceAll("(?i)\\.docx$", "");
    outName = outName + ".docx";
    FileVO stored =
        fileApplicationService.storeFromBytes(
            patched,
            outName,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "private");
    String path = "/api/files/" + stored.id() + "/download";

    TrainSubsidyMaterialVersionEntity newVersion =
        newVersion(instance, stored.id(), path, outName, "inline_edit", subsidy.getTenantId());
    versionMapper.insert(newVersion);
    instance.setCurrentVersionId(newVersion.getId());
    if (!"confirmed".equals(instance.getStatus())) {
      instance.setStatus("filled");
    }
    instanceMapper.updateById(instance);

    addEvent(
        subsidy,
        "instance_edit",
        "在线编辑材料 " + instance.getTitle(),
        Map.of(
            "instanceId", instance.getId(),
            "versionId", newVersion.getId(),
            "paragraphCount", req.getParagraphs().size()));
    return toVO(instance, subsidy, loadSlotFiles(subsidy.getId()), latestPhotoPack(subsidy));
  }

  @Transactional
  public SubsidyMaterialInstanceVO confirmInstance(Long instanceId) {
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    if (instance.getCurrentVersionId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，无法确认");
    }
    TrainSubsidyMaterialVersionEntity version = versionMapper.selectById(instance.getCurrentVersionId());
    if (version == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前版本不存在");
    }
    version.setConfirmed(1);
    version.setConfirmedBy(SecurityContext.currentUser().getUserId());
    version.setConfirmedAt(LocalDateTime.now());
    versionMapper.updateById(version);
    instance.setStatus("confirmed");
    instanceMapper.updateById(instance);
    addEvent(subsidy, "instance_confirm", "确认材料 " + instance.getTitle(),
        Map.of("instanceId", instance.getId(), "versionId", version.getId()));
    return toVO(instance, subsidy, loadSlotFiles(subsidy.getId()), latestPhotoPack(subsidy));
  }

  /** 从本班材料清单中删除（逻辑删除） */
  @Transactional
  public void deleteInstance(Long instanceId) {
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    assertEditable(subsidy);

    List<TrainSubsidySlotFileEntity> slotFiles = slotFileMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getInstanceId, instanceId)
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0));
    for (TrainSubsidySlotFileEntity f : slotFiles) {
      slotFileMapper.deleteById(f.getId());
    }

    addEvent(
        subsidy,
        "instance_delete",
        "删除材料 " + instance.getTitle(),
        Map.of("instanceId", instance.getId(), "required", instance.getRequiredFlag() != null && instance.getRequiredFlag() == 1));
    instanceMapper.deleteById(instanceId);
  }

  /** 本班 sync 或槽位变更后，将 slot_file 同步到实例版本 */
  @Transactional
  public void refreshFromSlotFiles(Long subsidyId) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    refreshInstancesFromSlotFiles(subsidy);
  }

  /**
   * 证书 PDF / 获证名册：仅在「实例无文件且槽位也无文件」时补同步；
   * 培训照片由 refreshInstancesFromSlotFiles 直接挂跟班册，不走整包 sync。
   * 已齐套时跳过，避免每次进页重导名册并上传 COS。
   */
  private void tryAutoSyncUpstream(TrainSubsidyEntity subsidy) {
    if (subsidy.getClassId() == null || subsidy.getProfileId() == null) {
      return;
    }
    try {
      assertEditable(subsidy);
    } catch (BusinessException ex) {
      return;
    }
    Set<String> needSlots = certificateSlotsNeedingSync(subsidy.getId());
    if (needSlots.isEmpty()) {
      return;
    }
    try {
      classMaterialService.syncSelectedSlotsQuiet(subsidy.getId(), needSlots);
    } catch (Exception ex) {
      log.warn("自动同步本班证书/名册失败 subsidyId={}: {}", subsidy.getId(), ex.getMessage());
    }
  }

  private Set<String> certificateSlotsNeedingSync(Long subsidyId) {
    List<TrainSubsidyMaterialInstanceEntity> instances =
        instanceMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
                .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidyId)
                .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0)
                .eq(
                    TrainSubsidyMaterialInstanceEntity::getMaterialType,
                    SubsidyMaterialTypes.CERTIFICATE));
    if (instances.isEmpty()) {
      return Set.of();
    }
    Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot =
        loadSlotFiles(subsidyId).stream()
            .filter(f -> StringUtils.hasText(f.getSlotCode()))
            .collect(Collectors.groupingBy(TrainSubsidySlotFileEntity::getSlotCode));
    Set<String> need = new HashSet<>();
    for (TrainSubsidyMaterialInstanceEntity inst : instances) {
      if (inst.getCurrentVersionId() != null) {
        continue;
      }
      String code = inst.getSlotCode();
      if (!StringUtils.hasText(code)) {
        continue;
      }
      if (filesBySlot.getOrDefault(code, List.of()).isEmpty()) {
        need.add(code);
      }
    }
    return need;
  }

  /**
   * 默认只保留培训照片 + 证书材料槽位；申请表/花名册/汇总等由「从模板生成」新建。
   * 已有补贴会补齐缺失的默认槽，并清掉无文件的非默认占位项。
   */
  private void ensureInstancesFromProfile(TrainSubsidyEntity subsidy) {
    if (subsidy.getProfileId() == null) {
      return;
    }
    List<TrainSubsidyPackSlotEntity> slots = profileService.listSlots(subsidy.getProfileId());
    if (slots == null || slots.isEmpty()) {
      return;
    }
    List<TrainSubsidyMaterialInstanceEntity> existing =
        instanceMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
                .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidy.getId())
                .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0));
    java.util.Set<String> existingSlots =
        existing.stream()
            .map(TrainSubsidyMaterialInstanceEntity::getSlotCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());

    for (TrainSubsidyPackSlotEntity slot : slots) {
      String mt = SubsidyMaterialTypes.resolveMaterialType(slot.getSlotCode(), slot.getRenderer());
      if (!SubsidyMaterialTypes.isDefaultWorkbenchType(mt)) {
        continue;
      }
      String code = slot.getSlotCode();
      if (StringUtils.hasText(code) && existingSlots.contains(code)) {
        continue;
      }
      TrainSubsidyMaterialInstanceEntity row = new TrainSubsidyMaterialInstanceEntity();
      row.setSubsidyId(subsidy.getId());
      row.setSlotCode(code);
      row.setMaterialType(mt);
      row.setTitle(slot.getTitle());
      row.setStatus("pending");
      row.setRequiredFlag(slot.getRequiredFlag() != null ? slot.getRequiredFlag() : 0);
      row.setSortOrder(slot.getSortOrder() != null ? slot.getSortOrder() : 0);
      row.setRenderer(slot.getRenderer());
      row.setTenantId(subsidy.getTenantId());
      row.setDeleted(0);
      instanceMapper.insert(row);
      if (StringUtils.hasText(code)) {
        existingSlots.add(code);
      }
    }
  }

  /**
   * 清掉配方里自动种下的、尚无文件的申请表/花名册/汇总占位；
   * 申领花名册（claim_roster）即使曾从本班同步也不再默认展示（改走模板生成）。
   * 用户已上传/填数的非默认材料予以保留。
   */
  private void pruneNonDefaultEmptyInstances(TrainSubsidyEntity subsidy) {
    List<TrainSubsidyMaterialInstanceEntity> instances =
        instanceMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
                .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidy.getId())
                .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0));
    for (TrainSubsidyMaterialInstanceEntity instance : instances) {
      if (SubsidyMaterialTypes.isDefaultWorkbenchType(instance.getMaterialType())) {
        continue;
      }
      boolean claimRoster = "claim_roster".equals(instance.getSlotCode());
      boolean empty = instance.getCurrentVersionId() == null;
      if (!claimRoster && !empty) {
        continue;
      }
      instance.setDeleted(1);
      instanceMapper.updateById(instance);
    }
  }

  private void refreshInstancesFromSlotFiles(TrainSubsidyEntity subsidy) {
    List<TrainSubsidyMaterialInstanceEntity> instances = instanceMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
            .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidy.getId())
            .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0));
    if (instances.isEmpty()) {
      return;
    }
    Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot = loadSlotFiles(subsidy.getId()).stream()
        .collect(Collectors.groupingBy(TrainSubsidySlotFileEntity::getSlotCode));

    TrainFollowPhotoPackEntity photoPack = latestPhotoPack(subsidy);

    for (TrainSubsidyMaterialInstanceEntity instance : instances) {
      if (isPhotoInstance(instance)) {
        if (photoPack != null && photoPack.getFileId() != null) {
          boolean needLink = instance.getCurrentVersionId() == null;
          if (!needLink) {
            TrainSubsidyMaterialVersionEntity cur = versionMapper.selectById(instance.getCurrentVersionId());
            needLink = cur == null || cur.getFileId() == null || !photoPack.getFileId().equals(cur.getFileId());
          }
          if (needLink) {
            upsertVersionFromPhotoPack(instance, photoPack, subsidy);
          }
        }
        continue;
      }
      String slotCode = instance.getSlotCode();
      if (!StringUtils.hasText(slotCode)) {
        continue;
      }
      List<TrainSubsidySlotFileEntity> files = filesBySlot.getOrDefault(slotCode, List.of());
      if (files.isEmpty()) {
        continue;
      }
      TrainSubsidySlotFileEntity latest = files.get(files.size() - 1);
      if (instance.getCurrentVersionId() != null) {
        TrainSubsidyMaterialVersionEntity cur = versionMapper.selectById(instance.getCurrentVersionId());
        if (cur != null && latest.getFileId() != null && latest.getFileId().equals(cur.getFileId())) {
          linkSlotFile(latest, instance, cur);
          // 多文件证书：确保全部 slot 都挂上 instance
          for (TrainSubsidySlotFileEntity f : files) {
            linkSlotFile(f, instance, cur);
          }
          continue;
        }
      }
      String source = isClassSyncFile(latest.getFileName()) ? "class_sync" : "upload";
      TrainSubsidyMaterialVersionEntity version = newVersion(
          instance, latest.getFileId(), latest.getFilePath(), latest.getFileName(), source, subsidy.getTenantId());
      if ("class_sync".equals(source)) {
        version.setConfirmed(0);
      } else {
        version.setConfirmed(1);
        version.setConfirmedBy(latest.getUploadedBy());
        version.setConfirmedAt(LocalDateTime.now());
      }
      versionMapper.insert(version);
      instance.setCurrentVersionId(version.getId());
      instance.setStatus("class_sync".equals(source) ? "from_class" : "upload_only");
      if (version.getConfirmed() != null && version.getConfirmed() == 1) {
        instance.setStatus("confirmed");
      }
      instanceMapper.updateById(instance);
      linkSlotFile(latest, instance, version);
      for (TrainSubsidySlotFileEntity f : files) {
        linkSlotFile(f, instance, version);
      }
    }
  }

  private static boolean isPhotoInstance(TrainSubsidyMaterialInstanceEntity instance) {
    return "follow_photo".equals(instance.getRenderer())
        || "photo_album".equals(instance.getSlotCode())
        || SubsidyMaterialTypes.PHOTO.equals(instance.getMaterialType());
  }

  private void upsertVersionFromPhotoPack(
      TrainSubsidyMaterialInstanceEntity instance, TrainFollowPhotoPackEntity photoPack, TrainSubsidyEntity subsidy) {
    TrainSubsidyMaterialVersionEntity version = newVersion(
        instance,
        photoPack.getFileId(),
        photoPack.getFilePath(),
        StringUtils.hasText(photoPack.getTitle()) ? photoPack.getTitle() + ".docx" : "培训照片.docx",
        "class_sync",
        subsidy.getTenantId());
    versionMapper.insert(version);
    instance.setCurrentVersionId(version.getId());
    instance.setStatus("from_class");
    instanceMapper.updateById(instance);
  }

  private TrainSubsidyMaterialVersionEntity newVersion(
      TrainSubsidyMaterialInstanceEntity instance,
      Long fileId,
      String filePath,
      String fileName,
      String source,
      Long tenantId) {
    int nextNo = nextVersionNo(instance.getId());
    TrainSubsidyMaterialVersionEntity version = new TrainSubsidyMaterialVersionEntity();
    version.setInstanceId(instance.getId());
    version.setVersionNo(nextNo);
    version.setFileId(fileId);
    version.setFilePath(filePath);
    version.setFileName(fileName);
    version.setSource(source);
    version.setConfirmed(0);
    version.setCreatedBy(SecurityContext.currentUser().getUserId());
    version.setTenantId(tenantId);
    version.setDeleted(0);
    return version;
  }

  private int nextVersionNo(Long instanceId) {
    TrainSubsidyMaterialVersionEntity last = versionMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
            .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
            .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
            .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
            .last("LIMIT 1"));
    return last == null ? 1 : last.getVersionNo() + 1;
  }

  private void linkSlotFile(
      TrainSubsidySlotFileEntity slotFile, TrainSubsidyMaterialInstanceEntity instance, TrainSubsidyMaterialVersionEntity version) {
    if (slotFile.getInstanceId() != null && slotFile.getVersionId() != null) {
      return;
    }
    slotFile.setInstanceId(instance.getId());
    slotFile.setVersionId(version.getId());
    slotFileMapper.updateById(slotFile);
  }

  private boolean isClassSyncFile(String fileName) {
    return StringUtils.hasText(fileName) && fileName.startsWith(TrainSubsidyClassMaterialService.AUTO_PREFIX);
  }

  private SubsidyMaterialInstanceListVO buildListVO(TrainSubsidyEntity subsidy) {
    List<TrainSubsidyMaterialInstanceEntity> instances = instanceMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
            .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidy.getId())
            .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0)
            .orderByAsc(TrainSubsidyMaterialInstanceEntity::getSortOrder)
            .orderByAsc(TrainSubsidyMaterialInstanceEntity::getId));
    List<TrainSubsidySlotFileEntity> allSlotFiles = loadSlotFiles(subsidy.getId());
    TrainFollowPhotoPackEntity photoPack = latestPhotoPack(subsidy);

    List<SubsidyMaterialInstanceVO> items = instances.stream()
        .map(i -> toVO(i, subsidy, allSlotFiles, photoPack))
        .sorted(Comparator.comparingInt((SubsidyMaterialInstanceVO v) -> SubsidyMaterialTypes.materialTypeOrder(v.materialType()))
            .thenComparing(SubsidyMaterialInstanceVO::title))
        .toList();

    Map<String, List<SubsidyMaterialInstanceVO>> grouped = items.stream()
        .collect(Collectors.groupingBy(SubsidyMaterialInstanceVO::materialType, LinkedHashMap::new, Collectors.toList()));

    List<SubsidyMaterialGroupVO> groups = grouped.entrySet().stream()
        .sorted(Comparator.comparingInt(e -> SubsidyMaterialTypes.materialTypeOrder(e.getKey())))
        .map(e -> new SubsidyMaterialGroupVO(
            e.getKey(),
            SubsidyMaterialTypes.materialTypeLabel(e.getKey()),
            e.getValue().size(),
            e.getValue()))
        .toList();

    int requiredTotal = (int) items.stream().filter(SubsidyMaterialInstanceVO::required).count();
    int confirmedRequired = (int) items.stream()
        .filter(SubsidyMaterialInstanceVO::required)
        .filter(i -> "confirmed".equals(i.status()))
        .count();
    int hasFileCount = (int) items.stream().filter(SubsidyMaterialInstanceVO::hasFile).count();

    return new SubsidyMaterialInstanceListVO(
        subsidy.getId(), items.size(), requiredTotal, confirmedRequired, hasFileCount, groups, items);
  }

  private SubsidyMaterialInstanceVO toVO(
      TrainSubsidyMaterialInstanceEntity instance,
      TrainSubsidyEntity subsidy,
      List<TrainSubsidySlotFileEntity> allSlotFiles,
      TrainFollowPhotoPackEntity photoPack) {
    TrainSubsidyMaterialVersionEntity version = instance.getCurrentVersionId() != null
        ? versionMapper.selectById(instance.getCurrentVersionId())
        : null;

    List<TrainSubsidySlotFileEntity> slotFiles = StringUtils.hasText(instance.getSlotCode())
        ? allSlotFiles.stream().filter(f -> instance.getSlotCode().equals(f.getSlotCode())).toList()
        : List.of();

    boolean photoPackReady = isPhotoInstance(instance)
        && photoPack != null && photoPack.getFileId() != null;
    boolean hasFile = version != null && version.getFileId() != null;
    if (!hasFile && photoPackReady) {
      hasFile = true;
    }
    if (!hasFile && !slotFiles.isEmpty()) {
      hasFile = true;
    }

    List<SubsidyMaterialSlotFileVO> slotFileVos = slotFiles.stream()
        .map(f -> new SubsidyMaterialSlotFileVO(f.getId(), f.getFileName(), f.getFilePath(), f.getFileId()))
        .toList();

    TrainSubsidyMaterialVersionEntity uploadTpl = versionMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
            .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instance.getId())
            .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
            .eq(TrainSubsidyMaterialVersionEntity::getSource, "upload")
            .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
            .last("LIMIT 1"));

    return new SubsidyMaterialInstanceVO(
        instance.getId(),
        instance.getSubsidyId(),
        instance.getSlotCode(),
        instance.getMaterialType(),
        SubsidyMaterialTypes.materialTypeLabel(instance.getMaterialType()),
        instance.getTitle(),
        instance.getStatus(),
        instance.getRenderer(),
        instance.getRequiredFlag() != null && instance.getRequiredFlag() == 1,
        instance.getCurrentVersionId(),
        version != null ? version.getVersionNo() : null,
        version != null ? version.getFileId() : (photoPackReady ? photoPack.getFileId() : null),
        version != null ? version.getFilePath() : (photoPackReady ? photoPack.getFilePath() : null),
        version != null ? version.getFileName() : (photoPackReady ? photoPack.getTitle() : null),
        hasFile,
        photoPackReady,
        photoPackReady ? photoPack.getFilePath() : null,
        photoPackReady ? photoPack.getTitle() : null,
        slotFiles.size(),
        slotFileVos,
        uploadTpl != null && uploadTpl.getFileId() != null,
        uploadTpl != null ? uploadTpl.getFileName() : null);
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

  private TrainSubsidyMaterialInstanceEntity requireInstance(Long instanceId) {
    TrainSubsidyMaterialInstanceEntity entity = instanceMapper.selectById(instanceId);
    if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "材料实例不存在");
    }
    return entity;
  }

  private void assertEditable(TrainSubsidyEntity subsidy) {
    String phase = subsidy.getPhase();
    if (!"draft".equals(phase) && !"checking".equals(phase) && !"ready".equals(phase) && !"rejected".equals(phase)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可修改材料");
    }
  }

  /** 取实例当前版本；无文件版本时报业务错（docx 元数据/补丁共用） */
  private TrainSubsidyMaterialVersionEntity currentVersionOf(
      TrainSubsidyMaterialInstanceEntity instance) {
    if (instance.getCurrentVersionId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，无法编辑");
    }
    TrainSubsidyMaterialVersionEntity version = versionMapper.selectById(instance.getCurrentVersionId());
    if (version == null || version.getFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，无法编辑");
    }
    return version;
  }

  private void addEvent(TrainSubsidyEntity subsidy, String action, String remark, Map<String, Object> payload) {
    TrainSubsidyEventEntity event = new TrainSubsidyEventEntity();
    event.setSubsidyId(subsidy.getId());
    event.setAction(action);
    event.setFromPhase(subsidy.getPhase());
    event.setToPhase(subsidy.getPhase());
    event.setRemark(remark);
    if (payload != null) {
      try {
        event.setPayloadJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
      } catch (Exception ignored) {
        event.setPayloadJson(null);
      }
    }
    event.setOperatorId(SecurityContext.currentUser().getUserId());
    event.setTenantId(subsidy.getTenantId());
    event.setDeleted(0);
    eventMapper.insert(event);
  }

  public List<TrainSubsidyMaterialInstanceEntity> listInstancesForExport(Long subsidyId, List<Long> instanceIds) {
    if (instanceIds == null || instanceIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导出的材料");
    }
    requireSubsidy(subsidyId);
    List<TrainSubsidyMaterialInstanceEntity> rows = instanceMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
            .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidyId)
            .in(TrainSubsidyMaterialInstanceEntity::getId, instanceIds)
            .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0));
    if (rows.size() != instanceIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "部分材料不存在或不属于本补贴单");
    }
    return rows;
  }
}

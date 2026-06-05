package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainFileDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFileListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.support.TrainClassMaterialPackService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 班级申报材料生成服务
 */
@Service
@Slf4j
public class TrainMaterialGenerateApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final FileMapper fileMapper;
  private final TrainFileListMapper trainFileListMapper;
  private final TrainFileDetailMapper trainFileDetailMapper;
  private final FileStorageProperties fileStorageProperties;
  private final TrainClassMaterialPackService materialPackService;

  public TrainMaterialGenerateApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      FileMapper fileMapper,
      TrainFileListMapper trainFileListMapper,
      TrainFileDetailMapper trainFileDetailMapper,
      FileStorageProperties fileStorageProperties,
      TrainClassMaterialPackService materialPackService) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.fileMapper = fileMapper;
    this.trainFileListMapper = trainFileListMapper;
    this.trainFileDetailMapper = trainFileDetailMapper;
    this.fileStorageProperties = fileStorageProperties;
    this.materialPackService = materialPackService;
  }

  /**
   * 生成班级申报材料 ZIP 包
   */
  @Transactional
  public Map<String, Object> generate(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // 1. 获取班级和培训信息
    TrainInfoClassEntity classEntity = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classEntity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    if (classEntity.getFileTemps() == null || classEntity.getFileTemps().isBlank()
        || "[]".equals(classEntity.getFileTemps().trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该班级未设置任何材料模板");
    }

    TrainInfoListEntity infoEntity = null;
    if (classEntity.getTrainInfoId() != null) {
      infoEntity = infoListMapper.selectById(classEntity.getTrainInfoId());
    }

    // 2. 获取学员列表
    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    if (students.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该班级未添加任何人员");
    }

    // 3. 获取教务人员列表
    List<TrainPeopleListEntity> staff = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "staff")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    byte[] zipBytes;
    try {
      zipBytes = materialPackService.buildZip(classEntity, infoEntity, students, staff);
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
    } catch (Exception e) {
      log.error("生成申报材料失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成申报材料失败: " + e.getMessage());
    }

    // 4. 保存文件到存储
    String safeName = sanitizeFileName(classEntity.getTitle()) + "_申报材料.zip";
    String objectKey = buildObjectKey(tenantId, safeName);

    try {
      Path storagePath = Paths.get(fileStorageProperties.localRoot(), objectKey);
      Files.createDirectories(storagePath.getParent());
      Files.write(storagePath, zipBytes);

      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zipBytes));

      FileEntity fileEntity = new FileEntity();
      fileEntity.setTenantId(tenantId);
      fileEntity.setStorageProvider(fileStorageProperties.storageProvider());
      fileEntity.setObjectKey(objectKey);
      fileEntity.setOriginalName(safeName);
      fileEntity.setContentType("application/zip");
      fileEntity.setSizeBytes((long) zipBytes.length);
      fileEntity.setSha256(sha256);
      fileEntity.setAccessPolicy("private");
      fileEntity.setStatus("uploaded");
      fileEntity.setCreatedBy(userId);
      fileEntity.setUpdatedBy(userId);
      fileMapper.insert(fileEntity);

      // 5. 创建培训文件记录
      TrainFileListEntity fileList = new TrainFileListEntity();
      fileList.setTableName("train_info_class");
      fileList.setPk(String.valueOf(classId));
      fileList.setTitle(classEntity.getTitle() + "_申报材料");
      fileList.setUserId(userId);
      fileList.setStatus(1);
      fileList.setSortNumber(0);
      fileList.setTenantId(tenantId);
      fileList.setCreateTime(LocalDateTime.now());
      trainFileListMapper.insert(fileList);

      TrainFileDetailEntity fileDetail = new TrainFileDetailEntity();
      fileDetail.setFileId(fileList.getId());
      fileDetail.setTitle(safeName);
      fileDetail.setPath("/api/files/" + fileEntity.getId() + "/download");
      fileDetail.setUserId(userId);
      fileDetail.setStatus(1);
      fileDetail.setSortNumber(0);
      fileDetail.setTenantId(tenantId);
      fileDetail.setCreateTime(LocalDateTime.now());
      trainFileDetailMapper.insert(fileDetail);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("downUrl", "/api/files/" + fileEntity.getId() + "/download");
      result.put("fileId", fileEntity.getId());
      return result;
    } catch (Exception e) {
      log.error("保存申报材料文件失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存申报材料文件失败: " + e.getMessage());
    }
  }

  private String sanitizeFileName(String name) {
    if (name == null) return "material";
    return name.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private String buildObjectKey(Long tenantId, String safeName) {
    LocalDateTime now = LocalDateTime.now();
    return String.format("%d/%d/%02d/%s-%s",
        tenantId, now.getYear(), now.getMonthValue(),
        UUID.randomUUID().toString().substring(0, 8), safeName);
  }
}

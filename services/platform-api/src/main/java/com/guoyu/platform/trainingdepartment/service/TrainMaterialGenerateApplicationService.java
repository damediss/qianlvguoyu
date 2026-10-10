package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final FileApplicationService fileApplicationService;
  private final TrainFileListMapper trainFileListMapper;
  private final TrainFileDetailMapper trainFileDetailMapper;
  private final TrainClassMaterialPackService materialPackService;

  public TrainMaterialGenerateApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      FileApplicationService fileApplicationService,
      TrainFileListMapper trainFileListMapper,
      TrainFileDetailMapper trainFileDetailMapper,
      TrainClassMaterialPackService materialPackService) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.fileApplicationService = fileApplicationService;
    this.trainFileListMapper = trainFileListMapper;
    this.trainFileDetailMapper = trainFileDetailMapper;
    this.materialPackService = materialPackService;
  }

  /**
   * 生成班级申报材料 ZIP 包
   */
  @Transactional
  public Map<String, Object> generate(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

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

    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    if (students.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该班级未添加任何人员");
    }

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

    String safeName = sanitizeFileName(classEntity.getTitle()) + "_申报材料.zip";
    try {
      // 走统一文件服务：配置为 cos 时会真正上传 COS，避免只写本地却标记 cos 导致下载失败
      FileVO stored = fileApplicationService.storeFromBytes(
          zipBytes, safeName, "application/zip", "private");

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
      fileDetail.setPath("/api/files/" + stored.id() + "/download");
      fileDetail.setUserId(userId);
      fileDetail.setStatus(1);
      fileDetail.setSortNumber(0);
      fileDetail.setTenantId(tenantId);
      fileDetail.setCreateTime(LocalDateTime.now());
      trainFileDetailMapper.insert(fileDetail);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("downUrl", "/api/files/" + stored.id() + "/download");
      result.put("fileId", stored.id());
      return result;
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("保存申报材料文件失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存申报材料文件失败: " + e.getMessage());
    }
  }

  private String sanitizeFileName(String name) {
    if (name == null) {
      return "material";
    }
    return name.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}

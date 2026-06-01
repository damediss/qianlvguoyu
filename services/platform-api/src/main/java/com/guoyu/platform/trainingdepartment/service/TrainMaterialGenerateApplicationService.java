package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
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
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final FileMapper fileMapper;
  private final TrainFileListMapper trainFileListMapper;
  private final TrainFileDetailMapper trainFileDetailMapper;
  private final FileStorageProperties fileStorageProperties;

  public TrainMaterialGenerateApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      FileMapper fileMapper,
      TrainFileListMapper trainFileListMapper,
      TrainFileDetailMapper trainFileDetailMapper,
      FileStorageProperties fileStorageProperties) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.fileMapper = fileMapper;
    this.trainFileListMapper = trainFileListMapper;
    this.trainFileDetailMapper = trainFileDetailMapper;
    this.fileStorageProperties = fileStorageProperties;
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

    TrainInfoListEntity infoEntity = null;
    if (classEntity.getTrainInfoId() != null) {
      infoEntity = infoListMapper.selectById(classEntity.getTrainInfoId());
    }

    // 2. 获取企业名称
    String companyName = "";
    if (classEntity.getCompanyId() != null) {
      OrgEntity org = orgMapper.selectById(classEntity.getCompanyId());
      if (org != null) companyName = org.getName();
    }

    // 3. 获取工种名称
    String workTypeName = "";
    if (classEntity.getWorkTypeId() != null) {
      ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(classEntity.getWorkTypeId());
      if (wt != null) workTypeName = wt.getTitle();
    }

    // 4. 获取学员列表
    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    // 5. 获取教务人员列表
    List<TrainPeopleListEntity> staff = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "staff")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    // 6. 生成内存中的 ZIP 文件
    byte[] zipBytes;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {

        // 开班信息
        zos.putNextEntry(new ZipEntry("开班信息.txt"));
        String info = buildClassInfo(classEntity, infoEntity, companyName, workTypeName, students.size());
        zos.write(info.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // 学员花名册
        zos.putNextEntry(new ZipEntry("学员花名册.csv"));
        zos.write(buildStudentCsv(students).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // 教务人员
        if (!staff.isEmpty()) {
          zos.putNextEntry(new ZipEntry("教务人员.csv"));
          zos.write(buildStaffCsv(staff).getBytes(StandardCharsets.UTF_8));
          zos.closeEntry();
        }
      }
      zipBytes = baos.toByteArray();
    } catch (Exception e) {
      log.error("生成申报材料失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成申报材料失败: " + e.getMessage());
    }

    // 7. 保存文件到存储
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

      // 8. 创建培训文件记录
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

  private String buildClassInfo(TrainInfoClassEntity clazz, TrainInfoListEntity info,
      String companyName, String workTypeName, int studentCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== 班级申报信息 ===\n\n");
    sb.append("班级名称: ").append(clazz.getTitle() != null ? clazz.getTitle() : "").append("\n");
    sb.append("培训项目: ").append(info != null ? info.getTitle() : "").append("\n");
    sb.append("企业名称: ").append(companyName).append("\n");
    sb.append("工种: ").append(workTypeName).append("\n");
    sb.append("班级人数: ").append(clazz.getNums() != null ? clazz.getNums() : 0).append("\n");
    sb.append("学员人数: ").append(studentCount).append("\n");
    sb.append("开始日期: ").append(clazz.getDateStart() != null ? clazz.getDateStart() : "").append("\n");
    sb.append("结束日期: ").append(clazz.getDateEnd() != null ? clazz.getDateEnd() : "").append("\n");
    if (info != null) {
      sb.append("培训地点: ").append(info.getPosition() != null ? info.getPosition() : "").append("\n");
      sb.append("课时数: ").append(info.getCourseNum() != null ? info.getCourseNum() : 0).append("\n");
    }
    sb.append("\n生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
    return sb.toString();
  }

  private String buildStudentCsv(List<TrainPeopleListEntity> students) {
    StringBuilder sb = new StringBuilder();
    sb.append("序号,姓名,手机号,身份证号,性别,是否班组长,是否班委,平时成绩,考试成绩,实操成绩\n");
    int index = 1;
    for (TrainPeopleListEntity s : students) {
      sb.append(index++).append(",");
      sb.append(csvEscape(s.getRealname())).append(",");
      sb.append(csvEscape(s.getPhone())).append(",");
      sb.append(csvEscape(s.getIdCard())).append(",");
      sb.append(","); // gender not stored directly
      sb.append(s.getIsLeader() != null && s.getIsLeader() == 1 ? "是" : "否").append(",");
      sb.append(s.getIsCommittee() != null && s.getIsCommittee() == 1 ? "是" : "否").append(",");
      sb.append(s.getNormalScore() != null ? s.getNormalScore() : "").append(",");
      sb.append(s.getExamScore() != null ? s.getExamScore() : "").append(",");
      sb.append(s.getRealScore() != null ? s.getRealScore() : "").append("\n");
    }
    return sb.toString();
  }

  private String buildStaffCsv(List<TrainPeopleListEntity> staff) {
    StringBuilder sb = new StringBuilder();
    sb.append("序号,姓名,手机号,身份证号\n");
    int index = 1;
    for (TrainPeopleListEntity s : staff) {
      sb.append(index++).append(",");
      sb.append(csvEscape(s.getRealname())).append(",");
      sb.append(csvEscape(s.getPhone())).append(",");
      sb.append(csvEscape(s.getIdCard())).append("\n");
    }
    return sb.toString();
  }

  private String csvEscape(String val) {
    if (val == null) return "";
    if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
      return "\"" + val.replace("\"", "\"\"") + "\"";
    }
    return val;
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

package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackSlotEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.support.SubsidyCertifiedPeopleSupport;
import com.guoyu.platform.trainingdepartment.support.SubsidyMaterialTypes;
import com.guoyu.platform.trainingdepartment.support.TrainingDocxVariableReplacer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 按班汇集上游材料到补贴单槽位：结业证书、获证名册、跟班照片册状态、个人申领表。
 */
@Service
@Slf4j
public class TrainSubsidyClassMaterialService {

  private static final Pattern FILE_ID = Pattern.compile("/api/files/(\\d+)");
  /** 自动同步产生的槽位文件名前缀，再次刷新时只替换这类文件，保留人工上传 */
  public static final String AUTO_PREFIX = "[班]";
  /** 个人技能提升补贴申领表（2025）模板 */
  private static final String CLAIM_FORM_TEMPLATE =
      "templates/subsidy/personal_skill_upgrade_claim_2025.docx";
  private static final String CLAIM_FORM_SLOT = "apply_form";
  private static final String CLAIM_FORM_TITLE = "个人技能提升补贴申领表";
  private static final String[] LEVEL_LABELS = {
      "",
      "五级/初级工",
      "四级/中级工",
      "三级/高级工",
      "二级/技师",
      "一级/高级技师"
  };
  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  private final TrainSubsidyMapper subsidyMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final TrainSubsidyPackProfileApplicationService profileService;
  private final GraduationCertifiedRosterExportService rosterExportService;
  private final FileApplicationService fileApplicationService;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TrainSubsidyClassMaterialService(
      TrainSubsidyMapper subsidyMapper,
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainCertificateMapper certificateMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainSubsidyEventMapper eventMapper,
      TrainSubsidyPackProfileApplicationService profileService,
      GraduationCertifiedRosterExportService rosterExportService,
      FileApplicationService fileApplicationService,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      PlatformUserDetailMapper platformUserDetailMapper) {
    this.subsidyMapper = subsidyMapper;
    this.classMapper = classMapper;
    this.peopleListMapper = peopleListMapper;
    this.certificateMapper = certificateMapper;
    this.photoPackMapper = photoPackMapper;
    this.slotFileMapper = slotFileMapper;
    this.eventMapper = eventMapper;
    this.profileService = profileService;
    this.rosterExportService = rosterExportService;
    this.fileApplicationService = fileApplicationService;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> preview(Long subsidyId) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    if (subsidy.getClassId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定班级");
    }
    TrainInfoClassEntity clazz = requireClass(subsidy.getClassId());
    Long tenantId = subsidy.getTenantId();

    List<TrainPeopleListEntity> allStudents = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getTrainClassId, clazz.getId())
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    Map<Long, TrainCertificateEntity> certByPeoplePreview = loadCertsByPeople(clazz.getId(), tenantId);
    long certifiedStudentCount =
        SubsidyCertifiedPeopleSupport.countCertified(allStudents, certByPeoplePreview);

    List<TrainCertificateEntity> certs = listClassCerts(clazz.getId(), tenantId);
    long certWithFile = certs.stream().filter(c -> StringUtils.hasText(c.getFilePath())).count();

    TrainFollowPhotoPackEntity photo = latestPhotoPack(clazz.getId(), tenantId);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("subsidyId", subsidy.getId());
    result.put("classId", clazz.getId());
    result.put("className", clazz.getTitle());
    /** 申报口径人数：仅已获证 */
    result.put("studentCount", certifiedStudentCount);
    result.put("certifiedStudentCount", certifiedStudentCount);
    result.put("classStudentTotal", allStudents.size());
    result.put("certificateTotal", certs.size());
    result.put("certificateWithFile", certWithFile);
    result.put("photoPackReady", photo != null && photo.getFileId() != null);
    result.put("photoPackId", photo != null ? photo.getId() : null);
    result.put("photoPackTitle", photo != null ? photo.getTitle() : null);
    result.put("photoPackPath", photo != null ? photo.getFilePath() : null);

    List<Map<String, Object>> certPreview = new ArrayList<>();
    Map<Long, TrainPeopleListEntity> peopleMap = loadPeopleMap(clazz.getId(), tenantId);
    for (TrainCertificateEntity c : certs) {
      if (certPreview.size() >= 20) break;
      TrainPeopleListEntity p = c.getPeopleId() != null ? peopleMap.get(c.getPeopleId()) : null;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("certificateId", c.getId());
      row.put("peopleId", c.getPeopleId());
      row.put("userName", c.getUserName() != null ? c.getUserName() : (p != null ? p.getRealname() : null));
      row.put("certificateNo", c.getCertificateNo());
      row.put("hasFile", StringUtils.hasText(c.getFilePath()));
      row.put("filePath", c.getFilePath());
      certPreview.add(row);
    }
    result.put("certificates", certPreview);

    if (subsidy.getProfileId() != null) {
      List<TrainSubsidyPackSlotEntity> slots = profileService.listSlots(subsidy.getProfileId());
      List<Map<String, Object>> slotHints = new ArrayList<>();
      for (TrainSubsidyPackSlotEntity slot : slots) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("slotCode", slot.getSlotCode());
        h.put("title", slot.getTitle());
        h.put("required", slot.getRequiredFlag() != null && slot.getRequiredFlag() == 1);
        h.put("renderer", slot.getRenderer());
        h.put("upstream", resolveUpstreamHint(slot.getSlotCode(), slot.getRenderer()));
        slotHints.add(h);
      }
      result.put("slots", slotHints);
    } else {
      result.put("slots", List.of());
    }
    return result;
  }

  /**
   * 从本班刷新可自动材料：证书 PDF、获证名册；照片槽仍引用跟班册无需落槽文件。
   */
  @Transactional
  public Map<String, Object> syncFromClass(Long subsidyId) {
    return syncFromClass(subsidyId, null, true);
  }

  /**
   * 进页清单用：只同步指定槽位，且不写时间线、不算 preview（避免每次打开重导名册上传 COS）。
   */
  @Transactional
  public Map<String, Object> syncSelectedSlotsQuiet(Long subsidyId, java.util.Set<String> slotCodes) {
    if (slotCodes == null || slotCodes.isEmpty()) {
      return Map.of();
    }
    return syncFromClass(subsidyId, slotCodes, false);
  }

  private Map<String, Object> syncFromClass(
      Long subsidyId, java.util.Set<String> onlySlotCodes, boolean fullResponse) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    assertEditable(subsidy);
    if (subsidy.getClassId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定班级");
    }
    if (subsidy.getProfileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定配方");
    }
    TrainInfoClassEntity clazz = requireClass(subsidy.getClassId());
    Long tenantId = subsidy.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    List<TrainSubsidyPackSlotEntity> slots = profileService.listSlots(subsidy.getProfileId());

    int certSynced = 0;
    int rosterSynced = 0;
    List<String> notes = new ArrayList<>();

    for (TrainSubsidyPackSlotEntity slot : slots) {
      String code = slot.getSlotCode() != null ? slot.getSlotCode() : "";
      if (onlySlotCodes != null && !onlySlotCodes.contains(code)) {
        continue;
      }
      String renderer = slot.getRenderer() != null ? slot.getRenderer() : "";
      String title = slot.getTitle() != null ? slot.getTitle() : "";
      if ("cert_pdf_bundle".equals(code)
          || "file_copy".equals(renderer)
          || "pdf_merge".equals(renderer)
          || title.contains("证书PDF")
          || title.contains("证书 pdf")
          || (title.contains("证书") && title.toUpperCase().contains("PDF"))) {
        int n = syncCertificates(subsidy, clazz, tenantId, userId, code);
        certSynced += n;
        if (n > 0) {
          notes.add(slot.getTitle() + "：同步证书 " + n + " 份");
        } else if (fullResponse) {
          notes.add(slot.getTitle() + "：本班暂无证书文件，可手工替换");
        }
      } else if ("cert_roster".equals(code)
          || "cert_pass".equals(code)
          || "cert_retake".equals(code)
          || title.contains("获证名册")
          || title.contains("获证人员名册")) {
        // 申领花名册 claim_roster 不自动同步，改由「从模板生成」
        boolean ok = syncRoster(subsidy, clazz, tenantId, userId, code);
        if (ok) {
          rosterSynced++;
          notes.add(slot.getTitle() + "：已写入获证名册");
        } else if (fullResponse) {
          notes.add(slot.getTitle() + "：名册同步失败或无已获证人员，可手工上传");
        }
      } else if (fullResponse
          && ("follow_photo".equals(renderer)
              || "photo_album".equals(code)
              || title.contains("培训照片")
              || SubsidyMaterialTypes.PHOTO.equals(
                  SubsidyMaterialTypes.resolveMaterialType(code, renderer)))) {
        TrainFollowPhotoPackEntity photo = latestPhotoPack(clazz.getId(), tenantId);
        if (photo != null && photo.getFileId() != null) {
          notes.add(slot.getTitle() + "：将引用跟班照片册 #" + photo.getId());
        } else {
          notes.add(slot.getTitle() + "：本班尚无跟班照片册，请先在跟班留痕生成或手工补传");
        }
      }
    }

    if (!fullResponse) {
      Map<String, Object> quiet = new LinkedHashMap<>();
      quiet.put("certificateFiles", certSynced);
      quiet.put("rosterSlots", rosterSynced);
      quiet.put("notes", notes);
      return quiet;
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("certificateFiles", certSynced);
    payload.put("rosterSlots", rosterSynced);
    payload.put("notes", notes);
    addEvent(subsidy, "sync_class", "从本班刷新材料引用（仅已获证）", payload);

    // 同步申报人数口径为已获证人数
    Map<String, Object> preview = preview(subsidyId);
    Object certified = preview.get("certifiedStudentCount");
    if (certified instanceof Number n) {
      subsidy.setStudentCount(n.intValue());
      subsidyMapper.updateById(subsidy);
    }

    Map<String, Object> result = preview;
    result.put("syncedCertificateFiles", certSynced);
    result.put("syncedRosterSlots", rosterSynced);
    result.put("notes", notes);
    result.put(
        "message",
        notes.isEmpty()
            ? "未同步到可自动材料，请检查本班已获证证书/名册/照片"
            : "已从本班刷新材料（仅已获证人员）");
    return result;
  }

  /**
   * 一键生成本班「已获证」学员的《个人技能提升补贴申领表（2025）》。
   * 未获证人员不进入申报补贴材料。
   */
  @Transactional
  public Map<String, Object> generatePersonalClaimForms(Long subsidyId) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    assertEditable(subsidy);
    if (subsidy.getClassId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定班级");
    }
    if (subsidy.getProfileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "补贴单未绑定配方");
    }
    TrainInfoClassEntity clazz = requireClass(subsidy.getClassId());
    Long tenantId = subsidy.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    String slotCode = resolveClaimFormSlot(subsidy.getProfileId());
    byte[] templateBytes = loadClaimFormTemplate();

    List<TrainPeopleListEntity> allStudents = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getTrainClassId, clazz.getId())
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .orderByAsc(TrainPeopleListEntity::getSortNumber)
            .orderByAsc(TrainPeopleListEntity::getId));
    if (allStudents.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "本班暂无学员，无法生成申领表");
    }

    Map<Long, TrainCertificateEntity> certByPeople = loadCertsByPeople(clazz.getId(), tenantId);
    int skippedNoCert = 0;
    List<TrainPeopleListEntity> students = new ArrayList<>();
    for (TrainPeopleListEntity s : allStudents) {
      if (!SubsidyCertifiedPeopleSupport.isCertified(certByPeople.get(s.getId()))) {
        skippedNoCert++;
        continue;
      }
      students.add(s);
    }
    if (students.isEmpty()) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "本班没有已获证学员（未获证 "
              + skippedNoCert
              + " 人），申报补贴仅生成已获证人员材料，请先在毕业典礼完成证书回填");
    }

    Map<String, TrainSignUpPeopleSelfEntity> signUpByIdCard = loadSignUpsByIdCard(students);
    Map<String, PlatformUserDetailEntity> detailByIdCard = loadPlatformDetailsByIdCard(students);

    String classWorkType = resolveWorkTypeTitle(clazz.getWorkTypeId());
    String classOccupation = resolveOccupationTitle(clazz.getWorkTypeId(), classWorkType);
    String classLevel = formatLevelLabel(clazz.getWorkTypeLevel());

    clearClaimFormAutoFiles(subsidy.getId(), slotCode);

    int generated = 0;
    int failed = 0;
    List<String> notes = new ArrayList<>();
    List<Map.Entry<String, byte[]>> zipEntries = new ArrayList<>();
    java.util.Set<String> usedZipNames = new java.util.HashSet<>();
    for (TrainPeopleListEntity student : students) {
      try {
        TrainCertificateEntity cert = certByPeople.get(student.getId());
        String idCard = StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : "";
        TrainSignUpPeopleSelfEntity signUp = StringUtils.hasText(idCard) ? signUpByIdCard.get(idCard) : null;
        PlatformUserDetailEntity detail = StringUtils.hasText(idCard) ? detailByIdCard.get(idCard) : null;

        String realname = firstNonBlank(
            student.getRealname(),
            cert != null ? cert.getUserName() : null,
            signUp != null ? signUp.getName() : null,
            "学员");
        String phone = firstNonBlank(
            student.getPhone(),
            signUp != null ? signUp.getPhone() : null,
            detail != null ? detail.getPhone() : null);
        String address = firstNonBlank(
            signUp != null ? signUp.getAddress() : null,
            detail != null ? detail.getAddress() : null);

        String workType = firstNonBlank(
            cert != null ? cert.getWorkType() : null,
            signUp != null ? signUp.getWorkTypeName() : null,
            classWorkType);
        String occupation = firstNonBlank(classOccupation, workType);
        String jobPost = firstNonBlank(
            signUp != null ? signUp.getJob() : null,
            signUp != null ? signUp.getJog() : null,
            workType);
        String level = firstNonBlank(
            formatLevelText(cert != null ? cert.getLevel() : null),
            classLevel);
        String certificateNo = cert != null ? nullToEmpty(cert.getCertificateNo()) : "";
        String issuingAuthority = cert != null ? nullToEmpty(cert.getIssuingAuthority()) : "";
        String issueDate = formatIssueDate(cert != null ? cert.getIssueDate() : null);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("name", nullToEmpty(realname));
        vars.put("idCard", idCard);
        vars.put("phone", nullToEmpty(phone));
        vars.put("address", nullToEmpty(address));
        vars.put("occupation", nullToEmpty(occupation));
        vars.put("workType", nullToEmpty(workType));
        vars.put("jobPost", nullToEmpty(jobPost));
        vars.put("level", nullToEmpty(level));
        vars.put("certificateNo", certificateNo);
        vars.put("issuingAuthority", issuingAuthority);
        vars.put("issueDate", issueDate);

        byte[] filled = TrainingDocxVariableReplacer.replaceAll(templateBytes, vars);
        String fileName = AUTO_PREFIX
            + CLAIM_FORM_TITLE
            + "_"
            + sanitizeFileName(realname)
            + (StringUtils.hasText(idCard) ? "_" + idCard : "")
            + ".docx";
        FileVO stored = fileApplicationService.storeFromBytes(filled, fileName, DOCX_CONTENT_TYPE, "private");
        String path = "/api/files/" + stored.id() + "/download";
        insertSlotFile(subsidy, slotCode, stored.id(), path, fileName, userId, tenantId);
        zipEntries.add(Map.entry(uniqueZipEntryName(fileName, usedZipNames), filled));
        generated++;
      } catch (Exception ex) {
        failed++;
        String who = firstNonBlank(student.getRealname(), String.valueOf(student.getId()));
        notes.add(who + "：生成失败");
        log.warn("生成个人申领表失败 subsidyId={} peopleId={}", subsidyId, student.getId(), ex);
      }
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("slotCode", slotCode);
    payload.put("generated", generated);
    payload.put("failed", failed);
    payload.put("eligibleCount", students.size());
    payload.put("classStudentCount", allStudents.size());
    payload.put("skippedNoCert", skippedNoCert);
    addEvent(subsidy, "generate_claim_forms", "一键生成个人技能提升补贴申领表（仅已获证）", payload);

    Map<String, Object> result = preview(subsidyId);
    result.put("claimFormSlot", slotCode);
    result.put("claimFormsGenerated", generated);
    result.put("claimFormsFailed", failed);
    result.put("claimFormStudentCount", students.size());
    result.put("claimFormClassStudentCount", allStudents.size());
    result.put("claimFormSkippedNoCert", skippedNoCert);
    result.put("notes", notes);
    result.put(
        "message",
        generated > 0
            ? "已为 "
                + generated
                + " 名已获证学员生成申领表"
                + (failed > 0 ? "（失败 " + failed + "）" : "")
                + "；跳过未获证 "
                + skippedNoCert
                + " 人"
            : "未生成任何申领表，请检查本班已获证学员数据");

    if (!zipEntries.isEmpty()) {
      try {
        byte[] zipBytes = buildClaimFormsZip(subsidy, zipEntries);
        String zipName = "个人申领表_"
            + subsidy.getId()
            + "_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".zip";
        FileVO zipStored = fileApplicationService.storeFromBytes(zipBytes, zipName, "application/zip", "private");
        String zipPath = "/api/files/" + zipStored.id() + "/download";
        result.put("ok", true);
        result.put("fileId", zipStored.id());
        result.put("filePath", zipPath);
        // private 对象不可用 COS 裸链（浏览器直链 403），统一返回鉴权下载路径
        result.put("url", zipPath);
        result.put("downloadFileName", zipName);
      } catch (Exception ex) {
        log.warn("打包个人申领表 ZIP 失败 subsidyId={}", subsidyId, ex);
        notes.add("申领表已入库，但打包下载失败：" + ex.getMessage());
        result.put("notes", notes);
      }
    }
    return result;
  }

  private byte[] buildClaimFormsZip(TrainSubsidyEntity subsidy, List<Map.Entry<String, byte[]>> entries)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(bos)) {
      String readme = "个人技能提升补贴申领表\n"
          + "subsidyId=" + subsidy.getId() + "\n"
          + "classId=" + subsidy.getClassId() + "\n"
          + "count=" + entries.size() + "\n"
          + "generatedAt=" + LocalDateTime.now() + "\n";
      zos.putNextEntry(new ZipEntry("README.txt"));
      zos.write(readme.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      for (Map.Entry<String, byte[]> entry : entries) {
        zos.putNextEntry(new ZipEntry("个人申领表/" + entry.getKey()));
        zos.write(entry.getValue());
        zos.closeEntry();
      }
    }
    return bos.toByteArray();
  }

  /** 避免同名学员文件在 ZIP 中互相覆盖 */
  private static String uniqueZipEntryName(String fileName, java.util.Set<String> usedNames) {
    if (usedNames.add(fileName)) {
      return fileName;
    }
    int dot = fileName.lastIndexOf('.');
    String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
    String ext = dot > 0 ? fileName.substring(dot) : "";
    for (int i = 2; ; i++) {
      String candidate = stem + "_" + i + ext;
      if (usedNames.add(candidate)) {
        return candidate;
      }
    }
  }

  private int syncCertificates(
      TrainSubsidyEntity subsidy,
      TrainInfoClassEntity clazz,
      Long tenantId,
      Long userId,
      String slotCode) {
    clearAutoSlotFiles(subsidy.getId(), slotCode);
    List<TrainCertificateEntity> certs = listClassCerts(clazz.getId(), tenantId);
    Map<Long, TrainPeopleListEntity> peopleMap = loadPeopleMap(clazz.getId(), tenantId);
    int count = 0;
    for (TrainCertificateEntity cert : certs) {
      Long fileId = parseFileId(cert.getFilePath());
      if (fileId == null) continue;
      TrainPeopleListEntity p = cert.getPeopleId() != null ? peopleMap.get(cert.getPeopleId()) : null;
      String realname = firstNonBlank(cert.getUserName(), p != null ? p.getRealname() : null, "学员");
      String idCard = p != null && StringUtils.hasText(p.getIdCard()) ? p.getIdCard() : "";
      String certNo = StringUtils.hasText(cert.getCertificateNo()) ? cert.getCertificateNo() : String.valueOf(cert.getId());
      String fileName = AUTO_PREFIX + realname
          + (StringUtils.hasText(idCard) ? "_" + idCard : "")
          + "_" + certNo + ".pdf";
      insertSlotFile(subsidy, slotCode, fileId, cert.getFilePath(), fileName, userId, tenantId);
      count++;
    }
    return count;
  }

  private boolean syncRoster(
      TrainSubsidyEntity subsidy,
      TrainInfoClassEntity clazz,
      Long tenantId,
      Long userId,
      String slotCode) {
    try {
      // 申报补贴一律只导出已获证人员
      String scope = "certified";
      byte[] bytes = rosterExportService.exportWorkbookBytes(clazz.getId(), scope);
      if (bytes == null || bytes.length == 0) return false;
      clearAutoSlotFiles(subsidy.getId(), slotCode);
      String name = AUTO_PREFIX
          + ("claim_roster".equals(slotCode) ? "申领补贴人员花名册" : "获证人员名册")
          + "_" + (clazz.getTitle() != null ? clazz.getTitle() : clazz.getId())
          + ".xlsx";
      FileVO stored = fileApplicationService.storeFromBytes(
          bytes, name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "private");
      String path = "/api/files/" + stored.id() + "/download";
      insertSlotFile(subsidy, slotCode, stored.id(), path, name, userId, tenantId);
      return true;
    } catch (Exception ex) {
      log.warn("同步获证名册失败 subsidyId={} slot={}", subsidy.getId(), slotCode, ex);
      return false;
    }
  }

  private void clearAutoSlotFiles(Long subsidyId, String slotCode) {
    List<TrainSubsidySlotFileEntity> existing = slotFileMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidyId)
            .eq(TrainSubsidySlotFileEntity::getSlotCode, slotCode)
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0));
    for (TrainSubsidySlotFileEntity f : existing) {
      if (f.getFileName() != null && f.getFileName().startsWith(AUTO_PREFIX)) {
        slotFileMapper.deleteById(f.getId());
      }
    }
  }

  private void insertSlotFile(
      TrainSubsidyEntity subsidy,
      String slotCode,
      Long fileId,
      String filePath,
      String fileName,
      Long userId,
      Long tenantId) {
    TrainSubsidySlotFileEntity row = new TrainSubsidySlotFileEntity();
    row.setSubsidyId(subsidy.getId());
    row.setSlotCode(slotCode);
    row.setFileId(fileId);
    row.setFilePath(StringUtils.hasText(filePath) ? filePath : "/api/files/" + fileId + "/download");
    row.setFileName(fileName);
    row.setUploadedBy(userId);
    row.setTenantId(tenantId);
    row.setDeleted(0);
    slotFileMapper.insert(row);
  }

  private String resolveUpstreamHint(String slotCode, String renderer) {
    if ("follow_photo".equals(renderer) || "photo_album".equals(slotCode)) {
      return "跟班·留痕培训照片册";
    }
    if ("cert_pdf_bundle".equals(slotCode) || "file_copy".equals(renderer) || "pdf_merge".equals(renderer)) {
      return "结业·本班证书 PDF";
    }
    if ("claim_roster".equals(slotCode) || "cert_roster".equals(slotCode)
        || "cert_pass".equals(slotCode) || "cert_retake".equals(slotCode)) {
      return "结业·获证人员名册";
    }
    if ("syllabus".equals(slotCode) || "train_plan".equals(slotCode)) {
      return "开班材料（可手工上传）";
    }
    if ("apply_form".equals(slotCode)) {
      return "一键生成：个人技能提升补贴申领表（2025）";
    }
    return "需盖章或人工制作后上传";
  }

  private String resolveClaimFormSlot(Long profileId) {
    List<TrainSubsidyPackSlotEntity> slots = profileService.listSlots(profileId);
    for (TrainSubsidyPackSlotEntity slot : slots) {
      if (CLAIM_FORM_SLOT.equals(slot.getSlotCode())) {
        return CLAIM_FORM_SLOT;
      }
    }
    for (TrainSubsidyPackSlotEntity slot : slots) {
      String code = slot.getSlotCode() != null ? slot.getSlotCode().toLowerCase() : "";
      if (code.contains("apply") || code.contains("claim_form")) {
        return slot.getSlotCode();
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "配方中未配置「补贴申请表」槽位（apply_form）");
  }

  private byte[] loadClaimFormTemplate() {
    try {
      ClassPathResource resource = new ClassPathResource(CLAIM_FORM_TEMPLATE);
      if (!resource.exists()) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "申领表模板不存在");
      }
      try (InputStream in = resource.getInputStream()) {
        return in.readAllBytes();
      }
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取申领表模板失败: " + ex.getMessage());
    }
  }

  private void clearClaimFormAutoFiles(Long subsidyId, String slotCode) {
    List<TrainSubsidySlotFileEntity> existing = slotFileMapper.selectList(
        new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidyId)
            .eq(TrainSubsidySlotFileEntity::getSlotCode, slotCode)
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0));
    for (TrainSubsidySlotFileEntity f : existing) {
      String name = f.getFileName();
      if (name != null && name.startsWith(AUTO_PREFIX) && name.contains(CLAIM_FORM_TITLE)) {
        slotFileMapper.deleteById(f.getId());
      }
    }
  }

  private Map<Long, TrainCertificateEntity> loadCertsByPeople(Long classId, Long tenantId) {
    Map<Long, TrainCertificateEntity> map = new HashMap<>();
    for (TrainCertificateEntity c : listClassCerts(classId, tenantId)) {
      if (c.getPeopleId() != null) {
        map.putIfAbsent(c.getPeopleId(), c);
      }
    }
    return map;
  }

  /** @deprecated 使用 {@link SubsidyCertifiedPeopleSupport#isCertified} */
  private static boolean isBackfilledCertificate(TrainCertificateEntity c) {
    return SubsidyCertifiedPeopleSupport.isCertified(c);
  }

  private Map<String, TrainSignUpPeopleSelfEntity> loadSignUpsByIdCard(List<TrainPeopleListEntity> students) {
    List<String> idCards = students.stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    Map<String, TrainSignUpPeopleSelfEntity> map = new HashMap<>();
    if (idCards.isEmpty()) {
      return map;
    }
    signUpPeopleSelfMapper.selectList(
            new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
                .in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards)
                .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
                .orderByDesc(TrainSignUpPeopleSelfEntity::getId))
        .forEach(p -> {
          if (StringUtils.hasText(p.getIdCardNumber())) {
            map.putIfAbsent(p.getIdCardNumber().trim(), p);
          }
        });
    return map;
  }

  private Map<String, PlatformUserDetailEntity> loadPlatformDetailsByIdCard(
      List<TrainPeopleListEntity> students) {
    List<String> idCards = students.stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    Map<String, PlatformUserDetailEntity> map = new HashMap<>();
    if (idCards.isEmpty()) {
      return map;
    }
    platformUserDetailMapper.selectList(
            new LambdaQueryWrapper<PlatformUserDetailEntity>()
                .in(PlatformUserDetailEntity::getIdcard, idCards)
                .eq(PlatformUserDetailEntity::getDeleted, 0))
        .forEach(p -> {
          if (StringUtils.hasText(p.getIdcard())) {
            map.putIfAbsent(p.getIdcard().trim(), p);
          }
        });
    return map;
  }

  private String resolveWorkTypeTitle(Long workTypeId) {
    if (workTypeId == null || workTypeId <= 0) {
      return "";
    }
    ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(workTypeId.intValue());
    return wt != null && StringUtils.hasText(wt.getTitle()) ? wt.getTitle().trim() : "";
  }

  private String resolveOccupationTitle(Long workTypeId, String workTypeTitle) {
    if (workTypeId == null || workTypeId <= 0) {
      return workTypeTitle;
    }
    ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(workTypeId.intValue());
    if (wt == null) {
      return workTypeTitle;
    }
    if (wt.getParentId() != null && wt.getParentId() > 0) {
      ExamWorkTypeEntity parent = examWorkTypeMapper.selectById(wt.getParentId());
      if (parent != null && StringUtils.hasText(parent.getTitle())) {
        return parent.getTitle().trim();
      }
    }
    return StringUtils.hasText(wt.getTitle()) ? wt.getTitle().trim() : workTypeTitle;
  }

  private static String formatLevelLabel(Integer workTypeLevel) {
    if (workTypeLevel == null || workTypeLevel < 1 || workTypeLevel >= LEVEL_LABELS.length) {
      return "";
    }
    return LEVEL_LABELS[workTypeLevel];
  }

  private static String formatLevelText(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String v = raw.trim();
    if (v.contains("/")) {
      return v;
    }
    if (v.matches("[1-5]")) {
      return formatLevelLabel(Integer.parseInt(v));
    }
    if (v.contains("初级") || v.contains("五级")) {
      return LEVEL_LABELS[1];
    }
    if (v.contains("中级") || v.contains("四级")) {
      return LEVEL_LABELS[2];
    }
    if (v.contains("高级工") || v.contains("三级")) {
      return LEVEL_LABELS[3];
    }
    if (v.contains("高级技师") || v.contains("一级")) {
      return LEVEL_LABELS[5];
    }
    if (v.contains("技师") || v.contains("二级")) {
      return LEVEL_LABELS[4];
    }
    return v;
  }

  private static String formatIssueDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String digits = raw.trim().replaceAll("\\D", "");
    if (digits.length() >= 8) {
      return digits.substring(0, 8);
    }
    return raw.trim();
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v.trim();
  }

  private static String sanitizeFileName(String name) {
    if (!StringUtils.hasText(name)) {
      return "学员";
    }
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private List<TrainCertificateEntity> listClassCerts(Long classId, Long tenantId) {
    return certificateMapper.selectList(new LambdaQueryWrapper<TrainCertificateEntity>()
        .eq(TrainCertificateEntity::getClassId, classId)
        .eq(TrainCertificateEntity::getTenantId, tenantId)
        .eq(TrainCertificateEntity::getDeleted, 0)
        .orderByAsc(TrainCertificateEntity::getId));
  }

  private Map<Long, TrainPeopleListEntity> loadPeopleMap(Long classId, Long tenantId) {
    return peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getDeleted, 0))
        .stream()
        .collect(Collectors.toMap(TrainPeopleListEntity::getId, p -> p, (a, b) -> a));
  }

  private TrainFollowPhotoPackEntity latestPhotoPack(Long classId, Long tenantId) {
    return photoPackMapper.selectOne(new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
        .eq(TrainFollowPhotoPackEntity::getClassId, classId)
        .eq(TrainFollowPhotoPackEntity::getTenantId, tenantId)
        .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
        .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
        .last("LIMIT 1"));
  }

  private TrainSubsidyEntity requireSubsidy(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSubsidyEntity entity = subsidyMapper.selectOne(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getId, id)
        .eq(TrainSubsidyEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "补贴单不存在");
    }
    return entity;
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, classId)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private void assertEditable(TrainSubsidyEntity entity) {
    String phase = entity.getPhase();
    if (!"draft".equals(phase) && !"checking".equals(phase) && !"ready".equals(phase) && !"rejected".equals(phase)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可刷新材料，请先退回核验/草稿");
    }
  }

  static Long parseFileId(String path) {
    if (!StringUtils.hasText(path)) return null;
    Matcher m = FILE_ID.matcher(path.trim());
    if (m.find()) {
      try {
        return Long.parseLong(m.group(1));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private static String firstNonBlank(String... vals) {
    for (String v : vals) {
      if (StringUtils.hasText(v)) return v.trim();
    }
    return null;
  }

  private void addEvent(TrainSubsidyEntity entity, String action, String remark, Object payload) {
    TrainSubsidyEventEntity e = new TrainSubsidyEventEntity();
    e.setSubsidyId(entity.getId());
    e.setAction(action);
    e.setFromPhase(entity.getPhase());
    e.setToPhase(entity.getPhase());
    e.setRemark(remark);
    e.setOperatorId(SecurityContext.currentUser().getUserId());
    e.setTenantId(entity.getTenantId());
    e.setDeleted(0);
    e.setCreatedAt(LocalDateTime.now());
    if (payload != null) {
      try {
        e.setPayloadJson(objectMapper.writeValueAsString(payload));
      } catch (Exception ignored) {
      }
    }
    eventMapper.insert(e);
  }
}

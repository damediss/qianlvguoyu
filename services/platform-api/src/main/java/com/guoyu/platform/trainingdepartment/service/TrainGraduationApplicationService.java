package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.GraduationCertificateTemplateReq;
import com.guoyu.platform.trainingdepartment.dto.GraduationGraduateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainGraduationArchiveEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainGraduationNoticeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainGraduationNoticeItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationArchiveMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationNoticeItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationNoticeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 结业与毕业典礼（O3）：班级粒度打标、站内信通知、台账导出、归档包、证书轻关联。
 */
@Service
@Slf4j
public class TrainGraduationApplicationService {

  private static final int STATUS_PENDING = 0;
  private static final int STATUS_PASSED = 1;
  private static final int STATUS_FAILED = 2;
  private static final Set<Integer> PROCESSED = Set.of(STATUS_PASSED, STATUS_FAILED);
  private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainFileTempMapper fileTempMapper;
  private final TrainGraduationNoticeMapper noticeMapper;
  private final TrainGraduationNoticeItemMapper noticeItemMapper;
  private final TrainGraduationArchiveMapper archiveMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final FileApplicationService fileApplicationService;

  public TrainGraduationApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainCertificateMapper certificateMapper,
      TrainFileTempMapper fileTempMapper,
      TrainGraduationNoticeMapper noticeMapper,
      TrainGraduationNoticeItemMapper noticeItemMapper,
      TrainGraduationArchiveMapper archiveMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      FileApplicationService fileApplicationService) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.certificateMapper = certificateMapper;
    this.fileTempMapper = fileTempMapper;
    this.noticeMapper = noticeMapper;
    this.noticeItemMapper = noticeItemMapper;
    this.archiveMapper = archiveMapper;
    this.photoPackMapper = photoPackMapper;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> graduationCandidates(
      Integer page, Integer pageSize, String keyword, String graduateStatus) {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    LambdaQueryWrapper<TrainInfoClassEntity> q = new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getDeleted, 0)
        .orderByDesc(TrainInfoClassEntity::getId);
    if (StringUtils.hasText(keyword)) {
      q.like(TrainInfoClassEntity::getTitle, keyword.trim());
    }
    if (StringUtils.hasText(graduateStatus)) {
      q.eq(TrainInfoClassEntity::getGraduateStatus, graduateStatus.trim());
    }

    Page<TrainInfoClassEntity> mpPage = classMapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 20), q);

    List<Long> companyIds = new ArrayList<>();
    List<Long> workTypeIds = new ArrayList<>();
    List<Long> trainInfoIds = new ArrayList<>();
    for (TrainInfoClassEntity c : mpPage.getRecords()) {
      if (c.getCompanyId() != null) companyIds.add(c.getCompanyId());
      if (c.getWorkTypeId() != null) workTypeIds.add(c.getWorkTypeId());
      if (c.getTrainInfoId() != null) trainInfoIds.add(c.getTrainInfoId());
    }

    Map<Long, String> companyNames = loadCompanyNames(companyIds);
    Map<Long, String> workTypeNames = loadWorkTypeNames(workTypeIds);
    Map<Long, TrainInfoListEntity> infoMap = loadInfoMap(trainInfoIds);

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainInfoClassEntity c : mpPage.getRecords()) {
      List<TrainPeopleListEntity> students = listStudents(c.getId());
      int total = students.size();
      int graduated = (int) students.stream()
          .filter(s -> PROCESSED.contains(nzStatus(s.getGraduateStatus())))
          .count();
      TrainInfoListEntity info = infoMap.get(c.getTrainInfoId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", c.getId());
      item.put("title", c.getTitle());
      item.put("trainInfoId", c.getTrainInfoId());
      item.put("trainTitle", info != null ? info.getTitle() : "");
      item.put("companyId", c.getCompanyId());
      item.put("companyName", companyNames.getOrDefault(c.getCompanyId(), ""));
      item.put("workTypeId", c.getWorkTypeId());
      item.put("workTypeName", workTypeNames.getOrDefault(c.getWorkTypeId(), ""));
      item.put("studentCount", total);
      item.put("graduatedCount", graduated);
      item.put("graduateStatus", normalizeClassStatus(c.getGraduateStatus()));
      item.put("graduateAt", c.getGraduateAt());
      item.put("certificateTempId", c.getCertificateTempId());
      item.put("dateStart", c.getDateStart());
      item.put("dateEnd", c.getDateEnd());
      item.put("tenantId", tenantId);
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> graduationStudents(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    List<TrainPeopleListEntity> students = listStudents(classId);

    String templateTitle = null;
    if (clazz.getCertificateTempId() != null) {
      TrainFileTempEntity temp = fileTempMapper.selectById(clazz.getCertificateTempId());
      if (temp != null && (temp.getDeleted() == null || temp.getDeleted() == 0)) {
        templateTitle = temp.getTitle();
      }
    }

    Map<Long, TrainCertificateEntity> certByPeople = loadCertsByPeople(classId);

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      boolean qualified = isQualified(s);
      int st = nzStatus(s.getGraduateStatus());
      TrainCertificateEntity cert = certByPeople.get(s.getId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", s.getId());
      item.put("realname", s.getRealname());
      item.put("phone", s.getPhone());
      item.put("idCard", s.getIdCard());
      // 与考试模块（人社成绩）对齐：理论 / 技能 / 综合 / 工作业绩
      item.put("theoryScore", s.getTheoryScore());
      item.put("skillScore", s.getRealScore());
      item.put("examScore", s.getExamScore());
      item.put("performanceScore", s.getPerformanceScore());
      item.put("normalScore", s.getNormalScore());
      item.put("realScore", s.getRealScore());
      item.put("isLeader", s.getIsLeader());
      item.put("isCommittee", s.getIsCommittee());
      item.put("graduateStatus", st);
      item.put("graduateStatusLabel", statusLabel(st));
      item.put("graduateAt", s.getGraduateAt());
      item.put("qualified", qualified);
      item.put("qualifyHint", qualifyHint(s, qualified));
      item.put("examResultStatus", resolveExamResultStatus(s));
      item.put("templateTitle", templateTitle);
      item.put("certificateTempId", clazz.getCertificateTempId());
      item.put("certificateNo", cert != null ? cert.getCertificateNo() : null);
      item.put("certStatus", cert != null ? cert.getStatus() : null);
      item.put("hasCertificate", cert != null);
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classTitle", clazz.getTitle());
    result.put("graduateStatus", normalizeClassStatus(clazz.getGraduateStatus()));
    result.put("certificateTempId", clazz.getCertificateTempId());
    result.put("templateTitle", templateTitle);
    result.put("list", list);
    result.put("total", list.size());
    return result;
  }

  @Transactional
  public Map<String, Object> graduate(Long classId, GraduationGraduateReq req) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long operatorId = SecurityContext.currentUser().getUserId();
    LocalDateTime now = LocalDateTime.now();
    boolean allowUnqualified = req == null || req.getAllowUnqualified() == null || Boolean.TRUE.equals(req.getAllowUnqualified());
    String mode = req != null && StringUtils.hasText(req.getMode()) ? req.getMode().trim() : "all";
    String remark = req != null ? trimToNull(req.getRemark()) : null;

    List<TrainPeopleListEntity> students = listStudents(classId);
    Set<Long> selected = new HashSet<>();
    if ("selected".equalsIgnoreCase(mode)) {
      if (req == null || req.getPeopleIds() == null || req.getPeopleIds().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要结业的学员");
      }
      selected.addAll(req.getPeopleIds());
    }

    int changed = 0;
    int skipped = 0;
    for (TrainPeopleListEntity s : students) {
      if ("selected".equalsIgnoreCase(mode) && !selected.contains(s.getId())) {
        continue;
      }
      int cur = nzStatus(s.getGraduateStatus());
      if (PROCESSED.contains(cur)) {
        skipped++;
        continue;
      }
      boolean qualified = isQualified(s);
      int target = qualified ? STATUS_PASSED : STATUS_FAILED;
      if (!qualified && !allowUnqualified) {
        skipped++;
        continue;
      }
      s.setGraduateStatus(target);
      s.setGraduateAt(now);
      s.setGraduateBy(operatorId);
      s.setGraduateRemark(remark);
      peopleListMapper.updateById(s);
      changed++;
    }

    refreshClassGraduateStatus(clazz);
    maybeCompleteTrainInfo(clazz.getTrainInfoId());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("trainInfoId", clazz.getTrainInfoId());
    result.put("changedCount", changed);
    result.put("skippedCount", skipped);
    result.put("graduateStatus", normalizeClassStatus(clazz.getGraduateStatus()));
    result.put("graduateCount", changed);
    result.put("status", "completed");
    return result;
  }

  @Transactional
  public Map<String, Object> graduationNotify(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long operatorId = SecurityContext.currentUser().getUserId();
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainPeopleListEntity> students = listStudents(classId).stream()
        .filter(s -> PROCESSED.contains(nzStatus(s.getGraduateStatus())))
        .toList();
    if (students.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成结业打标再发送通知");
    }

    String title = "结业通知：" + (clazz.getTitle() != null ? clazz.getTitle() : ("班级" + classId));
    String body = "您好，班级「" + nullToDash(clazz.getTitle()) + "」已办理结业手续，请知悉。时间："
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

    TrainGraduationNoticeEntity notice = new TrainGraduationNoticeEntity();
    notice.setTenantId(tenantId);
    notice.setClassId(classId);
    notice.setChannel("inbox");
    notice.setTitle(title);
    notice.setBody(body);
    notice.setTotalCount(students.size());
    notice.setSuccessCount(0);
    notice.setFailCount(0);
    notice.setCreatedBy(operatorId);
    notice.setUpdatedBy(operatorId);
    notice.setDeleted(0);
    noticeMapper.insert(notice);

    int success = 0;
    int fail = 0;
    for (TrainPeopleListEntity s : students) {
      TrainGraduationNoticeItemEntity item = new TrainGraduationNoticeItemEntity();
      item.setTenantId(tenantId);
      item.setNoticeId(notice.getId());
      item.setPeopleId(s.getId());
      item.setPhone(s.getPhone());
      item.setCreatedBy(operatorId);
      item.setUpdatedBy(operatorId);
      item.setDeleted(0);

      Long uid = s.getPlatformUserId() != null ? s.getPlatformUserId() : s.getUserId();
      item.setPlatformUserId(uid);
      if (uid == null) {
        item.setStatus("skip");
        item.setFailReason("no_user");
        fail++;
      } else {
        // 站内信落库即视为投递成功（App 收件箱可二期消费本表）
        item.setStatus("success");
        success++;
      }
      noticeItemMapper.insert(item);
    }

    notice.setSuccessCount(success);
    notice.setFailCount(fail);
    notice.setUpdatedBy(operatorId);
    noticeMapper.updateById(notice);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("noticeId", notice.getId());
    result.put("notifiedCount", success);
    result.put("successCount", success);
    result.put("failCount", fail);
    result.put("totalCount", students.size());
    result.put("message", "已发送站内信：成功 " + success + "，失败/跳过 " + fail);
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listNotices(Long classId) {
    requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainGraduationNoticeEntity> rows = noticeMapper.selectList(
        new LambdaQueryWrapper<TrainGraduationNoticeEntity>()
            .eq(TrainGraduationNoticeEntity::getTenantId, tenantId)
            .eq(TrainGraduationNoticeEntity::getClassId, classId)
            .eq(TrainGraduationNoticeEntity::getDeleted, 0)
            .orderByDesc(TrainGraduationNoticeEntity::getCreatedAt));
    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainGraduationNoticeEntity n : rows) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", n.getId());
      m.put("channel", n.getChannel());
      m.put("title", n.getTitle());
      m.put("body", n.getBody());
      m.put("totalCount", n.getTotalCount());
      m.put("successCount", n.getSuccessCount());
      m.put("failCount", n.getFailCount());
      m.put("createdAt", n.getCreatedAt());
      list.add(m);
    }
    return list;
  }

  @Transactional
  public Map<String, Object> bindCertificateTemplate(Long classId, GraduationCertificateTemplateReq req) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tempId = req != null ? req.getCertificateTempId() : null;
    if (tempId != null) {
      TrainFileTempEntity temp = fileTempMapper.selectById(tempId);
      if (temp == null || (temp.getDeleted() != null && temp.getDeleted() != 0)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "证书模板不存在");
      }
    }
    clazz.setCertificateTempId(tempId);
    classMapper.updateById(clazz);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("certificateTempId", tempId);
    return out;
  }

  @Transactional
  public Map<String, Object> batchIssueCertificates(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    if (clazz.getCertificateTempId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先绑定结业证书模板");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long operatorId = SecurityContext.currentUser().getUserId();
    String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    List<TrainPeopleListEntity> passed = listStudents(classId).stream()
        .filter(s -> nzStatus(s.getGraduateStatus()) == STATUS_PASSED)
        .toList();
    Map<Long, TrainCertificateEntity> existing = loadCertsByPeople(classId);
    int created = 0;
    int skipped = 0;
    for (TrainPeopleListEntity s : passed) {
      if (existing.containsKey(s.getId())) {
        skipped++;
        continue;
      }
      TrainCertificateEntity cert = new TrainCertificateEntity();
      cert.setTenantId(tenantId);
      cert.setProjectId(clazz.getTrainInfoId());
      cert.setClassId(classId);
      cert.setPeopleId(s.getId());
      cert.setTemplateId(clazz.getCertificateTempId());
      cert.setUserId(s.getPlatformUserId() != null ? s.getPlatformUserId() : s.getUserId());
      cert.setUserName(s.getRealname());
      cert.setCertificateNo("GC-" + classId + "-" + s.getId());
      cert.setCertificateType("graduation");
      cert.setIssueDate(today);
      cert.setStatus(1);
      cert.setCreatedBy(operatorId);
      cert.setDeleted(0);
      certificateMapper.insert(cert);
      created++;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("created", created);
    out.put("skipped", skipped);
    return out;
  }

  @Transactional
  public Map<String, Object> createArchive(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long operatorId = SecurityContext.currentUser().getUserId();

    Long maxVer = archiveMapper.selectCount(
        new LambdaQueryWrapper<TrainGraduationArchiveEntity>()
            .eq(TrainGraduationArchiveEntity::getTenantId, tenantId)
            .eq(TrainGraduationArchiveEntity::getClassId, classId)
            .eq(TrainGraduationArchiveEntity::getDeleted, 0));
    int version = (maxVer == null ? 0 : maxVer.intValue()) + 1;

    byte[] zipBytes;
    try {
      zipBytes = buildArchiveZip(clazz, version);
    } catch (Exception ex) {
      log.warn("结业归档失败 classId={}", classId, ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "归档生成失败: " + ex.getMessage());
    }

    String fileName = "graduation-" + classId + "-v" + version + ".zip";
    FileVO stored = fileApplicationService.storeFromBytes(zipBytes, fileName, "application/zip", "private");
    String path = "/api/files/" + stored.id() + "/download";

    TrainGraduationArchiveEntity archive = new TrainGraduationArchiveEntity();
    archive.setTenantId(tenantId);
    archive.setClassId(classId);
    archive.setFileId(stored.id());
    archive.setFilePath(path);
    archive.setVersionNo(version);
    archive.setRemark("结业材料归档 v" + version);
    archive.setCreatedBy(operatorId);
    archive.setUpdatedBy(operatorId);
    archive.setDeleted(0);
    archiveMapper.insert(archive);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", archive.getId());
    out.put("classId", classId);
    out.put("fileId", stored.id());
    out.put("filePath", path);
    out.put("versionNo", version);
    return out;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listArchives(Long classId) {
    requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return archiveMapper.selectList(
            new LambdaQueryWrapper<TrainGraduationArchiveEntity>()
                .eq(TrainGraduationArchiveEntity::getTenantId, tenantId)
                .eq(TrainGraduationArchiveEntity::getClassId, classId)
                .eq(TrainGraduationArchiveEntity::getDeleted, 0)
                .orderByDesc(TrainGraduationArchiveEntity::getVersionNo))
        .stream()
        .map(a -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", a.getId());
          m.put("fileId", a.getFileId());
          m.put("filePath", a.getFilePath());
          m.put("versionNo", a.getVersionNo());
          m.put("remark", a.getRemark());
          m.put("createdAt", a.getCreatedAt());
          return m;
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportLedger(Long classId) {
    List<TrainInfoClassEntity> classes;
    if (classId != null) {
      classes = List.of(requireClass(classId));
    } else {
      classes = classMapper.selectList(new LambdaQueryWrapper<TrainInfoClassEntity>()
          .eq(TrainInfoClassEntity::getDeleted, 0)
          .orderByDesc(TrainInfoClassEntity::getId)
          .last("LIMIT 200"));
    }

    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("结业台账");
      String[] headers = {
          "班级ID", "班级名称", "班级结业状态", "姓名", "手机", "身份证",
          "理论", "技能", "综合", "工作业绩", "学员结业状态", "结业时间", "证书编号", "是否达标"
      };
      Row head = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        head.createCell(i).setCellValue(headers[i]);
      }
      int r = 1;
      for (TrainInfoClassEntity clazz : classes) {
        Map<Long, TrainCertificateEntity> certs = loadCertsByPeople(clazz.getId());
        for (TrainPeopleListEntity s : listStudents(clazz.getId())) {
          Row row = sheet.createRow(r++);
          int c = 0;
          row.createCell(c++).setCellValue(clazz.getId());
          row.createCell(c++).setCellValue(nullToDash(clazz.getTitle()));
          row.createCell(c++).setCellValue(normalizeClassStatus(clazz.getGraduateStatus()));
          row.createCell(c++).setCellValue(nullToDash(s.getRealname()));
          row.createCell(c++).setCellValue(nullToDash(s.getPhone()));
          row.createCell(c++).setCellValue(nullToDash(s.getIdCard()));
          row.createCell(c++).setCellValue(scoreStr(s.getTheoryScore()));
          row.createCell(c++).setCellValue(scoreStr(s.getRealScore()));
          row.createCell(c++).setCellValue(scoreStr(s.getExamScore()));
          row.createCell(c++).setCellValue(scoreStr(s.getPerformanceScore()));
          row.createCell(c++).setCellValue(statusLabel(nzStatus(s.getGraduateStatus())));
          row.createCell(c++).setCellValue(s.getGraduateAt() == null ? "" : s.getGraduateAt().toString());
          TrainCertificateEntity cert = certs.get(s.getId());
          row.createCell(c++).setCellValue(cert != null ? nullToDash(cert.getCertificateNo()) : "");
          row.createCell(c).setCellValue(isQualified(s) ? "是" : "否");
        }
      }
      wb.write(bos);
      String name = URLEncoder.encode(
          "结业台账_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(bos.toByteArray());
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出台账失败: " + ex.getMessage());
    }
  }

  // ---------- helpers ----------

  private byte[] buildArchiveZip(TrainInfoClassEntity clazz, int version) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(bos); XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("结业名单");
      Row head = sheet.createRow(0);
      String[] hs = {"姓名", "手机", "身份证", "理论", "技能", "综合", "工作业绩", "结业状态", "结业时间", "是否达标"};
      for (int i = 0; i < hs.length; i++) {
        head.createCell(i).setCellValue(hs[i]);
      }
      int r = 1;
      for (TrainPeopleListEntity s : listStudents(clazz.getId())) {
        Row row = sheet.createRow(r++);
        int c = 0;
        row.createCell(c++).setCellValue(nullToDash(s.getRealname()));
        row.createCell(c++).setCellValue(nullToDash(s.getPhone()));
        row.createCell(c++).setCellValue(nullToDash(s.getIdCard()));
        row.createCell(c++).setCellValue(scoreStr(s.getTheoryScore()));
        row.createCell(c++).setCellValue(scoreStr(s.getRealScore()));
        row.createCell(c++).setCellValue(scoreStr(s.getExamScore()));
        row.createCell(c++).setCellValue(scoreStr(s.getPerformanceScore()));
        row.createCell(c++).setCellValue(statusLabel(nzStatus(s.getGraduateStatus())));
        row.createCell(c++).setCellValue(s.getGraduateAt() == null ? "" : s.getGraduateAt().toString());
        row.createCell(c).setCellValue(isQualified(s) ? "是" : "否");
      }
      ByteArrayOutputStream xls = new ByteArrayOutputStream();
      wb.write(xls);
      zos.putNextEntry(new ZipEntry("结业名单.xlsx"));
      zos.write(xls.toByteArray());
      zos.closeEntry();

      TrainFollowPhotoPackEntity pack = photoPackMapper.selectOne(
          new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
              .eq(TrainFollowPhotoPackEntity::getClassId, clazz.getId())
              .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
              .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
              .last("LIMIT 1"));
      String note = "班级：" + nullToDash(clazz.getTitle()) + "\n归档版本：v" + version + "\n";
      if (pack != null && pack.getFileId() != null) {
        note += "培训照片包 fileId=" + pack.getFileId() + "\n";
      } else {
        note += "暂无培训照片包\n";
      }
      zos.putNextEntry(new ZipEntry("材料说明.txt"));
      zos.write(note.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return bos.toByteArray();
  }

  private void refreshClassGraduateStatus(TrainInfoClassEntity clazz) {
    List<TrainPeopleListEntity> students = listStudents(clazz.getId());
    String status = "pending";
    if (!students.isEmpty()) {
      long processed = students.stream()
          .filter(s -> PROCESSED.contains(nzStatus(s.getGraduateStatus())))
          .count();
      if (processed == 0) {
        status = "pending";
      } else if (processed < students.size()) {
        status = "partial";
      } else {
        status = "done";
      }
    } else {
      status = "done";
    }
    clazz.setGraduateStatus(status);
    if ("done".equals(status)) {
      clazz.setGraduateAt(LocalDateTime.now());
    }
    classMapper.updateById(clazz);
  }

  private void maybeCompleteTrainInfo(Long trainInfoId) {
    if (trainInfoId == null) {
      return;
    }
    List<TrainInfoClassEntity> classes = classMapper.selectList(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getTrainInfoId, trainInfoId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classes.isEmpty()) {
      return;
    }
    boolean allDone = classes.stream()
        .allMatch(c -> "done".equals(normalizeClassStatus(c.getGraduateStatus())));
    if (!allDone) {
      return;
    }
    TrainInfoListEntity info = infoListMapper.selectById(trainInfoId);
    if (info == null) {
      return;
    }
    if (info.getStatus() == null || info.getStatus() != 2) {
      info.setStatus(2);
      infoListMapper.updateById(info);
    }
  }

  /**
   * 与考试模块一致：理论≥60 且 技能≥60 为达标；
   * 理论与技能皆无时本期不拦结业（仍标「暂无成绩」）。
   */
  private boolean isQualified(TrainPeopleListEntity s) {
    BigDecimal theory = s.getTheoryScore();
    BigDecimal skill = s.getRealScore();
    if (theory == null && skill == null) {
      return true;
    }
    return theory != null
        && skill != null
        && theory.compareTo(PASS_SCORE) >= 0
        && skill.compareTo(PASS_SCORE) >= 0;
  }

  private String resolveExamResultStatus(TrainPeopleListEntity s) {
    BigDecimal theory = s.getTheoryScore();
    BigDecimal skill = s.getRealScore();
    if (theory == null && skill == null) {
      return "ABSENT";
    }
    if (theory != null
        && skill != null
        && theory.compareTo(PASS_SCORE) >= 0
        && skill.compareTo(PASS_SCORE) >= 0) {
      return "PASS";
    }
    return "FAIL";
  }

  private String qualifyHint(TrainPeopleListEntity s, boolean qualified) {
    if (s.getTheoryScore() == null && s.getRealScore() == null) {
      return "暂无成绩（本期不强制）";
    }
    if (qualified) {
      return "达标";
    }
    return "未达标（理论与技能需均≥60）";
  }

  private Map<Long, TrainCertificateEntity> loadCertsByPeople(Long classId) {
    Map<Long, TrainCertificateEntity> map = new HashMap<>();
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .eq(TrainCertificateEntity::getClassId, classId)
            .eq(TrainCertificateEntity::getDeleted, 0));
    for (TrainCertificateEntity c : rows) {
      if (c.getPeopleId() != null) {
        map.putIfAbsent(c.getPeopleId(), c);
      }
    }
    return map;
  }

  private List<TrainPeopleListEntity> listStudents(Long classId) {
    return peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByAsc(TrainPeopleListEntity::getId));
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private Map<Long, String> loadCompanyNames(List<Long> companyIds) {
    Map<Long, String> companyNames = new HashMap<>();
    if (!companyIds.isEmpty()) {
      for (OrgEntity o : orgMapper.selectBatchIds(companyIds)) {
        companyNames.put(o.getId().longValue(), o.getName());
      }
    }
    return companyNames;
  }

  private Map<Long, String> loadWorkTypeNames(List<Long> workTypeIds) {
    Map<Long, String> workTypeNames = new HashMap<>();
    if (!workTypeIds.isEmpty()) {
      for (ExamWorkTypeEntity w : examWorkTypeMapper.selectBatchIds(workTypeIds)) {
        workTypeNames.put(w.getId().longValue(), w.getTitle());
      }
    }
    return workTypeNames;
  }

  private Map<Long, TrainInfoListEntity> loadInfoMap(List<Long> trainInfoIds) {
    Map<Long, TrainInfoListEntity> map = new HashMap<>();
    if (trainInfoIds.isEmpty()) {
      return map;
    }
    for (TrainInfoListEntity i : infoListMapper.selectBatchIds(trainInfoIds)) {
      map.put(i.getId(), i);
    }
    return map;
  }

  private static int nzStatus(Integer status) {
    return status == null ? STATUS_PENDING : status;
  }

  private static String statusLabel(int status) {
    return switch (status) {
      case STATUS_PASSED -> "已结业";
      case STATUS_FAILED -> "不合格结业";
      default -> "未结业";
    };
  }

  private static String normalizeClassStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "pending";
    }
    return status.trim();
  }

  private static String nullToDash(String v) {
    return StringUtils.hasText(v) ? v : "";
  }

  private static String scoreStr(BigDecimal v) {
    return v == null ? "" : v.stripTrailingZeros().toPlainString();
  }

  private static String trimToNull(String v) {
    if (!StringUtils.hasText(v)) {
      return null;
    }
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}

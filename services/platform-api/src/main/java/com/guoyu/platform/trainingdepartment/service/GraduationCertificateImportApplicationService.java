package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.GraduationCertImportCommitReq;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateImportBatchEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateImportBatchMapper;
import com.guoyu.platform.trainingdepartment.support.RensheSkillCertPdfParser;
import com.guoyu.platform.trainingdepartment.support.RensheSkillCertPdfParser.Parsed;
import com.guoyu.platform.trainingdepartment.support.RensheSkillCertOcrSupport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 毕业典礼：证书文件夹上传 → 预览匹配（身份证）→ 确认回填证面原文 + 文件。
 */
@Service
@Slf4j
public class GraduationCertificateImportApplicationService {

  private static final String CERT_TYPE = "skill_level_cert";
  private static final long PREVIEW_TTL_SECONDS = 2 * 60 * 60;

  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainCertificateImportBatchMapper importBatchMapper;
  private final FileApplicationService fileApplicationService;
  private final RensheSkillCertOcrSupport ocrSupport;
  private final ObjectMapper objectMapper;
  private final Executor certImportExecutor;
  private final org.springframework.transaction.PlatformTransactionManager transactionManager;
  private final TrainGraduationApplicationService graduationApplicationService;
  private final ConcurrentHashMap<String, CachedPreview> previewCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PreviewJob> previewJobs = new ConcurrentHashMap<>();

  public GraduationCertificateImportApplicationService(
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainCertificateMapper certificateMapper,
      TrainCertificateImportBatchMapper importBatchMapper,
      FileApplicationService fileApplicationService,
      RensheSkillCertOcrSupport ocrSupport,
      ObjectMapper objectMapper,
      @Qualifier("certImportExecutor") Executor certImportExecutor,
      org.springframework.transaction.PlatformTransactionManager transactionManager,
      TrainGraduationApplicationService graduationApplicationService) {
    this.classMapper = classMapper;
    this.peopleListMapper = peopleListMapper;
    this.certificateMapper = certificateMapper;
    this.importBatchMapper = importBatchMapper;
    this.fileApplicationService = fileApplicationService;
    this.ocrSupport = ocrSupport;
    this.objectMapper = objectMapper;
    this.certImportExecutor = certImportExecutor;
    this.transactionManager = transactionManager;
    this.graduationApplicationService = graduationApplicationService;
  }

  @Transactional
  public Map<String, Object> preview(List<String> classIdRaw, List<MultipartFile> files) {
    purgeExpired();
    List<Long> classIds = normalizeClassIds(classIdRaw);
    if (classIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择班级");
    }
    if (files == null || files.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传证书文件或文件夹");
    }

    // 仅把上传字节收入内存后立刻返回任务；解压与解析放到后台，缩短「预览匹配」卡住感
    List<RawUpload> rawUploads = new ArrayList<>();
    for (MultipartFile mf : files) {
      if (mf == null || mf.isEmpty()) {
        continue;
      }
      String name = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : "upload.bin";
      try {
        rawUploads.add(new RawUpload(name, mf.getBytes()));
      } catch (Exception ex) {
        log.warn("读取证书上传文件失败 name={}", name, ex);
        rawUploads.add(new RawUpload(name, new byte[0], "PARSE_FAILED",
            "读取文件失败: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())));
      }
    }
    if (rawUploads.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传证书文件或文件夹");
    }
    return startAsyncPreview(classIds, rawUploads);
  }

  /** 查询异步预览任务状态 */
  @Transactional(readOnly = true)
  public Map<String, Object> previewJobStatus(String jobId) {
    purgeExpired();
    if (!StringUtils.hasText(jobId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "jobId 不能为空");
    }
    PreviewJob job = previewJobs.get(jobId.trim());
    if (job == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "预览任务不存在或已过期");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    if (!tenantId.equals(job.tenantId) || !userId.equals(job.userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该预览任务");
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("async", true);
    out.put("jobId", job.jobId);
    out.put("status", job.status);
    out.put("message", job.message);
    if ("done".equals(job.status) && job.result != null) {
      out.putAll(job.result);
    }
    if ("failed".equals(job.status)) {
      out.put("error", job.message);
    }
    return out;
  }

  private Map<String, Object> startAsyncPreview(List<Long> classIds, List<RawUpload> rawUploads) {
    String jobId = UUID.randomUUID().toString();
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    PreviewJob job = new PreviewJob(jobId, tenantId, userId);
    job.status = "pending";
    job.message = "排队处理中（已接收 " + rawUploads.size() + " 个上传）";
    previewJobs.put(jobId, job);

    var springCtx = org.springframework.security.core.context.SecurityContextHolder.getContext();
    var contextCopy =
        org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
    contextCopy.setAuthentication(springCtx.getAuthentication());

    certImportExecutor.execute(() -> {
      org.springframework.security.core.context.SecurityContextHolder.setContext(contextCopy);
      try {
        job.status = "running";
        job.message = "正在解压/识别文件…";
        List<ParsedFile> parsedFiles = expandRawUploads(rawUploads);
        if (parsedFiles.isEmpty()) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到可处理的证书 PDF");
        }
        job.message = "正在解析证书 0/" + parsedFiles.size();
        Map<String, Object> result = buildPreviewResult(classIds, parsedFiles, job);
        job.result = result;
        job.status = "done";
        job.message = "预览完成";
      } catch (Exception ex) {
        log.warn("异步证书预览失败 jobId={}", jobId, ex);
        job.status = "failed";
        job.message = ex.getMessage() != null ? ex.getMessage() : "预览失败";
      } finally {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
      }
    });

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("async", true);
    out.put("jobId", jobId);
    out.put("status", "pending");
    out.put("message", job.message);
    out.put("totalFiles", rawUploads.size());
    return out;
  }

  private List<ParsedFile> expandRawUploads(List<RawUpload> rawUploads) {
    List<ParsedFile> parsedFiles = new ArrayList<>();
    for (RawUpload raw : rawUploads) {
      if (raw.forceStatus != null) {
        ParsedFile fail = new ParsedFile(leafName(raw.fileName), raw.bytes != null ? raw.bytes : new byte[0]);
        fail.forceStatus = raw.forceStatus;
        fail.forceMessage = raw.forceMessage;
        parsedFiles.add(fail);
        continue;
      }
      String leaf = leafName(raw.fileName);
      byte[] bytes = raw.bytes != null ? raw.bytes : new byte[0];
      if (isZipName(leaf) || isZipMagic(bytes)) {
        parsedFiles.addAll(expandZip(leaf, bytes));
      } else if (isPdfName(leaf)) {
        parsedFiles.add(new ParsedFile(leaf, bytes));
      } else {
        ParsedFile skip = new ParsedFile(leaf, bytes);
        skip.forceStatus = "PARSE_FAILED";
        skip.forceMessage = "仅支持 PDF 或 ZIP";
        parsedFiles.add(skip);
      }
    }
    return parsedFiles;
  }

  private Map<String, Object> buildPreviewResult(
      List<Long> classIds, List<ParsedFile> parsedFiles, PreviewJob progressJob) {
    Map<Long, String> classTitles = loadClassTitles(classIds);
    RosterIndex roster = buildRosterIndex(classIds);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertLookup certLookup = preloadCertsForClasses(tenantId, roster.byIdCard);

    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    AtomicInteger done = new AtomicInteger(0);
    int total = parsedFiles.size();

    List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>(total);
    for (ParsedFile pf : parsedFiles) {
      futures.add(
          CompletableFuture.supplyAsync(
              () -> {
                var ctx = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(auth);
                org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);
                try {
                  Map<String, Object> row =
                      buildPreviewRow(pf, roster, classTitles, classIds, certLookup);
                  int n = done.incrementAndGet();
                  if (progressJob != null) {
                    progressJob.message = "正在解析证书 " + n + "/" + total;
                  }
                  return row;
                } finally {
                  org.springframework.security.core.context.SecurityContextHolder.clearContext();
                }
              },
              // 不可与 async 外层共用 certImportExecutor，否则 join 易死锁
              ForkJoinPool.commonPool()));
    }

    List<Map<String, Object>> rows = futures.stream().map(CompletableFuture::join).toList();

    // 未在已选班：批量补「其他班」提示，避免逐文件查库
    enrichOtherClassHints(rows, classIds);

    List<CachedItem> cachedItems = new ArrayList<>();
    int matched = 0;
    int blocked = 0;
    int failed = 0;

    for (int i = 0; i < rows.size(); i++) {
      Map<String, Object> row = rows.get(i);
      ParsedFile pf = parsedFiles.get(i);
      String status = String.valueOf(row.get("status"));
      boolean canCommit = Boolean.TRUE.equals(row.get("canCommit"));
      if (canCommit) {
        matched++;
        cachedItems.add(
            new CachedItem(
                (String) row.get("fileName"),
                pf.bytes,
                (Long) row.get("peopleId"),
                (Long) row.get("classId"),
                (String) row.get("certificateNo"),
                (String) row.get("parsedName"),
                (String) row.get("idDocType"),
                (String) row.get("issuingAuthority"),
                (String) row.get("occupation"),
                (String) row.get("workType"),
                (String) row.get("level"),
                (String) row.get("levelNormalized"),
                (String) row.get("evaluationOrg"),
                (String) row.get("issueDate"),
                (String) row.get("queryUrls"),
                (Long) row.get("trainInfoId")));
      } else if ("PARSE_FAILED".equals(status)) {
        failed++;
      } else {
        blocked++;
      }
    }

    String previewId = UUID.randomUUID().toString();
    Long userId = SecurityContext.currentUser().getUserId();
    previewCache.put(
        previewId,
        new CachedPreview(
            tenantId,
            userId,
            classIds,
            Instant.now().plusSeconds(PREVIEW_TTL_SECONDS),
            cachedItems));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("async", false);
    out.put("previewId", previewId);
    out.put("classIds", classIds);
    out.put("totalFiles", rows.size());
    out.put("matched", matched);
    out.put("blocked", blocked);
    out.put("failed", failed);
    out.put("items", rows);
    out.put("ocrEnabled", ocrSupport.isEnabled());
    return out;
  }

  /**
   * 确认回填：先并行落盘 PDF，再短事务批量写库。
   * 不做 OCR（预览阶段已完成识别；软字段缺失遵循宁缺毋错）。
   */
  public Map<String, Object> commit(GraduationCertImportCommitReq req) {
    purgeExpired();
    if (req == null || !StringUtils.hasText(req.getPreviewId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "previewId 不能为空");
    }
    String previewId = req.getPreviewId().trim();
    CachedPreview cached = previewCache.get(previewId);
    if (cached == null || cached.expiresAt.isBefore(Instant.now())) {
      previewCache.remove(previewId);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "预览已过期，请重新上传");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    if (!tenantId.equals(cached.tenantId) || !userId.equals(cached.userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权提交该预览");
    }
    if (cached.items.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可回填的匹配项");
    }

    // 1) 事务外并行上传文件（避免长事务锁连接）
    // 工作线程无 SecurityContext，必须显式传入 tenantId，否则会落到 tenant_id=0 导致下载 404
    final Long uploadTenantId = tenantId;
    List<CompletableFuture<StagedUpload>> uploadFutures = new ArrayList<>(cached.items.size());
    for (CachedItem item : cached.items) {
      uploadFutures.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  if (item.pdfBytes == null || item.pdfBytes.length == 0) {
                    return StagedUpload.failed(item, "文件内容为空");
                  }
                  FileVO stored =
                      fileApplicationService.storeFromBytesForTenant(
                          uploadTenantId,
                          item.pdfBytes,
                          item.fileName,
                          "application/pdf",
                          "private");
                  String path = "/api/files/" + stored.id() + "/download";
                  return StagedUpload.ok(item, stored.id(), path);
                } catch (Exception ex) {
                  log.warn("证书文件落盘失败 {}", item.fileName, ex);
                  return StagedUpload.failed(
                      item, "文件上传失败: " + (ex.getMessage() != null ? ex.getMessage() : "unknown"));
                }
              },
              ForkJoinPool.commonPool()));
    }
    List<StagedUpload> staged = uploadFutures.stream().map(CompletableFuture::join).toList();

    // 2) 预载学员与证书，短事务写库
    Set<Long> peopleIds =
        staged.stream()
            .filter(s -> s.error == null)
            .map(s -> s.item.peopleId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    Set<String> certNos =
        staged.stream()
            .filter(s -> s.error == null)
            .map(s -> s.item.certificateNo)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());

    Map<Long, TrainPeopleListEntity> peopleMap = loadPeopleByIds(peopleIds);
    Map<Long, TrainCertificateEntity> certByPeople = new HashMap<>();
    Map<String, TrainCertificateEntity> certByNo = new HashMap<>();
    preloadCertsForCommit(tenantId, peopleIds, certNos, certByPeople, certByNo);

    int[] written = {0};
    int[] updated = {0};
    int[] skipped = {0};
    List<String> errors = new ArrayList<>();
    Set<Long> touchedClassIds = new HashSet<>();
    for (StagedUpload s : staged) {
      if (s.error != null) {
        skipped[0]++;
        errors.add(s.item.fileName + ": " + s.error);
      }
    }

    org.springframework.transaction.support.TransactionTemplate tx =
        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    tx.executeWithoutResult(
        status -> {
          for (StagedUpload s : staged) {
            if (s.error != null) {
              continue;
            }
            CachedItem item = s.item;
            try {
              TrainPeopleListEntity person = peopleMap.get(item.peopleId);
              if (person == null || (person.getDeleted() != null && person.getDeleted() != 0)) {
                skipped[0]++;
                errors.add(item.fileName + ": 学员不存在");
                continue;
              }
              if (!item.classId.equals(person.getTrainClassId())) {
                skipped[0]++;
                errors.add(item.fileName + ": 学员班级已变更");
                continue;
              }

              TrainCertificateEntity byNo = certByNo.get(item.certificateNo);
              if (byNo != null
                  && (byNo.getPeopleId() == null || !byNo.getPeopleId().equals(item.peopleId))) {
                skipped[0]++;
                errors.add(item.fileName + ": 证号已被其他人占用");
                continue;
              }

              String faceName = firstNonBlank(item.parsedName, person.getRealname());
              TrainCertificateEntity existing = certByPeople.get(item.peopleId);
              if (existing == null) {
                TrainCertificateEntity cert = new TrainCertificateEntity();
                cert.setTenantId(tenantId);
                cert.setProjectId(item.trainInfoId);
                cert.setClassId(item.classId);
                cert.setPeopleId(item.peopleId);
                cert.setUserId(
                    person.getPlatformUserId() != null
                        ? person.getPlatformUserId()
                        : person.getUserId());
                cert.setUserName(faceName);
                cert.setIdDocType(blankToNull(item.idDocType));
                cert.setCertificateNo(item.certificateNo);
                cert.setCertificateType(CERT_TYPE);
                cert.setOccupation(blankToNull(item.occupation));
                cert.setWorkType(blankToNull(item.workType));
                cert.setLevel(blankToNull(item.level));
                cert.setLevelNormalized(blankToNull(item.levelNormalized));
                cert.setIssuingAuthority(blankToNull(item.issuingAuthority));
                cert.setEvaluationOrg(blankToNull(item.evaluationOrg));
                cert.setIssueDate(blankToNull(item.issueDate));
                cert.setQueryUrls(blankToNull(item.queryUrls));
                cert.setFilePath(s.filePath);
                cert.setFileId(s.fileId);
                cert.setStatus(1);
                cert.setCreatedBy(userId);
                cert.setDeleted(0);
                certificateMapper.insert(cert);
                certByPeople.put(item.peopleId, cert);
                if (StringUtils.hasText(item.certificateNo)) {
                  certByNo.put(item.certificateNo, cert);
                }
                written[0]++;
                touchedClassIds.add(item.classId);
              } else {
                String oldNo = existing.getCertificateNo();
                boolean fake = oldNo != null && oldNo.startsWith("GC-");
                boolean same = item.certificateNo.equals(oldNo);
                if (!fake && !same && StringUtils.hasText(oldNo)) {
                  skipped[0]++;
                  errors.add(item.fileName + ": 已有不同真实证号 " + oldNo);
                  continue;
                }
                existing.setCertificateNo(item.certificateNo);
                existing.setCertificateType(CERT_TYPE);
                existing.setFilePath(s.filePath);
                existing.setFileId(s.fileId);
                existing.setClassId(item.classId);
                existing.setPeopleId(item.peopleId);
                if (item.trainInfoId != null) {
                  existing.setProjectId(item.trainInfoId);
                }
                if (StringUtils.hasText(faceName)) {
                  existing.setUserName(faceName);
                }
                if (StringUtils.hasText(item.idDocType)) {
                  existing.setIdDocType(item.idDocType);
                }
                if (StringUtils.hasText(item.occupation)) {
                  existing.setOccupation(item.occupation);
                }
                if (StringUtils.hasText(item.workType)) {
                  existing.setWorkType(item.workType);
                }
                if (StringUtils.hasText(item.level)) {
                  existing.setLevel(item.level);
                }
                if (StringUtils.hasText(item.levelNormalized)) {
                  existing.setLevelNormalized(item.levelNormalized);
                }
                if (StringUtils.hasText(item.issuingAuthority)) {
                  existing.setIssuingAuthority(item.issuingAuthority);
                }
                if (StringUtils.hasText(item.evaluationOrg)) {
                  existing.setEvaluationOrg(item.evaluationOrg);
                }
                if (StringUtils.hasText(item.issueDate)) {
                  existing.setIssueDate(item.issueDate);
                }
                if (StringUtils.hasText(item.queryUrls)) {
                  existing.setQueryUrls(item.queryUrls);
                }
                existing.setStatus(1);
                certificateMapper.updateById(existing);
                if (StringUtils.hasText(item.certificateNo)) {
                  certByNo.put(item.certificateNo, existing);
                }
                updated[0]++;
                touchedClassIds.add(item.classId);
              }
            } catch (Exception ex) {
              log.warn("证书回填失败 file={}", item.fileName, ex);
              skipped[0]++;
              errors.add(item.fileName + ": " + ex.getMessage());
            }
          }

          if (!touchedClassIds.isEmpty()) {
            graduationApplicationService.syncGraduationFromCertificates(touchedClassIds);
          }

          CachedPreview committed = previewCache.remove(previewId);
          saveImportBatch(
              committed, previewId, written[0], updated[0], skipped[0], errors, matchedOf(committed));
        });

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("written", written[0]);
    out.put("updated", updated[0]);
    out.put("skipped", skipped[0]);
    out.put("errors", errors);
    return out;
  }

  private Map<Long, TrainPeopleListEntity> loadPeopleByIds(Set<Long> peopleIds) {
    Map<Long, TrainPeopleListEntity> map = new HashMap<>();
    if (peopleIds == null || peopleIds.isEmpty()) {
      return map;
    }
    for (TrainPeopleListEntity p :
        peopleListMapper.selectList(
            new LambdaQueryWrapper<TrainPeopleListEntity>().in(TrainPeopleListEntity::getId, peopleIds))) {
      map.put(p.getId(), p);
    }
    return map;
  }

  private void preloadCertsForCommit(
      Long tenantId,
      Set<Long> peopleIds,
      Set<String> certNos,
      Map<Long, TrainCertificateEntity> certByPeople,
      Map<String, TrainCertificateEntity> certByNo) {
    if (peopleIds != null && !peopleIds.isEmpty()) {
      for (TrainCertificateEntity c :
          certificateMapper.selectList(
              new LambdaQueryWrapper<TrainCertificateEntity>()
                  .eq(TrainCertificateEntity::getTenantId, tenantId)
                  .eq(TrainCertificateEntity::getDeleted, 0)
                  .in(TrainCertificateEntity::getPeopleId, peopleIds))) {
        if (c.getPeopleId() != null) {
          certByPeople.putIfAbsent(c.getPeopleId(), c);
        }
        if (StringUtils.hasText(c.getCertificateNo())) {
          certByNo.putIfAbsent(c.getCertificateNo(), c);
        }
      }
    }
    if (certNos != null && !certNos.isEmpty()) {
      for (TrainCertificateEntity c :
          certificateMapper.selectList(
              new LambdaQueryWrapper<TrainCertificateEntity>()
                  .eq(TrainCertificateEntity::getTenantId, tenantId)
                  .eq(TrainCertificateEntity::getDeleted, 0)
                  .in(TrainCertificateEntity::getCertificateNo, certNos))) {
        if (StringUtils.hasText(c.getCertificateNo())) {
          certByNo.putIfAbsent(c.getCertificateNo(), c);
        }
        if (c.getPeopleId() != null) {
          certByPeople.putIfAbsent(c.getPeopleId(), c);
        }
      }
    }
  }

  private static final class StagedUpload {
    final CachedItem item;
    final Long fileId;
    final String filePath;
    final String error;

    private StagedUpload(CachedItem item, Long fileId, String filePath, String error) {
      this.item = item;
      this.fileId = fileId;
      this.filePath = filePath;
      this.error = error;
    }

    static StagedUpload ok(CachedItem item, Long fileId, String filePath) {
      return new StagedUpload(item, fileId, filePath, null);
    }

    static StagedUpload failed(CachedItem item, String error) {
      return new StagedUpload(item, null, null, error);
    }
  }

  private static int matchedOf(CachedPreview cached) {
    return cached == null || cached.items == null ? 0 : cached.items.size();
  }

  private void saveImportBatch(
      CachedPreview cached,
      String previewId,
      int written,
      int updated,
      int skipped,
      List<String> errors,
      int matched) {
    try {
      TrainCertificateImportBatchEntity batch = new TrainCertificateImportBatchEntity();
      batch.setTenantId(SecurityContext.currentUser().getTenantId());
      batch.setOperatorId(SecurityContext.currentUser().getUserId());
      batch.setPreviewId(previewId);
      batch.setClassIdsJson(
          cached != null ? objectMapper.writeValueAsString(cached.classIds) : "[]");
      batch.setTotalFiles(cached != null ? cached.items.size() : 0);
      batch.setMatched(matched);
      batch.setWritten(written);
      batch.setUpdated(updated);
      batch.setSkipped(skipped);
      batch.setErrorsJson(objectMapper.writeValueAsString(errors == null ? List.of() : errors));
      batch.setRemark("毕业典礼证书文件夹确认回填");
      batch.setCreateTime(LocalDateTime.now());
      importBatchMapper.insert(batch);
    } catch (Exception ex) {
      log.warn("写入证书导入批次审计失败 previewId={}", previewId, ex);
    }
  }

  private Map<String, Object> buildPreviewRow(
      ParsedFile pf,
      RosterIndex roster,
      Map<Long, String> classTitles,
      List<Long> selectedClassIds,
      CertLookup certLookup) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("fileName", pf.fileName);
    row.put("canCommit", false);
    row.put("warning", null);

    if (pf.forceStatus != null) {
      row.put("status", pf.forceStatus);
      row.put("message", pf.forceMessage);
      return row;
    }

    Parsed parsed = parseWithOptionalOcr(pf, roster.byName);
    putParsedFields(row, parsed);
    if (StringUtils.hasText(parsed.warning)) {
      row.put("warning", parsed.warning);
    }
    if (parsed.ocrUsed) {
      row.put("ocrUsed", true);
      row.put("warning", appendWarn((String) row.get("warning"), "已使用 OCR 识别（无文本层或文本不足）"));
    }

    if (!StringUtils.hasText(parsed.idCard) || !StringUtils.hasText(parsed.certificateNo)) {
      row.put("status", "PARSE_FAILED");
      row.put(
          "message",
          ocrSupport.isEnabled()
              ? "未能从 PDF/OCR 解析出身份证或证书编号"
              : "未能从 PDF 解析出身份证或证书编号（未配置百度 OCR，扫描件无法识别）");
      return row;
    }

    List<PeopleRef> hits = roster.byIdCard.getOrDefault(normalizeIdCard(parsed.idCard), List.of());
    if (hits.isEmpty()) {
      row.put("status", "NOT_IN_SCOPE");
      row.put("idCard", normalizeIdCard(parsed.idCard));
      row.put("message", "身份证不在已选班级花名册中");
      return row;
    }
    if (hits.size() >= 2) {
      row.put("status", "DUP_IN_SELECTION");
      row.put(
          "message",
          "同一身份证出现在多个已选班: "
              + hits.stream().map(h -> h.classId + "/" + h.realname).reduce((a, b) -> a + "," + b).orElse(""));
      return row;
    }

    PeopleRef hit = hits.get(0);
    row.put("peopleId", hit.peopleId);
    row.put("classId", hit.classId);
    row.put("classTitle", classTitles.getOrDefault(hit.classId, ""));
    row.put("realname", hit.realname);
    row.put("trainInfoId", hit.trainInfoId);
    if (StringUtils.hasText(parsed.name)
        && StringUtils.hasText(hit.realname)
        && !parsed.name.equals(hit.realname)) {
      row.put("warning", appendWarn((String) row.get("warning"), "姓名与花名册不一致（以证面为准）"));
    }

    TrainCertificateEntity byNo = certLookup.findByCertNo(parsed.certificateNo);
    if (byNo != null && (byNo.getPeopleId() == null || !byNo.getPeopleId().equals(hit.peopleId))) {
      row.put("status", "DUP_CERT_NO");
      row.put("message", "证号已绑定其他学员");
      return row;
    }
    TrainCertificateEntity existing = certLookup.byPeopleId.get(hit.peopleId);
    if (existing != null) {
      String oldNo = existing.getCertificateNo();
      boolean fake = oldNo != null && oldNo.startsWith("GC-");
      boolean same = parsed.certificateNo.equals(oldNo);
      if (!fake && !same && StringUtils.hasText(oldNo)) {
        row.put("status", "EXISTING_CERT_CONFLICT");
        row.put("message", "该学员已有不同真实证号: " + oldNo);
        return row;
      }
    }

    // 预览阶段不落盘：确认回填时再上传，显著缩短匹配等待
    row.put("status", "MATCHED");
    row.put("message", "可回填");
    row.put("canCommit", true);
    return row;
  }

  private CertLookup preloadCertsForClasses(Long tenantId, Map<String, List<PeopleRef>> byIdCard) {
    Set<Long> peopleIds = new HashSet<>();
    for (List<PeopleRef> refs : byIdCard.values()) {
      for (PeopleRef ref : refs) {
        if (ref.peopleId != null) {
          peopleIds.add(ref.peopleId);
        }
      }
    }
    Map<Long, TrainCertificateEntity> byPeopleId = new ConcurrentHashMap<>();
    Map<String, TrainCertificateEntity> byCertNo = new ConcurrentHashMap<>();
    if (peopleIds.isEmpty()) {
      return new CertLookup(tenantId, byPeopleId, byCertNo);
    }
    List<TrainCertificateEntity> certs =
        certificateMapper.selectList(
            new LambdaQueryWrapper<TrainCertificateEntity>()
                .eq(TrainCertificateEntity::getTenantId, tenantId)
                .eq(TrainCertificateEntity::getDeleted, 0)
                .in(TrainCertificateEntity::getPeopleId, peopleIds));
    for (TrainCertificateEntity c : certs) {
      if (c.getPeopleId() != null) {
        byPeopleId.putIfAbsent(c.getPeopleId(), c);
      }
      if (StringUtils.hasText(c.getCertificateNo())) {
        byCertNo.putIfAbsent(c.getCertificateNo(), c);
      }
    }
    return new CertLookup(tenantId, byPeopleId, byCertNo);
  }

  private void enrichOtherClassHints(List<Map<String, Object>> rows, List<Long> selectedClassIds) {
    Set<String> missingIds = new HashSet<>();
    for (Map<String, Object> row : rows) {
      if ("NOT_IN_SCOPE".equals(String.valueOf(row.get("status")))) {
        String id = normalizeIdCard(String.valueOf(row.get("idCard")));
        if (StringUtils.hasText(id) && !"null".equals(id)) {
          missingIds.add(id);
        }
      }
    }
    if (missingIds.isEmpty()) {
      return;
    }
    Map<String, String> hints = batchOtherClassHints(missingIds, selectedClassIds);
    for (Map<String, Object> row : rows) {
      if (!"NOT_IN_SCOPE".equals(String.valueOf(row.get("status")))) {
        continue;
      }
      String id = normalizeIdCard(String.valueOf(row.get("idCard")));
      String hint = hints.get(id);
      if (StringUtils.hasText(hint)) {
        row.put("message", "身份证不在已选班级花名册中；" + hint);
      }
    }
  }

  private Map<String, String> batchOtherClassHints(Set<String> idCards, List<Long> selectedClassIds) {
    Map<String, String> result = new HashMap<>();
    if (idCards.isEmpty()) {
      return result;
    }
    Set<String> queryIds = new HashSet<>(idCards);
    for (String id : idCards) {
      queryIds.add(id.toLowerCase(Locale.ROOT));
    }
    List<TrainPeopleListEntity> others =
        peopleListMapper.selectList(
            new LambdaQueryWrapper<TrainPeopleListEntity>()
                .eq(TrainPeopleListEntity::getType, "student")
                .eq(TrainPeopleListEntity::getDeleted, 0)
                .in(TrainPeopleListEntity::getIdCard, queryIds)
                .last("LIMIT 500"));
    Map<String, List<Long>> idToClasses = new HashMap<>();
    Set<Long> classIdsNeeded = new HashSet<>();
    for (TrainPeopleListEntity p : others) {
      String id = normalizeIdCard(p.getIdCard());
      if (!idCards.contains(id) || p.getTrainClassId() == null) {
        continue;
      }
      if (selectedClassIds != null && selectedClassIds.contains(p.getTrainClassId())) {
        continue;
      }
      idToClasses.computeIfAbsent(id, k -> new ArrayList<>()).add(p.getTrainClassId());
      classIdsNeeded.add(p.getTrainClassId());
    }
    Map<Long, String> titles = new HashMap<>();
    if (!classIdsNeeded.isEmpty()) {
      for (TrainInfoClassEntity c :
          classMapper.selectList(
              new LambdaQueryWrapper<TrainInfoClassEntity>()
                  .in(TrainInfoClassEntity::getId, classIdsNeeded)
                  .eq(TrainInfoClassEntity::getDeleted, 0))) {
        titles.put(c.getId(), c.getTitle());
      }
    }
    for (Map.Entry<String, List<Long>> e : idToClasses.entrySet()) {
      String hint =
          "该身份证在其他班: "
              + e.getValue().stream()
                  .distinct()
                  .limit(5)
                  .map(id -> id + "/" + titles.getOrDefault(id, ""))
                  .collect(Collectors.joining(", "));
      result.put(e.getKey(), hint);
    }
    return result;
  }

  private Parsed parseWithOptionalOcr(ParsedFile pf, Map<String, List<PeopleRef>> byName) {
    if (pf.bytes == null || pf.bytes.length == 0) {
      return RensheSkillCertPdfParser.parseText("", pf.fileName);
    }
    try (PDDocument doc = Loader.loadPDF(pf.bytes)) {
      String text = RensheSkillCertPdfParser.extractFirstPageText(doc);
      Parsed parsed = RensheSkillCertPdfParser.parseText(text, pf.fileName);
      // 文件名有证号+姓名，且本班姓名唯一：用花名册身份证补全，多数扫描件可跳过 OCR
      tryFillIdCardFromUniqueName(parsed, byName);
      if (!RensheSkillCertPdfParser.needsOcr(parsed)) {
        return parsed;
      }
      if (!ocrSupport.isEnabled()) {
        return parsed;
      }
      String ocrText = ocrSupport.ocrFirstPage(doc);
      if (!StringUtils.hasText(ocrText)) {
        return parsed;
      }
      Parsed ocrParsed = RensheSkillCertPdfParser.parseText(ocrText, pf.fileName);
      tryFillIdCardFromUniqueName(ocrParsed, byName);
      ocrParsed.ocrUsed = true;
      return ocrParsed;
    } catch (Exception ex) {
      log.debug("证书 PDF 解析失败 {}", pf.fileName, ex);
      Parsed parsed = RensheSkillCertPdfParser.parseText("", pf.fileName);
      tryFillIdCardFromUniqueName(parsed, byName);
      return parsed;
    }
  }

  /** 证面未识别身份证时，若文件名/正文姓名在已选班唯一，则用花名册身份证补全。 */
  private void tryFillIdCardFromUniqueName(Parsed parsed, Map<String, List<PeopleRef>> byName) {
    if (parsed == null || StringUtils.hasText(parsed.idCard) || !StringUtils.hasText(parsed.name)) {
      return;
    }
    if (byName == null || byName.isEmpty()) {
      return;
    }
    List<PeopleRef> hits = byName.get(parsed.name.trim());
    if (hits == null || hits.size() != 1) {
      return;
    }
    PeopleRef hit = hits.get(0);
    if (!StringUtils.hasText(hit.idCard)) {
      return;
    }
    parsed.idCard = hit.idCard;
    parsed.warning = appendWarn(parsed.warning, "证面未识别身份证，已按花名册姓名唯一匹配补全");
  }

  private void putParsedFields(Map<String, Object> row, Parsed parsed) {
    row.put("idCard", parsed.idCard);
    row.put("certificateNo", parsed.certificateNo);
    row.put("parsedName", parsed.name);
    row.put("idDocType", parsed.idDocType);
    row.put("issuingAuthority", parsed.issuingAuthority);
    row.put("occupation", parsed.occupation);
    row.put("workType", parsed.workType);
    row.put("level", parsed.level);
    row.put("levelNormalized", parsed.levelNormalized);
    row.put("evaluationOrg", parsed.evaluationOrg);
    row.put("issueDate", parsed.issueDate);
    if (parsed.queryUrls != null && !parsed.queryUrls.isEmpty()) {
      try {
        row.put("queryUrls", objectMapper.writeValueAsString(parsed.queryUrls));
      } catch (Exception ignored) {
        row.put("queryUrls", String.join("|", parsed.queryUrls));
      }
      row.put("queryUrlList", parsed.queryUrls);
    } else {
      row.put("queryUrls", null);
      row.put("queryUrlList", List.of());
    }
  }

  private List<ParsedFile> expandZip(String zipName, byte[] bytes) {
    Path tmp = null;
    try {
      tmp = Files.createTempFile("grad-cert-zip-", ".zip");
      Files.write(tmp, bytes);

      List<ParsedFile> out = readZipFile(tmp, StandardCharsets.UTF_8);
      if (out.isEmpty()) {
        out = readZipFile(tmp, Charset.forName("GBK"));
      }
      if (out.isEmpty()) {
        out = readZipStream(bytes, StandardCharsets.UTF_8);
      }
      if (out.isEmpty()) {
        out = readZipStream(bytes, Charset.forName("GBK"));
      }
      if (out.isEmpty()) {
        ParsedFile fail = new ParsedFile(leafName(zipName), new byte[0]);
        fail.forceStatus = "PARSE_FAILED";
        fail.forceMessage = "ZIP 内未找到 PDF（请确认压缩包未加密，且内含 .pdf）";
        out.add(fail);
      }
      return out;
    } catch (Exception ex) {
      log.warn("解压证书 ZIP 失败 name={}", zipName, ex);
      ParsedFile fail = new ParsedFile(leafName(zipName), new byte[0]);
      fail.forceStatus = "PARSE_FAILED";
      fail.forceMessage =
          "ZIP 解压失败: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
      return List.of(fail);
    } finally {
      if (tmp != null) {
        try {
          Files.deleteIfExists(tmp);
        } catch (Exception ignored) {
          // ignore
        }
      }
    }
  }

  private List<ParsedFile> readZipFile(Path zipPath, Charset charset) {
    List<ParsedFile> out = new ArrayList<>();
    try (ZipFile zf = new ZipFile(zipPath.toFile(), charset)) {
      Enumeration<? extends ZipEntry> entries = zf.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        String en = entry.getName();
        if (en == null || en.contains("__MACOSX") || en.endsWith(".DS_Store")) {
          continue;
        }
        if (!isPdfName(en)) {
          continue;
        }
        try (InputStream in = zf.getInputStream(entry)) {
          byte[] content = in.readAllBytes();
          if (content.length > 0) {
            out.add(new ParsedFile(leafName(en), content));
          }
        }
      }
    } catch (Exception ex) {
      log.debug("ZipFile 打开失败 charset={} path={}", charset.name(), zipPath, ex);
    }
    return out;
  }

  private List<ParsedFile> readZipStream(byte[] bytes, Charset charset) {
    List<ParsedFile> out = new ArrayList<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes), charset)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        String en = entry.getName();
        if (en == null || en.contains("__MACOSX") || en.endsWith(".DS_Store")) {
          continue;
        }
        if (!isPdfName(en)) {
          continue;
        }
        byte[] content = zis.readAllBytes();
        if (content.length > 0) {
          out.add(new ParsedFile(leafName(en), content));
        }
      }
    } catch (Exception ex) {
      log.debug("ZipInputStream 打开失败 charset={}", charset.name(), ex);
    }
    return out;
  }

  private RosterIndex buildRosterIndex(List<Long> classIds) {
    List<TrainPeopleListEntity> people =
        peopleListMapper.selectList(
            new LambdaQueryWrapper<TrainPeopleListEntity>()
                .in(TrainPeopleListEntity::getTrainClassId, classIds)
                .eq(TrainPeopleListEntity::getType, "student")
                .eq(TrainPeopleListEntity::getDeleted, 0));
    Map<Long, Long> classTrainInfo = new HashMap<>();
    for (TrainInfoClassEntity c :
        classMapper.selectList(
            new LambdaQueryWrapper<TrainInfoClassEntity>()
                .in(TrainInfoClassEntity::getId, classIds)
                .eq(TrainInfoClassEntity::getDeleted, 0))) {
      classTrainInfo.put(c.getId(), c.getTrainInfoId());
    }
    Map<String, List<PeopleRef>> byIdCard = new HashMap<>();
    Map<String, List<PeopleRef>> byName = new HashMap<>();
    for (TrainPeopleListEntity p : people) {
      String id = normalizeIdCard(p.getIdCard());
      PeopleRef ref =
          new PeopleRef(
              p.getId(),
              p.getTrainClassId(),
              p.getRealname(),
              id,
              classTrainInfo.get(p.getTrainClassId()));
      if (StringUtils.hasText(id)) {
        byIdCard.computeIfAbsent(id, k -> new ArrayList<>()).add(ref);
      }
      if (StringUtils.hasText(p.getRealname())) {
        byName.computeIfAbsent(p.getRealname().trim(), k -> new ArrayList<>()).add(ref);
      }
    }
    return new RosterIndex(byIdCard, byName);
  }

  private Map<Long, String> loadClassTitles(List<Long> classIds) {
    Map<Long, String> map = new LinkedHashMap<>();
    List<TrainInfoClassEntity> classes =
        classMapper.selectList(
            new LambdaQueryWrapper<TrainInfoClassEntity>()
                .in(TrainInfoClassEntity::getId, classIds)
                .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classes.size() != classIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "部分班级不存在或无权限");
    }
    for (TrainInfoClassEntity c : classes) {
      map.put(c.getId(), c.getTitle());
    }
    return map;
  }

  private TrainCertificateEntity findByPeopleId(Long tenantId, Long peopleId) {
    return certificateMapper.selectOne(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .eq(TrainCertificateEntity::getTenantId, tenantId)
            .eq(TrainCertificateEntity::getPeopleId, peopleId)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private TrainCertificateEntity findByCertificateNo(Long tenantId, String certificateNo) {
    return certificateMapper.selectOne(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .eq(TrainCertificateEntity::getTenantId, tenantId)
            .eq(TrainCertificateEntity::getCertificateNo, certificateNo)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private static List<Long> normalizeClassIds(List<String> raw) {
    if (raw == null) {
      return List.of();
    }
    List<Long> ids = new ArrayList<>();
    for (String s : raw) {
      if (!StringUtils.hasText(s)) {
        continue;
      }
      try {
        long id = Long.parseLong(s.trim());
        if (id > 0 && !ids.contains(id)) {
          ids.add(id);
        }
      } catch (NumberFormatException ignored) {
        // skip
      }
    }
    return ids;
  }

  private static String normalizeIdCard(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw.trim().replace(" ", "").toUpperCase(Locale.ROOT);
  }

  private static boolean isPdfName(String name) {
    if (name == null) {
      return false;
    }
    String leaf = leafName(name).toLowerCase(Locale.ROOT);
    return leaf.endsWith(".pdf");
  }

  private static boolean isZipName(String name) {
    if (name == null) {
      return false;
    }
    String leaf = leafName(name).toLowerCase(Locale.ROOT);
    return leaf.endsWith(".zip");
  }

  /** PK\\x03\\x04 / PK\\x05\\x06 / PK\\x07\\x08 */
  private static boolean isZipMagic(byte[] bytes) {
    return bytes != null
        && bytes.length >= 4
        && bytes[0] == 'P'
        && bytes[1] == 'K'
        && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7);
  }

  private static String leafName(String path) {
    if (path == null) {
      return "unknown.pdf";
    }
    int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return i >= 0 ? path.substring(i + 1) : path;
  }

  private static String appendWarn(String existing, String next) {
    if (!StringUtils.hasText(existing)) {
      return next;
    }
    return existing + "；" + next;
  }

  private static String firstNonBlank(String... vals) {
    if (vals == null) {
      return null;
    }
    for (String v : vals) {
      if (StringUtils.hasText(v)) {
        return v.trim();
      }
    }
    return null;
  }

  private static String blankToNull(String v) {
    return StringUtils.hasText(v) ? v.trim() : null;
  }

  private void purgeExpired() {
    Instant now = Instant.now();
    previewCache.entrySet().removeIf(e -> {
      if (!e.getValue().expiresAt.isBefore(now)) {
        return false;
      }
      // 过期未确认：内存缓存即可丢弃（预览已不落盘）
      return true;
    });
    previewJobs.entrySet().removeIf(e -> {
      PreviewJob job = e.getValue();
      return job.createdAt.plusSeconds(PREVIEW_TTL_SECONDS).isBefore(now);
    });
  }

  private static final class RawUpload {
    final String fileName;
    final byte[] bytes;
    final String forceStatus;
    final String forceMessage;

    RawUpload(String fileName, byte[] bytes) {
      this(fileName, bytes, null, null);
    }

    RawUpload(String fileName, byte[] bytes, String forceStatus, String forceMessage) {
      this.fileName = fileName;
      this.bytes = bytes;
      this.forceStatus = forceStatus;
      this.forceMessage = forceMessage;
    }
  }

  private static final class ParsedFile {
    final String fileName;
    final byte[] bytes;
    String forceStatus;
    String forceMessage;

    ParsedFile(String fileName, byte[] bytes) {
      this.fileName = fileName;
      this.bytes = bytes != null ? bytes : new byte[0];
    }
  }

  private record PeopleRef(
      Long peopleId, Long classId, String realname, String idCard, Long trainInfoId) {}

  private record RosterIndex(
      Map<String, List<PeopleRef>> byIdCard, Map<String, List<PeopleRef>> byName) {}

  private final class CertLookup {
    final Long tenantId;
    final Map<Long, TrainCertificateEntity> byPeopleId;
    final Map<String, TrainCertificateEntity> byCertNo;

    CertLookup(
        Long tenantId,
        Map<Long, TrainCertificateEntity> byPeopleId,
        Map<String, TrainCertificateEntity> byCertNo) {
      this.tenantId = tenantId;
      this.byPeopleId = byPeopleId;
      this.byCertNo = byCertNo;
    }

    TrainCertificateEntity findByCertNo(String certificateNo) {
      if (!StringUtils.hasText(certificateNo)) {
        return null;
      }
      TrainCertificateEntity cached = byCertNo.get(certificateNo);
      if (cached != null) {
        return cached;
      }
      TrainCertificateEntity loaded = findByCertificateNo(tenantId, certificateNo);
      if (loaded != null) {
        byCertNo.putIfAbsent(certificateNo, loaded);
      }
      return loaded;
    }
  }

  private record CachedItem(
      String fileName,
      byte[] pdfBytes,
      Long peopleId,
      Long classId,
      String certificateNo,
      String parsedName,
      String idDocType,
      String issuingAuthority,
      String occupation,
      String workType,
      String level,
      String levelNormalized,
      String evaluationOrg,
      String issueDate,
      String queryUrls,
      Long trainInfoId) {}

  private static final class CachedPreview {
    final Long tenantId;
    final Long userId;
    final List<Long> classIds;
    final Instant expiresAt;
    final List<CachedItem> items;

    CachedPreview(
        Long tenantId,
        Long userId,
        List<Long> classIds,
        Instant expiresAt,
        List<CachedItem> items) {
      this.tenantId = tenantId;
      this.userId = userId;
      this.classIds = classIds;
      this.expiresAt = expiresAt;
      this.items = items;
    }
  }

  private static final class PreviewJob {
    final String jobId;
    final Long tenantId;
    final Long userId;
    final Instant createdAt = Instant.now();
    volatile String status;
    volatile String message;
    volatile Map<String, Object> result;

    PreviewJob(String jobId, Long tenantId, Long userId) {
      this.jobId = jobId;
      this.tenantId = tenantId;
      this.userId = userId;
    }
  }
}

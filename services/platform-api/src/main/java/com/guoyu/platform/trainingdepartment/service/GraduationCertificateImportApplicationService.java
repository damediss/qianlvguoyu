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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 毕业典礼：证书文件夹上传 → 预览匹配（身份证）→ 确认回填证号+文件。
 */
@Service
@Slf4j
public class GraduationCertificateImportApplicationService {

  private static final String CERT_TYPE = "skill_level_cert";
  private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{17}[\\dXx])(?!\\d)");
  private static final Pattern CERT_NO = Pattern.compile("(S\\d{18,})");
  private static final long PREVIEW_TTL_SECONDS = 2 * 60 * 60;

  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainCertificateMapper certificateMapper;
  private final FileApplicationService fileApplicationService;
  private final ConcurrentHashMap<String, CachedPreview> previewCache = new ConcurrentHashMap<>();

  public GraduationCertificateImportApplicationService(
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainCertificateMapper certificateMapper,
      FileApplicationService fileApplicationService) {
    this.classMapper = classMapper;
    this.peopleListMapper = peopleListMapper;
    this.certificateMapper = certificateMapper;
    this.fileApplicationService = fileApplicationService;
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

    Map<Long, String> classTitles = loadClassTitles(classIds);
    Map<String, List<PeopleRef>> byIdCard = buildIdCardIndex(classIds);

    List<ParsedFile> parsedFiles = new ArrayList<>();
    for (MultipartFile mf : files) {
      if (mf == null || mf.isEmpty()) {
        continue;
      }
      String name = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : "upload.bin";
      try {
        byte[] bytes = mf.getBytes();
        String leaf = leafName(name);
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
      } catch (Exception ex) {
        log.warn("读取证书上传文件失败 name={}", name, ex);
        ParsedFile fail = new ParsedFile(leafName(name), new byte[0]);
        fail.forceStatus = "PARSE_FAILED";
        fail.forceMessage = "读取文件失败: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        parsedFiles.add(fail);
      }
    }
    if (parsedFiles.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到可处理的证书 PDF");
    }

    List<CachedItem> cachedItems = new ArrayList<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    int matched = 0;
    int blocked = 0;
    int failed = 0;

    for (ParsedFile pf : parsedFiles) {
      Map<String, Object> row = buildPreviewRow(pf, byIdCard, classTitles);
      rows.add(row);
      String status = String.valueOf(row.get("status"));
      boolean canCommit = Boolean.TRUE.equals(row.get("canCommit"));
      if (canCommit) {
        matched++;
      } else if ("PARSE_FAILED".equals(status)) {
        failed++;
      } else {
        blocked++;
      }
      if (canCommit) {
        cachedItems.add(new CachedItem(
            (String) row.get("fileName"),
            (Long) row.get("fileId"),
            (String) row.get("filePath"),
            (Long) row.get("peopleId"),
            (Long) row.get("classId"),
            (String) row.get("certificateNo"),
            (String) row.get("workType"),
            (String) row.get("level"),
            (Long) row.get("trainInfoId")));
      }
    }

    String previewId = UUID.randomUUID().toString();
    Long tenantId = SecurityContext.currentUser().getTenantId();
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
    out.put("previewId", previewId);
    out.put("classIds", classIds);
    out.put("totalFiles", rows.size());
    out.put("matched", matched);
    out.put("blocked", blocked);
    out.put("failed", failed);
    out.put("items", rows);
    return out;
  }

  @Transactional
  public Map<String, Object> commit(GraduationCertImportCommitReq req) {
    purgeExpired();
    if (req == null || !StringUtils.hasText(req.getPreviewId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "previewId 不能为空");
    }
    CachedPreview cached = previewCache.get(req.getPreviewId().trim());
    if (cached == null || cached.expiresAt.isBefore(Instant.now())) {
      previewCache.remove(req.getPreviewId());
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

    String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    int written = 0;
    int updated = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();

    for (CachedItem item : cached.items) {
      try {
        TrainPeopleListEntity person = peopleListMapper.selectById(item.peopleId);
        if (person == null || (person.getDeleted() != null && person.getDeleted() != 0)) {
          skipped++;
          errors.add(item.fileName + ": 学员不存在");
          continue;
        }
        if (!item.classId.equals(person.getTrainClassId())) {
          skipped++;
          errors.add(item.fileName + ": 学员班级已变更");
          continue;
        }

        TrainCertificateEntity byNo = findByCertificateNo(tenantId, item.certificateNo);
        if (byNo != null && (byNo.getPeopleId() == null || !byNo.getPeopleId().equals(item.peopleId))) {
          skipped++;
          errors.add(item.fileName + ": 证号已被其他人占用");
          continue;
        }

        TrainCertificateEntity existing = findByPeopleId(tenantId, item.peopleId);
        if (existing == null) {
          TrainCertificateEntity cert = new TrainCertificateEntity();
          cert.setTenantId(tenantId);
          cert.setProjectId(item.trainInfoId);
          cert.setClassId(item.classId);
          cert.setPeopleId(item.peopleId);
          cert.setUserId(person.getPlatformUserId() != null ? person.getPlatformUserId() : person.getUserId());
          cert.setUserName(person.getRealname());
          cert.setCertificateNo(item.certificateNo);
          cert.setCertificateType(CERT_TYPE);
          cert.setWorkType(item.workType);
          cert.setLevel(item.level);
          cert.setIssueDate(today);
          cert.setFilePath(item.filePath);
          cert.setStatus(1);
          cert.setCreatedBy(userId);
          cert.setDeleted(0);
          certificateMapper.insert(cert);
          written++;
        } else {
          String oldNo = existing.getCertificateNo();
          boolean fake = oldNo != null && oldNo.startsWith("GC-");
          boolean same = item.certificateNo.equals(oldNo);
          if (!fake && !same && StringUtils.hasText(oldNo)) {
            skipped++;
            errors.add(item.fileName + ": 已有不同真实证号 " + oldNo);
            continue;
          }
          existing.setCertificateNo(item.certificateNo);
          existing.setCertificateType(CERT_TYPE);
          existing.setFilePath(item.filePath);
          existing.setClassId(item.classId);
          existing.setPeopleId(item.peopleId);
          if (item.trainInfoId != null) {
            existing.setProjectId(item.trainInfoId);
          }
          if (StringUtils.hasText(item.workType)) {
            existing.setWorkType(item.workType);
          }
          if (StringUtils.hasText(item.level)) {
            existing.setLevel(item.level);
          }
          existing.setUserName(person.getRealname());
          existing.setStatus(1);
          certificateMapper.updateById(existing);
          updated++;
        }
      } catch (Exception ex) {
        log.warn("证书回填失败 file={}", item.fileName, ex);
        skipped++;
        errors.add(item.fileName + ": " + ex.getMessage());
      }
    }

    previewCache.remove(req.getPreviewId().trim());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("written", written);
    out.put("updated", updated);
    out.put("skipped", skipped);
    out.put("errors", errors);
    return out;
  }

  private Map<String, Object> buildPreviewRow(
      ParsedFile pf, Map<String, List<PeopleRef>> byIdCard, Map<Long, String> classTitles) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("fileName", pf.fileName);
    row.put("canCommit", false);
    row.put("warning", null);

    if (pf.forceStatus != null) {
      row.put("status", pf.forceStatus);
      row.put("message", pf.forceMessage);
      return row;
    }

    ParseResult parsed = parsePdf(pf.bytes, pf.fileName);
    row.put("idCard", parsed.idCard);
    row.put("certificateNo", parsed.certificateNo);
    row.put("parsedName", parsed.name);
    row.put("workType", parsed.workType);
    row.put("level", parsed.level);
    if (StringUtils.hasText(parsed.warning)) {
      row.put("warning", parsed.warning);
    }

    if (!StringUtils.hasText(parsed.idCard) || !StringUtils.hasText(parsed.certificateNo)) {
      row.put("status", "PARSE_FAILED");
      row.put("message", "未能从 PDF 解析出身份证或证书编号");
      return row;
    }

    List<PeopleRef> hits = byIdCard.getOrDefault(normalizeIdCard(parsed.idCard), List.of());
    if (hits.isEmpty()) {
      row.put("status", "NOT_IN_SCOPE");
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
      row.put("warning", appendWarn((String) row.get("warning"), "姓名与花名册不一致（以身份证为准）"));
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainCertificateEntity byNo = findByCertificateNo(tenantId, parsed.certificateNo);
    if (byNo != null && (byNo.getPeopleId() == null || !byNo.getPeopleId().equals(hit.peopleId))) {
      row.put("status", "DUP_CERT_NO");
      row.put("message", "证号已绑定其他学员");
      return row;
    }
    TrainCertificateEntity existing = findByPeopleId(tenantId, hit.peopleId);
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

    try {
      FileVO stored =
          fileApplicationService.storeFromBytes(
              pf.bytes, pf.fileName, "application/pdf", "private");
      String path = "/api/files/" + stored.id() + "/download";
      row.put("fileId", stored.id());
      row.put("filePath", path);
    } catch (Exception ex) {
      log.warn("证书文件暂存失败 {}", pf.fileName, ex);
      row.put("status", "PARSE_FAILED");
      row.put("message", "文件上传失败");
      return row;
    }

    row.put("status", "MATCHED");
    row.put("message", "可回填");
    row.put("canCommit", true);
    return row;
  }

  private ParseResult parsePdf(byte[] bytes, String fileName) {
    ParseResult r = new ParseResult();
    String fnCert = null;
    String fnName = null;
    String base = fileName;
    int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
    if (slash >= 0) {
      base = fileName.substring(slash + 1);
    }
    if (base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      base = base.substring(0, base.length() - 4);
    }
    String[] parts = base.split("_");
    if (parts.length >= 1 && parts[0].startsWith("S")) {
      fnCert = parts[0].trim();
    }
    if (parts.length >= 2) {
      fnName = parts[1].trim();
      r.name = fnName;
    }

    String text = "";
    if (bytes != null && bytes.length > 0) {
      try (PDDocument doc = Loader.loadPDF(bytes)) {
        PDFTextStripper stripper = new PDFTextStripper();
        text = stripper.getText(doc);
      } catch (Exception ex) {
        log.debug("PDF 文本抽取失败 {}", fileName, ex);
      }
    }

    Matcher idm = ID_CARD.matcher(text);
    if (idm.find()) {
      r.idCard = idm.group(1).toUpperCase(Locale.ROOT);
    }
    Matcher cm = CERT_NO.matcher(text);
    if (cm.find()) {
      r.certificateNo = cm.group(1);
    }
    if (!StringUtils.hasText(r.certificateNo) && StringUtils.hasText(fnCert)) {
      r.certificateNo = fnCert;
    }
    if (StringUtils.hasText(r.certificateNo)
        && StringUtils.hasText(fnCert)
        && !r.certificateNo.equals(fnCert)) {
      r.warning = "正文证号与文件名不一致，已采用正文证号";
    }
    if (text.contains("三级") || text.contains("高级工")) {
      r.level = text.contains("三级") ? "三级" : r.level;
    }
    if (text.contains("四级") || text.contains("中级工")) {
      if (text.contains("四级")) {
        r.level = "四级";
      }
    }
    if (text.contains("技师")) {
      r.level = "技师";
    }
    // 粗提工种：取「铝电解」「炭极」等常见词
    if (text.contains("炭极生产工")) {
      r.workType = "炭极生产工";
    } else if (text.contains("铝电解工")) {
      r.workType = "铝电解工";
    } else if (text.contains("铝电解槽操作工")) {
      r.workType = "铝电解槽操作工";
    }
    return r;
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

  private Map<String, List<PeopleRef>> buildIdCardIndex(List<Long> classIds) {
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
    Map<String, List<PeopleRef>> index = new HashMap<>();
    for (TrainPeopleListEntity p : people) {
      String id = normalizeIdCard(p.getIdCard());
      if (!StringUtils.hasText(id)) {
        continue;
      }
      index
          .computeIfAbsent(id, k -> new ArrayList<>())
          .add(
              new PeopleRef(
                  p.getId(),
                  p.getTrainClassId(),
                  p.getRealname(),
                  classTrainInfo.get(p.getTrainClassId())));
    }
    return index;
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

  private void purgeExpired() {
    Instant now = Instant.now();
    previewCache.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
  }

  private static final class ParseResult {
    String idCard;
    String certificateNo;
    String name;
    String workType;
    String level;
    String warning;
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

  private record PeopleRef(Long peopleId, Long classId, String realname, Long trainInfoId) {}

  private record CachedItem(
      String fileName,
      Long fileId,
      String filePath,
      Long peopleId,
      Long classId,
      String certificateNo,
      String workType,
      String level,
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
}

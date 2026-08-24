package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 毕业典礼 · 职业技能等级认定获证人员名册导出（模板填充）。
 * 页眉仅含标题与班级名称（无计划编码/填报/取证日期）。
 */
@Service
@Slf4j
public class GraduationCertifiedRosterExportService {

  private static final String TEMPLATE_PATH = "templates/职业技能等级认定获证人员名册.xlsx";
  /** 0-based：标题 + 班级名称 + 双行表头后，数据从第5行开始 */
  private static final int DATA_START_ROW = 4;
  /** 0-based：页脚在第6行 */
  private static final int FOOTER_ROW_TEMPLATE = 5;
  /** 0-based：班级名称行 */
  private static final int CLASS_NAME_ROW = 1;

  /** workTypeLevel 1–5（初级→高级技师）→ 人社「级数/称谓」 */
  private static final String[] LEVEL_LABELS = {
      "",
      "五级/初级工",
      "四级/中级工",
      "三级/高级工",
      "二级/技师",
      "一级/高级技师"
  };

  /** 导出人员范围：已获证（证号非空）/ 全班 */
  public enum RosterScope {
    CERTIFIED,
    ALL;

    static RosterScope fromParam(String scope) {
      if (!StringUtils.hasText(scope) || "certified".equalsIgnoreCase(scope.trim())) {
        return CERTIFIED;
      }
      if ("all".equalsIgnoreCase(scope.trim())) {
        return ALL;
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST, "scope 仅支持 certified 或 all");
    }

    String fileLabel() {
      return this == ALL ? "全部" : "已获证";
    }
  }

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainCertificateMapper certificateMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;

  public GraduationCertifiedRosterExportService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainCertificateMapper certificateMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.certificateMapper = certificateMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> export(Long classId) {
    return export(classId, null, null);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> export(Long classId, String preferredClassTitle) {
    return export(classId, preferredClassTitle, null);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> export(Long classId, String preferredClassTitle, String scopeParam) {
    RosterScope scope = RosterScope.fromParam(scopeParam);
    TrainInfoClassEntity clazz = requireClass(classId);
    String displayName = resolveClassDisplayName(clazz, preferredClassTitle);
    byte[] bytes = buildWorkbookBytes(clazz, displayName, scope);
    String fileName = "职业技能等级认定获证人员名册_"
        + sanitizeFileName(displayName)
        + "_"
        + scope.fileLabel()
        + "_"
        + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        + ".xlsx";
    return xlsxResponse(bytes, fileName);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportZip(List<Long> classIds) {
    return exportZip(classIds, null);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportZip(List<Long> classIds, String scopeParam) {
    RosterScope scope = RosterScope.fromParam(scopeParam);
    List<Long> ids = classIds == null
        ? List.of()
        : classIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    if (ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择班级");
    }
    if (ids.size() == 1) {
      return export(ids.get(0), null, scopeParam);
    }
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos)) {
      for (Long id : ids) {
        TrainInfoClassEntity clazz = requireClass(id);
        String displayName = resolveClassDisplayName(clazz, null);
        byte[] bytes = buildWorkbookBytes(clazz, displayName, scope);
        String entryName = "职业技能等级认定获证人员名册_"
            + sanitizeFileName(displayName)
            + "_"
            + scope.fileLabel()
            + "_"
            + id
            + ".xlsx";
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(bytes);
        zos.closeEntry();
      }
      zos.finish();
      String zipName = URLEncoder.encode(
              "获证人员名册_" + scope.fileLabel() + "_已选" + ids.size() + "班_" + LocalDate.now() + ".zip",
              StandardCharsets.UTF_8)
          .replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + zipName)
          .contentType(MediaType.parseMediaType("application/zip"))
          .body(bos.toByteArray());
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出获证名册失败: " + ex.getMessage());
    }
  }

  private byte[] buildWorkbookBytes(
      TrainInfoClassEntity clazz, String classDisplayName, RosterScope scope) {
    List<TrainPeopleListEntity> allStudents = listStudents(clazz.getId());
    Map<Long, TrainCertificateEntity> certs = loadCertsByPeople(clazz.getId());
    List<TrainPeopleListEntity> students = filterByScope(allStudents, certs, scope);
    Map<String, String> genderByIdCard = loadGenders(students);
    String companyName = resolveCompanyName(clazz);
    String workTypeName = resolveWorkTypeName(clazz.getWorkTypeId());
    String levelLabel = formatLevelLabel(clazz.getWorkTypeLevel());

    try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream();
        XSSFWorkbook wb = new XSSFWorkbook(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.getSheetAt(0);
      ensureDataRows(sheet, Math.max(students.size(), 1));
      fillStudents(sheet, students, certs, genderByIdCard, companyName, workTypeName, levelLabel);
      if (students.isEmpty()) {
        clearRowValues(sheet.getRow(DATA_START_ROW));
      }
      writeClassName(sheet, classDisplayName);
      wb.write(bos);
      return bos.toByteArray();
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("获证人员名册导出失败 classId={} scope={}", clazz.getId(), scope, ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出获证名册失败: " + ex.getMessage());
    }
  }

  /** 已获证：证书编号非空；全部：不过滤。 */
  static List<TrainPeopleListEntity> filterByScope(
      List<TrainPeopleListEntity> students,
      Map<Long, TrainCertificateEntity> certs,
      RosterScope scope) {
    if (scope == RosterScope.ALL || students == null || students.isEmpty()) {
      return students == null ? List.of() : students;
    }
    List<TrainPeopleListEntity> out = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      TrainCertificateEntity cert = certs.get(s.getId());
      if (cert != null && StringUtils.hasText(cert.getCertificateNo())) {
        out.add(s);
      }
    }
    return out;
  }

  /**
   * 班级显示名：请求入参（列表所见）→ 班级 title → 培训项目 title → 班级ID。
   */
  private String resolveClassDisplayName(TrainInfoClassEntity clazz, String preferred) {
    if (StringUtils.hasText(preferred)) {
      return preferred.trim();
    }
    if (StringUtils.hasText(clazz.getTitle())) {
      return clazz.getTitle().trim();
    }
    if (clazz.getTrainInfoId() != null) {
      TrainInfoListEntity info = infoListMapper.selectById(clazz.getTrainInfoId());
      if (info != null && StringUtils.hasText(info.getTitle())) {
        return info.getTitle().trim();
      }
    }
    log.warn("获证名册班级名称为空 classId={}，将回退为班级ID", clazz.getId());
    return "班级" + clazz.getId();
  }

  /**
   * Excel 第2行（合并 A2:M2）写入「班级名称：xxx」。
   * 模板 A2 为 inlineStr，直接 setCellValue 可能写不进，需先删再建单元格。
   */
  private void writeClassName(Sheet sheet, String classTitle) {
    ensureMergedRegion(sheet, CLASS_NAME_ROW, CLASS_NAME_ROW, 0, 12);
    Row row = sheet.getRow(CLASS_NAME_ROW);
    if (row == null) {
      row = sheet.createRow(CLASS_NAME_ROW);
    }
    CellStyle style = null;
    Cell existing = row.getCell(0);
    if (existing != null) {
      style = existing.getCellStyle();
      row.removeCell(existing);
    }
    Cell cell = row.createCell(0);
    cell.setCellValue("班级名称：" + nullToEmpty(classTitle));
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  private static void ensureMergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress existing = sheet.getMergedRegion(i);
      if (existing.getFirstRow() == firstRow
          && existing.getLastRow() == lastRow
          && existing.getFirstColumn() == firstCol
          && existing.getLastColumn() == lastCol) {
        return;
      }
    }
    sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
  }

  /**
   * 模板：第5行样式原型，第6行页脚，第7行空白合并。人数 N&gt;1 时在页脚前插入 N-1 行。
   */
  private void ensureDataRows(Sheet sheet, int studentCount) {
    int need = Math.max(studentCount, 1);
    if (need <= 1) {
      return;
    }
    int extra = need - 1;
    int lastRow = sheet.getLastRowNum();
    List<CellRangeAddress> footerMerges = new ArrayList<>();
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      if (region.getFirstRow() >= FOOTER_ROW_TEMPLATE) {
        footerMerges.add(region);
        sheet.removeMergedRegion(i);
      }
    }
    sheet.shiftRows(FOOTER_ROW_TEMPLATE, lastRow, extra, true, false);
    Row proto = sheet.getRow(DATA_START_ROW);
    for (int i = 1; i < need; i++) {
      int rowIdx = DATA_START_ROW + i;
      Row row = sheet.getRow(rowIdx);
      if (row == null) {
        row = sheet.createRow(rowIdx);
      }
      if (proto != null) {
        row.setHeight(proto.getHeight());
        for (int c = 0; c < 13; c++) {
          Cell src = proto.getCell(c);
          Cell dst = row.getCell(c);
          if (dst == null) {
            dst = row.createCell(c);
          }
          if (src != null && src.getCellStyle() != null) {
            dst.setCellStyle(src.getCellStyle());
          }
        }
      }
    }
    for (CellRangeAddress region : footerMerges) {
      sheet.addMergedRegion(new CellRangeAddress(
          region.getFirstRow() + extra,
          region.getLastRow() + extra,
          region.getFirstColumn(),
          region.getLastColumn()));
    }
  }

  private void fillStudents(
      Sheet sheet,
      List<TrainPeopleListEntity> students,
      Map<Long, TrainCertificateEntity> certs,
      Map<String, String> genderByIdCard,
      String companyName,
      String workTypeName,
      String levelLabel) {
    int seq = 1;
    for (TrainPeopleListEntity s : students) {
      Row row = sheet.getRow(DATA_START_ROW + seq - 1);
      if (row == null) {
        row = sheet.createRow(DATA_START_ROW + seq - 1);
      }
      TrainCertificateEntity cert = certs.get(s.getId());
      String idCard = nullToEmpty(s.getIdCard());
      setCellNumberOrString(row, 0, seq);
      setCellString(row, 1, nullToEmpty(s.getTicketNo()));
      setCellString(row, 2, nullToEmpty(s.getRealname()));
      setCellString(row, 3, resolveGender(idCard, genderByIdCard));
      setCellString(row, 4, idCard);
      setCellString(row, 5, companyName);
      setCellString(row, 6, workTypeName);
      setCellString(row, 7, levelLabel);
      setCellString(row, 8, scoreCell(s.getTheoryScore()));
      setCellString(row, 9, scoreCell(s.getRealScore()));
      setCellString(row, 10, scoreCell(s.getExamScore()));
      setCellString(row, 11, scoreCell(s.getPerformanceScore()));
      setCellString(row, 12, cert != null ? nullToEmpty(cert.getCertificateNo()) : "");
      seq++;
    }
  }

  private void clearRowValues(Row row) {
    if (row == null) {
      return;
    }
    for (int c = 0; c < 13; c++) {
      Cell cell = row.getCell(c);
      if (cell != null) {
        cell.setBlank();
      }
    }
  }

  private Map<String, String> loadGenders(List<TrainPeopleListEntity> students) {
    List<String> idCards = students.stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    Map<String, String> result = new HashMap<>();
    if (idCards.isEmpty()) {
      return result;
    }
    platformUserDetailMapper.selectList(
            new LambdaQueryWrapper<PlatformUserDetailEntity>()
                .in(PlatformUserDetailEntity::getIdcard, idCards)
                .eq(PlatformUserDetailEntity::getDeleted, 0))
        .forEach(p -> {
          if (StringUtils.hasText(p.getIdcard()) && p.getGender() != null) {
            result.putIfAbsent(p.getIdcard().trim(), p.getGender() > 0 ? "女" : "男");
          }
        });
    signUpPeopleSelfMapper.selectList(
            new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
                .in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards)
                .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
                .orderByDesc(TrainSignUpPeopleSelfEntity::getId))
        .forEach(p -> {
          if (!StringUtils.hasText(p.getIdCardNumber()) || !StringUtils.hasText(p.getGender())) {
            return;
          }
          String g = p.getGender().trim();
          String label = "女".equals(g) || "2".equals(g) ? "女" : ("男".equals(g) || "1".equals(g) ? "男" : g);
          result.putIfAbsent(p.getIdCardNumber().trim(), label);
        });
    return result;
  }

  private String resolveGender(String idCard, Map<String, String> genderByIdCard) {
    if (StringUtils.hasText(idCard) && genderByIdCard.containsKey(idCard.trim())) {
      return genderByIdCard.get(idCard.trim());
    }
    if (StringUtils.hasText(idCard) && idCard.trim().length() >= 17) {
      char ch = idCard.trim().charAt(16);
      if (Character.isDigit(ch)) {
        return ((ch - '0') % 2 == 0) ? "女" : "男";
      }
    }
    return "";
  }

  private String resolveCompanyName(TrainInfoClassEntity clazz) {
    if (clazz.getCompanyId() != null && clazz.getCompanyId() > 0) {
      OrgEntity org = orgMapper.selectById(clazz.getCompanyId());
      if (org != null && StringUtils.hasText(org.getName())) {
        return org.getName().trim();
      }
    }
    if (clazz.getTrainInfoId() != null) {
      TrainInfoListEntity info = infoListMapper.selectById(clazz.getTrainInfoId());
      if (info != null && StringUtils.hasText(info.getCompanyName())) {
        return info.getCompanyName().trim();
      }
    }
    return "";
  }

  private String resolveWorkTypeName(Long workTypeId) {
    if (workTypeId == null || workTypeId <= 0) {
      return "";
    }
    ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(workTypeId);
    return wt != null && StringUtils.hasText(wt.getTitle()) ? wt.getTitle().trim() : "";
  }

  static String formatLevelLabel(Integer workTypeLevel) {
    if (workTypeLevel == null || workTypeLevel < 1 || workTypeLevel >= LEVEL_LABELS.length) {
      return "";
    }
    return LEVEL_LABELS[workTypeLevel];
  }

  private static String scoreCell(BigDecimal score) {
    if (score == null) {
      return "--";
    }
    return score.setScale(1, RoundingMode.HALF_UP).toPlainString();
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

  private static void setCellString(Row row, int col, String value) {
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    CellStyle style = cell.getCellStyle();
    cell.setCellValue(value != null ? value : "");
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  private static void setCellNumberOrString(Row row, int col, int value) {
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    CellStyle style = cell.getCellStyle();
    cell.setCellValue(value);
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v.trim();
  }

  private static String sanitizeFileName(String name) {
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private static ResponseEntity<byte[]> xlsxResponse(byte[] bytes, String fileName) {
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }
}

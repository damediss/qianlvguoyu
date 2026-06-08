package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 报名人员默认模板材料打包（对齐 PHP Yunxinxi taskexport） */
@Component
@Slf4j
public class SignUpPeopleDefaultTemplatePackService {

  private static final String[] LEVEL_LABELS = {
      "初级工/五级", "中级工/四级", "高级工/三级", "技师/二级", "高级技师/一级"
  };

  private final String cdnPath;
  private final ObjectMapper objectMapper;

  public SignUpPeopleDefaultTemplatePackService(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      ObjectMapper objectMapper) {
    this.cdnPath = cdnPath;
    this.objectMapper = objectMapper;
  }

  public String run(
      TrainSignUpSelfEntity signUp,
      OrgEntity company,
      List<TrainSignUpPeopleSelfEntity> people,
      ProgressCallback callback) throws Exception {
    ExportContext ctx = new ExportContext(cdnPath, signUp, company);
    initDirectories(ctx);
    initSummaryExcels(ctx);

    int index = 0;
    for (TrainSignUpPeopleSelfEntity person : people) {
      processPerson(ctx, person, index + 2);
      index++;
      if (callback != null) {
        callback.onProgress(index, people.size());
      }
    }

    Path zipPath = finalizeZip(ctx);
    return "down/" + ctx.folderName + ".zip";
  }

  private void initDirectories(ExportContext ctx) throws Exception {
    Files.createDirectories(ctx.rootDir);
    ctx.idCardFrontDir = mkdir(ctx.rootDir, "身份证正面");
    ctx.idCardBackDir = mkdir(ctx.rootDir, "身份证反面");
    ctx.idCardHeaderDir = mkdir(ctx.rootDir, "小二寸证件照");
    ctx.contractDir = mkdir(ctx.rootDir, "合同");
    ctx.idCardMergeDir = mkdir(ctx.rootDir, "身份证正反面合并");
    ctx.levelApplyDir = mkdir(ctx.rootDir, "职业技能等级认定申请表");
    ctx.commentRegisterDir = mkdir(ctx.rootDir, "职业技能评价登记表（2025新）");
    ctx.gxRegisterDir = mkdir(ctx.rootDir, "广西壮族自治区职业技能鉴定登记表");
    ctx.workYearDir = mkdir(ctx.rootDir, "工作年限证明");
    ctx.schoolDir = mkdir(ctx.rootDir, "毕业证");
    ctx.lastLevelDir = mkdir(ctx.rootDir, "上一等级证书");
  }

  private void initSummaryExcels(ExportContext ctx) throws Exception {
    ctx.bmallExcel = copyTemplate("template/报名人员汇总表导出.xlsx", ctx.rootDir.resolve("报名人员汇总表.xlsx"));
    ctx.examGxExcel = copyTemplate("template/考试报名导出模板(广西).xlsx", ctx.rootDir.resolve("考试报名导入模板(广西).xlsx"));
    ctx.examAllExcel = copyTemplate("template/考试报名导出模板(有色金属行业).xlsx", ctx.rootDir.resolve("考试报名导入模板(有色金属行业).xlsx"));
  }

  private void processPerson(ExportContext ctx, TrainSignUpPeopleSelfEntity person, int rowNum) throws Exception {
    String idCard = normalize(person.getIdCardNumber());
    if (!StringUtils.hasText(idCard) || idCard.length() != 18) {
      return;
    }
    String realname = safe(person.getName());
    copyIdCardAssets(ctx, person, idCard, realname);
    Map<String, String> vars = buildVars(ctx, person, idCard, realname);
    fillDocxTemplate("template/职业技能等级认定申请表.docx",
        ctx.levelApplyDir.resolve(realname + "_" + idCard + ".docx"), vars);
    fillDocxTemplate("template/职业技能评价登记表（2025新）.docx",
        ctx.commentRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars);
    fillDocxTemplate("template/广西壮族自治区职业技能鉴定登记表.docx",
        ctx.gxRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars);
    fillDocxTemplate("template/工作年限证明.docx",
        ctx.workYearDir.resolve(realname + "_" + idCard + ".docx"), vars);
    appendSummaryRows(ctx, person, idCard, realname, rowNum, vars);
  }

  private Map<String, String> buildVars(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname) {
    Map<String, String> vars = new LinkedHashMap<>();
    String gender = genderFromIdCard(idCard);
    String birthday = formatBirthday(idCard);
    int age = calcAge(idCard);
    WorkExp exp = parseWorkExp(person.getWorkExp(), person.getWorkDateStart(), person.getWorkDateEnd());
    String companyName = ctx.companyName;
    vars.put("name", realname);
    vars.put("realname", realname);
    vars.put("gender", gender);
    vars.put("age", String.valueOf(age));
    vars.put("phone", safe(person.getPhone()));
    vars.put("idcard", idCard);
    vars.put("address", safe(person.getAddress()));
    vars.put("nation", safe(person.getNation()));
    vars.put("political", safe(person.getPolitical()));
    vars.put("education", safe(person.getEducation()));
    vars.put("school", safe(person.getSchool()));
    vars.put("major", safe(person.getMajor()));
    vars.put("school_no", safe(person.getSchoolNo()));
    vars.put("com_name", companyName);
    vars.put("company_name", companyName);
    vars.put("req_work_type", safe(person.getReqWorkTypeName()));
    vars.put("req_work_level", levelLabel(person.getReqLevel()));
    vars.put("work_type", safe(person.getWorkTypeName()));
    vars.put("condition", safe(person.getDeclarationCondition()));
    vars.put("paper_no", safe(person.getPaperNo()));
    vars.put("exp_date", exp.rangeText());
    vars.put("exp_company", exp.company());
    vars.put("exp_position", exp.position());
    vars.put("exp_user", exp.proveName());
    vars.put("exp_phone", exp.provePhone());
    vars.put("work_start_date", exp.startDate());
    vars.put("work_age", String.valueOf(workAge(exp.startDate())));
    vars.put("create_time", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
    return vars;
  }

  private void appendSummaryRows(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname,
      int rowNum,
      Map<String, String> vars) throws Exception {
    if (ctx.bmallExcel != null) {
      try (InputStream in = Files.newInputStream(ctx.bmallExcel);
          Workbook wb = WorkbookFactory.create(in)) {
        Sheet sheet = wb.getSheetAt(0);
        setCell(sheet, rowNum, 0, realname);
        setCell(sheet, rowNum, 1, idCard);
        setCell(sheet, rowNum, 2, safe(person.getPhone()));
        setCell(sheet, rowNum, 3, vars.get("age"));
        setCell(sheet, rowNum, 4, vars.get("company_name"));
        setCell(sheet, rowNum, 6, safe(person.getWorkTypeName()));
        setCell(sheet, rowNum, 7, levelLabel(person.getLevel()));
        setCell(sheet, rowNum, 8, safe(person.getEducation()));
        setCell(sheet, rowNum, 11, safe(person.getSchool()));
        setCell(sheet, rowNum, 12, safe(person.getMajor()));
        setCell(sheet, rowNum, 13, safe(person.getSchoolNo()));
        setCell(sheet, rowNum, 19, vars.get("exp_date"));
        setCell(sheet, rowNum, 20, safe(person.getAddress()));
        wb.write(Files.newOutputStream(ctx.bmallExcel));
      }
    }
    if (ctx.examGxExcel != null) {
      try (InputStream in = Files.newInputStream(ctx.examGxExcel);
          Workbook wb = WorkbookFactory.create(in)) {
        Sheet sheet = wb.getSheetAt(0);
        setCell(sheet, rowNum, 0, String.valueOf(rowNum - 1));
        setCell(sheet, rowNum, 1, safe(person.getReqWorkTypeName()));
        setCell(sheet, rowNum, 2, levelLabel(person.getReqLevel()));
        setCell(sheet, rowNum, 3, safe(person.getDeclarationCondition()));
        setCell(sheet, rowNum, 4, "居民身份证");
        setCell(sheet, rowNum, 5, idCard);
        setCell(sheet, rowNum, 6, realname);
        setCell(sheet, rowNum, 7, vars.get("gender"));
        setCell(sheet, rowNum, 8, formatBirthday(idCard));
        setCell(sheet, rowNum, 9, safe(person.getPhone()));
        wb.write(Files.newOutputStream(ctx.examGxExcel));
      }
    }
    if (ctx.examAllExcel != null) {
      try (InputStream in = Files.newInputStream(ctx.examAllExcel);
          Workbook wb = WorkbookFactory.create(in)) {
        Sheet sheet = wb.getSheetAt(0);
        setCell(sheet, rowNum, 0, String.valueOf(rowNum - 1));
        setCell(sheet, rowNum, 1, safe(person.getReqWorkTypeName()));
        setCell(sheet, rowNum, 2, levelLabel(person.getReqLevel()));
        setCell(sheet, rowNum, 6, "居民身份证");
        setCell(sheet, rowNum, 7, idCard);
        setCell(sheet, rowNum, 8, realname);
        setCell(sheet, rowNum, 10, vars.get("gender"));
        wb.write(Files.newOutputStream(ctx.examAllExcel));
      }
    }
  }

  private Path finalizeZip(ExportContext ctx) throws Exception {
    Path oldZip = Paths.get(cdnPath, "down", ctx.folderName + ".zip");
    Files.deleteIfExists(oldZip);
    List<Path> entries = List.of(
        ctx.idCardFrontDir, ctx.idCardBackDir, ctx.idCardHeaderDir, ctx.idCardMergeDir,
        ctx.levelApplyDir, ctx.commentRegisterDir, ctx.gxRegisterDir, ctx.workYearDir,
        ctx.contractDir, ctx.schoolDir, ctx.lastLevelDir,
        ctx.bmallExcel, ctx.examGxExcel, ctx.examAllExcel
    ).stream().filter(p -> p != null && Files.exists(p)).toList();
    TrainingZipUtil.zipEntries(oldZip, entries);
    deleteRecursively(ctx.rootDir);
    return oldZip;
  }

  private void copyIdCardAssets(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname) throws Exception {
    copyIfExists(cdnPath + "/idCard/headerOrigin/" + idCard + "_header.jpg",
        ctx.idCardHeaderDir.resolve(idCard + ".jpg"));
    copyIfExists(cdnPath + "/idCard/old/headerOrigin/" + idCard + "_full.png",
        ctx.idCardHeaderDir.resolve(idCard + ".jpg"));
    copyIfExists(cdnPath + "/idCard/headerOrigin/" + idCard + "_front.jpg",
        ctx.idCardFrontDir.resolve(idCard + ".jpg"));
    copyIfExists(cdnPath + "/idCard/old/headerOrigin/" + idCard + "_front.png",
        ctx.idCardFrontDir.resolve(idCard + ".jpg"));
    copyIfExists(cdnPath + "/idCard/headerOrigin/" + idCard + "_back.jpg",
        ctx.idCardBackDir.resolve(idCard + ".jpg"));
    copyIfExists(cdnPath + "/idCard/old/headerOrigin/" + idCard + "_back.png",
        ctx.idCardBackDir.resolve(idCard + ".jpg"));
    if (StringUtils.hasText(person.getIdCardHeader())) {
      copyStoredAsset(person.getIdCardHeader(), ctx.idCardHeaderDir.resolve(idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getSchoolPaper())) {
      copyStoredAsset(person.getSchoolPaper(), ctx.schoolDir.resolve(realname + "_" + idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getWorkTypePaper())) {
      copyStoredAsset(person.getWorkTypePaper(), ctx.lastLevelDir.resolve(realname + "_" + idCard + ".jpg"));
    }
  }

  private void fillDocxTemplate(String templateRel, Path target, Map<String, String> vars) {
    Path template = Paths.get(cdnPath, templateRel);
    if (!Files.exists(template)) {
      return;
    }
    try {
      Files.createDirectories(target.getParent());
      try (InputStream in = Files.newInputStream(template);
          XWPFDocument doc = new XWPFDocument(in)) {
        replaceDocxVariables(doc, vars);
        doc.write(Files.newOutputStream(target));
      }
    } catch (Exception e) {
      log.warn("生成 docx 失败: {} -> {}", templateRel, target, e);
    }
  }

  private void replaceDocxVariables(XWPFDocument doc, Map<String, String> vars) {
    for (XWPFParagraph p : doc.getParagraphs()) {
      replaceParagraph(p, vars);
    }
    for (XWPFTable table : doc.getTables()) {
      for (XWPFTableRow row : table.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph p : cell.getParagraphs()) {
            replaceParagraph(p, vars);
          }
        }
      }
    }
  }

  private void replaceParagraph(XWPFParagraph paragraph, Map<String, String> vars) {
    String text = paragraph.getText();
    if (!StringUtils.hasText(text)) {
      return;
    }
    String replaced = text;
    for (Map.Entry<String, String> e : vars.entrySet()) {
      replaced = replaced.replace("${" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
    }
    if (replaced.equals(text)) {
      return;
    }
    List<XWPFRun> runs = paragraph.getRuns();
    if (runs.isEmpty()) {
      XWPFRun run = paragraph.createRun();
      run.setText(replaced);
      return;
    }
    for (int i = runs.size() - 1; i > 0; i--) {
      paragraph.removeRun(i);
    }
    runs.get(0).setText(replaced, 0);
  }

  private Path copyTemplate(String templateRel, Path target) throws Exception {
    Path template = Paths.get(cdnPath, templateRel);
    if (!Files.exists(template)) {
      return null;
    }
    Files.createDirectories(target.getParent());
    Files.copy(template, target, StandardCopyOption.REPLACE_EXISTING);
    return target;
  }

  private void copyIfExists(String source, Path target) throws Exception {
    Path src = Paths.get(source);
    if (!Files.exists(src)) {
      return;
    }
    Files.createDirectories(target.getParent());
    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
  }

  private void copyStoredAsset(String stored, Path target) throws Exception {
    if (!StringUtils.hasText(stored)) {
      return;
    }
    if (stored.startsWith("http://") || stored.startsWith("https://")) {
      return;
    }
    String relative = stored.startsWith("/") ? stored.substring(1) : stored;
    copyIfExists(Paths.get(cdnPath, relative).toString(), target);
  }

  private static Path mkdir(Path root, String name) throws Exception {
    Path dir = root.resolve(name);
    Files.createDirectories(dir);
    return dir;
  }

  private static void setCell(Sheet sheet, int rowNum, int col, String value) {
    Row row = sheet.getRow(rowNum - 1);
    if (row == null) {
      row = sheet.createRow(rowNum - 1);
    }
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    cell.setCellValue(value != null ? value : "");
  }

  private static void deleteRecursively(Path path) throws Exception {
    if (!Files.exists(path)) {
      return;
    }
    Files.walk(path)
        .sorted((a, b) -> b.compareTo(a))
        .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {
            // ignore cleanup errors
          }
        });
  }

  private WorkExp parseWorkExp(String json, String startFallback, String endFallback) {
    try {
      if (StringUtils.hasText(json)) {
        List<Map<String, String>> list = objectMapper.readValue(json, new TypeReference<>() {});
        if (!list.isEmpty()) {
          Map<String, String> item = list.get(0);
          return new WorkExp(
              item.getOrDefault("startDate", startFallback),
              item.getOrDefault("endDate", endFallback),
              item.getOrDefault("company", ""),
              item.getOrDefault("position", ""),
              item.getOrDefault("proveName", ""),
              item.getOrDefault("provePhone", ""));
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    return new WorkExp(startFallback, endFallback, "", "", "", "");
  }

  private static String levelLabel(Integer level) {
    if (level == null || level < 0 || level >= LEVEL_LABELS.length) {
      return "";
    }
    return LEVEL_LABELS[level];
  }

  private static int workAge(String startDate) {
    if (!StringUtils.hasText(startDate) || startDate.length() < 4) {
      return 0;
    }
    try {
      return LocalDate.now().getYear() - Integer.parseInt(startDate.substring(0, 4));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static int calcAge(String idCard) {
    try {
      int year = Integer.parseInt(idCard.substring(6, 10));
      return LocalDate.now().getYear() - year;
    } catch (Exception e) {
      return 0;
    }
  }

  private static String genderFromIdCard(String idCard) {
    int digit = Character.getNumericValue(idCard.charAt(idCard.length() - 2));
    return digit % 2 > 0 ? "男" : "女";
  }

  private static String formatBirthday(String idCard) {
    try {
      return idCard.substring(6, 10) + "-" + idCard.substring(10, 12) + "-" + idCard.substring(12, 14);
    } catch (Exception e) {
      return "";
    }
  }

  private static String normalize(String value) {
    return value == null ? "" : value.replace(" ", "");
  }

  private static String safe(String value) {
    return value != null ? value : "";
  }

  public interface ProgressCallback {
    void onProgress(int done, int total);
  }

  private static final class ExportContext {
    private final String folderName;
    private final String companyName;
    private final Path rootDir;
    private Path idCardFrontDir;
    private Path idCardBackDir;
    private Path idCardHeaderDir;
    private Path contractDir;
    private Path idCardMergeDir;
    private Path levelApplyDir;
    private Path commentRegisterDir;
    private Path gxRegisterDir;
    private Path workYearDir;
    private Path schoolDir;
    private Path lastLevelDir;
    private Path bmallExcel;
    private Path examGxExcel;
    private Path examAllExcel;

    private ExportContext(String cdnRoot, TrainSignUpSelfEntity signUp, OrgEntity company) {
      this.folderName = sanitize(signUp.getTitle());
      this.companyName = company != null && StringUtils.hasText(company.getName()) ? company.getName() : "";
      this.rootDir = Paths.get(cdnRoot, "down", folderName);
    }
  }

  private static String sanitize(String title) {
    if (!StringUtils.hasText(title)) {
      return "sign-up-export";
    }
    return title.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private record WorkExp(
      String startDate,
      String endDate,
      String company,
      String position,
      String proveName,
      String provePhone) {

    String rangeText() {
      if (!StringUtils.hasText(startDate) && !StringUtils.hasText(endDate)) {
        return "";
      }
      return startDate + "至" + endDate + ",在" + company + "从事" + position + "工作";
    }
  }
}

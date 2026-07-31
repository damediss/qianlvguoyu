package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 报名人员默认模板材料打包（对齐 PHP Yunxinxi peopleselfexport / taskexport）。
 * 目录与命名对齐参考包：证件照、合同、合同(合并)、身份证正反面合并、职称证、各类登记表与 Excel。
 */
@Component
@Slf4j
public class SignUpPeopleDefaultTemplatePackService {

  private static final String[] LEVEL_LABELS = {
      "初级工/五级", "中级工/四级", "高级工/三级", "技师/二级", "高级技师/一级"
  };
  private static final String[] CHINESE_DIGITS = {
      "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
  };

  private final String cdnPath;
  private final ObjectMapper objectMapper;
  private final SignUpExportAssetService assetService;
  private final IdCardImageInsetCropper idCardCropper;

  public SignUpPeopleDefaultTemplatePackService(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      ObjectMapper objectMapper,
      SignUpExportAssetService assetService,
      IdCardImageInsetCropper idCardCropper) {
    this.cdnPath = cdnPath;
    this.objectMapper = objectMapper;
    this.assetService = assetService;
    this.idCardCropper = idCardCropper;
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
      processPerson(ctx, person, index + 2, index + 1);
      index++;
      if (callback != null) {
        callback.onProgress(index, people.size());
      }
    }

    writeSummaryExcels(ctx);
    finalizeZip(ctx);
    return "down/" + ctx.folderName + ".zip";
  }

  private void initDirectories(ExportContext ctx) throws Exception {
    Files.createDirectories(ctx.rootDir);
    ctx.idCardFrontDir = mkdir(ctx.rootDir, "身份证正面");
    ctx.idCardBackDir = mkdir(ctx.rootDir, "身份证反面");
    ctx.idCardHeaderDir = mkdir(ctx.rootDir, "小二寸证件照");
    ctx.contractDir = mkdir(ctx.rootDir, "合同");
    ctx.contractMergeDir = mkdir(ctx.rootDir, "合同(合并)");
    ctx.idCardMergeDir = mkdir(ctx.rootDir, "身份证正反面合并");
    ctx.levelApplyDir = mkdir(ctx.rootDir, "职业技能等级认定申请表");
    ctx.commentRegisterDir = mkdir(ctx.rootDir, "职业技能评价登记表（2025新）");
    ctx.gxRegisterDir = mkdir(ctx.rootDir, "广西壮族自治区职业技能鉴定登记表");
    ctx.workYearDir = mkdir(ctx.rootDir, "工作年限证明");
    ctx.schoolDir = mkdir(ctx.rootDir, "毕业证");
    ctx.lastLevelDir = mkdir(ctx.rootDir, "上一等级证书");
    ctx.titlePaperDir = mkdir(ctx.rootDir, "职称证");
  }

  private void initSummaryExcels(ExportContext ctx) throws Exception {
    ctx.bmallExcel = copyOrCreateExcel(
        "template/报名人员汇总表导出.xlsx",
        ctx.rootDir.resolve("报名人员汇总表.xlsx"),
        List.of("姓名", "身份证号", "手机号", "年龄", "单位", "产业园", "工种", "等级", "学历"));
    ctx.examGxExcel = copyOrCreateExcel(
        "template/考试报名导出模板(广西).xlsx",
        ctx.rootDir.resolve("考试报名导入模板(广西).xlsx"),
        List.of("序号", "申报工种", "申报等级", "申报条件", "证件类型", "证件号", "姓名", "性别", "出生日期", "手机号"));
    ctx.examAllExcel = copyOrCreateExcel(
        "template/考试报名导出模板(有色金属行业).xlsx",
        ctx.rootDir.resolve("考试报名导入模板(有色金属行业).xlsx"),
        List.of("序号", "申报工种", "申报等级", "认定类型", "晋级类型", "申报条件", "证件类型", "证件号", "姓名", "备用", "性别"));

    if (ctx.bmallExcel != null) {
      ctx.bmallWb = openWorkbook(ctx.bmallExcel);
    }
    if (ctx.examGxExcel != null) {
      ctx.examGxWb = openWorkbook(ctx.examGxExcel);
    }
    if (ctx.examAllExcel != null) {
      ctx.examAllWb = openWorkbook(ctx.examAllExcel);
    }
  }

  private void processPerson(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      int excelRowNum,
      int sequence) throws Exception {
    String idCard = normalize(person.getIdCardNumber());
    if (!StringUtils.hasText(idCard) || idCard.length() != 18) {
      return;
    }
    String realname = safe(person.getName());
    PersonAssets assets = copyPersonAssets(ctx, person, idCard, realname);
    Map<String, String> vars = buildVars(ctx, person, idCard, realname);
    Map<String, ImageSlot> images = buildImageSlots(assets);

    fillDocxTemplate("template/职业技能等级认定申请表.docx",
        ctx.levelApplyDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    fillDocxTemplate("template/职业技能评价登记表（2025新）.docx",
        ctx.commentRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    fillDocxTemplate("template/广西壮族自治区职业技能鉴定登记表.docx",
        ctx.gxRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    fillDocxTemplate("template/工作年限证明.docx",
        ctx.workYearDir.resolve(realname + "_" + idCard + ".docx"), vars, Map.of());

    if (assets.front != null && assets.back != null) {
      writeIdCardMergeDocx(
          ctx.idCardMergeDir.resolve(realname + "_" + idCard + ".docx"),
          assets.front,
          assets.back);
    }

    appendSummaryRows(ctx, person, idCard, realname, excelRowNum, sequence, vars);
  }

  private PersonAssets copyPersonAssets(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname) throws Exception {
    PersonAssets assets = new PersonAssets();
    Long signUpId = person.getSignUpId();

    Path header = ctx.idCardHeaderDir.resolve(idCard + ".jpg");
    if (assetService.copyFirstExisting(header,
        "idCard/headerOrigin/" + idCard + "_header.jpg",
        "idCard/old/headerOrigin/" + idCard + "_header.jpg",
        "idCard/old/headerOrigin/" + idCard + "_full.png",
        person.getIdCardHeader(),
        person.getPhoto())) {
      assets.header = header;
    }

    Path front = ctx.idCardFrontDir.resolve(idCard + ".jpg");
    if (assetService.copyFirstExisting(front,
        "idCard/headerOrigin/" + idCard + "_front.jpg",
        "idCard/old/headerOrigin/" + idCard + "_front.png",
        person.getIdCardFront())) {
      idCardCropper.tightenInPlace(front);
      assets.front = front;
    }

    Path back = ctx.idCardBackDir.resolve(idCard + ".jpg");
    if (assetService.copyFirstExisting(back,
        "idCard/headerOrigin/" + idCard + "_back.jpg",
        "idCard/old/headerOrigin/" + idCard + "_back.png",
        person.getIdCardBack())) {
      idCardCropper.tightenInPlace(back);
      assets.back = back;
    }

    if (StringUtils.hasText(person.getSchoolPaper())) {
      assetService.copyTo(person.getSchoolPaper(),
          ctx.schoolDir.resolve(realname + "_" + idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getWorkTypePaper())) {
      assetService.copyTo(person.getWorkTypePaper(),
          ctx.lastLevelDir.resolve(realname + "_" + idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getTitlePaper())) {
      assetService.copyTo(person.getTitlePaper(),
          ctx.titlePaperDir.resolve(realname + "_" + idCard + ".jpg"));
    }

    Path contractFirst = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同第一页.jpg");
    Path contractExpire = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同到期页.jpg");
    Path contractSign = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同盖章页.jpg");

    boolean hasFirst = assetService.copyFirstExisting(contractFirst,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_first_page.jpg",
        person.getContactFirstPage());
    boolean hasExpire = assetService.copyFirstExisting(contractExpire,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_expire_page.jpg",
        person.getContactExpirePage());
    boolean hasSign = assetService.copyFirstExisting(contractSign,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_sign_page.jpg",
        person.getContactSignPage());

    if (hasFirst && hasExpire && hasSign) {
      try {
        TrainingImageCombiner.combineVertical(
            List.of(contractFirst, contractExpire, contractSign),
            ctx.contractMergeDir.resolve(idCard + ".jpg"));
      } catch (Exception e) {
        log.warn("合同合并失败 idCard={}", idCard, e);
      }
    }
    return assets;
  }

  private Map<String, ImageSlot> buildImageSlots(PersonAssets assets) {
    Map<String, ImageSlot> images = new LinkedHashMap<>();
    if (assets.header != null) {
      images.put("header", new ImageSlot(assets.header, 120, 168));
    }
    if (assets.front != null) {
      images.put("front", new ImageSlot(assets.front, 243, 153));
    }
    if (assets.back != null) {
      images.put("back", new ImageSlot(assets.back, 243, 153));
    }
    return images;
  }

  private Map<String, String> buildVars(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname) {
    Map<String, String> vars = new LinkedHashMap<>();
    String gender = StringUtils.hasText(person.getGender()) ? person.getGender() : genderFromIdCard(idCard);
    String birthday = formatBirthday(idCard);
    int age = person.getAge() != null && person.getAge() > 0 ? person.getAge() : calcAge(idCard);
    WorkExp exp = parseWorkExp(person.getWorkExp(), person.getWorkDateStart(), person.getWorkDateEnd());
    String companyName = firstNonBlank(person.getCompanyName(), ctx.companyName);
    String companyAddress = firstNonBlank(person.getCompanyAddress(), "");
    String companyZip = firstNonBlank(person.getZipCode(), "");
    String companyCode = firstNonBlank(person.getCompanyCode(), "");
    String companyPhone = firstNonBlank(exp.provePhone(), "");
    int reqAge = workAge(firstNonBlank(exp.startDate(), person.getWorkDateStart()));
    String workStart = firstNonBlank(exp.startDate(), person.getWorkDateStart(), person.getWorkTime());

    vars.put("name", realname);
    vars.put("realname", realname);
    vars.put("gender", gender);
    vars.put("age", String.valueOf(age));
    vars.put("birthday", birthday);
    vars.put("phone", safe(person.getPhone()));
    vars.put("idcard", idCard);
    vars.put("address", safe(person.getAddress()));
    vars.put("nation", safe(person.getNation()));
    vars.put("political", safe(person.getPolitical()));
    vars.put("education", safe(person.getEducation()));
    vars.put("school", safe(person.getSchool()));
    vars.put("major", safe(person.getMajor()));
    vars.put("school_no", safe(person.getSchoolNo()));
    vars.put("graduate_date", safe(person.getGraduateDate()));
    vars.put("com_name", companyName);
    vars.put("company_name", companyName);
    vars.put("com_phone", companyPhone);
    vars.put("com_address", companyAddress);
    vars.put("com_zipcode", companyZip);
    vars.put("com_code", companyCode);
    vars.put("com_user", safe(exp.proveName()));
    vars.put("req_work_type", safe(person.getReqWorkTypeName()));
    vars.put("req_work_level", levelLabel(person.getReqLevel()));
    vars.put("req_work_age", reqAge + "年");
    vars.put("work_type", safe(person.getWorkTypeName()));
    vars.put("work_type_level", levelLabel(person.getLevel()));
    vars.put("work_paper_no", safe(person.getPaperNo()));
    vars.put("condition", safe(person.getDeclarationCondition()));
    vars.put("paper_no", safe(person.getPaperNo()));
    vars.put("exp_date", firstNonBlank(exp.rangeShort(), exp.rangeText()));
    vars.put("exp_company", exp.company());
    vars.put("exp_position", exp.position());
    vars.put("exp_user", exp.proveName());
    vars.put("exp_phone", exp.provePhone());
    vars.put("workExp", exp.rangeOngoing());
    vars.put("work_time", safe(person.getWorkTime()));
    vars.put("work_start_date", formatYearMonth(workStart));
    vars.put("work_age", intToChinese(reqAge));
    vars.put("create_time", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
    return vars;
  }

  private void appendSummaryRows(
      ExportContext ctx,
      TrainSignUpPeopleSelfEntity person,
      String idCard,
      String realname,
      int rowNum,
      int sequence,
      Map<String, String> vars) {
    if (ctx.bmallWb != null) {
      Sheet sheet = ctx.bmallWb.getSheetAt(0);
      setCell(sheet, rowNum, 0, realname);
      setCell(sheet, rowNum, 1, idCard);
      setCell(sheet, rowNum, 2, safe(person.getPhone()));
      setCell(sheet, rowNum, 3, vars.get("age"));
      setCell(sheet, rowNum, 4, vars.get("company_name"));
      setCell(sheet, rowNum, 5, safe(person.getIndustrialPark()));
      setCell(sheet, rowNum, 6, safe(person.getWorkTypeName()));
      setCell(sheet, rowNum, 7, levelLabel(person.getLevel()));
      setCell(sheet, rowNum, 8, safe(person.getEducation()));
      setCell(sheet, rowNum, 9, safe(person.getTitleType()));
      setCell(sheet, rowNum, 11, safe(person.getSchool()));
      setCell(sheet, rowNum, 12, safe(person.getMajor()));
      setCell(sheet, rowNum, 13, safe(person.getSchoolNo()));
      setCell(sheet, rowNum, 14, safe(person.getInSchoolDate()));
      setCell(sheet, rowNum, 15, safe(person.getGraduateDate()));
      setCell(sheet, rowNum, 16, safe(person.getWorkTime()));
      setCell(sheet, rowNum, 18, safe(person.getDeclarationCondition()));
      setCell(sheet, rowNum, 19, vars.get("exp_date"));
      setCell(sheet, rowNum, 20, safe(person.getAddress()));
    }
    if (ctx.examGxWb != null) {
      Sheet sheet = ctx.examGxWb.getSheetAt(0);
      setCell(sheet, rowNum, 0, String.valueOf(sequence));
      setCell(sheet, rowNum, 1, safe(person.getReqWorkTypeName()));
      setCell(sheet, rowNum, 2, levelLabel(person.getReqLevel()));
      setCell(sheet, rowNum, 3, safe(person.getDeclarationCondition()));
      setCell(sheet, rowNum, 4, "居民身份证");
      setCell(sheet, rowNum, 5, idCard);
      setCell(sheet, rowNum, 6, realname);
      setCell(sheet, rowNum, 7, vars.get("gender"));
      setCell(sheet, rowNum, 8, formatBirthday(idCard));
      setCell(sheet, rowNum, 9, safe(person.getPhone()));
      setCell(sheet, rowNum, 10, person.getWorkTypeId() != null && person.getWorkTypeId() > 0 ? "是" : "否");
      setCell(sheet, rowNum, 11, levelLabel(person.getLevel()));
      setCell(sheet, rowNum, 12, safe(person.getPaperNo()));
      setCell(sheet, rowNum, 13, safe(person.getEducation()));
      setCell(sheet, rowNum, 16, vars.get("company_name"));
      setCell(sheet, rowNum, 17, vars.get("company_name"));
      setCell(sheet, rowNum, 18, "晋级认定");
      setCell(sheet, rowNum, 19, safe(person.getMajor()));
      setCell(sheet, rowNum, 20, "正考");
      setCell(sheet, rowNum, 21, safe(person.getNation()));
      setCell(sheet, rowNum, 22, safe(person.getWorkTime()));
      setCell(sheet, rowNum, 24, "1");
      setCell(sheet, rowNum, 25, safe(person.getAddress()));
      setCell(sheet, rowNum, 26, safe(person.getPolitical()));
      setCell(sheet, rowNum, 27, safe(person.getSchoolNo()));
      setCell(sheet, rowNum, 28, vars.get("exp_date"));
      setCell(sheet, rowNum, 29, safe(person.getAddress()));
      setCell(sheet, rowNum, 30, "邮寄");
      setCell(sheet, rowNum, 31, vars.get("com_zipcode"));
      setCell(sheet, rowNum, 32, vars.get("com_address"));
    }
    if (ctx.examAllWb != null) {
      Sheet sheet = ctx.examAllWb.getSheetAt(0);
      String isInit = person.getWorkTypeId() != null && person.getWorkTypeId() > 0 ? "再次认定" : "初次认定";
      setCell(sheet, rowNum, 0, String.valueOf(sequence));
      setCell(sheet, rowNum, 1, safe(person.getReqWorkTypeName()));
      setCell(sheet, rowNum, 2, levelLabel(person.getReqLevel()));
      setCell(sheet, rowNum, 3, isInit);
      setCell(sheet, rowNum, 4, "晋级认定");
      setCell(sheet, rowNum, 5, safe(person.getDeclarationCondition()));
      setCell(sheet, rowNum, 6, "居民身份证");
      setCell(sheet, rowNum, 7, idCard);
      setCell(sheet, rowNum, 8, realname);
      setCell(sheet, rowNum, 10, vars.get("gender"));
      setCell(sheet, rowNum, 11, formatBirthday(idCard));
    }
  }

  private void writeSummaryExcels(ExportContext ctx) throws Exception {
    writeWorkbook(ctx.bmallWb, ctx.bmallExcel);
    writeWorkbook(ctx.examGxWb, ctx.examGxExcel);
    writeWorkbook(ctx.examAllWb, ctx.examAllExcel);
  }

  private void finalizeZip(ExportContext ctx) throws Exception {
    Path oldZip = Paths.get(cdnPath, "down", ctx.folderName + ".zip");
    Files.deleteIfExists(oldZip);
    List<Path> entries = new ArrayList<>();
    entries.add(ctx.idCardFrontDir);
    entries.add(ctx.idCardBackDir);
    entries.add(ctx.idCardHeaderDir);
    entries.add(ctx.idCardMergeDir);
    entries.add(ctx.levelApplyDir);
    entries.add(ctx.commentRegisterDir);
    entries.add(ctx.gxRegisterDir);
    entries.add(ctx.workYearDir);
    entries.add(ctx.contractDir);
    entries.add(ctx.contractMergeDir);
    entries.add(ctx.schoolDir);
    entries.add(ctx.lastLevelDir);
    entries.add(ctx.titlePaperDir);
    if (ctx.bmallExcel != null) {
      entries.add(ctx.bmallExcel);
    }
    if (ctx.examGxExcel != null) {
      entries.add(ctx.examGxExcel);
    }
    if (ctx.examAllExcel != null) {
      entries.add(ctx.examAllExcel);
    }
    TrainingZipUtil.zipEntries(oldZip, entries.stream().filter(p -> p != null && Files.exists(p)).toList());
    deleteRecursively(ctx.rootDir);
  }

  private void fillDocxTemplate(
      String templateRel,
      Path target,
      Map<String, String> vars,
      Map<String, ImageSlot> images) {
    try {
      byte[] raw = loadClasspathTemplate(templateRel);
      if (!isOoxmlZip(raw)) {
        log.warn("docx 模板不是有效 OOXML，跳过: {}", templateRel);
        return;
      }
      Files.createDirectories(target.getParent());
      // 先修复被 Word 拆开的 ${var}，再替换文本（图片占位留给后续 POI 插入）
      byte[] replaced = TrainingDocxVariableReplacer.replaceAll(raw, vars);
      try (InputStream in = new ByteArrayInputStream(replaced);
          XWPFDocument doc = new XWPFDocument(in)) {
        replaceDocxImages(doc, images);
        try (OutputStream out = Files.newOutputStream(target)) {
          doc.write(out);
        }
      }
    } catch (Exception e) {
      log.warn("生成 docx 失败: {} -> {}", templateRel, target, e);
    }
  }

  /** 无模板时仍生成正反面合并 docx（对齐参考包命名）。 */
  private void writeIdCardMergeDocx(Path target, Path front, Path back) {
    Map<String, ImageSlot> images = Map.of(
        "front", new ImageSlot(front, 243, 153),
        "back", new ImageSlot(back, 243, 153));
    ClassPathResource resource = new ClassPathResource("template/身份证正反面合并模板.docx");
    if (resource.exists()) {
      fillDocxTemplate("template/身份证正反面合并模板.docx", target, Map.of(), images);
      try {
        if (Files.exists(target) && Files.size(target) > 0) {
          return;
        }
      } catch (Exception ignored) {
        // fall through to programmatic generation
      }
    }
    try {
      Files.createDirectories(target.getParent());
      try (XWPFDocument doc = new XWPFDocument()) {
        addCenteredImage(doc, front, 243, 153);
        addCenteredImage(doc, back, 243, 153);
        try (OutputStream out = Files.newOutputStream(target)) {
          doc.write(out);
        }
      }
    } catch (Exception e) {
      log.warn("生成身份证正反面合并失败: {}", target, e);
    }
  }

  private void addCenteredImage(XWPFDocument doc, Path image, int widthPx, int heightPx) throws Exception {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    try (InputStream in = Files.newInputStream(image)) {
      run.addPicture(
          in,
          pictureType(image),
          image.getFileName().toString(),
          Units.pixelToEMU(widthPx),
          Units.pixelToEMU(heightPx));
    }
  }

  private void replaceDocxImages(XWPFDocument doc, Map<String, ImageSlot> images) {
    if (images == null || images.isEmpty()) {
      return;
    }
    for (XWPFParagraph p : doc.getParagraphs()) {
      replaceParagraphImages(p, images);
    }
    for (XWPFTable table : doc.getTables()) {
      for (XWPFTableRow row : table.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph p : cell.getParagraphs()) {
            replaceParagraphImages(p, images);
          }
        }
      }
    }
  }

  private void replaceParagraphImages(XWPFParagraph paragraph, Map<String, ImageSlot> images) {
    String text = paragraph.getText();
    if (!StringUtils.hasText(text)) {
      return;
    }
    for (Map.Entry<String, ImageSlot> e : images.entrySet()) {
      String token = "${" + e.getKey() + "}";
      if (!text.contains(token)) {
        continue;
      }
      List<XWPFRun> runs = paragraph.getRuns();
      if (runs == null || runs.isEmpty()) {
        continue;
      }
      for (int i = runs.size() - 1; i > 0; i--) {
        paragraph.removeRun(i);
      }
      XWPFRun run = runs.get(0);
      run.setText("", 0);
      ImageSlot slot = e.getValue();
      try (InputStream in = Files.newInputStream(slot.path())) {
        run.addPicture(
            in,
            pictureType(slot.path()),
            slot.path().getFileName().toString(),
            Units.pixelToEMU(slot.widthPx()),
            Units.pixelToEMU(slot.heightPx()));
      } catch (Exception ex) {
        log.warn("插入图片失败 key={} path={}", e.getKey(), slot.path(), ex);
        run.setText(token, 0);
      }
      text = paragraph.getText();
    }
  }

  private Path copyOrCreateExcel(String templateRel, Path target, List<String> fallbackHeaders) throws Exception {
    Files.createDirectories(target.getParent());
    try {
      byte[] raw = loadClasspathTemplate(templateRel);
      if (isOoxmlZip(raw)) {
        Files.write(target, raw);
        return target;
      }
      log.warn("excel 模板不是有效 OOXML，使用空表头回退: {}", templateRel);
    } catch (Exception e) {
      log.warn("读取 classpath excel 模板失败，使用空表头回退: {}", templateRel, e);
    }
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Sheet1");
      Row header = sheet.createRow(0);
      for (int i = 0; i < fallbackHeaders.size(); i++) {
        header.createCell(i).setCellValue(fallbackHeaders.get(i));
      }
      try (OutputStream out = Files.newOutputStream(target)) {
        wb.write(out);
      }
    }
    return target;
  }

  private byte[] loadClasspathTemplate(String classpathLocation) throws Exception {
    ClassPathResource resource = new ClassPathResource(classpathLocation);
    if (!resource.exists()) {
      throw new IllegalStateException("classpath 模板不存在: " + classpathLocation);
    }
    try (InputStream in = resource.getInputStream()) {
      return in.readAllBytes();
    }
  }

  private static Workbook openWorkbook(Path path) throws Exception {
    try (InputStream in = Files.newInputStream(path)) {
      return WorkbookFactory.create(in);
    }
  }

  private static void writeWorkbook(Workbook wb, Path path) throws Exception {
    if (wb == null || path == null) {
      return;
    }
    try (OutputStream out = Files.newOutputStream(path)) {
      wb.write(out);
    } finally {
      wb.close();
    }
  }

  private static boolean isOoxmlZip(byte[] data) {
    return data != null && data.length >= 2 && data[0] == 0x50 && data[1] == 0x4B;
  }

  private static int pictureType(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    if (name.endsWith(".png")) {
      return Document.PICTURE_TYPE_PNG;
    }
    if (name.endsWith(".gif")) {
      return Document.PICTURE_TYPE_GIF;
    }
    if (name.endsWith(".bmp")) {
      return Document.PICTURE_TYPE_BMP;
    }
    return Document.PICTURE_TYPE_JPEG;
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

  private static String formatYearMonth(String date) {
    if (!StringUtils.hasText(date)) {
      return "";
    }
    String normalized = date.trim().replace('.', '-').replace('/', '-');
    try {
      if (normalized.length() >= 7) {
        String year = normalized.substring(0, 4);
        String month = normalized.substring(5, 7);
        return year + "年" + month + "月";
      }
      if (normalized.length() >= 4) {
        return normalized.substring(0, 4) + "年";
      }
    } catch (Exception ignored) {
      // fall through
    }
    return date;
  }

  /** 对齐 PHP intToChinese（年限常用小数）。 */
  private static String intToChinese(int num) {
    if (num < 0) {
      return "";
    }
    if (num < 10) {
      return CHINESE_DIGITS[num];
    }
    if (num < 20) {
      return "十" + (num % 10 == 0 ? "" : CHINESE_DIGITS[num % 10]);
    }
    if (num < 100) {
      int tens = num / 10;
      int ones = num % 10;
      return CHINESE_DIGITS[tens] + "十" + (ones == 0 ? "" : CHINESE_DIGITS[ones]);
    }
    return String.valueOf(num);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.replace(" ", "");
  }

  private static String safe(String value) {
    return value != null ? value : "";
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  public interface ProgressCallback {
    void onProgress(int done, int total);
  }

  private record ImageSlot(Path path, int widthPx, int heightPx) {}

  private static final class PersonAssets {
    private Path header;
    private Path front;
    private Path back;
  }

  private static final class ExportContext {
    private final String folderName;
    private final String companyName;
    private final Path rootDir;
    private Path idCardFrontDir;
    private Path idCardBackDir;
    private Path idCardHeaderDir;
    private Path contractDir;
    private Path contractMergeDir;
    private Path idCardMergeDir;
    private Path levelApplyDir;
    private Path commentRegisterDir;
    private Path gxRegisterDir;
    private Path workYearDir;
    private Path schoolDir;
    private Path lastLevelDir;
    private Path titlePaperDir;
    private Path bmallExcel;
    private Path examGxExcel;
    private Path examAllExcel;
    private Workbook bmallWb;
    private Workbook examGxWb;
    private Workbook examAllWb;

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
      return safe(startDate) + "至" + safe(endDate) + ",在" + safe(company) + "从事" + safe(position) + "工作";
    }

    String rangeShort() {
      if (!StringUtils.hasText(startDate) && !StringUtils.hasText(endDate)) {
        return "";
      }
      return safe(startDate) + "至" + safe(endDate);
    }

    String rangeOngoing() {
      if (!StringUtils.hasText(startDate)) {
        return "";
      }
      return safe(startDate) + "至今，在" + safe(company) + "从事" + safe(position) + "工作";
    }

    private static String safe(String value) {
      return value != null ? value : "";
    }
  }
}

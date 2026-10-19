package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.util.SensitiveMaskUtils;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.PlatformOrganizationLookupMapper;
import com.guoyu.platform.system.mapper.PlatformOrganizationLookupMapper.PlatformOrganizationBrief;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
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

  /** 身份证正反面合并文档：宽 8.6cm、高 5.4cm（不按原图纵横比缩放）。 */
  private static final double ID_CARD_MERGE_WIDTH_CM = 8.6;
  private static final double ID_CARD_MERGE_HEIGHT_CM = 5.4;

  private static final String FONT_SONG_TI = "宋体";
  /** 中文小四 = 12pt */
  private static final int FONT_SIZE_XIAO_SI_PT = 12;

  private static final String LEVEL_APPLY_TEMPLATE = "template/职业技能等级认定申请表.docx";
  private static final String COMMENT_REGISTER_TEMPLATE = "template/职业技能评价登记表（2025新）.docx";
  /** 勾选 □ / ☑ */
  private static final String CHECKBOX_OFF = "\u25A1";
  private static final String CHECKBOX_ON = "\u2611";
  private static final String[] LEVEL_LABELS = {
      "五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"
  };
  private static final String[] CHINESE_DIGITS = {
      "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
  };

  private final String cdnPath;
  private final ObjectMapper objectMapper;
  private final SignUpExportAssetService assetService;
  private final IdCardImageInsetCropper idCardCropper;
  private final TrainInfoClassMapper classMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final PlatformOrganizationLookupMapper platformOrganizationLookupMapper;
  private final TrainSignUpPeopleSelfMapper peopleSelfMapper;

  public SignUpPeopleDefaultTemplatePackService(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      ObjectMapper objectMapper,
      SignUpExportAssetService assetService,
      IdCardImageInsetCropper idCardCropper,
      TrainInfoClassMapper classMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      PlatformOrganizationLookupMapper platformOrganizationLookupMapper,
      TrainSignUpPeopleSelfMapper peopleSelfMapper) {
    this.cdnPath = cdnPath;
    this.objectMapper = objectMapper;
    this.assetService = assetService;
    this.idCardCropper = idCardCropper;
    this.classMapper = classMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.platformOrganizationLookupMapper = platformOrganizationLookupMapper;
    this.peopleSelfMapper = peopleSelfMapper;
  }

  public String run(
      TrainSignUpSelfEntity signUp,
      OrgEntity company,
      List<TrainSignUpPeopleSelfEntity> people,
      ProgressCallback callback) throws Exception {
    return run(signUp, company, people, null, callback);
  }

  /**
   * @param taskId 非空时工作目录与 zip 带任务后缀，避免覆盖/命中上一次导出包
   */
  public String run(
      TrainSignUpSelfEntity signUp,
      OrgEntity company,
      List<TrainSignUpPeopleSelfEntity> people,
      Long taskId,
      ProgressCallback callback) throws Exception {
    ExportContext ctx = new ExportContext(cdnPath, signUp, company, taskId);
    loadClassContext(ctx, signUp);
    initDirectories(ctx);
    initSummaryExcels(ctx);
    // 同一次导出共用一个素材解析器：同一候选路径只探测/下载一次
    SignUpExportAssetService.AssetResolver assetResolver = assetService.newResolver();

    int index = 0;
    for (TrainSignUpPeopleSelfEntity person : people) {
      processPerson(ctx, person, index + 2, index + 1, assetResolver);
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
      int sequence,
      SignUpExportAssetService.AssetResolver assetResolver) throws Exception {
    String idCard = normalize(person.getIdCardNumber());
    if (!StringUtils.hasText(idCard) || idCard.length() != 18) {
      return;
    }
    String realname = safe(person.getName());
    PersonAssets assets = copyPersonAssets(ctx, person, idCard, realname, assetResolver);
    Map<String, String> vars = buildVars(ctx, person, idCard, realname);
    Map<String, ImageSlot> images = buildImageSlots(assets);

    fillDocxTemplate(LEVEL_APPLY_TEMPLATE,
        ctx.levelApplyDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    fillDocxTemplate(COMMENT_REGISTER_TEMPLATE,
        ctx.commentRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    fillDocxTemplate("template/广西壮族自治区职业技能鉴定登记表.docx",
        ctx.gxRegisterDir.resolve(realname + "_" + idCard + ".docx"), vars, images);
    // 工作年限证明：优先班级单位联系人/电话，缺省再回落人员工作经历证明人
    Map<String, String> workYearVars = new LinkedHashMap<>(vars);
    workYearVars.put("com_user", firstNonBlank(ctx.companyContactName, vars.get("exp_user"), ""));
    workYearVars.put("com_phone", firstNonBlank(ctx.companyContactPhone, vars.get("exp_phone"), ""));
    fillDocxTemplate("template/工作年限证明.docx",
        ctx.workYearDir.resolve(realname + "_" + idCard + ".docx"), workYearVars, Map.of());

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
      String realname,
      SignUpExportAssetService.AssetResolver assetResolver) throws Exception {
    PersonAssets assets = new PersonAssets();
    Long signUpId = person.getSignUpId();

    Path header = ctx.idCardHeaderDir.resolve(idCard + ".jpg");
    if (assetResolver.copyFirstExisting(header, withRecordCandidates(
        StringUtils.hasText(person.getIdCardHeader()) || StringUtils.hasText(person.getPhoto()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_header.jpg",
        "idCard/old/headerOrigin/" + idCard + "_header.jpg",
        "idCard/old/headerOrigin/" + idCard + "_full.png",
        person.getIdCardHeader(),
        person.getPhoto()))) {
      assets.header = header;
    }

    Path front = ctx.idCardFrontDir.resolve(idCard + ".jpg");
    if (assetResolver.copyFirstExisting(front, withRecordCandidates(
        StringUtils.hasText(person.getIdCardFront()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_front.jpg",
        "idCard/old/headerOrigin/" + idCard + "_front.png",
        person.getIdCardFront()))) {
      idCardCropper.tightenInPlace(front);
      assets.front = front;
    }

    Path back = ctx.idCardBackDir.resolve(idCard + ".jpg");
    if (assetResolver.copyFirstExisting(back, withRecordCandidates(
        StringUtils.hasText(person.getIdCardBack()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_back.jpg",
        "idCard/old/headerOrigin/" + idCard + "_back.png",
        person.getIdCardBack()))) {
      idCardCropper.tightenInPlace(back);
      assets.back = back;
    }

    if (StringUtils.hasText(person.getSchoolPaper())) {
      assetResolver.copyTo(person.getSchoolPaper(),
          ctx.schoolDir.resolve(realname + "_" + idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getWorkTypePaper())) {
      assetResolver.copyTo(person.getWorkTypePaper(),
          ctx.lastLevelDir.resolve(realname + "_" + idCard + ".jpg"));
    }
    if (StringUtils.hasText(person.getTitlePaper())) {
      assetResolver.copyTo(person.getTitlePaper(),
          ctx.titlePaperDir.resolve(realname + "_" + idCard + ".jpg"));
    }

    Path contractFirst = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同第一页.jpg");
    Path contractExpire = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同到期页.jpg");
    Path contractSign = ctx.contractDir.resolve(realname + "_" + idCard + "_劳动合同盖章页.jpg");

    boolean hasFirst = assetResolver.copyFirstExisting(contractFirst, withRecordCandidates(
        StringUtils.hasText(person.getContactFirstPage()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_first_page.jpg",
        person.getContactFirstPage()));
    boolean hasExpire = assetResolver.copyFirstExisting(contractExpire, withRecordCandidates(
        StringUtils.hasText(person.getContactExpirePage()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_expire_page.jpg",
        person.getContactExpirePage()));
    boolean hasSign = assetResolver.copyFirstExisting(contractSign, withRecordCandidates(
        StringUtils.hasText(person.getContactSignPage()),
        assetResolver::existsLocally,
        "idCard/headerOrigin/" + idCard + "_" + signUpId + "_contact_sign_page.jpg",
        person.getContactSignPage()));

    if (hasFirst && hasExpire && hasSign) {
      try {
        TrainingImageCombiner.combineVertical(
            List.of(contractFirst, contractExpire, contractSign),
            ctx.contractMergeDir.resolve(idCard + ".jpg"));
      } catch (Exception e) {
        log.warn("合同合并失败 idCard={}", SensitiveMaskUtils.maskIdCard(idCard), e);
      }
    }
    return assets;
  }

  /**
   * 槽位候选路径：有材料记录时按原顺序全试；整槽在库中无记录时，约定路径只保留本机磁盘能直接命中的。
   * 无记录的人盲试 idCard/** 约定路径只能靠远端 CDN 碰运气，白白多打若干次网络请求。
   */
  static List<String> withRecordCandidates(
      boolean hasRecord, Predicate<String> existsLocally, String... candidates) {
    List<String> resolved = new ArrayList<>(candidates.length);
    for (String candidate : candidates) {
      if (hasRecord || (StringUtils.hasText(candidate) && existsLocally.test(candidate))) {
        resolved.add(candidate);
      }
    }
    return resolved;
  }

  private Map<String, ImageSlot> buildImageSlots(PersonAssets assets) {
    Map<String, ImageSlot> images = new LinkedHashMap<>();
    if (assets.header != null) {
      images.put("header", ImageSlot.px(assets.header, 120, 168));
    }
    if (assets.front != null) {
      images.put("front", ImageSlot.px(assets.front, 243, 153));
    }
    if (assets.back != null) {
      images.put("back", ImageSlot.px(assets.back, 243, 153));
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
    String companyName = resolveCompanyName(
        person.getCompanyName(),
        ctx.companyName,
        exp.company(),
        ctx.signUpTitle);
    PlatformOrganizationBrief legacyOrg = lookupLegacyOrganization(ctx, companyName);
    String legacyAddress = legacyOrg != null ? safe(legacyOrg.address()) : "";
    String legacyPhone = legacyOrg != null ? safe(legacyOrg.phone()) : "";
    String legacyZip = legacyOrg != null ? RegionZipCodeSupport.normalizeZip(legacyOrg.zipcode()) : "";
    String legacyCode = legacyOrg != null ? safe(legacyOrg.titleCode()) : "";
    // 认定/评价表企业电话：企业主数据 → 证明人 → 学员手机
    String companyPhone = firstNonBlank(ctx.companyPhone, legacyPhone, exp.provePhone(), person.getPhone());
    String companyAddress = firstNonBlank(
        person.getCompanyAddress(),
        legacyAddress,
        ctx.companyAddress,
        person.getAddress());
    String companyZip = resolveCompanyZipcode(
        ctx,
        companyName,
        person.getZipCode(),
        legacyZip,
        companyAddress,
        person.getAddress(),
        legacyAddress);
    String companyCode = firstNonBlank(person.getCompanyCode(), legacyCode, ctx.companyCode);
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
    vars.put("graduate_date", formatGraduateDate(person.getGraduateDate()));
    vars.put("com_name", companyName);
    vars.put("company_name", companyName);
    vars.put("com_phone", companyPhone);
    vars.put("com_address", companyAddress);
    vars.put("com_zipcode", companyZip);
    vars.put("com_code", companyCode);
    vars.put("com_user", firstNonBlank(exp.proveName(), ""));
    Long reqWorkTypeId = person.getReqWorkTypeId() != null && person.getReqWorkTypeId() > 0
        ? person.getReqWorkTypeId()
        : ctx.appointment.workTypeId();
    String reqWorkType = firstNonBlank(
        person.getReqWorkTypeName(),
        lookupWorkTypeTitle(person.getReqWorkTypeId()),
        ctx.appointment.workTypeName(),
        lookupWorkTypeTitle(ctx.appointment.workTypeId()),
        exp.position(),
        person.getJog(),
        person.getJob(),
        person.getWorkTypeName());
    Integer reqLevelIndex = resolveReqLevelIndex(
        person.getReqLevel(),
        person.getReqWorkTypeId(),
        person.getReqWorkTypeName(),
        ctx.appointment.workTypeId(),
        ctx.appointment.classLevel());
    vars.put("req_work_type", reqWorkType);
    vars.put("req_work_level", levelLabel(reqLevelIndex));
    vars.put("req_work_code", firstNonBlank(
        lookupWorkTypeCode(reqWorkTypeId),
        ctx.appointment.workTypeCode()));
    vars.put("req_work_age", reqAge + "年");
    String currentWorkType = firstNonBlank(
        person.getWorkTypeName(),
        lookupWorkTypeTitle(person.getWorkTypeId()));
    vars.put("work_type", currentWorkType);
    vars.put("work_type_level", optionalCurrentLevelLabel(person));
    vars.put("work_paper_no", firstNonBlank(person.getPaperNo(), person.getWorkTypePaperNo()));
    vars.put("condition", safe(person.getDeclarationCondition()));
    boolean promoteEval = hasPreviousLevelCertificate(person);
    vars.put("eval_promote", promoteEval ? "1" : "0");
    vars.put("paper_no", promoteEval
        ? firstNonBlank(person.getPaperNo(), person.getWorkTypePaperNo())
        : "");
    vars.put("exp_date", firstNonBlank(exp.rangeShort(), exp.rangeText()));
    vars.put("exp_company", exp.company());
    vars.put("exp_position", exp.position());
    vars.put("exp_user", exp.proveName());
    vars.put("exp_phone", exp.provePhone());
    // 认定申请表「个人工作简历」：入职日期至今 + 本公司 + 申报工种，不用学员自填经历里的单位/岗位
    vars.put("workExp", levelApplyWorkResume(
        workStart, firstNonBlank(ctx.companyName, companyName), reqWorkType));
    vars.put("work_time", firstNonBlank(person.getWorkTime(), workStart));
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
      setCell(sheet, rowNum, 6, firstNonBlank(person.getWorkTypeName(), vars.get("work_type")));
      setCell(sheet, rowNum, 7, firstNonBlank(vars.get("work_type_level"), ""));
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
      setCell(sheet, rowNum, 1, vars.get("req_work_type"));
      setCell(sheet, rowNum, 2, vars.get("req_work_level"));
      setCell(sheet, rowNum, 3, safe(person.getDeclarationCondition()));
      setCell(sheet, rowNum, 4, "居民身份证");
      setCell(sheet, rowNum, 5, idCard);
      setCell(sheet, rowNum, 6, realname);
      setCell(sheet, rowNum, 7, vars.get("gender"));
      setCell(sheet, rowNum, 8, formatBirthday(idCard));
      setCell(sheet, rowNum, 9, safe(person.getPhone()));
      setCell(sheet, rowNum, 10, person.getWorkTypeId() != null && person.getWorkTypeId() > 0 ? "是" : "否");
      setCell(sheet, rowNum, 11, vars.get("work_type_level"));
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
      setCell(sheet, rowNum, 1, vars.get("req_work_type"));
      setCell(sheet, rowNum, 2, vars.get("req_work_level"));
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
    // 清理同标题无 _t 后缀的旧 zip，避免 check 兜底命中旧包（全量导出 → 勾选导出误下全部）
    if (ctx.taskId != null && ctx.taskId > 0) {
      Path legacyZip = Paths.get(cdnPath, "down", sanitize(ctx.signUpTitle) + ".zip");
      if (!legacyZip.equals(oldZip)) {
        Files.deleteIfExists(legacyZip);
      }
    }
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
      if (COMMENT_REGISTER_TEMPLATE.equals(templateRel)) {
        replaced = applyCommentRegisterEvalChecks(replaced, "1".equals(vars.get("eval_promote")));
      }
      try (InputStream in = new ByteArrayInputStream(replaced);
          XWPFDocument doc = new XWPFDocument(in)) {
        replaceDocxImages(doc, images);
        if (LEVEL_APPLY_TEMPLATE.equals(templateRel)) {
          applySongTiXiaoSiToLevelApplyForm(doc);
        }
        try (OutputStream out = Files.newOutputStream(target)) {
          doc.write(out);
        }
      }
    } catch (Exception e) {
      log.warn("生成 docx 失败: {} -> {}", templateRel, target, e);
    }
  }

  /**
   * 评价登记表「评价情况」：有上一等级证书勾选晋级，否则勾选初次。
   */
  static boolean hasPreviousLevelCertificate(TrainSignUpPeopleSelfEntity person) {
    if (person == null) {
      return false;
    }
    if (StringUtils.hasText(person.getPaperNo()) || StringUtils.hasText(person.getWorkTypePaperNo())) {
      return true;
    }
    if (StringUtils.hasText(person.getWorkTypePaper())) {
      return true;
    }
    return person.getWorkTypePaperHas() != null && person.getWorkTypePaperHas() > 0;
  }

  /**
   * 将模板中的「□初次评价 / □晋级评价」按是否晋级改为 ☑。
   */
  static byte[] applyCommentRegisterEvalChecks(byte[] docxBytes, boolean promote) throws Exception {
    if (docxBytes == null || docxBytes.length == 0) {
      return docxBytes;
    }
    String firstChecked = CHECKBOX_ON + "初次评价";
    String firstUnchecked = CHECKBOX_OFF + "初次评价";
    String promoteChecked = CHECKBOX_ON + "晋级评价";
    String promoteUnchecked = CHECKBOX_OFF + "晋级评价";
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new ByteArrayInputStream(docxBytes));
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out)) {
      java.util.zip.ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        byte[] content = zis.readAllBytes();
        if (TrainingDocxTemplateValidator.isWordPart(entry.getName())) {
          String xml = new String(content, java.nio.charset.StandardCharsets.UTF_8);
          if (promote) {
            xml = xml.replace(promoteUnchecked, promoteChecked);
            xml = xml.replace(firstChecked, firstUnchecked);
          } else {
            xml = xml.replace(firstUnchecked, firstChecked);
            xml = xml.replace(promoteChecked, promoteUnchecked);
          }
          content = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        java.util.zip.ZipEntry outEntry = new java.util.zip.ZipEntry(entry.getName());
        zos.putNextEntry(outEntry);
        zos.write(content);
        zos.closeEntry();
      }
    }
    return out.toByteArray();
  }

  /**
   * 认定申请表填入区（表格内）统一为「宋体 / 小四」。
   * 表外大标题等段落不动，避免把文头字号压成小四。
   */
  static void applySongTiXiaoSiToLevelApplyForm(XWPFDocument doc) {
    if (doc == null) {
      return;
    }
    for (XWPFTable table : doc.getTables()) {
      applySongTiXiaoSiOnTable(table);
    }
  }

  private static void applySongTiXiaoSiOnTable(XWPFTable table) {
    for (XWPFTableRow row : table.getRows()) {
      for (XWPFTableCell cell : row.getTableCells()) {
        for (XWPFParagraph p : cell.getParagraphs()) {
          applySongTiXiaoSiOnParagraph(p);
        }
        for (XWPFTable nested : cell.getTables()) {
          applySongTiXiaoSiOnTable(nested);
        }
      }
    }
  }

  private static void applySongTiXiaoSiOnParagraph(XWPFParagraph paragraph) {
    if (paragraph == null) {
      return;
    }
    for (XWPFRun run : paragraph.getRuns()) {
      if (run == null) {
        continue;
      }
      // 跳过纯图片 run（无文本）
      if (!StringUtils.hasText(run.text())) {
        continue;
      }
      applySongTiXiaoSi(run);
    }
  }

  static void applySongTiXiaoSi(XWPFRun run) {
    if (run == null) {
      return;
    }
    // 去掉主题字体（Word 里常显示为「宋体 (标题)」），改为显式宋体 + 小四
    CTRPr rPr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
    CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
    if (fonts.isSetAsciiTheme()) {
      fonts.unsetAsciiTheme();
    }
    if (fonts.isSetEastAsiaTheme()) {
      fonts.unsetEastAsiaTheme();
    }
    if (fonts.isSetHAnsiTheme()) {
      fonts.unsetHAnsiTheme();
    }
    if (fonts.isSetCstheme()) {
      fonts.unsetCstheme();
    }
    fonts.setAscii(FONT_SONG_TI);
    fonts.setEastAsia(FONT_SONG_TI);
    fonts.setHAnsi(FONT_SONG_TI);
    fonts.setCs(FONT_SONG_TI);
    fonts.setHint(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHint.EAST_ASIA);

    BigInteger halfPoints = BigInteger.valueOf(FONT_SIZE_XIAO_SI_PT * 2L);
    CTHpsMeasure sz = rPr.sizeOfSzArray() > 0 ? rPr.getSzArray(0) : rPr.addNewSz();
    sz.setVal(halfPoints);
    CTHpsMeasure szCs = rPr.sizeOfSzCsArray() > 0 ? rPr.getSzCsArray(0) : rPr.addNewSzCs();
    szCs.setVal(halfPoints);
  }

  /** 无模板时仍生成正反面合并 docx（对齐参考包命名）。 */
  private void writeIdCardMergeDocx(Path target, Path front, Path back) {
    Map<String, ImageSlot> images = Map.of(
        "front", ImageSlot.idCardMerge(front),
        "back", ImageSlot.idCardMerge(back));
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
        addCenteredImage(doc, front, cmToEmu(ID_CARD_MERGE_WIDTH_CM), cmToEmu(ID_CARD_MERGE_HEIGHT_CM), false);
        addCenteredImage(doc, back, cmToEmu(ID_CARD_MERGE_WIDTH_CM), cmToEmu(ID_CARD_MERGE_HEIGHT_CM), false);
        try (OutputStream out = Files.newOutputStream(target)) {
          doc.write(out);
        }
      }
    } catch (Exception e) {
      log.warn("生成身份证正反面合并失败: {}", target, e);
    }
  }

  private void addCenteredImage(XWPFDocument doc, Path image, int widthEmu, int heightEmu, boolean lockAspect)
      throws Exception {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    try (InputStream in = Files.newInputStream(image)) {
      insertPicture(run, in, pictureType(image), image.getFileName().toString(), widthEmu, heightEmu, lockAspect);
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
        insertPicture(
            run,
            in,
            pictureType(slot.path()),
            slot.path().getFileName().toString(),
            slot.widthEmu(),
            slot.heightEmu(),
            slot.lockAspect());
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
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
        if (!list.isEmpty()) {
          Map<String, Object> item = list.get(0);
          return new WorkExp(
              textOf(item.get("startDate"), startFallback),
              textOf(item.get("endDate"), endFallback),
              textOf(item.get("company"), ""),
              textOf(item.get("position"), ""),
              textOf(item.get("proveName"), textOf(item.get("proveUser"), "")),
              textOf(item.get("provePhone"), ""));
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    return new WorkExp(startFallback, endFallback, "", "", "", "");
  }

  private static String textOf(Object value, String fallback) {
    if (value == null) {
      return fallback != null ? fallback : "";
    }
    String text = String.valueOf(value).trim();
    return StringUtils.hasText(text) ? text : (fallback != null ? fallback : "");
  }

  /**
   * 现工种等级：库默认 {@code "0"} 与「五级」同码；无工种/证书痕迹时留空，避免认定表误填五级。
   */
  public static String optionalCurrentLevelLabel(TrainSignUpPeopleSelfEntity person) {
    if (person == null) {
      return "";
    }
    String raw = person.getLevel();
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String t = raw.trim();
    boolean numericZero = "0".equals(t);
    boolean hasEvidence = (person.getWorkTypeId() != null && person.getWorkTypeId() > 0)
        || StringUtils.hasText(person.getWorkTypeName())
        || StringUtils.hasText(person.getPaperNo())
        || (person.getWorkTypePaperHas() != null && person.getWorkTypePaperHas() > 0)
        || StringUtils.hasText(person.getWorkTypePaper());
    if (numericZero && !hasEvidence) {
      return "";
    }
    return SignUpLevelSupport.toLabel(t);
  }

  private static String formatGraduateDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String normalized = raw.trim().replace('.', '-').replace('/', '-');
    if (normalized.length() >= 10 && normalized.charAt(4) == '-' && normalized.charAt(7) == '-') {
      return normalized.substring(0, 10);
    }
    if (normalized.length() >= 7 && normalized.charAt(4) == '-') {
      return normalized.substring(0, 7);
    }
    return raw.trim();
  }

  private static String levelLabel(Integer level) {
    if (level == null || level < 0 || level >= LEVEL_LABELS.length) {
      return "";
    }
    return LEVEL_LABELS[level];
  }

  /**
   * 班级工种等级 1=一级/高级技师 … 5=五级/初级工 → 报名人员 req_level 0=五级 … 4=一级。
   */
  public static Integer classLevelToReqIndex(Integer classLevel) {
    if (classLevel == null || classLevel < 1 || classLevel > 5) {
      return null;
    }
    return 5 - classLevel;
  }

  /**
   * 申请表申报工种/等级：人员未填预约报考时回退到班级设置，避免库默认 req_level=0 写成「五级/初级工」。
   */
  public static Integer resolveReqLevelIndex(
      Integer personReqLevel,
      Long personReqWorkTypeId,
      String personReqWorkTypeName,
      Long classWorkTypeId,
      Integer classLevel) {
    Integer classReq = classLevelToReqIndex(classLevel);
    boolean hasOwnId = personReqWorkTypeId != null && personReqWorkTypeId > 0;
    boolean hasOwnName = StringUtils.hasText(personReqWorkTypeName);
    boolean sameAsClass = hasOwnId && classWorkTypeId != null && personReqWorkTypeId.equals(classWorkTypeId);
    // 人员误把班级编码 1–5 写进 req_level 时，按班级口径换算
    if (personReqLevel != null && classLevel != null
        && personReqLevel.equals(classLevel)
        && classLevel >= 1 && classLevel <= 5) {
      return classReq;
    }
    // 明确填了 1–4（四级…一级）时以人员为准
    if (personReqLevel != null && personReqLevel > 0 && personReqLevel <= 4) {
      return personReqLevel;
    }
    // 人员写了 0，且预约工种与班级不同 → 视为明确报五级
    if (personReqLevel != null && personReqLevel == 0 && (hasOwnId || hasOwnName) && !sameAsClass) {
      return 0;
    }
    // 默认 0 / 空：优先班级预约等级，避免一律「五级」
    if (classReq != null) {
      return classReq;
    }
    return personReqLevel != null && personReqLevel >= 0 && personReqLevel <= 4 ? personReqLevel : null;
  }

  public static String resolveReqWorkTypeName(String personName, String className) {
    if (StringUtils.hasText(personName)) {
      return personName.trim();
    }
    return className != null ? className : "";
  }

  private void loadClassContext(ExportContext ctx, TrainSignUpSelfEntity signUp) {
    if (signUp == null || signUp.getTrainClassId() == null || signUp.getTrainClassId() <= 0) {
      ctx.appointment = ClassAppointment.EMPTY;
      return;
    }
    TrainInfoClassEntity clazz = classMapper.selectById(signUp.getTrainClassId());
    if (clazz == null) {
      ctx.appointment = ClassAppointment.EMPTY;
      return;
    }
    ctx.companyContactName = safe(clazz.getCompanyContactName());
    ctx.companyContactPhone = safe(clazz.getCompanyContactPhone());
    Long workTypeId = clazz.getWorkTypeId();
    String workTypeName = "";
    String workTypeCode = "";
    if (workTypeId != null && workTypeId > 0) {
      ExamWorkTypeEntity type = examWorkTypeMapper.selectById(workTypeId.intValue());
      if (type != null) {
        if (StringUtils.hasText(type.getTitle())) {
          workTypeName = type.getTitle();
        }
        workTypeCode = lookupWorkTypeCode(workTypeId);
      }
    }
    ctx.appointment = new ClassAppointment(workTypeId, workTypeName, clazz.getWorkTypeLevel(), workTypeCode);
  }

  private String lookupWorkTypeTitle(Long workTypeId) {
    if (workTypeId == null || workTypeId <= 0) {
      return "";
    }
    ExamWorkTypeEntity type = examWorkTypeMapper.selectById(workTypeId.intValue());
    return type != null && StringUtils.hasText(type.getTitle()) ? type.getTitle() : "";
  }

  private String lookupWorkTypeCode(Long workTypeId) {
    if (workTypeId == null || workTypeId <= 0) {
      return "";
    }
    ExamWorkTypeEntity type = examWorkTypeMapper.selectById(workTypeId.intValue());
    if (type == null) {
      return "";
    }
    if (type.getParentId() != null && type.getParentId() > 0) {
      ExamWorkTypeEntity parent = examWorkTypeMapper.selectById(type.getParentId());
      if (parent != null && StringUtils.hasText(parent.getSeriesNo())) {
        return parent.getSeriesNo();
      }
    }
    return StringUtils.hasText(type.getSeriesNo()) ? type.getSeriesNo() : "";
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

  /**
   * 职业技能等级认定申请表个人工作简历。
   * 例：2012-06-05至今，在广西强强碳素股份有限公司从事炭极生产工工作。
   */
  static String levelApplyWorkResume(String startDate, String company, String workType) {
    String start = normalizeResumeDate(startDate);
    String period = StringUtils.hasText(start) ? start + "至今" : "至今";
    return period + "，在" + safe(company) + "从事" + safe(workType) + "工作";
  }

  static String normalizeResumeDate(String date) {
    if (!StringUtils.hasText(date)) {
      return "";
    }
    String normalized = date.trim()
        .replace('.', '-')
        .replace('/', '-')
        .replace("年", "-")
        .replace("月", "-")
        .replace("日", "");
    while (normalized.endsWith("-")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    String[] parts = normalized.split("-");
    try {
      if (parts.length >= 3 && parts[0].matches("\\d{4}")) {
        return String.format(
            "%s-%02d-%02d",
            parts[0],
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]));
      }
      if (parts.length == 2 && parts[0].matches("\\d{4}")) {
        return String.format("%s-%02d", parts[0], Integer.parseInt(parts[1]));
      }
    } catch (NumberFormatException ignored) {
      return date.trim();
    }
    return date.trim();
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

  /**
   * 导出「工作单位」：用企业名，不用资料收集计划标题。
   * 人员 company_name 常被误写成计划标题（如「…材料收集」），优先报名计划所属企业 / 工作经历单位。
   */
  static String resolveCompanyName(
      String personCompanyName,
      String orgCompanyName,
      String workExpCompany,
      String signUpTitle) {
    if (StringUtils.hasText(orgCompanyName)) {
      return orgCompanyName.trim();
    }
    if (StringUtils.hasText(workExpCompany) && !looksLikeCollectionPlanTitle(workExpCompany, signUpTitle)) {
      return workExpCompany.trim();
    }
    if (StringUtils.hasText(personCompanyName) && !looksLikeCollectionPlanTitle(personCompanyName, signUpTitle)) {
      return personCompanyName.trim();
    }
    return "";
  }

  /**
   * 企业邮编：人员填写 → PHP 企业库 → 同企业历史众数 → 地址地名推断。
   */
  String resolveCompanyZipcode(
      ExportContext ctx,
      String companyName,
      String personZip,
      String legacyOrgZip,
      String... addressHints) {
    String fromPerson = RegionZipCodeSupport.normalizeZip(personZip);
    if (StringUtils.hasText(fromPerson)) {
      return fromPerson;
    }
    String fromLegacy = RegionZipCodeSupport.normalizeZip(legacyOrgZip);
    if (StringUtils.hasText(fromLegacy)) {
      return fromLegacy;
    }
    String fromCtx = RegionZipCodeSupport.normalizeZip(ctx.companyZipcode);
    if (StringUtils.hasText(fromCtx)) {
      return fromCtx;
    }
    String fromPeers = lookupPeerZipByCompany(ctx, companyName);
    if (StringUtils.hasText(fromPeers)) {
      return fromPeers;
    }
    return RegionZipCodeSupport.inferFromAddresses(addressHints);
  }

  private PlatformOrganizationBrief lookupLegacyOrganization(ExportContext ctx, String companyName) {
    if (!StringUtils.hasText(companyName)) {
      return null;
    }
    if (ctx.legacyOrgByName.containsKey(companyName)) {
      return ctx.legacyOrgByName.get(companyName);
    }
    PlatformOrganizationBrief brief = null;
    try {
      brief = platformOrganizationLookupMapper.findByTitle(companyName);
    } catch (Exception e) {
      log.debug("查询 platform_organization 失败 name={}", companyName, e);
    }
    ctx.legacyOrgByName.put(companyName, brief);
    return brief;
  }

  private String lookupPeerZipByCompany(ExportContext ctx, String companyName) {
    if (!StringUtils.hasText(companyName)) {
      return "";
    }
    return ctx.peerZipByCompany.computeIfAbsent(companyName, name -> {
      try {
        return RegionZipCodeSupport.normalizeZip(peopleSelfMapper.findMostCommonZipByCompanyName(name));
      } catch (Exception e) {
        log.debug("查询同企业历史邮编失败 name={}", name, e);
        return "";
      }
    });
  }

  /** 资料收集计划标题误入企业字段时的识别 */
  static boolean looksLikeCollectionPlanTitle(String name, String signUpTitle) {
    if (!StringUtils.hasText(name)) {
      return false;
    }
    String n = name.trim();
    if (StringUtils.hasText(signUpTitle) && n.equals(signUpTitle.trim())) {
      return true;
    }
    return n.contains("材料收集")
        || n.contains("资料收集")
        || n.contains("信息收集")
        || n.endsWith("收集");
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

  private static int cmToEmu(double cm) {
    return (int) Math.round(cm * Units.EMU_PER_CENTIMETER);
  }

  private static void insertPicture(
      XWPFRun run,
      InputStream in,
      int pictureType,
      String filename,
      int widthEmu,
      int heightEmu,
      boolean lockAspect)
      throws Exception {
    XWPFPicture picture = run.addPicture(in, pictureType, filename, widthEmu, heightEmu);
    var cNvPicPr = picture.getCTPicture().getNvPicPr().getCNvPicPr();
    var picLocks = cNvPicPr.isSetPicLocks() ? cNvPicPr.getPicLocks() : cNvPicPr.addNewPicLocks();
    picLocks.setNoChangeAspect(lockAspect);
    if (run.getCTR().sizeOfDrawingArray() > 0) {
      var drawing = run.getCTR().getDrawingArray(run.getCTR().sizeOfDrawingArray() - 1);
      if (drawing.sizeOfInlineArray() > 0) {
        var inline = drawing.getInlineArray(drawing.sizeOfInlineArray() - 1);
        var framePr = inline.isSetCNvGraphicFramePr()
            ? inline.getCNvGraphicFramePr()
            : inline.addNewCNvGraphicFramePr();
        var frameLocks = framePr.isSetGraphicFrameLocks()
            ? framePr.getGraphicFrameLocks()
            : framePr.addNewGraphicFrameLocks();
        frameLocks.setNoChangeAspect(lockAspect);
      }
    }
  }

  private record ImageSlot(Path path, int widthEmu, int heightEmu, boolean lockAspect) {
    static ImageSlot px(Path path, int widthPx, int heightPx) {
      return new ImageSlot(path, Units.pixelToEMU(widthPx), Units.pixelToEMU(heightPx), true);
    }

    static ImageSlot idCardMerge(Path path) {
      return new ImageSlot(
          path,
          cmToEmu(ID_CARD_MERGE_WIDTH_CM),
          cmToEmu(ID_CARD_MERGE_HEIGHT_CM),
          false);
    }
  }

  private record ClassAppointment(Long workTypeId, String workTypeName, Integer classLevel, String workTypeCode) {
    static final ClassAppointment EMPTY = new ClassAppointment(null, "", null, "");
  }

  private static final class PersonAssets {
    private Path header;
    private Path front;
    private Path back;
  }

  private static final class ExportContext {
    private final String folderName;
    private final String signUpTitle;
    private final Long taskId;
    private final String companyName;
    private final String companyPhone;
    private final String companyAddress;
    private final String companyZipcode;
    private final String companyCode;
    private String companyContactName = "";
    private String companyContactPhone = "";
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
    private ClassAppointment appointment = ClassAppointment.EMPTY;
    private final Map<String, PlatformOrganizationBrief> legacyOrgByName = new HashMap<>();
    private final Map<String, String> peerZipByCompany = new HashMap<>();

    private ExportContext(String cdnRoot, TrainSignUpSelfEntity signUp, OrgEntity company) {
      this(cdnRoot, signUp, company, null);
    }

    private ExportContext(String cdnRoot, TrainSignUpSelfEntity signUp, OrgEntity company, Long taskId) {
      this.signUpTitle = signUp != null && StringUtils.hasText(signUp.getTitle()) ? signUp.getTitle().trim() : "";
      this.taskId = taskId;
      String base = sanitize(signUpTitle);
      // 每次导出独立目录/zip，避免同标题覆盖后仍被缓存或 check 命中旧包
      this.folderName = taskId != null && taskId > 0 ? base + "_t" + taskId : base;
      // 对齐 PHP：工作单位取报名计划所属企业（sys_org.name），不是人员表误写的计划标题
      this.companyName = company != null && StringUtils.hasText(company.getName()) ? company.getName().trim() : "";
      this.companyPhone = company != null && StringUtils.hasText(company.getPhone()) ? company.getPhone() : "";
      this.companyAddress = "";
      this.companyZipcode = "";
      // 营业执照号走人员 company_code；sys_org.code 是内部编码，不能当 com_code
      this.companyCode = "";
      this.rootDir = Paths.get(cdnRoot, "down", folderName);
    }
  }

  private static String sanitize(String title) {
    if (!StringUtils.hasText(title)) {
      return "sign-up-export";
    }
    return title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
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
      if (!StringUtils.hasText(startDate) && !StringUtils.hasText(company) && !StringUtils.hasText(position)) {
        return "";
      }
      String start = safe(startDate);
      String end = safe(endDate);
      boolean ongoing = !StringUtils.hasText(end) || "至今".equals(end) || "今".equals(end);
      String period = ongoing
          ? (StringUtils.hasText(start) ? start + "至今" : "至今")
          : (StringUtils.hasText(start) ? start + "至" + end : end);
      return period + "，在" + safe(company) + "从事" + safe(position) + "工作";
    }

    private static String safe(String value) {
      return value != null ? value : "";
    }
  }
}

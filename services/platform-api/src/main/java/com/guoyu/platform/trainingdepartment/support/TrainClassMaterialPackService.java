package com.guoyu.platform.trainingdepartment.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按模板生成开班申报材料（Word/Excel + train_base_data），对齐 PHP classfilecreate
 */
@Component
@Slf4j
public class TrainClassMaterialPackService {

  private static final String[] LEVEL_LABELS = {
      "", "初级工", "中级工", "高级工", "技师", "高级技师"
  };

  private final TrainFileTempMapper fileTempMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final TrainTeacherBankMapper teacherBankMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper workTypeMapper;
  private final TrainingStoredFileResolver fileResolver;
  private final TrainBaseDataLookup baseDataLookup;
  private final TrainingRemoteImageUtil remoteImageUtil;
  private final TrainingDocxTemplateValidator docxTemplateValidator;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DataFormatter formatter = new DataFormatter();

  public TrainClassMaterialPackService(
      TrainFileTempMapper fileTempMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      TrainTeacherBankMapper teacherBankMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper workTypeMapper,
      TrainingStoredFileResolver fileResolver,
      TrainBaseDataLookup baseDataLookup,
      TrainingRemoteImageUtil remoteImageUtil,
      TrainingDocxTemplateValidator docxTemplateValidator) {
    this.fileTempMapper = fileTempMapper;
    this.signUpPeopleMapper = signUpPeopleMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.teacherBankMapper = teacherBankMapper;
    this.orgMapper = orgMapper;
    this.workTypeMapper = workTypeMapper;
    this.fileResolver = fileResolver;
    this.baseDataLookup = baseDataLookup;
    this.remoteImageUtil = remoteImageUtil;
    this.docxTemplateValidator = docxTemplateValidator;
  }

  public byte[] buildZip(
      TrainInfoClassEntity clazz,
      TrainInfoListEntity trainInfo,
      List<TrainPeopleListEntity> students,
      List<TrainPeopleListEntity> teachers) throws Exception {
    List<Long> templateIds = parseTemplateIds(clazz.getFileTemps());
    if (templateIds.isEmpty()) {
      throw new IllegalStateException("该班级未设置任何材料模板");
    }
    List<TrainFileTempEntity> templates = fileTempMapper.selectList(
        new LambdaQueryWrapper<TrainFileTempEntity>()
            .in(TrainFileTempEntity::getId, templateIds)
            .eq(TrainFileTempEntity::getDeleted, 0));
    if (templates.isEmpty()) {
      throw new IllegalStateException("模板不存在");
    }

    String companyName = "";
    if (clazz.getCompanyId() != null) {
      OrgEntity org = orgMapper.selectById(clazz.getCompanyId());
      if (org != null) {
        companyName = org.getName();
      }
    }
    String workTypeTitle = "";
    Long workTypeId = clazz.getWorkTypeId();
    if (workTypeId == null && trainInfo != null) {
      workTypeId = trainInfo.getWorkTypeId();
    }
    if (workTypeId != null) {
      ExamWorkTypeEntity wt = workTypeMapper.selectById(workTypeId);
      if (wt != null) {
        workTypeTitle = wt.getTitle();
      }
    }
    Map<String, Map<String, String>> baseData = baseDataLookup.loadGroupedByTag();
    Map<String, StudentDetail> userDetails = loadStudentDetails(students);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      for (TrainFileTempEntity tmpl : templates) {
        String title = tmpl.getTitle();
        if ("docx".equalsIgnoreCase(tmpl.getExt()) && "开班申请表".equals(title)) {
          byte[] doc = buildKaiBanDocx(tmpl, clazz, trainInfo, companyName, workTypeTitle, teachers, students);
          if (doc != null) {
            addZipEntry(zos, "开班申请表/开班申请表.docx", doc);
          }
        } else if (!"docx".equalsIgnoreCase(tmpl.getExt())) {
          if ("导入表".equals(title)) {
            byte[] xlsx = buildImportSheet(tmpl, students, userDetails, companyName, baseData);
            addZipEntry(zos, "导入表.xlsx", xlsx);
          } else if ("开班花名册".equals(title)) {
            byte[] xlsx = buildRosterSheet(tmpl, students, userDetails);
            addZipEntry(zos, "开班花名册.xlsx", xlsx);
          } else if ("课时导入表".equals(title) && StringUtils.hasText(clazz.getKcbiaoFile())) {
            byte[] xlsx = buildCourseHourSheet(tmpl, clazz, teachers);
            if (xlsx != null) {
              addZipEntry(zos, "课时导入表.xlsx", xlsx);
            }
          }
        }
      }
      packTeacherMaterials(zos, teachers);
    }
    return baos.toByteArray();
  }

  /** 教师证照材料目录，对齐 PHP 教师材料/{姓名}/ */
  private void packTeacherMaterials(ZipOutputStream zos, List<TrainPeopleListEntity> teachers) throws Exception {
    if (teachers.isEmpty()) {
      return;
    }
    List<Long> bankIds = teachers.stream()
        .map(TrainPeopleListEntity::getUserId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, TrainTeacherBankEntity> bankMap = new HashMap<>();
    if (!bankIds.isEmpty()) {
      teacherBankMapper.selectList(
          new LambdaQueryWrapper<TrainTeacherBankEntity>()
              .in(TrainTeacherBankEntity::getId, bankIds)
              .eq(TrainTeacherBankEntity::getDeleted, 0))
          .forEach(t -> bankMap.put(t.getId(), t));
    }
    for (TrainPeopleListEntity teacher : teachers) {
      String folderName = sanitizeZipName(teacher.getRealname());
      if (!StringUtils.hasText(folderName)) {
        folderName = "未命名";
      }
      String base = "教师材料/" + folderName + "/";
      TrainTeacherBankEntity bank = teacher.getUserId() != null ? bankMap.get(teacher.getUserId()) : null;
      if (bank == null) {
        continue;
      }
      addJsonImages(zos, base, bank.getIdCard(), "身份证");
      addJsonImages(zos, base, bank.getTeacherLicense(), "教师资格证");
      addJsonImages(zos, base, bank.getEngineerLicense(), "工程师证");
      addJsonImages(zos, base, bank.getLevelLicense(), "技能等级证");
      addJsonImages(zos, base, bank.getEduLicense(), "学历证");
      addJsonImages(zos, base, bank.getGraduatePaper(), "毕业证");
    }
  }

  private void addJsonImages(ZipOutputStream zos, String folder, String jsonField, String namePrefix) throws Exception {
    List<String> urls = parseImageUrls(jsonField);
    int index = 0;
    for (String url : urls) {
      if (!StringUtils.hasText(url)) {
        continue;
      }
      Optional<TrainingRemoteImageUtil.ImageBytes> img = remoteImageUtil.fetchImage(url);
      if (img.isPresent()) {
        String entryName = folder + namePrefix + index + img.get().extension();
        addZipEntry(zos, entryName, img.get().data());
        index++;
      }
    }
  }

  private List<String> parseImageUrls(String raw) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    String trimmed = raw.trim();
    try {
      if (trimmed.startsWith("[")) {
        return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
      }
    } catch (Exception ignored) {
      // single url string
    }
    if (trimmed.startsWith("http") || trimmed.startsWith("/")) {
      return List.of(trimmed);
    }
    return List.of();
  }

  private String sanitizeZipName(String name) {
    if (name == null) {
      return "";
    }
    return name.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private byte[] buildKaiBanDocx(
      TrainFileTempEntity tmpl,
      TrainInfoClassEntity clazz,
      TrainInfoListEntity trainInfo,
      String companyName,
      String workTypeTitle,
      List<TrainPeopleListEntity> teachers,
      List<TrainPeopleListEntity> students) throws Exception {
    String templatePath = resolveKaiBanTemplatePath(tmpl);
    Path local = fileResolver.resolveLocalPath(templatePath);
    byte[] templateBytes = Files.readAllBytes(local);
    if (!docxTemplateValidator.hasKaiBanMacros(templateBytes)) {
      throw new IllegalStateException(
          "开班申请表模板未设置 Word 变量（需含 ${pxdw}、${company}、${worktype} 等）。"
              + "请在原 Word 文件对应位置插入占位符后重新上传，或挂载已含变量的模板。");
    }
    String wlevel = "";
    Integer levelCode = clazz.getWorkTypeLevel();
    if (levelCode == null || levelCode <= 0) {
      levelCode = trainInfo != null ? trainInfo.getWorkTypeLevel() : null;
    }
    int level = levelCode != null ? levelCode : 0;
    if (level > 0 && level < LEVEL_LABELS.length) {
      wlevel = LEVEL_LABELS[level];
    }
    String pxtime = "";
    if (StringUtils.hasText(clazz.getDateStart()) && StringUtils.hasText(clazz.getDateEnd())) {
      pxtime = clazz.getDateStart() + "至" + clazz.getDateEnd();
    }
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("pxdw", resolvePxdw(trainInfo));
    vars.put("fzr", "");
    vars.put("fzrtel", "");
    vars.put("company", companyName);
    vars.put("cfzr", "");
    vars.put("cfzrtel", "");
    vars.put("pxaddr", companyName);
    vars.put("pxtime", pxtime);
    vars.put("banqiname", "xx县xx学院xx工种x级第x期");
    vars.put("teacher1", teachers.size() > 0 && teachers.get(0).getRealname() != null ? teachers.get(0).getRealname() : "");
    vars.put("teatel1", teachers.size() > 0 && teachers.get(0).getPhone() != null ? teachers.get(0).getPhone() : "");
    vars.put("teacher2", teachers.size() > 1 && teachers.get(1).getRealname() != null ? teachers.get(1).getRealname() : "");
    vars.put("teatel2", teachers.size() > 1 && teachers.get(1).getPhone() != null ? teachers.get(1).getPhone() : "");
    vars.put("wktype", workTypeTitle);
    vars.put("worktype", workTypeTitle);
    vars.put("level", wlevel);
    vars.put("nums", String.valueOf(students != null ? students.size() : 0));
    vars.put("keshi", "");
    vars.put("pxchengnuo", "培训承诺*****");

    return TrainingDocxVariableReplacer.replaceAll(templateBytes, vars);
  }

  /** ${pxdw}：培训机构名；库为空时兜底默认机构，避免开班申请表机构栏空白 */
  private static String resolvePxdw(TrainInfoListEntity trainInfo) {
    if (trainInfo != null && StringUtils.hasText(trainInfo.getTrainCompanyName())) {
      return trainInfo.getTrainCompanyName().trim();
    }
    return "千铝工投";
  }

  /** 优先 path_used；为空时用 path_source（单文件上传场景） */
  private String resolveKaiBanTemplatePath(TrainFileTempEntity tmpl) {
    if (StringUtils.hasText(tmpl.getPathUsed())) {
      return tmpl.getPathUsed();
    }
    return tmpl.getPathSource();
  }

  private String resolveTemplatePath(TrainFileTempEntity tmpl) {
    return StringUtils.hasText(tmpl.getPathUsed()) ? tmpl.getPathUsed() : tmpl.getPathSource();
  }

  private byte[] buildImportSheet(
      TrainFileTempEntity tmpl,
      List<TrainPeopleListEntity> students,
      Map<String, StudentDetail> userDetails,
      String companyName,
      Map<String, Map<String, String>> baseData) throws Exception {
    Path local = fileResolver.resolveLocalPath(resolveTemplatePath(tmpl));
    try (InputStream in = Files.newInputStream(local);
        Workbook workbook = WorkbookFactory.create(in)) {
      Sheet sheet = workbook.getSheetAt(0);
      int rowNum = 2;
      for (TrainPeopleListEntity st : students) {
        StudentDetail ud = userDetails.getOrDefault(st.getIdCard(), StudentDetail.empty());
        String rowA = "060901-其他职工（其他职工）";
        String rowE = "10-非农业户口";
        String rowF = "01-汉族";
        String rowG = "90-其他";
        String rowL = "0-否";
        if (StringUtils.hasText(ud.address)
            && (ud.address.contains("镇") || ud.address.contains("村") || ud.address.contains("屯"))) {
          rowA = "060101-在岗农民工（在岗农民工）";
          rowE = "20-农业户口";
        }
        if (StringUtils.hasText(ud.nation)) {
          String nation = ud.nation.contains("族") ? ud.nation : ud.nation + "族";
          if (baseData.getOrDefault("民族", Map.of()).containsKey(nation)) {
            rowF = baseData.get("民族").get(nation);
          }
        }
        if (StringUtils.hasText(ud.education) && baseData.getOrDefault("文化程度", Map.of()).containsKey(ud.education)) {
          rowG = baseData.get("文化程度").get(ud.education);
        }
        if (StringUtils.hasText(ud.political) && ud.political.contains("党员")) {
          rowL = "1-是";
        }
        setCell(sheet, rowNum, 0, rowA);
        setCell(sheet, rowNum, 1, "90-无");
        setCell(sheet, rowNum, 2, st.getIdCard());
        setCell(sheet, rowNum, 3, firstNonBlank(ud.realname, st.getRealname()));
        setCell(sheet, rowNum, 4, rowE);
        setCell(sheet, rowNum, 5, rowF);
        setCell(sheet, rowNum, 6, rowG);
        setCell(sheet, rowNum, 7, ud.phone);
        setCell(sheet, rowNum, 8, ud.address);
        setCell(sheet, rowNum, 11, rowL);
        setCell(sheet, rowNum, 15, "3-从事培训对口职业工作");
        setCell(sheet, rowNum, 16, companyName);
        rowNum++;
      }
      return workbookToBytes(workbook);
    }
  }

  private byte[] buildRosterSheet(
      TrainFileTempEntity tmpl,
      List<TrainPeopleListEntity> students,
      Map<String, StudentDetail> userDetails) throws Exception {
    Path local = fileResolver.resolveLocalPath(resolveTemplatePath(tmpl));
    try (InputStream in = Files.newInputStream(local);
        Workbook workbook = WorkbookFactory.create(in)) {
      Sheet sheet = workbook.getSheetAt(0);
      RosterLayout layout = findRosterLayout(sheet);
      int dataStartRow = layout.dataStartRow();
      Map<String, Integer> colMap = layout.columns();
      int female = 0;
      int rowNum = dataStartRow;
      for (int k = 0; k < students.size(); k++) {
        TrainPeopleListEntity st = students.get(k);
        StudentDetail ud = userDetails.getOrDefault(st.getIdCard(), StudentDetail.empty());
        String gender = "男";
        if (ud.genderFemale()) {
          gender = "女";
          female++;
        }
        String leibie = "其他职工（其他职工）";
        if (StringUtils.hasText(ud.address)
            && (ud.address.contains("镇") || ud.address.contains("村") || ud.address.contains("屯"))) {
          leibie = "在岗农民工（在岗农民工）";
        }
        setCol(sheet, rowNum, colMap.get("序号"), String.valueOf(k + 1));
        setCol(sheet, rowNum, colMap.get("姓名"), firstNonBlank(ud.realname, st.getRealname()));
        setCol(sheet, rowNum, colMap.get("性别"), gender);
        setCol(sheet, rowNum, colMap.get("民族"), ud.nation);
        setCol(sheet, rowNum, colMap.get("学历"), ud.education);
        setCol(sheet, rowNum, colMap.get("政治面貌"), ud.political);
        setCol(sheet, rowNum, colMap.get("身份证号码"), StringUtils.hasText(ud.idcard) ? ud.idcard : st.getIdCard());
        setCol(sheet, rowNum, colMap.get("家庭住址"), ud.address);
        setCol(sheet, rowNum, colMap.get("联系电话"), ud.phone);
        setCol(sheet, rowNum, colMap.get("社保号"), "");
        setCol(sheet, rowNum, colMap.get("签订劳动合同时间(起、止）"), "");
        setCol(sheet, rowNum, colMap.get("培训人员类别"), leibie);
        setCol(sheet, rowNum, colMap.get("备注"), "");
        rowNum++;
      }
      double femaleRate = students.isEmpty() ? 0 : Math.round((female * 10000.0 / students.size())) / 100.0;
      Integer femaleCol = colMap.get("联系电话");
      setCell(sheet, rowNum, femaleCol != null ? femaleCol : 7, "女性占" + female + "，占总人数{" + femaleRate + "}%");
      return workbookToBytes(workbook);
    }
  }

  /** 对齐 PHP：扫描「姓名」列确定表头行，数据从表头下一行开始 */
  private RosterLayout findRosterLayout(Sheet sheet) {
    for (int headerRowIdx = 0; headerRowIdx <= sheet.getLastRowNum(); headerRowIdx++) {
      Map<String, Integer> columns = findRosterColumns(sheet, headerRowIdx);
      if (columns.containsKey("姓名")) {
        return new RosterLayout(headerRowIdx + 1, columns);
      }
    }
    return new RosterLayout(5, findRosterColumns(sheet, 4));
  }

  private record RosterLayout(int dataStartRow, Map<String, Integer> columns) {}

  private Map<String, Integer> findRosterColumns(Sheet sheet, int headerRowIdx) {
    Row header = sheet.getRow(headerRowIdx);
    Map<String, Integer> map = new HashMap<>();
    if (header == null) {
      return map;
    }
    for (int c = 0; c < 30; c++) {
      String v = cellText(header, c);
      if ("序号".equals(v)) map.put("序号", c);
      else if ("姓名".equals(v)) map.put("姓名", c);
      else if ("性别".equals(v)) map.put("性别", c);
      else if ("民族".equals(v)) map.put("民族", c);
      else if ("学历".equals(v)) map.put("学历", c);
      else if ("政治面貌".equals(v)) map.put("政治面貌", c);
      else if ("身份证号码".equals(v)) map.put("身份证号码", c);
      else if ("家庭住址".equals(v)) map.put("家庭住址", c);
      else if ("联系电话".equals(v)) map.put("联系电话", c);
      else if ("社保号".equals(v)) map.put("社保号", c);
      else if (v != null && v.contains("签订劳动合同")) map.put("签订劳动合同时间(起、止）", c);
      else if ("培训人员类别".equals(v)) map.put("培训人员类别", c);
      else if ("备注".equals(v)) map.put("备注", c);
    }
    return map;
  }

  private byte[] buildCourseHourSheet(
      TrainFileTempEntity tmpl,
      TrainInfoClassEntity clazz,
      List<TrainPeopleListEntity> teachers) throws Exception {
    Path templateLocal = fileResolver.resolveLocalPath(resolveTemplatePath(tmpl));
    Path kcbiaoLocal = fileResolver.resolveLocalPath(clazz.getKcbiaoFile());
    Map<String, TrainPeopleListEntity> teacherByName = new HashMap<>();
    for (TrainPeopleListEntity t : teachers) {
      if (StringUtils.hasText(t.getRealname())) {
        teacherByName.put(t.getRealname(), t);
      }
    }
    try (InputStream templateIn = Files.newInputStream(templateLocal);
        InputStream kcbiaoIn = Files.newInputStream(kcbiaoLocal);
        Workbook outWb = WorkbookFactory.create(templateIn);
        Workbook kcbiaoWb = WorkbookFactory.create(kcbiaoIn)) {
      Sheet outSheet = outWb.getSheetAt(0);
      Sheet kcSheet = kcbiaoWb.getSheetAt(0);
      int outRow = 2;
      boolean afterHeader = false;
      for (int r = 0; r <= kcSheet.getLastRowNum(); r++) {
        Row row = kcSheet.getRow(r);
        if (row == null) continue;
        String c0 = cellText(row, 0);
        if (c0.contains("合计")) break;
        if ("日期".equals(cellText(row, 1))) {
          afterHeader = true;
          continue;
        }
        if (!afterHeader) continue;
        String date = cellText(row, 1);
        String timeRange = cellText(row, 2);
        String courseName = cellText(row, 3);
        String hours = cellText(row, 4);
        String skType = cellText(row, 5);
        String teacherName = cellText(row, 6);
        String[] times = timeRange.split("-");
        String skTime = times.length > 0 ? times[0].trim() : "";
        String xkTime = times.length > 1 ? times[1].trim() : "";
        TrainPeopleListEntity teacher = teacherByName.get(teacherName);
        String teacherIdcard = teacher != null ? teacher.getIdCard() : "";
        setCell(outSheet, outRow, 0, teacherIdcard);
        setCell(outSheet, outRow, 1, teacherName);
        setCell(outSheet, outRow, 2, skType);
        setCell(outSheet, outRow, 3, date);
        setCell(outSheet, outRow, 4, courseName);
        setCell(outSheet, outRow, 5, skTime);
        setCell(outSheet, outRow, 6, xkTime);
        if ("理论".equals(skType)) {
          setCell(outSheet, outRow, 7, hours);
          setCell(outSheet, outRow, 8, "0");
        } else {
          setCell(outSheet, outRow, 7, "0");
          setCell(outSheet, outRow, 8, hours);
        }
        outRow++;
      }
      return workbookToBytes(outWb);
    }
  }

  private Map<String, StudentDetail> loadStudentDetails(List<TrainPeopleListEntity> students) {
    List<String> idCards = students.stream()
        .map(TrainPeopleListEntity::getIdCard)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    Map<String, StudentDetail> map = new HashMap<>();
    if (idCards.isEmpty()) {
      return map;
    }
    List<PlatformUserDetailEntity> platformDetails = platformUserDetailMapper.selectList(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .in(PlatformUserDetailEntity::getIdcard, idCards)
            .eq(PlatformUserDetailEntity::getDeleted, 0));
    for (PlatformUserDetailEntity p : platformDetails) {
      if (!StringUtils.hasText(p.getIdcard())) {
        continue;
      }
      map.put(p.getIdcard(), StudentDetail.fromPlatform(p));
    }
    List<TrainSignUpPeopleSelfEntity> signUps = signUpPeopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    for (TrainSignUpPeopleSelfEntity p : signUps) {
      String idcard = p.getIdCardNumber();
      if (!StringUtils.hasText(idcard)) {
        continue;
      }
      map.computeIfAbsent(idcard, k -> StudentDetail.empty()).mergeSignUp(p);
    }
    for (TrainPeopleListEntity st : students) {
      if (!StringUtils.hasText(st.getIdCard())) {
        continue;
      }
      map.computeIfAbsent(st.getIdCard(), k -> StudentDetail.empty()).mergePeople(st);
    }
    return map;
  }

  private List<Long> parseTemplateIds(String fileTemps) {
    if (!StringUtils.hasText(fileTemps)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(fileTemps, new TypeReference<List<Long>>() {});
    } catch (Exception e) {
      List<Long> ids = new ArrayList<>();
      for (String part : fileTemps.replace("[", "").replace("]", "").split(",")) {
        String t = part.trim();
        if (!t.isEmpty()) {
          try {
            ids.add(Long.parseLong(t));
          } catch (NumberFormatException ignored) {
            // skip
          }
        }
      }
      return ids;
    }
  }

  private byte[] workbookToBytes(Workbook workbook) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    if (workbook instanceof XSSFWorkbook xssf) {
      xssf.close();
    } else {
      workbook.close();
    }
    return out.toByteArray();
  }

  private void addZipEntry(ZipOutputStream zos, String name, byte[] data) throws Exception {
    zos.putNextEntry(new ZipEntry(name));
    zos.write(data);
    zos.closeEntry();
  }

  private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
      row = sheet.createRow(rowIdx);
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
      cell = row.createCell(colIdx);
    }
    cell.setCellValue(value != null ? value : "");
  }

  private void setCol(Sheet sheet, int rowIdx, Integer colIdx, String value) {
    if (colIdx != null) {
      setCell(sheet, rowIdx, colIdx, value);
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private String cellText(Row row, int col) {
    if (row == null || row.getCell(col) == null) {
      return "";
    }
    return formatter.formatCellValue(row.getCell(col)).trim();
  }

  private static class StudentDetail {
    String realname = "";
    String phone = "";
    String idcard = "";
    Integer genderCode;
    String education = "";
    String nation = "";
    String political = "";
    String address = "";

    static StudentDetail empty() {
      return new StudentDetail();
    }

    static StudentDetail fromPlatform(PlatformUserDetailEntity p) {
      StudentDetail d = new StudentDetail();
      d.realname = p.getRealname();
      d.phone = p.getPhone();
      d.idcard = p.getIdcard();
      d.genderCode = p.getGender();
      d.nation = p.getNation();
      d.education = p.getEducation();
      d.political = p.getPolitical();
      d.address = p.getAddress();
      return d;
    }

    void mergeSignUp(TrainSignUpPeopleSelfEntity p) {
      if (!StringUtils.hasText(realname)) {
        realname = p.getName();
      }
      if (!StringUtils.hasText(phone)) {
        phone = p.getPhone();
      }
      if (!StringUtils.hasText(education)) {
        education = p.getEducation();
      }
      if (genderCode == null && StringUtils.hasText(p.getGender())) {
        if ("女".equals(p.getGender()) || "2".equals(p.getGender())) {
          genderCode = 1;
        } else {
          genderCode = 0;
        }
      }
    }

    void mergePeople(TrainPeopleListEntity st) {
      if (!StringUtils.hasText(realname)) {
        realname = st.getRealname();
      }
      if (!StringUtils.hasText(phone)) {
        phone = st.getPhone();
      }
      if (!StringUtils.hasText(idcard)) {
        idcard = st.getIdCard();
      }
    }

    /** PHP: gender>0 为女 */
    boolean genderFemale() {
      if (genderCode != null) {
        return genderCode > 0;
      }
      return false;
    }
  }
}

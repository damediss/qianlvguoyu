package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 「仅导出名单」对齐 PHP {@code 仅报名人员汇总表导出.xlsx} /
 * 桌面《报名人员汇总表模板》A–Y 共 25 列。
 */
@Component
public class SignUpPeopleSummaryExcelSupport {

  static final List<String> HEADERS = List.of(
      "姓名", "身份证号", "手机号", "年龄", "公司名称", "所属工业园区",
      "现工种", "现工种等级", "学历", "职称", "职称等级",
      "毕业院校", "所学专业", "毕业证编号", "入学时间", "毕业时间", "从业时间",
      "报名时间", "申报条件", "工作经历", "家庭地址",
      "合同", "合同(合并)", "社保证明", "缺少的信息");

  private static final Set<String> LOW_EDU = Set.of("小学", "初中");
  private static final DateTimeFormatter CREATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ObjectMapper objectMapper;

  public SignUpPeopleSummaryExcelSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 下载名：{班级名称}-报名人员汇总表-{signUpId}.xlsx */
  public static String peopleRosterFileName(String classTitle, Long signUpId) {
    String name = SignUpDefaultTemplateExportPaths.sanitizeTitle(
        StringUtils.hasText(classTitle) ? classTitle : "班级");
    return name + "-报名人员汇总表-" + (signUpId != null ? signUpId : "") + ".xlsx";
  }

  public byte[] write(String companyName, List<RosterPerson> people) {
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Sheet0");
      Row header = sheet.createRow(0);
      for (int i = 0; i < HEADERS.size(); i++) {
        header.createCell(i).setCellValue(HEADERS.get(i));
        sheet.setColumnWidth(i, 14 * 256);
      }
      sheet.setColumnWidth(24, 40 * 256);
      int rowIdx = 1;
      for (RosterPerson person : people) {
        fillRow(sheet.createRow(rowIdx++), person, companyName);
      }
      wb.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("生成报名人员汇总表失败", e);
    }
  }

  void fillRow(Row row, RosterPerson person, String companyName) {
    String idCard = safe(person.idCardNumber).replace(" ", "");
    write(row, 0, safe(person.name));
    write(row, 1, idCard);
    write(row, 2, safe(person.phone));
    write(row, 3, ageOf(person, idCard));
    write(row, 4, firstNonBlank(companyName, person.companyName));
    write(row, 5, safe(person.industrialPark));
    write(row, 6, safe(person.workTypeName));
    write(row, 7, currentLevelLabel(person));
    write(row, 8, safe(person.education));
    write(row, 9, safe(person.titleType));
    write(row, 10, safe(person.titleLevel));
    write(row, 11, safe(person.school));
    write(row, 12, safe(person.major));
    write(row, 13, safe(person.schoolNo));
    write(row, 14, safe(person.inSchoolDate));
    write(row, 15, safe(person.graduateDate));
    write(row, 16, safe(person.workTime));
    write(row, 17, person.createTime != null ? person.createTime.format(CREATE_TIME) : "");
    write(row, 18, safe(person.declarationCondition));
    write(row, 19, formatWorkExp(person));
    write(row, 20, safe(person.address));
    write(row, 21, "");
    write(row, 22, "");
    write(row, 23, "");
    write(row, 24, String.join(",", missingTips(person)));
  }

  List<String> missingTips(RosterPerson da) {
    List<String> tips = new ArrayList<>();
    if (!hasText(da.idCardFront)) {
      tips.add("身份证正面");
    }
    if (!hasText(da.idCardBack)) {
      tips.add("身份证反面");
    }
    if (!hasText(da.idCardHeader)) {
      tips.add("证件照");
    }
    if (!hasText(da.school)) {
      tips.add("毕业院校");
    }
    String education = safe(da.education);
    if (!hasText(education)) {
      tips.add("学历");
    }
    if (!LOW_EDU.contains(education)) {
      if (!hasText(da.major) && !"高中".equals(education)) {
        tips.add("专业");
      }
      if (!hasText(da.schoolPaper)) {
        tips.add("毕业证照片");
      }
      if (!hasText(da.schoolNo)) {
        tips.add("毕业证编号");
      }
    }
    if (!hasText(da.inSchoolDate)) {
      tips.add("入学日期");
    }
    if (!hasText(da.graduateDate)) {
      tips.add("毕业日期");
    }
    if (!hasText(da.workTime)) {
      tips.add("从业日期");
    }
    if (!hasText(da.workDateStart)) {
      tips.add("本公司入职日期");
    }
    if (da.workTypePaperHas != null && da.workTypePaperHas > 0 && !hasText(da.workTypePaper)) {
      tips.add("上一等级证书");
    }
    if (!hasText(da.titleType)) {
      tips.add("职称");
    }
    if (!hasText(da.workExp)) {
      tips.add("工作经历");
    }
    if (!hasText(da.contactFirstPage)) {
      tips.add("劳动合同第一页");
    }
    if (!hasText(da.contactExpirePage)) {
      tips.add("劳动合同期限页");
    }
    if (!hasText(da.contactSignPage)) {
      tips.add("劳动合同盖章页");
    }
    return tips;
  }

  private String formatWorkExp(RosterPerson person) {
    try {
      if (StringUtils.hasText(person.workExp)) {
        List<Map<String, Object>> list =
            objectMapper.readValue(person.workExp, new TypeReference<>() {});
        if (!list.isEmpty() && list.get(0) != null) {
          Map<String, Object> item = list.get(0);
          String start = text(item.get("startDate"), person.workDateStart);
          String end = text(item.get("endDate"), person.workDateEnd);
          String company = text(item.get("company"), "");
          String position = text(item.get("position"), firstNonBlank(person.job, person.jog));
          if (hasText(start) || hasText(end) || hasText(company) || hasText(position)) {
            return start + "至" + end + ",在" + company + "从事" + position + "工作";
          }
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    if (hasText(person.workDateStart) || hasText(person.workDateEnd)) {
      return safe(person.workDateStart) + "至" + safe(person.workDateEnd)
          + ",在" + safe(person.companyName) + "从事"
          + firstNonBlank(person.job, person.jog) + "工作";
    }
    return "";
  }

  static String currentLevelLabel(RosterPerson person) {
    if (person == null || !StringUtils.hasText(person.level)) {
      return "";
    }
    String t = person.level.trim();
    boolean numericZero = "0".equals(t);
    boolean hasEvidence = (person.workTypeId != null && person.workTypeId > 0)
        || StringUtils.hasText(person.workTypeName)
        || StringUtils.hasText(person.paperNo)
        || (person.workTypePaperHas != null && person.workTypePaperHas > 0)
        || StringUtils.hasText(person.workTypePaper);
    if (numericZero && !hasEvidence) {
      return "";
    }
    return SignUpLevelSupport.toLabel(t);
  }

  private static String ageOf(RosterPerson person, String idCard) {
    if (person.age != null && person.age > 0) {
      return String.valueOf(person.age);
    }
    if (idCard != null && idCard.length() >= 14) {
      try {
        LocalDate birth = LocalDate.parse(idCard.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
        return String.valueOf(Period.between(birth, LocalDate.now()).getYears());
      } catch (Exception ignored) {
        return "";
      }
    }
    return "";
  }

  private static void write(Row row, int col, String value) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value != null ? value : "");
  }

  private static String text(Object value, String fallback) {
    if (value == null) {
      return fallback != null ? fallback : "";
    }
    String t = String.valueOf(value).trim();
    return StringUtils.hasText(t) ? t : (fallback != null ? fallback : "");
  }

  private static String firstNonBlank(String a, String b) {
    return hasText(a) ? a.trim() : safe(b);
  }

  private static boolean hasText(String value) {
    return StringUtils.hasText(value);
  }

  private static String safe(String value) {
    return value != null ? value.trim() : "";
  }

  /** 报名人员汇总表行数据（不依赖 persistence 实体，避免 IDE 增量编译找不到符号）。 */
  public static final class RosterPerson {
    public String name;
    public String idCardNumber;
    public String phone;
    public Integer age;
    public String companyName;
    public String industrialPark;
    public String workTypeName;
    public String level;
    public Long workTypeId;
    public String paperNo;
    public Integer workTypePaperHas;
    public String workTypePaper;
    public String education;
    public String titleType;
    public String titleLevel;
    public String school;
    public String major;
    public String schoolNo;
    public String inSchoolDate;
    public String graduateDate;
    public String workTime;
    public LocalDateTime createTime;
    public String declarationCondition;
    public String workExp;
    public String address;
    public String workDateStart;
    public String workDateEnd;
    public String job;
    public String jog;
    public String idCardFront;
    public String idCardBack;
    public String idCardHeader;
    public String schoolPaper;
    public String contactFirstPage;
    public String contactExpirePage;
    public String contactSignPage;
  }
}

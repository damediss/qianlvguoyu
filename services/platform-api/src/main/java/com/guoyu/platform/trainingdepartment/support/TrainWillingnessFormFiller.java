package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.util.StringUtils;

/**
 * 《培训意愿需求登记表》填空：不改模板版式，只写入空白单元格 / 勾选 □ / 填意愿句空格。
 */
public final class TrainWillingnessFormFiller {

  private static final String CHECKBOX_OFF = "\u25A1";
  private static final String CHECKBOX_ON = "\u2611";
  private static final String FONT = "宋体";
  private static final int FONT_SIZE_PT = 12;

  /** 表内人员类别勾选项（与官方模板文案一致） */
  private static final String[] CATEGORY_OPTIONS = {
      "困难家庭劳动者",
      "应届毕业生（高校、职业院校）",
      "农村转移就业劳动者",
      "城镇登记失业人员",
      "未继续升学应届初高中毕(肄)业生",
      "企业在岗职工",
      "就业帮扶车间和农民专业合作社用工",
      "灵活就业人员",
      "退役军人",
      "残疾人"
  };

  private static final String[] LEVEL_DIGIT = {
      "", "一", "二", "三", "四", "五"
  };

  public record FillInput(
      String name,
      String gender,
      String nation,
      String age,
      String idCard,
      String phone,
      String address,
      /** 原始人员类别文案，用于模糊匹配勾选 */
      String personCategoryHint,
      boolean employed,
      String jobPost,
      boolean startedBusiness,
      String businessProject,
      String willingnessWorkType,
      /** 班级等级 1=一级 … 5=五级 */
      Integer classLevel) {}

  private TrainWillingnessFormFiller() {}

  public static byte[] fill(byte[] templateBytes, FillInput input) throws IOException {
    if (templateBytes == null || templateBytes.length == 0) {
      throw new IOException("意愿登记表模板为空");
    }
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(templateBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (doc.getTables().isEmpty()) {
        throw new IOException("意愿登记表模板无表格");
      }
      XWPFTable table = doc.getTables().get(0);
      fillBasic(table, input);
      fillCategory(table, input);
      fillEmployment(table, input);
      fillWillingness(table, input);
      doc.write(out);
      return out.toByteArray();
    }
  }

  private static void fillBasic(XWPFTable table, FillInput input) {
    setEmptyCell(table, 0, 1, nullToEmpty(input.name()));
    setEmptyCell(table, 0, 3, nullToEmpty(input.gender()));
    setEmptyCell(table, 0, 5, nullToEmpty(input.nation()));
    setEmptyCell(table, 0, 7, nullToEmpty(input.age()));
    setEmptyCell(table, 1, 1, nullToEmpty(input.idCard()));
    setEmptyCell(table, 1, 3, nullToEmpty(input.phone()));
    setEmptyCell(table, 2, 1, nullToEmpty(input.address()));
  }

  private static void fillCategory(XWPFTable table, FillInput input) {
    XWPFTableCell cell = cellAt(table, 3, 1);
    if (cell == null) {
      return;
    }
    List<String> matched = matchCategories(input);
    if (matched.isEmpty()) {
      // 企业培训默认：其他人员 + 企业在岗职工
      matched = List.of("企业在岗职工");
    }
    boolean fiveClass = matched.stream().anyMatch(TrainWillingnessFormFiller::isFiveClassOption);
    boolean otherClass = matched.stream().anyMatch(c -> !isFiveClassOption(c));
    for (XWPFParagraph p : cell.getParagraphs()) {
      for (XWPFRun run : p.getRuns()) {
        String text = run.getText(0);
        if (text == null || !text.contains(CHECKBOX_OFF)) {
          continue;
        }
        String updated = text;
        if (fiveClass) {
          updated = checkOption(updated, "五类人员");
        }
        if (otherClass) {
          updated = checkOption(updated, "其他人员");
        }
        for (String opt : matched) {
          updated = checkOption(updated, opt);
        }
        // 拆开的「□」+「企业在岗职工」
        if (updated.equals(CHECKBOX_OFF) && nextRunStartsWith(p, run, "企业在岗职工")
            && matched.contains("企业在岗职工")) {
          updated = CHECKBOX_ON;
        }
        if (updated.equals(CHECKBOX_OFF) && nextRunStartsWith(p, run, "未继续升学")
            && matched.stream().anyMatch(s -> s.startsWith("未继续升学"))) {
          updated = CHECKBOX_ON;
        }
        if (!updated.equals(text)) {
          run.setText(updated, 0);
        }
      }
    }
  }

  private static void fillEmployment(XWPFTable table, FillInput input) {
    XWPFTableCell cell = cellAt(table, 5, 1);
    if (cell == null) {
      return;
    }
    boolean employed = input.employed();
    String job = nullToEmpty(input.jobPost());
    for (XWPFParagraph p : cell.getParagraphs()) {
      List<XWPFRun> runs = p.getRuns();
      for (int i = 0; i < runs.size(); i++) {
        XWPFRun run = runs.get(i);
        String text = run.getText(0);
        if (text == null) {
          continue;
        }
        String updated = text;
        // 仅勾选就业状况；第 3 点创业情况、第 4 点创业项目一律留空
        if (employed) {
          updated = checkOption(updated, "已就业");
        } else {
          updated = checkOption(updated, "未就业");
        }
        if (!updated.equals(text)) {
          run.setText(updated, 0);
          continue;
        }
        if (isBlankRun(text)) {
          String prev = i > 0 ? nullToEmpty(runs.get(i - 1).getText(0)) : "";
          if (prev.contains("从事工种或岗位") && employed && StringUtils.hasText(job)) {
            run.setText(fitBlank(job, text.length()), 0);
          }
        }
      }
    }
  }

  private static void fillWillingness(XWPFTable table, FillInput input) {
    XWPFTableCell cell = cellAt(table, 6, 1);
    if (cell == null) {
      return;
    }
    String name = nullToEmpty(input.name());
    String workType = nullToEmpty(input.willingnessWorkType());
    String levelDigit = levelDigit(input.classLevel());
    int blankIdx = 0;
    for (XWPFParagraph p : cell.getParagraphs()) {
      for (XWPFRun run : p.getRuns()) {
        String text = run.getText(0);
        if (text == null || !isBlankRun(text)) {
          continue;
        }
        // 意愿句中前 4 个空格 run：姓名、意愿工种、等级字、申报工种
        String value = switch (blankIdx) {
          case 0 -> name;
          case 1 -> workType;
          case 2 -> levelDigit;
          case 3 -> workType;
          default -> null;
        };
        blankIdx++;
        if (value != null && StringUtils.hasText(value)) {
          run.setText(fitBlank(value, text.length()), 0);
        }
      }
    }
  }

  private static List<String> matchCategories(FillInput input) {
    List<String> matched = new ArrayList<>();
    String hint = nullToEmpty(input.personCategoryHint()).toLowerCase(Locale.ROOT);
    String address = nullToEmpty(input.address());
    if (StringUtils.hasText(hint)) {
      for (String opt : CATEGORY_OPTIONS) {
        if (hintContains(hint, opt)) {
          matched.add(opt);
        }
      }
      // 常见简称
      if (hint.contains("在岗") || hint.contains("企业职工") || hint.contains("其他职工")) {
        addUnique(matched, "企业在岗职工");
      }
      if (hint.contains("农民工") || hint.contains("农村转移")) {
        addUnique(matched, "农村转移就业劳动者");
      }
      if (hint.contains("失业")) {
        addUnique(matched, "城镇登记失业人员");
      }
      if (hint.contains("退役")) {
        addUnique(matched, "退役军人");
      }
      if (hint.contains("残疾")) {
        addUnique(matched, "残疾人");
      }
      if (hint.contains("灵活")) {
        addUnique(matched, "灵活就业人员");
      }
      if (hint.contains("应届") || hint.contains("高校") || hint.contains("院校")) {
        addUnique(matched, "应届毕业生（高校、职业院校）");
      }
      if (hint.contains("困难")) {
        addUnique(matched, "困难家庭劳动者");
      }
    }
    if (matched.isEmpty() && looksRural(address)) {
      // 无明确类别时不强行勾农村转移，留给默认企业在岗
    }
    return matched;
  }

  private static boolean hintContains(String hint, String opt) {
    String compactOpt = opt.replace("（", "(").replace("）", ")").toLowerCase(Locale.ROOT);
    return hint.contains(opt.toLowerCase(Locale.ROOT))
        || hint.contains(compactOpt)
        || (opt.length() >= 4 && hint.contains(opt.substring(0, 4).toLowerCase(Locale.ROOT)));
  }

  private static boolean isFiveClassOption(String opt) {
    return "困难家庭劳动者".equals(opt)
        || "应届毕业生（高校、职业院校）".equals(opt)
        || "农村转移就业劳动者".equals(opt)
        || "城镇登记失业人员".equals(opt)
        || (opt != null && opt.startsWith("未继续升学"));
  }

  private static boolean looksRural(String address) {
    return StringUtils.hasText(address)
        && (address.contains("镇") || address.contains("村") || address.contains("屯"));
  }

  private static void addUnique(List<String> list, String v) {
    if (!list.contains(v)) {
      list.add(v);
    }
  }

  private static String checkOption(String text, String optionLabel) {
    String off = CHECKBOX_OFF + optionLabel;
    if (text.contains(off)) {
      return text.replace(off, CHECKBOX_ON + optionLabel);
    }
    return text;
  }

  private static boolean nextRunStartsWith(XWPFParagraph p, XWPFRun current, String prefix) {
    List<XWPFRun> runs = p.getRuns();
    int idx = runs.indexOf(current);
    if (idx < 0 || idx + 1 >= runs.size()) {
      return false;
    }
    String next = runs.get(idx + 1).getText(0);
    return next != null && next.startsWith(prefix);
  }

  private static void setEmptyCell(XWPFTable table, int row, int col, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    XWPFTableCell cell = cellAt(table, row, col);
    if (cell == null) {
      return;
    }
    // 仅写入原本为空的值单元格，避免覆盖标签
    if (StringUtils.hasText(cell.getText())) {
      return;
    }
    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
    p.setAlignment(ParagraphAlignment.CENTER);
    // 清空可能存在的空 run，再写一个与模板一致的宋体 12 磅
    while (!p.getRuns().isEmpty()) {
      p.removeRun(0);
    }
    XWPFRun run = p.createRun();
    run.setFontFamily(FONT);
    run.setFontFamily(FONT, XWPFRun.FontCharRange.eastAsia);
    run.setFontFamily(FONT, XWPFRun.FontCharRange.cs);
    run.setFontSize(FONT_SIZE_PT);
    run.setText(value);
  }

  private static XWPFTableCell cellAt(XWPFTable table, int row, int col) {
    if (row < 0 || row >= table.getNumberOfRows()) {
      return null;
    }
    XWPFTableRow tr = table.getRow(row);
    if (tr == null || col < 0 || col >= tr.getTableCells().size()) {
      return null;
    }
    return tr.getCell(col);
  }

  private static boolean isBlankRun(String text) {
    return text != null && !text.isEmpty() && text.trim().isEmpty();
  }

  /** 尽量不改变空格占位长度：过长截断，过短左右补空格使文字在划线区域内居中 */
  private static String fitBlank(String value, int blankLen) {
    String v = value == null ? "" : value.trim();
    if (blankLen <= 0) {
      return v;
    }
    if (v.length() >= blankLen) {
      return v.substring(0, blankLen);
    }
    int pad = blankLen - v.length();
    int left = pad / 2;
    int right = pad - left;
    return " ".repeat(left) + v + " ".repeat(right);
  }

  private static String levelDigit(Integer classLevel) {
    if (classLevel == null || classLevel < 1 || classLevel >= LEVEL_DIGIT.length) {
      return "";
    }
    return LEVEL_DIGIT[classLevel];
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  /** 从 18 位身份证推算年龄；失败返回空 */
  public static String ageFromIdCard(String idCard, LocalDate today) {
    if (!StringUtils.hasText(idCard) || idCard.trim().length() < 14) {
      return "";
    }
    try {
      String raw = idCard.trim();
      int year;
      int month;
      int day;
      if (raw.length() == 18) {
        year = Integer.parseInt(raw.substring(6, 10));
        month = Integer.parseInt(raw.substring(10, 12));
        day = Integer.parseInt(raw.substring(12, 14));
      } else if (raw.length() == 15) {
        year = 1900 + Integer.parseInt(raw.substring(6, 8));
        month = Integer.parseInt(raw.substring(8, 10));
        day = Integer.parseInt(raw.substring(10, 12));
      } else {
        return "";
      }
      LocalDate birth = LocalDate.of(year, month, day);
      int age = Period.between(birth, today == null ? LocalDate.now() : today).getYears();
      return age > 0 && age < 120 ? String.valueOf(age) : "";
    } catch (Exception ex) {
      return "";
    }
  }

  public static String genderFromIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    String raw = idCard.trim();
    try {
      int code;
      if (raw.length() == 18) {
        code = Character.getNumericValue(raw.charAt(16));
      } else if (raw.length() == 15) {
        code = Character.getNumericValue(raw.charAt(14));
      } else {
        return "";
      }
      return code % 2 == 0 ? "女" : "男";
    } catch (Exception ex) {
      return "";
    }
  }
}

package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowEvidenceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按已审照片生成「培训照片」Word。
 * 版式对齐申报材料：标题 + 培训工种/级别/时间/单位/地址 + 插图图注。
 */
@Component
public class TrainFollowPhotoPackGenerator {

  private static final DateTimeFormatter CN_DATE =
      DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);

  private static final String FONT_FANGSONG = "仿宋_GB2312";
  /** 中文二号 ≈ 22pt */
  private static final int FONT_SIZE_ER_HAO = 22;
  /** 中文四号 ≈ 14pt */
  private static final int FONT_SIZE_SI_HAO = 14;
  /** 中文小四 ≈ 12pt */
  private static final int FONT_SIZE_XIAO_SI = 12;

  private final TrainingStoredFileResolver fileResolver;
  private final FileApplicationService fileApplicationService;

  public TrainFollowPhotoPackGenerator(
      TrainingStoredFileResolver fileResolver,
      FileApplicationService fileApplicationService) {
    this.fileResolver = fileResolver;
    this.fileApplicationService = fileApplicationService;
  }

  /** 封面元信息（弹窗可编辑） */
  public record HeaderMeta(
      String title,
      String workType,
      String level,
      String trainTime,
      String trainUnit,
      String trainAddress) {}

  public record Section(
      Long scheduleId,
      String bizDate,
      String titleLine,
      String teachFormLabel,
      String hoursLabel,
      String courseName,
      String startTime,
      /** 上午 / 下午 / 全天；手动添加时优先用此字段 */
      String dayPeriod,
      List<TrainFollowEvidenceEntity> photos) {}

  public FileVO generateAndStore(
      TrainInfoClassEntity clazz,
      List<Section> sections,
      Long tenantId,
      String fileTitle) {
    return generateAndStore(clazz, sections, tenantId, fileTitle, null);
  }

  public FileVO generateAndStore(
      TrainInfoClassEntity clazz,
      List<Section> sections,
      Long tenantId,
      String fileTitle,
      HeaderMeta header) {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      String title = fileTitle != null ? fileTitle : defaultTitle(clazz);
      if (header != null && StringUtils.hasText(header.title())) {
        title = header.title().trim();
      }
      addTitle(doc, title);
      addHeaderMeta(doc, header);

      int figureNo = 0;
      for (Section section : sections) {
        if (section.photos() == null || section.photos().isEmpty()) {
          continue;
        }
        for (TrainFollowEvidenceEntity photo : section.photos()) {
          figureNo += 1;
          appendImage(doc, photo);
          addCaption(doc, buildFigureCaption(figureNo, photo, section));
        }
      }

      if (figureNo == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无可用照片");
      }

      doc.write(bos);
      String name = sanitizeFileName(title) + ".docx";
      return fileApplicationService.storeFromBytesForTenant(
          tenantId, bos.toByteArray(), name,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "private");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成培训照片失败: " + ex.getMessage());
    }
  }

  private static void addHeaderMeta(XWPFDocument doc, HeaderMeta header) {
    if (header == null) {
      return;
    }
    addMetaLine(doc, "培训工种：", header.workType());
    addMetaLine(doc, "培训级别：", header.level());
    addMetaLine(doc, "培训时间：", header.trainTime());
    addMetaLine(doc, "培训单位：", header.trainUnit());
    addMetaLine(doc, "培训地址：", header.trainAddress());
    // 与图片之间留空行
    doc.createParagraph();
  }

  private static void addMetaLine(XWPFDocument doc, String label, String value) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.BOTH);
    setExactLineSpacing(p, 22);
    XWPFRun labelRun = p.createRun();
    applyFangSong(labelRun, FONT_SIZE_SI_HAO, false);
    labelRun.setText(label);
    XWPFRun valueRun = p.createRun();
    applyFangSong(valueRun, FONT_SIZE_SI_HAO, false);
    valueRun.setText(value != null ? value : "");
  }

  private static void applyFangSong(XWPFRun run, int fontSizePt, boolean bold) {
    run.setBold(bold);
    run.setFontSize(fontSizePt);
    run.setFontFamily(FONT_FANGSONG);
    run.setFontFamily(FONT_FANGSONG, XWPFRun.FontCharRange.eastAsia);
    run.setFontFamily(FONT_FANGSONG, XWPFRun.FontCharRange.cs);
  }

  /** 固定行距（磅 → twips，1pt = 20 twips） */
  private static void setExactLineSpacing(XWPFParagraph p, int points) {
    CTPPr pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
    CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
    spacing.setLine(BigInteger.valueOf(points * 20L));
    spacing.setLineRule(STLineSpacingRule.EXACT);
    spacing.setAfter(BigInteger.ZERO);
    spacing.setBefore(BigInteger.ZERO);
  }

  /** 单倍行距 */
  private static void setSingleLineSpacing(XWPFParagraph p) {
    CTPPr pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
    CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
    spacing.setLine(BigInteger.valueOf(240));
    spacing.setLineRule(STLineSpacingRule.AUTO);
    spacing.setAfter(BigInteger.ZERO);
    spacing.setBefore(BigInteger.ZERO);
  }

  /**
   * 图注：图1：开班、公共课                                 2024年6月6日上午
   */
  public static String buildFigureCaption(int figureNo, TrainFollowEvidenceEntity photo, Section section) {
    String desc = resolveCaptionDesc(photo, section);
    String datePart = formatChineseDate(
        photo != null && StringUtils.hasText(photo.getBizDate())
            ? photo.getBizDate()
            : (section != null ? section.bizDate() : null));
    String period = resolveDayPeriod(photo, section);
    String right = datePart + period;
    return "图" + figureNo + "：" + desc + "                                 " + right;
  }

  private static String resolveCaptionDesc(TrainFollowEvidenceEntity photo, Section section) {
    if (photo != null && StringUtils.hasText(photo.getCaption())) {
      return photo.getCaption().trim();
    }
    if (section != null && StringUtils.hasText(section.courseName())) {
      return section.courseName().trim();
    }
    if (section != null && StringUtils.hasText(section.teachFormLabel())
        && !isDayPeriod(section.teachFormLabel())
        && !"综合".equals(section.teachFormLabel())) {
      return section.teachFormLabel();
    }
    return "课堂";
  }

  private static String resolveDayPeriod(TrainFollowEvidenceEntity photo, Section section) {
    if (photo != null && isDayPeriod(photo.getTeachFormOverride())) {
      return photo.getTeachFormOverride().trim();
    }
    if (section != null && StringUtils.hasText(section.dayPeriod())) {
      return section.dayPeriod();
    }
    if (section != null) {
      return periodFromStartTime(section.startTime());
    }
    return "";
  }

  public static boolean isDayPeriod(String raw) {
    if (!StringUtils.hasText(raw)) {
      return false;
    }
    String t = raw.trim();
    return "上午".equals(t) || "下午".equals(t) || "全天".equals(t);
  }

  /** 从课表开始时间推断上午/下午；无时间则空。 */
  public static String periodFromStartTime(String startTime) {
    if (!StringUtils.hasText(startTime)) {
      return "";
    }
    String t = startTime.trim();
    int colon = t.indexOf(':');
    if (colon <= 0) {
      return "";
    }
    try {
      int hour = Integer.parseInt(t.substring(0, colon));
      if (hour < 12) {
        return "上午";
      }
      return "下午";
    } catch (NumberFormatException ex) {
      return "";
    }
  }

  public static String formatChineseDate(String yyyyMmDd) {
    if (!StringUtils.hasText(yyyyMmDd)) {
      return "";
    }
    String raw = yyyyMmDd.trim();
    try {
      LocalDate d = LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw);
      return d.format(CN_DATE);
    } catch (DateTimeParseException ex) {
      return raw;
    }
  }

  public static String normalizeTeachForm(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "综合";
    }
    String t = raw.trim();
    if (t.contains("实操") || t.contains("实作") || t.contains("操作")) {
      return "实操";
    }
    if (t.contains("理论")) {
      return "理论";
    }
    return t;
  }

  public static String resolveHours(TrainScheduleEntity schedule, String override) {
    if (StringUtils.hasText(override)) {
      return override.trim();
    }
    if (schedule == null) {
      return "";
    }
    if (StringUtils.hasText(schedule.getActualClassHours())) {
      return schedule.getActualClassHours().trim();
    }
    return schedule.getClassHours() != null ? schedule.getClassHours().trim() : "";
  }

  public static String buildSectionTitle(TrainScheduleEntity schedule, String bizDate,
      String teachForm, String hours) {
    String date = schedule != null && StringUtils.hasText(schedule.getScheduleDate())
        ? schedule.getScheduleDate()
        : (bizDate != null ? bizDate : "");
    String course = schedule != null && StringUtils.hasText(schedule.getCourseName())
        ? schedule.getCourseName()
        : "课堂";
    String form = normalizeTeachForm(teachForm);
    String time = "";
    if (schedule != null && StringUtils.hasText(schedule.getStartTime())) {
      time = schedule.getStartTime();
      if (StringUtils.hasText(schedule.getEndTime())) {
        time = time + "–" + schedule.getEndTime();
      }
      time = " " + time;
    }
    String hoursPart = StringUtils.hasText(hours) ? (" 学时：" + hours) : "";
    return date + " " + course + "（" + form + "）" + hoursPart + time;
  }

  private void appendImage(XWPFDocument doc, TrainFollowEvidenceEntity photo) throws Exception {
    Path path = fileResolver.resolveLocalPath(photo.getFilePath());
    if (!Files.isRegularFile(path)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "影像文件不存在: " + photo.getId());
    }
    String name = path.getFileName().toString().toLowerCase();
    int format;
    if (name.endsWith(".png")) {
      format = Document.PICTURE_TYPE_PNG;
    } else if (name.endsWith(".gif")) {
      format = Document.PICTURE_TYPE_GIF;
    } else {
      format = Document.PICTURE_TYPE_JPEG;
    }
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    try (InputStream in = Files.newInputStream(path)) {
      run.addPicture(in, format, path.getFileName().toString(),
          Units.toEMU(420), Units.toEMU(280));
    }
  }

  private static void addTitle(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    p.setSpacingAfter(200);
    XWPFRun run = p.createRun();
    applyFangSong(run, FONT_SIZE_ER_HAO, true);
    run.setText(text);
  }

  private static void addSubtitle(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setFontSize(10);
    run.setColor("666666");
    run.setText(text);
  }

  private static void addHeading(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    XWPFRun run = p.createRun();
    run.setBold(true);
    run.setFontSize(12);
    run.setText(text);
  }

  private static void addBody(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    XWPFRun run = p.createRun();
    run.setFontSize(11);
    run.setText(text);
  }

  private static void addCaption(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.LEFT);
    setSingleLineSpacing(p);
    XWPFRun run = p.createRun();
    applyFangSong(run, FONT_SIZE_XIAO_SI, false);
    run.setText(text);
  }

  private static String defaultTitle(TrainInfoClassEntity clazz) {
    String t = clazz.getTitle() != null ? clazz.getTitle() : ("班级" + clazz.getId());
    return t + "培训照片";
  }

  private static double parseHours(String raw) {
    if (!StringUtils.hasText(raw)) {
      return 0;
    }
    try {
      return Double.parseDouble(raw.replaceAll("[^\\d.-]", ""));
    } catch (Exception ex) {
      return 0;
    }
  }

  private static String formatHours(double v) {
    if (Math.abs(v - Math.rint(v)) < 0.05) {
      return String.valueOf((long) Math.rint(v));
    }
    return String.format("%.1f", v);
  }

  private static String sanitizeFileName(String title) {
    if (!StringUtils.hasText(title)) {
      return "班级";
    }
    return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  /** 按课次聚合排序 */
  public static List<Section> groupSections(
      List<TrainFollowEvidenceEntity> photos,
      Map<Long, TrainScheduleEntity> scheduleMap) {
    Map<String, List<TrainFollowEvidenceEntity>> buckets = new LinkedHashMap<>();
    List<TrainFollowEvidenceEntity> sorted = new ArrayList<>(photos);
    sorted.sort(Comparator
        .comparing((TrainFollowEvidenceEntity e) -> scheduleSortKey(e, scheduleMap))
        .thenComparing(e -> e.getSortNo() == null ? 0 : e.getSortNo())
        .thenComparing(TrainFollowEvidenceEntity::getId));
    for (TrainFollowEvidenceEntity e : sorted) {
      String key = e.getScheduleId() != null
          ? "s:" + e.getScheduleId()
          : "d:" + (e.getBizDate() != null ? e.getBizDate() : "");
      buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
    }
    List<Section> sections = new ArrayList<>();
    for (Map.Entry<String, List<TrainFollowEvidenceEntity>> entry : buckets.entrySet()) {
      TrainFollowEvidenceEntity first = entry.getValue().get(0);
      TrainScheduleEntity schedule = first.getScheduleId() != null
          ? scheduleMap.get(first.getScheduleId()) : null;
      String teachForm = schedule != null && StringUtils.hasText(schedule.getTeachForm())
          ? schedule.getTeachForm()
          : first.getTeachFormOverride();
      String hours = resolveHours(schedule, first.getClassHoursOverride());
      String courseName = schedule != null && StringUtils.hasText(schedule.getCourseName())
          ? schedule.getCourseName()
          : null;
      String startTime = schedule != null ? schedule.getStartTime() : null;
      String dayPeriod = isDayPeriod(first.getTeachFormOverride())
          ? first.getTeachFormOverride().trim()
          : periodFromStartTime(startTime);
      sections.add(new Section(
          first.getScheduleId(),
          first.getBizDate(),
          buildSectionTitle(schedule, first.getBizDate(), teachForm, hours),
          normalizeTeachForm(isDayPeriod(teachForm) ? null : teachForm),
          hours,
          courseName,
          startTime,
          dayPeriod,
          entry.getValue()));
    }
    return sections;
  }

  private static String scheduleSortKey(TrainFollowEvidenceEntity e, Map<Long, TrainScheduleEntity> map) {
    if (e.getScheduleId() != null) {
      TrainScheduleEntity s = map.get(e.getScheduleId());
      if (s != null) {
        return (s.getScheduleDate() != null ? s.getScheduleDate() : "")
            + "|" + (s.getStartTime() != null ? s.getStartTime() : "")
            + "|" + s.getId();
      }
      return "s|" + e.getScheduleId();
    }
    // 手动相册：仅按业务日分组排序，图序与列表一致（sortNo、id 由外层 Comparator 决定）
    return "d|" + (e.getBizDate() != null ? e.getBizDate() : "");
  }
}

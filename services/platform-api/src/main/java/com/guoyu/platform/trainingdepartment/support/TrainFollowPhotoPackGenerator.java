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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按已审照片生成「培训照片」Word（理论/实操 + 学时标题 + 插图）。
 */
@Component
public class TrainFollowPhotoPackGenerator {

  private final TrainingStoredFileResolver fileResolver;
  private final FileApplicationService fileApplicationService;

  public TrainFollowPhotoPackGenerator(
      TrainingStoredFileResolver fileResolver,
      FileApplicationService fileApplicationService) {
    this.fileResolver = fileResolver;
    this.fileApplicationService = fileApplicationService;
  }

  public record Section(
      Long scheduleId,
      String bizDate,
      String titleLine,
      String teachFormLabel,
      String hoursLabel,
      List<TrainFollowEvidenceEntity> photos) {}

  public FileVO generateAndStore(
      TrainInfoClassEntity clazz,
      List<Section> sections,
      Long tenantId,
      String fileTitle) {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      addTitle(doc, fileTitle != null ? fileTitle : defaultTitle(clazz));
      addSubtitle(doc, "说明：以下为课堂过程影像，按课次标注授课形式与学时。");

      double theoryHours = 0;
      double practiceHours = 0;
      for (Section section : sections) {
        if (section.photos() == null || section.photos().isEmpty()) {
          continue;
        }
        addHeading(doc, section.titleLine());
        for (TrainFollowEvidenceEntity photo : section.photos()) {
          appendImage(doc, photo);
          if (StringUtils.hasText(photo.getCaption())) {
            addCaption(doc, photo.getCaption());
          }
        }
        double h = parseHours(section.hoursLabel());
        String form = section.teachFormLabel() == null ? "" : section.teachFormLabel();
        if (form.contains("实操") || form.contains("实作") || form.contains("操作")) {
          practiceHours += h;
        } else if (form.contains("理论")) {
          theoryHours += h;
        }
      }

      addHeading(doc, "学时合计");
      addBody(doc, String.format("理论学时合计：%s　实操学时合计：%s",
          formatHours(theoryHours), formatHours(practiceHours)));

      doc.write(bos);
      String name = sanitizeFileName(clazz.getTitle()) + "_培训照片.docx";
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
    XWPFRun run = p.createRun();
    run.setBold(true);
    run.setFontSize(18);
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
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setFontSize(9);
    run.setColor("555555");
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
      sections.add(new Section(
          first.getScheduleId(),
          first.getBizDate(),
          buildSectionTitle(schedule, first.getBizDate(), teachForm, hours),
          normalizeTeachForm(teachForm),
          hours,
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
    return "d|" + (e.getBizDate() != null ? e.getBizDate() : "");
  }
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.trainingdepartment.service.RensheScoreRosterParseService.ParsedRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 多场成绩（初考/补考/重考）按考试时间合并：空不覆盖；仅同场分值不一致才冲突。
 */
final class ExamScoreAttemptMergeSupport {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final Pattern TIME_PATTERN = Pattern.compile(
      "(\\d{4}-\\d{1,2}-\\d{1,2}(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)");
  private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-M-d[ H:mm[:ss]]");
  private static final DateTimeFormatter DT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm[:ss]]");

  private ExamScoreAttemptMergeSupport() {}

  record MergeResult(
      List<ParsedRow> merged,
      Map<String, List<ParsedRow>> conflictsByIdCard) {}

  static MergeResult mergeByIdCard(List<ParsedRow> rows) {
    Map<String, List<ParsedRow>> byId = new LinkedHashMap<>();
    List<ParsedRow> noId = new ArrayList<>();
    for (ParsedRow row : rows) {
      if (!StringUtils.hasText(row.idCard())) {
        noId.add(row);
        continue;
      }
      byId.computeIfAbsent(row.idCard(), k -> new ArrayList<>()).add(row);
    }

    List<ParsedRow> merged = new ArrayList<>(noId);
    Map<String, List<ParsedRow>> conflicts = new LinkedHashMap<>();

    for (Map.Entry<String, List<ParsedRow>> e : byId.entrySet()) {
      List<ParsedRow> group = e.getValue();
      if (hasSameSessionConflict(group)) {
        conflicts.put(e.getKey(), group);
        continue;
      }
      List<ParsedRow> sorted = new ArrayList<>(group);
      sorted.sort(Comparator
          .comparingLong(ExamScoreAttemptMergeSupport::examSortKey)
          .thenComparingInt(ParsedRow::fileIndex)
          .thenComparingInt(ParsedRow::rowNo));
      merged.add(foldAttempts(sorted));
    }
    return new MergeResult(merged, conflicts);
  }

  /** 同一考试时间桶内，理论/技能/综合任一不等 → 硬冲突 */
  static boolean hasSameSessionConflict(List<ParsedRow> group) {
    if (group.size() <= 1) {
      return false;
    }
    Map<String, List<ParsedRow>> bySession = new LinkedHashMap<>();
    for (ParsedRow row : group) {
      bySession.computeIfAbsent(sessionBucket(row), k -> new ArrayList<>()).add(row);
    }
    for (List<ParsedRow> sessionRows : bySession.values()) {
      if (sessionRows.size() > 1 && scoresConflict(sessionRows)) {
        return true;
      }
    }
    return false;
  }

  static boolean scoresConflict(List<ParsedRow> group) {
    ParsedRow first = group.get(0);
    for (int i = 1; i < group.size(); i++) {
      ParsedRow other = group.get(i);
      if (!scoreEq(first.theory(), other.theory())
          || !scoreEq(first.skill(), other.skill())
          || !scoreEq(first.comprehensive(), other.comprehensive())) {
        return true;
      }
    }
    return false;
  }

  static ParsedRow foldAttempts(List<ParsedRow> sortedAsc) {
    ParsedRow first = sortedAsc.get(0);
    BigDecimal theory = null;
    BigDecimal skill = null;
    BigDecimal comprehensive = null;
    BigDecimal performance = null;
    String name = null;
    String idCardRaw = null;
    String ticketNo = null;
    String jobName = null;
    String levelName = null;
    boolean timeMissing = false;
    for (ParsedRow r : sortedAsc) {
      timeMissing |= r.examTimeMissing() || !StringUtils.hasText(r.examTimeText());
      if (StringUtils.hasText(r.name())) {
        name = r.name();
      }
      if (StringUtils.hasText(r.idCardRaw())) {
        idCardRaw = r.idCardRaw();
      }
      if (StringUtils.hasText(r.ticketNo())) {
        ticketNo = r.ticketNo();
      }
      if (StringUtils.hasText(r.jobName())) {
        jobName = r.jobName();
      }
      if (StringUtils.hasText(r.levelName())) {
        levelName = r.levelName();
      }
      if (r.theory() != null) {
        theory = r.theory();
      }
      if (r.skill() != null) {
        skill = r.skill();
      }
      if (r.comprehensive() != null) {
        comprehensive = r.comprehensive();
      }
      if (r.performance() != null) {
        performance = r.performance();
      }
    }
    ParsedRow last = sortedAsc.get(sortedAsc.size() - 1);
    String mergeNote = null;
    if (sortedAsc.size() > 1) {
      mergeNote = "合并" + sortedAsc.size() + "场";
      if (hasInheritedSkill(sortedAsc)) {
        mergeNote += "（补考继承实操）";
      }
    }
    return new ParsedRow(
        first.rowNo(),
        name != null ? name : last.name(),
        first.idCard(),
        idCardRaw != null ? idCardRaw : last.idCardRaw(),
        ticketNo,
        jobName,
        levelName,
        theory,
        skill,
        comprehensive,
        performance,
        last.examTimeText(),
        last.planCode(),
        last.planName(),
        last.fileIndex(),
        mergeNote,
        timeMissing);
  }

  /** 后场技能为空且前场有技能 → 典型补考继承 */
  private static boolean hasInheritedSkill(List<ParsedRow> sortedAsc) {
    boolean sawSkill = false;
    for (ParsedRow r : sortedAsc) {
      if (r.skill() != null) {
        sawSkill = true;
      } else if (sawSkill) {
        return true;
      }
    }
    return false;
  }

  static String sessionBucket(ParsedRow row) {
    Long start = parseExamTimeStartMillis(row.examTimeText());
    if (start != null) {
      return "T:" + start;
    }
    // 无时间：同一文件视为同一场；跨文件按文件序号区分，走时间轴合并
    return "F:" + row.fileIndex();
  }

  static long examSortKey(ParsedRow row) {
    Long start = parseExamTimeStartMillis(row.examTimeText());
    if (start != null) {
      return start;
    }
    return 9_000_000_000_000_000L + (long) row.fileIndex() * 1_000_000L + row.rowNo();
  }

  static Long parseExamTimeStartMillis(String text) {
    if (!StringUtils.hasText(text)) {
      return null;
    }
    Matcher m = TIME_PATTERN.matcher(text.trim());
    if (!m.find()) {
      return null;
    }
    String token = m.group(1).trim().replaceAll("\\s+", " ");
    try {
      if (token.length() <= 10) {
        return LocalDate.parse(token, DateTimeFormatter.ofPattern("yyyy-M-d"))
            .atStartOfDay(ZONE).toInstant().toEpochMilli();
      }
      LocalDateTime ldt;
      try {
        ldt = LocalDateTime.parse(token, DT2);
      } catch (DateTimeParseException ex) {
        ldt = LocalDateTime.parse(token, DT);
      }
      return ldt.atZone(ZONE).toInstant().toEpochMilli();
    } catch (Exception ex) {
      return null;
    }
  }

  static boolean scoreEq(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return a.compareTo(b) == 0;
  }
}

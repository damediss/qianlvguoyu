package com.guoyu.platform.trainingdepartment.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 从样表「持有何种职业资格证书」自由文本中尽力拆出证号。
 */
public final class TeacherCertTextParser {

  public record ParsedCerts(String teacherQualNo, String titleCertNo, String skillCertNo) {}

  private static final Pattern TOKEN =
      Pattern.compile(
          "(教师资格证(?:书)?|职称证(?:书)?|职业技能等级证(?:书)?|职业资格证(?:书)?)\\s*[:：]?\\s*([A-Za-z0-9]{6,})?",
          Pattern.CASE_INSENSITIVE);

  private TeacherCertTextParser() {}

  public static ParsedCerts parse(String raw) {
    if (!StringUtils.hasText(raw)) {
      return new ParsedCerts(null, null, null);
    }
    String text = raw.replace('\u3000', ' ').trim();
    if (text.contains("未扫描")) {
      // 仍尝试其它证号
    }
    Map<String, List<String>> buckets = new LinkedHashMap<>();
    buckets.put("teacher", new ArrayList<>());
    buckets.put("title", new ArrayList<>());
    buckets.put("skill", new ArrayList<>());

    String[] lines = text.split("[\\r\\n]+");
    String pendingKind = null;
    for (String line : lines) {
      String s = line.trim();
      if (!StringUtils.hasText(s)) {
        continue;
      }
      Matcher m = TOKEN.matcher(s);
      boolean any = false;
      while (m.find()) {
        any = true;
        String kind = classify(m.group(1));
        String no = m.group(2);
        if (StringUtils.hasText(no) && !isPlaceholder(no)) {
          buckets.get(kind).add(no.trim());
          pendingKind = null;
        } else {
          pendingKind = kind;
        }
      }
      if (!any && pendingKind != null) {
        String no = extractStandaloneNo(s);
        if (no != null) {
          buckets.get(pendingKind).add(no);
          pendingKind = null;
        }
      } else if (!any) {
        // 整行可能是「教师资格证2007...」无空格
        Matcher glued = Pattern.compile(
                "(教师资格证(?:书)?|职称证(?:书)?|职业技能等级证(?:书)?)([A-Za-z0-9]{6,})")
            .matcher(s.replace(" ", ""));
        while (glued.find()) {
          String kind = classify(glued.group(1));
          String no = glued.group(2);
          if (!isPlaceholder(no)) {
            buckets.get(kind).add(no);
          }
        }
      }
    }

    return new ParsedCerts(
        firstOrJoin(buckets.get("teacher")),
        firstOrJoin(buckets.get("title")),
        join(buckets.get("skill")));
  }

  private static String classify(String label) {
    String v = label == null ? "" : label;
    if (v.contains("教师资格")) {
      return "teacher";
    }
    if (v.contains("职称")) {
      return "title";
    }
    return "skill";
  }

  private static boolean isPlaceholder(String no) {
    return no == null || no.contains("未扫描") || no.length() < 6;
  }

  private static String extractStandaloneNo(String s) {
    Matcher m = Pattern.compile("([A-Za-z0-9]{6,})").matcher(s);
    if (m.find()) {
      String no = m.group(1);
      return isPlaceholder(no) ? null : no;
    }
    return null;
  }

  private static String firstOrJoin(List<String> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  private static String join(List<String> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return String.join(",", list);
  }
}

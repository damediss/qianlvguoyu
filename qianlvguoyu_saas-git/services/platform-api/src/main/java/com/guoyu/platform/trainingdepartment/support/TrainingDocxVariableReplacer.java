package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Word 模板变量替换（对齐 PHP PhpWord TemplateProcessor） */
public final class TrainingDocxVariableReplacer {

  private TrainingDocxVariableReplacer() {}

  public static byte[] replaceAll(byte[] docxBytes, Map<String, String> vars) throws IOException {
    List<Map.Entry<String, String>> ordered = new ArrayList<>(vars.entrySet());
    ordered.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes));
        ZipOutputStream zos = new ZipOutputStream(out)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        byte[] content = zis.readAllBytes();
        if (TrainingDocxTemplateValidator.isWordPart(entry.getName())) {
          String xml = new String(content, StandardCharsets.UTF_8);
          xml = TrainingDocxTemplateValidator.fixBrokenMacros(xml);
          for (Map.Entry<String, String> var : ordered) {
            xml = replaceMacro(xml, var.getKey(), escapeXml(var.getValue()));
          }
          content = xml.getBytes(StandardCharsets.UTF_8);
        }
        ZipEntry outEntry = new ZipEntry(entry.getName());
        zos.putNextEntry(outEntry);
        zos.write(content);
        zos.closeEntry();
      }
    }
    return out.toByteArray();
  }

  private static String replaceMacro(String xml, String key, String value) {
    String replaced = Pattern.compile(buildBraceMacroPattern(key))
        .matcher(xml)
        .replaceAll(Matcher.quoteReplacement(value));
    replaced = Pattern.compile(buildDollarMacroPattern(key))
        .matcher(replaced)
        .replaceAll(Matcher.quoteReplacement(value));
    return replaced;
  }

  /** ${key}，允许 XML 标签打断占位符（Word 拼写检查常拆开 worktype 等英文变量） */
  private static String buildBraceMacroPattern(String key) {
    StringBuilder sb = new StringBuilder("\\$(?:<[^>]+>)*\\{(?:<[^>]+>)*");
    for (int i = 0; i < key.length(); i++) {
      if (i > 0) {
        sb.append("(?:<[^>]+>)*");
      }
      sb.append(Pattern.quote(String.valueOf(key.charAt(i))));
    }
    sb.append("(?:<[^>]+>)*\\}");
    return sb.toString();
  }

  /** $key$ */
  private static String buildDollarMacroPattern(String key) {
    StringBuilder sb = new StringBuilder("\\$(?:<[^>]+>)*");
    for (int i = 0; i < key.length(); i++) {
      if (i > 0) {
        sb.append("(?:<[^>]+>)*");
      }
      sb.append(Pattern.quote(String.valueOf(key.charAt(i))));
    }
    sb.append("(?:<[^>]+>)*\\$");
    return sb.toString();
  }

  private static String escapeXml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}

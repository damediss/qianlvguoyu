package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.util.StringUtils;

/** 从 docx / xlsx 中解析 ${key} / $key$ 占位符（规则引擎，无 LLM） */
public final class SubsidyTemplatePlaceholderParser {

  private static final Pattern BRACE = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
  private static final Pattern DOLLAR = Pattern.compile("\\$([a-zA-Z0-9_]+)\\$");

  private SubsidyTemplatePlaceholderParser() {}

  public static Set<String> extract(byte[] bytes, String fileFormat) throws IOException {
    String fmt = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);
    if ("xlsx".equals(fmt) || "xls".equals(fmt)) {
      return extractFromXlsx(bytes);
    }
    return extractFromDocx(bytes);
  }

  public static Set<String> extractFromDocx(byte[] docxBytes) throws IOException {
    Set<String> keys = new LinkedHashSet<>();
    if (docxBytes == null || docxBytes.length == 0) {
      return keys;
    }
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!TrainingDocxTemplateValidator.isWordPart(entry.getName())) {
          continue;
        }
        String xml = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
        xml = TrainingDocxTemplateValidator.fixBrokenMacros(xml);
        collect(xml, keys);
      }
    }
    return keys;
  }

  public static Set<String> extractFromXlsx(byte[] xlsxBytes) throws IOException {
    return TrainingXlsxVariableReplacer.extractKeys(xlsxBytes);
  }

  private static void collect(String xml, Set<String> keys) {
    if (!StringUtils.hasText(xml)) {
      return;
    }
    Matcher m1 = BRACE.matcher(xml);
    while (m1.find()) {
      keys.add(m1.group(1));
    }
    Matcher m2 = DOLLAR.matcher(xml);
    while (m2.find()) {
      keys.add(m2.group(1));
    }
  }
}

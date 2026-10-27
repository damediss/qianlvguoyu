package com.guoyu.platform.exam.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;

/**
 * 解析纯文本化与手工编辑长度校验（《试题解析Markdown表格化_一期实施方案》§5.12/§5.8）。
 *
 * <p>FAQ 进 WeKnora 前把 Markdown 表格压成顿号/分号短句（无表格原样），
 * 保证纯文本通道无裸管道符；管理端写 resolve 统一走 {@link #assertEditableLength}。
 */
public final class ResolvePlainText {

  private ResolvePlainText() {
  }

  /** 手工编辑上限（§5.8 第三道闸门；前端 TextArea maxLength={2000} 同口径）。 */
  private static final int EDITABLE_MAX = 2000;

  /**
   * 表格扁平化：表头与数据行按列对应，数据行转
   * 「表头1值1、表头2值2；表头1值3、表头2值4」；分隔行与管道符不输出。
   * 无表格原样返回；异常/残行降级为剥掉 | 与分隔行，绝不抛错。
   */
  public static String flatten(String resolve) {
    if (resolve == null || resolve.isBlank()) {
      return resolve;
    }
    try {
      String normalized = resolve.replace("\r\n", "\n").replace('\r', '\n');
      if (!normalized.contains("|")) {
        return resolve;
      }
      String[] lines = normalized.split("\n", -1);
      StringBuilder out = new StringBuilder();
      int i = 0;
      while (i < lines.length) {
        String line = lines[i];
        if (!line.trim().startsWith("|")) {
          appendLine(out, line);
          i++;
          continue;
        }
        // 表格块识别：| 开头 + 分隔行 + | 开头，且块内列数一致
        if (i + 2 >= lines.length
            || !lines[i + 1].trim().startsWith("|")
            || !isSeparator(lines[i + 1])
            || !lines[i + 2].trim().startsWith("|")) {
          appendLine(out, stripPipes(line));
          i++;
          continue;
        }
        int end = i + 3;
        while (end < lines.length && lines[end].trim().startsWith("|")) {
          end++;
        }
        String[] header = cells(lines[i]);
        boolean ragged = header.length < 2;
        for (int k = i + 1; k < end && !ragged; k++) {
          if (isSeparator(lines[k])) {
            continue;
          }
          if (cells(lines[k]).length != header.length) {
            ragged = true;
          }
        }
        if (ragged) {
          // 残行降级：整块剥管道与分隔行
          for (int k = i; k < end; k++) {
            if (!isSeparator(lines[k])) {
              appendLine(out, stripPipes(lines[k]));
            }
          }
          i = end;
          continue;
        }
        // 正常块：表头与数据行按列对应，整表占一行「表头1值1、表头2值2；…」
        if (out.length() > 0) {
          out.append('\n');
        }
        for (int k = i + 2; k < end; k++) {
          String[] row = cells(lines[k]);
          StringBuilder rowText = new StringBuilder();
          for (int c = 0; c < header.length; c++) {
            if (c > 0) {
              rowText.append('、');
            }
            rowText.append(header[c]).append(row[c]);
          }
          if (k > i + 2) {
            out.append('；');
          }
          out.append(rowText);
        }
        i = end;
      }
      return out.toString();
    } catch (Exception e) {
      // 任何异常降级：剥掉管道符与分隔行
      return resolve.replace("|", "");
    }
  }

  /** 管理端写入口统一校验：resolve 超 2000 字抛业务异常（§5.8）。 */
  public static void assertEditableLength(String resolve) {
    if (resolve != null && resolve.length() > EDITABLE_MAX) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析不能超过 2000 字");
    }
  }

  private static void appendLine(StringBuilder out, String line) {
    if (out.length() > 0) {
      out.append('\n');
    }
    // 普通行文一并剥掉裸管道符，保证 WeKnora 纯文本无 |
    out.append(line.replace("|", ""));
  }

  private static boolean isSeparator(String line) {
    String t = line.trim();
    if (!t.startsWith("|")) {
      return false;
    }
    String body = t.replace("|", "").replace("-", "").replace(":", "").trim();
    return body.isEmpty();
  }

  private static String[] cells(String line) {
    String t = line.trim();
    java.util.List<String> parts = new java.util.ArrayList<>();
    for (String piece : t.split("\\|", -1)) {
      String cell = piece.trim();
      if (!cell.isEmpty()) {
        parts.add(cell);
      }
    }
    return parts.toArray(new String[0]);
  }

  private static String stripPipes(String line) {
    String t = line.trim();
    if (isSeparator(t)) {
      return "";
    }
    // 剥管道后压缩残余空白，避免"电压  场景"双空格
    return t.replace("|", "").replaceAll("\\s{2,}", " ").trim();
  }
}

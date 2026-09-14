package com.guoyu.platform.exam.knowledgefix.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 题干/答案规范化工具（去重判重键与分类预处理共用）。
 * 口径必须与去重清单生成 SQL 逐字符一致：去 HTML 标签 → 去 &nbsp; → 去空白（含全角空格）；
 * 答案额外剥离空选项片段（"content":"",）。防通用题干陷阱：判重键必须含选项答案（方案 §1.2/§10 KF-02）。
 */
public final class KnowledgeNormUtil {

  private KnowledgeNormUtil() {
  }

  /** 题干规范化：去标签、去 &nbsp;、去全部空白 */
  public static String normStem(String content) {
    if (content == null) {
      return "";
    }
    String s = content.replaceAll("<[^>]+>", "");
    s = s.replace("&nbsp;", "");
    return s.replaceAll("[\s　]+", "");
  }

  /** 答案规范化：去空白、剥离整个空选项对象、收敛悬挂逗号 */
  public static String normAnswer(String answer) {
    if (answer == null) {
      return "";
    }
    String s = answer.replaceAll("[\\s　]+", "");
    s = s.replaceAll("\\{\"content\":\"\",[^{}]*}", "");
    s = s.replace(",]", "]").replace(",[", "[").replace(",,", ",");
    return s;
  }

  /** 判重键 = MD5(type|normStem|normAnswer)，与生成 SQL 的 MD5(CONCAT(...)) 同构 */
  public static String groupKey(String type, String content, String answer) {
    String payload = (type == null ? "" : type) + "|" + normStem(content) + "|" + normAnswer(answer);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(32);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 不可用", e);
    }
  }

  /** 素养类节点识别（确定性卫兵的白名单口径，方案 §4.4-5） */
  public static boolean isQualityNode(String nodeTitle) {
    if (nodeTitle == null) {
      return false;
    }
    return nodeTitle.contains("精神") || nodeTitle.contains("意识") || nodeTitle.contains("道德")
        || nodeTitle.contains("协作") || nodeTitle.contains("创新") || nodeTitle.contains("诚信")
        || nodeTitle.contains("保密") || nodeTitle.contains("知识产权") || nodeTitle.contains("法规")
        || nodeTitle.contains("职业素养");
  }

  /** 关键词重合校验：节点 title 与题干存在 2 字组重合即视为相关（确定性卫兵） */
  public static boolean hasBigramOverlap(String nodeTitle, String questionText) {
    if (nodeTitle == null || questionText == null || nodeTitle.length() < 2) {
      return false;
    }
    for (int i = 0; i + 2 <= nodeTitle.length(); i++) {
      if (questionText.contains(nodeTitle.substring(i, i + 2))) {
        return true;
      }
    }
    return false;
  }
}

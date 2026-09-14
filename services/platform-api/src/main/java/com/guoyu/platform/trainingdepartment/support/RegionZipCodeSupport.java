package com.guoyu.platform.trainingdepartment.support;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 按地址关键字推断邮政编码（企业主数据缺 zip 时的导出回退）。
 * 长地名优先匹配，避免「平果」误伤。
 */
public final class RegionZipCodeSupport {

  private static final Map<String, String> PLACE_ZIPS = new LinkedHashMap<>();

  static {
    // 平果
    PLACE_ZIPS.put("平果铝", "531499");
    PLACE_ZIPS.put("新安镇", "531405");
    PLACE_ZIPS.put("平果市", "531400");
    PLACE_ZIPS.put("平果县", "531400");
    PLACE_ZIPS.put("平果", "531400");
    // 南宁及周边常见
    PLACE_ZIPS.put("宾阳", "530409");
    PLACE_ZIPS.put("横州", "530300");
    PLACE_ZIPS.put("横县", "530300");
    PLACE_ZIPS.put("邕宁", "530200");
    PLACE_ZIPS.put("武鸣", "530100");
    PLACE_ZIPS.put("南宁", "530000");
    // 百色
    PLACE_ZIPS.put("田东", "531500");
    PLACE_ZIPS.put("田阳", "533600");
    PLACE_ZIPS.put("靖西", "533800");
    PLACE_ZIPS.put("德保", "533700");
    PLACE_ZIPS.put("百色", "533000");
    // 柳州 / 来宾 / 河池 / 桂林 / 梧州 / 玉林 / 贵港 / 钦州 / 北海 / 防城港 / 贺州
    PLACE_ZIPS.put("柳州", "545000");
    PLACE_ZIPS.put("来宾", "546100");
    PLACE_ZIPS.put("河池", "547000");
    PLACE_ZIPS.put("桂林", "541000");
    PLACE_ZIPS.put("梧州", "543000");
    PLACE_ZIPS.put("玉林", "537000");
    PLACE_ZIPS.put("贵港", "537100");
    PLACE_ZIPS.put("钦州", "535000");
    PLACE_ZIPS.put("北海", "536000");
    PLACE_ZIPS.put("防城港", "538000");
    PLACE_ZIPS.put("贺州", "542800");
    PLACE_ZIPS.put("崇左", "532200");
  }

  private RegionZipCodeSupport() {}

  public static String normalizeZip(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String digits = raw.trim().replaceAll("\\D", "");
    return digits.length() == 6 ? digits : "";
  }

  /** 从若干地址文本中按最长地名匹配邮编 */
  public static String inferFromAddresses(String... addresses) {
    if (addresses == null) {
      return "";
    }
    String bestPlace = "";
    String bestZip = "";
    for (String address : addresses) {
      if (!StringUtils.hasText(address)) {
        continue;
      }
      String text = address.trim();
      for (Map.Entry<String, String> e : PLACE_ZIPS.entrySet()) {
        String place = e.getKey();
        if (text.contains(place) && place.length() > bestPlace.length()) {
          bestPlace = place;
          bestZip = e.getValue();
        }
      }
    }
    return bestZip;
  }
}

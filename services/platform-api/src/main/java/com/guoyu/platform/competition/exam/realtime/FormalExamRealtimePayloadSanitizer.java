package com.guoyu.platform.competition.exam.realtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class FormalExamRealtimePayloadSanitizer {

  private static final Set<String> BLOCKED = Set.of(
      "answer", "answers", "answerjson", "selectedoptionids",
      "phone", "mobile", "phonenumber",
      "idcard", "idnumber", "identity", "idno",
      "token", "accesstoken", "refreshtoken", "password", "secret");

  private FormalExamRealtimePayloadSanitizer() {
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> sanitize(Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if (entry.getKey() == null || blocked(entry.getKey())) {
        continue;
      }
      copy.put(entry.getKey(), sanitizeValue(entry.getValue()));
    }
    return copy;
  }

  private static Object sanitizeValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
        if (key == null || blocked(key)) {
          continue;
        }
        nested.put(key, sanitizeValue(entry.getValue()));
      }
      return nested;
    }
    if (value instanceof List<?> list) {
      List<Object> nested = new ArrayList<>(list.size());
      for (Object item : list) {
        nested.add(sanitizeValue(item));
      }
      return nested;
    }
    return value;
  }

  private static boolean blocked(String key) {
    return BLOCKED.contains(key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", ""));
  }
}

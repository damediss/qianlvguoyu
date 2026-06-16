package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.mapper.TrainBaseDataMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainBaseDataMaterialRow;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 加载 train_base_data 码表，对齐 PHP $_baseData[tag][title] = sno-title */
@Component
public class TrainBaseDataLookup {

  private final TrainBaseDataMapper mapper;

  public TrainBaseDataLookup(TrainBaseDataMapper mapper) {
    this.mapper = mapper;
  }

  /** 共享 PHP 库无 tenant_id/deleted，仅用 legacy 列查询 */
  public Map<String, Map<String, String>> loadGroupedByTag() {
    List<TrainBaseDataMaterialRow> rows = mapper.selectMaterialPackRowsLegacy();
    Map<String, Map<String, String>> result = new HashMap<>();
    for (TrainBaseDataMaterialRow row : rows) {
      if (!StringUtils.hasText(row.tag()) || !StringUtils.hasText(row.title())) {
        continue;
      }
      String value = (row.sno() != null ? row.sno() : "") + "-" + row.title();
      result.computeIfAbsent(row.tag(), k -> new LinkedHashMap<>()).put(row.title(), value);
    }
    return result;
  }
}

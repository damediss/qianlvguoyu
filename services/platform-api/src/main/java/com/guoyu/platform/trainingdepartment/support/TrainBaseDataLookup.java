package com.guoyu.platform.trainingdepartment.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.trainingdepartment.entity.TrainBaseDataEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainBaseDataMapper;
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

  public Map<String, Map<String, String>> loadGroupedByTag() {
    List<TrainBaseDataEntity> rows = mapper.selectList(
        new LambdaQueryWrapper<TrainBaseDataEntity>()
            .eq(TrainBaseDataEntity::getDeleted, 0)
            .isNotNull(TrainBaseDataEntity::getTag)
            .ne(TrainBaseDataEntity::getTag, ""));
    Map<String, Map<String, String>> result = new HashMap<>();
    for (TrainBaseDataEntity row : rows) {
      if (!StringUtils.hasText(row.getTag()) || !StringUtils.hasText(row.getTitle())) {
        continue;
      }
      String value = (row.getSno() != null ? row.getSno() : "") + "-" + row.getTitle();
      result.computeIfAbsent(row.getTag(), k -> new LinkedHashMap<>()).put(row.getTitle(), value);
    }
    return result;
  }
}

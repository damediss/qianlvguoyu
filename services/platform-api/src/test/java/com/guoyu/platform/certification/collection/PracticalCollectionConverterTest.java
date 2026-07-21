package com.guoyu.platform.certification.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.certification.collection.converter.PracticalCollectionConverter;
import com.guoyu.platform.certification.collection.vo.CollectionStatsVO;
import com.guoyu.platform.certification.collection.vo.CollectionTaskVO;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class PracticalCollectionConverterTest {

  private final PracticalCollectionConverter converter = new PracticalCollectionConverter();

  @Test
  void taskScopeSummaryUsesAllConfiguredWorkTypesAndLevels() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(10L);
    task.setTaskName("实操采集任务");

    CertificationTaskScopeEntity electrician = scope("电工", "高级", "senior", 1);
    CertificationTaskScopeEntity welder = scope("焊工", "中级", "intermediate", 2);
    CertificationTaskScopeEntity duplicate = scope("电工", "高级", "senior", 3);

    CollectionTaskVO result = converter.toTaskVO(
        task,
        new CollectionStatsVO(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        List.of(electrician, welder, duplicate));

    assertThat(result.workType()).isEqualTo("电工、焊工");
    assertThat(result.level()).isEqualTo("高级、中级");
    assertThat(result.workTypeLevel()).isEqualTo("电工 / 高级、焊工 / 中级");
  }

  @Test
  void levelCodeIsUsedWhenLevelNameIsMissing() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(10L);
    CertificationTaskScopeEntity scope = scope("电工", null, "senior", 1);

    CollectionTaskVO result = converter.toTaskVO(task, null, List.of(scope));

    assertThat(result.workType()).isEqualTo("电工");
    assertThat(result.level()).isEqualTo("senior");
    assertThat(result.workTypeLevel()).isEqualTo("电工 / senior");
  }

  private CertificationTaskScopeEntity scope(String workType, String levelName, String levelCode, int sortOrder) {
    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setWorkTypeName(workType);
    scope.setLevelName(levelName);
    scope.setLevelCode(levelCode);
    scope.setSortOrder(sortOrder);
    return scope;
  }
}

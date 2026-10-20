package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 可见性过滤 SQL 形态（方案 v2.1 §3.4）：and 包住 OR 分支，空 scope 退化为 exam_id。 */
class ZhentiScopeFilterTest {

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, ExamZhentiEntity.class);
  }

  @Test
  @DisplayName("空 scope → 仅 exam_id 等值")
  void emptyScopesFallsBackToExamId() {
    LambdaQueryWrapper<ExamZhentiEntity> wrapper = new LambdaQueryWrapper<>();
    ZhentiScopeFilter.apply(wrapper, 100L, List.of());
    assertThat(wrapper.getSqlSegment()).contains("exam_id");
    assertThat(wrapper.getSqlSegment()).doesNotContain("work_type_id");
  }

  @Test
  @DisplayName("多 scope → OR 分支 + 未归类旧数据兜底，整体被 and 包住")
  void scopesBuildOrGroup() {
    LambdaQueryWrapper<ExamZhentiEntity> wrapper = new LambdaQueryWrapper<>();
    ZhentiScopeFilter.apply(wrapper, 100L,
        List.of(new ZhentiScope(35L, 1), new ZhentiScope(76L, 0)));
    String segment = wrapper.getSqlSegment();
    assertThat(segment).contains("work_type_id");
    assertThat(segment).contains("level_code");
    assertThat(segment).contains("OR");
    assertThat(segment).contains("exam_id");
    // 每个 scope 两个参数 + 未归类兜底一个参数
    assertThat(wrapper.getParamNameValuePairs()).hasSize(5);
  }

  @Test
  @DisplayName("QueryWrapper 形态同样可用（years 用）")
  void queryWrapperVariant() {
    QueryWrapper<ExamZhentiEntity> wrapper = new QueryWrapper<>();
    ZhentiScopeFilter.apply(wrapper, 100L, List.of(new ZhentiScope(35L, 1)));
    assertThat(wrapper.getSqlSegment()).contains("work_type_id");
    assertThat(wrapper.getSqlSegment()).contains("OR");
    assertThat(wrapper.getParamNameValuePairs()).hasSize(3);
  }
}

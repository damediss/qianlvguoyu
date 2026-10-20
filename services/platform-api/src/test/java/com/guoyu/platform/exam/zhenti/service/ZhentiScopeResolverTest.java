package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZhentiScopeResolverTest {

  @Mock
  private TrainExamWorkTypeListMapper workTypeListMapper;

  @Mock
  private ExamWorkTypeMapper examWorkTypeMapper;

  @Mock
  private JdbcTemplate jdbcTemplate;

  private ZhentiScopeResolver resolver;

  /** 单测无 Spring 扫描，LambdaQueryWrapper 需要手动初始化实体的 TableInfo 缓存 */
  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TrainExamWorkTypeListEntity.class);
  }

  @BeforeEach
  void setUp() {
    resolver = new ZhentiScopeResolver(
        workTypeListMapper, examWorkTypeMapper, jdbcTemplate, true);
  }

  private static TrainExamWorkTypeListEntity binding(Long examId, Long workTypeId, String level) {
    TrainExamWorkTypeListEntity entity = new TrainExamWorkTypeListEntity();
    entity.setExamId(examId);
    entity.setWorkTypeId(workTypeId);
    entity.setLevel(level);
    entity.setDeleted(0);
    return entity;
  }

  private static ExamWorkTypeEntity workType(Integer id) {
    ExamWorkTypeEntity entity = new ExamWorkTypeEntity();
    entity.setId(id);
    entity.setTitle("工种" + id);
    entity.setDeleted(0);
    return entity;
  }

  @Test
  @DisplayName("单绑定 → 一个 scope")
  void singleBinding() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(100L, 35L, "1")));
    when(examWorkTypeMapper.selectById(35L)).thenReturn(workType(35));

    assertThat(resolver.resolve(100L)).containsExactly(new ZhentiScope(35L, 1));
  }

  @Test
  @DisplayName("重复 (工种,等级) 去重；多绑定返回并集")
  void dedupAndUnion() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(343L, 76L, "0"),
        binding(343L, 76L, "0"),
        binding(343L, 76L, "1"),
        binding(343L, 76L, "2")));
    when(examWorkTypeMapper.selectById(76L)).thenReturn(workType(76));

    assertThat(resolver.resolve(343L))
        .containsExactly(new ZhentiScope(76L, 0), new ZhentiScope(76L, 1),
            new ZhentiScope(76L, 2));
  }

  @Test
  @DisplayName("空工种/空等级/不可解析等级 → 过滤")
  void nullAndUnparsableFiltered() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(100L, null, "1"),
        binding(100L, 35L, (String) null),
        binding(100L, 35L, "中级")));
    when(examWorkTypeMapper.selectById(35L)).thenReturn(workType(35));

    assertThat(resolver.resolve(100L)).isEmpty();
  }

  @Test
  @DisplayName("工种不存在或已删除 → 过滤（降级 exam_id 私有）")
  void invalidWorkTypeFiltered() {
    ExamWorkTypeEntity deleted = workType(68);
    deleted.setDeleted(1);
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(100L, 68L, "2"),
        binding(100L, 104L, "1"),
        binding(100L, 35L, "1")));
    when(examWorkTypeMapper.selectById(68L)).thenReturn(deleted);
    when(examWorkTypeMapper.selectById(104L)).thenReturn(null);
    when(examWorkTypeMapper.selectById(35L)).thenReturn(workType(35));

    assertThat(resolver.resolve(100L)).containsExactly(new ZhentiScope(35L, 1));
  }

  @Test
  @DisplayName("无绑定 → 空（调用方降级 exam_id）")
  void noBinding() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of());
    assertThat(resolver.resolve(100L)).isEmpty();
    assertThat(resolver.resolveForUpload(100L, null, null)).isNull();
  }

  @Test
  @DisplayName("开关关闭 → 恒为空，上传走私有")
  void scopeDisabled() {
    ZhentiScopeResolver disabled =
        new ZhentiScopeResolver(workTypeListMapper, examWorkTypeMapper, jdbcTemplate, false);
    assertThat(disabled.resolve(100L)).isEmpty();
    assertThat(disabled.resolveForUpload(100L, 35L, 1)).isNull();
    verifyNoInteractions(workTypeListMapper);
  }

  @Test
  @DisplayName("单 scope 上传默认命中；多 scope 未选择 → 400")
  void uploadPolicySelection() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(100L, 35L, "1")));
    when(examWorkTypeMapper.selectById(35L)).thenReturn(workType(35));
    assertThat(resolver.resolveForUpload(100L, null, null)).isEqualTo(new ZhentiScope(35L, 1));

    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(343L, 76L, "0"), binding(343L, 76L, "1")));
    when(examWorkTypeMapper.selectById(76L)).thenReturn(workType(76));
    assertThatThrownBy(() -> resolver.resolveForUpload(343L, null, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("绑定了多个工种/等级");
  }

  @Test
  @DisplayName("多 scope 上传：选择不属于计划的范围 → 400；合法选择 → 命中")
  void uploadPolicyForeignSelection() {
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(343L, 76L, "0"), binding(343L, 76L, "1")));
    when(examWorkTypeMapper.selectById(76L)).thenReturn(workType(76));

    assertThatThrownBy(() -> resolver.resolveForUpload(343L, 99L, 4))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不一致");
    assertThat(resolver.resolveForUpload(343L, 76L, 1)).isEqualTo(new ZhentiScope(76L, 1));
  }

  @Test
  @DisplayName("组内计划数 SQL：join 活跃计划排除孤儿，tenant 前置，逐 scope OR")
  void scopePlanCountsQuery() {
    ZhentiScopeResolver.ScopeCountQuery single =
        ZhentiScopeResolver.buildScopePlanCountQuery(List.of(new ZhentiScope(35L, 1)));
    assertThat(single.sql())
        .contains("JOIN train_exam_list e ON e.id = w.exam_id AND e.deleted = 0");
    assertThat(single.sql()).contains("w.tenant_id = ?");
    assertThat(single.sql()).contains("(w.work_type_id = ? AND w.level = ?)");
    assertThat(single.args()).containsExactly(35L, 1);

    ZhentiScopeResolver.ScopeCountQuery multi = ZhentiScopeResolver.buildScopePlanCountQuery(
        List.of(new ZhentiScope(35L, 1), new ZhentiScope(76L, 0)));
    assertThat(multi.sql()).contains(" OR ");
    assertThat(multi.args()).containsExactly(35L, 1, 76L, 0);
  }

  @Test
  @DisplayName("空 scope 列表不查库")
  void emptyScopesNoQuery() {
    assertThat(resolver.scopePlanCounts(10159L, List.of())).isEmpty();
    verifyNoInteractions(jdbcTemplate);
  }
}

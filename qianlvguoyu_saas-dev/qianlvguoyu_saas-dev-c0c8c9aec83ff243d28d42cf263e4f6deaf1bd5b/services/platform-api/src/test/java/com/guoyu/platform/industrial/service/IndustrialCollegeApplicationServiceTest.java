package com.guoyu.platform.industrial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.industrial.dto.IndustrialCollegeCreateReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeUpdateReq;
import com.guoyu.platform.industrial.entity.IndustrialCollegeEntity;
import com.guoyu.platform.industrial.mapper.IndustrialCollegeMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class IndustrialCollegeApplicationServiceTest {

  private final IndustrialCollegeMapper mapper = Mockito.mock(IndustrialCollegeMapper.class);
  private final IndustrialCollegeApplicationService service = new IndustrialCollegeApplicationService(mapper);

  @Test
  void pageMapsCollegeFields() {
    IndustrialCollegeEntity entity = college(1L, "园区学院A", "本科院校");
    Page<IndustrialCollegeEntity> page = new Page<>(1, 10, 1);
    page.setRecords(List.of(entity));
    when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

    IndustrialCollegeQueryReq req = new IndustrialCollegeQueryReq();
    req.setKeyword("学院");
    req.setTag("本科院校");

    var result = service.page(req);

    assertThat(result.total()).isEqualTo(1);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).getName()).isEqualTo("园区学院A");
    assertThat(result.items().get(0).getBusinessScope()).isEqualTo("工业互联网培训");
  }

  @Test
  void statsReturnsTotalRecentAndTagCounts() {
    when(mapper.selectCount(any(Wrapper.class))).thenReturn(12L, 3L);
    when(mapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(
        Map.of("tag", "本科院校", "college_count", 8L),
        Map.of("tag", "职业院校", "college_count", 4L)
    ));

    var stats = service.stats();

    assertThat(stats.total()).isEqualTo(12);
    assertThat(stats.recentCreated()).isEqualTo(3);
    assertThat(stats.byTag()).extracting("tag").containsExactly("本科院校", "职业院校");
  }

  @Test
  void createDefaultsStatusAndSortOrder() {
    IndustrialCollegeCreateReq req = new IndustrialCollegeCreateReq(
        "园区学院A", "本科院校", "工业互联网培训", "简介", "培训资质", "认定资质", "地址", "123456", null, null);

    service.create(req);

    ArgumentCaptor<IndustrialCollegeEntity> captor = ArgumentCaptor.forClass(IndustrialCollegeEntity.class);
    verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("enabled");
    assertThat(captor.getValue().getSortOrder()).isEqualTo(100);
    assertThat(captor.getValue().getDeleted()).isZero();
  }

  @Test
  void updateRequiresExistingCollege() {
    when(mapper.selectById(99L)).thenReturn(null);
    IndustrialCollegeUpdateReq req = new IndustrialCollegeUpdateReq(
        "园区学院A", "本科院校", null, null, null, null, null, null, "enabled", 1);

    assertThatThrownBy(() -> service.update(99L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("园区学院不存在");
  }

  @Test
  void deleteUsesLogicalDeleteByIdAfterExistenceCheck() {
    when(mapper.selectById(1L)).thenReturn(college(1L, "园区学院A", "本科院校"));

    service.delete(1L);

    verify(mapper).deleteById(1L);
  }

  private IndustrialCollegeEntity college(Long id, String name, String tag) {
    IndustrialCollegeEntity entity = new IndustrialCollegeEntity();
    entity.setId(id);
    entity.setName(name);
    entity.setTag(tag);
    entity.setBusinessScope("工业互联网培训");
    entity.setDescription("简介");
    entity.setTrainingQualification("培训资质");
    entity.setCertificationQualification("认定资质");
    entity.setAddress("地址");
    entity.setPhone("123456");
    entity.setStatus("enabled");
    entity.setSortOrder(1);
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }
}

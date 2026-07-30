package com.guoyu.platform.industrial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.industrial.dto.IndustrialPlatformItemReq;
import com.guoyu.platform.industrial.dto.IndustrialPlatformQueryReq;
import com.guoyu.platform.industrial.entity.IndustrialPlatformItemEntity;
import com.guoyu.platform.industrial.mapper.IndustrialPlatformItemMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class IndustrialPlatformApplicationServiceTest {

  private final IndustrialPlatformItemMapper mapper = Mockito.mock(IndustrialPlatformItemMapper.class);
  private final IndustrialPlatformApplicationService service = new IndustrialPlatformApplicationService(mapper);

  @Test
  void pageReturnsOnlyRequestedPlatformCategoryFields() {
    IndustrialPlatformItemEntity entity = item(1L, "master", "国昱铝业", "技能大师工作室", 12);
    Page<IndustrialPlatformItemEntity> page = new Page<>(1, 10, 1);
    page.setRecords(List.of(entity));
    when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

    IndustrialPlatformQueryReq req = new IndustrialPlatformQueryReq();
    req.setKeyword("国昱");
    req.setPageSize(10);

    var result = service.page("master", req);

    assertThat(result.total()).isEqualTo(1);
    assertThat(result.items().getFirst().category()).isEqualTo("master");
    assertThat(result.items().getFirst().company()).isEqualTo("国昱铝业");
    assertThat(result.items().getFirst().name()).isEqualTo("技能大师工作室");
    assertThat(result.items().getFirst().nums()).isEqualTo(12);
  }

  @Test
  void createMasterRequiresTeamSize() {
    IndustrialPlatformItemReq req = new IndustrialPlatformItemReq("国昱铝业", "技能大师工作室", null, null, null);

    assertThatThrownBy(() -> service.create("master", req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("团队人数不能为空");
  }

  @Test
  void createMatchRequiresDate() {
    IndustrialPlatformItemReq req = new IndustrialPlatformItemReq("国昱铝业", "技能竞赛", null, null, null);

    assertThatThrownBy(() -> service.create("match", req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("日期不能为空");
  }

  @Test
  void createCommentPersistsState() {
    IndustrialPlatformItemReq req = new IndustrialPlatformItemReq("国昱铝业", "自主评价项目", null,
        LocalDate.of(2026, 5, 27), "已完成");

    service.create("comment", req);

    ArgumentCaptor<IndustrialPlatformItemEntity> captor = ArgumentCaptor.forClass(IndustrialPlatformItemEntity.class);
    verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getCategory()).isEqualTo("comment");
    assertThat(captor.getValue().getCompany()).isEqualTo("国昱铝业");
    assertThat(captor.getValue().getState()).isEqualTo("已完成");
    assertThat(captor.getValue().getDeleted()).isZero();
  }

  @Test
  void updateRejectsChangingMissingRecord() {
    when(mapper.selectById(99L)).thenReturn(null);
    IndustrialPlatformItemReq req = new IndustrialPlatformItemReq("国昱铝业", "学习型班组", 8, null, null);

    assertThatThrownBy(() -> service.update("study", 99L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("平台建设数据不存在");
  }

  @Test
  void getRejectsRecordFromAnotherCategory() {
    when(mapper.selectById(7L)).thenReturn(item(7L, "study", "国昱铝业", "学习型班组", 8));

    assertThatThrownBy(() -> service.getById("master", 7L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("平台建设数据不存在");
  }

  @Test
  void deleteRejectsUnsupportedCategoryBeforeDeleting() {
    assertThatThrownBy(() -> service.delete("unknown", 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("平台建设分类不正确");
  }

  private IndustrialPlatformItemEntity item(Long id, String category, String company, String name, Integer nums) {
    IndustrialPlatformItemEntity entity = new IndustrialPlatformItemEntity();
    entity.setId(id);
    entity.setCategory(category);
    entity.setCompany(company);
    entity.setName(name);
    entity.setNums(nums);
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }
}

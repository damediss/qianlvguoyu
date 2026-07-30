package com.guoyu.platform.industrial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.industrial.dto.IndustrialTalentServiceQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialTalentTrainingQueryReq;
import com.guoyu.platform.industrial.entity.IndustrialTalentServiceEntity;
import com.guoyu.platform.industrial.entity.IndustrialTalentTrainingEntity;
import com.guoyu.platform.industrial.mapper.IndustrialTalentServiceMapper;
import com.guoyu.platform.industrial.mapper.IndustrialTalentTrainingMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndustrialTalentApplicationServiceTest {

  @Mock
  private IndustrialTalentTrainingMapper trainingMapper;

  @Mock
  private IndustrialTalentServiceMapper serviceMapper;

  @InjectMocks
  private IndustrialTalentApplicationService service;

  @Test
  void pageTrainingMapsEntitiesAndReturnsLevelFromDatabase() {
    IndustrialTalentTrainingEntity entity = trainingEntity();
    Page<IndustrialTalentTrainingEntity> page = new Page<>(1, 10, 1);
    page.setRecords(List.of(entity));
    when(trainingMapper.selectPage(any(), any())).thenReturn(page);

    IndustrialTalentTrainingQueryReq req = new IndustrialTalentTrainingQueryReq();
    req.setKeyword("千铝");

    var result = service.pageTraining(req);

    assertThat(result.total()).isEqualTo(1);
    assertThat(result.items().getFirst().name()).isEqualTo("千铝培训");
    assertThat(result.items().getFirst().level()).isEqualTo("高级工");
    assertThat(result.items().getFirst().workType()).isEqualTo("电解槽操作工");
    assertThat(result.items().getFirst().education()).isEqualTo("大专及以上");
    verify(trainingMapper).selectPage(any(), any());
  }

  @Test
  void pageServiceMapsEntitiesWithDateField() {
    IndustrialTalentServiceEntity entity = serviceEntity();
    Page<IndustrialTalentServiceEntity> page = new Page<>(1, 10, 1);
    page.setRecords(List.of(entity));
    when(serviceMapper.selectPage(any(), any())).thenReturn(page);

    var result = service.pageService(new IndustrialTalentServiceQueryReq());

    assertThat(result.items().getFirst().title()).contains("技师认定");
    assertThat(result.items().getFirst().source()).isEqualTo("千铝工投");
    assertThat(result.items().getFirst().date()).isEqualTo("2025.9.10");
    verify(serviceMapper).selectPage(any(), any());
  }

  private IndustrialTalentTrainingEntity trainingEntity() {
    IndustrialTalentTrainingEntity entity = new IndustrialTalentTrainingEntity();
    entity.setId(800001L);
    entity.setName("千铝培训");
    entity.setLevel("高级工");
    entity.setWorkType("电解槽操作工");
    entity.setZhiwei("班组长");
    entity.setEducation("大专及以上");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  private IndustrialTalentServiceEntity serviceEntity() {
    IndustrialTalentServiceEntity entity = new IndustrialTalentServiceEntity();
    entity.setId(800101L);
    entity.setTitle("2025.8.5技师认定交流会");
    entity.setSource("千铝工投");
    entity.setServiceDate("2025.9.10");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }
}

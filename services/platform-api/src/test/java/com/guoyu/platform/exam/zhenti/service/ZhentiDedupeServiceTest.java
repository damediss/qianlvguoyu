package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZhentiDedupeServiceTest {

  @Mock
  private ExamZhentiMapper mapper;

  @Mock
  private ZhentiStorageService storageService;

  private ZhentiIngestService ingestService;

  private static final byte[] PDF =
      "%PDF-1.4\ncontent\n%%EOF".getBytes(StandardCharsets.US_ASCII);

  @BeforeEach
  void setUp() {
    ingestService = new ZhentiIngestService(
        mapper, storageService, new ZhentiUploadValidator(), new ZhentiFileNameParser());
  }

  @Test
  @DisplayName("同考试项目同 MD5 → 409 且提示已有标题，不写存储")
  void duplicateMd5Rejected() {
    ExamZhentiEntity existing = new ExamZhentiEntity();
    existing.setId(1L);
    existing.setTitle("已有真题");
    when(mapper.selectOne(any())).thenReturn(existing);

    assertThatThrownBy(() -> ingestService.ingest(
        10159L, 100L, 1L, "新文件-2024.pdf", PDF, null, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("409"))
        .hasMessageContaining("已存在相同文件：已有真题");

    verify(storageService, never()).putPdf(any(), any(), any());
    verify(mapper, never()).insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any());
  }

  @Test
  @DisplayName("无重复 → 入库并返回解析后的标题/年份")
  void ingestNewFile() {
    when(mapper.selectOne(any())).thenReturn(null);
    when(storageService.putPdf(any(), any(), any())).thenReturn("zhenti/100/x.pdf");
    when(mapper.insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any())).thenAnswer(invocation -> {
      ExamZhentiEntity entity = invocation.getArgument(0);
      entity.setId(9L);
      return 1;
    });

    ExamZhentiEntity saved = ingestService.ingest(
        10159L, 100L, 1L, "铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf", PDF, null, null);

    assertThat(saved.getId()).isEqualTo(9L);
    assertThat(saved.getTitle()).isEqualTo("铝电解工（铝及铝合金熔铸工）五级理论科目");
    assertThat(saved.getYear()).isEqualTo("2024");
    assertThat(saved.getStatus()).isEqualTo(1);
    assertThat(saved.getMetaPending()).isZero();
  }
}

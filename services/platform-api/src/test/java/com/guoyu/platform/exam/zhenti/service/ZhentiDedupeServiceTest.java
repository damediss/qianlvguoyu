package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.dao.DuplicateKeyException;

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
  @DisplayName("私有口径同 MD5 → 409，提示语带计划语义，不写存储")
  void duplicateMd5RejectedPrivate() {
    ExamZhentiEntity existing = new ExamZhentiEntity();
    existing.setId(1L);
    existing.setTitle("已有真题");
    when(mapper.selectOne(any())).thenReturn(existing);

    assertThatThrownBy(() -> ingestService.ingest(
        10159L, 100L, 1L, "新文件-2024.pdf", PDF, null, null, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("409"))
        .hasMessageContaining("该考试计划下已存在相同文件：已有真题");

    verify(storageService, never()).putPdf(anyLong(), anyString(), any());
    verify(mapper, never()).insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any());
  }

  @Test
  @DisplayName("共享组口径同 MD5 → 409，提示语带共享组语义")
  void duplicateMd5RejectedScoped() {
    ExamZhentiEntity existing = new ExamZhentiEntity();
    existing.setId(2L);
    existing.setTitle("共享真题");
    when(mapper.selectOne(any())).thenReturn(existing);

    assertThatThrownBy(() -> ingestService.ingest(
        10159L, 100L, 1L, "新文件-2024.pdf", PDF, null, null, new ZhentiScope(35L, 1)))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("409"))
        .hasMessageContaining("同工种同等级下已存在相同文件：共享真题");
  }

  @Test
  @DisplayName("并发撞唯一键 → 取回已提交行转 409，并回删存储")
  void duplicateKeyExceptionConvertedToConflict() {
    when(mapper.selectOne(any()))
        .thenReturn(null)
        .thenAnswer(invocation -> {
          ExamZhentiEntity raced = new ExamZhentiEntity();
          raced.setId(3L);
          raced.setTitle("并发已入");
          return raced;
        });
    when(storageService.putPdf(anyLong(), anyString(), any())).thenReturn("zhenti/100/x.pdf");
    when(mapper.insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any()))
        .thenThrow(new DuplicateKeyException("uk_zhenti_scope_md5"));

    assertThatThrownBy(() -> ingestService.ingest(
        10159L, 100L, 1L, "并发-2024.pdf", PDF, null, null, new ZhentiScope(35L, 1)))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("409"))
        .hasMessageContaining("同工种同等级下已存在相同文件：并发已入");

    verify(storageService).delete("zhenti/100/x.pdf");
  }

  @Test
  @DisplayName("无重复 → 入库并写入共享组字段")
  void ingestNewFile() {
    when(mapper.selectOne(any())).thenReturn(null);
    when(storageService.putPdf(anyLong(), anyString(), any())).thenReturn("zhenti/100/x.pdf");
    when(mapper.insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any())).thenAnswer(invocation -> {
      ExamZhentiEntity entity = invocation.getArgument(0);
      entity.setId(9L);
      return 1;
    });

    ExamZhentiEntity saved = ingestService.ingest(
        10159L, 100L, 1L, "铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf", PDF, null, null,
        new ZhentiScope(35L, 1));

    assertThat(saved.getId()).isEqualTo(9L);
    assertThat(saved.getTitle()).isEqualTo("铝电解工（铝及铝合金熔铸工）五级理论科目");
    assertThat(saved.getYear()).isEqualTo("2024");
    assertThat(saved.getWorkTypeId()).isEqualTo(35L);
    assertThat(saved.getLevelCode()).isEqualTo(1);
    assertThat(saved.getScopeKey()).isEqualTo("work:35:1");
    assertThat(saved.getStatus()).isEqualTo(1);
    assertThat(saved.getMetaPending()).isZero();
  }

  @Test
  @DisplayName("无共享组 → scope_key 为 exam 前缀，共享组列为空")
  void ingestPrivateScopeKey() {
    when(mapper.selectOne(any())).thenReturn(null);
    when(storageService.putPdf(anyLong(), anyString(), any())).thenReturn("zhenti/100/y.pdf");
    when(mapper.insert(org.mockito.ArgumentMatchers.<ExamZhentiEntity>any())).thenAnswer(invocation -> {
      ExamZhentiEntity entity = invocation.getArgument(0);
      entity.setId(10L);
      return 1;
    });

    ExamZhentiEntity saved = ingestService.ingest(
        10159L, 100L, 1L, "私有-2024.pdf", PDF, null, null, null);

    assertThat(saved.getScopeKey()).isEqualTo("exam:100");
    assertThat(saved.getWorkTypeId()).isNull();
    assertThat(saved.getLevelCode()).isNull();
  }
}

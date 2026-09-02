package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.exam.standard.service.StandardExportApplicationService;
import com.guoyu.platform.exam.standard.service.StandardLlmSummarizeService;
import com.guoyu.platform.exam.standard.service.StandardParseApplicationService;
import com.guoyu.platform.exam.standard.vo.StandardTaskVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.system.mapper.DictMapper;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * C 组：服务/接口（方案 §13.1 C1~C6 中可在单测覆盖的部分；权限矩阵见 C5 反射测试）。
 */
class StandardServiceTest {

  private StandardParseApplicationService newParseService(ExamStandardTaskMapper taskMapper,
      ExamStandardNodeMapper nodeMapper, FileApplicationService fileService) {
    return new StandardParseApplicationService(taskMapper, nodeMapper, fileService,
        new ObjectMapper());
  }

  private byte[] kuangshanDocx() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/standard/矿山救护工.docx")) {
      return in.readAllBytes();
    }
  }

  /** C1：上传解析——任务 status=parsed，staging 树落库 */
  @Test
  void c1_uploadAndParse() throws Exception {
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    FileApplicationService fileService = mock(FileApplicationService.class);
    when(taskMapper.selectOne(any())).thenReturn(null);
    when(taskMapper.insert(any(ExamStandardTaskEntity.class))).thenReturn(1);
    when(nodeMapper.selectCount(any())).thenReturn(0L);
    when(fileService.storeFromBytes(any(), any(), any(), any()))
        .thenReturn(new FileVO(999L, "矿山救护工.docx", "application/octet-stream", 100L,
            "private", "stored", LocalDateTime.now(), null));

    StandardTaskVO vo = newParseService(taskMapper, nodeMapper, fileService)
        .uploadAndParse(kuangshanDocx(), "矿山救护工.docx", 1L, 1L);

    assertEquals("parsed", vo.status());
    assertEquals("矿山救护工", vo.workTypeTitle());
    assertEquals("6-16-01-12", vo.seriesNo());
    assertEquals(List.of("默认"), vo.directions());
    assertEquals(5, vo.levels().size());
    // 骨架节点落库：5 等级 × (A 支 10 + B 支函数若干) > 100 个
    verify(nodeMapper, atLeast(100)).insert(any(ExamStandardNodeEntity.class));
  }

  /** C2：重复上传——同 fileHash → 409，不产生重复任务 */
  @Test
  void c2_duplicateUploadConflict() throws Exception {
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    FileApplicationService fileService = mock(FileApplicationService.class);
    ExamStandardTaskEntity existing = new ExamStandardTaskEntity();
    existing.setId(12345L);
    existing.setWorkTypeTitle("矿山救护工");
    when(taskMapper.selectOne(any())).thenReturn(existing);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> newParseService(taskMapper, nodeMapper, fileService)
            .uploadAndParse(kuangshanDocx(), "矿山救护工.docx", 1L, 1L));
    assertEquals("409", ex.getCode());
    assertTrue(ex.getMessage().contains("12345"));
  }

  /** C3：summarize 并发——同任务重复触发 → 409 */
  @Test
  void c3_summarizeConcurrentConflict() {
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);
    ExamStandardTaskEntity running = new ExamStandardTaskEntity();
    running.setId(1L);
    running.setStatus("llm_running");
    when(taskMapper.selectOne(any())).thenReturn(running);

    StandardLlmSummarizeService service = new StandardLlmSummarizeService(
        taskMapper, mock(ExamStandardNodeMapper.class), mock(FaqLlmClient.class),
        mock(FaqModelConfigApplicationService.class), mock(DictMapper.class),
        mock(AiGenerationDraftMapper.class), mock(ThreadPoolTaskExecutor.class));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.triggerSummarize(1L, 1L, 1L));
    assertEquals("409", ex.getCode());
  }

  /** C4：导出允许待审核叶节点，不再 400 拦截 */
  @Test
  void c4_exportAllowsPendingLeaves() {
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    ExamStandardTaskEntity task = new ExamStandardTaskEntity();
    task.setId(1L);
    task.setStatus("reviewing");
    task.setWorkTypeTitle("测试职业");
    task.setSeriesNo("1-1-1");
    task.setVersion("2024");
    when(taskMapper.selectOne(any())).thenReturn(task);

    ExamStandardNodeEntity rootA = new ExamStandardNodeEntity();
    rootA.setId(1L);
    rootA.setTaskId(1L);
    rootA.setNodeLevel(0);
    rootA.setNodeKey("A");
    rootA.setTitle("基本要求");
    rootA.setSortNumber(1);
    rootA.setDirection("默认");
    rootA.setLevelCode(0);
    rootA.setReviewStatus("approved");

    ExamStandardNodeEntity leaf = new ExamStandardNodeEntity();
    leaf.setId(2L);
    leaf.setParentId(1L);
    leaf.setTaskId(1L);
    leaf.setNodeLevel(3);
    leaf.setNodeKey("A-A-001");
    leaf.setTitle("工匠精神");
    leaf.setSortNumber(1);
    leaf.setDirection("默认");
    leaf.setLevelCode(0);
    leaf.setDimension("theory");
    leaf.setReviewStatus("pending");

    when(nodeMapper.selectList(any())).thenReturn(List.of(rootA, leaf));

    StandardExportApplicationService service =
        new StandardExportApplicationService(taskMapper, nodeMapper,
            new com.fasterxml.jackson.databind.ObjectMapper());
    StandardExportApplicationService.ExportFile file =
        service.exportSql(1L, 1L, "tester", "create");
    assertNotNull(file);
    assertTrue(file.fileName().startsWith("测试职业"));
    assertTrue(new String(file.content(), java.nio.charset.StandardCharsets.UTF_8)
        .contains("INSERT INTO"), "SQL 导出应包含 INSERT");
  }

  /** C6：LLM 配置缺失 → 触发报错且任务保持 parsed（降级：可人工补叶导出） */
  @Test
  void c6_llmConfigMissingDegrades() {
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);
    ExamStandardTaskEntity task = new ExamStandardTaskEntity();
    task.setId(1L);
    task.setStatus("parsed");
    when(taskMapper.selectOne(any())).thenReturn(task);
    DictMapper dictMapper = mock(DictMapper.class);
    when(dictMapper.selectList(any())).thenReturn(List.of());
    FaqModelConfigApplicationService modelConfigService =
        mock(FaqModelConfigApplicationService.class);
    when(modelConfigService.findByModelCode(any(), any())).thenReturn(null);

    StandardLlmSummarizeService service = new StandardLlmSummarizeService(
        taskMapper, mock(ExamStandardNodeMapper.class), mock(FaqLlmClient.class),
        modelConfigService, dictMapper, mock(AiGenerationDraftMapper.class),
        mock(ThreadPoolTaskExecutor.class));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.triggerSummarize(1L, 1L, 1L));
    assertTrue(ex.getMessage().contains("模型配置缺失"));
    assertEquals("parsed", task.getStatus(), "任务状态保持 parsed（人工补叶降级链不断）");
  }
}

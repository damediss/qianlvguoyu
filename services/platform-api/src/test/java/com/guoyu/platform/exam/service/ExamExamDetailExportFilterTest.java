package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 导出试题勾选过滤（2026-09-23）：ids 非空仅导出勾选题，null/空数组保持全量。
 * 用 getSqlSegment() 断言查询条件；selectList 返回空走业务异常，避免进入 POI 导出。
 */
@ExtendWith(MockitoExtension.class)
class ExamExamDetailExportFilterTest {

  @Mock
  private ExamExamDetailMapper mapper;
  @Mock
  private ExamKnowledgeStructureMapper knowledgeMapper;
  @Mock
  private ExamCourseMapper courseMapper;
  @Mock
  private ExamWorkTypeMapper workTypeMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private ExamKnowledgeStructureApplicationService knowledgeService;
  @Mock
  private ObjectMapper objectMapper;

  private ExamExamDetailApplicationService service;

  @BeforeAll
  static void initTableInfo() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExamExamDetailEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ExamExamDetailApplicationService(mapper, knowledgeMapper, courseMapper,
        workTypeMapper, userMapper, knowledgeService, objectMapper);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new PlatformUserPrincipal(1L, 10159L, "tester", "测试用户", "admin", List.of(), List.of(), null),
        "token", List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** 执行导出并捕获最终 SQL 片段；空结果集触发业务异常即返回（不进入 POI 写出）。 */
  private String captureSegment(List<Integer> ids) {
    AtomicReference<String> segment = new AtomicReference<>("");
    when(knowledgeService.getNodeIdsByCourse(anyInt())).thenReturn(List.of(11, 12));
    // 用 doAnswer 而非 when(...).thenAnswer：后者设桩时会真实调用一次 mock，
    // 触发上一个 answer 以 null 入参执行（同用例内第二次 captureSegment 即踩此坑）
    doAnswer(invocation -> {
      LambdaQueryWrapper<ExamExamDetailEntity> wrapper = invocation.getArgument(0);
      segment.set(wrapper.getSqlSegment());
      return List.of();
    }).when(mapper).selectList(any());
    assertThrows(BusinessException.class, () -> service.exportByCourse(205, ids));
    return segment.get();
  }

  @Test
  @DisplayName("★ 勾选导出：ids 非空时追加 id IN 过滤（可跨页勾选）")
  void exportWithSelectedIdsAddsIdFilter() {
    assertTrue(captureSegment(List.of(101, 102)).contains("AND id IN"));
  }

  @Test
  @DisplayName("未勾选（null 或空数组）：不加 id 过滤，保持全量导出")
  void exportWithoutIdsOmitsIdFilter() {
    assertFalse(captureSegment(null).contains("AND id IN"));
    assertFalse(captureSegment(List.of()).contains("AND id IN"));
  }

  @Test
  @DisplayName("科目下无知识点：业务异常且不查试题表")
  void exportWithoutKnowledgeThrows() {
    when(knowledgeService.getNodeIdsByCourse(anyInt())).thenReturn(List.of());
    BusinessException ex =
        assertThrows(BusinessException.class, () -> service.exportByCourse(205, List.of(1)));
    assertTrue(ex.getMessage().contains("无知识点"));
  }
}

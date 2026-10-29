package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.FaqModelConfigUpdateReq;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.vo.FaqModelConfigVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** v1.5 模型配置集中管理：专用 JEV 行保护、VO 脱敏、通用下拉过滤。 */
@ExtendWith(MockitoExtension.class)
class FaqModelConfigApplicationServiceTest {

  @Mock
  private ExamFaqModelConfigMapper mapper;

  private FaqModelConfigApplicationService service;
  private MockedStatic<SecurityContext> security;

  @BeforeAll
  static void initMyBatisPlusLambdaCache() {
    com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
        new com.baomidou.mybatisplus.core.MybatisConfiguration();
    org.apache.ibatis.builder.MapperBuilderAssistant assistant =
        new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
    com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant,
        ExamFaqModelConfigEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new FaqModelConfigApplicationService(mapper);
    security = mockStatic(SecurityContext.class);
    security.when(SecurityContext::currentUser).thenReturn(new PlatformUserPrincipal(1L, 10159L,
        "admin", "管理员", "admin", List.of("PLATFORM_ADMIN"), List.of("exam:faq:config"), null));
  }

  @AfterEach
  void tearDown() {
    security.close();
  }

  private ExamFaqModelConfigEntity objectiveJevConfig() {
    ExamFaqModelConfigEntity entity = new ExamFaqModelConfigEntity();
    entity.setId(202610290101L);
    entity.setTenantId(10159L);
    entity.setModelCode(FaqModelConfigApplicationService.OBJECTIVE_ANSWER_CHECK_JEV_MODEL_CODE);
    entity.setModelName("客观题答案检查 JEV");
    entity.setApiKey(FaqModelConfigApplicationService.MODEL_API_KEY_CONFIG_FILE_PLACEHOLDER);
    entity.setStatus("disabled");
    entity.setJevConcurrency(5);
    entity.setJevReadTimeoutSeconds(30);
    return entity;
  }

  @Test
  void updateObjectiveJevRejectsRealApiKeyAndPreservesPlaceholder() {
    ExamFaqModelConfigEntity entity = objectiveJevConfig();
    when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(entity);

    FaqModelConfigUpdateReq req = new FaqModelConfigUpdateReq(null, null, "real-secret", null,
        null, null, null, null, 8, 60, 1);
    BusinessException error = assertThrows(BusinessException.class,
        () -> service.update(entity.getId(), req));
    assertEquals("409", error.getCode());
    assertEquals(FaqModelConfigApplicationService.MODEL_API_KEY_CONFIG_FILE_PLACEHOLDER,
        entity.getApiKey());
  }

  @Test
  void updateObjectiveJevWithoutApiKeyCanManageNonSecretRuntimeFields() {
    ExamFaqModelConfigEntity entity = objectiveJevConfig();
    when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(entity);

    FaqModelConfigUpdateReq req = new FaqModelConfigUpdateReq("客观题答案检查 JEV",
        "https://tokendance.space/gateway", null, "custom", "ateve-jev-v1", new BigDecimal("0.70"),
        2000, null, 6, 45, 1);
    service.update(entity.getId(), req);

    verify(mapper).updateById(entity);
    assertEquals("enabled", entity.getStatus());
    assertEquals(6, entity.getJevConcurrency());
    assertEquals(45, entity.getJevReadTimeoutSeconds());
    assertEquals(FaqModelConfigApplicationService.MODEL_API_KEY_CONFIG_FILE_PLACEHOLDER,
        entity.getApiKey());
  }

  @Test
  void updateNormalModelIgnoresBlankApiKeyAndPreservesOldKey() {
    ExamFaqModelConfigEntity entity = objectiveJevConfig();
    entity.setId(202610290102L);
    entity.setModelCode("deepseek-v3");
    entity.setApiKey("old-secret");
    when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(entity);

    FaqModelConfigUpdateReq req = new FaqModelConfigUpdateReq("DeepSeek V3",
        "https://api.deepseek.com", "", "openai_compatible", "deepseek-chat", null, null, null,
        null, null, 1);
    service.update(entity.getId(), req);

    verify(mapper).updateById(entity);
    org.junit.jupiter.api.Assertions.assertEquals("old-secret", entity.getApiKey());
  }

  @Test
  void deleteObjectiveJevSystemManagedRowIsRejected() {
    ExamFaqModelConfigEntity entity = objectiveJevConfig();
    when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(entity);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.delete(entity.getId()));
    assertEquals("409", error.getCode());
    org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).deleteById(entity.getId());
  }

  @Test
  void createReservedObjectiveJevCodeIsRejected() {
    com.guoyu.platform.exam.dto.FaqModelConfigCreateReq req =
        new com.guoyu.platform.exam.dto.FaqModelConfigCreateReq(
            FaqModelConfigApplicationService.OBJECTIVE_ANSWER_CHECK_JEV_MODEL_CODE,
            "客观题答案检查 JEV", "https://tokendance.space/gateway",
            FaqModelConfigApplicationService.MODEL_API_KEY_CONFIG_FILE_PLACEHOLDER, "custom",
            "ateve-jev-v1", new BigDecimal("0.70"), 2000, null, 5, 30, 1);
    BusinessException error = assertThrows(BusinessException.class, () -> service.create(req));
    assertEquals("409", error.getCode());
  }

  @Test
  void placeholderApiKeyIsDisplayedAsConfigFileProvided() {
    FaqModelConfigVO vo = FaqModelConfigVO.from(objectiveJevConfig());
    assertEquals("配置文件提供", vo.getApiKeyMasked());
  }

  @Test
  void enabledListFiltersToFaqConversionScopes() {
    service.listEnabled();
    ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamFaqModelConfigEntity>> captor =
        ArgumentCaptor.forClass(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
    verify(mapper).selectList(captor.capture());
    String sql = captor.getValue().getSqlSegment();
    org.junit.jupiter.api.Assertions.assertTrue(sql.contains("feature_scope"));
  }
}

package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintVO;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import com.guoyu.platform.exam.skillgen.parser.SkillBlueprintXlsxParser;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

/** 细目表导入单测：校验报告、full_code 拼装、重做闸门、试题量列提示（方案 §T2/IMP-3~4） */
class SkillBlueprintApplicationServiceTest {

  private static final Long TENANT = 1L;

  private SkillBlueprintMapper blueprintMapper;
  private SkillBlueprintNodeMapper nodeMapper;
  private SkillGenBatchMapper batchMapper;
  private SkillGenDraftMapper draftMapper;
  private ExamWorkTypeMapper workTypeMapper;
  private FileApplicationService fileService;
  private SkillBlueprintApplicationService service;
  private MockedStatic<SecurityContext> securityContext;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, SkillBlueprintEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillBlueprintNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillGenDraftEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillGenBatchEntity.class);
  }

  @BeforeEach
  void setUp() {
    blueprintMapper = mock(SkillBlueprintMapper.class);
    nodeMapper = mock(SkillBlueprintNodeMapper.class);
    batchMapper = mock(SkillGenBatchMapper.class);
    draftMapper = mock(SkillGenDraftMapper.class);
    workTypeMapper = mock(ExamWorkTypeMapper.class);
    fileService = mock(FileApplicationService.class);
    service = new SkillBlueprintApplicationService(blueprintMapper, nodeMapper, batchMapper,
        draftMapper, new SkillBlueprintXlsxParser(), workTypeMapper,
        mock(ExamTaskExamshicaoMapper.class), mock(ExamTaskExamshicaoDetailMapper.class),
        new ObjectMapper(), fileService);
    ExamWorkTypeEntity wt = new ExamWorkTypeEntity();
    wt.setId(22);
    wt.setTitle("矿山生产集控员");
    when(workTypeMapper.selectById(22L)).thenReturn(wt);
    when(fileService.storeFromBytes(any(), any(), any(), any()))
        .thenReturn(new FileVO(1L, "样例.xlsx", null, null, "private", "stored", null, null));
    PlatformUserPrincipal user = mock(PlatformUserPrincipal.class);
    when(user.getUserId()).thenReturn(9L);
    securityContext = Mockito.mockStatic(SecurityContext.class);
    securityContext.when(SecurityContext::currentUser).thenReturn(user);
    com.guoyu.platform.common.tenant.TenantContextHolder.setTenantId(TENANT);
  }

  @AfterEach
  void tearDown() {
    securityContext.close();
    com.guoyu.platform.common.tenant.TenantContextHolder.clear();
  }

  /** 构造样例：2 一级 / 2 二级 / 2 考评点；可注入权重与选考方式坏值 */
  private MockMultipartFile xlsx(String l1Weight, String selectMode, String quota) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet s = wb.createSheet("Sheet1");
      s.createRow(0).createCell(0).setCellValue("矿山生产集控员（二级/技师）操作技能考评要素细目表");
      s.createRow(1).createCell(0).setCellValue("职业： 矿山生产集控员  级别： 二级/技师");
      Row block = s.createRow(2);
      block.createCell(0).setCellValue("考评范围一级");
      block.createCell(4).setCellValue("考评范围二级");
      block.createCell(9).setCellValue("考评点");
      s.createRow(3).createCell(0).setCellValue("代码\n（重要程度比例）");
      String[][] rows = {
          {"A\n（2:0:0）", "作业环境确认", l1Weight, selectMode, "A\n（1:0:0）", "设备准备",
              "1.1.1 能对集控室监控设备进行联机启动", "0.4", "任选一项", "001", "联机启动集控室监控设备",
              "X", quota},
          {"B\n（2:0:0）", "生产监控管理", "0.5", "必考", "A\n（2:0:0）", "生产现场监控",
              "2.1.1 能对监控设备联机方案进行调整", "0.6", "任选一项", "001", "调整监控设备联机方案",
              "Y", ""},
      };
      for (int i = 0; i < rows.length; i++) {
        Row r = s.createRow(4 + i);
        for (int c = 0; c < rows[i].length; c++) {
          r.createCell(c).setCellValue(rows[i][c]);
        }
      }
      wb.write(out);
      return new MockMultipartFile("file", "样例.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
  }

  private String capturedReport() {
    ArgumentCaptor<SkillBlueprintEntity> cap = ArgumentCaptor.forClass(SkillBlueprintEntity.class);
    verify(blueprintMapper).insert(cap.capture());
    return cap.getValue().getCheckReport();
  }

  @Test
  void import_reportsWeightAndSelectModeErrors() throws Exception {
    when(blueprintMapper.selectOne(any())).thenReturn(null);
    SkillBlueprintVO vo = service.importBlueprint(xlsx("0.4", "随便写", ""), 22, 2);
    assertEquals(2, vo.pointCount());
    assertEquals(0, vo.weightCheck());
    String report = capturedReport();
    assertTrue(report.contains("≠ 100%"), "权重合计校验应可解释: " + report);
    assertTrue(report.contains("选考方式非法"), report);
    // 点数×5 = 10 < 二级下限 30 → 报告中给出预检提示（不拦截导入）
    assertTrue(report.contains("低于本等级题库总量下限"), report);
  }

  @Test
  void import_quotaNonFiveWarnsButDoesNotBlock() throws Exception {
    when(blueprintMapper.selectOne(any())).thenReturn(null);
    service.importBlueprint(xlsx("0.5", "必考", "3"), 22, 2);
    String report = capturedReport();
    assertTrue(report.contains("忽略该列值"), report);
  }

  @Test
  void import_assemblesFullCodeAlongParentChain() throws Exception {
    when(blueprintMapper.selectOne(any())).thenReturn(null);
    service.importBlueprint(xlsx("0.5", "必考", ""), 22, 2);
    ArgumentCaptor<SkillBlueprintNodeEntity> cap = ArgumentCaptor.forClass(SkillBlueprintNodeEntity.class);
    verify(nodeMapper, org.mockito.Mockito.times(6)).insert(cap.capture());
    List<String> fullCodes = cap.getAllValues().stream()
        .map(SkillBlueprintNodeEntity::getFullCode).toList();
    assertTrue(fullCodes.contains("A"), fullCodes.toString());
    assertTrue(fullCodes.contains("A-A"), fullCodes.toString());
    assertTrue(fullCodes.contains("A-A-001"), fullCodes.toString());
    assertTrue(fullCodes.contains("B-A-001"), fullCodes.toString());
  }

  @Test
  void import_blocksRebuildWhenPublishedDraftsExist() throws Exception {
    SkillBlueprintEntity old = new SkillBlueprintEntity();
    old.setId(55L);
    old.setTenantId(TENANT);
    when(blueprintMapper.selectOne(any())).thenReturn(old);
    when(draftMapper.selectCount(any())).thenReturn(1L); // 有已入库草稿

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.importBlueprint(xlsx("0.5", "必考", ""), 22, 2));
    assertTrue(ex.getMessage().contains("不允许重建"));
    verify(blueprintMapper, never()).insert(any(SkillBlueprintEntity.class));
  }

  @Test
  void import_rebuildCleansUnpublishedAndRecreates() throws Exception {
    SkillBlueprintEntity old = new SkillBlueprintEntity();
    old.setId(55L);
    old.setTenantId(TENANT);
    when(blueprintMapper.selectOne(any())).thenReturn(old);
    when(draftMapper.selectCount(any())).thenReturn(0L); // 无入库记录 → 允许重建
    SkillBlueprintNodeEntity oldNode = new SkillBlueprintNodeEntity();
    oldNode.setId(77L);
    when(nodeMapper.selectList(any())).thenReturn(List.of(oldNode));

    service.importBlueprint(xlsx("0.5", "必考", ""), 22, 2);

    verify(draftMapper).physicalDeleteUnpublished(TENANT, 55L);
    verify(blueprintMapper).deleteById(55L);
    verify(nodeMapper).deleteById(77L);
    verify(blueprintMapper).insert(any(SkillBlueprintEntity.class));
  }
}

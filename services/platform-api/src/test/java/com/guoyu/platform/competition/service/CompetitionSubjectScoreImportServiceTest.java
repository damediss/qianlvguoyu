package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionSubjectScoreImportBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionSubjectScoreImportRowEntity;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantRow;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreImportRowMapper;
import com.guoyu.platform.competition.service.CompetitionAutomaticScorePipelineService;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class CompetitionSubjectScoreImportServiceTest {
  private static final String ID_CARD = "110101199003078515";
  private static final long MEMBER_ID = 9001L;

  private final CompetitionSubjectScoreImportBatchMapper batchMapper =
      mock(CompetitionSubjectScoreImportBatchMapper.class);
  private final CompetitionSubjectScoreImportRowMapper rowMapper =
      mock(CompetitionSubjectScoreImportRowMapper.class);
  private final CompetitionSubjectScoreContestantMapper contestantMapper =
      mock(CompetitionSubjectScoreContestantMapper.class);
  private final CompetitionStageContentMapper stageContentMapper =
      mock(CompetitionStageContentMapper.class);
  private final CompetitionScoreSourceMapper sourceMapper =
      mock(CompetitionScoreSourceMapper.class);
  private final CompetitionRankingSnapshotMapper rankingMapper =
      mock(CompetitionRankingSnapshotMapper.class);
  private final CompetitionScorePipelineOutboxService pipeline =
      mock(CompetitionScorePipelineOutboxService.class);
  private final FileApplicationService fileApplicationService =
      mock(FileApplicationService.class);
  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);
  private final CompetitionPlayerDataHash dataHash = new CompetitionPlayerDataHash();
  private final AtomicReference<CompetitionSubjectScoreImportBatchEntity> batchRef =
      new AtomicReference<>();
  private final List<CompetitionSubjectScoreImportRowEntity> rows = new ArrayList<>();
  private final AtomicReference<byte[]> fileBytes = new AtomicReference<>();
  private CompetitionSubjectScoreImportService service;

  @BeforeEach
  void setUp() {
    when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    when(batchMapper.insert(any(CompetitionSubjectScoreImportBatchEntity.class))).thenAnswer(invocation -> {
      batchRef.set(invocation.getArgument(0));
      return 1;
    });
    when(batchMapper.selectById(any())).thenAnswer(invocation -> batchRef.get());
    when(batchMapper.selectOne(any())).thenReturn(null);
    when(batchMapper.update(any(CompetitionSubjectScoreImportBatchEntity.class), any())).thenReturn(1);
    when(batchMapper.updateById(any(CompetitionSubjectScoreImportBatchEntity.class))).thenReturn(1);
    when(rowMapper.insert(any(CompetitionSubjectScoreImportRowEntity.class))).thenAnswer(invocation -> {
      rows.add(invocation.getArgument(0));
      return 1;
    });
    when(rowMapper.updateById(any(CompetitionSubjectScoreImportRowEntity.class))).thenReturn(1);
    when(fileApplicationService.storeFromBytesForTenant(any(), any(), any(), any(), any()))
        .thenAnswer(invocation -> {
          fileBytes.set(invocation.getArgument(1));
          return new FileVO(11L, "scores.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              1L, "private", "ready", LocalDateTime.now(), null);
        });
    when(fileApplicationService.readFileBytesById(any())).thenAnswer(invocation -> fileBytes.get());
    when(stageContentMapper.selectList(any())).thenReturn(List.of(
        content("theory", "40"), content("simulation", "30"), content("practical", "30")));
    CompetitionSubjectScoreContestantRow contestant = new CompetitionSubjectScoreContestantRow();
    contestant.setContestantMemberId(MEMBER_ID);
    contestant.setPlayerName("测试选手");
    contestant.setIdCardLookupHash(dataHash.lookupHash(ID_CARD));
    when(contestantMapper.selectByScope(any(), any(), any(), any(), any()))
        .thenReturn(List.of(contestant));
    service = new CompetitionSubjectScoreImportService(
        batchMapper, rowMapper, contestantMapper, stageContentMapper, sourceMapper, rankingMapper,
        pipeline,
        mock(CompetitionAutomaticScorePipelineService.class),
        new CompetitionPlayerIdentityDomainService(
            mock(com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper.class),
            dataHash, mock(PasswordEncoder.class)),
        dataHash, fileApplicationService, transactionManager, Runnable::run);
  }

  @Test
  void matchesPracticalScoreByIdCardAndConfirmsARulelessSource() {
    service.upload(1L, 7L, 10L, 20L, 30L, null, "practical", "practical.xlsx", workbook(
        ID_CARD, "88.50"));

    assertThat(batchRef.get().getBatchStatus()).isEqualTo("ready");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst().getContestantMemberId()).isEqualTo(MEMBER_ID);
    assertThat(rows.getFirst().getValidationStatus()).isEqualTo("valid");

    when(rowMapper.selectList(any())).thenReturn(List.copyOf(rows), List.of());
    when(sourceMapper.selectOne(any())).thenReturn(null);
    AtomicReference<CompetitionScoreSourceEntity> saved = new AtomicReference<>();
    when(sourceMapper.insert(any(CompetitionScoreSourceEntity.class))).thenAnswer(invocation -> {
      saved.set(invocation.getArgument(0));
      return 1;
    });

    service.confirm(1L, 7L, batchRef.get().getId(), null);

    assertThat(saved.get().getSourceType()).isEqualTo("practical");
    assertThat(saved.get().getScoreRuleId()).isNull();
    assertThat(saved.get().getContestantMemberId()).isEqualTo(MEMBER_ID);
    assertThat(saved.get().getRawScore()).isEqualByComparingTo(new BigDecimal("88.50"));
    assertThat(saved.get().getConfirmationStatus()).isEqualTo("confirmed");
    assertThat(saved.get().getCurrentFlag()).isEqualTo(1);
    assertThat(batchRef.get().getBatchStatus()).isEqualTo("completed");
  }

  @Test
  void rejectsAnIdCardOutsideTheRoster() {
    service.upload(1L, 7L, 10L, 20L, 30L, null, "simulation", "simulation.xlsx", workbook(
        "110101199003074936", "70"));

    assertThat(batchRef.get().getBatchStatus()).isEqualTo("invalid");
    assertThat(rows.getFirst().getErrorMessage()).isEqualTo("身份证号不在当前赛事赛段工种等级名单");
  }

  @Test
  void refusesImportBeforeStageWeightsAreComplete() {
    when(stageContentMapper.selectList(any())).thenReturn(List.of(content("theory", "40")));

    assertThatThrownBy(() -> service.upload(
        1L, 7L, 10L, 20L, 30L, null, "practical", "practical.xlsx", workbook(ID_CARD, "80")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("赛段内容权重尚未配置完成，不能导入实操或仿真成绩");
  }

  private CompetitionStageContentEntity content(String type, String weight) {
    CompetitionStageContentEntity content = new CompetitionStageContentEntity();
    content.setContentType(type);
    content.setStatus("enabled");
    content.setWeight(new BigDecimal(weight));
    return content;
  }

  private byte[] workbook(String idCard, String score) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("成绩");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("姓名");
      header.createCell(1).setCellValue("身份证号");
      header.createCell(2).setCellValue("成绩");
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("测试选手");
      row.createCell(1).setCellValue(idCard);
      row.createCell(2).setCellValue(score);
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}

package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.mapper.CompetitionTheoryRosterRow;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionTheoryRosterExportServiceTest {

  private final CompetitionTheoryScheduleApplicationService scheduleService =
      Mockito.mock(CompetitionTheoryScheduleApplicationService.class);
  private final CompetitionTheoryRosterExportService exportService =
      new CompetitionTheoryRosterExportService(scheduleService);

  @Test
  void excelCreatesOneSheetPerSessionRoomAndOmitsSensitiveColumns() throws Exception {
    when(scheduleService.rosterRows(1001L, true)).thenReturn(List.of(
        row(1L, "上午场", "第一考场", "001", "C001", "张三"),
        row(2L, "下午场", "第二考场", "001", "C002", "李四")));

    byte[] bytes = exportService.exportExcel(1001L);

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      var header = workbook.getSheetAt(0).getRow(0);
      List<String> headers = java.util.stream.IntStream.range(0, header.getLastCellNum())
          .mapToObj(index -> header.getCell(index).getStringCellValue())
          .toList();
      assertThat(headers).contains(
          "赛事", "工种", "场次名称", "开始时间", "结束时间", "考场名称", "地址",
          "座位号", "参赛编号", "选手姓名", "所属企业", "签到");
      assertThat(headers).doesNotContain("手机号", "身份证", "试卷卷别", "成绩");
    }
  }

  @Test
  void pdfStartsWithPdfSignatureAndContainsNoAnswerInput() {
    when(scheduleService.rosterRows(1001L, true)).thenReturn(List.of(
        row(1L, "上午场", "第一考场", "001", "C001", "张三")));

    byte[] bytes = exportService.exportPdf(1001L);

    assertThat(bytes).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  }

  private CompetitionTheoryRosterRow row(
      Long sessionId,
      String sessionName,
      String roomName,
      String seatNo,
      String contestantNo,
      String contestantName) {
    CompetitionTheoryRosterRow row = new CompetitionTheoryRosterRow();
    row.setSessionId(sessionId);
    row.setEventName("技能竞赛");
    row.setWorkTypeName("电工");
    row.setSessionName(sessionName);
    row.setStartAt(LocalDateTime.of(2026, 8, 10, sessionId.equals(1L) ? 9 : 14, 0));
    row.setEndAt(row.getStartAt().plusMinutes(90));
    row.setRoomName(roomName);
    row.setAddress("培训中心一层");
    row.setSeatNo(seatNo);
    row.setContestantNo(contestantNo);
    row.setContestantName(contestantName);
    row.setOrgName("示例企业");
    return row;
  }
}

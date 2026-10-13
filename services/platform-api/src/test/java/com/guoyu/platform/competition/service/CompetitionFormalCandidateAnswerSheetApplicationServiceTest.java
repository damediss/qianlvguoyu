package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetFile;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateQuestionScoreVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateResultVO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompetitionFormalCandidateAnswerSheetApplicationServiceTest {

  private final CompetitionFormalExamSessionApplicationService sessionService =
      mock(CompetitionFormalExamSessionApplicationService.class);
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper =
      mock(CompetitionFormalExamQuestionSnapshotMapper.class);
  private final CompetitionFormalCandidateAnswerMapper answerMapper =
      mock(CompetitionFormalCandidateAnswerMapper.class);
  private CompetitionFormalCandidateAnswerSheetApplicationService service;

  @BeforeEach
  void setUp() {
    service = new CompetitionFormalCandidateAnswerSheetApplicationService(
        sessionService,
        questionSnapshotMapper,
        answerMapper,
        new FormalExamCandidateQuestionProjection(new ObjectMapper()),
        new CompetitionFormalCandidateAnswerSheetDocxWriter(),
        mock(CompetitionFormalCandidateExamMapper.class),
        mock(CompetitionRegistrationMapper.class));
  }

  @Test
  void exportRejectsUnscoredCandidate() {
    when(sessionService.getCandidate(11L)).thenReturn(candidate(11L, "李四", "QL010"));
    when(sessionService.getCandidateResult(11L)).thenReturn(result(11L, "pending", null));

    assertThatThrownBy(() -> service.exportAnswerSheet(11L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前选手成绩尚未完成，不能读取答题卡");
  }

  @Test
  void exportWritesOneDocxNamedByCandidate() throws Exception {
    stubScoredCandidate(21L, "张三", "QL001", "氧化铝浓度过低");

    CompetitionFormalCandidateAnswerSheetFile file = service.exportAnswerSheet(21L);

    assertThat(file.filename()).isEqualTo("张三.docx");
    assertThat(file.mediaType()).contains("wordprocessingml");
    assertThat(documentText(file.data())).contains("姓     名  张三", "一、单选题(2分)", "1.A");
  }

  @Test
  void exportManyKeepsOneDocxPerPersonAndDistinguishesSameNames() throws Exception {
    stubScoredCandidate(31L, "张三", "QL001", "甲");
    stubScoredCandidate(32L, "张三", "QL002", "乙");
    stubScoredCandidate(33L, "李四", "QL003", "丙");

    CompetitionFormalCandidateAnswerSheetFile file = service.exportAnswerSheets(List.of(31L, 32L, 31L, 33L));

    assertThat(file.filename()).isEqualTo("理论考试答题卡.zip");
    assertThat(zipNames(file.data())).containsExactly("张三.docx", "张三-QL002.docx", "李四.docx");
  }

  private void stubScoredCandidate(Long candidateExamId, String name, String candidateNo, String selected) {
    when(sessionService.getCandidate(candidateExamId))
        .thenReturn(candidate(candidateExamId, name, candidateNo));
    when(sessionService.getCandidateResult(candidateExamId)).thenReturn(result(
        candidateExamId,
        "scored",
        List.of(new CompetitionFormalCandidateQuestionScoreVO(
            "101", 1, "single", 1, true, true, new BigDecimal("2"), new BigDecimal("2")))));
    CompetitionFormalExamQuestionSnapshotEntity question = new CompetitionFormalExamQuestionSnapshotEntity();
    question.setId(101L);
    question.setExamSessionId(20L);
    question.setPaperSnapshotId(10L);
    question.setQuestionOrder(1);
    question.setQuestionType("single");
    question.setStem("题干" + selected);
    question.setOptionsJson("[{\"key\":\"A\",\"content\":\"" + selected + "\"},{\"key\":\"B\",\"content\":\"其他\"}]");
    when(questionSnapshotMapper.selectByPaperSnapshotId(10L)).thenReturn(List.of(question));
    CompetitionFormalCandidateAnswerEntity answer = new CompetitionFormalCandidateAnswerEntity();
    answer.setQuestionSnapshotId(101L);
    answer.setAnswerJson("{\"selectedOptionIds\":[\"101:A\"]}");
    when(answerMapper.selectByCandidateExamId(candidateExamId)).thenReturn(List.of(answer));
  }

  private static CompetitionFormalCandidateExamVO candidate(
      Long candidateExamId, String name, String candidateNo) {
    return new CompetitionFormalCandidateExamVO(
        String.valueOf(candidateExamId),
        "20",
        null,
        null,
        candidateNo,
        name,
        "10",
        null,
        null,
        null,
        "scored",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static CompetitionFormalCandidateResultVO result(
      Long candidateExamId,
      String status,
      List<CompetitionFormalCandidateQuestionScoreVO> scores) {
    return new CompetitionFormalCandidateResultVO(
        "1",
        "20",
        String.valueOf(candidateExamId),
        status,
        new BigDecimal("2"),
        new BigDecimal("2"),
        1,
        1,
        1,
        0,
        0,
        1,
        null,
        scores,
        1,
        true,
        null);
  }

  private static String documentText(byte[] bytes) throws Exception {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      StringBuilder text = new StringBuilder();
      XmlCursor cursor = document.getDocument().newCursor();
      cursor.selectPath(
          "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:t");
      while (cursor.toNextSelection()) {
        text.append(cursor.getTextValue());
      }
      cursor.dispose();
      return text.toString();
    }
  }

  private static List<String> zipNames(byte[] bytes) throws Exception {
    List<String> names = new java.util.ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        names.add(entry.getName());
        zip.closeEntry();
      }
    }
    return names;
  }
}

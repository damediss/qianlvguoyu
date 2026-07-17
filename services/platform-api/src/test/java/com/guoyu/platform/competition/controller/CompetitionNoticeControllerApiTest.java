package com.guoyu.platform.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.service.CompetitionNoticeApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionNoticeVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionNoticeControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionNoticeApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionNoticeApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionNoticeController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createReturnsDraftNotice() throws Exception {
    CompetitionNoticeVO notice = notice(9101L, "draft");
    when(service.create(any(Long.class), any(CompetitionNoticeCreateReq.class))).thenReturn(notice);

    mockMvc.perform(post("/competition/events/1001/notices")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"Title","content":"Content","version":1}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.status").value("draft"));

    verify(service).create(any(Long.class), any(CompetitionNoticeCreateReq.class));
  }

  @Test
  void listReturnsPageResponse() throws Exception {
    when(service.page(any(Long.class), any(CompetitionNoticeQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(notice(9101L, "published")), 1, 10, 1));

    mockMvc.perform(get("/competition/events/1001/notices")
            .param("status", "published")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].status").value("published"));

    verify(service).page(any(Long.class), any(CompetitionNoticeQueryReq.class));
  }

  @Test
  void detailReturnsNotice() throws Exception {
    when(service.getById(9101L)).thenReturn(notice(9101L, "draft"));

    mockMvc.perform(get("/competition/notices/9101"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9101))
        .andExpect(jsonPath("$.data.title").value("Title"));

    verify(service).getById(9101L);
  }

  @Test
  void publishReturnsStatusTransition() throws Exception {
    CompetitionNoticePublishResultVO result = new CompetitionNoticePublishResultVO();
    result.setNoticeId(9101L);
    result.setEventId(1001L);
    result.setBeforeStatus("draft");
    result.setAfterStatus("published");
    when(service.publish(9101L)).thenReturn(result);

    mockMvc.perform(post("/competition/notices/9101/publish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.noticeId").value(9101))
        .andExpect(jsonPath("$.data.beforeStatus").value("draft"))
        .andExpect(jsonPath("$.data.afterStatus").value("published"));

    verify(service).publish(9101L);
  }

  @Test
  void publishedNoticeUpdateReturnsConflict() throws Exception {
    when(service.update(Mockito.eq(9101L), any(CompetitionNoticeUpdateReq.class)))
        .thenThrow(new BusinessException(ErrorCode.CONFLICT, "仅草稿通知允许修改"));

    mockMvc.perform(put("/competition/notices/9101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"New\",\"content\":\"New content\",\"version\":2}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"));
  }

  @Test
  void invalidCreateRequestIsRejectedBeforeService() throws Exception {
    mockMvc.perform(post("/competition/events/1001/notices")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"\",\"content\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }

  private CompetitionNoticeVO notice(Long id, String status) {
    CompetitionNoticeVO notice = new CompetitionNoticeVO();
    notice.setId(id);
    notice.setEventId(1001L);
    notice.setTitle("Title");
    notice.setContent("Content");
    notice.setVersion(1);
    notice.setStatus(status);
    return notice;
  }
}

package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.service.CompetitionContestantWorkTypeApplicationService;
import com.guoyu.platform.competition.service.CompetitionEventWorkTypeApplicationService;
import com.guoyu.platform.competition.vo.CompetitionContestantWorkTypeReconciliationVO;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionWorkTypeControllerApiTest {

  private CompetitionEventWorkTypeApplicationService eventWorkTypeService;
  private CompetitionContestantWorkTypeApplicationService contestantWorkTypeService;
  private MockMvc eventWorkTypeMvc;
  private MockMvc contestantWorkTypeMvc;

  @BeforeEach
  void setUp() {
    eventWorkTypeService = Mockito.mock(CompetitionEventWorkTypeApplicationService.class);
    contestantWorkTypeService =
        Mockito.mock(CompetitionContestantWorkTypeApplicationService.class);
    eventWorkTypeMvc = MockMvcBuilders
        .standaloneSetup(new CompetitionEventWorkTypeController(eventWorkTypeService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    contestantWorkTypeMvc = MockMvcBuilders
        .standaloneSetup(new CompetitionContestantWorkTypeController(contestantWorkTypeService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void eventWorkTypeEndpointsExposeListCreateAndUpdate() throws Exception {
    CompetitionEventWorkTypeVO workType = workType();
    when(eventWorkTypeService.list(200L)).thenReturn(List.of(workType));
    when(eventWorkTypeService.create(
        Mockito.eq(200L), any(CompetitionEventWorkTypeCreateReq.class)))
        .thenReturn(workType);
    when(eventWorkTypeService.update(
        Mockito.eq(200L), Mockito.eq(10L), any(CompetitionEventWorkTypeUpdateReq.class)))
        .thenReturn(workType);

    eventWorkTypeMvc.perform(get("/competition/events/200/work-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(10))
        .andExpect(jsonPath("$.data[0].displayName").value("焊工"));

    String body = """
        {"workTypeId":100,"displayName":"焊工","status":"active","sortOrder":1}
        """;
    eventWorkTypeMvc.perform(post("/competition/events/200/work-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.workTypeId").value(100));
    eventWorkTypeMvc.perform(put("/competition/events/200/work-types/10")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("active"));

    verify(eventWorkTypeService).create(
        Mockito.eq(200L), any(CompetitionEventWorkTypeCreateReq.class));
    verify(eventWorkTypeService).update(
        Mockito.eq(200L), Mockito.eq(10L), any(CompetitionEventWorkTypeUpdateReq.class));
  }

  @Test
  void missingSourceWorkTypeIsRejectedBeforeServiceCall() throws Exception {
    eventWorkTypeMvc.perform(post("/competition/events/200/work-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"焊工\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));
    verifyNoInteractions(eventWorkTypeService);
  }

  @Test
  void reconciliationAndManualConfirmationExposeCandidateAndStableAssignment()
      throws Exception {
    CompetitionContestantWorkTypeReconciliationVO unresolved =
        new CompetitionContestantWorkTypeReconciliationVO();
    unresolved.setProfileId(31L);
    unresolved.setContestantMemberId(21L);
    unresolved.setRosterWorkType("焊工");
    unresolved.setMatchStatus("matched");
    unresolved.setConfirmed(false);
    when(contestantWorkTypeService.reconcile(200L)).thenReturn(List.of(unresolved));

    CompetitionContestantWorkTypeReconciliationVO confirmed =
        new CompetitionContestantWorkTypeReconciliationVO();
    confirmed.setProfileId(31L);
    confirmed.setContestantMemberId(21L);
    confirmed.setEventWorkTypeId(10L);
    confirmed.setMatchStatus("matched");
    confirmed.setConfirmed(true);
    when(contestantWorkTypeService.confirm(
        Mockito.eq(200L), Mockito.eq(21L),
        any(CompetitionContestantWorkTypeConfirmReq.class)))
        .thenReturn(confirmed);

    contestantWorkTypeMvc.perform(
            get("/competition/events/200/work-type-reconciliation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].matchStatus").value("matched"))
        .andExpect(jsonPath("$.data[0].confirmed").value(false));
    contestantWorkTypeMvc.perform(
            put("/competition/events/200/contestants/21/work-type")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventWorkTypeId\":10,\"note\":\"人工确认\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eventWorkTypeId").value(10))
        .andExpect(jsonPath("$.data.confirmed").value(true));
  }

  @Test
  void controllerPermissionsSeparateReadAndManage() {
    assertThat(method(CompetitionEventWorkTypeController.class, "list")
        .getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-work-type:view')");
    assertThat(method(CompetitionEventWorkTypeController.class, "create")
        .getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-work-type:manage')");
    assertThat(method(CompetitionEventWorkTypeController.class, "update")
        .getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-work-type:manage')");
    assertThat(method(CompetitionContestantWorkTypeController.class, "reconcile")
        .getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-work-type:view')");
    assertThat(method(CompetitionContestantWorkTypeController.class, "confirm")
        .getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-work-type:manage')");
  }

  private CompetitionEventWorkTypeVO workType() {
    CompetitionEventWorkTypeVO workType = new CompetitionEventWorkTypeVO();
    workType.setId(10L);
    workType.setEventId(200L);
    workType.setWorkTypeId(100L);
    workType.setWorkTypeName("焊工");
    workType.setDisplayName("焊工");
    workType.setStatus("active");
    workType.setSortOrder(1);
    return workType;
  }

  private Method method(Class<?> type, String name) {
    for (Method method : type.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}

package com.guoyu.platform.competition.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.competition.messaging.vo.CompetitionPlayerMessageRow;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.service.CompetitionNoticeAssetApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPlayerMessageApplicationServiceTest {

  private final CompetitionPlayerMessageMapper mapper =
      Mockito.mock(CompetitionPlayerMessageMapper.class);
  private final CompetitionNoticeAssetApplicationService assetService =
      Mockito.mock(CompetitionNoticeAssetApplicationService.class);
  private final CompetitionPlayerMessageApplicationService service =
      new CompetitionPlayerMessageApplicationService(mapper, assetService);

  @BeforeEach
  void authenticatePlayer() {
    CompetitionPlayerPrincipal principal = new CompetitionPlayerPrincipal(9001L, 1L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsOnlyMapperFilteredAccessibleMessages() {
    CompetitionPlayerMessageRow row = new CompetitionPlayerMessageRow();
    row.setMessageId(8001L);
    row.setEventId(1001L);
    row.setNoticeId(7001L);
    row.setType("notice_published");
    row.setTitle("比赛安排");
    row.setContent("请按时参加");
    row.setReceivedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
    Page<CompetitionPlayerMessageRow> mapperPage = new Page<>(1, 5, 1);
    mapperPage.setRecords(List.of(row));
    when(mapper.selectPageAccessible(any(Page.class), eq(1L), eq(9001L)))
        .thenReturn(mapperPage);

    PageQuery query = new PageQuery();
    query.setPage(1);
    query.setPageSize(5);
    var result = service.page(query);

    assertThat(result.total()).isEqualTo(1);
    assertThat(result.items()).singleElement().satisfies(message -> {
      assertThat(message.getMessageId()).isEqualTo(8001L);
      assertThat(message.getTitle()).isEqualTo("比赛安排");
      assertThat(message.getContent()).isEqualTo("请按时参加");
    });
  }

  @Test
  void mapsContentTypeAndControlledAssetsIntoPlayerMessage() {
    CompetitionPlayerMessageRow row = new CompetitionPlayerMessageRow();
    row.setMessageId(8001L);
    row.setEventId(1001L);
    row.setNoticeId(7001L);
    row.setType("notice_published");
    row.setTitle("Rules");
    row.setContentType("pdf");
    row.setContent("Description");
    Page<CompetitionPlayerMessageRow> mapperPage = new Page<>(1, 5, 1);
    mapperPage.setRecords(List.of(row));
    when(mapper.selectPageAccessible(any(Page.class), eq(1L), eq(9001L)))
        .thenReturn(mapperPage);
    CompetitionNoticeAssetVO asset = new CompetitionNoticeAssetVO();
    asset.setAssetId(7002L);
    asset.setAssetType("pdf");
    asset.setSourceUri("notice-asset://7002");
    when(assetService.listByNoticeIds(1L, List.of(7001L)))
        .thenReturn(Map.of(7001L, List.of(asset)));
    PageQuery query = new PageQuery();
    query.setPage(1);
    query.setPageSize(5);

    var message = service.page(query).items().getFirst();

    assertThat(message.getContentType()).isEqualTo("pdf");
    assertThat(message.getAssets()).singleElement()
        .extracting(CompetitionNoticeAssetVO::getAssetId)
        .isEqualTo(7002L);
    assertThat(message.getAssets().getFirst().getSourceUri())
        .isEqualTo("/api/competition/player/messages/8001/assets/7002");
    verify(assetService).listByNoticeIds(1L, List.of(7001L));
  }

  @Test
  void delegatesControlledAssetDownloadWithMessageAndAssetIds() {
    ResponseEntity<org.springframework.core.io.Resource> response =
        ResponseEntity.ok(new ByteArrayResource("pdf".getBytes()));
    when(assetService.playerDownload(8001L, 7002L)).thenReturn(response);

    assertThat(service.downloadAsset(8001L, 7002L)).isSameAs(response);

    verify(assetService).playerDownload(8001L, 7002L);
  }

  @Test
  void readsUnreadCountInCurrentPlayerScope() {
    when(mapper.countUnreadAccessible(1L, 9001L)).thenReturn(3L);

    assertThat(service.unreadCount()).isEqualTo(3L);
  }

  @Test
  void rejectsMarkReadWhenMessageIsOutsideCurrentPlayerScope() {
    when(mapper.markReadIfAccessible(eq(1L), eq(9001L), eq(8001L), any()))
        .thenReturn(0);

    assertThatThrownBy(() -> service.markRead(8001L))
        .hasMessageContaining("消息不存在");
  }

  @Test
  void marksAccessibleMessageRead() {
    when(mapper.markReadIfAccessible(eq(1L), eq(9001L), eq(8001L), any()))
        .thenReturn(1);

    service.markRead(8001L);

    verify(mapper).markReadIfAccessible(eq(1L), eq(9001L), eq(8001L), any());
  }
}

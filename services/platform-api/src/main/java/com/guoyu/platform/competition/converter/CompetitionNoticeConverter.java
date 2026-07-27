package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.enums.CompetitionNoticeContentType;
import com.guoyu.platform.competition.vo.CompetitionNoticeVO;

public final class CompetitionNoticeConverter {

  private CompetitionNoticeConverter() {
  }

  public static CompetitionNoticeEntity toEntity(CompetitionNoticeCreateReq source) {
    if (source == null) {
      return null;
    }
    CompetitionNoticeEntity target = new CompetitionNoticeEntity();
    target.setTitle(source.title().trim());
    target.setContent(source.content() == null ? "" : source.content());
    target.setContentType(CompetitionNoticeContentType.normalize(source.contentType()));
    target.setVersion(source.version());
    return target;
  }

  public static void applyUpdate(CompetitionNoticeEntity target, CompetitionNoticeUpdateReq source) {
    if (target == null || source == null) {
      return;
    }
    target.setTitle(source.title().trim());
    target.setContent(source.content() == null ? "" : source.content());
    target.setContentType(CompetitionNoticeContentType.normalize(source.contentType()));
    if (source.version() != null) {
      target.setVersion(source.version());
    }
  }

  public static CompetitionNoticeVO toVO(CompetitionNoticeEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionNoticeVO target = new CompetitionNoticeVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setTitle(source.getTitle());
    target.setContentType(source.getContentType());
    target.setContent(source.getContent());
    target.setVersion(source.getVersion());
    target.setStatus(source.getStatus());
    target.setPublishedBy(source.getPublishedBy());
    target.setPublishedAt(source.getPublishedAt());
    target.setCreatedBy(source.getCreatedBy());
    target.setCreatedAt(source.getCreatedAt());
    target.setUpdatedAt(source.getUpdatedAt());
    return target;
  }
}

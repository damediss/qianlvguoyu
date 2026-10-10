package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionTaskTemplateCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.competition.vo.CompetitionTaskTemplateVO;

public final class CompetitionTaskTemplateConverter {

  private CompetitionTaskTemplateConverter() {
  }

  public static CompetitionTaskTemplateEntity toEntity(CompetitionTaskTemplateCreateReq source) {
    if (source == null) {
      return null;
    }
    CompetitionTaskTemplateEntity target = new CompetitionTaskTemplateEntity();
    target.setTaskType(source.taskType());
    target.setTitle(source.title());
    target.setDescription(source.description());
    target.setCompletionRule(source.completionRule());
    target.setRequiresReview(source.requiresReview());
    target.setDeadlineRule(source.deadlineRule());
    target.setAllowedFileTypes(source.allowedFileTypes());
    target.setSortOrder(source.sortOrder());
    return target;
  }

  public static void applyUpdate(
      CompetitionTaskTemplateEntity target, CompetitionTaskTemplateUpdateReq source) {
    if (target == null || source == null) {
      return;
    }
    target.setTaskType(source.taskType());
    target.setTitle(source.title());
    target.setDescription(source.description());
    target.setCompletionRule(source.completionRule());
    if (source.requiresReview() != null) {
      target.setRequiresReview(source.requiresReview());
    }
    target.setDeadlineRule(source.deadlineRule());
    target.setAllowedFileTypes(source.allowedFileTypes());
    if (source.sortOrder() != null) {
      target.setSortOrder(source.sortOrder());
    }
  }

  public static CompetitionTaskTemplateVO toVO(CompetitionTaskTemplateEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionTaskTemplateVO target = new CompetitionTaskTemplateVO();
    target.setId(source.getId());
    target.setStageId(source.getStageId());
    target.setTaskType(source.getTaskType());
    target.setTitle(source.getTitle());
    target.setDescription(source.getDescription());
    target.setCompletionRule(source.getCompletionRule());
    target.setRequiresReview(source.getRequiresReview());
    target.setDeadlineRule(source.getDeadlineRule());
    target.setAllowedFileTypes(source.getAllowedFileTypes());
    target.setSortOrder(source.getSortOrder());
    target.setStatus(source.getStatus());
    return target;
  }
}

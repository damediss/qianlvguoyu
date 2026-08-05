package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Parses the legacy levels_no text without writing or depending on exam-domain state.
 * Ambiguous text is intentionally omitted instead of guessing a level.
 */
@Component
@Slf4j
public class ExamWorkTypeLevelSourceAdapter implements CompetitionEventWorkTypeLevelSourceAdapter {

  private static final Pattern DIGIT = Pattern.compile("[1-5]");
  private static final String[] NAMES = {"", "一级/高级技师", "二级/技师", "三级/高级工", "四级/中级工", "五级/初级工"};

  private final ExamWorkTypeMapper mapper;

  public ExamWorkTypeLevelSourceAdapter(ExamWorkTypeMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<SourceLevel> listAvailableLevels(Long tenantId, Long workTypeId) {
    if (tenantId == null || workTypeId == null) {
      return new ArrayList<>();
    }
    final int sourceId;
    final int sourceTenantId;
    try {
      sourceId = Math.toIntExact(workTypeId);
      sourceTenantId = Math.toIntExact(tenantId);
    } catch (ArithmeticException ex) {
      log.warn("Exam work type level source id is outside legacy integer range, tenantId={}, workTypeId={}",
          tenantId, workTypeId);
      return new ArrayList<>();
    }
    ExamWorkTypeEntity source = mapper.selectOne(new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getId, sourceId)
        .eq(ExamWorkTypeEntity::getTenantId, sourceTenantId)
        .eq(ExamWorkTypeEntity::getStatus, 1)
        .eq(ExamWorkTypeEntity::getDeleted, 0));
    if (source == null || source.getLevelsNo() == null || source.getLevelsNo().isBlank()) {
      return new ArrayList<>();
    }
    List<SourceLevel> result = new ArrayList<>();
    Matcher matcher = DIGIT.matcher(source.getLevelsNo());
    while (matcher.find()) {
      String code = matcher.group();
      if (result.stream().noneMatch(level -> level.levelCode().equals(code))) {
        int level = Integer.parseInt(code);
        result.add(new SourceLevel(code, NAMES[level], code, source.getLevelsNo()));
      }
    }
    result.sort(Comparator.comparing(SourceLevel::levelCode));
    if (result.isEmpty()) {
      log.warn("Exam work type level source text could not be reconciled, tenantId={}, workTypeId={}",
          tenantId, workTypeId);
    }
    return result;
  }
}

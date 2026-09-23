package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionAssessmentContentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 竞赛导入多值 INSERT。每个分片一条 SQL，避免循环 Mapper.insert 打远程库。
 */
public class CompetitionQuestionImportBatchWriter {

  private static final String QUESTION_SQL = "INSERT INTO competition_question ("
      + "id, tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, question_type, "
      + "sort_order, stem, score, reference_answer, fill_answer, options_json, difficulty, status, source_batch_id, "
      + "created_by, updated_by, created_at, updated_at, deleted) VALUES ";
  private static final String BINDING_SQL = "INSERT INTO competition_question_blueprint_binding ("
      + "id, tenant_id, question_id, blueprint_id, blueprint_node_id, match_mode, source_batch_id, "
      + "created_by, updated_by, created_at, updated_at, deleted) VALUES ";
  private static final String DETAIL_SQL = "INSERT INTO competition_practical_question_detail ("
      + "id, tenant_id, question_id, duration_minutes, score_difference, total_score, scoring_method, "
      + "assessment_requirements_html, exam_room_prepare_html, candidate_prepare_html, negative_items_html, "
      + "question_materials_html, operation_content, student_prepare, equipment_prepare, "
      + "created_by, updated_by, created_at, updated_at, deleted) VALUES ";
  private static final String CONTENT_SQL = "INSERT INTO competition_practical_question_assessment_content ("
      + "id, tenant_id, question_id, sort_order, content_html, "
      + "created_by, updated_by, created_at, updated_at, deleted) VALUES ";
  private static final String SCORE_ITEM_SQL = "INSERT INTO competition_practical_question_score_item ("
      + "id, tenant_id, question_id, content_id, element_order, item_no, item_name, element_html, "
      + "allocation_enabled, criterion, standard_html, max_score, max_deduction, deduction_step, status, "
      + "created_by, updated_by, created_at, updated_at, deleted) VALUES ";

  public static final int QUESTION_COLUMNS = columnCount(QUESTION_SQL);
  public static final int BINDING_COLUMNS = columnCount(BINDING_SQL);
  public static final int DETAIL_COLUMNS = columnCount(DETAIL_SQL);
  public static final int CONTENT_COLUMNS = columnCount(CONTENT_SQL);
  public static final int SCORE_ITEM_COLUMNS = columnCount(SCORE_ITEM_SQL);

  private final SqlUpdate sqlUpdate;
  private final int batchSize;

  @FunctionalInterface
  public interface SqlUpdate {
    int update(String sql, Object[] args);
  }

  public record FlushStats(int batchCount, int sqlCount, List<Long> batchWriteMs, long writeMs) {
    static FlushStats empty() {
      return new FlushStats(0, 0, List.of(), 0L);
    }

    FlushStats plus(FlushStats other) {
      List<Long> times = new ArrayList<>(batchWriteMs);
      times.addAll(other.batchWriteMs);
      return new FlushStats(batchCount + other.batchCount, sqlCount + other.sqlCount, List.copyOf(times),
          writeMs + other.writeMs);
    }
  }

  public CompetitionQuestionImportBatchWriter(SqlUpdate sqlUpdate, int batchSize) {
    this.sqlUpdate = sqlUpdate;
    this.batchSize = batchSize < 1 ? 200 : batchSize;
  }

  public int batchSize() {
    return batchSize;
  }

  public FlushStats insertQuestions(List<CompetitionQuestionEntity> rows) {
    return insert(QUESTION_SQL, QUESTION_COLUMNS, rows, this::questionArgs, "竞赛试题保存失败");
  }

  public FlushStats insertBindings(List<CompetitionQuestionBlueprintBindingEntity> rows) {
    return insert(BINDING_SQL, BINDING_COLUMNS, rows, this::bindingArgs, "题目考评点绑定保存失败");
  }

  public FlushStats insertDetails(List<CompetitionPracticalQuestionDetailEntity> rows) {
    return insert(DETAIL_SQL, DETAIL_COLUMNS, rows, this::detailArgs, "实操题详情保存失败");
  }

  public FlushStats insertContents(List<CompetitionPracticalQuestionAssessmentContentEntity> rows) {
    return insert(CONTENT_SQL, CONTENT_COLUMNS, rows, this::contentArgs, "实操考核内容保存失败");
  }

  public FlushStats insertScoreItems(List<CompetitionPracticalQuestionScoreItemEntity> rows) {
    return insert(SCORE_ITEM_SQL, SCORE_ITEM_COLUMNS, rows, this::scoreItemArgs, "实操评分项保存失败");
  }

  static int columnCount(String insertPrefix) {
    int start = insertPrefix.indexOf('(');
    int end = insertPrefix.indexOf(')', start);
    if (start < 0 || end <= start) {
      throw new IllegalArgumentException("INSERT 语句缺少列清单");
    }
    int count = 1;
    for (int i = start + 1; i < end; i++) {
      if (insertPrefix.charAt(i) == ',') count++;
    }
    return count;
  }

  static String valuesClause(int rows, int columns) {
    String one = "(" + String.join(",", Collections.nCopies(columns, "?")) + ")";
    return String.join(",", Collections.nCopies(rows, one));
  }

  private <T> FlushStats insert(
      String prefix, int columns, List<T> rows, Function<T, Object[]> toArgs, String errorMessage) {
    if (rows == null || rows.isEmpty()) return FlushStats.empty();
    List<Long> times = new ArrayList<>();
    int batches = 0;
    long started = System.nanoTime();
    for (int from = 0; from < rows.size(); from += batchSize) {
      int to = Math.min(from + batchSize, rows.size());
      int count = to - from;
      Object[] args = new Object[count * columns];
      int offset = 0;
      for (int i = from; i < to; i++) {
        Object[] row = toArgs.apply(rows.get(i));
        if (row.length != columns) {
          throw new BusinessException(ErrorCode.INTERNAL_ERROR,
              errorMessage + "（参数列数 " + row.length + " != SQL 列数 " + columns + "）");
        }
        System.arraycopy(row, 0, args, offset, columns);
        offset += columns;
      }
      String sql = prefix + valuesClause(count, columns);
      long batchStarted = System.nanoTime();
      int updated = sqlUpdate.update(sql, args);
      times.add(elapsedMs(batchStarted));
      if (updated >= 0 && updated != count) {
        throw new BusinessException(ErrorCode.CONFLICT, errorMessage);
      }
      batches++;
    }
    return new FlushStats(batches, batches, List.copyOf(times), elapsedMs(started));
  }

  private Object[] questionArgs(CompetitionQuestionEntity row) {
    return new Object[] {
        row.getId(), row.getTenantId(), row.getEventId(), row.getEventWorkTypeId(), row.getEventWorkTypeLevelId(),
        row.getContentType(), row.getQuestionType(), row.getSortOrder(), row.getStem(), row.getScore(),
        row.getReferenceAnswer(), row.getFillAnswer(), row.getOptionsJson(), row.getDifficulty(), row.getStatus(),
        row.getSourceBatchId(), row.getCreatedBy(), row.getUpdatedBy(), timestamp(row.getCreatedAt()),
        timestamp(row.getUpdatedAt()), nz(row.getDeleted())
    };
  }

  private Object[] bindingArgs(CompetitionQuestionBlueprintBindingEntity row) {
    return new Object[] {
        row.getId(), row.getTenantId(), row.getQuestionId(), row.getBlueprintId(), row.getBlueprintNodeId(),
        row.getMatchMode(), row.getSourceBatchId(), row.getCreatedBy(), row.getUpdatedBy(),
        timestamp(row.getCreatedAt()), timestamp(row.getUpdatedAt()), nz(row.getDeleted())
    };
  }

  private Object[] detailArgs(CompetitionPracticalQuestionDetailEntity row) {
    return new Object[] {
        row.getId(), row.getTenantId(), row.getQuestionId(), row.getDurationMinutes(), row.getScoreDifference(),
        row.getTotalScore(), row.getScoringMethod(), row.getAssessmentRequirementsHtml(), row.getExamRoomPrepareHtml(),
        row.getCandidatePrepareHtml(), row.getNegativeItemsHtml(), row.getQuestionMaterialsHtml(),
        row.getOperationContent(), row.getStudentPrepare(), row.getEquipmentPrepare(),
        row.getCreatedBy(), row.getUpdatedBy(), timestamp(row.getCreatedAt()), timestamp(row.getUpdatedAt()),
        nz(row.getDeleted())
    };
  }

  private Object[] contentArgs(CompetitionPracticalQuestionAssessmentContentEntity row) {
    return new Object[] {
        row.getId(), row.getTenantId(), row.getQuestionId(), row.getSortOrder(), row.getContentHtml(),
        row.getCreatedBy(), row.getUpdatedBy(), timestamp(row.getCreatedAt()), timestamp(row.getUpdatedAt()),
        nz(row.getDeleted())
    };
  }

  private Object[] scoreItemArgs(CompetitionPracticalQuestionScoreItemEntity row) {
    return new Object[] {
        row.getId(), row.getTenantId(), row.getQuestionId(), row.getContentId(), row.getElementOrder(),
        row.getItemNo(), row.getItemName(), row.getElementHtml(), row.getAllocationEnabled(), row.getCriterion(),
        row.getStandardHtml(), row.getMaxScore(), row.getMaxDeduction(), row.getDeductionStep(), row.getStatus(),
        row.getCreatedBy(), row.getUpdatedBy(), timestamp(row.getCreatedAt()), timestamp(row.getUpdatedAt()),
        nz(row.getDeleted())
    };
  }

  private static Timestamp timestamp(LocalDateTime value) {
    return Timestamp.valueOf(value == null ? LocalDateTime.now() : value);
  }

  private static int nz(Integer value) {
    return value == null ? 0 : value;
  }

  private static long elapsedMs(long startedNanos) {
    return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
  }
}

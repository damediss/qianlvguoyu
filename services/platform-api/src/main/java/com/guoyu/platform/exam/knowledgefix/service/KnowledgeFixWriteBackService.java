package com.guoyu.platform.exam.knowledgefix.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.knowledgefix.dto.ApplyResultVO;
import com.guoyu.platform.exam.knowledgefix.dto.FixItemVO;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 知识修正写回与清单导出（方案 §6.4 / Phase 4）。
 * 写回 = 备份表 + 仅 apply 行逐行 PK 更新 + course 校验 + 可整体回滚。
 */
@Service
@Slf4j
public class KnowledgeFixWriteBackService {

  private static final String[] EXPORT_HEADERS = {
      "detail_id", "old_knowledge_id", "new_knowledge_id", "course_id",
      "confidence", "basis", "reason", "status"
  };

  private final ExamDetailKnowledgeFixMapper fixMapper;
  private final ExamExamDetailFixMapper queryMapper;

  public KnowledgeFixWriteBackService(ExamDetailKnowledgeFixMapper fixMapper,
      ExamExamDetailFixMapper queryMapper) {
    this.fixMapper = fixMapper;
    this.queryMapper = queryMapper;
  }

  /** 知识修正写回（方案 §6.4）：备份 → 预校验 → 仅 apply 行逐行更新 → 标记 applied */
  public ApplyResultVO apply(Long tenantId, Long userId) {
    List<ExamDetailKnowledgeFixEntity> applyRows = fixMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamDetailKnowledgeFixEntity>()
            .eq(ExamDetailKnowledgeFixEntity::getStatus, ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    if (applyRows.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "没有待写回的改挂行");
    }
    // 预校验：新节点必须存在且课程一致（课程可信假设不破坏）
    var nodeMap = new java.util.HashMap<Integer, com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow>();
    for (com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow n : queryMapper.selectAllNodes()) {
      nodeMap.put(n.getId(), n);
    }
    for (ExamDetailKnowledgeFixEntity row : applyRows) {
      var node = nodeMap.get(row.getNewKnowledgeId());
      if (node == null || !java.util.Objects.equals(node.getCourseId(), row.getCourseId())) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "修正行 " + row.getDetailId() + " 的新节点越界（course 不一致），已阻止写回");
      }
    }
    // 备份（整行；幂等）。FIX-8：写回按 apply 行备份（清单状态域无 approved，
    // 此前的实现写死 'approved' 导致备份表为空、回滚失效）
    String backupTable = "exam_exam_detail_bak_kfix_"
        + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    queryMapper.createBackupTable(backupTable);
    queryMapper.backupRows(backupTable, "exam_detail_knowledge_fix", "apply");
    // 逐行更新（PK 更新，应用行已按课程树封闭校验）
    int updated = 0;
    for (ExamDetailKnowledgeFixEntity row : applyRows) {
      updated += queryMapper.updateKnowledgeId(row.getDetailId(), row.getNewKnowledgeId());
      if (updated % 1000 == 0 && updated > 0) {
        log.info("知识修正写回进度：{}/{}", updated, applyRows.size());
      }
    }
    fixMapper.markAllApplied();
    log.info("知识修正写回完成：backupTable={}, updated={}", backupTable, updated);
    return new ApplyResultVO(backupTable, applyRows.size(), updated);
  }

  /** 知识修正回滚（按备份表还原 knowledge_id） */
  public int rollback(String backupTable) {
    if (backupTable == null || backupTable.isBlank() || !backupTable.startsWith("exam_exam_detail_bak_kfix_")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "备份表名非法");
    }
    int restored = queryMapper.rollbackKnowledge(backupTable);
    log.info("知识修正回滚完成：restored={}", restored);
    return restored;
  }

  /** 清单状态 → 唯一中文术语（改挂/保留/存疑/待分类/已写回，状态码本身不动） */
  private static String statusText(String status) {
    if (status == null) {
      return "";
    }
    return switch (status) {
      case "apply" -> "改挂";
      case "keep" -> "保留";
      case "flag" -> "存疑";
      case "pending" -> "待分类";
      case "applied" -> "已写回";
      default -> status;
    };
  }

  /** 修正清单 xlsx 导出（留痕；虽不做人工抽验，清单仍归档） */
  public byte[] exportFixList() {
    List<ExamDetailKnowledgeFixEntity> rows = fixMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamDetailKnowledgeFixEntity>()
            .orderByAsc(ExamDetailKnowledgeFixEntity::getId));
    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("知识修正清单");
      Row head = sheet.createRow(0);
      for (int i = 0; i < EXPORT_HEADERS.length; i++) {
        head.createCell(i).setCellValue(EXPORT_HEADERS[i]);
      }
      int r = 1;
      for (ExamDetailKnowledgeFixEntity e : rows) {
        Row row = sheet.createRow(r++);
        row.createCell(0).setCellValue(e.getDetailId() == null ? "" : String.valueOf(e.getDetailId()));
        row.createCell(1).setCellValue(e.getOldKnowledgeId() == null ? "" : String.valueOf(e.getOldKnowledgeId()));
        row.createCell(2).setCellValue(e.getNewKnowledgeId() == null ? "" : String.valueOf(e.getNewKnowledgeId()));
        row.createCell(3).setCellValue(e.getCourseId() == null ? "" : String.valueOf(e.getCourseId()));
        row.createCell(4).setCellValue(e.getConfidence() == null ? "" : String.valueOf(e.getConfidence()));
        row.createCell(5).setCellValue(e.getBasis() == null ? "" : e.getBasis());
        row.createCell(6).setCellValue(e.getLlmReason() == null ? "" : e.getLlmReason());
        row.createCell(7).setCellValue(statusText(e.getStatus()));
      }
      wb.write(out);
      return out.toByteArray();
    } catch (java.io.IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "清单导出失败: " + e.getMessage());
    }
  }
}

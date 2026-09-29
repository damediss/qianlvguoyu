package com.guoyu.platform.exam.knowledgefix.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.knowledgefix.dto.ApplyResultVO;
import com.guoyu.platform.exam.knowledgefix.dto.DedupStatsVO;
import com.guoyu.platform.exam.knowledgefix.dto.FixBatchVO;
import com.guoyu.platform.exam.knowledgefix.dto.FixItemVO;
import com.guoyu.platform.exam.knowledgefix.dto.QualityReportVO;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixBatchMapper;
import com.guoyu.platform.exam.knowledgefix.service.KnowledgeFixClassifyService;
import com.guoyu.platform.exam.knowledgefix.service.KnowledgeFixDedupService;
import com.guoyu.platform.exam.knowledgefix.service.KnowledgeFixWriteBackService;
import com.guoyu.platform.security.SecurityContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识归属修正（方案 §7.3）：去重软删除 + LLM 知识归属分类 + 置信分层写回。
 * 入口：课程管理 → 资源管理 → 知识归属修正。
 */
@RestController
@RequestMapping("/exam/knowledgefix")
@Slf4j
public class KnowledgeFixController {

  private final KnowledgeFixDedupService dedupService;
  private final KnowledgeFixClassifyService classifyService;
  private final KnowledgeFixWriteBackService writeBackService;
  private final ExamKnowledgeFixBatchMapper batchMapper;
  private final ExamDetailKnowledgeFixMapper fixMapper;
  private final com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixLlmLogMapper llmLogMapper;

  @Autowired
  public KnowledgeFixController(KnowledgeFixDedupService dedupService,
      KnowledgeFixClassifyService classifyService, KnowledgeFixWriteBackService writeBackService,
      ExamKnowledgeFixBatchMapper batchMapper, ExamDetailKnowledgeFixMapper fixMapper,
      com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixLlmLogMapper llmLogMapper) {
    this.dedupService = dedupService;
    this.classifyService = classifyService;
    this.writeBackService = writeBackService;
    this.batchMapper = batchMapper;
    this.fixMapper = fixMapper;
    this.llmLogMapper = llmLogMapper;
  }

  // ───────── 去重清理 ─────────

  /** 生成/刷新去重清单（幂等） */
  @PostMapping("/dedup/generate")
  @PreAuthorize("hasAuthority('exam:knowledgefix:dedup:generate')")
  @AuditLog(module = "exam", action = "knowledgefix:dedup:generate", targetType = "exam_detail_dedup_list")
  public ApiResponse<DedupStatsVO> generateDedupList() {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(dedupService.generate(userId));
  }

  /** 去重统计卡 */
  @GetMapping("/dedup/stats")
  @PreAuthorize("hasAuthority('exam:knowledgefix:view')")
  public ApiResponse<DedupStatsVO> dedupStats() {
    return ApiResponse.ok(dedupService.stats());
  }

  /** 执行去重软删除（备份 → 分批 deleted=1） */
  @PostMapping("/dedup/apply")
  @PreAuthorize("hasAuthority('exam:knowledgefix:dedup:apply')")
  @AuditLog(module = "exam", action = "knowledgefix:dedup:apply", targetType = "exam_detail_dedup_list")
  public ApiResponse<ApplyResultVO> applyDedup() {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(dedupService.applySoftDelete(tenantId, userId));
  }

  /** 去重回滚 */
  @PostMapping("/dedup/rollback")
  @PreAuthorize("hasAuthority('exam:knowledgefix:rollback')")
  @AuditLog(module = "exam", action = "knowledgefix:dedup:rollback", targetType = "exam_exam_detail")
  public ApiResponse<Integer> rollbackDedup(@RequestParam String backupTable) {
    return ApiResponse.ok(dedupService.rollbackSoftDelete(backupTable));
  }

  // ───────── 知识修正 ─────────

  /**
   * 提交 LLM 分类批次（秒回 batchNo）。可选 courseId/limit 做范围提交（方案 §13 步骤 8
   * 小批次实测用）：courseId 限定课程、limit 限制行数（1~1000），均缺省 = 全量。
   * 暂停续跑须携带相同参数。权限与审计口径不变。
   */
  @PostMapping("/classify/submit")
  @PreAuthorize("hasAuthority('exam:knowledgefix:classify:submit')")
  @AuditLog(module = "exam", action = "knowledgefix:classify:submit", targetType = "exam_knowledge_fix_batch")
  public ApiResponse<FixBatchVO> submitClassify(
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer limit) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(classifyService.submit(tenantId, userId, courseId, limit));
  }

  /** 重跑通道（FIX-3）：flag 行复位 pending，返回复位行数 */
  @PostMapping("/classify/reset")
  @PreAuthorize("hasAuthority('exam:knowledgefix:classify:submit')")
  @AuditLog(module = "exam", action = "knowledgefix:classify:reset", targetType = "exam_detail_knowledge_fix")
  public ApiResponse<Integer> resetClassify() {
    return ApiResponse.ok(classifyService.resetClassify());
  }

  /**
   * 人工暂停（FIX-10/v1.7）：登记暂停标志，编排线程在下一行派发边界停止派发；
   * 已派发行跑完后批次收尾为 paused，剩余行保持 pending，再次「提交」即可接着跑。
   * 权限沿用 classify:submit（与 reset 同口径，无需新迁移）。
   */
  @PostMapping("/classify/pause")
  @PreAuthorize("hasAuthority('exam:knowledgefix:classify:submit')")
  @AuditLog(module = "exam", action = "knowledgefix:classify:pause", targetType = "exam_knowledge_fix_batch")
  public ApiResponse<Long> pauseClassify() {
    return ApiResponse.ok(classifyService.pause());
  }

  /** 最新批次进度（含累计 LLM 用量，供"费用超预期就暂停"的现场判断） */
  @GetMapping("/classify/progress")
  @PreAuthorize("hasAuthority('exam:knowledgefix:view')")
  public ApiResponse<FixBatchVO> classifyProgress() {
    TenantContextHolder.getTenantId();
    List<ExamKnowledgeFixBatchEntity> list = batchMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
            .orderByDesc(ExamKnowledgeFixBatchEntity::getId)
            .last("LIMIT 1"));
    if (list.isEmpty()) {
      return ApiResponse.ok(null);
    }
    ExamKnowledgeFixBatchEntity b = list.get(0);
    return ApiResponse.ok(FixBatchVO.from(b, usageOf(b.getBatchNo())));
  }

  /** 批次累计用量；聚合失败不阻断进度查询（FIX-10） */
  private com.guoyu.platform.exam.knowledgefix.dto.FixBatchUsageVO usageOf(Long batchNo) {
    if (batchNo == null) {
      return null;
    }
    try {
      return com.guoyu.platform.exam.knowledgefix.dto.FixBatchUsageVO
          .from(llmLogMapper.sumUsageByBatch(batchNo));
    } catch (Exception e) {
      log.warn("批次用量聚合失败（不影响进度查询）：batchNo={}, err={}", batchNo, e.getMessage());
      return null;
    }
  }

  /** 修正清单分页（改挂/保留/存疑 筛选）；统一 PageResponse {items, page, pageSize, total} */
  @GetMapping("/fix/list")
  @PreAuthorize("hasAuthority('exam:knowledgefix:view')")
  public ApiResponse<com.guoyu.platform.common.api.PageResponse<FixItemVO>> fixList(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    TenantContextHolder.getTenantId();
    var result = fixMapper.selectPage(
        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExamDetailKnowledgeFixEntity>(page, pageSize),
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamDetailKnowledgeFixEntity>()
            .eq(status != null && !status.isBlank(),
                ExamDetailKnowledgeFixEntity::getStatus, status)
            .orderByAsc(ExamDetailKnowledgeFixEntity::getId));
    List<FixItemVO> items = result.getRecords().stream().map(e -> new FixItemVO(
        e.getId(), e.getDetailId(), e.getOldKnowledgeId(), e.getCourseId(),
        e.getNewKnowledgeId(), e.getConfidence(), e.getBasis(), e.getLlmReason(),
        e.getStatus(), null)).toList();
    return ApiResponse.ok(new com.guoyu.platform.common.api.PageResponse<>(items, page, pageSize,
        result.getTotal()));
  }

  /** 修正清单 xlsx 导出（归档留痕） */
  @GetMapping("/fix/export")
  @PreAuthorize("hasAuthority('exam:knowledgefix:classify:export')")
  public ResponseEntity<byte[]> exportFixList() {
    byte[] content = writeBackService.exportFixList();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=knowledge-fix-list.xlsx")
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(content);
  }

  /** 执行知识修正写回（仅 apply 行） */
  @PostMapping("/fix/apply")
  @PreAuthorize("hasAuthority('exam:knowledgefix:fix:apply')")
  @AuditLog(module = "exam", action = "knowledgefix:fix:apply", targetType = "exam_exam_detail")
  public ApiResponse<ApplyResultVO> applyFix() {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(writeBackService.apply(tenantId, userId));
  }

  /** 知识修正回滚 */
  @PostMapping("/fix/rollback")
  @PreAuthorize("hasAuthority('exam:knowledgefix:rollback')")
  @AuditLog(module = "exam", action = "knowledgefix:fix:rollback", targetType = "exam_exam_detail")
  public ApiResponse<Integer> rollbackFix(@RequestParam String backupTable) {
    return ApiResponse.ok(writeBackService.rollback(backupTable));
  }

  /** 质量报告（最新批次） */
  @GetMapping("/report")
  @PreAuthorize("hasAuthority('exam:knowledgefix:report:view')")
  public ApiResponse<QualityReportVO> report() {
    TenantContextHolder.getTenantId();
    List<ExamKnowledgeFixBatchEntity> list = batchMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
            .orderByDesc(ExamKnowledgeFixBatchEntity::getId)
            .last("LIMIT 1"));
    if (list.isEmpty()) {
      return ApiResponse.ok(null);
    }
    ExamKnowledgeFixBatchEntity b = list.get(0);
    int classified = b.getDone() == null ? 0 : b.getDone();
    int total = b.getTotal() == null ? 0 : b.getTotal();
    return ApiResponse.ok(new QualityReportVO(b.getBatchNo(), b.getTotal(), classified,
        b.getApplyCnt(), b.getKeepCnt(), b.getFlagCnt(), b.getFailed(),
        b.getSelfConsistencyRate(), b.getStatus(), Math.max(0, total - classified)));
  }
}

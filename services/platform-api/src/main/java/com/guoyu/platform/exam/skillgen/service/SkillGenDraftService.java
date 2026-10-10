package com.guoyu.platform.exam.skillgen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillDraftVO;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishReq;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishResultVO;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 草稿审核/查询/编辑 + 人工入库（publish，方案 v1.1 §T4 硬闸门）。
 *
 * <p>三闸门：① 仅 review_status=approved 且未入库可入库；② **编辑保存后 review_status 回退 draft**
 * （防"改完仍带 approved 绕过审核"，S2）；③ 总量硬门槛：该（工种,等级）累计有效明细（全部来源，S10）
 * + 本次 ≥ 等级下限，不足拦截提示缺口，无二次确认放行（S3）。
 * 入库映射与 PHP 导入链路口径一致（pingfen 五键/渲染模板 S11）；一期不写知识点树（C2/S1）。
 */
@Service
@Slf4j
public class SkillGenDraftService {

  private static final int SOURCE_AI = 2;
  private static final String[] PINGFEN_KEYS = {"item", "score", "scoreDesc", "maxDeduct", "step"};

  private final SkillGenDraftMapper draftMapper;
  private final SkillGenBatchMapper batchMapper;
  private final SkillBlueprintMapper blueprintMapper;
  private final SkillBlueprintNodeMapper nodeMapper;
  private final SkillBlueprintApplicationService blueprintService;
  private final ExamTaskExamshicaoMapper taskMapper;
  private final ExamTaskExamshicaoDetailMapper detailMapper;
  private final ObjectMapper objectMapper;

  public SkillGenDraftService(SkillGenDraftMapper draftMapper, SkillGenBatchMapper batchMapper,
      SkillBlueprintMapper blueprintMapper, SkillBlueprintNodeMapper nodeMapper,
      SkillBlueprintApplicationService blueprintService, ExamTaskExamshicaoMapper taskMapper,
      ExamTaskExamshicaoDetailMapper detailMapper, ObjectMapper objectMapper) {
    this.draftMapper = draftMapper;
    this.batchMapper = batchMapper;
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.blueprintService = blueprintService;
    this.taskMapper = taskMapper;
    this.detailMapper = detailMapper;
    this.objectMapper = objectMapper;
  }

  /** 草稿分页（batchId 可选：不传为"全部批次"模式，按创建时间倒序） */
  @Transactional(readOnly = true)
  public PageResponse<SkillDraftVO> page(Long tenantId, Long batchId, String reviewStatus,
      Integer publishStatus, Long nodeId, long page, long pageSize) {
    LambdaQueryWrapper<SkillGenDraftEntity> w = new LambdaQueryWrapper<SkillGenDraftEntity>()
        .eq(SkillGenDraftEntity::getTenantId, tenantId)
        .eq(batchId != null, SkillGenDraftEntity::getBatchId, batchId)
        .eq(reviewStatus != null && !reviewStatus.isBlank(),
            SkillGenDraftEntity::getReviewStatus, reviewStatus)
        .eq(publishStatus != null, SkillGenDraftEntity::getPublishStatus, publishStatus)
        .eq(nodeId != null, SkillGenDraftEntity::getNodeId, nodeId);
    if (batchId == null) {
      // "全部批次"模式：按创建时间倒序（命中 idx_esgd_tenant_time）
      w.orderByDesc(SkillGenDraftEntity::getCreatedAt).orderByDesc(SkillGenDraftEntity::getId);
    } else {
      w.orderByAsc(SkillGenDraftEntity::getNodeId).orderByAsc(SkillGenDraftEntity::getId);
    }
    Page<SkillGenDraftEntity> p = draftMapper.selectPage(new Page<>(page, pageSize), w);
    return new PageResponse<>(p.getRecords().stream().map(SkillDraftVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 编辑草稿（已入库不可编辑；保存后 review_status 回退 draft，S2） */
  @Transactional
  public SkillDraftVO edit(Long tenantId, Long userId, Long draftId, SkillGenDraftEntity patch) {
    SkillGenDraftEntity d = getOwned(tenantId, draftId);
    if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "已入库草稿不可编辑");
    }
    if (patch.getQuestionName() != null) {
      d.setQuestionName(patch.getQuestionName());
    }
    if (patch.getDurationMinutes() != null) {
      if (patch.getDurationMinutes() < 5 || patch.getDurationMinutes() > 240) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "考核时间必须为 5~240 分钟");
      }
      d.setDurationMinutes(patch.getDurationMinutes());
    }
    if (patch.getExamForm() != null) {
      d.setExamForm(patch.getExamForm());
    }
    if (patch.getSitePreparation() != null) {
      d.setSitePreparation(patch.getSitePreparation());
    }
    if (patch.getExamineePrepare() != null) {
      d.setExamineePrepare(patch.getExamineePrepare());
    }
    if (patch.getRequirements() != null) {
      d.setRequirements(patch.getRequirements());
    }
    if (patch.getNegatives() != null) {
      d.setNegatives(patch.getNegatives());
    }
    if (patch.getScoringItems() != null) {
      // 评分表行级编辑：保存前校验配分合计 = 100（与生成期 Validator 同规则）
      validateScoringTotal(patch.getScoringItems());
      d.setScoringItems(patch.getScoringItems());
    }
    if (patch.getDifficulty() != null) {
      if (patch.getDifficulty() < 1 || patch.getDifficulty() > 5) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "难度必须 1~5");
      }
      d.setDifficulty(patch.getDifficulty());
    }
    // S2：内容变更后已审核状态失效，需重新过审
    d.setReviewStatus(SkillGenDraftEntity.REVIEW_DRAFT);
    d.setReviewBy(null);
    d.setReviewAt(null);
    d.setUpdatedBy(userId);
    d.setUpdatedAt(LocalDateTime.now());
    draftMapper.updateById(d);
    return SkillDraftVO.from(d);
  }

  /** 评分表配分合计校验（配分合计必须 = 100） */
  private void validateScoringTotal(String scoringJson) {
    double sum = 0;
    for (Map<String, String> row : parseScoring(scoringJson)) {
      String score = row.get("score");
      if (score == null || score.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "评分表存在缺少配分的行");
      }
      try {
        sum += Double.parseDouble(score.trim().replace("分", ""));
      } catch (NumberFormatException e) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "评分表配分非法：" + score);
      }
    }
    if (Math.abs(sum - 100.0) > 0.01) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          String.format("评分表配分合计 %.1f，必须等于 100", sum));
    }
  }

  /** 批量审核：通过/驳回（已入库的不可再审） */
  @Transactional
  public int review(Long tenantId, Long userId, List<Long> draftIds, boolean approve) {
    int count = 0;
    for (Long id : draftIds) {
      SkillGenDraftEntity d = getOwned(tenantId, id);
      if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
        continue;
      }
      d.setReviewStatus(approve ? SkillGenDraftEntity.REVIEW_APPROVED
          : SkillGenDraftEntity.REVIEW_REJECTED);
      d.setReviewBy(userId);
      d.setReviewAt(LocalDateTime.now());
      d.setUpdatedBy(userId);
      d.setUpdatedAt(LocalDateTime.now());
      draftMapper.updateById(d);
      count++;
    }
    return count;
  }

  /**
   * 人工入库：仅 approved 且未入库的草稿 → task_examshicao(+_detail)。
   * 硬闸门：总量累计口径（全部来源）≥ 等级下限（S3/S10）；幂等：publish_status=1 跳过。
   */
  @Transactional
  public SkillPublishResultVO publish(Long tenantId, Long userId, SkillPublishReq req) {
    SkillGenBatchEntity batch = batchMapper.selectById(req.batchId());
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    SkillBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    List<SkillGenDraftEntity> drafts = draftMapper.selectList(
        new LambdaQueryWrapper<SkillGenDraftEntity>()
            .eq(SkillGenDraftEntity::getTenantId, tenantId)
            .eq(SkillGenDraftEntity::getBatchId, req.batchId())
            .eq(SkillGenDraftEntity::getReviewStatus, SkillGenDraftEntity.REVIEW_APPROVED));
    if (drafts == null || drafts.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "无审核通过的草稿可入库（仅审核通过的草稿可入库）");
    }
    // 总量硬门槛（S3/S10）：与发起预检共用同一统计方法；无二次确认放行
    int floor = SkillBlueprintApplicationService.MIN_QUESTIONS.getOrDefault(bp.getLevel(), 50);
    SkillBlueprintApplicationService.CumulativeStat before =
        blueprintService.cumulativeStat(tenantId, bp.getWorkTypeId(), bp.getLevel());
    long afterTotal = before.total() + drafts.size();
    if (afterTotal < floor) {
      throw new BusinessException(ErrorCode.CONFLICT, String.format(
          "本次入库后共 %d 道（其中 AI 生成 %d 道），低于本等级题库总量下限 %d 道，还差 %d 道，"
              + "请继续生成并审核后入库",
          afterTotal, before.aiTotal() + drafts.size(), floor, floor - afterTotal));
    }

    // 考评点名称一次性载入（避免逐行查库）
    Map<Long, SkillBlueprintNodeEntity> nodeMap = new java.util.HashMap<>();
    for (SkillBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId, bp.getId())) {
      nodeMap.put(n.getId(), n);
    }

    ExamTaskExamshicaoEntity task = resolveTask(tenantId, userId, bp, req);
    int published = 0;
    int skipped = 0;
    for (SkillGenDraftEntity d : drafts) {
      if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
        skipped++;
        continue;
      }
      SkillBlueprintNodeEntity point = nodeMap.get(d.getNodeId());
      ExamTaskExamshicaoDetailEntity detail = new ExamTaskExamshicaoDetailEntity();
      detail.setTaskId(task.getId());
      detail.setJianding(truncate(point == null ? "" : point.getName(), 500));
      detail.setContent(renderContent(d));
      detail.setKaochang(renderKaochang(d));
      detail.setKaosheng(renderKaosheng(d));
      detail.setPingfen(normalizePingfen(d.getScoringItems()));
      detail.setNanyicd(String.valueOf(d.getDifficulty() == null ? 3 : d.getDifficulty()));
      // xingwei/kaohe1-4：AI 链路不产采集链数据，留 NULL；isblod=0
      detail.setIsblod(0);
      detail.setAiBatchId(batch.getId());
      detail.setSource(SOURCE_AI);
      detail.setTenantId(tenantId.intValue());
      detail.setDeleted(0);
      detail.setCreateTime(LocalDateTime.now());
      detail.setUpdatedAt(LocalDateTime.now());
      detail.setCreatedBy(userId == null ? null : userId.intValue());
      detail.setUpdatedBy(userId == null ? null : userId.intValue());
      detailMapper.insert(detail);
      d.setPublishedTaskId(Long.valueOf(task.getId()));
      d.setPublishedDetailId(Long.valueOf(detail.getId()));
      d.setPublishStatus(1);
      d.setUpdatedBy(userId);
      d.setUpdatedAt(LocalDateTime.now());
      draftMapper.updateById(d);
      published++;
    }

    // 任务 nums：SQL 原子自增（新建任务初始 0，追加任务在原值上累加，S13 防并发丢更新）
    taskMapper.incrementNums(task.getId(), published);

    SkillBlueprintApplicationService.CumulativeStat after =
        blueprintService.cumulativeStat(tenantId, bp.getWorkTypeId(), bp.getLevel());
    log.info("操作技能草稿入库完成：batchId={}, published={}, skipped={}, 累计 {}/{}（AI {}）",
        batch.getId(), published, skipped, after.total(), floor, after.aiTotal());
    return new SkillPublishResultVO(published, skipped, Long.valueOf(task.getId()),
        after.total(), after.aiTotal());
  }

  /** 入库预览（前端闸门 UI：累计进度与缺口展示，与 publish 同一统计公式） */
  @Transactional(readOnly = true)
  public java.util.Map<String, Object> publishPreview(Long tenantId, Long batchId) {
    SkillGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    SkillBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    Long approved = draftMapper.selectCount(new LambdaQueryWrapper<SkillGenDraftEntity>()
        .eq(SkillGenDraftEntity::getTenantId, tenantId)
        .eq(SkillGenDraftEntity::getBatchId, batchId)
        .eq(SkillGenDraftEntity::getReviewStatus, SkillGenDraftEntity.REVIEW_APPROVED)
        .eq(SkillGenDraftEntity::getPublishStatus, 0));
    long approvedCount = approved == null ? 0 : approved;
    int floor = SkillBlueprintApplicationService.MIN_QUESTIONS.getOrDefault(bp.getLevel(), 50);
    SkillBlueprintApplicationService.CumulativeStat stat =
        blueprintService.cumulativeStat(tenantId, bp.getWorkTypeId(), bp.getLevel());
    long afterTotal = stat.total() + approvedCount;
    java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
    out.put("approvedCount", approvedCount);
    out.put("cumulativeTotal", stat.total());
    out.put("cumulativeAiTotal", stat.aiTotal());
    out.put("floor", floor);
    out.put("afterTotal", afterTotal);
    out.put("gap", Math.max(0, floor - afterTotal));
    out.put("canPublish", approvedCount > 0 && afterTotal >= floor);
    return out;
  }

  /** 批次全量草稿（导出用，按考评点与 id 定序） */
  @Transactional(readOnly = true)
  public List<SkillGenDraftEntity> listAll(Long tenantId, Long batchId) {
    return draftMapper.selectList(new LambdaQueryWrapper<SkillGenDraftEntity>()
        .eq(SkillGenDraftEntity::getTenantId, tenantId)
        .eq(SkillGenDraftEntity::getBatchId, batchId)
        .orderByAsc(SkillGenDraftEntity::getNodeId)
        .orderByAsc(SkillGenDraftEntity::getId));
  }

  // ─────────── 渲染模板（2026-09-11 需求方确认：考场/考生准备均为纯文字清单，不带表格与标签） ───────────

  /** content（考核试题）：名称/分值/时间/形式/具体要求/否定项 */
  String renderContent(SkillGenDraftEntity d) {
    StringBuilder sb = new StringBuilder();
    sb.append("【试题名称】").append(nvl(d.getQuestionName())).append("\n")
        .append("【本题分值】100分，权重").append(weightText(d)).append("%\n")
        .append("【考核时间】").append(d.getDurationMinutes() == null ? "" : d.getDurationMinutes())
        .append("分钟\n")
        .append("【考核形式】").append(nvl(d.getExamForm())).append("\n")
        .append("【具体考核要求】").append(nvl(d.getRequirements())).append("\n")
        .append("【否定项】").append(nvl(d.getNegatives()));
    return sb.toString();
  }

  /**
   * kaochang（考场准备）：仅设备设施清单一句文字（样本版式）：「名称×数量+单位」，顿号分隔。
   * 考核有关说明/故障·工况设置不再输出（用户 2026-09-11 确认"只保留清单"）；
   * sitePreparation JSON 中仍保留 equipment 的 name/unit/qty 供清单拼装。
   */
  String renderKaochang(SkillGenDraftEntity d) {
    return equipmentListText(d);
  }

  /** kaosheng（考生准备）：纯清单文本，不带四项头部、不带"（工、量具及）其他准备"标签（用户 2026-09-11 确认） */
  String renderKaosheng(SkillGenDraftEntity d) {
    return nvl(d.getExamineePrepare()).trim();
  }

  /** 导出与入库同版式（2026-09-11 起两者一致，均为清单文字） */
  String renderKaochangForExport(SkillGenDraftEntity d) {
    return renderKaochang(d);
  }

  /** 导出与入库同版式（2026-09-11 起两者一致，均为清单文本） */
  String renderKaoshengForExport(SkillGenDraftEntity d) {
    return renderKaosheng(d);
  }

  /** 设备设施清单 → 「名称×数量+单位、名称×数量+单位」一句文字；数量为空的条目只写名称 */
  private String equipmentListText(SkillGenDraftEntity d) {
    SkillQuestionItem.SitePrepare site = parseSite(d.getSitePreparation());
    if (site == null || site.getEquipment() == null || site.getEquipment().isEmpty()) {
      return "";
    }
    List<String> items = new ArrayList<>();
    for (SkillQuestionItem.Equipment e : site.getEquipment()) {
      if (e == null || nvl(e.getName()).isBlank()) {
        continue;
      }
      String qty = nvl(e.getQty()).trim();
      String unit = nvl(e.getUnit()).trim();
      items.add(qty.isEmpty() ? nvl(e.getName()).trim() : nvl(e.getName()).trim() + "×" + qty + unit);
    }
    return String.join("、", items);
  }

  /** pingfen 归一：五键（item/score/scoreDesc/maxDeduct/step）字符串值，与 buildPingfenItem 口径一致 */
  String normalizePingfen(String scoringJson) {
    List<Map<String, String>> rows = parseScoring(scoringJson);
    List<Map<String, String>> out = new ArrayList<>();
    for (Map<String, String> row : rows) {
      Map<String, String> item = new LinkedHashMap<>();
      for (String key : PINGFEN_KEYS) {
        item.put(key, row.getOrDefault(key, ""));
      }
      out.add(item);
    }
    try {
      return objectMapper.writeValueAsString(out);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分表序列化失败");
    }
  }

  // ─────────── 私有辅助 ───────────

  private SkillQuestionItem.SitePrepare parseSite(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, SkillQuestionItem.SitePrepare.class);
    } catch (Exception e) {
      return null;
    }
  }

  private List<Map<String, String>> parseScoring(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() { });
    } catch (Exception e) {
      return List.of();
    }
  }

  /** 查找既有待审核任务或新建（create/append 两种模式） */
  private ExamTaskExamshicaoEntity resolveTask(Long tenantId, Long userId, SkillBlueprintEntity bp,
      SkillPublishReq req) {
    if ("append".equals(req.taskMode()) && req.taskId() != null) {
      ExamTaskExamshicaoEntity t = taskMapper.selectById(req.taskId());
      if (t == null || !t.getTenantId().equals(tenantId.intValue())) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "要追加的实操命题任务不存在");
      }
      return t;
    }
    ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
    t.setWorkTypeId(bp.getWorkTypeId());
    t.setLevel(bp.getLevel());
    t.setNums(0);
    t.setStatus(0);
    t.setAddtime((int) (System.currentTimeMillis() / 1000));
    t.setTenantId(tenantId.intValue());
    t.setDeleted(0);
    t.setCreateTime(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t.setCreatedBy(userId == null ? null : userId.intValue());
    t.setUpdatedBy(userId == null ? null : userId.intValue());
    taskMapper.insert(t);
    return t;
  }

  private String weightText(SkillGenDraftEntity d) {
    return d.getWeight() == null ? "-" : d.getWeight().stripTrailingZeros().toPlainString();
  }

  private SkillGenDraftEntity getOwned(Long tenantId, Long draftId) {
    SkillGenDraftEntity d = draftMapper.selectById(draftId);
    if (d == null || !d.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "草稿不存在");
    }
    return d;
  }

  private String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }

  private String nvl(String s) {
    return s == null ? "" : s;
  }
}

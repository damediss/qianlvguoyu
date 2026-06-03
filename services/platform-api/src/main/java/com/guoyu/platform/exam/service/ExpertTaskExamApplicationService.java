package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExpertTaskExamSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskSubmitResultVO;
import com.guoyu.platform.exam.vo.ExpertTaskVO;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 专家端 - 理论题审核 (菜单 4713)。tag = "exam"。
 * 与现有 ExpertAuditApplicationService 并存,职责更聚焦:不读 detail,只读 expert 分配 + 提交。
 */
@Service
@Slf4j
public class ExpertTaskExamApplicationService {

  private static final String TAG = "exam";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ExamTaskExpertMapper expertMapper;
  private final ExamTaskExamMapper taskMapper;
  private final ExamTaskExamAuditRecordMapper auditMapper;

  public ExpertTaskExamApplicationService(
      ExamTaskExpertMapper expertMapper,
      ExamTaskExamMapper taskMapper,
      ExamTaskExamAuditRecordMapper auditMapper) {
    this.expertMapper = expertMapper;
    this.taskMapper = taskMapper;
    this.auditMapper = auditMapper;
  }

  // ─── myTasks ───
  @Transactional(readOnly = true)
  public PageResponse<ExpertTaskVO> myTasks(int role, long pageNum, long pageSize) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert task-exam list, tenant={}, userId={}, role={}", tenantId, userId, role);

    LambdaQueryWrapper<ExamTaskExpertEntity> wrapper = new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tenantId)
        .eq(ExamTaskExpertEntity::getExpertId, userId)
        .eq(ExamTaskExpertEntity::getTaskRole, role)
        .eq(ExamTaskExpertEntity::getTaskType, TAG)
        .orderByDesc(ExamTaskExpertEntity::getAddtime);
    Page<ExamTaskExpertEntity> page = expertMapper.selectPage(
        new Page<>(pageNum, pageSize), wrapper);
    List<ExamTaskExpertEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }
    List<ExpertTaskVO> vos = new ArrayList<>();
    for (ExamTaskExpertEntity r : records) {
      vos.add(toVO(r, tenantId, userId, role));
    }
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  // ─── getTaskDetail ───
  @Transactional(readOnly = true)
  public ExpertTaskVO getTaskDetail(Integer taskId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    ExamTaskExpertEntity rec = expertMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExpertEntity>()
            .eq(ExamTaskExpertEntity::getTenantId, tenantId)
            .eq(ExamTaskExpertEntity::getTaskId, taskId)
            .eq(ExamTaskExpertEntity::getTaskType, TAG)
            .eq(ExamTaskExpertEntity::getExpertId, userId));
    if (rec == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或未指派给您");
    }
    return toVO(rec, tenantId, userId, rec.getTaskRole());
  }

  // ─── submit ───
  @Transactional
  public ExamTaskSubmitResultVO submit(ExpertTaskExamSubmitReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert submit task-exam, tenant={}, userId={}, taskId={}, role={}",
        tenantId, userId, req.getTaskId(), req.getRole());

    ExamTaskExpertEntity rec = expertMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExpertEntity>()
            .eq(ExamTaskExpertEntity::getTenantId, tenantId)
            .eq(ExamTaskExpertEntity::getTaskId, req.getTaskId())
            .eq(ExamTaskExpertEntity::getTaskType, TAG)
            .eq(ExamTaskExpertEntity::getExpertId, userId)
            .eq(ExamTaskExpertEntity::getTaskRole, req.getRole()));
    if (rec == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "未指派给您或角色不匹配");
    }

    ExamTaskExamEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamEntity>()
            .eq(ExamTaskExamEntity::getTenantId, tenantId)
            .eq(ExamTaskExamEntity::getId, req.getTaskId()));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }

    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    java.util.List<ExpertTaskExamSubmitReq.ExamItem> items = req.getItems() != null
        ? req.getItems() : new java.util.ArrayList<>();
    // 兼容单题模式:id 直接挂在 req 上
    if (items.isEmpty() && req.getId() != null) {
      ExpertTaskExamSubmitReq.ExamItem single = new ExpertTaskExamSubmitReq.ExamItem();
      single.setId(req.getId());
      single.setExamContent(req.getExamContent());
      single.setExamA(req.getExamA());
      single.setExamB(req.getExamB());
      single.setExamC(req.getExamC());
      single.setExamD(req.getExamD());
      single.setExamE(req.getExamE());
      single.setExamF(req.getExamF());
      single.setExamG(req.getExamG());
      single.setExamH(req.getExamH());
      single.setExamAnswer(req.getExamAnswer());
      items.add(single);
    }

    for (ExpertTaskExamSubmitReq.ExamItem it : items) {
      if (it.getId() == null) {
        log.warn("submit skipping item with null id");
        continue;
      }
      // 查重
      if (auditMapper.exists(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
          .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamAuditRecordEntity::getTaskId, req.getTaskId())
          .eq(ExamTaskExamAuditRecordEntity::getExamId, it.getId())
          .eq(ExamTaskExamAuditRecordEntity::getAuditRole, req.getRole()))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "已经提交过该题: " + it.getId());
      }
      ExamTaskExamAuditRecordEntity r = new ExamTaskExamAuditRecordEntity();
      r.setTenantId(tenantId);
      r.setExpertId(userId);
      r.setAuditRole(req.getRole());
      r.setTaskId(req.getTaskId());
      r.setExamId(it.getId());
      r.setExamType(req.getExamType());
      r.setExamContent(it.getExamContent());
      r.setExamA(it.getExamA());
      r.setExamB(it.getExamB());
      r.setExamC(it.getExamC());
      r.setExamD(it.getExamD());
      r.setExamE(it.getExamE());
      r.setExamF(it.getExamF());
      r.setExamG(it.getExamG());
      r.setExamH(it.getExamH());
      r.setExamAnswer(it.getExamAnswer());
      r.setAuditStatus(1);
      r.setYmd(today);
      r.setAddtime(LocalDateTime.now());
      r.setCreatedBy(userId);
      r.setUpdatedBy(userId);
      auditMapper.insert(r);
    }

    // 统计 + 状态推进
    long audited = auditMapper.selectCount(
        new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
            .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamAuditRecordEntity::getTaskId, req.getTaskId())
            .eq(ExamTaskExamAuditRecordEntity::getAuditRole, req.getRole())
            .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
    long total = task.getNums() != null ? task.getNums() : 0;
    boolean isdone = audited >= total;

    ExamTaskSubmitResultVO out = new ExamTaskSubmitResultVO();
    out.setCount(audited);
    out.setTotal(total);
    out.setIsdone(isdone ? 1 : 0);

    if (isdone) {
      if (rec.getEndtime() == null) {
        rec.setEndtime(LocalDateTime.now());
        rec.setUpdatedBy(userId);
        expertMapper.updateById(rec);
      }
      // 推 task.status:初审完 → 2(终审中);终审完 → 3(完成)
      int newStatus = req.getRole() == 0 ? 2 : 3;
      task.setStatus(newStatus);
      task.setUpdatedBy(userId);
      taskMapper.updateById(task);
    } else if (req.getRole() == 0 && (task.getStatus() == null || task.getStatus() == 0)) {
      // 推进 task.status 到 1(初审核中)
      task.setStatus(1);
      task.setUpdatedBy(userId);
      taskMapper.updateById(task);
    }
    return out;
  }

  // ─── helpers ───
  private ExpertTaskVO toVO(ExamTaskExpertEntity r, Integer tenantId, Integer userId, int role) {
    ExpertTaskVO vo = new ExpertTaskVO();
    vo.setId(r.getId());
    vo.setTaskId(r.getTaskId());
    vo.setTag(TAG);
    vo.setTagCn("题库理论审核");
    vo.setAddtimeCn(r.getAddtime() != null ? r.getAddtime().format(DATETIME_FMT) : "-");
    vo.setEndtimeCn(r.getEndtime() != null ? r.getEndtime().format(DATETIME_FMT) : "-");
    vo.setEndtime(r.getEndtime() != null ? 1L : 0L);
    vo.setStatus(r.getEndtime() != null ? "完成" : "进行中");

    ExamTaskExamEntity t = taskMapper.selectById(r.getTaskId());
    if (t != null) {
      vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
      vo.setNums(t.getNums());
      long audited = auditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
          .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamAuditRecordEntity::getTaskId, r.getTaskId())
          .eq(ExamTaskExamAuditRecordEntity::getExpertId, userId)
          .eq(ExamTaskExamAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
      vo.setNums(Math.max(0, t.getNums() - (int) audited));
      if (vo.getNums() == 0 && vo.getEndtime() == 0L) {
        vo.setStatus("完成");
      }
    } else {
      vo.setTitle("-");
      vo.setNums(0);
    }
    return vo;
  }

  private String levelName(Integer level) {
    if (level == null) return "-";
    return level >= 1 && level < LEVELS.length ? LEVELS[level] : "-";
  }
}

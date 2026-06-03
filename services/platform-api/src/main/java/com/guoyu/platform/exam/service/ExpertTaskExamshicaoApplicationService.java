package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExpertTaskExamshicaoSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
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

/** 专家端 - 实操题审核 (菜单 4714)。tag = "examshicao"。 */
@Service
@Slf4j
public class ExpertTaskExamshicaoApplicationService {

  private static final String TAG = "examshicao";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ExamTaskExpertMapper expertMapper;
  private final ExamTaskExamshicaoMapper taskMapper;
  private final ExamTaskExamshicaoAuditRecordMapper auditMapper;

  public ExpertTaskExamshicaoApplicationService(
      ExamTaskExpertMapper expertMapper,
      ExamTaskExamshicaoMapper taskMapper,
      ExamTaskExamshicaoAuditRecordMapper auditMapper) {
    this.expertMapper = expertMapper;
    this.taskMapper = taskMapper;
    this.auditMapper = auditMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExpertTaskVO> myTasks(int role, long pageNum, long pageSize) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert task-examshicao list, tenant={}, userId={}, role={}", tenantId, userId, role);
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

  @Transactional
  public ExamTaskSubmitResultVO submit(ExpertTaskExamshicaoSubmitReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert submit task-examshicao, tenant={}, userId={}, taskId={}, role={}",
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
    ExamTaskExamshicaoEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
            .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoEntity::getId, req.getTaskId()));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }

    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    List<ExpertTaskExamshicaoSubmitReq.ShicaoItem> items = req.getItems() != null
        ? req.getItems() : new ArrayList<>();
    if (items.isEmpty() && req.getId() != null) {
      ExpertTaskExamshicaoSubmitReq.ShicaoItem single = new ExpertTaskExamshicaoSubmitReq.ShicaoItem();
      single.setId(req.getId());
      single.setJianding(req.getJianding());
      single.setContent(req.getContent());
      single.setPingfen(req.getPingfen());
      single.setKaochang(req.getKaochang());
      single.setKaosheng(req.getKaosheng());
      items.add(single);
    }

    for (ExpertTaskExamshicaoSubmitReq.ShicaoItem it : items) {
      if (it.getId() == null) continue;
      if (auditMapper.exists(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, req.getTaskId())
          .eq(ExamTaskExamshicaoAuditRecordEntity::getExamId, it.getId())
          .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, req.getRole()))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "已经提交过该题: " + it.getId());
      }
      ExamTaskExamshicaoAuditRecordEntity r = new ExamTaskExamshicaoAuditRecordEntity();
      r.setTenantId(tenantId);
      r.setExpertId(userId);
      r.setAuditRole(req.getRole());
      r.setTaskId(req.getTaskId());
      r.setExamId(it.getId());
      r.setJianding(it.getJianding());
      r.setContent(it.getContent());
      r.setPingfen(it.getPingfen());
      r.setKaochang(it.getKaochang());
      r.setKaosheng(it.getKaosheng());
      r.setAuditStatus(1);
      r.setYmd(today);
      r.setAddtime(LocalDateTime.now());
      r.setCreatedBy(userId);
      r.setUpdatedBy(userId);
      auditMapper.insert(r);
    }

    long audited = auditMapper.selectCount(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, req.getTaskId())
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, req.getRole())
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
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
      int newStatus = req.getRole() == 0 ? 2 : 3;
      task.setStatus(newStatus);
      task.setUpdatedBy(userId);
      taskMapper.updateById(task);
    } else if (req.getRole() == 0 && (task.getStatus() == null || task.getStatus() == 0)) {
      task.setStatus(1);
      task.setUpdatedBy(userId);
      taskMapper.updateById(task);
    }
    return out;
  }

  private ExpertTaskVO toVO(ExamTaskExpertEntity r, Integer tenantId, Integer userId, int role) {
    ExpertTaskVO vo = new ExpertTaskVO();
    vo.setId(r.getId());
    vo.setTaskId(r.getTaskId());
    vo.setTag(TAG);
    vo.setTagCn("题库实操审核");
    vo.setAddtimeCn(r.getAddtime() != null ? r.getAddtime().format(DATETIME_FMT) : "-");
    vo.setEndtimeCn(r.getEndtime() != null ? r.getEndtime().format(DATETIME_FMT) : "-");
    vo.setEndtime(r.getEndtime() != null ? 1L : 0L);
    vo.setStatus(r.getEndtime() != null ? "完成" : "进行中");
    ExamTaskExamshicaoEntity t = taskMapper.selectById(r.getTaskId());
    if (t != null) {
      vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
      vo.setNums(t.getNums());
      long audited = auditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, r.getTaskId())
          .eq(ExamTaskExamshicaoAuditRecordEntity::getExpertId, userId)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
      vo.setNums(Math.max(0, t.getNums() - (int) audited));
      if (vo.getNums() == 0 && vo.getEndtime() == 0L) vo.setStatus("完成");
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

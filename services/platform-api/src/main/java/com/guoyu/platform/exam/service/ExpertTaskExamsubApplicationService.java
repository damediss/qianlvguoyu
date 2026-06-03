package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExpertTaskExamsubSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
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

/** 专家端 - 主观题审核 (菜单 4715)。tag = "examsub"。 */
@Service
@Slf4j
public class ExpertTaskExamsubApplicationService {

  private static final String TAG = "examsub";
  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ExamTaskExpertMapper expertMapper;
  private final ExamTaskExamsubMapper taskMapper;
  private final ExamTaskExamsubAuditRecordMapper auditMapper;

  public ExpertTaskExamsubApplicationService(
      ExamTaskExpertMapper expertMapper,
      ExamTaskExamsubMapper taskMapper,
      ExamTaskExamsubAuditRecordMapper auditMapper) {
    this.expertMapper = expertMapper;
    this.taskMapper = taskMapper;
    this.auditMapper = auditMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExpertTaskVO> myTasks(int role, long pageNum, long pageSize) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert task-examsub list, tenant={}, userId={}, role={}", tenantId, userId, role);
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
  public ExamTaskSubmitResultVO submit(ExpertTaskExamsubSubmitReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Integer userId = SecurityContext.currentUser().getUserId().intValue();
    log.info("expert submit task-examsub, tenant={}, userId={}, taskId={}, role={}",
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
    ExamTaskExamsubEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<ExamTaskExamsubEntity>()
            .eq(ExamTaskExamsubEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubEntity::getId, req.getTaskId()));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }

    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    List<ExpertTaskExamsubSubmitReq.SubItem> items = req.getItems() != null
        ? req.getItems() : new ArrayList<>();
    if (items.isEmpty() && req.getId() != null) {
      ExpertTaskExamsubSubmitReq.SubItem single = new ExpertTaskExamsubSubmitReq.SubItem();
      single.setId(req.getId());
      single.setContent(req.getContent());
      single.setAnswer(req.getAnswer());
      single.setPic(req.getPic());
      items.add(single);
    }

    for (ExpertTaskExamsubSubmitReq.SubItem it : items) {
      if (it.getId() == null) continue;
      if (auditMapper.exists(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
          .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, req.getTaskId())
          .eq(ExamTaskExamsubAuditRecordEntity::getExamId, it.getId())
          .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, req.getRole()))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "已经提交过该题: " + it.getId());
      }
      ExamTaskExamsubAuditRecordEntity r = new ExamTaskExamsubAuditRecordEntity();
      r.setTenantId(tenantId);
      r.setExpertId(userId);
      r.setAuditRole(req.getRole());
      r.setTaskId(req.getTaskId());
      r.setExamId(it.getId());
      r.setContent(it.getContent());
      r.setAnswer(it.getAnswer());
      r.setPic(it.getPic());
      r.setAuditStatus(1);
      r.setYmd(today);
      r.setAddtime(LocalDateTime.now());
      r.setCreatedBy(userId);
      r.setUpdatedBy(userId);
      auditMapper.insert(r);
    }

    long audited = auditMapper.selectCount(
        new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
            .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, req.getTaskId())
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, req.getRole())
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
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
    vo.setTagCn("题库主观审核");
    vo.setAddtimeCn(r.getAddtime() != null ? r.getAddtime().format(DATETIME_FMT) : "-");
    vo.setEndtimeCn(r.getEndtime() != null ? r.getEndtime().format(DATETIME_FMT) : "-");
    vo.setEndtime(r.getEndtime() != null ? 1L : 0L);
    vo.setStatus(r.getEndtime() != null ? "完成" : "进行中");
    ExamTaskExamsubEntity t = taskMapper.selectById(r.getTaskId());
    if (t != null) {
      vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
      vo.setNums(t.getNums());
      long audited = auditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
          .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, r.getTaskId())
          .eq(ExamTaskExamsubAuditRecordEntity::getExpertId, userId)
          .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
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

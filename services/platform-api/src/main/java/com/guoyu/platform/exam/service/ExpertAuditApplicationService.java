package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExpertAuditSubmitReq;
import com.guoyu.platform.exam.dto.ExpertTaskQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuEntity;
import com.guoyu.platform.exam.entity.ExamTaskXimuExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuExpertMapper;
import com.guoyu.platform.exam.mapper.ExamTaskXimuMapper;
import com.guoyu.platform.exam.vo.ExpertTaskVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ExpertAuditApplicationService {

  private static final String[] LEVELS = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final Map<String, String> TAG_CN = Map.of(
      "exam", "题库理论审核",
      "examshicao", "题库实操审核",
      "examsub", "题库主观审核",
      "ximu", "细目表审核"
  );

  private final ObjectMapper objectMapper;
  private final ExamTaskXimuExpertMapper expertMapper;
  private final ExamTaskXimuMapper ximuMapper;
  private final ExamTaskXimuDetailMapper ximuDetailMapper;
  private final ExamTaskXimuAuditRecordMapper ximuAuditMapper;
  private final ExamTaskExamMapper examMapper;
  private final ExamTaskExamDetailMapper examDetailMapper;
  private final ExamTaskExamAuditRecordMapper examAuditMapper;
  private final ExamTaskExamshicaoMapper examshicaoMapper;
  private final ExamTaskExamshicaoDetailMapper examshicaoDetailMapper;
  private final ExamTaskExamshicaoAuditRecordMapper examshicaoAuditMapper;
  private final ExamTaskExamsubMapper examsubMapper;
  private final ExamTaskExamsubDetailMapper examsubDetailMapper;
  private final ExamTaskExamsubAuditRecordMapper examsubAuditMapper;

  public ExpertAuditApplicationService(
      ExamTaskXimuExpertMapper expertMapper,
      ExamTaskXimuMapper ximuMapper,
      ExamTaskXimuDetailMapper ximuDetailMapper,
      ExamTaskXimuAuditRecordMapper ximuAuditMapper,
      ExamTaskExamMapper examMapper,
      ExamTaskExamDetailMapper examDetailMapper,
      ExamTaskExamAuditRecordMapper examAuditMapper,
      ExamTaskExamshicaoMapper examshicaoMapper,
      ExamTaskExamshicaoDetailMapper examshicaoDetailMapper,
      ExamTaskExamshicaoAuditRecordMapper examshicaoAuditMapper,
      ExamTaskExamsubMapper examsubMapper,
      ExamTaskExamsubDetailMapper examsubDetailMapper,
      ExamTaskExamsubAuditRecordMapper examsubAuditMapper,
      ObjectMapper objectMapper
  ) {
    this.objectMapper = objectMapper;
    this.expertMapper = expertMapper;
    this.ximuMapper = ximuMapper;
    this.ximuDetailMapper = ximuDetailMapper;
    this.ximuAuditMapper = ximuAuditMapper;
    this.examMapper = examMapper;
    this.examDetailMapper = examDetailMapper;
    this.examAuditMapper = examAuditMapper;
    this.examshicaoMapper = examshicaoMapper;
    this.examshicaoDetailMapper = examshicaoDetailMapper;
    this.examshicaoAuditMapper = examshicaoAuditMapper;
    this.examsubMapper = examsubMapper;
    this.examsubDetailMapper = examsubDetailMapper;
    this.examsubAuditMapper = examsubAuditMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExpertTaskVO> pageStartTasks(ExpertTaskQueryReq req) {
    return pageTasks(req, 0);
  }

  @Transactional
  public PageResponse<ExpertTaskVO> pageEndTasks(ExpertTaskQueryReq req) {
    tryAutoCompleteEndTasks();
    return pageTasks(req, 1);
  }

  private PageResponse<ExpertTaskVO> pageTasks(ExpertTaskQueryReq req, int taskRole) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("expert task list, tenant={}, userId={}, role={}, page={}", tenantId, userId, taskRole, req.getPage());

    // debug: check raw DB count
    long rawCount = expertMapper.selectCount(new LambdaQueryWrapper<ExamTaskXimuExpertEntity>()
        .eq(ExamTaskXimuExpertEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuExpertEntity::getExpertId, userId.intValue())
        .eq(ExamTaskXimuExpertEntity::getTaskRole, taskRole)
        .in(ExamTaskXimuExpertEntity::getTag, List.of("exam", "examshicao", "examsub", "ximu")));
    log.info(">>> DEBUG start/end tasks: tenantId={}, userId={}, taskRole={}, rawCount={}",
        tenantId, userId, taskRole, rawCount);


    LambdaQueryWrapper<ExamTaskXimuExpertEntity> wrapper = new LambdaQueryWrapper<ExamTaskXimuExpertEntity>()
        .eq(ExamTaskXimuExpertEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuExpertEntity::getExpertId, userId.intValue())
        .eq(ExamTaskXimuExpertEntity::getTaskRole, taskRole)
        .in(ExamTaskXimuExpertEntity::getTag, List.of("exam", "examshicao", "examsub", "ximu"));

    if (req.getStatus() != null && req.getStatus() > 0) {
      if (req.getStatus() == 1) {
        wrapper.isNull(ExamTaskXimuExpertEntity::getEndtime);
      } else if (req.getStatus() == 2) {
        wrapper.isNotNull(ExamTaskXimuExpertEntity::getEndtime);
      }
    }

    wrapper.orderByDesc(ExamTaskXimuExpertEntity::getAddtime);

    Page<ExamTaskXimuExpertEntity> page = expertMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamTaskXimuExpertEntity> records = page.getRecords();
    log.info(">>> DEBUG page result: total={}, page={}, size={}, records.size={}", page.getTotal(), page.getCurrent(), page.getSize(), records.size());
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    List<ExpertTaskVO> vos = new ArrayList<>();
    for (ExamTaskXimuExpertEntity r : records) {
      ExpertTaskVO vo = new ExpertTaskVO();
      vo.setId(r.getId());
      vo.setTaskId(r.getTaskId());
      vo.setTag(r.getTag());
      vo.setTagCn(TAG_CN.getOrDefault(r.getTag(), r.getTag()));
      vo.setAddtimeCn(r.getAddtime() != null ? r.getAddtime().format(DATETIME_FMT) : "-");
      vo.setEndtimeCn(r.getEndtime() != null ? r.getEndtime().format(DATETIME_FMT) : "-");
      vo.setEndtime(r.getEndtime() != null ? 1L : 0L);
      vo.setStatus(r.getEndtime() != null ? "完成" : "进行中");

      resolveTaskInfo(vo, r.getTag(), r.getTaskId(), userId.intValue(), taskRole);
      vos.add(vo);
    }
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  private void resolveTaskInfo(ExpertTaskVO vo, String tag, Integer taskId, Integer expertId, Integer role) {
    try {
      switch (tag) {
        case "ximu" -> {
          ExamTaskXimuEntity t = ximuMapper.selectById(taskId);
          if (t != null) {
            vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()) + "-" + (t.getTypes() > 0 ? "实操" : "理论"));
            vo.setNums(t.getNums());
            long audited = ximuAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
                .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
                .eq(ExamTaskXimuAuditRecordEntity::getExpertId, expertId)
                .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, role)
                .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1));
            vo.setNums(Math.max(0, t.getNums() - (int) audited));
            if (vo.getNums() == 0 && vo.getEndtime() == 0L) {
              vo.setStatus("完成");
            }
          }
        }
        case "exam" -> {
          ExamTaskExamEntity t = examMapper.selectById(taskId);
          if (t != null) {
            vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
            vo.setNums(t.getNums());
            long audited = examAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
                .eq(ExamTaskExamAuditRecordEntity::getTaskId, taskId)
                .eq(ExamTaskExamAuditRecordEntity::getExpertId, expertId)
                .eq(ExamTaskExamAuditRecordEntity::getAuditRole, role)
                .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
            vo.setNums(Math.max(0, t.getNums() - (int) audited));
            if (vo.getNums() == 0 && vo.getEndtime() == 0L) {
              vo.setStatus("完成");
            }
          }
        }
        case "examshicao" -> {
          ExamTaskExamshicaoEntity t = examshicaoMapper.selectById(taskId);
          if (t != null) {
            vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
            vo.setNums(t.getNums());
            long audited = examshicaoAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
                .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
                .eq(ExamTaskExamshicaoAuditRecordEntity::getExpertId, expertId)
                .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
                .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
            vo.setNums(Math.max(0, t.getNums() - (int) audited));
            if (vo.getNums() == 0 && vo.getEndtime() == 0L) {
              vo.setStatus("完成");
            }
          }
        }
        case "examsub" -> {
          ExamTaskExamsubEntity t = examsubMapper.selectById(taskId);
          if (t != null) {
            vo.setTitle(t.getWorkTypeId() + "-" + levelName(t.getLevel()));
            vo.setNums(t.getNums());
            long audited = examsubAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
                .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, taskId)
                .eq(ExamTaskExamsubAuditRecordEntity::getExpertId, expertId)
                .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, role)
                .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
            vo.setNums(Math.max(0, t.getNums() - (int) audited));
            if (vo.getNums() == 0 && vo.getEndtime() == 0L) {
              vo.setStatus("完成");
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to resolve task info for tag={} taskId={}", tag, taskId, e);
      vo.setTitle("-");
      vo.setNums(0);
    }
  }

  // exam

  @Transactional(readOnly = true)
  public List<Map<String, Object>> getExamList(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    List<Integer> auditedIds = examAuditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
            .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskExamAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamAuditRecordEntity::getExpertId, userId)
            .eq(ExamTaskExamAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskExamAuditRecordEntity::getExamId))
        .stream().map(ExamTaskExamAuditRecordEntity::getExamId).collect(Collectors.toList());

    LambdaQueryWrapper<ExamTaskExamDetailEntity> dw = new LambdaQueryWrapper<ExamTaskExamDetailEntity>()
        .eq(ExamTaskExamDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamDetailEntity::getTaskId, taskId);
    if (!auditedIds.isEmpty()) {
      dw.notIn(ExamTaskExamDetailEntity::getId, auditedIds);
    }

    List<ExamTaskExamDetailEntity> list = examDetailMapper.selectList(dw);
    return list.stream().map(d -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId());
      m.put("task_id", d.getTaskId());
      m.put("exam_type", d.getExamType());
      m.put("exam_content", d.getExamContent());
      m.put("exam_a", d.getExamA());
      m.put("exam_b", d.getExamB());
      m.put("exam_c", d.getExamC());
      m.put("exam_d", d.getExamD());
      m.put("exam_e", d.getExamE());
      m.put("exam_f", d.getExamF());
      m.put("exam_g", d.getExamG());
      m.put("exam_h", d.getExamH());
      m.put("exam_answer", d.getExamAnswer());
      return m;
    }).collect(Collectors.toList());
  }

  @Transactional
  public Map<String, Object> submitExam(ExpertAuditSubmitReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    if (examAuditMapper.exists(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
        .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamAuditRecordEntity::getTaskId, req.getTaskId())
        .eq(ExamTaskExamAuditRecordEntity::getExamId, req.getId())
        .eq(ExamTaskExamAuditRecordEntity::getAuditRole, req.getRole()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已提交过");
    }

    ExamTaskExamAuditRecordEntity record = new ExamTaskExamAuditRecordEntity();
    record.setTenantId(tenantId.intValue());
    record.setExpertId(userId.intValue());
    record.setAuditRole(req.getRole());
    record.setTaskId(req.getTaskId());
    record.setExamId(req.getId());
    record.setExamType(req.getExamType());
    record.setExamContent(req.getExamContent());
    record.setExamA(req.getExamA());
    record.setExamB(req.getExamB());
    record.setExamC(req.getExamC());
    record.setExamD(req.getExamD());
    record.setExamE(req.getExamE());
    record.setExamF(req.getExamF());
    record.setExamG(req.getExamG());
    record.setExamH(req.getExamH());
    record.setExamAnswer(req.getExamAnswer());
    record.setAuditStatus(1);
    record.setYmd(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    record.setAddtime(LocalDateTime.now());
    examAuditMapper.insert(record);

    return checkAndComplete("exam", req.getTaskId(), userId.intValue(), req.getRole(), tenantId.intValue());
  }

  // examshicao

  @Transactional(readOnly = true)
  public List<Map<String, Object>> getExamshicaoList(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    List<Integer> auditedIds = examshicaoAuditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getExpertId, userId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskExamshicaoAuditRecordEntity::getExamId))
        .stream().map(ExamTaskExamshicaoAuditRecordEntity::getExamId).collect(Collectors.toList());

    LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity> dw = new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
        .eq(ExamTaskExamshicaoDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamshicaoDetailEntity::getTaskId, taskId);
    if (!auditedIds.isEmpty()) {
      dw.notIn(ExamTaskExamshicaoDetailEntity::getId, auditedIds);
    }

    List<ExamTaskExamshicaoDetailEntity> list = examshicaoDetailMapper.selectList(dw);
    return list.stream().map(d -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId());
      m.put("task_id", d.getTaskId());
      m.put("jianding", d.getJianding());
      m.put("content", d.getContent());
      m.put("pingfen", d.getPingfen());
      m.put("kaochang", d.getKaochang());
      m.put("kaosheng", d.getKaosheng());
      return m;
    }).collect(Collectors.toList());
  }

  @Transactional
  public Map<String, Object> submitExamshicao(ExpertAuditSubmitReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    if (examshicaoAuditMapper.exists(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
        .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, req.getTaskId())
        .eq(ExamTaskExamshicaoAuditRecordEntity::getExamId, req.getId())
        .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, req.getRole()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已提交过");
    }

    ExamTaskExamshicaoAuditRecordEntity record = new ExamTaskExamshicaoAuditRecordEntity();
    record.setTenantId(tenantId.intValue());
    record.setExpertId(userId.intValue());
    record.setAuditRole(req.getRole());
    record.setTaskId(req.getTaskId());
    record.setExamId(req.getId());
    record.setJianding(req.getJianding());
    record.setContent(req.getContent());
    record.setPingfen(req.getPingfen());
    record.setKaochang(req.getKaochang());
    record.setKaosheng(req.getKaosheng());
    record.setAuditStatus(1);
    record.setYmd(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    record.setAddtime(LocalDateTime.now());
    examshicaoAuditMapper.insert(record);

    return checkAndComplete("examshicao", req.getTaskId(), userId.intValue(), req.getRole(), tenantId.intValue());
  }

  // examsub

  @Transactional(readOnly = true)
  public List<Map<String, Object>> getExamsubList(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    List<Integer> auditedIds = examsubAuditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
            .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamsubAuditRecordEntity::getExpertId, userId)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskExamsubAuditRecordEntity::getExamId))
        .stream().map(ExamTaskExamsubAuditRecordEntity::getExamId).collect(Collectors.toList());

    LambdaQueryWrapper<ExamTaskExamsubDetailEntity> dw = new LambdaQueryWrapper<ExamTaskExamsubDetailEntity>()
        .eq(ExamTaskExamsubDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamsubDetailEntity::getTaskId, taskId);
    if (!auditedIds.isEmpty()) {
      dw.notIn(ExamTaskExamsubDetailEntity::getId, auditedIds);
    }

    List<ExamTaskExamsubDetailEntity> list = examsubDetailMapper.selectList(dw);
    return list.stream().map(d -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId());
      m.put("task_id", d.getTaskId());
      m.put("zhishidian", d.getZhishidian());
      m.put("content", d.getContent());
      m.put("answer", d.getAnswer());
      m.put("pic", d.getPic());
      return m;
    }).collect(Collectors.toList());
  }

  @Transactional
  public Map<String, Object> submitExamsub(ExpertAuditSubmitReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    if (examsubAuditMapper.exists(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
        .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, req.getTaskId())
        .eq(ExamTaskExamsubAuditRecordEntity::getExamId, req.getId())
        .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, req.getRole()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已提交过");
    }

    ExamTaskExamsubAuditRecordEntity record = new ExamTaskExamsubAuditRecordEntity();
    record.setTenantId(tenantId.intValue());
    record.setExpertId(userId.intValue());
    record.setAuditRole(req.getRole());
    record.setTaskId(req.getTaskId());
    record.setExamId(req.getId());
    record.setContent(req.getContent());
    record.setAnswer(req.getAnswer());
    record.setPic(req.getPic());
    record.setAuditStatus(1);
    record.setYmd(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    record.setAddtime(LocalDateTime.now());
    examsubAuditMapper.insert(record);

    return checkAndComplete("examsub", req.getTaskId(), userId.intValue(), req.getRole(), tenantId.intValue());
  }

  // ximu

  @Transactional(readOnly = true)
  public Map<String, Object> getXimuList(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("expert ximu list, tenant={}, userId={}, taskId={}, role={}", tenantId, userId, taskId, role);

    ExamTaskXimuEntity task = ximuMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }

    LambdaQueryWrapper<ExamTaskXimuDetailEntity> dw = new LambdaQueryWrapper<ExamTaskXimuDetailEntity>()
        .eq(ExamTaskXimuDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuDetailEntity::getTaskId, taskId);

    // 终审且初审未完成时，只看初审已审的 detail
    if (role > 0 && task.getStatus() != null && task.getStatus() == 1) {
      List<Integer> startAuditedIds = ximuAuditMapper.selectList(
          new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
              .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
              .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
              .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, 0)
              .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1)
              .select(ExamTaskXimuAuditRecordEntity::getXimuId))
          .stream().map(ExamTaskXimuAuditRecordEntity::getXimuId).distinct().collect(Collectors.toList());
      if (!startAuditedIds.isEmpty()) {
        dw.in(ExamTaskXimuDetailEntity::getId, startAuditedIds);
      }
    }

    // 排除当前用户已审的 detail
    List<Integer> auditedIds = ximuAuditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
            .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskXimuAuditRecordEntity::getExpertId, userId)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskXimuAuditRecordEntity::getXimuId))
        .stream().map(ExamTaskXimuAuditRecordEntity::getXimuId).collect(Collectors.toList());
    if (!auditedIds.isEmpty()) {
      dw.notIn(ExamTaskXimuDetailEntity::getId, auditedIds);
    }

    List<ExamTaskXimuDetailEntity> list = ximuDetailMapper.selectList(dw);
    int types = task.getTypes() != null ? task.getTypes() : 0;
    List<Map<String, Object>> treeData = buildXimuTree(list, types);
    long ximuTotal = treeData.stream().flatMapToLong(n -> countLeafNodes(n)).sum();

    Map<String, Object> result = new LinkedHashMap<>();
    Map<String, Object> taskXimu = new LinkedHashMap<>();
    taskXimu.put("id", task.getId());
    taskXimu.put("types", types);
    taskXimu.put("status", task.getStatus());
    result.put("taskXimu", taskXimu);
    result.put("treeData", treeData);
    result.put("ximuTotal", ximuTotal);
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> buildXimuTree(List<ExamTaskXimuDetailEntity> list, int types) {
    try {
      // sno_0 层级分组 (LinkedHashMap 保证字母顺序)
      Map<String, Map<String, Object>> sno0Map = new LinkedHashMap<>();
      for (ExamTaskXimuDetailEntity d : list) {
        Map<String, String> sno0Json = objectMapper.readValue(d.getSno0(), new TypeReference<LinkedHashMap<String, String>>() {});
        Map<String, String> sno1Json = objectMapper.readValue(d.getSno1(), new TypeReference<LinkedHashMap<String, String>>() {});
        for (Map.Entry<String, String> s0 : sno0Json.entrySet()) {
          String s0Key = s0.getKey();
          sno0Map.putIfAbsent(s0Key, new LinkedHashMap<>());
          Map<String, Object> s1Map = sno0Map.get(s0Key);
          s1Map.putIfAbsent("_label", s0.getKey() + ":" + s0.getValue());
          s1Map.putIfAbsent("_children", new LinkedHashMap<String, Map<String, Object>>());
          Map<String, Map<String, Object>> s1Children = (Map<String, Map<String, Object>>) s1Map.get("_children");

          for (Map.Entry<String, String> s1 : sno1Json.entrySet()) {
            String s1Key = s1.getKey();
            s1Children.putIfAbsent(s1Key, new LinkedHashMap<>());
            Map<String, Object> s1Node = s1Children.get(s1Key);
            s1Node.putIfAbsent("_label", s1.getKey() + ":" + s1.getValue());
            if (types == 0) {
              // 理论: 3层，sno_0→sno_1→sno→list
              s1Node.putIfAbsent("_children", new LinkedHashMap<String, Map<String, Object>>());
              Map<String, Map<String, Object>> snoMap = (Map<String, Map<String, Object>>) s1Node.get("_children");
              String snoKey = d.getSno();
              snoMap.putIfAbsent(snoKey, new LinkedHashMap<>());
              Map<String, Object> snoNode = snoMap.get(snoKey);
              snoNode.putIfAbsent("_label", d.getSno());
              snoNode.putIfAbsent("_task_id", d.getTaskId());
              snoNode.putIfAbsent("_title", d.getTitle());
              snoNode.putIfAbsent("_list", new ArrayList<Map<String, Object>>());
              List<Map<String, Object>> items = (List<Map<String, Object>>) snoNode.get("_list");
              items.add(itemMap(d));
            } else {
              // 实操: 2层，sno_0→sno_1→list
              s1Node.putIfAbsent("_task_id", d.getTaskId());
              s1Node.putIfAbsent("_title", d.getTitle());
              s1Node.putIfAbsent("_list", new ArrayList<Map<String, Object>>());
              List<Map<String, Object>> items = (List<Map<String, Object>>) s1Node.get("_list");
              items.add(itemMap(d));
            }
          }
        }
      }

      // 转换为树形结构
      List<Map<String, Object>> tree = new ArrayList<>();
      int idCounter = 0;
      for (Map.Entry<String, Map<String, Object>> s0 : sno0Map.entrySet()) {
        Map<String, Object> s0Node = new LinkedHashMap<>();
        String s0RawLabel = (String) s0.getValue().get("_label");
        String[] parts = s0RawLabel.split(":", 2);
        String s0Label = "<" + parts[0] + ">" + (parts.length > 1 ? parts[1] : "");
        s0Node.put("label", s0Label);
        s0Node.put("id", "s0-" + (++idCounter));
        s0Node.put("disabled", true);

        List<Map<String, Object>> s0Children = new ArrayList<>();
        Map<String, Map<String, Object>> s1Children = (Map<String, Map<String, Object>>) s0.getValue().get("_children");
        for (Map.Entry<String, Map<String, Object>> s1 : s1Children.entrySet()) {
          Map<String, Object> s1Node = new LinkedHashMap<>();
          String s1RawLabel = (String) s1.getValue().get("_label");
          String[] s1Parts = s1RawLabel.split(":", 2);
          String s1Label = "<" + s1Parts[0] + ">" + (s1Parts.length > 1 ? s1Parts[1] : "");
          s1Node.put("label", s1Label);
          s1Node.put("id", "s1-" + (++idCounter));

          if (types == 0) {
            // 理论: s1 disabled=true, 叶子在 sno 层
            s1Node.put("disabled", true);
            List<Map<String, Object>> snoChildren = new ArrayList<>();
            Map<String, Map<String, Object>> snoMap = (Map<String, Map<String, Object>>) s1.getValue().get("_children");
            for (Map.Entry<String, Map<String, Object>> sno : snoMap.entrySet()) {
              Map<String, Object> snoNode = new LinkedHashMap<>();
              snoNode.put("label", sno.getValue().get("_label"));
              snoNode.put("id", "s2-" + (++idCounter));
              snoNode.put("disabled", false);
              snoNode.put("task_id", sno.getValue().get("_task_id"));
              snoNode.put("title", sno.getValue().get("_title"));
              snoNode.put("list", sno.getValue().get("_list"));
              snoChildren.add(snoNode);
            }
            s1Node.put("children", snoChildren);
          } else {
            // 实操: s1 disabled=false, 直接带 list
            s1Node.put("disabled", false);
            s1Node.put("task_id", s1.getValue().get("_task_id"));
            s1Node.put("title", s1.getValue().get("_title"));
            s1Node.put("list", s1.getValue().get("_list"));
          }
          s0Children.add(s1Node);
        }
        s0Node.put("children", s0Children);
        tree.add(s0Node);
      }
      return tree;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "构建细目表树失败: " + e.getMessage());
    }
  }

  private Map<String, Object> itemMap(ExamTaskXimuDetailEntity d) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", d.getId());
    item.put("task_id", d.getTaskId());
    item.put("sno", d.getSno());
    item.put("sno_item", d.getSnoItem());
    item.put("sno_title", d.getSnoTitle());
    item.put("title", d.getTitle());
    return item;
  }

  private LongStream countLeafNodes(Map<String, Object> node) {
    Object childrenObj = node.get("children");
    if (childrenObj instanceof List && !((List<?>) childrenObj).isEmpty()) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
      return children.stream().flatMapToLong(this::countLeafNodes);
    }
    Object listObj = node.get("list");
    if (listObj instanceof List) {
      return LongStream.of(((List<?>) listObj).size());
    }
    return LongStream.empty();
  }

  @Transactional
  public Map<String, Object> submitXimu(ExpertAuditSubmitReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    if (ximuAuditMapper.exists(new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
        .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId.intValue())
        .eq(ExamTaskXimuAuditRecordEntity::getTaskId, req.getTaskId())
        .eq(ExamTaskXimuAuditRecordEntity::getXimuId, req.getId())
        .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, req.getRole()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已提交过");
    }

    ExamTaskXimuAuditRecordEntity record = new ExamTaskXimuAuditRecordEntity();
    record.setTenantId(tenantId.intValue());
    record.setExpertId(userId.intValue());
    record.setAuditRole(req.getRole());
    record.setTaskId(req.getTaskId());
    record.setXimuId(req.getId());
    record.setSno(req.getSno());
    record.setSnoItem(req.getSnoItem());
    record.setSnoTitle(req.getSnoTitle());
    record.setAuditStatus(1);
    record.setYmd(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    record.setAddtime(LocalDateTime.now());
    ximuAuditMapper.insert(record);

    return checkAndComplete("ximu", req.getTaskId(), userId.intValue(), req.getRole(), tenantId.intValue());
  }

  // helpers

  private Map<String, Object> checkAndComplete(String tag, Integer taskId, Integer expertId, Integer role, Integer tenantId) {
    long audited = countAudited(tag, taskId, expertId, role, tenantId);
    long total = countTotal(tag, taskId, tenantId);
    boolean isdone = audited >= total;

    if (isdone) {
      ExamTaskXimuExpertEntity et = expertMapper.selectOne(new LambdaQueryWrapper<ExamTaskXimuExpertEntity>()
          .eq(ExamTaskXimuExpertEntity::getTenantId, tenantId)
          .eq(ExamTaskXimuExpertEntity::getTaskId, taskId)
          .eq(ExamTaskXimuExpertEntity::getExpertId, expertId)
          .eq(ExamTaskXimuExpertEntity::getTaskRole, role)
          .eq(ExamTaskXimuExpertEntity::getTag, tag));
      if (et != null && et.getEndtime() == null) {
        et.setEndtime(LocalDateTime.now());
        expertMapper.updateById(et);
      }

      if (role == 0) {
        updateTaskStatus(tag, taskId, 2);
      } else {
        updateTaskStatus(tag, taskId, 3);
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", audited);
    result.put("total", total);
    result.put("isdone", isdone ? 1 : 0);
    return result;
  }

  private long countAudited(String tag, Integer taskId, Integer expertId, Integer role, Integer tenantId) {
    return switch (tag) {
      case "exam" -> examAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
          .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamAuditRecordEntity::getTaskId, taskId)
          .eq(ExamTaskExamAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
      case "examshicao" -> examshicaoAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
      case "examsub" -> examsubAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
          .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskExamsubAuditRecordEntity::getTaskId, taskId)
          .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
      case "ximu" -> ximuAuditMapper.selectCount(new LambdaQueryWrapper<ExamTaskXimuAuditRecordEntity>()
          .eq(ExamTaskXimuAuditRecordEntity::getTenantId, tenantId)
          .eq(ExamTaskXimuAuditRecordEntity::getTaskId, taskId)
          .eq(ExamTaskXimuAuditRecordEntity::getAuditRole, role)
          .eq(ExamTaskXimuAuditRecordEntity::getAuditStatus, 1));
      default -> 0L;
    };
  }

  private long countTotal(String tag, Integer taskId, Integer tenantId) {
    return switch (tag) {
      case "exam" -> examDetailMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamDetailEntity>()
          .eq(ExamTaskExamDetailEntity::getTenantId, tenantId)
          .eq(ExamTaskExamDetailEntity::getTaskId, taskId));
      case "examshicao" -> examshicaoDetailMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
          .eq(ExamTaskExamshicaoDetailEntity::getTenantId, tenantId)
          .eq(ExamTaskExamshicaoDetailEntity::getTaskId, taskId));
      case "examsub" -> examsubDetailMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamsubDetailEntity>()
          .eq(ExamTaskExamsubDetailEntity::getTenantId, tenantId)
          .eq(ExamTaskExamsubDetailEntity::getTaskId, taskId));
      case "ximu" -> ximuDetailMapper.selectCount(new LambdaQueryWrapper<ExamTaskXimuDetailEntity>()
          .eq(ExamTaskXimuDetailEntity::getTenantId, tenantId)
          .eq(ExamTaskXimuDetailEntity::getTaskId, taskId));
      default -> 0L;
    };
  }

  private void updateTaskStatus(String tag, Integer taskId, Integer newStatus) {
    switch (tag) {
      case "exam" -> {
        ExamTaskExamEntity t = new ExamTaskExamEntity();
        t.setId(taskId);
        t.setStatus(newStatus);
        examMapper.updateById(t);
      }
      case "examshicao" -> {
        ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
        t.setId(taskId);
        t.setStatus(newStatus);
        examshicaoMapper.updateById(t);
      }
      case "examsub" -> {
        ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
        t.setId(taskId);
        t.setStatus(newStatus);
        examsubMapper.updateById(t);
      }
      case "ximu" -> {
        ExamTaskXimuEntity t = new ExamTaskXimuEntity();
        t.setId(taskId);
        t.setStatus(newStatus);
        ximuMapper.updateById(t);
      }
    }
  }

  private void tryAutoCompleteEndTasks() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    List<ExamTaskXimuExpertEntity> inProgress = expertMapper.selectList(
        new LambdaQueryWrapper<ExamTaskXimuExpertEntity>()
            .eq(ExamTaskXimuExpertEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskXimuExpertEntity::getExpertId, userId.intValue())
            .eq(ExamTaskXimuExpertEntity::getTaskRole, 1)
            .isNull(ExamTaskXimuExpertEntity::getEndtime)
            .in(ExamTaskXimuExpertEntity::getTag, List.of("exam", "examshicao", "examsub", "ximu")));
    if (inProgress.isEmpty()) return;

    for (ExamTaskXimuExpertEntity et : inProgress) {
      long total = countTotal(et.getTag(), et.getTaskId(), tenantId.intValue());
      long audited = countAudited(et.getTag(), et.getTaskId(), userId.intValue(), 1, tenantId.intValue());
      if (total > 0 && audited >= total) {
        et.setEndtime(LocalDateTime.now());
        expertMapper.updateById(et);
        updateTaskStatus(et.getTag(), et.getTaskId(), 3);
        log.info("Auto-completed end audit: tag={}, taskId={}", et.getTag(), et.getTaskId());
      }
    }
  }

  private String levelName(int level) {
    return level >= 1 && level < LEVELS.length ? LEVELS[level] : "-";
  }
}

package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
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
import com.guoyu.platform.exam.mapper.ExamTaskXimuMapper;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
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
  private final ExamTaskExpertMapper taskExpertMapper;
  private final TaskAuditAssignHelper helper;
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
      ExamTaskExpertMapper taskExpertMapper,
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
      ObjectMapper objectMapper,
      TaskAuditAssignHelper helper
  ) {
    this.objectMapper = objectMapper;
    this.taskExpertMapper = taskExpertMapper;
    this.helper = helper;
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
  public PageResponse<ExpertTaskVO> pageStartTasks(ExpertTaskQueryReq req, String taskType) {
    return pageTasks(req, 0, taskType);
  }

  @Transactional
  public PageResponse<ExpertTaskVO> pageEndTasks(ExpertTaskQueryReq req, String taskType) {
    tryAutoCompleteEndTasks();
    return pageTasks(req, 1, taskType);
  }

  /**
   * 专家指派记录统一载体(exam_task_expert 表):
   *   tag ∈ {ximu, exam, examshicao, examsub}
   */
  private record ExpertRecord(int id, int taskId, String tag,
                              LocalDateTime addtime, LocalDateTime endtime) {}

  private PageResponse<ExpertTaskVO> pageTasks(ExpertTaskQueryReq req, int taskRole, String taskType) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    log.info("expert task list, tenant={}, userId={}, role={}, taskType={}, page={}", tenantId, userId, taskRole, taskType, req.getPage());

    java.util.Set<String> allowedTypes = java.util.Set.of("exam", "examshicao", "examsub", "ximu");
    String filterType = (taskType != null && !taskType.isEmpty() && allowedTypes.contains(taskType)) ? taskType : null;

    int tid = tenantId.intValue();
    int uid = userId.intValue();

    // 从 exam_task_expert 表查询所有符合条件的专家指派记录
    java.util.Set<String> queryTags = (filterType != null)
        ? java.util.Set.of(filterType)
        : java.util.Set.of("exam", "examshicao", "examsub", "ximu");

    var w = new LambdaQueryWrapper<ExamTaskExpertEntity>()
        .eq(ExamTaskExpertEntity::getTenantId, tid)
        .eq(ExamTaskExpertEntity::getExpertId, uid)
        .eq(ExamTaskExpertEntity::getTaskRole, taskRole)
        .in(ExamTaskExpertEntity::getTag, queryTags);
    if (req.getStatus() != null && req.getStatus() > 0) {
      if (req.getStatus() == 1) w.isNull(ExamTaskExpertEntity::getEndtime);
      else if (req.getStatus() == 2) w.isNotNull(ExamTaskExpertEntity::getEndtime);
    }

    List<ExpertRecord> all = new ArrayList<>();
    for (ExamTaskExpertEntity e : taskExpertMapper.selectList(w)) {
      all.add(new ExpertRecord(e.getId(), e.getTaskId(), e.getTag(), e.getAddtime(), e.getEndtime()));
    }

    // ── 排序 + 内存分页 ──
    all.sort((a, b) -> {
      if (a.addtime == null && b.addtime == null) return 0;
      if (a.addtime == null) return 1;
      if (b.addtime == null) return -1;
      return b.addtime.compareTo(a.addtime); // DESC
    });
    long total = all.size();
    int page = (int) req.getPage();
    int pageSize = (int) req.getPageSize();
    int from = (page - 1) * pageSize;
    int to = Math.min(from + pageSize, all.size());
    List<ExpertRecord> pageRecords = (from >= all.size()) ? List.of() : all.subList(from, to);

    log.info(">>> expert tasks: tenantId={}, userId={}, role={}, filterType={}, total={}, pageRecords={}",
        tid, uid, taskRole, filterType, total, pageRecords.size());

    if (pageRecords.isEmpty()) {
      return new PageResponse<>(List.of(), page, pageSize, total);
    }

    // 批量加载工种名称
    Set<Integer> workTypeIds = new java.util.HashSet<>();
    for (ExpertRecord r : pageRecords) {
      Integer wtId = resolveWorkTypeId(r.tag(), r.taskId());
      if (wtId != null) workTypeIds.add(wtId);
    }
    Map<Integer, String> wtNames = helper.loadWorkTypeNames(workTypeIds);

    List<ExpertTaskVO> vos = new ArrayList<>();
    for (ExpertRecord r : pageRecords) {
      ExpertTaskVO vo = new ExpertTaskVO();
      vo.setId(r.id());
      vo.setTaskId(r.taskId());
      vo.setTag(r.tag());
      vo.setTagCn(TAG_CN.getOrDefault(r.tag(), r.tag()));
      vo.setAddtimeCn(r.addtime() != null ? r.addtime().format(DATETIME_FMT) : "-");
      vo.setEndtimeCn(r.endtime() != null ? r.endtime().format(DATETIME_FMT) : "-");
      vo.setEndtime(r.endtime() != null ? 1L : 0L);
      vo.setStatus(r.endtime() != null ? "完成" : "进行中");

      resolveTaskInfo(vo, r.tag(), r.taskId(), uid, taskRole, wtNames);
      vos.add(vo);
    }
    return new PageResponse<>(vos, (long) page, (long) pageSize, total);
  }

  /**
   * 快速取 workTypeId(仅用于批量加载字典,不做其他逻辑)。
   */
  private Integer resolveWorkTypeId(String tag, Integer taskId) {
    try {
      return switch (tag) {
        case "ximu" -> { var t = ximuMapper.selectById(taskId); yield t != null ? t.getWorkTypeId() : null; }
        case "exam" -> { var t = examMapper.selectById(taskId); yield t != null ? t.getWorkTypeId() : null; }
        case "examshicao" -> { var t = examshicaoMapper.selectById(taskId); yield t != null ? t.getWorkTypeId() : null; }
        case "examsub" -> { var t = examsubMapper.selectById(taskId); yield t != null ? t.getWorkTypeId() : null; }
        default -> null;
      };
    } catch (Exception e) {
      return null;
    }
  }

  private void resolveTaskInfo(ExpertTaskVO vo, String tag, Integer taskId,
                               Integer expertId, Integer role,
                               Map<Integer, String> wtNames) {
    try {
      switch (tag) {
        case "ximu" -> {
          ExamTaskXimuEntity t = ximuMapper.selectById(taskId);
          if (t != null) {
            String wtCn = wtNames.getOrDefault(t.getWorkTypeId(), "-");
            vo.setTitle(wtCn + "-" + levelName(t.getLevel()) + "-" + (t.getTypes() > 0 ? "实操" : "理论"));
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
            String wtCn = wtNames.getOrDefault(t.getWorkTypeId(), "-");
            vo.setTitle(wtCn + "-" + levelName(t.getLevel()));
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
            String wtCn = wtNames.getOrDefault(t.getWorkTypeId(), "-");
            vo.setTitle(wtCn + "-" + levelName(t.getLevel()));
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
            String wtCn = wtNames.getOrDefault(t.getWorkTypeId(), "-");
            vo.setTitle(wtCn + "-" + levelName(t.getLevel()));
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
    record.setAddtime((int) (System.currentTimeMillis() / 1000));
    examAuditMapper.insert(record);

    return checkAndComplete("exam", req.getTaskId(), userId.intValue(), req.getRole(), tenantId.intValue());
  }

  // examshicao

  @Transactional(readOnly = true)
  public Map<String, Object> getExamshicaoList(Integer taskId, Integer role) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    int tid = tenantId.intValue();
    int uid = userId.intValue();

    // 1) task info: id, status
    ExamTaskExamshicaoEntity task = examshicaoMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务不存在");
    }
    Map<String, Object> taskExam = new LinkedHashMap<>();
    taskExam.put("id", task.getId());
    taskExam.put("status", task.getStatus());

    // 2) auditcount - 已审题数
    long auditcount = examshicaoAuditMapper.selectCount(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tid)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getExpertId, uid)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));

    // 3) 构建查询条件
    LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity> dw = new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
        .eq(ExamTaskExamshicaoDetailEntity::getTenantId, tid)
        .eq(ExamTaskExamshicaoDetailEntity::getTaskId, taskId);

    // 排除当前专家已审的题
    List<ExamTaskExamshicaoAuditRecordEntity> auditedRecords = examshicaoAuditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tid)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getExpertId, uid)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, role)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1)
            .select(ExamTaskExamshicaoAuditRecordEntity::getExamId));
    List<Integer> auditedIds = auditedRecords.stream()
        .map(ExamTaskExamshicaoAuditRecordEntity::getExamId).distinct().collect(Collectors.toList());
    if (!auditedIds.isEmpty()) {
      dw.notIn(ExamTaskExamshicaoDetailEntity::getId, auditedIds);
    }

    // 终审(role>0)且初审未完成(status==1): 只能获取初审已审的题
    Map<Integer, ExamTaskExamshicaoAuditRecordEntity> startsMap = new LinkedHashMap<>();
    if (role > 0 && task.getStatus() != null && task.getStatus() == 1) {
      List<ExamTaskExamshicaoAuditRecordEntity> startAudits = examshicaoAuditMapper.selectList(
          new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
              .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tid)
              .eq(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskId)
              .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, 0)
              .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
      List<Integer> startIds = startAudits.stream()
          .map(ExamTaskExamshicaoAuditRecordEntity::getExamId).distinct().collect(Collectors.toList());
      for (ExamTaskExamshicaoAuditRecordEntity a : startAudits) {
        startsMap.putIfAbsent(a.getExamId(), a);
      }
      if (startIds.isEmpty()) {
        // 初审还没审完任何题，返回空列表
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskExam", taskExam);
        result.put("list", List.of());
        result.put("auditcount", auditcount);
        return result;
      }
      dw.in(ExamTaskExamshicaoDetailEntity::getId, startIds);
    }

    // 4) 查询 detail 列表
    List<ExamTaskExamshicaoDetailEntity> details = examshicaoDetailMapper.selectList(dw);

    // 5) 构建返回 list，解析 pingfen JSON
    List<Map<String, Object>> list = new ArrayList<>();
    for (ExamTaskExamshicaoDetailEntity d : details) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId());
      m.put("task_id", d.getTaskId());
      m.put("jianding", d.getJianding());
      m.put("content", d.getContent());
      m.put("pingfen", parsePingfen(d.getPingfen()));
      m.put("kaochang", d.getKaochang());
      m.put("kaosheng", d.getKaosheng());

      // 终审:用初审数据覆盖 content/pingfen/kaochang/kaosheng
      if (role > 0 && startsMap.containsKey(d.getId())) {
        ExamTaskExamshicaoAuditRecordEntity sa = startsMap.get(d.getId());
        m.put("content", sa.getContent());
        m.put("pingfen", parsePingfen(sa.getPingfen()));
        m.put("kaochang", sa.getKaochang());
        m.put("kaosheng", sa.getKaosheng());
      }
      list.add(m);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("taskExam", taskExam);
    result.put("list", list);
    result.put("auditcount", auditcount);
    return result;
  }

  /** 解析 pingfen JSON 字符串为 List<Map>，失败返回空列表 */
  private Object parsePingfen(String pingfenStr) {
    if (pingfenStr == null || pingfenStr.isBlank()) return List.of();
    try {
      return objectMapper.readValue(pingfenStr, new TypeReference<List<Map<String, String>>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse pingfen JSON: {}", e.getMessage());
      return List.of();
    }
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
    record.setAddtime((int) (System.currentTimeMillis() / 1000));
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
    record.setAddtime((int) (System.currentTimeMillis() / 1000));
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
      ExamTaskExpertEntity et = taskExpertMapper.selectOne(new LambdaQueryWrapper<ExamTaskExpertEntity>()
          .eq(ExamTaskExpertEntity::getTenantId, tenantId)
          .eq(ExamTaskExpertEntity::getTaskId, taskId)
          .eq(ExamTaskExpertEntity::getExpertId, expertId)
          .eq(ExamTaskExpertEntity::getTaskRole, role)
          .eq(ExamTaskExpertEntity::getTag, tag));
      if (et != null && et.getEndtime() == null) {
        et.setEndtime(LocalDateTime.now());
        taskExpertMapper.updateById(et);
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
    int tid = tenantId.intValue();
    int uid = userId.intValue();

    // 从 exam_task_expert 表收集所有进行中的终审指派记录(tag ∈ {ximu, exam, examshicao, examsub})
    List<ExpertRecord> inProgress = new ArrayList<>();
    for (ExamTaskExpertEntity e : taskExpertMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExpertEntity>()
            .eq(ExamTaskExpertEntity::getTenantId, tid)
            .eq(ExamTaskExpertEntity::getExpertId, uid)
            .eq(ExamTaskExpertEntity::getTaskRole, 1)
            .isNull(ExamTaskExpertEntity::getEndtime)
            .in(ExamTaskExpertEntity::getTag, List.of("ximu", "exam", "examshicao", "examsub")))) {
      inProgress.add(new ExpertRecord(e.getId(), e.getTaskId(), e.getTag(), e.getAddtime(), e.getEndtime()));
    }
    if (inProgress.isEmpty()) return;

    for (ExpertRecord er : inProgress) {
      long total = countTotal(er.tag(), er.taskId(), tid);
      long audited = countAudited(er.tag(), er.taskId(), uid, 1, tid);
      if (total > 0 && audited >= total) {
        ExamTaskExpertEntity et = taskExpertMapper.selectById(er.id());
        if (et != null && et.getEndtime() == null) {
          et.setEndtime(LocalDateTime.now());
          taskExpertMapper.updateById(et);
        }
        updateTaskStatus(er.tag(), er.taskId(), 3);
        log.info("Auto-completed end audit: tag={}, taskId={}", er.tag(), er.taskId());
      }
    }
  }

  private String levelName(int level) {
    return level >= 1 && level < LEVELS.length ? LEVELS[level] : "-";
  }
}

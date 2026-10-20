package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 真题共享组解析（方案 v2.1 §3/§6，Q1~Q7 已拍板）。
 *
 * <p>scope 的唯一来源是 train_exam_work_type_list（不对 train_exam_list 加列）；
 * 过滤空值/失效工种后按 (work_type_id, level) 去重；无有效 scope 返回空列表，由调用方降级 exam_id。
 * 不加缓存（Q7）：查询按 exam_id 走小表，绑定变更立即生效。
 *
 * <p>租户口径：Mapper 查询依赖 MyBatis-Plus 租户拦截器（HTTP 线程自动、Worker 线程自行 set），
 * 本类不触碰 SecurityContext；JdbcTemplate 计数显式传 tenantId。
 */
@Service
public class ZhentiScopeResolver {

  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final JdbcTemplate jdbcTemplate;
  private final boolean scopeEnabled;

  public ZhentiScopeResolver(
      TrainExamWorkTypeListMapper workTypeListMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      JdbcTemplate jdbcTemplate,
      @Value("${platform.zhenti.scope-enabled:true}") boolean scopeEnabled) {
    this.workTypeListMapper = workTypeListMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.scopeEnabled = scopeEnabled;
  }

  public boolean scopeEnabled() {
    return scopeEnabled;
  }

  /**
   * 计划的全部有效共享组；无有效绑定时返回空列表（降级判定依据，方案 §3.3）。
   */
  public List<ZhentiScope> resolve(Long examId) {
    if (!scopeEnabled || examId == null) {
      return List.of();
    }
    List<TrainExamWorkTypeListEntity> bindings = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .eq(TrainExamWorkTypeListEntity::getExamId, examId)
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    Map<String, ZhentiScope> dedup = new LinkedHashMap<>();
    for (TrainExamWorkTypeListEntity binding : bindings) {
      if (binding.getWorkTypeId() == null) {
        continue;
      }
      Integer level = ZhentiLevel.parseLevel(binding.getLevel());
      if (level == null) {
        continue;
      }
      ExamWorkTypeEntity workType = examWorkTypeMapper.selectById(binding.getWorkTypeId());
      if (workType == null || workType.getDeleted() == null || workType.getDeleted() != 0) {
        continue;
      }
      dedup.putIfAbsent(binding.getWorkTypeId() + ":" + level,
          new ZhentiScope(binding.getWorkTypeId(), level));
    }
    return List.copyOf(dedup.values());
  }

  /**
   * 上传口径（方案 §3.4）：0 个 scope → 私有（null）；1 个 → 默认；N 个 → 必须显式选择且属于该计划。
   */
  public ZhentiScope resolveForUpload(Long examId, Long scopeWorkTypeId, Integer scopeLevel) {
    List<ZhentiScope> scopes = resolve(examId);
    if (scopes.isEmpty()) {
      return null;
    }
    if (scopes.size() == 1) {
      return scopes.get(0);
    }
    if (scopeWorkTypeId == null || scopeLevel == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "该考试计划绑定了多个工种/等级，请先选择共享范围");
    }
    ZhentiScope selected = new ZhentiScope(scopeWorkTypeId, scopeLevel);
    if (!scopes.contains(selected)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "所选共享范围与考试计划绑定的工种/等级不一致");
    }
    return selected;
  }

  /**
   * 每个共享组下的活跃计划数（共享提示条用）。必须 join 活跃 train_exam_list，排除孤儿绑定。
   *
   * @return key=ZhentiScope，value=计划数；scopes 为空返回空 Map
   */
  public Map<ZhentiScope, Long> scopePlanCounts(Long tenantId, List<ZhentiScope> scopes) {
    if (scopes.isEmpty()) {
      return Map.of();
    }
    ScopeCountQuery query = buildScopePlanCountQuery(scopes);
    Object[] args = new Object[query.args().length + 1];
    args[0] = tenantId;
    System.arraycopy(query.args(), 0, args, 1, query.args().length);
    Map<ZhentiScope, Long> counts = new LinkedHashMap<>();
    for (Map<String, Object> row : jdbcTemplate.queryForList(query.sql(), args)) {
      Integer level = ZhentiLevel.parseLevel(row.get("level"));
      if (level == null) {
        continue;
      }
      long workTypeId = ((Number) row.get("work_type_id")).longValue();
      long cnt = ((Number) row.get("cnt")).longValue();
      counts.put(new ZhentiScope(workTypeId, level), cnt);
    }
    return counts;
  }

  /** 计数 SQL（纯函数，tenantId 由调用方前置为第一个参数）。 */
  static ScopeCountQuery buildScopePlanCountQuery(List<ZhentiScope> scopes) {
    StringBuilder sql = new StringBuilder(
        "SELECT w.work_type_id, w.level, COUNT(DISTINCT w.exam_id) AS cnt"
            + " FROM train_exam_work_type_list w"
            + " JOIN train_exam_list e ON e.id = w.exam_id AND e.deleted = 0"
            + " WHERE w.deleted = 0 AND w.tenant_id = ? AND (");
    List<Object> args = new ArrayList<>();
    for (int i = 0; i < scopes.size(); i++) {
      if (i > 0) {
        sql.append(" OR ");
      }
      sql.append("(w.work_type_id = ? AND w.level = ?)");
      args.add(scopes.get(i).workTypeId());
      args.add(scopes.get(i).level());
    }
    sql.append(") GROUP BY w.work_type_id, w.level");
    return new ScopeCountQuery(sql.toString(), args.toArray());
  }

  record ScopeCountQuery(String sql, Object[] args) {
  }
}

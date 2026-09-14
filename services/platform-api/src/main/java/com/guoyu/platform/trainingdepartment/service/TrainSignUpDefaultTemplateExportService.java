package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.SignUpDefaultTemplateExportCheckReq;
import com.guoyu.platform.trainingdepartment.dto.SignUpDefaultTemplateExportReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfTaskEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfTaskMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleDefaultTemplatePackService;
import com.guoyu.platform.trainingdepartment.support.SignUpSelfFilesResolver;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportCheckVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportStartVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** 默认模板异步导出（对齐 Yunxinxi peopleselfexport / peopleselfexportcheck） */
@Service
@Slf4j
public class TrainSignUpDefaultTemplateExportService {

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final TrainSignUpSelfTaskMapper taskMapper;
  private final OrgMapper orgMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final SignUpPeopleDefaultTemplatePackService packService;
  private final SignUpSelfFilesResolver filesResolver;
  private final ObjectMapper objectMapper;
  private final Executor signUpExportExecutor;
  private final String cdnPath;

  public TrainSignUpDefaultTemplateExportService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      TrainSignUpSelfTaskMapper taskMapper,
      OrgMapper orgMapper,
      TrainingOperatorScopeService operatorScopeService,
      SignUpPeopleDefaultTemplatePackService packService,
      SignUpSelfFilesResolver filesResolver,
      ObjectMapper objectMapper,
      @Qualifier("signUpExportExecutor") Executor signUpExportExecutor,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.taskMapper = taskMapper;
    this.orgMapper = orgMapper;
    this.operatorScopeService = operatorScopeService;
    this.packService = packService;
    this.filesResolver = filesResolver;
    this.objectMapper = objectMapper;
    this.signUpExportExecutor = signUpExportExecutor;
    this.cdnPath = cdnPath;
  }

  /** 卡住超过该秒数的 endtime=0 任务视为失败，避免永久阻塞新导出 */
  private static final long STUCK_TASK_TIMEOUT_SECONDS = 30 * 60;

  @Transactional
  public SignUpDefaultTemplateExportStartVO startExport(SignUpDefaultTemplateExportReq req) {
    TrainSignUpSelfEntity signUp = getSignUp(req.getSignUpId());
    Long tenantId = SecurityContext.currentUser().getTenantId();

    TrainSignUpSelfTaskEntity running = taskMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfTaskEntity>()
        .eq(TrainSignUpSelfTaskEntity::getEndtime, 0L)
        .orderByDesc(TrainSignUpSelfTaskEntity::getId)
        .last("LIMIT 1"));
    if (running != null) {
      if (isStuckTask(running)) {
        failTask(running, "导出任务超时未完成，已自动取消，请重试");
        running = null;
      } else if (running.getSignUpId().equals(signUp.getId())) {
        // 同计划已有进行中任务：若从未推进进度，可能是事务未提交就启动了异步线程，补提一次
        if (readIdList(running.getIdsOut()).isEmpty()) {
          submitExportAfterCommit(running.getId(), tenantId);
        }
        return new SignUpDefaultTemplateExportStartVO(running.getId(), progressOf(running));
      } else {
        throw new BusinessException(ErrorCode.CONFLICT, "有未完成的任务在进行中，无法添加任务");
      }
    }

    List<Long> peopleIds = resolvePeopleIds(signUp.getId(), tenantId, req.getIds(), req.getStatus());
    if (peopleIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的人员");
    }

    TrainSignUpSelfTaskEntity task = new TrainSignUpSelfTaskEntity();
    task.setSignUpId(signUp.getId());
    task.setTenantId(tenantId);
    task.setAddtime(System.currentTimeMillis() / 1000);
    task.setEndtime(0L);
    try {
      task.setIdsIn(objectMapper.writeValueAsString(peopleIds));
      task.setIdsOut(objectMapper.writeValueAsString(List.of()));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建导出任务失败");
    }
    taskMapper.insert(task);
    submitExportAfterCommit(task.getId(), tenantId);
    return new SignUpDefaultTemplateExportStartVO(task.getId(), progressOf(task));
  }

  private boolean isStuckTask(TrainSignUpSelfTaskEntity task) {
    if (task.getAddtime() == null) {
      return true;
    }
    long age = System.currentTimeMillis() / 1000 - task.getAddtime();
    return age >= STUCK_TASK_TIMEOUT_SECONDS;
  }

  private void submitExportAfterCommit(Long taskId, Long tenantIdForJob) {
    Runnable job = () -> {
      try {
        TenantContextHolder.setTenantId(tenantIdForJob);
        runExport(taskId);
      } finally {
        TenantContextHolder.clear();
      }
    };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          signUpExportExecutor.execute(job);
        }
      });
    } else {
      signUpExportExecutor.execute(job);
    }
  }

  @Transactional(readOnly = true)
  public SignUpDefaultTemplateExportCheckVO checkExport(SignUpDefaultTemplateExportCheckReq req) {
    TrainSignUpSelfEntity signUp = getSignUp(req.getSignUpId());
    TrainSignUpSelfTaskEntity task = taskMapper.selectById(req.getTaskId());
    if (task == null || !signUp.getId().equals(task.getSignUpId())) {
      return new SignUpDefaultTemplateExportCheckVO(null, null, 2, "未知的任务");
    }

    String jindu = progressOf(task);
    if (task.getEndtime() != null && task.getEndtime() > 0 && StringUtils.hasText(task.getErrorMsg())) {
      return new SignUpDefaultTemplateExportCheckVO(null, jindu, 1, task.getErrorMsg());
    }

    String folderName = sanitizeFolderName(
        StringUtils.hasText(req.getSignUpTitle()) ? req.getSignUpTitle() : signUp.getTitle());
    ResolvedZip resolved = resolveExportZip(signUp, folderName);
    if (resolved != null && task.getEndtime() != null && task.getEndtime() > 0) {
      return new SignUpDefaultTemplateExportCheckVO(
          filesResolver.publicUrl(resolved.relativePath()), jindu, null, "成功");
    }
    if (task.getEndtime() != null && task.getEndtime() > 0) {
      // 任务已结束且无 errorMsg：多为打包成功但 check 侧找不到 zip（CDN 路径/标题不一致）
      String hint = "导出完成但未找到压缩包。cdn=" + cdnPath
          + "，期望=down/" + folderName + ".zip";
      log.warn("默认模板导出 check 未找到 zip taskId={} signUpId={} {}",
          task.getId(), signUp.getId(), hint);
      return new SignUpDefaultTemplateExportCheckVO(null, jindu, 1, hint);
    }
    return new SignUpDefaultTemplateExportCheckVO(null, jindu, null, "当前进度");
  }

  /**
   * 按标题 + signUp.files 回退定位 zip，避免仅因标题瞬时不一致导致「导出失败」。
   */
  private ResolvedZip resolveExportZip(TrainSignUpSelfEntity signUp, String folderName) {
    List<String> relativeCandidates = new ArrayList<>();
    relativeCandidates.add("down/" + folderName + ".zip");
    for (String stored : readFilePaths(signUp.getFiles())) {
      if (!StringUtils.hasText(stored)) {
        continue;
      }
      String normalized = stored.startsWith("/") ? stored.substring(1) : stored;
      if (normalized.toLowerCase().endsWith(".zip") && !relativeCandidates.contains(normalized)) {
        relativeCandidates.add(normalized);
      }
    }
    for (String relative : relativeCandidates) {
      Path absolute = Paths.get(cdnPath).resolve(relative).toAbsolutePath().normalize();
      if (Files.exists(absolute) && Files.isRegularFile(absolute)) {
        return new ResolvedZip(relative.replace('\\', '/'), absolute);
      }
    }
    return null;
  }

  private List<String> readFilePaths(String filesJson) {
    if (!StringUtils.hasText(filesJson)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(filesJson, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private record ResolvedZip(String relativePath, Path absolutePath) {}

  public void runExport(Long taskId) {
    TrainSignUpSelfTaskEntity task = taskMapper.selectById(taskId);
    if (task == null || task.getEndtime() != null && task.getEndtime() > 0) {
      return;
    }
    try {
      TrainSignUpSelfEntity signUp = signUpMapper.selectById(task.getSignUpId());
      if (signUp == null) {
        failTask(task, "报名计划不存在");
        return;
      }
      OrgEntity company = signUp.getOrgId() != null ? orgMapper.selectById(signUp.getOrgId()) : null;
      List<Long> ids = readIdList(task.getIdsIn());
      List<TrainSignUpPeopleSelfEntity> people = ids.isEmpty()
          ? List.of()
          : peopleMapper.selectList(new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .in(TrainSignUpPeopleSelfEntity::getId, ids)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
              .orderByAsc(TrainSignUpPeopleSelfEntity::getId));

      List<Long> doneIds = new ArrayList<>();
      String zipRelative = packService.run(signUp, company, people, (done, total) -> {
        if (done <= people.size()) {
          doneIds.clear();
          doneIds.addAll(ids.subList(0, Math.min(done, ids.size())));
          updateTaskProgress(task.getId(), doneIds);
        }
      });

      Path zipAbsolute = Paths.get(cdnPath).resolve(zipRelative).toAbsolutePath().normalize();
      if (!Files.exists(zipAbsolute) || !Files.isRegularFile(zipAbsolute)) {
        failTask(task, "打包完成但压缩包未生成: " + zipAbsolute);
        return;
      }

      signUp.setFiles(filesResolver.appendFilePath(signUp, zipRelative.replace('\\', '/')));
      signUp.setUpdateTime(LocalDateTime.now());
      signUpMapper.updateById(signUp);

      task.setIdsOut(objectMapper.writeValueAsString(ids));
      task.setEndtime(System.currentTimeMillis() / 1000);
      task.setErrorMsg(null);
      taskMapper.updateById(task);
    } catch (Exception e) {
      log.error("默认模板导出失败 taskId={}", taskId, e);
      failTask(task, e.getMessage() != null ? e.getMessage() : "导出失败");
    }
  }

  private void updateTaskProgress(Long taskId, List<Long> doneIds) {
    try {
      TrainSignUpSelfTaskEntity task = taskMapper.selectById(taskId);
      if (task == null) {
        return;
      }
      task.setIdsOut(objectMapper.writeValueAsString(doneIds));
      taskMapper.updateById(task);
    } catch (Exception e) {
      log.warn("更新导出进度失败 taskId={}", taskId, e);
    }
  }

  private void failTask(TrainSignUpSelfTaskEntity task, String message) {
    task.setEndtime(System.currentTimeMillis() / 1000);
    task.setErrorMsg(message);
    taskMapper.updateById(task);
  }

  private List<Long> resolvePeopleIds(Long signUpId, Long tenantId, List<Long> ids, Integer status) {
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
        .orderByAsc(TrainSignUpPeopleSelfEntity::getId);
    if (ids != null && !ids.isEmpty()) {
      wrapper.in(TrainSignUpPeopleSelfEntity::getId, ids);
    }
    if (status != null) {
      wrapper.eq(TrainSignUpPeopleSelfEntity::getStatus, status);
    }
    return peopleMapper.selectList(wrapper).stream().map(TrainSignUpPeopleSelfEntity::getId).toList();
  }

  private List<Long> readIdList(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private String progressOf(TrainSignUpSelfTaskEntity task) {
    List<Long> all = readIdList(task.getIdsIn());
    List<Long> done = readIdList(task.getIdsOut());
    return done.size() + "/" + all.size();
  }

  private TrainSignUpSelfEntity getSignUp(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = signUpMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名计划不存在");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
    }
    return entity;
  }

  private static String sanitizeFolderName(String title) {
    if (!StringUtils.hasText(title)) {
      return "sign-up-export";
    }
    return title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}

package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.trainingdepartment.support.SignUpDefaultTemplateExportPaths;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleDefaultTemplatePackService;
import com.guoyu.platform.trainingdepartment.support.SignUpSelfFilesResolver;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportCheckVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportStartVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
  private final FileApplicationService fileApplicationService;
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
      FileApplicationService fileApplicationService,
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
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
    this.signUpExportExecutor = signUpExportExecutor;
    this.cdnPath = cdnPath;
  }

  /** 卡住超过该秒数的 endtime=0 任务视为失败，避免永久阻塞新导出 */
  private static final long STUCK_TASK_TIMEOUT_SECONDS = 30 * 60;

  /** 任务已完成后 zip 尚未可见时继续当进行中，避免前端立刻停轮询（落盘/COS 可见性延迟） */
  private static final long ZIP_VISIBLE_GRACE_SECONDS = 90;

  /** 同一 taskId 禁止并行跑两份打包（连点/复用补提会导致后一份删掉成功包） */
  private final Set<Long> runningExportTaskIds = ConcurrentHashMap.newKeySet();

  @Transactional
  public SignUpDefaultTemplateExportStartVO startExport(SignUpDefaultTemplateExportReq req) {
    TrainSignUpSelfEntity signUp = getSignUp(req.getSignUpId());
    Long tenantId = SecurityContext.currentUser().getTenantId();

    List<Long> peopleIds = resolvePeopleIds(signUp.getId(), tenantId, req.getIds(), req.getStatus());
    if (peopleIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的人员");
    }

    TrainSignUpSelfTaskEntity running = taskMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfTaskEntity>()
        .eq(TrainSignUpSelfTaskEntity::getEndtime, 0L)
        .orderByDesc(TrainSignUpSelfTaskEntity::getId)
        .last("LIMIT 1"));
    if (running != null) {
      if (isStuckTask(running)) {
        failTask(running, "导出任务超时未完成，已自动取消，请重试");
        running = null;
      } else if (running.getSignUpId().equals(signUp.getId())) {
        List<Long> runningIds = readIdList(running.getIdsIn());
        if (sameIdSet(runningIds, peopleIds)) {
          // 同计划、同人员：只复用，禁止再 submit（否则双异步会删掉成功 zip）
          return new SignUpDefaultTemplateExportStartVO(running.getId(), progressOf(running));
        }
        // 勾选人员已变：取消旧任务，按新勾选重新导出（否则会继续打旧包）
        failTask(running, "已被新的导出请求取消");
      } else {
        throw new BusinessException(ErrorCode.CONFLICT, "有未完成的任务在进行中，无法添加任务");
      }
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

    if (task.getEndtime() != null && task.getEndtime() > 0
        && SignUpDefaultTemplateExportPaths.isStoredDownloadUrl(task.getResultFile())) {
      String url = task.getResultFile().trim();
      if (!url.contains("?")) {
        url = url + "?v=" + task.getId();
      }
      return new SignUpDefaultTemplateExportCheckVO(url, jindu, null, "成功");
    }

    String folderName = SignUpDefaultTemplateExportPaths.sanitizeTitle(
        StringUtils.hasText(req.getSignUpTitle()) ? req.getSignUpTitle() : signUp.getTitle());
    ResolvedZip resolved = resolveExportZip(signUp, folderName, task);
    if (resolved != null && task.getEndtime() != null && task.getEndtime() > 0) {
      // 带任务号防缓存：同标题多次导出 URL 必须不同，否则浏览器/CDN 会下到旧包
      String url = filesResolver.publicUrl(resolved.relativePath()) + "?v=" + task.getId();
      return new SignUpDefaultTemplateExportCheckVO(url, jindu, null, "成功");
    }
    if (task.getEndtime() != null && task.getEndtime() > 0) {
      long ageSeconds = System.currentTimeMillis() / 1000 - task.getEndtime();
      if (ageSeconds < ZIP_VISIBLE_GRACE_SECONDS) {
        log.info("默认模板导出 check 等待产物可见 taskId={} age={}s resultFile={}",
            task.getId(), ageSeconds, task.getResultFile());
        return new SignUpDefaultTemplateExportCheckVO(null, jindu, null, "当前进度");
      }
      String expectedRel = StringUtils.hasText(task.getResultFile())
          ? task.getResultFile()
          : SignUpDefaultTemplateExportPaths.taskZipRelative(folderName, task.getId());
      Path expectedAbs = SignUpDefaultTemplateExportPaths.isStoredDownloadUrl(expectedRel)
          ? null
          : SignUpDefaultTemplateExportPaths.zipAbsolute(cdnPath, expectedRel);
      Path downDir = SignUpDefaultTemplateExportPaths.downDir(cdnPath);
      String hint = "导出完成但未找到压缩包。已查找="
          + (expectedAbs != null ? expectedAbs.toString() : expectedRel)
          + "（down目录" + (Files.isDirectory(downDir) ? "存在" : "不存在") + "）";
      log.warn("默认模板导出 check 未找到 zip taskId={} signUpId={} resultFile={} {}",
          task.getId(), signUp.getId(), task.getResultFile(), hint);
      return new SignUpDefaultTemplateExportCheckVO(null, jindu, 1, hint);
    }
    return new SignUpDefaultTemplateExportCheckVO(null, jindu, null, "当前进度");
  }

  /**
   * 先扫 {cdn}/down/*_t{taskId}.zip，再按 result_file / 标题拼路径。
   * 禁止回退到无 taskId 的历史同名包。
   */
  private ResolvedZip resolveExportZip(
      TrainSignUpSelfEntity signUp, String folderName, TrainSignUpSelfTaskEntity task) {
    Long taskId = task != null ? task.getId() : null;
    if (taskId == null || taskId <= 0) {
      return null;
    }
    Path scanned = SignUpDefaultTemplateExportPaths.findZipByTaskId(cdnPath, taskId);
    if (scanned != null && Files.isRegularFile(scanned)) {
      return new ResolvedZip("down/" + scanned.getFileName(), scanned);
    }
    List<String> relativeCandidates = new ArrayList<>();
    if (StringUtils.hasText(task.getResultFile())) {
      relativeCandidates.add(task.getResultFile().replace('\\', '/'));
    }
    String taskZip = SignUpDefaultTemplateExportPaths.taskZipRelative(folderName, taskId);
    if (!relativeCandidates.contains(taskZip)) {
      relativeCandidates.add(taskZip);
    }
    for (String stored : readFilePaths(signUp.getFiles())) {
      if (!SignUpDefaultTemplateExportPaths.endsWithTaskZip(stored, taskId)) {
        continue;
      }
      String normalized = stored.startsWith("/") ? stored.substring(1) : stored.replace('\\', '/');
      if (!relativeCandidates.contains(normalized)) {
        relativeCandidates.add(normalized);
      }
    }
    for (String relative : relativeCandidates) {
      Path absolute = SignUpDefaultTemplateExportPaths.zipAbsolute(cdnPath, relative);
      if (Files.isRegularFile(absolute)) {
        return new ResolvedZip("down/" + absolute.getFileName(), absolute);
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
    if (!runningExportTaskIds.add(taskId)) {
      log.info("默认模板导出跳过重复执行 taskId={}", taskId);
      return;
    }
    try {
      doRunExport(taskId);
    } finally {
      runningExportTaskIds.remove(taskId);
    }
  }

  private void doRunExport(Long taskId) {
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
      String zipRelative = packService.run(signUp, company, people, taskId, (done, total) -> {
        if (done <= people.size()) {
          doneIds.clear();
          doneIds.addAll(ids.subList(0, Math.min(done, ids.size())));
          updateTaskProgress(task.getId(), doneIds);
        }
      }).replace('\\', '/');

      // 导出中途被「新勾选」取消时，仅在失败取消时丢弃 zip；已成功则绝不能删
      TrainSignUpSelfTaskEntity latest = taskMapper.selectById(taskId);
      if (latest == null) {
        return;
      }
      if (latest.getEndtime() != null && latest.getEndtime() > 0) {
        if (StringUtils.hasText(latest.getErrorMsg())) {
          deleteQuietly(SignUpDefaultTemplateExportPaths.zipAbsolute(cdnPath, zipRelative));
          log.info("默认模板导出已取消，丢弃产物 taskId={} zip={}", taskId, zipRelative);
        } else {
          log.info("默认模板导出结束时任务已成功，保留 zip taskId={} zip={}", taskId, zipRelative);
        }
        return;
      }

      Path zipAbsolute = SignUpDefaultTemplateExportPaths.zipAbsolute(cdnPath, zipRelative);
      if (!Files.exists(zipAbsolute) || !Files.isRegularFile(zipAbsolute)) {
        failTask(latest, "打包完成但压缩包未生成: " + zipAbsolute);
        return;
      }

      Long tenantId = latest.getTenantId() != null ? latest.getTenantId() : TenantContextHolder.getTenantId();
      if (tenantId == null) {
        failTask(latest, "上传压缩包失败: 缺少租户");
        return;
      }
      String zipFileName = zipAbsolute.getFileName().toString();
      byte[] zipBytes = Files.readAllBytes(zipAbsolute);
      FileVO stored = fileApplicationService.storeFromBytesForTenant(
          tenantId, zipBytes, zipFileName, "application/zip", "private");
      String downloadPath = SignUpDefaultTemplateExportPaths.fileDownloadPath(stored.id());

      long endSec = System.currentTimeMillis() / 1000;
      int updated = taskMapper.update(null, new LambdaUpdateWrapper<TrainSignUpSelfTaskEntity>()
          .eq(TrainSignUpSelfTaskEntity::getId, taskId)
          .eq(TrainSignUpSelfTaskEntity::getEndtime, 0L)
          .set(TrainSignUpSelfTaskEntity::getIdsOut, objectMapper.writeValueAsString(ids))
          .set(TrainSignUpSelfTaskEntity::getEndtime, endSec)
          .set(TrainSignUpSelfTaskEntity::getErrorMsg, null)
          .set(TrainSignUpSelfTaskEntity::getResultFile, downloadPath));
      if (updated == 0) {
        TrainSignUpSelfTaskEntity after = taskMapper.selectById(taskId);
        if (after != null && StringUtils.hasText(after.getErrorMsg())) {
          deleteQuietly(zipAbsolute);
          log.info("默认模板导出提交成功时已被取消 taskId={} zip={}", taskId, zipRelative);
        } else {
          log.info("默认模板导出提交成功时任务已结束且无错误，保留 zip taskId={} zip={}",
              taskId, zipRelative);
        }
        return;
      }
      log.info("默认模板导出成功 taskId={} zip={} fileId={} download={}",
          taskId, zipRelative, stored.id(), downloadPath);

      // 重新加载后再写 files，避免覆盖其它并发字段
      TrainSignUpSelfEntity signUpLatest = signUpMapper.selectById(signUp.getId());
      if (signUpLatest != null) {
        signUpLatest.setFiles(filesResolver.appendFilePath(signUpLatest, zipRelative));
        signUpLatest.setUpdateTime(LocalDateTime.now());
        signUpMapper.updateById(signUpLatest);
      }
    } catch (Exception e) {
      log.error("默认模板导出失败 taskId={}", taskId, e);
      failTask(task, e.getMessage() != null ? e.getMessage() : "导出失败");
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignored) {
      // ignore
    }
  }

  private void updateTaskProgress(Long taskId, List<Long> doneIds) {
    try {
      TrainSignUpSelfTaskEntity task = taskMapper.selectById(taskId);
      if (task == null || (task.getEndtime() != null && task.getEndtime() > 0)) {
        return;
      }
      task.setIdsOut(objectMapper.writeValueAsString(doneIds));
      taskMapper.update(task, new LambdaQueryWrapper<TrainSignUpSelfTaskEntity>()
          .eq(TrainSignUpSelfTaskEntity::getId, taskId)
          .eq(TrainSignUpSelfTaskEntity::getEndtime, 0L));
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

  private static boolean sameIdSet(List<Long> a, List<Long> b) {
    if (a == null || b == null || a.size() != b.size()) {
      return false;
    }
    Set<Long> left = new HashSet<>(a);
    Set<Long> right = new HashSet<>(b);
    return left.equals(right);
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
}

package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiImportLogEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiImportLogMapper;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportFileResult;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步批量导入 Worker（方案 §2.9）。
 *
 * <p>异步红线：不使用 SecurityContext，tenantId/operatorId 由 HTTP 线程捕获后入参传递；
 * Worker 自行 set/clear TenantContextHolder；进度直接更新 exam_zhenti_import_log，D6 轮询读表。
 */
@Service
@Slf4j
public class ZhentiImportWorker {

  private final ZhentiZipProcessor zipProcessor;
  private final ZhentiStorageService storageService;
  private final ExamZhentiImportLogMapper importLogMapper;
  private final ObjectMapper objectMapper;

  public ZhentiImportWorker(
      ZhentiZipProcessor zipProcessor,
      ZhentiStorageService storageService,
      ExamZhentiImportLogMapper importLogMapper,
      ObjectMapper objectMapper) {
    this.zipProcessor = zipProcessor;
    this.storageService = storageService;
    this.importLogMapper = importLogMapper;
    this.objectMapper = objectMapper;
  }

  @Async("zhentiImportExecutor")
  public void run(
      Long tenantId,
      Long importId,
      Long examId,
      Long operatorId,
      String zipObjectKey,
      int total) {
    TenantContextHolder.setTenantId(tenantId);
    Path temp = null;
    try {
      temp = Files.createTempFile("zhenti-async-" + importId + "-", ".zip");
      try (InputStream in = storageService.open(zipObjectKey)) {
        Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
      }
      List<ZhentiImportFileResult> results = zipProcessor.process(
          temp, tenantId, examId, operatorId,
          finished -> updateProgress(tenantId, importId, finished));
      updateDone(tenantId, importId, total, results);
      log.info("[ZhentiImport] 异步导入完成 importId={} total={}", importId, total);
    } catch (Exception ex) {
      log.warn("[ZhentiImport] 异步导入失败 importId={} err={}", importId, ex.toString(), ex);
      markFailed(tenantId, importId);
    } finally {
      TenantContextHolder.clear();
      if (temp != null) {
        try {
          Files.deleteIfExists(temp);
        } catch (Exception ignored) {
          // 临时文件清理失败不影响业务
        }
      }
      storageService.delete(zipObjectKey);
    }
  }

  private void updateProgress(Long tenantId, Long importId, int finished) {
    importLogMapper.update(null, new LambdaUpdateWrapper<ExamZhentiImportLogEntity>()
        .eq(ExamZhentiImportLogEntity::getTenantId, tenantId)
        .eq(ExamZhentiImportLogEntity::getId, importId)
        .set(ExamZhentiImportLogEntity::getFinished, finished));
  }

  private void updateDone(
      Long tenantId, Long importId, int total, List<ZhentiImportFileResult> results) {
    importLogMapper.update(null, new LambdaUpdateWrapper<ExamZhentiImportLogEntity>()
        .eq(ExamZhentiImportLogEntity::getTenantId, tenantId)
        .eq(ExamZhentiImportLogEntity::getId, importId)
        .set(ExamZhentiImportLogEntity::getFinished, total)
        .set(ExamZhentiImportLogEntity::getStatus, "done")
        .set(ExamZhentiImportLogEntity::getResultsJson, toJson(results)));
  }

  private void markFailed(Long tenantId, Long importId) {
    importLogMapper.update(null, new LambdaUpdateWrapper<ExamZhentiImportLogEntity>()
        .eq(ExamZhentiImportLogEntity::getTenantId, tenantId)
        .eq(ExamZhentiImportLogEntity::getId, importId)
        .set(ExamZhentiImportLogEntity::getStatus, "failed"));
  }

  private String toJson(List<ZhentiImportFileResult> results) {
    try {
      return objectMapper.writeValueAsString(results);
    } catch (JsonProcessingException ex) {
      log.warn("[ZhentiImport] 结果序列化失败: {}", ex.getMessage());
      return "[]";
    }
  }
}

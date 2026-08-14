package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidenceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidencePatchReq;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidenceReviewReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowEvidenceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpEventCode;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogCommand;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowEvidenceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.support.TrainFollowPhotoPackGenerator;
import com.guoyu.platform.trainingdepartment.vo.FollowEvidenceVO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TrainFollowEvidenceApplicationService {

  /** 每课次统一配额：照片 5 张 + 视频 3 段×约 15 秒 */
  public static final int MIN_PHOTOS_PER_SESSION = 5;
  public static final int MIN_VIDEOS_PER_SESSION = 3;
  public static final int VIDEO_DURATION_TARGET_SEC = 15;
  public static final int VIDEO_DURATION_TOLERANCE_SEC = 2;

  /** @deprecated 已统一为 MIN_PHOTOS_PER_SESSION */
  public static final int MIN_THEORY_PHOTOS = MIN_PHOTOS_PER_SESSION;
  /** @deprecated 已统一为 MIN_PHOTOS_PER_SESSION */
  public static final int MIN_PRACTICE_PHOTOS = MIN_PHOTOS_PER_SESSION;

  private final TrainFollowEvidenceMapper evidenceMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainClassRoleApplicationService roleService;
  private final FileApplicationService fileApplicationService;
  private final TrainFollowPhotoPackGenerator photoPackGenerator;
  private final FollowOpLogWriter opLogWriter;
  private final TrainFollowEvidenceApplicationService self;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TrainFollowEvidenceApplicationService(
      TrainFollowEvidenceMapper evidenceMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      TrainScheduleMapper scheduleMapper,
      TrainInfoClassMapper classMapper,
      TrainClassRoleApplicationService roleService,
      FileApplicationService fileApplicationService,
      TrainFollowPhotoPackGenerator photoPackGenerator,
      FollowOpLogWriter opLogWriter,
      @Lazy TrainFollowEvidenceApplicationService self) {
    this.evidenceMapper = evidenceMapper;
    this.photoPackMapper = photoPackMapper;
    this.scheduleMapper = scheduleMapper;
    this.classMapper = classMapper;
    this.roleService = roleService;
    this.fileApplicationService = fileApplicationService;
    this.photoPackGenerator = photoPackGenerator;
    this.opLogWriter = opLogWriter;
    this.self = self;
  }

  public List<FollowEvidenceVO> list(
      Long classId, Long scheduleId, String bizDate, String reviewStatus, String mediaType) {
    requireClass(classId);
    LambdaQueryWrapper<TrainFollowEvidenceEntity> q = new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
        .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
        .eq(TrainFollowEvidenceEntity::getClassId, classId)
        .eq(TrainFollowEvidenceEntity::getDeleted, 0)
        .orderByAsc(TrainFollowEvidenceEntity::getSortNo)
        .orderByDesc(TrainFollowEvidenceEntity::getId);
    if (scheduleId != null) {
      q.eq(TrainFollowEvidenceEntity::getScheduleId, scheduleId);
    }
    if (StringUtils.hasText(bizDate)) {
      q.eq(TrainFollowEvidenceEntity::getBizDate, bizDate.trim());
    }
    if (StringUtils.hasText(reviewStatus)) {
      q.eq(TrainFollowEvidenceEntity::getReviewStatus, reviewStatus.trim());
    }
    if (StringUtils.hasText(mediaType)) {
      q.eq(TrainFollowEvidenceEntity::getMediaType, mediaType.trim());
    }
    List<TrainFollowEvidenceEntity> rows = evidenceMapper.selectList(q);
    Map<Long, TrainScheduleEntity> scheduleMap = loadSchedules(classId);
    return rows.stream().map(e -> toVo(e, scheduleMap.get(e.getScheduleId()))).toList();
  }

  public Map<String, Object> coverage(Long classId) {
    requireClass(classId);
    Map<Long, TrainScheduleEntity> scheduleMap = loadSchedules(classId);
    List<TrainFollowEvidenceEntity> all = evidenceMapper.selectList(
        new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
            .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
            .eq(TrainFollowEvidenceEntity::getClassId, classId)
            .eq(TrainFollowEvidenceEntity::getDeleted, 0));

    Map<Long, List<TrainFollowEvidenceEntity>> bySchedule = all.stream()
        .filter(e -> e.getScheduleId() != null)
        .collect(Collectors.groupingBy(TrainFollowEvidenceEntity::getScheduleId));

    List<Map<String, Object>> items = new ArrayList<>();
    int required = 0;
    int ok = 0;
    for (TrainScheduleEntity s : scheduleMap.values().stream()
        .sorted((a, b) -> {
          String da = a.getScheduleDate() != null ? a.getScheduleDate() : "";
          String db = b.getScheduleDate() != null ? b.getScheduleDate() : "";
          int c = da.compareTo(db);
          if (c != 0) return c;
          String ta = a.getStartTime() != null ? a.getStartTime() : "";
          String tb = b.getStartTime() != null ? b.getStartTime() : "";
          return ta.compareTo(tb);
        }).toList()) {
      if (s.getEvidenceRequired() != null && s.getEvidenceRequired() == 0) {
        continue;
      }
      required++;
      List<TrainFollowEvidenceEntity> list = bySchedule.getOrDefault(s.getId(), List.of());
      List<TrainFollowEvidenceEntity> photos = list.stream()
          .filter(e -> "photo".equals(e.getMediaType())).toList();
      List<TrainFollowEvidenceEntity> videos = list.stream()
          .filter(e -> "video".equals(e.getMediaType())).toList();
      long photoApproved = photos.stream().filter(e -> "approved".equals(e.getReviewStatus())).count();
      long photoPending = photos.stream().filter(e -> "pending".equals(e.getReviewStatus())).count();
      long videoApproved = videos.stream().filter(e -> "approved".equals(e.getReviewStatus())).count();
      long videoPending = videos.stream().filter(e -> "pending".equals(e.getReviewStatus())).count();
      String form = TrainFollowPhotoPackGenerator.normalizeTeachForm(s.getTeachForm());
      boolean photoOk = photoApproved >= MIN_PHOTOS_PER_SESSION;
      boolean videoOk = videoApproved >= MIN_VIDEOS_PER_SESSION;
      boolean pass = photoOk && videoOk;
      if (pass) {
        ok++;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("scheduleId", s.getId());
      row.put("scheduleDate", s.getScheduleDate());
      row.put("courseName", s.getCourseName());
      row.put("teachForm", s.getTeachForm());
      row.put("teachFormLabel", form);
      row.put("classHours", s.getClassHours());
      row.put("actualClassHours", s.getActualClassHours());
      row.put("hoursDisplay", TrainFollowPhotoPackGenerator.resolveHours(s, null));
      row.put("startTime", s.getStartTime());
      row.put("endTime", s.getEndTime());
      row.put("photoCount", photos.size());
      row.put("approvedCount", photoApproved);
      row.put("pendingCount", photoPending);
      row.put("minRequired", MIN_PHOTOS_PER_SESSION);
      row.put("minPhotos", MIN_PHOTOS_PER_SESSION);
      row.put("videoCount", videos.size());
      row.put("videoApprovedCount", videoApproved);
      row.put("videoPendingCount", videoPending);
      row.put("minVideos", MIN_VIDEOS_PER_SESSION);
      row.put("videoDurationTargetSec", VIDEO_DURATION_TARGET_SEC);
      row.put("photoOk", photoOk);
      row.put("videoOk", videoOk);
      row.put("coverageOk", pass);
      items.add(row);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("requiredSessions", required);
    result.put("okSessions", ok);
    result.put("missingSessions", Math.max(0, required - ok));
    result.put("ready", required > 0 && ok == required);
    result.put("minPhotos", MIN_PHOTOS_PER_SESSION);
    result.put("minVideos", MIN_VIDEOS_PER_SESSION);
    result.put("videoDurationTargetSec", VIDEO_DURATION_TARGET_SEC);
    result.put("items", items);
    return result;
  }

  @Transactional
  public FollowEvidenceVO createAdmin(Long classId, FollowEvidenceCreateReq req) {
    return createInternal(classId, req, currentOrgUserId(), "admin");
  }

  @Transactional
  public FollowEvidenceVO createApp(Long classId, Long platformUserId, FollowEvidenceCreateReq req) {
    String role = roleService.requireAnyActiveRole(
        platformUserId,
        classId,
        TrainClassRoleApplicationService.MONITOR,
        TrainClassRoleApplicationService.HEAD_TEACHER,
        TrainClassRoleApplicationService.FOLLOW_STAFF,
        TrainClassRoleApplicationService.LEAD_TEACHER,
        TrainClassRoleApplicationService.ASSISTANT);
    return createInternal(classId, req, platformUserId, role);
  }

  /**
   * 上传与写证据分事务：先提交文件，再经 createApp/createAdmin 写证据，避免套同一事务 rollback-only。
   */
  public FollowEvidenceVO createAppMultipart(
      Long classId,
      Long platformUserId,
      MultipartFile file,
      Long scheduleId,
      String bizDate,
      String teachFormOverride,
      String classHoursOverride,
      String caption,
      String mediaType,
      Integer durationSeconds) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择文件");
    }
    // 先鉴权，避免无效上传
    roleService.requireAnyActiveRole(
        platformUserId,
        classId,
        TrainClassRoleApplicationService.MONITOR,
        TrainClassRoleApplicationService.HEAD_TEACHER,
        TrainClassRoleApplicationService.FOLLOW_STAFF,
        TrainClassRoleApplicationService.LEAD_TEACHER,
        TrainClassRoleApplicationService.ASSISTANT);
    // App 主体 tenantId 常为 0，按请求租户落库，避免管理端按租户下载对不上
    FileVO stored = fileApplicationService.uploadForTenant(tenantId(), file, "private");
    FollowEvidenceCreateReq req = new FollowEvidenceCreateReq();
    req.setScheduleId(scheduleId);
    req.setBizDate(bizDate);
    req.setTeachFormOverride(teachFormOverride);
    req.setClassHoursOverride(classHoursOverride);
    req.setCaption(caption);
    req.setMediaType(StringUtils.hasText(mediaType) ? mediaType : "photo");
    req.setDurationSeconds(durationSeconds);
    req.setFileId(stored.id());
    // 必须保留 /api/files/{id}/download，培训照片打包依赖此路径解析 fileId
    req.setFilePath("/api/files/" + stored.id() + "/download");
    return self.createApp(classId, platformUserId, req);
  }

  public FollowEvidenceVO createAdminMultipart(
      Long classId,
      MultipartFile file,
      Long scheduleId,
      String bizDate,
      String teachFormOverride,
      String classHoursOverride,
      String caption,
      String mediaType,
      Integer durationSeconds) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择文件");
    }
    FileVO stored = fileApplicationService.upload(file, "private");
    FollowEvidenceCreateReq req = new FollowEvidenceCreateReq();
    req.setScheduleId(scheduleId);
    req.setBizDate(bizDate);
    req.setTeachFormOverride(teachFormOverride);
    req.setClassHoursOverride(classHoursOverride);
    req.setCaption(caption);
    req.setMediaType(StringUtils.hasText(mediaType) ? mediaType : "photo");
    req.setDurationSeconds(durationSeconds);
    req.setFileId(stored.id());
    req.setFilePath("/api/files/" + stored.id() + "/download");
    return self.createAdmin(classId, req);
  }

  /**
   * 影像预览流：按 fileId 读内容（不依赖 sys_file.tenant_id / COS 公网 ACL）。
   * 调用方须已完成业务鉴权。
   */
  public ResponseEntity<byte[]> previewContent(Long evidenceId) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    if (entity.getFileId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "影像文件不存在");
    }
    FileVO meta = fileApplicationService.getById(entity.getFileId());
    byte[] bytes = fileApplicationService.readFileBytesById(entity.getFileId());
    String contentType = meta != null && StringUtils.hasText(meta.contentType())
        ? meta.contentType()
        : MediaType.IMAGE_JPEG_VALUE;
    String name = meta != null && StringUtils.hasText(meta.originalName())
        ? meta.originalName()
        : ("evidence-" + evidenceId);
    String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
        .contentType(MediaType.parseMediaType(contentType))
        .contentLength(bytes.length)
        .body(bytes);
  }

  public ResponseEntity<byte[]> previewApp(Long evidenceId, Long platformUserId) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    roleService.assertClassMember(platformUserId, entity.getClassId());
    return previewContent(evidenceId);
  }

  private FollowEvidenceVO createInternal(
      Long classId, FollowEvidenceCreateReq req, Long uploaderId, String uploaderRole) {
    requireClass(classId);
    if (!StringUtils.hasText(req.getFilePath())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少文件路径");
    }
    TrainScheduleEntity schedule = null;
    if (req.getScheduleId() != null) {
      schedule = scheduleMapper.selectById(req.getScheduleId());
      if (schedule == null || !Objects.equals(schedule.getClassId(), classId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "课次不存在或不属于本班");
      }
    } else {
      if (!StringUtils.hasText(req.getBizDate())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无课次时请填写业务日期");
      }
      if (!StringUtils.hasText(req.getTeachFormOverride())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无课次时请选择理论/实操");
      }
    }

    String media = StringUtils.hasText(req.getMediaType()) ? req.getMediaType().trim() : "photo";
    if (!"photo".equals(media) && !"video".equals(media)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "mediaType 仅支持 photo/video");
    }
    assertMediaQuota(classId, req.getScheduleId(), media, req.getDurationSeconds());

    TrainFollowEvidenceEntity entity = new TrainFollowEvidenceEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setScheduleId(req.getScheduleId());
    entity.setBizDate(schedule != null ? schedule.getScheduleDate() : req.getBizDate().trim());
    entity.setTeachFormOverride(req.getTeachFormOverride());
    entity.setClassHoursOverride(req.getClassHoursOverride());
    entity.setMediaType(media);
    entity.setDurationSeconds("video".equals(media) ? req.getDurationSeconds() : null);
    entity.setFileId(req.getFileId());
    entity.setFilePath(req.getFilePath().trim());
    entity.setThumbPath(req.getThumbPath());
    entity.setCaption(req.getCaption());
    entity.setUploaderUserId(uploaderId);
    entity.setUploaderRole(uploaderRole);
    entity.setReviewStatus("pending");
    entity.setSortNo(req.getSortNo() != null ? req.getSortNo() : nextSortNo(classId, req.getScheduleId()));
    entity.setCreatedBy(uploaderId);
    entity.setUpdatedBy(uploaderId);
    evidenceMapper.insert(entity);

    String mediaLabel = "video".equals(media) ? "视频" : "照片";
    String actorLabel = evidenceActorLabel(uploaderRole);
    String title = StringUtils.hasText(actorLabel)
        ? actorLabel + "上传课堂" + mediaLabel
        : "上传课堂" + mediaLabel;
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("mediaType", media);
    if (entity.getFileId() != null) {
      detail.put("fileId", entity.getFileId());
    }
    opLogWriter.write(FollowOpLogCommand.of(classId, FollowOpEventCode.EVIDENCE_UPLOAD)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .title(title)
        .summary(mediaLabel + (StringUtils.hasText(entity.getCaption()) ? "：" + entity.getCaption() : ""))
        .actor(uploaderId, null, uploaderRole)
        .client("admin".equals(uploaderRole) ? "admin" : "app")
        .target("evidence", entity.getId())
        .detail(detail));

    return toVo(entity, schedule);
  }

  /** 每课次：照片最多 5（待审+已审）、视频最多 3；视频时长约 15 秒 */
  private void assertMediaQuota(Long classId, Long scheduleId, String media, Integer durationSeconds) {
    if ("video".equals(media)) {
      if (durationSeconds == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "视频须上报时长（秒）");
      }
      int min = VIDEO_DURATION_TARGET_SEC - VIDEO_DURATION_TOLERANCE_SEC;
      int max = VIDEO_DURATION_TARGET_SEC + VIDEO_DURATION_TOLERANCE_SEC;
      if (durationSeconds < min || durationSeconds > max) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "视频须约 " + VIDEO_DURATION_TARGET_SEC + " 秒（允许 " + min + "～" + max + " 秒），当前 "
                + durationSeconds + " 秒");
      }
    }
    if (scheduleId == null) {
      return;
    }
    long active = evidenceMapper.selectCount(
        new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
            .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
            .eq(TrainFollowEvidenceEntity::getClassId, classId)
            .eq(TrainFollowEvidenceEntity::getScheduleId, scheduleId)
            .eq(TrainFollowEvidenceEntity::getMediaType, media)
            .eq(TrainFollowEvidenceEntity::getDeleted, 0)
            .in(TrainFollowEvidenceEntity::getReviewStatus, List.of("pending", "approved")));
    int limit = "video".equals(media) ? MIN_VIDEOS_PER_SESSION : MIN_PHOTOS_PER_SESSION;
    if (active >= limit) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "video".equals(media)
              ? "本课次视频已满 " + limit + " 段（待审+已审），请先删除驳回项或待审项后再传"
              : "本课次照片已满 " + limit + " 张（待审+已审），请先删除驳回项或待审项后再传");
    }
  }

  @Transactional
  public FollowEvidenceVO patch(Long evidenceId, FollowEvidencePatchReq req, Long actorId, boolean appMode) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    if (appMode) {
      if (!"pending".equals(entity.getReviewStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "已审核影像不可修改，请联系教务");
      }
      if (!Objects.equals(entity.getUploaderUserId(), actorId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "只能修改自己上传的未审影像");
      }
    }
    if (req.getCaption() != null) {
      entity.setCaption(req.getCaption());
    }
    if (req.getSortNo() != null) {
      entity.setSortNo(req.getSortNo());
    }
    entity.setUpdatedBy(actorId);
    evidenceMapper.updateById(entity);
    return toVo(entity, entity.getScheduleId() != null ? scheduleMapper.selectById(entity.getScheduleId()) : null);
  }

  @Transactional
  public FollowEvidenceVO review(Long evidenceId, FollowEvidenceReviewReq req, Long reviewerId) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    String status = req.getReviewStatus() != null ? req.getReviewStatus().trim() : "";
    if (!"approved".equals(status) && !"rejected".equals(status)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "reviewStatus 须为 approved 或 rejected");
    }
    if ("rejected".equals(status) && !StringUtils.hasText(req.getRejectReason())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回请填写原因");
    }
    entity.setReviewStatus(status);
    entity.setReviewedBy(reviewerId);
    entity.setReviewedAt(LocalDateTime.now());
    entity.setRejectReason("rejected".equals(status) ? req.getRejectReason().trim() : null);
    entity.setUpdatedBy(reviewerId);
    evidenceMapper.updateById(entity);

    FollowOpEventCode ev = "approved".equals(status)
        ? FollowOpEventCode.EVIDENCE_REVIEW_APPROVE
        : FollowOpEventCode.EVIDENCE_REVIEW_REJECT;
    opLogWriter.write(FollowOpLogCommand.of(entity.getClassId(), ev)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .summary("rejected".equals(status) ? entity.getRejectReason() : ("影像#" + evidenceId))
        .actor(reviewerId, null, null)
        .target("evidence", entity.getId())
        .severity("rejected".equals(status) ? "warn" : "info"));

    return toVo(entity, entity.getScheduleId() != null ? scheduleMapper.selectById(entity.getScheduleId()) : null);
  }

  @Transactional
  public FollowEvidenceVO reviewApp(Long evidenceId, Long platformUserId, FollowEvidenceReviewReq req) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    roleService.requireAnyActiveRole(
        platformUserId,
        entity.getClassId(),
        TrainClassRoleApplicationService.HEAD_TEACHER,
        TrainClassRoleApplicationService.FOLLOW_STAFF);
    return review(evidenceId, req, platformUserId);
  }

  @Transactional
  public void delete(Long evidenceId, Long actorId, boolean appMode) {
    TrainFollowEvidenceEntity entity = requireEvidence(evidenceId);
    if (appMode) {
      if (!"pending".equals(entity.getReviewStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "已审核影像不可删除");
      }
      if (!Objects.equals(entity.getUploaderUserId(), actorId)) {
        roleService.requireAnyActiveRole(
            actorId,
            entity.getClassId(),
            TrainClassRoleApplicationService.HEAD_TEACHER,
            TrainClassRoleApplicationService.FOLLOW_STAFF);
      }
    }
    entity.setDeleted(1);
    entity.setUpdatedBy(actorId);
    evidenceMapper.updateById(entity);

    opLogWriter.write(FollowOpLogCommand.of(entity.getClassId(), FollowOpEventCode.EVIDENCE_DELETE)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .summary("删除影像#" + evidenceId)
        .actor(actorId, null, null)
        .target("evidence", evidenceId)
        .severity("warn")
        .client(appMode ? "app" : "admin"));
  }

  @Transactional
  public Map<String, Object> generatePhotoPack(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    List<TrainFollowEvidenceEntity> photos = evidenceMapper.selectList(
        new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
            .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
            .eq(TrainFollowEvidenceEntity::getClassId, classId)
            .eq(TrainFollowEvidenceEntity::getDeleted, 0)
            .eq(TrainFollowEvidenceEntity::getMediaType, "photo")
            .eq(TrainFollowEvidenceEntity::getReviewStatus, "approved"));
    if (photos.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无已审核通过的课堂照片，无法生成");
    }
    Map<Long, TrainScheduleEntity> scheduleMap = loadSchedules(classId);
    var sections = TrainFollowPhotoPackGenerator.groupSections(photos, scheduleMap);
    String title = (clazz.getTitle() != null ? clazz.getTitle() : ("班级" + classId)) + "培训照片";
    FileVO file = photoPackGenerator.generateAndStore(clazz, sections, tenantId(), title);

    TrainFollowPhotoPackEntity pack = new TrainFollowPhotoPackEntity();
    pack.setTenantId(tenantId());
    pack.setClassId(classId);
    pack.setFileId(file.id());
    pack.setFilePath("/api/files/" + file.id() + "/download");
    pack.setTitle(title);
    pack.setGeneratedAt(LocalDateTime.now());
    pack.setGeneratedBy(currentOrgUserId());
    pack.setCreatedBy(currentOrgUserId());
    pack.setUpdatedBy(currentOrgUserId());
    try {
      Map<String, Object> snap = new HashMap<>();
      snap.put("photoCount", photos.size());
      snap.put("sectionCount", sections.size());
      snap.put("scheduleIds", photos.stream().map(TrainFollowEvidenceEntity::getScheduleId)
          .filter(Objects::nonNull).distinct().toList());
      pack.setSnapshotJson(objectMapper.writeValueAsString(snap));
    } catch (Exception ignored) {
      pack.setSnapshotJson(null);
    }
    photoPackMapper.insert(pack);

    Map<String, Object> packDetail = new LinkedHashMap<>();
    packDetail.put("photoCount", photos.size());
    packDetail.put("fileId", file.id());
    opLogWriter.write(FollowOpLogCommand.of(classId, FollowOpEventCode.EVIDENCE_PHOTO_PACK_GENERATE)
        .title("生成培训照片包")
        .summary(title + "（" + photos.size() + " 张）")
        .target("photo_pack", pack.getId())
        .detail(packDetail));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("packId", pack.getId());
    result.put("fileId", file.id());
    result.put("filePath", pack.getFilePath());
    // 私有 COS 直链浏览器打不开；对外统一给鉴权下载路径
    result.put("url", pack.getFilePath());
    result.put("title", title);
    result.put("generatedAt", pack.getGeneratedAt());
    return result;
  }

  public Map<String, Object> latestPhotoPack(Long classId) {
    requireClass(classId);
    TrainFollowPhotoPackEntity pack = photoPackMapper.selectOne(
        new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
            .eq(TrainFollowPhotoPackEntity::getTenantId, tenantId())
            .eq(TrainFollowPhotoPackEntity::getClassId, classId)
            .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
            .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
            .last("LIMIT 1"));
    if (pack == null) {
      return Map.of("exists", false);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("exists", true);
    result.put("packId", pack.getId());
    result.put("fileId", pack.getFileId());
    String path = StringUtils.hasText(pack.getFilePath())
        ? pack.getFilePath()
        : (pack.getFileId() != null ? "/api/files/" + pack.getFileId() + "/download" : null);
    result.put("filePath", path);
    result.put("url", path);
    result.put("title", pack.getTitle());
    result.put("generatedAt", pack.getGeneratedAt());
    return result;
  }

  private int nextSortNo(Long classId, Long scheduleId) {
    LambdaQueryWrapper<TrainFollowEvidenceEntity> q = new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
        .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
        .eq(TrainFollowEvidenceEntity::getClassId, classId)
        .eq(TrainFollowEvidenceEntity::getDeleted, 0)
        .orderByDesc(TrainFollowEvidenceEntity::getSortNo)
        .last("LIMIT 1");
    if (scheduleId != null) {
      q.eq(TrainFollowEvidenceEntity::getScheduleId, scheduleId);
    }
    TrainFollowEvidenceEntity last = evidenceMapper.selectOne(q);
    return last == null || last.getSortNo() == null ? 1 : last.getSortNo() + 1;
  }

  private Map<Long, TrainScheduleEntity> loadSchedules(Long classId) {
    List<TrainScheduleEntity> list = scheduleMapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getTenantId, tenantId())
            .eq(TrainScheduleEntity::getDeleted, 0));
    Map<Long, TrainScheduleEntity> map = new HashMap<>();
    for (TrainScheduleEntity s : list) {
      map.put(s.getId(), s);
    }
    return map;
  }

  private FollowEvidenceVO toVo(TrainFollowEvidenceEntity e, TrainScheduleEntity schedule) {
    // COS private 直链 403；/files/{id}/download 又受 sys_file.tenant_id 限制。
    // 统一给业务预览地址，由 App/管理端带鉴权拉取。
    String url = "/api/training/follow/evidences/" + e.getId() + "/preview";
    String filePath = StringUtils.hasText(e.getFilePath())
        ? e.getFilePath()
        : (e.getFileId() != null ? "/api/files/" + e.getFileId() + "/download" : null);
    return new FollowEvidenceVO(
        e.getId(),
        e.getClassId(),
        e.getScheduleId(),
        e.getBizDate(),
        e.getMediaType(),
        e.getDurationSeconds(),
        e.getFileId(),
        filePath,
        url,
        e.getCaption(),
        e.getUploaderUserId(),
        e.getUploaderRole(),
        e.getReviewStatus(),
        e.getReviewedBy(),
        e.getReviewedAt(),
        e.getRejectReason(),
        e.getSortNo(),
        schedule != null ? schedule.getScheduleDate() : e.getBizDate(),
        schedule != null ? schedule.getCourseName() : null,
        schedule != null ? schedule.getTeachForm() : e.getTeachFormOverride(),
        schedule != null ? schedule.getClassHours() : e.getClassHoursOverride(),
        schedule != null ? schedule.getActualClassHours() : null,
        schedule != null ? schedule.getStartTime() : null,
        schedule != null ? schedule.getEndTime() : null,
        e.getCreatedAt());
  }

  private TrainFollowEvidenceEntity requireEvidence(Long id) {
    TrainFollowEvidenceEntity entity = evidenceMapper.selectOne(
        new LambdaQueryWrapper<TrainFollowEvidenceEntity>()
            .eq(TrainFollowEvidenceEntity::getId, id)
            .eq(TrainFollowEvidenceEntity::getTenantId, tenantId())
            .eq(TrainFollowEvidenceEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "影像不存在");
    }
    return entity;
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private Long currentOrgUserId() {
    try {
      return SecurityContext.currentUser().getUserId();
    } catch (Exception ex) {
      return null;
    }
  }

  private static String evidenceActorLabel(String role) {
    if (!StringUtils.hasText(role)) {
      return "";
    }
    return switch (role.trim()) {
      case "monitor" -> "班长";
      case "head_teacher" -> "班主任";
      case "follow_staff" -> "跟班教务";
      case "lead_teacher" -> "主讲";
      case "assistant" -> "助教";
      case "admin" -> "管理端";
      default -> "";
    };
  }
}

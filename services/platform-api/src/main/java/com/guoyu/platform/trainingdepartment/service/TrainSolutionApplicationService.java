package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionBadgeItem;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionSaveReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSolutionBadgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSolutionEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSolutionBadgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSolutionMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionBadgeVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 方案管理服务实现
 */
@Service
@Slf4j
public class TrainSolutionApplicationService {

  private static final Pattern FILE_API_PATTERN = Pattern.compile("/api/files/(\\d+)/download");

  private final TrainSolutionMapper mapper;
  private final TrainSolutionBadgeMapper badgeMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final FileMapper fileMapper;

  public TrainSolutionApplicationService(
      TrainSolutionMapper mapper,
      TrainSolutionBadgeMapper badgeMapper,
      TrainInfoListMapper trainInfoListMapper,
      FileMapper fileMapper) {
    this.mapper = mapper;
    this.badgeMapper = badgeMapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.fileMapper = fileMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSolutionVO> page(TrainSolutionQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSolutionEntity> wrapper = new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0)
        .orderByDesc(TrainSolutionEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainSolutionEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainSolutionEntity::getCompanyName, req.getKeyword()));
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainSolutionEntity::getStatus, req.getStatus());
    }
    if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainSolutionEntity::getTrainInfoId, req.getTrainInfoId());
    }
    if (req.getCompanyId() != null) {
      wrapper.eq(TrainSolutionEntity::getCompanyId, req.getCompanyId());
    }
    Page<TrainSolutionEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSolutionVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public PageResponse<TrainSolutionListVO> create(TrainSolutionCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainSolutionEntity entity = new TrainSolutionEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainInfoId(req.getTrainInfoId());
    entity.setCompanyId(req.getCompanyId());
    entity.setCompanyName(req.getCompanyName());
    entity.setTitle(req.getTitle());
    entity.setNationPolicy(req.getNationPolicy());
    entity.setIndustrySituationAnalysis(req.getIndustrySituationAnalysis());
    entity.setCompanyDevelopmentAnalysis(req.getCompanyDevelopmentAnalysis());
    entity.setResearchDataAnalysis(req.getResearchDataAnalysis());
    entity.setProjectDevelopmentIdeas(req.getProjectDevelopmentIdeas());
    entity.setMainObjectives(req.getMainObjectives());
    entity.setMainInitiatives(req.getMainInitiatives());
    entity.setDevelopmentPlan(req.getDevelopmentPlan());
    entity.setDivision(req.getDivision());
    entity.setRisk(req.getRisk());
    entity.setCommunication(req.getCommunication());
    entity.setCheckpoint(req.getCheckpoint());
    entity.setResults(req.getResults());
    entity.setTeacherIntro(req.getTeacherIntro());
    entity.setRequestAnalysis(req.getRequestAnalysis());
    entity.setTrainContent(req.getTrainContent());
    entity.setTrainMethod(req.getTrainMethod());
    entity.setCourse(req.getCourse());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setContent(req.getContent());
    entity.setFilePath(req.getFilePath());
    entity.setRejectReason(req.getRejectReason());
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    mapper.insert(entity);
    return listForPage(1, 10);
  }

  @Transactional
  public TrainSolutionVO update(Long id, TrainSolutionUpdateReq req) {
    TrainSolutionEntity entity = getExisting(id);
    if (req.getTrainInfoId() != null) { entity.setTrainInfoId(req.getTrainInfoId()); }
    if (req.getCompanyId() != null) { entity.setCompanyId(req.getCompanyId()); }
    if (req.getCompanyName() != null) { entity.setCompanyName(req.getCompanyName()); }
    if (req.getTitle() != null) { entity.setTitle(req.getTitle()); }
    if (req.getNationPolicy() != null) { entity.setNationPolicy(req.getNationPolicy()); }
    if (req.getIndustrySituationAnalysis() != null) { entity.setIndustrySituationAnalysis(req.getIndustrySituationAnalysis()); }
    if (req.getCompanyDevelopmentAnalysis() != null) { entity.setCompanyDevelopmentAnalysis(req.getCompanyDevelopmentAnalysis()); }
    if (req.getResearchDataAnalysis() != null) { entity.setResearchDataAnalysis(req.getResearchDataAnalysis()); }
    if (req.getProjectDevelopmentIdeas() != null) { entity.setProjectDevelopmentIdeas(req.getProjectDevelopmentIdeas()); }
    if (req.getMainObjectives() != null) { entity.setMainObjectives(req.getMainObjectives()); }
    if (req.getMainInitiatives() != null) { entity.setMainInitiatives(req.getMainInitiatives()); }
    if (req.getDevelopmentPlan() != null) { entity.setDevelopmentPlan(req.getDevelopmentPlan()); }
    if (req.getDivision() != null) { entity.setDivision(req.getDivision()); }
    if (req.getRisk() != null) { entity.setRisk(req.getRisk()); }
    if (req.getCommunication() != null) { entity.setCommunication(req.getCommunication()); }
    if (req.getCheckpoint() != null) { entity.setCheckpoint(req.getCheckpoint()); }
    if (req.getResults() != null) { entity.setResults(req.getResults()); }
    if (req.getTeacherIntro() != null) { entity.setTeacherIntro(req.getTeacherIntro()); }
    if (req.getRequestAnalysis() != null) { entity.setRequestAnalysis(req.getRequestAnalysis()); }
    if (req.getTrainContent() != null) { entity.setTrainContent(req.getTrainContent()); }
    if (req.getTrainMethod() != null) { entity.setTrainMethod(req.getTrainMethod()); }
    if (req.getCourse() != null) { entity.setCourse(req.getCourse()); }
    if (req.getStatus() != null) { entity.setStatus(req.getStatus()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    if (req.getContent() != null) { entity.setContent(req.getContent()); }
    if (req.getFilePath() != null) { entity.setFilePath(req.getFilePath()); }
    if (req.getRejectReason() != null) { entity.setRejectReason(req.getRejectReason()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSolutionEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
    List<TrainSolutionBadgeEntity> badges = badgeMapper.selectList(
        new LambdaQueryWrapper<TrainSolutionBadgeEntity>()
            .eq(TrainSolutionBadgeEntity::getTenantId, tenantId)
            .eq(TrainSolutionBadgeEntity::getSolutionId, id)
            .eq(TrainSolutionBadgeEntity::getDeleted, 0));
    for (TrainSolutionBadgeEntity b : badges) {
      b.setDeleted(1);
      badgeMapper.deleteById(b.getId());
    }
  }

  @Transactional
  public TrainSolutionVO submit(Long id) {
    TrainSolutionEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有草稿状态的方案才能提交审批");
    }
    entity.setStatus(1);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSolutionVO approve(Long id) {
    TrainSolutionEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待审批状态的方案才能审批通过");
    }
    entity.setStatus(2);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSolutionVO reject(Long id, String reason) {
    TrainSolutionEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待审批状态的方案才能驳回");
    }
    entity.setStatus(0);
    entity.setRejectReason(reason);
    mapper.updateById(entity);
    return toVO(entity);
  }

  // ======== Badge management (经费明细) ========

  @Transactional(readOnly = true)
  public List<TrainSolutionBadgeVO> getBadges(Long solutionId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainSolutionBadgeEntity> entities = badgeMapper.selectList(
        new LambdaQueryWrapper<TrainSolutionBadgeEntity>()
            .eq(TrainSolutionBadgeEntity::getTenantId, tenantId)
            .eq(TrainSolutionBadgeEntity::getSolutionId, solutionId)
            .eq(TrainSolutionBadgeEntity::getDeleted, 0)
            .orderByAsc(TrainSolutionBadgeEntity::getId));
    return entities.stream().map(this::toBadgeVO).toList();
  }

  @Transactional
  public void saveBadges(Long solutionId, List<TrainSolutionBadgeVO> badges) {
    if (badges == null) {
      return;
    }
    List<TrainSolutionBadgeItem> items = badges.stream().map(b -> {
      TrainSolutionBadgeItem item = new TrainSolutionBadgeItem();
      item.setTitle(b.title());
      item.setBadge(b.badge());
      item.setExpend(b.expend());
      return item;
    }).toList();
    replaceBadges(solutionId, items);
  }

  private void replaceBadges(Long solutionId, List<TrainSolutionBadgeItem> badges) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainSolutionBadgeEntity> existing = badgeMapper.selectList(
        new LambdaQueryWrapper<TrainSolutionBadgeEntity>()
            .eq(TrainSolutionBadgeEntity::getTenantId, tenantId)
            .eq(TrainSolutionBadgeEntity::getSolutionId, solutionId)
            .eq(TrainSolutionBadgeEntity::getDeleted, 0));
    for (TrainSolutionBadgeEntity e : existing) {
      e.setDeleted(1);
      badgeMapper.deleteById(e.getId());
    }
    if (badges == null) {
      return;
    }
    for (TrainSolutionBadgeItem b : badges) {
      if (b == null || !StringUtils.hasText(b.getTitle())) {
        continue;
      }
      TrainSolutionBadgeEntity entity = new TrainSolutionBadgeEntity();
      entity.setSolutionId(solutionId);
      entity.setTitle(b.getTitle());
      entity.setBadge(b.getBadge() != null ? b.getBadge() : BigDecimal.ZERO);
      entity.setExpend(b.getExpend() != null ? b.getExpend() : BigDecimal.ZERO);
      entity.setTenantId(tenantId);
      entity.setDeleted(0);
      badgeMapper.insert(entity);
    }
  }

  // ======== 培训方案管理业务方法 ========

  @Transactional(readOnly = true)
  public PageResponse<TrainSolutionListVO> listForPage(Integer page, Integer pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Page<TrainSolutionEntity> result = mapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10),
        new LambdaQueryWrapper<TrainSolutionEntity>()
            .eq(TrainSolutionEntity::getTenantId, tenantId)
            .eq(TrainSolutionEntity::getDeleted, 0)
            .gt(TrainSolutionEntity::getTrainInfoId, 0L)
            .orderByAsc(TrainSolutionEntity::getSortNumber)
            .orderByDesc(TrainSolutionEntity::getCreateTime));

    List<TrainSolutionEntity> records = result.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    // 批量获取培训标题 Map
    List<Long> trainInfoIds = records.stream().map(TrainSolutionEntity::getTrainInfoId).distinct().toList();
    Map<Long, String> trainTitleMap = trainInfoListMapper.selectList(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .in(TrainInfoListEntity::getId, trainInfoIds)
            .eq(TrainInfoListEntity::getTenantId, tenantId)
            .eq(TrainInfoListEntity::getDeleted, 0))
        .stream().collect(Collectors.toMap(TrainInfoListEntity::getId,
            t -> t.getTitle() != null ? t.getTitle() : "", (a, b) -> a));

    // 批量获取费用汇总 Map (solutionId → {badge, expend})
    List<Long> solutionIds = records.stream().map(TrainSolutionEntity::getId).toList();
    List<TrainSolutionBadgeEntity> allBadges = badgeMapper.selectList(
        new LambdaQueryWrapper<TrainSolutionBadgeEntity>()
            .in(TrainSolutionBadgeEntity::getSolutionId, solutionIds)
            .eq(TrainSolutionBadgeEntity::getDeleted, 0));
    Map<Long, BigDecimal> badgeMap = allBadges.stream()
        .filter(b -> b.getBadge() != null)
        .collect(Collectors.groupingBy(TrainSolutionBadgeEntity::getSolutionId,
            Collectors.reducing(BigDecimal.ZERO, TrainSolutionBadgeEntity::getBadge, BigDecimal::add)));
    Map<Long, BigDecimal> expendMap = allBadges.stream()
        .filter(b -> b.getExpend() != null)
        .collect(Collectors.groupingBy(TrainSolutionBadgeEntity::getSolutionId,
            Collectors.reducing(BigDecimal.ZERO, TrainSolutionBadgeEntity::getExpend, BigDecimal::add)));

    List<TrainSolutionListVO> vos = records.stream().map(e ->
        new TrainSolutionListVO(e.getId(), e.getTitle(), e.getCompanyId(),
            e.getCompanyName(), trainTitleMap.getOrDefault(e.getTrainInfoId(), ""),
            badgeMap.getOrDefault(e.getId(), BigDecimal.ZERO),
            expendMap.getOrDefault(e.getId(), BigDecimal.ZERO),
            e.getCreateTime(), e.getRejectReason()))
        .toList();

    return new PageResponse<>(vos, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getDetail(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSolutionEntity entity = getExisting(id);

    // 培训需求标题
    String trainTitle = "";
    if (entity.getTrainInfoId() != null) {
      TrainInfoListEntity info = trainInfoListMapper.selectOne(
          new LambdaQueryWrapper<TrainInfoListEntity>()
              .eq(TrainInfoListEntity::getTenantId, tenantId)
              .eq(TrainInfoListEntity::getId, entity.getTrainInfoId())
              .eq(TrainInfoListEntity::getDeleted, 0));
      if (info != null) {
        trainTitle = info.getTitle() != null ? info.getTitle() : "";
      }
    }

    // 经费明细
    List<TrainSolutionBadgeVO> badgeList = getBadges(id);

    // 文件类型（优先从 platform file 元数据解析，兼容 /api/files/{id}/download）
    String fileUrl = entity.getFilePath() != null ? entity.getFilePath() : "";
    String fileType = resolveFileType(fileUrl);
    String fileName = resolveFileName(fileUrl);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", entity.getId());
    data.put("title", entity.getTitle());
    data.put("company_id", entity.getCompanyId());
    data.put("company_name", entity.getCompanyName());
    data.put("train_info_id", entity.getTrainInfoId());
    data.put("train_title", trainTitle);
    data.put("file_path", entity.getFilePath());
    data.put("file", fileUrl);
    data.put("file_name", fileName);
    data.put("fileType", fileType);
    data.put("content", entity.getContent());
    data.put("reject_reason", entity.getRejectReason());
    data.put("badgeList", badgeList);
    return data;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getByDemandId(Long traindemandId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    // Step 1: 通过需求ID找培训信息
    TrainInfoListEntity trainInfo = trainInfoListMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getTenantId, tenantId)
            .eq(TrainInfoListEntity::getTraindemandId, traindemandId)
            .eq(TrainInfoListEntity::getDeleted, 0));
    if (trainInfo == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在或已删除");
    }

    // Step 2: 通过培训信息ID找方案
    TrainSolutionEntity solution = mapper.selectOne(
        new LambdaQueryWrapper<TrainSolutionEntity>()
            .eq(TrainSolutionEntity::getTenantId, tenantId)
            .eq(TrainSolutionEntity::getDeleted, 0)
            .eq(TrainSolutionEntity::getTrainInfoId, trainInfo.getId()));
    if (solution == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "方案不存在或已删除");
    }

    // Step 3: 组装返回数据
    String trainTitle = trainInfo.getTitle() != null ? trainInfo.getTitle() : "";
    List<TrainSolutionBadgeVO> badgeList = getBadges(solution.getId());
    String fileUrl = solution.getFilePath() != null ? solution.getFilePath() : "";
    String fileType = resolveFileType(fileUrl);
    String fileName = resolveFileName(fileUrl);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", solution.getId());
    data.put("title", solution.getTitle());
    data.put("company_id", solution.getCompanyId());
    data.put("company_name", solution.getCompanyName());
    data.put("train_info_id", solution.getTrainInfoId());
    data.put("train_title", trainTitle);
    data.put("file_path", solution.getFilePath());
    data.put("file", fileUrl);
    data.put("file_name", fileName);
    data.put("fileType", fileType);
    data.put("badgeList", badgeList);
    data.put("train_info_status", trainInfo.getCompanyHasConfirm());
    data.put("reject_reason", solution.getRejectReason());
    return data;
  }

  @Transactional
  public Map<String, Object> save(TrainSolutionSaveReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainSolutionEntity entity;

    if (req.getId() != null && req.getId() > 0) {
      // Update
      entity = getExisting(req.getId());
      entity.setTitle(req.getTitle());
      entity.setCompanyId(req.getCompanyId());
      entity.setTrainInfoId(req.getTraininfoId());
      if (req.getFilePath() != null) {
        entity.setFilePath(req.getFilePath());
      }
      if (req.getCompanyName() != null) {
        entity.setCompanyName(req.getCompanyName());
      }
      mapper.updateById(entity);
    } else {
      // Create
      entity = new TrainSolutionEntity();
      entity.setTenantId(tenantId);
      entity.setUserId(userId);
      entity.setTitle(req.getTitle());
      entity.setCompanyId(req.getCompanyId());
      entity.setTrainInfoId(req.getTraininfoId());
      entity.setFilePath(req.getFilePath());
      entity.setCompanyName(req.getCompanyName());
      entity.setSortNumber(1);
      entity.setDeleted(0);
      entity.setCreateTime(LocalDateTime.now());
      mapper.insert(entity);
      // 对齐 PHP YuntrainController::solutionsaveAction：新建方案后培训信息 status=1（待确认）
      markTrainInfoPendingConfirm(req.getTraininfoId(), tenantId);
    }

    // Save badge list
    if (req.getBadgeList() != null) {
      replaceBadges(entity.getId(), req.getBadgeList());
    }

    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", entity.getId());
    info.put("title", entity.getTitle());
    return Map.of("info", info);
  }

  private void markTrainInfoPendingConfirm(Long trainInfoId, Long tenantId) {
    if (trainInfoId == null || trainInfoId <= 0) {
      return;
    }
    TrainInfoListEntity info = trainInfoListMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getTenantId, tenantId)
            .eq(TrainInfoListEntity::getId, trainInfoId)
            .eq(TrainInfoListEntity::getDeleted, 0));
    if (info != null) {
      info.setStatus(1);
      trainInfoListMapper.updateById(info);
    }
  }

  private TrainSolutionBadgeVO toBadgeVO(TrainSolutionBadgeEntity entity) {
    return new TrainSolutionBadgeVO(
        entity.getId(),
        entity.getSolutionId(),
        entity.getTitle(),
        entity.getBadge(),
        entity.getExpend(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }

  private TrainSolutionEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSolutionEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getId, id)
        .eq(TrainSolutionEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainSolutionVO toVO(TrainSolutionEntity entity) {
    return new TrainSolutionVO(
        entity.getId(),
        entity.getTrainInfoId(),
        entity.getCompanyId(),
        entity.getCompanyName(),
        entity.getTitle(),
        entity.getNationPolicy(),
        entity.getIndustrySituationAnalysis(),
        entity.getCompanyDevelopmentAnalysis(),
        entity.getResearchDataAnalysis(),
        entity.getProjectDevelopmentIdeas(),
        entity.getMainObjectives(),
        entity.getMainInitiatives(),
        entity.getDevelopmentPlan(),
        entity.getDivision(),
        entity.getRisk(),
        entity.getCommunication(),
        entity.getCheckpoint(),
        entity.getResults(),
        entity.getTeacherIntro(),
        entity.getRequestAnalysis(),
        entity.getTrainContent(),
        entity.getTrainMethod(),
        entity.getCourse(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getUserId(),
        entity.getContent(),
        entity.getFilePath(),
        entity.getRejectReason()
    );
  }

  private String resolveFileType(String fileUrl) {
    FileEntity entity = findPlatformFile(fileUrl);
    if (entity != null) {
      return inferFileType(entity.getOriginalName(), entity.getContentType());
    }
    return inferFileTypeFromPath(fileUrl);
  }

  private String resolveFileName(String fileUrl) {
    FileEntity entity = findPlatformFile(fileUrl);
    if (entity != null && StringUtils.hasText(entity.getOriginalName())) {
      return entity.getOriginalName();
    }
    return inferFileNameFromPath(fileUrl);
  }

  private FileEntity findPlatformFile(String fileUrl) {
    if (!StringUtils.hasText(fileUrl)) {
      return null;
    }
    Matcher matcher = FILE_API_PATTERN.matcher(fileUrl.trim());
    if (!matcher.find()) {
      return null;
    }
    Long fileId = Long.parseLong(matcher.group(1));
    return fileMapper.selectOne(new LambdaQueryWrapper<FileEntity>()
        .eq(FileEntity::getId, fileId)
        .eq(FileEntity::getDeleted, 0)
        .eq(FileEntity::getStatus, "uploaded"));
  }

  private String inferFileTypeFromPath(String fileUrl) {
    if (!StringUtils.hasText(fileUrl) || !fileUrl.contains(".")) {
      return "";
    }
    String ext = fileUrl.substring(fileUrl.lastIndexOf('.') + 1).toLowerCase();
    int slash = ext.indexOf('/');
    return slash >= 0 ? ext.substring(0, slash) : ext;
  }

  private String inferFileNameFromPath(String fileUrl) {
    if (!StringUtils.hasText(fileUrl)) {
      return "";
    }
    String path = fileUrl.split("\\?")[0];
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String inferFileType(String originalName, String contentType) {
    if (StringUtils.hasText(originalName) && originalName.contains(".")) {
      return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    }
    if (!StringUtils.hasText(contentType)) {
      return "";
    }
    String type = contentType.toLowerCase();
    if (type.contains("pdf")) {
      return "pdf";
    }
    if (type.contains("wordprocessingml") || type.contains("msword") || type.contains("docx")) {
      return "docx";
    }
    return "";
  }
}

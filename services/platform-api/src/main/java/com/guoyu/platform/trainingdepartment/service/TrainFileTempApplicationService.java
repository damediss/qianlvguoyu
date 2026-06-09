package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempSaveReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.support.TrainExcelTemplateHeaderValidator;
import com.guoyu.platform.trainingdepartment.vo.TrainFileTempVO;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainFileTempApplicationService {

  private final TrainFileTempMapper mapper;
  private final TrainExcelTemplateHeaderValidator excelHeaderValidator;

  public TrainFileTempApplicationService(
      TrainFileTempMapper mapper,
      TrainExcelTemplateHeaderValidator excelHeaderValidator) {
    this.mapper = mapper;
    this.excelHeaderValidator = excelHeaderValidator;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainFileTempVO> page(TrainFileTempQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainFileTempEntity> wrapper = buildListWrapper(tenantId, req);
    Page<TrainFileTempEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  /** 对齐 PHP file-temp-list */
  @Transactional(readOnly = true)
  public Map<String, Object> listForLegacy(TrainFileTempQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainFileTempEntity> wrapper = buildListWrapper(tenantId, req);
    List<TrainFileTempEntity> entities = mapper.selectList(wrapper.orderByDesc(TrainFileTempEntity::getId));
    List<Map<String, Object>> list = entities.stream().map(this::toLegacyItem).toList();
    return Map.of("list", list);
  }

  /** 对齐 PHP file-temp-save */
  @Transactional
  public Map<String, Object> saveLegacy(TrainFileTempSaveReq req) {
    if (!StringUtils.hasText(req.getTitle()) || !StringUtils.hasText(req.getPathSource())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请求不合法");
    }
    if ("开班申请表".equals(req.getTitle()) && !StringUtils.hasText(req.getCity())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择地市");
    }
    excelHeaderValidator.validate(req.getTitle(), req.getPathSource());
    String ext = resolveExt(req.getPathSource(), req.getExt());
    String city = "开班申请表".equals(req.getTitle()) ? req.getCity() : "";

    TrainFileTempEntity entity;
    if (req.getId() != null && req.getId() > 0) {
      entity = getExisting(req.getId());
      entity.setType(String.valueOf(req.getType() != null ? req.getType() : 1));
      entity.setTitle(req.getTitle());
      entity.setCity(city);
      entity.setPathSource(req.getPathSource());
      entity.setExt(ext);
      if (StringUtils.hasText(req.getPathUsed())) {
        entity.setPathUsed(req.getPathUsed());
      }
      mapper.updateById(entity);
    } else {
      Long tenantId = SecurityContext.currentUser().getTenantId();
      entity = new TrainFileTempEntity();
      entity.setTenantId(tenantId);
      entity.setType(String.valueOf(req.getType() != null ? req.getType() : 1));
      entity.setTitle(req.getTitle());
      entity.setCity(city);
      entity.setPathSource(req.getPathSource());
      entity.setExt(ext);
      entity.setPathUsed(req.getPathUsed());
      entity.setDeleted(0);
      mapper.insert(entity);
    }
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", entity.getId());
    info.put("title", entity.getTitle());
    info.put("path_source", entity.getPathSource());
    return Map.of("info", info);
  }

  @Transactional
  public Map<String, Object> batchDelete(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请求不合法");
    }
    int count = 0;
    for (Long id : ids) {
      if (id != null && id > 0) {
        delete(id);
        count++;
      }
    }
    return Map.of("count", count);
  }

  private LambdaQueryWrapper<TrainFileTempEntity> buildListWrapper(Long tenantId, TrainFileTempQueryReq req) {
    LambdaQueryWrapper<TrainFileTempEntity> wrapper = new LambdaQueryWrapper<TrainFileTempEntity>()
        .eq(TrainFileTempEntity::getTenantId, tenantId)
        .eq(TrainFileTempEntity::getDeleted, 0);
    if (req.getType() != null) {
      wrapper.eq(TrainFileTempEntity::getType, String.valueOf(req.getType()));
    }
    if (StringUtils.hasText(req.getIds())) {
      List<Long> idList = Arrays.stream(req.getIds().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Long::parseLong)
          .collect(Collectors.toList());
      if (!idList.isEmpty()) {
        wrapper.in(TrainFileTempEntity::getId, idList);
      }
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainFileTempEntity::getTitle, req.getKeyword());
    }
    return wrapper;
  }

  private String resolveExt(String pathSource, String ext) {
    if (StringUtils.hasText(ext)) {
      return ext.toLowerCase();
    }
    if (pathSource != null && pathSource.contains(".")) {
      return pathSource.substring(pathSource.lastIndexOf('.') + 1).toLowerCase();
    }
    return "";
  }

  private Map<String, Object> toLegacyItem(TrainFileTempEntity e) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", e.getId());
    item.put("type", e.getType());
    item.put("city", e.getCity());
    item.put("title", e.getTitle());
    item.put("path_source", e.getPathSource());
    item.put("path_used", e.getPathUsed());
    item.put("ext", e.getExt() != null ? e.getExt() : resolveExt(e.getPathSource(), null));
    String urlSource = toPublicUrl(e.getPathSource());
    item.put("urlSource", urlSource);
    item.put("urlUsed", StringUtils.hasText(e.getPathUsed()) ? toPublicUrl(e.getPathUsed()) : "");
    return item;
  }

  private String toPublicUrl(String path) {
    if (!StringUtils.hasText(path)) {
      return "";
    }
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    if (path.startsWith("/api/")) {
      return path;
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  @Transactional(readOnly = true)
  public TrainFileTempVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainFileTempVO create(TrainFileTempCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainFileTempEntity entity = new TrainFileTempEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.getType());
    entity.setTitle(req.getTitle());
    entity.setCity(req.getCity());
    entity.setPathSource(req.getPathSource());
    entity.setExt(resolveExt(req.getPathSource(), null));
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainFileTempVO update(Long id, TrainFileTempUpdateReq req) {
    TrainFileTempEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getType())) { entity.setType(req.getType()); }
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getCity())) { entity.setCity(req.getCity()); }
    if (StringUtils.hasText(req.getPathSource())) { entity.setPathSource(req.getPathSource()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainFileTempEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainFileTempEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainFileTempEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainFileTempEntity>()

        .eq(TrainFileTempEntity::getTenantId, tenantId)
        .eq(TrainFileTempEntity::getId, id)
        .eq(TrainFileTempEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainFileTempVO toVO(TrainFileTempEntity e) {
    return new TrainFileTempVO(
        e.getId(),
        e.getType(),
        e.getTitle(),
        e.getCity(),
        e.getPathSource(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}

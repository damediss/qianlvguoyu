package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainProtocolEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainProtocolMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainProtocolVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class TrainProtocolApplicationService {

  private final TrainProtocolMapper mapper;

  public TrainProtocolApplicationService(TrainProtocolMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainProtocolVO> page(TrainProtocolQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainProtocolEntity> wrapper = new LambdaQueryWrapper<TrainProtocolEntity>()
        .eq(TrainProtocolEntity::getTenantId, tenantId)
        .eq(TrainProtocolEntity::getDeleted, 0)
        .orderByDesc(TrainProtocolEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainProtocolEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainProtocolEntity::getContent, req.getKeyword()));
    }
    if (req.getProjectId() != null) {
      wrapper.eq(TrainProtocolEntity::getProjectId, req.getProjectId());
    }
    if (req.getType() != null) {
      wrapper.eq(TrainProtocolEntity::getType, req.getType());
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainProtocolEntity::getStatus, req.getStatus());
    }
    Page<TrainProtocolEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainProtocolVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainProtocolVO create(TrainProtocolCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainProtocolEntity entity = new TrainProtocolEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setProjectId(req.getProjectId());
    entity.setTitle(req.getTitle());
    entity.setType(req.getType());
    entity.setContent(req.getContent());
    entity.setVersion(req.getVersion());
    entity.setStatus(req.getStatus());
    entity.setSignedAt(req.getSignedAt());
    entity.setSignerId(req.getSignerId());
    entity.setApproverId(req.getApproverId());
    entity.setRejectReason(req.getRejectReason());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO update(Long id, TrainProtocolUpdateReq req) {
    TrainProtocolEntity entity = getExisting(id);
    if (req.getProjectId() != null) {
      entity.setProjectId(req.getProjectId());
    }
    if (req.getTitle() != null) {
      entity.setTitle(req.getTitle());
    }
    if (req.getType() != null) {
      entity.setType(req.getType());
    }
    if (req.getContent() != null) {
      entity.setContent(req.getContent());
    }
    if (req.getVersion() != null) {
      entity.setVersion(req.getVersion());
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getSignedAt() != null) {
      entity.setSignedAt(req.getSignedAt());
    }
    if (req.getSignerId() != null) {
      entity.setSignerId(req.getSignerId());
    }
    if (req.getApproverId() != null) {
      entity.setApproverId(req.getApproverId());
    }
    if (req.getRejectReason() != null) {
      entity.setRejectReason(req.getRejectReason());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  @Transactional
  public TrainProtocolVO submitReview(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有草稿状态的协议才能提交审核");
    }
    entity.setStatus(1);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO officeApprove(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有办公室审核状态的协议才能进行办公室审批");
    }
    entity.setStatus(2);
    entity.setApproverId(SecurityContext.currentUser().getUserId());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO gmApprove(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有总经理审核状态的协议才能进行总经理审批");
    }
    entity.setStatus(3);
    entity.setApproverId(SecurityContext.currentUser().getUserId());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO enterpriseConfirm(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有企业确认状态的协议才能进行企业确认");
    }
    entity.setStatus(4);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO sign(Long id) {
    TrainProtocolEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 4) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已确认状态的协议才能签署");
    }
    entity.setStatus(5);
    entity.setSignedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainProtocolVO reject(Long id, String reason) {
    TrainProtocolEntity entity = getExisting(id);
    Integer status = entity.getStatus();
    if (status == null || (status != 1 && status != 2 && status != 3)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有审核中的协议才能驳回");
    }
    entity.setStatus(0);
    entity.setRejectReason(reason);
    Integer currentVersion = entity.getVersion();
    entity.setVersion(currentVersion == null ? 1 : currentVersion + 1);
    mapper.updateById(entity);
    return toVO(entity);
  }

  private TrainProtocolEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainProtocolEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainProtocolEntity>()
        .eq(TrainProtocolEntity::getTenantId, tenantId)
        .eq(TrainProtocolEntity::getId, id)
        .eq(TrainProtocolEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainProtocolVO toVO(TrainProtocolEntity entity) {
    return new TrainProtocolVO(
        entity.getId(),
        entity.getProjectId(),
        entity.getTitle(),
        entity.getType(),
        entity.getContent(),
        entity.getVersion(),
        entity.getStatus(),
        entity.getSignedAt(),
        entity.getSignerId(),
        entity.getApproverId(),
        entity.getRejectReason(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getUserId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}

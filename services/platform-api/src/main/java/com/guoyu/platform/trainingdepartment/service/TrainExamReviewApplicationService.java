package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamReviewEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamReviewMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamReviewApplicationService {

  private final TrainExamReviewMapper mapper;

  public TrainExamReviewApplicationService(TrainExamReviewMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamReviewVO> page(TrainExamReviewQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamReviewEntity> wrapper = new LambdaQueryWrapper<TrainExamReviewEntity>()
        .eq(TrainExamReviewEntity::getTenantId, tenantId)
        .eq(TrainExamReviewEntity::getDeleted, 0)
        .orderByDesc(TrainExamReviewEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamReviewEntity::getId, req.getKeyword());
    }
    Page<TrainExamReviewEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamReviewVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamReviewVO create(TrainExamReviewCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamReviewEntity entity = new TrainExamReviewEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setPaperId(req.getPaperId());
    entity.setUserId(req.getUserId());
    entity.setNum(req.getNum());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamReviewVO update(Long id, TrainExamReviewUpdateReq req) {
    TrainExamReviewEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getPaperId() != null) { entity.setPaperId(req.getPaperId()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    if (req.getNum() != null) { entity.setNum(req.getNum()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamReviewEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamReviewEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamReviewEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamReviewEntity>()

        .eq(TrainExamReviewEntity::getTenantId, tenantId)
        .eq(TrainExamReviewEntity::getId, id)
        .eq(TrainExamReviewEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamReviewVO toVO(TrainExamReviewEntity e) {
    return new TrainExamReviewVO(
        e.getId(),
        e.getExamId(),
        e.getPaperId(),
        e.getUserId(),
        e.getNum(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}

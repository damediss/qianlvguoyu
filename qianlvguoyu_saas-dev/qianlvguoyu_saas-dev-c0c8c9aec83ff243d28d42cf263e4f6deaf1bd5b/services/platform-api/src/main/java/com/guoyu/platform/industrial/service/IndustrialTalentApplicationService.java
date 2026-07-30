package com.guoyu.platform.industrial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.industrial.dto.IndustrialTalentServiceQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialTalentTrainingQueryReq;
import com.guoyu.platform.industrial.entity.IndustrialTalentServiceEntity;
import com.guoyu.platform.industrial.entity.IndustrialTalentTrainingEntity;
import com.guoyu.platform.industrial.mapper.IndustrialTalentServiceMapper;
import com.guoyu.platform.industrial.mapper.IndustrialTalentTrainingMapper;
import com.guoyu.platform.industrial.vo.IndustrialTalentServiceVO;
import com.guoyu.platform.industrial.vo.IndustrialTalentTrainingVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IndustrialTalentApplicationService {

  private final IndustrialTalentTrainingMapper trainingMapper;
  private final IndustrialTalentServiceMapper serviceMapper;

  public IndustrialTalentApplicationService(
      IndustrialTalentTrainingMapper trainingMapper,
      IndustrialTalentServiceMapper serviceMapper
  ) {
    this.trainingMapper = trainingMapper;
    this.serviceMapper = serviceMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<IndustrialTalentTrainingVO> pageTraining(IndustrialTalentTrainingQueryReq req) {
    LambdaQueryWrapper<IndustrialTalentTrainingEntity> wrapper = new LambdaQueryWrapper<IndustrialTalentTrainingEntity>()
        .orderByAsc(IndustrialTalentTrainingEntity::getSortOrder)
        .orderByDesc(IndustrialTalentTrainingEntity::getCreatedAt);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(IndustrialTalentTrainingEntity::getName, req.getKeyword())
          .or()
          .like(IndustrialTalentTrainingEntity::getLevel, req.getKeyword())
          .or()
          .like(IndustrialTalentTrainingEntity::getWorkType, req.getKeyword())
          .or()
          .like(IndustrialTalentTrainingEntity::getZhiwei, req.getKeyword())
          .or()
          .like(IndustrialTalentTrainingEntity::getEducation, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getWorkType())) {
      wrapper.eq(IndustrialTalentTrainingEntity::getWorkType, req.getWorkType());
    }

    Page<IndustrialTalentTrainingEntity> page = trainingMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        wrapper
    );
    return new PageResponse<>(
        page.getRecords().stream().map(IndustrialTalentTrainingVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public PageResponse<IndustrialTalentServiceVO> pageService(IndustrialTalentServiceQueryReq req) {
    LambdaQueryWrapper<IndustrialTalentServiceEntity> wrapper = new LambdaQueryWrapper<IndustrialTalentServiceEntity>()
        .orderByAsc(IndustrialTalentServiceEntity::getSortOrder)
        .orderByDesc(IndustrialTalentServiceEntity::getCreatedAt);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(IndustrialTalentServiceEntity::getTitle, req.getKeyword())
          .or()
          .like(IndustrialTalentServiceEntity::getSource, req.getKeyword())
          .or()
          .like(IndustrialTalentServiceEntity::getServiceDate, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getSource())) {
      wrapper.eq(IndustrialTalentServiceEntity::getSource, req.getSource());
    }

    Page<IndustrialTalentServiceEntity> page = serviceMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        wrapper
    );
    return new PageResponse<>(
        page.getRecords().stream().map(IndustrialTalentServiceVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }
}

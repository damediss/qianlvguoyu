package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleSelfAssembler;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpPeopleSelfVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSignUpPeopleSelfApplicationService {

  private final TrainSignUpPeopleSelfMapper mapper;
  private final SignUpPeopleSelfAssembler assembler;

  public TrainSignUpPeopleSelfApplicationService(
      TrainSignUpPeopleSelfMapper mapper,
      SignUpPeopleSelfAssembler assembler) {
    this.mapper = mapper;
    this.assembler = assembler;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpPeopleSelfVO> page(TrainSignUpPeopleSelfQueryReq req) {
    if (req.getSignUpId() == null) {
      return new PageResponse<>(List.of(), req.getPage().longValue(), req.getPageSize().longValue(), 0L);
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, req.getSignUpId())
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
        .orderByDesc(TrainSignUpPeopleSelfEntity::getId);
    if (req.getStatus() != null) {
      wrapper.eq(TrainSignUpPeopleSelfEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainSignUpPeopleSelfEntity::getName, req.getKeyword())
          .or().like(TrainSignUpPeopleSelfEntity::getPhone, req.getKeyword())
          .or().like(TrainSignUpPeopleSelfEntity::getIdCardNumber, req.getKeyword()));
    }
    Page<TrainSignUpPeopleSelfEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<TrainSignUpPeopleSelfVO> items = page.getRecords().stream()
        .map(entity -> {
          TrainSignUpPeopleSelfVO vo = assembler.toVo(entity);
          try {
            assembler.enrich(vo);
          } catch (Exception ex) {
            log.warn("assembler.enrich failed for signUpPeople list id={}, ignore", entity.getId(), ex);
          }
          return vo;
        })
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSignUpPeopleSelfVO getById(Long id) {
    TrainSignUpPeopleSelfEntity entity = getExisting(id);
    TrainSignUpPeopleSelfVO vo = assembler.toVo(entity);
    try {
      assembler.enrich(vo);
    } catch (Exception ex) {
      // enrich 仅做 URL 解析/装饰，绝不能让列表/详情接口整体 500。
      // 失败时只丢失展示用 URL，原始字段已在 vo 里。
      log.warn("assembler.enrich failed for signUpPeople id={}, ignore and return base VO", id, ex);
    }
    return vo;
  }

  @Transactional
  public TrainSignUpPeopleSelfVO create(TrainSignUpPeopleSelfCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
    entity.setTenantId(tenantId);
    entity.setSignUpId(req.getSignUpId());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setName(req.getName());
    entity.setPhone(req.getPhone());
    entity.setIdCardNumber(req.getIdCardNumber());
    entity.setGender(req.getGender());
    entity.setEducation(req.getEducation());
    entity.setPhoto(req.getPhoto());
    entity.setContract(req.getContract());
    entity.setCertificate(req.getCertificate());
    entity.setStatus(req.getStatus() != null ? req.getStatus() : 0);
    entity.setCompanyStatus(0);
    entity.setHasPay(0);
    entity.setSchoolPaperHas(0);
    entity.setWorkTypePaperHas(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    mapper.insert(entity);
    TrainSignUpPeopleSelfVO vo = assembler.toVo(entity);
    assembler.enrich(vo);
    return vo;
  }

  @Transactional
  public TrainSignUpPeopleSelfVO update(Long id, TrainSignUpPeopleSelfUpdateReq req) {
    TrainSignUpPeopleSelfEntity entity = getExisting(id);
    assembler.applyUpdate(entity, req);
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    TrainSignUpPeopleSelfVO vo = assembler.toVo(entity);
    assembler.enrich(vo);
    return vo;
  }

  @Transactional
  public void delete(Long id) {
    getExisting(id);
    mapper.deleteById(id);
  }

  private TrainSignUpPeopleSelfEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpPeopleSelfEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getId, id)
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名人员不存在或已删除");
    }
    return entity;
  }
}

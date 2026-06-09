package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.PostCreateReq;
import com.guoyu.platform.system.dto.PostQueryReq;
import com.guoyu.platform.system.dto.PostUpdateReq;
import com.guoyu.platform.system.entity.PostEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.PostMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.vo.PostVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class PostApplicationService {

  private final PostMapper postMapper;
  private final UserMapper userMapper;

  public PostApplicationService(PostMapper postMapper, UserMapper userMapper) {
    this.postMapper = postMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<PostVO> page(PostQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<PostEntity> wrapper = new LambdaQueryWrapper<PostEntity>()
        .eq(PostEntity::getTenantId, tenantId)
        .eq(PostEntity::getDeleted, 0)
        .orderByAsc(PostEntity::getSortOrder)
        .orderByDesc(PostEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(PostEntity::getCode, req.getKeyword())
          .or()
          .like(PostEntity::getName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(PostEntity::getStatus, req.getStatus());
    }
    Page<PostEntity> page = postMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public PostVO detail(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return toVO(getExisting(tenantId, id));
  }

  @Transactional(readOnly = true)
  public List<PostVO> listAll() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<PostEntity> list = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
        .eq(PostEntity::getTenantId, tenantId)
        .eq(PostEntity::getStatus, "enabled")
        .eq(PostEntity::getDeleted, 0)
        .orderByAsc(PostEntity::getSortOrder));
    return list.stream().map(this::toVO).toList();
  }

  @Transactional
  public PostVO create(PostCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ensureCodeAvailable(tenantId, req.code());

    PostEntity entity = new PostEntity();
    entity.setTenantId(tenantId);
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setRemark(req.remark());
    entity.setDeleted(0);
    postMapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public PostVO update(Long id, PostUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    PostEntity entity = getExisting(tenantId, id);
    entity.setName(req.name());
    if (req.sortOrder() != null) {
      entity.setSortOrder(req.sortOrder());
    }
    if (StringUtils.hasText(req.status())) {
      entity.setStatus(req.status());
    }
    if (req.remark() != null) {
      entity.setRemark(req.remark());
    }
    postMapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    PostEntity entity = getExisting(tenantId, id);

    boolean usedByUsers = userMapper.exists(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getPostId, id)
        .eq(UserEntity::getDeleted, 0));
    if (usedByUsers) {
      throw new BusinessException(ErrorCode.CONFLICT, "Post is used by users");
    }

    entity.setDeleted(1);
    postMapper.deleteById(entity.getId());
  }

  private void ensureCodeAvailable(Long tenantId, String code) {
    boolean exists = postMapper.exists(new LambdaQueryWrapper<PostEntity>()
        .eq(PostEntity::getTenantId, tenantId)
        .eq(PostEntity::getCode, code)
        .eq(PostEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Post code already exists");
    }
  }

  private PostEntity getExisting(Long tenantId, Long id) {
    PostEntity entity = postMapper.selectOne(new LambdaQueryWrapper<PostEntity>()
        .eq(PostEntity::getTenantId, tenantId)
        .eq(PostEntity::getId, id)
        .eq(PostEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return entity;
  }

  private PostVO toVO(PostEntity entity) {
    return new PostVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getName(),
        entity.getSortOrder(),
        entity.getStatus(),
        entity.getRemark(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}

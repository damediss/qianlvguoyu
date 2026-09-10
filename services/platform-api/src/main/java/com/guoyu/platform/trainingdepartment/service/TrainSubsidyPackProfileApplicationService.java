package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackProfileEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackSlotEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyPackProfileMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyPackSlotMapper;
import com.guoyu.platform.trainingdepartment.vo.SubsidyPackProfileVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyPackProfileVO.SubsidyPackSlotVO;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainSubsidyPackProfileApplicationService {

  private final TrainSubsidyPackProfileMapper profileMapper;
  private final TrainSubsidyPackSlotMapper slotMapper;

  public TrainSubsidyPackProfileApplicationService(
      TrainSubsidyPackProfileMapper profileMapper,
      TrainSubsidyPackSlotMapper slotMapper) {
    this.profileMapper = profileMapper;
    this.slotMapper = slotMapper;
  }

  @Transactional(readOnly = true)
  public List<SubsidyPackProfileVO> list(String regionCode, String trainMode, Integer policyYear, String status) {
    LambdaQueryWrapper<TrainSubsidyPackProfileEntity> q = new LambdaQueryWrapper<TrainSubsidyPackProfileEntity>()
        .eq(TrainSubsidyPackProfileEntity::getDeleted, 0)
        .and(w -> w.eq(TrainSubsidyPackProfileEntity::getTenantId, 0)
            .or()
            .eq(TrainSubsidyPackProfileEntity::getTenantId, currentTenantOrZero()))
        .orderByDesc(TrainSubsidyPackProfileEntity::getPolicyYear)
        .orderByAsc(TrainSubsidyPackProfileEntity::getRegionCode);
    if (StringUtils.hasText(regionCode)) {
      q.and(w -> w.eq(TrainSubsidyPackProfileEntity::getRegionCode, regionCode.trim())
          .or()
          .like(TrainSubsidyPackProfileEntity::getRegionName, regionCode.trim()));
    }
    if (StringUtils.hasText(trainMode)) {
      q.eq(TrainSubsidyPackProfileEntity::getTrainMode, trainMode.trim());
    }
    if (policyYear != null) {
      q.eq(TrainSubsidyPackProfileEntity::getPolicyYear, policyYear);
    }
    if (StringUtils.hasText(status)) {
      q.eq(TrainSubsidyPackProfileEntity::getStatus, status.trim());
    }
    return profileMapper.selectList(q).stream().map(p -> toVo(p, false)).toList();
  }

  @Transactional(readOnly = true)
  public SubsidyPackProfileVO get(Long id) {
    TrainSubsidyPackProfileEntity p = require(id);
    return toVo(p, true);
  }

  /** 解析默认 active 配方 */
  @Transactional(readOnly = true)
  public TrainSubsidyPackProfileEntity resolveActive(String regionCode, String regionName, String trainMode, Integer policyYear) {
    return resolveActive(regionCode, regionName, trainMode, policyYear, false);
  }

  /**
   * @param softFallback ensure 场景：同年无匹配时回退邻近年 / 任意 active 配方
   */
  @Transactional(readOnly = true)
  public TrainSubsidyPackProfileEntity resolveActive(
      String regionCode, String regionName, String trainMode, Integer policyYear, boolean softFallback) {
    if (!StringUtils.hasText(trainMode) || policyYear == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "培训模式与政策年不能为空");
    }
    TrainSubsidyPackProfileEntity hit = findActive(regionCode, regionName, trainMode, policyYear);
    if (hit != null) return hit;
    if (softFallback) {
      // 同年任意地市 active → 邻近年同地市 → 任意 active
      hit = findActive(null, null, trainMode, policyYear);
      if (hit != null) return hit;
      for (int delta : new int[] { -1, 1, -2, 2 }) {
        hit = findActive(regionCode, regionName, trainMode, policyYear + delta);
        if (hit != null) return hit;
      }
      List<TrainSubsidyPackProfileEntity> any = profileMapper.selectList(
          new LambdaQueryWrapper<TrainSubsidyPackProfileEntity>()
              .eq(TrainSubsidyPackProfileEntity::getDeleted, 0)
              .eq(TrainSubsidyPackProfileEntity::getTrainMode, trainMode.trim())
              .eq(TrainSubsidyPackProfileEntity::getStatus, "active")
              .orderByDesc(TrainSubsidyPackProfileEntity::getIsDefault)
              .orderByDesc(TrainSubsidyPackProfileEntity::getPolicyYear)
              .orderByDesc(TrainSubsidyPackProfileEntity::getId)
              .last("LIMIT 1"));
      if (!any.isEmpty()) return any.get(0);
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST,
        "未找到配方：地市=" + (StringUtils.hasText(regionName) ? regionName : regionCode)
            + " 模式=" + trainMode + " 年=" + policyYear
            + "。请确认已发布对应地市配方，或在新建时指定配方");
  }

  private TrainSubsidyPackProfileEntity findActive(
      String regionCode, String regionName, String trainMode, Integer policyYear) {
    LambdaQueryWrapper<TrainSubsidyPackProfileEntity> q = new LambdaQueryWrapper<TrainSubsidyPackProfileEntity>()
        .eq(TrainSubsidyPackProfileEntity::getDeleted, 0)
        .eq(TrainSubsidyPackProfileEntity::getTrainMode, trainMode.trim())
        .eq(TrainSubsidyPackProfileEntity::getPolicyYear, policyYear)
        .eq(TrainSubsidyPackProfileEntity::getStatus, "active")
        .orderByDesc(TrainSubsidyPackProfileEntity::getIsDefault)
        .orderByDesc(TrainSubsidyPackProfileEntity::getId);
    List<TrainSubsidyPackProfileEntity> all = profileMapper.selectList(q);
    if (all.isEmpty()) return null;
    String code = regionCode != null ? regionCode.trim() : "";
    String name = regionName != null ? regionName.trim() : "";
    if (!StringUtils.hasText(code) && !StringUtils.hasText(name)) {
      return all.get(0);
    }
    return all.stream().filter(p -> matchRegion(p, code, name)).findFirst().orElse(null);
  }

  public List<TrainSubsidyPackSlotEntity> listSlots(Long profileId) {
    return slotMapper.selectList(new LambdaQueryWrapper<TrainSubsidyPackSlotEntity>()
            .eq(TrainSubsidyPackSlotEntity::getProfileId, profileId)
            .eq(TrainSubsidyPackSlotEntity::getDeleted, 0))
        .stream()
        .sorted(Comparator.comparing(s -> s.getSortOrder() == null ? 0 : s.getSortOrder()))
        .toList();
  }

  private boolean matchRegion(TrainSubsidyPackProfileEntity p, String code, String name) {
    if (StringUtils.hasText(code) && code.equals(p.getRegionCode())) return true;
    if (StringUtils.hasText(name) && (name.equals(p.getRegionName()) || name.contains(p.getRegionCode())
        || p.getRegionName().contains(name) || name.contains(p.getRegionName()))) {
      return true;
    }
    // 来宾市 → 来宾
    if (StringUtils.hasText(name) && StringUtils.hasText(p.getRegionCode()) && name.contains(p.getRegionCode())) {
      return true;
    }
    return false;
  }

  private TrainSubsidyPackProfileEntity require(Long id) {
    TrainSubsidyPackProfileEntity p = profileMapper.selectById(id);
    if (p == null || (p.getDeleted() != null && p.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "配方不存在");
    }
    return p;
  }

  private SubsidyPackProfileVO toVo(TrainSubsidyPackProfileEntity p, boolean withSlots) {
    List<SubsidyPackSlotVO> slots = withSlots
        ? listSlots(p.getId()).stream()
            .map(s -> new SubsidyPackSlotVO(
                s.getId(), s.getSlotCode(), s.getTitle(),
                s.getRequiredFlag() != null && s.getRequiredFlag() == 1,
                s.getSortOrder(), s.getRenderer(), s.getTemplateId(),
                s.getDataBinding(), s.getFolderPath(), s.getRemark()))
            .toList()
        : List.of();
    return new SubsidyPackProfileVO(
        p.getId(), p.getRegionCode(), p.getRegionName(), p.getTrainMode(), p.getPolicyYear(),
        p.getProfileVersion(), p.getTitle(), p.getStatus(), p.getIsDefault(), p.getRemark(), slots);
  }

  private Long currentTenantOrZero() {
    try {
      Long t = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
      return t == null ? 0L : t;
    } catch (Exception e) {
      return 0L;
    }
  }
}

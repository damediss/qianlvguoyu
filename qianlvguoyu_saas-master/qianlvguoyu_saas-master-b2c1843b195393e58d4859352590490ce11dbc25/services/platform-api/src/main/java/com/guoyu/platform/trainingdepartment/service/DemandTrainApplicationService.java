package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.DemandConvertReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainCreateReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainQueryReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.DemandTrainEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.mapper.DemandTrainMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.vo.DemandPendingVO;
import com.guoyu.platform.trainingdepartment.vo.DemandTrainVO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DemandTrainApplicationService {

  private final DemandTrainMapper mapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainInfoListMapper trainInfoListMapper;

  public DemandTrainApplicationService(DemandTrainMapper mapper, OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper, TrainInfoListMapper trainInfoListMapper) {
    this.mapper = mapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.trainInfoListMapper = trainInfoListMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<DemandTrainVO> page(DemandTrainQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<DemandTrainEntity> wrapper = new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getDeleted, 0)
        .orderByDesc(DemandTrainEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(DemandTrainEntity::getId, req.getKeyword());
    }
    Page<DemandTrainEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public DemandTrainVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public DemandTrainVO create(DemandTrainCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DemandTrainEntity entity = new DemandTrainEntity();
    entity.setTenantId(tenantId);
    entity.setOrgId(req.getOrgId());
    entity.setContent(req.getContent());
    entity.setNums(req.getNums());
    entity.setStatus(req.getStatus());
    entity.setAddtime(req.getAddtime());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public DemandTrainVO update(Long id, DemandTrainUpdateReq req) {
    DemandTrainEntity entity = getExisting(id);
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getContent())) { entity.setContent(req.getContent()); }
    if (req.getNums() != null) { entity.setNums(req.getNums()); }
    if (req.getStatus() != null) { entity.setStatus(req.getStatus()); }
    if (req.getAddtime() != null) { entity.setAddtime(req.getAddtime()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    DemandTrainEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  // ======== 需求管理业务方法 ========

  @Transactional(readOnly = true)
  public PageResponse<DemandPendingVO> getPendings(Integer page, Integer pageSize, String keyword) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<DemandTrainEntity> wrapper = new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getStatus, 0)
        .eq(DemandTrainEntity::getDeleted, 0)
        .orderByDesc(DemandTrainEntity::getAddtime);
    if (StringUtils.hasText(keyword)) {
      wrapper.like(DemandTrainEntity::getContent, keyword);
    }
    Page<DemandTrainEntity> result = mapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 30), wrapper);

    // 获取所有企业/工作室信息用于填充
    List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .in(OrgEntity::getOrgType, "company", "master")
        .eq(OrgEntity::getDeleted, 0));
    Map<Long, OrgEntity> orgMap = orgs.stream()
        .collect(Collectors.toMap(OrgEntity::getId, o -> o, (a, b) -> a));
    Map<String, String> typeLabels = Map.of("master", "技能大师工作室", "company", "企业");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    List<DemandPendingVO> vos = result.getRecords().stream().map(e -> {
      OrgEntity org = orgMap.get(e.getOrgId());
      String company = org != null ? org.getName() : "-";
      String companyType = "-";
      if (org != null && org.getOrgType() != null) {
        companyType = typeLabels.getOrDefault(org.getOrgType(), org.getOrgType());
      }
      String addtimeCn = "-";
      if (e.getAddtime() != null && e.getAddtime() > 0) {
        addtimeCn = LocalDateTime.ofInstant(Instant.ofEpochSecond(e.getAddtime()), ZoneId.systemDefault())
            .format(fmt);
      }
      return new DemandPendingVO(e.getId(), e.getOrgId(), e.getContent(), e.getNums(),
          e.getStatus(), e.getAddtime(), addtimeCn, company, companyType);
    }).toList();

    return new PageResponse<>(vos, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getPublicData(List<String> tags) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    boolean all = (tags == null || tags.isEmpty());
    Map<String, Object> data = new LinkedHashMap<>();

    // 企业/工作室列表
    if (all || tags.contains("company")) {
      List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, tenantId)
          .in(OrgEntity::getOrgType, "company", "master")
          .eq(OrgEntity::getDeleted, 0));
      List<Map<String, Object>> companyList = orgs.stream().map(o -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("title", o.getName());
        m.put("tag", o.getOrgType());
        return m;
      }).toList();
      data.put("company_list", companyList);
    }

    // 工种列表
    if (all || tags.contains("worktype")) {
      List<ExamWorkTypeEntity> workTypes = examWorkTypeMapper.selectList(
          new LambdaQueryWrapper<ExamWorkTypeEntity>()
              .eq(ExamWorkTypeEntity::getDeleted, 0)
              .orderByAsc(ExamWorkTypeEntity::getParentId));
      Map<Integer, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
      List<Map<String, Object>> workTypeList = new ArrayList<>();
      for (ExamWorkTypeEntity wt : workTypes) {
        if (wt.getParentId() != null && wt.getParentId() > 0) {
          grouped.computeIfAbsent(wt.getParentId(), k -> new ArrayList<>())
              .add(Map.of("id", wt.getId(), "title", wt.getTitle()));
        } else {
          Map<String, Object> group = new LinkedHashMap<>();
          group.put("id", wt.getId());
          group.put("title", wt.getTitle());
          group.put("options", new ArrayList<>());
          workTypeList.add(group);
        }
      }
      for (Map<String, Object> group : workTypeList) {
        Integer groupId = (Integer) group.get("id");
        List<Map<String, Object>> options = grouped.get(groupId);
        if (options != null) {
          group.put("options", options);
        }
      }
      data.put("worktype_list", workTypeList);
    }

    // 培训信息列表
    if (all || tags.contains("traininfo")) {
      List<TrainInfoListEntity> trainInfoList = trainInfoListMapper.selectList(
          new LambdaQueryWrapper<TrainInfoListEntity>()
              .eq(TrainInfoListEntity::getTenantId, tenantId)
              .eq(TrainInfoListEntity::getDeleted, 0));
      List<Map<String, Object>> traininfoList = trainInfoList.stream().map(t -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        return m;
      }).toList();
      data.put("traininfo_list", traininfoList);
    }

    return data;
  }

  @Transactional
  public Map<String, Object> convertToTraining(DemandConvertReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // Look up company name
    String companyName = "";
    if (req.getCompanyId() != null) {
      OrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, tenantId)
          .eq(OrgEntity::getId, req.getCompanyId())
          .eq(OrgEntity::getDeleted, 0));
      if (org != null) {
        companyName = org.getName();
      }
    }

    TrainInfoListEntity entity = new TrainInfoListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setCompanyId(req.getCompanyId());
    entity.setCompanyName(companyName);
    entity.setTrainCompanyName(req.getTrainCompanyName() != null ? req.getTrainCompanyName() : "千铝工投");
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setNum(req.getNum());
    entity.setPassStatus(1);
    entity.setStatus(0);
    entity.setSortNumber(1);
    entity.setDeleted(0);
    if (req.getTraindemandId() != null) {
      entity.setTraindemandId(req.getTraindemandId());
    }
    trainInfoListMapper.insert(entity);

    // 标记原需求为已处理
    if (req.getTraindemandId() != null && req.getTraindemandId() > 0) {
      DemandTrainEntity demand = mapper.selectOne(new LambdaQueryWrapper<DemandTrainEntity>()
          .eq(DemandTrainEntity::getTenantId, tenantId)
          .eq(DemandTrainEntity::getId, req.getTraindemandId())
          .eq(DemandTrainEntity::getDeleted, 0));
      if (demand != null) {
        demand.setStatus(1);
        mapper.updateById(demand);
      }
    }

    return Map.of("id", entity.getId(), "title", entity.getTitle());
  }

  private DemandTrainEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DemandTrainEntity entity = mapper.selectOne(new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getId, id)
        .eq(DemandTrainEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private DemandTrainVO toVO(DemandTrainEntity e) {
    return new DemandTrainVO(
        e.getId(),
        e.getOrgId(),
        e.getContent(),
        e.getNums(),
        e.getStatus(),
        e.getAddtime(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}

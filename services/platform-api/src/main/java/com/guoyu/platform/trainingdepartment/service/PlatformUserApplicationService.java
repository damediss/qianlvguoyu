package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserSaveReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 学员管理（对齐 yun_shang student.vue / YunstudentController） */
@Service
public class PlatformUserApplicationService {

  private final PlatformUserMapper mapper;
  private final PlatformUserDetailMapper detailMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public PlatformUserApplicationService(
      PlatformUserMapper mapper,
      PlatformUserDetailMapper detailMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.detailMapper = detailMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<PlatformUserVO> page(PlatformUserQueryReq req) {
    boolean platformAdmin = operatorScopeService.isPlatformAdmin();
    LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .orderByDesc(PlatformUserEntity::getId);
    // 机构账号仅看本机构；平台管理员可看全部（可选再按企业筛选）
    if (!platformAdmin) {
      wrapper.eq(PlatformUserEntity::getOrgId, resolveOrgId());
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getCompanyId());
    if (companyFilter != null) {
      wrapper.eq(PlatformUserEntity::getCompanyId, companyFilter);
    }
    if (StringUtils.hasText(req.getKeyword())) {
      List<Long> companyIds = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
              .like(OrgEntity::getName, req.getKeyword()))
          .stream()
          .map(OrgEntity::getId)
          .toList();
      wrapper.and(w -> {
        w.like(PlatformUserEntity::getRealname, req.getKeyword())
            .or().like(PlatformUserEntity::getIdcard, req.getKeyword())
            .or().like(PlatformUserEntity::getPhone, req.getKeyword());
        if (!companyIds.isEmpty()) {
          w.or().in(PlatformUserEntity::getCompanyId, companyIds);
        }
      });
    }
    Page<PlatformUserEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, String> companyNames = resolveCompanyNames(page.getRecords());
    List<PlatformUserVO> items = page.getRecords().stream()
        .map(e -> toVO(e, companyNames.get(e.getCompanyId())))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public PlatformUserVO save(PlatformUserSaveReq req) {
    operatorScopeService.requireOrgOperator();
    validateIdentity(req.getIdcard(), req.getPhone(), req.getId());
    Long orgId = resolveOrgId();
    if (req.getId() != null && req.getId() > 0) {
      PlatformUserEntity entity = getExisting(req.getId(), orgId);
      entity.setRealname(trim(req.getRealname()));
      entity.setIdcard(trim(req.getIdcard()));
      entity.setPhone(trim(req.getPhone()));
      if (req.getCompanyId() != null) {
        entity.setCompanyId(req.getCompanyId());
      }
      if (StringUtils.hasText(req.getPassword())) {
        String stat = StringUtils.hasText(entity.getStat()) ? entity.getStat() : randomStat();
        entity.setStat(stat);
        entity.setPassword(md5(req.getPassword(), stat));
      } else if (!StringUtils.hasText(entity.getPassword())) {
        String stat = StringUtils.hasText(entity.getStat()) ? entity.getStat() : randomStat();
        entity.setStat(stat);
        entity.setPassword(md5("123123", stat));
      }
      mapper.updateById(entity);
      syncDetail(entity);
      return toVO(entity, companyName(entity.getCompanyId()));
    }
    PlatformUserEntity entity = new PlatformUserEntity();
    entity.setRealname(trim(req.getRealname()));
    entity.setIdcard(trim(req.getIdcard()));
    entity.setPhone(trim(req.getPhone()));
    entity.setCompanyId(req.getCompanyId());
    entity.setOrgId(orgId);
    entity.setAddtime(System.currentTimeMillis() / 1000);
    entity.setDeleted(0);
    String stat = randomStat();
    entity.setStat(stat);
    entity.setPassword(md5(StringUtils.hasText(req.getPassword()) ? req.getPassword() : "123123", stat));
    mapper.insert(entity);
    syncDetail(entity);
    return toVO(entity, companyName(entity.getCompanyId()));
  }

  @Transactional
  public void delete(Long id) {
    operatorScopeService.requireOrgOperator();
    PlatformUserEntity entity = getExisting(id, resolveOrgId());
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private PlatformUserEntity getExisting(Long id, Long orgId) {
    LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getId, id)
        .eq(PlatformUserEntity::getDeleted, 0);
    if (!operatorScopeService.isPlatformAdmin()) {
      wrapper.eq(PlatformUserEntity::getOrgId, orgId);
    }
    PlatformUserEntity entity = mapper.selectOne(wrapper);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "学员不存在");
    }
    return entity;
  }

  private void validateIdentity(String idcard, String phone, Long excludeId) {
    if (idcard.length() != 18) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号位数不正确");
    }
    if (phone.length() != 11) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号位数不正确");
    }
    LambdaQueryWrapper<PlatformUserEntity> idcardWrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getIdcard, idcard);
    if (excludeId != null && excludeId > 0) {
      idcardWrapper.ne(PlatformUserEntity::getId, excludeId);
    }
    if (mapper.selectCount(idcardWrapper) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该身份证号已经存在");
    }
    LambdaQueryWrapper<PlatformUserEntity> phoneWrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getPhone, phone);
    if (excludeId != null && excludeId > 0) {
      phoneWrapper.ne(PlatformUserEntity::getId, excludeId);
    }
    if (mapper.selectCount(phoneWrapper) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该手机号已经存在");
    }
  }

  private void syncDetail(PlatformUserEntity user) {
    PlatformUserDetailEntity detail = detailMapper.selectOne(new LambdaQueryWrapper<PlatformUserDetailEntity>()
        .eq(PlatformUserDetailEntity::getIdcard, user.getIdcard())
        .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (detail == null) {
      detail = new PlatformUserDetailEntity();
      detail.setUserId(user.getId());
      detail.setIdcard(user.getIdcard());
      detail.setDeleted(0);
    }
    detail.setRealname(user.getRealname());
    detail.setPhone(user.getPhone());
    if (detail.getId() == null) {
      detailMapper.insert(detail);
    } else {
      detailMapper.updateById(detail);
    }
  }

  private Long resolveOrgId() {
    UserEntity user = userMapper.selectById(SecurityContext.currentUser().getUserId());
    if (user != null && user.getOrgId() != null) {
      return user.getOrgId();
    }
    return 0L;
  }

  private Map<Long, String> resolveCompanyNames(List<PlatformUserEntity> users) {
    List<Long> ids = users.stream()
        .map(PlatformUserEntity::getCompanyId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, String> map = new LinkedHashMap<>();
    if (ids.isEmpty()) {
      return map;
    }
    for (OrgEntity org : orgMapper.selectBatchIds(ids)) {
      map.put(org.getId(), org.getName());
    }
    return map;
  }

  private String companyName(Long companyId) {
    if (companyId == null || companyId <= 0) {
      return "-";
    }
    OrgEntity org = orgMapper.selectById(companyId);
    return org != null ? org.getName() : "-";
  }

  private PlatformUserVO toVO(PlatformUserEntity e, String companyName) {
    String addtimeCn = "-";
    if (e.getAddtime() != null && e.getAddtime() > 0) {
      addtimeCn = LocalDateTime.ofInstant(Instant.ofEpochSecond(e.getAddtime()), ZoneId.systemDefault())
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    return new PlatformUserVO(
        e.getId(), e.getRealname(), e.getIdcard(), e.getPhone(),
        e.getOrgId(), e.getCompanyId(), companyName != null ? companyName : "-", addtimeCn);
  }

  private static String trim(String value) {
    return value == null ? "" : value.replace(" ", "");
  }

  private static String randomStat() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 4);
  }

  private static String md5(String password, String stat) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest((password + stat).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "密码加密失败");
    }
  }
}

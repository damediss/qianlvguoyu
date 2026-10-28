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
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 学员管理（对齐 yun_shang student.vue / YunstudentController） */
@Service
public class PlatformUserApplicationService {

  private final PlatformUserMapper mapper;
  private final PlatformUserDetailMapper detailMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public PlatformUserApplicationService(
      PlatformUserMapper mapper,
      PlatformUserDetailMapper detailMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.detailMapper = detailMapper;
    this.peopleListMapper = peopleListMapper;
    this.signUpPeopleMapper = signUpPeopleMapper;
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
    java.util.Set<Long> companyScope =
        operatorScopeService.resolveCompanyIdScope(req.getCompanyId());
    if (companyScope != null) {
      if (companyScope.isEmpty()) {
        wrapper.eq(PlatformUserEntity::getCompanyId, -1L);
      } else {
        wrapper.in(PlatformUserEntity::getCompanyId, companyScope);
      }
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
    String realname = trim(req.getRealname());
    String idcard = trim(req.getIdcard()).replace(" ", "");
    String phone = trim(req.getPhone()).replace(" ", "");
    validateIdentity(idcard, phone, req.getId());
    Long orgId = resolveOrgId();
    if (req.getId() != null && req.getId() > 0) {
      PlatformUserEntity entity = getExisting(req.getId(), orgId);
      String previousIdCard = entity.getIdcard();
      entity.setRealname(realname);
      entity.setIdcard(idcard);
      entity.setPhone(phone);
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
      syncLinkedProfiles(entity, previousIdCard);
      return toVO(entity, companyName(entity.getCompanyId()));
    }
    PlatformUserEntity entity = new PlatformUserEntity();
    entity.setRealname(realname);
    entity.setIdcard(idcard);
    entity.setPhone(phone);
    entity.setCompanyId(req.getCompanyId());
    entity.setOrgId(orgId);
    entity.setAddtime(System.currentTimeMillis() / 1000);
    entity.setDeleted(0);
    String stat = randomStat();
    entity.setStat(stat);
    entity.setPassword(md5(StringUtils.hasText(req.getPassword()) ? req.getPassword() : "123123", stat));
    mapper.insert(entity);
    syncDetail(entity);
    syncLinkedProfiles(entity, null);
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
    if (idcard == null || idcard.length() != 18) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号位数不正确");
    }
    if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入11位有效手机号");
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

  /**
   * 学员账号是主数据。班级花名册、报名人员按账号或身份证指向同一个人，改手机号/姓名/证件号时一起回写。
   * 跟班考勤的手机列读花名册，不单独存号码。
   */
  private void syncLinkedProfiles(PlatformUserEntity user, String previousIdCard) {
    if (user.getId() == null) {
      return;
    }
    List<String> idCards = new ArrayList<>();
    if (StringUtils.hasText(previousIdCard)) {
      idCards.add(previousIdCard.trim());
    }
    if (StringUtils.hasText(user.getIdcard()) && !idCards.contains(user.getIdcard().trim())) {
      idCards.add(user.getIdcard().trim());
    }

    LambdaQueryWrapper<TrainPeopleListEntity> peopleQuery = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .and(w -> {
          w.eq(TrainPeopleListEntity::getPlatformUserId, user.getId());
          if (!idCards.isEmpty()) {
            w.or().in(TrainPeopleListEntity::getIdCard, idCards);
          }
        });
    for (TrainPeopleListEntity row : peopleListMapper.selectList(peopleQuery)) {
      boolean changed = false;
      if (!Objects.equals(row.getPhone(), user.getPhone())) {
        row.setPhone(user.getPhone());
        changed = true;
      }
      if (StringUtils.hasText(user.getRealname()) && !Objects.equals(row.getRealname(), user.getRealname())) {
        row.setRealname(user.getRealname());
        changed = true;
      }
      if (StringUtils.hasText(user.getIdcard()) && !Objects.equals(row.getIdCard(), user.getIdcard())) {
        row.setIdCard(user.getIdcard());
        changed = true;
      }
      if (row.getPlatformUserId() == null || row.getPlatformUserId() <= 0) {
        row.setPlatformUserId(user.getId());
        changed = true;
      }
      if (changed) {
        peopleListMapper.updateById(row);
      }
    }

    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> signUpQuery = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
        .and(w -> {
          w.eq(TrainSignUpPeopleSelfEntity::getPlatformUserId, user.getId());
          if (!idCards.isEmpty()) {
            w.or().in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards);
          }
        });
    for (TrainSignUpPeopleSelfEntity row : signUpPeopleMapper.selectList(signUpQuery)) {
      boolean changed = false;
      if (!Objects.equals(row.getPhone(), user.getPhone())) {
        row.setPhone(user.getPhone());
        changed = true;
      }
      if (StringUtils.hasText(user.getRealname()) && !Objects.equals(row.getName(), user.getRealname())) {
        row.setName(user.getRealname());
        changed = true;
      }
      if (StringUtils.hasText(user.getIdcard()) && !Objects.equals(row.getIdCardNumber(), user.getIdcard())) {
        row.setIdCardNumber(user.getIdcard());
        changed = true;
      }
      if (row.getPlatformUserId() == null || row.getPlatformUserId() <= 0) {
        row.setPlatformUserId(user.getId());
        changed = true;
      }
    if (changed) {
      signUpPeopleMapper.updateById(row);
      }
    }
  }

  /**
   * 手机号有值时必须是 11 位有效号；空值允许（部分场景可不填）。
   */
  public static void assertPhoneIfPresent(String phone) {
    if (!StringUtils.hasText(phone)) {
      return;
    }
    String normalized = phone.replace(" ", "").trim();
    if (!normalized.matches("^1[3-9]\\d{9}$")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入11位有效手机号");
    }
  }

  /**
   * 花名册 / 报名人员回写学员主数据，并再同步到其他关联表。
   * 找不到账号时静默跳过。
   */
  @Transactional
  public void syncFromExternalProfile(Long platformUserId, String idCard, String realname, String phone) {
    String normalizedIdCard = trim(idCard);
    String normalizedPhone = trim(phone);
    String normalizedName = trim(realname);
    if ((platformUserId == null || platformUserId <= 0) && !StringUtils.hasText(normalizedIdCard)) {
      return;
    }

    PlatformUserEntity user = null;
    if (platformUserId != null && platformUserId > 0) {
      user = mapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
          .eq(PlatformUserEntity::getId, platformUserId)
          .eq(PlatformUserEntity::getDeleted, 0)
          .last("LIMIT 1"));
    }
    if (user == null && StringUtils.hasText(normalizedIdCard)) {
      user = mapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
          .eq(PlatformUserEntity::getDeleted, 0)
          .eq(PlatformUserEntity::getIdcard, normalizedIdCard)
          .last("LIMIT 1"));
    }
    if (user == null) {
      return;
    }

    String previousIdCard = user.getIdcard();
    boolean changed = false;
    if (StringUtils.hasText(normalizedName) && !Objects.equals(user.getRealname(), normalizedName)) {
      user.setRealname(normalizedName);
      changed = true;
    }
    if (StringUtils.hasText(normalizedIdCard)
        && normalizedIdCard.length() == 18
        && !Objects.equals(user.getIdcard(), normalizedIdCard)
        && !idCardTaken(normalizedIdCard, user.getId())) {
      user.setIdcard(normalizedIdCard);
      changed = true;
    }
    if (StringUtils.hasText(normalizedPhone)
        && normalizedPhone.matches("^1[3-9]\\d{9}$")
        && !Objects.equals(user.getPhone(), normalizedPhone)
        && !phoneTaken(normalizedPhone, user.getId())) {
      user.setPhone(normalizedPhone);
      changed = true;
    }
    if (changed) {
      mapper.updateById(user);
      syncDetail(user);
    }
    syncLinkedProfiles(user, previousIdCard);
  }

  private boolean idCardTaken(String idcard, Long excludeId) {
    LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getIdcard, idcard);
    if (excludeId != null && excludeId > 0) {
      wrapper.ne(PlatformUserEntity::getId, excludeId);
    }
    return mapper.selectCount(wrapper) > 0;
  }

  private boolean phoneTaken(String phone, Long excludeId) {
    LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getPhone, phone);
    if (excludeId != null && excludeId > 0) {
      wrapper.ne(PlatformUserEntity::getId, excludeId);
    }
    return mapper.selectCount(wrapper) > 0;
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

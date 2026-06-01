package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.CourseKejianEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.mapper.CourseKejianMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训人员列表管理服务实现
 */
@Service
@Slf4j
public class TrainPeopleListApplicationService {

  private final TrainPeopleListMapper mapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainTeacherBankMapper teacherBankMapper;
  private final CourseKejianMapper courseKejianMapper;

  public TrainPeopleListApplicationService(TrainPeopleListMapper mapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainTeacherBankMapper teacherBankMapper,
      CourseKejianMapper courseKejianMapper) {
    this.mapper = mapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.teacherBankMapper = teacherBankMapper;
    this.courseKejianMapper = courseKejianMapper;
  }

  // ==================== 基础CRUD ====================

  @Transactional(readOnly = true)
  public PageResponse<TrainPeopleListVO> page(TrainPeopleListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainPeopleListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainId, req.getTrainId());
    }
    if (req.getTrainClassId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainClassId, req.getTrainClassId());
    }
    if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainPeopleListEntity::getType, req.getType());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainPeopleListEntity::getRealname, req.getKeyword())
          .or()
          .like(TrainPeopleListEntity::getPhone, req.getKeyword()));
    }
    Page<TrainPeopleListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainPeopleListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainPeopleListVO create(TrainPeopleListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity entity = new TrainPeopleListEntity();
    entity.setTenantId(tenantId);
    entity.setTrainId(req.getTrainId());
    entity.setTrainClassId(req.getTrainClassId());
    entity.setType(req.getType());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    entity.setIdCard(req.getIdCard());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainPeopleListVO update(Long id, TrainPeopleListUpdateReq req) {
    TrainPeopleListEntity entity = getExisting(id);
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainPeopleListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  @Transactional
  public void batchAudit(SignupAuditReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    mapper.update(null, new LambdaUpdateWrapper<TrainPeopleListEntity>()
        .set(TrainPeopleListEntity::getStatus, req.getStatus())
        .in(TrainPeopleListEntity::getId, req.getIds())
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0));
  }

  // ==================== 班级学员 ====================

  /** 班级学员列表（type=student, by classId） */
  @Transactional(readOnly = true)
  public Map<String, Object> classStudentList(Long classId, int page, int pageSize) {
    LambdaQueryWrapper<TrainPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .orderByDesc(TrainPeopleListEntity::getId);
    Page<TrainPeopleListEntity> mpPage = mapper.selectPage(new Page<>(page, pageSize), wrapper);

    List<Map<String, Object>> list = mpPage.getRecords().stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("realname", e.getRealname());
      item.put("phone", e.getPhone());
      item.put("train_id", e.getTrainId());
      item.put("train_class_id", e.getTrainClassId());
      item.put("id_card", e.getIdCard());
      item.put("is_leader", e.getIsLeader());
      item.put("is_leader_cn", e.getIsLeader() != null && e.getIsLeader() == 1 ? "是" : "否");
      item.put("is_committee", e.getIsCommittee());
      item.put("is_committee_cn", e.getIsCommittee() != null && e.getIsCommittee() == 1 ? "是" : "否");
      item.put("exam_score", e.getExamScore());
      item.put("real_score", e.getRealScore());
      item.put("normal_score", e.getNormalScore());
      return item;
    }).toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    result.put("limit", pageSize);
    return result;
  }

  /** 编辑学员信息 */
  @Transactional
  public Map<String, Object> saveStudent(Map<String, Object> params) {
    Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : null;
    if (id == null || id <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持编辑已有学员");
    }
    TrainPeopleListEntity entity = getExisting(id);
    if (params.get("realname") != null) entity.setRealname(params.get("realname").toString());
    if (params.get("phone") != null) entity.setPhone(params.get("phone").toString());
    if (params.get("id_card") != null) entity.setIdCard(params.get("id_card").toString());
    if (params.get("normal_score") != null) entity.setNormalScore(new java.math.BigDecimal(params.get("normal_score").toString()));
    if (params.get("is_leader") != null) entity.setIsLeader(Integer.valueOf(params.get("is_leader").toString()));
    if (params.get("is_committee") != null) entity.setIsCommittee(Integer.valueOf(params.get("is_committee").toString()));
    mapper.updateById(entity);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", entity.getId());
    result.put("realname", entity.getRealname());
    return result;
  }

  /** 批量删除学员 */
  @Transactional
  public Map<String, Object> batchDeleteStudents(List<Long> ids) {
    int count = 0;
    for (Long id : ids) {
      TrainPeopleListEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        entity.setDeleted(1);
        mapper.updateById(entity);
        count++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    return result;
  }

  /** 可导入的报名人员列表 */
  @Transactional(readOnly = true)
  public Map<String, Object> signUpSelfList(Long traininfoId, String keyword) {
    TrainSignUpSelfEntity signUp = signUpSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTrainInfoId, traininfoId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUp == null) {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("lists", List.of());
      return result;
    }
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId());
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(TrainSignUpPeopleSelfEntity::getName, keyword)
          .or()
          .like(TrainSignUpPeopleSelfEntity::getPhone, keyword));
    }
    List<TrainSignUpPeopleSelfEntity> people = signUpPeopleSelfMapper.selectList(wrapper);
    List<Map<String, Object>> lists = people.stream().map(p -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", p.getId());
      item.put("name", p.getName());
      item.put("phone", p.getPhone());
      return item;
    }).toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("lists", lists);
    return result;
  }

  /** 从报名人员导入学员 */
  @Transactional
  public Map<String, Object> importStudents(Long traininfoId, Long classId, List<Long> studentIds) {
    List<TrainSignUpPeopleSelfEntity> signUpPeople = signUpPeopleSelfMapper.selectBatchIds(studentIds);
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (Long sid : studentIds) {
      TrainSignUpPeopleSelfEntity sp = signUpPeople.stream()
          .filter(p -> p.getId().equals(sid)).findFirst().orElse(null);
      if (sp == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", sid);
        f.put("reason", "报名人员不存在");
        failed.add(f);
        continue;
      }
      Long existCount = mapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
          .eq(TrainPeopleListEntity::getType, "student")
          .eq(TrainPeopleListEntity::getDeleted, 0)
          .eq(TrainPeopleListEntity::getTrainId, traininfoId)
          .eq(TrainPeopleListEntity::getTrainClassId, classId)
          .eq(TrainPeopleListEntity::getPlatformUserId, sp.getPlatformUserId()));
      if (existCount != null && existCount > 0) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", sid);
        f.put("reason", "已存在该学员");
        failed.add(f);
        continue;
      }

      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType("student");
      entity.setTrainId(traininfoId);
      entity.setTrainClassId(classId);
      entity.setRealname(sp.getName());
      entity.setPhone(sp.getPhone());
      entity.setIdCard(sp.getIdCardNumber());
      entity.setPlatformUserId(sp.getPlatformUserId());
      entity.setDeleted(0);
      mapper.insert(entity);
      count++;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    return result;
  }

  /** 按身份证号导入学员 */
  @Transactional
  public Map<String, Object> importStudentsByIdCard(Long traininfoId, Long classId, String idCardsText) {
    TrainSignUpSelfEntity signUp = signUpSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTrainInfoId, traininfoId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUp == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录未找到");
    }
    String[] lines = idCardsText.split("[\\n,，\\s]+");
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (String line : lines) {
      String idCard = line.trim();
      if (idCard.isEmpty()) continue;
      if (idCard.length() != 18) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("reason", "身份证号格式错误");
        failed.add(f);
        continue;
      }
      TrainSignUpPeopleSelfEntity sp = signUpPeopleSelfMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard));
      if (sp == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("reason", "未在报名记录中找到该身份证号");
        failed.add(f);
        continue;
      }
      Long existCount = mapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
          .eq(TrainPeopleListEntity::getType, "student")
          .eq(TrainPeopleListEntity::getDeleted, 0)
          .eq(TrainPeopleListEntity::getTrainId, traininfoId)
          .eq(TrainPeopleListEntity::getTrainClassId, classId)
          .eq(TrainPeopleListEntity::getPlatformUserId, sp.getPlatformUserId()));
      if (existCount != null && existCount > 0) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("idCard", idCard);
        f.put("reason", "已存在该学员");
        failed.add(f);
        continue;
      }

      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType("student");
      entity.setTrainId(traininfoId);
      entity.setTrainClassId(classId);
      entity.setRealname(sp.getName());
      entity.setPhone(sp.getPhone());
      entity.setIdCard(sp.getIdCardNumber());
      entity.setPlatformUserId(sp.getPlatformUserId());
      entity.setDeleted(0);
      mapper.insert(entity);
      count++;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    return result;
  }

  // ==================== 教务/负责人 ====================

  /** 查询教务/负责人列表 */
  @Transactional(readOnly = true)
  public Map<String, Object> classOtherList(Long trainInfoId, Long classId, String tag) {
    String type = tag.equals("teacher") ? "staff" : "inCharge";
    List<TrainPeopleListEntity> records = mapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .eq(TrainPeopleListEntity::getType, type)
            .eq(TrainPeopleListEntity::getTrainId, trainInfoId)
            .eq(TrainPeopleListEntity::getTrainClassId, classId));
    List<Map<String, Object>> list = records.stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("train_id", e.getTrainId());
      item.put("train_class_id", e.getTrainClassId());
      item.put("realname", e.getRealname());
      item.put("phone", e.getPhone());
      item.put("id_card", e.getIdCard());
      return item;
    }).toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    return result;
  }

  /** 新增/编辑教务/负责人 */
  @Transactional
  public Map<String, Object> saveOther(Map<String, Object> params) {
    Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : 0L;
    String type = params.get("type") != null ? params.get("type").toString() : null;
    if (id > 0) {
      TrainPeopleListEntity entity = getExisting(id);
      if (params.get("realname") != null) entity.setRealname(params.get("realname").toString());
      if (params.get("phone") != null) entity.setPhone(params.get("phone").toString());
      if (params.get("id_card") != null) entity.setIdCard(params.get("id_card").toString());
      mapper.updateById(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      return result;
    } else {
      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType(type);
      entity.setTrainId(params.get("train_id") != null ? Long.valueOf(params.get("train_id").toString()) : null);
      entity.setTrainClassId(params.get("classid") != null ? Long.valueOf(params.get("classid").toString()) : null);
      entity.setRealname(params.get("realname") != null ? params.get("realname").toString() : null);
      entity.setPhone(params.get("phone") != null ? params.get("phone").toString() : null);
      entity.setIdCard(params.get("id_card") != null ? params.get("id_card").toString() : null);
      entity.setDeleted(0);
      mapper.insert(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      return result;
    }
  }

  /** 批量删除教务/负责人 */
  @Transactional
  public Map<String, Object> batchDeleteOthers(List<Long> ids) {
    int count = 0;
    for (Long id : ids) {
      TrainPeopleListEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        entity.setDeleted(1);
        mapper.updateById(entity);
        count++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    return result;
  }

  /** 可导入的教务数据源（教师库） */
  @Transactional(readOnly = true)
  public Map<String, Object> otherImportData(String tag) {
    List<Map<String, Object>> list = List.of();
    if ("teacher".equals(tag)) {
      list = teacherBankMapper.selectList(
              new LambdaQueryWrapper<TrainTeacherBankEntity>()
                  .eq(TrainTeacherBankEntity::getDeleted, 0))
          .stream().map(t -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("name", t.getName());
            item.put("phone", t.getPhone());
            return item;
          }).toList();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    return result;
  }

  /** 从教师库导入教务人员 */
  @Transactional
  public Map<String, Object> importOthers(Long traininfoId, Long classId, List<Long> ids) {
    List<TrainTeacherBankEntity> teachers = teacherBankMapper.selectBatchIds(ids);
    int count = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (Long tid : ids) {
      TrainTeacherBankEntity t = teachers.stream()
          .filter(x -> x.getId().equals(tid)).findFirst().orElse(null);
      if (t == null) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", tid);
        f.put("reason", "教师不存在");
        failed.add(f);
        continue;
      }
      Long existCount = mapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
          .eq(TrainPeopleListEntity::getType, "staff")
          .eq(TrainPeopleListEntity::getDeleted, 0)
          .eq(TrainPeopleListEntity::getTrainId, traininfoId)
          .eq(TrainPeopleListEntity::getTrainClassId, classId)
          .eq(TrainPeopleListEntity::getUserId, t.getId()));
      if (existCount != null && existCount > 0) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", tid);
        f.put("reason", "已存在该教务人员");
        failed.add(f);
        continue;
      }

      TrainPeopleListEntity entity = new TrainPeopleListEntity();
      entity.setType("staff");
      entity.setTrainId(traininfoId);
      entity.setTrainClassId(classId);
      entity.setUserId(t.getId());
      entity.setRealname(t.getName());
      entity.setPhone(t.getPhone());
      entity.setIdCard(t.getIdCardNumber());
      entity.setDeleted(0);
      mapper.insert(entity);
      count++;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("failed", failed);
    return result;
  }

  private TrainPeopleListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getId, id)
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainPeopleListVO toVO(TrainPeopleListEntity entity) {
    return new TrainPeopleListVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getTrainClassId(),
        entity.getType(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getRealname(),
        entity.getPhone(),
        entity.getIsLeader(),
        entity.getIsCommittee(),
        entity.getExamScore(),
        entity.getRealScore(),
        entity.getPaper(),
        entity.getNormalScore(),
        entity.getNationalPaper(),
        entity.getCurrentLevel(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getIdCard(),
        entity.getStatus()
    );
  }
}

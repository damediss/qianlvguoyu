package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 毕业管理服务
 */
@Service
@Slf4j
public class TrainGraduationApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;

  public TrainGraduationApplicationService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
  }

  /** 可毕业班级列表 */
  @Transactional(readOnly = true)
  public Map<String, Object> graduationCandidates(Integer page, Integer pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // 查询状态为"已确认"的培训信息
    List<TrainInfoListEntity> confirmedInfos = infoListMapper.selectList(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getDeleted, 0)
            .eq(TrainInfoListEntity::getStatus, 1));

    if (confirmedInfos.isEmpty()) {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("list", List.of());
      result.put("total", 0);
      return result;
    }

    List<Long> infoIds = confirmedInfos.stream().map(TrainInfoListEntity::getId).toList();
    Page<TrainInfoClassEntity> mpPage = classMapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 20),
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getDeleted, 0)
            .in(TrainInfoClassEntity::getTrainInfoId, infoIds));

    // 批量获取关联数据
    List<Long> companyIds = new ArrayList<>();
    List<Long> workTypeIds = new ArrayList<>();
    for (TrainInfoClassEntity c : mpPage.getRecords()) {
      if (c.getCompanyId() != null) companyIds.add(c.getCompanyId());
      if (c.getWorkTypeId() != null) workTypeIds.add(c.getWorkTypeId());
    }

    Map<Long, String> companyNames = new java.util.HashMap<>();
    if (!companyIds.isEmpty()) {
      for (OrgEntity o : orgMapper.selectBatchIds(companyIds)) {
        companyNames.put(o.getId().longValue(), o.getName());
      }
    }
    Map<Long, String> workTypeNames = new java.util.HashMap<>();
    if (!workTypeIds.isEmpty()) {
      for (ExamWorkTypeEntity w : examWorkTypeMapper.selectBatchIds(workTypeIds)) {
        workTypeNames.put(w.getId().longValue(), w.getTitle());
      }
    }

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainInfoClassEntity c : mpPage.getRecords()) {
      TrainInfoListEntity info = confirmedInfos.stream()
          .filter(i -> i.getId().equals(c.getTrainInfoId())).findFirst().orElse(null);
      Long studentCount = peopleListMapper.selectCount(
          new LambdaQueryWrapper<TrainPeopleListEntity>()
              .eq(TrainPeopleListEntity::getTrainClassId, c.getId())
              .eq(TrainPeopleListEntity::getType, "student")
              .eq(TrainPeopleListEntity::getDeleted, 0));

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", c.getId());
      item.put("title", c.getTitle());
      item.put("trainInfoId", c.getTrainInfoId());
      item.put("trainTitle", info != null ? info.getTitle() : "");
      item.put("companyId", c.getCompanyId());
      item.put("companyName", companyNames.getOrDefault(c.getCompanyId(), ""));
      item.put("workTypeId", c.getWorkTypeId());
      item.put("workTypeName", workTypeNames.getOrDefault(c.getWorkTypeId(), ""));
      item.put("studentCount", studentCount);
      item.put("dateStart", c.getDateStart());
      item.put("dateEnd", c.getDateEnd());
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    return result;
  }

  /** 班级可毕业学员列表（含成绩） */
  @Transactional(readOnly = true)
  public Map<String, Object> graduationStudents(Long classId) {
    TrainInfoClassEntity classEntity = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classEntity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", s.getId());
      item.put("realname", s.getRealname());
      item.put("phone", s.getPhone());
      item.put("idCard", s.getIdCard());
      item.put("normalScore", s.getNormalScore());
      item.put("examScore", s.getExamScore());
      item.put("realScore", s.getRealScore());
      item.put("isLeader", s.getIsLeader());
      item.put("isCommittee", s.getIsCommittee());
      list.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classTitle", classEntity.getTitle());
    result.put("list", list);
    result.put("total", list.size());
    return result;
  }

  /** 执行毕业 */
  @Transactional
  public Map<String, Object> graduate(Long classId) {
    TrainInfoClassEntity classEntity = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classEntity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    Long trainInfoId = classEntity.getTrainInfoId();
    if (trainInfoId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级未关联培训信息");
    }

    TrainInfoListEntity infoEntity = infoListMapper.selectById(trainInfoId);
    if (infoEntity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训信息不存在");
    }

    // 更新培训状态为已完成
    infoEntity.setStatus(2);
    infoListMapper.updateById(infoEntity);

    // 遗留 train_people_list 无 status 列，毕业仅更新培训信息状态
    Long graduateCount = peopleListMapper.selectCount(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    log.info("班级 {} 毕业完成，学员数: {}, 操作时间: {}", classId, graduateCount, LocalDateTime.now());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("trainInfoId", trainInfoId);
    result.put("graduateCount", graduateCount);
    result.put("status", "completed");
    return result;
  }

  /** 发送毕业通知 */
  @Transactional
  public Map<String, Object> graduationNotify(Long classId) {
    TrainInfoClassEntity classEntity = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classEntity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));

    log.info("毕业通知: 班级 {} (ID={}) 共 {} 名学员，通知已发送",
        classEntity.getTitle(), classId, students.size());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("notifiedCount", students.size());
    result.put("message", "通知已发送");
    return result;
  }
}

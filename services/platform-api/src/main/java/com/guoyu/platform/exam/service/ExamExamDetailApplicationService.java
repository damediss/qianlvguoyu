package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamExamDetailApplicationService {

  private final ExamExamDetailMapper mapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final UserMapper userMapper;
  private final ExamKnowledgeStructureApplicationService knowledgeService;

  public ExamExamDetailApplicationService(
      ExamExamDetailMapper mapper,
      ExamKnowledgeStructureMapper knowledgeMapper,
      UserMapper userMapper,
      ExamKnowledgeStructureApplicationService knowledgeService) {
    this.mapper = mapper;
    this.knowledgeMapper = knowledgeMapper;
    this.userMapper = userMapper;
    this.knowledgeService = knowledgeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamExamDetailVO> page(ExamExamDetailQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId);

    // 知识点展开逻辑: 根据 knowledgeId 或 courseId 获取 L3 ID 列表
    List<Integer> knowledgeIds = resolveKnowledgeIds(req);
    if (knowledgeIds != null) {
      if (knowledgeIds.isEmpty()) {
        return new PageResponse<>(List.of(), req.getPage(), req.getPageSize(), 0L);
      }
      wrapper.in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds);
    }

    wrapper.eq(StringUtils.hasText(req.getType()), ExamExamDetailEntity::getType, req.getType())
        .eq(req.getSource() != null, ExamExamDetailEntity::getSource, req.getSource())
        .eq(req.getDifficulty() != null, ExamExamDetailEntity::getDifficulty, req.getDifficulty())
        .eq(req.getStatus() != null, ExamExamDetailEntity::getStatus, req.getStatus())
        .like(StringUtils.hasText(req.getKeyword()), ExamExamDetailEntity::getContent, req.getKeyword())
        .orderByDesc(ExamExamDetailEntity::getSortNumber)
        .orderByDesc(ExamExamDetailEntity::getId);

    Page<ExamExamDetailEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamExamDetailEntity> records = page.getRecords();
    if (records.isEmpty()) {
      return new PageResponse<>(List.of(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    // 批量加载关联数据: 创建人用户名 + 知识点名称
    Set<Integer> userIds = records.stream()
        .map(ExamExamDetailEntity::getUserId)
        .filter(uid -> uid != null && uid > 0)
        .collect(Collectors.toSet());
    Set<Integer> kIds = records.stream()
        .map(ExamExamDetailEntity::getKnowledgeId)
        .filter(kid -> kid != null && kid > 0)
        .collect(Collectors.toSet());

    Map<Integer, String> userNameMap = loadUserNames(userIds);
    Map<Integer, String> knowledgeTitleMap = loadKnowledgeTitles(kIds);

    List<ExamExamDetailVO> vos = records.stream().map(entity -> {
      ExamExamDetailVO vo = ExamExamDetailVO.from(entity);
      if (entity.getUserId() != null) {
        vo.setCreatorName(userNameMap.getOrDefault(entity.getUserId(), ""));
      }
      if (entity.getKnowledgeId() != null) {
        vo.setKnowledgeTitle(knowledgeTitleMap.getOrDefault(entity.getKnowledgeId(), ""));
      }
      return vo;
    }).toList();

    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public ExamExamDetailVO getById(Integer id) {
    ExamExamDetailEntity entity = findExisting(id);
    ExamExamDetailVO vo = ExamExamDetailVO.from(entity);
    if (entity.getUserId() != null) {
      UserEntity user = userMapper.selectById((long) entity.getUserId());
      if (user != null) {
        vo.setCreatorName(user.getDisplayName());
      }
    }
    if (entity.getKnowledgeId() != null) {
      ExamKnowledgeStructureEntity ks = knowledgeMapper.selectById(entity.getKnowledgeId());
      if (ks != null) {
        vo.setKnowledgeTitle(ks.getTitle());
      }
    }
    return vo;
  }

  @Transactional
  public ExamExamDetailVO create(ExamExamDetailCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamExamDetailEntity entity = new ExamExamDetailEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.type());
    entity.setContent(req.content());
    entity.setAnswer(req.answer());
    entity.setKnowledgeId(req.knowledgeId());
    entity.setDifficulty(req.difficulty() != null ? req.difficulty() : 2);
    entity.setScore(req.score());
    entity.setResolve(req.resolve());
    entity.setKeyword(req.keyword());
    entity.setSource(req.source() != null ? req.source() : 0);
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 1);
    entity.setUserId(userId.intValue());
    entity.setCheckStatus(0);
    entity.setFinalCheckStatus(0);
    entity.setIsPrivate(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());

    mapper.insert(entity);
    return ExamExamDetailVO.from(entity);
  }

  @Transactional
  public ExamExamDetailVO update(Integer id, ExamExamDetailUpdateReq req) {
    ExamExamDetailEntity entity = findExisting(id);

    if (req.type() != null) entity.setType(req.type());
    if (req.content() != null) entity.setContent(req.content());
    if (req.answer() != null) entity.setAnswer(req.answer());
    if (req.knowledgeId() != null) entity.setKnowledgeId(req.knowledgeId());
    if (req.difficulty() != null) entity.setDifficulty(req.difficulty());
    if (req.score() != null) entity.setScore(req.score());
    if (req.resolve() != null) entity.setResolve(req.resolve());
    if (req.keyword() != null) entity.setKeyword(req.keyword());
    if (req.source() != null) entity.setSource(req.source());
    if (req.status() != null) entity.setStatus(req.status());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());
    if (req.checkStatus() != null) {
      entity.setCheckStatus(req.checkStatus());
      entity.setCheckTime(LocalDateTime.now());
      entity.setCheckUserId(SecurityContext.currentUser().getUserId().intValue());
    }
    if (req.finalCheckStatus() != null) {
      entity.setFinalCheckStatus(req.finalCheckStatus());
      entity.setFinalCheckTime(LocalDateTime.now());
      entity.setFinalCheckUserId(SecurityContext.currentUser().getUserId().intValue());
    }

    mapper.updateById(entity);
    return ExamExamDetailVO.from(entity);
  }

  @Transactional
  public void delete(Integer id) {
    findExisting(id);
    mapper.deleteById(id);
  }

  @Transactional
  public int batchDelete(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    // 限定 tenant 范围,避免跨租户删除
    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId)
        .in(ExamExamDetailEntity::getId, ids);
    return mapper.delete(wrapper);
  }

  /**
   * 根据查询条件解析 L3 知识点ID列表。
   * 返回 null 表示不需要知识点过滤; 返回空列表表示无匹配。
   */
  private List<Integer> resolveKnowledgeIds(ExamExamDetailQueryReq req) {
    if (req.getKnowledgeId() != null && req.getKnowledgeId() > 0) {
      return knowledgeService.getLevel3DescendantIds(req.getKnowledgeId());
    }
    if (req.getCourseId() != null && req.getCourseId() > 0) {
      return knowledgeService.getLevel3IdsByCourse(req.getCourseId());
    }
    return null;
  }

  private Map<Integer, String> loadUserNames(Set<Integer> userIds) {
    if (userIds.isEmpty()) return Map.of();
    List<UserEntity> users = userMapper.selectBatchIds(
        userIds.stream().map(Integer::longValue).toList());
    return users.stream().collect(Collectors.toMap(
        u -> u.getId().intValue(),
        u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
        (a, b) -> a));
  }

  private Map<Integer, String> loadKnowledgeTitles(Set<Integer> kIds) {
    if (kIds.isEmpty()) return Map.of();
    List<ExamKnowledgeStructureEntity> list = knowledgeMapper.selectBatchIds(kIds.stream().toList());
    return list.stream().collect(Collectors.toMap(
        ExamKnowledgeStructureEntity::getId,
        ExamKnowledgeStructureEntity::getTitle,
        (a, b) -> a));
  }

  private ExamExamDetailEntity findExisting(Integer id) {
    ExamExamDetailEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "试题不存在");
    }
    return entity;
  }
}

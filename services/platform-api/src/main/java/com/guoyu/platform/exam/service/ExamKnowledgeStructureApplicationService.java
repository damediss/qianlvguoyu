package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureCreateReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureQueryReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureUpdateReq;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.vo.ExamKnowledgeStructureVO;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamKnowledgeStructureApplicationService {

  private final ExamKnowledgeStructureMapper mapper;

  public ExamKnowledgeStructureApplicationService(ExamKnowledgeStructureMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamKnowledgeStructureVO> page(ExamKnowledgeStructureQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    LambdaQueryWrapper<ExamKnowledgeStructureEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
        .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
        .eq(req.getCourseId() != null, ExamKnowledgeStructureEntity::getCourseId, req.getCourseId())
        .eq(req.getType() != null, ExamKnowledgeStructureEntity::getType, req.getType())
        .eq(req.getLevel() != null, ExamKnowledgeStructureEntity::getLevel, req.getLevel())
        .like(StringUtils.hasText(req.getTitle()), ExamKnowledgeStructureEntity::getTitle, req.getTitle())
        .orderByAsc(ExamKnowledgeStructureEntity::getSortNumber)
        .orderByAsc(ExamKnowledgeStructureEntity::getId);

    Page<ExamKnowledgeStructureEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(ExamKnowledgeStructureVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public List<ExamKnowledgeStructureVO> tree(Integer courseId, Integer type) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    LambdaQueryWrapper<ExamKnowledgeStructureEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
        .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
        .eq(courseId != null, ExamKnowledgeStructureEntity::getCourseId, courseId)
        .eq(type != null, ExamKnowledgeStructureEntity::getType, type)
        .eq(ExamKnowledgeStructureEntity::getStatus, 1)
        .orderByAsc(ExamKnowledgeStructureEntity::getSortNumber)
        .orderByAsc(ExamKnowledgeStructureEntity::getId);

    List<ExamKnowledgeStructureEntity> all = mapper.selectList(wrapper);
    return buildTree(all);
  }

  @Transactional(readOnly = true)
  public ExamKnowledgeStructureVO getById(Integer id) {
    ExamKnowledgeStructureEntity entity = findExisting(id);
    return ExamKnowledgeStructureVO.from(entity);
  }

  @Transactional
  public ExamKnowledgeStructureVO create(ExamKnowledgeStructureCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    int level = calculateLevel(req.parentId());

    ExamKnowledgeStructureEntity entity = new ExamKnowledgeStructureEntity();
    entity.setTenantId(tenantId);
    entity.setTitle(req.title());
    entity.setType(req.type() != null ? req.type() : 0);
    entity.setCourseId(req.courseId());
    entity.setParentId(req.parentId() != null ? req.parentId() : 0);
    entity.setSeriesNo(req.seriesNo());
    entity.setLevel(level);
    entity.setImportant(req.important() != null ? req.important() : 0);
    entity.setUserId(userId.intValue());
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 1);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());

    mapper.insert(entity);
    return ExamKnowledgeStructureVO.from(entity);
  }

  @Transactional
  public ExamKnowledgeStructureVO update(Integer id, ExamKnowledgeStructureUpdateReq req) {
    ExamKnowledgeStructureEntity entity = findExisting(id);

    if (req.title() != null) entity.setTitle(req.title());
    if (req.type() != null) entity.setType(req.type());
    if (req.courseId() != null) entity.setCourseId(req.courseId());
    if (req.seriesNo() != null) entity.setSeriesNo(req.seriesNo());
    if (req.important() != null) entity.setImportant(req.important());
    if (req.status() != null) entity.setStatus(req.status());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());
    if (req.parentId() != null) {
      entity.setParentId(req.parentId());
      entity.setLevel(calculateLevel(req.parentId()));
    }

    mapper.updateById(entity);
    return ExamKnowledgeStructureVO.from(entity);
  }

  @Transactional
  public void delete(Integer id) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    findExisting(id);

    List<Integer> allIds = new ArrayList<>();
    collectChildIds(tenantId, id, allIds);
    allIds.add(id);

    for (Integer delId : allIds) {
      mapper.deleteById(delId);
    }
    log.info("Deleted knowledge structure {} and {} child nodes", id, allIds.size() - 1);
  }

  @Transactional
  public int batchImport(Integer courseId, Integer type, List<String> lines) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    if (lines == null || lines.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "导入内容不能为空");
    }

    int count = 0;
    // 跟踪各层级的父节点: levelKey -> 最近创建的节点ID
    Map<String, Integer> parentStack = new LinkedHashMap<>();

    for (String rawLine : lines) {
      if (rawLine == null || rawLine.isBlank()) continue;

      // 计算缩进层级（2空格=1级，或用前导空格数/2）
      int indent = 0;
      for (char c : rawLine.toCharArray()) {
        if (c == ' ' || c == '\t') {
          indent++;
        } else {
          break;
        }
      }
      String trimmed = rawLine.strip();

      // 解析 "01 名称" 格式
      String title;
      String seriesNo = null;
      int spaceIdx = trimmed.indexOf(' ');
      if (spaceIdx > 0 && spaceIdx <= 5) {
        String code = trimmed.substring(0, spaceIdx);
        if (code.matches("\\d+")) {
          seriesNo = code;
          title = trimmed.substring(spaceIdx + 1).strip();
        } else {
          title = trimmed;
        }
      } else {
        title = trimmed;
      }

      if (title.isBlank()) continue;

      // level = indent 级别对应的 knowledge level
      // 缩进0 -> L0, 缩进1 -> L1, 缩进2 -> L2, 缩进3 -> L3
      int level = Math.min(indent / 2, 3);

      // 查找父节点
      Integer parentId = 0;
      if (level > 0) {
        Integer pId = parentStack.get("L" + (level - 1));
        if (pId != null) parentId = pId;
      }

      // 去重: title + type + courseId + level
      LambdaQueryWrapper<ExamKnowledgeStructureEntity> dupWrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
          .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
          .eq(ExamKnowledgeStructureEntity::getTitle, title)
          .eq(ExamKnowledgeStructureEntity::getType, type != null ? type : 0)
          .eq(courseId != null, ExamKnowledgeStructureEntity::getCourseId, courseId)
          .eq(ExamKnowledgeStructureEntity::getLevel, level);
      if (mapper.selectCount(dupWrapper) > 0) {
        // 已存在，查出其ID作为后续节点的父节点
        ExamKnowledgeStructureEntity existing = mapper.selectOne(dupWrapper.last("LIMIT 1"));
        if (existing != null) {
          parentStack.put("L" + level, existing.getId());
        }
        continue;
      }

      ExamKnowledgeStructureEntity entity = new ExamKnowledgeStructureEntity();
      entity.setTenantId(tenantId);
      entity.setTitle(title);
      entity.setType(type != null ? type : 0);
      entity.setCourseId(courseId);
      entity.setParentId(parentId);
      entity.setSeriesNo(seriesNo);
      entity.setLevel(level);
      entity.setImportant(0);
      entity.setUserId(userId.intValue());
      entity.setStatus(1);
      entity.setSortNumber(count + 1);
      entity.setDeleted(0);
      entity.setCreateTime(LocalDateTime.now());

      mapper.insert(entity);
      parentStack.put("L" + level, entity.getId());
      count++;
    }

    log.info("Batch imported {} knowledge structures for course={}", count, courseId);
    return count;
  }

  /**
   * 获取某个节点下所有 L3 后代ID列表（用于试题分页的知识点展开）
   */
  @Transactional(readOnly = true)
  public List<Integer> getLevel3DescendantIds(Integer knowledgeId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    ExamKnowledgeStructureEntity node = findExisting(knowledgeId);

    if (node.getLevel() != null && node.getLevel() == 3) {
      return List.of(knowledgeId);
    }

    List<Integer> result = new ArrayList<>();
    collectLevel3Ids(tenantId, knowledgeId, result);
    return result;
  }

  /**
   * 获取某个 course 下所有 L3 知识点ID列表
   */
  @Transactional(readOnly = true)
  public List<Integer> getLevel3IdsByCourse(Integer courseId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    LambdaQueryWrapper<ExamKnowledgeStructureEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
        .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
        .eq(ExamKnowledgeStructureEntity::getCourseId, courseId)
        .eq(ExamKnowledgeStructureEntity::getLevel, 3)
        .eq(ExamKnowledgeStructureEntity::getStatus, 1)
        .select(ExamKnowledgeStructureEntity::getId);
    return mapper.selectList(wrapper).stream()
        .map(ExamKnowledgeStructureEntity::getId)
        .toList();
  }

  private void collectLevel3Ids(Integer tenantId, Integer parentId, List<Integer> result) {
    LambdaQueryWrapper<ExamKnowledgeStructureEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
        .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
        .eq(ExamKnowledgeStructureEntity::getParentId, parentId);
    List<ExamKnowledgeStructureEntity> children = mapper.selectList(wrapper);
    if (children == null) return;
    for (ExamKnowledgeStructureEntity child : children) {
      if (child.getLevel() != null && child.getLevel() == 3) {
        result.add(child.getId());
      } else {
        collectLevel3Ids(tenantId, child.getId(), result);
      }
    }
  }

  private void collectChildIds(Integer tenantId, Integer parentId, List<Integer> result) {
    LambdaQueryWrapper<ExamKnowledgeStructureEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
        .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
        .eq(ExamKnowledgeStructureEntity::getParentId, parentId)
        .select(ExamKnowledgeStructureEntity::getId);
    List<ExamKnowledgeStructureEntity> children = mapper.selectList(wrapper);
    if (children == null) return;
    for (ExamKnowledgeStructureEntity child : children) {
      result.add(child.getId());
      collectChildIds(tenantId, child.getId(), result);
    }
  }

  private int calculateLevel(Integer parentId) {
    if (parentId == null || parentId == 0) return 0;
    ExamKnowledgeStructureEntity parent = mapper.selectById(parentId);
    if (parent == null) return 0;
    return (parent.getLevel() != null ? parent.getLevel() : 0) + 1;
  }

  private List<ExamKnowledgeStructureVO> buildTree(List<ExamKnowledgeStructureEntity> all) {
    Map<Integer, ExamKnowledgeStructureVO> map = new LinkedHashMap<>();
    for (ExamKnowledgeStructureEntity entity : all) {
      ExamKnowledgeStructureVO vo = ExamKnowledgeStructureVO.from(entity);
      vo.setChildren(new ArrayList<>());
      map.put(vo.getId(), vo);
    }

    List<ExamKnowledgeStructureVO> roots = new ArrayList<>();
    for (ExamKnowledgeStructureVO vo : map.values()) {
      Integer pid = vo.getParentId();
      if (pid == null || pid == 0 || !map.containsKey(pid)) {
        roots.add(vo);
      } else {
        ExamKnowledgeStructureVO parent = map.get(pid);
        parent.getChildren().add(vo);
      }
    }
    return roots;
  }

  private ExamKnowledgeStructureEntity findExisting(Integer id) {
    ExamKnowledgeStructureEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "知识点不存在");
    }
    return entity;
  }
}

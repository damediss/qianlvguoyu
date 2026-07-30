package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.CourseItem;
import com.guoyu.platform.exam.dto.ExamUserAuthCourseCreateReq;
import com.guoyu.platform.exam.dto.ExamUserAuthCourseQueryReq;
import com.guoyu.platform.exam.entity.ExamUserAuthCourseEntity;
import com.guoyu.platform.exam.mapper.ExamUserAuthCourseMapper;
import com.guoyu.platform.exam.vo.ExamUserAuthCourseVO;
import com.guoyu.platform.security.SecurityContext;
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
public class ExamUserAuthCourseApplicationService {

  private final ExamUserAuthCourseMapper mapper;

  public ExamUserAuthCourseApplicationService(ExamUserAuthCourseMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamUserAuthCourseVO> page(ExamUserAuthCourseQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamUserAuthCourseEntity> wrapper = new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .orderByDesc(ExamUserAuthCourseEntity::getSortNumber)
        .orderByAsc(ExamUserAuthCourseEntity::getId);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(ExamUserAuthCourseEntity::getRealname, req.getKeyword())
          .or()
          .like(ExamUserAuthCourseEntity::getIdCard, req.getKeyword())
          .or()
          .like(ExamUserAuthCourseEntity::getCourseTitle, req.getKeyword()));
    }

    List<ExamUserAuthCourseEntity> allRows = mapper.selectList(wrapper);
    List<ExamUserAuthCourseVO> aggregated = aggregate(allRows);

    long total = aggregated.size();
    long page = req.getPage();
    long pageSize = req.getPageSize();
    long from = (page - 1) * pageSize;
    long to = Math.min(from + pageSize, total);
    List<ExamUserAuthCourseVO> pageItems = from < total
        ? aggregated.subList((int) from, (int) to)
        : List.of();

    return new PageResponse<>(pageItems, page, pageSize, total);
  }

  @Transactional(readOnly = true)
  public ExamUserAuthCourseVO getByKey(String realname, String idCard) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<ExamUserAuthCourseEntity> rows = mapper.selectList(new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .eq(ExamUserAuthCourseEntity::getRealname, realname)
        .eq(ExamUserAuthCourseEntity::getIdCard, idCard)
        .orderByDesc(ExamUserAuthCourseEntity::getSortNumber));
    if (rows.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "专家授权记录不存在");
    }
    return toVO(rows);
  }

  @Transactional
  public ExamUserAuthCourseVO create(ExamUserAuthCourseCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    int sortNumber = req.sortNumber() != null ? req.sortNumber() : 1;
    int status = req.status() != null ? req.status() : 1;

    for (CourseItem item : req.items()) {
      ExamUserAuthCourseEntity entity = new ExamUserAuthCourseEntity();
      entity.setTenantId(tenantId);
      entity.setRealname(req.realname());
      entity.setIdCard(req.idCard());
      entity.setCourseId(item.courseId());
      entity.setCourseTitle(item.courseTitle());
      entity.setType(item.type());
      entity.setStatus(status);
      entity.setSortNumber(sortNumber);
      entity.setDeleted(0);
      mapper.insert(entity);
    }

    List<ExamUserAuthCourseEntity> rows = mapper.selectList(new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .eq(ExamUserAuthCourseEntity::getRealname, req.realname())
        .eq(ExamUserAuthCourseEntity::getIdCard, req.idCard()));
    return toVO(rows);
  }

  @Transactional
  public ExamUserAuthCourseVO update(String realname, String idCard, ExamUserAuthCourseCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // Physical-delete old rows for this expert (current tenant only), then re-insert
    LambdaQueryWrapper<ExamUserAuthCourseEntity> oldWrapper = new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .eq(ExamUserAuthCourseEntity::getRealname, realname)
        .eq(ExamUserAuthCourseEntity::getIdCard, idCard);
    List<ExamUserAuthCourseEntity> oldRows = mapper.selectList(oldWrapper);
    if (oldRows.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "专家授权记录不存在");
    }
    for (ExamUserAuthCourseEntity old : oldRows) {
      mapper.deleteById(old.getId());
    }

    int sortNumber = req.sortNumber() != null ? req.sortNumber() : 1;
    int status = req.status() != null ? req.status() : 1;

    for (CourseItem item : req.items()) {
      ExamUserAuthCourseEntity entity = new ExamUserAuthCourseEntity();
      entity.setTenantId(tenantId);
      entity.setRealname(req.realname());
      entity.setIdCard(req.idCard());
      entity.setCourseId(item.courseId());
      entity.setCourseTitle(item.courseTitle());
      entity.setType(item.type());
      entity.setStatus(status);
      entity.setSortNumber(sortNumber);
      entity.setDeleted(0);
      mapper.insert(entity);
    }

    List<ExamUserAuthCourseEntity> rows = mapper.selectList(new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .eq(ExamUserAuthCourseEntity::getRealname, req.realname())
        .eq(ExamUserAuthCourseEntity::getIdCard, req.idCard()));
    return toVO(rows);
  }

  @Transactional
  public void delete(String realname, String idCard) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamUserAuthCourseEntity> wrapper = new LambdaQueryWrapper<ExamUserAuthCourseEntity>()
        .eq(ExamUserAuthCourseEntity::getTenantId, tenantId)
        .eq(ExamUserAuthCourseEntity::getRealname, realname)
        .eq(ExamUserAuthCourseEntity::getIdCard, idCard);
    List<ExamUserAuthCourseEntity> rows = mapper.selectList(wrapper);
    if (rows.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "专家授权记录不存在");
    }
    mapper.delete(wrapper);
  }

  private List<ExamUserAuthCourseVO> aggregate(List<ExamUserAuthCourseEntity> rows) {
    Map<String, List<ExamUserAuthCourseEntity>> groups = new LinkedHashMap<>();
    for (ExamUserAuthCourseEntity row : rows) {
      String key = key(row.getRealname(), row.getIdCard());
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    return groups.values().stream()
        .map(this::toVO)
        .toList();
  }

  private ExamUserAuthCourseVO toVO(List<ExamUserAuthCourseEntity> rows) {
    if (rows.isEmpty()) {
      return null;
    }
    ExamUserAuthCourseEntity first = rows.get(0);
    ExamUserAuthCourseVO vo = new ExamUserAuthCourseVO();
    vo.setRealname(first.getRealname());
    vo.setIdCard(first.getIdCard());
    vo.setStatus(first.getStatus());
    vo.setSortNumber(first.getSortNumber());
    vo.setCreateTime(first.getCreateTime());
    vo.setCourses(rows.stream()
        .map(r -> new CourseItem(r.getCourseId(), r.getCourseTitle(), r.getType()))
        .toList());
    return vo;
  }

  private String key(String realname, String idCard) {
    return (realname != null ? realname : "") + "::" + (idCard != null ? idCard : "");
  }
}

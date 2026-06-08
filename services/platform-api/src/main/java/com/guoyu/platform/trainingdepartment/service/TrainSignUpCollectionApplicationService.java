package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfListVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 信息收集（对齐 yun_shang information.vue） */
@Service
public class TrainSignUpCollectionApplicationService {

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final OrgMapper orgMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final String cdnPath;
  private final String cdnUrl;

  public TrainSignUpCollectionApplicationService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      OrgMapper orgMapper,
      TrainingOperatorScopeService operatorScopeService,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.orgMapper = orgMapper;
    this.operatorScopeService = operatorScopeService;
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpSelfListVO> pageSignUps(TrainSignUpSelfQueryReq req) {
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .orderByAsc(TrainSignUpSelfEntity::getSortNumber)
        .orderByDesc(TrainSignUpSelfEntity::getCreateTime);
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(TrainSignUpSelfEntity::getOrgId, companyFilter);
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpSelfEntity::getTitle, req.getKeyword());
    }
    Page<TrainSignUpSelfEntity> page = signUpMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<Long> signUpIds = page.getRecords().stream().map(TrainSignUpSelfEntity::getId).toList();
    Map<Long, Long> peopleCounts = countPeopleBySignUp(signUpIds);
    Map<Long, String> orgNames = resolveOrgNames(page.getRecords());
    List<TrainSignUpSelfListVO> items = page.getRecords().stream()
        .map(e -> toListVO(e, peopleCounts.getOrDefault(e.getId(), 0L), orgNames.get(e.getOrgId())))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> resolveQrcode(Long signUpId) {
    TrainSignUpSelfEntity signUp = getSignUp(signUpId);
    Path file = Paths.get(cdnPath).resolve("wxqrcode/signup_self_" + signUpId + ".png");
    String qrcode = Files.exists(file) ? cdnUrl + "/wxqrcode/signup_self_" + signUpId + ".png" : "";
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("signUpId", signUpId);
    result.put("title", signUp.getTitle());
    result.put("qrcode", qrcode);
    result.put("registrationPath", "pages/registration/detail?id=" + signUpId);
    result.put("generated", StringUtils.hasText(qrcode));
    return result;
  }

  @Transactional
  public int batchUpdatePeople(List<Long> ids, String field, Integer value) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择人员");
    }
    if (!"status".equals(field) && !"deleted".equals(field)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的批量字段");
    }
    LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity>()
        .in(TrainSignUpPeopleSelfEntity::getId, ids);
    if ("status".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getStatus, value != null ? value : 0);
    } else {
      wrapper.set(TrainSignUpPeopleSelfEntity::getDeleted, 1);
    }
    wrapper.set(TrainSignUpPeopleSelfEntity::getUpdateTime, LocalDateTime.now());
    return peopleMapper.update(null, wrapper);
  }

  @Transactional(readOnly = true)
  public byte[] exportPeopleExcel(Long signUpId) {
    getSignUp(signUpId);
    List<TrainSignUpPeopleSelfEntity> people = peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .orderByDesc(TrainSignUpPeopleSelfEntity::getId));
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("报名人员");
      Row header = sheet.createRow(0);
      String[] titles = {"ID", "姓名", "手机号", "身份证号", "性别", "学历", "状态", "创建时间"};
      for (int i = 0; i < titles.length; i++) {
        header.createCell(i).setCellValue(titles[i]);
      }
      int rowIdx = 1;
      for (TrainSignUpPeopleSelfEntity p : people) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(p.getId() != null ? p.getId() : 0);
        row.createCell(1).setCellValue(p.getName() != null ? p.getName() : "");
        row.createCell(2).setCellValue(p.getPhone() != null ? p.getPhone() : "");
        row.createCell(3).setCellValue(p.getIdCardNumber() != null ? p.getIdCardNumber() : "");
        row.createCell(4).setCellValue(p.getGender() != null ? p.getGender() : "");
        row.createCell(5).setCellValue(p.getEducation() != null ? p.getEducation() : "");
        row.createCell(6).setCellValue(statusLabel(p.getStatus()));
        row.createCell(7).setCellValue(p.getCreateTime() != null ? p.getCreateTime().toString() : "");
      }
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败");
    }
  }

  private TrainSignUpSelfEntity getSignUp(Long id) {
    TrainSignUpSelfEntity entity = signUpMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名计划不存在");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
    }
    return entity;
  }

  private Map<Long, Long> countPeopleBySignUp(List<Long> signUpIds) {
    if (signUpIds.isEmpty()) {
      return Map.of();
    }
    List<TrainSignUpPeopleSelfEntity> rows = peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .in(TrainSignUpPeopleSelfEntity::getSignUpId, signUpIds)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    return rows.stream().collect(Collectors.groupingBy(TrainSignUpPeopleSelfEntity::getSignUpId, Collectors.counting()));
  }

  private Map<Long, String> resolveOrgNames(List<TrainSignUpSelfEntity> signUps) {
    List<Long> orgIds = signUps.stream()
        .map(TrainSignUpSelfEntity::getOrgId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (orgIds.isEmpty()) {
      return Map.of();
    }
    List<OrgEntity> orgs = orgMapper.selectBatchIds(orgIds);
    Map<Long, String> map = new LinkedHashMap<>();
    for (OrgEntity org : orgs) {
      map.put(org.getId(), org.getName());
    }
    return map;
  }

  private TrainSignUpSelfListVO toListVO(TrainSignUpSelfEntity e, long peopleCount, String orgName) {
    Path qr = Paths.get(cdnPath).resolve("wxqrcode/signup_self_" + e.getId() + ".png");
    String wxMiniQrcode = Files.exists(qr) ? cdnUrl + "/wxqrcode/signup_self_" + e.getId() + ".png" : "";
    return new TrainSignUpSelfListVO(
        e.getId(),
        e.getTitle(),
        e.getCover(),
        e.getTrainTime(),
        e.getExamTime(),
        e.getOrgId(),
        orgName,
        e.getTrainInfoId(),
        e.getSortNumber(),
        peopleCount,
        wxMiniQrcode,
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }

  private static String statusLabel(Integer status) {
    if (status == null || status == 0) {
      return "待审核";
    }
    return switch (status) {
      case 1 -> "已通过";
      case 2 -> "拒绝";
      default -> "待审核";
    };
  }
}

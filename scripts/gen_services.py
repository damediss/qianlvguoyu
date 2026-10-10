"""Generate proper ApplicationService and Controller files for new training tables."""
import os

BASE = "D:/qianlvguoyu_saas/services/platform-api/src/main/java/com/guoyu/platform/trainingdepartment"

# (prefix, resource_url, permission_suffix)
# Fields are read from entity files
TABLES = [
    ("TrainExamSession", "exam-sessions", "exam-session"),
    ("TrainExamRoomList", "exam-rooms", "exam-room"),
    ("TrainExamPeopleInRoom", "exam-people-rooms", "exam-people-room"),
    ("TrainExamStaff", "exam-staffs", "exam-staff"),
    ("TrainExamStaffConfig", "exam-staff-configs", "exam-staff-config"),
    ("TrainExamShijuan", "exam-shijuans", "exam-shijuan"),
    ("TrainExamWorkTypeList", "exam-work-types", "exam-work-type"),
    ("TrainExamReview", "exam-reviews", "exam-review"),
    ("TrainSignUpSelf", "sign-ups", "sign-up"),
    ("TrainSignUpPeopleSelf", "sign-up-people", "sign-up-people"),
    ("TrainSignUpOrcSelf", "sign-up-orcs", "sign-up-orc"),
    ("TrainFileTemp", "file-temps", "file-temp"),
    ("TrainBaseData", "base-data", "base-data"),
    ("TrainPositionToWorkType", "position-worktypes", "position-worktype"),
    ("DemandTrain", "demand-trains", "demand-train"),
    ("DemandEmployee", "demand-employees", "demand-employee"),
]

def read_entity_fields(prefix):
    """Read entity file and extract custom field names (exclude id, tenantId, createTime, updateTime, deleted)."""
    entity_path = os.path.join(BASE, "entity", f"{prefix}Entity.java")
    if not os.path.exists(entity_path):
        return []

    with open(entity_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Find all private field declarations, skip standard fields
    import re
    fields = []
    skip_fields = {"id", "tenantId", "createTime", "updateTime", "deleted"}
    for match in re.finditer(r'private\s+(\w+(?:<\w+>)?)\s+(\w+)\s*;', content):
        ftype = match.group(1)
        fname = match.group(2)
        if fname not in skip_fields:
            fields.append((fname, ftype))
    return fields


def generate_service(prefix, fields):
    camel = prefix[0].lower() + prefix[1:]

    lines = f'''package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.{prefix}CreateReq;
import com.guoyu.platform.trainingdepartment.dto.{prefix}QueryReq;
import com.guoyu.platform.trainingdepartment.dto.{prefix}UpdateReq;
import com.guoyu.platform.trainingdepartment.entity.{prefix}Entity;
import com.guoyu.platform.trainingdepartment.mapper.{prefix}Mapper;
import com.guoyu.platform.trainingdepartment.vo.{prefix}VO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class {prefix}ApplicationService {{

  private final {prefix}Mapper mapper;

  public {prefix}ApplicationService({prefix}Mapper mapper) {{
    this.mapper = mapper;
  }}

  @Transactional(readOnly = true)
  public PageResponse<{prefix}VO> page({prefix}QueryReq req) {{
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<{prefix}Entity> wrapper = new LambdaQueryWrapper<{prefix}Entity>()
        .eq({prefix}Entity::getTenantId, tenantId)
        .eq({prefix}Entity::getDeleted, 0)
        .orderByDesc({prefix}Entity::getId);
    if (StringUtils.hasText(req.getKeyword())) {{
      wrapper.like({prefix}Entity::getId, req.getKeyword());
    }}
    Page<{prefix}Entity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }}

  @Transactional(readOnly = true)
  public {prefix}VO getById(Long id) {{
    return toVO(getExisting(id));
  }}

  @Transactional
  public {prefix}VO create({prefix}CreateReq req) {{
    Long tenantId = SecurityContext.currentUser().getTenantId();
    {prefix}Entity entity = new {prefix}Entity();
    entity.setTenantId(tenantId);
'''

    # Set each field from createReq
    for fname, ftype in fields:
        getter = f"req.get{fname[0].upper() + fname[1:]}()"
        lines += f"    entity.set{fname[0].upper() + fname[1:]}({getter});\n"

    lines += '''    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public ''' + f'{prefix}VO update(Long id, {prefix}UpdateReq req)' + ''' {
    ''' + f'{prefix}Entity entity = getExisting(id);\n'

    for fname, ftype in fields:
        getter = f"req.get{fname[0].upper() + fname[1:]}()"
        setter = f"entity.set{fname[0].upper() + fname[1:]}"
        if ftype in ("String",):
            lines += f'    if (StringUtils.hasText(req.get{fname[0].upper() + fname[1:]}())) {{ {setter}({getter}); }}\n'
        else:
            lines += f'    if (req.get{fname[0].upper() + fname[1:]}() != null) {{ {setter}({getter}); }}\n'

    lines += '''    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    ''' + f'{prefix}Entity entity = getExisting(id);\n' + '''
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private ''' + f'{prefix}Entity getExisting(Long id)' + ''' {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ''' + f'{prefix}Entity entity = mapper.selectOne(new LambdaQueryWrapper<{prefix}Entity>()\n' + '''
        .eq(''' + f'{prefix}Entity::getTenantId, tenantId)\n' + f'''        .eq({prefix}Entity::getId, id)
        .eq({prefix}Entity::getDeleted, 0));
    if (entity == null) {{
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }}
    return entity;
  }}

  private ''' + f'{prefix}VO toVO({prefix}Entity e)' + ''' {
    return new ''' + f'{prefix}VO(\n'

    vo_fields = ['e.getId()']
    for fname, ftype in fields:
        vo_fields.append(f'e.get{fname[0].upper() + fname[1:]}()')
    vo_fields += ['e.getTenantId()', 'e.getCreateTime()', 'e.getUpdateTime()']

    for i, vf in enumerate(vo_fields):
        comma = ',' if i < len(vo_fields) - 1 else ''
        lines += f'        {vf}{comma}\n'

    lines += '''    );
  }
}
'''
    return lines


def generate_controller(prefix, url, perm):
    camel = prefix[0].lower() + prefix[1:]
    return f'''package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.{prefix}CreateReq;
import com.guoyu.platform.trainingdepartment.dto.{prefix}QueryReq;
import com.guoyu.platform.trainingdepartment.dto.{prefix}UpdateReq;
import com.guoyu.platform.trainingdepartment.service.{prefix}ApplicationService;
import com.guoyu.platform.trainingdepartment.vo.{prefix}VO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class {prefix}Controller {{

  private final {prefix}ApplicationService {camel}ApplicationService;

  public {prefix}Controller({prefix}ApplicationService {camel}ApplicationService) {{
    this.{camel}ApplicationService = {camel}ApplicationService;
  }}

  @GetMapping("/{url}")
  @PreAuthorize("hasAuthority('train:{perm}:view')")
  public ApiResponse<PageResponse<{prefix}VO>> page(@Valid @ModelAttribute {prefix}QueryReq req) {{
    return ApiResponse.ok({camel}ApplicationService.page(req));
  }}

  @GetMapping("/{url}/{{id}}")
  @PreAuthorize("hasAuthority('train:{perm}:view')")
  public ApiResponse<{prefix}VO> getById(@PathVariable Long id) {{
    return ApiResponse.ok({camel}ApplicationService.getById(id));
  }}

  @PostMapping("/{url}")
  @PreAuthorize("hasAuthority('train:{perm}:create')")
  @AuditLog(module = "training", action = "{perm}:create", targetType = "{url}")
  public ApiResponse<{prefix}VO> create(@Valid @RequestBody {prefix}CreateReq req) {{
    return ApiResponse.ok({camel}ApplicationService.create(req));
  }}

  @PutMapping("/{url}/{{id}}")
  @PreAuthorize("hasAuthority('train:{perm}:update')")
  @AuditLog(module = "training", action = "{perm}:update", targetType = "{url}")
  public ApiResponse<{prefix}VO> update(@PathVariable Long id, @Valid @RequestBody {prefix}UpdateReq req) {{
    return ApiResponse.ok({camel}ApplicationService.update(id, req));
  }}

  @DeleteMapping("/{url}/{{id}}")
  @PreAuthorize("hasAuthority('train:{perm}:delete')")
  @AuditLog(module = "training", action = "{perm}:delete", targetType = "{url}")
  public ApiResponse<Void> delete(@PathVariable Long id) {{
    {camel}ApplicationService.delete(id);
    return ApiResponse.ok();
  }}
}}
'''


for prefix, url, perm in TABLES:
    fields = read_entity_fields(prefix)
    if not fields:
        print(f"  SKIP {prefix} (no entity fields found)")
        continue

    # Service
    svc_path = os.path.join(BASE, "service", f"{prefix}ApplicationService.java")
    if not os.path.exists(svc_path):
        with open(svc_path, "w", encoding="utf-8") as f:
            f.write(generate_service(prefix, fields))
        print(f"  Service: {prefix}ApplicationService.java  ({len(fields)} fields)")

    # Controller
    ctrl_path = os.path.join(BASE, "controller", f"{prefix}Controller.java")
    if not os.path.exists(ctrl_path):
        with open(ctrl_path, "w", encoding="utf-8") as f:
            f.write(generate_controller(prefix, url, perm))
        print(f"  Controller: {prefix}Controller.java")

print("\nDone!")

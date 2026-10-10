"""Generate Entity, Mapper, VO, DTO files for new training tables."""
import os

BASE = "D:/qianlvguoyu_saas/services/platform-api/src/main/java/com/guoyu/platform/trainingdepartment"

# Table definitions: (table_name, class_prefix, fields)
# Fields: (javaName, javaType, dbColumn, dbType, comment)
TABLES = [
    ("train_solution_badge", "TrainSolutionBadge", [
        ("solutionId", "Long", "solution_id", "BIGINT NOT NULL", "方案ID"),
        ("title", "String", "title", "VARCHAR(255)", "费用项目名称"),
        ("badge", "BigDecimal", "badge", "DECIMAL(12,2)", "费用金额"),
        ("expend", "BigDecimal", "expend", "DECIMAL(12,2)", "支出金额"),
    ]),
    ("train_exam_session", "TrainExamSession", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("courseId", "Long", "course_id", "BIGINT", "课程ID"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
        ("startTime", "String", "start_time", "VARCHAR(20)", "开始时间"),
        ("endTime", "String", "end_time", "VARCHAR(20)", "结束时间"),
        ("courseName", "String", "course_name", "VARCHAR(255)", "课程名称"),
    ]),
    ("train_exam_room_list", "TrainExamRoomList", [
        ("orgId", "Long", "org_id", "BIGINT", "组织ID"),
        ("orgName", "String", "org_name", "VARCHAR(255)", "组织名称"),
        ("type", "String", "type", "VARCHAR(50)", "考场类型"),
        ("roomName", "String", "room_name", "VARCHAR(255)", "考场名称"),
        ("position", "String", "position", "VARCHAR(255)", "位置"),
        ("maxNum", "Integer", "max_num", "INT", "最大容纳人数"),
    ]),
    ("train_exam_people_in_room", "TrainExamPeopleInRoom", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("courseId", "Long", "course_id", "BIGINT", "课程ID"),
        ("sessionId", "Long", "session_id", "BIGINT", "场次ID"),
        ("roomId", "Long", "room_id", "BIGINT", "考场ID"),
        ("examRoomId", "Long", "exam_room_id", "BIGINT", "考场关联ID"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
        ("realname", "String", "realname", "VARCHAR(100)", "姓名"),
        ("peopleId", "Long", "people_id", "BIGINT", "考生人员ID"),
    ]),
    ("train_exam_staff", "TrainExamStaff", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("examRoomId", "Long", "exam_room_id", "BIGINT", "考场ID"),
        ("roomId", "Long", "room_id", "BIGINT", "教室ID"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
        ("realname", "String", "realname", "VARCHAR(100)", "姓名"),
        ("type", "String", "type", "VARCHAR(50)", "人员类型"),
    ]),
    ("train_exam_staff_config", "TrainExamStaffConfig", [
        ("type", "String", "type", "VARCHAR(50)", "人员类型"),
        ("realname", "String", "realname", "VARCHAR(100)", "姓名"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
    ]),
    ("train_exam_shijuan", "TrainExamShijuan", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("paperId", "Long", "paper_id", "BIGINT NOT NULL", "试卷ID"),
        ("examRoomId", "Long", "exam_room_id", "BIGINT", "考场ID"),
        ("operatePaperId", "Long", "operate_paper_id", "BIGINT", "实操试卷ID"),
        ("roomId", "Long", "room_id", "BIGINT", "教室ID"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
    ]),
    ("train_exam_work_type_list", "TrainExamWorkTypeList", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("workTypeId", "Long", "work_type_id", "BIGINT", "工种ID"),
        ("courseId", "Long", "course_id", "BIGINT", "课程ID"),
        ("level", "String", "level", "VARCHAR(50)", "等级"),
    ]),
    ("train_exam_review", "TrainExamReview", [
        ("examId", "Long", "exam_id", "BIGINT NOT NULL", "考试计划ID"),
        ("paperId", "Long", "paper_id", "BIGINT", "试卷ID"),
        ("userId", "Long", "user_id", "BIGINT", "用户ID"),
        ("num", "Integer", "num", "INT", "复习题数"),
    ]),
    ("train_sign_up_self", "TrainSignUpSelf", [
        ("title", "String", "title", "VARCHAR(255)", "标题"),
        ("cover", "String", "cover", "VARCHAR(500)", "封面图"),
        ("trainTime", "String", "train_time", "VARCHAR(255)", "培训时间"),
        ("examTime", "String", "exam_time", "VARCHAR(255)", "考试时间"),
        ("orgId", "Long", "org_id", "BIGINT", "组织ID"),
        ("trainInfoId", "Long", "train_info_id", "BIGINT", "培训信息ID"),
        ("sortNumber", "Integer", "sort_number", "INT", "排序号"),
    ]),
    ("train_sign_up_people_self", "TrainSignUpPeopleSelf", [
        ("signUpId", "Long", "sign_up_id", "BIGINT NOT NULL", "报名计划ID"),
        ("platformUserId", "Long", "platform_user_id", "BIGINT", "平台用户ID"),
        ("name", "String", "name", "VARCHAR(100)", "姓名"),
        ("phone", "String", "phone", "VARCHAR(20)", "手机号"),
        ("idCardNumber", "String", "id_card_number", "VARCHAR(20)", "身份证号"),
        ("gender", "String", "gender", "VARCHAR(10)", "性别"),
        ("education", "String", "education", "VARCHAR(50)", "学历"),
        ("photo", "String", "photo", "VARCHAR(500)", "照片路径"),
        ("contract", "String", "contract", "VARCHAR(500)", "合同路径"),
        ("certificate", "String", "certificate", "VARCHAR(500)", "证书路径"),
        ("status", "Integer", "status", "INT", "状态"),
    ]),
    ("train_sign_up_orc_self", "TrainSignUpOrcSelf", [
        ("signUpId", "Long", "sign_up_id", "BIGINT", "报名计划ID"),
        ("platformUserId", "Long", "platform_user_id", "BIGINT", "平台用户ID"),
        ("orcData", "String", "orc_data", "TEXT", "OCR识别原始数据"),
        ("status", "Integer", "status", "INT", "状态 0-失败 1-成功"),
        ("failReason", "String", "fail_reason", "VARCHAR(500)", "失败原因"),
    ]),
    ("train_task_log", "TrainTaskLog", [
        ("trainClassId", "Long", "train_class_id", "BIGINT", "班级ID"),
        ("ckId", "Long", "ck_id", "BIGINT", "任务检查点ID"),
        ("date", "String", "date", "VARCHAR(50)", "日期"),
        ("addtime", "Integer", "addtime", "INT", "添加时间戳"),
        ("status", "Integer", "status", "INT", "状态"),
        ("times", "String", "times", "VARCHAR(100)", "时间范围"),
    ]),
    ("train_file_temp", "TrainFileTemp", [
        ("type", "String", "type", "VARCHAR(50)", "模板类型"),
        ("title", "String", "title", "VARCHAR(255)", "模板名称"),
        ("city", "String", "city", "VARCHAR(100)", "城市/地区"),
        ("pathSource", "String", "path_source", "VARCHAR(500)", "源文件路径"),
    ]),
    ("train_base_data", "TrainBaseData", [
        ("dataKey", "String", "data_key", "VARCHAR(100)", "配置键"),
        ("dataValue", "String", "data_value", "TEXT", "配置值"),
        ("dataType", "String", "data_type", "VARCHAR(50)", "数据类型"),
        ("description", "String", "description", "VARCHAR(500)", "描述"),
    ]),
    ("train_position_to_work_type", "TrainPositionToWorkType", [
        ("positionName", "String", "position_name", "VARCHAR(255)", "岗位名称"),
        ("workTypeId", "Long", "work_type_id", "BIGINT", "工种ID"),
    ]),
    ("demand_train", "DemandTrain", [
        ("orgId", "Long", "org_id", "BIGINT NOT NULL", "企业ID"),
        ("content", "String", "content", "TEXT", "需求内容"),
        ("nums", "Integer", "nums", "INT", "人数"),
        ("status", "Integer", "status", "INT", "状态 0-待处理 1-已处理"),
        ("addtime", "Integer", "addtime", "INT", "添加时间戳"),
    ]),
    ("demand_employee", "DemandEmployee", [
        ("orgId", "Long", "org_id", "BIGINT NOT NULL", "企业ID"),
        ("work", "String", "work", "VARCHAR(255)", "工种/岗位"),
        ("education", "String", "education", "VARCHAR(50)", "学历要求"),
        ("major", "String", "major", "VARCHAR(255)", "专业要求"),
        ("addtime", "Integer", "addtime", "INT", "添加时间戳"),
    ]),
]

IMPORTS_MAP = {
    "BigDecimal": "import java.math.BigDecimal;",
    "LocalDateTime": "import java.time.LocalDateTime;",
}

def generate_entity(table, prefix, fields):
    """Generate Entity.java"""
    has_bigdecimal = any(f[1] == "BigDecimal" for f in fields)
    imports = [
        "import com.baomidou.mybatisplus.annotation.IdType;",
        "import com.baomidou.mybatisplus.annotation.TableId;",
        "import com.baomidou.mybatisplus.annotation.TableLogic;",
        "import com.baomidou.mybatisplus.annotation.TableName;",
    ]
    if has_bigdecimal:
        imports.append("import java.math.BigDecimal;")
    imports.append("import java.time.LocalDateTime;")

    lines = ['package com.guoyu.platform.trainingdepartment.entity;', '']
    for imp in sorted(set(imports)):
        lines.append(imp)
    lines.append('')
    lines.append(f'/**')
    lines.append(f' * {table}')
    lines.append(f' */')
    lines.append(f'@TableName("{table}")')
    lines.append(f'public class {prefix}Entity {{')
    lines.append('')
    lines.append('  @TableId(type = IdType.AUTO)')
    lines.append('  private Long id;')

    for javaName, javaType, dbCol, dbType, comment in fields:
        lines.append('')
        lines.append(f'  /** {comment} */')
        lines.append(f'  private {javaType} {javaName};')

    lines.append('')
    lines.append('  /** 租户ID */')
    lines.append('  private Long tenantId;')
    lines.append('  /** 创建时间 */')
    lines.append('  private LocalDateTime createTime;')
    lines.append('  /** 更新时间 */')
    lines.append('  private LocalDateTime updateTime;')
    lines.append('')
    lines.append('  /** 逻辑删除 0-未删除 1-已删除 */')
    lines.append('  @TableLogic')
    lines.append('  private Integer deleted;')

    # getters/setters
    for javaName, javaType, dbCol, dbType, comment in [("id","Long","id","BIGINT","主键")] + fields + [
        ("tenantId","Long","tenant_id","BIGINT","租户ID"),
        ("createTime","LocalDateTime","create_time","DATETIME","创建时间"),
        ("updateTime","LocalDateTime","update_time","DATETIME","更新时间"),
        ("deleted","Integer","deleted","TINYINT","逻辑删除"),
    ]:
        cap = javaName[0].upper() + javaName[1:]
        lines.append(f'  public {javaType} get{cap}() {{ return {javaName}; }}')
        lines.append(f'  public void set{cap}({javaType} {javaName}) {{ this.{javaName} = {javaName}; }}')

    lines.append('}')
    return '\n'.join(lines) + '\n'


def generate_mapper(table, prefix):
    """Generate Mapper.java"""
    return f'''package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.{prefix}Entity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {prefix}Mapper extends BaseMapper<{prefix}Entity> {{
}}
'''


def generate_vo(table, prefix, fields):
    """Generate VO record"""
    has_bigdecimal = any(f[1] == "BigDecimal" for f in fields)
    imports = []
    if has_bigdecimal:
        imports.append("import java.math.BigDecimal;")
    imports.append("import java.time.LocalDateTime;")

    lines = [f'package com.guoyu.platform.trainingdepartment.vo;', '']
    for imp in sorted(set(imports)):
        lines.append(imp)
    lines.append('')
    lines.append(f'/** {table} 视图对象 */')
    lines.append(f'public record {prefix}VO(')
    lines.append('    Long id,')

    for i, (javaName, javaType, dbCol, dbType, comment) in enumerate(fields):
        comma = ',' if i < len(fields) - 1 or True else ''
        lines.append(f'    /** {comment} */')
        lines.append(f'    {javaType} {javaName},')

    lines.append('    /** 租户ID */')
    lines.append('    Long tenantId,')
    lines.append('    /** 创建时间 */')
    lines.append('    LocalDateTime createTime,')
    lines.append('    /** 更新时间 */')
    lines.append('    LocalDateTime updateTime')
    lines.append(') {}')
    return '\n'.join(lines) + '\n'


def generate_create_req(table, prefix, fields):
    """Generate CreateReq DTO"""
    has_bigdecimal = any(f[1] == "BigDecimal" for f in fields)
    has_notblank = any(f[0] in ("title", "name") and f[1] == "String" for f in fields)
    imports = []
    if has_notblank:
        imports.append("import jakarta.validation.constraints.NotBlank;")
    imports.append("import jakarta.validation.constraints.Size;")
    if has_bigdecimal:
        imports.append("import java.math.BigDecimal;")

    lines = [f'package com.guoyu.platform.trainingdepartment.dto;', '']
    for imp in sorted(set(imports)):
        lines.append(imp)
    lines.append('')
    lines.append(f'/** {table} 创建请求 */')
    lines.append(f'public class {prefix}CreateReq {{')

    for javaName, javaType, dbCol, dbType, comment in fields:
        lines.append('')
        if javaName in ("title", "name") and javaType == "String":
            lines.append('  @NotBlank')
        lines.append(f'  @Size(max = 255)')
        lines.append(f'  /** {comment} */')
        lines.append(f'  private {javaType} {javaName};')

    # getters/setters
    for javaName, javaType, dbCol, dbType, comment in fields:
        cap = javaName[0].upper() + javaName[1:]
        lines.append('')
        lines.append(f'  public {javaType} get{cap}() {{')
        lines.append(f'    return {javaName};')
        lines.append(f'  }}')
        lines.append('')
        lines.append(f'  public void set{cap}({javaType} {javaName}) {{')
        lines.append(f'    this.{javaName} = {javaName};')
        lines.append(f'  }}')

    lines.append('}')
    return '\n'.join(lines) + '\n'


def generate_query_req(table, prefix, fields):
    """Generate QueryReq DTO"""
    # Pick key foreign key fields and add keyword
    fk_fields = [(n, t, c, d, cm) for n, t, c, d, cm in fields if n.endswith("Id") and t == "Long"][:3]
    has_bigdecimal = any(f[1] == "BigDecimal" for f in fields)

    imports = []
    if has_bigdecimal:
        imports.append("import java.math.BigDecimal;")

    lines = [f'package com.guoyu.platform.trainingdepartment.dto;', '']
    for imp in sorted(set(imports)):
        lines.append(imp)
    lines.append('')
    lines.append(f'/** {table} 查询请求 */')
    lines.append(f'public class {prefix}QueryReq {{')
    lines.append('')
    lines.append('  private String keyword;')
    lines.append('  private Integer page = 1;')
    lines.append('  private Integer pageSize = 10;')

    for javaName, javaType, dbCol, dbType, comment in fk_fields:
        lines.append(f'  /** {comment} */')
        lines.append(f'  private {javaType} {javaName};')

    # getters/setters
    all_query_fields = [("keyword","String"), ("page","Integer"), ("pageSize","Integer")] + [(n, t) for n, t, _, _, _ in fk_fields]
    for javaName, javaType in all_query_fields:
        cap = javaName[0].upper() + javaName[1:]
        lines.append(f'  public {javaType} get{cap}() {{ return {javaName}; }}')
        lines.append(f'  public void set{cap}({javaType} {javaName}) {{ this.{javaName} = {javaName}; }}')

    lines.append('}')
    return '\n'.join(lines) + '\n'


def generate_update_req(table, prefix, fields):
    """Generate UpdateReq DTO"""
    has_bigdecimal = any(f[1] == "BigDecimal" for f in fields)
    imports = []
    imports.append("import jakarta.validation.constraints.Size;")
    if has_bigdecimal:
        imports.append("import java.math.BigDecimal;")

    lines = [f'package com.guoyu.platform.trainingdepartment.dto;', '']
    for imp in sorted(set(imports)):
        lines.append(imp)
    lines.append('')
    lines.append(f'/** {table} 更新请求 */')
    lines.append(f'public class {prefix}UpdateReq {{')

    for javaName, javaType, dbCol, dbType, comment in fields:
        lines.append('')
        lines.append(f'  @Size(max = 255)')
        lines.append(f'  /** {comment} */')
        lines.append(f'  private {javaType} {javaName};')

    for javaName, javaType, dbCol, dbType, comment in fields:
        cap = javaName[0].upper() + javaName[1:]
        lines.append(f'  public {javaType} get{cap}() {{ return {javaName}; }}')
        lines.append(f'  public void set{cap}({javaType} {javaName}) {{ this.{javaName} = {javaName}; }}')

    lines.append('}')
    return '\n'.join(lines) + '\n'


# Generate all files
for table, prefix, fields in TABLES:
    # Entity
    entity_path = os.path.join(BASE, "entity", f"{prefix}Entity.java")
    with open(entity_path, "w", encoding="utf-8") as f:
        f.write(generate_entity(table, prefix, fields))
    print(f"  Entity: {prefix}Entity.java")

    # Mapper
    mapper_path = os.path.join(BASE, "mapper", f"{prefix}Mapper.java")
    with open(mapper_path, "w", encoding="utf-8") as f:
        f.write(generate_mapper(table, prefix))
    print(f"  Mapper: {prefix}Mapper.java")

    # VO
    vo_path = os.path.join(BASE, "vo", f"{prefix}VO.java")
    with open(vo_path, "w", encoding="utf-8") as f:
        f.write(generate_vo(table, prefix, fields))
    print(f"  VO: {prefix}VO.java")

    # CreateReq
    create_path = os.path.join(BASE, "dto", f"{prefix}CreateReq.java")
    with open(create_path, "w", encoding="utf-8") as f:
        f.write(generate_create_req(table, prefix, fields))
    print(f"  DTO: {prefix}CreateReq.java")

    # QueryReq
    query_path = os.path.join(BASE, "dto", f"{prefix}QueryReq.java")
    with open(query_path, "w", encoding="utf-8") as f:
        f.write(generate_query_req(table, prefix, fields))
    print(f"  DTO: {prefix}QueryReq.java")

    # UpdateReq
    update_path = os.path.join(BASE, "dto", f"{prefix}UpdateReq.java")
    with open(update_path, "w", encoding="utf-8") as f:
        f.write(generate_update_req(table, prefix, fields))
    print(f"  DTO: {prefix}UpdateReq.java")

print(f"\nDone! Generated {len(TABLES)} entities × 6 files = {len(TABLES)*6} files.")

package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

/**
 * 课程课件视图对象
 */
public record CourseKejianVO(
    /** 主键ID */
    Long id,
    /** 课件编号 */
    String sno,
    /** 课件名称 */
    String title,
    /** 课时 */
    Integer keshi,
    /** 封面 */
    String cover,
    /** 获取方式:0免费,1收费 */
    Integer type,
    /** 单价 */
    BigDecimal price,
    /** 描述 */
    String description,
    /** 内容 */
    String content,
    /** 文件路径 */
    String filePath,
    /** 工种id */
    Long workTypeId,
    /** 0平台,1企业 */
    Integer tag,
    /** 机构id */
    Long jigouId,
    /** 拥有者id */
    Long ownerId,
    /** 添加时间 */
    Long addtime
) {}

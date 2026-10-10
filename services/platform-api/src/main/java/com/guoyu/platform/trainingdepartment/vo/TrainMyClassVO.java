package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/** App「我的班级」列表项 */
public record TrainMyClassVO(
    Long classId,
    String title,
    List<String> roleCodes,
    String primaryRole,
    boolean unboundWarning
) {
}

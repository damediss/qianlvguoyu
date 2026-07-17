package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record TrainDivisionReadyVO(
    Long planId,
    Integer status,
    /** 名单+排班均已上传（完整回传） */
    Boolean enterpriseReturnReady,
    /** 排班表是否已上传（主路径必填） */
    Boolean shiftReady,
    /** 筛选名单是否已上传 */
    Boolean rosterReady,
    /** 配置：主路径是否要求名单 */
    Boolean requireRoster,
    /** 是否可走排班表驱动主路径 */
    Boolean divisionPathReady,
    Long rosterFileId,
    Long shiftFileId,
    String rosterFileName,
    String shiftFileName,
    List<String> missing
) {
}

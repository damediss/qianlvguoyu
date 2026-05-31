package com.guoyu.platform.system.vo;

import java.util.List;

public record PermissionMatrixVO(
    Long menuId,
    String menuName,
    String menuCode,
    Integer menuSortOrder,
    List<PermissionItemVO> permissions
) {
}

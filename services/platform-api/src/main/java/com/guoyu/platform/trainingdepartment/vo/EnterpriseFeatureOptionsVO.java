package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record EnterpriseFeatureOptionsVO(
    List<MenuOption> menus,
    List<RoleOption> roles
) {
  public record MenuOption(Long id, Long parentId, String code, String name, String menuType) {}

  public record RoleOption(Long id, String code, String name, boolean defaultRole) {}
}

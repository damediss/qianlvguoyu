package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record EnterpriseFeatureOptionsVO(
    List<MenuOption> menus,
    List<RoleOption> roles
) {
  public record MenuOption(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      @JsonSerialize(using = ToStringSerializer.class) Long parentId,
      String code,
      String name,
      String menuType
  ) {}

  public record RoleOption(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      String code,
      String name,
      boolean defaultRole
  ) {}
}

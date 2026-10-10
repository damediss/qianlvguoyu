package com.guoyu.platform.system.mapper;

import com.guoyu.platform.system.entity.MenuEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMenuMapper {

  @Select("""
      SELECT DISTINCT m.*
      FROM sys_menu m
      INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
      INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
      WHERE m.tenant_id = #{tenantId}
        AND ur.tenant_id = #{tenantId}
        AND rm.tenant_id = #{tenantId}
        AND ur.user_id = #{userId}
        AND m.deleted = 0
        AND rm.deleted = 0
        AND ur.deleted = 0
        AND m.status = 'enabled'
        AND m.visible = 1
      ORDER BY m.sort_order, m.id
      """)
  List<MenuEntity> selectMenusByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

  @Select("""
      SELECT DISTINCT p.code
      FROM sys_permission p
      INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
      INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
      WHERE p.tenant_id = #{tenantId}
        AND rp.tenant_id = #{tenantId}
        AND ur.tenant_id = #{tenantId}
        AND ur.user_id = #{userId}
        AND p.deleted = 0
        AND rp.deleted = 0
        AND ur.deleted = 0
        AND p.status = 'enabled'
      ORDER BY p.code
      """)
  List<String> selectPermissionCodesByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

  @Select("""
      SELECT menu_id
      FROM sys_role_menu
      WHERE tenant_id = #{tenantId}
        AND role_id = #{roleId}
        AND deleted = 0
      ORDER BY menu_id
      """)
  List<Long> selectMenuIdsByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

  @Select("""
      SELECT permission_id
      FROM sys_role_permission
      WHERE tenant_id = #{tenantId}
        AND role_id = #{roleId}
        AND deleted = 0
      ORDER BY permission_id
      """)
  List<Long> selectPermissionIdsByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}

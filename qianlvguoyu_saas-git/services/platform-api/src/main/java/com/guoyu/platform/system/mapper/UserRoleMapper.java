package com.guoyu.platform.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper {

  @Select("""
      SELECT r.code
      FROM sys_role r
      INNER JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.tenant_id = #{tenantId}
        AND ur.user_id = #{userId}
        AND ur.deleted = 0
        AND r.deleted = 0
        AND r.status = 'enabled'
      ORDER BY r.id
      """)
  List<String> selectRoleCodes(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

  @Select("""
      SELECT r.id
      FROM sys_role r
      INNER JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.tenant_id = #{tenantId}
        AND ur.user_id = #{userId}
        AND ur.deleted = 0
        AND r.deleted = 0
        AND r.status = 'enabled'
      ORDER BY r.id
      """)
  List<Long> selectRoleIds(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}

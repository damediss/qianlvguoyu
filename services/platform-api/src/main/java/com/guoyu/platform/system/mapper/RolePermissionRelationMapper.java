package com.guoyu.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.system.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface RolePermissionRelationMapper extends BaseMapper<RolePermissionEntity> {

  @Delete("""
      DELETE FROM sys_role_permission
      WHERE tenant_id = #{tenantId}
        AND role_id = #{roleId}
      """)
  int physicalDeleteByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}

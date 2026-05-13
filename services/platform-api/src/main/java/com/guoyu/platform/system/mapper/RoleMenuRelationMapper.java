package com.guoyu.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.system.entity.RoleMenuEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface RoleMenuRelationMapper extends BaseMapper<RoleMenuEntity> {

  @Delete("""
      DELETE FROM sys_role_menu
      WHERE tenant_id = #{tenantId}
        AND role_id = #{roleId}
      """)
  int physicalDeleteByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}

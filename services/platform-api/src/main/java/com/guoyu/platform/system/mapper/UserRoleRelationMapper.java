package com.guoyu.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.system.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserRoleRelationMapper extends BaseMapper<UserRoleEntity> {

  @Delete("""
      DELETE FROM sys_user_role
      WHERE tenant_id = #{tenantId}
        AND user_id = #{userId}
      """)
  int physicalDeleteByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}

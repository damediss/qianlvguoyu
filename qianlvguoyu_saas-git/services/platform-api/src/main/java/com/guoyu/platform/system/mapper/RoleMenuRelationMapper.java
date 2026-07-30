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

  /**
   * 级联删菜单时使用：先在调用方收集所有待删 menuId（含子树），再调用此方法一次性清空关联。
   *
   * 注意：命名为 softDelete 是因为 BaseEntity.deleted 标了 @TableLogic，
   * delete(wrapper) 会被劫持为 UPDATE ... SET deleted=1，本质是软删。
   * 如果将来真要物理清（比如数据归档后批量清脏关联），需要新增带
   * @InterceptorIgnore(tenantLine = "true") 的注解 + 原生 SQL。
   */
  default int softDeleteByMenus(Long tenantId, java.util.List<Long> menuIds) {
    if (menuIds == null || menuIds.isEmpty()) {
      return 0;
    }
    return delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleMenuEntity>()
        .eq(RoleMenuEntity::getTenantId, tenantId)
        .in(RoleMenuEntity::getMenuId, menuIds));
  }
}

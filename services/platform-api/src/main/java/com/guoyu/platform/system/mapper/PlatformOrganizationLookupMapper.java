package com.guoyu.platform.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 只读查询 PHP 侧企业库 {@code platform_organization}（含地址/邮编/电话），供默认模板导出回填。
 */
@Mapper
public interface PlatformOrganizationLookupMapper {

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT title AS title,
             address AS address,
             phone AS phone,
             zipcode AS zipcode,
             title_code AS titleCode
      FROM platform_organization
      WHERE deleted = 0 AND title = #{title}
      LIMIT 1
      """)
  PlatformOrganizationBrief findByTitle(@Param("title") String title);

  record PlatformOrganizationBrief(
      String title,
      String address,
      String phone,
      String zipcode,
      String titleCode) {}
}

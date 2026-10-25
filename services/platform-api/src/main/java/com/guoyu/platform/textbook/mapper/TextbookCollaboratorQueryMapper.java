package com.guoyu.platform.textbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.guoyu.platform.textbook.vo.TextbookCollaboratorVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * C1 候选人查询（方案 §6.1）：同租户、enabled、且同时拥有指定全部权限码的 sys_user。
 *
 * <p>权限判定走 sys_user_role → sys_role_permission → sys_permission 三表 JOIN，
 * HAVING COUNT(DISTINCT p.code) = 需要的权限数保证「同时拥有」；多租户条件由
 * MyBatis-Plus 租户插件对全部别名的表自动追加，本 SQL 不手写 tenant 条件。
 * 不返回手机号等敏感字段（方案 §12.2-6）。userId 转字符串下发（雪花 ID 超 JS 安全整数）。
 */
public interface TextbookCollaboratorQueryMapper extends BaseMapper<Void> {

  String COLLABORATOR_SQL = """
      <script>
      SELECT DISTINCT CAST(u.id AS CHAR) AS userId, u.display_name AS displayName,
             u.username AS username, o.name AS orgName
      FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
      JOIN sys_role_permission rp ON rp.role_id = ur.role_id AND rp.deleted = 0
      JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
      LEFT JOIN sys_org o ON o.id = u.org_id AND o.deleted = 0
      WHERE u.status = 'enabled' AND u.deleted = 0
        AND p.code IN
        <foreach collection="requiredPermissions" item="code" open="(" separator="," close=")">
          #{code}
        </foreach>
        <if test="keyword != null and keyword != ''">
          AND (u.display_name LIKE CONCAT('%', #{keyword}, '%')
               OR u.username LIKE CONCAT('%', #{keyword}, '%'))
        </if>
      GROUP BY u.id, u.display_name, u.username, o.name
      HAVING COUNT(DISTINCT p.code) = #{requiredCount}
      ORDER BY u.id
      </script>
      """;

  @Select(COLLABORATOR_SQL)
  List<TextbookCollaboratorVO> selectCollaborators(IPage<TextbookCollaboratorVO> page,
      @Param("requiredPermissions") List<String> requiredPermissions,
      @Param("requiredCount") int requiredCount,
      @Param("keyword") String keyword);

  /** 指定用户是否同时拥有全部权限码（批量分配预检，避免逐候选分页遗漏）。 */
  @Select("""
      <script>
      SELECT COUNT(DISTINCT p.code) FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
      JOIN sys_role_permission rp ON rp.role_id = ur.role_id AND rp.deleted = 0
      JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
      WHERE u.id = #{userId} AND u.status = 'enabled' AND u.deleted = 0
        AND p.code IN
        <foreach collection="requiredPermissions" item="code" open="(" separator="," close=")">
          #{code}
        </foreach>
      </script>
      """)
  int countUserPermissions(@Param("userId") Long userId,
      @Param("requiredPermissions") List<String> requiredPermissions);
}

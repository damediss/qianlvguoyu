package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionRegistrationMapper extends BaseMapper<CompetitionRegistrationEntity> {

  @Select("SELECT * FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND id = #{registrationId} AND deleted = 0 FOR UPDATE")
  CompetitionRegistrationEntity selectForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("registrationId") Long registrationId);

  @Select("SELECT COUNT(1) FROM competition_registration r "
      + "JOIN competition_roster_item i ON i.id = r.roster_item_id "
      + "AND i.tenant_id = r.tenant_id AND i.deleted = 0 "
      + "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId} AND r.org_id = #{orgId} "
      + "AND i.employee_no = #{employeeNo} AND r.deleted = 0 "
      + "AND r.status NOT IN ('rejected', 'withdrawn', 'expired')")
  boolean existsEffectiveByEmployeeNo(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId,
      @Param("employeeNo") String employeeNo);

  @Select("SELECT COUNT(1) FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND org_id = #{orgId} "
      + "AND deleted = 0 AND status IN ('pending_audit', 'supplement_required', 'eligible_unactivated')")
  long countEffectiveByEventOrg(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId);

  @Select({
      "<script>",
      "SELECT r.id AS registration_id, r.event_id, e.name AS event_name,",
      "r.org_id, o.name AS org_name, r.roster_item_id, i.employee_no, i.name,",
      "i.work_type, i.level, r.source, r.status, r.audit_reason, r.audited_by,",
      "r.audited_at, r.created_at",
      "FROM competition_registration r",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event e ON e.id = r.event_id",
      "AND e.tenant_id = r.tenant_id AND e.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id",
      "AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.deleted = 0",
      "<if test='eventId != null'> AND r.event_id = #{eventId}</if>",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "<if test='status != null and status != \"\"'> AND r.status = #{status}</if>",
      "<if test='employeeNo != null and employeeNo != \"\"'> AND i.employee_no LIKE CONCAT('%', #{employeeNo}, '%')</if>",
      "<if test='name != null and name != \"\"'> AND i.name LIKE CONCAT('%', #{name}, '%')</if>",
      "ORDER BY r.created_at DESC, r.id DESC",
      "</script>"
  })
  Page<CompetitionRegistrationQueryRow> selectPageWithDetails(
      Page<CompetitionRegistrationQueryRow> page,
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId,
      @Param("status") String status,
      @Param("employeeNo") String employeeNo,
      @Param("name") String name);
}

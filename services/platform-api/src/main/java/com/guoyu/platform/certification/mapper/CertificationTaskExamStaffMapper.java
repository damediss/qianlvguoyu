package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CertificationTaskExamStaffMapper extends BaseMapper<CertificationTaskExamStaffEntity> {

  @Select("""
      SELECT id, tenant_id, task_id, staff_id, staff_name, phone, masked_phone, remark, status, task_role, role_name, is_leader, sort_order,
             created_at, updated_at, created_by, updated_by, deleted
      FROM certification_task_exam_staff
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND staff_id = #{staffId}
      LIMIT 1
      """)
  CertificationTaskExamStaffEntity selectByTaskAndStaffIncludingDeleted(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("staffId") Long staffId
  );

  @Update("""
      UPDATE certification_task_exam_staff
      SET task_role = 'exam_staff',
          role_name = '考务员',
          is_leader = 0
      WHERE task_id = #{taskId}
        AND deleted = 0
      """)
  int clearInternalSupervisorsByTask(@Param("taskId") Long taskId);

  default int clearLeadersByTask(Long taskId) {
    return clearInternalSupervisorsByTask(taskId);
  }

  @Delete("""
      DELETE FROM certification_task_exam_staff
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND staff_id = #{staffId}
        AND deleted = 1
      """)
  int purgeSoftDeletedByTaskAndStaff(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("staffId") Long staffId);

  @Update("""
      UPDATE certification_task_exam_staff
      SET staff_name = #{entity.staffName},
          phone = #{entity.phone},
          masked_phone = #{entity.maskedPhone},
          remark = #{entity.remark},
          status = #{entity.status},
          task_role = #{entity.taskRole},
          role_name = #{entity.roleName},
          is_leader = #{entity.isLeader},
          sort_order = #{entity.sortOrder},
          deleted = 0,
          updated_at = NOW()
      WHERE id = #{entity.id}
      """)
  int restoreSoftDeleted(@Param("entity") CertificationTaskExamStaffEntity entity);

  @Select("""
      <script>
      SELECT tes.staff_id AS staffId, ct.task_no AS taskNo
      FROM certification_task_exam_staff tes
      INNER JOIN certification_task ct ON ct.id = tes.task_id AND ct.deleted = 0
      WHERE tes.deleted = 0
                  AND ct.current_phase != 'certification_completed'
        AND tes.staff_id IN
        <foreach collection="staffIds" item="staffId" open="(" separator="," close=")">
          #{staffId}
        </foreach>
      ORDER BY ct.task_no
      </script>
      """)
  List<ExamStaffAssignedTaskRow> selectAssignedTaskNosByStaffIds(@Param("staffIds") Collection<Long> staffIds);
}

package com.guoyu.platform.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.entity.ExamFaqConversionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExamFaqConversionMapper extends BaseMapper<ExamFaqConversionEntity> {

  /** 批量插入（MyBatis-Plus 内置 saveBatch 即可，此处留扩展用） */

  /** 原子递增成功计数 */
  @Update("UPDATE exam_faq_conversion_batch SET success_count = success_count + 1 WHERE id = #{batchId}")
  int incrementSuccess(@Param("batchId") Long batchId);

  /** 原子递增失败计数 */
  @Update("UPDATE exam_faq_conversion_batch SET failed_count = failed_count + 1 WHERE id = #{batchId}")
  int incrementFailed(@Param("batchId") Long batchId);

  /** 原子递增跳过计数 */
  @Update("UPDATE exam_faq_conversion_batch SET skipped_count = skipped_count + 1 WHERE id = #{batchId}")
  int incrementSkipped(@Param("batchId") Long batchId);

  /** 原子递减待转换计数 */
  @Update("UPDATE exam_faq_conversion_batch SET pending_count = pending_count - 1 WHERE id = #{batchId}")
  int decrementPending(@Param("batchId") Long batchId);

  /** 查询批次下指定状态的记录数 */
  @Select("SELECT COUNT(*) FROM exam_faq_conversion WHERE batch_id IS NULL AND tenant_id = #{tenantId} AND conversion_status = #{status} AND deleted = 0")
  int countByStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

  /** 查询某批次下所有成功的、未导入的记录 */
  @Select("SELECT * FROM exam_faq_conversion WHERE tenant_id = #{tenantId} AND conversion_status = 'success' AND weknora_imported = 0 AND deleted = 0 ORDER BY tag_name")
  List<ExamFaqConversionEntity> findExportable(@Param("tenantId") Long tenantId);
}

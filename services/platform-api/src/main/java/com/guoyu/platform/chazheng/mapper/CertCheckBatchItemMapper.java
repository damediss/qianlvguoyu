package com.guoyu.platform.chazheng.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.chazheng.entity.CertCheckBatchItemEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 查证明细 Mapper。
 */
public interface CertCheckBatchItemMapper extends BaseMapper<CertCheckBatchItemEntity> {

  @Update("UPDATE chazheng_cert_batch_item SET status=#{status}, started_at=NOW(), updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND user_id=#{userId}")
  int updateStatus(@Param("batchId") Long batchId, @Param("userId") Long userId,
                   @Param("status") String status, @Param("error") String error);

  @Update("UPDATE chazheng_cert_batch_item SET status=#{status}, completed_at=NOW(), updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND user_id=#{userId}")
  int updateSuccess(@Param("batchId") Long batchId, @Param("userId") Long userId,
                    @Param("status") String status);

  @Update("UPDATE chazheng_cert_batch_item SET status='failed', error_message=#{error}, "
      + "completed_at=NOW(), updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND user_id=#{userId}")
  int updateFailed(@Param("batchId") Long batchId, @Param("userId") Long userId,
                   @Param("error") String error);

  @Update("UPDATE chazheng_cert_batch_item SET retry_count = retry_count + 1, updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND user_id=#{userId}")
  int incrementRetry(@Param("batchId") Long batchId, @Param("userId") Long userId);

  @Select("SELECT status, COUNT(*) AS cnt FROM chazheng_cert_batch_item "
      + "WHERE batch_id=#{batchId} GROUP BY status")
  List<Map<String, Object>> countByStatus(@Param("batchId") Long batchId);

  @Select("SELECT * FROM chazheng_cert_batch_item "
      + "WHERE batch_id=#{batchId} AND status='failed'")
  List<CertCheckBatchItemEntity> selectFailedItems(@Param("batchId") Long batchId);

  @Update("UPDATE chazheng_cert_batch_item SET status='pending', retry_count=0, "
      + "error_message=NULL, updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND status='failed'")
  int resetFailedToPending(@Param("batchId") Long batchId);

  /** 熔断中止：把批次内所有未完成的明细一次性标记为失败（不逐条 incrementCompleted，批次状态由 markAborted 收口） */
  @Update("UPDATE chazheng_cert_batch_item SET status='failed', error_message=#{error}, "
      + "completed_at=NOW(), updated_at=NOW() "
      + "WHERE batch_id=#{batchId} AND status IN ('pending','processing')")
  int failOpenItems(@Param("batchId") Long batchId, @Param("error") String error);
}

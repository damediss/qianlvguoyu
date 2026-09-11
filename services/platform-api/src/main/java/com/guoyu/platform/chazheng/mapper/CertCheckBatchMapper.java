package com.guoyu.platform.chazheng.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.chazheng.entity.CertCheckBatchEntity;
import org.apache.ibatis.annotations.Update;

/**
 * 查证批次 Mapper。
 */
public interface CertCheckBatchMapper extends BaseMapper<CertCheckBatchEntity> {

  /**
   * 原子递增 completed_count 并自动判断是否完成。
   */
  @Update("UPDATE chazheng_cert_batch SET completed_count = completed_count + 1, "
      + "status = CASE WHEN completed_count + 1 >= total_count THEN 'completed' ELSE status END, "
      + "completed_at = CASE WHEN completed_count + 1 >= total_count THEN NOW() ELSE completed_at END "
      + "WHERE id = #{batchId} AND status = 'processing'")
  int incrementCompleted(Long batchId);

  /**
   * 标记批次为完成(原子操作,并发安全)。
   */
  @Update("UPDATE chazheng_cert_batch SET status = 'completed', completed_at = NOW() "
      + "WHERE id = #{batchId} AND completed_count >= total_count AND status = 'processing'")
  int markCompletedIfDone(Long batchId);

  /** 熔断中止批次（仅 processing 状态可中止，幂等） */
  @Update("UPDATE chazheng_cert_batch SET status = 'aborted', completed_at = NOW() "
      + "WHERE id = #{batchId} AND status = 'processing'")
  int markAborted(Long batchId);

  /** 人工重试失败用户时把中止批次重新打开（incrementCompleted 只认 processing） */
  @Update("UPDATE chazheng_cert_batch SET status = 'processing', completed_at = NULL "
      + "WHERE id = #{batchId} AND status = 'aborted'")
  int reopenAborted(Long batchId);
}

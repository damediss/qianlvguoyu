package com.guoyu.platform.competition.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionStageUnlockOutboxEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
public interface CompetitionStageUnlockOutboxMapper extends BaseMapper<CompetitionStageUnlockOutboxEntity> {
  @InterceptorIgnore(tenantLine = "true")
  @Select("SELECT DISTINCT tenant_id FROM competition_stage_unlock_outbox WHERE status = 'pending' AND deleted = 0")
  List<Long> selectPendingTenantIds();

  @Select("SELECT * FROM competition_stage_unlock_outbox WHERE tenant_id = #{tenantId} AND status = 'pending' AND deleted = 0 ORDER BY id LIMIT 100")
  List<CompetitionStageUnlockOutboxEntity> selectPending(@Param("tenantId") Long tenantId);

  @Update("UPDATE competition_stage_unlock_outbox SET status = 'completed', processed_at = NOW(), updated_at = NOW() WHERE tenant_id = #{tenantId} AND id = #{id} AND status = 'pending' AND deleted = 0")
  int markCompleted(@Param("tenantId") Long tenantId, @Param("id") Long id);
}

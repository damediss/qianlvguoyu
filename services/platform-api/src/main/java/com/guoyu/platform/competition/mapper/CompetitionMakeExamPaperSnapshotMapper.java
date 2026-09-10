package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionMakeExamPaperSnapshotMapper
    extends BaseMapper<CompetitionMakeExamPaperSnapshotEntity> {

  @Select("SELECT * FROM competition_make_exam_paper_snapshot "
      + "WHERE tenant_id = #{tenantId} AND plan_id = #{planId} AND deleted = 0 "
      + "ORDER BY paper_code, version_no DESC, id")
  List<CompetitionMakeExamPaperSnapshotEntity> selectByPlan(
      @Param("tenantId") Long tenantId, @Param("planId") Long planId);
}

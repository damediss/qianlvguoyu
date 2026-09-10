package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalExamQuestionSnapshotMapper
    extends BaseMapper<CompetitionFormalExamQuestionSnapshotEntity> {

  @Select("""
      SELECT * FROM competition_formal_exam_question_snapshot
      WHERE paper_snapshot_id = #{paperSnapshotId} AND deleted = 0
      ORDER BY question_order ASC, id ASC
      """)
  List<CompetitionFormalExamQuestionSnapshotEntity> selectByPaperSnapshotId(
      @Param("paperSnapshotId") Long paperSnapshotId);

  @Select({
      "<script>",
      "SELECT * FROM competition_formal_exam_question_snapshot",
      "WHERE paper_snapshot_id = #{paperSnapshotId} AND deleted = 0",
      "AND id IN",
      "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
      "#{id}",
      "</foreach>",
      "</script>"
  })
  List<CompetitionFormalExamQuestionSnapshotEntity> selectByPaperSnapshotIdAndIds(
      @Param("paperSnapshotId") Long paperSnapshotId,
      @Param("ids") List<Long> ids);
}

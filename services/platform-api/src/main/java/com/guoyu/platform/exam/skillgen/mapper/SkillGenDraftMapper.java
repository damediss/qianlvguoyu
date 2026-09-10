package com.guoyu.platform.exam.skillgen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkillGenDraftMapper extends BaseMapper<SkillGenDraftEntity> {

  /** 物理删除某细目表下未入库草稿（重建细目表时腾位，复盘"重做一等语义"） */
  @Delete("DELETE FROM exam_skill_gen_draft "
      + "WHERE tenant_id = #{tenantId} AND blueprint_id = #{blueprintId} AND publish_status = 0")
  int physicalDeleteUnpublished(@Param("tenantId") Long tenantId,
      @Param("blueprintId") Long blueprintId);
}

package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainBaseDataEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainBaseDataMapper extends BaseMapper<TrainBaseDataEntity> {

  /** 兼容 PHP 遗留 train_base_data（仅 id/tag/sno/title，无 tenant_id/deleted） */
  @Select("""
      SELECT id, title FROM train_base_data
      WHERE tag IN ('申报条件', 'condition', 'signup_declaration_condition')
      ORDER BY id ASC
      """)
  List<TrainBaseDataLegacyRow> selectDeclarationConditionsLegacy();

  /** 申报材料打包：加载全部码表（共享 PHP 库无 tenant_id/deleted） */
  @Select("""
      SELECT id, tag, sno, title FROM train_base_data
      WHERE tag IS NOT NULL AND TRIM(tag) <> ''
      ORDER BY id ASC
      """)
  List<TrainBaseDataMaterialRow> selectMaterialPackRowsLegacy();
}

package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;

/**
 * 培训项目表单管理服务接口
 */
public interface TrainProjectTableService {
    /** 分页查询项目表单 */
    PageResponse<TrainProjectTableVO> page(TrainProjectTableQueryReq req);
    /** 根据ID查询项目表单 */
    TrainProjectTableVO getById(Long id);
    /** 创建项目表单 */
    TrainProjectTableVO create(TrainProjectTableCreateReq req);
    /** 更新项目表单 */
    TrainProjectTableVO update(Long id, TrainProjectTableUpdateReq req);
    /** 删除项目表单 */
    void delete(Long id);
}

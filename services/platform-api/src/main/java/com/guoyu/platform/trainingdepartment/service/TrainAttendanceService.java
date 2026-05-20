package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceVO;

/**
 * 培训考勤管理服务接口
 */
public interface TrainAttendanceService {
    /** 分页查询考勤记录 */
    PageResponse<TrainAttendanceVO> page(TrainAttendanceQueryReq req);
    /** 根据ID查询考勤记录 */
    TrainAttendanceVO getById(Long id);
    /** 创建考勤记录 */
    TrainAttendanceVO create(TrainAttendanceCreateReq req);
    /** 更新考勤记录 */
    TrainAttendanceVO update(Long id, TrainAttendanceUpdateReq req);
    /** 删除考勤记录 */
    void delete(Long id);
    /** 签到 */
    TrainAttendanceVO signIn(TrainAttendanceCreateReq req);
}

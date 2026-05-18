package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.TrainFileListVO;

/**
 * 文件列表管理服务接口
 */
public interface TrainFileListService {

    /**
     * 分页查询文件列表
     *
     * @param req 查询请求参数
     * @return 分页结果
     */
    PageResponse<TrainFileListVO> page(TrainFileListQueryReq req);

    /**
     * 根据ID查询文件列表记录
     *
     * @param id 主键ID
     * @return 文件列表VO
     */
    TrainFileListVO getById(Long id);

    /**
     * 创建文件列表记录
     *
     * @param req 创建请求参数
     * @return 创建后的文件列表VO
     */
    TrainFileListVO create(TrainFileListCreateReq req);

    /**
     * 更新文件列表记录
     *
     * @param id  主键ID
     * @param req 更新请求参数
     * @return 更新后的文件列表VO
     */
    TrainFileListVO update(Long id, TrainFileListUpdateReq req);

    /**
     * 删除文件列表记录
     *
     * @param id 主键ID
     */
    void delete(Long id);
}

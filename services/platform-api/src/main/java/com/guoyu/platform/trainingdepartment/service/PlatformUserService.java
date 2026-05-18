package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserCreateReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;

/**
 * 平台用户管理服务接口
 */
public interface PlatformUserService {

    /**
     * 分页查询平台用户
     */
    PageResponse<PlatformUserVO> page(PlatformUserQueryReq req);

    /**
     * 根据ID查询平台用户
     */
    PlatformUserVO getById(Long id);

    /**
     * 创建平台用户
     */
    PlatformUserVO create(PlatformUserCreateReq req);

    /**
     * 更新平台用户
     */
    PlatformUserVO update(Long id, PlatformUserUpdateReq req);

    /**
     * 删除平台用户
     */
    void delete(Long id);
}

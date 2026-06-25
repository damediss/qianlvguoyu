package com.guoyu.platform.app.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 对齐 PHP AppuserController */
@Service
public class AppUserApplicationService {

  private final PlatformUserDetailMapper detailMapper;

  public AppUserApplicationService(PlatformUserDetailMapper detailMapper) {
    this.detailMapper = detailMapper;
  }

  public Map<String, Object> infoByIdCard(String idcard) {
    if (!StringUtils.hasText(idcard)) {
      throw new AppSmsException(-1, "证件号为空");
    }
    PlatformUserDetailEntity detail = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idcard.trim())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (detail == null) {
      throw new AppSmsException(-1, "该身份证号还未注册");
    }
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", detail.getId());
    info.put("phone", detail.getPhone());
    info.put("idcard", detail.getIdcard());
    info.put("realname", detail.getRealname());
    return info;
  }
}

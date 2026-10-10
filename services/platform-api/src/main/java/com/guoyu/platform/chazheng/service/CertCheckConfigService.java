package com.guoyu.platform.chazheng.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 查证配置服务 —— 从 sys_dict 表读第三方接口地址和请求模板。
 *
 * <p>配置项（由用户在 sys_dict 表中维护）：
 * <ul>
 *   <li>{@code chazheng_url}：第三方证书查询接口地址</li>
 *   <li>{@code chazheng_datas}：请求 JSON 模板数组（每轮一个元素）</li>
 * </ul>
 */
@Service
@Slf4j
public class CertCheckConfigService {

  private static final String CODE_URL = "chazheng_url";
  private static final String CODE_DATAS = "chazheng_datas";

  private final DictMapper dictMapper;
  private final ObjectMapper objectMapper;

  public CertCheckConfigService(DictMapper dictMapper, ObjectMapper objectMapper) {
    this.dictMapper = dictMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * 查询第三方接口 URL。
   *
   * @return 接口地址，未配置返回 null
   */
  public String findApiUrl(Long tenantId) {
    // tenantId 由调用方传入,避免工作线程调 SecurityContext
    DictEntity dict = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getCode, CODE_URL)
        .eq(DictEntity::getDeleted, 0));
    return dict != null ? dict.getValue() : null;
  }

  /**
   * 查询请求模板列表（JSON 反序列化）。
   *
   * <p>模板是一个 JSON 数组，每轮查证取一个元素。运行时用用户的 idcode/username
   * 覆盖模板中的对应字段后发送请求。
   *
   * @return 模板 Map 列表，未配置返回空列表
   */
  public List<Map<String, Object>> findRequestTemplates(Long tenantId) {
    // tenantId 由调用方传入,避免工作线程调 SecurityContext
    DictEntity dict = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getCode, CODE_DATAS)
        .eq(DictEntity::getDeleted, 0));
    if (dict == null || !org.springframework.util.StringUtils.hasText(dict.getValue())) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(dict.getValue(),
          new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception e) {
      log.error("解析 chazheng_datas JSON 模板失败 tenant={}", tenantId, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证请求模板 JSON 格式错误");
    }
  }
}
